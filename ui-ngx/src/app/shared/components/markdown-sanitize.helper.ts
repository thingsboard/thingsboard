///
/// Copyright © 2016-2026 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

/** Block keywords: @keyword (...) { body } */
const BLOCK_KEYWORDS = [
  'if', 'else if', 'else', 'for', 'empty', 'switch', 'case',
  'default', 'defer', 'placeholder', 'loading', 'error',
];

/** Inline keywords: @keyword identifier = expr; */
const INLINE_KEYWORDS = ['let'];

/** Chain keywords that can follow a closing '}' */
const CHAIN_KEYWORDS = [
  'else if', 'else', 'empty', 'error', 'placeholder', 'loading',
];

/** Validation: inline keyword → regex that must match after keyword */
const INLINE_VALIDATORS: Record<string, RegExp> = {
  let: /^\s*[a-zA-Z_$][a-zA-Z0-9_$]*\s*=/,
};

/** HTML entities decoded in preserved blocks (order matters: &amp; must be last) */
const HTML_ENTITIES: [RegExp, string][] = [
  [/&#39;/g,  "'"],
  [/&quot;/g, '"'],
  [/&lt;/g,   '<'],
  [/&gt;/g,   '>'],
  [/&amp;/g,  '&'],
];

/** Matches any known @keyword at position 0 */
const KEYWORD_RE = new RegExp(
  `@(${[...BLOCK_KEYWORDS, ...INLINE_KEYWORDS]
    .sort((a, b) => b.length - a.length).join('|')})(?=[\\s(;{])`,
);

/** Matches a chain keyword after '}' */
const CHAIN_RE = new RegExp(
  `^\\s*@(${CHAIN_KEYWORDS
    .sort((a, b) => b.length - a.length).join('|')})(?=[\\s(;{])`,
);

// ─── Public API ──────────────────────────────────────────────────────

/**
 * Sanitize a markdown-compiled HTML template for safe Angular compilation.
 *
 * Escapes `{`, `}`, `@` that are not part of Angular control flow,
 * preserves recognized control flow blocks (@if, @for, @switch, @let, @defer …),
 * decodes HTML entities inside them, and protects `<pre>` code blocks.
 */
export function sanitizeTemplate(template: string): string {
  // Phase 1 — protect zones that must not be processed
  const protectedZones: string[] = [];
  template = extractProtectedZones(template, protectedZones);

  // Phase 2 — extract Angular control flow blocks
  const blocks: string[] = [];
  template = extractControlFlowBlocks(template, blocks);

  // Phase 3 — decode HTML entities inside control flow blocks
  decodeHtmlEntities(blocks);

  // Phase 4 — escape Angular-sensitive chars in remaining text
  template = escapeAngularChars(template);

  // Phase 5 — restore control flow blocks (as-is)
  template = restoreBlocks(template, '__NG_BLOCK_', blocks);

  // Phase 6 — restore protected zones (with escaping)
  template = restoreBlocks(template, '__PROTECTED_', protectedZones, true);

  return template;
}

// ─── Phase helpers ───────────────────────────────────────────────────

function extractProtectedZones(template: string, zones: string[]): string {
  return template.replace(/<pre[\s\S]*?<\/pre>/g, match => {
    zones.push(match);
    return `__PROTECTED_${zones.length - 1}__`;
  });
}

function decodeHtmlEntities(blocks: string[]): void {
  for (let i = 0; i < blocks.length; i++) {
    for (const [pattern, replacement] of HTML_ENTITIES) {
      blocks[i] = blocks[i].replace(pattern, replacement);
    }
  }
}

function escapeAngularChars(text: string): string {
  return text.replace(/{/g, '&#123;').replace(/}/g, '&#125;').replace(/@/g, '&#64;');
}

function restoreBlocks(template: string, prefix: string, blocks: string[], escape = false): string {
  let restored = '';
  let searchStart = 0;
  for (let i = 0; i < blocks.length; i++) {
    const token = `${prefix}${i}__`;
    const tokenIndex = template.indexOf(token, searchStart);
    if (tokenIndex === -1) continue;
    const content = escape ? escapeAngularChars(blocks[i]) : blocks[i];
    restored += template.slice(searchStart, tokenIndex) + content;
    searchStart = tokenIndex + token.length;
  }
  restored += template.slice(searchStart);
  return restored;
}

// ─── Control flow extraction ─────────────────────────────────────────

function extractControlFlowBlocks(template: string, blocks: string[]): string {
  let result = '';
  let i = 0;
  while (i < template.length) {
    if (template[i] !== '@') {
      result += template[i++];
      continue;
    }
    const extracted = tryExtractInline(template, i)
                   ?? tryExtractBlock(template, i);
    if (extracted) {
      blocks.push(extracted.content);
      result += `__NG_BLOCK_${blocks.length - 1}__`;
      i = extracted.end;
    } else {
      result += template[i++];
    }
  }
  return result;
}

/** Try to extract an inline statement: @let identifier = expr; */
function tryExtractInline(template: string, pos: number): { content: string; end: number } | null {
  const match = matchKeywordAt(template, pos);
  if (!match || !INLINE_KEYWORDS.includes(match.keyword)) {
    return null;
  }
  const validator = INLINE_VALIDATORS[match.keyword];
  if (validator && !validator.test(template.substring(pos + match.raw.length))) {
    return null;
  }
  const semicolonIdx = findStatementSemicolon(template, pos);
  if (semicolonIdx === -1) {
    return null;
  }
  return { content: template.substring(pos, semicolonIdx + 1), end: semicolonIdx + 1 };
}

/** Try to extract a block statement: @keyword (...) { body } with optional chains */
function tryExtractBlock(template: string, start: number): { content: string; end: number } | null {
  let i = start;
  const len = template.length;

  while (i < len) {
    const match = matchKeywordAt(template, i);
    if (!match) {
      return null;
    }
    i += match.raw.length;

    // optional parenthesized params
    i = skipWhitespace(template, i);
    if (i < len && template[i] === '(') {
      const parenEnd = findMatchingPair(template, i, '(', ')');
      if (parenEnd === -1) return null;
      i = skipWhitespace(template, parenEnd + 1);
    }

    // required { body }
    if (i >= len || template[i] !== '{') return null;
    const braceEnd = findMatchingPair(template, i, '{', '}');
    if (braceEnd === -1) return null;
    i = braceEnd + 1;

    // check for chain keyword (@else, @empty, etc.)
    const chainMatch = template.substring(i).match(CHAIN_RE);
    if (chainMatch) {
      i += chainMatch[0].indexOf('@');
      continue;
    }

    return { content: template.substring(start, i), end: i };
  }
  return null;
}

// ─── Low-level helpers ───────────────────────────────────────────────

function matchKeywordAt(template: string, pos: number): { keyword: string; raw: string } | null {
  const remaining = template.substring(pos);
  const m = KEYWORD_RE.exec(remaining);
  return m && m.index === 0 ? { keyword: m[1], raw: m[0] } : null;
}

function skipWhitespace(str: string, pos: number): number {
  while (pos < str.length && (str[pos] === ' ' || str[pos] === '\n' || str[pos] === '\r' || str[pos] === '\t')) {
    pos++;
  }
  return pos;
}

function findMatchingPair(template: string, start: number, open: string, close: string): number {
  let depth = 0;
  return scanOutsideStrings(template, start, pos => {
    if (template[pos] === open) depth++;
    else if (template[pos] === close) {
      if (--depth === 0) return pos;
    }
  });
}

/** Find the real statement-terminating ; (skipping ; inside HTML entities and entity-encoded strings) */
function findStatementSemicolon(template: string, start: number): number {
  return scanOutsideStrings(template, start, pos => {
    if (template[pos] === ';') {
      const before = template.substring(Math.max(start, pos - 12), pos);
      if (!/&[a-zA-Z0-9#]+$/.test(before)) return pos;
    }
  });
}

/** Scan template skipping entity-encoded strings (&quot;...&quot;, &#39;...&#39;), call handler for each char outside strings */
function scanOutsideStrings(template: string, start: number, onChar: (pos: number) => number): number {
  const ENTITY_QUOTES = ['&quot;', '&#39;'];
  let inString: string | null = null;
  let i = start;
  while (i < template.length) {
    if (!inString) {
      const quote = ENTITY_QUOTES.find(q => template.startsWith(q, i));
      if (quote) { inString = quote; i += quote.length; continue; }
    } else if (template.startsWith(inString, i)) {
      i += inString.length; inString = null; continue;
    }
    if (!inString) {
      const result = onChar(i);
      if (result !== undefined) return result;
    }
    i++;
  }
  return -1;
}
