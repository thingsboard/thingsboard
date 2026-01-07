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

import { TbLatestChart } from '@home/components/widget/lib/chart/latest-chart';
import { radarChartDefaultSettings, RadarChartSettings } from '@home/components/widget/lib/chart/radar-chart.models';
import { WidgetContext } from '@home/models/widget-component.models';
import { DeepPartial } from '@shared/models/common';
import { Renderer2 } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import {
  ChartFillType,
  createChartTextStyle,
  createRadialOpacityGradient,
  toAnimationOption
} from '@home/components/widget/lib/chart/chart.models';
import { isDefinedAndNotNull } from '@core/utils';
import { ComponentStyle } from '@shared/models/widget-settings.models';
import { AreaStyleOption, SeriesLabelOption } from 'echarts/types/src/util/types';
import { RadarIndicatorOption } from 'echarts/types/src/coord/radar/RadarModel';
import { DataKey } from '@shared/models/widget.models';
import { LatestChartDataItem } from '@home/components/widget/lib/chart/latest-chart.models';

export class TbRadarChart extends TbLatestChart<RadarChartSettings> {

  constructor(ctx: WidgetContext,
              inputSettings: DeepPartial<RadarChartSettings>,
              chartElement: HTMLElement,
              renderer: Renderer2,
              translate: TranslateService,
              autoResize = true) {

    super(ctx, inputSettings, chartElement, renderer, translate, autoResize);
  }

  protected defaultSettings(): RadarChartSettings {
    return radarChartDefaultSettings;
  }

  protected prepareLatestChartOption() {

    const axisNameStyle = createChartTextStyle(this.settings.axisLabelFont,
      '#000', false, 'axis.label');
    const axisTickLabelStyle = createChartTextStyle(this.settings.axisTickLabelFont,
      this.settings.axisTickLabelColor, false, 'axis.tickLabel');

    this.latestChartOption.radar = [{
      shape: this.settings.shape,
      radius: '85%',
      indicator: [{}],
      axisName: {
        show: this.settings.axisShowLabel,
        fontStyle: axisNameStyle.fontStyle,
        fontWeight: axisNameStyle.fontWeight,
        fontFamily: axisNameStyle.fontFamily,
        fontSize: axisNameStyle.fontSize
      },
      axisLabel: {
        show: this.settings.axisShowTickLabels,
        color: axisTickLabelStyle.color,
        fontStyle: axisTickLabelStyle.fontStyle,
        fontWeight: axisTickLabelStyle.fontWeight,
        fontFamily: axisTickLabelStyle.fontFamily,
        fontSize: axisTickLabelStyle.fontSize,
        formatter: (value: any) => this.valueFormatter.format(value)
      }
    }];

    let labelStyle: ComponentStyle = {};
    if (this.settings.showLabel) {
      labelStyle = createChartTextStyle(this.settings.labelFont, this.settings.labelColor, false, 'series.label');
    }

    const labelOption: SeriesLabelOption = {
      show: this.settings.showLabel,
      position: this.settings.labelPosition,
      formatter: (params) => {
        let result = '';
        if (isDefinedAndNotNull(params.value)) {
          result = this.valueFormatter.format(params.value);
        }
        return `{value|${result}}`;
      },
      rich: {
        value: labelStyle
      }
    };

    let areaStyleOption: AreaStyleOption;
    if (this.settings.fillAreaSettings.type !== ChartFillType.none) {
      areaStyleOption = {};
      if (this.settings.fillAreaSettings.type === ChartFillType.opacity) {
        areaStyleOption.opacity = this.settings.fillAreaSettings.opacity;
      } else if (this.settings.fillAreaSettings.type === ChartFillType.gradient) {
        areaStyleOption.opacity = 1;
        areaStyleOption.color = createRadialOpacityGradient(this.settings.color, this.settings.fillAreaSettings.gradient);
      }
    }

    this.latestChartOption.series = [
      {
        type: 'radar',
        data: [{
          id: 1,
          itemStyle: {
            color: this.settings.color
          },
          label: labelOption,
          symbol: this.settings.showPoints ? this.settings.pointShape : 'none',
          symbolSize: this.settings.pointSize,
          lineStyle: {
            width: this.settings.showLine ? this.settings.lineWidth : 0,
            type: this.settings.lineType
          },
          areaStyle: areaStyleOption,
          value: []
        }],
        emphasis: {
          focus: 'self'
        },
        ...toAnimationOption(this.ctx, this.settings.animation)
      }
    ];
  }

  protected doUpdateSeriesData() {
    const indicator: RadarIndicatorOption[] = [];
    const value: number[] = [];
    for (const dataItem of this.dataItems) {
      if (dataItem.enabled && dataItem.hasValue) {
        indicator.push({
          name: dataItem.dataKey.label,
          color: dataItem.dataKey.color,
        });
        value.push(dataItem.value);
      }
    }
    if (!indicator.length) {
      indicator.push({});
    }
    if (this.settings.normalizeAxes && indicator.length > 1) {
      const maxDataItem = this.findMaxDataItem(this.dataItems);
      indicator.map(value => value.max = maxDataItem.value);
    }
    this.latestChartOption.radar[0].indicator = indicator;
    this.latestChartOption.series[0].data[0].value = value;
  }

  private findMaxDataItem(array: LatestChartDataItem[]): LatestChartDataItem {
    if (!array || array.length === 0) return null;
    return array.reduce((maxObj, currentObj) => {
      return currentObj.value > maxObj.value ? currentObj : maxObj;
    }, array[0]);
  }

  protected forceRedrawOnResize(): boolean {
    return true;
  }

  public keyEnter(dataKey: DataKey): void {}

  public keyLeave(dataKey: DataKey): void {}

  public toggleKey(dataKey: DataKey): void {
    const enable = dataKey.hidden;
    const dataItem = this.dataItems.find(d => d.dataKey === dataKey);
    if (dataItem) {
      dataItem.enabled = enable;
      this.updateSeriesData();
      dataKey.hidden = !enable;
    }
  }
}
