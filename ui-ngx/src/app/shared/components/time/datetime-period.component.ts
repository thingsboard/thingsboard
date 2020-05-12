///
/// Copyright © 2016-2020 The Thingsboard Authors
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
import { FixedWindow } from '@shared/models/time/time.models';

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
  ]
})
export class DatetimePeriodComponent implements OnInit, ControlValueAccessor {

  @Input() disabled: boolean;

  modelValue: FixedWindow;

  startDate: Date;
  endDate: Date;

  endTime: any;

  maxStartDate: Date;
  minEndDate: Date;
  maxEndDate: Date;

  changePending = false;

  private propagateChange = null;

  constructor() {
  }

  ngOnInit(): void {
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
  }

  writeValue(datePeriod: FixedWindow): void {
    this.modelValue = datePeriod;
    if (this.modelValue) {
      this.startDate = new Date(this.modelValue.startTimeMs);
      this.endDate = new Date(this.modelValue.endTimeMs);
    } else {
      const date = new Date();
      this.startDate = new Date(
        date.getFullYear(),
        date.getMonth(),
        date.getDate() - 1,
        date.getHours(),
        date.getMinutes(),
        date.getSeconds(),
        date.getMilliseconds());
      this.endDate = date;
      this.updateView();
    }
    this.updateMinMaxDates();
  }

  updateView() {
    let value: FixedWindow = null;
    if (this.startDate && this.endDate) {
      value = {
        startTimeMs: this.startDate.getTime(),
        endTimeMs: this.endDate.getTime()
      };
    }
    this.modelValue = value;
    if (!this.propagateChange) {
      this.changePending = true;
    } else {
      this.propagateChange(this.modelValue);
    }
  }

  updateMinMaxDates() {
    this.maxStartDate = new Date(this.endDate.getTime() - 1000);
    this.minEndDate = new Date(this.startDate.getTime() + 1000);
    this.maxEndDate = new Date();
  }

  onStartDateChange() {
    if (this.startDate) {
      if (this.startDate.getTime() > this.maxStartDate.getTime()) {
        this.startDate = new Date(this.maxStartDate.getTime());
      }
      this.updateMinMaxDates();
    }
    this.updateView();
  }

  onEndDateChange() {
    if (this.endDate) {
      if (this.endDate.getTime() < this.minEndDate.getTime()) {
        this.endDate = new Date(this.minEndDate.getTime());
      } else if (this.endDate.getTime() > this.maxEndDate.getTime()) {
        this.endDate = new Date(this.maxEndDate.getTime());
      }
      this.updateMinMaxDates();
    }
    this.updateView();
  }

}
