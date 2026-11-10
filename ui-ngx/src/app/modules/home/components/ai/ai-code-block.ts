// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { TranslateService } from '@ngx-translate/core';

export interface CodeLanguage {
  lang: string;
  label: string;
  icon: string;
}

const FOLD_LINES = 10;
const LONG_LINE = 72;
const COPIED_MS = 1500;
const SIDEWAYS = ['python', 'js', 'c', 'json', 'yaml'];

const LANGUAGES: Record<string, CodeLanguage> = {
  bash: {lang: 'shell', label: 'Terminal', icon: 'terminal'},
  sh: {lang: 'shell', label: 'Terminal', icon: 'terminal'},
  shell: {lang: 'shell', label: 'Terminal', icon: 'terminal'},
  zsh: {lang: 'shell', label: 'Terminal', icon: 'terminal'},
  console: {lang: 'shell', label: 'Terminal', icon: 'terminal'},
  powershell: {lang: 'powershell', label: 'PowerShell', icon: 'terminal'},
  ps1: {lang: 'powershell', label: 'PowerShell', icon: 'terminal'},
  json: {lang: 'json', label: 'JSON', icon: 'data_object'},
  yaml: {lang: 'yaml', label: 'YAML', icon: 'description'},
  yml: {lang: 'yaml', label: 'YAML', icon: 'description'},
  log: {lang: 'log', label: 'Log', icon: 'notes'},
  diff: {lang: 'diff', label: 'Diff', icon: 'difference'},
  patch: {lang: 'diff', label: 'Diff', icon: 'difference'},
  js: {lang: 'js', label: 'JavaScript', icon: 'code'},
  javascript: {lang: 'js', label: 'JavaScript', icon: 'code'},
  ts: {lang: 'js', label: 'TypeScript', icon: 'code'},
  typescript: {lang: 'js', label: 'TypeScript', icon: 'code'},
  tbel: {lang: 'js', label: 'TBEL', icon: 'code'},
  python: {lang: 'python', label: 'Python', icon: 'code'},
  py: {lang: 'python', label: 'Python', icon: 'code'},
  c: {lang: 'c', label: 'C', icon: 'code'},
  cpp: {lang: 'c', label: 'C++', icon: 'code'},
  arduino: {lang: 'c', label: 'Arduino', icon: 'code'},
  java: {lang: 'c', label: 'Java', icon: 'code'},
  sql: {lang: 'text', label: 'SQL', icon: 'code'},
  xml: {lang: 'text', label: 'XML', icon: 'code'},
  html: {lang: 'text', label: 'HTML', icon: 'code'},
  css: {lang: 'text', label: 'CSS', icon: 'code'}
};

const FILE_LANGUAGES: Record<string, CodeLanguage> = {
  ino: {lang: 'c', label: 'Arduino', icon: 'code'},
  h: {lang: 'c', label: 'C', icon: 'code'},
  hpp: {lang: 'c', label: 'C++', icon: 'code'},
  md: {lang: 'text', label: 'Markdown', icon: 'article'},
  svg: {lang: 'text', label: 'SVG', icon: 'image'},
  markdown: {lang: 'text', label: 'Markdown', icon: 'article'},
  txt: {lang: 'text', label: 'Text', icon: 'notes'},
  csv: {lang: 'text', label: 'CSV', icon: 'table_chart'},
  toml: {lang: 'text', label: 'TOML', icon: 'description'},
  ini: {lang: 'text', label: 'INI', icon: 'description'},
  conf: {lang: 'text', label: 'Config', icon: 'description'},
  env: {lang: 'shell', label: 'Env', icon: 'description'}
};

export function fileLanguage(name: string): CodeLanguage {
  const ext = name.includes('.') ? name.slice(name.lastIndexOf('.') + 1).toLowerCase() : '';
  return FILE_LANGUAGES[ext] ?? LANGUAGES[ext] ?? {lang: 'text', label: ext ? ext.toUpperCase() : 'Text', icon: 'description'};
}

export function decorateCodeBlocks(root: HTMLElement, translate: TranslateService, copy: (text: string) => boolean): void {
  root.querySelectorAll('pre').forEach(pre => {
    if (!pre.closest('.tb-codeblock')) {
      decorate(pre, translate, copy);
    }
  });
}

