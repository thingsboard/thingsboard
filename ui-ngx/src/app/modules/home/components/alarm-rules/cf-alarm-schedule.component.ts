///
/// Copyright © 2016-2025 The Thingsboard Authors
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
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormArray,
  UntypedFormBuilder,
  UntypedFormGroup,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import {
  dayOfWeekTranslations,
  getAlarmScheduleRangeText,
  timeOfDayToUTCTimestamp,
  utcTimestampToTimeOfDay
} from '@shared/models/device.models';
import { isDefined } from '@core/utils';
import { getDefaultTimezone } from '@shared/models/time/time.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  AlarmRuleSchedule,
  AlarmRuleScheduleType,
  AlarmRuleScheduleTypeTranslationMap
} from "@shared/models/alarm-rule.models";
import { CalculatedFieldArgument } from "@shared/models/calculated-field.models";
import { MatChipSelectionChange } from "@angular/material/chips";

@Component({
  selector: 'tb-cf-alarm-schedule',
  templateUrl: './cf-alarm-schedule.component.html',
  styleUrls: [],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => CfAlarmScheduleComponent),
    multi: true
  }, {
    provide: NG_VALIDATORS,
    useExisting: forwardRef(() => CfAlarmScheduleComponent),
    multi: true
  }]
})
export class CfAlarmScheduleComponent implements ControlValueAccessor, Validator, OnInit {
  @Input()
  disabled: boolean;

  @Input()
  arguments: Record<string, CalculatedFieldArgument>;

  private settingsModeValue: 'static' | 'dynamic';
  get settingsMode(): 'static' | 'dynamic' {
    return this.settingsModeValue;
  }
  @Input()
  set settingsMode(value: 'static' | 'dynamic') {
    if (value !== this.settingsModeValue && this.alarmScheduleForm) {
      this.settingsModeValue = value;
      this.updateModeValidators(value);
      this.updateModel();
    }
  }

  alarmScheduleForm = this.fb.group({
    staticValue: this.fb.group({
      type: [AlarmRuleScheduleType.ANY_TIME, Validators.required],
      timezone: [null, Validators.required],
      daysOfWeek: [null, Validators.required],
      startsOn: [0, Validators.required],
      endsOn: [0, Validators.required],
      items: this.fb.array(Array.from({length: 7}, (value, i) => this.defaultItemsScheduler(i)), this.validateItems),
    }),
    dynamicValueArgument: [null, Validators.required]
  });

  alarmScheduleTypes = Object.keys(AlarmRuleScheduleType);
  alarmScheduleType = AlarmRuleScheduleType;
  alarmScheduleTypeTranslate = AlarmRuleScheduleTypeTranslationMap;
  dayOfWeekTranslationsArray = dayOfWeekTranslations;

  allDays = Array(7).fill(0).map((x, i) => i);

  private modelValue: AlarmRuleSchedule;

  private defaultItems = Array.from({length: 7}, (value, i) => ({
    enabled: true,
    dayOfWeek: i + 1
  }));

  private propagateChange = (v: any) => { };

  constructor(private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
  }

