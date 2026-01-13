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
  ViewChild
} from '@angular/core';
import {
  aggregatedValueCardDefaultSettings,
  AggregatedValueCardKeyPosition,
  AggregatedValueCardValue,
  AggregatedValueCardWidgetSettings,
  computeAggregatedCardValue,
  getTsValueByLatestDataKey
} from '@home/components/widget/lib/cards/aggregated-value-card.models';
import { WidgetContext } from '@home/models/widget-component.models';
import { Observable } from 'rxjs';
import {
  autoDateFormat,
  backgroundStyle,
  ComponentStyle,
  DateFormatProcessor,
  getDataKey,
  getLatestSingleTsValue,
  overlayStyle,
  textStyle
} from '@shared/models/widget-settings.models';
import { DataKey } from '@shared/models/widget.models';
import { formatNumberValue, formatValue, isDefined, isDefinedAndNotNull, isNumeric } from '@core/utils';
import { map } from 'rxjs/operators';
import { ImagePipe } from '@shared/pipe/image.pipe';
import { DomSanitizer } from '@angular/platform-browser';
import { TbTimeSeriesChart } from '@home/components/widget/lib/chart/time-series-chart';
import {
  TimeSeriesChartKeySettings,
  TimeSeriesChartSeriesType,
  TimeSeriesChartSettings
} from '@home/components/widget/lib/chart/time-series-chart.models';
import { DeepPartial } from '@shared/models/common';

const valuesLayoutHeight = 66;
const valuesLayoutVerticalPadding = 16;

@Component({
  selector: 'tb-aggregated-value-card-widget',
  templateUrl: './aggregated-value-card-widget.component.html',
  styleUrls: ['./aggregated-value-card-widget.component.scss']
})
export class AggregatedValueCardWidgetComponent implements OnInit, AfterViewInit, OnDestroy {

  @ViewChild('chartElement', {static: false}) chartElement: ElementRef;

  @ViewChild('valueCardValues', {static: false})
  valueCardValues: ElementRef<HTMLElement>;

  @ViewChild('valueCardValueContainer', {static: false})
  valueCardValueContainer: ElementRef<HTMLElement>;

  aggregatedValueCardKeyPosition = AggregatedValueCardKeyPosition;

  settings: AggregatedValueCardWidgetSettings;

  @Input()
  ctx: WidgetContext;

  @Input()
  widgetTitlePanel: TemplateRef<any>;

  showSubtitle = true;
  subtitle$: Observable<string>;
  subtitleStyle: ComponentStyle = {};
  subtitleColor: string;

  showValues = false;

  values: {[key: string]: AggregatedValueCardValue} = {};

  showChart = true;

  showDate = true;
  dateFormat: DateFormatProcessor;
  dateStyle: ComponentStyle = {};
  dateColor: string;

  backgroundStyle$: Observable<ComponentStyle>;
  overlayStyle: ComponentStyle = {};
  padding: string;

  private lineChart: TbTimeSeriesChart;
  private lineChartDataKey: DataKey;

  private lastUpdateTs: number;

  tickMin$: Observable<string>;
  tickMax$: Observable<string>;

  private panelResize$: ResizeObserver;

  constructor(private imagePipe: ImagePipe,
              private sanitizer: DomSanitizer,
              private renderer: Renderer2,
              private cd: ChangeDetectorRef) {
  }

  ngOnInit(): void {
    this.ctx.$scope.aggregatedValueCardWidget = this;
    this.settings = {...aggregatedValueCardDefaultSettings, ...this.ctx.settings};
    this.showSubtitle = this.settings.showSubtitle && this.ctx.datasources?.length > 0;
    const subtitle = this.settings.subtitle;
    this.subtitle$ = this.ctx.registerLabelPattern(subtitle, this.subtitle$);
    this.subtitleStyle = textStyle(this.settings.subtitleFont);
    this.subtitleColor =  this.settings.subtitleColor;

    const dataKey = getDataKey(this.ctx.defaultSubscription.datasources);
    if (dataKey?.name && this.ctx.defaultSubscription.firstDatasource?.latestDataKeys?.length) {
      const dataKeys = this.ctx.defaultSubscription.firstDatasource?.latestDataKeys;
      for (const position of Object.keys(AggregatedValueCardKeyPosition)) {
        const value = computeAggregatedCardValue(dataKeys, dataKey?.name, AggregatedValueCardKeyPosition[position], this.ctx.$injector, this.ctx.decimals);
        if (value) {
          this.values[position] = value;
        }
      }
      this.showValues = !!Object.keys(this.values).length;
    }

    this.showChart = this.settings.showChart;
    if (this.showChart) {
      if (this.ctx.defaultSubscription.firstDatasource?.dataKeys?.length) {
        this.lineChartDataKey = this.ctx.defaultSubscription.firstDatasource?.dataKeys[0];
        this.lineChartDataKey.settings = {
          type: TimeSeriesChartSeriesType.line,
          lineSettings: {
            showLine: true,
            step: false,
            smooth: false,
            lineWidth: 2,
            showPoints: false,
            showPointLabel: false
          }
        } as TimeSeriesChartKeySettings;
      }
    }

    this.showDate = this.settings.showDate;
    this.dateFormat = DateFormatProcessor.fromSettings(this.ctx.$injector, this.settings.dateFormat);
    this.dateStyle = textStyle(this.settings.dateFont);
    this.dateColor = this.settings.dateColor;

    this.backgroundStyle$ = backgroundStyle(this.settings.background, this.imagePipe, this.sanitizer);
    this.overlayStyle = overlayStyle(this.settings.background.overlay);
    this.padding = this.settings.background.overlay.enabled ? undefined : this.settings.padding;
  }

