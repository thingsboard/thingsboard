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
  Validator,
  Validators
} from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { DefaultDeviceProfileTransportConfiguration, DeviceTransportType } from '@shared/models/device.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
    selector: 'tb-default-device-profile-transport-configuration',
    templateUrl: './default-device-profile-transport-configuration.component.html',
    styleUrls: [],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DefaultDeviceProfileTransportConfigurationComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => DefaultDeviceProfileTransportConfigurationComponent),
            multi: true
        }
    ],
    standalone: false
})
export class DefaultDeviceProfileTransportConfigurationComponent implements ControlValueAccessor, OnInit, Validator {

  defaultDeviceProfileTransportConfigurationFormGroup: UntypedFormGroup;

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
    this.defaultDeviceProfileTransportConfigurationFormGroup = this.fb.group({
      configuration: [null, Validators.required]
    });
    this.defaultDeviceProfileTransportConfigurationFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateModel();
    });
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.defaultDeviceProfileTransportConfigurationFormGroup.disable({emitEvent: false});
    } else {
      this.defaultDeviceProfileTransportConfigurationFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: DefaultDeviceProfileTransportConfiguration | null): void {
    this.defaultDeviceProfileTransportConfigurationFormGroup.patchValue({configuration: value}, {emitEvent: false});
  }

  validate(c: UntypedFormControl): ValidationErrors | null {
    return (this.defaultDeviceProfileTransportConfigurationFormGroup.valid) ? null : {
      configuration: {
        valid: false
      }
    }
  }

  private updateModel() {
    const configuration = this.defaultDeviceProfileTransportConfigurationFormGroup.getRawValue().configuration;
    configuration.type = DeviceTransportType.DEFAULT;
    this.propagateChange(configuration);
  }
}
