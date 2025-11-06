///
/// Copyright © 2016-2025 The Thingsboard Authors
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
  FormBuilder,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator
} from '@angular/forms';
import {
  BooleanOperation,
  booleanOperationTranslationMap,
  EntityKeyValueType,
  FilterPredicateType,
  NumericOperation,
  numericOperationTranslationMap,
  StringOperation,
  stringOperationTranslationMap
} from '@shared/models/query/query.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AlarmRuleFilterPredicate, ComplexAlarmRuleFilterPredicate } from "@shared/models/alarm-rule.models";
import { MatDialog } from "@angular/material/dialog";
import {
  AlarmRuleComplexFilterPredicateDialogComponent,
  AlarmRuleComplexFilterPredicateDialogData
} from "@home/components/alarm-rules/filter/alarm-rule-complex-filter-predicate-dialog.component";
import { CalculatedFieldArgument } from "@shared/models/calculated-field.models";

@Component({
  selector: 'tb-alarm-rule-filter-predicate',
  templateUrl: './alarm-rule-filter-predicate.component.html',
  styleUrls: [],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => AlarmRuleFilterPredicateComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => AlarmRuleFilterPredicateComponent),
      multi: true
    }
  ]
})
export class AlarmRuleFilterPredicateComponent implements ControlValueAccessor, Validator {

  @Input()
  valueType: EntityKeyValueType;

  @Input()
  arguments: Record<string, CalculatedFieldArgument>;

  filterPredicateFormGroup = this.fb.group({
    operation: [],
    ignoreCase: false,
    predicates: [],
    value: []
  });

  type: FilterPredicateType;

  filterPredicateType = FilterPredicateType;

  stringOperations = Object.keys(StringOperation);
  stringOperation = StringOperation;
  stringOperationTranslationMap = stringOperationTranslationMap;

  numericOperations = Object.keys(NumericOperation);
  numericOperationEnum = NumericOperation;
  numericOperationTranslations = numericOperationTranslationMap;

  booleanOperations = Object.keys(BooleanOperation);
  booleanOperationEnum = BooleanOperation;
  booleanOperationTranslations = booleanOperationTranslationMap;

  private propagateChange= (v: any) => { };

  constructor(private fb: FormBuilder,
              private dialog: MatDialog,
              private destroyRef: DestroyRef) {
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

  validate(): ValidationErrors | null {
    return this.filterPredicateFormGroup.valid ? null : {
      filterPredicate: {valid: false}
    };
  }

  writeValue(predicate: AlarmRuleFilterPredicate): void {
    this.type = predicate.type;
    this.filterPredicateFormGroup.patchValue(predicate, {emitEvent: false});
  }

  private updateModel() {
    this.propagateChange({type: this.type, ...this.filterPredicateFormGroup.value});
  }

  public openComplexFilterDialog() {
    this.dialog.open<AlarmRuleComplexFilterPredicateDialogComponent, AlarmRuleComplexFilterPredicateDialogData,
      ComplexAlarmRuleFilterPredicate>(AlarmRuleComplexFilterPredicateDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        complexPredicate: this.filterPredicateFormGroup.value as ComplexAlarmRuleFilterPredicate,
        valueType: this.valueType,
        isAdd: false,
        arguments: this.arguments,
      }
    }).afterClosed().subscribe(
      (result) => {
        if (result) {
          this.filterPredicateFormGroup.patchValue(result);
        }
      }
    );
  }
}
