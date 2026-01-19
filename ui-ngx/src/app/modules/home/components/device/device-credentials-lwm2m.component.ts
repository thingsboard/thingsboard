///
/// Copyright © 2016-2026 The Thingsboard Authors
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

import { Component, forwardRef, Input, OnDestroy } from '@angular/core';
import {
  ControlValueAccessor,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormGroup,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import {
  getDefaultClientSecurityConfig,
  getDefaultServerSecurityConfig,
  Lwm2mClientKeyTooltipTranslationsMap,
  Lwm2mSecurityConfigModels,
  Lwm2mSecurityType,
  Lwm2mSecurityTypeTranslationMap
} from '@shared/models/lwm2m-security-config.models';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { isDefinedAndNotNull } from '@core/utils';
import { DeviceId } from "@shared/models/id/device-id";
import { DeviceService } from "@core/http/device.service";
import { ActionNotificationShow } from "@core/notification/notification.actions";
import { Store } from "@ngrx/store";
import { AppState } from "@core/core.state";

@Component({
  selector: 'tb-device-credentials-lwm2m',
  templateUrl: './device-credentials-lwm2m.component.html',
  styleUrls: ['./device-credentials-lwm2m.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => DeviceCredentialsLwm2mComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => DeviceCredentialsLwm2mComponent),
      multi: true
    }
  ]
})

export class DeviceCredentialsLwm2mComponent implements ControlValueAccessor, Validator, OnDestroy {

  lwm2mConfigFormGroup: UntypedFormGroup;
  securityConfigLwM2MType = Lwm2mSecurityType;
  securityConfigLwM2MTypes = Object.keys(Lwm2mSecurityType);
  credentialTypeLwM2MNamesMap = Lwm2mSecurityTypeTranslationMap;
  clientKeyTooltipNamesMap = Lwm2mClientKeyTooltipTranslationsMap;

  private destroy$ = new Subject<void>();
  private propagateChange = (v: any) => {};

