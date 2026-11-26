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
  xml: {lang: 'markup', label: 'XML', icon: 'code'},
  html: {lang: 'markup', label: 'HTML', icon: 'code'},
  svg: {lang: 'markup', label: 'SVG', icon: 'image'},
  css: {lang: 'css', label: 'CSS', icon: 'code'}
};

const FILE_LANGUAGES: Record<string, CodeLanguage> = {
  ino: {lang: 'c', label: 'Arduino', icon: 'code'},
  h: {lang: 'c', label: 'C', icon: 'code'},
  hpp: {lang: 'c', label: 'C++', icon: 'code'},
  md: {lang: 'text', label: 'Markdown', icon: 'article'},
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
  readFenceMeta(root);
  mergeTabs(root, translate, copy);
  root.querySelectorAll('pre').forEach(pre => {
    if (!pre.closest('.tb-codeblock')) {
      decorate(pre, translate, copy);
    }
  });
}

const FENCE_META = '\u2063tb:';
const OS_TABS: Record<string, string> = {macos: 'macos', mac: 'macos', windows: 'windows', linux: 'linux'};

export function aiFenceMeta(markdown: string): string {
  let open: string | null = null;
  return (markdown ?? '').split('\n').map(line => {
    const fence = /^(\s*)(`{3,}|~{3,})(.*)$/.exec(line);
    if (!fence) {
      return line;
    }
    if (open) {
      if (fence[2].startsWith(open) && !fence[3].trim()) {
        open = null;
      }
      return line;
    }
    open = fence[2];
    const info = fence[3].trim();
    const tab = /(?:^|\s)tab=(.+)$/.exec(info)?.[1]?.trim();
    const words = info.replace(/(?:^|\s)tab=.+$/, '').trim().split(/\s+/).filter(Boolean);
    const fileOf = (word: string) => /^[\w.-]+\.\w+$/.test(word ?? '') ? word : null;
    const file = words.length > 1 ? fileOf(words[1]) : fileOf(words[0]);
    if (!file && !tab) {
      return line;
    }
    const lang = words.length > 1 || !file ? (words[0] ?? '') : fileLanguage(file).lang;
    const meta = JSON.stringify({...(file ? {file} : {}), ...(tab ? {tab} : {})});
    return fence[1] + fence[2] + lang + '\n' + FENCE_META + encodeURIComponent(meta);
  }).join('\n');
}

function readFenceMeta(root: HTMLElement): void {
  root.querySelectorAll('pre > code').forEach(code => {
    const text = code.textContent ?? '';
    if (!text.startsWith(FENCE_META)) {
      return;
    }
    const end = text.indexOf('\n');
    let meta: {file?: string; tab?: string} = {};
    try {
      meta = JSON.parse(decodeURIComponent(text.slice(FENCE_META.length, end < 0 ? text.length : end)));
    } catch {
      meta = {};
    }
    code.textContent = end < 0 ? '' : text.slice(end + 1);
    if (meta.file) {
      code.setAttribute('data-file', meta.file);
      code.setAttribute('data-label', meta.file);
    }
    if (meta.tab) {
      code.setAttribute('data-tab', meta.tab);
    }
  });
}

function mergeTabs(root: HTMLElement, translate: TranslateService, copy: (text: string) => boolean): void {
  const unitOf = (pre: Element): Element => {
    const parent = pre.parentElement;
    return parent && parent !== root && parent.tagName === 'DIV' && !/tb-markdown-view/.test(parent.className)
      && parent.querySelectorAll(':scope > pre').length === 1 ? parent : pre;
  };
  const pres = Array.from(root.querySelectorAll('pre')).filter(pre => pre.querySelector('code[data-tab]') && !pre.closest('.tb-codeblock'));
  const used = new Set<Element>();
  pres.forEach(first => {
    if (used.has(first)) {
      return;
    }
    const group = [first];
    let next = unitOf(first).nextElementSibling;
    while (next) {
      const pre = next.matches('pre') ? next : next.querySelector(':scope > pre');
      if (!pre || !pre.querySelector('code[data-tab]')) {
        break;
      }
      group.push(pre as HTMLPreElement);
      next = unitOf(pre).nextElementSibling;
    }
    group.forEach(pre => used.add(pre));
    if (group.length < 2) {
      return;
    }
    const tabs: CodeTab[] = group.map(pre => {
      const code = pre.querySelector('code');
      const label = code.getAttribute('data-tab');
      const lang = /(?:^|\s)language-([\w+#-]+)/.exec(code.className)?.[1]?.toLowerCase();
      return {id: OS_TABS[label.toLowerCase()] ?? label, label, text: (code.textContent ?? '').replace(/\n$/, ''), lang};
    });
    const os = tabs.every(tab => Object.values(OS_TABS).includes(tab.id));
    const key = os ? 'os' : 'tabs:' + tabs.map(tab => tab.id).join('|');
    const local = /Windows/i.test(navigator.userAgent) ? 'windows' : /Mac/i.test(navigator.userAgent) ? 'macos' : 'linux';
    const preferred = codeTabGroup(key) ?? (os ? local : null);
    const initial = Math.max(0, tabs.findIndex(tab => tab.id === preferred));
    const block = codeTabsBlock(tabs, tabs[0].lang ?? 'shell', translate, copy, initial, undefined, {group: key});
    const anchor = unitOf(group[0]);
    anchor.parentNode.insertBefore(block, anchor);
    group.forEach(pre => unitOf(pre).remove());
  });
}

function languageOf(pre: HTMLElement): CodeLanguage | null {
  const classes = [pre.className, pre.querySelector('code')?.className ?? ''].join(' ');
  const name = /(?:^|\s)language-([\w+#-]+)/.exec(classes)?.[1]?.toLowerCase();
  const file = pre.querySelector('code')?.getAttribute('data-file');
  if (file && (!name || name === 'text' || name === 'plaintext' || name === 'none')) {
    return {...fileLanguage(file), label: file};
  }
  if (!name || name === 'text' || name === 'plaintext' || name === 'none') {
    return null;
  }
  const language = LANGUAGES[name] ?? {lang: 'text', label: name.toUpperCase(), icon: 'code'};
  const label = pre.querySelector('code')?.getAttribute('data-label');
  return label ? {...language, label} : language;
}

function decorate(pre: HTMLElement, translate: TranslateService, copy: (text: string) => boolean): void {
  const text = (pre.textContent ?? '').replace(/\n$/, '');
  const file = pre.querySelector('code')?.getAttribute('data-file');
  const noCopy = pre.querySelector('code')?.hasAttribute('data-nocopy');
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
  const folds = lineCount > FOLD_LINES && lang !== 'diff';
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
    head.append(...[wrap, noCopy ? null : copyButton, file ? downloadBtn(file, text, translate) : null].filter(Boolean));
    block.append(head, pre);
  } else {
    copyButton.classList.add('floating');
    block.append(...[pre, noCopy ? null : copyButton].filter(Boolean));
  }
}

export interface CodeTab {
  label: string;
  text: string;
  id?: string;
  lang?: string;
}

export interface CodeBlockOptions {
  group?: string;
  label?: string;
  icon?: boolean;
  check?: boolean;
  labelledCopy?: boolean;
  classes?: string;
}

const GROUP_EVENT = 'tb-code-tab';
const LABELLED_COPIED_MS = 1600;

interface GroupPick {
  group: string;
  id: string;
  from: HTMLElement;
}

type PickableBlock = HTMLElement & {tbPick?: (id: string) => void};

let groupListening = false;

function listenToGroups(): void {
  if (groupListening) {
    return;
  }
  groupListening = true;
  document.addEventListener(GROUP_EVENT, (event: CustomEvent<GroupPick>) => {
    document.querySelectorAll<PickableBlock>('[data-code-group]').forEach(block => {
      if (block !== event.detail.from && block.dataset.codeGroup === event.detail.group) {
        block.tbPick?.(event.detail.id);
      }
    });
  });
}

export function codeTabGroup(group: string): string | null {
  try {
    return localStorage.getItem('tb-code-' + group);
  } catch {
    return null;
  }
}

function storeCodeTab(group: string, id: string): void {
  try {
    localStorage.setItem('tb-code-' + group, id);
  } catch {
    return;
  }
}

export function pickCodeTab(group: string, id: string, from: HTMLElement = document.body): void {
  storeCodeTab(group, id);
  listenToGroups();
  document.dispatchEvent(new CustomEvent<GroupPick>(GROUP_EVENT, {detail: {group, id, from}}));
}

export function onCodeTabGroup(group: string, picked: (id: string) => void): () => void {
  const listener = (event: CustomEvent<GroupPick>) => {
    if (event.detail.group === group) {
      picked(event.detail.id);
    }
  };
  document.addEventListener(GROUP_EVENT, listener);
  return () => document.removeEventListener(GROUP_EVENT, listener);
}

export function codeTabsBlock(tabs: CodeTab[], lang: string, translate: TranslateService, copy: (text: string) => boolean,
                              initial = 0, picked?: (tab: CodeTab) => void, options: CodeBlockOptions = {}): HTMLElement {
  const languageOf = (tab: CodeTab) => {
    const name = tab.lang ?? lang;
    return LANGUAGES[name] ?? {lang: 'text', label: name.toUpperCase(), icon: 'code'};
  };
  const keyOf = (tab: CodeTab) => tab.id ?? tab.label;
  let current = Math.min(Math.max(initial, 0), tabs.length - 1);
  if (options.group) {
    const stored = codeTabGroup(options.group);
    const index = tabs.findIndex(tab => keyOf(tab) === stored);
    if (index >= 0) {
      current = index;
    }
  }
  const block: PickableBlock = element('div', 'tb-codeblock labeled tabbed' + (options.classes ? ' ' + options.classes : ''));
  const head = element('div', 'tb-codeblock-head');
  const list = element('div', 'tb-codetabs');
  list.setAttribute('role', 'tablist');
  if (options.label) {
    list.setAttribute('aria-label', options.label);
  }
  const code = element('code', '');
  const pre = element('pre', 'tb-code');
  pre.append(code);
  const select = (index: number, byPerson: boolean) => {
    if (index === current) {
      return;
    }
    current = index;
    paint();
    if (byPerson) {
      if (options.group) {
        pickCodeTab(options.group, keyOf(tabs[current]), block);
      }
      picked?.(tabs[current]);
    }
  };
  const buttons = tabs.map((tab, index) => {
    const button = element('button', 'tb-codetab') as HTMLButtonElement;
    button.type = 'button';
    button.setAttribute('role', 'tab');
    button.textContent = tab.label;
    button.addEventListener('click', () => select(index, true));
    button.addEventListener('keydown', event => {
      const step = event.key === 'ArrowRight' ? 1 : event.key === 'ArrowLeft' ? -1 : 0;
      if (step) {
        event.preventDefault();
        select((current + step + tabs.length) % tabs.length, true);
        buttons[current].focus();
      }
    });
    return button;
  });
  const paint = () => {
    const language = languageOf(tabs[current]);
    block.dataset.lang = language.lang;
    block.classList.toggle('shell', language.lang === 'shell' || language.lang === 'powershell');
    code.className = 'lang-' + language.lang;
    buttons.forEach((button, index) => {
      button.setAttribute('aria-selected', String(index === current));
      button.tabIndex = index === current ? 0 : -1;
    });
    code.replaceChildren(...lines(language.lang, tabs[current].text));
  };
  if (options.group) {
    listenToGroups();
    block.dataset.codeGroup = options.group;
    block.tbPick = id => {
      const index = tabs.findIndex(tab => keyOf(tab) === id);
      if (index >= 0) {
        select(index, false);
      }
    };
  }
  list.append(...buttons);
  head.append(...[options.icon === false ? null : icon(languageOf(tabs[current]).icon, 'tb-mat-16'), list,
    copyBtn(() => tabs[current].text, translate, copy, options)].filter(Boolean));
  block.append(head, pre);
  paint();
  return block;
}

export function labelledCodeBlock(text: string, label: string, lang: string, translate: TranslateService, copy: (text: string) => boolean,
                                  options: CodeBlockOptions = {}): HTMLElement {
  const language = LANGUAGES[lang]?.lang ?? 'text';
  const block = element('div', 'tb-codeblock labeled' + (options.classes ? ' ' + options.classes : ''));
  block.dataset.lang = language;
  block.classList.toggle('shell', language === 'shell' || language === 'powershell');
  const head = element('div', 'tb-codeblock-head');
  const name = element('span', 'tb-codeblock-label');
  name.textContent = label;
  head.append(name, copyBtn(text, translate, copy, options));
  const pre = element('pre', 'tb-code');
  const code = element('code', 'lang-' + language);
  code.append(...lines(language, text));
  pre.append(code);
  block.append(head, pre);
  return block;
}

export function valueBlock(label: string, value: string, translate: TranslateService, copy: (text: string) => boolean,
                           ariaLabel?: string): HTMLElement {
  const block = element('div', 'tb-codeblock value');
  block.dataset.lang = 'text';
  const name = element('span', 'tb-codeblock-label');
  name.textContent = label;
  const shown = element('code', 'tb-value');
  shown.textContent = value;
  shown.title = value;
  const button = copyBtn(value, translate, copy, {check: true});
  if (ariaLabel) {
    button.setAttribute('aria-label', ariaLabel);
  }
  block.append(name, shown, button);
  return block;
}

export function commandRow(text: string, windows: boolean, translate: TranslateService, copy: (text: string) => boolean): HTMLElement {
  const row = element('div', 'tb-cmdrow');
  const mark = element('span', 'tb-cmd-p');
  mark.setAttribute('aria-hidden', 'true');
  mark.textContent = windows ? '>' : '$';
  const command = element('code', 'tb-cmd-t');
  command.append(...lines('shell', text).flatMap(line => Array.from(line.childNodes)));
  const button = copyBtn(text, translate, copy, {check: true});
  button.classList.add('tb-cmd-copy');
  button.setAttribute('aria-label', translate.instant('ai-assistant.copy') + ': ' + text);
  row.append(mark, command, button);
  return row;
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

function copyBtn(text: string | (() => string), translate: TranslateService, copy: (text: string) => boolean,
                 options: CodeBlockOptions = {}): HTMLButtonElement {
  const button = element('button', 'tb-codecopy') as HTMLButtonElement;
  button.type = 'button';
  button.dataset.tip = translate.instant('ai-assistant.copy');
  button.dataset.copied = translate.instant('ai-assistant.copied');
  button.setAttribute('aria-label', translate.instant('ai-assistant.copy'));
  const glyph = icon('content_copy', 'tb-mat-16');
  button.append(glyph);
  let timer: ReturnType<typeof setTimeout>;
  if (options.labelledCopy) {
    button.classList.add('labelled');
    const word = element('span', 'lbl');
    word.setAttribute('aria-live', 'polite');
    word.textContent = translate.instant('ai-assistant.copy');
    button.append(word);
    const done = (ms: number) => {
      clearTimeout(timer);
      button.classList.add('done');
      word.textContent = translate.instant('ai-assistant.copied');
      glyph.textContent = 'check';
      timer = setTimeout(() => {
        button.classList.remove('done');
        word.textContent = translate.instant('ai-assistant.copy');
        glyph.textContent = 'content_copy';
      }, ms);
    };
    button.addEventListener('click', () => {
      if (copy(typeof text === 'function' ? text() : text)) {
        done(LABELLED_COPIED_MS);
      }
    });
    return button;
  }
  button.append(icon('check', 'tb-mat-16 tb-copy-ok'));
  button.addEventListener('click', () => {
    if (copy(typeof text === 'function' ? text() : text)) {
      button.classList.add('copied');
      announceCopied(translate.instant('ai-assistant.copied'));
      clearTimeout(timer);
      timer = setTimeout(() => button.classList.remove('copied'), COPIED_MS);
    }
  });
  return button;
}

let copiedRegion: HTMLElement | null = null;

export function announceCopied(words: string): void {
  if (!copiedRegion?.isConnected) {
    copiedRegion = element('div', 'tb-ai-sr-only');
    copiedRegion.setAttribute('role', 'status');
    copiedRegion.setAttribute('aria-live', 'polite');
    document.body.append(copiedRegion);
  }
  const region = copiedRegion;
  region.textContent = '';
  setTimeout(() => region.textContent = words, 30);
}

function downloadBtn(file: string, text: string, translate: TranslateService): HTMLButtonElement {
  const button = element('button', 'tb-codecopy labelled tb-download') as HTMLButtonElement;
  button.type = 'button';
  button.setAttribute('aria-label', translate.instant('ai-assistant.code.download-file', {name: file}));
  const word = element('span', 'lbl');
  word.textContent = translate.instant('ai-assistant.code.download');
  button.append(icon('download', 'tb-mat-16'), word);
  button.addEventListener('click', () => {
    const url = URL.createObjectURL(new Blob([text.endsWith('\n') ? text : text + '\n'], {type: 'text/plain'}));
    const link = document.createElement('a');
    link.href = url;
    link.download = file;
    link.click();
    setTimeout(() => URL.revokeObjectURL(url));
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
  comment?: boolean;
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
    case 'markup':
      return line => markupLine(line, state);
    case 'css':
      return line => cssLine(line, state);
    case 'python':
    case 'js':
    case 'c':
      return line => codeLine(lang, line);
    default:
      return line => [line];
  }
}

function blockComment(line: string, state: ShellState, open: string, close: string): [Piece[], string] {
  const out: Piece[] = [];
  let rest = line;
  while (rest) {
    if (state.comment) {
      const end = rest.indexOf(close);
      if (end < 0) {
        out.push(token('com', rest));
        return [out, ''];
      }
      out.push(token('com', rest.slice(0, end + close.length)));
      rest = rest.slice(end + close.length);
      state.comment = false;
    }
    const start = rest.indexOf(open);
    if (start < 0) {
      return [out, rest];
    }
    out.push(rest.slice(0, start));
    state.comment = true;
    rest = rest.slice(start);
    const end = rest.indexOf(close, open.length);
    if (end >= 0) {
      out.push(token('com', rest.slice(0, end + close.length)));
      rest = rest.slice(end + close.length);
      state.comment = false;
    } else {
      out.push(token('com', rest));
      return [out, ''];
    }
  }
  return [out, ''];
}

function markupLine(line: string, state: ShellState): Piece[] {
  if (state.comment || line.includes('<!--')) {
    const [pieces, rest] = blockComment(line, state, '<!--', '-->');
    return [...pieces.flatMap(piece => typeof piece === 'string' ? markupText(piece) : [piece]), ...markupText(rest)];
  }
  return markupText(line);
}

function markupText(text: string): Piece[] {
  const out: Piece[] = [];
  const pattern = /(?<tag><\/?[\w:-]+)|(?<close>\/?>)|(?<attr>[\w:@.[\]()*#-]+)(?==)|(?<eq>=)|(?<str>"[^"]*"?|'[^']*'?)|(?<bind>\{\{.*?\}\})|(?<flow>@(?:if|else if|else|for|empty|switch|case|default|let)\b)|(?<other>[^<>="'{@]+|[{@>])/g;
  for (const match of text.matchAll(pattern)) {
    const groups = match.groups ?? {};
    if (groups.tag) {
      out.push(token('op', groups.tag.startsWith('</') ? '</' : '<'), token('kw', groups.tag.replace(/^<\/?/, '')));
    } else if (groups.close || groups.eq) {
      out.push(token('op', match[0]));
    } else if (groups.attr) {
      out.push(token('key', groups.attr));
    } else if (groups.str) {
      out.push(token('str', groups.str));
    } else if (groups.bind) {
      out.push(token('var', groups.bind));
    } else if (groups.flow) {
      out.push(token('kw', groups.flow));
    } else {
      out.push(match[0]);
    }
  }
  return out;
}

function cssLine(line: string, state: ShellState): Piece[] {
  const [pieces, rest] = state.comment || line.includes('/*') ? blockComment(line, state, '/*', '*/') : [[], line];
  return [...pieces.flatMap(piece => typeof piece === 'string' ? cssText(piece) : [piece]), ...cssText(rest)];
}

function cssText(text: string): Piece[] {
  const declaration = /^(\s*)([\w-]+)(\s*:\s*)([^;{}]*)(;?)(.*)$/.exec(text);
  if (declaration && !/[{]/.test(text) && !/^\s*[.#&:@]/.test(text)) {
    const [, indent, property, colon, value, semi, tail] = declaration;
    return [indent, token('key', property), token('op', colon), ...cssValue(value), semi ? token('op', semi) : '', ...cssText(tail)];
  }
  const out: Piece[] = [];
  matches(text, /@[\w-]+|[{};]|[^{};@]+/g).forEach(part => {
    if (part.startsWith('@')) {
      out.push(token('kw', part));
    } else if (/^[{};]$/.test(part)) {
      out.push(token('op', part));
    } else {
      out.push(part.trim() ? token('fn', part) : part);
    }
  });
  return out;
}

function cssValue(value: string): Piece[] {
  return matches(value, /#[0-9a-fA-F]{3,8}\b|-?\d*\.?\d+(?:px|%|em|rem|vh|vw|s|ms|deg|fr)?|"[^"]*"|'[^']*'|var\([^)]*\)|[^\s#\d"'(-]+\(?|\s+|./g)
    .map(part => /^["']/.test(part) ? token('str', part) : /^(#|-?\d|\.\d)/.test(part) ? token('num', part)
      : /^var\(/.test(part) ? token('var', part) : /^\s+$/.test(part) ? part : token('val', part));
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
  root.querySelectorAll('table td:first-child').forEach((cell: HTMLElement) => {
    if (cell.firstElementChild?.classList.contains('k-md-lead') && cell.childNodes.length === 1) {
      return;
    }
    const lead = element('span', 'k-md-lead');
    lead.append(...Array.from(cell.childNodes));
    cell.append(lead);
  });
}
