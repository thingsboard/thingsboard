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
import { isDefinedAndNotNull } from '@core/utils';
import { takeUntil } from 'rxjs/operators';
import { IntegrationForm } from '@home/components/integration/configuration/integration-form';
import { RabbitMqIntegration } from '@shared/models/integration.models';
import { privateNetworkAddressValidator } from '@home/components/integration/integration.models';

@Component({
    selector: 'tb-rabbit-mq-integration-form',
    templateUrl: './rabbit-mq-integration-form.component.html',
    styleUrls: [],
    providers: [{
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => RabbitMqIntegrationFormComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => RabbitMqIntegrationFormComponent),
            multi: true,
        }],
    standalone: false
})
export class RabbitMqIntegrationFormComponent extends IntegrationForm implements ControlValueAccessor, Validator {

  rabbitMqIntegrationConfigForm: UntypedFormGroup;

  private propagateChange = (v: any) => { };

  constructor(private fb: UntypedFormBuilder) {
    super();
    this.rabbitMqIntegrationConfigForm = this.fb.group({
      exchangeName: ['', []],
      host: ['', [Validators.required]],
      port: [5672, [Validators.required, Validators.min(1), Validators.max(65535)]],
      virtualHost: ['', []],
      username: ['', []],
      password: ['', []],
      downlinkTopic: ['', []],
      queues: ['my-queue', [Validators.required]],
      connectionTimeout: [60000, [Validators.min(0)]],
      handshakeTimeout: [10000, [Validators.min(0)]],
      pollPeriod: [5000, [Validators.min(0)]],
      durable: [false, []],
      exclusive: [true, []],
      autoDelete: [true, []],
    });
    this.rabbitMqIntegrationConfigForm.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.updateModels(this.rabbitMqIntegrationConfigForm.getRawValue());
    });
  }

  writeValue(value: RabbitMqIntegration) {
    if (isDefinedAndNotNull(value?.clientConfiguration)) {
      this.rabbitMqIntegrationConfigForm.reset(value.clientConfiguration, {emitEvent: false});
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any) { }

  setDisabledState(isDisabled: boolean) {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.rabbitMqIntegrationConfigForm.disable({emitEvent: false});
    } else {
      this.rabbitMqIntegrationConfigForm.enable({emitEvent: false});
    }
  }

  private updateModels(value) {
    this.propagateChange({clientConfiguration: value});
  }

  validate(): ValidationErrors | null {
    return this.rabbitMqIntegrationConfigForm.valid ? null : {
      rabbitMqIntegrationConfigForm: {valid: false}
    };
  }

  updatedValidationPrivateNetwork() {
    if (this.allowLocalNetwork) {
      this.rabbitMqIntegrationConfigForm.get('host').removeValidators(privateNetworkAddressValidator);
    } else {
      this.rabbitMqIntegrationConfigForm.get('host').addValidators(privateNetworkAddressValidator);
    }
    this.rabbitMqIntegrationConfigForm.get('host').updateValueAndValidity({emitEvent: false});
  }
}
