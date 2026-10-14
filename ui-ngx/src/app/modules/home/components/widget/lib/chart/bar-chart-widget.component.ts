// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { Component, Input, OnInit, Optional, TemplateRef, ViewChild, ViewEncapsulation } from '@angular/core';
import { WidgetContext } from '@home/models/widget-component.models';
import { WidgetComponent } from '@home/components/widget/widget.component';
import { TranslateService } from '@ngx-translate/core';
import {
  LatestChartComponent,
  LatestChartComponentCallbacks
} from '@home/components/widget/lib/chart/latest-chart.component';
import {
  barChartWidgetBarsChartSettings,
  barChartWidgetDefaultSettings,
  BarChartWidgetSettings
} from '@home/components/widget/lib/chart/bar-chart-widget.models';
import { TbBarsChart } from '@home/components/widget/lib/chart/bars-chart';
import { ChartWidgetComponent } from '@home/components/widget/lib/chart/chart.models';
import { coerceBoolean } from '@shared/decorators/coercion';

@Component({
    selector: 'tb-bar-chart-widget',
    templateUrl: './latest-chart-widget.component.html',
    styleUrls: [],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class BarChartWidgetComponent implements OnInit, ChartWidgetComponent {

  @ViewChild('latestChart')
  latestChart: LatestChartComponent;

  @Input()
  ctx: WidgetContext;

  @Input()
  @coerceBoolean()
  reportMode = false;

  @Input()
  widgetTitlePanel: TemplateRef<any>;

  settings: BarChartWidgetSettings;

  callbacks: LatestChartComponentCallbacks;

  constructor(@Optional() private widgetComponent: WidgetComponent,
              private translate: TranslateService) {
  }

  ngOnInit(): void {
    this.ctx.$scope.barChartWidget = this;
    this.settings = {...barChartWidgetDefaultSettings, ...this.ctx.settings};
    this.callbacks = {
      createChart: (chartShape, renderer) => {
        const settings = barChartWidgetBarsChartSettings(this.settings);
        return new TbBarsChart(this.ctx, settings, chartShape.nativeElement, renderer, this.translate, true);
      }
    };
  }

  public onInit() {
    this.latestChart?.onInit();
  }

  public onDataUpdated() {
    this.latestChart?.onDataUpdated();
  }
}
