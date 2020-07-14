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
  SECURITY_CONFIG_MODE_NAMES,
  SECURITY_CONFIG_MODE,
  SecurityConfigModels,
  gatDefaultSecurityConfig,
  ClientSecurityInfoPSK, ClientSecurityInfoRPK, BOOTSTRAP_SERVER, LWM2M_SERVER, JSON_PROFILE, SecurityConfig
} from "./security-config.models";
import {WINDOW} from "@core/services/window.service";

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
  securityConfigLwM2MType = SECURITY_CONFIG_MODE;
  securityConfigLwM2MTypes = Object.keys(SECURITY_CONFIG_MODE);
  credentialTypeLwM2MNamesMap = SECURITY_CONFIG_MODE_NAMES;
  formControlNameJsonAllConfig: string;
  jsonAllConfig: SecurityConfigModels;
  formControlNameJsonProfile: string;
  jsonObserveData: {};
  bootstrapServerData: SecurityConfig;
  bootstrapServer: string;
  lwm2mServerData: SecurityConfig;
  lwm2mServer: string;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: DeviceCredentialsDialogLwm2mData,
              public dialogRef: MatDialogRef<SecurityConfigComponent, object>,
              public fb: FormBuilder,
              private translate: TranslateService,
              @Inject(WINDOW) private window: Window) {
    super(store, router, dialogRef);
  }

  ngOnInit(): void {
    this.formControlNameJsonAllConfig = Object.keys(this.data).entries().next().value[1];
    this.formControlNameJsonProfile = JSON_PROFILE;
    this.title = this.data.title ? this.data.title : this.translate.instant('details.edit-json');
    this.jsonObserveData = this.data.jsonAllConfig;
    this.jsonAllConfig = JSON.parse(JSON.stringify(this.data.jsonAllConfig)) as SecurityConfigModels;
    this.bootstrapServer = BOOTSTRAP_SERVER;
    this.bootstrapServerData = (this.jsonAllConfig.bootstrap[this.bootstrapServer]) ? this.jsonAllConfig.bootstrap[this.bootstrapServer] : {} as SecurityConfig;
    this.lwm2mServer = LWM2M_SERVER;
    this.lwm2mServerData = (this.jsonAllConfig.bootstrap[this.lwm2mServer]) ? this.jsonAllConfig.bootstrap[this.lwm2mServer] : {} as SecurityConfig;
    this.lwm2mConfigFormGroup = this.fb.group({
      jsonAllConfig: [this.jsonAllConfig, []],
      bootstrapServer: [this.bootstrapServerData, []],
      lwm2mServer: [this.lwm2mServerData, []],
      jsonProfile: [this.jsonObserveData, []],
      endPoint: [this.data.endPoint, []],
      securityConfigClientMode: SECURITY_CONFIG_MODE.NO_SEC,
      identityPSK: ['', []],
      clientKey: ['', []],
      clientCertificate: [false],
      shortId: [null, Validators.required],
      lifetime: [null, Validators.required],
      defaultMinPeriod: [],
      notifIfDisabled: [true],
      binding: ['', Validators.required]
    });
    if (this.isReadOnly) {
      this.lwm2mConfigFormGroup.disable({emitEvent: false});
    } else {
      this.registerDisableOnLoadFormControl(this.lwm2mConfigFormGroup.get('securityConfigClientMode'));
    }
    this.loadConfigFromParent();
  }

  securityConfigClientModeChanged(): void {
    const securityConfigClientMode = this.lwm2mConfigFormGroup.get('securityConfigClientMode').value as SECURITY_CONFIG_MODE;
    switch (securityConfigClientMode) {
      case SECURITY_CONFIG_MODE.NO_SEC:
      case SECURITY_CONFIG_MODE.X509:
        this.lwm2mConfigFormGroup.patchValue({
          identityPSK: '',
          clientKey: '',
          clientCertificate: true
        }, {emitEvent: true});
        break;
      default:
        this.lwm2mConfigFormGroup.patchValue({
          identityPSK: '',
          clientKey: '',
          clientCertificate: false
        }, {emitEvent: true});
    }
    this.securityConfigDefault();
    this.securityConfigClientUpdateValidators();
  }

  securityConfigClientUpdateValidators(): void {
    const securityConfigClientMode = this.lwm2mConfigFormGroup.get('securityConfigClientMode').value as SECURITY_CONFIG_MODE;
    switch (securityConfigClientMode) {
      case SECURITY_CONFIG_MODE.NO_SEC:
      case SECURITY_CONFIG_MODE.X509:
        this.lwm2mConfigFormGroup.get('identityPSK').setValidators([]);
        this.lwm2mConfigFormGroup.get('identityPSK').updateValueAndValidity();
        this.lwm2mConfigFormGroup.get('clientKey').setValidators([]);
        this.lwm2mConfigFormGroup.get('clientKey').updateValueAndValidity();
        break;
      case SECURITY_CONFIG_MODE.PSK:
        this.lwm2mConfigFormGroup.get('identityPSK').setValidators([]);
        this.lwm2mConfigFormGroup.get('identityPSK').updateValueAndValidity();
        this.lwm2mConfigFormGroup.get('clientKey').setValidators([Validators.pattern(/^[0-9a-fA-F]{64,64}$/)]);
        this.lwm2mConfigFormGroup.get('clientKey').updateValueAndValidity();
        break;
      case SECURITY_CONFIG_MODE.RPK:
        this.lwm2mConfigFormGroup.get('clientKey').setValidators([Validators.pattern(/^[0-9a-fA-F]{182,182}$/)]);
        this.lwm2mConfigFormGroup.get('clientKey').updateValueAndValidity();
        break;

    }
  }

  bootstrapServersUpdateValidators(): void {
    this.lwm2mConfigFormGroup.get('shortId').setValidators([]);
    this.lwm2mConfigFormGroup.get('shortId').updateValueAndValidity();
    this.lwm2mConfigFormGroup.get('lifetime').setValidators([]);
    this.lwm2mConfigFormGroup.get('lifetime').updateValueAndValidity();
    this.lwm2mConfigFormGroup.get('defaultMinPeriod').setValidators([]);
    this.lwm2mConfigFormGroup.get('defaultMinPeriod').updateValueAndValidity();
    this.lwm2mConfigFormGroup.get('binding').setValidators([]);
    this.lwm2mConfigFormGroup.get('binding').updateValueAndValidity();
  }

  private securityConfigDefault(jsonAllConfig: SecurityConfigModels = gatDefaultSecurityConfig(this.lwm2mConfigFormGroup.get('securityConfigClientMode').value, this.lwm2mConfigFormGroup.get('endPoint').value.split('\"').join(''))): void {
    jsonAllConfig.bootstrap.bootstrapServer.host = this.window.location.hostname;
    jsonAllConfig.bootstrap.lwm2mServer.host = this.window.location.hostname;
    if (jsonAllConfig.client.securityModeServer.toString()=== SECURITY_CONFIG_MODE.NO_SEC.toString()){
      jsonAllConfig.bootstrap.bootstrapServer.port = 5687;
      jsonAllConfig.bootstrap.lwm2mServer.port = 5685;
    }
    this.jsonAllConfig = jsonAllConfig
    this.upDatejsonAllConfig ();
  }

  loadConfigFromParent(): void {
    let jsonAllConfig = this.lwm2mConfigFormGroup.get(this.formControlNameJsonAllConfig).value as SecurityConfigModels;
    // let securityConfigClientMode: SECURITY_CONFIG_MODE;
    this.lwm2mConfigFormGroup.patchValue({
      securityConfigClientMode: SECURITY_CONFIG_MODE[jsonAllConfig.client.securityModeServer.toString()]
    }, {emitEvent: false});
    switch (jsonAllConfig.client.securityModeServer.toString()) {
      case SECURITY_CONFIG_MODE.PSK.toString():
        // securityConfigClientMode = SECURITY_CONFIG_MODE[jsonAllConfig.client.securityModeServer.toString()];
        const clientSecurityInfoPSK = jsonAllConfig.client as ClientSecurityInfoPSK;
        this.lwm2mConfigFormGroup.patchValue({
          identityPSK: clientSecurityInfoPSK.identity,
          clientKey: clientSecurityInfoPSK.key,
        }, {emitEvent: true});
        break;
      case SECURITY_CONFIG_MODE.RPK.toString():
        // securityConfigClientMode = SECURITY_CONFIG_MODE.RPK;
        const clientSecurityInfoRPK = jsonAllConfig.client as ClientSecurityInfoRPK;
        this.lwm2mConfigFormGroup.patchValue({
          clientKey: clientSecurityInfoRPK.key,
        }, {emitEvent: true});
        break;
      case SECURITY_CONFIG_MODE.X509.toString():
        // securityConfigClientMode = SECURITY_CONFIG_MODE.X509;
        this.lwm2mConfigFormGroup.patchValue({
          X509: true,
        }, {emitEvent: true});
        break;
      case SECURITY_CONFIG_MODE.NO_SEC.toString():
        // securityConfigClientMode = SECURITY_CONFIG_MODE.NO_SEC;
        break;
    }

    this.securityConfigClientUpdateValidators();
    this.lwm2mConfigFormGroup.patchValue({
      shortId: jsonAllConfig.bootstrap.servers.shortId,
      lifetime: jsonAllConfig.bootstrap.servers.lifetime,
      defaultMinPeriod: jsonAllConfig.bootstrap.servers.defaultMinPeriod,
      notifIfDisabled: jsonAllConfig.bootstrap.servers.notifIfDisabled,
      binding: jsonAllConfig.bootstrap.servers.binding
    }, {emitEvent: true});
    this.bootstrapServersUpdateValidators();
  }

  upDatejsonAllConfig(): void {
    this.lwm2mConfigFormGroup.patchValue({
      jsonAllConfig: JSON.parse(JSON.stringify(this.jsonAllConfig))
    }, {emitEvent: false});
  }

  cancel(): void {
    this.dialogRef.close(undefined);
  }

  save(): void {
    this.data.endPoint = this.lwm2mConfigFormGroup.get('endPoint').value.split('\"').join('');
    this.data.jsonAllConfig = this.lwm2mConfigFormGroup.get(this.formControlNameJsonAllConfig).value;
    this.jsonObserveData = this.lwm2mConfigFormGroup.get(this.formControlNameJsonProfile).value;
    this.dialogRef.close(this.data);
  }

}


