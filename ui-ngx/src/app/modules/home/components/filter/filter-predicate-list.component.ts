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

import { Component, forwardRef, Inject, Input, OnDestroy, OnInit } from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  UntypedFormArray,
  UntypedFormBuilder,
  UntypedFormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { Observable, of, Subject } from 'rxjs';
import {
  ComplexFilterPredicateInfo,
  ComplexOperation,
  complexOperationTranslationMap,
  createDefaultFilterPredicateInfo,
  EntityKeyValueType,
  KeyFilterPredicateInfo
} from '@shared/models/query/query.models';
import { MatDialog } from '@angular/material/dialog';
import { map, takeUntil } from 'rxjs/operators';
import { ComponentType } from '@angular/cdk/portal';
import { COMPLEX_FILTER_PREDICATE_DIALOG_COMPONENT_TOKEN } from '@home/components/tokens';
import { ComplexFilterPredicateDialogData } from '@home/components/filter/filter-component.models';

@Component({
    selector: 'tb-filter-predicate-list',
    templateUrl: './filter-predicate-list.component.html',
    styleUrls: ['./filter-predicate-list.component.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => FilterPredicateListComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => FilterPredicateListComponent),
            multi: true
        }
    ],
    standalone: false
})
export class FilterPredicateListComponent implements ControlValueAccessor, Validator, OnInit, OnDestroy {

  @Input() disabled: boolean;

  @Input() valueType: EntityKeyValueType;

  @Input() key: string;

  @Input() operation: ComplexOperation = ComplexOperation.AND;

  @Input() displayUserParameters = true;

  @Input() allowUserDynamicSource = true;

  @Input() onlyUserDynamicSource = false;

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

  writeValue(predicates: Array<KeyFilterPredicateInfo>): void {
    if (predicates.length === this.predicatesFormArray.length) {
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
    const predicate = createDefaultFilterPredicateInfo(this.valueType, complex);
    let observable: Observable<KeyFilterPredicateInfo>;
    if (complex) {
      observable = this.openComplexFilterDialog(predicate);
    } else {
      observable = of(predicate);
    }
    observable.subscribe((result) => {
      if (result) {
        predicatesFormArray.push(this.fb.control(result, [Validators.required]));
      }
    });
  }

  private openComplexFilterDialog(predicate: KeyFilterPredicateInfo): Observable<KeyFilterPredicateInfo> {
    return this.dialog.open<any, ComplexFilterPredicateDialogData,
      ComplexFilterPredicateInfo>(this.complexFilterPredicateDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        complexPredicate: predicate.keyFilterPredicate as ComplexFilterPredicateInfo,
        readonly: this.disabled,
        valueType: this.valueType,
        key: this.key,
        isAdd: true,
        displayUserParameters: this.displayUserParameters,
        allowUserDynamicSource: this.allowUserDynamicSource,
        onlyUserDynamicSource: this.onlyUserDynamicSource
      }
    }).afterClosed().pipe(
      map((result) => {
        if (result) {
          predicate.keyFilterPredicate = result;
          return predicate;
        } else {
          return null;
        }
      })
    );
  }

  private updateModel() {
    const predicates: Array<KeyFilterPredicateInfo> = this.filterListFormGroup.getRawValue().predicates;
    if (predicates.length) {
      this.propagateChange(predicates);
    } else {
      this.propagateChange(null);
    }
  }
}
