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

export const ITEM_LINK_PLACEHOLDER_REGEX =
  /\$\{item-link:([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})}/g;

export function itemLinkCardTag(itemId: string): string {
  return `<tb-iot-hub-item-link-card itemId="${itemId}"></tb-iot-hub-item-link-card>`;
}

export function replaceItemLinkPlaceholders(markdown: string): string {
  if (!markdown) {
    return markdown;
  }
  return markdown.replace(ITEM_LINK_PLACEHOLDER_REGEX, (_match, uuid) => itemLinkCardTag(uuid));
}

export interface DocLinks {
  productURL?: string;
  datasheetURL?: string;
}

export interface DocLinkLabels {
  productPage: string;
  datasheet: string;
}

export function resolveDocLinkPlaceholders(
  markdown: string,
  name: string,
  links: DocLinks,
  labels: DocLinkLabels
): string {
  return markdown
  .replace(/\$\{product\.button}/g, () =>
    links.productURL ? buildDocLinkButton(links.productURL, `${name} ${labels.productPage}`, 'open_in_new') : '')
  .replace(/\$\{datasheet\.button}/g, () =>
    links.datasheetURL ? buildDocLinkButton(links.datasheetURL, `${name} ${labels.datasheet}`, 'description') : '');
}

function buildDocLinkButton(url: string, text: string, icon: string): string {
  const safeUrl = escapeHtmlAttr(url);
  const safeText = escapeHtml(text);
  return `<a href="${safeUrl}" target="_blank" rel="noopener noreferrer" class="mr-2 mb-2" mat-stroked-button>` +
    `<tb-icon matButtonIcon>${icon}</tb-icon><span>${safeText}</span></a>`;
}

export function escapeHtml(value: string): string {
  return value.replace(/[&<>]/g, ch => ch === '&' ? '&amp;' : ch === '<' ? '&lt;' : '&gt;');
}

export function escapeHtmlAttr(value: string): string {
  return value.replace(/[&<>"']/g, ch => {
    switch (ch) {
      case '&': return '&amp;';
      case '<': return '&lt;';
      case '>': return '&gt;';
      case '"': return '&quot;';
      default: return '&#39;';
    }
  });
}

// Inline-only tags + a small attribute whitelist that are safe to keep
// in caption strings (or other small bits of user-authored HTML inside
// generated markdown). Anything else is dropped — disallowed tags are
// replaced by their text content and disallowed attributes are removed.
const SAFE_INLINE_TAGS: ReadonlySet<string> = new Set([
  'B', 'STRONG', 'I', 'EM', 'U', 'S', 'MARK',
  'SMALL', 'SUB', 'SUP', 'BR', 'CODE', 'SPAN'
]);

const SAFE_INLINE_ATTRS: ReadonlySet<string> = new Set(['class', 'style']);

export function sanitizeInlineHtml(value: string): string {
  if (!value) {
    return '';
  }
  const doc = new DOMParser().parseFromString(`<div>${value}</div>`, 'text/html');
  const root = doc.body.firstElementChild as HTMLElement | null;
  if (!root) {
    return '';
  }
  const sanitize = (parent: Element): void => {
    const children = Array.from(parent.childNodes);
    for (const node of children) {
      if (node.nodeType !== Node.ELEMENT_NODE) {
        continue;
      }
      const el = node as Element;
      if (!SAFE_INLINE_TAGS.has(el.tagName)) {
        // Replace disallowed elements with their plain text content.
        parent.replaceChild(doc.createTextNode(el.textContent || ''), el);
        continue;
      }
      for (const attr of Array.from(el.attributes)) {
        if (!SAFE_INLINE_ATTRS.has(attr.name)) {
          el.removeAttribute(attr.name);
          continue;
        }
        if (attr.name === 'style' && /(expression\s*\(|javascript:|url\s*\()/i.test(attr.value)) {
          el.removeAttribute(attr.name);
        }
      }
      sanitize(el);
    }
  };
  sanitize(root);
  return root.innerHTML;
}

