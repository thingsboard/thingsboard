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


import {Component, Inject, OnInit} from '@angular/core';
import {DialogComponent} from '@shared/components/dialog.component';
import {Store} from '@ngrx/store';
import {AppState} from '@core/core.state';
import {Router} from '@angular/router';
import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material/dialog';
import {FormBuilder, FormGroup, Validators} from '@angular/forms';
import {TranslateService} from '@ngx-translate/core';
import {
  credentialTypeLwM2MNames, DefaultSecurityConfigModels,
  DeviceSecurityConfigLwM2MType, END_POINT, SecurityConfigModels
} from "./security-config.models";

export interface DeviceCredentialsDialogLwm2mData {
  jsonAllConfig: object;
  title?: string;
  endPoint: string;
}

@Component({
  selector: 'tb-security-config-lwm2m',
  templateUrl: './security-config.component.html',
  styleUrls: []
})

export class SecurityConfigComponent extends DialogComponent<SecurityConfigComponent, object> implements OnInit {

  lwm2mConfigFormGroup: FormGroup;
  isReadOnly: boolean;
  title: string;
  submitted = false;
  securityConfigLwM2MType = DeviceSecurityConfigLwM2MType;
  securityConfigLwM2MTypes = Object.keys(DeviceSecurityConfigLwM2MType);
  credentialTypeLwM2MNamesMap = credentialTypeLwM2MNames;
  formControlNameJsonObject: string;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: DeviceCredentialsDialogLwm2mData,
              public dialogRef: MatDialogRef<SecurityConfigComponent, object>,
              public fb: FormBuilder,
              private translate: TranslateService) {
    super(store, router, dialogRef);
  }

  ngOnInit(): void {
    debugger
    this.formControlNameJsonObject = Object.keys(this.data).entries().next().value[1];
    this.title = this.data.title ? this.data.title : this.translate.instant('details.edit-json');
    this.lwm2mConfigFormGroup = this.fb.group({
      jsonAllConfig: [this.data.jsonAllConfig, []],
      endPoint: [this.data.endPoint, []],
      securityConfigClientMode: DeviceSecurityConfigLwM2MType.NOSEC,
      identityPSK: [''],
      clientKey: [''],
      clientCertificate: [false],
      curServerShortId: [],
      curServerLifetime: [],
      curServerDefaultMinPeriod: [],
      curServerBinding: []
    });
    if (this.isReadOnly) {
      this.lwm2mConfigFormGroup.disable({emitEvent: false});
    } else {
      this.registerDisableOnLoadFormControl(this.lwm2mConfigFormGroup.get('securityConfigClientMode'));
    }
    this.loadConfigFromParent();
  }

  securityConfigClientModeChanged(): void {
    const securityConfigClientMode = this.lwm2mConfigFormGroup.get('securityConfigClientMode').value as DeviceSecurityConfigLwM2MType;
    switch (securityConfigClientMode) {
      case DeviceSecurityConfigLwM2MType.NOSEC:
      case DeviceSecurityConfigLwM2MType.X509:
        this.lwm2mConfigFormGroup.patchValue({
          identityPSK: null,
          clientKey: null,
          clientCertificate: true
        }, {emitEvent: true});
        break;
      default:
        this.lwm2mConfigFormGroup.patchValue({
          identityPSK: null,
          clientKey: null,
          clientCertificate: false
        }, {emitEvent: true});
    }
    this.securityConfigClientDefault();
    this.securityConfigClientUpdateValidators();
  }

  securityConfigClientUpdateValidators(): void {
    const securityConfigClientMode = this.lwm2mConfigFormGroup.get('securityConfigClientMode').value as DeviceSecurityConfigLwM2MType;
    switch (securityConfigClientMode) {
      case DeviceSecurityConfigLwM2MType.NOSEC:
      case DeviceSecurityConfigLwM2MType.X509:
        this.lwm2mConfigFormGroup.get('identityPSK').setValidators([]);
        this.lwm2mConfigFormGroup.get('identityPSK').updateValueAndValidity();
        this.lwm2mConfigFormGroup.get('clientKey').setValidators([]);
        this.lwm2mConfigFormGroup.get('clientKey').updateValueAndValidity();
        break;
      case DeviceSecurityConfigLwM2MType.PSK:
        this.lwm2mConfigFormGroup.get('identityPSK').setValidators([]);
        this.lwm2mConfigFormGroup.get('identityPSK').updateValueAndValidity();
        this.lwm2mConfigFormGroup.get('clientKey').setValidators([Validators.pattern(/^[0-9a-fA-F]{64,64}$/)]);
        this.lwm2mConfigFormGroup.get('clientKey').updateValueAndValidity();
        break;
      case DeviceSecurityConfigLwM2MType.RPK:
        this.lwm2mConfigFormGroup.get('clientKey').setValidators([Validators.pattern(/^[0-9a-fA-F]{182,182}$/)]);
        this.lwm2mConfigFormGroup.get('clientKey').updateValueAndValidity();
        break;

    }
  }

  securityConfigClientDefault(): void {
    this.lwm2mConfigFormGroup.patchValue({
      jsonAllConfig: JSON.parse(JSON.stringify(DefaultSecurityConfigModels))
    }, {emitEvent: false});
  }

  loadConfigFromParent(): void {
    this.lwm2mConfigFormGroup.patchValue({
      securityConfigClientMode: DeviceSecurityConfigLwM2MType.NOSEC
    });
    this.securityConfigClientUpdateValidators();
  }

  cancel(): void {
    this.dialogRef.close(undefined);
  }

  save(): void {
    this.data.endPoint = this.lwm2mConfigFormGroup.get(END_POINT).value;
    this.data.jsonAllConfig = this.lwm2mConfigFormGroup.get(this.formControlNameJsonObject).value;
    this.dialogRef.close(this.data);
  }

}

