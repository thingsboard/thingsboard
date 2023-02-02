///
/// Copyright © 2016-2022 The Thingsboard Authors
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
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { MatChipInputEvent } from '@angular/material/chips';
import { COMMA, ENTER, SEMICOLON } from '@angular/cdk/keycodes';

@Component({
  selector: 'tb-alarm-type-list',
  templateUrl: './alarm-type-list.component.html',
  styleUrls: [],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => AlarmTypeListComponent),
      multi: true
    }
  ]
})
export class AlarmTypeListComponent implements ControlValueAccessor{

  alarmTypeForm: FormGroup;
  private modelValue: Array<string> | null;

  readonly separatorKeysCodes: number[] = [ENTER, COMMA, SEMICOLON];

  private requiredValue: boolean;
  get required(): boolean {
    return this.requiredValue;
  }
  @Input()
  set required(value: boolean) {
    const newVal = coerceBooleanProperty(value);
    if (this.requiredValue !== newVal) {
      this.requiredValue = newVal;
      this.updateValidators();
    }
  }

  @Input()
  disabled: boolean;

  private propagateChange = (v: any) => { };

  constructor(private fb: FormBuilder) {
    this.alarmTypeForm = this.fb.group({
      alarmTypes: [null, this.required ? [Validators.required] : []]
    });
  }

  updateValidators() {
    this.alarmTypeForm.get('alarmTypes').setValidators(this.required ? [Validators.required] : []);
    this.alarmTypeForm.get('alarmTypes').updateValueAndValidity();
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.alarmTypeForm.disable({emitEvent: false});
    } else {
      this.alarmTypeForm.enable({emitEvent: false});
    }
  }

  writeValue(value: Array<string> | null): void {
    if (value != null && value.length > 0) {
      this.modelValue = [...value];
      this.alarmTypeForm.get('alarmTypes').setValue(value);
    } else {
      this.alarmTypeForm.get('alarmTypes').setValue(null);
      this.modelValue = null;
    }
  }

  addAlarmType(event: MatChipInputEvent): void {
    let alarmType = event.value || '';
    const input = event.chipInput.inputElement;
    alarmType = alarmType.trim();
    if (alarmType) {
      if (!this.modelValue || this.modelValue.indexOf(alarmType) === -1) {
        if (!this.modelValue) {
          this.modelValue = [];
        }
        this.modelValue.push(alarmType);
        this.alarmTypeForm.get('alarmTypes').setValue(this.modelValue);
      }
      this.propagateChange(this.modelValue);
      if (input) {
        input.value = '';
      }
    }
  }

  removeAlarmType(alarmType: string) {
    const index = this.modelValue.indexOf(alarmType);
    if (index >= 0) {
      this.modelValue.splice(index, 1);
      if (!this.modelValue.length) {
        this.modelValue = null;
      }
      this.alarmTypeForm.get('alarmTypes').setValue(this.modelValue);
      this.propagateChange(this.modelValue);
    }
  }

  get alarmTypeList(): string[] {
    return this.alarmTypeForm.get('alarmTypes').value;
  }

}
