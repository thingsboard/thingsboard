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

import {Component, forwardRef, Inject, Input, OnInit, ViewChild} from "@angular/core";

import {
  ControlValueAccessor,
  FormBuilder, FormGroup, NG_VALUE_ACCESSOR, Validators
} from "@angular/forms";
import {
  BOOTSTRAP_SERVER,
  SECURITY_CONFIG_MODE,
  SECURITY_CONFIG_MODE_NAMES,
  DEFAULT_HOST,
  SecurityConfig,
  DEFAULT_PORT_BOOTSTRAP,
  DEFAULT_PORT_SERVER,
  DEFAULT_PORT_SERVER_NOSEC,
  DEFAULT_PORT_BOOTSTRAP_NOSEC, DEFAULT_ID_BOOTSTRAP, DEFAULT_ID_SERVER
} from "@home/pages/device/lwm2m/security-config.models";
import {Store} from "@ngrx/store";
import {AppState} from "@core/core.state";
import {MAT_DIALOG_DATA, MatDialogRef} from "@angular/material/dialog";
import {
  DeviceCredentialsDialogLwm2mData
} from "@home/pages/device/lwm2m/security-config.component";
import {PageComponent} from "@shared/components/page.component";
import {MatPaginator} from "@angular/material/paginator";

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

  securityConfigLwM2MType = SECURITY_CONFIG_MODE;
  securityConfigLwM2MTypes = Object.keys(SECURITY_CONFIG_MODE);
  credentialTypeLwM2MNamesMap = SECURITY_CONFIG_MODE_NAMES;
  @Input() server: string;
  @Input() serverFormGroup: FormGroup;
  serverData: SecurityConfig;

  @ViewChild(MatPaginator) paginator: MatPaginator;

  constructor(protected store: Store<AppState>,
              @Inject(MAT_DIALOG_DATA) public data: DeviceCredentialsDialogLwm2mData,
              public dialogRef: MatDialogRef<SecurityConfigServerComponent, object>,
              public fb: FormBuilder) {
    super(store);
  }

  ngOnInit(): void {
    this.serverFormGroup.addControl('host', this.fb.control('', [Validators.required]));
    this.serverFormGroup.addControl('port', this.fb.control(null, [Validators.required]));
    this.serverFormGroup.addControl('isBootstrapServer', this.fb.control(false, [Validators.required]));
    this.serverFormGroup.addControl('securityMode', this.fb.control(SECURITY_CONFIG_MODE.NO_SEC, []));
    this.serverFormGroup.addControl('clientPublicKeyOrId', this.fb.control('', []));
    this.serverFormGroup.addControl('clientSecretKey', this.fb.control('', []));
    this.serverFormGroup.addControl('serverPublicKey', this.fb.control('', []));
    this.serverFormGroup.addControl('clientHoldOffTime', this.fb.control(null, [Validators.required]));
    this.serverFormGroup.addControl('serverId', this.fb.control(null, [Validators.required]));
    this.serverFormGroup.addControl('bootstrapServerAccountTimeout', this.fb.control(null, [Validators.required]));
    this.registerDisableOnLoadFormControl(this.serverFormGroup);
  }

  updateValueFields(): void {
    let defaultPortServer = (!this.serverData.securityMode || this.serverData.securityMode === SECURITY_CONFIG_MODE.NO_SEC.toString()) ? DEFAULT_PORT_SERVER_NOSEC : DEFAULT_PORT_SERVER;
    let defaultPortBootstrap = (!this.serverData.securityMode || this.serverData.securityMode === SECURITY_CONFIG_MODE.NO_SEC.toString()) ? DEFAULT_PORT_BOOTSTRAP_NOSEC : DEFAULT_PORT_BOOTSTRAP;
    this.serverFormGroup.patchValue({
      host: (this.serverData.host) ? this.serverData.host : DEFAULT_HOST,
      port: (this.serverData.port) ? this.serverData.port : (this.server === BOOTSTRAP_SERVER) ? defaultPortBootstrap : defaultPortServer,
      isBootstrapServer: (this.serverData.isBootstrapServer) ? this.serverData.isBootstrapServer : (this.server === BOOTSTRAP_SERVER) ? true : false,
      securityMode: (this.serverData.securityMode) ? SECURITY_CONFIG_MODE[this.serverData.securityMode] : SECURITY_CONFIG_MODE.NO_SEC,
      clientPublicKeyOrId: (this.serverData.clientPublicKeyOrId) ? this.serverData.clientPublicKeyOrId : null,
      clientSecretKey: (this.serverData.clientSecretKey) ? this.serverData.clientSecretKey : null,
      serverPublicKey: (this.serverData.serverPublicKey) ? this.serverData.serverPublicKey : null,
      clientHoldOffTime: (this.serverData.clientHoldOffTime) ? this.serverData.clientHoldOffTime : 1,
      serverId: (this.serverData.serverId) ? this.serverData.serverId : (this.server === BOOTSTRAP_SERVER) ? DEFAULT_ID_BOOTSTRAP : DEFAULT_ID_SERVER,
      bootstrapServerAccountTimeout: (this.serverData.bootstrapServerAccountTimeout) ? this.serverData.bootstrapServerAccountTimeout : 0
    }, {emitEvent: true});
  }

  securityModeChanged(): void {
    const securityConfigClientMode = this.serverFormGroup.get('securityMode').value as SECURITY_CONFIG_MODE;
    switch (securityConfigClientMode) {
      case SECURITY_CONFIG_MODE.NO_SEC:
        this.serverFormGroup.get('clientPublicKeyOrId').setValidators([]);
        this.serverFormGroup.get('clientSecretKey').setValidators([]);
        this.serverFormGroup.get('serverPublicKey').setValidators([]);
        break;
      case SECURITY_CONFIG_MODE.PSK:
        this.serverFormGroup.get('clientPublicKeyOrId').setValidators([Validators.required]);
        this.serverFormGroup.get('clientSecretKey').setValidators([Validators.required]);
        this.serverFormGroup.get('serverPublicKey').setValidators([]);
        break;
      case SECURITY_CONFIG_MODE.RPK:
      case SECURITY_CONFIG_MODE.X509:
        this.serverFormGroup.get('clientPublicKeyOrId').setValidators([Validators.required]);
        this.serverFormGroup.get('clientSecretKey').setValidators([Validators.required]);
        this.serverFormGroup.get('serverPublicKey').setValidators([Validators.required]);
        break;
    }
    this.serverFormGroup.updateValueAndValidity();
  }

  writeValue(value: any): void {
    this.serverData = value;
    if (this.serverData) {
      this.updateValueFields();
    }
  }

  registerOnChange(fn: (value: any) => any): void {
    console.log('test1')
  }

  registerOnTouched(fn: any): void {
    console.log('test2')
  }

  setDisabledState?(isDisabled: boolean): void {
    console.log('test3')
  }
}
