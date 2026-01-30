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
import { ControlValueAccessor, NG_VALUE_ACCESSOR, UntypedFormControl } from '@angular/forms';
import { cssUnit, cssUnits } from '@shared/models/widget-settings.models';
import { coerceBoolean } from '@shared/decorators/coercion';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
    selector: 'tb-css-unit-select',
    templateUrl: './css-unit-select.component.html',
    styleUrls: [],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => CssUnitSelectComponent),
            multi: true
        }
    ],
    standalone: false
})
export class CssUnitSelectComponent implements OnInit, ControlValueAccessor {

  @Input()
  disabled: boolean;

  @Input()
  @coerceBoolean()
  allowEmpty = false;

  @Input()
  width = '100%';

  cssUnitsList = cssUnits;

  cssUnitFormControl: UntypedFormControl;

  modelValue: cssUnit;

  private propagateChange = null;

  constructor(private destroyRef: DestroyRef) {}

  ngOnInit(): void {
    this.cssUnitFormControl = new UntypedFormControl();
    this.cssUnitFormControl.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((value: cssUnit) => {
      this.updateModel(value);
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.cssUnitFormControl.disable({emitEvent: false});
    } else {
      this.cssUnitFormControl.enable({emitEvent: false});
    }
  }

  writeValue(value: cssUnit): void {
    this.modelValue = value;
    this.cssUnitFormControl.patchValue(this.modelValue, {emitEvent: false});
  }

  updateModel(value: cssUnit): void {
    if (this.modelValue !== value) {
      this.modelValue = value;
      this.propagateChange(this.modelValue);
    }
  }
}
