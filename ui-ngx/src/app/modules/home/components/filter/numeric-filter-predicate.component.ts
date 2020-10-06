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
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import {
  EntityKeyValueType,
  FilterPredicateType,
  NumericFilterPredicate,
  NumericOperation,
  numericOperationTranslationMap,
} from '@shared/models/query/query.models';

@Component({
  selector: 'tb-numeric-filter-predicate',
  templateUrl: './numeric-filter-predicate.component.html',
  styleUrls: ['./filter-predicate.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => NumericFilterPredicateComponent),
      multi: true
    }
  ]
})
export class NumericFilterPredicateComponent implements ControlValueAccessor, OnInit {

  @Input() disabled: boolean;

  @Input() allowUserDynamicSource = true;

  @Input() valueType: EntityKeyValueType;

  numericFilterPredicateFormGroup: FormGroup;

  valueTypeEnum = EntityKeyValueType;

  numericOperations = Object.keys(NumericOperation);
  numericOperationEnum = NumericOperation;
  numericOperationTranslations = numericOperationTranslationMap;

  private propagateChange = null;

  constructor(private fb: FormBuilder) {
  }

  ngOnInit(): void {
    this.numericFilterPredicateFormGroup = this.fb.group({
      operation: [NumericOperation.EQUAL, [Validators.required]],
      value: [null, [Validators.required]]
    });
    this.numericFilterPredicateFormGroup.valueChanges.subscribe(() => {
      this.updateModel();
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState?(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.numericFilterPredicateFormGroup.disable({emitEvent: false});
    } else {
      this.numericFilterPredicateFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(predicate: NumericFilterPredicate): void {
    this.numericFilterPredicateFormGroup.get('operation').patchValue(predicate.operation, {emitEvent: false});
    this.numericFilterPredicateFormGroup.get('value').patchValue(predicate.value, {emitEvent: false});
  }

  private updateModel() {
    let predicate: NumericFilterPredicate = null;
    if (this.numericFilterPredicateFormGroup.valid) {
      predicate = this.numericFilterPredicateFormGroup.getRawValue();
      predicate.type = FilterPredicateType.NUMERIC;
    }
    this.propagateChange(predicate);
  }

}
