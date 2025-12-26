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

import { ChangeDetectorRef, Component, forwardRef, Input, OnChanges, SimpleChanges } from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  FormBuilder,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator
} from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { deepClone, isDefinedAndNotNull } from '@core/utils';
import { TranslateService } from '@ngx-translate/core';
import {
  dayOfWeekTranslations,
  getAlarmScheduleRangeText,
  utcTimestampToTimeOfDay
} from '@shared/models/device.models';
import { TimeUnit, timeUnitTranslationMap } from '@shared/models/time/time.models';
import {
  CfAlarmRuleConditionDialogComponent,
  CfAlarmRuleConditionDialogData
} from "@home/components/alarm-rules/cf-alarm-rule-condition-dialog.component";
import {
  AlarmRuleCondition,
  AlarmRuleConditionType,
  AlarmRuleExpressionType,
  AlarmRuleSchedule,
  AlarmRuleScheduleType,
  checkPredicates
} from "@shared/models/alarm-rule.models";
import { CalculatedFieldArgument } from "@shared/models/calculated-field.models";
import {
  AlarmRuleScheduleDialogData,
  CfAlarmScheduleDialogComponent
} from "@home/components/alarm-rules/cf-alarm-schedule-dialog.component";
import { coerceBoolean } from "@shared/decorators/coercion";
import { Observable } from "rxjs";

@Component({
  selector: 'tb-cf-alarm-rule-condition',
  templateUrl: './cf-alarm-rule-condition.component.html',
  styleUrls: ['./cf-alarm-rule-condition.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => CfAlarmRuleConditionComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => CfAlarmRuleConditionComponent),
      multi: true,
    }
  ]
})
export class CfAlarmRuleConditionComponent implements ControlValueAccessor, Validator, OnChanges {

  @Input()
  @coerceBoolean()
  disabled: boolean;

  @Input()
  @coerceBoolean()
  required: boolean;

  @Input()
  arguments: Record<string, CalculatedFieldArgument>;

  @Input()
  @coerceBoolean()
  isClearCondition = false;

  @Input({required: true})
  testScript: (expression: string) => Observable<string>;

  alarmRuleConditionFormGroup = this.fb.group({
    type: ['SIMPLE'],
    expression: [{type: AlarmRuleExpressionType.SIMPLE}],
    schedule: [null],
  });

  specText = '';

  filtersArgumentsValid: boolean = true;
  schedulerArgumentsValid: boolean = true;

  scheduleText = '';

  private modelValue: AlarmRuleCondition;

  private propagateChange = (v: any) => { };
  private onValidatorChange = () => { };

  constructor(private dialog: MatDialog,
              private fb: FormBuilder,
              private cd: ChangeDetectorRef,
              private translate: TranslateService) {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  registerOnValidatorChange(fn: () => void): void {
    this.onValidatorChange = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.alarmRuleConditionFormGroup.disable({emitEvent: false});
    } else {
      this.alarmRuleConditionFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: AlarmRuleCondition): void {
    this.modelValue = value;
    this.updateConditionInfo();
    if (value && !this.disabled) {
      if (this.validate(this.alarmRuleConditionFormGroup)) {
        this.onValidatorChange();
      }
    }
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes.arguments) {
      if (changes.arguments && !changes.arguments.firstChange && this.modelValue && !this.disabled) {
        if (this.validate(this.alarmRuleConditionFormGroup)) {
          this.onValidatorChange();
        }
      }
    }
  }

  private isScheduleArgumentValid(obj: any, validArguments: string[]): boolean {
    const arg = obj?.schedule?.dynamicValueArgument;
    return !arg || validArguments.includes(arg);
  }

  private areFilterAndPredicateArgumentsValid(obj: any, args: Record<string, CalculatedFieldArgument>): boolean {
    const validSet = new Set(Object.keys(args));
    const filters = obj?.expression?.filters || obj?.filters || [];
    for (const filter of filters) {
      if (filter.argument && !validSet.has(filter.argument)) {
        return false;
      }
    }
    for (const filter of filters) {
      if (Array.isArray(filter.predicates)) {
        if (!checkPredicates(filter.predicates, validSet)) {
          return false;
        }
      }
    }
    return true;
  }

  public conditionSet() {
    return this.modelValue && (this.modelValue.expression?.expression || this.modelValue.expression?.filters);
  }

  public validate(control: AbstractControl): ValidationErrors | null {
    this.filtersArgumentsValid = this.areFilterAndPredicateArgumentsValid(this.modelValue, this.arguments);
    this.schedulerArgumentsValid = this.isScheduleArgumentValid(this.modelValue, Object.keys(this.arguments));
    this.onValidatorChange = () => {
      control.updateValueAndValidity({ emitEvent: !this.disabled });
    };
    return this.conditionSet() && this.filtersArgumentsValid && this.schedulerArgumentsValid ? null : {
      alarmRuleCondition: {
        valid: false,
      }
    };
  }

