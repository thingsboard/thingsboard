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

import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { LegendConfig, LegendData, LegendDirection, LegendKey, LegendPosition } from '@shared/models/widget.models';

@Component({
  selector: 'tb-legend',
  templateUrl: './legend.component.html',
  styleUrls: ['./legend.component.scss']
})
export class LegendComponent implements OnInit {

  @Input()
  legendConfig: LegendConfig;

  @Input()
  legendData: LegendData;

  @Output()
  legendKeyHiddenChange = new EventEmitter<number>();

  displayHeader: boolean;

  isHorizontal: boolean;

  isRowDirection: boolean;

  ngOnInit(): void {
    this.displayHeader = this.legendConfig.showMin === true ||
      this.legendConfig.showMax === true ||
      this.legendConfig.showAvg === true ||
      this.legendConfig.showTotal === true;

    this.isHorizontal = this.legendConfig.position === LegendPosition.bottom ||
      this.legendConfig.position === LegendPosition.top;

    this.isRowDirection = this.legendConfig.direction === LegendDirection.row;
  }

  toggleHideData(index: number) {
    const dataKey = this.legendData.keys.find(key => key.dataIndex === index).dataKey;
    if (!dataKey.settings.disableDataHiding) {
      dataKey.hidden = !dataKey.hidden;
      this.legendKeyHiddenChange.emit(index);
    }
  }

  legendKeys(): LegendKey[] {
    let keys = this.legendData.keys;
    if (this.legendConfig.sortDataKeys) {
      keys = this.legendData.keys.sort((key1, key2) => key1.dataKey.label.localeCompare(key2.dataKey.label));
    }
    return keys.filter(legendKey => this.legendData.keys[legendKey.dataIndex].dataKey.inLegend);
  }

}
