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

import { Component, DestroyRef, forwardRef, Input, OnInit } from '@angular/core';
import { ControlValueAccessor, UntypedFormBuilder, UntypedFormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import {
  DefaultDeviceConfiguration,
  DeviceConfiguration,
  DeviceProfileType
} from '@shared/models/device.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'tb-default-device-configuration',
  templateUrl: './default-device-configuration.component.html',
  styleUrls: [],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => DefaultDeviceConfigurationComponent),
    multi: true
  }]
})
export class DefaultDeviceConfigurationComponent implements ControlValueAccessor, OnInit {

  defaultDeviceConfigurationFormGroup: UntypedFormGroup;

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
              private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.defaultDeviceConfigurationFormGroup = this.fb.group({
      configuration: [null, Validators.required]
    });
    this.defaultDeviceConfigurationFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateModel();
    });
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.defaultDeviceConfigurationFormGroup.disable({emitEvent: false});
    } else {
      this.defaultDeviceConfigurationFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: DefaultDeviceConfiguration | null): void {
    this.defaultDeviceConfigurationFormGroup.patchValue({configuration: value}, {emitEvent: false});
  }

  private updateModel() {
    let configuration: DeviceConfiguration = null;
    if (this.defaultDeviceConfigurationFormGroup.valid) {
      configuration = this.defaultDeviceConfigurationFormGroup.getRawValue().configuration;
      configuration.type = DeviceProfileType.DEFAULT;
    }
    this.propagateChange(configuration);
  }
}
