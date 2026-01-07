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
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR } from '@angular/forms';
import { TimeService } from '@core/services/time.service';
import { MatFormFieldAppearance, SubscriptSizing } from '@angular/material/form-field';
import { coerceBoolean, coerceNumber } from '@shared/decorators/coercion';
import {
  Interval,
  intervalValuesToTimeIntervals,
  TimeInterval,
  TimewindowAggIntervalOptions
} from '@shared/models/time/time.models';
import { isDefined } from '@core/utils';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'tb-grouping-interval-options',
  templateUrl: './grouping-interval-options.component.html',
  styleUrls: ['./grouping-interval-options.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => GroupingIntervalOptionsComponent),
      multi: true
    }
  ]
})
export class GroupingIntervalOptionsComponent implements OnInit, ControlValueAccessor {

  @Input()
  @coerceNumber()
  min: number;

  @Input()
  @coerceNumber()
  max: number;

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
  selectedIntervals: Array<TimeInterval>;

  timeintervalFormGroup: FormGroup;

  private modelValue: TimewindowAggIntervalOptions;
  private rendered = false;
  private propagateChangeValue: any;

  private propagateChange = (value: any) => {
    this.propagateChangeValue = value;
  };

  constructor(private timeService: TimeService,
              private fb: FormBuilder) {
    this.timeintervalFormGroup = this.fb.group({
      aggIntervals: [ [] ],
      defaultAggInterval: [ null ],
    });
    this.timeintervalFormGroup.get('aggIntervals').valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe(selectedIntervalValues => this.setSelectedIntervals(selectedIntervalValues));
    this.timeintervalFormGroup.valueChanges.pipe(
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

  writeValue(intervalOptions: TimewindowAggIntervalOptions): void {
    this.modelValue = intervalOptions;
    this.rendered = true;
    this.timeintervalFormGroup.get('defaultAggInterval').patchValue(intervalOptions.defaultAggInterval, { emitEvent: false });
    this.setIntervals(intervalOptions.aggIntervals);
  }

  private updateIntervalsList() {
    this.allIntervals = this.timeService.getIntervals(this.min, this.max, this.useCalendarIntervals);
    this.allIntervalValues = this.allIntervals.map(interval => interval.value);
  }

  private setIntervals(intervals: Array<Interval>) {
    const selectedIntervals = !intervals?.length ? this.allIntervalValues : intervals;
    this.timeintervalFormGroup.get('aggIntervals').patchValue(
      selectedIntervals,
      {emitEvent: false});
    this.setSelectedIntervals(selectedIntervals);
  }

  private setSelectedIntervals(selectedIntervalValues: Array<Interval>) {
    if (!selectedIntervalValues.length || selectedIntervalValues.length === this.allIntervalValues.length) {
      this.selectedIntervals = this.allIntervals;
    } else {
      this.selectedIntervals = intervalValuesToTimeIntervals(selectedIntervalValues);
    }
    const defaultInterval: Interval = this.timeintervalFormGroup.get('defaultAggInterval').value;
    if (defaultInterval && !selectedIntervalValues.includes(defaultInterval)) {
      this.timeintervalFormGroup.get('defaultAggInterval').patchValue(null);
    }
  }

  private updateView() {
    if (!this.rendered) {
      return;
    }
    let selectedIntervals: Array<Interval>;
    const intervals: Array<Interval> = this.timeintervalFormGroup.get('aggIntervals').value;

    if (intervals.length < this.allIntervals.length) {
      selectedIntervals = intervals;
    } else {
      selectedIntervals = [];
    }
    this.modelValue = {
      aggIntervals: selectedIntervals,
      defaultAggInterval: this.timeintervalFormGroup.get('defaultAggInterval').value
    };
    this.propagateChange(this.modelValue);
  }

}
