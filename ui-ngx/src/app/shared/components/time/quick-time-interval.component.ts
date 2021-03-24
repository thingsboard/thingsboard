///
/// Copyright © 2016-2021 The Thingsboard Authors
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
import { QuickTimeInterval, QuickTimeIntervalTranslationMap } from '@shared/models/time/time.models';

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
export class QuickTimeIntervalComponent implements OnInit, ControlValueAccessor {

  private allIntervals = Object.values(QuickTimeInterval);

  modelValue: QuickTimeInterval;
  timeIntervalTranslationMap = QuickTimeIntervalTranslationMap;

  rendered = false;

  @Input() disabled: boolean;

  @Input() onlyCurrentInterval = false;

  private propagateChange = (_: any) => {};

  constructor() {
  }

  get intervals() {
    if (this.onlyCurrentInterval) {
      return this.allIntervals.filter(interval => interval.startsWith('CURRENT_'));
    }
    return this.allIntervals;
  }

  ngOnInit(): void {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  writeValue(interval: QuickTimeInterval): void {
    this.modelValue = interval;
  }

  onIntervalChange() {
    this.propagateChange(this.modelValue);
  }
}
