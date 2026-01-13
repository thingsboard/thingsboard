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
import {
  ControlValueAccessor,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import {
  DeviceProvisionConfiguration,
  DeviceProvisionType,
  deviceProvisionTypeTranslationMap
} from '@shared/models/device.models';
import { generateSecret, isBoolean, isDefinedAndNotNull } from '@core/utils';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

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

  provisionConfigurationFormGroup: UntypedFormGroup;

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

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder,
              private translate: TranslateService,
              private destroyRef: DestroyRef) {
  }

  ngOnInit(): void {
    this.provisionConfigurationFormGroup = this.fb.group({
      type: [DeviceProvisionType.DISABLED, Validators.required],
      provisionDeviceSecret: [{value: null, disabled: true}, Validators.required],
      provisionDeviceKey: [{value: null, disabled: true}, Validators.required],
      certificateValue: [{value: null, disabled: true}, Validators.required],
      certificateRegExPattern: [{value: null, disabled: true}, Validators.required],
      allowCreateNewDevicesByX509Certificate: [{value: null, disabled: true}, Validators.required]
    });
    this.provisionConfigurationFormGroup.get('type').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((type) => {
      if (type === DeviceProvisionType.DISABLED) {
        for (const field in this.provisionConfigurationFormGroup.controls) {
          if (field !== 'type') {
            const control = this.provisionConfigurationFormGroup.get(field);
            control.disable({emitEvent: false});
            control.patchValue(null, {emitEvent: false});
          }
        }
      } else if (type === DeviceProvisionType.X509_CERTIFICATE_CHAIN) {
        const certificateValue: string = this.provisionConfigurationFormGroup.get('certificateValue').value;
        if (!certificateValue || !certificateValue.length) {
          this.provisionConfigurationFormGroup.get('certificateValue').patchValue(null, {emitEvent: false});
        }
        const certificateRegExPattern: string = this.provisionConfigurationFormGroup.get('certificateRegExPattern').value;
        if (!certificateRegExPattern || !certificateRegExPattern.length) {
          this.provisionConfigurationFormGroup.get('certificateRegExPattern').patchValue('(.*)', {emitEvent: false});
        }
        const allowCreateNewDevicesByX509Certificate: boolean | null = this.provisionConfigurationFormGroup.get('allowCreateNewDevicesByX509Certificate').value;
        if (!isBoolean(allowCreateNewDevicesByX509Certificate)) {
          this.provisionConfigurationFormGroup.get('allowCreateNewDevicesByX509Certificate').patchValue(true, {emitEvent: false});
        }
        this.provisionConfigurationFormGroup.get('certificateValue').enable({emitEvent: false});
        this.provisionConfigurationFormGroup.get('certificateRegExPattern').enable({emitEvent: false});
        this.provisionConfigurationFormGroup.get('allowCreateNewDevicesByX509Certificate').enable({emitEvent: false});
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
    this.provisionConfigurationFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateModel();
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  writeValue(value: DeviceProvisionConfiguration | null): void {
    if (isDefinedAndNotNull(value)) {
      if (value.type === DeviceProvisionType.X509_CERTIFICATE_CHAIN) {
        value.certificateValue = value.provisionDeviceSecret;
      }
      this.provisionConfigurationFormGroup.patchValue(value, {emitEvent: false});
    } else {
      this.provisionConfigurationFormGroup.patchValue({type: DeviceProvisionType.DISABLED});
    }
  }

  setDisabledState(isDisabled: boolean) {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.provisionConfigurationFormGroup.disable({emitEvent: false});
    } else {
      if (this.provisionConfigurationFormGroup.get('type').value !== DeviceProvisionType.DISABLED) {
        this.provisionConfigurationFormGroup.enable({emitEvent: false});
      } else {
        this.provisionConfigurationFormGroup.get('type').enable({emitEvent: false});
      }
    }
  }

  validate(c: UntypedFormControl): ValidationErrors | null {
    return (this.provisionConfigurationFormGroup.valid) ? null : {
      provisionConfiguration: {
        valid: false,
      },
    };
  }

  private updateModel(): void {
    let deviceProvisionConfiguration: DeviceProvisionConfiguration = null;
    this.resetFormControls(this.provisionConfigurationFormGroup.value);
    if (this.provisionConfigurationFormGroup.valid) {
      deviceProvisionConfiguration = this.provisionConfigurationFormGroup.getRawValue();
      if (deviceProvisionConfiguration.type === DeviceProvisionType.X509_CERTIFICATE_CHAIN) {
        deviceProvisionConfiguration.provisionDeviceSecret = deviceProvisionConfiguration.certificateValue;
      }
    }
    this.propagateChange(deviceProvisionConfiguration);
  }

  onProvisionCopied(isKey: boolean) {
    this.store.dispatch(new ActionNotificationShow(
      {
        message: this.translate.instant(isKey ? 'device-profile.provision-key-copied-message' : 'device-profile.provision-secret-copied-message'),
        type: 'success',
        duration: 1200,
        verticalPosition: 'bottom',
        horizontalPosition: 'right'
      }));
  }

  private resetFormControls(value: DeviceProvisionConfiguration) {
    if (value.type === DeviceProvisionType.CHECK_PRE_PROVISIONED_DEVICES || value.type === DeviceProvisionType.ALLOW_CREATE_NEW_DEVICES) {
      this.provisionConfigurationFormGroup.get('certificateValue').reset({value: null, disabled: true}, {emitEvent: false});
      this.provisionConfigurationFormGroup.get('certificateRegExPattern').reset({value: null, disabled: true}, {emitEvent: false});
      this.provisionConfigurationFormGroup.get('allowCreateNewDevicesByX509Certificate').reset({value: null, disabled: true}, {emitEvent: false});
    } else if (value.type === DeviceProvisionType.X509_CERTIFICATE_CHAIN) {
      this.provisionConfigurationFormGroup.get('provisionDeviceSecret').reset({value: null, disabled: true}, {emitEvent: false});
      this.provisionConfigurationFormGroup.get('provisionDeviceKey').reset({value: null, disabled: true}, {emitEvent: false});
    }
  }
}
