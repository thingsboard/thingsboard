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
  AbstractControl,
  ControlValueAccessor,
  FormArray,
  FormBuilder,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { Observable, of } from 'rxjs';
import {
  ComplexOperation,
  complexOperationTranslationMap,
  EntityKeyValueType,
  entityKeyValueTypeToFilterPredicateType
} from '@shared/models/query/query.models';
import { MatDialog } from '@angular/material/dialog';
import { map } from 'rxjs/operators';
import {
  AlarmRuleComplexFilterPredicateDialogComponent,
  AlarmRuleComplexFilterPredicateDialogData
} from "@home/components/alarm-rules/filter/alarm-rule-complex-filter-predicate-dialog.component";
import {
  AlarmRuleBooleanOperation,
  AlarmRuleFilterPredicate,
  AlarmRuleFilterPredicateType,
  AlarmRuleNumericOperation,
  AlarmRulePredicateInfo,
  AlarmRuleStringOperation,
  ComplexAlarmRuleFilterPredicate
} from "@shared/models/alarm-rule.models";
import { CalculatedFieldArgument } from "@shared/models/calculated-field.models";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";

@Component({
  selector: 'tb-alarm-rule-filter-predicate-list',
  templateUrl: './alarm-rule-filter-predicate-list.component.html',
  styleUrls: ['./alarm-rule-filter-predicate-list.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => AlarmRuleFilterPredicateListComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => AlarmRuleFilterPredicateListComponent),
      multi: true
    }
  ]
})
export class AlarmRuleFilterPredicateListComponent implements ControlValueAccessor, Validator {

  @Input() disabled: boolean;

  @Input() valueType: EntityKeyValueType;

  @Input() operation: ComplexOperation = ComplexOperation.AND;

  @Input() arguments: Record<string, CalculatedFieldArgument>;

  @Input() argumentInUse: string;

  filterListFormGroup = this.fb.group({
    predicates: this.fb.array([])
  });

  valueTypeEnum = EntityKeyValueType;

  complexOperationTranslations = complexOperationTranslationMap;

  private propagateChange= (v: any) => { };

  constructor(private fb: FormBuilder,
              private dialog: MatDialog,
              private destroyRef: DestroyRef) {
    this.filterListFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => this.updateModel());
  }

  get predicatesFormArray(): FormArray {
    return this.filterListFormGroup.get('predicates') as FormArray;
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.filterListFormGroup.disable({emitEvent: false});
    } else {
      this.filterListFormGroup.enable({emitEvent: false});
    }
  }

  validate(control: AbstractControl): ValidationErrors | null {
    return this.filterListFormGroup.valid ? null : {
      filterList: {valid: false}
    };
  }

  writeValue(predicates: Array<AlarmRulePredicateInfo>): void {
    const predicateControls: Array<AbstractControl> = [];
    if (predicates) {
      for (const predicate of predicates) {
        predicateControls.push(this.fb.control(predicate, [Validators.required]));
      }
    }
    this.predicatesFormArray.clear();
    predicateControls.forEach(predicate => this.predicatesFormArray.push(predicate));
  }

  public removePredicate(index: number) {
    this.predicatesFormArray.removeAt(index);
  }

  public addPredicate(complex: boolean) {
    const predicatesFormArray = this.filterListFormGroup.get('predicates') as FormArray;
    const predicate = this.createDefaultFilterPredicate(this.valueType, complex);
    let observable: Observable<AlarmRuleFilterPredicate>;
    if (complex) {
      observable = this.openComplexFilterDialog(predicate as ComplexAlarmRuleFilterPredicate);
    } else {
      observable = of(predicate);
    }
    observable.subscribe((result) => {
      if (result) {
        predicatesFormArray.push(this.fb.control(result, [Validators.required]));
      }
    });
  }

  private createDefaultFilterPredicate(valueType: EntityKeyValueType, complex: boolean): AlarmRuleFilterPredicate {
    const predicate = {
      type: complex ? AlarmRuleFilterPredicateType.COMPLEX : entityKeyValueTypeToFilterPredicateType(valueType)
    } as AlarmRuleFilterPredicate;
    switch (predicate.type) {
      case AlarmRuleFilterPredicateType.STRING:
        predicate.operation = AlarmRuleStringOperation.STARTS_WITH;
        predicate.value = {
          staticValue: ''
        };
        predicate.ignoreCase = false;
        break;
      case AlarmRuleFilterPredicateType.NUMERIC:
        predicate.operation = AlarmRuleNumericOperation.EQUAL;
        predicate.value = {
          staticValue: valueType === EntityKeyValueType.DATE_TIME ? Date.now() : 0
        };
        break;
      case AlarmRuleFilterPredicateType.BOOLEAN:
        predicate.operation = AlarmRuleBooleanOperation.EQUAL;
        predicate.value = {
          staticValue: false
        };
        break;
      case AlarmRuleFilterPredicateType.COMPLEX:
        predicate.operation = ComplexOperation.AND;
        predicate.predicates = [];
        break;
    }
    return predicate;
  }

  private openComplexFilterDialog(predicate: ComplexAlarmRuleFilterPredicate): Observable<ComplexAlarmRuleFilterPredicate> {
    return this.dialog.open<AlarmRuleComplexFilterPredicateDialogComponent, AlarmRuleComplexFilterPredicateDialogData,
      ComplexAlarmRuleFilterPredicate>(AlarmRuleComplexFilterPredicateDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        complexPredicate: predicate as ComplexAlarmRuleFilterPredicate,
        valueType: this.valueType,
        isAdd: true,
        arguments: this.arguments,
        argumentInUse: this.argumentInUse
      }
    }).afterClosed().pipe(
      map(result => result)
    );
  }

  private updateModel() {
    this.propagateChange(this.filterListFormGroup.get('predicates').value);
  }
}
