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
  ControlValueAccessor,
  FormBuilder,
  FormControl,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  Validator,
  Validators
} from '@angular/forms';
import { AlarmConditionType, AlarmConditionTypeTranslationMap, AlarmRule } from '@shared/models/device.models';
import { MatDialog } from '@angular/material/dialog';
import { TimeUnit, timeUnitTranslationMap } from '@shared/models/time/time.models';
import { coerceBooleanProperty } from '@angular/cdk/coercion';

@Component({
  selector: 'tb-alarm-rule',
  templateUrl: './alarm-rule.component.html',
  styleUrls: ['./alarm-rule.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => AlarmRuleComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => AlarmRuleComponent),
      multi: true,
    }
  ]
})
export class AlarmRuleComponent implements ControlValueAccessor, OnInit, Validator {

  timeUnits = Object.keys(TimeUnit);
  timeUnitTranslations = timeUnitTranslationMap;
  alarmConditionTypes = Object.keys(AlarmConditionType);
  AlarmConditionType = AlarmConditionType;
  alarmConditionTypeTranslation = AlarmConditionTypeTranslationMap;

  @Input()
  disabled: boolean;

  private requiredValue: boolean;
  get required(): boolean {
    return this.requiredValue;
  }
  @Input()
  set required(value: boolean) {
    this.requiredValue = coerceBooleanProperty(value);
  }

  private modelValue: AlarmRule;

  alarmRuleFormGroup: FormGroup;

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
    this.alarmRuleFormGroup = this.fb.group({
      condition:  this.fb.group({
        condition: [null, Validators.required],
        spec: this.fb.group({
          type: [AlarmConditionType.SIMPLE, Validators.required],
          unit: [{value: null, disable: true}, Validators.required],
          value: [{value: null, disable: true}, [Validators.required, Validators.min(1), Validators.max(2147483647), Validators.pattern('[0-9]*')]],
          count: [{value: null, disable: true}, [Validators.required, Validators.min(1), Validators.max(2147483647), Validators.pattern('[0-9]*')]]
        })
      }, Validators.required),
      alarmDetails: [null]
    });
    this.alarmRuleFormGroup.get('condition.spec.type').valueChanges.subscribe((type) => {
      this.updateValidators(type, true, true);
    });
    this.alarmRuleFormGroup.valueChanges.subscribe(() => {
      this.updateModel();
    });
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.alarmRuleFormGroup.disable({emitEvent: false});
    } else {
      this.alarmRuleFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: AlarmRule): void {
    this.modelValue = value;
    if (this.modelValue?.condition?.spec === null) {
      this.modelValue.condition.spec = {
        type: AlarmConditionType.SIMPLE
      };
    }
    this.alarmRuleFormGroup.reset(this.modelValue || undefined, {emitEvent: false});
    this.updateValidators(this.modelValue?.condition?.spec?.type);
  }

  public validate(c: FormControl) {
    return (!this.required && !this.modelValue || this.alarmRuleFormGroup.valid) ? null : {
      alarmRule: {
        valid: false,
      },
    };
  }

  private updateValidators(type: AlarmConditionType, resetDuration = false, emitEvent = false) {
    switch (type) {
      case AlarmConditionType.DURATION:
        this.alarmRuleFormGroup.get('condition.spec.value').enable();
        this.alarmRuleFormGroup.get('condition.spec.unit').enable();
        this.alarmRuleFormGroup.get('condition.spec.count').disable();
        if (resetDuration) {
          this.alarmRuleFormGroup.get('condition.spec').patchValue({
            count: null
          });
        }
        break;
      case AlarmConditionType.REPEATING:
        this.alarmRuleFormGroup.get('condition.spec.count').enable();
        this.alarmRuleFormGroup.get('condition.spec.value').disable();
        this.alarmRuleFormGroup.get('condition.spec.unit').disable();
        if (resetDuration) {
          this.alarmRuleFormGroup.get('condition.spec').patchValue({
            value: null,
            unit: null
          });
        }
        break;
      case AlarmConditionType.SIMPLE:
        this.alarmRuleFormGroup.get('condition.spec.value').disable();
        this.alarmRuleFormGroup.get('condition.spec.unit').disable();
        this.alarmRuleFormGroup.get('condition.spec.count').disable();
        if (resetDuration) {
          this.alarmRuleFormGroup.get('condition.spec').patchValue({
            value: null,
            unit: null,
            count: null
          });
        }
        break;
    }
    this.alarmRuleFormGroup.get('condition.spec.value').updateValueAndValidity({emitEvent});
    this.alarmRuleFormGroup.get('condition.spec.unit').updateValueAndValidity({emitEvent});
    this.alarmRuleFormGroup.get('condition.spec.count').updateValueAndValidity({emitEvent});
  }

  private updateModel() {
    const value = this.alarmRuleFormGroup.value;
    if (this.modelValue) {
      this.modelValue = {...this.modelValue, ...value};
      this.propagateChange(this.modelValue);
    }
  }
}
