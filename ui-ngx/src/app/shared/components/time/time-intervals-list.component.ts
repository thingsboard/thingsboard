///
/// Copyright © 2016-2024 The Thingsboard Authors
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
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR } from '@angular/forms';
import { TimeService } from '@core/services/time.service';
import { MatFormFieldAppearance, SubscriptSizing } from '@angular/material/form-field';
import { coerceBoolean, coerceNumber } from '@shared/decorators/coercion';
import { Interval, TimeInterval } from '@shared/models/time/time.models';
import { isDefined } from '@core/utils';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'tb-time-intervals-list',
  templateUrl: './time-intervals-list.component.html',
  styleUrls: ['./time-intervals-list.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => TimeIntervalsListComponent),
      multi: true
    }
  ]
})
export class TimeIntervalsListComponent implements OnInit, ControlValueAccessor {

  @Input()
  @coerceNumber()
  min: number;

  @Input()
  @coerceNumber()
  max: number;

  @Input() predefinedName: string;

  @Input()
  @coerceBoolean()
  setAllIfEmpty = false;

  @Input()
  @coerceBoolean()
  returnEmptyIfAllSet = false;

  @Input()
  @coerceBoolean()
  useCalendarIntervals = false;

  @Input() disabled: boolean;

  @Input()
  subscriptSizing: SubscriptSizing = 'fixed';

  @Input()
  appearance: MatFormFieldAppearance = 'fill';

  allIntervals: Array<TimeInterval>;
  allIntervalValues: Array<Interval>;

  timeintervalFormGroup: FormGroup;

  private modelValue: Array<Interval>;
  private rendered = false;
  private propagateChangeValue: any;

  private propagateChange = (value: any) => {
    this.propagateChangeValue = value;
  };

  constructor(private timeService: TimeService,
              private fb: FormBuilder) {
    this.timeintervalFormGroup = this.fb.group({
      intervals: [ [] ]
    });
    this.timeintervalFormGroup.get('intervals').valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe(() => this.updateView());
  }

  ngOnInit(): void {
    this.updateIntervalsList();
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
    if (isDefined(this.propagateChangeValue)) {
      this.propagateChange(this.propagateChangeValue);
    }
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.timeintervalFormGroup.disable({emitEvent: false});
    } else {
      this.timeintervalFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(intervals: Array<Interval>): void {
    this.modelValue = intervals;
    this.rendered = true;
    this.setIntervals(this.modelValue);
  }

  private updateIntervalsList() {
    this.allIntervals = this.timeService.getIntervals(this.min, this.max, this.useCalendarIntervals);
    this.allIntervalValues = this.allIntervals.map(interval => interval.value);
  }

  private setIntervals(intervals: Array<Interval>) {
    this.timeintervalFormGroup.get('intervals').patchValue(
      (this.setAllIfEmpty && !intervals?.length) ? this.allIntervalValues : intervals,
      {emitEvent: false});
  }

  private updateView() {
    if (!this.rendered) {
      return;
    }
    let value: Array<Interval>;
    const intervals: Array<Interval> = this.timeintervalFormGroup.get('intervals').value;

    if (!this.returnEmptyIfAllSet || intervals.length < this.allIntervals.length) {
      value = intervals;
    } else {
      value = [];
    }
    this.modelValue = value;
    this.propagateChange(this.modelValue);
  }

}
