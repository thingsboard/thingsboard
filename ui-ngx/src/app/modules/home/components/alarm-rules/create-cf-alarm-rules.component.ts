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

import { Component, forwardRef, Input, OnDestroy, OnInit } from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormArray,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  Validator,
  Validators
} from '@angular/forms';
import { Subject } from 'rxjs';
import { AlarmSeverity, alarmSeverityTranslations } from '@shared/models/alarm.models';
import { takeUntil } from 'rxjs/operators';
import { AlarmRule } from "@shared/models/alarm-rule.models";
import { CalculatedFieldArgument } from "@shared/models/calculated-field.models";
import { AlarmSeverityNotificationColors } from "@shared/models/notification.models";

@Component({
  selector: 'tb-create-cf-alarm-rules',
  templateUrl: './create-cf-alarm-rules.component.html',
  styleUrls: ['./create-cf-alarm-rules.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => CreateCfAlarmRulesComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => CreateCfAlarmRulesComponent),
      multi: true,
    }
  ]
})
export class CreateCfAlarmRulesComponent implements ControlValueAccessor, OnInit, Validator, OnDestroy {

  alarmSeverities = Object.keys(AlarmSeverity);
  alarmSeverityEnum = AlarmSeverity;
  alarmSeverityTranslationMap = alarmSeverityTranslations;

  AlarmSeverityNotificationColors = AlarmSeverityNotificationColors;

  @Input()
  disabled: boolean;

  @Input()
  arguments: Record<string, CalculatedFieldArgument>;

  createAlarmRulesFormGroup: UntypedFormGroup;

  private usedSeverities: AlarmSeverity[] = [];

  private destroy$ = new Subject<void>();
  private propagateChange = (v: any) => { };

  constructor(private fb: UntypedFormBuilder) {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.createAlarmRulesFormGroup = this.fb.group({
      createAlarmRules: this.fb.array([])
    });
    this.createAlarmRulesFormGroup.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => this.updateModel());
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  createAlarmRulesFormArray(): UntypedFormArray {
    return this.createAlarmRulesFormGroup.get('createAlarmRules') as UntypedFormArray;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.createAlarmRulesFormGroup.disable({emitEvent: false});
    } else {
      this.createAlarmRulesFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(createAlarmRules: {[severity: string]: AlarmRule}): void {
    const createAlarmRulesControls: Array<AbstractControl> = [];
    if (createAlarmRules) {
      Object.keys(createAlarmRules).forEach((severity) => {
        const createAlarmRule = createAlarmRules[severity];
        if (severity === 'empty') {
          severity = null;
        }
        createAlarmRulesControls.push(this.fb.group({
          severity: [severity, Validators.required],
          alarmRule: [createAlarmRule, Validators.required]
        }));
      });
    }
    this.createAlarmRulesFormGroup.setControl('createAlarmRules', this.fb.array(createAlarmRulesControls), {emitEvent: false});
    if (this.disabled) {
      this.createAlarmRulesFormGroup.disable({emitEvent: false});
    } else {
      this.createAlarmRulesFormGroup.enable({emitEvent: false});
    }
    this.updateUsedSeverities();
    if (!this.disabled && !this.createAlarmRulesFormGroup.valid) {
      this.updateModel();
    }
  }

  public removeCreateAlarmRule(index: number) {
    (this.createAlarmRulesFormGroup.get('createAlarmRules') as UntypedFormArray).removeAt(index);
  }

  public addCreateAlarmRule() {
    const createAlarmRulesArray = this.createAlarmRulesFormGroup.get('createAlarmRules') as UntypedFormArray;
    createAlarmRulesArray.push(this.fb.group({
      severity: [this.getFirstUnusedSeverity(), Validators.required],
      alarmRule: [null, Validators.required]
    }));
    this.createAlarmRulesFormGroup.updateValueAndValidity();
    if (!this.createAlarmRulesFormGroup.valid) {
      this.updateModel();
    }
  }

  private getFirstUnusedSeverity(): AlarmSeverity {
    for (const severityKey of Object.keys(AlarmSeverity)) {
      const severity = AlarmSeverity[severityKey];
      if (this.usedSeverities.indexOf(severity) === -1) {
        return severity;
      }
    }
    return null;
  }

  public validate(c: UntypedFormControl) {
    return (this.createAlarmRulesFormGroup.valid) ? null : {
      createAlarmRules: {
        valid: false,
      },
    };
  }

  public isDisabledSeverity(severity: AlarmSeverity, index: number): boolean {
    const usedIndex = this.usedSeverities.indexOf(severity);
    return usedIndex > -1 && usedIndex !== index;
  }

  private updateUsedSeverities() {
    this.usedSeverities = [];
    const value: {severity: string, alarmRule: AlarmRule}[] = this.createAlarmRulesFormGroup.get('createAlarmRules').value;
    value.forEach((rule, index) => {
      this.usedSeverities[index] = AlarmSeverity[rule.severity];
    });
  }

  private updateModel() {
    const value: {severity: string, alarmRule: AlarmRule}[] = this.createAlarmRulesFormGroup.get('createAlarmRules').value;
    const createAlarmRules: {[severity: string]: AlarmRule} = {};
    value.forEach(v => createAlarmRules[v.severity] = v.alarmRule);
    this.updateUsedSeverities();
    this.propagateChange(createAlarmRules);
  }
}
