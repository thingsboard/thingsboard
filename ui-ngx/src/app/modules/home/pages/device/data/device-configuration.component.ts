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

import { Component, DestroyRef, forwardRef, Input, OnInit } from '@angular/core';
import { ControlValueAccessor, UntypedFormBuilder, UntypedFormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { DeviceConfiguration, DeviceProfileType } from '@shared/models/device.models';
import { deepClone } from '@core/utils';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'tb-device-configuration',
  templateUrl: './device-configuration.component.html',
  styleUrls: [],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => DeviceConfigurationComponent),
    multi: true
  }]
})
export class DeviceConfigurationComponent implements ControlValueAccessor, OnInit {

  deviceProfileType = DeviceProfileType;

  deviceConfigurationFormGroup: UntypedFormGroup;

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

  type: DeviceProfileType;

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.deviceConfigurationFormGroup = this.fb.group({
      configuration: [null, Validators.required]
    });
    this.deviceConfigurationFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateModel();
    });
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.deviceConfigurationFormGroup.disable({emitEvent: false});
    } else {
      this.deviceConfigurationFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: DeviceConfiguration | null): void {
    this.type = value?.type;
    const configuration = deepClone(value);
    if (configuration) {
      delete configuration.type;
    }
    this.deviceConfigurationFormGroup.patchValue({configuration}, {emitEvent: false});
  }

  private updateModel() {
    let configuration: DeviceConfiguration = null;
    if (this.deviceConfigurationFormGroup.valid) {
      configuration = this.deviceConfigurationFormGroup.getRawValue().configuration;
      configuration.type = this.type;
    }
    this.propagateChange(configuration);
  }
}
