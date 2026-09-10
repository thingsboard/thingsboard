#!/usr/bin/env node
/**
 * skill-lint.mjs — valida a estrutura das skills e testa se as descriptions casam com
 * o que o usuário realmente digita.
 *
 * O modo de falha que este script existe para pegar: o corpo da skill está impecável e a
 * `description` nunca casa com a frase real do usuário, porque foi escrita no vocabulário
 * do autor ("administração do ThingsBoard self-hosted") e não no do usuário
 * ("docker compose do tb não sobe" / "Port 443 is already in use").
 *
 *   node scripts/skill-lint.mjs            # validate + triggers
 *   node scripts/skill-lint.mjs validate
 *   node scripts/skill-lint.mjs triggers [--verbose]
 *
 * LIMITE HONESTO: o modo `triggers` é um scorer léxico, não o modelo. Ele prova que os
 * termos discriminantes da frase existem na description certa e não na errada — condição
 * necessária, não suficiente. A validação de verdade é rodar as frases no agente
 * (`claude plugin eval` / `/skill-doctor`). Este lint é o pré-voo barato que roda em CI.
 */

import { readFileSync, readdirSync, existsSync, statSync } from 'node:fs';
import { spawnSync } from 'node:child_process';
import { join } from 'node:path';

const SKILLS_DIR = '.claude/skills';
const TRIGGERS = 'tests/triggers.jsonl';
const MAX_DESC = 1024;
const MAX_SKILL_LINES = 500;

const args = process.argv.slice(2);
const mode = args.find((a) => !a.startsWith('--')) || 'all';
const VERBOSE = args.includes('--verbose');

const C = process.stdout.isTTY && !process.env.NO_COLOR
  ? { d: '\x1b[2m', r: '\x1b[0m', g: '\x1b[32m', y: '\x1b[33m', red: '\x1b[31m', b: '\x1b[1m' }
  : { d: '', r: '', g: '', y: '', red: '', b: '' };

const out = (s) => process.stdout.write(s + '\n');
let errors = 0;
let warns = 0;
const fail = (s) => { errors++; out('  ' + C.red + 'FALHA' + C.r + ' ' + s); };
const warn = (s) => { warns++; out('  ' + C.y + 'AVISO' + C.r + ' ' + s); };
const pass = (s) => { if (VERBOSE) out('  ' + C.g + 'ok' + C.r + '    ' + s); };

/* ---------------------------------------------------------------- parsing */

