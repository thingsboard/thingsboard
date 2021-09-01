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
  AbstractControl,
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
  getDefaultClientSecurityConfig,
  getDefaultServerSecurityConfig,
  KEY_REGEXP_HEX_DEC,
  LEN_MAX_PSK,
  Lwm2mSecurityConfigModels,
  Lwm2mSecurityType,
  Lwm2mSecurityTypeTranslationMap
} from '@shared/models/lwm2m-security-config.models';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { isDefinedAndNotNull } from '@core/utils';

@Component({
  selector: 'tb-device-credentials-lwm2m',
  templateUrl: './device-credentials-lwm2m.component.html',
  styleUrls: ['./device-credentials-lwm2m.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => DeviceCredentialsLwm2mComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => DeviceCredentialsLwm2mComponent),
      multi: true
    }
  ]
})

export class DeviceCredentialsLwm2mComponent implements ControlValueAccessor, Validator, OnDestroy {

  lwm2mConfigFormGroup: FormGroup;
  securityConfigLwM2MType = Lwm2mSecurityType;
  securityConfigLwM2MTypes = Object.keys(Lwm2mSecurityType);
  credentialTypeLwM2MNamesMap = Lwm2mSecurityTypeTranslationMap;
  lenMaxKeyClient = LEN_MAX_PSK;
  allowLengthKey = [32, 64, LEN_MAX_PSK];

  private destroy$ = new Subject();
  private propagateChange = (v: any) => {};

  constructor(private fb: FormBuilder) {
    this.lwm2mConfigFormGroup = this.initLwm2mConfigForm();
  }

  writeValue(obj: string) {
    if (isDefinedAndNotNull(obj)) {
      this.initClientSecurityConfig(JSON.parse(obj));
    }
  }

  registerOnChange(fn: any) {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any) {}

  setDisabledState(isDisabled: boolean): void {
    if (isDisabled) {
      this.lwm2mConfigFormGroup.disable({emitEvent: false});
    } else {
      this.lwm2mConfigFormGroup.enable({emitEvent: false});
      this.securityConfigClientUpdateValidators(this.lwm2mConfigFormGroup.get('client.securityConfigClientMode').value);
    }
  }

  validate(): ValidationErrors | null {
    return this.lwm2mConfigFormGroup.valid ? null : {
      securityConfigLWm2m: false
    };
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private initClientSecurityConfig(config: Lwm2mSecurityConfigModels): void {
    this.lwm2mConfigFormGroup.patchValue(config, {emitEvent: false});
    this.securityConfigClientUpdateValidators(config.client.securityConfigClientMode);
  }

  private securityConfigClientModeChanged(type: Lwm2mSecurityType): void {
    const config = getDefaultClientSecurityConfig(type, this.lwm2mConfigFormGroup.get('client.endpoint').value);
    switch (type) {
      case Lwm2mSecurityType.PSK:
        config.key = this.lwm2mConfigFormGroup.get('client.key').value;
        break;
      case Lwm2mSecurityType.RPK:
        config.key = this.lwm2mConfigFormGroup.get('client.key').value;
        break;
    }
    this.lwm2mConfigFormGroup.get('client').reset(config, {emitEvent: false});
    this.securityConfigClientUpdateValidators(type);
  }

  private securityConfigClientUpdateValidators = (mode: Lwm2mSecurityType): void => {
    switch (mode) {
      case Lwm2mSecurityType.NO_SEC:
        this.setValidatorsNoSecX509();
        this.lwm2mConfigFormGroup.get('client.cert').disable({emitEvent: false});
        break;
      case Lwm2mSecurityType.X509:
        this.setValidatorsNoSecX509();
        this.lwm2mConfigFormGroup.get('client.cert').enable({emitEvent: false});
        break;
      case Lwm2mSecurityType.PSK:
        this.lenMaxKeyClient = LEN_MAX_PSK;
        this.setValidatorsPskRpk(mode);
        this.lwm2mConfigFormGroup.get('client.identity').enable({emitEvent: false});
        break;
      case Lwm2mSecurityType.RPK:
        this.lenMaxKeyClient = null;
        this.setValidatorsPskRpk(mode);
        this.lwm2mConfigFormGroup.get('client.identity').disable({emitEvent: false});
        break;
    }
    this.lwm2mConfigFormGroup.get('client.identity').updateValueAndValidity({emitEvent: false});
    this.lwm2mConfigFormGroup.get('client.key').updateValueAndValidity({emitEvent: false});
  }

  private setValidatorsNoSecX509 = (): void => {
    this.lwm2mConfigFormGroup.get('client.identity').clearValidators();
    this.lwm2mConfigFormGroup.get('client.key').clearValidators();
    this.lwm2mConfigFormGroup.get('client.identity').disable({emitEvent: false});
    this.lwm2mConfigFormGroup.get('client.key').disable({emitEvent: false});
  }

  private setValidatorsPskRpk = (mode: Lwm2mSecurityType): void => {
    const keyValidators = [Validators.required, Validators.pattern(KEY_REGEXP_HEX_DEC)];
    if (mode === Lwm2mSecurityType.PSK) {
      this.lwm2mConfigFormGroup.get('client.identity').setValidators([Validators.required]);
      keyValidators.push(this.maxLength(this.allowLengthKey));
    } else {
      this.lwm2mConfigFormGroup.get('client.identity').clearValidators();
    }
    this.lwm2mConfigFormGroup.get('client.key').setValidators(keyValidators);
    this.lwm2mConfigFormGroup.get('client.key').enable({emitEvent: false});
    this.lwm2mConfigFormGroup.get('client.cert').disable({emitEvent: false});
  }

  private maxLength(keyLengths: number[]) {
    return (control: AbstractControl): ValidationErrors | null => {
      const value = control.value;
      if (keyLengths.some(len => value.length === len)) {
        return null;
      }
      return {length: true};
    };
  }

  private initLwm2mConfigForm = (): FormGroup => {
    const formGroup =  this.fb.group({
      client: this.fb.group({
        endpoint: ['', Validators.required],
        securityConfigClientMode: [Lwm2mSecurityType.NO_SEC],
        identity: [{value: '', disabled: true}],
        key: [{value: '', disabled: true}],
        cert: [{value: '', disabled: true}]
      }),
      bootstrap: this.fb.group({
        bootstrapServer: [getDefaultServerSecurityConfig()],
        lwm2mServer: [getDefaultServerSecurityConfig()]
      })
    });
    formGroup.get('client.securityConfigClientMode').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((type) => {
      this.securityConfigClientModeChanged(type);
    });
    formGroup.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value) => {
      this.propagateChange(JSON.stringify(value));
    });
    return formGroup;
  }
}
