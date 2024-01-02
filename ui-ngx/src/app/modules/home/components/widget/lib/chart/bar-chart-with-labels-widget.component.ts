///
/// Copyright © 2016-2023 The Thingsboard Authors
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

import {
  AfterViewInit,
  ChangeDetectorRef,
  Component,
  ElementRef,
  Input,
  OnDestroy,
  OnInit,
  Renderer2,
  TemplateRef,
  ViewChild,
  ViewEncapsulation
} from '@angular/core';
import { WidgetContext } from '@home/models/widget-component.models';
import {
  backgroundStyle,
  ComponentStyle,
  DateFormatProcessor,
  overlayStyle,
  textStyle
} from '@shared/models/widget-settings.models';
import { ResizeObserver } from '@juggle/resize-observer';
import * as echarts from 'echarts/core';
import { Axis } from 'echarts';
import { formatValue } from '@core/utils';
import { GridComponent, GridComponentOption, TooltipComponent, TooltipComponentOption } from 'echarts/components';
import { LabelLayout } from 'echarts/features';
import { BarChart, BarSeriesOption} from 'echarts/charts';
import { CanvasRenderer } from 'echarts/renderers';
import { DataKey, DataSet } from '@shared/models/widget.models';
import { Observable } from 'rxjs';
import { ImagePipe } from '@shared/pipe/image.pipe';
import { DomSanitizer } from '@angular/platform-browser';
import {
  barChartWithLabelsDefaultSettings,
  BarChartWithLabelsWidgetSettings
} from '@home/components/widget/lib/chart/bar-chart-with-labels-widget.models';
import { LabelLayoutOptionCallback } from 'echarts/types/dist/shared';
import BarSeriesModel from 'echarts/types/src/chart/bar/BarSeries';

const axisGetBandWidth = Axis.prototype.getBandWidth;

Axis.prototype.getBandWidth = function(){
  const series: any[] = this.model?.parentModel?.getSeries();
  if (this.scale.type === 'time' && series?.length && series[0].type === 'series.bar') {
    const barWidth = (series[0] as BarSeriesModel).getData().getLayout('size');
    return barWidth * (series.length + 1);
  } else {
    return axisGetBandWidth.call(this);
  }
};

echarts.use([
  TooltipComponent,
  GridComponent,
  BarChart,
  LabelLayout,
  CanvasRenderer
]);

type EChartsOption = echarts.ComposeOption<
  | TooltipComponentOption
  | GridComponentOption
  | BarSeriesOption
>;

type ECharts = echarts.ECharts;

type NamedDataSet = {name: string; value: [number, any]}[];

type BarLabelOption = NonNullable<BarSeriesOption['label']>;

interface BarChartDataItem {
  id: string;
  dataKey: DataKey;
  data: NamedDataSet;
  enabled: boolean;
}

interface BarChartLegendItem {
  id: string;
  color: string;
  label: string;
  enabled: boolean;
}

const toNamedData = (data: DataSet): NamedDataSet => {
  if (!data?.length) {
    return [];
  } else {
    return data.map(d => ({
      name: d[0] + '',
      value: d
    }));
  }
};

