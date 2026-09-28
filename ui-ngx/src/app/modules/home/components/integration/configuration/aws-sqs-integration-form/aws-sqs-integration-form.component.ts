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
import { AwsSqsIntegration } from '@shared/models/integration.models';
import { privateNetworkAddressValidator } from '@home/components/integration/integration.models';

@Component({
    selector: 'tb-aws-sqs-integration-form',
    templateUrl: './aws-sqs-integration-form.component.html',
    styleUrls: [],
    providers: [{
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => AwsSqsIntegrationFormComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => AwsSqsIntegrationFormComponent),
            multi: true,
        }],
    standalone: false
})
export class AwsSqsIntegrationFormComponent extends IntegrationForm implements ControlValueAccessor, Validator {

  awsSqsIntegrationConfigForm: UntypedFormGroup;

  private propagateChange = (v: any) => { };

  constructor(private fb: UntypedFormBuilder) {
    super();
    this.awsSqsIntegrationConfigForm = this.fb.group({
      queueUrl: ['', [Validators.required]],
      pollingPeriodSeconds: [5, [Validators.required, Validators.min(1)]],
      region: ['us-west-2', [Validators.required]],
      accessKeyId: ['', [Validators.required]],
      secretAccessKey: ['', [Validators.required]]
    });
    this.awsSqsIntegrationConfigForm.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.updateModels(this.awsSqsIntegrationConfigForm.getRawValue());
    });
  }

  writeValue(value: AwsSqsIntegration) {
    if (isDefinedAndNotNull(value?.sqsConfiguration)) {
      this.awsSqsIntegrationConfigForm.reset(value.sqsConfiguration, {emitEvent: false});
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any) { }

  setDisabledState(isDisabled: boolean) {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.awsSqsIntegrationConfigForm.disable({emitEvent: false});
    } else {
      this.awsSqsIntegrationConfigForm.enable({emitEvent: false});
    }
  }

  private updateModels(value) {
    this.propagateChange({sqsConfiguration: value});
  }

  validate(): ValidationErrors | null {
    return this.awsSqsIntegrationConfigForm.valid ? null : {
      awsSqsIntegrationConfigForm: {valid: false}
    };
  }

  updatedValidationPrivateNetwork() {
    if (this.allowLocalNetwork) {
      this.awsSqsIntegrationConfigForm.get('queueUrl').removeValidators(privateNetworkAddressValidator);
    } else {
      this.awsSqsIntegrationConfigForm.get('queueUrl').addValidators(privateNetworkAddressValidator);
    }
    this.awsSqsIntegrationConfigForm.get('queueUrl').updateValueAndValidity({emitEvent: false});
  }
}
