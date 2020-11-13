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

import { Component, forwardRef, Inject, Input, OnInit } from "@angular/core";

import {
  ControlValueAccessor,
  FormBuilder, FormGroup, NG_VALUE_ACCESSOR, NgModel, Validators
} from "@angular/forms";
import {
  SECURITY_CONFIG_MODE,
  SECURITY_CONFIG_MODE_NAMES,
  KEY_PUBLIC_REGEXP_PSK,
  ServerSecurityConfig,
  LEN_MAX_PSK,
  LEN_MAX_PRIVATE_KEY,
  LEN_MAX_PUBLIC_KEY_RPK,
  LEN_MAX_PUBLIC_KEY_X509,
  KEY_PUBLIC_REGEXP_X509
} from "./profile-config.models";
import { Store } from "@ngrx/store";
import { AppState } from "@core/core.state";
import { coerceBooleanProperty } from "@angular/cdk/coercion";
import {
  DEFAULT_CLIENT_HOLD_OFF_TIME, DEFAULT_ID_SERVER,
  DEFAULT_PORT_BOOTSTRAP_NO_SEC,
  DEFAULT_PORT_SERVER_NO_SEC
} from "../../../../pages/device/lwm2m/security-config.models";
import { WINDOW } from "../../../../../../core/services/window.service";
import { pairwise, startWith } from 'rxjs/operators';
import { DeviceProfileService } from '@core/http/device-profile.service';

@Component({
  selector: 'tb-profile-lwm2m-device-config-server',
  templateUrl: './lwm2m-device-config-server.component.html',
  styleUrls: [],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => Lwm2mDeviceConfigServerComponent),
      multi: true
    }
  ]
})

export class Lwm2mDeviceConfigServerComponent implements OnInit, ControlValueAccessor, Validators {

  valuePrev = null as any;
  private requiredValue: boolean;
  serverFormGroup: FormGroup;
  securityConfigLwM2MType = SECURITY_CONFIG_MODE;
  securityConfigLwM2MTypes = Object.keys(SECURITY_CONFIG_MODE);
  credentialTypeLwM2MNamesMap = SECURITY_CONFIG_MODE_NAMES;
  lenMaxClientPublicKeyOrId = LEN_MAX_PSK;
  lenMaxClientSecretKey = LEN_MAX_PRIVATE_KEY;
  lenMaxServerPublicKey = LEN_MAX_PUBLIC_KEY_RPK;
  currentSecurityMode = null;


  @Input()
  disabled: boolean;

  @Input()
  bootstrapServerIs: boolean

  get required(): boolean {
    return this.requiredValue;
  }

  @Input()
  set required(value: boolean) {
    this.requiredValue = coerceBooleanProperty(value);
  }

  constructor(protected store: Store<AppState>,
              public fb: FormBuilder,
              private deviceProfileService: DeviceProfileService,
              @Inject(WINDOW) private window: Window) {
    this.serverFormGroup = this.getServerGroup();
    this.serverFormGroup.get('securityMode').valueChanges.pipe(
      startWith(null),
      pairwise()
    ).subscribe(([previousValue, currentValue]) => {
      if (previousValue === null || previousValue !== currentValue) {
        this.getLwm2mBootstrapSecurityInfo(currentValue);
        this.updateValidate(currentValue);
        this.serverFormGroup.updateValueAndValidity();
      }

    });
    this.serverFormGroup.valueChanges.subscribe(value => {
      if (this.disabled !== undefined && !this.disabled) {
        this.propagateChangeState(value);
      }
    });
  }

  ngOnInit(): void {
  }

  updateValueFields(serverData: ServerSecurityConfig): void {
    serverData['bootstrapServerIs'] = this.bootstrapServerIs;
    this.serverFormGroup.patchValue(serverData, {emitEvent: false});
    this.serverFormGroup.get('bootstrapServerIs').disable();
    const securityMode = this.serverFormGroup.get('securityMode').value as SECURITY_CONFIG_MODE;
    this.updateValidate(securityMode);
  }

