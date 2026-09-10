#!/usr/bin/env node
/**
 * verify-gateway.mjs — os conectores e chaves de config documentados existem mesmo?
 *
 * O modo de falha específico deste domínio: conector que não existe na versão instalada.
 * O gateway sobe normalmente, ignora a entrada desconhecida, e nada acontece — sem erro.
 * Pior, a lista de conectores do `master` não é a da release: `s7` está no master e não
 * no 3.8.4. Quem lê o repositório e instala via pip encontra coisa diferente.
 *
 * Fontes da verdade, ambas do repositório oficial na ref alvo:
 *   thingsboard_gateway/connectors/<nome>/   -> conectores realmente distribuídos
 *   thingsboard_gateway/config/tb_gateway.json -> chaves de configuração reais
 *
 * Zero dependências. GITHUB_TOKEN opcional (evita rate limit em CI).
 *
 *   node scripts/verify-gateway.mjs
 *   node scripts/verify-gateway.mjs --ref 3.8.4 --json
 *   node scripts/verify-gateway.mjs --update-header
 */

import { readFileSync, existsSync, writeFileSync } from 'node:fs';

const REPO = 'thingsboard/thingsboard-gateway';
const CONNECTORS_DIR = 'thingsboard_gateway/connectors/';
const CONFIG_DIR = 'thingsboard_gateway/config/';
const DOC = '.claude/skills/tb-gateway/references/connectors.md';
const SKILL = '.claude/skills/tb-gateway/SKILL.md';

// A skill também documenta o protocolo MQTT de gateway, que é definido na PLATAFORMA e
// não no serviço gateway. Tópico errado aqui falha em silêncio: o broker aceita o publish
// e a mensagem nunca vira telemetria.
const TB_REPO = 'thingsboard/thingsboard';
const TB_REF = 'v4.3.1.4';
const TOPICS_SRC = 'common/data/src/main/java/org/thingsboard/server/common/data/device/profile/MqttTopics.java';
const TB_YML = 'application/src/main/resources/thingsboard.yml';

/**
 * Conectores citados nos docs que sabidamente NÃO estão em toda release. Documentá-los é
 * correto — o ponto é justamente avisar. Não podem contar como erro, mas a seção
 * "Disponibilidade por versão" precisa mencioná-los.
 */
const VERSION_DEPENDENT = new Set(['s7']);

const args = process.argv.slice(2);
const flag = (n) => { const i = args.indexOf('--' + n); if (i === -1) return undefined; const v = args[i + 1]; return v === undefined || v.startsWith('--') ? true : v; };
const AS_JSON = flag('json') === true;
const UPDATE = flag('update-header') === true;

const C = process.stdout.isTTY && !process.env.NO_COLOR
  ? { d: '\x1b[2m', r: '\x1b[0m', g: '\x1b[32m', y: '\x1b[33m', red: '\x1b[31m', b: '\x1b[1m' }
  : { d: '', r: '', g: '', y: '', red: '', b: '' };
const log = (s) => process.stderr.write(s + '\n');
const out = (s) => process.stdout.write(s + '\n');

const GH = { Accept: 'application/vnd.github+json', 'User-Agent': 'tb-skills-verify-gateway' };
if (process.env.GITHUB_TOKEN) GH.Authorization = 'Bearer ' + process.env.GITHUB_TOKEN;

async function gh(path) {
  const res = await fetch('https://api.github.com' + path, { headers: GH });
  if (!res.ok) {
    const extra = res.headers.get('x-ratelimit-remaining') === '0' ? ' (rate limit — defina GITHUB_TOKEN)' : '';
    throw new Error('GET ' + path + ' -> HTTP ' + res.status + extra);
  }
  return res.json();
}

async function rawFile2(repo, ref, path) {
  const res = await fetch('https://raw.githubusercontent.com/' + repo + '/' + ref + '/' + path,
    { headers: { 'User-Agent': 'tb-skills-verify-gateway' } });
  if (!res.ok) throw new Error('raw ' + repo + '/' + path + ' -> HTTP ' + res.status);
  return res.text();
}

const rawFile = (ref, path) => rawFile2(REPO, ref, path);

