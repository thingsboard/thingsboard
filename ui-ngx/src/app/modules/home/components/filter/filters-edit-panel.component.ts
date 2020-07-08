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
  styleUrls: ['./filters-edit-panel.component.scss']
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
