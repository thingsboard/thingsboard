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

import { Component, forwardRef, Input, OnDestroy, OnInit } from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  FormControl,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormArray,
  UntypedFormBuilder,
  UntypedFormGroup,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { Observable, Subject } from 'rxjs';
import {
  ComplexOperation,
  complexOperationTranslationMap,
  EntityKeyValueType
} from '@shared/models/query/query.models';
import { MatDialog } from '@angular/material/dialog';
import { deepClone } from '@core/utils';
import { takeUntil } from 'rxjs/operators';
import {
  AlarmRuleFilterDialogComponent,
  AlarmRuleFilterDialogData
} from "@home/components/alarm-rules/filter/alarm-rule-filter-dialog.component";
import { AlarmRuleFilter, FilterPredicateTypeTranslationMap } from "@shared/models/alarm-rule.models";
import { CalculatedFieldArgument } from "@shared/models/calculated-field.models";
import { UtilsService } from "@core/services/utils.service";

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
export class AlarmRuleFilterListComponent implements ControlValueAccessor, Validator, OnInit, OnDestroy {

  @Input()
  arguments: Record<string, CalculatedFieldArgument>;

  @Input() operation: ComplexOperation = ComplexOperation.AND;

  filterListFormGroup: UntypedFormGroup;
  filtersControl: FormControl;

  complexOperationTranslationMap = complexOperationTranslationMap;
  FilterPredicateTypeTranslationMap = FilterPredicateTypeTranslationMap

  private destroy$ = new Subject<void>();
  private propagateChange = null;

  constructor(private fb: UntypedFormBuilder,
              private utils: UtilsService,
              private dialog: MatDialog) {
  }

  ngOnInit(): void {
    this.filterListFormGroup = this.fb.group({
      filters: this.fb.array([])
    });
    this.filtersControl = this.fb.control(null);

    this.filterListFormGroup.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => this.updateModel());
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get filtersFormArray(): UntypedFormArray {
    return this.filterListFormGroup.get('filters') as UntypedFormArray;
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  validate(): ValidationErrors | null {
    return this.filterListFormGroup.valid && this.filtersControl.valid && this.filterListFormGroup.get('filters').value?.length ? null : {
      filterList: {valid: false}
    };
  }

  writeValue(filters: Array<AlarmRuleFilter>): void {
    if (filters?.length === this.filtersFormArray?.length) {
      this.filtersFormArray.patchValue(filters, {emitEvent: false});
    } else {
      const keyFilterControls: Array<AbstractControl> = [];
      if (filters) {
        for (const filter of filters) {
          keyFilterControls.push(this.fb.control(filter, [Validators.required]));
        }
      }
      this.filterListFormGroup.setControl('filters', this.fb.array(keyFilterControls), {emitEvent: false});
    }
    this.filtersControl.patchValue(filters, {emitEvent: false});
  }

  public removeFilter(index: number) {
    (this.filterListFormGroup.get('filters') as UntypedFormArray).removeAt(index);
  }

  public addFilter() {
    const filtersFormArray = this.filterListFormGroup.get('filters') as UntypedFormArray;
    this.openFilterDialog(null).subscribe(result => {
      if (result) {
        filtersFormArray.push(this.fb.control(result, [Validators.required]));
      }
    });
  }

  public editFilter(index: number) {
    const filter: AlarmRuleFilter =
      (this.filterListFormGroup.get('filters') as UntypedFormArray).at(index).value;
    this.openFilterDialog(filter).subscribe(result => {
      if (result) {
        (this.filterListFormGroup.get('filters') as UntypedFormArray).at(index).patchValue(result);
      }
    });
  }

  private openFilterDialog(filter?: AlarmRuleFilter): Observable<AlarmRuleFilter> {
    const isAdd = !filter;
    if (!filter) {
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
        usedArguments: this.getUsedArguments
      }
    }).afterClosed();
  }

  get getUsedArguments(): Array<string> {
    const filters = this.filterListFormGroup.get('filters').value ?? [];
    return filters.length ? filters.map((filter: AlarmRuleFilter) => filter.argument) : filters;
  }

  private updateModel() {
    const filters: Array<AlarmRuleFilter> = this.filterListFormGroup.getRawValue().filters;
    this.filtersControl.patchValue(filters, {emitEvent: false});
    if (filters.length) {
      this.propagateChange(filters);
    } else {
      this.propagateChange(null);
    }
  }

}
