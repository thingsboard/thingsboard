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

import { Component, forwardRef, Input, OnChanges, OnInit, SimpleChanges } from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR } from '@angular/forms';
import { QuickTimeInterval, QuickTimeIntervalTranslationMap } from '@shared/models/time/time.models';
import { MatFormFieldAppearance, SubscriptSizing } from '@angular/material/form-field';
import { coerceBoolean } from '@shared/decorators/coercion';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { isEqual } from '@core/utils';

@Component({
  selector: 'tb-quick-time-interval',
  templateUrl: './quick-time-interval.component.html',
  styleUrls: ['./quick-time-interval.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => QuickTimeIntervalComponent),
      multi: true
    }
  ]
})
export class QuickTimeIntervalComponent implements OnInit, ControlValueAccessor, OnChanges {

  private allIntervals = Object.values(QuickTimeInterval) as QuickTimeInterval[];

  modelValue: QuickTimeInterval;
  timeIntervalTranslationMap = QuickTimeIntervalTranslationMap;

  rendered = false;

  @Input()
  @coerceBoolean()
  displayLabel = true;

  @Input() disabled: boolean;

  @Input() onlyCurrentInterval = false;

  @Input()
  allowedIntervals: Array<QuickTimeInterval>

  @Input()
  subscriptSizing: SubscriptSizing = 'fixed';

  @Input()
  appearance: MatFormFieldAppearance = 'fill';

  intervals: Array<QuickTimeInterval>;

  private allAvailableIntervals: Array<QuickTimeInterval>;

  quickIntervalFormGroup: FormGroup;

  private propagateChange = (_: any) => {};

  constructor(private fb: FormBuilder) {
    this.quickIntervalFormGroup = this.fb.group({
      interval: [ null ]
    });
    this.quickIntervalFormGroup.get('interval').valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe((value) => {
      let modelValue;
      if (!value) {
        modelValue = null;
      } else {
        modelValue = value;
      }
      this.updateView(modelValue);
    });
  }

  ngOnInit(): void {
    this.allAvailableIntervals = this.getAllAvailableIntervals();
    this.intervals = this.allowedIntervals?.length ? this.allowedIntervals : this.allAvailableIntervals;
  }

  ngOnChanges({allowedIntervals}: SimpleChanges): void {
    if (allowedIntervals && !allowedIntervals.firstChange && !isEqual(allowedIntervals.currentValue, allowedIntervals.previousValue)) {
      this.intervals = this.allowedIntervals?.length ? this.allowedIntervals : this.allAvailableIntervals;
      const currentInterval: QuickTimeInterval = this.quickIntervalFormGroup.get('interval').value;
      if (currentInterval && !this.intervals.includes(currentInterval)) {
        this.quickIntervalFormGroup.get('interval').patchValue(this.intervals[0], {emitEvent: true});
      }
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.quickIntervalFormGroup.disable({emitEvent: false});
    } else {
      this.quickIntervalFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: QuickTimeInterval): void {
    let interval: QuickTimeInterval;
    if (value && this.allowedIntervals?.length && !this.allowedIntervals.includes(value)) {
      interval = this.allowedIntervals[0];
    } else {
      interval = value;
    }
    this.modelValue = interval;
    this.quickIntervalFormGroup.get('interval').patchValue(interval, {emitEvent: false});
  }

  updateView(value: QuickTimeInterval | null) {
    if (this.modelValue !== value) {
      this.modelValue = value;
      this.propagateChange(this.modelValue);
    }
  }

  private getAllAvailableIntervals() {
    if (this.onlyCurrentInterval) {
      return this.allIntervals.filter(interval => interval.startsWith('CURRENT_'));
    }
    return this.allIntervals;
  }
}
