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

import { Component, forwardRef, Input, OnInit } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { DialogService } from '@core/services/dialog.service';

@Component({
  selector: 'tb-color-input',
  templateUrl: './color-input.component.html',
  styleUrls: ['./color-input.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ColorInputComponent),
      multi: true
    }
  ]
})
export class ColorInputComponent extends PageComponent implements OnInit, ControlValueAccessor {

  @Input()
  icon: string;

  @Input()
  label: string;

  @Input()
  requiredText: string;

  private colorClearButtonValue: boolean;
  get colorClearButton(): boolean {
    return this.colorClearButtonValue;
  }
  @Input()
  set colorClearButton(value: boolean) {
    const newVal = coerceBooleanProperty(value);
    if (this.colorClearButtonValue !== newVal) {
      this.colorClearButtonValue = newVal;
    }
  }

  private openOnInputValue: boolean;
  get openOnInput(): boolean {
    return this.openOnInputValue;
  }
  @Input()
  set openOnInput(value: boolean) {
    const newVal = coerceBooleanProperty(value);
    if (this.openOnInputValue !== newVal) {
      this.openOnInputValue = newVal;
    }
  }

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

  private modelValue: string;

  private propagateChange = null;

  public colorFormGroup: FormGroup;

  constructor(protected store: Store<AppState>,
              private dialogs: DialogService,
              private translate: TranslateService,
              private fb: FormBuilder) {
    super(store);
  }

  ngOnInit(): void {
    this.colorFormGroup = this.fb.group({
      color: [null, this.required ? [Validators.required] : []]
    });

    this.colorFormGroup.valueChanges.subscribe(() => {
      this.updateModel();
    });
  }

  updateValidators() {
    if (this.colorFormGroup) {
      this.colorFormGroup.get('color').setValidators(this.required ? [Validators.required] : []);
      this.colorFormGroup.get('color').updateValueAndValidity();
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.colorFormGroup.disable({emitEvent: false});
    } else {
      this.colorFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: string): void {
    this.modelValue = value;
    this.colorFormGroup.patchValue(
      { color: this.modelValue }, {emitEvent: false}
    );
  }

  private updateModel() {
    const color: string = this.colorFormGroup.get('color').value;
    if (this.modelValue !== color) {
      this.modelValue = color;
      this.propagateChange(this.modelValue);
    }
  }

  showColorPicker() {
    this.dialogs.colorPicker(this.colorFormGroup.get('color').value).subscribe(
      (color) => {
        if (color) {
          this.colorFormGroup.patchValue(
            {color}, {emitEvent: true}
          );
        }
      }
    );
  }

  clear() {
    this.colorFormGroup.get('color').patchValue(null, {emitEvent: true});
  }
}
