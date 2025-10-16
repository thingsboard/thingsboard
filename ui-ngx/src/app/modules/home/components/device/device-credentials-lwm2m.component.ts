///
/// Copyright © 2016-2025 The Thingsboard Authors
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

import {Component, forwardRef, Input, OnDestroy} from '@angular/core';
import {
  ControlValueAccessor,
  UntypedFormBuilder,
  UntypedFormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import {
  getDefaultClientSecurityConfig,
  getDefaultServerSecurityConfig, Lwm2mClientKeyTooltipTranslationsMap,
  Lwm2mSecurityConfigModels,
  Lwm2mSecurityType,
  Lwm2mSecurityTypeTranslationMap
} from '@shared/models/lwm2m-security-config.models';
import {Subject, throwError, timeout, catchError, of} from 'rxjs';
import {map, takeUntil} from 'rxjs/operators';
import { isDefinedAndNotNull } from '@core/utils';
import { HttpClient } from '@angular/common/http';
import {DeviceId} from "@shared/models/id/device-id";
import {Observable} from "rxjs/internal/Observable";

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

  constructor(private fb: UntypedFormBuilder,
              private http: HttpClient) {
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
   * requestBody =  "{\"method\": \"Execute\", \"params\": {\"id\": \"/1_1.2/0/8\"}}
   * - "Bootstrap-Request Trigger"
   * requestBody =  "{\"method\": \"Execute\", \"params\": {\"id\": \"/1_1.2/0/9\"}}
   */
  public rebootDevice(isBootstrapServer: boolean): void {
    const urlApi = `/api/plugins/rpc/twoway/${this.deviceId.id}`;
    // DiscoveryAll}
    this.http.post(urlApi, { method: "DiscoverAll" })
      .pipe(
        timeout(10000), // 10 sec
        catchError(err => {
          console.error('DiscoverAll timeout or error', err);
          return throwError(() => err);
        })
      )
      .subscribe({
        next: (response: any) => {
          console.log('success: Discovery');
          console.log(response);
          // result = 'CONTENT'
          if (response.result && response.result.toUpperCase() === 'CONTENT') {
            const verId = this.getVerId(response.value);
            console.log("ObjectId=1 ver:", verId);
            const resourceId = isBootstrapServer ? 9 : 8;
            const resourcePath = `/1_${verId}/0/${resourceId}`;

            // first rebootTrigger
            this.rebootTrigger(resourcePath, urlApi).subscribe(first => {
              if (first.result === 'CHANGED') {
                console.log('Reboot success first');
              }
              else if (first.result === 'BAD_REQUEST' && first.newVersionId && first.newVersionId !== verId) {
                // Retry with new version
                const correctedPath = `/1_${first.newVersionId}/0/${resourceId}`;
                console.log(`Retrying with version ${first.newVersionId}`);

                this.rebootTrigger(correctedPath, urlApi).subscribe(second => {
                  if (second.result === 'CHANGED') {
                    console.log('Success reboot after retry');
                  } else {
                    console.error(`error1: Reboot second failed: ${second.toString()}`);
                  }
                });
              } else {
                console.error(`error2: Reboot first failed: ${first.toString()}`);
              }
            });
          }

          else {
            console.error(`error3: Bad registration device with id = ${this.deviceId.id} ❗ RPC result is not CONTENT`);
          }
        },
        error: (e) => {
          console.error(`error4: Bad registration device with id = ${this.deviceId.id} ${e.toString()}`);
          return throwError(() => e);
        },
        complete: () => {
          console.log('Discovery stream complete'); }
      });
  }

  private getVerId(value: string): string {
    const verDef = '1.1';
    try {
      const arr = JSON.parse(value);
      if (!Array.isArray(arr)) return verDef;
      const obj1 = arr.find((s: string) => s.startsWith('</1'));
      const match = obj1?.match(/ver="?([\d.]+)"?/);
      return match ? match[1] : verDef;
    } catch {
      return verDef;
    }
  }

  private rebootTrigger(resourcePath: string, urlApi: string): Observable<{ result: string; newVersionId?: string | null }> {
    console.log(`Sending reboot command to ${resourcePath}`);

    return this.http.post<any>(urlApi, {
      method: 'Execute',
      params: { id: resourcePath }
    }).pipe(
      timeout(10000),
      map(res => {
        console.log(`Reboot for ${resourcePath}`);
        console.log(res);
        if (res?.result?.toUpperCase() === 'CHANGED') {
          return { result: 'CHANGED' };
        }

        if (res?.result?.toUpperCase() === 'BAD_REQUEST' && res?.error) {
          const match = (res.error as string).match(/version[:=]\s*([\d.]+)/i);
          const newVersionId = match ? match[1] : null;
          console.warn(`BAD_REQUEST: suggested version ${newVersionId ?? 'unknown'}`);
          return { result: 'BAD_REQUEST', newVersionId };
        }

        return { result: 'ERROR' };
      }),
      catchError(err => {
        console.error(`Execute error5 for ${resourcePath}:`, err);
        return of({ result: 'ERROR' });
      })
    );
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
