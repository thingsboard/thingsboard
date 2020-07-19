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
  DEFAULT_LIFE_TIME,
  DEFAULT_DEFAULT_MIN_PERIOD,
  DEFAULT_BINDING,
  ClientSecurityConfigX509,
  ClientSecurityConfigNO_SEC,
  getDefaultClientSecurityConfigType,
  DEFAULT_PORT_BOOTSTRAP_NO_SEC,
  DEFAULT_PORT_SERVER_NO_SEC,
  DEFAULT_CLIENT_HOLD_OFF_TIME,
  LEN_MAX_PSK,
  LEN_MAX_PUBLIC_KEY,
  bootstarpServerPublicKey,
  lwm2mServerPublicKey
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
  bootstrapServers: string;
  bootstrapServer: string;
  lwm2mServer: string;
  formControlNameJsonObserve: string;
  observeData: ObjectLwM2M[];
  jsonObserveData: {};
  observe: string;
  bootstrapFormGroup: FormGroup;
  lwm2mServerFormGroup: FormGroup;
  observeFormGroup: FormGroup;
  lenMinMaxClient = 64;

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
    this.endPoint = this.data.endPoint;
    this.jsonAllConfig = JSON.parse(JSON.stringify(this.data.jsonAllConfig)) as SecurityConfigModels;
    this.initConstants();
    this.lwm2mConfigFormGroup = this.initLwm2mConfigFormGroup();
    console.log(this.data.isNew);
    this.title = this.translate.instant('device.lwm2m-security-info') + ": " + this.endPoint
    this.lwm2mConfigFormGroup.get('clientCertificate').disable();
    this.initChildesFormGroup();
    this.initClientSecurityConfig(this.lwm2mConfigFormGroup.get('jsonAllConfig').value);
    this.registerDisableOnLoadFormControl(this.lwm2mConfigFormGroup.get('securityConfigClientMode'));
    this.upDateValueFromForm();
  }

  initConstants(): void {
    this.bootstrapServers = BOOTSTRAP_SERVERS;
    this.bootstrapServer = BOOTSTRAP_SERVER;
    this.lwm2mServer = LWM2M_SERVER;
    this.observe = OBSERVE;
    this.formControlNameJsonObserve = JSON_OBSERVE;
  }

  initChildesFormGroup(): void {
    this.bootstrapFormGroup = this.lwm2mConfigFormGroup.get('bootstrapFormGroup') as FormGroup;
    this.lwm2mServerFormGroup = this.lwm2mConfigFormGroup.get('lwm2mServerFormGroup') as FormGroup;
    this.observeFormGroup = this.lwm2mConfigFormGroup.get('observeFormGroup') as FormGroup;
  }

  initClientSecurityConfig(jsonAllConfig: SecurityConfigModels): void {
    switch (jsonAllConfig.client.securityConfigClientMode.toString()) {
      case SECURITY_CONFIG_MODE.NO_SEC.toString():
        break;
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
    }
    this.securityConfigClientUpdateValidators(this.lwm2mConfigFormGroup.get('securityConfigClientMode').value);
  }

  securityConfigClientModeChanged(mode: SECURITY_CONFIG_MODE): void {
    switch (mode) {
      case SECURITY_CONFIG_MODE.NO_SEC:
        let clientSecurityConfigNO_SEC = getDefaultClientSecurityConfigType(mode, this.lwm2mConfigFormGroup.get('endPoint').value) as ClientSecurityConfigNO_SEC;
        this.jsonAllConfig.client = clientSecurityConfigNO_SEC;
        this.lwm2mConfigFormGroup.patchValue({
          jsonAllConfig: this.jsonAllConfig,
          clientCertificate: false
        }, {emitEvent: true});
        break;
      case SECURITY_CONFIG_MODE.PSK:
        let clientSecurityConfigPSK = getDefaultClientSecurityConfigType(mode, this.lwm2mConfigFormGroup.get('endPoint').value) as ClientSecurityConfigPSK;
        clientSecurityConfigPSK.identity = this.lwm2mConfigFormGroup.get('identityPSK').value;
        clientSecurityConfigPSK.key = this.lwm2mConfigFormGroup.get('clientKey').value;
        this.jsonAllConfig.client = clientSecurityConfigPSK;
        this.lwm2mConfigFormGroup.patchValue({
          jsonAllConfig: this.jsonAllConfig,
          clientCertificate: false
        }, {emitEvent: true});
        if (this.data.isNew) this.updateBootstrapPublicKey(mode);
        break;
      case SECURITY_CONFIG_MODE.RPK:
        let clientSecurityConfigRPK = getDefaultClientSecurityConfigType(mode, this.lwm2mConfigFormGroup.get('endPoint').value) as ClientSecurityConfigRPK;
        clientSecurityConfigRPK.key = this.lwm2mConfigFormGroup.get('clientKey').value;
        this.jsonAllConfig.client = clientSecurityConfigRPK;
        this.lwm2mConfigFormGroup.patchValue({
          jsonAllConfig: this.jsonAllConfig,
          clientCertificate: false
        }, {emitEvent: true})
        if (this.data.isNew) this.updateBootstrapPublicKey(mode);
        break;
      case SECURITY_CONFIG_MODE.X509:
        let clientSecurityConfigX509 = getDefaultClientSecurityConfigType(mode, this.lwm2mConfigFormGroup.get('endPoint').value) as ClientSecurityConfigX509;
        this.jsonAllConfig.client = clientSecurityConfigX509;
        this.lwm2mConfigFormGroup.patchValue({
          jsonAllConfig: this.jsonAllConfig,
          clientCertificate: true
        }, {emitEvent: true})
        break;

    }
    this.securityConfigClientUpdateValidators(mode);
  }

  securityConfigClientUpdateValidators(mode: SECURITY_CONFIG_MODE): void {
    switch (mode) {
      case SECURITY_CONFIG_MODE.NO_SEC:
        this.lwm2mConfigFormGroup.get('identityPSK').setValidators([]);
        this.lwm2mConfigFormGroup.get('identityPSK').updateValueAndValidity();
        this.lwm2mConfigFormGroup.get('clientKey').setValidators([]);
        this.lwm2mConfigFormGroup.get('clientKey').updateValueAndValidity();
      case SECURITY_CONFIG_MODE.PSK:
        this.lenMinMaxClient = LEN_MAX_PSK;
        this.lwm2mConfigFormGroup.get('identityPSK').setValidators([]);
        this.lwm2mConfigFormGroup.get('identityPSK').updateValueAndValidity();
        this.lwm2mConfigFormGroup.get('clientKey').setValidators([Validators.required, Validators.pattern(KEY_IDENT_REGEXP_PSK)]);
        this.lwm2mConfigFormGroup.get('clientKey').updateValueAndValidity();
        break;
      case SECURITY_CONFIG_MODE.RPK:
        this.lenMinMaxClient = LEN_MAX_PUBLIC_KEY;
        this.lwm2mConfigFormGroup.get('identityPSK').setValidators([]);
        this.lwm2mConfigFormGroup.get('identityPSK').updateValueAndValidity();
        this.lwm2mConfigFormGroup.get('clientKey').setValidators([Validators.required, Validators.pattern(KEY_PUBLIC_REGEXP)]);
        this.lwm2mConfigFormGroup.get('clientKey').updateValueAndValidity();
        break;
      case SECURITY_CONFIG_MODE.X509:
        this.lenMinMaxClient = LEN_MAX_PUBLIC_KEY;
        this.lwm2mConfigFormGroup.get('identityPSK').setValidators([]);
        this.lwm2mConfigFormGroup.get('identityPSK').updateValueAndValidity();
        this.lwm2mConfigFormGroup.get('clientKey').setValidators([]);
        this.lwm2mConfigFormGroup.get('clientKey').updateValueAndValidity();
        break;
    }
  }

  updateBootstrapPublicKey(mode: SECURITY_CONFIG_MODE): void {
    this.jsonAllConfig.bootstrap.bootstrapServer.securityMode = mode.toString();
    this.jsonAllConfig.bootstrap.bootstrapServer.serverPublicKey = bootstarpServerPublicKey;
    this.lwm2mConfigFormGroup.get('bootstrapServer').patchValue(
      this.jsonAllConfig.bootstrap.bootstrapServer, {emitEvent: false});
    this.jsonAllConfig.bootstrap.lwm2mServer.securityMode = mode.toString();
    this.jsonAllConfig.bootstrap.lwm2mServer.serverPublicKey = lwm2mServerPublicKey;
    this.lwm2mConfigFormGroup.get('lwm2mServer').patchValue(
      this.jsonAllConfig.bootstrap.lwm2mServer, {emitEvent: false});
    this.upDateJsonAllConfig();
    this.data.isNew = false;
  }

  upDateValueFromForm(): void {
    this.lwm2mConfigFormGroup.get('endPoint').valueChanges.subscribe(val => {
      if (!this.lwm2mConfigFormGroup.get('endPoint').pristine && this.lwm2mConfigFormGroup.get(JSON_ALL_CONFIG).valid) {
        this.data.endPoint = this.lwm2mConfigFormGroup.get('endPoint').value;
        // Client mode == PSK
        if (this.lwm2mConfigFormGroup.get('securityConfigClientMode').value === SECURITY_CONFIG_MODE.PSK) {
          this.lwm2mConfigFormGroup.patchValue({
            identityPSK: this.data.endPoint
          }, {emitEvent: false});
          this.jsonAllConfig.client["endpoint"] = this.data.endPoint;
        }
        if (this.lwm2mConfigFormGroup.get('bootstrapServer').get('securityMode').value === SECURITY_CONFIG_MODE.PSK) {
          this.lwm2mConfigFormGroup.get('bootstrapServer').patchValue({
            clientPublicKeyOrId: this.data.endPoint
          }, {emitEvent: false});
          this.jsonAllConfig.bootstrap.bootstrapServer.clientPublicKeyOrId = this.data.endPoint
        }
        if (this.lwm2mConfigFormGroup.get('lwm2mServer').get('securityMode').value === SECURITY_CONFIG_MODE.PSK) {
          this.lwm2mConfigFormGroup.get('lwm2mServer').patchValue({
            clientPublicKeyOrId: this.data.endPoint
          }, {emitEvent: false});
          this.jsonAllConfig.bootstrap.lwm2mServer.clientPublicKeyOrId = this.data.endPoint
        }
        this.upDateJsonAllConfig();
      }
    })

    this.lwm2mConfigFormGroup.get('identityPSK').valueChanges.subscribe(val => {
      if (!this.lwm2mConfigFormGroup.get('identityPSK').pristine && this.lwm2mConfigFormGroup.get(JSON_ALL_CONFIG).valid) {

      }
    })

    this.lwm2mConfigFormGroup.get('clientKey').valueChanges.subscribe(val => {
      if (!this.lwm2mConfigFormGroup.get('clientKey').pristine && this.lwm2mConfigFormGroup.get(JSON_ALL_CONFIG).valid) {

      }
    })

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

  // updateClient(mode: SECURITY_CONFIG_MODE): void {
  //   /** if first time isDirty == true
  //    * updateDefault bootstrap
  //    */
  //   if (this.isDirty) {
  //     this.jsonAllConfig.client.securityConfigClientMode = mode.toString();
  //     this.jsonAllConfig.bootstrap.bootstrapServer.securityMode = mode.toString();
  //     this.jsonAllConfig.bootstrap.lwm2mServer.securityMode = mode.toString();
  //     this.lwm2mConfigFormGroup.patchValue({
  //       jsonAllConfig: JSON.parse(JSON.stringify(this.jsonAllConfig)),
  //       bootstrapServer: this.jsonAllConfig.bootstrap[this.bootstrapServer],
  //       lwm2mServer: this.jsonAllConfig.bootstrap[this.lwm2mServer]
  //     }, {emitEvent: true});
  //     this.isDirty = false;
  //   }
  // }

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
      bootstrapFormGroup: this.getServerGroup(true),
      lwm2mServerFormGroup: this.getServerGroup(false),
      observeFormGroup: this.fb.group({}),
      endPoint: [this.endPoint, []],
      securityConfigClientMode: [SECURITY_CONFIG_MODE[this.jsonAllConfig.client.securityConfigClientMode.toString()], []],
      identityPSK: ['', []],
      clientKey: ['', []],
      clientCertificate: [false, []],
      shortId: [this.jsonAllConfig.bootstrap.servers.shortId, Validators.required],
      lifetime: [this.jsonAllConfig.bootstrap.servers.lifetime, Validators.required],
      defaultMinPeriod: [this.jsonAllConfig.bootstrap.servers.defaultMinPeriod, Validators.required],
      notifIfDisabled: [this.jsonAllConfig.bootstrap.servers.notifIfDisabled, []],
      binding: [this.jsonAllConfig.bootstrap.servers.binding, Validators.required]
    });
  }

  getServerGroup(isBootstrapServer: boolean): FormGroup {
    const port = (isBootstrapServer) ? DEFAULT_PORT_BOOTSTRAP_NO_SEC : DEFAULT_PORT_SERVER_NO_SEC;
    const serverPublicKey = (isBootstrapServer) ? bootstarpServerPublicKey : lwm2mServerPublicKey;
    return this.fb.group({
      host: [this.window.location.hostname, [Validators.required]],
      port: [port, [Validators.required]],
      isBootstrapServer: [isBootstrapServer, []],
      securityMode: [this.fb.control(SECURITY_CONFIG_MODE.NO_SEC), []],
      clientPublicKeyOrId: ['', []],
      clientSecretKey: ['', []],
      serverPublicKey: [serverPublicKey, Validators.required],
      clientHoldOffTime: [DEFAULT_CLIENT_HOLD_OFF_TIME, [Validators.required]],
      serverId: [DEFAULT_ID_SERVER, [Validators.required]],
      bootstrapServerAccountTimeout: ['', [Validators.required]],
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


