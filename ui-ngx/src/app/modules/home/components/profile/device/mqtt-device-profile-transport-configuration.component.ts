///
/// Copyright © 2016-2026 The Thingsboard Authors
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
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  ValidationErrors,
  Validator,
  ValidatorFn,
  Validators
} from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import {
  defaultAttributesSchema,
  defaultRpcRequestSchema,
  defaultRpcResponseSchema,
  defaultTelemetrySchema,
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
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => MqttDeviceProfileTransportConfigurationComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => MqttDeviceProfileTransportConfigurationComponent),
      multi: true
    }
  ]
})
export class MqttDeviceProfileTransportConfigurationComponent implements ControlValueAccessor, OnInit, OnDestroy, Validator {

  transportPayloadTypes = Object.keys(TransportPayloadType);

  transportPayloadTypeTranslations = transportPayloadTypeTranslationMap;

  mqttDeviceProfileTransportConfigurationFormGroup: UntypedFormGroup;

  private destroy$ = new Subject<void>();

  @Input()
  disabled: boolean;

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              private fb: UntypedFormBuilder) {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.mqttDeviceProfileTransportConfigurationFormGroup = this.fb.group({
        deviceAttributesTopic: [null, [Validators.required, this.validationMQTTTopic()]],
        deviceAttributesSubscribeTopic: [null, [Validators.required, this.validationMQTTTopic()]],
        deviceTelemetryTopic: [null, [Validators.required, this.validationMQTTTopic()]],
        sparkplug: [false],
        sparkplugAttributesMetricNames: [null],
        sendAckOnValidationException: [false, Validators.required],
        transportPayloadTypeConfiguration: this.fb.group({
          transportPayloadType: [TransportPayloadType.JSON, Validators.required],
          deviceTelemetryProtoSchema: [defaultTelemetrySchema, Validators.required],
          deviceAttributesProtoSchema: [defaultAttributesSchema, Validators.required],
          deviceRpcRequestProtoSchema: [defaultRpcRequestSchema, Validators.required],
          deviceRpcResponseProtoSchema: [defaultRpcResponseSchema, Validators.required],
          enableCompatibilityWithJsonPayloadFormat: [false, Validators.required],
          useJsonPayloadFormatForDefaultDownlinkTopics: [false, Validators.required]
        })
      }, {validators: this.uniqueDeviceTopicValidator}
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
    this.mqttDeviceProfileTransportConfigurationFormGroup.get('sparkplug').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value) => {
      if (value) {
        this.mqttDeviceProfileTransportConfigurationFormGroup.disable({emitEvent: false});
        this.mqttDeviceProfileTransportConfigurationFormGroup.get('sparkplug').enable({emitEvent: false});
        this.mqttDeviceProfileTransportConfigurationFormGroup.get('sparkplugAttributesMetricNames').enable({emitEvent: false});
      } else {
        this.mqttDeviceProfileTransportConfigurationFormGroup.enable({emitEvent: false});
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
      this.mqttDeviceProfileTransportConfigurationFormGroup.get('sparkplug').updateValueAndValidity({onlySelf: true});
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
      if (!this.disabled) {
        this.mqttDeviceProfileTransportConfigurationFormGroup.get('sparkplug').updateValueAndValidity({onlySelf: true});
      }
    }
  }

  public validate(c: UntypedFormControl): ValidationErrors | null {
    return (this.mqttDeviceProfileTransportConfigurationFormGroup.valid) ? null : {
      valid: false,
    };
  }

  private updateModel() {
    const configuration = this.mqttDeviceProfileTransportConfigurationFormGroup.getRawValue();
    configuration.type = DeviceTransportType.MQTT;
    this.propagateChange(configuration);
  }

  private updateTransportPayloadBasedControls(type: TransportPayloadType, forceUpdated = false) {
    const transportPayloadTypeForm = this.mqttDeviceProfileTransportConfigurationFormGroup
      .get('transportPayloadTypeConfiguration') as UntypedFormGroup;
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
    return (c: UntypedFormControl) => {
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

  private uniqueDeviceTopicValidator(control: UntypedFormGroup): { [key: string]: boolean } | null {
    if (control.getRawValue()) {
      const formValue = control.getRawValue() as MqttDeviceProfileTransportConfiguration;
      if (formValue.deviceAttributesTopic === formValue.deviceTelemetryTopic) {
        return {unique: true};
      }
    }
    return null;
  }
}
