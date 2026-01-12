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
import { Observable } from 'rxjs';
import {
  ComplexOperation,
  complexOperationTranslationMap,
  EntityKeyValueType
} from '@shared/models/query/query.models';
import { MatDialog } from '@angular/material/dialog';
import { deepClone } from '@core/utils';
import {
  AlarmRuleFilterDialogComponent,
  AlarmRuleFilterDialogData
} from "@home/components/alarm-rules/filter/alarm-rule-filter-dialog.component";
import {
  AlarmRuleFilter,
  areFilterAndPredicateArgumentsValid,
  FilterPredicateTypeTranslationMap
} from "@shared/models/alarm-rule.models";
import { CalculatedFieldArgument } from "@shared/models/calculated-field.models";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";

@Component({
  selector: 'tb-alarm-rule-filter-list',
  templateUrl: './alarm-rule-filter-list.component.html',
  styleUrls: ['./alarm-rule-filter-list.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => AlarmRuleFilterListComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => AlarmRuleFilterListComponent),
      multi: true
    }
  ]
})
export class AlarmRuleFilterListComponent implements ControlValueAccessor, Validator {

  @Input()
  arguments: Record<string, CalculatedFieldArgument>;

  @Input()
  operation: ComplexOperation = ComplexOperation.AND;

  @Input()
  readonly = false;

  filterListFormGroup = this.fb.group({
    filters: this.fb.array([])
  });

  disabled = false;

  areFilterAndPredicateArgumentsValid = areFilterAndPredicateArgumentsValid;

  complexOperationTranslationMap = complexOperationTranslationMap;
  FilterPredicateTypeTranslationMap = FilterPredicateTypeTranslationMap

  private propagateChange = (v: any) => { };

  constructor(private fb: FormBuilder,
              private dialog: MatDialog,
              private destroyRef: DestroyRef) {
    this.filterListFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => this.updateModel());
  }


  get filtersFormArray(): FormArray {
    return this.filterListFormGroup.get('filters') as FormArray;
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  validate(): ValidationErrors | null {
    return this.filterListFormGroup.valid && this.filterListFormGroup.get('filters').value?.length ? null : {
      filterList: {valid: false}
    };
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.filterListFormGroup.disable({emitEvent: false});
    } else {
      this.filterListFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(filters: Array<AlarmRuleFilter>): void {
    const keyFilterControls: Array<AbstractControl> = [];
    if (filters) {
      for (const filter of filters) {
        keyFilterControls.push(this.fb.control(filter, [Validators.required]));
      }
    }
    this.filtersFormArray.clear();
    keyFilterControls.forEach(c => this.filtersFormArray.push(c));
  }

  public removeFilter(index: number) {
    (this.filterListFormGroup.get('filters') as FormArray).removeAt(index);
  }

  public addFilter() {
    const filtersFormArray = this.filterListFormGroup.get('filters') as FormArray;
    this.openFilterDialog(null).subscribe(result => {
      if (result) {
        filtersFormArray.push(this.fb.control(result, [Validators.required]));
      }
    });
  }

  public editFilter(index: number, readonly = false) {
    const filter: AlarmRuleFilter =
      (this.filterListFormGroup.get('filters') as FormArray).at(index).value;
    this.openFilterDialog(filter, readonly).subscribe(result => {
      if (result) {
        (this.filterListFormGroup.get('filters') as FormArray).at(index).patchValue(result);
      }
    });
  }

  private openFilterDialog(filter?: AlarmRuleFilter, readonly = false): Observable<AlarmRuleFilter> {
    const isAdd = !filter;
    if (isAdd) {
      filter = {
        argument: null,
        valueType: EntityKeyValueType.STRING,
        operation: ComplexOperation.AND,
        predicates: []
      };
    }
    return this.dialog.open<AlarmRuleFilterDialogComponent, AlarmRuleFilterDialogData,
      AlarmRuleFilter>(AlarmRuleFilterDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        filter: filter ? deepClone(filter) : null,
        isAdd,
        arguments: this.arguments,
        usedArguments: this.getUsedArguments,
        readonly
      }
    }).afterClosed();
  }

  get getUsedArguments(): Array<string> {
    const filters = this.filterListFormGroup.get('filters').value as Array<AlarmRuleFilter>;
    return filters.length ? filters.map((filter: AlarmRuleFilter) => filter.argument) : [];
  }

  private updateModel() {
    const filters = this.filterListFormGroup.value.filters as Array<AlarmRuleFilter>;
    this.propagateChange(filters);
  }
}
