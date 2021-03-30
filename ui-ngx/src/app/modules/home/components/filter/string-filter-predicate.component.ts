///
/// Copyright © 2016-2021 The Thingsboard Authors
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
  StringFilterPredicate,
  StringOperation,
  stringOperationTranslationMap
} from '@shared/models/query/query.models';

@Component({
  selector: 'tb-string-filter-predicate',
  templateUrl: './string-filter-predicate.component.html',
  styleUrls: ['./filter-predicate.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => StringFilterPredicateComponent),
      multi: true
    }
  ]
})
export class StringFilterPredicateComponent implements ControlValueAccessor, OnInit {

  @Input() disabled: boolean;

  @Input() allowUserDynamicSource = true;

  @Input() onlyUserDynamicSource = false;

  valueTypeEnum = EntityKeyValueType;

  stringFilterPredicateFormGroup: FormGroup;

  stringOperations = Object.keys(StringOperation);
  stringOperationEnum = StringOperation;
  stringOperationTranslations = stringOperationTranslationMap;

  private propagateChange = null;

  constructor(private fb: FormBuilder) {
  }

  ngOnInit(): void {
    this.stringFilterPredicateFormGroup = this.fb.group({
      operation: [StringOperation.STARTS_WITH, [Validators.required]],
      value: [null, [Validators.required]],
      ignoreCase: [false]
    });
    this.stringFilterPredicateFormGroup.valueChanges.subscribe(() => {
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
      this.stringFilterPredicateFormGroup.disable({emitEvent: false});
    } else {
      this.stringFilterPredicateFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(predicate: StringFilterPredicate): void {
    this.stringFilterPredicateFormGroup.get('operation').patchValue(predicate.operation, {emitEvent: false});
    this.stringFilterPredicateFormGroup.get('value').patchValue(predicate.value, {emitEvent: false});
    this.stringFilterPredicateFormGroup.get('ignoreCase').patchValue(predicate.ignoreCase, {emitEvent: false});
  }

  private updateModel() {
    let predicate: StringFilterPredicate = null;
    if (this.stringFilterPredicateFormGroup.valid) {
      predicate = this.stringFilterPredicateFormGroup.getRawValue();
      predicate.type = FilterPredicateType.STRING;
    }
    this.propagateChange(predicate);
  }

}
