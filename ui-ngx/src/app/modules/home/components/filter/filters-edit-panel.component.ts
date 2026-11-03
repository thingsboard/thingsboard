// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, Inject, InjectionToken } from '@angular/core';
import { IAliasController } from '@core/api/widget-api.models';
import { Filter, FilterInfo } from '@shared/models/query/query.models';
import { MatDialog } from '@angular/material/dialog';
import { deepClone } from '@core/utils';
import { UserFilterDialogComponent, UserFilterDialogData } from '@home/components/filter/user-filter-dialog.component';

export const FILTER_EDIT_PANEL_DATA = new InjectionToken<any>('FiltersEditPanelData');

export interface FiltersEditPanelData {
  aliasController: IAliasController;
  filtersInfo: {[filterId: string]: FilterInfo};
}

@Component({
    selector: 'tb-filters-edit-panel',
    templateUrl: './filters-edit-panel.component.html',
    styleUrls: ['./filters-edit-panel.component.scss'],
    standalone: false
})
export class FiltersEditPanelComponent {

  filtersInfo: {[filterId: string]: FilterInfo};

  constructor(@Inject(FILTER_EDIT_PANEL_DATA) public data: FiltersEditPanelData,
              private dialog: MatDialog) {
    this.filtersInfo = this.data.filtersInfo;
  }

  public editFilter(filterId: string, filter: FilterInfo) {
    const singleFilter: Filter = {id: filterId, ...deepClone(filter)};
    this.dialog.open<UserFilterDialogComponent, UserFilterDialogData,
      Filter>(UserFilterDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        filter: singleFilter
      }
    }).afterClosed().subscribe(
      (result) => {
        if (result) {
          this.filtersInfo[result.id] = result;
          this.data.aliasController.updateUserFilter(result);
        }
      });
  }
}
