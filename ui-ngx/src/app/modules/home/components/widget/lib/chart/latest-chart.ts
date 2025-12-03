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

import { ECharts, echartsModule, EChartsOption } from '@home/components/widget/lib/chart/echarts-widget.models';
import {
  LatestChartDataItem,
  LatestChartLegendItem,
  LatestChartSettings,
  latestChartTooltipFormatter
} from '@home/components/widget/lib/chart/latest-chart.models';
import { WidgetContext } from '@home/models/widget-component.models';
import { DeepPartial } from '@shared/models/common';
import { Renderer2 } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { isDefinedAndNotNull, isNumeric, mergeDeep } from '@core/utils';
import { DataKey } from '@shared/models/widget.models';
import * as echarts from 'echarts/core';
import { CallbackDataParams } from 'echarts/types/dist/shared';
import { SVG, Svg } from '@svgdotjs/svg.js';
import { toAnimationOption } from '@home/components/widget/lib/chart/chart.models';
import { ValueFormatProcessor } from '@shared/models/widget-settings.models';

export abstract class TbLatestChart<S extends LatestChartSettings> {

  private readonly shapeResize$: ResizeObserver;
  private showTotalValueInLegend: boolean;

  protected readonly settings: S;

  protected valueFormatter: ValueFormatProcessor;

  protected total = 0;
  protected totalText = 'N/A';

  protected latestChart: ECharts;
  protected latestChartOption: EChartsOption;

  protected svgShape: Svg;

  protected dataItems: LatestChartDataItem[] = [];
  private legendItems: LatestChartLegendItem[] = [];

  private itemClick: ($event: Event, item: LatestChartDataItem) => void;

  protected constructor(protected ctx: WidgetContext,
                        private readonly inputSettings: DeepPartial<S>,
                        protected chartElement: HTMLElement,
                        private renderer: Renderer2,
                        protected translate: TranslateService,
                        private autoResize = true) {
    this.settings = mergeDeep({} as S,
      this.defaultSettings(),
      this.inputSettings as S);

    this.initSettings();
    this.prepareValueFormat();
    this.setupData();

    this.onResize();
    if (this.autoResize) {
      this.shapeResize$ = new ResizeObserver(() => {
        this.onResize();
      });
      this.shapeResize$.observe(this.chartElement);
    }
  }

  private prepareValueFormat() {
    const units = this.ctx.units;

    if (this.settings.showTooltip) {
      this.settings.tooltipValueFormater = ValueFormatProcessor.fromSettings(this.ctx.$injector, {units, decimals: this.settings.tooltipValueDecimals});
    }
    this.valueFormatter = ValueFormatProcessor.fromSettings(this.ctx.$injector, {units, decimals: this.ctx.decimals});
  }

  private setupData(): void {
    let counter = 0;
    if (this.ctx.datasources.length) {
      for (const datasource of this.ctx.datasources) {
        const dataKeys = datasource.dataKeys;
        for (const dataKey of dataKeys) {
          const id = counter++;
          this.dataItems.push({
            id,
            datasource,
            dataKey,
            value: 0,
            hasValue: false,
            enabled: true
          });
          if (this.settings.showLegend) {
            this.legendItems.push(
              {
                dataKey,
                value: '--',
                label: dataKey.label,
                color: dataKey.color,
                hasValue: false
              }
            );
          }
        }
      }
    }
    if (this.settings.sortSeries) {
      this.dataItems.sort((a, b) => a.dataKey.label.localeCompare(b.dataKey.label));
      if (this.settings.showLegend) {
        this.legendItems.sort((a, b) => a.label.localeCompare(b.label));
      }
    }
    this.showTotalValueInLegend = this.settings.showLegend && !this.settings.showTotal && this.settings.legendShowTotal;
    if (this.showTotalValueInLegend) {
      this.legendItems.push(
        {
          value: '--',
          label: this.translate.instant('widgets.latest-chart.total'),
          color: 'rgba(0, 0, 0, 0.06)',
          hasValue: false,
          total: true
        }
      );
    }
  }

