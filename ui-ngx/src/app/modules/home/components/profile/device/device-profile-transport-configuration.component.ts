// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, DestroyRef, forwardRef, Input, OnInit } from '@angular/core';
import {
  ControlValueAccessor,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  ValidationErrors,
  Validator
} from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { DeviceProfileTransportConfiguration, DeviceTransportType } from '@shared/models/device.models';
import { deepClone } from '@core/utils';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
    selector: 'tb-device-profile-transport-configuration',
    templateUrl: './device-profile-transport-configuration.component.html',
    styleUrls: [],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DeviceProfileTransportConfigurationComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => DeviceProfileTransportConfigurationComponent),
            multi: true,
        }
    ],
    standalone: false
})
export class DeviceProfileTransportConfigurationComponent implements ControlValueAccessor, OnInit, Validator {

  deviceTransportType = DeviceTransportType;

  deviceProfileTransportConfigurationFormGroup: UntypedFormGroup;

  @Input()
  disabled: boolean;

  @Input()
  isAdd: boolean;

  transportType: DeviceTransportType;

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
    this.deviceProfileTransportConfigurationFormGroup = this.fb.group({
      configuration: [null]
    });
    this.deviceProfileTransportConfigurationFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateModel();
    });
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.deviceProfileTransportConfigurationFormGroup.disable({emitEvent: false});
    } else {
      this.deviceProfileTransportConfigurationFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: DeviceProfileTransportConfiguration | null): void {
    this.transportType = value?.type;
    const configuration = deepClone(value);
    if (configuration) {
      delete configuration.type;
    }
    setTimeout(() => {
      this.deviceProfileTransportConfigurationFormGroup.patchValue({configuration}, {emitEvent: this.isAdd});
    }, 0);
  }

  private updateModel() {
    const configuration = this.deviceProfileTransportConfigurationFormGroup.getRawValue().configuration;
    configuration.type = this.transportType;
    this.propagateChange(configuration);
  }

  public validate(c: UntypedFormControl): ValidationErrors | null {
    return (this.deviceProfileTransportConfigurationFormGroup.valid) ? null : {
      configuration: {
        valid: false,
      },
    };
  }
}
