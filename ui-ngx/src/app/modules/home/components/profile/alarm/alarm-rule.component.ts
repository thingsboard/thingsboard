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

import { ChangeDetectorRef, Component, forwardRef, Input, NgZone, OnInit } from '@angular/core';
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
import { AlarmRule } from '@shared/models/device.models';
import { MatDialog } from '@angular/material/dialog';
import { TimeUnit, timeUnitTranslationMap } from '../../../../../shared/models/time/time.models';
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

  enableDuration = false;

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
        durationUnit: [null],
        durationValue: [null]
      }, Validators.required),
      alarmDetails: [null]
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
    this.enableDuration = value && !!value.condition.durationValue;
    this.alarmRuleFormGroup.reset(this.modelValue || undefined, {emitEvent: false});
    this.updateValidators();
  }

  public validate(c: FormControl) {
    return (!this.required && !this.modelValue || this.alarmRuleFormGroup.valid) ? null : {
      alarmRule: {
        valid: false,
      },
    };
  }

  public enableDurationChanged(enableDuration) {
    this.enableDuration = enableDuration;
    this.updateValidators(true, true);
  }

  private updateValidators(resetDuration = false, emitEvent = false) {
    if (this.enableDuration) {
      this.alarmRuleFormGroup.get('condition').get('durationValue')
        .setValidators([Validators.required, Validators.min(1), Validators.max(2147483647)]);
      this.alarmRuleFormGroup.get('condition').get('durationUnit')
        .setValidators([Validators.required]);
    } else {
      this.alarmRuleFormGroup.get('condition').get('durationValue')
        .setValidators([]);
      this.alarmRuleFormGroup.get('condition').get('durationUnit')
        .setValidators([]);
      if (resetDuration) {
        this.alarmRuleFormGroup.get('condition').patchValue({
          durationValue: null,
          durationUnit: null
        });
      }
    }
    this.alarmRuleFormGroup.get('condition').get('durationValue').updateValueAndValidity({emitEvent});
    this.alarmRuleFormGroup.get('condition').get('durationUnit').updateValueAndValidity({emitEvent});
  }

  private updateModel() {
    const value = this.alarmRuleFormGroup.value;
    if (this.modelValue) {
      this.modelValue = {...this.modelValue, ...value};
      this.propagateChange(this.modelValue);
    }
  }
}
