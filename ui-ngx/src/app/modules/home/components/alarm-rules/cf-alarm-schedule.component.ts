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

import { Component, DestroyRef, forwardRef, Input, OnChanges, OnInit, SimpleChanges } from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  FormArray,
  FormBuilder,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import {
  CustomTimeSchedulerItem,
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
import { coerceBoolean } from "@shared/decorators/coercion";

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
        }],
    standalone: false
})
export class CfAlarmScheduleComponent implements ControlValueAccessor, Validator, OnInit, OnChanges {

  @Input()
  @coerceBoolean()
  disabled: boolean;

  @Input()
  arguments: Record<string, CalculatedFieldArgument>;

  @Input()
  @coerceBoolean()
  dynamicMode: boolean;

  alarmScheduleForm = this.fb.group({
    staticValue: this.fb.group({
      type: [AlarmRuleScheduleType.ANY_TIME, Validators.required],
      timezone: ['', Validators.required],
      daysOfWeek: this.fb.control<number[] | null>(null, Validators.required),
      startsOn: this.fb.control<Date | number>(0, Validators.required),
      endsOn: this.fb.control<Date | number>(0, Validators.required),
      items: this.fb.array(Array.from({length: 7}, (value, i) => this.defaultItemsScheduler(i)), this.validateItems),
    }),
    dynamicValueArgument: ['', Validators.required]
  });

  alarmScheduleTypes = Object.keys(AlarmRuleScheduleType) as Array<AlarmRuleScheduleType>;
  alarmScheduleType = AlarmRuleScheduleType;
  alarmScheduleTypeTranslate = AlarmRuleScheduleTypeTranslationMap;
  dayOfWeekTranslationsArray = dayOfWeekTranslations;

  argumentsList: Array<string>;

  allDays = Array(7).fill(0).map((x, i) => i);

  private modelValue: AlarmRuleSchedule;

  private defaultItems = Array.from({length: 7}, (value, i) => ({
    enabled: true,
    dayOfWeek: i + 1
  })) as CustomTimeSchedulerItem[];

  private propagateChange = (v: any) => { };

  constructor(private fb: FormBuilder,
              private destroyRef: DestroyRef) {
  }

  ngOnInit(): void {
    this.argumentsList = this.arguments ? Object.keys(this.arguments): [];
    this.alarmScheduleForm.get('staticValue.type').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((type) => {
      const defaultTimezone = getDefaultTimezone();
      const staticValue = {...this.alarmScheduleForm.get('staticValue').value, type, items: this.defaultItems, timezone: defaultTimezone};
      this.alarmScheduleForm.get('staticValue').patchValue(staticValue, {emitEvent: false});
      this.alarmScheduleForm.get('dynamicValueArgument').patchValue(null, {emitEvent: false});
      this.updateValidators(type);
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

  ngOnChanges(changes: SimpleChanges) {
    if (changes.dynamicMode) {
      const dynamicModeChanges = changes.dynamicMode;
      if (!dynamicModeChanges.firstChange && dynamicModeChanges.currentValue !== dynamicModeChanges.previousValue) {
        this.updateModeValidators(dynamicModeChanges.currentValue);
        this.updateModel();
      }
    }
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
      this.updateModeValidators(this.dynamicMode);
    }
  }

  writeValue(value: AlarmRuleSchedule): void {
    if (value) {
      this.modelValue = value;
      if (this.modelValue.dynamicValueArgument) {
        this.alarmScheduleForm.get('dynamicValueArgument').patchValue(Object.keys(this.arguments).includes(this.modelValue.dynamicValueArgument) ? this.modelValue.dynamicValueArgument : null, {emitEvent: false});
      } else {
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
      this.updateModeValidators(this.dynamicMode);
    }
  }

  validate(control: FormGroup): ValidationErrors | null {
    return this.alarmScheduleForm.valid ? null : {
      alarmScheduler: {
        valid: false
      }
    };
  }

  private updateModeValidators(mode: boolean) {
    if (mode) {
      this.alarmScheduleForm.get('staticValue').disable({emitEvent: false});
      this.alarmScheduleForm.get('dynamicValueArgument').enable({emitEvent: false});
    } else {
      this.alarmScheduleForm.get('staticValue').enable({emitEvent: false});
      this.alarmScheduleForm.get('dynamicValueArgument').disable({emitEvent: false});
      this.updateValidators(this.alarmScheduleForm.get('staticValue.type').value);
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
        this.alarmScheduleForm.get('staticValue.items').enable({emitEvent: false});
        break;
    }
  }

  private updateModel() {
    const value = this.alarmScheduleForm.value as AlarmRuleSchedule;
    if (!this.dynamicMode) {
      if (isDefined(value.staticValue.startsOn) && value.staticValue.startsOn !== 0) {
        value.staticValue.startsOn = timeOfDayToUTCTimestamp(value.staticValue.startsOn);
      }
      if (isDefined(value.staticValue.endsOn) && value.staticValue.endsOn !== 0) {
        value.staticValue.endsOn = timeOfDayToUTCTimestamp(value.staticValue.endsOn);
      }
      if (isDefined(value.staticValue.items)){
        value.staticValue.items = this.alarmScheduleForm.getRawValue().staticValue.items as CustomTimeSchedulerItem[];
        value.staticValue.items = value.staticValue.items.map((item) => {
          return { ...item, startsOn: timeOfDayToUTCTimestamp(item.startsOn), endsOn: timeOfDayToUTCTimestamp(item.endsOn)};
        });
      }
    }
    this.modelValue = value;
    this.propagateChange(this.modelValue);
  }


  private defaultItemsScheduler(index: number): FormGroup {
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

  getSchedulerRangeText(control: FormGroup | AbstractControl): string {
    return getAlarmScheduleRangeText(control.get('startsOn').value, control.get('endsOn').value);
  }

  get itemsSchedulerForm(): FormArray {
    return this.alarmScheduleForm.get('staticValue.items') as FormArray;
  }
}
