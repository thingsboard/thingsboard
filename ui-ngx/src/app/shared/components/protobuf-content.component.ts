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
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

// @deprecated
@Component({
  selector: 'tb-protobuf-content',
  templateUrl: './protobuf-content.component.html',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ProtobufContentComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => ProtobufContentComponent),
      multi: true
    }
  ]
})
export class ProtobufContentComponent implements OnInit, OnDestroy, ControlValueAccessor, Validator {

  @Input() label: string;

  @Input() disabled: boolean;

  @Input() fillHeight: boolean;

  @Input() editorStyle: {[klass: string]: any};

  private readonlyValue: boolean;
  get readonly(): boolean {
    return this.readonlyValue;
  }
  @Input()
  set readonly(value: boolean) {
    this.readonlyValue = coerceBooleanProperty(value);
  }

  private propagateChange = null;
  private destroy$ = new Subject();

  protobufValueFormGroup: FormGroup;

  constructor(private fb: FormBuilder) {
  }

  ngOnInit() {
    this.protobufValueFormGroup = this.fb.group({
      protobufValue: [null]
    });
    this.protobufValueFormGroup.get('protobufValue').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.updateModel();
    });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  registerOnChange(fn: any) {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any) {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.protobufValueFormGroup.disable({emitEvent: false});
    } else {
      this.protobufValueFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: string): void {
    this.protobufValueFormGroup.patchValue({ protobufValue: value }, {emitEvent: false});
  }

  validate() {
    return this.protobufValueFormGroup.get('protobufValue').valid ? null : {
      protobuf: false
    };
  }

  private updateModel() {
    const protobufValue: string = this.protobufValueFormGroup.get('protobufValue').value;
    this.propagateChange(protobufValue);
  }
}
