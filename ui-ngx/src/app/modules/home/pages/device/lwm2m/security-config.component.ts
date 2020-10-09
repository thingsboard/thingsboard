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
  OBSERVE_ATTR,
  ObjectLwM2M,
  JSON_ALL_CONFIG,
  KEY_IDENT_REGEXP_PSK,
  KEY_PUBLIC_REGEXP_PSK,
  DEFAULT_END_POINT,
  DeviceCredentialsDialogLwm2mData,
  BOOTSTRAP_SERVER,
  BOOTSTRAP_SERVERS,
  LWM2M_SERVER,
  DEFAULT_ID_SERVER,
  ClientSecurityConfigX509,
  ClientSecurityConfigNO_SEC,
  getDefaultClientSecurityConfigType,
  DEFAULT_CLIENT_HOLD_OFF_TIME,
  LEN_MAX_PSK,
  LEN_MAX_PUBLIC_KEY_RPK,
  BOOTSTRAP_PUBLIC_KEY_RPK,
  LWM2M_SERVER_PUBLIC_KEY_RPK,
  DEFAULT_PORT_BOOTSTRAP_SEC,
  DEFAULT_PORT_SERVER_SEC,
  DEFAULT_PORT_BOOTSTRAP_NO_SEC,
  DEFAULT_PORT_SERVER_NO_SEC,
  BOOTSTRAP_PUBLIC_KEY_X509,
  LWM2M_SERVER_PUBLIC_KEY_X509,
  getDefaultClientObserveAttr,
  DEFAULT_PORT_BOOTSTRAP_SEC_CERT, DEFAULT_PORT_SERVER_SEC_CERT, ATTR, OBSERVE, TELEMETRY,
} from "./security-config.models";
import {WINDOW} from "@core/services/window.service";
import {MatTabChangeEvent, MatTabGroup} from "@angular/material/tabs";
import {MatTab} from "@angular/material/tabs/tab";


@Component({
  selector: 'tb-security-config-lwm2m',
  templateUrl: './security-config.component.html',
  styleUrls: []
})

export class SecurityConfigComponent extends DialogComponent<SecurityConfigComponent, object> implements OnInit {

