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

import { Component, forwardRef, Input, OnDestroy, OnInit } from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  Validator
} from '@angular/forms';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs';

// @deprecated
@Component({
  selector: 'tb-css',
  templateUrl: './css.component.html',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => CssComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => CssComponent),
      multi: true,
    }
  ]
})
export class CssComponent implements OnInit, OnDestroy, ControlValueAccessor, Validator {

  @Input() label: string;

  @Input() disabled: boolean;

  @Input() fillHeight: boolean;

  private requiredValue: boolean;
  get required(): boolean {
    return this.requiredValue;
  }
  @Input()
  set required(value: boolean) {
    this.requiredValue = coerceBooleanProperty(value);
  }

  private propagateChange = null;
  private destroy$ = new Subject();

  cssValueFormGroup: FormGroup;

  constructor(private fb: FormBuilder) {
  }

  ngOnInit() {
    this.cssValueFormGroup = this.fb.group({
      cssValue: [null]
    });
    this.cssValueFormGroup.get('cssValue').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.updateModel();
    });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.cssValueFormGroup.disable({emitEvent: false});
    } else {
      this.cssValueFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: string): void {
    this.cssValueFormGroup.patchValue({ cssValue: value }, {emitEvent: false});
  }

  validate() {
    return this.cssValueFormGroup.get('cssValue').valid ? null : {
      css: false
    };
  }

  private updateModel() {
    const cssValue: string = this.cssValueFormGroup.get('cssValue').value;
    this.propagateChange(cssValue);
  }
}
