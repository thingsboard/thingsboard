///
/// Copyright © 2016-2024 The Thingsboard Authors
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
import { formatValue } from '@core/utils';
import { Observable } from 'rxjs';
import { ImagePipe } from '@shared/pipe/image.pipe';
import { DomSanitizer } from '@angular/platform-browser';
import {
  barChartWithLabelsDefaultSettings,
  BarChartWithLabelsWidgetSettings
} from '@home/components/widget/lib/chart/bar-chart-with-labels-widget.models';

import * as echarts from 'echarts/core';
import { CustomSeriesOption } from 'echarts/charts';
import { CallbackDataParams, CustomSeriesRenderItem, LabelLayoutOptionCallback } from 'echarts/types/dist/shared';

import {
  ECharts,
  echartsModule,
  EChartsOption,
  EChartsSeriesItem,
  echartsTooltipFormatter, timeAxisBandWidthCalculator,
  toNamedData
} from '@home/components/widget/lib/chart/echarts-widget.models';
import { AggregationType, IntervalMath } from '@shared/models/time/time.models';

type BarChartDataItem = EChartsSeriesItem;

interface BarChartLegendItem {
  id: string;
  color: string;
  label: string;
  enabled: boolean;
}

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

  private get noAggregation(): boolean {
    return this.ctx.defaultSubscription.timeWindowConfig?.aggregation?.type === AggregationType.NONE;
  }

  private shapeResize$: ResizeObserver;

  private decimals = 0;
  private units = '';

  private dataItems: BarChartDataItem[] = [];

  private drawChartPending = false;
  private barChart: ECharts;
  private barChartOptions: EChartsOption;

  private tooltipDateFormat: DateFormatProcessor;

  private barRenderItem: CustomSeriesRenderItem;
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
    barValueStyle.fill = this.settings.barValueColor;

    const barLabelStyle: ComponentStyle = textStyle(this.settings.barLabelFont);
    delete barLabelStyle.lineHeight;
    barLabelStyle.fontSize = this.settings.barLabelFont.size;
    barLabelStyle.fill = this.settings.barLabelColor;

    this.barRenderItem = (params, api) => {

      const time = api.value(0) as number;
      let start = api.value(2) as number;
      const end = api.value(3) as number;
      let interval = end - start;
      if (!start || !end || !interval) {
        interval = IntervalMath.numberValue(this.ctx.timeWindow.interval);
        start = time - interval / 2;
      }
      const enabledDataItems = this.dataItems.filter(d => d.enabled);
      const barInterval = interval / (enabledDataItems.length + 1);
      const intervalGap = barInterval / 2;

      const index = enabledDataItems.findIndex(d => d.id === params.seriesId);
      const value = api.value(1);
      const startTime = start + intervalGap + barInterval * index;
      const delta = barInterval;
      const lowerLeft = api.coord([startTime, value >= 0 ? value : 0]);
      const height = api.size([delta, value])[1];
      const width = api.size([delta, 10])[0];

      const coordSys: {x: number; y: number; width: number; height: number} = params.coordSys as any;

      const rectShape = echarts.graphic.clipRectByRect({
        x: lowerLeft[0],
        y: lowerLeft[1],
        width,
        height
      }, {
        x: coordSys.x,
        y: coordSys.y,
        width: coordSys.width,
        height: coordSys.height
      });

      const zeroPos = api.coord([0, 0]);
      const labelParts: string[] = [];
      if (this.settings.showBarValue) {
        const labelValue = formatValue(value, this.decimals, '', false);
        labelParts.push(`{value|${labelValue}}`);
      }
      if (this.settings.showBarLabel) {
        labelParts.push(`{label|${params.seriesName}}`);
      }
      const barLabel = labelParts.join(' ');
      return rectShape && {
        type: 'rect',
        id: time + '',
        shape: rectShape,
        style: {
          fill: api.visual('color'),
          text: barLabel,
          textPosition: 'insideBottom',
          textRotation: Math.PI / 2,
          textDistance: 15,
          textStrokeWidth: 0,
          textAlign: 'left',
          textVerticalAlign: 'middle',
          rich: {
            value: barValueStyle,
            label: barLabelStyle
          }
        },
        focus: 'series',
        transition: 'all',
        enterFrom: {
          style: { opacity: 0 },
          shape: { height: 0, y: zeroPos[1] }
        }
      };
    };

    this.barLabelLayoutCallback = (params) => {
      if (params.rect.width - params.labelRect.width < 2) {
        return {
          y: '100000%',
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
    for (const item of this.dataItems) {
      const datasourceData = this.ctx.data ? this.ctx.data.find(d => d.dataKey === item.dataKey) : null;
      item.data = datasourceData?.data ? toNamedData(datasourceData.data) : [];
    }
    if (this.barChart) {
      (this.barChartOptions.xAxis as any).min = this.ctx.defaultSubscription.timeWindow.minTime;
      (this.barChartOptions.xAxis as any).max = this.ctx.defaultSubscription.timeWindow.maxTime;
      (this.barChartOptions.xAxis as any).tbTimeWindow = this.ctx.defaultSubscription.timeWindow;
      this.barChartOptions.series = this.updateSeries();
      this.barChart.setOption(this.barChartOptions);
    }
  }

  private updateSeries(): Array<CustomSeriesOption> {
    const series: Array<CustomSeriesOption> = [];
    for (const item of this.dataItems) {
      if (item.enabled) {
        const seriesOption: CustomSeriesOption = {
          type: 'custom',
          id: item.id,
          name: item.dataKey.label,
          color: item.dataKey.color,
          data: item.data,
          renderItem: this.barRenderItem,
          labelLayout: this.barLabelLayoutCallback,
          dimensions: [
            {name: 'intervalStart', type: 'number'},
            {name: 'intervalEnd', type: 'number'}
          ],
          encode: {
            intervalStart: 2,
            intervalEnd: 3
          }
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
      if (!enable) {
        this.barChart.dispatchAction({
          type: 'downplay',
          seriesId: item.id
        });
      }
      this.barChartOptions.series = this.updateSeries();
      this.barChart.setOption(this.barChartOptions, {replaceMerge: ['series']});
      item.enabled = enable;
      if (enable) {
        this.barChart.dispatchAction({
          type: 'highlight',
          seriesId: item.id
        });
      }
    }
  }

  private drawChart() {
    echartsModule.init();
    this.barChart = echarts.init(this.chartShape.nativeElement, null, {
      renderer: 'canvas',
    });
    this.barChartOptions = {
      tooltip: {
        trigger: 'axis',
        confine: true,
        appendTo: 'body',
        axisPointer: {
          type: 'shadow'
        },
        formatter: (params: CallbackDataParams[]) => {
          if (this.settings.showTooltip) {
            const focusedSeriesIndex = this.focusedSeriesIndex();
            return echartsTooltipFormatter(this.renderer, this.tooltipDateFormat,
              this.settings, params, this.decimals, this.units, focusedSeriesIndex, null,
              this.noAggregation ? null : this.ctx.timeWindow.interval);
          } else {
            return undefined;
          }
        },
        padding: [8, 12],
        backgroundColor: this.settings.tooltipBackgroundColor,
        borderWidth: 0,
        extraCssText: `line-height: 1; backdrop-filter: blur(${this.settings.tooltipBackgroundBlur}px);`
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
        scale: true,
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
        max: this.ctx.defaultSubscription.timeWindow.maxTime,
        bandWidthCalculator: timeAxisBandWidthCalculator
      },
      yAxis: {
        type: 'value',
        axisLabel: {
          formatter: (value: any) => formatValue(value, this.decimals, this.units, false)
        }
      }
    };

    (this.barChartOptions.xAxis as any).tbTimeWindow = this.ctx.defaultSubscription.timeWindow;

    this.barChartOptions.series = this.updateSeries();

    this.barChart.setOption(this.barChartOptions);

    this.shapeResize$ = new ResizeObserver(() => {
      this.onResize();
    });
    this.shapeResize$.observe(this.chartShape.nativeElement);
    this.onResize();
  }

  private focusedSeriesIndex(): number {
    let index = - 1;
    const views: any[] = (this.barChart as any)._chartsViews;
    if (views) {
      const hasBlurredView = !!views.find(view => {
        const graphicEls: any[] = view._data._graphicEls;
        return !!graphicEls.find(el => el?.currentStates.includes('blur'));
      });
      if (hasBlurredView) {
        const focusedView = views.find(view => {
          const graphicEls: any[] = view._data._graphicEls;
          return !!graphicEls.find(el => !el?.currentStates.includes('blur'));
        });
        if (focusedView) {
          index = !!focusedView._model ?
            focusedView._model.seriesIndex : (!!focusedView.__model ? focusedView.__model.seriesIndex : -1);
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
