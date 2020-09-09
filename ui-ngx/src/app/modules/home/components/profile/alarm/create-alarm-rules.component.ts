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

import { Component, forwardRef, Input, OnInit } from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  FormArray,
  FormBuilder,
  FormControl,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  Validator,
  Validators
} from '@angular/forms';
import { AlarmRule } from '@shared/models/device.models';
import { MatDialog } from '@angular/material/dialog';
import { Subscription } from 'rxjs';
import { AlarmSeverity, alarmSeverityTranslations } from '../../../../../shared/models/alarm.models';

@Component({
  selector: 'tb-create-alarm-rules',
  templateUrl: './create-alarm-rules.component.html',
  styleUrls: ['./create-alarm-rules.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => CreateAlarmRulesComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => CreateAlarmRulesComponent),
      multi: true,
    }
  ]
})
export class CreateAlarmRulesComponent implements ControlValueAccessor, OnInit, Validator {

  alarmSeverities = Object.keys(AlarmSeverity);
  alarmSeverityEnum = AlarmSeverity;

  alarmSeverityTranslationMap = alarmSeverityTranslations;

  @Input()
  disabled: boolean;

  createAlarmRulesFormGroup: FormGroup;

  private valueChangeSubscription: Subscription = null;

  private propagateChange = (v: any) => { };

  constructor(private dialog: MatDialog,
              private fb: FormBuilder) {
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
  }

  createAlarmRulesFormArray(): FormArray {
    return this.createAlarmRulesFormGroup.get('createAlarmRules') as FormArray;
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
    if (this.valueChangeSubscription) {
      this.valueChangeSubscription.unsubscribe();
    }
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
    this.createAlarmRulesFormGroup.setControl('createAlarmRules', this.fb.array(createAlarmRulesControls));
    if (this.disabled) {
      this.createAlarmRulesFormGroup.disable({emitEvent: false});
    } else {
      this.createAlarmRulesFormGroup.enable({emitEvent: false});
    }
    this.valueChangeSubscription = this.createAlarmRulesFormGroup.valueChanges.subscribe(() => {
      this.updateModel();
    });
  }

  public removeCreateAlarmRule(index: number) {
    (this.createAlarmRulesFormGroup.get('createAlarmRules') as FormArray).removeAt(index);
  }

  public addCreateAlarmRule() {
    const createAlarmRule: AlarmRule = {
      condition: {
        condition: []
      }
    };
    const createAlarmRulesArray = this.createAlarmRulesFormGroup.get('createAlarmRules') as FormArray;
    createAlarmRulesArray.push(this.fb.group({
      severity: [null, Validators.required],
      alarmRule: [createAlarmRule, Validators.required]
    }));
    this.createAlarmRulesFormGroup.updateValueAndValidity();
  }

  public validate(c: FormControl) {
    return (this.createAlarmRulesFormGroup.valid && this.createAlarmRulesFormGroup.get('createAlarmRules').value.length) ? null : {
      createAlarmRules: {
        valid: false,
      },
    };
  }

  private updateModel() {
    if (this.createAlarmRulesFormGroup.valid) {
      const value: {severity: string, alarmRule: AlarmRule}[] = this.createAlarmRulesFormGroup.get('createAlarmRules').value;
      const createAlarmRules: {[severity: string]: AlarmRule} = {};
      value.forEach(v => {
        createAlarmRules[v.severity] = v.alarmRule;
      });
      this.propagateChange(createAlarmRules);
    } else {
      this.propagateChange(null);
    }
  }
}
