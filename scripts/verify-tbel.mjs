#!/usr/bin/env node
/**
 * verify-tbel.mjs — verifica as funções TBEL documentadas contra o código-fonte upstream.
 *
 * references/tbel.md é o maior arquivo do pack e era o único grande sem verificação
 * automática. Uma função inventada ali é exatamente o modo de falha que o pack inteiro
 * existe para evitar: o agente escreve `date.addMilliseconds(500)`, o TBEL não tem esse
 * método, e o script quebra em runtime dentro de um rule node.
 *
 * Fonte da verdade (a mesma que o ThingsBoard usa em runtime):
 *   TbUtils.java            -> parserConfig.addImport("nome", ...) registra as globais
 *   TbDate.java             -> métodos públicos de uma instância de Date
 *   TbelCfTsRollingArg.java -> métodos de janela rolante em Calculated Fields
 *                              (avg/mean/std/median/count/first/last/merge/mergeAll)
 *
 * Só considera identificadores dentro de crase ou de bloco de código: em prosa,
 * "binário(2)" casaria como uma função chamada `rio`.
 *
 * Zero dependências. GITHUB_TOKEN opcional (evita rate limit em CI).
 *
 *   node scripts/verify-tbel.mjs
 *   node scripts/verify-tbel.mjs --ref v4.3.1.4 --json
 */

import { readFileSync, existsSync } from 'node:fs';

const REPO = 'thingsboard/thingsboard';
const BASE = 'common/script/script-api/src/main/java/org/thingsboard/script/api/tbel/';
const DOC = '.claude/skills/tb-rule-engine/references/tbel.md';

// TBEL é um fork do MVEL mantido em repositório separado; os métodos de coleção
// (ExecutionArrayList / ExecutionHashMap / ExecutionLinkedHashSet) moram lá.
const TBEL_REPO = 'thingsboard/tbel';
const TBEL_REF = 'master';
const TBEL_COLLECTION_FILES = [
  'ExecutionArrayList.java', 'ExecutionHashMap.java', 'ExecutionLinkedHashSet.java',
  'ExecutionCollections.java', 'ExecutionEntry.java', 'ExecutionObject.java',
];

/**
 * Identificadores que aparecem legitimamente nos exemplos sem serem funções do TBEL:
 * palavras-chave de linguagem, métodos de String/List/Map do Java, e globais de JS que
 * o MVEL também expõe. Manter explícito é mais honesto que alargar o regex até o
 * relatório ficar limpo.
 */
const ALLOWLIST = new Set([
  // controle de fluxo / MVEL
  'if', 'else', 'for', 'foreach', 'while', 'do', 'return', 'def', 'function', 'new', 'switch',
  // métodos de coleção/String do Java expostos pelo MVEL
  'add', 'remove', 'get', 'put', 'size', 'contains', 'containsKey', 'containsValue',
  'indexOf', 'lastIndexOf', 'substring', 'split', 'join', 'replace', 'replaceAll',
  'toLowerCase', 'toUpperCase', 'trim', 'startsWith', 'endsWith', 'charAt', 'length',
  'toString', 'equals', 'keySet', 'values', 'entrySet', 'isEmpty', 'sort', 'reverse',
  'sublist', 'toArray', 'clear', 'putIfAbsent', 'getOrDefault', 'addAll', 'removeAll',
  'retainAll', 'slice', 'splice', 'push', 'pop', 'shift', 'unshift', 'concat', 'fill',
  'find', 'filter', 'map', 'reduce', 'forEach', 'some', 'every', 'includes', 'sum',
  // globais que o engine expõe além do TbUtils
  'Date', 'Math', 'String', 'Number', 'JSON', 'parse', 'stringify',
  'isNaN', 'toFixed', 'padStart', 'padEnd', 'encodeURI', 'decodeURI',
  'abs', 'round', 'floor', 'ceil', 'min', 'max', 'pow', 'sqrt', 'random',
  // métodos de Date fora do TbDate.java (herdados/JS-like)
  'getTime', 'getFullYear', 'getMonth', 'getDate', 'getHours', 'getMinutes',
  'getSeconds', 'getMilliseconds', 'toISOString', 'toLocaleString', 'valueOf',
  // nome da funcao de entrada que o USUARIO define num script de Calculated Field,
  // nao uma API do TBEL — por isso nao aparece em nenhum fonte upstream
  'calculate',
  // palavras-chave de laço do MVEL: `until (cond) {}` e `do {} until (cond)`
  'until',
  // placeholders dos exemplos de sintaxe — não são API
  'doSomething', 'something', 'isTrue', 'isFalse',
  // aparece só em exemplo NEGATIVO ("new java.util.ArrayList() NÃO é permitido"),
  // documentar o que falha é tão útil quanto documentar o que funciona
  'ArrayList',
]);

