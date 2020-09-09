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
  FormGroup, NG_VALIDATORS,
  NG_VALUE_ACCESSOR, Validator,
  Validators
} from '@angular/forms';
import { DeviceProfileAlarm } from '@shared/models/device.models';
import { deepClone } from '@core/utils';
import { MatDialog } from '@angular/material/dialog';
import {
  DeviceProfileAlarmDialogComponent,
  DeviceProfileAlarmDialogData
} from './device-profile-alarm-dialog.component';

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

  private modelValue: DeviceProfileAlarm;

  alarmFormGroup: FormGroup;

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
    this.alarmFormGroup = this.fb.group({
      id: [null, Validators.required],
      alarmType: [null, Validators.required],
      createRules: [null],
      clearRule: [null],
      propagate: [null],
      propagateRelationTypes: [null]
    });
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
    this.modelValue = value;
    this.alarmFormGroup.reset(this.modelValue, {emitEvent: false});
  }

/*  openAlarm($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<DeviceProfileAlarmDialogComponent, DeviceProfileAlarmDialogData,
      DeviceProfileAlarm>(DeviceProfileAlarmDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        isAdd: false,
        alarm: this.disabled ? this.modelValue : deepClone(this.modelValue),
        isReadOnly: this.disabled
      }
    }).afterClosed().subscribe(
      (deviceProfileAlarm) => {
        if (deviceProfileAlarm) {
          this.modelValue = deviceProfileAlarm;
          this.updateModel();
        }
      }
    );
  } */

  public validate(c: FormControl) {
    return (this.alarmFormGroup.valid) ? null : {
      alarm: {
        valid: false,
      },
    };
  }

  private updateModel() {
    if (this.alarmFormGroup.valid) {
      const value = this.alarmFormGroup.value;
      this.modelValue = {...this.modelValue, ...value};
      this.propagateChange(this.modelValue);
    } else {
      this.propagateChange(null);
    }
  }
}
