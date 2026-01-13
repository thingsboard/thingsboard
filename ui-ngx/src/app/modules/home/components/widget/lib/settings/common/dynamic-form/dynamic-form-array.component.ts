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

import { Component, DestroyRef, forwardRef, Input, OnInit, ViewEncapsulation } from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormArray,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  Validator
} from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { defaultFormPropertyValue, FormProperty } from '@shared/models/dynamic-form.models';
import { CdkDragDrop } from '@angular/cdk/drag-drop';

@Component({
  selector: 'tb-dynamic-form-array',
  templateUrl: './dynamic-form-array.component.html',
  styleUrls: ['./dynamic-form-array.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => DynamicFormArrayComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => DynamicFormArrayComponent),
      multi: true
    }
  ],
  encapsulation: ViewEncapsulation.None
})
export class DynamicFormArrayComponent implements ControlValueAccessor, OnInit, Validator {

  @Input()
  disabled: boolean;

  @Input()
  itemProperty: FormProperty;

  @Input()
  title: string;

  propertiesFormGroup: UntypedFormGroup;

  get dragEnabled(): boolean {
    return !this.disabled && this.propertiesFormArray().controls.length > 1;
  }

  private propagateChange = (_val: any) => {};

  constructor(private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
  }

  ngOnInit() {
    this.propertiesFormGroup = this.fb.group({
      properties: this.fb.array([])
    });
    this.propertiesFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      () => {
        const properties: {[id: string]: any}[] = this.propertiesFormGroup.get('properties').value;
        const value = properties.map(prop => prop[this.itemProperty.id]);
        this.propagateChange(value);
      }
    );
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.propertiesFormGroup.disable({emitEvent: false});
    } else {
      this.propertiesFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(values: any[] | undefined): void {
    this.propertiesFormGroup.setControl('properties', this.preparePropertiesFormArray(values || []), {emitEvent: false});
  }

  public validate(_c: UntypedFormControl) {
    const valid =  this.propertiesFormGroup.valid;
    return valid ? null : {
      properties: {
        valid: false,
      },
    };
  }

  propertyDrop(event: CdkDragDrop<string[]>) {
    const propertiesArray = this.propertiesFormGroup.get('properties') as UntypedFormArray;
    const property = propertiesArray.at(event.previousIndex);
    propertiesArray.removeAt(event.previousIndex, {emitEvent: false});
    propertiesArray.insert(event.currentIndex, property, {emitEvent: true});
  }

  propertiesFormArray(): UntypedFormArray {
    return this.propertiesFormGroup.get('properties') as UntypedFormArray;
  }

  trackByProperty(_index: number, propertyControl: AbstractControl): any {
    return propertyControl;
  }

  removeProperty(index: number, emitEvent = true) {
    (this.propertiesFormGroup.get('properties') as UntypedFormArray).removeAt(index, {emitEvent});
  }

  addProperty() {
    const property = {
      [this.itemProperty.id]: defaultFormPropertyValue(this.itemProperty)
    };
    const propertiesArray = this.propertiesFormGroup.get('properties') as UntypedFormArray;
    const propertyControl = this.fb.control(property, []);
    propertiesArray.push(propertyControl);
    setTimeout(() => {
      propertyControl.updateValueAndValidity();
    });
  }

  private preparePropertiesFormArray(values: any[] | undefined): UntypedFormArray {
    const propertiesControls: Array<AbstractControl> = [];
    if (values) {
      values.forEach((value) => {
        const property = {
          [this.itemProperty.id]: value
        };
        propertiesControls.push(this.fb.control(property, []));
      });
    }
    return this.fb.array(propertiesControls);
  }
}
