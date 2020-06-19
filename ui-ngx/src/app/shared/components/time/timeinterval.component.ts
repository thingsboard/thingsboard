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

import { Component, EventEmitter, forwardRef, Input, OnInit, Output } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { TimeInterval, TimeService } from '@core/services/time.service';
import { coerceBooleanProperty } from '@angular/cdk/coercion';

@Component({
  selector: 'tb-timeinterval',
  templateUrl: './timeinterval.component.html',
  styleUrls: ['./timeinterval.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => TimeintervalComponent),
      multi: true
    }
  ]
})
export class TimeintervalComponent implements OnInit, ControlValueAccessor {

  minValue: number;
  maxValue: number;

  @Input()
  set min(min: number) {
    if (typeof min !== 'undefined' && min !== this.minValue) {
      this.minValue = min;
      this.maxValue = Math.max(this.maxValue, this.minValue);
      this.updateView();
    }
  }

  @Input()
  set max(max: number) {
    if (typeof max !== 'undefined' && max !== this.maxValue) {
      this.maxValue = max;
      this.minValue = Math.min(this.minValue, this.maxValue);
      this.updateView();
    }
  }

  @Input() predefinedName: string;

  isEditValue = false;

  @Input()
  set isEdit(val) {
    this.isEditValue = coerceBooleanProperty(val);
  }

  get isEdit() {
    return this.isEditValue;
  }

  hideFlagValue = false;

  @Input()
  get hideFlag() {
    return this.hideFlagValue;
  }

  set hideFlag(val) {
    this.hideFlagValue = val;
  }

  @Output() hideFlagChange = new EventEmitter<boolean>();

  @Input() disabled: boolean;

  days = 0;
  hours = 0;
  mins = 1;
  secs = 0;

  intervalMs = 0;
  modelValue: number;

  advanced = false;
  rendered = false;

  intervals: Array<TimeInterval>;

  private propagateChange = (_: any) => {};

  constructor(private timeService: TimeService) {
  }

  ngOnInit(): void {
    this.boundInterval();
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  writeValue(intervalMs: number): void {
    this.modelValue = intervalMs;
    this.rendered = true;
    if (typeof this.modelValue !== 'undefined') {
      const min = this.timeService.boundMinInterval(this.minValue);
      const max = this.timeService.boundMaxInterval(this.maxValue);
      if (this.modelValue >= min && this.modelValue <= max) {
        this.advanced = !this.timeService.matchesExistingInterval(this.minValue, this.maxValue, this.modelValue);
        this.setIntervalMs(this.modelValue);
      } else {
        this.boundInterval();
      }
    }
  }

  setIntervalMs(intervalMs: number) {
    if (!this.advanced) {
      this.intervalMs = intervalMs;
    }
    const intervalSeconds = Math.floor(intervalMs / 1000);
    this.days = Math.floor(intervalSeconds / 86400);
    this.hours = Math.floor((intervalSeconds % 86400) / 3600);
    this.mins = Math.floor(((intervalSeconds % 86400) % 3600) / 60);
    this.secs = intervalSeconds % 60;
  }

  boundInterval() {
    const min = this.timeService.boundMinInterval(this.minValue);
    const max = this.timeService.boundMaxInterval(this.maxValue);
    this.intervals = this.timeService.getIntervals(this.minValue, this.maxValue);
    if (this.rendered) {
      let newIntervalMs = this.modelValue;
      if (newIntervalMs < min) {
        newIntervalMs = min;
      } else if (newIntervalMs > max) {
        newIntervalMs = max;
      }
      if (!this.advanced) {
        newIntervalMs = this.timeService.boundToPredefinedInterval(min, max, newIntervalMs);
      }
      if (newIntervalMs !== this.modelValue) {
        this.setIntervalMs(newIntervalMs);
        this.updateView();
      }
    }
  }

  updateView() {
    if (!this.rendered) {
      return;
    }
    let value = null;
    let intervalMs;
    if (!this.advanced) {
      intervalMs = this.intervalMs;
      if (!intervalMs || isNaN(intervalMs)) {
        intervalMs = this.calculateIntervalMs();
      }
    } else {
      intervalMs = this.calculateIntervalMs();
    }
    if (!isNaN(intervalMs) && intervalMs > 0) {
      value = intervalMs;
    }
    this.modelValue = value;
    this.propagateChange(this.modelValue);
    this.boundInterval();
  }

  calculateIntervalMs(): number {
    return (this.days * 86400 +
      this.hours * 3600 +
      this.mins * 60 +
      this.secs) * 1000;
  }

  onIntervalMsChange() {
    this.updateView();
  }

  onAdvancedChange() {
    if (!this.advanced) {
      this.intervalMs = this.calculateIntervalMs();
    } else {
      let intervalMs = this.intervalMs;
      if (!intervalMs || isNaN(intervalMs)) {
        intervalMs = this.calculateIntervalMs();
      }
      this.setIntervalMs(intervalMs);
    }
    this.updateView();
  }

  onHideFlagChange() {
    this.hideFlagChange.emit(this.hideFlagValue);
  }

  onTimeInputChange(type: string) {
    switch (type) {
      case 'secs':
        setTimeout(() => this.onSecsChange(), 0);
        break;
      case 'mins':
        setTimeout(() => this.onMinsChange(), 0);
        break;
      case 'hours':
        setTimeout(() => this.onHoursChange(), 0);
        break;
      case 'days':
        setTimeout(() => this.onDaysChange(), 0);
        break;
    }
  }

  onSecsChange() {
    if (typeof this.secs === 'undefined') {
      return;
    }
    if (this.secs < 0) {
      if ((this.days + this.hours + this.mins) > 0) {
        this.secs = this.secs + 60;
        this.mins--;
        this.onMinsChange();
      } else {
        this.secs = 0;
      }
    } else if (this.secs >= 60) {
      this.secs = this.secs - 60;
      this.mins++;
      this.onMinsChange();
    }
    this.updateView();
  }

  onMinsChange() {
    if (typeof this.mins === 'undefined') {
      return;
    }
    if (this.mins < 0) {
      if ((this.days + this.hours) > 0) {
        this.mins = this.mins + 60;
        this.hours--;
        this.onHoursChange();
      } else {
        this.mins = 0;
      }
    } else if (this.mins >= 60) {
      this.mins = this.mins - 60;
      this.hours++;
      this.onHoursChange();
    }
    this.updateView();
  }

  onHoursChange() {
    if (typeof this.hours === 'undefined') {
      return;
    }
    if (this.hours < 0) {
      if (this.days > 0) {
        this.hours = this.hours + 24;
        this.days--;
        this.onDaysChange();
      } else {
        this.hours = 0;
      }
    } else if (this.hours >= 24) {
      this.hours = this.hours - 24;
      this.days++;
      this.onDaysChange();
    }
    this.updateView();
  }

  onDaysChange() {
    if (typeof this.days === 'undefined') {
      return;
    }
    if (this.days < 0) {
      this.days = 0;
    }
    this.updateView();
  }

}
