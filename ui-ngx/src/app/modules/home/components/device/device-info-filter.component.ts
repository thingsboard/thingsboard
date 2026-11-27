// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { ChangeDetectionStrategy, Component, DestroyRef, forwardRef, inject } from '@angular/core';
import { ControlValueAccessor, FormBuilder, NG_VALUE_ACCESSOR } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DeviceInfoFilter } from '@shared/models/device.models';

@Component({
    selector: 'tb-device-info-filter',
    templateUrl: './device-info-filter.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: {class: 'contents'},
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DeviceInfoFilterComponent),
            multi: true
        }
    ],
    standalone: false
})
export class DeviceInfoFilterComponent implements ControlValueAccessor {

  private fb = inject(FormBuilder);
  private destroyRef = inject(DestroyRef);

  deviceInfoFilterForm = this.fb.group({
    deviceProfileId: [null as DeviceInfoFilter['deviceProfileId']],
    active: [null as boolean]
  });

  private propagateChange = (_: DeviceInfoFilter) => {};

  constructor() {
    this.deviceInfoFilterForm.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(value => this.propagateChange({
      deviceProfileId: value.deviceProfileId ?? null,
      active: value.active ?? null
    }));
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    if (isDisabled) {
      this.deviceInfoFilterForm.disable({emitEvent: false});
    } else {
      this.deviceInfoFilterForm.enable({emitEvent: false});
    }
  }

  writeValue(deviceInfoFilter?: DeviceInfoFilter): void {
    this.deviceInfoFilterForm.patchValue({
      deviceProfileId: deviceInfoFilter?.deviceProfileId ?? null,
      active: deviceInfoFilter?.active ?? null
    }, {emitEvent: false});
  }
}