  ngOnInit(): void {
    this.alarmScheduleForm.get('staticValue.type').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((type) => {
      const defaultTimezone = getDefaultTimezone();
      this.alarmScheduleForm.get('staticValue').patchValue({type, items: this.defaultItems, timezone: defaultTimezone}, {emitEvent: false});
      this.alarmScheduleForm.get('dynamicValueArgument').patchValue(null, {emitEvent: false});
      this.updateValidators(type);
      this.alarmScheduleForm.updateValueAndValidity();
    });
    this.alarmScheduleForm.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateModel();
    });

    this.alarmScheduleForm.get('staticValue.items').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((items) => {
      items.forEach((item, index) => this.disabledSelectedTime(item.enabled, index, false))
    });
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
      this.updateModeValidators(this.settingsMode);
    }
  }

  writeValue(value: AlarmRuleSchedule): void {
    if (value) {
      this.modelValue = value;
      if (this.modelValue.dynamicValueArgument) {
        this.settingsModeValue = 'dynamic';
        this.alarmScheduleForm.get('dynamicValueArgument').patchValue(this.modelValue.dynamicValueArgument, {emitEvent: false});
      } else {
        this.settingsModeValue = 'static';
        switch (this.modelValue.staticValue.type) {
          case AlarmRuleScheduleType.SPECIFIC_TIME:
            this.alarmScheduleForm.patchValue({
              staticValue: {
                type: this.modelValue.staticValue.type,
                timezone: this.modelValue.staticValue.timezone,
                daysOfWeek: this.modelValue.staticValue.daysOfWeek,
                startsOn: utcTimestampToTimeOfDay(this.modelValue.staticValue.startsOn),
                endsOn: utcTimestampToTimeOfDay(this.modelValue.staticValue.endsOn),
              },
            }, {emitEvent: false});
            break;
          case AlarmRuleScheduleType.CUSTOM:
            if (this.modelValue?.dynamicValueArgument) {
              this.alarmScheduleForm.patchValue({
                staticValue: {
                  type: this.modelValue.staticValue.type,
                },
              }, {emitEvent: false});
            } else if (this.modelValue.staticValue?.items) {
              const alarmDays = [];
              this.modelValue.staticValue.items
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
                staticValue: {
                  type: this.modelValue.staticValue.type,
                  timezone: this.modelValue.staticValue.timezone,
                  items: alarmDays,
                },
              }, {emitEvent: false});
            }
            break;
          default:
            this.alarmScheduleForm.patchValue(this.modelValue || undefined, {emitEvent: false});
        }
        this.updateValidators(this.modelValue.staticValue.type);
      }
      this.updateModeValidators(this.settingsMode);
    }
  }

  validate(control: UntypedFormGroup): ValidationErrors | null {
    return this.alarmScheduleForm.valid ? null : {
      alarmScheduler: {
        valid: false
      }
    };
  }

  private updateModeValidators(mode: 'static' | 'dynamic') {
    if (mode === 'static') {
      this.alarmScheduleForm.get('staticValue').enable({emitEvent: false});
      this.alarmScheduleForm.get('dynamicValueArgument').disable({emitEvent: false});
      this.updateValidators(this.alarmScheduleForm.get('staticValue.type').value);
    } else {
      this.alarmScheduleForm.get('staticValue').disable({emitEvent: false});
      this.alarmScheduleForm.get('dynamicValueArgument').enable({emitEvent: false});
    }
  }

  private updateValidators(type: AlarmRuleScheduleType){
    switch (type){
      case AlarmRuleScheduleType.ANY_TIME:
        this.alarmScheduleForm.get('staticValue.timezone').disable({emitEvent: false});
        this.alarmScheduleForm.get('staticValue.daysOfWeek').disable({emitEvent: false});
        this.alarmScheduleForm.get('staticValue.startsOn').disable({emitEvent: false});
        this.alarmScheduleForm.get('staticValue.endsOn').disable({emitEvent: false});
        this.alarmScheduleForm.get('staticValue.items').disable({emitEvent: false});
        break;
      case AlarmRuleScheduleType.SPECIFIC_TIME:
        this.alarmScheduleForm.get('staticValue.timezone').enable({emitEvent: false});
        this.alarmScheduleForm.get('staticValue.daysOfWeek').enable({emitEvent: false});
        this.alarmScheduleForm.get('staticValue.startsOn').enable({emitEvent: false});
        this.alarmScheduleForm.get('staticValue.endsOn').enable({emitEvent: false});
        this.alarmScheduleForm.get('staticValue.items').disable({emitEvent: false});
        break;
      case AlarmRuleScheduleType.CUSTOM:
        this.alarmScheduleForm.get('staticValue.timezone').enable({emitEvent: false});
        this.alarmScheduleForm.get('staticValue.daysOfWeek').disable({emitEvent: false});
        this.alarmScheduleForm.get('staticValue.startsOn').disable({emitEvent: false});
        this.alarmScheduleForm.get('staticValue.endsOn').disable({emitEvent: false});
        this.alarmScheduleForm.get('staticValue.items').enable({emitEvent: true});
        break;
    }
  }

  private updateModel() {
    const value = this.alarmScheduleForm.value as AlarmRuleSchedule;
    if (this.settingsMode === 'static') {
      if (isDefined(value.staticValue.startsOn) && value.staticValue.startsOn !== 0) {
        value.staticValue.startsOn = timeOfDayToUTCTimestamp(value.staticValue.startsOn);
      }
      if (isDefined(value.staticValue.endsOn) && value.staticValue.endsOn !== 0) {
        value.staticValue.endsOn = timeOfDayToUTCTimestamp(value.staticValue.endsOn);
      }
      if (isDefined(value.staticValue.items)){
        value.staticValue.items = this.alarmScheduleForm.getRawValue().staticValue.items;
        value.staticValue.items = value.staticValue.items.map((item) => {
          return { ...item, startsOn: timeOfDayToUTCTimestamp(item.startsOn), endsOn: timeOfDayToUTCTimestamp(item.endsOn)};
        });
      }
    }
    this.modelValue = value;
    if (this.alarmScheduleForm.valid) {
      this.propagateChange(this.modelValue);
    } else {
      this.propagateChange(null);
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

  changeCustomScheduler($event: MatChipSelectionChange, index: number) {
    const value = $event.selected;
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
    return this.alarmScheduleForm.get('staticValue.items') as UntypedFormArray;
  }

  get argumentsList(): Array<string> {
    return this.arguments ? Object.keys(this.arguments): [];
  }

}
