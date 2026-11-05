// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ChangeDetectionStrategy, Component, computed, DestroyRef, inject, input, NgZone, OnInit, signal } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { AiSpinKind, aiWorkClock } from '@home/components/ai/ai-action.models';

const CLOCK_AFTER_MS = 10_000;
const STILL_AFTER_MS = 30_000;

@Component({
  selector: 'tb-ai-work-line',
  template: `
    <tb-ai-spin [kind]="kind()"></tb-ai-spin>
    <span class="say">{{ say() }}</span>
    @if (clock(); as time) {
      <span class="clock">{{ time }}</span>
    }
  `,
  styleUrls: ['./ai-work-line.component.scss'],
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    'role': 'status',
    '[attr.aria-label]': 'words()'
  }
})
export class AiWorkLineComponent implements OnInit {

  words = input.required<string>();

  kind = input<AiSpinKind>('think');

  since = input<number>(Date.now());

  private now = signal(Date.now());

  private elapsed = computed(() => Math.max(0, this.now() - this.since()));

  say = computed(() => {
    const words = this.words();
    if (this.elapsed() < STILL_AFTER_MS || !words) {
      return words;
    }
    return this.translate.instant('ai-assistant.work.still', {words: words.charAt(0).toLocaleLowerCase() + words.slice(1)});
  });

  clock = computed(() => this.elapsed() >= CLOCK_AFTER_MS ? aiWorkClock(this.elapsed()) : undefined);

  private translate = inject(TranslateService);
  private zone = inject(NgZone);
  private destroyRef = inject(DestroyRef);

  ngOnInit(): void {
    const timer = this.zone.runOutsideAngular(() => setInterval(() => this.zone.run(() => this.now.set(Date.now())), 1000));
    this.destroyRef.onDestroy(() => clearInterval(timer));
  }
}
