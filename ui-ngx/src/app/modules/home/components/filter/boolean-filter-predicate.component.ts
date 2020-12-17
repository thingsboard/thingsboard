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
  BooleanFilterPredicate,
  BooleanOperation,
  booleanOperationTranslationMap, EntityKeyValueType,
  FilterPredicateType
} from '@shared/models/query/query.models';

@Component({
  selector: 'tb-boolean-filter-predicate',
  templateUrl: './boolean-filter-predicate.component.html',
  styleUrls: ['./filter-predicate.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => BooleanFilterPredicateComponent),
      multi: true
    }
  ]
})
export class BooleanFilterPredicateComponent implements ControlValueAccessor, OnInit {

  @Input() disabled: boolean;

  @Input() allowUserDynamicSource = true;

  valueTypeEnum = EntityKeyValueType;

  booleanFilterPredicateFormGroup: FormGroup;

  booleanOperations = Object.keys(BooleanOperation);
  booleanOperationEnum = BooleanOperation;
  booleanOperationTranslations = booleanOperationTranslationMap;

  private propagateChange = null;

  constructor(private fb: FormBuilder) {
  }

  ngOnInit(): void {
    this.booleanFilterPredicateFormGroup = this.fb.group({
      operation: [BooleanOperation.EQUAL, [Validators.required]],
      value: [null, [Validators.required]]
    });
    this.booleanFilterPredicateFormGroup.valueChanges.subscribe(() => {
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
      this.booleanFilterPredicateFormGroup.disable({emitEvent: false});
    } else {
      this.booleanFilterPredicateFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(predicate: BooleanFilterPredicate): void {
    this.booleanFilterPredicateFormGroup.get('operation').patchValue(predicate.operation, {emitEvent: false});
    this.booleanFilterPredicateFormGroup.get('value').patchValue(predicate.value, {emitEvent: false});
  }

  private updateModel() {
    let predicate: BooleanFilterPredicate = null;
    if (this.booleanFilterPredicateFormGroup.valid) {
      predicate = this.booleanFilterPredicateFormGroup.getRawValue();
      predicate.type = FilterPredicateType.BOOLEAN;
    }
    this.propagateChange(predicate);
  }

}
