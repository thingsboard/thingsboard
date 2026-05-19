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

import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormBuilder, FormControl, FormGroup, ValidatorFn, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { DialogComponent } from '@app/shared/components/dialog.component';
import { UtilsService } from '@core/services/utils.service';
import {
  ComplexOperation,
  complexOperationTranslationMap,
  Filter,
  FilterInfo,
  Filters
} from '@shared/models/query/query.models';
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import { FormControlsFrom } from '@shared/models/tenant.model';

export interface FilterDialogData {
  isAdd: boolean;
  filters: Filters | Array<Filter>;
  filter?: Filter;
}

@Component({
  selector: 'tb-filter-dialog',
  templateUrl: './filter-dialog.component.html',
  standalone: false
})
export class FilterDialogComponent extends DialogComponent<FilterDialogComponent, Filter> {

  isAdd: boolean;

  filterFormGroup: FormGroup<FormControlsFrom<FilterInfo>>;

  ComplexOperation = ComplexOperation;
  complexOperationTranslationMap = complexOperationTranslationMap;
  allowKeyFiltersOrConditions: boolean;

  private readonly filter: Filter;
  private filters: Array<Filter>;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: FilterDialogData,
              protected dialogRef: MatDialogRef<FilterDialogComponent, Filter>,
              private fb: FormBuilder,
              private utils: UtilsService) {
    super(store, router, dialogRef);
    this.isAdd = data.isAdd;
    if (Array.isArray(data.filters)) {
      this.filters = data.filters;
    } else {
      this.filters = [];
      for (const filterId of Object.keys(data.filters)) {
        this.filters.push(data.filters[filterId]);
      }
    }
    if (this.isAdd && !this.data.filter) {
      this.filter = {
        id: null,
        filter: '',
        keyFilters: [],
        editable: true
      };
    } else {
      this.filter = data.filter;
    }
    this.allowKeyFiltersOrConditions = getCurrentAuthState(this.store).allowKeyFiltersOrConditions !== false;

    this.filterFormGroup = this.fb.group({
      filter: [this.filter.filter, [this.validateDuplicateFilterName(), Validators.required]],
      editable: [this.filter.editable],
      keyFilters: [this.filter.keyFilters, Validators.required],
      keyFiltersOperation: [this.filter.keyFiltersOperation ?? ComplexOperation.AND]
    });
    if (!this.allowKeyFiltersOrConditions) {
      if (this.filter.keyFiltersOperation === ComplexOperation.OR) {
        this.filterFormGroup.controls.keyFiltersOperation.setValue(ComplexOperation.AND);
        this.filterFormGroup.markAsDirty();
      }
      this.filterFormGroup.controls.keyFiltersOperation.disable();
    }
  }

  validateDuplicateFilterName(): ValidatorFn {
    return (c: FormControl<string>) => {
      const newFilter = c.value.trim();
      const found = this.filters.find((filter) => filter.filter === newFilter);
      if (found) {
        if (this.isAdd || this.filter.id !== found.id) {
          return {
            duplicateFilterName: {
              valid: false
            }
          };
        }
      }
      return null;
    };
  }

  onEditableChange(event: MouseEvent): void {
    event.stopPropagation();
    const editableControl = this.filterFormGroup.controls.editable;
    editableControl.setValue(!editableControl.value);
    editableControl.markAsDirty();
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  save(): void {
    const {filter, editable, keyFilters, keyFiltersOperation} = this.filterFormGroup.getRawValue();
    this.filter.filter = filter.trim();
    this.filter.editable = editable;
    this.filter.keyFilters = keyFilters;
    this.filter.keyFiltersOperation = keyFiltersOperation;
    if (this.isAdd) {
      this.filter.id = this.utils.guid();
    }
    this.dialogRef.close(this.filter);
  }
}
