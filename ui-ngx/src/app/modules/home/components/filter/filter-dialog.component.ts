// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, Inject, OnInit, SkipSelf } from '@angular/core';
import { ErrorStateMatcher } from '@angular/material/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import {
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
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
  filters: Filters | Array<Filter>;
  filter?: Filter;
}

@Component({
    selector: 'tb-filter-dialog',
    templateUrl: './filter-dialog.component.html',
    providers: [{ provide: ErrorStateMatcher, useExisting: FilterDialogComponent }],
    styleUrls: ['./filter-dialog.component.scss'],
    standalone: false
})
export class FilterDialogComponent extends DialogComponent<FilterDialogComponent, Filter>
  implements OnInit, ErrorStateMatcher {

  isAdd: boolean;
  filters: Array<Filter>;

  filter: Filter;

  filterFormGroup: UntypedFormGroup;

  submitted = false;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: FilterDialogData,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              public dialogRef: MatDialogRef<FilterDialogComponent, Filter>,
              private fb: UntypedFormBuilder,
              private utils: UtilsService,
              public translate: TranslateService) {
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

    this.filterFormGroup = this.fb.group({
      filter: [this.filter.filter, [this.validateDuplicateFilterName(), Validators.required]],
      editable: [this.filter.editable],
      keyFilters: [this.filter.keyFilters, Validators.required]
    });
  }

  validateDuplicateFilterName(): ValidatorFn {
    return (c: UntypedFormControl) => {
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

  ngOnInit(): void {
  }

  isErrorState(control: UntypedFormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid && this.submitted);
    return originalErrorState || customErrorState;
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  save(): void {
    this.submitted = true;
    this.filter.filter = this.filterFormGroup.get('filter').value.trim();
    this.filter.editable = this.filterFormGroup.get('editable').value;
    this.filter.keyFilters = this.filterFormGroup.get('keyFilters').value;
    if (this.isAdd) {
      this.filter.id = this.utils.guid();
    }
    this.dialogRef.close(this.filter);
  }
}
