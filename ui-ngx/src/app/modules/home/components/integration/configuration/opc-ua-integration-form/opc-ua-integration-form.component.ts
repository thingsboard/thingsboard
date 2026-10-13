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
import {
  IdentityType,
  IdentityTypeTranslation,
  OpcKeystoreType,
  OpcSecurityType,
  OpcUaIntegration
} from '@shared/models/integration.models';
import { IntegrationForm } from '@home/components/integration/configuration/integration-form';
import { isDefinedAndNotNull } from '@core/utils';
import { takeUntil } from 'rxjs/operators';
import { privateNetworkAddressValidator } from '@home/components/integration/integration.models';

@Component({
    selector: 'tb-opc-ua-integration-form',
    templateUrl: './opc-ua-integration-form.component.html',
    styleUrls: ['./opc-ua-integration-form.component.scss'],
    providers: [{
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => OpcUaIntegrationFormComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => OpcUaIntegrationFormComponent),
            multi: true,
        }],
    standalone: false
})
export class OpcUaIntegrationFormComponent extends IntegrationForm implements ControlValueAccessor, Validator {

  opcIntegrationConfigForm: UntypedFormGroup;

  identityTypes = Object.values(IdentityType) as IdentityType[];
  IdentityType = IdentityType;
  IdentityTypeTranslation = IdentityTypeTranslation;
  OpcKeystoreType = Object.values(OpcKeystoreType);
  OpcSecurityType = OpcSecurityType;

  private propagateChange = (v: any) => { };

  constructor(private fb: UntypedFormBuilder) {
    super();

    this.opcIntegrationConfigForm = this.fb.group({
      applicationName: '',
      applicationUri: '',
      host: ['', Validators.required],
      port: [49320, [Validators.required, Validators.min(1), Validators.max(65535)]],
      endpoint: ['', []],
      scanPeriodInSeconds: [10, Validators.required],
      timeoutInMillis: [5000, Validators.required],
      security: [OpcSecurityType.None, Validators.required],
      identity: this.fb.group({
        password: [{value: '', disabled: true}, Validators.required],
        username: [{value: '', disabled: true}, Validators.required],
        type: [IdentityType.Anonymous, Validators.required]
      }),
      mapping: [null, Validators.required],
      keystore: this.fb.group({
        location: [{value: '', disabled: true}, Validators.required],
        type: [{value: OpcKeystoreType.JKS, disabled: true}, Validators.required],
        fileContent: [{value: '', disabled: true}, Validators.required],
        password: [{value: 'secret', disabled: true}, Validators.required],
        alias: [{value: 'opc-ua-extension', disabled: true}, Validators.required],
        keyPassword: [{value: 'secret', disabled: true}, Validators.required]
      })
    });

    this.opcIntegrationConfigForm.get('security').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((type) => {
      this.updateSecurityTypeValidation(type);
    });

    this.opcIntegrationConfigForm.get('identity.type').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((type) => {
      this.updateIdentityTypeValidation(type);
    });

    this.opcIntegrationConfigForm.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.updateModels(this.opcIntegrationConfigForm.getRawValue());
    });
  }

  writeValue(value: OpcUaIntegration) {
    if (isDefinedAndNotNull(value?.clientConfiguration)) {
      this.opcIntegrationConfigForm.reset(value.clientConfiguration, {emitEvent: false});
      if (!this.disabled) {
        this.opcIntegrationConfigForm.get('security').updateValueAndValidity({onlySelf: true});
        this.opcIntegrationConfigForm.get('identity.type').updateValueAndValidity({onlySelf: true});
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
      this.opcIntegrationConfigForm.disable({emitEvent: false});
    } else {
      this.opcIntegrationConfigForm.enable({emitEvent: false});
      this.opcIntegrationConfigForm.get('security').updateValueAndValidity({onlySelf: true});
      this.opcIntegrationConfigForm.get('identity.type').updateValueAndValidity({onlySelf: true});
    }
  }

  private updateModels(value) {
    this.propagateChange({clientConfiguration: value});
  }

  private updateSecurityTypeValidation(type: OpcSecurityType) {
    if (type === OpcSecurityType.None) {
      this.opcIntegrationConfigForm.get('keystore').disable({emitEvent: false});
    } else {
      this.opcIntegrationConfigForm.get('keystore').enable({emitEvent: false});
    }
  }

  private updateIdentityTypeValidation(type: IdentityType) {
    if (type === IdentityType.Anonymous) {
      this.opcIntegrationConfigForm.get('identity.username').disable({emitEvent: false});
      this.opcIntegrationConfigForm.get('identity.password').disable({emitEvent: false});
    } else {
      this.opcIntegrationConfigForm.get('identity.username').enable({emitEvent: false});
      this.opcIntegrationConfigForm.get('identity.password').enable({emitEvent: false});
    }
  }

  validate(): ValidationErrors | null {
    return this.opcIntegrationConfigForm.valid ? null : {
      opcUaIntegrationConfigForm: {valid: false}
    };
  }

  updatedValidationPrivateNetwork() {
    if (this.allowLocalNetwork) {
      this.opcIntegrationConfigForm.get('host').removeValidators(privateNetworkAddressValidator);
    } else {
      this.opcIntegrationConfigForm.get('host').addValidators(privateNetworkAddressValidator);
    }
    this.opcIntegrationConfigForm.get('host').updateValueAndValidity({emitEvent: false});
  }
}
