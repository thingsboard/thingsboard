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

import { Component, forwardRef, Input, OnDestroy, OnInit } from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { DeviceTransportConfiguration, Lwm2mDeviceTransportConfiguration } from '@shared/models/device.models';
import { PowerMode, PowerModeTranslationMap } from '@home/components/profile/device/lwm2m/lwm2m-profile-config.models';
import { takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs';
import { isDefinedAndNotNull } from '@core/utils';

@Component({
  selector: 'tb-lwm2m-device-transport-configuration',
  templateUrl: './lwm2m-device-transport-configuration.component.html',
  styleUrls: [],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => Lwm2mDeviceTransportConfigurationComponent),
    multi: true
  }]
})
export class Lwm2mDeviceTransportConfigurationComponent implements ControlValueAccessor, OnInit, OnDestroy {

  lwm2mDeviceTransportConfigurationFormGroup: FormGroup;
  powerMods = Object.values(PowerMode);
  powerModeTranslationMap = PowerModeTranslationMap;

  private requiredValue: boolean;
  get required(): boolean {
    return this.requiredValue;
  }
  @Input()
  set required(value: boolean) {
    this.requiredValue = coerceBooleanProperty(value);
  }

  @Input()
  disabled: boolean;

  private destroy$ = new Subject();
  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              private fb: FormBuilder) {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.lwm2mDeviceTransportConfigurationFormGroup = this.fb.group({
      powerMode: [null],
      edrxCycle: [0]
    });
    this.lwm2mDeviceTransportConfigurationFormGroup.get('powerMode').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((powerMode: PowerMode) => {
      if (powerMode === PowerMode.E_DRX) {
        this.lwm2mDeviceTransportConfigurationFormGroup.get('edrxCycle').enable({emitEvent: false});
        this.lwm2mDeviceTransportConfigurationFormGroup.get('edrxCycle').patchValue(0, {emitEvent: false});
        this.lwm2mDeviceTransportConfigurationFormGroup.get('edrxCycle')
          .setValidators([Validators.required, Validators.min(0), Validators.pattern('[0-9]*')]);
      } else {
        this.lwm2mDeviceTransportConfigurationFormGroup.get('edrxCycle').disable({emitEvent: false});
        this.lwm2mDeviceTransportConfigurationFormGroup.get('edrxCycle').clearValidators();
      }
      this.lwm2mDeviceTransportConfigurationFormGroup.get('edrxCycle').updateValueAndValidity({emitEvent: false});
    });
    this.lwm2mDeviceTransportConfigurationFormGroup.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.updateModel();
    });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.lwm2mDeviceTransportConfigurationFormGroup.disable({emitEvent: false});
    } else {
      this.lwm2mDeviceTransportConfigurationFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: Lwm2mDeviceTransportConfiguration | null): void {
    if (isDefinedAndNotNull(value)) {
      this.lwm2mDeviceTransportConfigurationFormGroup.get('powerMode').patchValue(value.powerMode, {emitEvent: false, onlySelf: true});
      this.lwm2mDeviceTransportConfigurationFormGroup.get('edrxCycle').patchValue(value.edrxCycle || 0, {emitEvent: false});
    } else {
      this.lwm2mDeviceTransportConfigurationFormGroup.patchValue({powerMode: null, edrxCycle: 0}, {emitEvent: false});
    }
  }

  private updateModel() {
    let configuration: DeviceTransportConfiguration = null;
    if (this.lwm2mDeviceTransportConfigurationFormGroup.valid) {
      configuration = this.lwm2mDeviceTransportConfigurationFormGroup.value;
      // configuration.type = DeviceTransportType.LWM2M;
    }
    this.propagateChange(configuration);
  }
}