/**
 * Recorta uma seção do markdown pelo título, até o próximo heading de mesmo nível.
 * Sem isso a extração de conector captura as tabelas de campos do Modbus e de tipos de
 * storage — `tag`, `address`, `memory`, `sqlite` viram "conectores inexistentes".
 */
function section(md, titleRe, level = '##') {
  const lines = md.split(/\r?\n/);
  const start = lines.findIndex((l) => l.startsWith(level + ' ') && titleRe.test(l));
  if (start === -1) return '';
  const rest = lines.slice(start + 1);
  const end = rest.findIndex((l) => l.startsWith(level + ' '));
  return (end === -1 ? rest : rest.slice(0, end)).join('\n');
}

/** Nomes de conector — só da seção de catálogo, onde a 1ª coluna é o conector. */
function claimedConnectors(md) {
  const known = new Set();
  const scope = section(md, /Cat[áa]logo/i) + '\n' + section(md, /Disponibilidade por vers/i);
  for (const m of scope.matchAll(/^\|\s*`([a-z0-9_]+)`\s*\|/gm)) known.add(m[1]);
  return known;
}

/** Chaves de config citadas: `thingsboard.host`, `storage.type`, `security.accessToken`... */
function claimedConfigKeys(md) {
  const keys = new Set();
  for (const m of md.matchAll(/`([a-z][a-zA-Z0-9]*(?:\.[a-zA-Z][a-zA-Z0-9]*)+)`/g)) {
    const k = m[1];
    if (/\.(json|py|md|yaml|yml|sh)$/.test(k)) continue;   // é nome de arquivo, não chave
    keys.add(k);
  }
  return keys;
}

/** Achata o JSON de config em caminhos pontilhados, entrando no 1º item de cada array. */
function flatten(obj, prefix = '', acc = new Set()) {
  if (Array.isArray(obj)) { if (obj.length) flatten(obj[0], prefix, acc); return acc; }
  if (obj && typeof obj === 'object') {
    for (const [k, v] of Object.entries(obj)) {
      const p = prefix ? prefix + '.' + k : k;
      acc.add(p);
      flatten(v, p, acc);
    }
  }
  return acc;
}

function updateHeader(md, meta) {
  const header = [
    '# Conectores do ThingsBoard IoT Gateway',
    '',
    '<!-- PROVENANCE:BEGIN (gerado por scripts/verify-gateway.mjs — não editar à mão) -->',
    '> **Proveniência fixada.** Verificado contra',
    '> [`' + REPO + '`](https://github.com/' + REPO + '/tree/' + meta.sha + '/' + CONNECTORS_DIR + '),',
    '> release **`' + meta.ref + '`**, commit **`' + meta.sha.slice(0, 12) + '`**, em **' + meta.date + '**.',
    '> ' + meta.connectors + ' conectores distribuídos nessa release; ' + meta.keys + ' chaves de config conferidas.',
    '>',
    '> Reverificar noutra versão: `node scripts/verify-gateway.mjs --ref X.Y.Z`.',
    '<!-- PROVENANCE:END -->',
  ].join('\n');
  const begin = md.indexOf('<!-- PROVENANCE:BEGIN');
  const endTag = '<!-- PROVENANCE:END -->';
  if (begin !== -1) return header + md.slice(md.indexOf(endTag) + endTag.length);
  return header + '\n\n' + md;
}

