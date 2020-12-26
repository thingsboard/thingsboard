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

import { Component, EventEmitter, forwardRef, Input, OnInit, Output } from '@angular/core';
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
import { AlarmRule, DeviceProfileAlarm, deviceProfileAlarmValidator } from '@shared/models/device.models';
import { MatDialog } from '@angular/material/dialog';
import { COMMA, ENTER, SEMICOLON } from '@angular/cdk/keycodes';
import { MatChipInputEvent } from '@angular/material/chips';
import { EntityId } from '@shared/models/id/entity-id';

@Component({
  selector: 'tb-device-profile-alarm',
  templateUrl: './device-profile-alarm.component.html',
  styleUrls: ['./device-profile-alarm.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => DeviceProfileAlarmComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => DeviceProfileAlarmComponent),
      multi: true,
    }
  ]
})
export class DeviceProfileAlarmComponent implements ControlValueAccessor, OnInit, Validator {

  @Input()
  disabled: boolean;

  @Output()
  removeAlarm = new EventEmitter();

  separatorKeysCodes = [ENTER, COMMA, SEMICOLON];

  @Input()
  expanded = false;

  @Input()
  deviceProfileId: EntityId;

  private modelValue: DeviceProfileAlarm;

  alarmFormGroup: FormGroup;

  private propagateChange = null;
  private propagateChangePending = false;

  constructor(private dialog: MatDialog,
              private fb: FormBuilder) {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
    if (this.propagateChangePending) {
      this.propagateChangePending = false;
      setTimeout(() => {
        this.propagateChange(this.modelValue);
      }, 0);
    }
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.alarmFormGroup = this.fb.group({
      id: [null, Validators.required],
      alarmType: [null, Validators.required],
      createRules: [null],
      clearRule: [null],
      propagate: [null],
      propagateRelationTypes: [null]
    }, { validators: deviceProfileAlarmValidator });
    this.alarmFormGroup.valueChanges.subscribe(() => {
      this.updateModel();
    });
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.alarmFormGroup.disable({emitEvent: false});
    } else {
      this.alarmFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: DeviceProfileAlarm): void {
    this.propagateChangePending = false;
    this.modelValue = value;
    if (!this.modelValue.alarmType) {
      this.expanded = true;
    }
    this.alarmFormGroup.reset(this.modelValue || undefined, {emitEvent: false});
    if (!this.disabled && !this.alarmFormGroup.valid) {
      this.updateModel();
    }
  }

  public addClearAlarmRule() {
    const clearAlarmRule: AlarmRule = {
      condition: {
        condition: []
      }
    };
    this.alarmFormGroup.patchValue({clearRule: clearAlarmRule});
  }

  public removeClearAlarmRule() {
    this.alarmFormGroup.patchValue({clearRule: null});
  }

  public validate(c: FormControl) {
    if (c.parent) {
      const alarmType = c.value.alarmType;
      const profileAlarmsType = [];
      c.parent.getRawValue().forEach((alarm: DeviceProfileAlarm) => {
          profileAlarmsType.push(alarm.alarmType);
        }
      );
      if (profileAlarmsType.filter(profileAlarmType => profileAlarmType === alarmType).length > 1) {
        this.alarmFormGroup.get('alarmType').setErrors({
          unique: true
        });
      }
    }
    return (this.alarmFormGroup.valid) ? null : {
      alarm: {
        valid: false,
      },
    };
  }

  removeRelationType(key: string): void {
    const keys: string[] = this.alarmFormGroup.get('propagateRelationTypes').value;
    const index = keys.indexOf(key);
    if (index >= 0) {
      keys.splice(index, 1);
      this.alarmFormGroup.get('propagateRelationTypes').setValue(keys, {emitEvent: true});
    }
  }

  addRelationType(event: MatChipInputEvent): void {
    const input = event.input;
    let value = event.value;
    if ((value || '').trim()) {
      value = value.trim();
      let keys: string[] = this.alarmFormGroup.get('propagateRelationTypes').value;
      if (!keys || keys.indexOf(value) === -1) {
        if (!keys) {
          keys = [];
        }
        keys.push(value);
        this.alarmFormGroup.get('propagateRelationTypes').setValue(keys, {emitEvent: true});
      }
    }
    if (input) {
      input.value = '';
    }
  }


  private updateModel() {
    const value = this.alarmFormGroup.value;
    this.modelValue = {...this.modelValue, ...value};
    if (this.propagateChange) {
      this.propagateChange(this.modelValue);
    } else {
      this.propagateChangePending = true;
    }
  }
}
