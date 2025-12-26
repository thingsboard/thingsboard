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

import { Component, DestroyRef, forwardRef, Input } from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  FormArray,
  FormBuilder,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormArray,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { AlarmSeverity, alarmSeverityColors, alarmSeverityTranslations } from '@shared/models/alarm.models';
import { AlarmRule } from "@shared/models/alarm-rule.models";
import { CalculatedFieldArgument } from "@shared/models/calculated-field.models";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { coerceBoolean } from "@shared/decorators/coercion";
import { Observable } from "rxjs";

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
export class CreateCfAlarmRulesComponent implements ControlValueAccessor, Validator {

  @Input()
  @coerceBoolean()
  disabled: boolean;

  @Input()
  arguments: Record<string, CalculatedFieldArgument>;

  @Input({required: true})
  testScript: (expression: string) => Observable<string>;

  alarmSeverities = Object.keys(AlarmSeverity);
  alarmSeverityEnum = AlarmSeverity;
  alarmSeverityTranslationMap = alarmSeverityTranslations;

  AlarmSeverityNotificationColors = alarmSeverityColors;

  createAlarmRulesFormGroup = this.fb.group({
    createAlarmRules: this.fb.array<{severity: AlarmSeverity, alarmRule: AlarmRule}>([])
  });

  private usedSeverities: AlarmSeverity[] = [];

  private propagateChange = (v: any) => { };

  constructor(private fb: FormBuilder,
              private destroyRef: DestroyRef) {
    this.createAlarmRulesFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => this.updateModel());
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
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

  writeValue(createAlarmRules: Record<AlarmSeverity, AlarmRule>): void {
    const createAlarmRulesControls: Array<AbstractControl> = [];
    if (createAlarmRules) {
      Object.keys(createAlarmRules).forEach((severity) => {
        const createAlarmRule = createAlarmRules[severity];
        if (severity === 'empty') {
          severity = null;
        }
        createAlarmRulesControls.push(this.fb.group({
          severity: [severity, Validators.required],
          alarmRule: {value: [createAlarmRule, Validators.required], disabled: this.disabled}
        }));
      });
    }
    const formArray = this.createAlarmRulesFormGroup.get('createAlarmRules') as FormArray;
    formArray.clear({emitEvent: false});
    createAlarmRulesControls.forEach(c => formArray.push(c, {emitEvent: false}));
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
    (this.createAlarmRulesFormGroup.get('createAlarmRules') as FormArray).removeAt(index);
  }

  public addCreateAlarmRule() {
    const createAlarmRulesArray = this.createAlarmRulesFormGroup.get('createAlarmRules') as FormArray;
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

  public validate(): ValidationErrors | null {
    return this.createAlarmRulesFormGroup.valid && this.createAlarmRulesFormArray().length > 0 ? null : {
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
    const value = this.createAlarmRulesFormGroup.get('createAlarmRules').value;
    value.forEach((rule, index) => {
      this.usedSeverities[index] = AlarmSeverity[rule.severity];
    });
  }

  private updateModel() {
    const value = this.createAlarmRulesFormGroup.get('createAlarmRules').value;
    const createAlarmRules = {} as Record<AlarmSeverity, AlarmRule>;
    value.forEach(v => createAlarmRules[v.severity] = v.alarmRule);
    this.updateUsedSeverities();
    this.propagateChange(createAlarmRules);
  }
}
