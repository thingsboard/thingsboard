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

import { Component, DestroyRef, forwardRef, Input, OnInit } from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  UntypedFormArray,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import {
  AlarmSchedule,
  AlarmScheduleType,
  AlarmScheduleTypeTranslationMap,
  dayOfWeekTranslations,
  getAlarmScheduleRangeText,
  timeOfDayToUTCTimestamp,
  utcTimestampToTimeOfDay
} from '@shared/models/device.models';
import { isDefined, isDefinedAndNotNull } from '@core/utils';
import { MatCheckboxChange } from '@angular/material/checkbox';
import { getDefaultTimezone } from '@shared/models/time/time.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'tb-alarm-schedule',
  templateUrl: './alarm-schedule.component.html',
  styleUrls: ['./alarm-schedule.component.scss'],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => AlarmScheduleComponent),
    multi: true
  }, {
    provide: NG_VALIDATORS,
    useExisting: forwardRef(() => AlarmScheduleComponent),
    multi: true
  }]
})
export class AlarmScheduleComponent implements ControlValueAccessor, Validator, OnInit {
  @Input()
  disabled: boolean;

  alarmScheduleForm: UntypedFormGroup;

  alarmScheduleTypes = Object.keys(AlarmScheduleType);
  alarmScheduleType = AlarmScheduleType;
  alarmScheduleTypeTranslate = AlarmScheduleTypeTranslationMap;
  dayOfWeekTranslationsArray = dayOfWeekTranslations;

  allDays = Array(7).fill(0).map((x, i) => i);

  firstRowDays = Array(4).fill(0).map((x, i) => i);
  secondRowDays = Array(3).fill(0).map((x, i) => i + 4);

  private modelValue: AlarmSchedule;

  private defaultItems = Array.from({length: 7}, (value, i) => ({
    enabled: true,
    dayOfWeek: i + 1
  }));

  private propagateChange = (v: any) => { };

  constructor(private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
  }

