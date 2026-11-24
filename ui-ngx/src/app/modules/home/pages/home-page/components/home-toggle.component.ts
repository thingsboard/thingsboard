// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

export interface HomeToggleOption<T> {
  value: T;
  label: string;
}

@Component({
  selector: 'tb-home-toggle',
  template: `
    <div class="tb-home-toggle" role="group" [attr.aria-label]="label()">
      @for (option of options(); track option.value) {
        <button type="button" class="tb-home-toggle-option" [attr.aria-pressed]="option.value === value()"
                (click)="option.value !== value() && valueChange.emit(option.value)">{{ option.label | translate }}</button>
      }
    </div>
  `,
  styleUrls: ['./home-toggle.component.scss'],
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class HomeToggleComponent<T = string> {

  options = input.required<HomeToggleOption<T>[]>();

  value = input.required<T>();

  label = input<string>();

  valueChange = output<T>();
}
