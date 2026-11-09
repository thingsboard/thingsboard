// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ChangeDetectionStrategy, Component, DestroyRef, effect, inject, input, signal, untracked } from '@angular/core';

interface ShownLine {
  id: number;
  text: string;
  state: '' | 'in' | 'out';
}

const ROLL_OUT_MS = 650;

@Component({
  selector: 'tb-ai-load-lines',
  template: `
    @if (lines().length > 1) {
      @for (ghost of lines(); track $index) {
        <b class="art-load-line art-load-ghost" aria-hidden="true">{{ ghost }}</b>
      }
    }
    @for (item of shown(); track item.id) {
      <b class="art-load-line" [class.in]="item.state === 'in'" [class.out]="item.state === 'out'">{{ item.text }}</b>
    }
  `,
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    'class': 'art-load-lines',
    'aria-live': 'polite'
  }
})
export class AiLoadLinesComponent {

  line = input<string | null | undefined>('');
  lines = input<string[]>([]);

  shown = signal<ShownLine[]>([]);

  private next = 0;
  private timers: ReturnType<typeof setTimeout>[] = [];

  constructor() {
    effect(() => {
      const text = this.line() ?? '';
      untracked(() => this.show(text));
    });
    inject(DestroyRef).onDestroy(() => this.timers.forEach(timer => clearTimeout(timer)));
  }

  private show(text: string): void {
    const current = this.shown().filter(item => item.state !== 'out');
    if (current.length && current[current.length - 1].text === text) {
      return;
    }
    const first = !current.length;
    const id = this.next++;
    this.shown.set([...current.map(item => ({...item, state: 'out' as const})), {id, text, state: first ? '' : 'in'}]);
    if (!first) {
      const outIds = current.map(item => item.id);
      this.timers.push(setTimeout(() => this.shown.set(this.shown().filter(item => !outIds.includes(item.id))), ROLL_OUT_MS));
    }
  }
}
