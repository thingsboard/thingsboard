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
  DeviceProfileTransportConfiguration,
  DeviceTransportType, MqttDeviceProfileTransportConfiguration
} from '@shared/models/device.models';
import { isDefinedAndNotNull } from '../../../../../core/utils';

@Component({
  selector: 'tb-mqtt-device-profile-transport-configuration',
  templateUrl: './mqtt-device-profile-transport-configuration.component.html',
  styleUrls: ['./mqtt-device-profile-transport-configuration.component.scss'],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => MqttDeviceProfileTransportConfigurationComponent),
    multi: true
  }]
})
export class MqttDeviceProfileTransportConfigurationComponent implements ControlValueAccessor, OnInit {

  mqttDeviceProfileTransportConfigurationFormGroup: FormGroup;

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
    this.mqttDeviceProfileTransportConfigurationFormGroup = this.fb.group({
      configuration: this.fb.group({
        deviceAttributesTopic: [null, Validators.required],
        deviceTelemetryTopic: [null, Validators.required],
        deviceRpcRequestTopic: [null, Validators.required],
        deviceRpcResponseTopic: [null, Validators.required]
      })
    });
    this.mqttDeviceProfileTransportConfigurationFormGroup.valueChanges.subscribe(() => {
      this.updateModel();
    });
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.mqttDeviceProfileTransportConfigurationFormGroup.disable({emitEvent: false});
    } else {
      this.mqttDeviceProfileTransportConfigurationFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: MqttDeviceProfileTransportConfiguration | null): void {
    if (isDefinedAndNotNull(value)) {
      this.mqttDeviceProfileTransportConfigurationFormGroup.patchValue({configuration: value}, {emitEvent: false});
    }
  }

  private updateModel() {
    let configuration: DeviceProfileTransportConfiguration = null;
    if (this.mqttDeviceProfileTransportConfigurationFormGroup.valid) {
      configuration = this.mqttDeviceProfileTransportConfigurationFormGroup.getRawValue().configuration;
      configuration.type = DeviceTransportType.MQTT;
    }
    this.propagateChange(configuration);
  }
}