async function main() {
  let ref = flag('ref');
  if (!ref || ref === true) ref = (await gh('/repos/' + REPO + '/releases/latest')).tag_name;

  let sha;
  try { sha = (await gh('/repos/' + REPO + '/commits/' + encodeURIComponent(String(ref)))).sha; }
  catch (e) {
    if (!/HTTP (404|422)/.test(e.message)) throw e;
    log(C.red + 'erro' + C.r + ' ref "' + ref + '" não existe em ' + REPO);
    try { log('       releases: ' + (await gh('/repos/' + REPO + '/releases?per_page=8')).map((r) => r.tag_name).join(', ')); } catch {}
    process.exitCode = 2;
    return;
  }
  log(C.b + 'gateway' + C.r + '  ' + REPO + '  ref ' + ref + '  ' + C.d + sha.slice(0, 12) + C.r);

  const tree = await gh('/repos/' + REPO + '/git/trees/' + sha + '?recursive=1');
  const real = new Set();
  for (const e of tree.tree) {
    const m = e.path.match(new RegExp('^' + CONNECTORS_DIR.replace(/\//g, '\\/') + '([a-z0-9_]+)\\/'));
    if (m) real.add(m[1]);
  }
  log(C.b + 'upstream' + C.r + ' ' + real.size + ' conectores nesta release');

  if (!existsSync(DOC) || !existsSync(SKILL)) {
    log(C.red + 'erro' + C.r + ' arquivos da skill tb-gateway não encontrados');
    process.exitCode = 1; return;
  }
  const docMd = readFileSync(DOC, 'utf8');
  const skillMd = readFileSync(SKILL, 'utf8');
  const bothMd = docMd + '\n' + skillMd;

  const claimed = claimedConnectors(docMd);
  if (!claimed.size) { log(C.red + 'erro' + C.r + ' nenhum conector extraído — regex quebrada'); process.exitCode = 2; return; }

  const inexistentes = [...claimed].filter((c) => !real.has(c) && !VERSION_DEPENDENT.has(c)).sort();
  const naoDocumentados = [...real].filter((c) => !claimed.has(c) && !VERSION_DEPENDENT.has(c)).sort();

  // Um conector marcado como dependente de versão precisa estar de fato explicado,
  // senão a ressalva some do doc e vira afirmação errada.
  const avisoFaltando = [...VERSION_DEPENDENT].filter(
    (c) => bothMd.includes('`' + c + '`') && !/Disponibilidade por vers/i.test(docMd));

  const cfg = JSON.parse(await rawFile(sha, CONFIG_DIR + 'tb_gateway.json'));
  const realKeys = flatten(cfg);
  const claimedKeys = claimedConfigKeys(bothMd);
  const chavesInexistentes = [...claimedKeys].filter((k) => {
    if (realKeys.has(k)) return false;
    // aceita citação por sufixo: "security.accessToken" existe como "thingsboard.security.accessToken"
    for (const r of realKeys) if (r === k || r.endsWith('.' + k)) return false;
    return true;
  }).sort();
  log(C.b + 'config' + C.r + '   ' + realKeys.size + ' chaves reais, ' + claimedKeys.size + ' citadas na skill');

  // --- protocolo MQTT de gateway: definido na plataforma, não no serviço gateway ---
  const topicsJava = await rawFile2(TB_REPO, TB_REF, TOPICS_SRC);
  const consts = Object.fromEntries(
    [...topicsJava.matchAll(/(?:private|public)\s+static\s+final\s+String\s+(\w+)\s*=\s*([^;]+);/g)]
      .map((m) => [m[1], m[2].trim()]));
  // resolve concatenação de constantes: BASE_GATEWAY_API_TOPIC + TELEMETRY
  const resolve = (expr, depth = 0) => {
    if (depth > 6) return null;
    return expr.split('+').map((p) => {
      const t = p.trim();
      const lit = t.match(/^"(.*)"$/);
      if (lit) return lit[1];
      return consts[t] !== undefined ? resolve(consts[t], depth + 1) : null;
    }).reduce((a, b) => (a === null || b === null ? null : a + b), '');
  };
  // Os BASE_* ("v1/gateway", "v1/devices/me") sao prefixos, nao topicos. Deixa-los no
  // conjunto faz a tolerancia por prefixo aceitar QUALQUER coisa sob eles — testado:
  // "v1/gateway/desconectar" passava batido.
  const BASES = new Set(['v1/gateway', 'v1/devices/me', 'v1/devices', 'v2/devices/me']);
  const realTopics = new Set();
  for (const [k, v] of Object.entries(consts)) {
    if (!/GATEWAY|DEVICE_(TELEMETRY|ATTRIBUTES)_TOPIC/.test(k)) continue;
    const t = resolve(v);
    if (t && t.startsWith('v1/') && !BASES.has(t.replace(/\/$/, ''))) realTopics.add(t);
  }

  const claimedTopics = new Set(
    [...bothMd.matchAll(/`(v1\/(?:gateway|devices)\/[\w/+#.-]*)`/g)].map((m) => m[1])
      .filter((t) => !BASES.has(t.replace(/\/$/, ''))));
  const topicosInexistentes = [...claimedTopics].filter((t) => {
    const norm = (s) => s.replace(/\/$/, '');
    if (realTopics.has(t) || realTopics.has(norm(t))) return false;
    // tolera sufixo de wildcard sobre um topico real COMPLETO (ex. .../response/+),
    // exigindo a barra para nao casar "disconnect" com "desconectar" por prefixo parcial
    for (const r of realTopics) {
      if (norm(t).startsWith(norm(r) + '/') || norm(r).startsWith(norm(t) + '/')) return false;
    }
    return true;
  }).sort();

  // --- limite de payload MQTT, citado na skill como número concreto ---
  const yml = await rawFile2(TB_REPO, TB_REF, TB_YML);
  const maxPayload = (yml.match(/NETTY_MAX_PAYLOAD_SIZE:(\d+)/) || [])[1] || null;
  const payloadErrado = maxPayload && /NETTY_MAX_PAYLOAD_SIZE/.test(bothMd) && !bothMd.includes(maxPayload);
  log(C.b + 'protocolo' + C.r + ' ' + realTopics.size + ' tópicos MQTT no fonte da plataforma, '
    + claimedTopics.size + ' citados' + (maxPayload ? ' | payload máx ' + maxPayload : ''));

  const report = {
    repo: REPO, ref, sha, date: new Date().toISOString().slice(0, 10),
    connectorsUpstream: real.size, connectorsDocumented: claimed.size,
    inexistentes, naoDocumentados, chavesInexistentes, avisoFaltando,
    topicosInexistentes, maxPayload, payloadErrado,
    clean: inexistentes.length === 0 && chavesInexistentes.length === 0
      && avisoFaltando.length === 0 && topicosInexistentes.length === 0 && !payloadErrado,
  };

  if (UPDATE && report.clean) {
    writeFileSync(DOC, updateHeader(docMd, { ref, sha, date: report.date, connectors: real.size, keys: claimedKeys.size }));
    log(C.g + 'ok' + C.r + ' proveniência fixada em ' + DOC);
  } else if (UPDATE) {
    log(C.y + '! não fixei a proveniência: há divergência. Corrija antes.' + C.r);
  }

  if (AS_JSON) out(JSON.stringify(report, null, 2));
  else {
    out('');
    if (inexistentes.length) {
      out(C.red + 'CONECTORES QUE NÃO EXISTEM' + C.r + ' em ' + ref + ' (' + inexistentes.length + '):');
      inexistentes.forEach((c) => out('  ' + c));
      out(C.d + '  Se for conector novo ainda não lançado, adicione a VERSION_DEPENDENT e' + C.r);
      out(C.d + '  explique na seção "Disponibilidade por versão".' + C.r);
      out('');
    }
    if (chavesInexistentes.length) {
      out(C.red + 'CHAVES DE CONFIG INEXISTENTES' + C.r + ' (' + chavesInexistentes.length + '):');
      chavesInexistentes.forEach((k) => out('  ' + k));
      out('');
    }
    if (avisoFaltando.length) {
      out(C.red + 'RESSALVA DE VERSÃO SUMIU DO DOC' + C.r + ': ' + avisoFaltando.join(', '));
      out('');
    }
    if (topicosInexistentes.length) {
      out(C.red + 'TÓPICOS MQTT QUE NÃO EXISTEM' + C.r + ' na plataforma (' + topicosInexistentes.length + '):');
      topicosInexistentes.forEach((t) => out('  ' + t));
      out(C.d + '  Tópico errado falha em silêncio: o broker aceita o publish e nada vira telemetria.' + C.r);
      out('');
    }
    if (payloadErrado) {
      out(C.red + 'LIMITE DE PAYLOAD DIVERGENTE' + C.r + ': o fonte diz ' + maxPayload
        + ', a skill cita outro valor');
      out('');
    }
    if (naoDocumentados.length) {
      out(C.d + 'conectores distribuídos e não documentados (' + naoDocumentados.length + ', informativo):' + C.r);
      out(C.d + '  ' + naoDocumentados.join(', ') + C.r);
      out('');
    }
    if (report.clean) out(C.g + 'catálogo do gateway em dia com ' + ref + C.r);
  }

  process.exitCode = report.clean ? 0 : 1;
}

main().catch((e) => {
  process.stderr.write(C.red + 'erro' + C.r + ' ' + (e.stack || e.message) + '\n');
  process.exitCode = 2;
});