  ngOnInit(): void {
    this.alarmScheduleForm = this.fb.group({
      type: [AlarmScheduleType.ANY_TIME, Validators.required],
      timezone: [null, Validators.required],
      daysOfWeek: this.fb.array(new Array(7).fill(false), this.validateDayOfWeeks),
      startsOn: [0, Validators.required],
      endsOn: [0, Validators.required],
      items: this.fb.array(Array.from({length: 7}, (value, i) => this.defaultItemsScheduler(i)), this.validateItems),
      dynamicValue: [null]
    });

    this.alarmScheduleForm.get('type').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((type) => {
      const defaultTimezone = getDefaultTimezone();
      this.alarmScheduleForm.reset({type, items: this.defaultItems, timezone: defaultTimezone}, {emitEvent: false});
      this.updateValidators(type, true);
      this.alarmScheduleForm.updateValueAndValidity();
    });
    this.alarmScheduleForm.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateModel();
    });
  }

  validateDayOfWeeks(control: AbstractControl): ValidationErrors | null {
    const dayOfWeeks: boolean[] = control.value;
    if (!dayOfWeeks || !dayOfWeeks.length || !dayOfWeeks.find(v => v === true)) {
      return {
        dayOfWeeks: true
      };
    }
    return null;
  }

  validateItems(control: AbstractControl): ValidationErrors | null {
    const items: any[] = control.value;
    if (!items || !items.length || !items.find(v => v.enabled === true)) {
      return {
        dayOfWeeks: true
      };
    }
    return null;
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.alarmScheduleForm.disable({emitEvent: false});
    } else {
      this.updateValidators(this.alarmScheduleForm.get('type').value);
    }
  }

  writeValue(value: AlarmSchedule): void {
    this.modelValue = value;
    if (!isDefinedAndNotNull(this.modelValue)) {
      this.modelValue = {
        type: AlarmScheduleType.ANY_TIME
      };
    }
    switch (this.modelValue.type) {
      case AlarmScheduleType.SPECIFIC_TIME:
        let daysOfWeek = new Array(7).fill(false);
        if (isDefined(this.modelValue.daysOfWeek)) {
          daysOfWeek = daysOfWeek.map((item, index) => this.modelValue.daysOfWeek.indexOf(index + 1) > -1);
        }
        this.alarmScheduleForm.patchValue({
          type: this.modelValue.type,
          timezone: this.modelValue.timezone,
          daysOfWeek,
          startsOn: utcTimestampToTimeOfDay(this.modelValue.startsOn),
          endsOn: utcTimestampToTimeOfDay(this.modelValue.endsOn),
          dynamicValue: this.modelValue.dynamicValue
        }, {emitEvent: false});
        break;
      case AlarmScheduleType.CUSTOM:
        if (this.modelValue.items) {
          const alarmDays = [];
          this.modelValue.items
            .sort((a, b) => a.dayOfWeek - b.dayOfWeek)
            .forEach((item, index) => {
              this.disabledSelectedTime(item.enabled, index);
              alarmDays.push({
                enabled: item.enabled,
                startsOn: utcTimestampToTimeOfDay(item.startsOn),
                endsOn: utcTimestampToTimeOfDay(item.endsOn)
              });
            });
          this.alarmScheduleForm.patchValue({
            type: this.modelValue.type,
            timezone: this.modelValue.timezone,
            items: alarmDays,
            dynamicValue: this.modelValue.dynamicValue
          }, {emitEvent: false});
        }
        break;
      default:
        this.alarmScheduleForm.patchValue(this.modelValue || undefined, {emitEvent: false});
    }
    this.updateValidators(this.modelValue.type);
  }

  validate(control: UntypedFormGroup): ValidationErrors | null {
    return this.alarmScheduleForm.valid ? null : {
      alarmScheduler: {
        valid: false
      }
    };
  }

  weeklyRepeatControl(index: number): UntypedFormControl {
    return (this.alarmScheduleForm.get('daysOfWeek') as UntypedFormArray).at(index) as UntypedFormControl;
  }

  private updateValidators(type: AlarmScheduleType, changedType = false){
    switch (type){
      case AlarmScheduleType.ANY_TIME:
        this.alarmScheduleForm.get('timezone').disable({emitEvent: false});
        this.alarmScheduleForm.get('daysOfWeek').disable({emitEvent: false});
        this.alarmScheduleForm.get('startsOn').disable({emitEvent: false});
        this.alarmScheduleForm.get('endsOn').disable({emitEvent: false});
        this.alarmScheduleForm.get('items').disable({emitEvent: false});
        break;
      case AlarmScheduleType.SPECIFIC_TIME:
        this.alarmScheduleForm.get('timezone').enable({emitEvent: false});
        this.alarmScheduleForm.get('daysOfWeek').enable({emitEvent: false});
        this.alarmScheduleForm.get('startsOn').enable({emitEvent: false});
        this.alarmScheduleForm.get('endsOn').enable({emitEvent: false});
        this.alarmScheduleForm.get('items').disable({emitEvent: false});
        break;
      case AlarmScheduleType.CUSTOM:
        this.alarmScheduleForm.get('timezone').enable({emitEvent: false});
        this.alarmScheduleForm.get('daysOfWeek').disable({emitEvent: false});
        this.alarmScheduleForm.get('startsOn').disable({emitEvent: false});
        this.alarmScheduleForm.get('endsOn').disable({emitEvent: false});
        if (changedType) {
          this.alarmScheduleForm.get('items').enable({emitEvent: false});
        }
        break;
    }
  }

  private updateModel() {
    const value = this.alarmScheduleForm.value;
    if (this.modelValue) {
      if (isDefined(value.daysOfWeek)) {
        value.daysOfWeek = value.daysOfWeek
          .map((day: boolean, index: number) => day ? index + 1 : null)
          .filter(day => !!day);
      }
      if (isDefined(value.startsOn) && value.startsOn !== 0) {
        value.startsOn = timeOfDayToUTCTimestamp(value.startsOn);
      }
      if (isDefined(value.endsOn) && value.endsOn !== 0) {
        value.endsOn = timeOfDayToUTCTimestamp(value.endsOn);
      }
      if (isDefined(value.items)){
        value.items = this.alarmScheduleForm.getRawValue().items;
        value.items = value.items.map((item) => {
          return { ...item, startsOn: timeOfDayToUTCTimestamp(item.startsOn), endsOn: timeOfDayToUTCTimestamp(item.endsOn)};
        });
      }
      this.modelValue = value;
      this.propagateChange(this.modelValue);
    }
  }


  private defaultItemsScheduler(index): UntypedFormGroup {
    return this.fb.group({
      enabled: [true],
      dayOfWeek: [index + 1],
      startsOn: [0, Validators.required],
      endsOn: [0, Validators.required]
    });
  }

  changeCustomScheduler($event: MatCheckboxChange, index: number) {
    const value = $event.checked;
    this.disabledSelectedTime(value, index, true);
  }

  private disabledSelectedTime(enable: boolean, index: number, emitEvent = false) {
    if (enable) {
      this.itemsSchedulerForm.at(index).get('startsOn').enable({emitEvent: false});
      this.itemsSchedulerForm.at(index).get('endsOn').enable({emitEvent});
    } else {
      this.itemsSchedulerForm.at(index).get('startsOn').disable({emitEvent: false});
      this.itemsSchedulerForm.at(index).get('endsOn').disable({emitEvent});
    }
  }

  getSchedulerRangeText(control: UntypedFormGroup | AbstractControl): string {
    return getAlarmScheduleRangeText(control.get('startsOn').value, control.get('endsOn').value);
  }

  get itemsSchedulerForm(): UntypedFormArray {
    return this.alarmScheduleForm.get('items') as UntypedFormArray;
  }
}
