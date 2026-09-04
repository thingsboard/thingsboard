// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, forwardRef, Input, OnDestroy, OnInit } from '@angular/core';
import { ControlValueAccessor, UntypedFormBuilder, UntypedFormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { DeviceProfileConfiguration, DeviceProfileType } from '@shared/models/device.models';
import { deepClone } from '@core/utils';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

@Component({
    selector: 'tb-device-profile-configuration',
    templateUrl: './device-profile-configuration.component.html',
    styleUrls: [],
    providers: [{
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DeviceProfileConfigurationComponent),
            multi: true
        }],
    standalone: false
})
export class DeviceProfileConfigurationComponent implements ControlValueAccessor, OnInit, OnDestroy {

  deviceProfileType = DeviceProfileType;

  deviceProfileConfigurationFormGroup: UntypedFormGroup;

  private destroy$ = new Subject<void>();

  @Input()
  disabled: boolean;

  type: DeviceProfileType;

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
    this.deviceProfileConfigurationFormGroup = this.fb.group({
      configuration: [null, Validators.required]
    });
    this.deviceProfileConfigurationFormGroup.valueChanges.pipe(
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
      this.deviceProfileConfigurationFormGroup.disable({emitEvent: false});
    } else {
      this.deviceProfileConfigurationFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: DeviceProfileConfiguration | null): void {
    this.type = value?.type;
    const configuration = deepClone(value);
    if (configuration) {
      delete configuration.type;
    }
    this.deviceProfileConfigurationFormGroup.patchValue({configuration}, {emitEvent: false});
  }

  private updateModel() {
    let configuration: DeviceProfileConfiguration = null;
    if (this.deviceProfileConfigurationFormGroup.valid) {
      configuration = this.deviceProfileConfigurationFormGroup.getRawValue().configuration;
      configuration.type = this.type;
    }
    this.propagateChange(configuration);
  }
}
