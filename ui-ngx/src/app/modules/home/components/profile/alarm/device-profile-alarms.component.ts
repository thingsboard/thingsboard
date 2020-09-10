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
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { DeviceProfileAlarm } from '@shared/models/device.models';
import { guid } from '@core/utils';
import { Subscription } from 'rxjs';
import { MatDialog } from '@angular/material/dialog';

@Component({
  selector: 'tb-device-profile-alarms',
  templateUrl: './device-profile-alarms.component.html',
  styleUrls: ['./device-profile-alarms.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => DeviceProfileAlarmsComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => DeviceProfileAlarmsComponent),
      multi: true,
    }
  ]
})
export class DeviceProfileAlarmsComponent implements ControlValueAccessor, OnInit, Validator {

  deviceProfileAlarmsFormGroup: FormGroup;

  private requiredValue: boolean;
  get required(): boolean {
    return this.requiredValue;
  }
  @Input()
  set required(value: boolean) {
    this.requiredValue = coerceBooleanProperty(value);
  }

  @Input()
  disabled: boolean;

  private valueChangeSubscription: Subscription = null;

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              private fb: FormBuilder,
              private dialog: MatDialog) {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.deviceProfileAlarmsFormGroup = this.fb.group({
      alarms: this.fb.array([])
    });
  }

  alarmsFormArray(): FormArray {
    return this.deviceProfileAlarmsFormGroup.get('alarms') as FormArray;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.deviceProfileAlarmsFormGroup.disable({emitEvent: false});
    } else {
      this.deviceProfileAlarmsFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(alarms: Array<DeviceProfileAlarm> | null): void {
    if (this.valueChangeSubscription) {
      this.valueChangeSubscription.unsubscribe();
    }
    const alarmsControls: Array<AbstractControl> = [];
    if (alarms) {
      alarms.forEach((alarm) => {
        alarmsControls.push(this.fb.control(alarm, [Validators.required]));
      });
    }
    this.deviceProfileAlarmsFormGroup.setControl('alarms', this.fb.array(alarmsControls));
    if (this.disabled) {
      this.deviceProfileAlarmsFormGroup.disable({emitEvent: false});
    } else {
      this.deviceProfileAlarmsFormGroup.enable({emitEvent: false});
    }
    this.valueChangeSubscription = this.deviceProfileAlarmsFormGroup.valueChanges.subscribe(() => {
      this.updateModel();
    });
  }

  public trackByAlarm(index: number, alarmControl: AbstractControl): string {
    if (alarmControl) {
      return alarmControl.value.id;
    } else {
      return null;
    }
  }

  public removeAlarm(index: number) {
    (this.deviceProfileAlarmsFormGroup.get('alarms') as FormArray).removeAt(index);
  }

  public addAlarm() {
    const alarm: DeviceProfileAlarm = {
      id: guid(),
      alarmType: '',
      createRules: {
        empty: {
          condition: {
            condition: []
          }
        }
      }
    };
    const alarmsArray = this.deviceProfileAlarmsFormGroup.get('alarms') as FormArray;
    alarmsArray.push(this.fb.control(alarm, [Validators.required]));
    this.deviceProfileAlarmsFormGroup.updateValueAndValidity();
  }

  public validate(c: FormControl) {
    return (this.deviceProfileAlarmsFormGroup.valid) ? null : {
      alarms: {
        valid: false,
      },
    };
  }

  private updateModel() {
//    if (this.deviceProfileAlarmsFormGroup.valid) {
      const alarms: Array<DeviceProfileAlarm> = this.deviceProfileAlarmsFormGroup.get('alarms').value;
      this.propagateChange(alarms);
  /*  } else {
      this.propagateChange(null);
    } */
  }
}
