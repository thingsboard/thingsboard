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

import { Component, DestroyRef, forwardRef, Injector, Input, OnInit } from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  FormArray,
  FormBuilder,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  NgControl,
  ValidationErrors,
  Validator,
  ValidatorFn,
  Validators
} from '@angular/forms';
import { coerceBoolean, PageComponent } from '@shared/public-api';
import { isDefinedAndNotNull, isEqual } from '@core/public-api';
import { TranslateService } from '@ngx-translate/core';
import { OriginatorFieldsMappingValues, SvMapOption } from '../rule-node-config.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'tb-sv-map-config',
  templateUrl: './sv-map-config.component.html',
  styleUrls: ['./sv-map-config.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => SvMapConfigComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => SvMapConfigComponent),
      multi: true,
    }
  ]
})
export class SvMapConfigComponent extends PageComponent implements ControlValueAccessor, OnInit, Validator {

  private propagateChange = null;

  svListFormGroup: FormGroup;
  ngControl: NgControl;

  @Input() selectOptions: SvMapOption[];

  @Input()
  @coerceBoolean()
  disabled = false;

  @Input() labelText: string;

  @Input() requiredText: string;

  @Input() targetKeyPrefix: string;

  @Input() selectText: string;

  @Input() selectRequiredText: string;

  @Input() valText: string;

  @Input() valRequiredText: string;

  @Input() hintText: string;

  @Input() popupHelpLink: string;

  @Input()
  @coerceBoolean()
  required = false;

  constructor(public translate: TranslateService,
              private injector: Injector,
              private fb: FormBuilder,
              private destroyRef: DestroyRef) {
    super();
  }

  ngOnInit(): void {
    this.ngControl = this.injector.get(NgControl);
    if (this.ngControl != null) {
      this.ngControl.valueAccessor = this;
    }

    this.svListFormGroup = this.fb.group({
      keyVals: this.fb.array([])
    }, {validators: [this.propagateNestedErrors, this.oneMapRequiredValidator]});

    this.svListFormGroup.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.updateModel();
      });
  }

  keyValsFormArray(): FormArray {
    return this.svListFormGroup.get('keyVals') as FormArray;
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.svListFormGroup.disable({emitEvent: false});
    } else {
      this.svListFormGroup.enable({emitEvent: false});
    }
  }

  private oneMapRequiredValidator: ValidatorFn = (control: FormGroup): ValidationErrors | null => control.get('keyVals').value.length;

  private propagateNestedErrors: ValidatorFn = (controls: FormArray | FormGroup | AbstractControl): ValidationErrors | null => {
    if (this.svListFormGroup && this.svListFormGroup.get('keyVals') && this.svListFormGroup.get('keyVals')?.status === 'VALID') {
      return null;
    }
    const errors = {};
    if (this.svListFormGroup) {this.svListFormGroup.setErrors(null);}
    if (controls instanceof FormArray || controls instanceof FormGroup) {
      if (controls.errors) {
        for (const errorKey of Object.keys(controls.errors)) {
          errors[errorKey] = true;
        }
      }
      for (const control of Object.keys(controls.controls)) {
        const innerErrors = this.propagateNestedErrors(controls.controls[control]);
        if (innerErrors && Object.keys(innerErrors).length) {
          for (const errorKey of Object.keys(innerErrors)) {
            errors[errorKey] = true;
          }
        }
      }
      return errors;
    } else {
      if (controls.errors) {
        for (const errorKey of Object.keys(controls.errors)) {
          errors[errorKey] = true;
        }
      }
    }
    return !isEqual(errors, {}) ? errors : null;
  };

  writeValue(keyValMap: { [key: string]: string }): void {
    const keyValuesData = Object.keys(keyValMap).map(key => ({key, value: keyValMap[key]}));
    if (this.keyValsFormArray().length === keyValuesData.length) {
      this.keyValsFormArray().patchValue(keyValuesData, {emitEvent: false})
    } else {
      const keyValsControls: Array<FormGroup> = [];
      keyValuesData.forEach(data => {
        keyValsControls.push(this.fb.group({
          key: [data.key, [Validators.required, ]],
          value: [data.value, [Validators.required, Validators.pattern(/(?:.|\s)*\S(&:.|\s)*/)]]
        }));
      });
      this.svListFormGroup.setControl('keyVals', this.fb.array(keyValsControls, this.propagateNestedErrors), {emitEvent: false});
      for (const formGroup of this.keyValsFormArray().controls) {
        this.keyChangeSubscribe(formGroup as FormGroup);
      }
    }
  }

  public filterSelectOptions(keyValControl?: AbstractControl) {
    const deleteFieldsArray = [];
    for (const fieldMap of this.svListFormGroup.get('keyVals').value) {
      const findDeleteField = this.selectOptions.find((field) => field.value === fieldMap.key);
      if (findDeleteField) {
        deleteFieldsArray.push(findDeleteField);
      }
    }

    const filterSelectOptions = [];
    for (const selectOption of this.selectOptions) {
      if (!isDefinedAndNotNull(deleteFieldsArray.find((deleteField) => deleteField.value === selectOption.value)) ||
        selectOption.value === keyValControl?.get('key').value) {
        filterSelectOptions.push(selectOption);
      }
    }

    return filterSelectOptions;
  }

  public removeKeyVal(index: number) {
    this.keyValsFormArray().removeAt(index);
  }

  public addKeyVal() {
    this.keyValsFormArray().push(this.fb.group({
      key: ['', [Validators.required]],
      value: ['', [Validators.required, Validators.pattern(/(?:.|\s)*\S(&:.|\s)*/)]]
    }));
    this.keyChangeSubscribe(this.keyValsFormArray().at(this.keyValsFormArray().length - 1) as FormGroup);
  }

  private keyChangeSubscribe(formGroup: FormGroup) {
    formGroup.get('key').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((value) => {
      const mappedValue = OriginatorFieldsMappingValues.get(value);
      formGroup.get('value').patchValue(this.targetKeyPrefix + mappedValue[0].toUpperCase() + mappedValue.slice(1));
    });
  }

  public validate() {
    const svList: { key: string; value: string }[] = this.svListFormGroup.get('keyVals').value;
    if (!svList.length && this.required) {
      return {
        svMapRequired: true
      };
    }
    if (!this.svListFormGroup.valid) {
      return {
        svFieldsRequired: true
      };
    }
    return null;
  }

  private updateModel() {
    const svList: { key: string; value: string }[] = this.svListFormGroup.get('keyVals').value;
    if (this.required && !svList.length || !this.svListFormGroup.valid) {
      this.propagateChange(null);
    } else {
      const keyValMap: { [key: string]: string } = {};
      svList.forEach((entry) => {
        keyValMap[entry.key] = entry.value;
      });
      this.propagateChange(keyValMap);
    }
  }
}
