///
/// Copyright © 2016-2023 The Thingsboard Authors
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

import { Component, forwardRef, Input } from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import { MatChipInputEvent } from '@angular/material/chips';
import { COMMA, ENTER, SEMICOLON } from '@angular/cdk/keycodes';
import { FloatLabelType, MatFormFieldAppearance } from '@angular/material/form-field';
import { coerceBoolean } from '@shared/decorators/coerce-boolean';

@Component({
  selector: 'tb-string-items-list',
  templateUrl: './string-items-list.component.html',
  styleUrls: [],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => StringItemsListComponent),
      multi: true
    }
  ]
})
export class StringItemsListComponent implements ControlValueAccessor{

  stringItemsForm: FormGroup;
  private modelValue: Array<string> | null;

  readonly separatorKeysCodes: number[] = [ENTER, COMMA, SEMICOLON];

  private requiredValue: boolean;
  get required(): boolean {
    return this.requiredValue;
  }
  @Input()
  @coerceBoolean()
  set required(value: boolean) {
    if (this.requiredValue !== value) {
      this.requiredValue = value;
      this.updateValidators();
    }
  }

  @Input()
  @coerceBoolean()
  disabled: boolean;

  @Input()
  label: string;

  @Input()
  placeholder: string;

  @Input()
  hint: string;

  @Input()
  requiredText: string;

  @Input()
  floatLabel: FloatLabelType = 'auto';

  @Input()
  appearance: MatFormFieldAppearance = 'fill';

  @Input()
  @coerceBoolean()
  editable: boolean = false

  private propagateChange = (v: any) => { };

  constructor(private fb: FormBuilder) {
    this.stringItemsForm = this.fb.group({
      items: [null, this.required ? [Validators.required] : []]
    });
  }

  updateValidators() {
    this.stringItemsForm.get('items').setValidators(this.required ? [Validators.required] : []);
    this.stringItemsForm.get('items').updateValueAndValidity();
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.stringItemsForm.disable({emitEvent: false});
    } else {
      this.stringItemsForm.enable({emitEvent: false});
    }
  }

  writeValue(value: Array<string> | null): void {
    if (value != null && value.length > 0) {
      this.modelValue = [...value];
      this.stringItemsForm.get('items').setValue(value);
    } else {
      this.stringItemsForm.get('items').setValue(null);
      this.modelValue = null;
    }
  }

  addItem(event: MatChipInputEvent): void {
    let item = event.value || '';
    const input = event.chipInput.inputElement;
    item = item.trim();
    if (item) {
      if (!this.modelValue || this.modelValue.indexOf(item) === -1) {
        if (!this.modelValue) {
          this.modelValue = [];
        }
        this.modelValue.push(item);
        this.stringItemsForm.get('items').setValue(this.modelValue);
      }
      this.propagateChange(this.modelValue);
      if (input) {
        input.value = '';
      }
    }
  }

  removeItems(item: string) {
    const index = this.modelValue.indexOf(item);
    if (index >= 0) {
      this.modelValue.splice(index, 1);
      if (!this.modelValue.length) {
        this.modelValue = null;
      }
      this.stringItemsForm.get('items').setValue(this.modelValue);
      this.propagateChange(this.modelValue);
    }
  }

  get stringItemsList(): string[] {
    return this.stringItemsForm.get('items').value;
  }

}