  public getLegendItems(): LatestChartLegendItem[] {
    return this.legendItems;
  }

  public update(): void {
    for (const dsData of this.ctx.data) {
      console.log("this.ctx.data",this.ctx.data)
      let value = 0;
      const tsValue = dsData.data[0];
      const dataItem = this.dataItems.find(item => item.dataKey === dsData.dataKey);
      if (tsValue && isDefinedAndNotNull(tsValue[1]) && isNumeric(tsValue[1])) {
        value = tsValue[1];
        dataItem.hasValue = true;
        dataItem.value = Number(value);
      } else {
        dataItem.hasValue = false;
        dataItem.value = 0;
      }
    }
    this.updateSeriesData();
  }

  public keyEnter(dataKey: DataKey): void {
    const item = this.dataItems.find(d => d.dataKey === dataKey);
    if (item) {
      const dataIndex = this.latestChartOption.series[0].data.findIndex(
        (d: any) => d.id === item.id);
      if (dataIndex > -1) {
        this.latestChart.dispatchAction({
          type: 'highlight',
          dataIndex
        });
      }
    }
  }

  public keyLeave(dataKey: DataKey): void {
    const item = this.dataItems.find(d => d.dataKey === dataKey);
    if (item) {
      const dataIndex = this.latestChartOption.series[0].data.findIndex(
        (d: any) => d.id === item.id);
      if (dataIndex > -1) {
        this.latestChart.dispatchAction({
          type: 'downplay',
          dataIndex
        });
      }
    }
  }

  public toggleKey(dataKey: DataKey): void {
    const enable = dataKey.hidden;
    const dataItem = this.dataItems.find(d => d.dataKey === dataKey);
    if (dataItem) {
      let dataIndex = this.latestChartOption.series[0].data.findIndex(
        (d: any) => d.id === dataItem.id);
      dataItem.enabled = enable;
      if (!enable && dataIndex > -1) {
        this.latestChart.dispatchAction({
          type: 'downplay',
          dataIndex
        });
      }
      this.updateSeriesData();
      dataKey.hidden = !enable;
      if (enable) {
        dataIndex = this.latestChartOption.series[0].data.findIndex(
          (d: any) => d.id === dataItem.id);
        if (dataIndex > -1) {
          this.latestChart.dispatchAction({
            type: 'highlight',
            dataIndex
          });
        }
      }
    }
  }

  public destroy(): void {
    if (this.shapeResize$) {
      this.shapeResize$.disconnect();
    }
    if (this.latestChart) {
      this.latestChart.dispose();
    }
  }

  public resize(): void {
    this.onResize();
  }

  public onItemClick(itemClick: ($event: Event, item: LatestChartDataItem) => void) {
    this.itemClick = itemClick;
  }

  protected updateSeriesData(initial = false) {
    if (!this.latestChart.isDisposed()) {
      this.total = 0;
      this.totalText = 'N/A';
      let hasValue = false;
      for (const dataItem of this.dataItems) {
        if (dataItem.enabled && dataItem.hasValue) {
          hasValue = true;
          this.total += dataItem.value;
        }
        if (this.settings.showLegend) {
          const legendItem = this.legendItems.find(item => item.dataKey === dataItem.dataKey);
          if (dataItem.hasValue) {
            legendItem.hasValue = true;
            legendItem.value = this.valueFormatter.format(dataItem.value);
          } else {
            legendItem.hasValue = false;
            legendItem.value = '--';
          }
        }
      }
      if (this.settings.showTotal || this.settings.showLegend) {
        if (hasValue) {
          this.totalText = this.valueFormatter.format(this.total);
          if (this.showTotalValueInLegend) {
            this.legendItems[this.legendItems.length - 1].hasValue = true;
            this.legendItems[this.legendItems.length - 1].value = this.totalText;
          }
        } else if (this.showTotalValueInLegend) {
          this.legendItems[this.legendItems.length - 1].hasValue = false;
          this.legendItems[this.legendItems.length - 1].value = '--';
        }
      }
      this.doUpdateSeriesData();
      this.latestChart.setOption(this.latestChartOption);
      this.afterUpdateSeriesData(initial);
    }
  }