function parseFrontmatter(text) {
  const m = text.match(/^---\r?\n([\s\S]*?)\r?\n---\r?\n?/);
  if (!m) return null;
  const fm = {};
  const rawScalar = {};
  let key = null;
  for (const line of m[1].split(/\r?\n/)) {
    const kv = line.match(/^([a-zA-Z][\w-]*):\s*(.*)$/);
    if (kv) { key = kv[1]; fm[key] = kv[2].trim(); rawScalar[key] = kv[2].trim(); }
    else if (key && /^\s+\S/.test(line)) fm[key] += ' ' + line.trim();
  }
  // desembrulha scalar YAML quoted, para o resto do lint ver o texto real
  for (const k of Object.keys(fm)) {
    const v = fm[k];
    if (v.length > 1 && v[0] === "'" && v[v.length - 1] === "'") fm[k] = v.slice(1, -1).replace(/''/g, "'");
    else if (v.length > 1 && v[0] === '"' && v[v.length - 1] === '"') fm[k] = v.slice(1, -1).replace(/\\"/g, '"');
  }
  return { fm, raw: rawScalar, body: text.slice(m[0].length) };
}

function loadSkills() {
  if (!existsSync(SKILLS_DIR)) { out(C.red + 'erro' + C.r + ' ' + SKILLS_DIR + ' não existe'); process.exitCode = 2; throw new Error("skills dir ausente"); }
  const skills = [];
  for (const dir of readdirSync(SKILLS_DIR)) {
    const p = join(SKILLS_DIR, dir);
    if (!statSync(p).isDirectory()) continue;
    const f = join(p, 'SKILL.md');
    if (!existsSync(f)) continue;
    const text = readFileSync(f, 'utf8');
    const parsed = parseFrontmatter(text);
    skills.push({ dir, path: f, text, fm: parsed ? parsed.fm : null, raw: parsed ? parsed.raw : {}, body: parsed ? parsed.body : text });
  }
  return skills;
}

/* ----------------------------------------------------------------- segredos */

/**
 * Um arquivo `.example` é rastreado por definição — é para isso que ele serve. Quem
 * preenche credencial nele commita segredo sem perceber, e o .gitignore não ajuda
 * porque não é ele que está ignorado. Aconteceu neste repo. Vira invariante de CI.
 */
function cmdSecrets() {
  out(C.b + '\nsecrets' + C.r + '   arquivos de exemplo devem conter só placeholders');
  const files = ['.tb.env.example', '.env.example', '.env.tb.example'].filter(existsSync);
  if (!files.length) { pass('nenhum arquivo .example para checar'); return; }

  // Um valor é placeholder se for vazio, ou se contiver marca óbvia de exemplo.
  const PLACEHOLDER = /^$|exemplo|example|changeme|your[-_]?|<.*>|\.\.\.|xxx+|placeholder/i;

  for (const f of files) {
    out('\n' + C.b + f + C.r);
    for (const [i, line] of readFileSync(f, 'utf8').split(/\r?\n/).entries()) {
      const m = line.match(/^\s*([A-Z_][A-Z0-9_]*)\s*=\s*(.*?)\s*$/);
      if (!m) continue;
      const [, key, value] = m;
      if (PLACEHOLDER.test(value)) { pass(key + ' = placeholder'); continue; }
      fail(f + ':' + (i + 1) + ' — `' + key + '` tem valor real, não placeholder');
      out('        arquivo .example é COMMITADO. Mova o valor para o arquivo ignorado.');
    }
  }

  // O arquivo ignorado nunca pode estar rastreado, mesmo que alguém force o add.
  for (const f of ['.tb.env', '.env.tb', '.env']) {
    if (!existsSync(f)) continue;
    const tracked = spawnSync('git', ['ls-files', '--error-unmatch', f], { stdio: 'pipe' }).status === 0;
    if (tracked) fail(f + ' está RASTREADO pelo git — deveria estar só no .gitignore');
    else pass(f + ' existe localmente e não está rastreado');
  }
}

/* --------------------------------------------------------------- validate */

function cmdValidate(skills) {
  out(C.b + '\nvalidate' + C.r + '  ' + skills.length + ' skills em ' + SKILLS_DIR);
  for (const s of skills) {
    out('\n' + C.b + s.dir + C.r);
    if (!s.fm) { fail('sem frontmatter YAML (--- ... ---) no topo'); continue; }

    if (!s.fm.name) fail('frontmatter sem `name`');
    else if (s.fm.name !== s.dir) fail('`name: ' + s.fm.name + '` != nome do diretório `' + s.dir + '`');
    else pass('name casa com o diretório');

    const d = s.fm.description;
    if (!d) { fail('frontmatter sem `description` — a skill nunca vai disparar'); }
    else {
      // Armadilha real: mensagens de erro em descriptions costumam ter ": " ("Can't
      // compile script: null"). Num scalar YAML sem aspas isso vira separador de
      // mapping e o loader trunca a description em silêncio — a skill para de disparar
      // e nada avisa. Ou quota o scalar inteiro, ou tira o ": ".
      const rawDesc = s.raw.description || '';
      const quoted = /^['"]/.test(rawDesc);
      if (!quoted && /:\s/.test(rawDesc)) {
        fail('description tem ": " sem estar entre aspas — YAML trunca em silêncio.');
        out('        envolva o valor em aspas simples (escapando \' como \'\') ou remova o ": "');
      } else pass('description YAML segura');

      if (d.length > MAX_DESC) fail('description com ' + d.length + ' chars (máx ' + MAX_DESC + ')');
      else pass('description com ' + d.length + ' chars');
      if (d.length < 80) warn('description muito curta (' + d.length + ') — provavelmente casa pouco');
      if (!/\bUse\b|\bTrigger|\bquando\b|\bao\b/i.test(d)) {
        warn('description não diz QUANDO usar (sem "Use ao…"/"Trigger…") — só descreve o conteúdo');
      }
    }

    const lines = s.text.split(/\r?\n/).length;
    if (lines > MAX_SKILL_LINES) warn('SKILL.md com ' + lines + ' linhas (>' + MAX_SKILL_LINES + ') — mover detalhe para references/');
    else pass('SKILL.md com ' + lines + ' linhas');

    // links relativos precisam existir: link morto vira alucinação silenciosa
    const dirPath = join(SKILLS_DIR, s.dir);
    for (const m of s.body.matchAll(/\]\((?!https?:|#)([^)#]+)(?:#[^)]*)?\)/g)) {
      const target = m[1].trim();
      const abs = target.startsWith('.claude/') || target.startsWith('scripts/') || target.startsWith('tests/')
        ? target : join(dirPath, target);
      if (!existsSync(abs)) fail('link quebrado: ' + target);
      else pass('link ok: ' + target);
    }
  }
}

/* --------------------------------------------------------------- triggers */

// Palavras sem sinal de domínio. As descriptions inevitavelmente contêm verbos de
// enquadramento ("Use ao escrever, revisar ou depurar...") e conectivos ("diferença
// entre...") — deixá-los pontuar faz "escrever um teste em pytest" casar com uma skill
// de ThingsBoard. Não confundir com termos raros de domínio, que devem pontuar.
const STOP = new Set((
  'a o e de da do das dos em no na nos nas um uma uns umas para pra por com sem que qual quais ' +
  'como quando onde meu minha meus minhas ta tá esta está isso esse essa nao não sim mais menos ' +
  'the an of in on to for is are my how what when where it its and or not with ' +
  // verbos/conectivos de enquadramento que toda description carrega
  'use usar usando uso ao escrever revisar depurar criar editar montar fazer ver checar ' +
  'construir explicar planejar gerar entre diferenca diferença tipicos típicos sintomas ' +
  'exemplo exemplos etc caso casos'
).split(/\s+/));

function tokens(s) {
  const flat = String(s).toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '');
  const outSet = new Set();
  for (const t of flat.split(/[^a-z0-9/._{}-]+/)) {
    if (!t) continue;
    // Mantém o token inteiro (preserva "/api/auth/login") E as partes separadas por
    // "/", senão "postgresql/cassandra/timescaledb" vira um token único que nunca casa
    // com quem digita só "cassandra".
    for (const part of [t, ...t.split(/[/]+/)]) {
      const p = part.replace(/^[._-]+|[._-]+$/g, '');
      if (p.length > 2 && !STOP.has(p)) outSet.add(p);
    }
  }
  return [...outSet];
}

/**
 * Score léxico entre a frase e uma description. Termos raros no corpus de descriptions
 * pesam mais (idf simplificado): "thingsboard" aparece em todas, então não discrimina;
 * "widget", "kafka", "tbel" discriminam.
 */
function score(qTokens, descTokens, idf) {
  let s = 0;
  let hits = 0;   // termos distintos que casaram, não só a soma dos pesos
  for (const t of new Set(qTokens)) {
    if (descTokens.has(t)) { s += idf.get(t) || 1; hits++; continue; }
    // Prefixo comum cobre flexão sem stemmer: alarme/alarmes, relacao/relacoes,
    // licenca/licenciamento. 5 chars é curto o bastante para pegar flexão e longo o
    // bastante para não colar palavras diferentes.
    for (const d of descTokens) {
      if (d.length < 5 || t.length < 5) continue;
      let i = 0;
      while (i < d.length && i < t.length && d[i] === t[i]) i++;
      if (i >= 5) { s += (idf.get(t) || 1) * 0.6; hits++; break; }
    }
  }
  return { score: s, hits };
}

function cmdTriggers(skills) {
  if (!existsSync(TRIGGERS)) { out(C.red + 'erro' + C.r + ' ' + TRIGGERS + ' não existe'); process.exitCode = 2; throw new Error("triggers ausente"); }

  const cases = readFileSync(TRIGGERS, 'utf8').split(/\r?\n/)
    .map((l) => l.trim()).filter((l) => l.startsWith('{'))
    .map((l) => JSON.parse(l)).filter((c) => c.q);

  const descTokens = new Map();
  for (const s of skills) descTokens.set(s.dir, new Set(tokens((s.fm && s.fm.description) || '')));

  // idf: termo presente em toda description não discrimina nada
  const df = new Map();
  for (const set of descTokens.values()) for (const t of set) df.set(t, (df.get(t) || 0) + 1);
  const idf = new Map();
  for (const [t, n] of df) idf.set(t, Math.log(1 + skills.length / n));

  out(C.b + '\ntriggers' + C.r + '  ' + cases.length + ' frases contra ' + skills.length + ' descriptions');
  out(C.d + '          scorer léxico — pré-voo, não substitui rodar no agente' + C.r + '\n');

  let hit = 0, miss = 0, ambiguous = 0, falsePos = 0, negOk = 0;

  for (const c of cases) {
    const qt = tokens(c.q);
    const ranked = skills
      .map((s) => {
        const r = score(qt, descTokens.get(s.dir), idf);
        return { skill: s.dir, score: r.score, hits: r.hits };
      })
      .sort((a, b) => b.score - a.score);
    const top = ranked[0];
    const second = ranked[1];

    if (c.skill === null) {
      // Uma única palavra em comum não é match. "git rebase vs merge" compartilha
      // exatamente um termo com uma skill de fork, e isso é coincidência de vocabulário,
      // não intenção. Exigir 2 termos distintos evita ter que empobrecer a description
      // removendo palavras que o usuário realmente digita.
      if (top.score > 1.5 && top.hits >= 2) {
        falsePos++;
        fail('FALSO POSITIVO  "' + c.q + '"');
        out('        nenhuma skill deveria disparar, mas ' + top.skill + ' pontuou ' + top.score.toFixed(2));
      } else { negOk++; pass('negativo ok: "' + c.q + '"'); }
      continue;
    }

    const wanted = ranked.find((r) => r.skill === c.skill);
    if (!wanted || wanted.score === 0) {
      miss++;
      fail('SEM MATCH  "' + c.q + '"');
      out('        esperada: ' + c.skill + ' (score 0) — nenhum termo da frase existe na description');
      const novel = qt.filter((t) => ![...descTokens.values()].some((s) => s.has(t)));
      if (novel.length) out(C.d + '        termos ausentes de TODA description: ' + novel.slice(0, 8).join(', ') + C.r);
      continue;
    }
    if (top.skill !== c.skill) {
      miss++;
      fail('SKILL ERRADA  "' + c.q + '"');
      out('        esperada ' + c.skill + ' (' + wanted.score.toFixed(2) + '), venceu ' + top.skill + ' (' + top.score.toFixed(2) + ')');
      continue;
    }
    if (second && top.score - second.score < 0.5) {
      ambiguous++;
      warn('AMBÍGUA  "' + c.q + '"');
      out('        ' + top.skill + ' ' + top.score.toFixed(2) + ' vs ' + second.skill + ' ' + second.score.toFixed(2) + ' — margem < 0.5');
      continue;
    }
    hit++;
    pass('"' + c.q + '" -> ' + top.skill + ' (' + top.score.toFixed(2) + ')');
  }

  out('');
  const total = cases.length;
  const clean = hit + negOk;
  out(C.b + 'resultado' + C.r + '  ' + clean + '/' + total + ' limpas'
    + C.d + ' (' + hit + ' positivas + ' + negOk + ' negativas)' + C.r
    + '  |  ' + ambiguous + ' ambíguas  |  ' + miss + ' erradas/sem match  |  ' + falsePos + ' falsos positivos');
  out(C.d + '           cobertura: ' + Math.round((clean / total) * 100) + '%' + C.r);
}

/* -------------------------------------------------------------------- main */

const skills = loadSkills();
if (mode === 'secrets' || mode === 'all') cmdSecrets();
if (mode === 'validate' || mode === 'all') cmdValidate(skills);
if (mode === 'triggers' || mode === 'all') cmdTriggers(skills);
if (!['validate', 'triggers', 'secrets', 'all'].includes(mode)) {
  out('uso: node scripts/skill-lint.mjs [validate|triggers|secrets] [--verbose]');
  process.exitCode = 2;
} else {
  out('');
  out(errors ? C.red + errors + ' falha(s)' + C.r + (warns ? ', ' + warns + ' aviso(s)' : '')
             : C.g + 'sem falhas' + C.r + (warns ? ', ' + warns + ' aviso(s)' : ''));
  // else: senão isto sobrescreveria o exit 2 de "modo desconhecido" com 0
  process.exitCode = errors ? 1 : 0;
}
