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
import { backgroundStyle, ComponentStyle, overlayStyle, textStyle } from '@shared/models/widget-settings.models';
import { Observable } from 'rxjs';
import { ImagePipe } from '@shared/pipe/image.pipe';
import { DomSanitizer } from '@angular/platform-browser';
import {
  barChartWithLabelsDefaultSettings,
  barChartWithLabelsTimeSeriesKeySettings,
  barChartWithLabelsTimeSeriesSettings,
  BarChartWithLabelsWidgetSettings
} from '@home/components/widget/lib/chart/bar-chart-with-labels-widget.models';
import { TbTimeSeriesChart } from '@home/components/widget/lib/chart/time-series-chart';
import { DataKey } from '@shared/models/widget.models';
import { WidgetComponent } from '@home/components/widget/widget.component';

@Component({
    selector: 'tb-bar-chart-with-labels-widget',
    templateUrl: './bar-chart-with-labels-widget.component.html',
    styleUrls: ['./bar-chart-with-labels-widget.component.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
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
  padding: string;

  legendKeys: DataKey[];
  legendLabelStyle: ComponentStyle;
  disabledLegendLabelStyle: ComponentStyle;

  private timeSeriesChart: TbTimeSeriesChart;

  constructor(public widgetComponent: WidgetComponent,
              private imagePipe: ImagePipe,
              private sanitizer: DomSanitizer,
              private renderer: Renderer2,
              private cd: ChangeDetectorRef) {
  }

  ngOnInit(): void {
    this.ctx.$scope.barChartWidget = this;
    this.settings = {...barChartWithLabelsDefaultSettings, ...this.ctx.settings};

    this.backgroundStyle$ = backgroundStyle(this.settings.background, this.imagePipe, this.sanitizer);
    this.overlayStyle = overlayStyle(this.settings.background.overlay);
    this.padding = this.settings.background.overlay.enabled ? undefined : this.settings.padding;

    this.showLegend = this.settings.showLegend;

    if (this.showLegend) {
      this.legendKeys = [];
      this.legendClass = `legend-${this.settings.legendPosition}`;
      this.legendLabelStyle = textStyle(this.settings.legendLabelFont);
      this.disabledLegendLabelStyle = textStyle(this.settings.legendLabelFont);
      this.legendLabelStyle.color = this.settings.legendLabelColor;
    }
    const barSettings = barChartWithLabelsTimeSeriesKeySettings(this.settings, this.ctx.decimals);
    for (const datasource of this.ctx.datasources) {
      const dataKeys = datasource.dataKeys;
      for (const dataKey of dataKeys) {
        dataKey.settings = barSettings;
        if (this.showLegend) {
          this.legendKeys.push(dataKey);
        }
      }
    }
  }

  ngAfterViewInit() {
    const settings = barChartWithLabelsTimeSeriesSettings(this.settings);
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

  public onLegendKeyEnter(key: DataKey) {
    this.timeSeriesChart.keyEnter(key);
  }

  public onLegendKeyLeave(key: DataKey) {
    this.timeSeriesChart.keyLeave(key);
  }

  public toggleLegendKey(key: DataKey) {
    this.timeSeriesChart.toggleKey(key);
  }
}
