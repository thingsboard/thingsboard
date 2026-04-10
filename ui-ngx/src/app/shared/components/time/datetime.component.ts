///
/// Copyright © 2016-2026 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { Component, forwardRef, Input, OnInit } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { FloatLabelType, MatFormFieldAppearance, SubscriptSizing } from '@angular/material/form-field';
import { coerceBoolean } from '@shared/decorators/coercion';

@Component({
    selector: 'tb-datetime',
    templateUrl: './datetime.component.html',
    styleUrls: ['./datetime.component.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DatetimeComponent),
            multi: true
        }
    ],
    standalone: false
})
export class DatetimeComponent implements OnInit, ControlValueAccessor {

  private requiredValue: boolean;
  get required(): boolean {
    return this.requiredValue;
  }
  @Input()
  set required(value: boolean) {
    this.requiredValue = coerceBooleanProperty(value);
  }

  @Input()
  floatLabel: FloatLabelType = 'auto';

  @Input()
  subscriptSizing: SubscriptSizing = 'fixed';

  @Input()
  appearance: MatFormFieldAppearance = 'fill';

  @Input()
  type: 'date' | 'time' | 'month' | 'year' | 'datetime' = 'datetime';

  @Input()
  disabled: boolean;

  @Input()
  dateText: string;

  @Input()
  @coerceBoolean()
  showLabel = true;

  @Input()
  @coerceBoolean()
  allowClear = false;

  @Input()
  fieldClass: string;

  minDateValue: Date | null;

  @Input()
  set minDate(minDate: number | null) {
    this.minDateValue = minDate ? new Date(minDate) : null;
  }

  maxDateValue: Date | null;

  @Input()
  set maxDate(maxDate: number | null) {
    this.maxDateValue = maxDate ? new Date(maxDate) : null;
  }

  modelValue: number;

  date: Date;

  private propagateChange = (v: any) => { };

  constructor() {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  ngOnInit(): void {
  }

  writeValue(datetime: number | null): void {
    this.modelValue = datetime;
    if (this.modelValue) {
      this.date = new Date(this.modelValue);
    } else {
      this.date = null;
    }
  }

  updateView(value: number | null) {
    if (this.modelValue !== value) {
      this.modelValue = value;
      this.propagateChange(this.modelValue);
    }
  }

  onDateChange() {
    const value = this.date ? this.date.getTime() : null;
    this.updateView(value);
  }

  clear($event: Event) {
    $event.stopPropagation();
    this.date = null;
    this.updateView(null);
  }

}