  updateValidate(securityMode: SECURITY_CONFIG_MODE): void {
    switch (securityMode) {
      case SECURITY_CONFIG_MODE.NO_SEC:
        this.serverFormGroup.get('serverPublicKey').setValidators([]);
        break;
      case SECURITY_CONFIG_MODE.PSK:
        this.lenMaxClientPublicKeyOrId = LEN_MAX_PUBLIC_KEY_RPK;
        this.lenMaxClientSecretKey = LEN_MAX_PSK;
        this.lenMaxServerPublicKey = LEN_MAX_PUBLIC_KEY_RPK;
        this.serverFormGroup.get('serverPublicKey').setValidators([]);
        break;
      case SECURITY_CONFIG_MODE.RPK:
        this.lenMaxClientPublicKeyOrId = LEN_MAX_PUBLIC_KEY_X509;
        this.lenMaxClientSecretKey = LEN_MAX_PRIVATE_KEY;
        this.lenMaxServerPublicKey = LEN_MAX_PUBLIC_KEY_X509;
        this.serverFormGroup.get('serverPublicKey').setValidators([Validators.required, Validators.pattern(KEY_PUBLIC_REGEXP_PSK)]);
        break;
      case SECURITY_CONFIG_MODE.X509:
        this.lenMaxClientPublicKeyOrId = LEN_MAX_PUBLIC_KEY_X509;
        this.lenMaxClientSecretKey = LEN_MAX_PRIVATE_KEY;
        this.lenMaxServerPublicKey = LEN_MAX_PUBLIC_KEY_X509;
        this.serverFormGroup.get('serverPublicKey').setValidators([Validators.required, Validators.pattern(KEY_PUBLIC_REGEXP_X509)]);
        break;
    }
    this.checkValueWithNewValidate();
  }

  checkValueWithNewValidate(): void {
    this.serverFormGroup.patchValue({
        host: this.serverFormGroup.get('host').value,
        port: this.serverFormGroup.get('port').value,
        bootstrapServerIs: this.serverFormGroup.get('bootstrapServerIs').value,
        serverPublicKey: this.serverFormGroup.get('serverPublicKey').value,
        clientHoldOffTime: this.serverFormGroup.get('clientHoldOffTime').value,
        serverId: this.serverFormGroup.get('serverId').value,
        bootstrapServerAccountTimeout: this.serverFormGroup.get('bootstrapServerAccountTimeout').value,
      },
      {emitEvent: true});
  }

  writeValue(value: any): void {
    if (value) {
      this.updateValueFields(value);
    }
  }

  private propagateChange = (v: any) => {
  };

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  private propagateChangeState(value: any): void {
    if (value !== undefined) {
      if (this.valuePrev === null) {
        this.valuePrev = "init";
      } else if (this.valuePrev === "init") {
        this.valuePrev = value;
      } else if (JSON.stringify(value) !== JSON.stringify(this.valuePrev)) {
        this.valuePrev = value;
        // debugger
        if (this.serverFormGroup.valid) {
          this.propagateChange(value);
        } else {
          this.propagateChange(null);
        }
      }
    }
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    this.valuePrev = null;
    if (isDisabled) {
      this.serverFormGroup.disable({emitEvent: false});
    } else {
      this.serverFormGroup.enable({emitEvent: false});
    }
  }

  registerOnTouched(fn: any): void {
  }

  getServerGroup(): FormGroup {
    const port = (this.bootstrapServerIs) ? DEFAULT_PORT_BOOTSTRAP_NO_SEC : DEFAULT_PORT_SERVER_NO_SEC;
    return this.fb.group({
      host: [this.window.location.hostname, this.required ? [Validators.required] : []],
      port: [port, this.required ? [Validators.required] : []],
      bootstrapServerIs: [this.bootstrapServerIs, []],
      securityMode: [this.fb.control(SECURITY_CONFIG_MODE.NO_SEC), []],
      serverPublicKey: ['', this.required ? [Validators.required] : []],
      clientHoldOffTime: [DEFAULT_CLIENT_HOLD_OFF_TIME, this.required ? [Validators.required] : []],
      serverId: [DEFAULT_ID_SERVER, this.required ? [Validators.required] : []],
      bootstrapServerAccountTimeout: ['', this.required ? [Validators.required] : []],
    })
  }

  getLwm2mBootstrapSecurityInfo(mode: string) {
    this.deviceProfileService.getLwm2mBootstrapSecurityInfo(mode, this.serverFormGroup.get('bootstrapServerIs').value).subscribe(
      (serverSecurityConfig) => {
        this.serverFormGroup.patchValue({
            host: serverSecurityConfig.host,
            port: serverSecurityConfig.port,
            serverPublicKey: serverSecurityConfig.serverPublicKey,
            clientHoldOffTime: serverSecurityConfig.clientHoldOffTime,
            serverId: serverSecurityConfig.serverId,
            bootstrapServerAccountTimeout: serverSecurityConfig.bootstrapServerAccountTimeout
          },
          {emitEvent: true});
      }
    );
  }
}
