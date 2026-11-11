// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, forwardRef, Input } from '@angular/core';
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
import { AzureEventHubIntegration } from '@shared/models/integration.models';

@Component({
    selector: 'tb-azure-event-hub-integration-form',
    templateUrl: './azure-event-hub-integration-form.component.html',
    styleUrls: ['./azure-event-hub-integration-form.component.scss'],
    providers: [{
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => AzureEventHubIntegrationFormComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => AzureEventHubIntegrationFormComponent),
            multi: true,
        }],
    standalone: false
})
export class AzureEventHubIntegrationFormComponent extends IntegrationForm implements ControlValueAccessor, Validator{

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

  azureEventHubIntegrationConfigForm: UntypedFormGroup;

  private propagateChange = (v: any) => { };

  constructor(private fb: UntypedFormBuilder) {
    super();
    this.azureEventHubIntegrationConfigForm = this.fb.group({
      connectTimeoutSec: [10, [Validators.required, Validators.min(1), Validators.max(200)]],
      connectionString: ['', [Validators.required]],
      consumerGroup: [''],
      iotHubName: [''],
      storageConnectionString: [{value: '', disabled: true}, Validators.required],
      containerName: [{value: '', disabled: true}, Validators.required],
      enablePersistentCheckpoints: [false]
    });
    this.azureEventHubIntegrationConfigForm.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.updateModels(this.azureEventHubIntegrationConfigForm.getRawValue());
    });
    this.azureEventHubIntegrationConfigForm.get('enablePersistentCheckpoints').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.updateEnableFields();
    });
  }

  writeValue(value: AzureEventHubIntegration) {
    if (isDefinedAndNotNull(value?.clientConfiguration)) {
      this.azureEventHubIntegrationConfigForm.reset(value.clientConfiguration, {emitEvent: false});
      if (!this.disabled) {
        this.updateEnableFields();
      }
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any) { }

  setDisabledState(isDisabled: boolean) {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.azureEventHubIntegrationConfigForm.disable({emitEvent: false});
    } else {
      this.azureEventHubIntegrationConfigForm.enable({emitEvent: false});
      this.updateEnableFields();
    }
  }

  private updateModels(value) {
    this.propagateChange({clientConfiguration: value});
  }

  validate(): ValidationErrors | null {
    return this.azureEventHubIntegrationConfigForm.valid ? null : {
      azureEventHubIntegrationConfigForm: {valid: false}
    };
  }

  private updateEnableFields() {
    const enablePersistentCheckpoints = this.azureEventHubIntegrationConfigForm.get('enablePersistentCheckpoints').value;
    if (enablePersistentCheckpoints) {
      this.azureEventHubIntegrationConfigForm.get('storageConnectionString').enable({emitEvent: false});
      this.azureEventHubIntegrationConfigForm.get('containerName').enable({emitEvent: false});
    } else {
      this.azureEventHubIntegrationConfigForm.get('storageConnectionString').disable({emitEvent: false});
      this.azureEventHubIntegrationConfigForm.get('containerName').disable({emitEvent: false});
    }
  }

  private downlinkConverterChanged() {
    if (this.azureEventHubIntegrationConfigForm) {
      if (this.isSetDownLink) {
        this.azureEventHubIntegrationConfigForm.get('iotHubName').setValidators(Validators.required);
      } else {
        this.azureEventHubIntegrationConfigForm.get('iotHubName').setValidators([]);
      }
      this.azureEventHubIntegrationConfigForm.get('iotHubName').updateValueAndValidity();
    }
  }
}
