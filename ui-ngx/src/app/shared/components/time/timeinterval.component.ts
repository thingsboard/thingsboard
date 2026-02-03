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

import {
  booleanAttribute,
  Component,
  forwardRef,
  Input,
  OnChanges,
  OnDestroy,
  OnInit,
  SimpleChanges
} from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR } from '@angular/forms';
import { TimeService } from '@core/services/time.service';
import { coerceNumberProperty } from '@angular/cdk/coercion';
import { MatFormFieldAppearance, SubscriptSizing } from '@angular/material/form-field';
import { coerceBoolean } from '@shared/decorators/coercion';
import { Interval, IntervalMath, intervalValuesToTimeIntervals, TimeInterval } from '@shared/models/time/time.models';
import { isDefined, isEqual } from '@core/utils';
import { IntervalType } from '@shared/models/telemetry/telemetry.models';
import { takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs';

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
    ],
    standalone: false
})
export class TimeintervalComponent implements OnInit, ControlValueAccessor, OnChanges, OnDestroy {

  minValue: number;
  maxValue: number;

  @Input()
  set min(min: number) {
    const minValueData = coerceNumberProperty(min);
    if (typeof minValueData !== 'undefined' && minValueData !== this.minValue) {
      this.minValue = minValueData;
      this.maxValue = Math.max(this.maxValue, this.minValue);
      this.updateView();
    }
  }

  @Input()
  set max(max: number) {
    const maxValueData = coerceNumberProperty(max);
    if (typeof maxValueData !== 'undefined' && maxValueData !== this.maxValue) {
      this.maxValue = maxValueData;
      this.minValue = Math.min(this.minValue, this.maxValue);
      this.updateView(true);
    }
  }

  @Input() predefinedName: string;

  @Input({transform : booleanAttribute})
  disabledAdvanced = false;

  @Input()
  allowedIntervals: Array<Interval>;

  @Input()
  @coerceBoolean()
  useCalendarIntervals = false;

  @Input() disabled: boolean;

  @Input()
  subscriptSizing: SubscriptSizing = 'fixed';

  @Input()
  appearance: MatFormFieldAppearance = 'fill';

  intervals: Array<TimeInterval>;

  advanced = false;

  timeintervalFormGroup: FormGroup;

  customTimeInterval: TimeInterval = {
    name: 'timeinterval.custom',
    translateParams: {},
    value: IntervalType.CUSTOM
  };

  private modelValue: Interval;
  private rendered = false;
  private propagateChangeValue: any;

  private propagateChange = (value: any) => {
    this.propagateChangeValue = value;
  };

  private destroy$ = new Subject<void>();

  constructor(private timeService: TimeService,
              private fb: FormBuilder) {
    this.timeintervalFormGroup = this.fb.group({
      interval: [ 1 ],
      customInterval: this.fb.group({
        days: [ 0 ],
        hours: [ 0 ],
        mins: [ 1 ],
        secs: [ 0 ]
      })
    });

    this.timeintervalFormGroup.get('interval').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => this.onIntervalChange());

