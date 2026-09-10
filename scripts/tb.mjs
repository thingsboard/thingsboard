#!/usr/bin/env node
/**
 * tb.mjs — CLI de verificação contra uma instância ThingsBoard real.
 *
 * Existe para tornar barato o que as 4 skills mandam fazer e nunca deram meio de fazer:
 * conferir contra a instância em vez de chutar a partir de tabela memorizada.
 *
 * Zero dependências (Node >= 18, usa fetch global). Windows/Linux/macOS.
 *
 * Configuração (precedência: flag > env > arquivo .tb.env no cwd):
 *   TB_URL       https://tb.exemplo.com
 *   TB_USER      usuario@exemplo.com
 *   TB_PASSWORD  senha
 *   TB_INSECURE  1  -> aceita certificado self-signed (comum em self-hosted)
 *
 * O JWT fica em cache no diretório temporário do SO, nunca no repositório.
 * A senha nunca é gravada em disco nem impressa.
 */

import { readFileSync, writeFileSync, mkdirSync, existsSync, rmSync, chmodSync } from 'node:fs';
import { join } from 'node:path';
import { tmpdir } from 'node:os';
import { createHash } from 'node:crypto';

const VERSION = '1.0.0';

/* ------------------------------------------------------------------ args */

function parseArgs(argv) {
  const pos = [];
  const flags = {};
  for (let i = 0; i < argv.length; i++) {
    const a = argv[i];
    if (a.startsWith('--')) {
      const eq = a.indexOf('=');
      if (eq !== -1) {
        flags[a.slice(2, eq)] = a.slice(eq + 1);
      } else {
        const next = argv[i + 1];
        if (next !== undefined && !next.startsWith('--')) { flags[a.slice(2)] = next; i++; }
        else flags[a.slice(2)] = true;
      }
    } else pos.push(a);
  }
  return { pos, flags };
}

const { pos, flags } = parseArgs(process.argv.slice(2));
const cmd = pos[0];

/* ------------------------------------------------------------------- out */

const C = process.stdout.isTTY && !process.env.NO_COLOR
  ? { d: '\x1b[2m', r: '\x1b[0m', g: '\x1b[32m', y: '\x1b[33m', red: '\x1b[31m', b: '\x1b[1m' }
  : { d: '', r: '', g: '', y: '', red: '', b: '' };

function out(s) { process.stdout.write(s + '\n'); }
function warn(s) { process.stderr.write(C.y + '! ' + s + C.r + '\n'); }
function ok(s) { process.stderr.write(C.g + 'ok' + C.r + ' ' + s + '\n'); }
function dim(s) { process.stderr.write(C.d + s + C.r + '\n'); }

/**
 * Encerramento limpo. `process.exit()` com socket ou escrita pendente aborta o processo
 * no Windows com "Assertion failed: !(handle->flags & UV_HANDLE_CLOSING)" do libuv, o que
 * transforma um erro tratado numa mensagem de crash. Em vez disso define o exit code,
 * levanta um sentinela e deixa o loop de eventos drenar sozinho.
 */
class ExitError extends Error {}
function bail(code = 1) { process.exitCode = code; throw new ExitError(); }
function die(s, code = 1) { process.stderr.write(C.red + 'erro' + C.r + ' ' + s + '\n'); bail(code); }
function json(v) { out(JSON.stringify(v, null, 2)); }

/* ------------------------------------------------------------------ conf */

function loadDotEnv() {
  for (const f of ['.tb.env', '.env.tb']) {
    if (!existsSync(f)) continue;
    for (const line of readFileSync(f, 'utf8').split(/\r?\n/)) {
      const m = line.match(/^\s*([A-Z_][A-Z0-9_]*)\s*=\s*(.*?)\s*$/);
      if (!m) continue;
      let v = m[2];
      if ((v.startsWith('"') && v.endsWith('"')) || (v.startsWith("'") && v.endsWith("'"))) v = v.slice(1, -1);
      if (process.env[m[1]] === undefined) process.env[m[1]] = v;
    }
    return f;
  }
  return null;
}
const dotEnvUsed = loadDotEnv();

const CONF = {
  url: String(flags.url || process.env.TB_URL || '').replace(/\/+$/, ''),
  user: flags.user || process.env.TB_USER || '',
  password: flags.password || process.env.TB_PASSWORD || '',
  insecure: flags.insecure === true || flags.insecure === '1' || process.env.TB_INSECURE === '1',
};

