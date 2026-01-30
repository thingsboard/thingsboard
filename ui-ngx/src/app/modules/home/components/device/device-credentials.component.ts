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
  FormBuilder,
  FormControl,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  Validator,
  Validators
} from '@angular/forms';
import {
  credentialTypeNames,
  credentialTypesByTransportType,
  DeviceCredentials,
  DeviceCredentialsType,
  DeviceTransportType
} from '@shared/models/device.models';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { generateSecret, isDefinedAndNotNull } from '@core/utils';
import { coerceBoolean } from '@shared/decorators/coercion';
import { DeviceId } from "@shared/models/id/device-id";

@Component({
    selector: 'tb-device-credentials',
    templateUrl: './device-credentials.component.html',
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DeviceCredentialsComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => DeviceCredentialsComponent),
            multi: true,
        }
    ],
    styleUrls: ['./device-credentials.component.scss'],
    standalone: false
})
export class DeviceCredentialsComponent implements ControlValueAccessor, OnInit, Validator, OnDestroy {

  @Input()
  disabled: boolean;

  private deviceTransportTypeValue = DeviceTransportType.DEFAULT;
  get deviceTransportType(): DeviceTransportType {
    return this.deviceTransportTypeValue;
  }
  @Input()
  set deviceTransportType(type: DeviceTransportType) {
    if (type) {
      this.deviceTransportTypeValue = type;
      this.credentialsTypes = credentialTypesByTransportType.get(type);
      const currentType = this.deviceCredentialsFormGroup.get('credentialsType').value;
      if (!this.credentialsTypes.includes(currentType)) {
        this.deviceCredentialsFormGroup.get('credentialsType').patchValue(this.credentialsTypes[0], {onlySelf: true});
      }
    }
  }

  @Input()
  @coerceBoolean()
  initAccessToken = false;

  private destroy$ = new Subject<void>();

  deviceCredentialsFormGroup: FormGroup;

  deviceCredentialsType = DeviceCredentialsType;

  credentialsTypes = credentialTypesByTransportType.get(DeviceTransportType.DEFAULT);

  credentialTypeNamesMap = credentialTypeNames;

  deviceId: DeviceId;

  private propagateChange = null;
  private propagateChangePending = false;

  constructor(public fb: FormBuilder) {
    this.deviceCredentialsFormGroup = this.fb.group({
      credentialsType: [DeviceCredentialsType.ACCESS_TOKEN],
      credentialsId: [null],
      credentialsValue: [null]
    });
    this.deviceCredentialsFormGroup.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.updateView();
    });
    this.deviceCredentialsFormGroup.get('credentialsType').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value) => {
      this.credentialsTypeChanged(value);
    });
  }

  ngOnInit(): void {
    if (this.disabled) {
      this.deviceCredentialsFormGroup.disable({emitEvent: false});
    }
    if (this.initAccessToken && !this.deviceCredentialsFormGroup.get('credentialsId').value &&
      this.deviceCredentialsFormGroup.get('credentialsType').value === DeviceCredentialsType.ACCESS_TOKEN) {
      this.deviceCredentialsFormGroup.get('credentialsId').patchValue(generateSecret(20));
    }
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  writeValue(value: DeviceCredentials | null): void {
    if (isDefinedAndNotNull(value)) {
      this.deviceId = value.deviceId;
      const credentialsType = this.credentialsTypes.includes(value.credentialsType) ? value.credentialsType : this.credentialsTypes[0];
      this.deviceCredentialsFormGroup.patchValue({
        credentialsType,
        credentialsId: value.credentialsId,
        credentialsValue: value.credentialsValue
      }, {emitEvent: false});
      this.updateValidators();
    }
  }

  updateView() {
    const deviceCredentialsValue = this.deviceCredentialsFormGroup.value;
    if (this.propagateChange) {
      this.propagateChange(deviceCredentialsValue);
    } else {
      this.propagateChangePending = true;
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
    if (this.propagateChangePending) {
      this.propagateChangePending = false;
      setTimeout(() => {
        this.updateView();
      }, 0);
    }
  }

  registerOnTouched(fn: any): void {}

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.deviceCredentialsFormGroup.disable({emitEvent: false});
    } else {
      this.deviceCredentialsFormGroup.enable({emitEvent: false});
      this.updateValidators();
    }
  }

  public validate(c: FormControl) {
    return this.deviceCredentialsFormGroup.valid ? null : {
      deviceCredentials: {
        valid: false,
      },
    };
  }

  credentialsTypeChanged(type: DeviceCredentialsType): void {
    this.deviceCredentialsFormGroup.patchValue({
      credentialsId: null,
      credentialsValue: null
    });
    this.updateValidators();
    if (type === DeviceCredentialsType.ACCESS_TOKEN && this.initAccessToken) {
      this.deviceCredentialsFormGroup.get('credentialsId').patchValue(generateSecret(20));
    }
  }

  updateValidators(): void {
    const credentialsType = this.deviceCredentialsFormGroup.get('credentialsType').value as DeviceCredentialsType;
    switch (credentialsType) {
      case DeviceCredentialsType.ACCESS_TOKEN:
        this.deviceCredentialsFormGroup.get('credentialsId').setValidators([Validators.required, Validators.pattern(/^.{1,32}$/)]);
        this.deviceCredentialsFormGroup.get('credentialsId').updateValueAndValidity({emitEvent: false});
        this.deviceCredentialsFormGroup.get('credentialsValue').setValidators([]);
        this.deviceCredentialsFormGroup.get('credentialsValue').updateValueAndValidity({emitEvent: false});
        break;
      default:
        this.deviceCredentialsFormGroup.get('credentialsValue').setValidators([Validators.required]);
        this.deviceCredentialsFormGroup.get('credentialsValue').updateValueAndValidity({emitEvent: false});
        this.deviceCredentialsFormGroup.get('credentialsId').setValidators([]);
        this.deviceCredentialsFormGroup.get('credentialsId').updateValueAndValidity({emitEvent: false});
        break;
    }
  }

  public generate(formControlName: string) {
    this.deviceCredentialsFormGroup.get(formControlName).patchValue(generateSecret(20));
  }
}
