///
/// Copyright © 2016-2024 The Thingsboard Authors
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

import { Component, forwardRef } from '@angular/core';
import {
  ControlValueAccessor,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  Validator,
  ValidationErrors,
  FormBuilder,
  FormGroup
} from '@angular/forms';
import { PageComponent } from '@shared/components/page.component';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'tb-calculated-field-test-arguments',
  templateUrl: './calculated-field-test-arguments.component.html',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => CalculatedFieldTestArgumentsComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => CalculatedFieldTestArgumentsComponent),
      multi: true,
    }
  ]
})
export class CalculatedFieldTestArgumentsComponent extends PageComponent implements ControlValueAccessor, Validator {


  argumentsFormArray = this.fb.array<FormGroup>([]);

  private propagateChange: (value: { argumentName: string; value: unknown }) => void;

  constructor(private fb: FormBuilder) {
    super();
    this.argumentsFormArray.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.propagateChange(this.getValue()));
  }

  registerOnChange(propagateChange: (value: { argumentName: string; value: unknown }) => void): void {
    this.propagateChange = propagateChange;
  }

  registerOnTouched(_): void {
  }

  writeValue(argumentsObj: Record<string, unknown>): void {
    this.argumentsFormArray.clear();
    Object.keys(argumentsObj).forEach(key => {
      this.argumentsFormArray.push(this.fb.group({
        argumentName: [{ value: key, disabled: true}],
        value: [argumentsObj[key]]
      }) as FormGroup, {emitEvent: false});
    });
  }

  validate(): ValidationErrors | null {
    return this.argumentsFormArray.valid ? null : { arguments: { valid: false } };
  }

  private getValue(): { argumentName: string; value: unknown } {
    return this.argumentsFormArray.getRawValue().reduce((acc, rowItem) => {
      const { argumentName, value } = rowItem;
      acc[argumentName] = value;
      return acc;
    }, {}) as { argumentName: string; value: unknown };
  }
}
