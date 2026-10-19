// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, forwardRef, OnDestroy } from '@angular/core';
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
  Lwm2mClientSecretKeyTooltipTranslationsMap,
  Lwm2mPublicKeyOrIdTooltipTranslationsMap,
  Lwm2mSecurityType,
  Lwm2mSecurityTypeTranslationMap,
  ServerSecurityConfig
} from '@shared/models/lwm2m-security-config.models';
import { takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs';

@Component({
    selector: 'tb-device-credentials-lwm2m-server',
    templateUrl: './device-credentials-lwm2m-server.component.html',
    styleUrls: [],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DeviceCredentialsLwm2mServerComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => DeviceCredentialsLwm2mServerComponent),
            multi: true
        }
    ],
    standalone: false
})

export class DeviceCredentialsLwm2mServerComponent implements OnDestroy, ControlValueAccessor, Validator {

  serverFormGroup: UntypedFormGroup;
  securityConfigLwM2MType = Lwm2mSecurityType;
  securityConfigLwM2MTypes = Object.values(Lwm2mSecurityType);
  lwm2mSecurityTypeTranslationMap = Lwm2mSecurityTypeTranslationMap;
  publicKeyOrIdTooltipNamesMap = Lwm2mPublicKeyOrIdTooltipTranslationsMap;
  clientSecretKeyTooltipNamesMap = Lwm2mClientSecretKeyTooltipTranslationsMap;

  private destroy$ = new Subject<void>();
  private propagateChange = (v: any) => {};

  constructor(private fb: UntypedFormBuilder) {
    this.serverFormGroup = this.fb.group({
      securityMode: [Lwm2mSecurityType.NO_SEC],
      clientPublicKeyOrId: [''],
      clientSecretKey: ['']
    });
    this.serverFormGroup.get('securityMode').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((securityMode) => {
      this.updateValidate(securityMode);
    });

    this.serverFormGroup.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value) => {
      this.propagateChange(value);
    });
  }

  writeValue(value: any): void {
    if (value) {
      this.updateValueFields(value);
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    if (isDisabled) {
      this.serverFormGroup.disable({emitEvent: false});
    } else {
      this.serverFormGroup.enable({emitEvent: false});
    }
  }

  validate(control): ValidationErrors | null {
    return this.serverFormGroup.valid ? null : {
      securityConfig: {valid: false}
    };
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private updateValueFields(serverData: ServerSecurityConfig): void {
    this.serverFormGroup.patchValue(serverData, {emitEvent: false});
    this.updateValidate(serverData.securityMode, true);
  }

  private updateValidate(securityMode: Lwm2mSecurityType, initValue = false): void {
    switch (securityMode) {
      case Lwm2mSecurityType.NO_SEC:
        this.serverFormGroup.get('clientPublicKeyOrId').clearValidators();
        this.serverFormGroup.get('clientSecretKey').clearValidators();
        this.serverFormGroup.get('clientPublicKeyOrId').disable({emitEvent: false});
        this.serverFormGroup.get('clientSecretKey').disable();
        break;
      case Lwm2mSecurityType.PSK:
      case Lwm2mSecurityType.RPK:
      case Lwm2mSecurityType.X509:
        this.setValidatorsSecurity();
        break;
    }
    this.serverFormGroup.get('clientPublicKeyOrId').updateValueAndValidity({emitEvent: false});
    this.serverFormGroup.get('clientSecretKey').updateValueAndValidity({emitEvent: !initValue});
  }

  private setValidatorsSecurity = (): void => {
    const clientSecretKeyValidators = [Validators.required];
    const clientPublicKeyOrIdValidators = [Validators.required];

    this.serverFormGroup.get('clientPublicKeyOrId').setValidators(clientPublicKeyOrIdValidators);
    this.serverFormGroup.get('clientSecretKey').setValidators(clientSecretKeyValidators);

    this.serverFormGroup.get('clientPublicKeyOrId').enable({emitEvent: false});
    this.serverFormGroup.get('clientSecretKey').enable();
  };
}
