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

  running = computed(() => this.translate.instant(this.words().key + '.run', this.words().params));

  outcome = computed(() => {
    const action = this.action();
    const words = this.words();
    switch (action.status) {
      case 'SUCCESS':
        return this.translate.instant(words.key + '.done', words.params);
      case 'FAILURE':
        return this.translate.instant('ai-assistant.action.failed', {words: this.lowerFirst(this.running())});
      default:
        return this.translate.instant('ai-assistant.work.stopped', {words: this.lowerFirst(this.running())});
    }
  });

  icon = computed(() => {
    switch (this.action().status) {
      case 'SUCCESS':
        return 'check';
      case 'FAILURE':
        return 'error_outline';
      default:
        return 'block';
    }
  });

  duration = computed(() => this.action().durationMs !== undefined ? aiActionDuration(this.action().durationMs) : '');

  request = computed(() => aiActionDetail(this.action().arguments));

  result = computed(() => aiActionDetail(this.action().result));

  hasDetails = computed(() => !!this.request() || !!this.result());

  private lowerFirst(words: string): string {
    return words ? words.charAt(0).toLocaleLowerCase() + words.slice(1) : words;
  }
}
