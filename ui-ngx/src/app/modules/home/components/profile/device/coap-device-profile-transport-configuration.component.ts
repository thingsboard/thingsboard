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

import { Component, forwardRef, Input, OnInit } from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  FormControl,
  FormGroup,
  NG_VALUE_ACCESSOR,
  ValidatorFn,
  Validators
} from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import {
  CoapDeviceProfileTransportConfiguration, coapDeviceTypeTranslationMap,
  CoapTransportDeviceType,
  DeviceProfileTransportConfiguration,
  DeviceTransportType,
} from '@shared/models/device.models';
import { isDefinedAndNotNull } from '@core/utils';

@Component({
  selector: 'tb-coap-device-profile-transport-configuration',
  templateUrl: './coap-device-profile-transport-configuration.component.html',
  styleUrls: ['./coap-device-profile-transport-configuration.component.scss'],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => CoapDeviceProfileTransportConfigurationComponent),
    multi: true
  }]
})
export class CoapDeviceProfileTransportConfigurationComponent implements ControlValueAccessor, OnInit {

  coapTransportDeviceTypes = Object.keys(CoapTransportDeviceType);

  coapTransportDeviceTypeTranslations = coapDeviceTypeTranslationMap;

  coapDeviceProfileTransportConfigurationFormGroup: FormGroup;

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
    console.log("init coap");
    this.coapDeviceProfileTransportConfigurationFormGroup = this.fb.group({
      coapDeviceTypeConfiguration: this.fb.group({
          coapDeviceType: [CoapTransportDeviceType.DEFAULT, Validators.required],
        })
      }
    );
    this.coapDeviceProfileTransportConfigurationFormGroup.valueChanges.subscribe(() => {
      this.updateModel();
    });
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.coapDeviceProfileTransportConfigurationFormGroup.disable({emitEvent: false});
    } else {
      this.coapDeviceProfileTransportConfigurationFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: CoapDeviceProfileTransportConfiguration | null): void {
    console.log("[writeValue] CoapDeviceProfileTransportConfiguration: ", value);
    if (isDefinedAndNotNull(value)) {
      this.coapDeviceProfileTransportConfigurationFormGroup.patchValue(value, {emitEvent: false});
    }
  }

  private updateModel() {
    let configuration: DeviceProfileTransportConfiguration = null;
    if (this.coapDeviceProfileTransportConfigurationFormGroup.valid) {
      configuration = this.coapDeviceProfileTransportConfigurationFormGroup.value;
      configuration.type = DeviceTransportType.COAP;
    }
    this.propagateChange(configuration);
  }

}
