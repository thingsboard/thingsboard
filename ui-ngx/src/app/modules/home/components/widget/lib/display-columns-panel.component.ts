// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
    this.data.columnsUpdated(this.data.columns);
  }
}