const args = process.argv.slice(2);
const flag = (n) => { const i = args.indexOf('--' + n); if (i === -1) return undefined; const v = args[i + 1]; return v === undefined || v.startsWith('--') ? true : v; };
const AS_JSON = flag('json') === true;

const C = process.stdout.isTTY && !process.env.NO_COLOR
  ? { d: '\x1b[2m', r: '\x1b[0m', g: '\x1b[32m', y: '\x1b[33m', red: '\x1b[31m', b: '\x1b[1m' }
  : { d: '', r: '', g: '', y: '', red: '', b: '' };
const log = (s) => process.stderr.write(s + '\n');
const out = (s) => process.stdout.write(s + '\n');

const GH_HEADERS = { Accept: 'application/vnd.github+json', 'User-Agent': 'tb-skills-verify-tbel' };
if (process.env.GITHUB_TOKEN) GH_HEADERS.Authorization = 'Bearer ' + process.env.GITHUB_TOKEN;

async function gh(path) {
  const res = await fetch('https://api.github.com' + path, { headers: GH_HEADERS });
  if (!res.ok) {
    const extra = res.headers.get('x-ratelimit-remaining') === '0' ? ' (rate limit da API do GitHub — defina GITHUB_TOKEN)' : '';
    throw new Error('GET ' + path + ' -> HTTP ' + res.status + extra);
  }
  return res.json();
}

async function rawFile2(repo, ref, path) {
  const res = await fetch('https://raw.githubusercontent.com/' + repo + '/' + ref + '/' + path, {
    headers: { 'User-Agent': 'tb-skills-verify-tbel' },
  });
  if (!res.ok) throw new Error('raw ' + repo + '/' + path + ' -> HTTP ' + res.status);
  return res.text();
}

const rawFile = (sha, path) => rawFile2(REPO, sha, path);

