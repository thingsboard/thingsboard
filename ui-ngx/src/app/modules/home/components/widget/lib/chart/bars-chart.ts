///
/// Copyright © 2016-2025 The Thingsboard Authors
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
import { barsChartDefaultSettings, BarsChartSettings } from '@home/components/widget/lib/chart/bars-chart.models';
import { WidgetContext } from '@home/models/widget-component.models';
import { DeepPartial } from '@shared/models/common';
import { PieChartSettings } from '@home/components/widget/lib/chart/pie-chart.models';
import { Renderer2 } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { ComponentStyle } from '@shared/models/widget-settings.models';
import { LinearGradientObject } from 'zrender/lib/graphic/LinearGradient';
import tinycolor from 'tinycolor2';
import { BarDataItemOption, BarSeriesLabelOption } from 'echarts/types/src/chart/bar/BarSeries';
import { isDefinedAndNotNull } from '@core/utils';
import {
  ChartFillType,
  ChartLabelPosition,
  createChartTextStyle,
  createLinearOpacityGradient,
  toAnimationOption
} from '@home/components/widget/lib/chart/chart.models';
import { ValueAxisBaseOption } from 'echarts/types/src/coord/axisCommonTypes';
import { RadiusAxisOption, YAXisOption } from 'echarts/types/dist/shared';

export class TbBarsChart extends TbLatestChart<BarsChartSettings> {

  constructor(ctx: WidgetContext,
              inputSettings: DeepPartial<PieChartSettings>,
              chartElement: HTMLElement,
              renderer: Renderer2,
              translate: TranslateService,
              autoResize = true) {

    super(ctx, inputSettings, chartElement, renderer, translate, autoResize);
  }

  protected defaultSettings(): BarsChartSettings {
    return barsChartDefaultSettings;
  }

  protected prepareLatestChartOption() {
    let labelStyle: ComponentStyle = {};
    if (this.settings.barSettings.showLabel) {
      labelStyle = createChartTextStyle(this.settings.barSettings.labelFont,
        this.settings.barSettings.labelColor, false, 'series.label', false);
    }
    const labelOption: BarSeriesLabelOption = {
      show: this.settings.barSettings.showLabel,
      position: this.settings.barSettings.labelPosition,
      formatter: (params) => `{label|${params.name}}`,
      rich: {
        label: labelStyle
      }
    };
    if (this.settings.barSettings.enableLabelBackground) {
      labelOption.backgroundColor = this.settings.barSettings.labelBackground;
      labelOption.padding = [4, 5];
      labelOption.borderRadius = 4;
    }
    this.latestChartOption.series = [
      {
        type: 'bar',
        barWidth: isDefinedAndNotNull(this.settings.barSettings.barWidth) ? this.settings.barSettings.barWidth + '%' : undefined,
        itemStyle: {
          borderWidth: this.settings.barSettings.showBorder ? this.settings.barSettings.borderWidth : 0
        },
        emphasis: {
          focus: 'self'
        },
        coordinateSystem: this.settings.polar ? 'polar' : 'cartesian2d',
        label: labelOption,
        ...toAnimationOption(this.ctx, this.settings.animation)
      }
    ];

    const axisTickLabelStyle = createChartTextStyle(this.settings.axisTickLabelFont,
      this.settings.axisTickLabelColor, false, 'axis.tickLabel');
    const valueAxis: ValueAxisBaseOption = {
      type: 'value',
      min: this.settings.axisMin,
      max: this.settings.axisMax,
      axisLabel: {
        color: axisTickLabelStyle.color,
        fontStyle: axisTickLabelStyle.fontStyle,
        fontWeight: axisTickLabelStyle.fontWeight,
        fontFamily: axisTickLabelStyle.fontFamily,
        fontSize: axisTickLabelStyle.fontSize,
        formatter: (value: any) => this.valueFormatter.format(value)
      }
    };
    if (this.settings.polar) {
      this.latestChartOption.polar = {
        radius: '100%'
      };
      this.latestChartOption.radiusAxis = valueAxis as RadiusAxisOption;
      this.latestChartOption.angleAxis = {
        type: 'category',
        data: [],
        startAngle: this.settings.angleAxisStartAngle
      };
    } else {
      let minTop = 0;
      let minBottom = 0;
      if (this.settings.barSettings.showLabel) {
        if (this.settings.barSettings.labelPosition === ChartLabelPosition.top) {
          minTop = this.settings.barSettings.labelFont.size;
        } else if (this.settings.barSettings.labelPosition === ChartLabelPosition.bottom) {
          minBottom = this.settings.barSettings.labelFont.size;
        }
      }
      this.latestChartOption.grid = [{
        containLabel: true,
        top: minTop,
        bottom: minBottom,
        left: 0,
        right: 0
      }];
      this.latestChartOption.xAxis = {
        type: 'category',
        data: []
      };
      this.latestChartOption.yAxis = valueAxis as YAXisOption;
    }
  }

  protected doUpdateSeriesData() {
    const seriesData: BarDataItemOption[] = [];
    for (const dataItem of this.dataItems) {
      if (dataItem.enabled && dataItem.hasValue) {
        const barSettings = this.settings.barSettings;
        let borderRadius: number[];
        if (dataItem.value < 0) {
          borderRadius = [0, 0, barSettings.borderRadius, barSettings.borderRadius];
        } else {
          borderRadius = [barSettings.borderRadius, barSettings.borderRadius, 0, 0];
        }
        let barColor: string | LinearGradientObject;
        if (barSettings.backgroundSettings.type === ChartFillType.none) {
          barColor = dataItem.dataKey.color;
        } else if (barSettings.backgroundSettings.type === ChartFillType.opacity) {
          barColor = tinycolor(dataItem.dataKey.color).setAlpha(barSettings.backgroundSettings.opacity).toRgbString();
        } else {
          barColor = createLinearOpacityGradient(dataItem.dataKey.color, barSettings.backgroundSettings.gradient);
        }
        seriesData.push(
          {id: dataItem.id, value: dataItem.value, name: dataItem.dataKey.label,
            itemStyle: {color: barColor, borderColor: dataItem.dataKey.color, borderRadius}}
        );
      }
    }
    this.latestChartOption.series[0].data = seriesData;
  }
}
