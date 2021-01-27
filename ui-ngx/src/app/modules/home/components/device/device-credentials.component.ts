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

import { Component, forwardRef, Input, OnDestroy, OnInit } from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  FormControl,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  ValidatorFn,
  Validators
} from '@angular/forms';
import {
  credentialTypeNames,
  DeviceCredentialMQTTBasic,
  DeviceCredentials,
  DeviceCredentialsType
} from '@shared/models/device.models';
import { Subscription } from 'rxjs';
import { distinctUntilChanged } from 'rxjs/operators';
import { SecurityConfigComponent } from '@home/pages/device/lwm2m/security-config.component';
import {
  DEFAULT_END_POINT,
  DeviceCredentialsDialogLwm2mData,
  END_POINT,
  getDefaultSecurityConfig,
  JSON_ALL_CONFIG
} from '@home/pages/device/lwm2m/security-config.models';
import { TranslateService } from '@ngx-translate/core';
import { MatDialog } from '@angular/material/dialog';
import { isDefinedAndNotNull } from '@core/utils';

@Component({
  selector: 'tb-device-credentials',
  templateUrl: './device-credentials.component.html',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => DeviceCredentialsComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => DeviceCredentialsComponent),
      multi: true,
    }],
  styleUrls: []
})
export class DeviceCredentialsComponent implements ControlValueAccessor, OnInit, Validator, OnDestroy {

  deviceCredentialsFormGroup: FormGroup;

  subscriptions: Subscription[] = [];

  @Input()
  disabled: boolean;

  deviceCredentials: DeviceCredentials = null;

  submitted = false;

  deviceCredentialsType = DeviceCredentialsType;

  credentialsTypes = Object.keys(DeviceCredentialsType);

  credentialTypeNamesMap = credentialTypeNames;

  hidePassword = true;

  private propagateChange = (v: any) => {};

  constructor(public fb: FormBuilder,
              private translate: TranslateService,
              private dialog: MatDialog) {
    this.deviceCredentialsFormGroup = this.fb.group({
      credentialsType: [DeviceCredentialsType.ACCESS_TOKEN],
      credentialsId: [null],
      credentialsValue: [null],
      credentialsBasic: this.fb.group({
        clientId: [null, [Validators.pattern(/^[A-Za-z0-9]+$/)]],
        userName: [null],
        password: [null]
      }, {validators: this.atLeastOne(Validators.required, ['clientId', 'userName'])})
    });
    this.deviceCredentialsFormGroup.get('credentialsBasic').disable();
    this.subscriptions.push(
      this.deviceCredentialsFormGroup.valueChanges.pipe(distinctUntilChanged()).subscribe(() => {
        this.updateView();
      })
    );
    this.subscriptions.push(
      this.deviceCredentialsFormGroup.get('credentialsType').valueChanges.subscribe(() => {
        this.credentialsTypeChanged();
      })
    );
  }

  ngOnInit(): void {
    if (this.disabled) {
      this.deviceCredentialsFormGroup.disable({emitEvent: false});
    }
  }

  ngOnDestroy() {
    this.subscriptions.forEach(s => s.unsubscribe());
  }

  writeValue(value: DeviceCredentials | null): void {
    if (isDefinedAndNotNull(value)) {
      this.deviceCredentials = value;
      let credentialsBasic = {clientId: null, userName: null, password: null};
      let credentialsValue = null;
      if (value.credentialsType === DeviceCredentialsType.MQTT_BASIC) {
        credentialsBasic = JSON.parse(value.credentialsValue) as DeviceCredentialMQTTBasic;
      } else {
        credentialsValue = value.credentialsValue;
      }
      this.deviceCredentialsFormGroup.patchValue({
        credentialsType: value.credentialsType,
        credentialsId: value.credentialsId,
        credentialsValue,
        credentialsBasic
      }, {emitEvent: false});
      this.updateValidators();
    }
  }

