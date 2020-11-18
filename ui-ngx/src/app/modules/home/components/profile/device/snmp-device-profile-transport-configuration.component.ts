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

import {Component, forwardRef, Input, OnInit} from '@angular/core';
import {ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR, Validators} from '@angular/forms';
import {Store} from '@ngrx/store';
import {AppState} from '@app/core/core.state';
import {coerceBooleanProperty} from '@angular/cdk/coercion';
import {
  DeviceProfileTransportConfiguration,
  DeviceTransportType,
  SnmpDeviceProfileTransportConfiguration
} from '@shared/models/device.models';
import {isDefinedAndNotNull} from "@core/utils";

export interface OidMappingConfiguration {
  isAttribute: boolean;
  key: string;
  type: string;
  method: string;
  oid: string;
}

@Component({
  selector: 'tb-snmp-device-profile-transport-configuration',
  templateUrl: './snmp-device-profile-transport-configuration.component.html',
  styleUrls: [],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => SnmpDeviceProfileTransportConfigurationComponent),
    multi: true
  }]
})
export class SnmpDeviceProfileTransportConfigurationComponent implements ControlValueAccessor, OnInit {
  snmpDeviceProfileTransportConfigurationFormGroup: FormGroup;
  private requiredValue: boolean;
  private configuration = [];

  get required(): boolean {
    return this.requiredValue;
  }

  @Input()
  set required(value: boolean) {
    this.requiredValue = coerceBooleanProperty(value);
  }

  @Input()
  disabled: boolean;

  private propagateChange = (v: any) => {
  }

  constructor(private store: Store<AppState>, private fb: FormBuilder) {
  }

  ngOnInit(): void {
    this.snmpDeviceProfileTransportConfigurationFormGroup = this.fb.group({
      configuration: [null, Validators.required]
    });
    this.snmpDeviceProfileTransportConfigurationFormGroup.valueChanges.subscribe(() => {
      this.updateModel();
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  writeValue(value: SnmpDeviceProfileTransportConfiguration | null): void {
    if (isDefinedAndNotNull(value)) {
      this.snmpDeviceProfileTransportConfigurationFormGroup.patchValue({configuration: value}, {emitEvent: false});
    }
  }

  private updateModel() {
    let configuration: DeviceProfileTransportConfiguration = null;
    if (this.snmpDeviceProfileTransportConfigurationFormGroup.valid) {
      configuration = this.snmpDeviceProfileTransportConfigurationFormGroup.getRawValue().configuration;
      configuration.type = DeviceTransportType.SNMP;
    }
    this.propagateChange(configuration);
  }
}
