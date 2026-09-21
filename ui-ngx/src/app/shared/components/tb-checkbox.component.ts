// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, EventEmitter, forwardRef, Input, Output } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

@Component({
    selector: 'tb-checkbox',
    templateUrl: './tb-checkbox.component.html',
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => TbCheckboxComponent),
            multi: true
        }
    ],
    standalone: false
})
export class TbCheckboxComponent implements ControlValueAccessor {

  innerValue: boolean;

  @Input() disabled: boolean;
  @Input() trueValue: any = true;
  @Input() falseValue: any = false;
  @Output() valueChange = new EventEmitter();

  private propagateChange = (_: any) => {};

  onHostChange(ev) {
    this.propagateChange(ev.checked ? this.trueValue : this.falseValue);
  }

  modelChange($event) {
    if ($event) {
      this.innerValue = true;
      this.valueChange.emit(this.trueValue);
    } else {
      this.innerValue = false;
      this.valueChange.emit(this.falseValue);
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  writeValue(obj: any): void {
    if (obj === this.trueValue) {
      this.innerValue = true;
    } else {
      this.innerValue = false;
    }
  }
}
