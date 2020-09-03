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
import { DeviceProfileTransportConfiguration, DeviceTransportType } from '@shared/models/device.models';
import { deepClone } from '@core/utils';

@Component({
  selector: 'tb-device-profile-transport-configuration',
  templateUrl: './device-profile-transport-configuration.component.html',
  styleUrls: [],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => DeviceProfileTransportConfigurationComponent),
    multi: true
  }]
})
export class DeviceProfileTransportConfigurationComponent implements ControlValueAccessor, OnInit {

  deviceTransportType = DeviceTransportType;

  deviceProfileTransportConfigurationFormGroup: FormGroup;

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

  transportType: DeviceTransportType;

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
    this.deviceProfileTransportConfigurationFormGroup = this.fb.group({
      configuration: [null, Validators.required]
    });
    this.deviceProfileTransportConfigurationFormGroup.valueChanges.subscribe(() => {
      this.updateModel();
    });
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.deviceProfileTransportConfigurationFormGroup.disable({emitEvent: false});
    } else {
      this.deviceProfileTransportConfigurationFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: DeviceProfileTransportConfiguration | null): void {
    this.transportType = value?.type;
    const configuration = deepClone(value);
    if (configuration) {
      delete configuration.type;
    }
    this.deviceProfileTransportConfigurationFormGroup.patchValue({configuration}, {emitEvent: false});
  }

  private updateModel() {
    let configuration: DeviceProfileTransportConfiguration = null;
    if (this.deviceProfileTransportConfigurationFormGroup.valid) {
      configuration = this.deviceProfileTransportConfigurationFormGroup.getRawValue().configuration;
      configuration.type = this.transportType;
    }
    this.propagateChange(configuration);
  }
}