if (CONF.insecure) {
  process.env.NODE_TLS_REJECT_UNAUTHORIZED = '0';
  warn('TB_INSECURE=1 — validacao de certificado TLS DESLIGADA. Use so contra instancia interna/de teste.');
}

/* ----------------------------------------------------------------- cache */

const CACHE_DIR = join(tmpdir(), 'tb-skill-cache');

function cacheKey(kind) {
  const h = createHash('sha256').update(CONF.url + '|' + CONF.user).digest('hex').slice(0, 16);
  return join(CACHE_DIR, h + '-' + kind + '.json');
}
function cacheRead(kind, maxAgeMs) {
  const f = cacheKey(kind);
  if (!existsSync(f)) return null;
  try {
    const raw = JSON.parse(readFileSync(f, 'utf8'));
    if (maxAgeMs && Date.now() - raw._cachedAt > maxAgeMs) return null;
    return raw;
  } catch { return null; }
}
function cacheWrite(kind, value) {
  mkdirSync(CACHE_DIR, { recursive: true });
  const f = cacheKey(kind);
  writeFileSync(f, JSON.stringify({ ...value, _cachedAt: Date.now() }));
  try { chmodSync(f, 0o600); } catch { /* Windows: sem POSIX mode */ }
  return f;
}

/* ------------------------------------------------------------------ http */

class HttpError extends Error {
  constructor(status, msg, body) { super(msg); this.status = status; this.body = body; }
}

function requireUrl() {
  if (!CONF.url) die('TB_URL nao definido. Use --url https://host, ou export TB_URL, ou crie .tb.env');
}

async function raw(method, path, opts = {}) {
  const { body, headers = {}, auth = true, timeoutMs = 30000 } = opts;
  requireUrl();
  const h = Object.assign({ Accept: 'application/json' }, headers);
  if (body !== undefined) h['Content-Type'] = 'application/json';
  if (auth) h['X-Authorization'] = 'Bearer ' + (await getToken());
  const ac = new AbortController();
  const t = setTimeout(() => ac.abort(), timeoutMs);
  let res;
  try {
    res = await fetch(CONF.url + path, {
      method,
      headers: h,
      signal: ac.signal,
      body: body === undefined ? undefined : (typeof body === 'string' ? body : JSON.stringify(body)),
    });
  } catch (e) {
    clearTimeout(t);
    if (e.name === 'AbortError') throw new HttpError(0, 'timeout apos ' + timeoutMs + 'ms em ' + method + ' ' + path);
    const code = String((e.cause && e.cause.code) || e.message);
    const hint = /self.signed|certificate|CERT_/i.test(code) ? ' (certificado self-signed? tente TB_INSECURE=1)' : '';
    throw new HttpError(0, 'falha de rede em ' + method + ' ' + path + ': ' + code + hint);
  }
  clearTimeout(t);
  const text = await res.text();
  let parsed = null;
  try { parsed = text ? JSON.parse(text) : null; } catch { parsed = text; }
  return { status: res.status, ok: res.ok, body: parsed };
}

async function api(method, path, opts = {}) {
  let r = await raw(method, path, opts);
  if (r.status === 401 && opts.auth !== false && !opts._retried) {
    dim('401 — token expirado, refazendo login');
    dropToken();
    r = await raw(method, path, Object.assign({}, opts, { _retried: true }));
  }
  if (!r.ok) {
    const m = (r.body && typeof r.body === 'object' && r.body.message) || String(JSON.stringify(r.body)).slice(0, 400);
    throw new HttpError(r.status, method + ' ' + path + ' -> HTTP ' + r.status + ': ' + m, r.body);
  }
  return r.body;
}

/* ------------------------------------------------------------------ auth */

let memToken = null;

function dropToken() {
  memToken = null;
  try { rmSync(cacheKey('token')); } catch { /* nao existia */ }
}

function jwtExp(tok) {
  try {
    const p = JSON.parse(Buffer.from(tok.split('.')[1], 'base64').toString('utf8'));
    return (p.exp || 0) * 1000;
  } catch { return 0; }
}

