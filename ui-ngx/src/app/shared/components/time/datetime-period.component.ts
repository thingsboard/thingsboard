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

import { Component, forwardRef, Input } from '@angular/core';
import { ControlValueAccessor, FormBuilder, NG_VALUE_ACCESSOR } from '@angular/forms';
import { DAY, FixedWindow, MINUTE } from '@shared/models/time/time.models';
import { MatFormFieldAppearance, SubscriptSizing } from '@angular/material/form-field';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { distinctUntilChanged } from 'rxjs/operators';

interface DateTimePeriod {
  startDate: Date;
  endDate: Date;
}

@Component({
    selector: 'tb-datetime-period',
    templateUrl: './datetime-period.component.html',
    styleUrls: ['./datetime-period.component.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DatetimePeriodComponent),
            multi: true
        }
    ],
    standalone: false
})
export class DatetimePeriodComponent implements ControlValueAccessor {

  @Input() disabled: boolean;

  @Input()
  subscriptSizing: SubscriptSizing = 'fixed';

  @Input()
  appearance: MatFormFieldAppearance = 'fill';

  private modelValue: FixedWindow;

  maxStartDate: Date;
  maxEndDate: Date;

  private maxStartDateTs: number;
  private minEndDateTs: number;
  private maxStartTs: number;
  private maxEndTs: number;

  private timeShiftMs = MINUTE;

  dateTimePeriodFormGroup = this.fb.group({
    startDate: this.fb.control<Date>(null),
    endDate: this.fb.control<Date>(null)
  });

  private changePending = false;

  private propagateChange = null;

  constructor(private fb: FormBuilder) {
    this.dateTimePeriodFormGroup.valueChanges.pipe(
      distinctUntilChanged((prevDateTimePeriod, dateTimePeriod) =>
        prevDateTimePeriod.startDate === dateTimePeriod.startDate && prevDateTimePeriod.endDate === dateTimePeriod.endDate),
      takeUntilDestroyed()
    ).subscribe((dateTimePeriod: DateTimePeriod) => {
      this.updateMinMaxDates(dateTimePeriod);
      this.updateView();
    });

    this.dateTimePeriodFormGroup.get('startDate').valueChanges.pipe(
      distinctUntilChanged(),
      takeUntilDestroyed()
    ).subscribe(startDate => this.onStartDateChange(startDate));
    this.dateTimePeriodFormGroup.get('endDate').valueChanges.pipe(
      distinctUntilChanged(),
      takeUntilDestroyed()
    ).subscribe(endDate => this.onEndDateChange(endDate));
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
    if (this.changePending && this.propagateChange) {
      this.changePending = false;
      this.propagateChange(this.modelValue);
    }
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.dateTimePeriodFormGroup.disable({emitEvent: false});
    } else {
      this.dateTimePeriodFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(datePeriod: FixedWindow): void {
    this.modelValue = datePeriod;
    if (this.modelValue) {
      this.dateTimePeriodFormGroup.patchValue({
        startDate: new Date(this.modelValue.startTimeMs),
        endDate: new Date(this.modelValue.endTimeMs)
      }, {emitEvent: false});
    } else {
      const date = new Date();
      this.dateTimePeriodFormGroup.patchValue({
        startDate: new Date(date.getTime() - DAY),
        endDate: date
      }, {emitEvent: false});
      this.updateView();
    }
    this.updateMinMaxDates(this.dateTimePeriodFormGroup.value);
  }

  private updateView() {
    let value: FixedWindow = null;
    const dateTimePeriod = this.dateTimePeriodFormGroup.value;
    if (dateTimePeriod.startDate && dateTimePeriod.endDate) {
      value = {
        startTimeMs: dateTimePeriod.startDate.getTime(),
        endTimeMs: dateTimePeriod.endDate.getTime()
      };
    }
    this.modelValue = value;
    if (!this.propagateChange) {
      this.changePending = true;
    } else {
      this.propagateChange(this.modelValue);
    }
  }

  private updateMinMaxDates(dateTimePeriod: Partial<DateTimePeriod>) {
    this.maxEndDate = new Date();
    this.maxEndTs = this.maxEndDate.getTime();
    this.maxStartTs = this.maxEndTs - this.timeShiftMs;
    this.maxStartDate = new Date(this.maxStartTs);

    if (dateTimePeriod.endDate) {
      this.maxStartDateTs = dateTimePeriod.endDate.getTime() - this.timeShiftMs;
    }
    if (dateTimePeriod.startDate) {
      this.minEndDateTs = dateTimePeriod.startDate.getTime() + this.timeShiftMs;
    }
  }

  private onStartDateChange(startDate: Date) {
    if (startDate) {
      let startDateTs = startDate.getTime();
      if (startDateTs > this.maxStartTs) {
        startDateTs = this.maxStartTs;
        this.dateTimePeriodFormGroup.get('startDate').patchValue(new Date(startDateTs), { emitEvent: false });
      }
      if (startDateTs > this.maxStartDateTs) {
        this.dateTimePeriodFormGroup.get('endDate').patchValue(new Date(startDateTs + this.timeShiftMs), { emitEvent: false });
      }
    }
  }

  private onEndDateChange(endDate: Date) {
    if (endDate) {
      let endDateTs = endDate.getTime();
      if (endDateTs > this.maxEndTs) {
        endDateTs = this.maxEndTs;
        this.dateTimePeriodFormGroup.get('endDate').patchValue(new Date(endDateTs), { emitEvent: false });
      }
      if (endDateTs < this.minEndDateTs) {
        this.dateTimePeriodFormGroup.get('startDate').patchValue(new Date(endDateTs - this.timeShiftMs), { emitEvent: false });
      }
    }
  }

}
