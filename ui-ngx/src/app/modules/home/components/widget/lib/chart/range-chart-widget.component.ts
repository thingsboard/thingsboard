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
  ColorRange,
  ComponentStyle,
  DateFormatProcessor,
  filterIncludingColorRanges,
  getDataKey,
  overlayStyle,
  sortedColorRange,
  textStyle
} from '@shared/models/widget-settings.models';
import { ResizeObserver } from '@juggle/resize-observer';
import * as echarts from 'echarts/core';
import { formatValue, isDefinedAndNotNull, isNumber } from '@core/utils';
import { rangeChartDefaultSettings, RangeChartWidgetSettings } from './range-chart-widget.models';
import { Observable } from 'rxjs';
import { ImagePipe } from '@shared/pipe/image.pipe';
import { DomSanitizer } from '@angular/platform-browser';
import {
  ECharts,
  echartsModule,
  EChartsOption,
  echartsTooltipFormatter,
  toNamedData
} from '@home/components/widget/lib/chart/echarts-widget.models';
import { CallbackDataParams } from 'echarts/types/dist/shared';
import { AggregationType } from '@shared/models/time/time.models';

interface VisualPiece {
  lt?: number;
  gt?: number;
  lte?: number;
  gte?: number;
  value?: number;
  color?: string;
}

interface RangeItem {
  index: number;
  from?: number;
  to?: number;
  piece: VisualPiece;
  color: string;
  label: string;
  visible: boolean;
  enabled: boolean;
}

const rangeItemLabel = (from?: number, to?: number): string => {
  if (isNumber(from) && isNumber(to)) {
    if (from === to) {
      return `${from}`;
    } else {
      return `${from} - ${to}`;
    }
  } else if (isNumber(from)) {
    return `≥ ${from}`;
  } else if (isNumber(to)) {
    return `< ${to}`;
  } else {
    return null;
  }
};

const toVisualPiece = (color: string, from?: number, to?: number): VisualPiece => {
  const piece: VisualPiece = {
    color
  };
  if (isNumber(from) && isNumber(to)) {
    if (from === to) {
      piece.value = from;
    } else {
      piece.gte = from;
      piece.lt = to;
    }
  } else if (isNumber(from)) {
    piece.gte = from;
  } else if (isNumber(to)) {
    piece.lt = to;
  }
  return piece;
};

const toRangeItems = (colorRanges: Array<ColorRange>): RangeItem[] => {
  const rangeItems: RangeItem[] = [];
  let counter = 0;
  const ranges = sortedColorRange(filterIncludingColorRanges(colorRanges)).filter(r => isNumber(r.from) || isNumber(r.to));
  for (let i = 0; i < ranges.length; i++) {
    const range = ranges[i];
    let from = range.from;
    const to = range.to;
    if (i > 0) {
      const prevRange = ranges[i - 1];
      if (isNumber(prevRange.to) && isNumber(from) && from < prevRange.to) {
        from = prevRange.to;
      }
    }
    rangeItems.push(
      {
        index: counter++,
        color: range.color,
        enabled: true,
        visible: true,
        from,
        to,
        label: rangeItemLabel(from, to),
        piece: toVisualPiece(range.color, from, to)
      }
    );
    if (!isNumber(from) || !isNumber(to)) {
      const value = !isNumber(from) ? to : from;
      rangeItems.push(
        {
          index: counter++,
          color: 'transparent',
          enabled: true,
          visible: false,
          label: '',
          piece: { gt: value - 0.000000001, lt: value + 0.000000001, color: 'transparent'}
        }
      );
    }
  }
  return rangeItems;
};

const getMarkPoints = (ranges: Array<RangeItem>): number[] => {
  const points = new Set<number>();
  for (const range of ranges) {
    if (range.visible) {
      if (isNumber(range.from)) {
        points.add(range.from);
      }
      if (isNumber(range.to)) {
        points.add(range.to);
      }
    }
  }
  return Array.from(points).sort();
};

