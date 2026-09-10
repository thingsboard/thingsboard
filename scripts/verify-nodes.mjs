#!/usr/bin/env node
/**
 * verify-nodes.mjs — reverifica o catálogo de rule nodes contra o código-fonte upstream.
 *
 * O trecho de maior valor do pack (references/node-types.md) é também o que apodrece mais
 * rápido: foi verificado à mão contra a branch `master` numa data. Sem âncora de versão e
 * sem reverificação, daqui a alguns releases ele continua afirmando "verificado no
 * código-fonte oficial" com a mesma autoridade e nenhuma forma de saber contra o quê.
 *
 * Este script transforma aquele trabalho manual em invariante:
 *   1. resolve uma ref (tag de release, default: a última) para um commit SHA
 *   2. lê a anotação @RuleNode de cada classe em rule-engine-components
 *   3. diffa contra o catálogo em markdown
 *   4. --update-header grava a proveniência fixada (tag + SHA + data) no cabeçalho
 *
 * Zero dependências. GITHUB_TOKEN opcional (evita rate limit em CI).
 *
 *   node scripts/verify-nodes.mjs
 *   node scripts/verify-nodes.mjs --ref v4.3.0 --json
 *   node scripts/verify-nodes.mjs --update-header
 */

import { readFileSync, writeFileSync, existsSync } from 'node:fs';

const REPO = 'thingsboard/thingsboard';
const SRC_PREFIX = 'rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/';
const CATALOG = '.claude/skills/tb-rule-engine/references/node-types.md';
const DEFAULT_RELATIONS = ['Success', 'Failure'];

/** org.thingsboard.rule.engine.api.TbNodeConnectionType — constante -> label real. */
const CONNECTION_CONSTANTS = {
  SUCCESS: 'Success', FAILURE: 'Failure', TRUE: 'True', FALSE: 'False', OTHER: 'Other',
};

/** Heading a partir do qual o catálogo documenta nodes PE (ausentes do source CE). */
const PE_SECTION_RE = /^##\s+.*(instância PE|instancia PE|PE real)/i;

const args = process.argv.slice(2);
const flag = (n) => {
  const i = args.indexOf('--' + n);
  if (i === -1) return undefined;
  const v = args[i + 1];
  return v === undefined || v.startsWith('--') ? true : v;
};
const AS_JSON = flag('json') === true;
const UPDATE = flag('update-header') === true;

const C = process.stdout.isTTY && !process.env.NO_COLOR
  ? { d: '\x1b[2m', r: '\x1b[0m', g: '\x1b[32m', y: '\x1b[33m', red: '\x1b[31m', b: '\x1b[1m' }
  : { d: '', r: '', g: '', y: '', red: '', b: '' };

const log = (s) => process.stderr.write(s + '\n');

/* ------------------------------------------------------------------ fetch */

const GH_HEADERS = {
  Accept: 'application/vnd.github+json',
  'User-Agent': 'tb-skills-verify-nodes',
};
if (process.env.GITHUB_TOKEN) GH_HEADERS.Authorization = 'Bearer ' + process.env.GITHUB_TOKEN;

async function gh(path) {
  const res = await fetch('https://api.github.com' + path, { headers: GH_HEADERS });
  if (!res.ok) {
    const remaining = res.headers.get('x-ratelimit-remaining');
    const extra = remaining === '0' ? ' (rate limit da API do GitHub esgotado — defina GITHUB_TOKEN)' : '';
    throw new Error('GET ' + path + ' -> HTTP ' + res.status + extra);
  }
  return res.json();
}

async function rawFile(sha, path) {
  const res = await fetch('https://raw.githubusercontent.com/' + REPO + '/' + sha + '/' + path, {
    headers: { 'User-Agent': 'tb-skills-verify-nodes' },
  });
  if (!res.ok) throw new Error('raw ' + path + ' -> HTTP ' + res.status);
  return res.text();
}

async function pool(items, limit, fn) {
  const results = new Array(items.length);
  let next = 0;
  const workers = Array.from({ length: Math.min(limit, items.length) }, async () => {
    while (true) {
      const i = next++;
      if (i >= items.length) return;
      try { results[i] = await fn(items[i], i); } catch (e) { results[i] = { _error: e.message }; }
    }
  });
  await Promise.all(workers);
  return results;
}

/* ----------------------------------------------------------- java parsing */

