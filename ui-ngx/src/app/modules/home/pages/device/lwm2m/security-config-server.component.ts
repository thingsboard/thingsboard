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

import {Component, forwardRef, Inject, Input, OnInit} from "@angular/core";
import {
  ControlValueAccessor,
  FormBuilder, FormGroup, NG_VALIDATORS, NG_VALUE_ACCESSOR, Validator
} from "@angular/forms";
import {
  BOOTSTRAP_SERVER,
  SECURITY_CONFIG_MODE,
  SECURITY_CONFIG_MODE_NAMES,
  DEFAULT_HOST,
  SecurityConfig,
  SecurityConfigModels,
  DEFAULT_PORT_BOOTSTRAP,
  DEFAULT_PORT_SERVER,
  DEFAULT_PORT_SERVER_NOSEC,
  DEFAULT_PORT_BOOTSTRAP_NOSEC, DEFAULT_ID_BOOTSTRAP, DEFAULT_ID_SERVER
} from "@home/pages/device/lwm2m/security-config.models";
import {coerceBooleanProperty} from "@angular/cdk/coercion";
import {Store} from "@ngrx/store";
import {AppState} from "@core/core.state";
import {Router} from "@angular/router";
import {MAT_DIALOG_DATA, MatDialogRef} from "@angular/material/dialog";
import {TranslateService} from "@ngx-translate/core";
import {WINDOW} from "@core/services/window.service";
import {
  DeviceCredentialsDialogLwm2mData,
  upDatejsonAllConfig
} from "@home/pages/device/lwm2m/security-config.component";

@Component({
  selector: 'tb-security-config-server-lwm2m',
  templateUrl: './security-config-server.component.html',
  styleUrls: [],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => SecurityConfigServerComponent),
      multi: true
    }
  ]
})

export class SecurityConfigServerComponent implements OnInit {

  lwm2mBootstrapServer: FormGroup;
  securityConfigLwM2MType = SECURITY_CONFIG_MODE;
  securityConfigLwM2MTypes = Object.keys(SECURITY_CONFIG_MODE);
  credentialTypeLwM2MNamesMap = SECURITY_CONFIG_MODE_NAMES;
  jsonAllConfig: SecurityConfigModels

  @Input() server: string;
  @Input() parentFormGroup: FormGroup;
  @Input() formControlNameJsonObject: string;
  // @Input() disabled: boolean;
  // private propagateChange = null;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: DeviceCredentialsDialogLwm2mData,
              public dialogRef: MatDialogRef<SecurityConfigServerComponent, object>,
              public fb: FormBuilder,
              private translate: TranslateService,
              @Inject(WINDOW) private window: Window) {
    // @ts-ignore
    // super(store, router, dialogRef);
  }


  ngOnInit(): void {
    debugger
    this.jsonAllConfig = JSON.parse(JSON.stringify(this.data.jsonAllConfig)) as SecurityConfigModels;
    let server = (this.jsonAllConfig.bootstrap[this.server]) ? this.jsonAllConfig.bootstrap[this.server] : {} as SecurityConfig;
    let defaultPortServer = (!server.securityMode || server.securityMode === SECURITY_CONFIG_MODE.NO_SEC.toString()) ? DEFAULT_PORT_SERVER_NOSEC : DEFAULT_PORT_SERVER;
    let defaultPortBootstrap = (!server.securityMode || server.securityMode === SECURITY_CONFIG_MODE.NO_SEC.toString()) ? DEFAULT_PORT_BOOTSTRAP_NOSEC : DEFAULT_PORT_BOOTSTRAP;
    this.lwm2mBootstrapServer = this.fb.group({
      // bootstrapServer: server,
      host: (server.host) ? server.host : DEFAULT_HOST,
      port: (server.port) ? server.port : (this.server === BOOTSTRAP_SERVER) ? defaultPortBootstrap : defaultPortServer,
      isBootstrapServer: (server.isBootstrapServer) ? server.isBotstrapServer : (this.server === BOOTSTRAP_SERVER) ? true : false,
      securityMode: (server.securityMode) ? SECURITY_CONFIG_MODE[server.securityMode] : SECURITY_CONFIG_MODE.NO_SEC,
      clientPublicKeyOrId: (server.clientPublicKeyOrId) ? server.clientPublicKeyOrId : '',
      clientSecretKey: (server.clientSecretKey) ? server.clientSecretKey : '',
      serverPublicKey: (server.serverPublicKey) ? server.serverPublicKey : '',
      clientHoldOffTime: (server.clientHoldOffTime) ? server.clientHoldOffTime : 1,
      serverId: (server.serverId) ? server.serverId : (this.server === BOOTSTRAP_SERVER) ? DEFAULT_ID_BOOTSTRAP : DEFAULT_ID_SERVER,
      bootstrapServerAccountTimeout: (server.bootstrapServerAccountTimeout) ? server.bootstrapServerAccountTimeout : 0,
    })
  }


  writeValue(value: any): void {

  }

  securityConfigClientModeChanged(): void {
    debugger
    let jsonAllConfig = this.parentFormGroup.get(this.formControlNameJsonObject).value as SecurityConfigModels;
    jsonAllConfig.bootstrap.bootstrapServer.securityMode = this.lwm2mBootstrapServer.get("securityMode").value.toString();
    upDatejsonAllConfig(this.parentFormGroup, jsonAllConfig);
  }

  // registerOnChange(fn: any): void {
  //   this.propagateChange = fn;
  // }
  //
  // registerOnTouched(fn: any): void {
  // }
  //
  // setDisabledState(isDisabled: boolean): void {
  //   this.disabled = isDisabled;
  // }
}
