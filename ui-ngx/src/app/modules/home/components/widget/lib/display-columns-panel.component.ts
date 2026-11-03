// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, Inject, InjectionToken } from '@angular/core';
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

  constructor(@Inject(DISPLAY_COLUMNS_PANEL_DATA) public data: DisplayColumnsPanelData) {
    this.columns = this.data.columns;
  }

  public update() {
    this.data.columnsUpdated(this.columns);
  }
}