/** Extrai o conteúdo de @RuleNode( ... ) balanceando parênteses. */
function ruleNodeBlock(src) {
  const at = src.indexOf('@RuleNode');
  if (at === -1) return null;
  const open = src.indexOf('(', at);
  if (open === -1) return null;
  let depth = 0;
  let inStr = false;
  let esc = false;
  for (let i = open; i < src.length; i++) {
    const ch = src[i];
    if (inStr) {
      if (esc) esc = false;
      else if (ch === '\\') esc = true;
      else if (ch === '"') inStr = false;
      continue;
    }
    if (ch === '"') { inStr = true; continue; }
    if (ch === '(') depth++;
    else if (ch === ')') { depth--; if (depth === 0) return { body: src.slice(open + 1, i), end: i }; }
  }
  return null;
}

function parseJava(path, src) {
  const blk = ruleNodeBlock(src);
  if (!blk) return null;
  const body = blk.body;

  const pkg = (src.match(/^\s*package\s+([\w.]+)\s*;/m) || [])[1];
  const cls = (src.slice(blk.end).match(/\bclass\s+(\w+)/) || [])[1];
  if (!pkg || !cls) return null;

  const type = (body.match(/\btype\s*=\s*ComponentType\.(\w+)/) || [])[1] || null;
  const name = (body.match(/\bname\s*=\s*"((?:[^"\\]|\\.)*)"/) || [])[1] || null;
  const custom = /\bcustomRelations\s*=\s*true/.test(body);

  const relMatch = body.match(/\brelationTypes\s*=\s*\{([\s\S]*?)\}/);
  let declared = null;
  if (relMatch) {
    declared = [];
    // As entradas misturam literais ("Rate limited") e constantes
    // (TbNodeConnectionType.SUCCESS) — parsear só strings entre aspas perde metade
    // dos labels e produz falso positivo de divergência em quase todo filter node.
    const items = relMatch[1].replace(/\/\/[^\n]*/g, '').split(',');
    for (const it of items) {
      const t = it.trim();
      if (!t) continue;
      const lit = t.match(/^"((?:[^"\\]|\\.)*)"$/);
      if (lit) { declared.push(lit[1]); continue; }
      const konst = t.match(/TbNodeConnectionType\.(\w+)/);
      if (konst) { declared.push(CONNECTION_CONSTANTS[konst[1]] || konst[1]); continue; }
    }
  }

  // `relationTypes = {}` explicitamente vazio é o marcador upstream de conexões
  // calculadas em runtime (AnnotationComponentDiscoveryService as injeta depois).
  const dynamic = !!(relMatch && declared.length === 0);

  return {
    fqn: pkg + '.' + cls,
    file: path,
    componentType: type,
    displayName: name,
    customRelations: custom,
    dynamic,
    relationTypes: dynamic ? null : (declared && declared.length ? declared : DEFAULT_RELATIONS),
    deprecated: /@Deprecated/.test(src.slice(0, src.indexOf('@RuleNode'))) || /\(deprecated\)/.test(name || ''),
  };
}

/* ------------------------------------------------------- markdown parsing */

