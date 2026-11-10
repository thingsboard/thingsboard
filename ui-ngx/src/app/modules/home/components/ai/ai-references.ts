// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { baseDetailsPageByEntityType, EntityType } from '@shared/models/entity-type.models';
import { aiEntityKind } from '@home/components/ai/ai-action.models';
import { aiSevMarkElement } from '@home/components/ai/ai-sev-mark.component';

export interface AiReference {
  name: string;
  entityType: string;
  id: string;
}

const SKIP = 'code, pre, a, table, .tb-chiplink, .k-ent';

export function decorateReferences(root: HTMLElement, references: AiReference[] | undefined): void {
  if (!references?.length) {
    return;
  }
  const linkable = references
    .filter(reference => reference.name?.trim().length >= 3)
    .sort((a, b) => b.name.length - a.name.length);
  if (!linkable.length) {
    return;
  }
  const pattern = new RegExp(`(?<![\\p{L}\\p{N}_])(${linkable.map(reference => escape(reference.name)).join('|')})(?![\\p{L}\\p{N}_])`, 'u');
  const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT, {
    acceptNode: node => node.parentElement?.closest(SKIP) || !node.textContent?.trim() ? NodeFilter.FILTER_REJECT : NodeFilter.FILTER_ACCEPT
  });
  const nodes: Text[] = [];
  while (walker.nextNode()) {
    nodes.push(walker.currentNode as Text);
  }
  nodes.forEach(node => replaceIn(node, pattern, linkable));
}

function replaceIn(node: Text, pattern: RegExp, references: AiReference[]): void {
  let rest = node.textContent ?? '';
  let match = pattern.exec(rest);
  if (!match) {
    return;
  }
  const fragment = document.createDocumentFragment();
  while (match) {
    if (match.index > 0) {
      fragment.append(rest.slice(0, match.index));
    }
    const reference = references.find(item => item.name === match[1]);
    fragment.append(reference ? chip(reference) : match[1]);
    rest = rest.slice(match.index + match[1].length);
    match = pattern.exec(rest);
  }
  if (rest) {
    fragment.append(rest);
  }
  node.replaceWith(fragment);
}

function chip(reference: AiReference): HTMLElement {
  const kind = aiEntityKind(reference.entityType);
  const code = document.createElement('code');
  code.className = 'k-ent';
  code.dataset.pal = kind.palette;
  const icon = document.createElement('span');
  icon.className = 'mat-icon notranslate material-icons mat-ligature-font k-ent-ic';
  icon.setAttribute('aria-hidden', 'true');
  icon.textContent = kind.icon;
  code.append(icon, reference.name);
  const base = reference.entityType === 'ALARM_RULE' ? '/alarms/alarm-rules' : baseDetailsPageByEntityType.get(reference.entityType as EntityType);
  if (!base || !reference.id) {
    return code;
  }
  const link = document.createElement('a');
  link.className = 'tb-chiplink';
  link.href = `${base}/${reference.id}`;
  link.append(code);
  return link;
}

function escape(text: string): string {
  return text.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

export function decorateLinks(root: HTMLElement): void {
  root.querySelectorAll<HTMLAnchorElement>('a[href]:not(.tb-chiplink)').forEach(link => {
    if (link.closest('pre, code') || link.origin === location.origin || !/^https?:$/.test(link.protocol)) {
      return;
    }
    link.target = '_blank';
    link.rel = 'noopener noreferrer';
    if (!link.querySelector('.k-ext')) {
      const icon = document.createElement('span');
      icon.className = 'mat-icon notranslate material-icons mat-ligature-font k-ext';
      icon.setAttribute('aria-hidden', 'true');
      icon.textContent = 'open_in_new';
      link.append(icon);
    }
  });
}

const SEVERITY_SKIP = 'code, pre, a, table, .k-sev, .tb-chiplink';

export function decorateSeverities(root: HTMLElement, severities: string[] | undefined, label: (severity: string) => string): void {
  if (!severities?.length) {
    return;
  }
  const pattern = new RegExp(`(?<![\\p{L}\\p{N}_])(${severities.map(severity => severity.toLowerCase()).join('|')})(?![\\p{L}\\p{N}_])`, 'iu');
  const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT, {
    acceptNode: node => node.parentElement?.closest(SEVERITY_SKIP) || !node.textContent?.trim() ? NodeFilter.FILTER_REJECT : NodeFilter.FILTER_ACCEPT
  });
  const nodes: Text[] = [];
  while (walker.nextNode()) {
    nodes.push(walker.currentNode as Text);
  }
  nodes.forEach(node => {
    let rest = node.textContent ?? '';
    let match = pattern.exec(rest);
    if (!match) {
      return;
    }
    const fragment = document.createDocumentFragment();
    while (match) {
      if (match.index > 0) {
        fragment.append(rest.slice(0, match.index));
      }
      const severity = match[1].toUpperCase();
      const chip = document.createElement('span');
      chip.className = 'k-sev ' + severity.toLowerCase();
      chip.append(aiSevMarkElement(severity), label(severity));
      fragment.append(chip);
      rest = rest.slice(match.index + match[1].length);
      match = pattern.exec(rest);
    }
    if (rest) {
      fragment.append(rest);
    }
    node.replaceWith(fragment);
  });
}

const HINT_KINDS: Record<string, {kind: string; icon: string}> = {
  note: {kind: 'note', icon: 'info_outline'},
  tip: {kind: 'tip', icon: 'lightbulb_outline'},
  'heads-up': {kind: 'heads-up', icon: 'outlined_flag'}
};

export function decorateHints(root: HTMLElement): void {
  root.querySelectorAll<HTMLElement>('blockquote').forEach(quote => {
    const paragraph = quote.querySelector('p') ?? quote;
    const first = paragraph.firstChild;
    const match = first?.nodeType === Node.TEXT_NODE ? /^\s*(note|tip|heads-up)\s*:\s*/i.exec(first.textContent ?? '') : null;
    if (!match) {
      return;
    }
    const hint = HINT_KINDS[match[1].toLowerCase()];
    first.textContent = (first.textContent ?? '').slice(match[0].length);
    const box = document.createElement('div');
    box.className = 'k-hint agent-hint ' + hint.kind;
    const icon = document.createElement('mat-icon');
    icon.className = 'mat-icon material-icons mat-icon-no-color';
    icon.setAttribute('aria-hidden', 'true');
    icon.textContent = hint.icon;
    const text = document.createElement('span');
    paragraph.querySelectorAll('strong').forEach(strong => {
      const lead = document.createElement('b');
      lead.append(...Array.from(strong.childNodes));
      strong.replaceWith(lead);
    });
    text.append(...Array.from(paragraph.childNodes));
    box.append(icon, text);
    quote.replaceWith(box);
  });
}
