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

import { booleanAttribute, Component, DestroyRef, forwardRef, Input } from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator
} from '@angular/forms';
import { EntityKeyValueType } from '@shared/models/query/query.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  AlarmRuleBooleanOperation,
  alarmRuleBooleanOperationTranslationMap,
  AlarmRuleFilterPredicate,
  AlarmRuleFilterPredicateType,
  AlarmRuleNumericOperation,
  alarmRuleNumericOperationTranslationMap,
  AlarmRuleStringOperation,
  alarmRuleStringOperationTranslationMap,
  ComplexAlarmRuleFilterPredicate
} from "@shared/models/alarm-rule.models";
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

  @Input({ transform: booleanAttribute })
  disabled: boolean;

  @Input()
  valueType: EntityKeyValueType;

  @Input()
  arguments: Record<string, CalculatedFieldArgument>;

  @Input()
  argumentInUse: string;

  filterPredicateFormGroup = this.fb.group({
    operation: [],
    ignoreCase: false,
    predicates: [],
    value: [],
    duration: []
  });

  type: AlarmRuleFilterPredicateType;

  filterPredicateType = AlarmRuleFilterPredicateType;

  stringOperations = Object.keys(AlarmRuleStringOperation);
  stringOperation = AlarmRuleStringOperation;
  stringOperationTranslationMap = alarmRuleStringOperationTranslationMap;

  numericOperations = Object.keys(AlarmRuleNumericOperation);
  numericOperationEnum = AlarmRuleNumericOperation;
  numericOperationTranslations = alarmRuleNumericOperationTranslationMap;

  booleanOperations = Object.keys(AlarmRuleBooleanOperation);
  booleanOperationEnum = AlarmRuleBooleanOperation;
  booleanOperationTranslations = alarmRuleBooleanOperationTranslationMap;

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

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.filterPredicateFormGroup.disable({emitEvent: false});
    } else {
      this.filterPredicateFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(predicate: AlarmRuleFilterPredicate): void {
    this.type = predicate.type;
    if (predicate.type === AlarmRuleFilterPredicateType.NO_DATA) {
      this.type = AlarmRuleFilterPredicateType[this.valueType];
      this.filterPredicateFormGroup.patchValue({operation: 'NO_DATA', duration: predicate}, {emitEvent: false});
    } else {
      this.filterPredicateFormGroup.patchValue(predicate, {emitEvent: false});
    }
  }

  private updateModel() {
    const predicate = this.filterPredicateFormGroup.value;
    if (predicate.operation === 'NO_DATA') {
      this.propagateChange(predicate.duration);
    } else {
      if (!predicate.value) {
        switch (this.valueType) {
          case EntityKeyValueType.STRING:
            predicate.value = {
              staticValue: ''
            };
            break;
          case EntityKeyValueType.NUMERIC:
            predicate.value = {
              staticValue: 0
            };
            break;
          case EntityKeyValueType.DATE_TIME:
            predicate.value = {
              staticValue: Date.now()
            };
            break;
          case EntityKeyValueType.BOOLEAN:
            predicate.value = {
              staticValue: false
            };
            break;
        }
      }
      this.propagateChange({type: this.type, ...predicate});
    }
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
        argumentInUse: this.argumentInUse,
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