  ngAfterViewInit(): void {
    if (this.showChart && this.ctx.datasources?.length) {
      const settings: DeepPartial<TimeSeriesChartSettings> = {
          dataZoom: false,
          xAxis: {
            show: false
          },
          yAxes: {
            default: {
              show: true,
              showLine: false,
              showTicks: false,
              showTickLabels: false,
              showSplitLines: true,
              min: 'dataMin',
              max: 'dataMax',
              ticksGenerator: (extent?: number[]) => (extent ? [{ value: (extent[0] + extent[1]) / 2}] : [])
            }
          },
          tooltipDateInterval: false,
          tooltipDateFormat: autoDateFormat()
      };

      this.lineChart = new TbTimeSeriesChart(this.ctx, settings, this.chartElement.nativeElement, this.renderer, true);

      this.tickMin$ =this.lineChart.yMin$.pipe(
        map((value) => formatValue(value, (this.lineChartDataKey?.decimals || this.ctx.decimals))
      ));
      this.tickMax$ = this.lineChart.yMax$.pipe(
        map((value) => formatValue(value, (this.lineChartDataKey?.decimals || this.ctx.decimals))
      ));
    }
    if (this.settings.autoScale && this.showValues) {
      this.renderer.setStyle(this.valueCardValueContainer.nativeElement, 'height', valuesLayoutHeight + 'px');
      this.renderer.setStyle(this.valueCardValueContainer.nativeElement, 'overflow', 'visible');
      this.renderer.setStyle(this.valueCardValueContainer.nativeElement, 'position', 'absolute');
      this.panelResize$ = new ResizeObserver(() => {
        this.onValueCardValuesResize();
      });
      this.panelResize$.observe(this.valueCardValues.nativeElement);
      this.onValueCardValuesResize();
    }
  }

  ngOnDestroy() {
    if (this.panelResize$) {
      this.panelResize$.disconnect();
    }
  }

  public onInit() {
    const borderRadius = this.ctx.$widgetElement.css('borderRadius');
    this.overlayStyle = {...this.overlayStyle, ...{borderRadius}};
    this.cd.detectChanges();
  }

  public onDataUpdated() {
    const tsValue = getLatestSingleTsValue(this.ctx.data);
    let ts;
    if (tsValue) {
      ts = tsValue[0];
    }

    if (this.showChart) {
      this.lineChart.update();
    }

    this.updateLastUpdateTs(ts);
    this.cd.detectChanges();
  }

  public onLatestDataUpdated() {
    if (this.showValues) {
      for (const aggValue of Object.values(this.values)) {
        const tsValue = getTsValueByLatestDataKey(this.ctx.latestData, aggValue.key);
        let ts;
        let value;
        if (tsValue && isDefinedAndNotNull(tsValue[1]) && isNumeric(tsValue[1])) {
          ts = tsValue[0];
          value = tsValue[1];
          aggValue.value = aggValue.valueFormat.format(value);
        } else {
          aggValue.value = 'N/A';
        }
        aggValue.color.update(value);
        const numeric = formatNumberValue(value, (aggValue.key.decimals || this.ctx.decimals));
        if (aggValue.showArrow && isDefined(numeric)) {
          aggValue.upArrow = numeric > 0;
          aggValue.downArrow = numeric < 0;
        } else {
          aggValue.upArrow = aggValue.downArrow = false;
        }
        this.updateLastUpdateTs(ts);
      }
      this.cd.detectChanges();
    }
  }

  public onResize() {
  }

  public onEditModeChanged() {
  }

  public onDestroy() {
    if (this.showChart) {
      this.lineChart.destroy();
    }
  }

  private updateLastUpdateTs(ts: number) {
    if (ts && (!this.lastUpdateTs || ts > this.lastUpdateTs)) {
      this.lastUpdateTs = ts;
      this.dateFormat.update(ts);
    }
  }

  private onValueCardValuesResize() {
    const panelWidth = this.valueCardValues.nativeElement.getBoundingClientRect().width;
    const panelHeight = this.valueCardValues.nativeElement.getBoundingClientRect().height - valuesLayoutVerticalPadding;
    const targetWidth = panelWidth;
    const minAspect = 0.25;
    const aspect = Math.min(panelHeight / targetWidth, minAspect);
    const targetHeight = targetWidth * aspect;
    const scale = targetHeight / valuesLayoutHeight;
    const width = targetWidth / scale;
    this.renderer.setStyle(this.valueCardValueContainer.nativeElement, 'width', width + 'px');
    this.renderer.setStyle(this.valueCardValueContainer.nativeElement, 'transform', `scale(${scale})`);
  }

}
