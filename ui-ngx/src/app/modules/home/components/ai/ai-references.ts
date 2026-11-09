// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { baseDetailsPageByEntityType, EntityType } from '@shared/models/entity-type.models';
import { aiEntityKind } from '@home/components/ai/ai-action.models';

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
  const base = baseDetailsPageByEntityType.get(reference.entityType as EntityType);
  if (!base) {
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