function languageOf(pre: HTMLElement): CodeLanguage | null {
  const classes = [pre.className, pre.querySelector('code')?.className ?? ''].join(' ');
  const name = /(?:^|\s)language-([\w+#-]+)/.exec(classes)?.[1]?.toLowerCase();
  if (!name || name === 'text' || name === 'plaintext' || name === 'none') {
    return null;
  }
  return LANGUAGES[name] ?? {lang: 'text', label: name.toUpperCase(), icon: 'code'};
}

function decorate(pre: HTMLElement, translate: TranslateService, copy: (text: string) => boolean): void {
  const text = (pre.textContent ?? '').replace(/\n$/, '');
  const language = languageOf(pre);
  const lang = language?.lang ?? 'text';
  const shell = lang === 'shell' || lang === 'powershell';
  const commands = shell ? splitCommands(text) : [text];
  const code = element('code', 'lang-' + lang);
  if (commands.length > 1) {
    code.append(...commands.map(command => {
      const row = element('div', 'tb-cmd');
      row.append(...lines(lang, command), commandCopyBtn(command, translate, copy));
      return row;
    }));
  } else {
    code.append(...lines(lang, text));
  }
  pre.className = 'tb-code';
  pre.replaceChildren(code);
  const lineCount = text.split('\n').length;
  const folds = lineCount > FOLD_LINES;
  const block = element('div', 'tb-codeblock');
  const wrapper = pre.closest('.code-wrapper');
  if (wrapper) {
    wrapper.parentNode.insertBefore(pre, wrapper);
    wrapper.remove();
  }
  block.dataset.lang = lang;
  block.classList.toggle('shell', shell);
  block.classList.toggle('labeled', !!language || folds);
  block.classList.toggle('folds', folds);
  block.classList.toggle('folded', folds);
  pre.parentNode.insertBefore(block, pre);
  const copyButton = copyBtn(text, translate, copy);
  if (language || folds) {
    const head = element('div', 'tb-codeblock-head');
    const label = element('span', 'tb-codeblock-label');
    label.textContent = language?.label ?? translate.instant('ai-assistant.code.script');
    if (folds) {
      const toggle = element('button', 'tb-codefold') as HTMLButtonElement;
      toggle.type = 'button';
      toggle.setAttribute('aria-expanded', 'false');
      const size = element('span', 'tb-codeblock-size');
      size.textContent = translate.instant('ai-assistant.code.lines', {count: lineCount});
      toggle.append(...[language ? icon(language.icon, 'tb-mat-16') : null, label, size, icon('expand_more', 'tb-mat-18 tb-codefold-tg')].filter(Boolean));
      toggle.addEventListener('click', () => {
        const open = block.classList.toggle('folded') === false;
        toggle.setAttribute('aria-expanded', String(open));
      });
      head.append(toggle);
    } else {
      head.append(icon(language.icon, 'tb-mat-16'), label);
    }
    const wrap = wrapBtn(block, text, translate);
    head.append(...[wrap, copyButton].filter(Boolean));
    block.append(head, pre);
  } else {
    copyButton.classList.add('floating');
    block.append(pre, copyButton);
  }
}

function wrapBtn(block: HTMLElement, text: string, translate: TranslateService): HTMLButtonElement | null {
  if (!SIDEWAYS.includes(block.dataset.lang) || !text.split('\n').some(line => line.length > LONG_LINE)) {
    return null;
  }
  const button = element('button', 'tb-codecopy tb-wrap') as HTMLButtonElement;
  button.type = 'button';
  const tip = (on: boolean) => translate.instant(on ? 'ai-assistant.code.one-line-each' : 'ai-assistant.code.wrap-lines');
  button.dataset.tip = tip(false);
  button.setAttribute('aria-label', translate.instant('ai-assistant.code.wrap-long-lines'));
  button.setAttribute('aria-pressed', 'false');
  button.append(icon('wrap_text', 'tb-mat-16'));
  button.addEventListener('click', () => {
    const on = block.classList.toggle('wrap');
    button.setAttribute('aria-pressed', String(on));
    button.dataset.tip = tip(on);
  });
  return button;
}

function commandCopyBtn(text: string, translate: TranslateService, copy: (text: string) => boolean): HTMLButtonElement {
  const button = copyBtn(text, translate, copy);
  button.classList.add('tb-cmdcopy');
  return button;
}

function copyBtn(text: string, translate: TranslateService, copy: (text: string) => boolean): HTMLButtonElement {
  const button = element('button', 'tb-codecopy') as HTMLButtonElement;
  button.type = 'button';
  button.dataset.tip = translate.instant('ai-assistant.copy');
  button.dataset.copied = translate.instant('ai-assistant.copied');
  button.setAttribute('aria-label', translate.instant('ai-assistant.copy'));
  button.append(icon('content_copy', 'tb-mat-16'));
  let timer: ReturnType<typeof setTimeout>;
  button.addEventListener('click', () => {
    if (copy(text)) {
      button.classList.add('copied');
      clearTimeout(timer);
      timer = setTimeout(() => button.classList.remove('copied'), COPIED_MS);
    }
  });
  return button;
}

function icon(name: string, size: string): HTMLElement {
  const glyph = element('span', `mat-icon material-icons notranslate ${size}`);
  glyph.setAttribute('aria-hidden', 'true');
  glyph.textContent = name;
  return glyph;
}

function element(tag: string, classes: string): HTMLElement {
  const created = document.createElement(tag);
  created.className = classes;
  return created;
}

type Piece = string | HTMLElement | null;

interface ShellState {
  command: boolean;
  quote: string | null;
}

const KEYWORDS: Record<string, RegExp> = {
  python: /^(import|from|as|def|return|if|else|elif|for|in|while|with|try|except|True|False|None|and|or|not|print)$/,
  js: /^(const|let|var|await|async|function|return|if|else|for|of|new|true|false|null|import|from|export)$/,
  c: /^(constexpr|const|char|int|uint16_t|uint32_t|bool|void|return|if|else|define|include|true|false)$/
};

function splitCommands(text: string): string[] {
  const groups: string[][] = [];
  let quote: string | null = null;
  let continued = false;
  text.split('\n').forEach(line => {
    if (!groups.length || (!quote && !continued)) {
      groups.push([]);
    }
    groups[groups.length - 1].push(line);
    quote = openQuoteAfter(line, quote);
    continued = !quote && /\\\s*$/.test(line);
  });
  return groups.map(group => group.join('\n'));
}

function openQuoteAfter(line: string, quote: string | null): string | null {
  for (let i = 0; i < line.length; i++) {
    const char = line[i];
    if (quote) {
      if (char === '\\' && quote === '"') {
        i++;
      } else if (char === quote) {
        quote = null;
      }
    } else if (char === '\\') {
      i++;
    } else if (char === '#' && (i === 0 || /\s/.test(line[i - 1]))) {
      break;
    } else if (char === '"' || char === '\'') {
      quote = char;
    }
  }
  return quote;
}

export function highlightCode(lang: string, text: string): HTMLElement[] {
  return lines(lang, text);
}

function lines(lang: string, text: string): HTMLElement[] {
  const state: ShellState = {command: true, quote: null};
  const highlight = lineHighlighter(lang, state);
  return text.split('\n').map((line, i, all) => {
    let classes = 'tb-line';
    if (i && /\\\s*$/.test(all[i - 1])) {
      classes += ' cont';
    }
    if (lang === 'diff') {
      classes += /^\+(?!\+\+ )/.test(line) ? ' add' : /^-(?!-- )/.test(line) ? ' del' : '';
    }
    const span = element('span', classes);
    span.append(...highlight(line).filter(piece => piece !== null && piece !== ''));
    return span;
  });
}

function lineHighlighter(lang: string, state: ShellState): (line: string) => Piece[] {
  switch (lang) {
    case 'shell':
    case 'powershell':
      return line => shellLine(line, state);
    case 'json':
      return jsonLine;
    case 'yaml':
      return yamlLine;
    case 'log':
      return logLine;
    case 'diff':
      return line => /^(\+\+\+ |--- |@@)/.test(line) ? [token('com', line)] : [line];
    case 'python':
    case 'js':
    case 'c':
      return line => codeLine(lang, line);
    default:
      return line => [line];
  }
}

function token(kind: string | null, text: string): Piece {
  if (!kind) {
    return text;
  }
  const span = element('span', 'hl-' + kind);
  span.textContent = text;
  return span;
}

function shellLine(line: string, state: ShellState): Piece[] {
  if (state.quote) {
    const close = closingQuote(line, state.quote);
    if (close < 0) {
      return [token('str', line)];
    }
    state.quote = null;
    state.command = false;
    return [token('str', line.slice(0, close + 1)), ...shellLine(line.slice(close + 1), state)];
  }
  if (/^\s*#/.test(line)) {
    state.command = true;
    return [token('com', line)];
  }
  const out: Piece[] = [];
  matches(line, /"(?:\\.|[^"\\])*"?|'[^']*'?|\s+|&&|\|\||[|;]|[^\s|;&]+|&/g).forEach(word => {
    if (/^\s+$/.test(word)) {
      out.push(word);
      return;
    }
    if (/^(&&|\|\||\||;|&)$/.test(word)) {
      state.command = true;
      out.push(token('op', word));
      return;
    }
    if (word === '\\') {
      out.push(token('op', word));
      return;
    }
    if (/^["']/.test(word)) {
      state.command = false;
      if (word.length === 1 || !word.endsWith(word[0])) {
        state.quote = word[0];
      }
      out.push(token('str', word));
      return;
    }
    if (state.command && /^[A-Za-z_]\w*=/.test(word)) {
      out.push(token('var', word));
      return;
    }
    const kind = state.command ? 'cmd' : /^--?[A-Za-z]/.test(word) ? 'flag' : /^https?:\/\//.test(word) ? 'str'
      : /^\$/.test(word) ? 'var' : /^\d+(\.\d+)?$/.test(word) ? 'num' : null;
    state.command = false;
    out.push(token(kind, word));
  });
  if (!state.quote && !/\\\s*$/.test(line)) {
    state.command = true;
  }
  return out;
}

