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
  ViewChild,
  ViewEncapsulation
} from '@angular/core';
import { WidgetContext } from '@home/models/widget-component.models';
import { isDefinedAndNotNull, isNumeric } from '@core/utils';
import {
  autoDateFormat,
  backgroundStyle,
  ColorProcessor,
  ComponentStyle,
  getDataKey,
  overlayStyle,
  resolveCssSize,
  textStyle,
  ValueFormatProcessor
} from '@shared/models/widget-settings.models';
import {
  valueChartCardDefaultSettings,
  ValueChartCardLayout,
  ValueChartCardWidgetSettings
} from '@home/components/widget/lib/cards/value-chart-card-widget.models';
import { DataKey } from '@shared/models/widget.models';
import { getTsValueByLatestDataKey } from '@home/components/widget/lib/cards/aggregated-value-card.models';
import { Observable } from 'rxjs';
import { ImagePipe } from '@shared/pipe/image.pipe';
import { DomSanitizer } from '@angular/platform-browser';
import { TbTimeSeriesChart } from '@home/components/widget/lib/chart/time-series-chart';
import {
  TimeSeriesChartKeySettings,
  TimeSeriesChartSeriesType,
  TimeSeriesChartSettings
} from '@home/components/widget/lib/chart/time-series-chart.models';
import { DeepPartial } from '@shared/models/common';

const layoutHeight = 56;
const valueRelativeWidth = 0.35;

