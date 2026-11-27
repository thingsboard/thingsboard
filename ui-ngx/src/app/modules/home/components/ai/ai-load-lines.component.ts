// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ChangeDetectionStrategy, Component, DestroyRef, effect, inject, input, signal, untracked } from '@angular/core';

const ROLL_MS = 720;

@Component({
  selector: 'tb-ai-load-lines',
  template: `
    <span class="tb-ai-sr-only" role="status">{{ current() }}</span>
    <span class="art-load-win" aria-hidden="true">
      @if (lines().length > 1) {
        @for (ghost of lines(); track $index) {
          <b class="art-load-line art-load-ghost">{{ ghost }}</b>
        }
      }
      <span class="art-load-strip" [class.rolling]="!!previous()">
        @if (previous(); as was) {
          <b class="art-load-line was">{{ was }}</b>
        }
        <b class="art-load-line now">{{ current() }}</b>
      </span>
    </span>
  `,
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    'class': 'art-load-lines'
  }
})
export class AiLoadLinesComponent {

  line = input<string | null | undefined>('');
  lines = input<string[]>([]);

  current = signal('');
  previous = signal<string | null>(null);

  private timer: ReturnType<typeof setTimeout>;

  constructor() {
    effect(() => {
      const text = this.line() ?? '';
      untracked(() => this.show(text));
    });
    inject(DestroyRef).onDestroy(() => clearTimeout(this.timer));
  }

  private show(text: string): void {
    const now = this.current();
    if (text === now) {
      return;
    }
    clearTimeout(this.timer);
    this.previous.set(now || null);
    this.current.set(text);
    if (now) {
      this.timer = setTimeout(() => this.previous.set(null), ROLL_MS);
    }
  }
}
