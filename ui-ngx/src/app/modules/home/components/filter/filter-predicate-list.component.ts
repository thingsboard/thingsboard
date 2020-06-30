///
/// Copyright © 2016-2020 The Thingsboard Authors
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
import {
  AbstractControl,
  ControlValueAccessor,
  FormArray,
  FormBuilder,
  FormGroup,
  NG_VALUE_ACCESSOR,
  Validators
} from '@angular/forms';
import { Observable, of, Subscription } from 'rxjs';
import {
  ComplexFilterPredicate,
  createDefaultFilterPredicate,
  EntityKeyValueType,
  KeyFilterPredicate
} from '@shared/models/query/query.models';
import {
  ComplexFilterPredicateDialogComponent,
  ComplexFilterPredicateDialogData
} from '@home/components/filter/complex-filter-predicate-dialog.component';
import { MatDialog } from '@angular/material/dialog';

@Component({
  selector: 'tb-filter-predicate-list',
  templateUrl: './filter-predicate-list.component.html',
  styleUrls: [],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => FilterPredicateListComponent),
      multi: true
    }
  ]
})
export class FilterPredicateListComponent implements ControlValueAccessor, OnInit {

  @Input() disabled: boolean;

  @Input() userMode: boolean;

  @Input() valueType: EntityKeyValueType;

  filterListFormGroup: FormGroup;

  private propagateChange = null;

  private valueChangeSubscription: Subscription = null;

  constructor(private fb: FormBuilder,
              private dialog: MatDialog) {
  }

  ngOnInit(): void {
    this.filterListFormGroup = this.fb.group({});
    this.filterListFormGroup.addControl('predicates',
      this.fb.array([]));
  }

  predicatesFormArray(): FormArray {
    return this.filterListFormGroup.get('predicates') as FormArray;
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

  writeValue(predicates: Array<KeyFilterPredicate>): void {
    if (this.valueChangeSubscription) {
      this.valueChangeSubscription.unsubscribe();
    }
    const predicateControls: Array<AbstractControl> = [];
    if (predicates) {
      for (const predicate of predicates) {
        predicateControls.push(this.fb.control(predicate, [Validators.required]));
      }
    }
    this.filterListFormGroup.setControl('predicates', this.fb.array(predicateControls));
    this.valueChangeSubscription = this.filterListFormGroup.valueChanges.subscribe(() => {
      this.updateModel();
    });
    if (this.disabled) {
      this.filterListFormGroup.disable({emitEvent: false});
    } else {
      this.filterListFormGroup.enable({emitEvent: false});
    }
  }

  public removePredicate(index: number) {
    (this.filterListFormGroup.get('predicates') as FormArray).removeAt(index);
  }

  public addPredicate(complex: boolean) {
    const predicatesFormArray = this.filterListFormGroup.get('predicates') as FormArray;
    const predicate = createDefaultFilterPredicate(this.valueType, complex);
    let observable: Observable<KeyFilterPredicate>;
    if (complex) {
      observable = this.openComplexFilterDialog(predicate as ComplexFilterPredicate);
    } else {
      observable = of(predicate);
    }
    observable.subscribe((result) => {
      if (result) {
        predicatesFormArray.push(this.fb.control(result, [Validators.required]));
      }
    });
  }

  private openComplexFilterDialog(predicate: ComplexFilterPredicate): Observable<KeyFilterPredicate> {
    return this.dialog.open<ComplexFilterPredicateDialogComponent, ComplexFilterPredicateDialogData,
      ComplexFilterPredicate>(ComplexFilterPredicateDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        complexPredicate: predicate,
        disabled: this.disabled,
        userMode: this.userMode,
        valueType: this.valueType
      }
    }).afterClosed();
  }

  private updateModel() {
    const predicates: Array<KeyFilterPredicate> = this.filterListFormGroup.getRawValue().predicates;
    if (predicates.length) {
      this.propagateChange(predicates);
    } else {
      this.propagateChange(null);
    }
  }
}
