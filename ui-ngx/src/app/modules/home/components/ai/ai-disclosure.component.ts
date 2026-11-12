// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ChangeDetectionStrategy, Component, computed, input, signal } from '@angular/core';

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

  private toggledOpen = signal<boolean | null>(null);

  open = computed(() => this.toggledOpen() ?? aiDisclosureRemembered(this.key()));

  classes = computed(() => 'tb-disc ' + (this.variant() === 'details' ? 'tb-disc-details'
    : this.variant() === 'divided' ? 'tb-disc-section tb-disc-divided' : 'tb-disc-section'));

  toggled(open: boolean): void {
    if (open !== this.open()) {
      this.toggledOpen.set(open);
      aiDisclosureRemember(this.key(), open);
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
