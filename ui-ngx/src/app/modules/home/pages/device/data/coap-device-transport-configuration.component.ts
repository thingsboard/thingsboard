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
  CoapDeviceTransportConfiguration,
  DeviceTransportConfiguration,
  DeviceTransportType
} from '@shared/models/device.models';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { isDefinedAndNotNull } from '@core/utils';

@Component({
  selector: 'tb-coap-device-transport-configuration',
  templateUrl: './coap-device-transport-configuration.component.html',
  styleUrls: [],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => CoapDeviceTransportConfigurationComponent),
    multi: true
  }]
})
export class CoapDeviceTransportConfigurationComponent implements ControlValueAccessor, OnInit, OnDestroy {

  coapDeviceTransportForm: UntypedFormGroup;

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
    this.coapDeviceTransportForm = this.fb.group({
      powerMode: [null],
      edrxCycle: [{disabled: true, value: 0}, Validators.required],
      psmActivityTimer: [{disabled: true, value: 0}, Validators.required],
      pagingTransmissionWindow: [{disabled: true, value: 0}, Validators.required]
    });
    this.coapDeviceTransportForm.valueChanges.pipe(
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
      this.coapDeviceTransportForm.disable({emitEvent: false});
    } else {
      this.coapDeviceTransportForm.enable({emitEvent: false});
      this.coapDeviceTransportForm.get('powerMode').updateValueAndValidity({onlySelf: true});
    }
  }

  writeValue(value: CoapDeviceTransportConfiguration | null): void {
    if (isDefinedAndNotNull(value)) {
      this.coapDeviceTransportForm.patchValue(value, {emitEvent: false});
    } else {
      this.coapDeviceTransportForm.get('powerMode').patchValue(null, {emitEvent: false});
    }
    if (!this.disabled) {
      this.coapDeviceTransportForm.get('powerMode').updateValueAndValidity({onlySelf: true});
    }
  }

  private updateModel() {
    let configuration: DeviceTransportConfiguration = null;
    if (this.coapDeviceTransportForm.valid) {
      configuration = this.coapDeviceTransportForm.value;
      configuration.type = DeviceTransportType.COAP;
    }
    this.propagateChange(configuration);
  }
}
