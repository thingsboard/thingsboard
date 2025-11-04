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

import { Component, forwardRef, Inject, Input, OnDestroy, OnInit } from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormArray,
  UntypedFormBuilder,
  UntypedFormGroup,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { Observable, of, Subject } from 'rxjs';
import {
  BooleanOperation,
  ComplexOperation,
  complexOperationTranslationMap,
  EntityKeyValueType,
  entityKeyValueTypeToFilterPredicateType,
  FilterPredicateType,
  NumericOperation,
  StringOperation
} from '@shared/models/query/query.models';
import { MatDialog } from '@angular/material/dialog';
import { map, takeUntil } from 'rxjs/operators';
import { ComponentType } from '@angular/cdk/portal';
import { COMPLEX_FILTER_PREDICATE_DIALOG_COMPONENT_TOKEN } from '@home/components/tokens';
import {
  AlarmRuleComplexFilterPredicateDialogComponent,
  AlarmRuleComplexFilterPredicateDialogData
} from "@home/components/alarm-rules/filter/alarm-rule-complex-filter-predicate-dialog.component";
import {
  AlarmRuleFilterPredicate,
  AlarmRulePredicateInfo,
  ComplexAlarmRuleFilterPredicate
} from "@shared/models/alarm-rule.models";
import { CalculatedFieldArgument } from "@shared/models/calculated-field.models";

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
export class AlarmRuleFilterPredicateListComponent implements ControlValueAccessor, Validator, OnInit, OnDestroy {

  @Input() disabled: boolean;

  @Input() valueType: EntityKeyValueType;

  @Input() operation: ComplexOperation = ComplexOperation.AND;

  @Input() arguments: Record<string, CalculatedFieldArgument>;

  filterListFormGroup: UntypedFormGroup;

  valueTypeEnum = EntityKeyValueType;

  complexOperationTranslations = complexOperationTranslationMap;

  private destroy$ = new Subject<void>();
  private propagateChange = null;

  constructor(private fb: UntypedFormBuilder,
              @Inject(COMPLEX_FILTER_PREDICATE_DIALOG_COMPONENT_TOKEN) private complexFilterPredicateDialogComponent: ComponentType<any>,
              private dialog: MatDialog) {
  }

  ngOnInit(): void {
    this.filterListFormGroup = this.fb.group({
      predicates: this.fb.array([])
    });
    this.filterListFormGroup.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => this.updateModel());
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get predicatesFormArray(): UntypedFormArray {
    return this.filterListFormGroup.get('predicates') as UntypedFormArray;
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState?(isDisabled: boolean): void {
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
    if (predicates?.length === this.predicatesFormArray.length) {
      this.predicatesFormArray.patchValue(predicates, {emitEvent: false});
    } else {
      const predicateControls: Array<AbstractControl> = [];
      if (predicates) {
        for (const predicate of predicates) {
          predicateControls.push(this.fb.control(predicate, [Validators.required]));
        }
      }
      this.filterListFormGroup.setControl('predicates', this.fb.array(predicateControls), {emitEvent: false});
      if (this.disabled) {
        this.filterListFormGroup.disable({emitEvent: false});
      } else {
        this.filterListFormGroup.enable({emitEvent: false});
      }
    }
  }

  public removePredicate(index: number) {
    this.predicatesFormArray.removeAt(index);
  }

  public addPredicate(complex: boolean) {
    const predicatesFormArray = this.filterListFormGroup.get('predicates') as UntypedFormArray;
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
      type: complex ? FilterPredicateType.COMPLEX : entityKeyValueTypeToFilterPredicateType(valueType)
    } as AlarmRuleFilterPredicate;
    switch (predicate.type) {
      case FilterPredicateType.STRING:
        predicate.operation = StringOperation.STARTS_WITH;
        predicate.value = {
          staticValue: ''
        };
        predicate.ignoreCase = false;
        break;
      case FilterPredicateType.NUMERIC:
        predicate.operation = NumericOperation.EQUAL;
        predicate.value = {
          staticValue: valueType === EntityKeyValueType.DATE_TIME ? Date.now() : 0
        };
        break;
      case FilterPredicateType.BOOLEAN:
        predicate.operation = BooleanOperation.EQUAL;
        predicate.value = {
          staticValue: false
        };
        break;
      case FilterPredicateType.COMPLEX:
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
      }
    }).afterClosed().pipe(
      map((result) => {
        if (result) {
          predicate = result;
          return predicate;
        } else {
          return null;
        }
      })
    );
  }

  private updateModel() {
    const predicates: Array<AlarmRulePredicateInfo> = this.filterListFormGroup.getRawValue().predicates;
    if (predicates.length) {
      this.propagateChange(predicates);
    } else {
      this.propagateChange(null);
    }
  }
}
