// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { aiEntityKind } from '@home/components/ai/ai-action.models';
import { AiAlarmState, AiHubMeta, AiRecordState } from '@home/components/ai/ai-record.models';

export { AiRecordState } from '@home/components/ai/ai-record.models';

@Component({
  selector: 'tb-ai-record',
  template: `
    <span class="tb-rec-tile"><mat-icon aria-hidden="true">{{ kind().icon }}</mat-icon></span>
    @if (url()) {
      <a class="tb-rec-name" [routerLink]="url()" [matTooltip]="name()" matTooltipPosition="above" [matTooltipShowDelay]="600">{{ name() }}</a>
    } @else {
      <span class="tb-rec-name" [matTooltip]="name()" matTooltipPosition="above" [matTooltipShowDelay]="600">{{ name() }}</span>
    }
    @if (hub(); as listing) {
      <span class="tb-rec-meta">
        <span class="tb-hub-ic" [matTooltip]="'ai-assistant.detail.on-iot-hub' | translate" matTooltipPosition="above"><mat-icon class="tb-mat-16" aria-hidden="true">hub</mat-icon></span>
        {{ listing.type }}@if (listing.creator) {{{ listing.type ? ' · ' : '' }}{{ listing.creator }}}@if (listing.verified) {<mat-icon class="k-ver" aria-hidden="true">verified</mat-icon>}@if (listing.installs) { · {{ listing.installs }}}
      </span>
    } @else if (meta()) {
      <span class="tb-rec-meta">{{ meta() }}</span>
    }
    @if (alarm(); as current) {
      <span [class]="'tb-state sev ' + (current.cleared ? 'cleared' : current.severity.toLowerCase())"
            [matTooltip]="current.tip" matTooltipPosition="above" [attr.aria-label]="current.tip">
        <tb-ai-sev-mark [severity]="current.severity"></tb-ai-sev-mark>{{ (current.cleared ? 'ai-assistant.record.cleared' : 'alarm.severity-' + current.severity.toLowerCase()) | translate }}
        @if (current.acknowledged) {
          <mat-icon class="tb-ack-ic" aria-hidden="true">done_all</mat-icon>
        }
      </span>
    } @else if (state(); as current) {
      <span [class]="'tb-state ' + current.tone"><i></i>{{ current.label }}</span>
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

  alarm = input<AiAlarmState>();

  hub = input<AiHubMeta>();

  kind = computed(() => aiEntityKind(this.entityType(), this.alarmRule()));
}
