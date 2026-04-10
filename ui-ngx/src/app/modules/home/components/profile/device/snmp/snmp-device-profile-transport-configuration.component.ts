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
  DeviceTransportType,
  SnmpDeviceProfileTransportConfiguration
} from '@shared/models/device.models';
import { isDefinedAndNotNull } from '@core/utils';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

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
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => SnmpDeviceProfileTransportConfigurationComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => SnmpDeviceProfileTransportConfigurationComponent),
            multi: true
        }
    ],
    standalone: false
})
export class SnmpDeviceProfileTransportConfigurationComponent implements OnInit, OnDestroy, ControlValueAccessor, Validator {

  snmpDeviceProfileTransportConfigurationFormGroup: UntypedFormGroup;

  private destroy$ = new Subject<void>();

  @Input()
  disabled: boolean;

  private propagateChange = (v: any) => {
  }

  constructor(private fb: UntypedFormBuilder) {
  }

  ngOnInit(): void {
    this.snmpDeviceProfileTransportConfigurationFormGroup = this.fb.group({
      timeoutMs: [500, [Validators.required, Validators.min(0), Validators.pattern('[0-9]*')]],
      retries: [0, [Validators.required, Validators.min(0), Validators.pattern('[0-9]*')]],
      communicationConfigs: [null, Validators.required],
    });
    this.snmpDeviceProfileTransportConfigurationFormGroup.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.updateModel();
    });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean) {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.snmpDeviceProfileTransportConfigurationFormGroup.disable({emitEvent: false});
    } else {
      this.snmpDeviceProfileTransportConfigurationFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: SnmpDeviceProfileTransportConfiguration | null): void {
    if (isDefinedAndNotNull(value)) {
      this.snmpDeviceProfileTransportConfigurationFormGroup.patchValue(value, {emitEvent: !value.communicationConfigs});
    }
  }

  private updateModel() {
    const configuration = this.snmpDeviceProfileTransportConfigurationFormGroup.getRawValue();
    configuration.type = DeviceTransportType.SNMP;
    this.propagateChange(configuration);
  }

  validate(): ValidationErrors | null {
    return this.snmpDeviceProfileTransportConfigurationFormGroup.valid ? null : {
      snmpDeviceProfileTransportConfiguration: false
    };
  }
}
