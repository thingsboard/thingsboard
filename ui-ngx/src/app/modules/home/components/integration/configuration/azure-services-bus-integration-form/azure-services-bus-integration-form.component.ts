// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, forwardRef, Input } from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { isDefinedAndNotNull } from '@core/utils';
import { takeUntil } from 'rxjs/operators';
import { IntegrationForm } from '@home/components/integration/configuration/integration-form';
import { AzureServicesBusIntegration } from '@shared/models/integration.models';

@Component({
    selector: 'tb-azure-services-bus-integration-form',
    templateUrl: './azure-services-bus-integration-form.component.html',
    styleUrls: [],
    providers: [{
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => AzureServicesBusIntegrationFormComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => AzureServicesBusIntegrationFormComponent),
            multi: true,
        }],
    standalone: false
})
export class AzureServicesBusIntegrationFormComponent extends IntegrationForm implements ControlValueAccessor, Validator{

  downlinkConverter: boolean;
  @Input()
  set isSetDownlink(value: boolean) {
    if (this.downlinkConverter !== value) {
      this.downlinkConverter = value;
      this.downlinkConverterChanged();
    }
  }

  get isSetDownLink(): boolean {
    return this.downlinkConverter;
  }

  azureServicesBusIntegrationConfigForm = this.fb.group({
    connectionString: ['', [Validators.required]],
    topicName: ['', [Validators.required]],
    subName: ['', Validators.required],
    downlinkConnectionString: [''],
    downlinkTopicName: ['']
  });

  private propagateChange = (v: any) => { };

  constructor(private fb: FormBuilder) {
    super();
    this.azureServicesBusIntegrationConfigForm.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.updateModels(this.azureServicesBusIntegrationConfigForm.getRawValue());
    });
  }

  writeValue(value: AzureServicesBusIntegration) {
    if (isDefinedAndNotNull(value?.clientConfiguration)) {
      this.azureServicesBusIntegrationConfigForm.reset(value.clientConfiguration, {emitEvent: false});
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any) { }

  setDisabledState(isDisabled: boolean) {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.azureServicesBusIntegrationConfigForm.disable({emitEvent: false});
    } else {
      this.azureServicesBusIntegrationConfigForm.enable({emitEvent: false});
    }
  }

  private updateModels(value) {
    this.propagateChange({clientConfiguration: value});
  }

  validate(): ValidationErrors | null {
    return this.azureServicesBusIntegrationConfigForm.valid ? null : {
      azureEventHubIntegrationConfigForm: {valid: false}
    };
  }

  private downlinkConverterChanged() {
    if (this.azureServicesBusIntegrationConfigForm) {
      if (this.isSetDownLink) {
        this.azureServicesBusIntegrationConfigForm.get('downlinkConnectionString').setValidators(Validators.required);
        this.azureServicesBusIntegrationConfigForm.get('downlinkTopicName').setValidators(Validators.required);
      } else {
        this.azureServicesBusIntegrationConfigForm.get('downlinkConnectionString').setValidators([]);
        this.azureServicesBusIntegrationConfigForm.get('downlinkTopicName').setValidators([]);
      }
      this.azureServicesBusIntegrationConfigForm.get('downlinkConnectionString').updateValueAndValidity();
      this.azureServicesBusIntegrationConfigForm.get('downlinkTopicName').updateValueAndValidity();
    }
  }
}
