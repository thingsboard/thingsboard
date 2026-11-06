// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import {
  AiAction,
  aiActionDetail,
  aiActionDuration,
  aiActionWords
} from '@home/components/ai/ai-action.models';

@Component({
  selector: 'tb-ai-action-line',
  templateUrl: './ai-action-line.component.html',
  styleUrls: ['./ai-action-line.component.scss'],
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '[class.run]': 'action().status === "RUNNING"',
    '[class.fail]': 'action().status === "FAILURE"',
    '[class.stopped]': 'action().status === "STOPPED"'
  }
})
export class AiActionLineComponent {

  action = input.required<AiAction>();

  private translate = inject(TranslateService);

  words = computed(() => aiActionWords(this.action().tool, this.action().arguments));

  more = input(0);

  outcomeText = input<string>();

  autoApproved = input(false);

  private runWords = computed(() => this.translate.instant(this.words().key + '.run', this.words().params));

  running = computed(() => this.more() > 0
    ? this.translate.instant('ai-assistant.work.more', {words: this.runWords(), count: this.more()})
    : this.runWords());

  outcome = computed(() => {
    const words = this.outcomeWords();
    return this.autoApproved() ? this.translate.instant('ai-assistant.action.auto-approved', {words}) : words;
  });

  private outcomeWords(): string {
    const action = this.action();
    if (this.outcomeText()) {
      return this.outcomeText();
    }
    if (action.status === 'FAILURE') {
      return this.reason(action.result) ?? this.translate.instant('ai-assistant.action.failed', {words: this.lowerFirst(this.runWords())});
    }
    const words = this.words();
    return this.translate.instant(words.key + '.done', words.params);
  }

  duration = computed(() => this.action().durationMs !== undefined ? aiActionDuration(this.action().durationMs) : '');

  request = computed(() => aiActionDetail(this.action().arguments));

  result = computed(() => aiActionDetail(this.action().result));

  hasDetails = computed(() => !!this.request() || !!this.result());

  private reason(result?: string): string | undefined {
    const text = result?.trim();
    if (!text || text.startsWith('{') || text.startsWith('[')) {
      return undefined;
    }
    const line = text.split('\n')[0].trim();
    return line.length > 300 ? line.slice(0, 300) + '…' : line;
  }

  private lowerFirst(words: string): string {
    return words ? words.charAt(0).toLocaleLowerCase() + words.slice(1) : words;
  }
}
