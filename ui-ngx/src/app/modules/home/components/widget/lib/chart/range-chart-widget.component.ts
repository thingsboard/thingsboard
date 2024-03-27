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
  filterIncludingColorRanges,
  getDataKey,
  overlayStyle,
  sortedColorRange,
  textStyle
} from '@shared/models/widget-settings.models';
import { isDefinedAndNotNull, isNumber } from '@core/utils';
import { rangeChartDefaultSettings, RangeChartWidgetSettings } from './range-chart-widget.models';
import { Observable } from 'rxjs';
import { ImagePipe } from '@shared/pipe/image.pipe';
import { DomSanitizer } from '@angular/platform-browser';
import { DeepPartial } from '@shared/models/common';
import {
  createTimeSeriesChartVisualMapPiece,
  SeriesFillType,
  TimeSeriesChartKeySettings,
  TimeSeriesChartSettings,
  TimeSeriesChartShape,
  TimeSeriesChartThreshold,
  TimeSeriesChartThresholdType, TimeSeriesChartVisualMapPiece
} from '@home/components/widget/lib/chart/time-series-chart.models';
import { TbTimeSeriesChart } from '@home/components/widget/lib/chart/time-series-chart';

interface RangeItem {
  index: number;
  from?: number;
  to?: number;
  color: string;
  label: string;
  visible: boolean;
  enabled: boolean;
  piece: TimeSeriesChartVisualMapPiece;
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
        piece: createTimeSeriesChartVisualMapPiece(range.color, from, to)
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

  private decimals = 0;
  private units = '';

  private rangeItems: RangeItem[];

  private timeSeriesChart: TbTimeSeriesChart;

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
    if (dataKey) {
      dataKey.settings = {
        type: 'line',
        lineSettings: {
          showLine: true,
          smooth: false,
          showPoints: false,
          fillAreaSettings: {
            type: this.settings.fillArea ? 'default' : SeriesFillType.none
          }
        }
      } as DeepPartial<TimeSeriesChartKeySettings>;
    }

    this.backgroundStyle$ = backgroundStyle(this.settings.background, this.imagePipe, this.sanitizer);
    this.overlayStyle = overlayStyle(this.settings.background.overlay);

    this.rangeItems = toRangeItems(this.settings.rangeColors);
    this.visibleRangeItems = this.rangeItems.filter(item => item.visible);

    this.showLegend = this.settings.showLegend && !!this.rangeItems.length;

    if (this.showLegend) {
      this.legendClass = `legend-${this.settings.legendPosition}`;
      this.legendLabelStyle = textStyle(this.settings.legendLabelFont);
      this.disabledLegendLabelStyle = textStyle(this.settings.legendLabelFont);
      this.legendLabelStyle.color = this.settings.legendLabelColor;
    }
  }

  ngAfterViewInit() {
    const thresholds: DeepPartial<TimeSeriesChartThreshold>[] = getMarkPoints(this.rangeItems).map(item => ({
      type: TimeSeriesChartThresholdType.constant,
      yAxisId: 'default',
      units: this.units,
      decimals: this.decimals,
      lineWidth: 1,
      lineColor: '#37383b',
      lineType: [3, 3],
      startSymbol: TimeSeriesChartShape.circle,
      startSymbolSize: 5,
      endSymbol: TimeSeriesChartShape.arrow,
      endSymbolSize: 7,
      showLabel: true,
      labelPosition: 'insideEndTop',
      labelColor: '#37383b',
      additionalLabelOption: {
        backgroundColor: 'rgba(255,255,255,0.56)',
        padding: [4, 5],
        borderRadius: 4,
      },
      value: item
    } as DeepPartial<TimeSeriesChartThreshold>));
    const settings: DeepPartial<TimeSeriesChartSettings> = {
      dataZoom: this.settings.dataZoom,
      thresholds,
      yAxes: {
        default: {
          show: true,
          showLine: false,
          showTicks: false,
          showTickLabels: true,
          showSplitLines: true,
          decimals: this.decimals,
          units: this.units
        }
      },
      xAxis: {
        show: true,
        showLine: true,
        showTicks: true,
        showTickLabels: true,
        showSplitLines: false
      },
      visualMapSettings: {
        outOfRangeColor: this.settings.outOfRangeColor,
        pieces: this.rangeItems.map(item => item.piece)
      },
      showTooltip: this.settings.showTooltip,
      tooltipValueFont: this.settings.tooltipValueFont,
      tooltipValueColor: this.settings.tooltipValueColor,
      tooltipShowDate: this.settings.tooltipShowDate,
      tooltipDateInterval: this.settings.tooltipDateInterval,
      tooltipDateFormat: this.settings.tooltipDateFormat,
      tooltipDateFont: this.settings.tooltipDateFont,
      tooltipDateColor: this.settings.tooltipDateColor,
      tooltipBackgroundColor: this.settings.tooltipBackgroundColor,
      tooltipBackgroundBlur: this.settings.tooltipBackgroundBlur,
    };
    this.timeSeriesChart = new TbTimeSeriesChart(this.ctx, settings, this.chartShape.nativeElement, this.renderer);
  }

  ngOnDestroy() {
    if (this.timeSeriesChart) {
      this.timeSeriesChart.destroy();
    }
  }

  public onInit() {
    const borderRadius = this.ctx.$widgetElement.css('borderRadius');
    this.overlayStyle = {...this.overlayStyle, ...{borderRadius}};
    this.cd.detectChanges();
  }

  public onDataUpdated() {
    if (this.timeSeriesChart) {
      this.timeSeriesChart.update();
    }
  }

  public toggleRangeItem(item: RangeItem) {
    item.enabled = !item.enabled;
    this.timeSeriesChart.toggleVisualMapRange(item.index);
  }
}