async function getToken() {
  if (memToken) return memToken;
  const c = cacheRead('token');
  if (c && c.token && jwtExp(c.token) > Date.now() + 30000) { memToken = c.token; return memToken; }
  if (c && c.refreshToken && jwtExp(c.refreshToken) > Date.now() + 30000) {
    const r = await raw('POST', '/api/auth/token', { body: { refreshToken: c.refreshToken }, auth: false });
    if (r.ok && r.body && r.body.token) {
      memToken = r.body.token;
      cacheWrite('token', { token: r.body.token, refreshToken: r.body.refreshToken });
      return memToken;
    }
  }
  return await doLogin();
}

async function doLogin() {
  requireUrl();
  // Falha de auth é HttpError, não die(): `check` precisa reportá-la uma vez e seguir
  // com o diagnóstico não autenticado (versão/edição costumam vir do OpenAPI público).
  // Com die() o sentinela vazava para os catch do cmdCheck e saía duplicado e sem texto.
  if (!CONF.user || !CONF.password) {
    throw new HttpError(0, 'TB_USER / TB_PASSWORD nao definidos (ou --user/--password). Nunca commite .tb.env.');
  }
  const r = await raw('POST', '/api/auth/login', {
    body: { username: CONF.user, password: CONF.password },
    auth: false,
  });
  if (!r.ok) {
    const m = (r.body && r.body.message) || ('HTTP ' + r.status);
    throw new HttpError(r.status, 'login falhou para ' + CONF.user + ': ' + m);
  }
  memToken = r.body.token;
  cacheWrite('token', { token: r.body.token, refreshToken: r.body.refreshToken });
  return memToken;
}

/* --------------------------------------------------------------- openapi */

const PE_MARKERS = [
  '/api/whiteLabel', '/api/scheduler', '/api/integration', '/api/converter',
  '/api/entityGroup', '/api/report', '/api/selfRegistration', '/api/blob',
  '/api/roles', '/api/groupPermission', '/api/customTranslation', '/api/solution',
];

async function getSpec(opts = {}) {
  if (!opts.refresh) {
    const c = cacheRead('apidocs', 24 * 3600 * 1000);
    if (c && c.spec) return c.spec;
  }
  requireUrl();   // uma vez, antes do loop: senão o erro sai 6x e é diagnosticado errado
  let spec = null;
  let from = null;
  const authModes = opts.noAuth ? [false] : [false, true];
  for (const p of ['/v3/api-docs', '/v2/api-docs', '/api-docs']) {
    for (const auth of authModes) {
      try {
        const r = await raw('GET', p, { auth, timeoutMs: 60000 });
        if (r.ok && r.body && typeof r.body === 'object' && r.body.paths) { spec = r.body; from = p; break; }
      } catch (e) {
        // ExitError é sinal de controle, não falha de tentativa — engoli-lo faz o loop
        // continuar e reportar "OpenAPI nao encontrado" no lugar da causa real.
        if (e instanceof ExitError) throw e;
      }
    }
    if (spec) break;
  }
  if (!spec) throw new HttpError(0, 'OpenAPI nao encontrado (/v3/api-docs, /v2/api-docs). Confirme /swagger-ui/ na instancia.');
  dim('OpenAPI carregado de ' + from + ' (' + Object.keys(spec.paths).length + ' paths) — cache 24h');
  cacheWrite('apidocs', { spec });
  return spec;
}

function editionOf(spec) {
  const paths = Object.keys(spec.paths || {});
  const hits = PE_MARKERS.filter((m) => paths.some((p) => p.startsWith(m)));
  return { edition: hits.length >= 2 ? 'PE' : 'CE', markers: hits };
}

/* -------------------------------------------------------------- commands */

