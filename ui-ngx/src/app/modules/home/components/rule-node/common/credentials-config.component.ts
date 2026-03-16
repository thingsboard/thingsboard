///
/// Copyright © 2016-2026 The Thingsboard Authors
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

import { Component, DestroyRef, forwardRef, Input, OnChanges, OnInit, SimpleChanges } from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  ValidatorFn,
  Validators
} from '@angular/forms';
import { isDefinedAndNotNull } from '@core/public-api';
import { PageComponent } from '@shared/public-api';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { credentialsType, credentialsTypes, credentialsTypeTranslations } from '../rule-node-config.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

interface CredentialsConfig {
  type: credentialsType;
  username?: string;
  password?: string;
  caCert?: string;
  caCertFileName?: string;
  privateKey?: string;
  privateKeyFileName?: string;
  cert?: string;
  certFileName?: string;
}

@Component({
    selector: 'tb-credentials-config',
    templateUrl: './credentials-config.component.html',
    styleUrls: [],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => CredentialsConfigComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => CredentialsConfigComponent),
            multi: true,
        }
    ],
    standalone: false
})
export class CredentialsConfigComponent extends PageComponent implements ControlValueAccessor, OnInit, Validator, OnChanges {

  credentialsConfigFormGroup: FormGroup;

  private requiredValue: boolean;

  get required(): boolean {
    return this.requiredValue;
  }

  @Input()
  set required(value: boolean) {
    this.requiredValue = coerceBooleanProperty(value);
  }

  @Input()
  disableCertPemCredentials = false;

  @Input()
  passwordFieldRequired = true;

  allCredentialsTypes = credentialsTypes;
  credentialsTypeTranslationsMap = credentialsTypeTranslations;

  private propagateChange = (_: any) => {};

  constructor(private fb: FormBuilder,
              private destroyRef: DestroyRef) {
    super();
  }

  ngOnInit(): void {
    this.credentialsConfigFormGroup = this.fb.group(
      {
        type: [null, [Validators.required]],
        username: [null, []],
        password: [null, []],
        caCert: [null, []],
        caCertFileName: [null, []],
        privateKey: [null, []],
        privateKeyFileName: [null, []],
        cert: [null, []],
        certFileName: [null, []]
      }
    );
    this.credentialsConfigFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateView();
    });
    this.credentialsConfigFormGroup.get('type').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.credentialsTypeChanged();
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (change.currentValue && propName === 'disableCertPemCredentials') {
          const credentialsTypeValue: credentialsType = this.credentialsConfigFormGroup.get('type').value;
          if (credentialsTypeValue === 'cert.PEM') {
            setTimeout(() => {
              this.credentialsConfigFormGroup.get('type').patchValue('anonymous', {emitEvent: true});
            });
          }
        }
      }
    }
  }

  writeValue(credentials: CredentialsConfig | null): void {
    if (isDefinedAndNotNull(credentials)) {
      this.credentialsConfigFormGroup.reset(credentials, {emitEvent: false});
      this.updateValidators();
    }
  }

  setDisabledState(isDisabled: boolean): void {
    if (isDisabled) {
      this.credentialsConfigFormGroup.disable({emitEvent: false});
    } else {
      this.credentialsConfigFormGroup.enable({emitEvent: false});
      this.updateValidators();
    }
  }

  updateView() {
    let credentialsConfigValue = this.credentialsConfigFormGroup.value;
    const credentialsTypeValue: credentialsType = credentialsConfigValue.type;
    switch (credentialsTypeValue) {
      case 'anonymous':
        credentialsConfigValue = {
          type: credentialsTypeValue
        };
        break;
      case 'basic':
        credentialsConfigValue = {
          type: credentialsTypeValue,
          username: credentialsConfigValue.username,
          password: credentialsConfigValue.password,
        };
        break;
      case 'cert.PEM':
        delete credentialsConfigValue.username;
        break;
    }
    this.propagateChange(credentialsConfigValue);
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(): void {
  }

  public validate() {
    return this.credentialsConfigFormGroup.valid ? null : {
      credentialsConfig: {
        valid: false,
      },
    };
  }

  credentialsTypeChanged(): void {
    this.credentialsConfigFormGroup.patchValue({
      username: null,
      password: null,
      caCert: null,
      caCertFileName: null,
      privateKey: null,
      privateKeyFileName: null,
      cert: null,
      certFileName: null,
    });
    this.updateValidators();
  }

  protected updateValidators(emitEvent: boolean = false) {
    const credentialsTypeValue: credentialsType = this.credentialsConfigFormGroup.get('type').value;
    if (emitEvent) {
      this.credentialsConfigFormGroup.reset({type: credentialsTypeValue}, {emitEvent: false});
    }
    this.credentialsConfigFormGroup.setValidators([]);
    this.credentialsConfigFormGroup.get('username').setValidators([]);
    this.credentialsConfigFormGroup.get('password').setValidators([]);
    switch (credentialsTypeValue) {
      case 'anonymous':
        break;
      case 'basic':
        this.credentialsConfigFormGroup.get('username').setValidators([Validators.required]);
        this.credentialsConfigFormGroup.get('password').setValidators(this.passwordFieldRequired ? [Validators.required] : []);
        break;
      case 'cert.PEM':
        this.credentialsConfigFormGroup.setValidators([this.requiredFilesSelected(
          Validators.required,
          [['caCert', 'caCertFileName'], ['privateKey', 'privateKeyFileName', 'cert', 'certFileName']]
        )]);
        break;
    }
    this.credentialsConfigFormGroup.get('username').updateValueAndValidity({emitEvent});
    this.credentialsConfigFormGroup.get('password').updateValueAndValidity({emitEvent});
    this.credentialsConfigFormGroup.updateValueAndValidity({emitEvent});
  }

  private requiredFilesSelected(validator: ValidatorFn,
                                requiredFieldsSet: string[][] = null) {
    return (group: FormGroup): ValidationErrors | null => {
      if (!requiredFieldsSet) {
        requiredFieldsSet = [Object.keys(group.controls)];
      }
      const allRequiredFilesSelected = group?.controls &&
        requiredFieldsSet.some(arrFields => arrFields.every(k => !validator(group.controls[k])));

      return allRequiredFilesSelected ? null : {notAllRequiredFilesSelected: true};
    };
  }
}
