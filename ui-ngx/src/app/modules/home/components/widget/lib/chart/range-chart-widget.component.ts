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
import {
  backgroundStyle,
  ComponentStyle,
  getDataKey,
  overlayStyle,
  textStyle,
  ValueFormatProcessor
} from '@shared/models/widget-settings.models';
import { isDefinedAndNotNull } from '@core/utils';
import {
  rangeChartDefaultSettings,
  rangeChartTimeSeriesKeySettings,
  rangeChartTimeSeriesSettings,
  RangeChartWidgetSettings,
  RangeItem,
  toRangeItems
} from './range-chart-widget.models';
import { Observable } from 'rxjs';
import { ImagePipe } from '@shared/pipe/image.pipe';
import { DomSanitizer } from '@angular/platform-browser';
import { TbTimeSeriesChart } from '@home/components/widget/lib/chart/time-series-chart';
import { WidgetComponent } from '@home/components/widget/widget.component';
import { TbUnit } from '@shared/models/unit.models';
import { UnitService } from '@core/services/unit.service';

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
  padding: string;

  legendLabelStyle: ComponentStyle;
  disabledLegendLabelStyle: ComponentStyle;
  visibleRangeItems: RangeItem[];

  private decimals = 0;
  private units: TbUnit = '';

  private rangeItems: RangeItem[];

  private timeSeriesChart: TbTimeSeriesChart;

  constructor(public widgetComponent: WidgetComponent,
              private imagePipe: ImagePipe,
              private sanitizer: DomSanitizer,
              private renderer: Renderer2,
              private cd: ChangeDetectorRef) {
  }

  ngOnInit(): void {
    this.ctx.$scope.rangeChartWidget = this;
    this.settings = {...rangeChartDefaultSettings, ...this.ctx.settings};
    const unitService = this.ctx.$injector.get(UnitService);

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
      dataKey.settings = rangeChartTimeSeriesKeySettings(this.settings);
    }

    const valueFormat = ValueFormatProcessor.fromSettings(this.ctx.$injector, {
      units: this.units,
      decimals: this.decimals,
      ignoreUnitSymbol: true
    });

    this.backgroundStyle$ = backgroundStyle(this.settings.background, this.imagePipe, this.sanitizer);
    this.overlayStyle = overlayStyle(this.settings.background.overlay);
    this.padding = this.settings.background.overlay.enabled ? undefined : this.settings.padding;

    this.rangeItems = toRangeItems(this.settings.rangeColors, valueFormat);
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
    const settings = rangeChartTimeSeriesSettings(this.settings, this.rangeItems, this.decimals, this.units);
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
