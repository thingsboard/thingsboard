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

import { Component, Inject, OnInit, SkipSelf } from '@angular/core';
import { ErrorStateMatcher } from '@angular/material/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import {
  FormBuilder,
  FormControl,
  FormGroup,
  FormGroupDirective,
  NgForm,
  ValidatorFn,
  Validators
} from '@angular/forms';
import { Router } from '@angular/router';
import { DialogComponent } from '@app/shared/components/dialog.component';
import { UtilsService } from '@core/services/utils.service';
import { TranslateService } from '@ngx-translate/core';
import { Filter, Filters } from '@shared/models/query/query.models';

export interface FilterDialogData {
  isAdd: boolean;
  userMode: boolean;
  filters: Filters | Array<Filter>;
  filter?: Filter;
}

@Component({
  selector: 'tb-filter-dialog',
  templateUrl: './filter-dialog.component.html',
  providers: [{provide: ErrorStateMatcher, useExisting: FilterDialogComponent}],
  styleUrls: ['./filter-dialog.component.scss']
})
export class FilterDialogComponent extends DialogComponent<FilterDialogComponent, Filter>
  implements OnInit, ErrorStateMatcher {

  isAdd: boolean;
  userMode: boolean;
  filters: Array<Filter>;

  filter: Filter;

  filterFormGroup: FormGroup;

  submitted = false;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: FilterDialogData,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              public dialogRef: MatDialogRef<FilterDialogComponent, Filter>,
              private fb: FormBuilder,
              private utils: UtilsService,
              public translate: TranslateService) {
    super(store, router, dialogRef);
    this.isAdd = data.isAdd;
    this.userMode = data.userMode;
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

    this.filterFormGroup = this.fb.group({
      filter: [this.filter.filter, [this.validateDuplicateFilterName(), Validators.required]],
      editable: [this.filter.editable],
      keyFilters: [this.filter.keyFilters, Validators.required]
    });
  }

  validateDuplicateFilterName(): ValidatorFn {
    return (c: FormControl) => {
      const newFilter = c.value;
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

  ngOnInit(): void {
  }

  isErrorState(control: FormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid && this.submitted);
    return originalErrorState || customErrorState;
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  save(): void {
    this.submitted = true;
    this.filter.filter = this.filterFormGroup.get('filter').value;
    this.filter.editable = this.filterFormGroup.get('editable').value;
    this.filter.keyFilters = this.filterFormGroup.get('keyFilters').value;
    if (this.isAdd) {
      this.filter.id = this.utils.guid();
    }
    this.dialogRef.close(this.filter);
  }
}
