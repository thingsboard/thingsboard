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

import { Component, Inject, InjectionToken } from '@angular/core';
import { isDefinedAndNotNull } from '@core/utils';
import { SelectableColumnsPipe } from '@shared/public-api';
import { DisplayColumn } from '@home/components/widget/lib/table-widget.models';

export const DISPLAY_COLUMNS_PANEL_DATA = new InjectionToken<any>('DisplayColumnsPanelData');

export interface DisplayColumnsPanelData {
  columns: DisplayColumn[];
  columnsUpdated: (columns: DisplayColumn[]) => void;
}

@Component({
    selector: 'tb-display-columns-panel',
    templateUrl: './display-columns-panel.component.html',
    styleUrls: ['./display-columns-panel.component.scss'],
    standalone: false
})
export class DisplayColumnsPanelComponent {

  columns: DisplayColumn[];

  constructor(@Inject(DISPLAY_COLUMNS_PANEL_DATA) public data: DisplayColumnsPanelData,
              private selectableColumnsPipe: SelectableColumnsPipe ) {
    this.columns = this.selectableColumnsPipe.transform(this.data.columns);
  }

  get allColumnsVisible(): boolean {
    return isDefinedAndNotNull(this.columns) && this.columns.every(column => column.display);
  }

  get someColumnsVisible(): boolean {
    const filtredColumns = this.columns.filter(item => item.display);
    return filtredColumns.length !== 0 && this.columns.length !== filtredColumns.length;
  }

  public toggleAllColumns(event: any): void {
    const isChecked = event.checked;
    
    this.columns.forEach(column => {
      column.display = isChecked;
    });
    
    this.update();
  }

  public update() {
    this.data.columnsUpdated(this.columns);
  }
}