  private drawChart() {
    echartsModule.init();
    this.renderer.setStyle(this.chartElement, 'letterSpacing', 'normal');
    this.latestChart = echarts.init(this.chartElement, null, {
      renderer: 'svg',
      width: this.settings.autoScale ? this.initialShapeWidth() : undefined,
      height: this.settings.autoScale ? this.initialShapeHeight() : undefined,
    });
    this.latestChartOption = {
      tooltip: {
        trigger: this.settings.showTooltip ? 'item' : 'none',
        confine: true,
        formatter: (params: CallbackDataParams) =>
          this.settings.showTooltip
            ? latestChartTooltipFormatter(this.renderer, this.settings, params, this.total, this.dataItems)
            : undefined,
        padding: [4, 8],
        backgroundColor: this.settings.tooltipBackgroundColor,
        extraCssText: `line-height: 1; backdrop-filter: blur(${this.settings.tooltipBackgroundBlur}px);`,
        position: (pos) => [pos[0] + 10, pos[1] + 10]
      },
      ...toAnimationOption(this.ctx, this.settings.animation)
    };
    this.prepareLatestChartOption();
    this.updateSeriesData(true);
    this.renderer.setStyle(this.latestChart.getDom().firstChild, 'overflow', 'visible');
    if (this.settings.autoScale) {
      this.renderer.setStyle(this.latestChart.getDom().firstChild, 'position', 'absolute');
    }
    this.renderer.setStyle(this.latestChart.getDom().firstChild.firstChild, 'overflow', 'visible');
    this.svgShape = SVG(this.latestChart.getDom().firstChild.firstChild).toRoot();
    this.afterDrawChart();
    this.latestChart.on('click', (params) => {
      this.onChartClick(params);
    });
  }

  protected onChartClick(params: echarts.ECElementEvent) {
    if (params.componentType === 'series') {
      if (params.data) {
        const data = params.data as any;
        if (isDefinedAndNotNull(data?.id)) {
          const item = this.dataItems.find(d => d.id === data.id);
          if (item && this.itemClick) {
            this.itemClick(params.event.event, item);
          }
        }
      }
    }
  };

  private onResize() {
    const shapeWidth = this.chartElement.offsetWidth;
    const shapeHeight = this.chartElement.offsetHeight;
    if (shapeWidth && shapeHeight) {
      if (!this.latestChart) {
        this.drawChart();
      } else {
        const width = this.latestChart.getWidth();
        const height = this.latestChart.getHeight();
        if (width !== shapeWidth || height !== shapeHeight) {
          this.beforeResize(shapeWidth, shapeHeight);
          if (!this.settings.autoScale) {
            if (this.forceRedrawOnResize()) {
              this.latestChart.dispose();
              this.drawChart();
            } else {
              this.latestChart.resize();
            }
          } else {
            let scale: number;
            if (shapeWidth < shapeHeight) {
              scale = shapeWidth / this.initialShapeWidth();
            } else {
              scale = shapeHeight / this.initialShapeHeight();
            }
            this.renderer.setStyle(this.latestChart.getDom().firstChild, 'transform', `scale(${scale})`);
          }
          this.afterResize(shapeWidth, shapeHeight);
        }
      }
    }
  }

  protected initSettings() {
  }

  protected initialShapeWidth(): number {
    return 100;
  }

  protected initialShapeHeight(): number {
    return 100;
  }

  protected forceRedrawOnResize(): boolean {
    return false;
  }

  protected beforeResize(_shapeWidth: number, _shapeHeight: number) {};

  protected afterResize(_shapeWidth: number, _shapeHeight: number) {};

  protected afterDrawChart() {};

  protected afterUpdateSeriesData(_initial: boolean) {};

  protected abstract defaultSettings(): S;

  protected abstract prepareLatestChartOption(): void;

  protected abstract doUpdateSeriesData(): void;

}
