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

import { Component, ElementRef, Input, OnInit, ViewChild } from '@angular/core';
import { WidgetContext } from '@home/models/widget-component.models';
import { ChartType, TbFlotSettings } from '@home/components/widget/lib/flot-widget.models';
import { TbFlot } from '@home/components/widget/lib/flot-widget';
import {
  defaultLegendConfig,
  LegendConfig,
  LegendData,
  LegendPosition,
  widgetType
} from '@shared/models/widget.models';
import { isDefinedAndNotNull } from '@core/utils';

@Component({
    selector: 'tb-flot-widget',
    templateUrl: './flot-widget.component.html',
    styleUrls: [],
    standalone: false
})
export class FlotWidgetComponent implements OnInit {

  @ViewChild('flotElement', {static: true}) flotElement: ElementRef;

  @Input()
  ctx: WidgetContext;

  @Input()
  chartType: ChartType;

  displayLegend: boolean;
  legendConfig: LegendConfig;
  legendData: LegendData;
  isLegendFirst: boolean;
  legendContainerLayoutType: 'flex-row' | 'flex-col';
  legendStyle: {[klass: string]: any};

  public settings: TbFlotSettings;
  private flot: TbFlot;

  constructor() {
  }

  ngOnInit(): void {
    this.ctx.$scope.flotWidget = this;
    this.settings = this.ctx.settings;
    this.chartType = this.chartType || 'line';
    this.configureLegend();
    if (this.ctx.datasources?.length) {
      this.flot = new TbFlot(this.ctx, this.chartType, $(this.flotElement.nativeElement));
    }
  }

  private configureLegend(): void {

    this.displayLegend = isDefinedAndNotNull(this.settings.showLegend) ? this.settings.showLegend
      : false;

    this.legendContainerLayoutType = 'flex-col';

    if (this.displayLegend) {
      this.legendConfig = this.settings.legendConfig || defaultLegendConfig(widgetType.timeseries);
      if (this.ctx.defaultSubscription) {
        this.legendData = this.ctx.defaultSubscription.legendData;
      } else {
        this.legendData = {
          keys: [],
          data: []
        };
      }
      if (this.legendConfig.position === LegendPosition.top ||
        this.legendConfig.position === LegendPosition.bottom) {
        this.legendContainerLayoutType = 'flex-col';
        this.isLegendFirst = this.legendConfig.position === LegendPosition.top;
      } else {
        this.legendContainerLayoutType = 'flex-row';
        this.isLegendFirst = this.legendConfig.position === LegendPosition.left;
      }
      switch (this.legendConfig.position) {
        case LegendPosition.top:
          this.legendStyle = {
            paddingBottom: '8px',
            maxHeight: '50%',
            overflowY: 'auto'
          };
          break;
        case LegendPosition.bottom:
          this.legendStyle = {
            paddingTop: '8px',
            maxHeight: '50%',
            overflowY: 'auto'
          };
          break;
        case LegendPosition.left:
          this.legendStyle = {
            paddingRight: '0px',
            maxWidth: '50%',
            overflowY: 'auto'
          };
          break;
        case LegendPosition.right:
          this.legendStyle = {
            paddingLeft: '0px',
            maxWidth: '50%',
            overflowY: 'auto'
          };
          break;
      }
    }
  }

  public onLegendKeyHiddenChange(index: number) {
    for (const id of Object.keys(this.ctx.subscriptions)) {
      const subscription = this.ctx.subscriptions[id];
      subscription.updateDataVisibility(index);
    }
  }

  public onDataUpdated() {
    this.flot.update();
  }

  public onLatestDataUpdated() {
    this.flot.latestDataUpdate();
  }

  public onResize() {
    this.flot.resize();
  }

  public onEditModeChanged() {
    this.flot.checkMouseEvents();
  }

  public onDestroy() {
    this.flot.destroy();
  }

}
