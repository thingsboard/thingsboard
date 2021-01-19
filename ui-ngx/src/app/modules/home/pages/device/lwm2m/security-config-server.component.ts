///
/// Copyright © 2016-2020 The Thingsboard Authors
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

import { Component, forwardRef, Inject, Input, OnInit } from '@angular/core';

import {
  ControlValueAccessor,
  FormBuilder, FormGroup, NG_VALUE_ACCESSOR, Validators
} from '@angular/forms';
import {
  SECURITY_CONFIG_MODE,
  SECURITY_CONFIG_MODE_NAMES,
  KEY_REGEXP_HEX_DEC,
  ServerSecurityConfig,
  DeviceCredentialsDialogLwm2mData,
  LEN_MAX_PSK,
  LEN_MAX_PRIVATE_KEY, LEN_MAX_PUBLIC_KEY_RPK, LEN_MAX_PUBLIC_KEY_X509
} from '@home/pages/device/lwm2m/security-config.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { PageComponent } from '@shared/components/page.component';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'tb-security-config-server-lwm2m',
  templateUrl: './security-config-server.component.html',
  styleUrls: [],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => SecurityConfigServerComponent),
      multi: true
    }
  ]
})

export class SecurityConfigServerComponent extends PageComponent implements OnInit, ControlValueAccessor {

  securityConfigLwM2MType = SECURITY_CONFIG_MODE;
  securityConfigLwM2MTypes = Object.keys(SECURITY_CONFIG_MODE);
  credentialTypeLwM2MNamesMap = SECURITY_CONFIG_MODE_NAMES;
  lenMinClientPublicKeyOrId = 0;
  lenMaxClientPublicKeyOrId = LEN_MAX_PUBLIC_KEY_RPK;
  lenMinClientSecretKey = LEN_MAX_PRIVATE_KEY;
  lenMaxClientSecretKey = LEN_MAX_PRIVATE_KEY;

  @Input() serverFormGroup: FormGroup;

  constructor(protected store: Store<AppState>,
              @Inject(MAT_DIALOG_DATA) public data: DeviceCredentialsDialogLwm2mData,
              public dialogRef: MatDialogRef<SecurityConfigServerComponent, object>,
              public translate: TranslateService,
              public fb: FormBuilder) {
    super(store);
  }

  ngOnInit(): void {
    this.registerDisableOnLoadFormControl(this.serverFormGroup.get('securityMode'));
  }

  private updateValueFields(serverData: ServerSecurityConfig): void {
    this.serverFormGroup.patchValue(serverData, {emitEvent: false});
    const securityMode = this.serverFormGroup.get('securityMode').value as SECURITY_CONFIG_MODE;
    this.updateValidate(securityMode);
  }

  private updateValidate(securityMode: SECURITY_CONFIG_MODE): void {
    switch (securityMode) {
      case SECURITY_CONFIG_MODE.NO_SEC:
        this.serverFormGroup.get('clientPublicKeyOrId').setValidators([]);
        this.serverFormGroup.get('clientSecretKey').setValidators([]);
        break;
      case SECURITY_CONFIG_MODE.PSK:
        this.lenMinClientPublicKeyOrId = 0;
        this.lenMaxClientPublicKeyOrId = LEN_MAX_PUBLIC_KEY_RPK;
        this.lenMinClientSecretKey = LEN_MAX_PSK;
        this.lenMaxClientSecretKey = LEN_MAX_PSK;
        this.setValidatorsSecurity(securityMode);
        break;
      case SECURITY_CONFIG_MODE.RPK:
        this.lenMinClientPublicKeyOrId = LEN_MAX_PUBLIC_KEY_RPK;
        this.lenMaxClientPublicKeyOrId = LEN_MAX_PUBLIC_KEY_RPK;
        this.lenMinClientSecretKey = LEN_MAX_PRIVATE_KEY;
        this.lenMaxClientSecretKey = LEN_MAX_PRIVATE_KEY;
        this.setValidatorsSecurity(securityMode);
        break;
      case SECURITY_CONFIG_MODE.X509:
        this.lenMinClientPublicKeyOrId = 0;
        this.lenMaxClientPublicKeyOrId = LEN_MAX_PUBLIC_KEY_X509;
        this.lenMinClientSecretKey = LEN_MAX_PRIVATE_KEY;
        this.lenMaxClientSecretKey = LEN_MAX_PRIVATE_KEY;
        this.setValidatorsSecurity(securityMode);
        break;
    }
    this.serverFormGroup.updateValueAndValidity();
  }

  private setValidatorsSecurity = (securityMode: SECURITY_CONFIG_MODE): void => {
    if (securityMode === SECURITY_CONFIG_MODE.PSK) {
      this.serverFormGroup.get('clientPublicKeyOrId').setValidators([Validators.required]);
    } else {
      this.serverFormGroup.get('clientPublicKeyOrId').setValidators([Validators.required,
        Validators.pattern(KEY_REGEXP_HEX_DEC),
        Validators.minLength(this.lenMinClientPublicKeyOrId),
        Validators.maxLength(this.lenMaxClientPublicKeyOrId)]);
    }

    this.serverFormGroup.get('clientSecretKey').setValidators([Validators.required,
      Validators.pattern(KEY_REGEXP_HEX_DEC),
      Validators.minLength(this.lenMinClientSecretKey),
      Validators.maxLength(this.lenMaxClientSecretKey)]);
  }

  securityModeChanged(securityMode: SECURITY_CONFIG_MODE): void {
    this.updateValidate(securityMode);
  }

  writeValue(value: any): void {
    if (value) {
      this.updateValueFields(value);
    }
  }

  registerOnChange(fn: (value: any) => any): void {
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState?(isDisabled: boolean): void {
  }
}