async function cmdCheck() {
  requireUrl();
  out(C.b + 'instancia ' + C.r + ' ' + CONF.url);
  if (dotEnvUsed) dim('config lida de ' + dotEnvUsed);

  const t0 = Date.now();
  let reach;
  try {
    reach = await raw('GET', '/api/noauth/oauth2Clients', { auth: false, timeoutMs: 15000 });
  } catch (e) {
    die('instancia inalcancavel: ' + e.message);
  }
  out(C.b + 'alcancavel' + C.r + ' HTTP ' + reach.status + ' em ' + (Date.now() - t0) + 'ms');

  let user = null;
  let authFailed = false;
  try {
    await getToken();
    user = await api('GET', '/api/auth/user');
    ok('autenticado como ' + user.email + ' (' + user.authority + ')');
  } catch (e) {
    if (e instanceof ExitError) throw e;
    authFailed = true;
    warn(e.message);
    dim('  seguindo sem autenticação — versão/edição costumam vir do OpenAPI público');
  }

  // Sem credencial válida não adianta o getSpec tentar o modo autenticado: repetiria
  // o mesmo login falho e imprimiria o erro uma segunda vez.
  let spec = null;
  try { spec = await getSpec({ noAuth: authFailed }); }
  catch (e) {
    if (e instanceof ExitError) throw e;
    if (e.message) warn(e.message);
  }

  if (spec) {
    const ed = editionOf(spec);
    out(C.b + 'edicao' + C.r + '     ' + ed.edition + (ed.markers.length ? '  ' + C.d + '(marcadores PE: ' + ed.markers.slice(0, 4).join(' ') + ')' + C.r : ''));
    out(C.b + 'versao API' + C.r + ' ' + ((spec.info && spec.info.version) || '?'));
    out(C.b + 'paths' + C.r + '      ' + Object.keys(spec.paths).length);
  }

  if (user) {
    try {
      const si = await api('GET', '/api/system/info');
      if (si && si.systemVersion) out(C.b + 'build' + C.r + '      ' + si.systemVersion);
    } catch (e) {
      if (e instanceof ExitError) throw e;
      dim('/api/system/info indisponivel (normal para tenant admin — exige sysadmin)');
    }
  }

  out('');
  if (authFailed) {
    // Diagnóstico parcial ainda é útil (versão/edição saíram), mas sem auth o `check`
    // não passou: script e CI precisam enxergar isso no exit code.
    dim('sem autenticação: nodes/export/find não vão funcionar até a credencial passar');
    process.exitCode = 1;
  } else {
    dim('proximo: tb.mjs api <regex> | tb.mjs nodes | tb.mjs export rulechain <nome>');
  }
}

async function cmdVersion() {
  const spec = await getSpec({ refresh: flags.refresh === true });
  const ed = editionOf(spec);
  json({
    url: CONF.url,
    edition: ed.edition,
    apiVersion: (spec.info && spec.info.version) || null,
    title: (spec.info && spec.info.title) || null,
    peMarkers: ed.markers,
    paths: Object.keys(spec.paths).length,
  });
}

async function cmdApi() {
  const pattern = pos[1];
  if (!pattern) die('uso: tb.mjs api <regex>     ex: tb.mjs api "telemetry|attributes"', 2);
  const spec = await getSpec({ refresh: flags.refresh === true });
  const re = new RegExp(pattern, 'i');
  const want = flags.method ? String(flags.method).toLowerCase() : null;
  const rows = [];
  for (const p of Object.keys(spec.paths)) {
    const ops = spec.paths[p];
    for (const m of Object.keys(ops)) {
      if (['get', 'post', 'put', 'delete', 'patch'].indexOf(m) === -1) continue;
      if (want && m !== want) continue;
      const op = ops[m];
      if (!re.test(p) && !re.test(op.summary || '') && !re.test(op.operationId || '')) continue;
      rows.push({ m: m.toUpperCase(), p: p, s: op.summary || '' });
    }
  }
  if (!rows.length) { warn('nenhum path casa /' + pattern + '/i'); bail(1); }
  rows.sort((a, b) => a.p.localeCompare(b.p) || a.m.localeCompare(b.m));
  const w = Math.max.apply(null, rows.map((r) => r.m.length));
  for (const r of rows) {
    out(r.m.padEnd(w) + '  ' + r.p + (r.s ? '  ' + C.d + r.s.split('\n')[0].slice(0, 90) + C.r : ''));
  }
  dim('\n' + rows.length + ' operacao(oes). Detalhe: tb.mjs spec <path> --method get');
}

/**
 * Git Bash / MSYS no Windows reescreve um argumento "/api/x" para
 * "C:/Program Files/Git/api/x" antes do Node ver. Sintoma: "path nao existe no spec"
 * com um caminho do Windows no erro. Desfaz isso em vez de exigir MSYS_NO_PATHCONV=1.
 */
