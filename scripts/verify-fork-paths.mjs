#!/usr/bin/env node
/**
 * verify-fork-paths.mjs — todo caminho de arquivo citado pela skill tb-fork-build
 * realmente existe na versão alvo?
 *
 * Num fork, caminho inventado é a alucinação mais cara: parece plausível, passa na
 * revisão, e só falha depois de um build de Maven de dezenas de minutos. Pior ainda,
 * caminhos do ThingsBoard mudam entre versões — um mapa correto em 4.3 pode estar errado
 * em 4.5, sem nada avisar.
 *
 * Cruza os caminhos citados na skill contra a árvore real do repositório, numa ref fixada.
 * Funciona tanto contra o upstream quanto contra um fork (que é o caso de uso real:
 * "meu fork ainda tem todos esses arquivos?").
 *
 * Zero dependências. GITHUB_TOKEN opcional (evita rate limit em CI).
 *
 *   node scripts/verify-fork-paths.mjs
 *   node scripts/verify-fork-paths.mjs --ref v4.3.1.4
 *   node scripts/verify-fork-paths.mjs --repo Samuelvieria/ThigsBoardCE-TWmonitor --ref twmonitor-whitelabel
 *   node scripts/verify-fork-paths.mjs --update-header
 */

import { readFileSync, existsSync, readdirSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';

const DEFAULT_REPO = 'thingsboard/thingsboard';
const SKILL_DIR = '.claude/skills/tb-fork-build';
const MAP = join(SKILL_DIR, 'references/whitelabel-map.md');

const args = process.argv.slice(2);
const flag = (n) => { const i = args.indexOf('--' + n); if (i === -1) return undefined; const v = args[i + 1]; return v === undefined || v.startsWith('--') ? true : v; };
const AS_JSON = flag('json') === true;
const UPDATE = flag('update-header') === true;
const REPO = typeof flag('repo') === 'string' ? flag('repo') : DEFAULT_REPO;

const C = process.stdout.isTTY && !process.env.NO_COLOR
  ? { d: '\x1b[2m', r: '\x1b[0m', g: '\x1b[32m', y: '\x1b[33m', red: '\x1b[31m', b: '\x1b[1m' }
  : { d: '', r: '', g: '', y: '', red: '', b: '' };
const log = (s) => process.stderr.write(s + '\n');
const out = (s) => process.stdout.write(s + '\n');

const GH_HEADERS = { Accept: 'application/vnd.github+json', 'User-Agent': 'tb-skills-verify-fork' };
if (process.env.GITHUB_TOKEN) GH_HEADERS.Authorization = 'Bearer ' + process.env.GITHUB_TOKEN;

async function gh(path) {
  const res = await fetch('https://api.github.com' + path, { headers: GH_HEADERS });
  if (!res.ok) {
    const extra = res.headers.get('x-ratelimit-remaining') === '0' ? ' (rate limit — defina GITHUB_TOKEN)' : '';
    throw new Error('GET ' + path + ' -> HTTP ' + res.status + extra);
  }
  return res.json();
}

/**
 * Extrai caminhos de arquivo dos spans em crase de todos os .md da skill.
 * Só conta o que parece caminho de verdade: tem barra e extensão conhecida, ou é um
 * diretório terminado em barra. Evita capturar `$tb-primary-color` ou `mat.m2-...`.
 */
const FILE_RE = /^[\w][\w./@-]*\/[\w./@-]*\.(ts|html|scss|css|json|xml|ico|svg|ftl|java|sh|yml|yaml|md)$/;
const DIR_RE = /^[\w][\w./@-]*\/$/;

function claimedPaths() {
  const found = new Map();
  const files = [];
  const walk = (d) => {
    for (const e of readdirSync(d, { withFileTypes: true })) {
      const p = join(d, e.name);
      if (e.isDirectory()) walk(p);
      else if (e.name.endsWith('.md')) files.push(p);
    }
  };
  walk(SKILL_DIR);

  for (const f of files) {
    const md = readFileSync(f, 'utf8');
    // \r? porque os arquivos do repo estão em CRLF
    const spans = [...md.matchAll(/`([^`\r\n]+)`/g)].map((m) => m[1].trim());
    for (const s of spans) {
      if (!FILE_RE.test(s) && !DIR_RE.test(s)) continue;
      if (s.startsWith('scripts/') || s.startsWith('.claude/') || s.startsWith('references/')) continue;
      if (!found.has(s)) found.set(s, []);
      found.get(s).push(f);
    }
  }
  return found;
}

function updateHeader(md, meta) {
  const header = [
    '# Mapa de white-label do ThingsBoard CE',
    '',
    '<!-- PROVENANCE:BEGIN (gerado por scripts/verify-fork-paths.mjs — não editar à mão) -->',
    '> **Proveniência fixada.** Todos os caminhos abaixo verificados contra',
    '> [`' + meta.repo + '`](https://github.com/' + meta.repo + '/tree/' + meta.sha + '),',
    '> ref **`' + meta.ref + '`**, commit **`' + meta.sha.slice(0, 12) + '`**, em **' + meta.date + '**.',
    '> ' + meta.checked + ' caminhos conferidos contra a árvore real do repositório.',
    '>',
    '> Reverificar noutra versão: `node scripts/verify-fork-paths.mjs --ref vX.Y.Z`.',
    '> Conferir contra o seu fork: `--repo <owner>/<fork> --ref <branch>`.',
    '<!-- PROVENANCE:END -->',
  ].join('\n');
  const begin = md.indexOf('<!-- PROVENANCE:BEGIN');
  const endTag = '<!-- PROVENANCE:END -->';
  if (begin !== -1) return header + md.slice(md.indexOf(endTag) + endTag.length);
  return header + '\n\n' + md;
}

async function main() {
  let ref = flag('ref');
  if (!ref || ref === true) {
    try { ref = (await gh('/repos/' + REPO + '/releases/latest')).tag_name; }
    catch { ref = (await gh('/repos/' + REPO)).default_branch; }
  }

  let sha;
  try { sha = (await gh('/repos/' + REPO + '/commits/' + encodeURIComponent(String(ref)))).sha; }
  catch (e) {
    if (!/HTTP (404|422)/.test(e.message)) throw e;
    log(C.red + 'erro' + C.r + ' ref "' + ref + '" não existe em ' + REPO);
    try { log('       tags recentes: ' + (await gh('/repos/' + REPO + '/tags?per_page=10')).map((t) => t.name).join(', ')); } catch {}
    process.exitCode = 2;
    return;
  }
  log(C.b + 'repo' + C.r + '  ' + REPO + '  ' + C.b + 'ref' + C.r + '  ' + ref + '  ' + C.d + sha.slice(0, 12) + C.r);

  const tree = await gh('/repos/' + REPO + '/git/trees/' + sha + '?recursive=1');
  if (tree.truncated) log(C.y + '! árvore truncada pela API — um "ausente" pode ser falso positivo' + C.r);
  const blobs = new Set(tree.tree.filter((e) => e.type === 'blob').map((e) => e.path));
  const dirs = new Set(tree.tree.filter((e) => e.type === 'tree').map((e) => e.path + '/'));
  log(C.b + 'arvore' + C.r + '  ' + blobs.size + ' arquivos');

  if (!existsSync(SKILL_DIR)) { log(C.red + 'erro' + C.r + ' ' + SKILL_DIR + ' não existe'); process.exitCode = 1; return; }
  const claimed = claimedPaths();
  if (!claimed.size) { log(C.red + 'erro' + C.r + ' nenhum caminho extraído — regex de extração quebrada'); process.exitCode = 2; return; }
  log(C.b + 'skill' + C.r + '   ' + claimed.size + ' caminhos citados');

  const missing = [];
  for (const [p, sources] of claimed) {
    if (blobs.has(p) || dirs.has(p)) continue;
    missing.push({ path: p, citedIn: [...new Set(sources)] });
  }
  missing.sort((a, b) => a.path.localeCompare(b.path));

  const report = {
    repo: REPO, ref, sha, date: new Date().toISOString().slice(0, 10),
    checked: claimed.size, missing, clean: missing.length === 0,
  };

  if (UPDATE && report.clean) {
    writeFileSync(MAP, updateHeader(readFileSync(MAP, 'utf8'), report));
    log(C.g + 'ok' + C.r + ' proveniência fixada em ' + MAP);
  } else if (UPDATE) {
    log(C.y + '! não fixei a proveniência: há caminho ausente. Corrija antes.' + C.r);
  }

  if (AS_JSON) out(JSON.stringify(report, null, 2));
  else {
    out('');
    if (missing.length) {
      out(C.red + 'CAMINHOS QUE NÃO EXISTEM' + C.r + ' (' + missing.length + ') em ' + REPO + '@' + ref + ':');
      for (const m of missing) {
        out('  ' + m.path);
        out('    ' + C.d + 'citado em: ' + m.citedIn.join(', ') + C.r);
      }
      out('');
      out(C.d + '  Ou o caminho está errado, ou mudou nesta versão. Num fork, também pode' + C.r);
      out(C.d + '  significar que o arquivo foi removido pela customização.' + C.r);
    } else {
      out(C.g + 'todos os ' + claimed.size + ' caminhos existem em ' + REPO + '@' + ref + C.r);
    }
  }

  process.exitCode = missing.length ? 1 : 0;
}

main().catch((e) => {
  process.stderr.write(C.red + 'erro' + C.r + ' ' + (e.stack || e.message) + '\n');
  process.exitCode = 2;
});
