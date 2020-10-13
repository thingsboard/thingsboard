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

import { Component, forwardRef, Input, OnInit } from "@angular/core";
import {
  ControlValueAccessor,
  FormBuilder,
  FormControl,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators
} from "@angular/forms";
import { coerceBooleanProperty } from "@angular/cdk/coercion";
import {
  DeviceProvisionConfiguration,
  DeviceProvisionType,
  deviceProvisionTypeTranslationMap
} from "@shared/models/device.models";
import { generateSecret, isDefinedAndNotNull } from '@core/utils';

@Component({
  selector: 'tb-device-profile-provision-configuration',
  templateUrl: './device-profile-provision-configuration.component.html',
  styleUrls: [],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => DeviceProfileProvisionConfigurationComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => DeviceProfileProvisionConfigurationComponent),
      multi: true,
    }
  ]
})
export class DeviceProfileProvisionConfigurationComponent implements ControlValueAccessor, OnInit, Validator {

  provisionConfigurationFormGroup: FormGroup;

  deviceProvisionType = DeviceProvisionType;
  deviceProvisionTypes = Object.keys(DeviceProvisionType);
  deviceProvisionTypeTranslateMap = deviceProvisionTypeTranslationMap;

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

  constructor(private fb: FormBuilder) {
  }

  ngOnInit(): void {
    this.provisionConfigurationFormGroup = this.fb.group({
      type: [DeviceProvisionType.DISABLED, Validators.required],
      provisionDeviceSecret: [{value: null, disabled: true}, Validators.required],
      provisionDeviceKey: [{value: null, disabled: true}, Validators.required]
    });
    this.provisionConfigurationFormGroup.get('type').valueChanges.subscribe((type) => {
      if (type === DeviceProvisionType.DISABLED) {
        this.provisionConfigurationFormGroup.get('provisionDeviceSecret').disable({emitEvent: false});
        this.provisionConfigurationFormGroup.get('provisionDeviceSecret').patchValue(null, {emitEvent: false});
        this.provisionConfigurationFormGroup.get('provisionDeviceKey').disable({emitEvent: false});
        this.provisionConfigurationFormGroup.get('provisionDeviceKey').patchValue(null);
      } else {
        const provisionDeviceSecret: string = this.provisionConfigurationFormGroup.get('provisionDeviceSecret').value;
        if (!provisionDeviceSecret || !provisionDeviceSecret.length) {
          this.provisionConfigurationFormGroup.get('provisionDeviceSecret').patchValue(generateSecret(20), {emitEvent: false});
        }
        const provisionDeviceKey: string = this.provisionConfigurationFormGroup.get('provisionDeviceKey').value;
        if (!provisionDeviceKey || !provisionDeviceKey.length) {
          this.provisionConfigurationFormGroup.get('provisionDeviceKey').patchValue(generateSecret(20), {emitEvent: false});
        }
        this.provisionConfigurationFormGroup.get('provisionDeviceSecret').enable({emitEvent: false});
        this.provisionConfigurationFormGroup.get('provisionDeviceKey').enable({emitEvent: false});
      }
    });
    this.provisionConfigurationFormGroup.valueChanges.subscribe(() => {
      this.updateModel();
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  writeValue(value: DeviceProvisionConfiguration | null): void {
    if (isDefinedAndNotNull(value)){
      this.provisionConfigurationFormGroup.patchValue(value, {emitEvent: false});
    } else {
      this.provisionConfigurationFormGroup.patchValue({type: DeviceProvisionType.DISABLED});
    }
  }

  setDisabledState(isDisabled: boolean){
    this.disabled = isDisabled;
    if (this.disabled){
      this.provisionConfigurationFormGroup.disable();
    } else {
      this.provisionConfigurationFormGroup.enable({emitEvent: false});
    }
  }

  validate(c: FormControl): ValidationErrors | null {
    return (this.provisionConfigurationFormGroup.valid) ? null : {
      provisionConfiguration: {
        valid: false,
      },
    };
  }

  private updateModel(): void {
    let deviceProvisionConfiguration: DeviceProvisionConfiguration = null;
    if (this.provisionConfigurationFormGroup.valid) {
      deviceProvisionConfiguration = this.provisionConfigurationFormGroup.getRawValue();
    }
    this.propagateChange(deviceProvisionConfiguration);
  }
}
