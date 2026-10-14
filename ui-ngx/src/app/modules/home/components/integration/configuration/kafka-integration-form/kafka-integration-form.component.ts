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
import { KafkaIntegration } from '@shared/models/integration.models';
import { privateNetworkAddressValidator } from '@home/components/integration/integration.models';

@Component({
    selector: 'tb-kafka-integration-form',
    templateUrl: './kafka-integration-form.component.html',
    styleUrls: [],
    providers: [{
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => KafkaIntegrationFormComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => KafkaIntegrationFormComponent),
            multi: true,
        }],
    standalone: false
})
export class KafkaIntegrationFormComponent extends IntegrationForm implements ControlValueAccessor, Validator {

  kafkaIntegrationConfigForm: UntypedFormGroup;

  private propagateChange = (v: any) => { };

  constructor(private fb: UntypedFormBuilder) {
    super();
    this.kafkaIntegrationConfigForm = this.fb.group({
      groupId: ['', [Validators.required]],
      clientId: ['', [Validators.required]],
      topics: ['my-topic-output', [Validators.required]],
      bootstrapServers: ['YOUR_KAFKA_DOMAIN:9092', [Validators.required]],
      pollInterval: [5000, [Validators.required]],
      autoCreateTopics: [false],
      otherProperties: [null]
    });
    this.kafkaIntegrationConfigForm.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.updateModels(this.kafkaIntegrationConfigForm.getRawValue());
    });
  }

  writeValue(value: KafkaIntegration) {
    if (isDefinedAndNotNull(value?.clientConfiguration)) {
      this.kafkaIntegrationConfigForm.reset(value.clientConfiguration, {emitEvent: false});
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any) { }

  setDisabledState(isDisabled: boolean) {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.kafkaIntegrationConfigForm.disable({emitEvent: false});
    } else {
      this.kafkaIntegrationConfigForm.enable({emitEvent: false});
    }
  }

  private updateModels(value) {
    this.propagateChange({clientConfiguration: value});
  }

  validate(): ValidationErrors | null {
    return this.kafkaIntegrationConfigForm.valid ? null : {
      kafkaIntegrationConfigForm: {valid: false}
    };
  }

  updatedValidationPrivateNetwork() {
    if (this.allowLocalNetwork) {
      this.kafkaIntegrationConfigForm.get('bootstrapServers').removeValidators(privateNetworkAddressValidator);
    } else {
      this.kafkaIntegrationConfigForm.get('bootstrapServers').addValidators(privateNetworkAddressValidator);
    }
    this.kafkaIntegrationConfigForm.get('bootstrapServers').updateValueAndValidity({emitEvent: false});
  }
}
