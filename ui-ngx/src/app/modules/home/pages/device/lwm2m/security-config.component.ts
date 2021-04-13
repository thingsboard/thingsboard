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


import {Component, Inject, OnInit} from '@angular/core';
import {DialogComponent} from '@shared/components/dialog.component';
import {Store} from '@ngrx/store';
import {AppState} from '@core/core.state';
import {Router} from '@angular/router';
import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material/dialog';
import {FormBuilder, FormGroup, Validators} from '@angular/forms';
import {TranslateService} from '@ngx-translate/core';
import {
  BOOTSTRAP_SERVER,
  BOOTSTRAP_SERVERS,
  ClientSecurityConfig,
  DeviceCredentialsDialogLwm2mData,
  getClientSecurityConfig,
  JSON_ALL_CONFIG,
  KEY_REGEXP_HEX_DEC,
  LEN_MAX_PSK,
  LEN_MAX_PUBLIC_KEY_RPK,
  LWM2M_SERVER,
  SECURITY_CONFIG_MODE,
  SECURITY_CONFIG_MODE_NAMES,
  SecurityConfigModels
} from './security-config.models';
import {WINDOW} from '@core/services/window.service';
import {MatTabChangeEvent} from '@angular/material/tabs';
import {MatTab} from '@angular/material/tabs/tab';

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
  jsonObserveData: {};
  lenMaxKeyClient = LEN_MAX_PSK;
  tabPrevious: MatTab;
  tabIndexPrevious = 0;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: DeviceCredentialsDialogLwm2mData,
              public dialogRef: MatDialogRef<SecurityConfigComponent, object>,
              public fb: FormBuilder,
              public translate: TranslateService,
              @Inject(WINDOW) private window: Window) {
    super(store, router, dialogRef);
  }

  ngOnInit(): void {
    this.jsonAllConfig = JSON.parse(JSON.stringify(this.data.jsonAllConfig)) as SecurityConfigModels;
    this.initConstants();
    this.lwm2mConfigFormGroup = this.initLwm2mConfigFormGroup();
    this.title = this.translate.instant('device.lwm2m-security-info') + ': ' + this.data.endPoint;
    this.lwm2mConfigFormGroup.get('clientCertificate').disable();
    this.initClientSecurityConfig(this.lwm2mConfigFormGroup.get('jsonAllConfig').value);
    this.registerDisableOnLoadFormControl(this.lwm2mConfigFormGroup.get('securityConfigClientMode'));
  }

  private initConstants = (): void => {
    this.bootstrapServers = BOOTSTRAP_SERVERS;
    this.bootstrapServer = BOOTSTRAP_SERVER;
    this.lwm2mServer = LWM2M_SERVER;
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

  private initClientSecurityConfig = (jsonAllConfig: SecurityConfigModels): void =>  {
    switch (jsonAllConfig.client.securityConfigClientMode) {
      case SECURITY_CONFIG_MODE.NO_SEC:
        break;
      case SECURITY_CONFIG_MODE.PSK:
        const clientSecurityConfigPSK = jsonAllConfig.client as ClientSecurityConfig;
        this.lwm2mConfigFormGroup.patchValue({
          identityPSK: clientSecurityConfigPSK.identity,
          clientKey: clientSecurityConfigPSK.key,
        }, {emitEvent: false});
        break;
      case SECURITY_CONFIG_MODE.RPK:
        const clientSecurityConfigRPK = jsonAllConfig.client as ClientSecurityConfig;
        this.lwm2mConfigFormGroup.patchValue({
          clientKey: clientSecurityConfigRPK.key,
        }, {emitEvent: false});
        break;
      case SECURITY_CONFIG_MODE.X509:
        const clientSecurityConfigX509 = jsonAllConfig.client as ClientSecurityConfig;
        this.lwm2mConfigFormGroup.patchValue({
          clientCertificate: clientSecurityConfigX509.x509
        }, {emitEvent: false});
        break;
    }
    this.securityConfigClientUpdateValidators(this.lwm2mConfigFormGroup.get('securityConfigClientMode').value);
  }

  securityConfigClientModeChanged = (mode: SECURITY_CONFIG_MODE): void => {
    switch (mode) {
      case SECURITY_CONFIG_MODE.NO_SEC:
        const clientSecurityConfigNoSEC = getClientSecurityConfig(mode) as ClientSecurityConfig;
        this.jsonAllConfig.client = clientSecurityConfigNoSEC;
        this.lwm2mConfigFormGroup.patchValue({
          jsonAllConfig: this.jsonAllConfig,
          clientCertificate: false
        }, {emitEvent: false});
        break;
      case SECURITY_CONFIG_MODE.PSK:
        const clientSecurityConfigPSK = getClientSecurityConfig(mode, this.lwm2mConfigFormGroup.get('endPoint')
          .value) as ClientSecurityConfig;
        clientSecurityConfigPSK.identity = this.data.endPoint;
        clientSecurityConfigPSK.key = this.lwm2mConfigFormGroup.get('clientKey').value;
        this.jsonAllConfig.client = clientSecurityConfigPSK;
        this.lwm2mConfigFormGroup.patchValue({
          identityPSK: clientSecurityConfigPSK.identity,
          clientCertificate: false
        }, {emitEvent: false});
        break;
      case SECURITY_CONFIG_MODE.RPK:
        const clientSecurityConfigRPK = getClientSecurityConfig(mode) as ClientSecurityConfig;
        clientSecurityConfigRPK.key = this.lwm2mConfigFormGroup.get('clientKey').value;
        this.jsonAllConfig.client = clientSecurityConfigRPK;
        this.lwm2mConfigFormGroup.patchValue({
          clientCertificate: false
        }, {emitEvent: false});
        break;
      case SECURITY_CONFIG_MODE.X509:
        this.jsonAllConfig.client = getClientSecurityConfig(mode) as ClientSecurityConfig;
        this.lwm2mConfigFormGroup.patchValue({
          clientCertificate: true
        }, {emitEvent: false});
        break;
    }
    this.securityConfigClientUpdateValidators(mode);
  }

  private securityConfigClientUpdateValidators = (mode: SECURITY_CONFIG_MODE): void => {
    switch (mode) {
      case SECURITY_CONFIG_MODE.NO_SEC:
        this.setValidatorsNoSecX509();
        break;
      case SECURITY_CONFIG_MODE.PSK:
        this.lenMaxKeyClient = LEN_MAX_PSK;
        this.setValidatorsPskRpk(mode);
        break;
      case SECURITY_CONFIG_MODE.RPK:
        this.lenMaxKeyClient = LEN_MAX_PUBLIC_KEY_RPK;
        this.setValidatorsPskRpk(mode);
        break;
      case SECURITY_CONFIG_MODE.X509:
        this.lenMaxKeyClient = LEN_MAX_PUBLIC_KEY_RPK;
        this.setValidatorsNoSecX509();
        break;
    }
    this.lwm2mConfigFormGroup.updateValueAndValidity();
  }

  private setValidatorsNoSecX509 = (): void => {
    this.lwm2mConfigFormGroup.get('identityPSK').setValidators([]);
    this.lwm2mConfigFormGroup.get('clientKey').setValidators([]);
  }

  private setValidatorsPskRpk = (mode: SECURITY_CONFIG_MODE): void => {
    if (mode === SECURITY_CONFIG_MODE.PSK) {
      this.lwm2mConfigFormGroup.get('identityPSK').setValidators([Validators.required]);
    } else {
      this.lwm2mConfigFormGroup.get('identityPSK').setValidators([]);
    }
    this.lwm2mConfigFormGroup.get('clientKey').setValidators([Validators.required,
      Validators.pattern(KEY_REGEXP_HEX_DEC),
      Validators.maxLength(this.lenMaxKeyClient), Validators.minLength(this.lenMaxKeyClient)]);
  }

  tabChanged = (tabChangeEvent: MatTabChangeEvent): void => {
    if (this.tabIndexPrevious !== tabChangeEvent.index) { this.upDateValueToJson(); }
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
      case 2:
        this.upDateValueToJsonTab2();
        break;
    }
  }

  private upDateValueToJsonTab0 = (): void => {
    if (this.lwm2mConfigFormGroup !== null) {
      if (!this.lwm2mConfigFormGroup.get('endPoint').pristine && this.lwm2mConfigFormGroup.get('endPoint').valid) {
        this.data.endPoint = this.lwm2mConfigFormGroup.get('endPoint').value;
        /** Client mode == PSK */
        if (this.lwm2mConfigFormGroup.get('securityConfigClientMode').value === SECURITY_CONFIG_MODE.PSK) {
          const endPoint = 'endpoint';
          this.jsonAllConfig.client[endPoint] = this.data.endPoint;
          this.lwm2mConfigFormGroup.get('endPoint').markAsPristine({
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

  private upDateValueToJsonTab1 = (): void => {
    if (this.lwm2mConfigFormGroup !== null) {
      if (this.bootstrapFormGroup !== null && !this.bootstrapFormGroup.pristine && this.bootstrapFormGroup.valid) {
        this.jsonAllConfig.bootstrap.bootstrapServer = this.bootstrapFormGroup.value;
        this.bootstrapFormGroup.markAsPristine({
          onlySelf: true
        });
        this.upDateJsonAllConfig();
      }

      if (this.lwm2mServerFormGroup !== null && !this.lwm2mServerFormGroup.pristine && this.lwm2mServerFormGroup.valid) {
        this.jsonAllConfig.bootstrap.lwm2mServer = this.lwm2mServerFormGroup.value;
        this.lwm2mServerFormGroup.markAsPristine({
          onlySelf: true
        });
        this.upDateJsonAllConfig();
      }
    }
  }

  private upDateValueToJsonTab2 = (): void => {
    if (!this.lwm2mConfigFormGroup.get(this.formControlNameJsonAllConfig).pristine &&
      this.lwm2mConfigFormGroup.get(this.formControlNameJsonAllConfig).valid) {
      this.jsonAllConfig = this.lwm2mConfigFormGroup.get(this.formControlNameJsonAllConfig).value;
      this.lwm2mConfigFormGroup.get(this.formControlNameJsonAllConfig).markAsPristine({
        onlySelf: true
      });
    }
  }

  private updateIdentityPSK = (): void => {
    const securityMode = 'securityMode';
    if (this.lwm2mConfigFormGroup.get('bootstrapServer').value[securityMode] === SECURITY_CONFIG_MODE.PSK) {
      this.lwm2mConfigFormGroup.get('bootstrapFormGroup').patchValue({
        clientPublicKeyOrId: this.lwm2mConfigFormGroup.get('identityPSK').value
      });
      const identity = 'identity';
      this.jsonAllConfig.client[identity] = this.lwm2mConfigFormGroup.get('identityPSK').value;
      this.upDateJsonAllConfig();
    }
    if (this.lwm2mConfigFormGroup.get('lwm2mServer').value[securityMode] === SECURITY_CONFIG_MODE.PSK) {
      this.lwm2mConfigFormGroup.get('lwm2mServerFormGroup').patchValue({
        clientPublicKeyOrId: this.lwm2mConfigFormGroup.get('identityPSK').value
      });
      this.jsonAllConfig.bootstrap.lwm2mServer.clientPublicKeyOrId = this.lwm2mConfigFormGroup.get('identityPSK').value;
      this.upDateJsonAllConfig();
    }
  }

  private updateClientKey = (): void => {
    const key = 'key';
    const securityMode = 'securityMode';
    this.jsonAllConfig.client[key] = this.lwm2mConfigFormGroup.get('clientKey').value;
    if (this.lwm2mConfigFormGroup.get('bootstrapServer').value[securityMode] === SECURITY_CONFIG_MODE.PSK) {
      this.lwm2mConfigFormGroup.get('bootstrapServer').patchValue({
        clientSecretKey: this.jsonAllConfig.client[key]
      }, {emitEvent: false});
      this.jsonAllConfig.bootstrap.bootstrapServer.clientSecretKey = this.jsonAllConfig.client[key];
    }
    if (this.lwm2mConfigFormGroup.get('lwm2mServer').value[securityMode] === SECURITY_CONFIG_MODE.PSK) {
      this.lwm2mConfigFormGroup.get('lwm2mServer').patchValue({
        clientSecretKey: this.jsonAllConfig.client[key]
      }, {emitEvent: false});
      this.jsonAllConfig.bootstrap.lwm2mServer.clientSecretKey = this.jsonAllConfig.client[key];
    }
    this.upDateJsonAllConfig();
  }

  private upDateJsonAllConfig = (): void => {
    this.data.jsonAllConfig = JSON.parse(JSON.stringify(this.jsonAllConfig));
    this.lwm2mConfigFormGroup.patchValue({
      jsonAllConfig: JSON.parse(JSON.stringify(this.jsonAllConfig))
    }, {emitEvent: false});
    this.lwm2mConfigFormGroup.markAsDirty();
  }

  private initLwm2mConfigFormGroup = (): FormGroup => {
    if (SECURITY_CONFIG_MODE[this.jsonAllConfig.client.securityConfigClientMode.toString()] === SECURITY_CONFIG_MODE.PSK) {
      const endpoint = 'endpoint';
      this.data.endPoint = this.jsonAllConfig.client[endpoint];
    }
    return this.fb.group({
      securityConfigClientMode: [SECURITY_CONFIG_MODE[this.jsonAllConfig.client.securityConfigClientMode.toString()], []],
      identityPSK: ['', []],
      clientKey: ['', []],
      clientCertificate: [false, []],
      bootstrapServer: [this.jsonAllConfig.bootstrap[this.bootstrapServer], []],
      lwm2mServer: [this.jsonAllConfig.bootstrap[this.lwm2mServer], []],
      bootstrapFormGroup: this.getServerGroup(),
      lwm2mServerFormGroup: this.getServerGroup(),
      endPoint: [this.data.endPoint, []],
      jsonAllConfig: [this.jsonAllConfig, []]
    });
  }

  private getServerGroup = (): FormGroup => {
    return this.fb.group({
      securityMode: [this.fb.control(SECURITY_CONFIG_MODE.NO_SEC), []],
      clientPublicKeyOrId: ['', []],
      clientSecretKey: ['', []]
    });
  }

  save(): void {
    this.upDateValueToJson();
    this.data.endPoint = this.lwm2mConfigFormGroup.get('endPoint').value.split('\'').join('');
    this.data.jsonAllConfig = this.jsonAllConfig;
    if (this.lwm2mConfigFormGroup.get('securityConfigClientMode').value === SECURITY_CONFIG_MODE.PSK) {
      const identity = 'identity';
      this.data.endPoint = this.data.jsonAllConfig.client[identity];
    }
    this.dialogRef.close(this.data);
  }

  cancel(): void {
    this.dialogRef.close(undefined);
  }
}


