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
import { TuyaEnv, TuyaIntegration, TuyaRegion, TuyaRegionTranslation } from '@shared/models/integration.models';

@Component({
    selector: 'tb-tuya-integration-form',
    templateUrl: './tuya-integration-form.component.html',
    styleUrls: [],
    providers: [{
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => TuyaIntegrationFormComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => TuyaIntegrationFormComponent),
            multi: true,
        }],
    standalone: false
})

export class TuyaIntegrationFormComponent extends IntegrationForm implements ControlValueAccessor, Validator {

  tuyaIntegrationConfigForm: UntypedFormGroup;
  tuyaRegion = TuyaRegion;
  tuyaEnv = TuyaEnv;
  TuyaRegionTranslation = TuyaRegionTranslation;

  private propagateChange = (v: any) => { };

  constructor(private fb: UntypedFormBuilder) {
    super();
    this.tuyaIntegrationConfigForm = this.fb.group({
      region: [TuyaRegion.CN, [Validators.required]],
      env: [TuyaEnv.PROD, [Validators.required]],
      accessId: ['', [Validators.required]],
      accessKey: ['', [Validators.required]]
    });
    this.tuyaIntegrationConfigForm.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => this.updateModels(this.tuyaIntegrationConfigForm.getRawValue()));
  }

  writeValue(value: TuyaIntegration) {
    if (isDefinedAndNotNull(value?.clientConfiguration)) {
      this.tuyaIntegrationConfigForm.reset(value.clientConfiguration, {emitEvent: false});
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any) { }

  setDisabledState(isDisabled: boolean) {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.tuyaIntegrationConfigForm.disable({emitEvent: false});
    } else {
      this.tuyaIntegrationConfigForm.enable({emitEvent: false});
    }
  }

  private updateModels(value) {
    this.propagateChange({clientConfiguration: value});
  }

  validate(): ValidationErrors | null {
    return this.tuyaIntegrationConfigForm.valid ? null : {
      tuyaIntegrationConfigForm: {valid: false}
    };
  }
}
