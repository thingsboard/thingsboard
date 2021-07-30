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

import { Component, forwardRef, Input, OnDestroy, OnInit } from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import {
  FullTimeUnit,
  HOUR,
  MINUTE,
  SECOND,
  TimeUnit,
  TimeUnitMilli,
  timeUnitTranslationMap
} from '@shared/models/time/time.models';
import { isDefinedAndNotNull, isNumber } from '@core/utils';

interface FormGroupModel {
  time: number;
  unit: FullTimeUnit;
}

@Component({
  selector: 'tb-time-unit-select',
  templateUrl: './time-unit-select.component.html',
  styleUrls: [],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => TimeUnitSelectComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => TimeUnitSelectComponent),
      multi: true
    }
  ]
})
export class TimeUnitSelectComponent implements OnInit, OnDestroy, ControlValueAccessor, Validator {

  timeUnitSelectFormGroup: FormGroup;

  timeUnits = Object.values({...TimeUnitMilli, ...TimeUnit}).filter(item => item !== TimeUnit.DAYS);
  timeUnitTranslations = timeUnitTranslationMap;

  private destroy$ = new Subject();

  private timeUnitToTimeMap = new Map<FullTimeUnit, number>(
    [
      [TimeUnitMilli.MILLISECONDS, 1],
      [TimeUnit.SECONDS, SECOND],
      [TimeUnit.MINUTES, MINUTE],
      [TimeUnit.HOURS, HOUR]
    ]
  );

  private timeToTimeUnitMap = new Map<number, FullTimeUnit>(
    [
      [SECOND, TimeUnitMilli.MILLISECONDS],
      [MINUTE, TimeUnit.SECONDS],
      [HOUR, TimeUnit.MINUTES]
    ]
  );

  @Input()
  disabled: boolean;

  @Input()
  labelText: string;

  @Input()
  requiredText: string;

  @Input()
  patternText: string;

  @Input()
  minTime = 0;

  @Input()
  minText: string;

  private propagateChange = (v: any) => {
  }

  constructor(private fb: FormBuilder) {
  }

  ngOnInit() {
    this.timeUnitSelectFormGroup = this.fb.group({
      time: [0, [Validators.required, Validators.min(this.minTime), Validators.pattern('[0-9]*')]],
      unit: [TimeUnitMilli.MILLISECONDS]
    });
    this.timeUnitSelectFormGroup.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value) => {
      this.updateModel(value);
    });
    this.timeUnitSelectFormGroup.get('unit').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((unit: FullTimeUnit) => {
      if (this.minTime > 0) {
        const unitTime = this.timeUnitToTimeMap.get(unit);
        const validationTime = Math.ceil(this.minTime / unitTime);
        this.timeUnitSelectFormGroup.get('time').setValidators([Validators.required, Validators.min(validationTime), Validators.pattern('[0-9]*')]);
        this.timeUnitSelectFormGroup.get('time').updateValueAndValidity({emitEvent: false});
      }
    });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  registerOnChange(fn: any) {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any) {
  }

  setDisabledState(isDisabled: boolean) {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.timeUnitSelectFormGroup.disable({emitEvent: false});
    } else {
      this.timeUnitSelectFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: number) {
    const formValue: FormGroupModel = {
      time: 0,
      unit: TimeUnitMilli.MILLISECONDS
    };
    if (isDefinedAndNotNull(value) && isNumber(value) && value >= 0) {
      formValue.unit = this.calculateTimeUnit(value);
      formValue.time = value / this.timeUnitToTimeMap.get(formValue.unit);
    }
    this.timeUnitSelectFormGroup.reset(formValue, {emitEvent: false});
    this.timeUnitSelectFormGroup.get('unit').updateValueAndValidity({onlySelf: true});
  }

  validate(): ValidationErrors | null {
    return this.timeUnitSelectFormGroup.valid ? null : {
      timeUnitSelect: false
    };
  }

  private updateModel(value: FormGroupModel) {
    const time = value.time * this.timeUnitToTimeMap.get(value.unit);
    this.propagateChange(time);
  }

  private calculateTimeUnit(value: number): FullTimeUnit {
    if (value === 0) {
      return TimeUnitMilli.MILLISECONDS;
    }
    const iterators = this.timeToTimeUnitMap[Symbol.iterator]();
    let iterator = iterators.next();
    while (!iterator.done) {
      if (!Number.isInteger(value / iterator.value[0])) {
        return iterator.value[1];
      }
      iterator = iterators.next();
    }
    return TimeUnit.HOURS;
  }
}