  lwm2mConfigFormGroup: FormGroup;
  title: string;
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
  observeAttr: string;
  observe: string;
  attr: string;
  telemetry: string;
  lenMaxKeyClient = LEN_MAX_PSK;
  bsPublikKeyRPK: string
  lwM2mPublikKeyRPK: string
  bsPublikKeyX509: string
  lwM2mPublikKeyX509: string
  tabPrevious: MatTab
  tabIndexPrevious = 0 as number;

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
    this.getFromYml();
     this.jsonAllConfig = JSON.parse(JSON.stringify(this.data.jsonAllConfig)) as SecurityConfigModels;
    this.initConstants();
    this.lwm2mConfigFormGroup = this.initLwm2mConfigFormGroup();
    this.title = this.translate.instant('device.lwm2m-security-info') + ": " + this.data.endPoint
    this.lwm2mConfigFormGroup.get('clientCertificate').disable();
    this.initClientSecurityConfig(this.lwm2mConfigFormGroup.get('jsonAllConfig').value);
    this.registerDisableOnLoadFormControl(this.lwm2mConfigFormGroup.get('securityConfigClientMode'));
  }

  initConstants(): void {
    this.bootstrapServers = BOOTSTRAP_SERVERS;
    this.bootstrapServer = BOOTSTRAP_SERVER;
    this.lwm2mServer = LWM2M_SERVER;
    this.observeAttr = OBSERVE_ATTR;
    this.observe = OBSERVE;
    this.attr = ATTR;
    this.telemetry = TELEMETRY;
    this.formControlNameJsonObserve = JSON_OBSERVE;
  }

  /**
   * initChildesFormGroup
   */
  get bootstrapFormGroup(): FormGroup {
    return this.lwm2mConfigFormGroup.get('bootstrapFormGroup') as FormGroup;
  }

  get lwm2mServerFormGroup(): FormGroup {
    return this.lwm2mConfigFormGroup.get('lwm2mServerFormGroup') as FormGroup;
  }

  get observeAttrFormGroup(): FormGroup {
    return this.lwm2mConfigFormGroup.get('observeFormGroup') as FormGroup;
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
        }, {emitEvent: false});
        break;
      case SECURITY_CONFIG_MODE.RPK.toString():
        const clientSecurityConfigRPK = jsonAllConfig.client as ClientSecurityConfigRPK;
        this.lwm2mConfigFormGroup.patchValue({
          clientKey: clientSecurityConfigRPK.key,
        }, {emitEvent: false});
        break;
      case SECURITY_CONFIG_MODE.X509.toString():
        const clientSecurityConfigX509 = jsonAllConfig.client as ClientSecurityConfigX509;
        this.lwm2mConfigFormGroup.patchValue({
          clientCertificate: clientSecurityConfigX509.x509
        }, {emitEvent: false});
        break;
    }
    this.securityConfigClientUpdateValidators(this.lwm2mConfigFormGroup.get('securityConfigClientMode').value);
  }

  securityConfigClientModeChanged(mode: SECURITY_CONFIG_MODE): void {
    switch (mode) {
      case SECURITY_CONFIG_MODE.NO_SEC:
        let clientSecurityConfigNO_SEC = getDefaultClientSecurityConfigType(mode) as ClientSecurityConfigNO_SEC;
        this.jsonAllConfig.client = clientSecurityConfigNO_SEC;
        this.lwm2mConfigFormGroup.patchValue({
          jsonAllConfig: this.jsonAllConfig,
          clientCertificate: false
        }, {emitEvent: true});
        break;
      case SECURITY_CONFIG_MODE.PSK:
        let clientSecurityConfigPSK = getDefaultClientSecurityConfigType(mode, this.lwm2mConfigFormGroup.get('endPoint').value) as ClientSecurityConfigPSK;
        clientSecurityConfigPSK.identity = this.data.endPoint;
        clientSecurityConfigPSK.key = this.lwm2mConfigFormGroup.get('clientKey').value;
        this.jsonAllConfig.client = clientSecurityConfigPSK;
        this.lwm2mConfigFormGroup.patchValue({
          identityPSK: clientSecurityConfigPSK.identity,
          clientCertificate: false
        }, {emitEvent: true});
        break;
      case SECURITY_CONFIG_MODE.RPK:
        let clientSecurityConfigRPK = getDefaultClientSecurityConfigType(mode) as ClientSecurityConfigRPK;
        clientSecurityConfigRPK.key = this.lwm2mConfigFormGroup.get('clientKey').value;
        this.jsonAllConfig.client = clientSecurityConfigRPK;
        this.lwm2mConfigFormGroup.patchValue({
          jsonAllConfig: this.jsonAllConfig,
          clientCertificate: false
        }, {emitEvent: true})
        break;
      case SECURITY_CONFIG_MODE.X509:
        let clientSecurityConfigX509 = getDefaultClientSecurityConfigType(mode) as ClientSecurityConfigX509;
        this.jsonAllConfig.client = clientSecurityConfigX509;
        this.lwm2mConfigFormGroup.patchValue({
          jsonAllConfig: this.jsonAllConfig,
          clientCertificate: true
        }, {emitEvent: true})
        break;
    }
    this.updateServerPublicKey(mode);
    this.securityConfigClientUpdateValidators(mode);
  }

  securityConfigClientUpdateValidators(mode: SECURITY_CONFIG_MODE): void {
    switch (mode) {
      case SECURITY_CONFIG_MODE.NO_SEC:
        this.lwm2mConfigFormGroup.get('identityPSK').setValidators([]);
        this.lwm2mConfigFormGroup.get('identityPSK').updateValueAndValidity();
        this.lwm2mConfigFormGroup.get('clientKey').setValidators([]);
        this.lwm2mConfigFormGroup.get('clientKey').updateValueAndValidity();
        break;
      case SECURITY_CONFIG_MODE.PSK:
        this.lenMaxKeyClient = LEN_MAX_PSK;
        this.lwm2mConfigFormGroup.get('identityPSK').setValidators([]);
        this.lwm2mConfigFormGroup.get('identityPSK').updateValueAndValidity();
        this.lwm2mConfigFormGroup.get('clientKey').setValidators([Validators.required, Validators.pattern(KEY_IDENT_REGEXP_PSK)]);
        this.lwm2mConfigFormGroup.get('clientKey').updateValueAndValidity();
        break;
      case SECURITY_CONFIG_MODE.RPK:
        this.lenMaxKeyClient = LEN_MAX_PUBLIC_KEY_RPK;
        this.lwm2mConfigFormGroup.get('identityPSK').setValidators([]);
        this.lwm2mConfigFormGroup.get('identityPSK').updateValueAndValidity();
        this.lwm2mConfigFormGroup.get('clientKey').setValidators([Validators.required, Validators.pattern(KEY_PUBLIC_REGEXP_PSK)]);
        this.lwm2mConfigFormGroup.get('clientKey').updateValueAndValidity();
        break;
      case SECURITY_CONFIG_MODE.X509:
        this.lenMaxKeyClient = LEN_MAX_PUBLIC_KEY_RPK;
        this.lwm2mConfigFormGroup.get('identityPSK').setValidators([]);
        this.lwm2mConfigFormGroup.get('identityPSK').updateValueAndValidity();
        this.lwm2mConfigFormGroup.get('clientKey').setValidators([]);
        this.lwm2mConfigFormGroup.get('clientKey').updateValueAndValidity();
        break;
    }
  }

  updateServerPublicKey(mode: SECURITY_CONFIG_MODE): void {
    this.jsonAllConfig.bootstrap.bootstrapServer.securityMode = mode.toString();
    this.jsonAllConfig.bootstrap.lwm2mServer.securityMode = mode.toString();
    this.jsonAllConfig.bootstrap.bootstrapServer.port = DEFAULT_PORT_BOOTSTRAP_SEC;
    this.jsonAllConfig.bootstrap.lwm2mServer.port = DEFAULT_PORT_SERVER_SEC;
    switch (mode) {
      case SECURITY_CONFIG_MODE.NO_SEC:
        this.jsonAllConfig.bootstrap.bootstrapServer.port = DEFAULT_PORT_BOOTSTRAP_NO_SEC;
        this.jsonAllConfig.bootstrap.lwm2mServer.port = DEFAULT_PORT_SERVER_NO_SEC;
        this.jsonAllConfig.bootstrap.bootstrapServer.serverPublicKey = '';
        this.jsonAllConfig.bootstrap.lwm2mServer.serverPublicKey = '';
        break;
      case SECURITY_CONFIG_MODE.PSK:
        this.jsonAllConfig.bootstrap.bootstrapServer.serverPublicKey = '';
        this.jsonAllConfig.bootstrap.lwm2mServer.serverPublicKey = '';
        break;
      case SECURITY_CONFIG_MODE.RPK:
        this.jsonAllConfig.bootstrap.bootstrapServer.serverPublicKey = this.bsPublikKeyRPK;
        this.jsonAllConfig.bootstrap.lwm2mServer.serverPublicKey = this.lwM2mPublikKeyRPK;
        break;
      case SECURITY_CONFIG_MODE.X509:
        this.jsonAllConfig.bootstrap.bootstrapServer.port = DEFAULT_PORT_BOOTSTRAP_SEC_CERT;
        this.jsonAllConfig.bootstrap.lwm2mServer.port = DEFAULT_PORT_SERVER_SEC_CERT;
        this.jsonAllConfig.bootstrap.bootstrapServer.serverPublicKey = this.bsPublikKeyX509;
        this.jsonAllConfig.bootstrap.lwm2mServer.serverPublicKey = this.lwM2mPublikKeyX509;
        break;
    }
    this.lwm2mConfigFormGroup.get('bootstrapServer').patchValue(
      this.jsonAllConfig.bootstrap.bootstrapServer, {emitEvent: false});
    this.lwm2mConfigFormGroup.get('lwm2mServer').patchValue(
      this.jsonAllConfig.bootstrap.lwm2mServer, {emitEvent: false});
    this.upDateJsonAllConfig();
  }

  tabChanged = (tabChangeEvent: MatTabChangeEvent): void => {
    if (this.tabIndexPrevious !== tabChangeEvent.index) this.upDateValueToJson();
    this.tabIndexPrevious = tabChangeEvent.index;
  }

  upDateValueToJson(): void {
    switch (this.tabIndexPrevious) {
      case 0:
        this.upDateValueToJsonTab_0();
        break;
      case 1:
        this.upDateValueToJsonTab_1();
        break;
      case 2:
        this.upDateValueToJsonTab_2();
        break;
      case 3:
        this.upDateValueToJsonTab_3();
        break;
    }
  }

  upDateValueToJsonTab_0(): void {
    if (this.lwm2mConfigFormGroup !== null) {
      if (!this.lwm2mConfigFormGroup.get('endPoint').pristine && this.lwm2mConfigFormGroup.get('endPoint').valid) {
        this.data.endPoint = this.lwm2mConfigFormGroup.get('endPoint').value;
        // Client mode == PSK
        if (this.lwm2mConfigFormGroup.get('securityConfigClientMode').value === SECURITY_CONFIG_MODE.PSK) {
          this.jsonAllConfig.client["endpoint"] = this.data.endPoint;
          this.jsonAllConfig.client["endpoint"].markAsPristine({
            onlySelf: true
          });
          this.upDateJsonAllConfig();
        }
      }
      /** only  Client mode == PSK */
      if (!this.lwm2mConfigFormGroup.get('identityPSK').pristine && this.lwm2mConfigFormGroup.get('identityPSK').valid) {
        this.lwm2mConfigFormGroup.get('identityPSK').markAsPristine({
          onlySelf: true
        });
        this.updateIdentityPSK();
      }
      /** only  Client mode == PSK (len = 64) || RPK (len = 182) */
      if (!this.lwm2mConfigFormGroup.get('clientKey').pristine && this.lwm2mConfigFormGroup.get('clientKey').valid) {
        this.lwm2mConfigFormGroup.get('clientKey').markAsPristine({
          onlySelf: true
        });
        this.updateClientKey();
      }
    }
  }

  upDateValueToJsonTab_1(): void {
    if (this.lwm2mConfigFormGroup !== null) {
      if (!this.lwm2mConfigFormGroup.get('shortId').pristine && this.lwm2mConfigFormGroup.get('shortId').valid) {
        this.jsonAllConfig.bootstrap.servers.shortId = this.lwm2mConfigFormGroup.get('shortId').value;
        this.lwm2mConfigFormGroup.get('shortId').markAsPristine({
          onlySelf: true
        });
        this.upDateJsonAllConfig();
      }

      if (!this.lwm2mConfigFormGroup.get('lifetime').pristine && this.lwm2mConfigFormGroup.get('lifetime').valid) {
        this.jsonAllConfig.bootstrap.servers.lifetime = this.lwm2mConfigFormGroup.get('lifetime').value;
        this.lwm2mConfigFormGroup.get('lifetime').markAsPristine({
          onlySelf: true
        });
        this.upDateJsonAllConfig();
      }

      if (!this.lwm2mConfigFormGroup.get('defaultMinPeriod').pristine && this.lwm2mConfigFormGroup.get('defaultMinPeriod').valid) {
        this.jsonAllConfig.bootstrap.servers.defaultMinPeriod = this.lwm2mConfigFormGroup.get('defaultMinPeriod').value;
        this.lwm2mConfigFormGroup.get('defaultMinPeriod').markAsPristine({
          onlySelf: true
        });
        this.upDateJsonAllConfig();
      }

      if (!this.lwm2mConfigFormGroup.get('notifIfDisabled').pristine && this.lwm2mConfigFormGroup.get('notifIfDisabled').valid) {
        this.jsonAllConfig.bootstrap.servers.notifIfDisabled = this.lwm2mConfigFormGroup.get('notifIfDisabled').value;
        this.lwm2mConfigFormGroup.get('notifIfDisabled').markAsPristine({
          onlySelf: true
        });
        this.upDateJsonAllConfig();
      }

      if (!this.lwm2mConfigFormGroup.get('binding').pristine && this.lwm2mConfigFormGroup.get('binding').valid) {
        this.jsonAllConfig.bootstrap.servers.binding = this.lwm2mConfigFormGroup.get('binding').value;
        this.lwm2mConfigFormGroup.get('binding').markAsPristine({
          onlySelf: true
        });
        this.upDateJsonAllConfig();
      }

      if (this.bootstrapFormGroup !== null && !this.bootstrapFormGroup.pristine && this.bootstrapFormGroup.valid) {
        this.jsonAllConfig.bootstrap.bootstrapServer = this.bootstrapFormGroup.value;
        this.bootstrapFormGroup.markAsPristine({
          onlySelf: true
        });
        this.upDateJsonAllConfig();
      }

      if (this.lwm2mServerFormGroup !== null && !this.lwm2mServerFormGroup.pristine && this.lwm2mServerFormGroup.valid) {
        this.jsonAllConfig.bootstrap.lwm2mServer = this.lwm2mServerFormGroup.value;
        // this.jsonAllConfig.bootstrap.lwm2mServer.bootstrapServerIs = false;
        this.lwm2mServerFormGroup.markAsPristine({
          onlySelf: true
        });
        this.upDateJsonAllConfig();
      }
    }
  }

  upDateValueToJsonTab_2(): void {
    if (this.lwm2mConfigFormGroup !== null) {
      if (!this.observeAttrFormGroup.pristine && this.observeAttrFormGroup.valid) {
        this.upDateObserveAttrFromGroup(this.observeAttrFormGroup.value);
        this.observeAttrFormGroup.markAsPristine({
          onlySelf: true
        });
        this.upDateJsonAllConfig();
      }
    }
  }

  upDateValueToJsonTab_3(): void {
    if (!this.lwm2mConfigFormGroup.get(this.formControlNameJsonAllConfig).pristine && this.lwm2mConfigFormGroup.get(this.formControlNameJsonAllConfig).valid) {
      this.jsonAllConfig = this.lwm2mConfigFormGroup.get(this.formControlNameJsonAllConfig).value;
      this.lwm2mConfigFormGroup.get(this.formControlNameJsonAllConfig).markAsPristine({
        onlySelf: true
      });
    }
  }

  updateIdentityPSK(): void {
    if (this.lwm2mConfigFormGroup.get('bootstrapServer').value['securityMode'] === SECURITY_CONFIG_MODE.PSK.toString()) {
      this.lwm2mConfigFormGroup.get('bootstrapFormGroup').patchValue({
        clientPublicKeyOrId: this.lwm2mConfigFormGroup.get('identityPSK').value
      });
      this.jsonAllConfig.client['identity'] = this.lwm2mConfigFormGroup.get('identityPSK').value;
      this.upDateJsonAllConfig();
    }
    if (this.lwm2mConfigFormGroup.get('lwm2mServer').value['securityMode'] === SECURITY_CONFIG_MODE.PSK.toString()) {
      this.lwm2mConfigFormGroup.get('lwm2mServerFormGroup').patchValue({
        clientPublicKeyOrId: this.lwm2mConfigFormGroup.get('identityPSK').value
      });
      this.jsonAllConfig.bootstrap.lwm2mServer.clientPublicKeyOrId = this.lwm2mConfigFormGroup.get('identityPSK').value;
      this.upDateJsonAllConfig();
    }
  }

  updateClientKey(): void {
    this.jsonAllConfig.client["key"] = this.lwm2mConfigFormGroup.get('clientKey').value;
    if (this.lwm2mConfigFormGroup.get('bootstrapServer').value['securityMode'] === SECURITY_CONFIG_MODE.PSK.toString()) {
      this.lwm2mConfigFormGroup.get('bootstrapServer').patchValue({
        clientSecretKey: this.jsonAllConfig.client["key"]
      }, {emitEvent: false});
      this.jsonAllConfig.bootstrap.bootstrapServer.clientSecretKey = this.jsonAllConfig.client["key"];
    }
    if (this.lwm2mConfigFormGroup.get('lwm2mServer').value['securityMode'] === SECURITY_CONFIG_MODE.PSK.toString()) {
      this.lwm2mConfigFormGroup.get('lwm2mServer').patchValue({
        clientSecretKey: this.jsonAllConfig.client["key"]
      }, {emitEvent: false});
      this.jsonAllConfig.bootstrap.lwm2mServer.clientSecretKey = this.jsonAllConfig.client["key"];
    }
    this.upDateJsonAllConfig();
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

  upDateObserveAttrFromGroup(val: any): void {
    let isObserve = [] as Array<string>;
    let isAttr = [] as Array<string>;
    let isTelemetry = [] as Array<string>;
    let observeJson = JSON.parse(JSON.stringify(val['clientLwM2M'])) as [];
    let pathObj;
    let pathInst;
    let pathRes
    observeJson.forEach(obj => {
      Object.entries(obj).forEach(([key, value]) => {
        if (key === 'id') {
          pathObj = value;
        }
        if (key === 'instance') {
          let instancesJson = JSON.parse(JSON.stringify(value)) as [];
          if (instancesJson.length > 0) {
            instancesJson.forEach(instance => {
              Object.entries(instance).forEach(([key, value]) => {
                if (key === 'id') {
                  pathInst = value;
                }
                let pathInstObserve;
                if (key === 'isObserve' && value) {
                  pathInstObserve = '/' + pathObj + '/' + pathInst;
                  isObserve.push(pathInstObserve)
                }
                if (key === 'resource') {
                  let resourcesJson = JSON.parse(JSON.stringify(value)) as [];
                  if (resourcesJson.length > 0) {
                    resourcesJson.forEach(res => {
                      Object.entries(res).forEach(([key, value]) => {
                        if (key === 'id') {
                          // pathRes = value
                          pathRes = '/' + pathObj + '/' + pathInst + '/' + value;
                        } else if (key === 'isObserve' && value) {
                          isObserve.push(pathRes)
                        } else if (key === 'isAttr' && value) {
                          isAttr.push(pathRes)
                        } else if (key === 'isTelemetry' && value) {
                          isTelemetry.push(pathRes)
                        }
                      });
                    });
                  }
                }
              });
            });
          }
        }
      });
    });
    if (this.jsonAllConfig[this.observeAttr] === undefined) {
      this.jsonAllConfig[this.observeAttr] = {
        [this.observe]: isObserve,
        [this.attr]: isAttr,
        [this.telemetry]: isTelemetry
      };
    }
    else {
      this.jsonAllConfig[this.observeAttr][this.observe] = isObserve;
      this.jsonAllConfig[this.observeAttr][this.attr] = isAttr;
      this.jsonAllConfig[this.observeAttr][this.telemetry] = isTelemetry;
    }
  }

  getObservAttrObjectFormGroup(): ObjectLwM2M [] {
    let clientObserveAttr = getDefaultClientObserveAttr() as ObjectLwM2M[];
    if (this.jsonAllConfig[this.observeAttr]) {
      let isObserve = this.jsonAllConfig[this.observeAttr][this.observe] as Array<string>;
      let isAttr = this.jsonAllConfig[this.observeAttr][this.attr] as Array<string>;
      let isTelemetry = this.jsonAllConfig[this.observeAttr][this.telemetry] as Array<string>;
      if (isObserve) clientObserveAttr = this.getObservAttrFormGroup(isObserve, clientObserveAttr, "isObserve");
      if (isAttr) clientObserveAttr = this.getObservAttrFormGroup(isAttr, clientObserveAttr, "isAttr");
      if (isTelemetry) clientObserveAttr = this.getObservAttrFormGroup(isTelemetry, clientObserveAttr, "isTelemetry");
    }
    return clientObserveAttr;
  }

  getObservAttrFormGroup(isParameter: Array<string>, clientObserveAttr: ObjectLwM2M[], nameParameter: string): ObjectLwM2M [] {
    isParameter.forEach(attr => {
      let pathParameter = Array.from(attr.substring(1).split('/'), Number);
      clientObserveAttr.forEach(obj => {
        if (obj.id === pathParameter[0]) {
          obj.instance.forEach(inst => {
            if (inst.id === pathParameter[1]) {
              inst.resource.forEach(res => {
                if (res.id === pathParameter[2]) res[nameParameter] = true;
              })
            }
          })
        }
      });
    });
    return clientObserveAttr;
  }


  initLwm2mConfigFormGroup(): FormGroup {
    if (SECURITY_CONFIG_MODE[this.jsonAllConfig.client.securityConfigClientMode.toString()] === SECURITY_CONFIG_MODE.PSK) {
      this.data.endPoint = this.jsonAllConfig.client['endpoint'];
    }
    return this.fb.group({
      securityConfigClientMode: [SECURITY_CONFIG_MODE[this.jsonAllConfig.client.securityConfigClientMode.toString()], []],
      identityPSK: ['', []],
      clientKey: ['', []],
      clientCertificate: [false, []],
      shortId: [this.jsonAllConfig.bootstrap.servers.shortId, Validators.required],
      lifetime: [this.jsonAllConfig.bootstrap.servers.lifetime, Validators.required],
      defaultMinPeriod: [this.jsonAllConfig.bootstrap.servers.defaultMinPeriod, Validators.required],
      notifIfDisabled: [this.jsonAllConfig.bootstrap.servers.notifIfDisabled, []],
      binding: [this.jsonAllConfig.bootstrap.servers.binding, Validators.required],
      bootstrapServer: [this.jsonAllConfig.bootstrap[this.bootstrapServer], []],
      lwm2mServer: [this.jsonAllConfig.bootstrap[this.lwm2mServer], []],
      observeAttr: [this.getObservAttrObjectFormGroup(), []],
      bootstrapFormGroup: this.getServerGroup(true),
      lwm2mServerFormGroup: this.getServerGroup(false),
      observeFormGroup: this.fb.group({}),
      endPoint: [this.data.endPoint, []],
      jsonAllConfig: [this.jsonAllConfig, []]
    });
  }

  getServerGroup(bootstrapServerIs: boolean): FormGroup {
    const port = (bootstrapServerIs) ? DEFAULT_PORT_BOOTSTRAP_NO_SEC : DEFAULT_PORT_SERVER_NO_SEC;
    return this.fb.group({
      host: [this.window.location.hostname, [Validators.required]],
      port: [port, [Validators.required]],
      bootstrapServerIs: [bootstrapServerIs, []],
      securityMode: [this.fb.control(SECURITY_CONFIG_MODE.NO_SEC), []],
      clientPublicKeyOrId: ['', []],
      clientSecretKey: ['', []],
      serverPublicKey: ['', []],
      clientHoldOffTime: [DEFAULT_CLIENT_HOLD_OFF_TIME, [Validators.required]],
      serverId: [DEFAULT_ID_SERVER, [Validators.required]],
      bootstrapServerAccountTimeout: ['', [Validators.required]],
    })
  }

  getFromYml(): void {
    this.bsPublikKeyRPK = BOOTSTRAP_PUBLIC_KEY_RPK;
    this.lwM2mPublikKeyRPK = LWM2M_SERVER_PUBLIC_KEY_RPK;
    this.bsPublikKeyX509 = BOOTSTRAP_PUBLIC_KEY_X509;
    this.lwM2mPublikKeyX509 = LWM2M_SERVER_PUBLIC_KEY_X509;
    //   DEFAULT_PORT_BOOTSTRAP,
    //   DEFAULT_PORT_SERVER,
    //   DEFAULT_PORT_BOOTSTRAP_NO_SEC,
    //   DEFAULT_PORT_SERVER_NO_SEC
  }


  save(): void {
    this.upDateValueToJson();
    this.data.endPoint = this.lwm2mConfigFormGroup.get('endPoint').value.split('\"').join('');
    this.data.jsonAllConfig = this.jsonAllConfig;
    if (this.lwm2mConfigFormGroup.get('securityConfigClientMode').value === SECURITY_CONFIG_MODE.PSK) {
      this.data.endPoint = this.data.jsonAllConfig.client["identity"];
    }
    this.dialogRef.close(this.data);
  }

  cancel(): void {
    this.dialogRef.close(undefined);
  }
}


