///
/// Copyright © 2016-2021 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { Component, forwardRef, OnDestroy } from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import {
  KEY_REGEXP_HEX_DEC,
  LEN_MAX_PRIVATE_KEY,
  LEN_MAX_PSK,
  LEN_MAX_PUBLIC_KEY_RPK,
  LEN_MAX_PUBLIC_KEY_X509,
  Lwm2mSecurityType,
  Lwm2mSecurityTypeTranslationMap,
  ServerSecurityConfig
} from '@shared/models/lwm2m-security-config.models';
import { takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs';

@Component({
  selector: 'tb-security-config-lwm2m-server',
  templateUrl: './security-config-lwm2m-server.component.html',
  styleUrls: ['./security-config-lwm2m-server.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => SecurityConfigLwm2mServerComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => SecurityConfigLwm2mServerComponent),
      multi: true
    }
  ]
})

export class SecurityConfigLwm2mServerComponent implements OnDestroy, ControlValueAccessor, Validator {

  serverFormGroup: FormGroup;
  securityConfigLwM2MType = Lwm2mSecurityType;
  securityConfigLwM2MTypes = Object.values(Lwm2mSecurityType);
  lwm2mSecurityTypeTranslationMap = Lwm2mSecurityTypeTranslationMap;
  lenMinClientPublicKeyOrId = 0;
  lenMaxClientPublicKeyOrId = LEN_MAX_PUBLIC_KEY_RPK;
  lengthClientSecretKey = LEN_MAX_PRIVATE_KEY;

  private destroy$ = new Subject();
  private propagateChange = (v: any) => {};

  constructor(private fb: FormBuilder) {
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
        break;
      case Lwm2mSecurityType.PSK:
        this.lenMinClientPublicKeyOrId = 0;
        this.lenMaxClientPublicKeyOrId = LEN_MAX_PUBLIC_KEY_RPK;
        this.lengthClientSecretKey = LEN_MAX_PSK;
        this.setValidatorsSecurity(securityMode);
        break;
      case Lwm2mSecurityType.RPK:
        this.lenMinClientPublicKeyOrId = LEN_MAX_PUBLIC_KEY_RPK;
        this.lenMaxClientPublicKeyOrId = LEN_MAX_PUBLIC_KEY_RPK;
        this.lengthClientSecretKey = LEN_MAX_PRIVATE_KEY;
        this.setValidatorsSecurity(securityMode);
        break;
      case Lwm2mSecurityType.X509:
        this.lenMinClientPublicKeyOrId = 0;
        this.lenMaxClientPublicKeyOrId = LEN_MAX_PUBLIC_KEY_X509;
        this.lengthClientSecretKey = LEN_MAX_PRIVATE_KEY;
        this.setValidatorsSecurity(securityMode);
        break;
    }
    this.serverFormGroup.get('clientPublicKeyOrId').updateValueAndValidity({emitEvent: false});
    this.serverFormGroup.get('clientSecretKey').updateValueAndValidity({emitEvent: !initValue});
  }

  private setValidatorsSecurity = (securityMode: Lwm2mSecurityType): void => {
    if (securityMode === Lwm2mSecurityType.PSK) {
      this.serverFormGroup.get('clientPublicKeyOrId').setValidators([Validators.required]);
    } else {
      this.serverFormGroup.get('clientPublicKeyOrId').setValidators([
        Validators.required,
        Validators.pattern(KEY_REGEXP_HEX_DEC),
        Validators.minLength(this.lenMinClientPublicKeyOrId),
        Validators.maxLength(this.lenMaxClientPublicKeyOrId)
      ]);
    }

    this.serverFormGroup.get('clientSecretKey').setValidators([
      Validators.required,
      Validators.pattern(KEY_REGEXP_HEX_DEC),
      Validators.minLength(this.lengthClientSecretKey),
      Validators.maxLength(this.lengthClientSecretKey)
    ]);
  }
}
