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
import { DisplayColumn } from '@home/components/widget/lib/table-widget.models';

export const DISPLAY_COLUMNS_PANEL_DATA = new InjectionToken<any>('DisplayColumnsPanelData');

export interface DisplayColumnsPanelData {
  columns: DisplayColumn[];
  columnsUpdated: (columns: DisplayColumn[]) => void;
}

@Component({
  selector: 'tb-display-columns-panel',
  templateUrl: './display-columns-panel.component.html',
  styleUrls: ['./display-columns-panel.component.scss']
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
