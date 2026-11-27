// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ChangeDetectionStrategy, Component, computed, ElementRef, inject, input, signal } from '@angular/core';

const STORE = 'tb-ai-disclosure';

export function aiDisclosureRemembered(key: string): boolean {
  try {
    return !!key && JSON.parse(sessionStorage.getItem(STORE) ?? '[]').includes(key);
  } catch {
    return false;
  }
}

export function aiDisclosureRemember(key: string, open: boolean): void {
  if (!key) {
    return;
  }
  try {
    const keys: string[] = JSON.parse(sessionStorage.getItem(STORE) ?? '[]').filter((item: string) => item !== key);
    if (open) {
      keys.push(key);
    }
    sessionStorage.setItem(STORE, JSON.stringify(keys.slice(-200)));
  } catch {
    return;
  }
}

const REVEAL_GAP = 12;

function scrollerOf(element: HTMLElement): HTMLElement | null {
  for (let node = element.parentElement; node; node = node.parentElement) {
    const overflow = getComputedStyle(node).overflowY;
    if ((overflow === 'auto' || overflow === 'scroll') && node.scrollHeight > node.clientHeight) {
      return node;
    }
  }
  return null;
}

export function aiRevealOpened(element: HTMLElement): void {
  requestAnimationFrame(() => requestAnimationFrame(() => {
    const scroller = scrollerOf(element);
    if (!scroller) {
      return;
    }
    const box = element.getBoundingClientRect();
    const view = scroller.getBoundingClientRect();
    const composer = scroller.querySelector<HTMLElement>('.ai-chat-composer') ?? element.closest('.ai-chat')?.querySelector<HTMLElement>('.ai-chat-composer');
    const composerTop = composer?.getBoundingClientRect().top ?? view.bottom;
    const bottom = Math.min(view.bottom, composerTop) - REVEAL_GAP;
    const overflow = box.bottom - bottom;
    if (overflow <= 0) {
      return;
    }
    const room = box.top - view.top - REVEAL_GAP;
    scroller.scrollBy({top: Math.min(overflow, Math.max(room, 0)), behavior: 'smooth'});
  }));
}

@Component({
  selector: 'tb-ai-disclosure',
  template: `
    <details [class]="classes()" [open]="open()" (toggle)="toggled($any($event.target).open)">
      <summary>{{ summary() }}</summary>
      @if (open()) {
        <ng-content></ng-content>
      }
    </details>
  `,
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    'class': 'tb-ai-disclosure'
  }
})
export class AiDisclosureComponent {

  summary = input.required<string>();

  variant = input<'details' | 'section' | 'divided'>('section');

  key = input<string>('');

  private host = inject(ElementRef<HTMLElement>);

  private toggledOpen = signal<boolean | null>(null);

  open = computed(() => this.toggledOpen() ?? aiDisclosureRemembered(this.key()));

  classes = computed(() => 'tb-disc ' + (this.variant() === 'details' ? 'tb-disc-details'
    : this.variant() === 'divided' ? 'tb-disc-section tb-disc-divided' : 'tb-disc-section'));

  toggled(open: boolean): void {
    if (open !== this.open()) {
      this.toggledOpen.set(open);
      aiDisclosureRemember(this.key(), open);
      if (open) {
        aiRevealOpened(this.host.nativeElement);
      }
    }
  }
}

export function decorateSections(root: HTMLElement, scope: string): void {
  root.querySelectorAll<HTMLDetailsElement>('details:not(.tb-disc)').forEach((section, index) => {
    const summary = section.querySelector(':scope > summary');
    if (!summary) {
      section.insertBefore(Object.assign(document.createElement('summary'), {textContent: '…'}), section.firstChild);
    }
    section.classList.add('tb-disc', 'tb-disc-section');
    const key = `${scope}:${index}:${section.querySelector(':scope > summary')?.textContent?.trim() ?? ''}`;
    if (aiDisclosureRemembered(key)) {
      section.open = true;
    }
    section.addEventListener('toggle', () => aiDisclosureRemember(key, section.open));
  });
}