function closingQuote(line: string, quote: string): number {
  for (let i = 0; i < line.length; i++) {
    if (line[i] === '\\' && quote === '"') {
      i++;
    } else if (line[i] === quote) {
      return i;
    }
  }
  return -1;
}

function codeLine(lang: string, line: string): Piece[] {
  const comment = lang === 'python' ? /^(\s*)(#.*)$/ : /^(\s*)(\/\/.*)$/;
  const match = line.match(comment);
  if (match) {
    return [match[1], token('com', match[2])];
  }
  const words = new RegExp('"(?:\\\\.|[^"\\\\])*"?|\'(?:\\\\.|[^\'\\\\])*\'?|`[^`]*`?|'
    + (lang === 'python' ? '#.*' : '\\/\\/.*') + '|\\d+(?:\\.\\d+)?|[A-Za-z_$][\\w$]*|\\s+|.', 'g');
  return matches(line, words).map((word, i, all) => {
    if (/^(#|\/\/)/.test(word)) {
      return token('com', word);
    }
    if (/^["'`]/.test(word)) {
      return token('str', word);
    }
    if (/^\d/.test(word)) {
      return token('num', word);
    }
    if (/^(true|false|null|undefined|True|False|None)$/.test(word)) {
      return token('bool', word);
    }
    if (KEYWORDS[lang]?.test(word)) {
      return token('kw', word);
    }
    if (/^[A-Za-z_$]/.test(word) && all[i + 1] === '(') {
      return token('fn', word);
    }
    return /^[{}()[\],.:;=]$/.test(word) ? token('op', word) : word;
  });
}

function jsonLine(line: string): Piece[] {
  return matches(line, /"(?:\\.|[^"\\])*"(\s*:)?|-?\d+(?:\.\d+)?|true|false|null|\s+|./g).flatMap(word => {
    if (/^".*:$/.test(word.replace(/\s/g, ''))) {
      return [token('key', word.replace(/\s*:$/, '')), token('op', word.match(/\s*:$/)[0])];
    }
    if (/^"/.test(word)) {
      return [token('str', word)];
    }
    if (/^-?\d/.test(word)) {
      return [token('num', word)];
    }
    if (/^(true|false|null)$/.test(word)) {
      return [token('bool', word)];
    }
    return [/^[{}[\],]$/.test(word) ? token('op', word) : word];
  });
}

