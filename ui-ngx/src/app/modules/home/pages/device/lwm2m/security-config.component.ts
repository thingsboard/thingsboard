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
import {ControlValueAccessor, FormBuilder, FormGroup, Validators} from '@angular/forms';
import {TranslateService} from '@ngx-translate/core';
import {
  SECURITY_CONFIG_MODE_NAMES,
  SECURITY_CONFIG_MODE,
  SecurityConfigModels,
  ClientSecurityConfigPSK,
  ClientSecurityConfigRPK,
  JSON_OBSERVE,
  ServerSecurityConfig,
  OBSERVE,
  ObjectLwM2M,
  JSON_ALL_CONFIG,
  KEY_IDENT_REGEXP_PSK,
  KEY_PUBLIC_REGEXP,
  DEFAULT_END_POINT,
  DeviceCredentialsDialogLwm2mData,
  BOOTSTRAP_SERVER,
  BOOTSTRAP_SERVERS,
  LWM2M_SERVER,
  DEFAULT_ID_SERVER,
  DEFAULT_LIFE_TIME, DEFAULT_DEFAULT_MIN_PERIOD, DEFAULT_BINDING, ClientSecurityConfigX509
} from "./security-config.models";
import {WINDOW} from "@core/services/window.service";


@Component({
  selector: 'tb-security-config-lwm2m',
  templateUrl: './security-config.component.html',
  styleUrls: []
})

export class SecurityConfigComponent extends DialogComponent<SecurityConfigComponent, object> implements OnInit {