  @Input()
  deviceId: DeviceId;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder,
              private deviceService: DeviceService) {
    this.lwm2mConfigFormGroup = this.initLwm2mConfigForm();
  }

  writeValue(obj: string) {
    if (isDefinedAndNotNull(obj)) {
      this.initClientSecurityConfig(JSON.parse(obj));
    }
  }

  registerOnChange(fn: any) {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any) {}

  setDisabledState(isDisabled: boolean): void {
    if (isDisabled) {
      this.lwm2mConfigFormGroup.disable({emitEvent: false});
    } else {
      this.lwm2mConfigFormGroup.enable({emitEvent: false});
      this.securityConfigClientUpdateValidators(this.lwm2mConfigFormGroup.get('client.securityConfigClientMode').value);
    }
  }

  validate(): ValidationErrors | null {
    return this.lwm2mConfigFormGroup.valid ? null : {
      securityConfigLWm2m: false
    };
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * AbstractRpcController -> rpcController
   * - API
   * "/api/plugins/rpc/twoway/${this.deviceId.id}"
   * - DiscoveryAll
   * requestBody =  "{\"method\":\"DiscoverAll\"}";
   * - "Registration Update Trigger",
   * requestBody =  "{\"method\": \"Execute\", \"params\": {\"id\": \"/1/0/8\"}}
   * - "Bootstrap-Request Trigger"
   * requestBody =  "{\"method\": \"Execute\", \"params\": {\"id\": \"/1/0/9\"}}
   */

  public rebootDevice(isBootstrapServer: boolean): void {
    this.deviceService.rebootDevice(this.deviceId.id, isBootstrapServer).subscribe(responseReboot => {
      if (responseReboot.result === 'SUCCESS') {
        this.store.dispatch(new ActionNotificationShow(
          {
            message: responseReboot.msg,
            type: 'success',
            duration: 1500,
            verticalPosition: 'top',
            horizontalPosition: 'left'
          }));
      } else {
        this.store.dispatch(new ActionNotificationShow(
          {
            message: responseReboot.msg,
            type: 'error',
            verticalPosition: 'top',
            horizontalPosition: 'left'
          }));
      }
    });
  }

  private initClientSecurityConfig(config: Lwm2mSecurityConfigModels): void {
    this.lwm2mConfigFormGroup.patchValue(config, {emitEvent: false});
    this.securityConfigClientUpdateValidators(config.client.securityConfigClientMode);
  }

  private securityConfigClientModeChanged(type: Lwm2mSecurityType): void {
    const config = getDefaultClientSecurityConfig(type, this.lwm2mConfigFormGroup.get('client.endpoint').value);
    switch (type) {
      case Lwm2mSecurityType.PSK:
        config.key = this.lwm2mConfigFormGroup.get('client.key').value;
        break;
      case Lwm2mSecurityType.RPK:
        config.key = this.lwm2mConfigFormGroup.get('client.key').value;
        break;
    }
    this.lwm2mConfigFormGroup.get('client').reset(config, {emitEvent: false});
    this.securityConfigClientUpdateValidators(type);
  }

  private securityConfigClientUpdateValidators = (mode: Lwm2mSecurityType): void => {
    switch (mode) {
      case Lwm2mSecurityType.NO_SEC:
        this.setValidatorsNoSecX509();
        this.lwm2mConfigFormGroup.get('client.cert').disable({emitEvent: false});
        break;
      case Lwm2mSecurityType.X509:
        this.setValidatorsNoSecX509();
        this.lwm2mConfigFormGroup.get('client.cert').enable({emitEvent: false});
        break;
      case Lwm2mSecurityType.PSK:
        this.setValidatorsPskRpk(mode);
        this.lwm2mConfigFormGroup.get('client.identity').enable({emitEvent: false});
        break;
      case Lwm2mSecurityType.RPK:
        this.setValidatorsPskRpk(mode);
        this.lwm2mConfigFormGroup.get('client.identity').disable({emitEvent: false});
        break;
    }
    this.lwm2mConfigFormGroup.get('client.identity').updateValueAndValidity({emitEvent: false});
    this.lwm2mConfigFormGroup.get('client.key').updateValueAndValidity({emitEvent: false});
  }

  private setValidatorsNoSecX509 = (): void => {
    this.lwm2mConfigFormGroup.get('client.identity').clearValidators();
    this.lwm2mConfigFormGroup.get('client.key').clearValidators();
    this.lwm2mConfigFormGroup.get('client.identity').disable({emitEvent: false});
    this.lwm2mConfigFormGroup.get('client.key').disable({emitEvent: false});
  }

  private setValidatorsPskRpk = (mode: Lwm2mSecurityType): void => {
    if (mode === Lwm2mSecurityType.PSK) {
      this.lwm2mConfigFormGroup.get('client.identity').setValidators([Validators.required]);
    } else {
      this.lwm2mConfigFormGroup.get('client.identity').clearValidators();
    }
    this.lwm2mConfigFormGroup.get('client.key').setValidators([Validators.required]);
    this.lwm2mConfigFormGroup.get('client.key').enable({emitEvent: false});
    this.lwm2mConfigFormGroup.get('client.cert').disable({emitEvent: false});
  }

  private initLwm2mConfigForm = (): UntypedFormGroup => {
    const formGroup =  this.fb.group({
      client: this.fb.group({
        endpoint: ['', Validators.required],
        securityConfigClientMode: [Lwm2mSecurityType.NO_SEC],
        identity: [{value: '', disabled: true}],
        key: [{value: '', disabled: true}],
        cert: [{value: '', disabled: true}]
      }),
      bootstrap: this.fb.group({
        bootstrapServer: [getDefaultServerSecurityConfig()],
        lwm2mServer: [getDefaultServerSecurityConfig()]
      })
    });
    formGroup.get('client.securityConfigClientMode').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((type) => {
      this.securityConfigClientModeChanged(type);
    });
    formGroup.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value) => {
      this.propagateChange(JSON.stringify(value));
    });
    return formGroup;
  }
}
