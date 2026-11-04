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
import { PubSubIntegration } from '@shared/models/integration.models';

@Component({
    selector: 'tb-pubsub-integration-form',
    templateUrl: './pubsub-integration-form.component.html',
    styleUrls: ['./pubsub-integration-form.component.scss'],
    providers: [{
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => PubSubIntegrationFormComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => PubSubIntegrationFormComponent),
            multi: true,
        }],
    standalone: false
})
export class PubSubIntegrationFormComponent extends IntegrationForm implements ControlValueAccessor, Validator {

  pubSubIntegrationConfigForm: UntypedFormGroup;

  private propagateChange = (v: any) => { };

  constructor(private fb: UntypedFormBuilder) {
    super();
    this.pubSubIntegrationConfigForm = this.fb.group({
      projectId: [null, Validators.required],
      subscriptionId: [null, Validators.required],
      serviceAccountKey: [null, Validators.required],
      serviceAccountKeyFileName: [null]
    });
    this.pubSubIntegrationConfigForm.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.updateModels(this.pubSubIntegrationConfigForm.getRawValue());
    });
  }

  writeValue(value: PubSubIntegration) {
    if (isDefinedAndNotNull(value?.clientConfiguration)) {
      this.pubSubIntegrationConfigForm.reset(value.clientConfiguration, {emitEvent: false});
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any) { }

  setDisabledState(isDisabled: boolean) {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.pubSubIntegrationConfigForm.disable({emitEvent: false});
    } else {
      this.pubSubIntegrationConfigForm.enable({emitEvent: false});
    }
  }

  private updateModels(value) {
    this.propagateChange({clientConfiguration: value});
  }

  validate(): ValidationErrors | null {
    return this.pubSubIntegrationConfigForm.valid ? null : {
      pubSubIntegrationConfigForm: {valid: false}
    };
  }
}