@Component({
  selector: 'tb-bar-chart-with-labels-widget',
  templateUrl: './bar-chart-with-labels-widget.component.html',
  styleUrls: ['./bar-chart-with-labels-widget.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class BarChartWithLabelsWidgetComponent implements OnInit, OnDestroy, AfterViewInit {

  @ViewChild('chartShape', {static: false})
  chartShape: ElementRef<HTMLElement>;

  settings: BarChartWithLabelsWidgetSettings;

  @Input()
  ctx: WidgetContext;

  @Input()
  widgetTitlePanel: TemplateRef<any>;

  showLegend: boolean;
  legendClass: string;

  backgroundStyle$: Observable<ComponentStyle>;
  overlayStyle: ComponentStyle = {};

  legendItems: BarChartLegendItem[];
  legendLabelStyle: ComponentStyle;
  disabledLegendLabelStyle: ComponentStyle;

  private shapeResize$: ResizeObserver;

  private decimals = 0;
  private units = '';

  private dataItems: BarChartDataItem[] = [];

  private drawChartPending = false;
  private barChart: ECharts;
  private barChartOptions: EChartsOption;

  private tooltipDateFormat: DateFormatProcessor;

  private barLabelOptions: BarLabelOption;
  private barLabelLayoutCallback: LabelLayoutOptionCallback;

  constructor(private imagePipe: ImagePipe,
              private sanitizer: DomSanitizer,
              private renderer: Renderer2,
              private cd: ChangeDetectorRef) {
  }

  ngOnInit(): void {
    this.ctx.$scope.barChartWidget = this;
    this.settings = {...barChartWithLabelsDefaultSettings, ...this.ctx.settings};

    this.decimals = this.ctx.decimals;
    this.units = this.ctx.units;

    this.backgroundStyle$ = backgroundStyle(this.settings.background, this.imagePipe, this.sanitizer);
    this.overlayStyle = overlayStyle(this.settings.background.overlay);

    this.showLegend = this.settings.showLegend;

    if (this.showLegend) {
      this.legendItems = [];
      this.legendClass = `legend-${this.settings.legendPosition}`;
      this.legendLabelStyle = textStyle(this.settings.legendLabelFont);
      this.disabledLegendLabelStyle = textStyle(this.settings.legendLabelFont);
      this.legendLabelStyle.color = this.settings.legendLabelColor;
    }
    let counter = 0;
    if (this.ctx.datasources.length) {
      for (const datasource of this.ctx.datasources) {
        const dataKeys = datasource.dataKeys;
        for (const dataKey of dataKeys) {
          const id = counter++;
          const datasourceData = this.ctx.data ? this.ctx.data.find(d => d.dataKey === dataKey) : null;
          const namedData = datasourceData?.data ? toNamedData(datasourceData.data) : [];
          this.dataItems.push({
            id: id+'',
            dataKey,
            data: namedData,
            enabled: true
          });
          if (this.showLegend) {
            this.legendItems.push(
              {
                id: id+'',
                label: dataKey.label,
                color: dataKey.color,
                enabled: true
              }
            );
          }
        }
      }
    }

    if (this.settings.showTooltip && this.settings.tooltipShowDate) {
      this.tooltipDateFormat = DateFormatProcessor.fromSettings(this.ctx.$injector, this.settings.tooltipDateFormat);
    }

    const barValueStyle: ComponentStyle = textStyle(this.settings.barValueFont);
    delete barValueStyle.lineHeight;
    barValueStyle.fontSize = this.settings.barValueFont.size;
    barValueStyle.color = this.settings.barValueColor;

    const barLabelStyle: ComponentStyle = textStyle(this.settings.barLabelFont);
    delete barLabelStyle.lineHeight;
    barLabelStyle.fontSize = this.settings.barLabelFont.size;
    barLabelStyle.color = this.settings.barLabelColor;

    this.barLabelOptions = {
      show: this.settings.showBarLabel || this.settings.showBarValue,
      position: 'insideBottom',
      distance: 15,
      align: 'left',
      verticalAlign: 'middle',
      rotate: 90,
      formatter: (params) => {
        const parts: string[] = [];
        if (this.settings.showBarValue) {
          const value = formatValue(params.value[1], this.decimals, '', false);
          parts.push(`{value|${value}}`);
        }
        if (this.settings.showBarLabel) {
          parts.push(`{label|${params.seriesName}}`);
        }
        return parts.join(' ');
      },
      rich: {
        value: barValueStyle,
        label: barLabelStyle
      }
    };

    this.barLabelLayoutCallback = (params) => {
      if (params.rect.width - params.labelRect.width < 2) {
        return {
          y: '-100%'
        };
      } else {
        return {
          hideOverlap: true
        };
      }
    };
  }

  ngAfterViewInit() {
    if (this.drawChartPending) {
      this.drawChart();
    }
  }

  ngOnDestroy() {
    if (this.shapeResize$) {
      this.shapeResize$.disconnect();
    }
    if (this.barChart) {
      this.barChart.dispose();
    }
  }

  public onInit() {
    const borderRadius = this.ctx.$widgetElement.css('borderRadius');
    this.overlayStyle = {...this.overlayStyle, ...{borderRadius}};
    if (this.chartShape) {
      this.drawChart();
    } else {
      this.drawChartPending = true;
    }
    this.cd.detectChanges();
  }

  public onDataUpdated() {
    let minTime = this.ctx.defaultSubscription.timeWindow.minTime;
    for (const item of this.dataItems) {
      const datasourceData = this.ctx.data ? this.ctx.data.find(d => d.dataKey === item.dataKey) : null;
      const namedData = datasourceData?.data ? toNamedData(datasourceData.data) : [];
      item.data = namedData;
      if (datasourceData.data.length) {
        minTime = Math.min(datasourceData.data[0][0], minTime);
      }
    }
    if (this.barChart) {
      (this.barChartOptions.xAxis as any).min = minTime;
      (this.barChartOptions.xAxis as any).max = this.ctx.defaultSubscription.timeWindow.maxTime;
      this.barChartOptions.series = this.updateSeries();
      this.barChart.setOption(this.barChartOptions);
    }
  }

  private updateSeries(): Array<BarSeriesOption> {
    const series: Array<BarSeriesOption> = [];
    for (const item of this.dataItems) {
      if (item.enabled) {
        const seriesOption: BarSeriesOption = {
          type: 'bar',
          barGap: 0,
          barMaxWidth: 40,
          id: item.id,
          name: item.dataKey.label,
          label: this.barLabelOptions,
          color: item.dataKey.color,
          data: item.data,
          emphasis: {
            focus: 'series'
          },
          labelLayout: this.barLabelLayoutCallback
        };
        series.push(seriesOption);
      }
    }
    return series;
  }

  public onLegendItemEnter(item: BarChartLegendItem) {
    this.barChart.dispatchAction({
      type: 'highlight',
      seriesId: item.id
    });
  }

  public onLegendItemLeave(item: BarChartLegendItem) {
    this.barChart.dispatchAction({
      type: 'downplay',
      seriesId: item.id
    });
  }

  public toggleLegendItem(item: BarChartLegendItem) {
    const enable = !item.enabled;
    const dataItem = this.dataItems.find(d => d.id === item.id);
    if (dataItem) {
      dataItem.enabled = enable;
      this.barChartOptions.series = this.updateSeries();
      this.barChart.setOption(this.barChartOptions, true);
      item.enabled = enable;
      if (enable) {
        this.barChart.dispatchAction({
          type: 'highlight',
          seriesId: item.id
        });
      } else {
        for (const otherItem of this.legendItems.filter(i => i.id !== item.id)) {
          this.barChart.dispatchAction({
            type: 'highlight',
            seriesId: otherItem.id
          });
        }
      }
    }
  }

  private drawChart() {
    this.barChart = echarts.init(this.chartShape.nativeElement, null, {
      renderer: 'canvas',
    });
    this.barChartOptions = {
      tooltip: {
        trigger: 'none'
      },
      grid: {
        containLabel: true,
        top: '30',
        left: 0,
        right: 0,
        bottom: 0
      },
      xAxis: {
        type: 'time',
        axisTick: {
          show: false
        },
        axisLabel: {
          hideOverlap: true,
          fontSize: 10
        },
        axisLine: {
          onZero: false
        },
        min: this.ctx.defaultSubscription.timeWindow.minTime,
        max: this.ctx.defaultSubscription.timeWindow.maxTime
      },
      yAxis: {
        type: 'value',
        axisLabel: {
          formatter: value => formatValue(value, this.decimals, this.units, false)
        }
      }
    };

    this.barChartOptions.series = this.updateSeries();

    if (this.settings.showTooltip) {
      this.barChartOptions.tooltip = {
        trigger: 'axis',
        axisPointer: {
          type: 'shadow'
        },
        formatter: (params: any[]) => {
          if (!params.length || !params[0]) {
            return null;
          }
          const tooltipElement: HTMLElement = this.renderer.createElement('div');
          this.renderer.setStyle(tooltipElement, 'display', 'flex');
          this.renderer.setStyle(tooltipElement, 'flex-direction', 'column');
          this.renderer.setStyle(tooltipElement, 'align-items', 'flex-start');
          this.renderer.setStyle(tooltipElement, 'gap', '4px');
          if (this.settings.tooltipShowDate) {
            const dateElement: HTMLElement = this.renderer.createElement('div');
            const ts = params[0].value[0];
            this.tooltipDateFormat.update(ts);
            this.renderer.appendChild(dateElement, this.renderer.createText(this.tooltipDateFormat.formatted));
            this.renderer.setStyle(dateElement, 'font-family', this.settings.tooltipDateFont.family);
            this.renderer.setStyle(dateElement, 'font-size', this.settings.tooltipDateFont.size + this.settings.tooltipDateFont.sizeUnit);
            this.renderer.setStyle(dateElement, 'font-style', this.settings.tooltipDateFont.style);
            this.renderer.setStyle(dateElement, 'font-weight', this.settings.tooltipDateFont.weight);
            this.renderer.setStyle(dateElement, 'line-height', this.settings.tooltipDateFont.lineHeight);
            this.renderer.setStyle(dateElement, 'color', this.settings.tooltipDateColor);
            this.renderer.appendChild(tooltipElement, dateElement);
          }
          let seriesParams = null;
          const focusedSeriesIndex = this.focusedSeriesIndex();
          if (focusedSeriesIndex > -1) {
            seriesParams = params.find(param => param.seriesIndex === focusedSeriesIndex);
          }
          if (seriesParams) {
            this.renderer.appendChild(tooltipElement, this.constructTooltipSeriesElement(seriesParams));
          } else {
            for (seriesParams of params) {
              this.renderer.appendChild(tooltipElement, this.constructTooltipSeriesElement(seriesParams));
            }
          }
          return tooltipElement;
        },
        padding: [8, 12],
        backgroundColor: this.settings.tooltipBackgroundColor,
        extraCssText: `line-height: 1; backdrop-filter: blur(${this.settings.tooltipBackgroundBlur}px);`
      };
    }

    this.barChart.setOption(this.barChartOptions);

    this.shapeResize$ = new ResizeObserver(() => {
      this.onResize();
    });
    this.shapeResize$.observe(this.chartShape.nativeElement);
    this.onResize();
  }

  private constructTooltipSeriesElement(seriesParams): HTMLElement {
    const labelValueElement: HTMLElement = this.renderer.createElement('div');
    this.renderer.setStyle(labelValueElement, 'display', 'flex');
    this.renderer.setStyle(labelValueElement, 'flex-direction', 'row');
    this.renderer.setStyle(labelValueElement, 'align-items', 'center');
    this.renderer.setStyle(labelValueElement, 'align-self', 'stretch');
    this.renderer.setStyle(labelValueElement, 'gap', '12px');
    const labelElement: HTMLElement = this.renderer.createElement('div');
    this.renderer.setStyle(labelElement, 'display', 'flex');
    this.renderer.setStyle(labelElement, 'align-items', 'center');
    this.renderer.setStyle(labelElement, 'gap', '8px');
    this.renderer.appendChild(labelValueElement, labelElement);
    const circleElement: HTMLElement = this.renderer.createElement('div');
    this.renderer.setStyle(circleElement, 'width', '8px');
    this.renderer.setStyle(circleElement, 'height', '8px');
    this.renderer.setStyle(circleElement, 'border-radius', '50%');
    this.renderer.setStyle(circleElement, 'background', seriesParams.color);
    this.renderer.appendChild(labelElement, circleElement);
    const labelTextElement: HTMLElement = this.renderer.createElement('div');
    this.renderer.appendChild(labelTextElement, this.renderer.createText(seriesParams.seriesName));
    this.renderer.setStyle(labelTextElement, 'font-family', 'Roboto');
    this.renderer.setStyle(labelTextElement, 'font-size', '12px');
    this.renderer.setStyle(labelTextElement, 'font-style', 'normal');
    this.renderer.setStyle(labelTextElement, 'font-weight', '400');
    this.renderer.setStyle(labelTextElement, 'line-height', '16px');
    this.renderer.setStyle(labelTextElement, 'letter-spacing', '0.4px');
    this.renderer.setStyle(labelTextElement, 'color', 'rgba(0, 0, 0, 0.76)');
    this.renderer.appendChild(labelElement, labelTextElement);
    const valueElement: HTMLElement = this.renderer.createElement('div');
    const value = formatValue(seriesParams.value[1], this.decimals, this.units, false);
    this.renderer.appendChild(valueElement, this.renderer.createText(value));
    this.renderer.setStyle(valueElement, 'flex', '1');
    this.renderer.setStyle(valueElement, 'text-align', 'end');
    this.renderer.setStyle(valueElement, 'font-family', this.settings.tooltipValueFont.family);
    this.renderer.setStyle(valueElement, 'font-size', this.settings.tooltipValueFont.size + this.settings.tooltipValueFont.sizeUnit);
    this.renderer.setStyle(valueElement, 'font-style', this.settings.tooltipValueFont.style);
    this.renderer.setStyle(valueElement, 'font-weight', this.settings.tooltipValueFont.weight);
    this.renderer.setStyle(valueElement, 'line-height', this.settings.tooltipValueFont.lineHeight);
    this.renderer.setStyle(valueElement, 'color', this.settings.tooltipValueColor);
    this.renderer.appendChild(labelValueElement, valueElement);
    return labelValueElement;
  }

  private focusedSeriesIndex(): number {
    let index = - 1;
    // @ts-ignore
    const views: any[] = this.barChart._chartsViews;
    if (views) {
      const hasBlurredView = !!views.find(view => {
        const graphicEls: any[] = view._data._graphicEls;
        return !!graphicEls.find(el => el.currentStates.includes('blur'));
      });
      if (hasBlurredView) {
        const focusedView = views.find(view => {
          const graphicEls: any[] = view._data._graphicEls;
          return !!graphicEls.find(el => !el.currentStates.includes('blur'));
        });
        if (focusedView) {
          index = focusedView._model.seriesIndex;
        }
      }
    }
    return index;
  }

  private onResize() {
    const width = this.barChart.getWidth();
    const height = this.barChart.getHeight();
    const shapeWidth = this.chartShape.nativeElement.offsetWidth;
    const shapeHeight = this.chartShape.nativeElement.offsetHeight;
    if (width !== shapeWidth || height !== shapeHeight) {
      this.barChart.resize();
    }
  }
}
