// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, forwardRef, Input, OnInit } from '@angular/core';
import {
  ControlValueAccessor,
  UntypedFormBuilder,
  UntypedFormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { privateNetworkAddressValidator } from '@home/components/integration/integration.models';
import { takeUntil } from 'rxjs/operators';
import { isDefinedAndNotNull } from '@core/utils';
import { IntegrationForm } from '@home/components/integration/configuration/integration-form';
import { IntegrationCredentialType, MqttIntegration } from '@shared/models/integration.models';
import { DEFAULT_MQTT_VERSION } from '@shared/models/mqtt.models';

@Component({
    selector: 'tb-mqtt-integration-form',
    templateUrl: './mqtt-integration-form.component.html',
    styleUrls: ['./mqtt-integration-form.component.scss'],
    providers: [{
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => MqttIntegrationFormComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => MqttIntegrationFormComponent),
            multi: true,
        }],
    standalone: false
})
export class MqttIntegrationFormComponent extends IntegrationForm implements OnInit, ControlValueAccessor, Validator {

  @Input() isEdgeTemplate = false;

  mqttIntegrationConfigForm: UntypedFormGroup;

  IntegrationCredentialType = IntegrationCredentialType;

  private propagateChange = (v: any) => { };

  constructor(private fb: UntypedFormBuilder) {
    super();
    this.mqttIntegrationConfigForm = this.fb.group({
      clientConfiguration: this.fb.group({
        host: ['', Validators.required],
        port: [1883, [Validators.min(1), Validators.max(65535)]],
        cleanSession: [true],
        retainedMessage: [false],
        ssl: [false],
        connectTimeoutSec: [10, [Validators.required, Validators.min(1), Validators.max(200)]],
        clientId: [''],
        maxBytesInMessage: [32368, [Validators.min(1), Validators.max(256000000)]],
        protocolVersion: [DEFAULT_MQTT_VERSION],
        credentials: [{
          type: IntegrationCredentialType.Anonymous
        }],
      }),
      topicFilters: [[{
        filter: '#',
        qos: 0
      }], Validators.required],
      downlinkTopicPattern: ['${topic}', Validators.required]
    });
    this.mqttIntegrationConfigForm.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => this.updateModels(this.mqttIntegrationConfigForm.getRawValue()));
  }

  ngOnInit() {
    if (this.isEdgeTemplate) {
      this.mqttIntegrationConfigForm.get('clientConfiguration.clientId').clearValidators();
      this.mqttIntegrationConfigForm.get('clientConfiguration.clientId').updateValueAndValidity({emitEvent: false});
    }
  }

  writeValue(value: MqttIntegration) {
    if (isDefinedAndNotNull(value)) {
      this.mqttIntegrationConfigForm.reset(value, {emitEvent: false});
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any) { }

  setDisabledState(isDisabled: boolean) {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.mqttIntegrationConfigForm.disable({emitEvent: false});
    } else {
      this.mqttIntegrationConfigForm.enable({emitEvent: false});
    }
  }

  private updateModels(value) {
    this.propagateChange(value);
  }

  validate(): ValidationErrors | null {
    return this.mqttIntegrationConfigForm.valid ? null : {
      mqttIntegrationConfigForm: {valid: false}
    };
  }

  updatedValidationPrivateNetwork() {
    if (this.allowLocalNetwork) {
      this.mqttIntegrationConfigForm.get('clientConfiguration.host').removeValidators(privateNetworkAddressValidator);
    } else {
      this.mqttIntegrationConfigForm.get('clientConfiguration.host').addValidators(privateNetworkAddressValidator);
    }
    this.mqttIntegrationConfigForm.get('clientConfiguration.host').updateValueAndValidity({emitEvent: false});
  }
}
