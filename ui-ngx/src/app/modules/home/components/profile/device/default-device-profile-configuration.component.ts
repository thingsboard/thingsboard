// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, DestroyRef, forwardRef, Input, OnInit } from '@angular/core';
import { ControlValueAccessor, UntypedFormBuilder, UntypedFormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import {
  DefaultDeviceProfileConfiguration,
  DeviceProfileConfiguration,
  DeviceProfileType
} from '@shared/models/device.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
    selector: 'tb-default-device-profile-configuration',
    templateUrl: './default-device-profile-configuration.component.html',
    styleUrls: [],
    providers: [{
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DefaultDeviceProfileConfigurationComponent),
            multi: true
        }],
    standalone: false
})
export class DefaultDeviceProfileConfigurationComponent implements ControlValueAccessor, OnInit {

  defaultDeviceProfileConfigurationFormGroup: UntypedFormGroup;

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
    this.defaultDeviceProfileConfigurationFormGroup = this.fb.group({
      configuration: [null, Validators.required]
    });
    this.defaultDeviceProfileConfigurationFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateModel();
    });
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.defaultDeviceProfileConfigurationFormGroup.disable({emitEvent: false});
    } else {
      this.defaultDeviceProfileConfigurationFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: DefaultDeviceProfileConfiguration | null): void {
    this.defaultDeviceProfileConfigurationFormGroup.patchValue({configuration: value}, {emitEvent: false});
  }

  private updateModel() {
    let configuration: DeviceProfileConfiguration = null;
    if (this.defaultDeviceProfileConfigurationFormGroup.valid) {
      configuration = this.defaultDeviceProfileConfigurationFormGroup.getRawValue().configuration;
      configuration.type = DeviceProfileType.DEFAULT;
    }
    this.propagateChange(configuration);
  }
}
