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

import { Component, Inject, SkipSelf } from '@angular/core';
import { ErrorStateMatcher } from '@angular/material/core';
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import {
  AbstractControl,
  FormGroupDirective,
  NgForm,
  UntypedFormArray,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  Validators
} from '@angular/forms';
import { Router } from '@angular/router';
import { DialogComponent } from '@app/shared/components/dialog.component';
import { DatasourceType, Widget } from '@shared/models/widget.models';
import { UtilsService } from '@core/services/utils.service';
import { TranslateService } from '@ngx-translate/core';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { DialogService } from '@core/services/dialog.service';
import { deepClone, isUndefined } from '@core/utils';
import { Filter, Filters, KeyFilterInfo } from '@shared/models/query/query.models';
import { FilterDialogComponent, FilterDialogData } from '@home/components/filter/filter-dialog.component';
import { DashboardUtilsService } from '@core/services/dashboard-utils.service';

export interface FiltersDialogData {
  filters: Filters;
  widgets: Array<Widget>;
  isSingleFilter?: boolean;
  isSingleWidget?: boolean;
  disableAdd?: boolean;
  singleFilter?: Filter;
  customTitle?: string;
}

@Component({
  selector: 'tb-filters-dialog',
  templateUrl: './filters-dialog.component.html',
  providers: [{provide: ErrorStateMatcher, useExisting: FiltersDialogComponent}],
  styleUrls: ['./filters-dialog.component.scss']
})
export class FiltersDialogComponent extends DialogComponent<FiltersDialogComponent, Filters>
  implements ErrorStateMatcher {

  title: string;
  disableAdd: boolean;

  filterToWidgetsMap: {[filterId: string]: Array<string>} = {};

  filterNames: Set<string> = new Set<string>();

  filtersFormGroup: UntypedFormGroup;

  submitted = false;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: FiltersDialogData,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              public dialogRef: MatDialogRef<FiltersDialogComponent, Filters>,
              private fb: UntypedFormBuilder,
              private utils: UtilsService,
              private dashboardUtils: DashboardUtilsService,
              private translate: TranslateService,
              private dialogs: DialogService,
              private dialog: MatDialog) {
    super(store, router, dialogRef);
    this.title = data.customTitle ? data.customTitle : 'filter.filters';
    this.disableAdd = this.data.disableAdd;

    if (data.widgets) {
      let widgetsTitleList: Array<string>;
      if (this.data.isSingleWidget && this.data.widgets.length === 1) {
        const widget = this.data.widgets[0];
        widgetsTitleList = [widget.config.title];
        for (const filterId of Object.keys(this.data.filters)) {
          this.filterToWidgetsMap[filterId] = widgetsTitleList;
        }
      } else {
        this.data.widgets.forEach((widget) => {
          this.dashboardUtils.getWidgetDatasources(widget).forEach((datasource) => {
            if (datasource.type !== DatasourceType.function && datasource.filterId) {
              widgetsTitleList = this.filterToWidgetsMap[datasource.filterId];
              if (!widgetsTitleList) {
                widgetsTitleList = [];
                this.filterToWidgetsMap[datasource.filterId] = widgetsTitleList;
              }
              if (!widgetsTitleList.includes(widget.config.title)) {
                widgetsTitleList.push(widget.config.title);
              }
            }
          });
        });
      }
    }
    const filterControls: Array<AbstractControl> = [];
    for (const filterId of Object.keys(this.data.filters)) {
      const filter = this.data.filters[filterId];
      if (isUndefined(filter.editable)) {
        filter.editable = true;
      }
      this.filterNames.add(filter.filter);
      filterControls.push(this.createFilterFormControl(filterId, filter));
    }

    this.filtersFormGroup = this.fb.group({
      filters: this.fb.array(filterControls)
    });
  }

  private createFilterFormControl(filterId: string, filter: Filter): AbstractControl {
    const filterFormControl = this.fb.group({
      id: [filterId],
      filter: [filter ? filter.filter : null, [Validators.required]],
      keyFilters: [filter ? filter.keyFilters : [], [Validators.required]],
      editable: [filter ? filter.editable : true]
    });
    return filterFormControl;
  }


  filtersFormArray(): UntypedFormArray {
    return this.filtersFormGroup.get('filters') as UntypedFormArray;
  }

  isErrorState(control: UntypedFormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid && this.submitted);
    return originalErrorState || customErrorState;
  }

  removeFilter(index: number) {
    const filter = (this.filtersFormGroup.get('filters').value as any[])[index];
    const widgetsTitleList = this.filterToWidgetsMap[filter.id];
    if (widgetsTitleList) {
      let widgetsListHtml = '';
      for (const widgetTitle of widgetsTitleList) {
        widgetsListHtml += '<br/>\'' + widgetTitle + '\'';
      }
      const message = this.translate.instant('filter.unable-delete-filter-text',
        {filter: filter.filter, widgetsList: widgetsListHtml});
      this.dialogs.alert(this.translate.instant('filter.unable-delete-filter-title'),
        message, this.translate.instant('action.close'), true);
    } else {
      (this.filtersFormGroup.get('filters') as UntypedFormArray).removeAt(index);
      this.filterNames.delete(filter.filter);
      this.filtersFormGroup.markAsDirty();
    }
  }

  private getNextDuplicatedName(filterName: string): string {
    const suffix = ` - ${this.translate.instant('action.copy')} `;
    let counter = 0;
    while (++counter < Number.MAX_SAFE_INTEGER) {
      const newName = `${filterName}${suffix}${counter}`;
      if (!this.filterNames.has(newName)) {
        return newName;
      }
    }

    return null;
  }

  duplicateFilter(index: number) {
    const originalFilter = (this.filtersFormGroup.get('filters').value as any[])[index];
    const newFilterName = this.getNextDuplicatedName(originalFilter.filter);
    if (newFilterName) {
      const duplicatedFilter = deepClone(originalFilter);
      duplicatedFilter.id = this.utils.guid();
      duplicatedFilter.filter = newFilterName;
      (this.filtersFormGroup.get('filters') as UntypedFormArray).
        insert(index + 1, this.createFilterFormControl(duplicatedFilter.id, duplicatedFilter));
      this.filterNames.add(duplicatedFilter.filter);
    }
  }

  public addFilter() {
    this.openFilterDialog(-1);
  }

  public editFilter(index: number) {
    this.openFilterDialog(index);
  }

  private openFilterDialog(index: number) {
    const isAdd = index === -1;
    let filter;
    const filtersArray = this.filtersFormGroup.get('filters').value as any[];
    if (!isAdd) {
      filter = filtersArray[index];
      this.filterNames.delete(filter.filter);
    }
    this.dialog.open<FilterDialogComponent, FilterDialogData,
      Filter>(FilterDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        isAdd,
        filters: filtersArray,
        filter: isAdd ? null : deepClone(filter)
      }
    }).afterClosed().subscribe((result) => {
      if (result) {
        if (isAdd) {
          (this.filtersFormGroup.get('filters') as UntypedFormArray)
            .push(this.createFilterFormControl(result.id, result));
        } else {
          const filterFormControl = (this.filtersFormGroup.get('filters') as UntypedFormArray).at(index);
          filterFormControl.get('filter').patchValue(result.filter);
          filterFormControl.get('editable').patchValue(result.editable);
          filterFormControl.get('keyFilters').patchValue(result.keyFilters);
        }
        this.filterNames.add(result.filter);
        this.filtersFormGroup.markAsDirty();
      }
    });
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  save(): void {
    this.submitted = true;
    const filters: Filters = {};
    const uniqueFilterList: {[filter: string]: string} = {};

    let valid = true;
    let message: string;

    const filtersArray = this.filtersFormGroup.get('filters').value as any[];
    for (const filterValue of filtersArray) {
      const filterId: string = filterValue.id;
      const filter: string = filterValue.filter;
      const keyFilters: Array<KeyFilterInfo> = filterValue.keyFilters;
      const editable: boolean = filterValue.editable;
      if (uniqueFilterList[filter]) {
        valid = false;
        message = this.translate.instant('filter.duplicate-filter-error', {filter});
        break;
      } else if (!keyFilters || !keyFilters.length) {
        valid = false;
        message = this.translate.instant('filter.missing-key-filters-error', {filter});
        break;
      } else {
        uniqueFilterList[filter] = filter;
        filters[filterId] = {id: filterId, filter, keyFilters, editable};
      }
    }
    if (valid) {
      this.dialogRef.close(filters);
    } else {
      this.store.dispatch(new ActionNotificationShow(
        {
          message,
          type: 'error'
        }));
    }
  }
}
