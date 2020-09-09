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
import { AlarmCondition } from '@shared/models/device.models';
import { MatDialog } from '@angular/material/dialog';

@Component({
  selector: 'tb-alarm-rule-condition',
  templateUrl: './alarm-rule-condition.component.html',
  styleUrls: [],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => AlarmRuleConditionComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => AlarmRuleConditionComponent),
      multi: true,
    }
  ]
})
export class AlarmRuleConditionComponent implements ControlValueAccessor, OnInit, Validator {

  @Input()
  disabled: boolean;

  private modelValue: AlarmCondition;

  alarmRuleConditionFormGroup: FormGroup;

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
    this.alarmRuleConditionFormGroup = this.fb.group({
      condition: [null, Validators.required],
      durationUnit: [null],
      durationValue: [null]
    });
    this.alarmRuleConditionFormGroup.valueChanges.subscribe(() => {
      this.updateModel();
    });
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.alarmRuleConditionFormGroup.disable({emitEvent: false});
    } else {
      this.alarmRuleConditionFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: AlarmCondition): void {
    this.modelValue = value;
    this.alarmRuleConditionFormGroup.reset(this.modelValue, {emitEvent: false});
  }

  public validate(c: FormControl) {
    return (this.alarmRuleConditionFormGroup.valid) ? null : {
      alarmRuleCondition: {
        valid: false,
      },
    };
  }

  private updateModel() {
    if (this.alarmRuleConditionFormGroup.valid) {
      const value = this.alarmRuleConditionFormGroup.value;
      this.modelValue = {...this.modelValue, ...value};
      this.propagateChange(this.modelValue);
    } else {
      this.propagateChange(null);
    }
  }
}