    this.timeintervalFormGroup.get('customInterval').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => this.updateView());

    this.timeintervalFormGroup.get('customInterval.secs').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((secs) => this.onSecsChange(secs));

    this.timeintervalFormGroup.get('customInterval.mins').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((mins) => this.onMinsChange(mins));

    this.timeintervalFormGroup.get('customInterval.hours').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((hours) => this.onHoursChange(hours));

    this.timeintervalFormGroup.get('customInterval.days').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((days) => this.onDaysChange(days));
  }

  ngOnInit(): void {
    this.boundInterval();
  }

  ngOnChanges({disabledAdvanced, allowedIntervals}: SimpleChanges): void {
    if ((disabledAdvanced && !disabledAdvanced.firstChange && disabledAdvanced.currentValue !== disabledAdvanced.previousValue) ||
        (allowedIntervals && !allowedIntervals.firstChange && !isEqual(allowedIntervals.currentValue, allowedIntervals.previousValue))) {
      this.updateIntervalValue(true);
    }
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

  writeValue(interval: Interval): void {
    this.modelValue = interval;
    this.rendered = true;
    this.updateIntervalValue();
  }

  private updateIntervalValue(forceBoundInterval = false) {
    if (typeof this.modelValue !== 'undefined') {
      const min = this.timeService.boundMinInterval(this.minValue);
      const max = this.timeService.boundMaxInterval(this.maxValue);
      if (IntervalMath.numberValue(this.modelValue) >= min && IntervalMath.numberValue(this.modelValue) <= max) {
        const advanced = this.allowedIntervals?.length
          ? !this.allowedIntervals.includes(this.modelValue)
          : !this.timeService.matchesExistingInterval(this.minValue, this.maxValue, this.modelValue,
          this.useCalendarIntervals);
        if (advanced && this.disabledAdvanced) {
          this.advanced = false;
          this.boundInterval();
        } else {
          this.advanced = advanced;
          this.setInterval(this.modelValue);
          if (forceBoundInterval) {
            this.boundInterval();
          }
        }
      } else {
        this.boundInterval();
      }
    }
  }

  private setInterval(interval: Interval) {
    if (!this.advanced) {
      this.timeintervalFormGroup.get('interval').patchValue(interval, {emitEvent: false});
    } else {
      this.timeintervalFormGroup.get('interval').patchValue(IntervalType.CUSTOM, {emitEvent: false});
      this.setCustomInterval(interval);
    }
  }

  private setCustomInterval(interval: Interval) {
    const intervalSeconds = Math.floor(IntervalMath.numberValue(interval) / 1000);
    this.timeintervalFormGroup.get('customInterval').patchValue({
      days: Math.floor(intervalSeconds / 86400),
      hours: Math.floor((intervalSeconds % 86400) / 3600),
      mins: Math.floor(((intervalSeconds % 86400) % 3600) / 60),
      secs: intervalSeconds % 60
    }, {emitEvent: false});
  }

  private boundInterval(updateToPreferred = false) {
    const min = this.timeService.boundMinInterval(this.minValue);
    const max = this.timeService.boundMaxInterval(this.maxValue);
    this.intervals = this.allowedIntervals?.length
      ? intervalValuesToTimeIntervals(this.allowedIntervals)
      : this.timeService.getIntervals(this.minValue, this.maxValue, this.useCalendarIntervals);
    if (!this.disabledAdvanced) {
      this.intervals.push(this.customTimeInterval);
    }
    if (this.rendered) {
      let newInterval = this.modelValue;
      if (this.allowedIntervals?.length) {
        if (!this.allowedIntervals.includes(newInterval) && !this.advanced) {
          newInterval = this.allowedIntervals[0];
        }
      } else {
        const newIntervalMs = IntervalMath.numberValue(newInterval);
        if (newIntervalMs < min) {
          newInterval = min;
        } else if (newIntervalMs >= max && updateToPreferred) {
          newInterval = this.timeService.boundMaxInterval(max / 7);
        }
        if (!this.advanced) {
          newInterval = this.timeService.boundToPredefinedInterval(min, max, newInterval, this.useCalendarIntervals);
        }
      }
      if (newInterval !== this.modelValue) {
        this.setInterval(newInterval);
        this.updateView();
      }
    }
  }

  private updateView(updateToPreferred = false) {
    if (!this.rendered) {
      return;
    }
    let value: Interval = null;
    let interval: Interval;
    if (!this.advanced) {
      interval = this.timeintervalFormGroup.get('interval').value;
      if (!interval || typeof interval === 'number' && isNaN(interval)) {
        interval = this.calculateIntervalMs();
      }
    } else {
      interval = this.calculateIntervalMs();
    }
    if (typeof interval === 'string' || !isNaN(interval) && interval > 0) {
      value = interval;
    }
    this.modelValue = value;
    this.propagateChange(this.modelValue);
    this.boundInterval(updateToPreferred);
  }

  private calculateIntervalMs(): number {
    const customInterval = this.timeintervalFormGroup.get('customInterval').value;
    return (customInterval.days * 86400 +
      customInterval.hours * 3600 +
      customInterval.mins * 60 +
      customInterval.secs) * 1000;
  }

  onIntervalChange() {
    const customIntervalSelected = this.timeintervalFormGroup.get('interval').value === IntervalType.CUSTOM;
    if (customIntervalSelected !== this.advanced) {
      this.advanced = customIntervalSelected;
      if (this.advanced) {
        this.setCustomInterval(this.modelValue);
      }
    }
    this.updateView();
  }

  private onSecsChange(secs: number) {
    const customInterval = this.timeintervalFormGroup.get('customInterval').value;
    if (typeof secs === 'undefined') {
      return;
    }
    if (secs < 0) {
      if ((customInterval.days + customInterval.hours + customInterval.mins) > 0) {
        this.timeintervalFormGroup.get('customInterval.secs').patchValue(secs + 60, {emitEvent: false});
        this.timeintervalFormGroup.get('customInterval.mins').patchValue(customInterval.mins - 1, {emitEvent: true});
      } else {
        this.timeintervalFormGroup.get('customInterval.secs').patchValue(0, {emitEvent: false});
      }
    } else if (secs >= 60) {
      this.timeintervalFormGroup.get('customInterval.secs').patchValue(secs - 60, {emitEvent: false});
      this.timeintervalFormGroup.get('customInterval.mins').patchValue(customInterval.mins + 1, {emitEvent: true});
    }
  }

  private onMinsChange(mins: number) {
    const customInterval = this.timeintervalFormGroup.get('customInterval').value;
    if (typeof mins === 'undefined') {
      return;
    }
    if (mins < 0) {
      if ((customInterval.days + customInterval.hours) > 0) {
        this.timeintervalFormGroup.get('customInterval.mins').patchValue(mins + 60, {emitEvent: false});
        this.timeintervalFormGroup.get('customInterval.hours').patchValue(customInterval.hours - 1, {emitEvent: true});
      } else {
        this.timeintervalFormGroup.get('customInterval.mins').patchValue(0, {emitEvent: false});
      }
    } else if (mins >= 60) {
      this.timeintervalFormGroup.get('customInterval.mins').patchValue(mins - 60, {emitEvent: false});
      this.timeintervalFormGroup.get('customInterval.hours').patchValue(customInterval.hours + 1, {emitEvent: true});
    }
  }

  private onHoursChange(hours: number) {
    const customInterval = this.timeintervalFormGroup.get('customInterval').value;
    if (typeof hours === 'undefined') {
      return;
    }
    if (hours < 0) {
      if (customInterval.days > 0) {
        this.timeintervalFormGroup.get('customInterval.hours').patchValue(hours + 24, {emitEvent: false});
        this.timeintervalFormGroup.get('customInterval.days').patchValue(customInterval.days - 1, {emitEvent: true});
      } else {
        this.timeintervalFormGroup.get('customInterval.hours').patchValue(0, {emitEvent: false});
      }
    } else if (hours >= 24) {
      this.timeintervalFormGroup.get('customInterval.hours').patchValue(hours - 24, {emitEvent: false});
      this.timeintervalFormGroup.get('customInterval.days').patchValue(customInterval.days + 1, {emitEvent: true});
    }
  }

  private onDaysChange(days: number) {
    if (typeof days === 'undefined') {
      return;
    }
    if (days < 0) {
      this.timeintervalFormGroup.get('customInterval.days').patchValue(0, {emitEvent: false});
    }
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

}
