// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

export interface AiCardAction {
  id: string;
  label: string;
  icon?: string;
  quiet?: boolean;
  compose?: string;
  pressed?: boolean;
  tip?: string;
}

@Component({
  selector: 'tb-ai-card-actions',
  template: `
    @for (action of actions(); track action.id) {
      <button type="button" class="ob-chip" [class.k-chip-quiet]="action.quiet" [class.k-chip-compose]="!!action.compose"
              [attr.aria-pressed]="action.pressed ?? null"
              [matTooltip]="action.tip ?? ''" [matTooltipDisabled]="!action.tip" matTooltipPosition="above"
              (click)="press(action)">
        @if (action.icon) {
          <mat-icon aria-hidden="true">{{ action.icon }}</mat-icon>
        }
        {{ action.label }}
      </button>
    }
  `,
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    'class': 'k-chips',
    'role': 'group'
  }
})
export class AiCardActionsComponent {

  actions = input.required<AiCardAction[]>();

  act = output<string>();

  compose = output<string>();

  press(action: AiCardAction): void {
    if (action.compose) {
      this.compose.emit(action.compose);
    } else {
      this.act.emit(action.id);
    }
  }
}