@Component({
  selector: 'tb-value-chart-card-widget',
  templateUrl: './value-chart-card-widget.component.html',
  styleUrls: ['./value-chart-card-widget.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class ValueChartCardWidgetComponent implements OnInit, AfterViewInit, OnDestroy {

  @ViewChild('chartElement', {static: false})
  chartElement: ElementRef;

  @ViewChild('valueChartCardContent', {static: false})
  valueChartCardContent: ElementRef<HTMLElement>;

  @ViewChild('valueChartCardValue', {static: false})
  valueChartCardValue: ElementRef<HTMLElement>;

  settings: ValueChartCardWidgetSettings;

  @Input()
  ctx: WidgetContext;

  @Input()
  widgetTitlePanel: TemplateRef<any>;

  layout: ValueChartCardLayout;
  autoScale: boolean;

  showValue = true;
  valueText = 'N/A';
  valueStyle: ComponentStyle = {};
  valueColor: ColorProcessor;

  backgroundStyle$: Observable<ComponentStyle>;
  overlayStyle: ComponentStyle = {};
  padding: string;

  private lineChart: TbTimeSeriesChart;
  private lineChartDataKey: DataKey;

  private valueKey: DataKey;
  private contentResize$: ResizeObserver;

  private valueFontSize: number;
  private valueFormat: ValueFormatProcessor;

  constructor(private imagePipe: ImagePipe,
              private sanitizer: DomSanitizer,
              private renderer: Renderer2,
              private cd: ChangeDetectorRef) {
  }

  ngOnInit(): void {
    this.ctx.$scope.valueChartCardWidget = this;
    this.settings = {...valueChartCardDefaultSettings, ...this.ctx.settings};

    if (this.showValue) {
      let decimals = this.ctx.decimals ?? 0;
      let units = this.ctx.units ?? '';
      const dataKey = getDataKey(this.ctx.datasources);
      if (dataKey?.name && this.ctx.defaultSubscription.firstDatasource?.latestDataKeys?.length) {
        const dataKeys = this.ctx.defaultSubscription.firstDatasource?.latestDataKeys;
        this.valueKey = dataKeys?.find(k => k.name === dataKey.name);
        if (isDefinedAndNotNull(this.valueKey?.decimals)) {
          decimals = this.valueKey.decimals;
        }
        if (this.valueKey?.units) {
          units = dataKey.units;
        }
      }
      this.valueFormat = ValueFormatProcessor.fromSettings(this.ctx.$injector, {units: units, decimals: decimals});
    }

    this.layout = this.settings.layout;
    this.autoScale = this.settings.autoScale;

    this.showValue = this.settings.showValue;
    this.valueStyle = textStyle(this.settings.valueFont);
    this.valueColor = ColorProcessor.fromSettings(this.settings.valueColor);

    this.backgroundStyle$ = backgroundStyle(this.settings.background, this.imagePipe, this.sanitizer);
    this.overlayStyle = overlayStyle(this.settings.background.overlay);
    this.padding = this.settings.background.overlay.enabled ? undefined : this.settings.padding;

    if (this.ctx.defaultSubscription.firstDatasource?.dataKeys?.length) {
      this.lineChartDataKey = this.ctx.defaultSubscription.firstDatasource?.dataKeys[0];
      this.lineChartDataKey.settings = {
        type: TimeSeriesChartSeriesType.line,
        lineSettings: {
          showLine: true,
          step: false,
          smooth: true,
          lineWidth: 2,
          showPoints: false,
          showPointLabel: false
        }
      } as TimeSeriesChartKeySettings;
    }
  }

  public ngAfterViewInit() {
    const settings: DeepPartial<TimeSeriesChartSettings> = {
      dataZoom: false,
      xAxis: {
        show: false
      },
      yAxes: {
        default: {
          show: false,
        }
      },
      tooltipDateInterval: false,
      tooltipDateFormat: autoDateFormat()
    };

    this.lineChart = new TbTimeSeriesChart(this.ctx, settings, this.chartElement.nativeElement, this.renderer, false);

    this.contentResize$ = new ResizeObserver(() => {
      this.onResize();
    });
    this.contentResize$.observe(this.valueChartCardContent.nativeElement);
    this.onResize();
  }

  ngOnDestroy() {
    if (this.contentResize$) {
      this.contentResize$.disconnect();
    }
  }

  public onInit() {
    const borderRadius = this.ctx.$widgetElement.css('borderRadius');
    this.overlayStyle = {...this.overlayStyle, ...{borderRadius}};
    this.cd.detectChanges();
  }

  public onDataUpdated() {
    if (this.lineChart) {
      this.lineChart.update();
    }
  }

  public onLatestDataUpdated() {
    if (this.showValue && this.valueKey) {
      const tsValue = getTsValueByLatestDataKey(this.ctx.latestData, this.valueKey);
      let value;
      if (tsValue && isDefinedAndNotNull(tsValue[1]) && isNumeric(tsValue[1])) {
        value = tsValue[1];
        this.valueText = this.valueFormat?.format(value);
      } else {
        this.valueText = 'N/A';
      }
      this.valueColor.update(value);
      this.cd.detectChanges();
      setTimeout(() => {
        this.onResize();
      }, 0);
    }
  }

  public onEditModeChanged() {
  }

  public onDestroy() {
    if (this.lineChart) {
      this.lineChart.destroy();
    }
  }

  private onResize(fitTargetWidth = true) {
    if (this.settings.autoScale && this.showValue) {
      const contentWidth = this.valueChartCardContent.nativeElement.getBoundingClientRect().width;
      const contentHeight = this.valueChartCardContent.nativeElement.getBoundingClientRect().height;
      if (!this.valueFontSize) {
        const fontSize = getComputedStyle(this.valueChartCardValue.nativeElement).fontSize;
        this.valueFontSize = resolveCssSize(fontSize)[0];
      }
      const valueRelativeHeight = Math.min(this.valueFontSize / layoutHeight, 1);
      const targetValueWidth = contentWidth * valueRelativeWidth;
      const maxValueHeight = contentHeight * valueRelativeHeight;
      this.setValueFontSize(targetValueWidth, maxValueHeight, fitTargetWidth);
    }
    this.lineChart.resize();
  }

  private setValueFontSize(targetWidth: number, maxHeight: number, fitTargetWidth = true) {
    const fontSize = getComputedStyle(this.valueChartCardValue.nativeElement).fontSize;
    let valueFontSize = resolveCssSize(fontSize)[0];
    this.renderer.setStyle(this.valueChartCardValue.nativeElement, 'fontSize', valueFontSize + 'px');
    this.renderer.setStyle(this.valueChartCardValue.nativeElement, 'lineHeight', '1');
    let valueWidth = this.valueChartCardValue.nativeElement.getBoundingClientRect().width;
    while (fitTargetWidth && valueWidth < targetWidth) {
      valueFontSize++;
      this.renderer.setStyle(this.valueChartCardValue.nativeElement, 'fontSize', valueFontSize + 'px');
      valueWidth = this.valueChartCardValue.nativeElement.getBoundingClientRect().width;
    }
    let valueHeight = this.valueChartCardValue.nativeElement.getBoundingClientRect().height;
    while ((valueWidth > targetWidth || valueHeight > maxHeight) && valueFontSize > 6) {
      valueFontSize--;
      this.renderer.setStyle(this.valueChartCardValue.nativeElement, 'fontSize', valueFontSize + 'px');
      valueWidth = this.valueChartCardValue.nativeElement.getBoundingClientRect().width;
      valueHeight = this.valueChartCardValue.nativeElement.getBoundingClientRect().height;
    }
  }

}
