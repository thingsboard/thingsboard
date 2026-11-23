// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output, ViewEncapsulation } from '@angular/core';

@Component({
  selector: 'tb-tour-popup',
  template: `
    <div class="tb-tour-popup" [class.hint]="hint" data-ai [attr.role]="hint ? 'status' : 'dialog'" [attr.aria-labelledby]="titleId">
      <h2 class="tb-tour-popup-title" [id]="titleId">
        @if (hint) {
          <span class="tb-ai-mark tb-tour-popup-mark" aria-hidden="true"><mat-icon fontSet="material-icons-round">auto_awesome</mat-icon></span>
        }
        {{ title }}
      </h2>
      @if (closable) {
        <button mat-icon-button type="button" class="tb-tour-popup-x" [attr.aria-label]="'action.close' | translate" (click)="closed.emit()">
          <mat-icon fontSet="material-icons-round">close</mat-icon>
        </button>
      }
      <p class="tb-tour-popup-text" [innerHTML]="text"></p>
      <div class="tb-tour-popup-actions">
        @if (count) {
          <span class="tb-tour-popup-count">{{ count }}</span>
        } @else if (foot) {
          <span class="tb-tour-popup-count tb-tour-popup-key" [innerHTML]="foot"></span>
        }
        @if (skipLabel) {
          <button mat-button type="button" class="tb-tour-popup-skip" (click)="skip.emit()">{{ skipLabel }}</button>
        }
        <button mat-flat-button type="button" class="tb-tour-popup-go" (click)="next.emit()">{{ nextLabel }}</button>
      </div>
    </div>
  `,
  styleUrls: ['./tour-popup.component.scss'],
  encapsulation: ViewEncapsulation.None,
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class TourPopupComponent {

  private static count = 0;

  readonly titleId = 'tb-tour-popup-title-' + (++TourPopupComponent.count);

  @Input() title: string;

  @Input() text: string;

  @Input() count: string | null = null;

  @Input() skipLabel: string | null = null;

  @Input() nextLabel: string;

  @Input() closable = true;

  @Input() hint = false;

  @Input() foot: string | null = null;

  @Output() next = new EventEmitter<void>();

  @Output() skip = new EventEmitter<void>();

  @Output() closed = new EventEmitter<void>();
}
