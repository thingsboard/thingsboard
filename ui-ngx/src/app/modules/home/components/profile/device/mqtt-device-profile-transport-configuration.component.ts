///
/// Copyright © 2016-2021 The Thingsboard Authors
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

import { Component, forwardRef, Input, OnDestroy, OnInit } from '@angular/core';
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
  defaultAttributesSchema,
  defaultRpcRequestSchema,
  defaultRpcResponseSchema,
  defaultTelemetrySchema,
  DeviceProfileTransportConfiguration,
  DeviceTransportType,
  MqttDeviceProfileTransportConfiguration,
  TransportPayloadType,
  transportPayloadTypeTranslationMap
} from '@shared/models/device.models';
import { isDefinedAndNotNull } from '@core/utils';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

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
export class MqttDeviceProfileTransportConfigurationComponent implements ControlValueAccessor, OnInit, OnDestroy {

  transportPayloadTypes = Object.keys(TransportPayloadType);

  transportPayloadTypeTranslations = transportPayloadTypeTranslationMap;

  mqttDeviceProfileTransportConfigurationFormGroup: FormGroup;

  private destroy$ = new Subject();
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
        deviceAttributesTopic: [null, [Validators.required, this.validationMQTTTopic()]],
        deviceTelemetryTopic: [null, [Validators.required, this.validationMQTTTopic()]],
        transportPayloadTypeConfiguration: this.fb.group({
          transportPayloadType: [TransportPayloadType.JSON, Validators.required],
          deviceTelemetryProtoSchema: [defaultTelemetrySchema, Validators.required],
          deviceAttributesProtoSchema: [defaultAttributesSchema, Validators.required],
          deviceRpcRequestProtoSchema: [defaultRpcRequestSchema, Validators.required],
          deviceRpcResponseProtoSchema: [defaultRpcResponseSchema, Validators.required],
          enableCompatibilityWithJsonPayloadFormat: [false, Validators.required],
          useJsonPayloadFormatForDefaultDownlinkTopics: [false, Validators.required]
        })
      }, {validator: this.uniqueDeviceTopicValidator}
    );
    this.mqttDeviceProfileTransportConfigurationFormGroup.get('transportPayloadTypeConfiguration.transportPayloadType').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(payloadType => {
      this.updateTransportPayloadBasedControls(payloadType, true);
    });
    this.mqttDeviceProfileTransportConfigurationFormGroup.get('transportPayloadTypeConfiguration.enableCompatibilityWithJsonPayloadFormat')
      .valueChanges.pipe(takeUntil(this.destroy$)
    ).subscribe(compatibilityWithJsonPayloadFormatEnabled => {
      if (!compatibilityWithJsonPayloadFormatEnabled) {
        this.mqttDeviceProfileTransportConfigurationFormGroup.get('transportPayloadTypeConfiguration.useJsonPayloadFormatForDefaultDownlinkTopics')
          .patchValue(false, {emitEvent: false});
      }
    });
    this.mqttDeviceProfileTransportConfigurationFormGroup.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.updateModel();
    });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.mqttDeviceProfileTransportConfigurationFormGroup.disable({emitEvent: false});
    } else {
      this.mqttDeviceProfileTransportConfigurationFormGroup.enable({emitEvent: false});
    }
  }

  get protoPayloadType(): boolean {
    const transportPayloadType = this.mqttDeviceProfileTransportConfigurationFormGroup.get('transportPayloadTypeConfiguration.transportPayloadType').value;
    return transportPayloadType === TransportPayloadType.PROTOBUF;
  }

  get compatibilityWithJsonPayloadFormatEnabled(): boolean {
    return this.mqttDeviceProfileTransportConfigurationFormGroup.get('transportPayloadTypeConfiguration.enableCompatibilityWithJsonPayloadFormat').value;
  }

  writeValue(value: MqttDeviceProfileTransportConfiguration | null): void {
    if (isDefinedAndNotNull(value)) {
      this.mqttDeviceProfileTransportConfigurationFormGroup.patchValue(value, {emitEvent: false});
      this.updateTransportPayloadBasedControls(value.transportPayloadTypeConfiguration?.transportPayloadType);
    }
  }

  private updateModel() {
    let configuration: DeviceProfileTransportConfiguration = null;
    if (this.mqttDeviceProfileTransportConfigurationFormGroup.valid) {
      configuration = this.mqttDeviceProfileTransportConfigurationFormGroup.value;
      configuration.type = DeviceTransportType.MQTT;
    }
    this.propagateChange(configuration);
  }

  private updateTransportPayloadBasedControls(type: TransportPayloadType, forceUpdated = false) {
    const transportPayloadTypeForm = this.mqttDeviceProfileTransportConfigurationFormGroup
      .get('transportPayloadTypeConfiguration') as FormGroup;
    if (forceUpdated) {
      transportPayloadTypeForm.patchValue({
        deviceTelemetryProtoSchema: defaultTelemetrySchema,
        deviceAttributesProtoSchema: defaultAttributesSchema,
        deviceRpcRequestProtoSchema: defaultRpcRequestSchema,
        deviceRpcResponseProtoSchema: defaultRpcResponseSchema,
        enableCompatibilityWithJsonPayloadFormat: false,
        useJsonPayloadFormatForDefaultDownlinkTopics: false
      }, {emitEvent: false});
    }
    if (type === TransportPayloadType.PROTOBUF && !this.disabled) {
      transportPayloadTypeForm.get('deviceTelemetryProtoSchema').enable({emitEvent: false});
      transportPayloadTypeForm.get('deviceAttributesProtoSchema').enable({emitEvent: false});
      transportPayloadTypeForm.get('deviceRpcRequestProtoSchema').enable({emitEvent: false});
      transportPayloadTypeForm.get('deviceRpcResponseProtoSchema').enable({emitEvent: false});
      transportPayloadTypeForm.get('enableCompatibilityWithJsonPayloadFormat').enable({emitEvent: false});
      transportPayloadTypeForm.get('useJsonPayloadFormatForDefaultDownlinkTopics').enable({emitEvent: false});
    } else {
      transportPayloadTypeForm.get('deviceTelemetryProtoSchema').disable({emitEvent: false});
      transportPayloadTypeForm.get('deviceAttributesProtoSchema').disable({emitEvent: false});
      transportPayloadTypeForm.get('deviceRpcRequestProtoSchema').disable({emitEvent: false});
      transportPayloadTypeForm.get('deviceRpcResponseProtoSchema').disable({emitEvent: false});
      transportPayloadTypeForm.get('enableCompatibilityWithJsonPayloadFormat').disable({emitEvent: false});
      transportPayloadTypeForm.get('useJsonPayloadFormatForDefaultDownlinkTopics').disable({emitEvent: false});
    }
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

  private uniqueDeviceTopicValidator(control: FormGroup): { [key: string]: boolean } | null {
    if (control.getRawValue()) {
      const formValue = control.getRawValue() as MqttDeviceProfileTransportConfiguration;
      if (formValue.deviceAttributesTopic === formValue.deviceTelemetryTopic) {
        return {unique: true};
      }
    }
    return null;
  }
}
