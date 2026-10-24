// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, forwardRef } from '@angular/core';
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
import { AwsIotIntegration, IntegrationCredentialType, MqttQos } from '@shared/models/integration.models';
import { DEFAULT_MQTT_VERSION } from '@shared/models/mqtt.models';

@Component({
    selector: 'tb-aws-iot-integration-form',
    templateUrl: './aws-iot-integration-form.component.html',
    styleUrls: ['./aws-iot-integration-form.component.scss'],
    providers: [{
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => AwsIotIntegrationFormComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => AwsIotIntegrationFormComponent),
            multi: true,
        }],
    standalone: false
})
export class AwsIotIntegrationFormComponent extends IntegrationForm implements ControlValueAccessor, Validator {

  awsIotIntegrationConfigForm: UntypedFormGroup;

  IntegrationCredentialType = IntegrationCredentialType;
  MqttQos = MqttQos;

  private propagateChange = (v: any) => { };

  constructor(private fb: UntypedFormBuilder) {
    super();
    this.awsIotIntegrationConfigForm = this.fb.group({
      clientConfiguration: this.fb.group({
        host: ['', Validators.required],
        connectTimeoutSec: [10, [Validators.required, Validators.min(1), Validators.max(200)]],
        clientId: [''],
        maxBytesInMessage: [32368, [Validators.min(1), Validators.max(256000000)]],
        protocolVersion: [DEFAULT_MQTT_VERSION],
        credentials: [{
          type: IntegrationCredentialType.CertPEM
        }],
      }),
      topicFilters: [[{
        filter: '#',
        qos: MqttQos.AT_MOST_ONE
      }], Validators.required],
      downlinkTopicPattern: ['${topic}', Validators.required]
    });
    this.awsIotIntegrationConfigForm.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => this.updateModels(this.awsIotIntegrationConfigForm.getRawValue()));
  }

  writeValue(value: AwsIotIntegration) {
    if (isDefinedAndNotNull(value)) {
      this.awsIotIntegrationConfigForm.reset(value, {emitEvent: false});
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any) { }

  setDisabledState(isDisabled: boolean) {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.awsIotIntegrationConfigForm.disable({emitEvent: false});
    } else {
      this.awsIotIntegrationConfigForm.enable({emitEvent: false});
    }
  }

  private updateModels(value) {
    this.propagateChange(value);
  }

  validate(): ValidationErrors | null {
    return this.awsIotIntegrationConfigForm.valid ? null : {
      aswIotIntegrationConfigForm: {valid: false}
    };
  }

  updatedValidationPrivateNetwork() {
    if (this.allowLocalNetwork) {
      this.awsIotIntegrationConfigForm.get('clientConfiguration.host').removeValidators(privateNetworkAddressValidator);
    } else {
      this.awsIotIntegrationConfigForm.get('clientConfiguration.host').addValidators(privateNetworkAddressValidator);
    }
    this.awsIotIntegrationConfigForm.get('clientConfiguration.host').updateValueAndValidity({emitEvent: false});
  }
}
