// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
