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
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import {
  ComplexFilterPredicate,
  ComplexFilterPredicateInfo,
  EntityKeyValueType
} from '@shared/models/query/query.models';
import { MatDialog } from '@angular/material/dialog';
import {
  ComplexFilterPredicateDialogComponent,
  ComplexFilterPredicateDialogData
} from '@home/components/filter/complex-filter-predicate-dialog.component';
import { deepClone } from '@core/utils';

@Component({
  selector: 'tb-complex-filter-predicate',
  templateUrl: './complex-filter-predicate.component.html',
  styleUrls: [],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ComplexFilterPredicateComponent),
      multi: true
    }
  ]
})
export class ComplexFilterPredicateComponent implements ControlValueAccessor, OnInit {

  @Input() disabled: boolean;

  @Input() valueType: EntityKeyValueType;

  @Input() key: string;

  @Input() displayUserParameters = true;

  @Input() allowUserDynamicSource = true;

  private propagateChange = null;

  private complexFilterPredicate: ComplexFilterPredicateInfo;

  constructor(private dialog: MatDialog) {
  }

  ngOnInit(): void {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState?(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  writeValue(predicate: ComplexFilterPredicateInfo): void {
    this.complexFilterPredicate = predicate;
  }

  public openComplexFilterDialog() {
    this.dialog.open<ComplexFilterPredicateDialogComponent, ComplexFilterPredicateDialogData,
      ComplexFilterPredicateInfo>(ComplexFilterPredicateDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        complexPredicate: this.disabled ? this.complexFilterPredicate : deepClone(this.complexFilterPredicate),
        readonly: this.disabled,
        valueType: this.valueType,
        isAdd: false,
        key: this.key,
        displayUserParameters: this.displayUserParameters,
        allowUserDynamicSource: this.allowUserDynamicSource
      }
    }).afterClosed().subscribe(
      (result) => {
        if (result) {
          this.complexFilterPredicate = result;
          this.updateModel();
        }
      }
    );
  }

  private updateModel() {
    this.propagateChange(this.complexFilterPredicate);
  }

}
