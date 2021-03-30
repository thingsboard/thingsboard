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
  FilterPredicateType, KeyFilterPredicate, KeyFilterPredicateInfo
} from '@shared/models/query/query.models';

@Component({
  selector: 'tb-filter-predicate',
  templateUrl: './filter-predicate.component.html',
  styleUrls: [],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => FilterPredicateComponent),
      multi: true
    }
  ]
})
export class FilterPredicateComponent implements ControlValueAccessor, OnInit {

  @Input() disabled: boolean;

  @Input() valueType: EntityKeyValueType;

  @Input() key: string;

  @Input() displayUserParameters = true;

  @Input() allowUserDynamicSource = true;

  @Input() onlyUserDynamicSource = false;

  filterPredicateFormGroup: FormGroup;

  type: FilterPredicateType;

  filterPredicateType = FilterPredicateType;

  private propagateChange = null;

  constructor(private fb: FormBuilder) {
  }

  ngOnInit(): void {
    this.filterPredicateFormGroup = this.fb.group({
      predicate: [null, [Validators.required]],
      userInfo: [null, []]
    });
    this.filterPredicateFormGroup.valueChanges.subscribe(() => {
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
      this.filterPredicateFormGroup.disable({emitEvent: false});
    } else {
      this.filterPredicateFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(predicate: KeyFilterPredicateInfo): void {
    this.type = predicate.keyFilterPredicate.type;
    this.filterPredicateFormGroup.get('predicate').patchValue(predicate.keyFilterPredicate, {emitEvent: false});
    this.filterPredicateFormGroup.get('userInfo').patchValue(predicate.userInfo, {emitEvent: false});
  }

  private updateModel() {
    let predicate: KeyFilterPredicateInfo = null;
    if (this.filterPredicateFormGroup.valid) {
      predicate = {
        keyFilterPredicate: this.filterPredicateFormGroup.getRawValue().predicate,
        userInfo: this.filterPredicateFormGroup.getRawValue().userInfo
      };
    }
    this.propagateChange(predicate);
  }

}
