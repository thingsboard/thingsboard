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

import { Component, forwardRef, Input, OnInit, ViewChild } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, NgForm } from '@angular/forms';
import { ValueType, valueTypesMap } from '@shared/models/constants';

@Component({
  selector: 'tb-value-input',
  templateUrl: './value-input.component.html',
  styleUrls: ['./value-input.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ValueInputComponent),
      multi: true
    }
  ]
})
export class ValueInputComponent implements OnInit, ControlValueAccessor {

  @Input() disabled: boolean;

  @Input() requiredText: string;

  @ViewChild('inputForm', {static: true}) inputForm: NgForm;

  modelValue: any;

  valueType: ValueType;

  public valueTypeEnum = ValueType;

  valueTypeKeys = Object.keys(ValueType);

  valueTypes = valueTypesMap;

  private propagateChange = null;

  constructor() {

  }

  ngOnInit(): void {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  writeValue(value: any): void {
    this.modelValue = value;
    if (this.modelValue === true || this.modelValue === false) {
      this.valueType = ValueType.BOOLEAN;
    } else if (typeof this.modelValue === 'number') {
      if (this.modelValue.toString().indexOf('.') === -1) {
        this.valueType = ValueType.INTEGER;
      } else {
        this.valueType = ValueType.DOUBLE;
      }
    } else {
      this.valueType = ValueType.STRING;
    }
  }

  updateView() {
    if (this.inputForm.valid || this.valueType === ValueType.BOOLEAN) {
      this.propagateChange(this.modelValue);
    } else {
      this.propagateChange(null);
    }
  }

  onValueTypeChanged() {
    if (this.valueType === ValueType.BOOLEAN) {
      this.modelValue = false;
    } else {
      this.modelValue = null;
    }
    this.updateView();
  }

  onValueChanged() {
    this.updateView();
  }

}
