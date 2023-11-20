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
import { formatValue, isDefinedAndNotNull, isNumeric } from '@core/utils';
import {
  backgroundStyle,
  ColorProcessor,
  ComponentStyle,
  getDataKey,
  overlayStyle, resolveCssSize,
  textStyle
} from '@shared/models/widget-settings.models';
import { WidgetComponent } from '@home/components/widget/widget.component';
import { ResizeObserver } from '@juggle/resize-observer';
import {
  valueChartCardDefaultSettings,
  ValueChartCardLayout,
  ValueChartCardWidgetSettings
} from '@home/components/widget/lib/cards/value-chart-card-widget.models';
import { TbFlot } from '@home/components/widget/lib/flot-widget';
import { DataKey } from '@shared/models/widget.models';
import { TbFlotKeySettings, TbFlotSettings } from '@home/components/widget/lib/flot-widget.models';
import { getTsValueByLatestDataKey } from '@home/components/widget/lib/cards/aggregated-value-card.models';

const layoutHeight = 56;
const valueRelativeMaxWidth = 0.5;

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

  showValue = true;
  valueText = 'N/A';
  valueStyle: ComponentStyle = {};
  valueColor: ColorProcessor;

  backgroundStyle: ComponentStyle = {};
  overlayStyle: ComponentStyle = {};

  private flot: TbFlot;
  private flotDataKey: DataKey;

  private valueKey: DataKey;
  private contentResize$: ResizeObserver;

  private decimals = 0;
  private units = '';

  private valueFontSize: number;

  constructor(private renderer: Renderer2,
              private widgetComponent: WidgetComponent,
              private cd: ChangeDetectorRef) {
  }

  ngOnInit(): void {
    this.ctx.$scope.valueChartCardWidget = this;
    this.settings = {...valueChartCardDefaultSettings, ...this.ctx.settings};

    if (this.showValue) {
      this.decimals = this.ctx.decimals;
      this.units = this.ctx.units;
      const dataKey = getDataKey(this.ctx.datasources);
      if (dataKey?.name && this.ctx.defaultSubscription.firstDatasource?.latestDataKeys?.length) {
        const dataKeys = this.ctx.defaultSubscription.firstDatasource?.latestDataKeys;
        this.valueKey = dataKeys?.find(k => k.name === dataKey.name);
        if (isDefinedAndNotNull(this.valueKey?.decimals)) {
          this.decimals = this.valueKey.decimals;
        }
        if (this.valueKey?.units) {
          this.units = dataKey.units;
        }
      }
    }

    this.layout = this.settings.layout;

    this.showValue = this.settings.showValue;
    this.valueStyle = textStyle(this.settings.valueFont);
    this.valueColor = ColorProcessor.fromSettings(this.settings.valueColor);

    this.backgroundStyle = backgroundStyle(this.settings.background);
    this.overlayStyle = overlayStyle(this.settings.background.overlay);

    if (this.ctx.defaultSubscription.firstDatasource?.dataKeys?.length) {
      this.flotDataKey = this.ctx.defaultSubscription.firstDatasource?.dataKeys[0];
      this.flotDataKey.settings = {
        fillLines: false,
        showLines: true,
        lineWidth: 2
      } as TbFlotKeySettings;
    }
  }

  public ngAfterViewInit() {
    const settings = {
      shadowSize: 0,
      enableSelection: false,
      smoothLines: true,
      grid: {
        tickColor: 'rgba(0,0,0,0.12)',
        horizontalLines: false,
        verticalLines: false,
        outlineWidth: 0,
        minBorderMargin: 0,
        margin: 0
      },
      yaxis: {
        showLabels: false,
        tickGenerator: 'return [(axis.max + axis.min) / 2];'
      },
      xaxis: {
        showLabels: false
      }
    } as TbFlotSettings;
    this.flot = new TbFlot(this.ctx, 'line', $(this.chartElement.nativeElement), settings);

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
    this.flot.update();
  }

  public onLatestDataUpdated() {
    if (this.showValue && this.valueKey) {
      const tsValue = getTsValueByLatestDataKey(this.ctx.latestData, this.valueKey);
      let value;
      if (tsValue && isDefinedAndNotNull(tsValue[1]) && isNumeric(tsValue[1])) {
        value = tsValue[1];
        this.valueText = formatValue(value, this.decimals, this.units, false);
      } else {
        this.valueText = 'N/A';
      }
      this.valueColor.update(value);
      this.cd.detectChanges();
      setTimeout(() => {
        this.onResize(false);
      }, 0);
    }
  }

  public onEditModeChanged() {
    this.flot.checkMouseEvents();
  }

  public onDestroy() {
    this.flot.destroy();
  }

  private onResize(fitTargetHeight = true) {
    if (this.settings.autoScale && this.showValue) {
      const contentWidth = this.valueChartCardContent.nativeElement.getBoundingClientRect().width;
      const contentHeight = this.valueChartCardContent.nativeElement.getBoundingClientRect().height;
      if (!this.valueFontSize) {
        const fontSize = getComputedStyle(this.valueChartCardValue.nativeElement).fontSize;
        this.valueFontSize = resolveCssSize(fontSize)[0];
      }
      const valueRelativeHeight = Math.min(this.valueFontSize / layoutHeight, 1);
      const targetValueHeight = contentHeight * valueRelativeHeight;
      const maxValueWidth = contentWidth * valueRelativeMaxWidth;
      this.setValueFontSize(targetValueHeight, maxValueWidth, fitTargetHeight);
    }
    this.flot.resize();
  }

  private setValueFontSize(targetHeight: number, maxWidth: number, fitTargetHeight = true) {
    const fontSize = getComputedStyle(this.valueChartCardValue.nativeElement).fontSize;
    let valueFontSize = resolveCssSize(fontSize)[0];
    this.renderer.setStyle(this.valueChartCardValue.nativeElement, 'fontSize', valueFontSize + 'px');
    this.renderer.setStyle(this.valueChartCardValue.nativeElement, 'lineHeight', '1');
    let valueHeight = this.valueChartCardValue.nativeElement.getBoundingClientRect().height;
    while (fitTargetHeight && valueHeight < targetHeight) {
      valueFontSize++;
      this.renderer.setStyle(this.valueChartCardValue.nativeElement, 'fontSize', valueFontSize + 'px');
      valueHeight = this.valueChartCardValue.nativeElement.getBoundingClientRect().height;
    }
    let valueWidth = this.valueChartCardValue.nativeElement.getBoundingClientRect().width;
    while ((valueHeight > targetHeight || valueWidth > maxWidth) && valueFontSize > 6) {
      valueFontSize--;
      this.renderer.setStyle(this.valueChartCardValue.nativeElement, 'fontSize', valueFontSize + 'px');
      valueWidth = this.valueChartCardValue.nativeElement.getBoundingClientRect().width;
      valueHeight = this.valueChartCardValue.nativeElement.getBoundingClientRect().height;
    }
  }

}
