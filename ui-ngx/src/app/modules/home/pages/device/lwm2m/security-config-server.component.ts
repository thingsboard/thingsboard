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
  FormBuilder, FormGroup, FormControl, NG_VALIDATORS, NG_VALUE_ACCESSOR, Validator, Validators
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
import {Store} from "@ngrx/store";
import {AppState} from "@core/core.state";
import {Router} from "@angular/router";
import {MAT_DIALOG_DATA, MatDialogRef} from "@angular/material/dialog";
import {TranslateService} from "@ngx-translate/core";
import {WINDOW} from "@core/services/window.service";
import {
  DeviceCredentialsDialogLwm2mData
} from "@home/pages/device/lwm2m/security-config.component";
import {PageComponent} from "@shared/components/page.component";

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

export class SecurityConfigServerComponent extends PageComponent implements OnInit, ControlValueAccessor {
  registerOnChange(fn: any): void {
    console.log('test1')
  }

  registerOnTouched(fn: any): void {
    console.log('test2')
  }

  setDisabledState?(isDisabled: boolean): void {
    console.log('test3')
  }

  isReadOnly: boolean;
  lwm2mBootstrapServer: FormGroup;
  securityConfigLwM2MType = SECURITY_CONFIG_MODE;
  securityConfigLwM2MTypes = Object.keys(SECURITY_CONFIG_MODE);
  credentialTypeLwM2MNamesMap = SECURITY_CONFIG_MODE_NAMES;
  @Input() server: string;
  @Input() parentFormGroup: FormGroup;
  serverData: SecurityConfig;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: DeviceCredentialsDialogLwm2mData,
              public dialogRef: MatDialogRef<SecurityConfigServerComponent, object>,
              public fb: FormBuilder,
              private translate: TranslateService,
              @Inject(WINDOW) private window: Window) {
    super(store);

  }

  ngOnInit(): void {
    this.lwm2mBootstrapServer = this.fb.group({
      host: ['', Validators.required],
      port: [null, Validators.required],
      isBootstrapServer: false,
      securityMode: SECURITY_CONFIG_MODE.NO_SEC,
      clientPublicKeyOrId: ['', []],
      clientSecretKey: ['', []],
      serverPublicKey: ['', []],
      clientHoldOffTime: [null, Validators.required],
      serverId: [null, Validators.required],
      bootstrapServerAccountTimeout: [null, Validators.required],
    })
    if (this.isReadOnly) {
      this.lwm2mBootstrapServer.disable({emitEvent: false});
    } else {
      this.registerDisableOnLoadFormControl(this.lwm2mBootstrapServer.get('securityMode'));
    }

  }

  securityConfigClientModeChanged(): void {

  }

  updateValueFields(): void {
    let defaultPortServer = (!this.serverData.securityMode || this.serverData.securityMode === SECURITY_CONFIG_MODE.NO_SEC.toString()) ? DEFAULT_PORT_SERVER_NOSEC : DEFAULT_PORT_SERVER;
    let defaultPortBootstrap = (!this.serverData.securityMode || this.serverData.securityMode === SECURITY_CONFIG_MODE.NO_SEC.toString()) ? DEFAULT_PORT_BOOTSTRAP_NOSEC : DEFAULT_PORT_BOOTSTRAP;
    this.lwm2mBootstrapServer.patchValue({
      host: (this.serverData.host) ? this.serverData.host : DEFAULT_HOST,
      port: (this.serverData.port) ? this.serverData.port : (this.server === BOOTSTRAP_SERVER) ? defaultPortBootstrap : defaultPortServer,
      isBootstrapServer: (this.serverData.isBootstrapServer) ? this.serverData.isBootstrapServer : (this.server === BOOTSTRAP_SERVER) ? true : false,
      securityMode: (this.serverData.securityMode) ? SECURITY_CONFIG_MODE[this.serverData.securityMode] : SECURITY_CONFIG_MODE.NO_SEC,
      clientPublicKeyOrId: (this.serverData.clientPublicKeyOrId) ? this.serverData.clientPublicKeyOrId : '',
      clientSecretKey: (this.serverData.clientSecretKey) ? this.serverData.clientSecretKey : '',
      serverPublicKey: (this.serverData.serverPublicKey) ? this.serverData.serverPublicKey : '',
      clientHoldOffTime: (this.serverData.clientHoldOffTime) ? this.serverData.clientHoldOffTime : 1,
      serverId: (this.serverData.serverId) ? this.serverData.serverId : (this.server === BOOTSTRAP_SERVER) ? DEFAULT_ID_BOOTSTRAP : DEFAULT_ID_SERVER,
      bootstrapServerAccountTimeout: (this.serverData.bootstrapServerAccountTimeout) ? this.serverData.bootstrapServerAccountTimeout : 0,
    }, {emitEvent: true});
  }

  writeValue(value: any): void {
    debugger
    this.serverData = value;
    console.log(this.serverData, this.serverData.isBootstrapServer);
    if (this.serverData) {this. updateValueFields();}
  }

}