  public openFilterDialog($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<CfAlarmRuleConditionDialogComponent, CfAlarmRuleConditionDialogData,
      AlarmRuleCondition>(CfAlarmRuleConditionDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        readonly: this.disabled,
        condition: this.disabled ? this.modelValue : deepClone(this.modelValue),
        arguments: this.arguments,
        testScript: this.testScript
      }
    }).afterClosed().subscribe((result) => {
      if (result) {
        this.modelValue = {...this.modelValue, ...result};
        this.updateModel();
        this.cd.detectChanges();
      }
    });
  }

  private updateConditionInfo() {
    this.alarmRuleConditionFormGroup.patchValue(
      this.modelValue ? {
        type: this.modelValue?.type,
        expression: this.modelValue?.expression,
        schedule: this.modelValue?.schedule,
      } : null, {emitEvent: false}
    );
    this.updateScheduleText();
    this.updateSpecText();
  }

  private updateSpecText() {
    this.specText = '';
    if (this.modelValue && this.modelValue.type) {
      const type = this.modelValue.type;
      switch (type) {
        case AlarmRuleConditionType.SIMPLE:
          break;
        case AlarmRuleConditionType.DURATION:
          let duringText = '';
          switch (this.modelValue.unit) {
            case TimeUnit.SECONDS:
              duringText = this.translate.instant('timewindow.seconds', {seconds: this.modelValue.value.staticValue});
              break;
            case TimeUnit.MINUTES:
              duringText = this.translate.instant('timewindow.minutes', {minutes: this.modelValue.value.staticValue});
              break;
            case TimeUnit.HOURS:
              duringText = this.translate.instant('timewindow.hours', {hours: this.modelValue.value.staticValue});
              break;
            case TimeUnit.DAYS:
              duringText = this.translate.instant('timewindow.days', {days: this.modelValue.value.staticValue});
              break;
          }
          if (this.modelValue.value.dynamicValueArgument) {
            this.specText = this.translate.instant('alarm-rule.condition-during-dynamic', {
              attribute: `${this.modelValue.value.dynamicValueArgument}`
            }) + ' ' + this.translate.instant(timeUnitTranslationMap.get(this.modelValue.unit)).toLowerCase();
          } else {
            this.specText = this.translate.instant('alarm-rule.condition-during', {
              during: duringText
            });
          }
          break;
        case AlarmRuleConditionType.REPEATING:
          if (this.modelValue.count.dynamicValueArgument) {
            this.specText = this.translate.instant('alarm-rule.condition-repeat-times-dynamic', {
              attribute: `${this.modelValue.count.dynamicValueArgument}`
            });
          } else {
            this.specText = this.translate.instant('alarm-rule.condition-repeat-times',
              {count: this.modelValue.count.staticValue});
          }
          break;
      }
    }
  }

  private updateModel() {
    this.updateConditionInfo();
    this.propagateChange(this.modelValue);
    if (this.modelValue) {
      this.onValidatorChange();
    }
  }

  public openScheduleDialog($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<CfAlarmScheduleDialogComponent, AlarmRuleScheduleDialogData,
      AlarmRuleSchedule>(CfAlarmScheduleDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        readonly: this.disabled,
        alarmSchedule: this.disabled ? this.modelValue?.schedule : deepClone(this.modelValue?.schedule),
        arguments: this.arguments
      }
    }).afterClosed().subscribe((result) => {
      if (result) {
        this.modelValue.schedule = result;
        this.updateModel();
        this.cd.detectChanges();
      }
    });
  }

  private updateScheduleText() {
    const schedule = this.modelValue?.schedule;
    this.scheduleText = '';
    if (isDefinedAndNotNull(schedule)) {
      if (schedule.dynamicValueArgument) {
        this.scheduleText = this.translate.instant('alarm-rule.value-argument') + ': ' + schedule?.dynamicValueArgument
      } else {
        switch (schedule.staticValue.type) {
          case AlarmRuleScheduleType.ANY_TIME:
            this.scheduleText = this.translate.instant('alarm-rule.schedule.any-time');
            break;
          case AlarmRuleScheduleType.SPECIFIC_TIME:
            for (const day of schedule.staticValue.daysOfWeek) {
              if (this.scheduleText.length) {
                this.scheduleText += ', ';
              }
              this.scheduleText += this.translate.instant(dayOfWeekTranslations[day - 1]);
            }
            this.scheduleText += ' <b>' + getAlarmScheduleRangeText(utcTimestampToTimeOfDay(schedule.staticValue.startsOn),
              utcTimestampToTimeOfDay(schedule.staticValue.endsOn)) + '</b>';
            break;
          case AlarmRuleScheduleType.CUSTOM:
            for (const item of schedule.staticValue.items) {
              if (item.enabled) {
                if (this.scheduleText.length) {
                  this.scheduleText += ', ';
                }
                this.scheduleText += this.translate.instant(dayOfWeekTranslations[item.dayOfWeek  - 1]);
                this.scheduleText += ' <b>' + getAlarmScheduleRangeText(utcTimestampToTimeOfDay(item.startsOn),
                  utcTimestampToTimeOfDay(item.endsOn)) + '</b>';
              }
            }
            break;
        }
      }
    }
    if (!this.scheduleText.length) {
      this.scheduleText = this.translate.instant('alarm-rule.schedule.any-time');
    }
  }
}
