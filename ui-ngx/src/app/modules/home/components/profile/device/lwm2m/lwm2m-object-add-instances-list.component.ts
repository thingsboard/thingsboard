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

import { Component, ElementRef, forwardRef, Input, ViewChild } from '@angular/core';
import {
  ControlValueAccessor,
  UntypedFormBuilder,
  UntypedFormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR, ValidationErrors, Validator,
  Validators
} from '@angular/forms';
import { INSTANCES_ID_VALUE_MAX, INSTANCES_ID_VALUE_MIN } from './lwm2m-profile-config.models';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { COMMA, ENTER, SEMICOLON } from '@angular/cdk/keycodes';
import { MatChipInputEvent } from '@angular/material/chips';

@Component({
    selector: 'tb-profile-lwm2m-object-add-instances-list',
    templateUrl: './lwm2m-object-add-instances-list.component.html',
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => Lwm2mObjectAddInstancesListComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => Lwm2mObjectAddInstancesListComponent),
            multi: true
        }
    ],
    standalone: false
})
export class Lwm2mObjectAddInstancesListComponent implements ControlValueAccessor, Validator {

  private requiredValue: boolean;

  @Input()
  disabled: boolean;

  get required(): boolean {
    return this.requiredValue;
  }

  @Input()
  set required(value: boolean) {
    this.requiredValue = coerceBooleanProperty(value);
    this.updateValidators();
  }

  @ViewChild('instanceId') instanceId: ElementRef<HTMLInputElement>;

  instancesListFormGroup: UntypedFormGroup;
  instancesId = new Set<number>();
  separatorKeysCodes = [ENTER, COMMA, SEMICOLON];
  instanceIdValueMax = INSTANCES_ID_VALUE_MAX;

  private propagateChange = (v: any) => { };

  constructor(private fb: UntypedFormBuilder) {
    this.instancesListFormGroup = this.fb.group({
      instanceList: [null],
      instanceId: [null, [
        Validators.min(INSTANCES_ID_VALUE_MIN),
        Validators.max(INSTANCES_ID_VALUE_MAX),
        Validators.pattern('[0-9]*')]]
    });
    this.instancesListFormGroup.get('instanceId').statusChanges.subscribe((value) => {
      if (value === 'INVALID') {
        const errors = this.instancesListFormGroup.get('instanceId').errors;
        this.instancesListFormGroup.get('instanceList').setErrors(errors);
      } else {
        this.instancesListFormGroup.get('instanceList').updateValueAndValidity({onlySelf: true});
      }
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.instancesListFormGroup.disable({emitEvent: false});
    } else {
      this.instancesListFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: Set<number>): void {
    if (value && value.size) {
      this.instancesId = value;
      this.instancesListFormGroup.patchValue({instanceList: Array.from(this.instancesId)}, {emitEvent: false});
    }
  }

  validate(): ValidationErrors | null {
    return this.instancesListFormGroup.valid ? null : {
      instancesListForm: false
    };
  }

  add = (event: MatChipInputEvent): void => {
    const value = event.value;
    if (this.instancesListFormGroup.get('instanceId').valid && value !== '' && Number.isFinite(Number(value))) {
      this.instancesId.add(Number(value));
      this.instancesListFormGroup.patchValue({instanceList: Array.from(this.instancesId)}, {emitEvent: false});
      this.instancesListFormGroup.get('instanceId').setValue(null, {emitEvent: false});
      this.propagateChange(this.instancesId);
    }
  }

  remove = (object: number): void => {
    this.instancesId.delete(object);
    this.instancesListFormGroup.patchValue({instanceList: Array.from(this.instancesId)}, {emitEvent: false});
    this.propagateChange(this.instancesId);
  }

  onFocus() {
    setTimeout(() => {
      this.instanceId.nativeElement.blur();
      this.instanceId.nativeElement.focus();
    }, 0);
  }

  private updateValidators() {
    this.instancesListFormGroup.get('instanceList').setValidators(this.required ? [Validators.required] : []);
    this.instancesListFormGroup.get('instanceList').updateValueAndValidity({emitEvent: false});
  }
}
