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
import {
  timeSeriesChartKeyDefaultSettings,
  TimeSeriesChartKeySettings
} from '@home/components/widget/lib/chart/time-series-chart.models';
import { WidgetContext } from '@home/models/widget-component.models';
import { Observable } from 'rxjs';
import { backgroundStyle, ComponentStyle, overlayStyle, textStyle } from '@shared/models/widget-settings.models';
import { ImagePipe } from '@shared/pipe/image.pipe';
import { DomSanitizer } from '@angular/platform-browser';
import { LegendConfig, LegendData, LegendKey, LegendPosition } from '@shared/models/widget.models';
import { TbTimeSeriesChart } from '@home/components/widget/lib/chart/time-series-chart';
import {
  timeSeriesChartWidgetDefaultSettings,
  TimeSeriesChartWidgetSettings
} from '@home/components/widget/lib/chart/time-series-chart-widget.models';
import { mergeDeep } from '@core/utils';
import { WidgetComponent } from '@home/components/widget/widget.component';

@Component({
    selector: 'tb-time-series-chart-widget',
    templateUrl: './time-series-chart-widget.component.html',
    styleUrls: ['./time-series-chart-widget.component.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class TimeSeriesChartWidgetComponent implements OnInit, OnDestroy, AfterViewInit {

  @ViewChild('chartShape', {static: false})
  chartShape: ElementRef<HTMLElement>;

  settings: TimeSeriesChartWidgetSettings;

  @Input()
  ctx: WidgetContext;

  @Input()
  widgetTitlePanel: TemplateRef<any>;

  horizontalLegendPosition = false;

  showLegend: boolean;
  legendClass: string;
  legendConfig: LegendConfig;
  legendData: LegendData;
  legendKeys: LegendKey[];

  backgroundStyle$: Observable<ComponentStyle>;
  overlayStyle: ComponentStyle = {};
  padding: string;

  legendColumnTitleStyle: ComponentStyle;
  legendLabelStyle: ComponentStyle;
  disabledLegendLabelStyle: ComponentStyle;
  legendValueStyle: ComponentStyle;

  displayLegendValues = false;

  private timeSeriesChart: TbTimeSeriesChart;

  constructor(public widgetComponent: WidgetComponent,
              private imagePipe: ImagePipe,
              private sanitizer: DomSanitizer,
              private renderer: Renderer2,
              private cd: ChangeDetectorRef) {
  }

  ngOnInit(): void {
    this.ctx.$scope.timeSeriesChartWidget = this;
    this.settings = {...timeSeriesChartWidgetDefaultSettings, ...this.ctx.settings};

    this.backgroundStyle$ = backgroundStyle(this.settings.background, this.imagePipe, this.sanitizer);
    this.overlayStyle = overlayStyle(this.settings.background.overlay);
    this.padding = this.settings.background.overlay.enabled ? undefined : this.settings.padding;

    this.showLegend = this.settings.showLegend;
    if (this.showLegend) {
      this.legendData = this.ctx.defaultSubscription.legendData;
      this.legendConfig = this.settings.legendConfig;
      this.legendKeys = this.legendData.keys;
      if (this.legendConfig.sortDataKeys) {
        this.legendKeys = this.legendData.keys.sort((key1, key2) => key1.dataKey.label.localeCompare(key2.dataKey.label));
      }
      this.legendKeys.forEach(legendKey => {
        legendKey.dataKey.settings = mergeDeep<TimeSeriesChartKeySettings>({} as TimeSeriesChartKeySettings,
          timeSeriesChartKeyDefaultSettings, legendKey.dataKey.settings);
        legendKey.dataKey.hidden = legendKey.dataKey.settings.dataHiddenByDefault;
      });
      this.legendKeys = this.legendKeys.filter(legendKey => legendKey.dataKey.settings.showInLegend);
      if (!this.legendKeys.length) {
        this.showLegend = false;
      }
    }

    if (this.showLegend) {
      this.horizontalLegendPosition = [LegendPosition.left, LegendPosition.right].includes(this.legendConfig.position);
      this.legendClass = `legend-${this.legendConfig.position}`;
      this.legendColumnTitleStyle = textStyle(this.settings.legendColumnTitleFont);
      this.legendColumnTitleStyle.color = this.settings.legendColumnTitleColor;
      this.legendLabelStyle = textStyle(this.settings.legendLabelFont);
      this.disabledLegendLabelStyle = textStyle(this.settings.legendLabelFont);
      this.legendLabelStyle.color = this.settings.legendLabelColor;
      this.legendValueStyle = textStyle(this.settings.legendValueFont);
      this.legendValueStyle.color = this.settings.legendValueColor;
      this.displayLegendValues = this.legendConfig.showMin || this.legendConfig.showMax ||
        this.legendConfig.showAvg || this.legendConfig.showTotal || this.legendConfig.showLatest;
    }
  }

  ngAfterViewInit() {
    this.timeSeriesChart = new TbTimeSeriesChart(this.ctx, this.settings, this.chartShape.nativeElement, this.renderer);
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

  public onLatestDataUpdated() {
    if (this.timeSeriesChart) {
      this.timeSeriesChart.latestUpdated();
    }
  }

  public onLegendKeyEnter(legendKey: LegendKey) {
    this.timeSeriesChart.keyEnter(legendKey.dataKey);
  }

  public onLegendKeyLeave(legendKey: LegendKey) {
    this.timeSeriesChart.keyLeave(legendKey.dataKey);
  }

  public toggleLegendKey(legendKey: LegendKey) {
    this.timeSeriesChart.toggleKey(legendKey.dataKey, legendKey.dataIndex);
  }
}
