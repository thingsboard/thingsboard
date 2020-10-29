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
  NG_VALUE_ACCESSOR,
  ValidatorFn,
  Validators
} from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import {
  DeviceProfileTransportConfiguration,
  DeviceTransportType,
  MqttDeviceProfileTransportConfiguration,
  MqttTransportPayloadType,
  mqttTransportPayloadTypeTranslationMap
} from '@shared/models/device.models';
import {isDefinedAndNotNull} from '@core/utils';

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

  mqttTransportPayloadTypes = Object.keys(MqttTransportPayloadType);

  mqttTransportPayloadTypeTranslations = mqttTransportPayloadTypeTranslationMap;


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
        deviceAttributesTopic: [null, [Validators.required, this.validationMQTTTopic()]],
        deviceTelemetryTopic: [null, [Validators.required, this.validationMQTTTopic()]],
        transportPayloadType: [MqttTransportPayloadType.JSON, Validators.required],
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

  protoPayloadType(): boolean {
    let configuration = this.mqttDeviceProfileTransportConfigurationFormGroup.getRawValue().configuration;
    return configuration.transportPayloadType === MqttTransportPayloadType.PROTOBUF;
  }

  writeValue(value: MqttDeviceProfileTransportConfiguration | null): void {
    if (isDefinedAndNotNull(value)) {
      let configurationFormGroup = this.mqttDeviceProfileTransportConfigurationFormGroup.controls.configuration as FormGroup;
      let payloadType = value.transportPayloadType;
      if (payloadType ===  MqttTransportPayloadType.PROTOBUF) {
        configurationFormGroup.registerControl('deviceTelemetryProtoSchema', this.fb.control(null, Validators.required));
        configurationFormGroup.registerControl('deviceAttributesProtoSchema', this.fb.control(null, Validators.required));
      }
      this.mqttDeviceProfileTransportConfigurationFormGroup.patchValue({configuration: value}, {emitEvent: false});
    }
  }

  private updateModel() {
    let configuration: DeviceProfileTransportConfiguration = null;
    if (this.mqttDeviceProfileTransportConfigurationFormGroup.valid) {
      configuration = this.mqttDeviceProfileTransportConfigurationFormGroup.getRawValue().configuration;
      configuration.type = DeviceTransportType.MQTT;
      let configurationFormGroup = this.mqttDeviceProfileTransportConfigurationFormGroup.controls.configuration as FormGroup;
      let transportPayloadType = configuration.transportPayloadType;
      if (transportPayloadType ===  MqttTransportPayloadType.PROTOBUF) {
        configurationFormGroup.registerControl('deviceTelemetryProtoSchema', this.fb.control(null, Validators.required));
        configurationFormGroup.registerControl('deviceAttributesProtoSchema', this.fb.control(null, Validators.required));
      } else {
        configurationFormGroup.removeControl('deviceTelemetryProtoSchema');
        configurationFormGroup.removeControl('deviceAttributesProtoSchema');
      }
    }
    this.propagateChange(configuration);
  }

  private validationMQTTTopic(): ValidatorFn {
    return (c: FormControl) => {
      const newTopic = c.value;
      const wildcardSymbols = /[#+]/g;
      let findSymbol = wildcardSymbols.exec(newTopic);
      while (findSymbol) {
        const index = findSymbol.index;
        const currentSymbol = findSymbol[0];
        const prevSymbol = index > 0 ? newTopic[index - 1] : null;
        const nextSymbol = index < (newTopic.length - 1) ? newTopic[index + 1] : null;
        if (currentSymbol === '#' && (index !== (newTopic.length - 1) || (prevSymbol !== null && prevSymbol !== '/'))) {
          return {
            invalidMultiTopicCharacter: {
              valid: false
            }
          };
        }
        if (currentSymbol === '+' && ((prevSymbol !== null && prevSymbol !== '/') || (nextSymbol !== null && nextSymbol !== '/'))) {
          return {
            invalidSingleTopicCharacter: {
              valid: false
            }
          };
        }
        findSymbol = wildcardSymbols.exec(newTopic);
      }
      return null;
    };
  }
}
