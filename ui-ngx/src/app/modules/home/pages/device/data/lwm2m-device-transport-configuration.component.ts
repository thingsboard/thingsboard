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

import { Component, forwardRef, Input, OnDestroy, OnInit } from '@angular/core';
import { ControlValueAccessor, UntypedFormBuilder, UntypedFormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import {
  DeviceTransportConfiguration,
  DeviceTransportType,
  Lwm2mDeviceTransportConfiguration
} from '@shared/models/device.models';
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
        }],
    standalone: false
})
export class Lwm2mDeviceTransportConfigurationComponent implements ControlValueAccessor, OnInit, OnDestroy {

  lwm2mDeviceTransportConfigurationFormGroup: UntypedFormGroup;

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

  private destroy$ = new Subject<void>();
  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              private fb: UntypedFormBuilder) {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.lwm2mDeviceTransportConfigurationFormGroup = this.fb.group({
      powerMode: [null],
      edrxCycle: [{disabled: true, value: 0}, Validators.required],
      psmActivityTimer: [{disabled: true, value: 0}, Validators.required],
      pagingTransmissionWindow: [{disabled: true, value: 0}, Validators.required]
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
      this.lwm2mDeviceTransportConfigurationFormGroup.get('powerMode').updateValueAndValidity({onlySelf: true});
    }
  }

  writeValue(value: Lwm2mDeviceTransportConfiguration | null): void {
    if (isDefinedAndNotNull(value)) {
      this.lwm2mDeviceTransportConfigurationFormGroup.patchValue(value, {emitEvent: false});
    } else {
      this.lwm2mDeviceTransportConfigurationFormGroup.get('powerMode').patchValue(null, {emitEvent: false});
    }
    if (!this.disabled) {
      this.lwm2mDeviceTransportConfigurationFormGroup.get('powerMode').updateValueAndValidity({onlySelf: true});
    }
  }

  private updateModel() {
    let configuration: DeviceTransportConfiguration = null;
    if (this.lwm2mDeviceTransportConfigurationFormGroup.valid) {
      configuration = this.lwm2mDeviceTransportConfigurationFormGroup.value;
      configuration.type = DeviceTransportType.LWM2M;
    }
    this.propagateChange(configuration);
  }
}