@Component({
  selector: 'tb-range-chart-widget',
  templateUrl: './range-chart-widget.component.html',
  styleUrls: ['./range-chart-widget.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class RangeChartWidgetComponent implements OnInit, OnDestroy, AfterViewInit {

  @ViewChild('chartShape', {static: false})
  chartShape: ElementRef<HTMLElement>;

  settings: RangeChartWidgetSettings;

  @Input()
  ctx: WidgetContext;

  @Input()
  widgetTitlePanel: TemplateRef<any>;

  showLegend: boolean;
  legendClass: string;

  backgroundStyle$: Observable<ComponentStyle>;
  overlayStyle: ComponentStyle = {};

  legendLabelStyle: ComponentStyle;
  disabledLegendLabelStyle: ComponentStyle;
  visibleRangeItems: RangeItem[];

  private get noAggregation(): boolean {
    return this.ctx.defaultSubscription.timeWindowConfig?.aggregation?.type === AggregationType.NONE;
  }

  private rangeItems: RangeItem[];

  private shapeResize$: ResizeObserver;

  private decimals = 0;
  private units = '';

  private drawChartPending = false;
  private rangeChart: ECharts;
  private rangeChartOptions: EChartsOption;
  private selectedRanges: {[key: number]: boolean} = {};

  private tooltipDateFormat: DateFormatProcessor;

  constructor(private imagePipe: ImagePipe,
              private sanitizer: DomSanitizer,
              private renderer: Renderer2,
              private cd: ChangeDetectorRef) {
  }

  ngOnInit(): void {
    this.ctx.$scope.rangeChartWidget = this;
    this.settings = {...rangeChartDefaultSettings, ...this.ctx.settings};

    this.decimals = this.ctx.decimals;
    this.units = this.ctx.units;
    const dataKey = getDataKey(this.ctx.datasources);
    if (isDefinedAndNotNull(dataKey?.decimals)) {
      this.decimals = dataKey.decimals;
    }
    if (dataKey?.units) {
      this.units = dataKey.units;
    }


    this.backgroundStyle$ = backgroundStyle(this.settings.background, this.imagePipe, this.sanitizer);
    this.overlayStyle = overlayStyle(this.settings.background.overlay);

    this.rangeItems = toRangeItems(this.settings.rangeColors);
    this.visibleRangeItems = this.rangeItems.filter(item => item.visible);
    for (const range of this.rangeItems) {
      this.selectedRanges[range.index] = true;
    }

    this.showLegend = this.settings.showLegend && !!this.rangeItems.length;

    if (this.showLegend) {
      this.legendClass = `legend-${this.settings.legendPosition}`;
      this.legendLabelStyle = textStyle(this.settings.legendLabelFont);
      this.disabledLegendLabelStyle = textStyle(this.settings.legendLabelFont);
      this.legendLabelStyle.color = this.settings.legendLabelColor;
    }

    if (this.settings.showTooltip && this.settings.tooltipShowDate) {
      this.tooltipDateFormat = DateFormatProcessor.fromSettings(this.ctx.$injector, this.settings.tooltipDateFormat);
    }
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
    if (this.rangeChart) {
      this.rangeChart.dispose();
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
    if (this.rangeChart) {
      this.rangeChart.setOption({
        xAxis: {
          min: this.ctx.defaultSubscription.timeWindow.minTime,
          max: this.ctx.defaultSubscription.timeWindow.maxTime,
          tbTimeWindow: this.ctx.defaultSubscription.timeWindow
        },
        series: [
          {data: this.ctx.data?.length ? toNamedData(this.ctx.data[0].data) : []}
        ],
        visualMap: {
          selected: this.selectedRanges
        }
      });
    }
  }

  public toggleRangeItem(item: RangeItem) {
    item.enabled = !item.enabled;
    this.selectedRanges[item.index] = item.enabled;
    this.rangeChart.dispatchAction({
      type: 'selectDataRange',
      selected: this.selectedRanges
    });
  }

  private drawChart() {
    echartsModule.init();
    const dataKey = getDataKey(this.ctx.datasources);
    this.rangeChart = echarts.init(this.chartShape.nativeElement, null, {
      renderer: 'canvas',
    });
    this.rangeChartOptions = {
      tooltip: {
        trigger: 'axis',
        confine: true,
        appendTo: 'body',
        axisPointer: {
          type: 'shadow'
        },
        formatter: (params: CallbackDataParams[]) =>
          this.settings.showTooltip ? echartsTooltipFormatter(this.renderer, this.tooltipDateFormat,
            this.settings, params, this.decimals, this.units, 0, null,
            this.noAggregation ? null : this.ctx.timeWindow.interval) : undefined,
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
        bottom: this.settings.dataZoom ? 60 : 0
      },
      xAxis: {
        type: 'time',
        axisTick: {
          show: true
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
          formatter: (value: any) => formatValue(value, this.decimals, this.units, false)
        }
      },
      series: [{
        type: 'line',
        name: dataKey?.label,
        smooth: false,
        showSymbol: false,
        animation: true,
        areaStyle: this.settings.fillArea ? {} : undefined,
        data: this.ctx.data?.length ? toNamedData(this.ctx.data[0].data) : [],
        markLine: this.rangeItems.length ? {
          animation: true,
          symbol: ['circle', 'arrow'],
          symbolSize: [5, 7],
          lineStyle: {
            width: 1,
            type: [3, 3],
            color: '#37383b'
          },
          label: {
            position: 'insideEndTop',
            color: '#37383b',
            backgroundColor: 'rgba(255,255,255,0.56)',
            padding: [4, 5],
            borderRadius: 4,
            formatter: params => formatValue(params.value, this.decimals, this.units, false)
          },
          emphasis: {
            disabled: true
          },
          data: getMarkPoints(this.rangeItems).map(point => ({ yAxis: point }))
        } : undefined
      }],
      dataZoom: [
        {
          type: 'inside',
          disabled: !this.settings.dataZoom
        },
        {
          type: 'slider',
          show: this.settings.dataZoom,
          showDetail: false,
          right: 10
        }
      ],
      visualMap: {
        show: false,
        type: 'piecewise',
        selected: this.selectedRanges,
        dimension: 1,
        pieces: this.rangeItems.map(item => item.piece),
        outOfRange: {
          color: this.settings.outOfRangeColor
        },
        inRange: !this.rangeItems.length ? {
          color: this.settings.outOfRangeColor
        } : undefined
      }
    };

    (this.rangeChartOptions.xAxis as any).tbTimeWindow = this.ctx.defaultSubscription.timeWindow;

    this.rangeChart.setOption(this.rangeChartOptions);

    this.shapeResize$ = new ResizeObserver(() => {
      this.onResize();
    });
    this.shapeResize$.observe(this.chartShape.nativeElement);
    this.onResize();
  }

  private onResize() {
    const width = this.rangeChart.getWidth();
    const height = this.rangeChart.getHeight();
    const shapeWidth = this.chartShape.nativeElement.offsetWidth;
    const shapeHeight = this.chartShape.nativeElement.offsetHeight;
    if (width !== shapeWidth || height !== shapeHeight) {
      this.rangeChart.resize();
    }
  }
}
