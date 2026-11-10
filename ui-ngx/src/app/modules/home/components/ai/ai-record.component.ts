// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { aiEntityKind } from '@home/components/ai/ai-action.models';

export interface AiRecordState {
  tone: 'ok' | 'off' | 'warn';
  label: string;
}

@Component({
  selector: 'tb-ai-record',
  template: `
    <span class="tb-rec-tile"><mat-icon aria-hidden="true">{{ kind().icon }}</mat-icon></span>
    @if (url()) {
      <a class="tb-rec-name" [routerLink]="url()" [matTooltip]="name()" matTooltipPosition="above" [matTooltipShowDelay]="600">{{ name() }}</a>
    } @else {
      <span class="tb-rec-name" [matTooltip]="name()" matTooltipPosition="above" [matTooltipShowDelay]="600">{{ name() }}</span>
    }
    @if (meta()) {
      <span class="tb-rec-meta">{{ meta() }}</span>
    }
    @if (state(); as current) {
      <span class="tb-state" [class]="'tb-state ' + current.tone"><i></i>{{ current.label }}</span>
    }
  `,
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    'class': 'tb-rec line',
    '[attr.data-pal]': 'kind().palette'
  }
})
export class AiRecordComponent {

  entityType = input.required<string>();

  alarmRule = input(false);

  name = input.required<string>();

  meta = input<string>();

  url = input<string>();

  state = input<AiRecordState>();

  kind = computed(() => aiEntityKind(this.entityType(), this.alarmRule()));
}