  lwm2mConfigFormGroup: FormGroup;
  title: string;
  endPoint: string;
  charactersMinMax: string;
  lenMinMax: number;
  submitted = false;
  securityConfigLwM2MType = SECURITY_CONFIG_MODE;
  securityConfigLwM2MTypes = Object.keys(SECURITY_CONFIG_MODE);
  credentialTypeLwM2MNamesMap = SECURITY_CONFIG_MODE_NAMES;
  formControlNameJsonAllConfig = JSON_ALL_CONFIG;
  jsonAllConfig: SecurityConfigModels;
  bootstrapServersData: ServerSecurityConfig;
  bootstrapServers: string;
  bootstrapServerData: ServerSecurityConfig;
  bootstrapServer: string;
  lwm2mServerData: ServerSecurityConfig;
  lwm2mServer: string;
  formControlNameJsonObserve: string;
  observeData: ObjectLwM2M[];
  jsonObserveData: {};
  observe: string;
  bootstrapFormGroup: FormGroup;
  lwm2mServerFormGroup: FormGroup;
  observeFormGroup: FormGroup;
  isDirty = false;

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
    debugger
    this.initSecurityConfigModels();
    this.endPoint = this.data.endPoint;
    this.jsonAllConfig = JSON.parse(JSON.stringify(this.data.jsonAllConfig)) as SecurityConfigModels;
    this.initConstants();
    this.lwm2mConfigFormGroup = this.initLwm2mConfigFormGroup();
    this.title = this.translate.instant('device.lwm2m-security-info') + ": " + this.endPoint
    this.charactersMinMax = this.translate.instant('device.lwm2m-security-config.characters64');
    this.lenMinMax = 64;
    this.lwm2mConfigFormGroup.get('clientCertificate').disable();
    this.initChildesFormGroup();
    this.initClientSecurityConfig(this.lwm2mConfigFormGroup.get('jsonAllConfig').value);
    this.registerDisableOnLoadFormControl(this.lwm2mConfigFormGroup.get('securityConfigClientMode'));
    this.upDateValueFromObject();
  }

  initConstants(): void {
    this.bootstrapServers = BOOTSTRAP_SERVERS;
    this.bootstrapServer = BOOTSTRAP_SERVER;
    this.lwm2mServer = LWM2M_SERVER;
    this.observe = OBSERVE;
    this.formControlNameJsonObserve = JSON_OBSERVE;
  }

  initSecurityConfigModels(): void {
    if (!this.data.endPoint) {
      this.data.endPoint = DEFAULT_END_POINT;
      this.isDirty = true;
    }
    if (this.data.jsonAllConfig.bootstrap.bootstrapServer.host.length === 0) {
      this.data.jsonAllConfig.bootstrap.bootstrapServer.host = this.window.location.hostname;
      this.isDirty = true;
    }
    if (this.data.jsonAllConfig.bootstrap.lwm2mServer.host.length === 0) {
      this.data.jsonAllConfig.bootstrap.lwm2mServer.host = this.window.location.hostname;
      this.isDirty = true;
    }
  }

  initChildesFormGroup(): void {
    this.bootstrapFormGroup = this.lwm2mConfigFormGroup.get('bootstrapFormGroup') as FormGroup;
    this.bootstrapFormGroup = this.lwm2mConfigFormGroup.get('bootstrapFormGroup') as FormGroup;
    this.lwm2mServerFormGroup = this.lwm2mConfigFormGroup.get('lwm2mServerFormGroup') as FormGroup;
    this.observeFormGroup = this.lwm2mConfigFormGroup.get('observeFormGroup') as FormGroup;
  }

  initClientSecurityConfig(jsonAllConfig: SecurityConfigModels): void {
    switch (jsonAllConfig.client.securityConfigClientMode.toString()) {
      case SECURITY_CONFIG_MODE.PSK.toString():
        const clientSecurityConfigPSK = jsonAllConfig.client as ClientSecurityConfigPSK;
        this.lwm2mConfigFormGroup.patchValue({
          identityPSK: clientSecurityConfigPSK.identity,
          clientKey: clientSecurityConfigPSK.key,
        }, {emitEvent: true});
        break;
      case SECURITY_CONFIG_MODE.RPK.toString():
        const clientSecurityConfigRPK = jsonAllConfig.client as ClientSecurityConfigRPK;
        this.lwm2mConfigFormGroup.patchValue({
          clientKey: clientSecurityConfigRPK.key,
        }, {emitEvent: true});
        break;
      case SECURITY_CONFIG_MODE.X509.toString():
        const clientSecurityConfigX509 = jsonAllConfig.client as ClientSecurityConfigX509;
        this.lwm2mConfigFormGroup.patchValue({
          X509: clientSecurityConfigX509.x509,
        }, {emitEvent: true});
        break;
      case SECURITY_CONFIG_MODE.NO_SEC.toString():
        break;
    }
    this.securityConfigClientUpdateValidators(this.lwm2mConfigFormGroup.get('securityConfigClientMode').value);
  }

  securityConfigClientModeChanged(mode: SECURITY_CONFIG_MODE): void {
    switch (mode) {
      case SECURITY_CONFIG_MODE.NO_SEC:
        this.lwm2mConfigFormGroup.patchValue({
          identityPSK: '',
          clientKey: '',
          clientCertificate: true
        }, {emitEvent: true});
        break;
      case SECURITY_CONFIG_MODE.X509:
        this.lwm2mConfigFormGroup.patchValue({
          identityPSK: '',
          clientKey: '',
          clientCertificate: true
        }, {emitEvent: true});
        break;
      case SECURITY_CONFIG_MODE.RPK:
        this.charactersMinMax = this.translate.instant('device.lwm2m-security-config.characters134');
        this.lenMinMax = 134;
        this.lwm2mConfigFormGroup.patchValue({
          identityPSK: '',
          clientKey: '',
          clientCertificate: false
        }, {emitEvent: true});
        break;
      case SECURITY_CONFIG_MODE.PSK:
        this.charactersMinMax = this.translate.instant('device.lwm2m-security-config.characters64');
        this.lenMinMax = 64;
        this.lwm2mConfigFormGroup.patchValue({
          identityPSK: '',
          clientKey: '',
          clientCertificate: false
        }, {emitEvent: true});
        break;
    }
    this.securityConfigClientUpdateValidators(mode);
  }

  securityConfigClientUpdateValidators(mode: SECURITY_CONFIG_MODE): void {
    switch (mode) {
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
        this.lwm2mConfigFormGroup.get('clientKey').setValidators([Validators.required, Validators.pattern(KEY_IDENT_REGEXP_PSK)]);
        this.lwm2mConfigFormGroup.get('clientKey').updateValueAndValidity();
        break;
      case SECURITY_CONFIG_MODE.RPK:
        this.lwm2mConfigFormGroup.get('clientKey').setValidators([Validators.required, Validators.pattern(KEY_PUBLIC_REGEXP)]);
        this.lwm2mConfigFormGroup.get('clientKey').updateValueAndValidity();
        break;
    }
  }

  upDateValueFromObject(): void {
    this.lwm2mConfigFormGroup.get(JSON_ALL_CONFIG).valueChanges.subscribe(val => {
      if (!this.lwm2mConfigFormGroup.get(JSON_ALL_CONFIG).pristine && this.lwm2mConfigFormGroup.get(JSON_ALL_CONFIG).valid) {
        this.data.jsonAllConfig = val;
        console.log(JSON_ALL_CONFIG + ': ', val);
      }
    })

    this.bootstrapFormGroup.valueChanges.subscribe(val => {
      if (!this.bootstrapFormGroup.pristine && this.bootstrapFormGroup.valid) {
        console.log('bootstrapFormGroup: ', val);
      }
    })

    this.lwm2mServerFormGroup.valueChanges.subscribe(val => {
      if (!this.lwm2mServerFormGroup.pristine && this.lwm2mServerFormGroup.valid) {
        console.log('lwm2mServerFormGroup: ', val);
      }
    })

    this.lwm2mConfigFormGroup.get(JSON_OBSERVE).valueChanges.subscribe(val => {
      if (!this.lwm2mConfigFormGroup.get(JSON_OBSERVE).pristine && this.lwm2mConfigFormGroup.get(JSON_OBSERVE).valid) {
        console.log(JSON_OBSERVE + ': ', val);
      }
    })

    this.observeFormGroup.valueChanges.subscribe(val => {
      if (!this.observeFormGroup.pristine && this.observeFormGroup.valid) {
        console.log('observeFormGroup: ', val);
      }
    })
  }

  upDateJsonAllConfig(): void {
    this.data.jsonAllConfig = JSON.parse(JSON.stringify(this.jsonAllConfig));
    this.lwm2mConfigFormGroup.patchValue({
      jsonAllConfig: JSON.parse(JSON.stringify(this.jsonAllConfig))
    }, {emitEvent: false});
    this.lwm2mConfigFormGroup.markAsDirty();
  }

  upDateBootstrapFormGroup(): void {
    this.data.jsonAllConfig = JSON.parse(JSON.stringify(this.jsonAllConfig));
    this.lwm2mConfigFormGroup.patchValue({
      jsonAllConfig: JSON.parse(JSON.stringify(this.jsonAllConfig))
    }, {emitEvent: false});
    this.lwm2mConfigFormGroup.markAsDirty();
  }

  initLwm2mConfigFormGroup(): FormGroup {
    return this.fb.group({
      jsonAllConfig: [this.jsonAllConfig, []],
      bootstrapServer: [this.jsonAllConfig.bootstrap[this.bootstrapServer], []],
      lwm2mServer: [this.jsonAllConfig.bootstrap[this.lwm2mServer], []],
      observe: [this.jsonAllConfig[this.observe], []],
      jsonObserve: [this.jsonAllConfig[this.observe], []],
      bootstrapFormGroup: this.getServerGroup(),
      lwm2mServerFormGroup: this.getServerGroup(),
      observeFormGroup: this.fb.group({}),
      endPoint: [this.endPoint, []],
      securityConfigClientMode: [SECURITY_CONFIG_MODE[this.jsonAllConfig.client.securityConfigClientMode.toString()], []],
      identityPSK: [[''], []],
      clientKey: [[''], []],
      clientCertificate: [false, []],
      shortId: [[DEFAULT_ID_SERVER], Validators.required],
      lifetime: [[DEFAULT_LIFE_TIME], Validators.required],
      defaultMinPeriod: [[DEFAULT_DEFAULT_MIN_PERIOD], Validators.required],
      notifIfDisabled: [true, []],
      binding: [[DEFAULT_BINDING], Validators.required]
    });
  }

  getServerGroup(): FormGroup {
    return this.fb.group({
      host: [[''], [Validators.required]],
      port: [[''], [Validators.required]],
      isBootstrapServer: [[false], []],
      securityMode: [[this.fb.control(SECURITY_CONFIG_MODE.NO_SEC)], []],
      clientPublicKeyOrId: [[''], []],
      clientSecretKey: [[''], []],
      serverPublicKey: [[''], []],
      clientHoldOffTime: [[''], [Validators.required]],
      serverId: [[''], [Validators.required]],
      bootstrapServerAccountTimeout: [[''], [Validators.required]],
    })
  }

  save(): void {
    console.log(this.lwm2mConfigFormGroup.get(this.bootstrapServer).value)
    this.data.endPoint = this.lwm2mConfigFormGroup.get('endPoint').value.split('\"').join('');
    this.dialogRef.close(this.data);
  }

  cancel(): void {
    this.dialogRef.close(undefined);
  }

}


