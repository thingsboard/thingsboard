// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { LegendConfig, LegendData, LegendDirection, LegendKey, LegendPosition } from '@shared/models/widget.models';
import { UtilsService } from '@core/services/utils.service';

@Component({
    selector: 'tb-legend',
    templateUrl: './legend.component.html',
    styleUrls: ['./legend.component.scss'],
    standalone: false
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

  constructor(private utils: UtilsService) {
  }

  ngOnInit(): void {
    this.displayHeader = this.legendConfig.showMin === true ||
      this.legendConfig.showMax === true ||
      this.legendConfig.showAvg === true ||
      this.legendConfig.showTotal === true ||
      this.legendConfig.showLatest === true;

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
    try {
      let keys = this.legendData.keys;
      if (this.legendConfig.sortDataKeys) {
        keys = this.legendData.keys.sort((key1, key2) => key1.dataKey.label.localeCompare(key2.dataKey.label));
      }
      return keys.filter(legendKey => this.legendData.keys[legendKey.dataIndex].dataKey.inLegend);
    } catch (e) {}
  }

  getDataKeyLabel(label: string): string {
    return this.utils.customTranslation(label, label);
  }

}
