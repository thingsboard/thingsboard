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
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import {
  DeviceProfileData,
  DeviceProfileType,
  deviceProfileTypeConfigurationInfoMap,
  DeviceTransportType, deviceTransportTypeConfigurationInfoMap
} from '@shared/models/device.models';

@Component({
  selector: 'tb-device-profile-data',
  templateUrl: './device-profile-data.component.html',
  styleUrls: [],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => DeviceProfileDataComponent),
    multi: true
  }]
})
export class DeviceProfileDataComponent implements ControlValueAccessor, OnInit {

  deviceProfileDataFormGroup: FormGroup;

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

  displayProfileConfiguration: boolean;
  displayTransportConfiguration: boolean;

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              private fb: FormBuilder) {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.deviceProfileDataFormGroup = this.fb.group({
      configuration: [null, Validators.required],
      transportConfiguration: [null, Validators.required],
      alarms: [null],
      provisionConfiguration: [null]
    });
    this.deviceProfileDataFormGroup.valueChanges.subscribe(() => {
      this.updateModel();
    });
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.deviceProfileDataFormGroup.disable({emitEvent: false});
    } else {
      this.deviceProfileDataFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: DeviceProfileData | null): void {
    const deviceProfileType = value?.configuration?.type;
    this.displayProfileConfiguration = deviceProfileType &&
      deviceProfileTypeConfigurationInfoMap.get(deviceProfileType).hasProfileConfiguration;
    const deviceTransportType = value?.transportConfiguration?.type;
    this.displayTransportConfiguration = deviceTransportType &&
      deviceTransportTypeConfigurationInfoMap.get(deviceTransportType).hasProfileConfiguration;
    this.deviceProfileDataFormGroup.patchValue({configuration: value?.configuration}, {emitEvent: false});
    this.deviceProfileDataFormGroup.patchValue({transportConfiguration: value?.transportConfiguration}, {emitEvent: false});
    this.deviceProfileDataFormGroup.patchValue({alarms: value?.alarms}, {emitEvent: false});
    this.deviceProfileDataFormGroup.patchValue({provisionConfiguration: value?.provisionConfiguration}, {emitEvent: false});
  }

  private updateModel() {
    let deviceProfileData: DeviceProfileData = null;
    if (this.deviceProfileDataFormGroup.valid) {
      deviceProfileData = this.deviceProfileDataFormGroup.getRawValue();
    }
    this.propagateChange(deviceProfileData);
  }
}