/** Só código: blocos cercados e spans entre crase. Prosa gera falso positivo. */
function claimedFunctions(md) {
  const spans = [];
  // \r?\n é obrigatório: os arquivos do repo estão em CRLF e, sem isso, NENHUM bloco
  // cercado casa. A ferramenta passa a checar só os spans inline e reporta "limpo"
  // ignorando justamente onde moram os exemplos. Verificado injetando uma função
  // inexistente num bloco ```tbel: antes do \r? o relatório continuava verde.
  for (const m of md.matchAll(/```[\w-]*\r?\n([\s\S]*?)```/g)) spans.push(m[1]);
  for (const m of md.matchAll(/`([^`\r\n]+)`/g)) spans.push(m[1]);
  if (!spans.length) throw new Error('nenhum bloco de código extraído de ' + DOC + ' — regex de extração quebrada');

  const defined = new Set([...md.matchAll(/\bdef\s+(\w+)\s*\(/g)].map((m) => m[1]));
  const found = new Map();
  for (const raw of spans) {
    // Comentário dentro de bloco de código é prosa, não chamada: "// não-mutantes
    // (retornam nova lista)" casaria como uma função `mutantes`. Remover antes de
    // extrair evita allowlistar palavras de português uma a uma.
    const s = raw.replace(/\/\*[\s\S]*?\*\//g, ' ').replace(/\/\/[^\n]*/g, ' ');
    for (const m of s.matchAll(/(?:^|[^\w.$])(\w+)\s*\(/g)) {
      const n = m[1];
      if (/^\d/.test(n) || defined.has(n)) continue;
      found.set(n, (found.get(n) || 0) + 1);
    }
    // chamadas de método: date.addDays(...)
    for (const m of s.matchAll(/\.(\w+)\s*\(/g)) {
      const n = m[1];
      if (defined.has(n)) continue;
      found.set(n, (found.get(n) || 0) + 1);
    }
  }
  return found;
}

async function main() {
  let ref = flag('ref');
  if (!ref || ref === true) {
    try { ref = (await gh('/repos/' + REPO + '/releases/latest')).tag_name; }
    catch { ref = (await gh('/repos/' + REPO + '/tags?per_page=1'))[0].name; }
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
  log(C.b + 'ref' + C.r + '  ' + ref + '  ' + C.d + '-> ' + sha + C.r);

  const [utils, date, rolling] = await Promise.all([
    rawFile(sha, BASE + 'TbUtils.java'),
    rawFile(sha, BASE + 'TbDate.java'),
    rawFile(sha, BASE + 'TbelCfTsRollingArg.java'),
  ]);

  // TBEL é um fork do MVEL que vive em OUTRO repositório. Os métodos de coleção
  // (toSorted, toReversed, sortByKey, ...) são definidos lá, não no thingsboard/.
  // Sem esta fonte eles apareceriam como "função inventada" — falso positivo que
  // levaria a documentar menos do que a linguagem realmente oferece.
  const collectionSrc = (await Promise.all(
    TBEL_COLLECTION_FILES.map((f) =>
      rawFile2(TBEL_REPO, TBEL_REF, 'src/main/java/org/mvel2/execution/' + f).catch(() => '')
    )
  )).join('\n');

  const publicMethods = (src) =>
    new Set([...src.matchAll(/public\s+(?:static\s+)?[\w<>\[\],.\s]+?\s+(\w+)\s*\(/g)].map((m) => m[1]));

  // addImport("nome", ...) é literalmente como o engine expõe cada função ao script
  const registered = new Set([...utils.matchAll(/addImport\(\s*"([^"]+)"/g)].map((m) => m[1]));
  const dateMethods = publicMethods(date);
  const rollingMethods = publicMethods(rolling);
  const collectionMethods = publicMethods(collectionSrc);
  if (!collectionMethods.size) log(C.y + '! ' + C.r + 'tbel/execution não carregou — métodos de coleção podem virar falso positivo');
  log(C.b + 'upstream' + C.r + '  ' + registered.size + ' em TbUtils, ' + dateMethods.size
    + ' em TbDate, ' + rollingMethods.size + ' em TbelCfTsRollingArg, '
    + collectionMethods.size + ' em tbel/execution');

  if (!existsSync(DOC)) { log(C.red + 'erro' + C.r + ' ' + DOC + ' não encontrado'); process.exitCode = 1; return; }
  const md = readFileSync(DOC, 'utf8');
  const claimed = claimedFunctions(md);
  log(C.b + 'doc' + C.r + '       ' + claimed.size + ' identificadores em código');

  const orphans = [];
  for (const [name, uses] of claimed) {
    if (registered.has(name) || dateMethods.has(name) || rollingMethods.has(name)
        || collectionMethods.has(name) || ALLOWLIST.has(name)) continue;
    orphans.push({ name, uses });
  }
  orphans.sort((a, b) => b.uses - a.uses || a.name.localeCompare(b.name));

  const undocumented = [...registered].filter((f) => !claimed.has(f)).sort();

  const report = {
    ref, sha, date: new Date().toISOString().slice(0, 10),
    tbUtilsFunctions: registered.size, tbDateMethods: dateMethods.size, rollingMethods: rollingMethods.size,
    claimedInDoc: claimed.size, orphans, undocumented,
    clean: orphans.length === 0,
  };

  if (AS_JSON) { out(JSON.stringify(report, null, 2)); }
  else {
    out('');
    if (orphans.length) {
      out(C.red + 'SEM RESPALDO NO SOURCE' + C.r + ' (' + orphans.length + ') — risco de função inventada:');
      for (const o of orphans) out('  ' + o.name.padEnd(30) + C.d + o.uses + ' uso(s) no doc' + C.r);
      out(C.d + '  Se for builtin de linguagem legítimo, adicione à ALLOWLIST deste script.' + C.r);
      out('');
    } else {
      out(C.g + 'toda função documentada existe em ' + ref + C.r);
    }
    if (undocumented.length) {
      out(C.d + 'registradas no TbUtils e não citadas no doc (' + undocumented.length + ', informativo):' + C.r);
      out(C.d + '  ' + undocumented.join(', ') + C.r);
    }
  }

  process.exitCode = orphans.length ? 1 : 0;
}

main().catch((e) => {
  process.stderr.write(C.red + 'erro' + C.r + ' ' + (e.stack || e.message) + '\n');
  process.exitCode = 2;
});
