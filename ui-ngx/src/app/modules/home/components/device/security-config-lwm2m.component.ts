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


import { Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';
import {
  DeviceCredentialsDialogLwm2mData,
  getClientSecurityConfig,
  JSON_ALL_CONFIG,
  KEY_REGEXP_HEX_DEC,
  LEN_MAX_PSK,
  LEN_MAX_PUBLIC_KEY_RPK,
  Lwm2mSecurityConfigModels,
  Lwm2mSecurityType,
  Lwm2mSecurityTypeTranslationMap
} from '@shared/models/lwm2m-security-config.models';
import { MatTabChangeEvent } from '@angular/material/tabs';
import { MatTab } from '@angular/material/tabs/tab';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

@Component({
  selector: 'tb-security-config-lwm2m',
  templateUrl: './security-config-lwm2m.component.html',
  styleUrls: ['./security-config-lwm2m.component.scss']
})

export class SecurityConfigLwm2mComponent extends DialogComponent<SecurityConfigLwm2mComponent, object> implements OnInit, OnDestroy {

  private destroy$ = new Subject();

  lwm2mConfigFormGroup: FormGroup;
  title: string;
  securityConfigLwM2MType = Lwm2mSecurityType;
  securityConfigLwM2MTypes = Object.keys(Lwm2mSecurityType);
  credentialTypeLwM2MNamesMap = Lwm2mSecurityTypeTranslationMap;
  formControlNameJsonAllConfig = JSON_ALL_CONFIG;
  jsonAllConfig: Lwm2mSecurityConfigModels;
  lenMaxKeyClient = LEN_MAX_PSK;
  tabPrevious: MatTab;
  tabIndexPrevious = 0;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: DeviceCredentialsDialogLwm2mData,
              public dialogRef: MatDialogRef<SecurityConfigLwm2mComponent, object>,
              public fb: FormBuilder,
              public translate: TranslateService) {
    super(store, router, dialogRef);
  }

  ngOnInit() {
    this.jsonAllConfig = JSON.parse(JSON.stringify(this.data.jsonAllConfig));
    this.lwm2mConfigFormGroup = this.initLwm2mConfigFormGroup();
    this.title = this.translate.instant('device.lwm2m-security-info') + ': ' + this.data.endPoint;
    this.lwm2mConfigFormGroup.get('x509').disable();
    this.initClientSecurityConfig(this.lwm2mConfigFormGroup.get('jsonAllConfig').value);
    this.registerDisableOnLoadFormControl(this.lwm2mConfigFormGroup.get('securityConfigClientMode'));
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private initClientSecurityConfig = (jsonAllConfig: Lwm2mSecurityConfigModels): void =>  {
    if (jsonAllConfig.client.securityConfigClientMode !== Lwm2mSecurityType.NO_SEC) {
      this.lwm2mConfigFormGroup.patchValue(jsonAllConfig.client, {emitEvent: false});
    }
    this.securityConfigClientUpdateValidators(jsonAllConfig.client.securityConfigClientMode);
  }

  private securityConfigClientModeChanged(type: Lwm2mSecurityType): void {
    const config = getClientSecurityConfig(type, this.lwm2mConfigFormGroup.get('endPoint').value);
    switch (type) {
      case Lwm2mSecurityType.PSK:
        config.identity = this.data.endPoint;
        config.key = this.lwm2mConfigFormGroup.get('key').value;
        break;
      case Lwm2mSecurityType.RPK:
        config.key = this.lwm2mConfigFormGroup.get('key').value;
        break;
    }
    this.jsonAllConfig.client = config;
    this.lwm2mConfigFormGroup.patchValue({
      ...config,
      jsonAllConfig: this.jsonAllConfig
    }, {emitEvent: false});
    this.securityConfigClientUpdateValidators(type);
  }

  private securityConfigClientUpdateValidators = (mode: Lwm2mSecurityType): void => {
    switch (mode) {
      case Lwm2mSecurityType.NO_SEC:
      case Lwm2mSecurityType.X509:
        this.setValidatorsNoSecX509();
        break;
      case Lwm2mSecurityType.PSK:
        this.lenMaxKeyClient = LEN_MAX_PSK;
        this.setValidatorsPskRpk(mode);
        break;
      case Lwm2mSecurityType.RPK:
        this.lenMaxKeyClient = LEN_MAX_PUBLIC_KEY_RPK;
        this.setValidatorsPskRpk(mode);
        break;
    }
    this.lwm2mConfigFormGroup.get('identity').updateValueAndValidity({emitEvent: false});
    this.lwm2mConfigFormGroup.get('key').updateValueAndValidity({emitEvent: false});
  }

  private setValidatorsNoSecX509 = (): void => {
    this.lwm2mConfigFormGroup.get('identity').setValidators([]);
    this.lwm2mConfigFormGroup.get('key').setValidators([]);
  }

  private setValidatorsPskRpk = (mode: Lwm2mSecurityType): void => {
    if (mode === Lwm2mSecurityType.PSK) {
      this.lwm2mConfigFormGroup.get('identity').setValidators([Validators.required]);
    } else {
      this.lwm2mConfigFormGroup.get('identity').setValidators([]);
    }
    this.lwm2mConfigFormGroup.get('key').setValidators([Validators.required,
      Validators.pattern(KEY_REGEXP_HEX_DEC),
      Validators.maxLength(this.lenMaxKeyClient), Validators.minLength(this.lenMaxKeyClient)]);
  }

  tabChanged = (tabChangeEvent: MatTabChangeEvent): void => {
    if (this.tabIndexPrevious !== tabChangeEvent.index) {
      this.upDateValueToJson();
    }
    this.tabIndexPrevious = tabChangeEvent.index;
  }

  private upDateValueToJson(): void {
    switch (this.tabIndexPrevious) {
      case 0:
        this.upDateValueToJsonTab0();
        break;
      case 1:
        this.upDateValueToJsonTab1();
        break;
    }
  }

  private upDateValueToJsonTab0 = (): void => {
    if (this.lwm2mConfigFormGroup.get('identity').dirty && this.lwm2mConfigFormGroup.get('identity').valid ||
        this.lwm2mConfigFormGroup.get('key').dirty && this.lwm2mConfigFormGroup.get('key').valid) {
      this.updateBootstrapSettings();
      this.upDateJsonAllConfig();
    }
  }

  private upDateValueToJsonTab1 = (): void => {
    const bootstrap = this.lwm2mConfigFormGroup.get('bootstrapServer').value;
    if (bootstrap !== null
      && this.lwm2mConfigFormGroup.get('bootstrapServer').dirty
      && this.lwm2mConfigFormGroup.get('bootstrapServer').valid) {
      this.jsonAllConfig.bootstrap.bootstrapServer = bootstrap;
      this.upDateJsonAllConfig();
    }
    const serverConfig = this.lwm2mConfigFormGroup.get('lwm2mServer').value;
    if (serverConfig !== null
      && this.lwm2mConfigFormGroup.get('lwm2mServer').dirty
      && this.lwm2mConfigFormGroup.get('lwm2mServer').valid) {
      this.jsonAllConfig.bootstrap.lwm2mServer = serverConfig;
      this.upDateJsonAllConfig();
    }
  }

  private updateBootstrapSettings() {
    const securityMode = 'securityMode';
    this.jsonAllConfig.client.identity =  this.lwm2mConfigFormGroup.get('identity').value;
    this.jsonAllConfig.client.key = this.lwm2mConfigFormGroup.get('key').value;
    if (this.lwm2mConfigFormGroup.get('bootstrapServer').value[securityMode] === Lwm2mSecurityType.PSK) {
      this.jsonAllConfig.bootstrap.bootstrapServer.clientPublicKeyOrId = this.jsonAllConfig.client.identity;
      this.jsonAllConfig.bootstrap.bootstrapServer.clientSecretKey = this.jsonAllConfig.client.key;
      this.lwm2mConfigFormGroup.get('bootstrapServer').patchValue(this.jsonAllConfig.bootstrap.bootstrapServer, {emitEvent: false});
    }
    if (this.lwm2mConfigFormGroup.get('lwm2mServer').value[securityMode] === Lwm2mSecurityType.PSK) {
      this.jsonAllConfig.bootstrap.lwm2mServer.clientPublicKeyOrId = this.jsonAllConfig.client.identity;
      this.jsonAllConfig.bootstrap.lwm2mServer.clientSecretKey = this.jsonAllConfig.client.key;
      this.lwm2mConfigFormGroup.get('lwm2mServer').patchValue(this.jsonAllConfig.bootstrap.lwm2mServer, {emitEvent: false});
    }
  }

  private upDateJsonAllConfig = (): void => {
    this.lwm2mConfigFormGroup.patchValue({
      jsonAllConfig: this.jsonAllConfig
    }, {emitEvent: false});
  }

  private initLwm2mConfigFormGroup = (): FormGroup => {
    if (this.jsonAllConfig.client.securityConfigClientMode === Lwm2mSecurityType.PSK) {
      this.data.endPoint = this.jsonAllConfig.client.endpoint;
    }
    const formGroup =  this.fb.group({
      securityConfigClientMode: [this.jsonAllConfig.client.securityConfigClientMode],
      identity: [''],
      key: [''],
      x509: [false],
      bootstrapServer: [this.jsonAllConfig.bootstrap.bootstrapServer],
      lwm2mServer: [this.jsonAllConfig.bootstrap.lwm2mServer],
      endPoint: [this.data.endPoint],
      jsonAllConfig: [this.jsonAllConfig]
    });
    formGroup.get('securityConfigClientMode').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((type) => {
      this.securityConfigClientModeChanged(type);
    });
    formGroup.get('endPoint').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((endpoint) => {
      if (formGroup.get('securityConfigClientMode').value === Lwm2mSecurityType.PSK) {
        this.jsonAllConfig.client.endpoint = endpoint;
        this.upDateJsonAllConfig();
      }
    });
    return formGroup;
  }

  save(): void {
    this.upDateValueToJson();
    this.data.endPoint = this.lwm2mConfigFormGroup.get('endPoint').value.split('\'').join('');
    this.data.jsonAllConfig = this.jsonAllConfig;
    if (this.lwm2mConfigFormGroup.get('securityConfigClientMode').value === Lwm2mSecurityType.PSK) {
      this.data.endPoint = this.data.jsonAllConfig.client.identity;
    }
    this.dialogRef.close(this.data);
  }

  cancel(): void {
    this.dialogRef.close(undefined);
  }
}


