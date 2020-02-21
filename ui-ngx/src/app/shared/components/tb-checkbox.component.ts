///
/// Copyright © 2016-2020 The Thingsboard Authors
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
  ]
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