function parseCatalog(md) {
  const entries = new Map();
  let inPeSection = false;
  for (const line of md.split(/\r?\n/)) {
    if (line.startsWith('## ')) inPeSection = PE_SECTION_RE.test(line);
    if (!line.trim().startsWith('|')) continue;
    const cells = line.split('|').slice(1, -1).map((c) => c.trim());
    if (cells.length < 2) continue;
    const joined = cells.join(' | ');
    // Uma linha pode documentar mais de uma classe ("synchronization start/end").
    // Capturar só a primeira faz o par silencioso aparecer como FALTANDO para sempre.
    const fqns = Array.from(joined.matchAll(/`(org\.thingsboard\.rule\.engine\.[\w.]*\w+Node)`/g)).map((m) => m[1]);
    if (!fqns.length) continue;
    const relCell = cells.length >= 3 ? cells[2] : null;
    let relationTypes = null;
    let dynamic = false;
    if (relCell) {
      if (/din[aâ]mic/i.test(relCell) || /nome do /i.test(relCell)) dynamic = true;
      const labels = Array.from(relCell.matchAll(/`([^`]+)`/g)).map((m) => m[1]);
      if (labels.length) relationTypes = labels;
    }
    for (const fqn of fqns) {
      entries.set(fqn, { fqn, displayName: cells[0], relationTypes, dynamic, pe: inPeSection, hasRelCol: cells.length >= 3 });
    }
  }
  return entries;
}

/* -------------------------------------------------------------- provenance */

function updateHeader(md, meta) {
  const header = [
    '# Catálogo de rule nodes — verificado no código-fonte oficial (CE)',
    '',
    '<!-- PROVENANCE:BEGIN (gerado por scripts/verify-nodes.mjs — não editar à mão) -->',
    '> **Proveniência fixada.** Fonte: [`' + REPO + '`](https://github.com/' + REPO + '/tree/' + meta.sha + '/' + SRC_PREFIX + '),',
    '> ref **`' + meta.ref + '`**, commit **`' + meta.sha.slice(0, 12) + '`**, verificado em **' + meta.date + '**.',
    '> Extraído da anotação `@RuleNode` de ' + meta.classes + ' classes (`type`, `name`, `relationTypes`).',
    '>',
    '> Reverificar contra um release mais novo: `node scripts/verify-nodes.mjs --ref vX.Y.Z`.',
    '> CI mensal em `.github/workflows/verify-catalog.yml` abre issue quando o upstream diverge.',
    '<!-- PROVENANCE:END -->',
  ].join('\n');

  const begin = md.indexOf('<!-- PROVENANCE:BEGIN');
  if (begin !== -1) {
    const end = md.indexOf('<!-- PROVENANCE:END -->');
    const tail = md.slice(end + '<!-- PROVENANCE:END -->'.length);
    return header + tail;
  }
  // primeira vez: substitui o H1 + o bloco de citação original que vinha logo abaixo
  const lines = md.split(/\r?\n/);
  let i = 0;
  while (i < lines.length && !lines[i].startsWith('# ')) i++;
  i++;
  while (i < lines.length && (lines[i].trim() === '' || lines[i].trim().startsWith('>'))) i++;
  return header + '\n\n' + lines.slice(i).join('\n');
}

/* -------------------------------------------------------------------- main */

async function main() {
  let ref = flag('ref');
  let sha;

  if (!ref || ref === true) {
    log(C.d + 'resolvendo último release de ' + REPO + '...' + C.r);
    try {
      const rel = await gh('/repos/' + REPO + '/releases/latest');
      ref = rel.tag_name;
    } catch {
      const tags = await gh('/repos/' + REPO + '/tags?per_page=1');
      ref = tags[0].name;
    }
  }

  let refInfo;
  try {
    refInfo = await gh('/repos/' + REPO + '/commits/' + encodeURIComponent(String(ref)));
  } catch (e) {
    // O ThingsBoard versiona com 4 componentes (v4.3.1.4), então "v4.3.0" quase sempre
    // não existe como tag. Sem isto o usuário só vê um HTTP 422 cru.
    if (!/HTTP (404|422)/.test(e.message)) throw e;
    log(C.red + 'erro' + C.r + ' ref "' + ref + '" não existe em ' + REPO);
    try {
      const tags = await gh('/repos/' + REPO + '/tags?per_page=12');
      log('       tags recentes: ' + tags.map((t) => t.name).join(', '));
    } catch { /* sem rede pra listar; a mensagem principal já basta */ }
    log('       omita --ref para usar o último release automaticamente.');
    process.exitCode = 2;
    return;
  }
  sha = refInfo.sha;
  log(C.b + 'ref' + C.r + '  ' + ref + '  ' + C.d + '-> ' + sha + C.r);

  log(C.d + 'listando árvore...' + C.r);
  const tree = await gh('/repos/' + REPO + '/git/trees/' + sha + '?recursive=1');
  if (tree.truncated) log(C.y + '! árvore truncada pela API — resultado pode estar incompleto' + C.r);
  const files = tree.tree
    .filter((e) => e.type === 'blob' && e.path.startsWith(SRC_PREFIX) && e.path.endsWith('.java'))
    .map((e) => e.path);

  if (!files.length) {
    log(C.red + 'erro' + C.r + ' nenhum .java sob ' + SRC_PREFIX + ' em ' + ref);
    log('       o caminho do módulo pode ter mudado nessa versão — ajuste SRC_PREFIX.');
    process.exitCode = 1; return;
  }
  log(C.d + files.length + ' arquivos java, baixando (concorrência 8)...' + C.r);

  const sources = await pool(files, 8, (p) => rawFile(sha, p));
  const nodes = [];
  let failed = 0;
  sources.forEach((src, i) => {
    if (typeof src !== 'string') { failed++; return; }
    const n = parseJava(files[i], src);
    if (n) nodes.push(n);
  });
  if (failed) log(C.y + '! ' + failed + ' arquivo(s) falharam no download' + C.r);
  log(C.b + 'upstream' + C.r + '  ' + nodes.length + ' classes com @RuleNode');

  if (!existsSync(CATALOG)) {
    log(C.red + 'erro' + C.r + ' catálogo não encontrado em ' + CATALOG);
    process.exitCode = 1; return;
  }
  const md = readFileSync(CATALOG, 'utf8');
  const catalog = parseCatalog(md);
  log(C.b + 'catálogo' + C.r + '  ' + catalog.size + ' nodes documentados');

  const upstreamByFqn = new Map(nodes.map((n) => [n.fqn, n]));

  const missing = nodes
    .filter((n) => !catalog.has(n.fqn))
    .map((n) => ({ fqn: n.fqn, displayName: n.displayName, componentType: n.componentType, relationTypes: n.relationTypes }));

  // Nodes documentados na seção PE são closed-source por definição: ausência do repo CE
  // é o estado esperado, não drift.
  const stale = [...catalog.entries()]
    .filter(([f, c]) => !upstreamByFqn.has(f) && !c.pe)
    .map(([f, c]) => ({ fqn: f, displayName: c.displayName }));

  const relationDrift = [];
  for (const [fqn, cat] of catalog) {
    const up = upstreamByFqn.get(fqn);
    if (!up || !cat.hasRelCol) continue;
    if (up.dynamic || cat.dynamic) {
      // Caso legítimo: o node declara labels fixos E gera labels dinâmicos (switch por
      // profile declara {"default"} e emite um label por nome de profile). O catálogo
      // descreve os dois; upstream só consegue declarar o fixo. Não é drift desde que
      // todo label declarado upstream esteja documentado.
      if (cat.dynamic && !up.dynamic && up.relationTypes) {
        const documented = cat.relationTypes || [];
        if (up.relationTypes.every((l) => documented.includes(l))) continue;
      }
      if (up.dynamic !== cat.dynamic) {
        relationDrift.push({
          fqn, issue: 'dinamicidade',
          upstream: up.dynamic ? 'dinâmico (relationTypes={})' : 'fixo: ' + JSON.stringify(up.relationTypes),
          catalogo: cat.dynamic ? 'dinâmico' : JSON.stringify(cat.relationTypes),
        });
      }
      continue;
    }
    if (!cat.relationTypes) continue;
    const a = [...up.relationTypes].sort().join(',');
    const b = [...cat.relationTypes].sort().join(',');
    if (a !== b) relationDrift.push({ fqn, issue: 'relationTypes', upstream: up.relationTypes, catalogo: cat.relationTypes });
  }

  const report = {
    ref, sha, date: new Date().toISOString().slice(0, 10),
    upstreamClasses: nodes.length, catalogEntries: catalog.size,
    missing, stale, relationDrift,
    clean: missing.length === 0 && stale.length === 0 && relationDrift.length === 0,
  };

  if (UPDATE) {
    writeFileSync(CATALOG, updateHeader(md, { ref, sha, date: report.date, classes: nodes.length }));
    log(C.g + 'ok' + C.r + ' proveniência fixada em ' + CATALOG + ' (' + ref + ' @ ' + sha.slice(0, 12) + ')');
  }

  if (AS_JSON) { process.stdout.write(JSON.stringify(report, null, 2) + '\n'); }
  else {
    const out = (s) => process.stdout.write(s + '\n');
    out('');
    if (missing.length) {
      out(C.y + 'FALTANDO no catálogo' + C.r + ' (' + missing.length + ') — existem no source, não documentados:');
      for (const m of missing.slice(0, 40)) out('  ' + (m.componentType || '?').padEnd(15) + ' ' + m.fqn + '  ' + C.d + (m.displayName || '') + C.r);
      if (missing.length > 40) out('  ' + C.d + '... +' + (missing.length - 40) + C.r);
      out('');
    }
    if (stale.length) {
      out(C.y + 'OBSOLETOS' + C.r + ' (' + stale.length + ') — no catálogo, ausentes do source nesta ref:');
      for (const s of stale) out('  ' + s.fqn + '  ' + C.d + s.displayName + C.r);
      out(C.d + '  (esperado para nodes PE closed-source — confira se estão na seção PE)' + C.r);
      out('');
    }
    if (relationDrift.length) {
      out(C.red + 'DIVERGÊNCIA de connections[].type' + C.r + ' (' + relationDrift.length + ') — a classe de erro mais cara:');
      for (const d of relationDrift) {
        out('  ' + d.fqn);
        out('    upstream: ' + JSON.stringify(d.upstream));
        out('    catálogo: ' + JSON.stringify(d.catalogo));
      }
      out('');
    }
    if (report.clean) out(C.g + 'catálogo em dia com ' + ref + ' @ ' + sha.slice(0, 12) + C.r);
    else out(C.d + 'resumo: ' + missing.length + ' faltando, ' + stale.length + ' obsoletos, ' + relationDrift.length + ' divergências' + C.r);
  }

  // relationDrift é o único que quebra o build: connections[].type errado falha em silêncio
  // em produção. missing/stale são informativos (PE closed-source aparece como obsoleto).
  process.exitCode = relationDrift.length ? 1 : 0;
}

main().catch((e) => {
  process.stderr.write(C.red + 'erro' + C.r + ' ' + (e.stack || e.message) + '\n');
  process.exitCode = 2;
});
