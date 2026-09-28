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
import { IntegrationForm } from '@home/components/integration/configuration/integration-form';
import { isDefinedAndNotNull } from '@core/utils';
import { takeUntil } from 'rxjs/operators';
import { ApachePulsarIntegration, IntegrationCredentialType } from '@shared/models/integration.models';
import { privateNetworkAddressValidator } from '@home/components/integration/integration.models';

@Component({
    selector: 'tb-apache-pulsar-integration-form',
    templateUrl: './apache-pulsar-integration-form.component.html',
    styleUrls: [],
    providers: [{
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => ApachePulsarIntegrationFormComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => ApachePulsarIntegrationFormComponent),
            multi: true,
        }],
    standalone: false
})
export class ApachePulsarIntegrationFormComponent extends IntegrationForm implements ControlValueAccessor, Validator {

  apachePulsarIntegrationConfigForm: UntypedFormGroup;

  IntegrationCredentialType = IntegrationCredentialType;

  private propagateChangePending = false;
  private propagateChange = (v: any) => { };

  constructor(private fb: UntypedFormBuilder) {
    super();
    this.apachePulsarIntegrationConfigForm = this.fb.group({
      serviceUrl: ['pulsar://YOUR_PULSAR_DOMAIN:6650', Validators.required],
      topics: ['my-topic', Validators.required],
      subscriptionName: ['my-subscription', Validators.required],
      maxNumMessages: [1000, Validators.required],
      maxNumBytes: [10 * 1024 * 1024, Validators.required],
      timeoutInMs: [100, Validators.required],
      credentials: [{
        type: IntegrationCredentialType.Anonymous
      }]
    });

    this.apachePulsarIntegrationConfigForm.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.updateModels(this.apachePulsarIntegrationConfigForm.getRawValue());
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
    if (this.propagateChangePending) {
      this.propagateChangePending = false;
      setTimeout(() => {
        this.updateModels(this.apachePulsarIntegrationConfigForm.getRawValue());
      }, 0);
    }
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.apachePulsarIntegrationConfigForm.disable({emitEvent: false});
    } else {
      this.apachePulsarIntegrationConfigForm.enable({emitEvent: false});
    }
  }

  validate(): ValidationErrors | null {
    return this.apachePulsarIntegrationConfigForm.valid ? null : {
      apachePulsarIntegrationConfigForm: {valid: false}
    };
  }

  writeValue(value: ApachePulsarIntegration) {
    if (isDefinedAndNotNull(value?.clientConfiguration)) {
      this.apachePulsarIntegrationConfigForm.reset(value.clientConfiguration, {emitEvent: false});
    } else {
      this.propagateChangePending = true;
    }
  }

  private updateModels(value) {
    this.propagateChange({clientConfiguration: value});
  }

  updatedValidationPrivateNetwork() {
    if (this.allowLocalNetwork) {
      this.apachePulsarIntegrationConfigForm.get('serviceUrl').removeValidators(privateNetworkAddressValidator);
    } else {
      this.apachePulsarIntegrationConfigForm.get('serviceUrl').addValidators(privateNetworkAddressValidator);
    }
    this.apachePulsarIntegrationConfigForm.get('serviceUrl').updateValueAndValidity({emitEvent: false});
  }
}