  updateView() {
    const deviceCredentialsValue = this.deviceCredentialsFormGroup.value;
    if (deviceCredentialsValue.credentialsType === DeviceCredentialsType.MQTT_BASIC) {
      deviceCredentialsValue.credentialsValue = JSON.stringify(deviceCredentialsValue.credentialsBasic);
    }
    delete deviceCredentialsValue.credentialsBasic;
    this.propagateChange(deviceCredentialsValue);
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {}

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.deviceCredentialsFormGroup.disable({emitEvent: false});
    } else {
      this.deviceCredentialsFormGroup.enable({emitEvent: false});
      this.updateValidators();
    }
  }

  public validate(c: FormControl) {
    return this.deviceCredentialsFormGroup.valid ? null : {
      deviceCredentials: {
        valid: false,
      },
    };
  }

  credentialsTypeChanged(): void {
    this.deviceCredentialsFormGroup.patchValue({
      credentialsId: null,
      credentialsValue: null,
      credentialsBasic: {clientId: '', userName: '', password: ''}
    });
    this.updateValidators();
  }

  updateValidators(): void {
    this.hidePassword = true;
    const credentialsType = this.deviceCredentialsFormGroup.get('credentialsType').value as DeviceCredentialsType;
    switch (credentialsType) {
      case DeviceCredentialsType.ACCESS_TOKEN:
        this.deviceCredentialsFormGroup.get('credentialsId').setValidators([Validators.required, Validators.pattern(/^.{1,20}$/)]);
        this.deviceCredentialsFormGroup.get('credentialsId').updateValueAndValidity({emitEvent: false});
        this.deviceCredentialsFormGroup.get('credentialsValue').setValidators([]);
        this.deviceCredentialsFormGroup.get('credentialsValue').updateValueAndValidity({emitEvent: false});
        this.deviceCredentialsFormGroup.get('credentialsBasic').disable({emitEvent: false});
        break;
      case DeviceCredentialsType.X509_CERTIFICATE:
      case DeviceCredentialsType.LWM2M_CREDENTIALS:
        this.deviceCredentialsFormGroup.get('credentialsValue').setValidators([Validators.required]);
        this.deviceCredentialsFormGroup.get('credentialsValue').updateValueAndValidity({emitEvent: false});
        this.deviceCredentialsFormGroup.get('credentialsId').setValidators([]);
        this.deviceCredentialsFormGroup.get('credentialsId').updateValueAndValidity({emitEvent: false});
        this.deviceCredentialsFormGroup.get('credentialsBasic').disable({emitEvent: false});
        break;
      case DeviceCredentialsType.MQTT_BASIC:
        this.deviceCredentialsFormGroup.get('credentialsBasic').enable({emitEvent: false});
        this.deviceCredentialsFormGroup.get('credentialsBasic').updateValueAndValidity({emitEvent: false});
        this.deviceCredentialsFormGroup.get('credentialsId').setValidators([]);
        this.deviceCredentialsFormGroup.get('credentialsId').updateValueAndValidity({emitEvent: false});
        this.deviceCredentialsFormGroup.get('credentialsValue').setValidators([]);
        this.deviceCredentialsFormGroup.get('credentialsValue').updateValueAndValidity({emitEvent: false});
        break;
    }
  }

  private atLeastOne(validator: ValidatorFn, controls: string[] = null) {
    return (group: FormGroup): ValidationErrors | null => {
      if (!controls) {
        controls = Object.keys(group.controls);
      }
      const hasAtLeastOne = group?.controls && controls.some(k => !validator(group.controls[k]));

      return hasAtLeastOne ? null : {atLeastOne: true};
    };
  }

  passwordChanged() {
    const value = this.deviceCredentialsFormGroup.get('credentialsBasic.password').value;
    if (value !== '') {
      this.deviceCredentialsFormGroup.get('credentialsBasic.userName').setValidators([Validators.required]);
    } else {
      this.deviceCredentialsFormGroup.get('credentialsBasic.userName').setValidators([]);
    }
    this.deviceCredentialsFormGroup.get('credentialsBasic.userName').updateValueAndValidity({
      emitEvent: false,
      onlySelf: true
    });
  }

  openSecurityInfoLwM2mDialog($event: Event): void {
    if ($event) {
      $event.stopPropagation();
      $event.preventDefault();
    }
    let credentialsValue = this.deviceCredentialsFormGroup.get('credentialsValue').value;
    if (credentialsValue === null || credentialsValue.length === 0) {
      credentialsValue = getDefaultSecurityConfig();
    } else {
      credentialsValue = JSON.parse(credentialsValue);
    }
    const credentialsId = this.deviceCredentialsFormGroup.get('credentialsId').value || DEFAULT_END_POINT;
    this.dialog.open<SecurityConfigComponent, DeviceCredentialsDialogLwm2mData, object>(SecurityConfigComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        jsonAllConfig: credentialsValue,
        endPoint: credentialsId
      }
    }).afterClosed().subscribe(
      (res) => {
        if (res) {
          this.deviceCredentialsFormGroup.patchValue({
            credentialsValue: this.isDefautLw2mResponse(res[JSON_ALL_CONFIG]) ? null : JSON.stringify(res[JSON_ALL_CONFIG]),
            credentialsId: this.isDefautLw2mResponse(res[END_POINT]) ? null : JSON.stringify(res[END_POINT]).split('\"').join('')
          });
          this.deviceCredentialsFormGroup.get('credentialsValue').markAsDirty();
        }
      }
    );
  }

  private isDefautLw2mResponse(response: object): boolean {
    return Object.keys(response).length === 0 || JSON.stringify(response) === '[{}]';
  }
}