function normPath(p) {
  if (!p) return p;
  const i = String(p).indexOf('/api/');
  if (i > 0) return String(p).slice(i);
  return String(p).charAt(0) === '/' ? String(p) : '/' + p;
}

function resolveRef(spec, ref) {
  if (!ref || ref.indexOf('#/') !== 0) return null;
  return ref.slice(2).split('/').reduce((o, k) => (o ? o[k] : null), spec);
}

function summarizeSchema(spec, schema, depth, seen) {
  depth = depth || 0;
  seen = seen || new Set();
  if (!schema || depth > 4) return '...';
  if (schema.$ref) {
    if (seen.has(schema.$ref)) return '<circular ' + schema.$ref.split('/').pop() + '>';
    seen.add(schema.$ref);
    return summarizeSchema(spec, resolveRef(spec, schema.$ref), depth, seen);
  }
  if (schema.type === 'array') return [summarizeSchema(spec, schema.items, depth + 1, seen)];
  if (schema.properties) {
    const o = {};
    for (const k of Object.keys(schema.properties)) {
      const v = schema.properties[k];
      const req = schema.required && schema.required.indexOf(k) !== -1 ? '*' : '';
      o[k + req] = (v.type === 'object' || v.$ref || v.properties)
        ? summarizeSchema(spec, v, depth + 1, seen)
        : (v.type || '?') + (v.enum ? ' ' + JSON.stringify(v.enum) : '') + (v.format ? ' (' + v.format + ')' : '');
    }
    return o;
  }
  return (schema.type || '?') + (schema.enum ? ' ' + JSON.stringify(schema.enum) : '');
}