function yamlValue(value: string): Piece[] {
  const match = value.match(/^(\s*)(.*?)(\s+#.*)?$/);
  const text = match[2];
  const kind = !text ? null : /^["']/.test(text) ? 'str' : /^(-?\d+(\.\d+)?)$/.test(text) ? 'num'
    : /^(true|false|null|yes|no)$/i.test(text) ? 'bool' : /^[|>]-?$/.test(text) ? 'op' : 'val';
  return [match[1], text ? token(kind, text) : '', match[3] ? token('com', match[3]) : null];
}

function yamlLine(line: string): Piece[] {
  if (/^\s*#/.test(line)) {
    return [token('com', line)];
  }
  const pair = line.match(/^(\s*)(-\s+)?([\w.\-/]+)(:)(.*)$/);
  if (pair) {
    return [pair[1], pair[2] ? token('op', pair[2]) : null, token('key', pair[3]), token('op', pair[4]), ...yamlValue(pair[5])];
  }
  const item = line.match(/^(\s*)(-\s+)(.*)$/);
  return item ? [item[1], token('op', item[2]), ...yamlValue(item[3])] : [line];
}

function logLine(line: string): Piece[] {
  const match = line.match(/^(.*?\|\s*|\S+\s+\d\d:\d\d:\d\d\S*\s+)?(INFO|WARN(?:ING)?|ERROR|DEBUG)?(.*)$/);
  const level = match[2];
  const kind = !level ? null : level === 'INFO' ? 'lvl' : level === 'DEBUG' ? 'com' : /^WARN/.test(level) ? 'warn' : 'err';
  return [match[1] ? token('com', match[1]) : null, level ? token(kind, level) : null, match[3]];
}

function matches(line: string, pattern: RegExp): string[] {
  return line.match(pattern) ?? [];
}

const LONG_CELL = 32;

export function decorateTables(root: HTMLElement): void {
  root.querySelectorAll('table td, table th').forEach((cell: HTMLElement) => {
    cell.classList.toggle('long', (cell.textContent ?? '').trim().length > LONG_CELL);
  });
}
