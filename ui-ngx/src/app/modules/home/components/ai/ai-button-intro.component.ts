// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'tb-ai-button-intro',
  template: `
    <div class="tb-ai-intro" data-ai role="status" aria-labelledby="tb-ai-intro-title">
      <h3 class="tb-ai-intro-title" id="tb-ai-intro-title">{{ title }}</h3>
      <button mat-icon-button type="button" class="tb-ai-intro-x" [attr.aria-label]="'action.close' | translate" (click)="dismiss.emit()">
        <mat-icon fontSet="material-icons-round">close</mat-icon>
      </button>
      <p class="tb-ai-intro-body">
        {{ lead }}
        @if (keyboard) {
          {{ 'ai-assistant.intro.press' | translate }} <kbd>/</kbd> {{ 'ai-assistant.intro.on-any-page' | translate }}
        }
      </p>
      <div class="tb-ai-intro-actions">
        <button mat-button type="button" class="tb-ai-intro-skip" (click)="dismiss.emit()">{{ 'ai-assistant.intro.got-it' | translate }}</button>
        <button mat-flat-button type="button" class="tb-ai-intro-go" (click)="openChat.emit()">{{ 'ai-assistant.intro.open-chat' | translate }}</button>
      </div>
    </div>
  `,
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AiButtonIntroComponent {

  @Input() title: string;

  @Input() lead: string;

  @Input() keyboard = false;

  @Output() dismiss = new EventEmitter<void>();

  @Output() openChat = new EventEmitter<void>();
}
