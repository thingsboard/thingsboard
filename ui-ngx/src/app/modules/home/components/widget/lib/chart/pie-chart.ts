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

import { pieChartDefaultSettings, PieChartSettings } from '@home/components/widget/lib/chart/pie-chart.models';
import { WidgetContext } from '@home/models/widget-component.models';
import { DeepPartial } from '@shared/models/common';
import { Renderer2 } from '@angular/core';
import { ColorProcessor, textStyle } from '@shared/models/widget-settings.models';
import { PieDataItemOption } from 'echarts/types/src/chart/pie/PieSeries';
import { Text } from '@svgdotjs/svg.js';
import { TranslateService } from '@ngx-translate/core';
import { TbLatestChart } from '@home/components/widget/lib/chart/latest-chart';
import { formatValue } from '@core/utils';
import { toAnimationOption } from '@home/components/widget/lib/chart/chart.models';

const shapeSize = 134;
const shapeSegmentWidth = 13.4;

export class TbPieChart extends TbLatestChart<PieChartSettings> {

  private totalValueColor: ColorProcessor;
  private totalTextNode: Text;

  constructor(ctx: WidgetContext,
              inputSettings: DeepPartial<PieChartSettings>,
              chartElement: HTMLElement,
              renderer: Renderer2,
              translate: TranslateService,
              autoResize = true) {

    super(ctx, inputSettings, chartElement, renderer, translate, autoResize);
  }

  protected defaultSettings(): PieChartSettings {
      return pieChartDefaultSettings;
  }

  protected initSettings() {
    if (this.settings.showTotal) {
      this.totalValueColor = ColorProcessor.fromSettings(this.settings.totalValueColor);
    }
  }

  protected prepareLatestChartOption() {
    const shapeWidth = this.chartElement.offsetWidth;
    const shapeHeight = this.chartElement.offsetHeight;
    const size = this.settings.autoScale ? shapeSize : Math.min(shapeWidth, shapeHeight);
    const innerRadius = size / 2 - shapeSegmentWidth;
    const outerRadius = size / 2;
    const labelStyle = textStyle(this.settings.labelFont);
    labelStyle.fontSize = this.settings.labelFont.size;
    labelStyle.lineHeight = labelStyle.fontSize * 1.2;
    labelStyle.color = this.settings.labelColor;
    this.latestChartOption.series = [
      {
        type: 'pie',
        clockwise: this.settings.clockwise,
        radius: this.settings.doughnut ? [innerRadius, outerRadius] : this.settings.radius,
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: this.settings.borderRadius,
          borderWidth: this.settings.borderWidth,
          borderColor: this.settings.borderColor
        },
        label: {
          show: this.settings.showLabel,
          position: this.settings.labelPosition,
          formatter: (params) => {
            const percents = params.percent;
            const value = formatValue(percents, 0, '%', false);
            return `{label|${params.name}\n${value}}`;
          },
          rich: {
            label: labelStyle
          }
        },
        emphasis: {
          scale: this.settings.emphasisScale,
          itemStyle: {
            borderColor: this.settings.emphasisBorderColor,
            borderWidth: this.settings.emphasisBorderWidth,
            shadowColor: this.settings.emphasisShadowColor,
            shadowBlur: this.settings.emphasisShadowBlur
          },
          label: {
            show: this.settings.showLabel
          }
        },
        ...toAnimationOption(this.ctx, this.settings.animation)
      }
    ];
  }

  protected afterDrawChart() {
    if (this.settings.showTotal) {
      this.totalTextNode = this.svgShape.text('').font({
        family: 'Roboto',
        leading: 1
      }).attr({'text-anchor': 'middle'});
      this.renderTotal();
    }
  };

  protected doUpdateSeriesData() {
    const seriesData: PieDataItemOption[] = [];
    const enabledDataItems = this.dataItems.filter(item => item.enabled && item.hasValue);
    for (const dataItem of this.dataItems) {
      if (dataItem.enabled && dataItem.hasValue) {
        seriesData.push(
          {id: dataItem.id, value: dataItem.value, name: dataItem.dataKey.label, itemStyle: {color: dataItem.dataKey.color}}
        );
      }
    }
    if (this.settings.doughnut) {
      this.latestChartOption.series[0].padAngle = enabledDataItems.length > 1 ? 2 : 0;
    }
    this.latestChartOption.series[0].data = seriesData;
  }

  protected afterUpdateSeriesData(initial: boolean) {
    if (this.settings.showTotal) {
      this.totalValueColor.update(this.total);
      if (!initial) {
        this.renderTotal();
      }
    }
  };

  protected initialShapeWidth(): number {
    return shapeSize;
  }

  protected initialShapeHeight(): number {
    return shapeSize;
  }

  protected beforeResize(shapeWidth: number, shapeHeight: number) {
    if (!this.settings.autoScale) {
      if (this.settings.doughnut) {
        const size = Math.min(shapeWidth, shapeHeight);
        const innerRadius = size / 2 - shapeSegmentWidth;
        const outerRadius = size / 2;
        this.latestChartOption.series[0].radius = [innerRadius, outerRadius];
        this.latestChart.setOption(this.latestChartOption);
      }
    }
  };

  protected afterResize(shapeWidth: number, shapeHeight: number) {
    if (this.settings.showTotal) {
      this.totalTextNode.center((this.settings.autoScale ? shapeSize : shapeWidth) / 2,
        (this.settings.autoScale ? shapeSize : shapeHeight) / 2);
    }
  };

  private renderTotal() {
    this.totalTextNode.text(add => {
      add.tspan(this.translate.instant('widgets.latest-chart.total')).font({size: '12px', weight: 400}).fill('rgba(0, 0, 0, 0.38)');
      add.tspan('').newLine().font({size: '4px'});
      add.tspan(this.totalText).newLine().font(
        {family: this.settings.totalValueFont.family,
          size: this.settings.totalValueFont.size + this.settings.totalValueFont.sizeUnit,
          weight: this.settings.totalValueFont.weight,
          style: this.settings.totalValueFont.style}
      ).fill(this.totalValueColor.color);
    }).center(this.svgShape.bbox().width / 2, this.svgShape.bbox().height / 2);
  }
}
