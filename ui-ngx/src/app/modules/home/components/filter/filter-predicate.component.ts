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

import { Component, DestroyRef, forwardRef, Input } from '@angular/core';
import {
  ControlValueAccessor,
  UntypedFormBuilder,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators,
} from '@angular/forms';
import {
  EntityKeyValueType,
  FilterPredicateType,
  KeyFilterPredicateInfo,
  BooleanOperation, booleanOperationTranslationMap,
  NumericOperation, numericOperationTranslationMap,
  StringOperation, stringOperationTranslationMap, ComplexFilterPredicateInfo, KeyFilterPredicateUserInfo,
  KeyFilterPredicate
} from '@shared/models/query/query.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ComplexFilterPredicateDialogData } from '@home/components/filter/filter-component.models';
import {
  ComplexFilterPredicateDialogComponent
} from '@home/components/filter/complex-filter-predicate-dialog.component';
import { MatDialog } from '@angular/material/dialog';
import {
  FilterUserInfoDialogComponent,
  FilterUserInfoDialogData
} from '@home/components/filter/filter-user-info-dialog.component';

@Component({
    selector: 'tb-filter-predicate',
    templateUrl: './filter-predicate.component.html',
    styleUrls: [],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => FilterPredicateComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => FilterPredicateComponent),
            multi: true
        }
    ],
    standalone: false
})
export class FilterPredicateComponent implements ControlValueAccessor, Validator {

  @Input() disabled: boolean;

  @Input() valueType: EntityKeyValueType;

  @Input() key: string;

  @Input() displayUserParameters = true;

  @Input() allowUserDynamicSource = true;

  @Input() onlyUserDynamicSource = false;

  filterPredicateFormGroup = this.fb.group({
    operation: [],
    ignoreCase: false,
    predicates: [],
    value: [],
    userInfo: []
  });

  type: FilterPredicateType;

  filterPredicateType = FilterPredicateType;

  stringOperations = Object.keys(StringOperation);
  stringOperationEnum = StringOperation;
  stringOperationTranslations = stringOperationTranslationMap;

  numericOperations = Object.keys(NumericOperation);
  numericOperationEnum = NumericOperation;
  numericOperationTranslations = numericOperationTranslationMap;

  booleanOperations = Object.keys(BooleanOperation);
  booleanOperationEnum = BooleanOperation;
  booleanOperationTranslations = booleanOperationTranslationMap;

  private propagateChange = null;

  constructor(private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef,
              private dialog: MatDialog) {
    this.filterPredicateFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateModel();
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
      this.filterPredicateFormGroup.disable({emitEvent: false});
    } else {
      this.filterPredicateFormGroup.enable({emitEvent: false});
    }
  }

  validate(): ValidationErrors | null {
    return this.filterPredicateFormGroup.valid ? null : {
      filterPredicate: {valid: false}
    };
  }

  writeValue(predicate: KeyFilterPredicateInfo): void {
    this.type = predicate.keyFilterPredicate.type;
    this.updateValidators();
    this.filterPredicateFormGroup.patchValue(predicate.keyFilterPredicate, {emitEvent: false});
    this.filterPredicateFormGroup.get('userInfo').patchValue(predicate.userInfo, {emitEvent: false});
  }

  private updateValidators(): void {
    const operationCtrl = this.filterPredicateFormGroup.get('operation');
    const predicatesCtrl = this.filterPredicateFormGroup.get('predicates');
    operationCtrl.setValidators([Validators.required]);
    if (this.type === FilterPredicateType.COMPLEX) {
      predicatesCtrl.setValidators([Validators.required]);
    } else {
      predicatesCtrl.clearValidators();
    }
    operationCtrl.updateValueAndValidity({emitEvent: false});
    predicatesCtrl.updateValueAndValidity({emitEvent: false});
  }

  private updateModel() {
    let predicate: KeyFilterPredicateInfo = null;
    if (this.filterPredicateFormGroup.valid) {
      const v = this.filterPredicateFormGroup.getRawValue();
      let keyFilterPredicate: KeyFilterPredicate;
      if (this.type === FilterPredicateType.COMPLEX) {
        keyFilterPredicate = {
          type: FilterPredicateType.COMPLEX,
          operation: v.operation,
          predicates: v.predicates
        } as KeyFilterPredicate;
      } else {
        keyFilterPredicate = {
          type: this.type,
          value: v.value,
          operation: v.operation,
          ignoreCase: !!v.ignoreCase
        } as KeyFilterPredicate;
      }
      predicate = {
        keyFilterPredicate,
        userInfo: v.userInfo
      };
    }
    this.propagateChange(predicate);
  }

  public openComplexFilterDialog() {
    this.dialog.open<ComplexFilterPredicateDialogComponent, ComplexFilterPredicateDialogData,
      ComplexFilterPredicateInfo>(ComplexFilterPredicateDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        complexPredicate: {
          type: FilterPredicateType.COMPLEX,
          operation: this.filterPredicateFormGroup.get('operation').value,
          predicates: this.filterPredicateFormGroup.get('predicates').value
        },
        readonly: this.disabled,
        valueType: this.valueType,
        isAdd: false,
        key: this.key,
        displayUserParameters: this.displayUserParameters,
        allowUserDynamicSource: this.allowUserDynamicSource,
        onlyUserDynamicSource: this.onlyUserDynamicSource
      }
    }).afterClosed().subscribe(
      (result) => {
        if (result) {
          this.filterPredicateFormGroup.patchValue(result);
        }
      }
    );
  }

  public openFilterUserInfoDialog() {
    this.dialog.open<FilterUserInfoDialogComponent, FilterUserInfoDialogData,
      KeyFilterPredicateUserInfo>(FilterUserInfoDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        keyFilterPredicateUserInfo: this.filterPredicateFormGroup.get('userInfo').value,
        valueType: this.valueType,
        key: this.key,
        operation: this.filterPredicateFormGroup.get('operation').value,
        readonly: this.disabled
      }
    }).afterClosed().subscribe(
      (result) => {
        if (result) {
          this.filterPredicateFormGroup.get('userInfo').patchValue(result);
        }
      }
    );
  }
}
