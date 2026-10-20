// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, forwardRef } from '@angular/core';
import { ContentType } from '@shared/models/constants';
import { IntegrationForm } from '@home/components/integration/configuration/integration-form';
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
import { takeUntil } from 'rxjs/operators';
import { isDefinedAndNotNull } from '@core/utils';
import { CustomIntegration } from '@shared/models/integration.models';

@Component({
    selector: 'tb-custom-integration-form',
    templateUrl: './custom-integration-form.component.html',
    styleUrls: [],
    providers: [{
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => CustomIntegrationFormComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => CustomIntegrationFormComponent),
            multi: true,
        }],
    standalone: false
})
export class CustomIntegrationFormComponent extends IntegrationForm implements ControlValueAccessor, Validator {

  customIntegrationConfigForm: UntypedFormGroup;

  // @ViewChild('jsonContentComponent', {static: true}) jsonContentComponent: JsonContentComponent;

  contentType = ContentType;

  private propagateChange = (v: any) => { };

  constructor(private fb: UntypedFormBuilder) {
    super();
    this.customIntegrationConfigForm = this.fb.group({
      clazz: ['', Validators.required],
      configuration: ['{}']
    });
    this.customIntegrationConfigForm.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.updateModels(this.customIntegrationConfigForm.getRawValue());
    });
  }

  writeValue(value: CustomIntegration) {
    if (isDefinedAndNotNull(value)) {
      this.customIntegrationConfigForm.reset(value, {emitEvent: false});
    }
  }

  registerOnChange(fn: any) {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any) { }

  setDisabledState(isDisabled: boolean) {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.customIntegrationConfigForm.disable({emitEvent: false});
    } else {
      this.customIntegrationConfigForm.enable({emitEvent: false});
    }
  }

  validate(): ValidationErrors {
    return this.customIntegrationConfigForm.valid ? null : {
      customIntegrationConfigForm: {valid: false}
    };
  }

  private updateModels(value) {
    this.propagateChange(value);
  }
}