async function cmdSpec() {
  const path = normPath(pos[1]);
  if (!pos[1]) die('uso: tb.mjs spec <path> [--method get]', 2);
  const spec = await getSpec();
  const key = spec.paths[path] ? path : Object.keys(spec.paths).find((p) => p.toLowerCase() === path.toLowerCase());
  const entry = key ? spec.paths[key] : null;
  if (!entry) {
    warn('path exato "' + path + '" nao existe no spec. Parecidos:');
    const frag = path.replace(/^\//, '').split('/')[0];
    Object.keys(spec.paths).filter((p) => p.indexOf(frag) !== -1).slice(0, 15).forEach((p) => out('  ' + p));
    bail(1);
  }
  const methods = flags.method
    ? [String(flags.method).toLowerCase()]
    : Object.keys(entry).filter((m) => ['get', 'post', 'put', 'delete', 'patch'].indexOf(m) !== -1);
  for (const m of methods) {
    const op = entry[m];
    if (!op) continue;
    out(C.b + m.toUpperCase() + ' ' + key + C.r);
    if (op.summary) out('  ' + op.summary.split('\n')[0]);
    const params = op.parameters || [];
    if (params.length) {
      out('  ' + C.b + 'params' + C.r);
      for (const p0 of params) {
        const p = p0.$ref ? resolveRef(spec, p0.$ref) : p0;
        if (!p) continue;
        const sch = p.schema || {};
        out('    ' + p.name + (p.required ? '*' : '') + '  ' + C.d + p.in + ' ' + (sch.type || '') + (sch.enum ? ' ' + JSON.stringify(sch.enum) : '') + C.r);
      }
    }
    const rb = op.requestBody && op.requestBody.content && op.requestBody.content['application/json'] && op.requestBody.content['application/json'].schema;
    if (rb) {
      out('  ' + C.b + 'body' + C.r);
      out(JSON.stringify(summarizeSchema(spec, rb), null, 2).split('\n').map((l) => '    ' + l).join('\n'));
    }
    out('');
  }
}

async function cmdGet() {
  if (!pos[1]) die('uso: tb.mjs get /api/auth/user', 2);
  json(await api('GET', normPath(pos[1])));
}

async function cmdPost() {
  const path = normPath(pos[1]);
  if (!pos[1]) die('uso: tb.mjs post /api/... --data <json> --yes', 2);
  if (flags.yes !== true) die('POST e escrita. Confirme com --yes depois de revisar o body.', 2);
  let body;
  if (flags.file) body = JSON.parse(readFileSync(String(flags.file), 'utf8'));
  else if (flags.data) body = JSON.parse(String(flags.data));
  else die('faltou --data <json> ou --file <arquivo.json>', 2);
  json(await api('POST', path, { body: body }));
}

const KINDS = {
  device: '/api/tenant/devices',
  asset: '/api/tenant/assets',
  rulechain: '/api/ruleChains',
  dashboard: '/api/tenant/dashboards',
  customer: '/api/customers',
  deviceprofile: '/api/deviceProfiles',
  widgetsbundle: '/api/widgetsBundles',
};

async function listAll(kind, text, limit) {
  limit = limit || 200;
  const base = KINDS[kind];
  if (!base) die('kind desconhecido "' + kind + '". Validos: ' + Object.keys(KINDS).join(', '), 2);
  const items = [];
  let page = 0;
  while (items.length < limit) {
    const q = new URLSearchParams({ pageSize: '100', page: String(page), sortProperty: 'name', sortOrder: 'ASC' });
    if (text) q.set('textSearch', text);
    const r = await api('GET', base + '?' + q.toString());
    items.push.apply(items, r.data || []);
    if (!r.hasNext) break;
    page++;
  }
  return items.slice(0, limit);
}

async function cmdFind() {
  const kind = String(pos[1] || '').toLowerCase();
  if (!kind) die('uso: tb.mjs find <' + Object.keys(KINDS).join('|') + '> [nome]', 2);
  const items = await listAll(kind, pos[2]);
  if (flags.raw) return json(items);
  if (!items.length) { warn('nenhum resultado'); bail(1); }
  for (const it of items) {
    out(((it.id && it.id.id) || '-') + '  ' + (it.name || it.title || '-') + (it.type ? '  ' + C.d + it.type + C.r : ''));
  }
  dim('\n' + items.length + ' resultado(s)');
}

const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

async function resolveId(kind, ref) {
  if (UUID_RE.test(ref)) return ref;
  const items = await listAll(kind, ref, 50);
  const exact = items.filter((i) => (i.name || i.title) === ref);
  const pool = exact.length ? exact : items;
  if (!pool.length) die('nenhum ' + kind + ' com nome ~ "' + ref + '"');
  if (pool.length > 1) {
    warn(pool.length + ' ' + kind + 's casam "' + ref + '" — seja especifico ou passe o UUID:');
    pool.slice(0, 10).forEach((i) => process.stderr.write('    ' + i.id.id + '  ' + (i.name || i.title) + '\n'));
    bail(1);
  }
  return pool[0].id.id;
}

async function cmdExport() {
  const what = String(pos[1] || '').toLowerCase();
  const ref = pos[2];
  let data;
  let suggested;
  if (what === 'rulechain') {
    if (!ref) die('uso: tb.mjs export rulechain <uuid|nome>', 2);
    const id = await resolveId('rulechain', ref);
    const ruleChain = await api('GET', '/api/ruleChain/' + id);
    const metadata = await api('GET', '/api/ruleChain/' + id + '/metadata');
    data = { ruleChain: ruleChain, metadata: metadata };
    suggested = 'rulechain-' + String(ruleChain.name || id).replace(/[^\w.-]+/g, '_') + '.json';
    dim((metadata.nodes ? metadata.nodes.length : 0) + ' nodes, ' + (metadata.connections ? metadata.connections.length : 0) + ' conexoes');
  } else if (what === 'dashboard') {
    if (!ref) die('uso: tb.mjs export dashboard <uuid|nome>', 2);
    const id = await resolveId('dashboard', ref);
    data = await api('GET', '/api/dashboard/' + id);
    suggested = 'dashboard-' + String(data.title || id).replace(/[^\w.-]+/g, '_') + '.json';
  } else if (what === 'deviceprofile') {
    if (!ref) die('uso: tb.mjs export deviceprofile <uuid|nome>', 2);
    const id = await resolveId('deviceprofile', ref);
    data = await api('GET', '/api/deviceProfile/' + id);
    suggested = 'deviceprofile-' + String(data.name || id).replace(/[^\w.-]+/g, '_') + '.json';
  } else {
    die('uso: tb.mjs export <rulechain|dashboard|deviceprofile> <uuid|nome> [--out arquivo.json]', 2);
  }
  if (flags.out) {
    const f = flags.out === true ? suggested : String(flags.out);
    writeFileSync(f, JSON.stringify(data, null, 2));
    ok('escrito em ' + f);
  } else {
    json(data);
  }
}

/**
 * Tipos de rule node disponiveis NESTA instancia.
 *
 * Estrategia 1: descritores de componente. O path e descoberto no proprio OpenAPI da
 *   instancia porque ele mudou entre versoes — nada hardcoded, esse e o ponto do design.
 * Estrategia 2 (--used): varre rule chains reais e coleta os `type` em uso. Sempre
 *   funciona, e cobre nodes PE closed-source que o catalogo do repo CE nao tem.
 */
async function cmdNodes() {
  if (flags.used) return await nodesFromChains();
  let spec = null;
  try { spec = await getSpec(); } catch (e) { if (e instanceof ExitError) throw e; /* segue sem spec */ }
  const candidates = [];
  if (spec) {
    for (const p of Object.keys(spec.paths)) {
      if (/component/i.test(p) && spec.paths[p].get && p.indexOf('{') === -1) candidates.push(p);
    }
  }
  candidates.push('/api/components', '/api/component/descriptors');
  const types = 'FILTER,ENRICHMENT,TRANSFORMATION,ACTION,EXTERNAL,FLOW';
  const tried = [];
  for (const base of Array.from(new Set(candidates))) {
    for (const qs of ['?componentTypes=' + types, '?componentTypes=' + types + '&ruleChainType=CORE', '']) {
      tried.push(base + qs);
      try {
        const r = await api('GET', base + qs);
        const arr = Array.isArray(r) ? r : (r && r.data);
        if (Array.isArray(arr) && arr.length && arr[0] && arr[0].clazz) {
          dim('descritores via GET ' + base + qs);
          return printDescriptors(arr);
        }
      } catch (e) {
        if (e instanceof ExitError) throw e;   // sinal de controle, não candidato ruim
      }
    }
  }
  warn('endpoint de descritores de componente nao encontrado nesta versao.');
  dim('tentados: ' + tried.slice(0, 6).join(' | '));
  warn('caindo para varredura de rule chains reais (equivale a --used):');
  return await nodesFromChains();
}

function printDescriptors(arr) {
  if (flags.raw) return json(arr);
  const byType = {};
  for (const d of arr) {
    const t = d.type || 'OUTRO';
    if (!byType[t]) byType[t] = [];
    byType[t].push(d);
  }
  for (const t of Object.keys(byType).sort()) {
    out('\n' + C.b + t + C.r + '  ' + C.d + '(' + byType[t].length + ')' + C.r);
    const sorted = byType[t].sort((a, b) => String(a.name).localeCompare(String(b.name)));
    for (const d of sorted) {
      const nd = (d.configurationDescriptor && d.configurationDescriptor.nodeDefinition) || {};
      const label = nd.customRelations
        ? 'dinamico (definido em runtime)'
        : (Array.isArray(nd.relationTypes) && nd.relationTypes.length ? nd.relationTypes.join(', ') : 'Success, Failure');
      out('  ' + String(d.name || '?').padEnd(32) + ' ' + d.clazz);
      out('  ' + ''.padEnd(32) + ' ' + C.d + 'connections[].type: ' + label + C.r);
    }
  }
  dim('\n' + arr.length + ' descritores. Catalogo REAL desta instancia — inclui nodes PE ausentes do repo CE.');
}

async function nodesFromChains() {
  const chains = await listAll('rulechain', undefined, 200);
  const seen = new Map();
  for (const ch of chains) {
    let md;
    try { md = await api('GET', '/api/ruleChain/' + ch.id.id + '/metadata'); }
    catch (e) { if (e instanceof ExitError) throw e; continue; }
    const nodes = md.nodes || [];
    for (const n of nodes) {
      let e = seen.get(n.type);
      if (!e) { e = { type: n.type, count: 0, relations: new Set(), chains: new Set() }; seen.set(n.type, e); }
      e.count++;
      e.chains.add(ch.name);
    }
    for (const c of md.connections || []) {
      const from = nodes[c.fromIndex];
      if (!from) continue;
      const e = seen.get(from.type);
      if (e) e.relations.add(c.type);
    }
  }
  const rows = Array.from(seen.values()).sort((a, b) => a.type.localeCompare(b.type));
  if (flags.raw) {
    return json(rows.map((r) => ({
      type: r.type,
      count: r.count,
      relationTypesObserved: Array.from(r.relations),
      chains: Array.from(r.chains),
    })));
  }
  for (const r of rows) {
    out(r.type);
    out('  ' + C.d + 'usos: ' + r.count + ' | conexoes observadas: ' + (Array.from(r.relations).join(', ') || '-') + C.r);
  }
  dim('\n' + rows.length + ' tipos distintos em ' + chains.length + ' rule chains.');
  dim('"conexoes observadas" = labels em uso real, nao o conjunto completo que o node emite.');
}

async function cmdTelemetry() {
  const token = pos[1];
  const payload = pos[2];
  if (!token || !payload) die('uso: tb.mjs telemetry <DEVICE_ACCESS_TOKEN> <json>', 2);
  let body;
  try { body = JSON.parse(payload); } catch { die('payload nao e JSON valido'); }
  const r = await raw('POST', '/api/v1/' + token + '/telemetry', { body: body, auth: false });
  if (!r.ok) die('HTTP ' + r.status + ': ' + JSON.stringify(r.body));
  ok('telemetria aceita (HTTP ' + r.status + ') — API de transporte do device, sem JWT');
}

function cmdHelp() {
  out('tb.mjs ' + VERSION + ' — verificacao contra instancia ThingsBoard real\n');
  out(C.b + 'config' + C.r + '  TB_URL, TB_USER, TB_PASSWORD [, TB_INSECURE=1]');
  out('        via env, arquivo .tb.env, ou --url/--user/--password\n');
  out(C.b + 'diagnostico' + C.r);
  out('  check                        conectividade + auth + edicao CE/PE + versao da API');
  out('  version                      edicao/versao em JSON');
  out('  whoami                       usuario autenticado');
  out('  login / logout               gerencia o JWT em cache\n');
  out(C.b + 'descoberta de contrato' + C.r + '   (substitui adivinhar endpoint de memoria)');
  out('  api <regex> [--method get]   busca paths no OpenAPI da instancia (cache 24h, --refresh)');
  out('  spec <path> [--method get]   params + schema de body de uma operacao');
  out('  nodes                        rule nodes disponiveis NESTA instancia (inclui PE)');
  out('  nodes --used                 tipos em uso nas rule chains reais + labels de conexao\n');
  out(C.b + 'dados' + C.r);
  out('  get <path>                   GET autenticado');
  out('  post <path> --data <json> --yes    POST autenticado (escrita, exige --yes)');
  out('  find <' + Object.keys(KINDS).join('|') + '> [nome]');
  out('  export rulechain|dashboard|deviceprofile <uuid|nome> [--out arq.json]');
  out('  telemetry <DEVICE_TOKEN> <json>    publica via API de transporte do device\n');
  out(C.b + 'flags' + C.r + '  --raw  --refresh  --out <arquivo>  --insecure  --method <verbo>\n');
  out(C.d + 'JWT em cache: ' + CACHE_DIR + C.r);
  out(C.d + 'A senha nunca e gravada em disco nem impressa.' + C.r);
}

/* ------------------------------------------------------------------ main */

const COMMANDS = {
  check: cmdCheck,
  version: cmdVersion,
  login: async () => { await doLogin(); ok('autenticado, token em cache'); },
  logout: async () => { dropToken(); ok('token removido do cache'); },
  whoami: async () => json(await api('GET', '/api/auth/user')),
  api: cmdApi,
  spec: cmdSpec,
  get: cmdGet,
  post: cmdPost,
  find: cmdFind,
  export: cmdExport,
  nodes: cmdNodes,
  telemetry: cmdTelemetry,
  help: async () => { cmdHelp(); },
};

if (!cmd || cmd === 'help' || flags.help === true) {
  cmdHelp();
  process.exitCode = cmd ? 0 : 2;
} else {
  try {
    if (!COMMANDS[cmd]) die('comando desconhecido "' + cmd + '". Use: tb.mjs help', 2);
    await COMMANDS[cmd]();
  } catch (e) {
    // ExitError já reportou e já definiu o exit code — não imprimir de novo nem stack.
    if (!(e instanceof ExitError)) {
      process.stderr.write(C.red + 'erro' + C.r + ' ' + (e instanceof HttpError ? e.message : (e.stack || String(e))) + '\n');
      process.exitCode = 1;
    }
  }
}
