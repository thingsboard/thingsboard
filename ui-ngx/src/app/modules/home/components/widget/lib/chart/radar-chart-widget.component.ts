// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, Input, OnInit, TemplateRef, ViewChild, ViewEncapsulation } from '@angular/core';
import { WidgetContext } from '@home/models/widget-component.models';
import { WidgetComponent } from '@home/components/widget/widget.component';
import { TranslateService } from '@ngx-translate/core';
import {
  LatestChartComponent,
  LatestChartComponentCallbacks
} from '@home/components/widget/lib/chart/latest-chart.component';
import {
  radarChartWidgetDefaultSettings,
  radarChartWidgetRadarChartSettings,
  RadarChartWidgetSettings
} from '@home/components/widget/lib/chart/radar-chart-widget.models';
import { TbRadarChart } from '@home/components/widget/lib/chart/radar-chart';

@Component({
    selector: 'tb-radar-chart-widget',
    templateUrl: './latest-chart-widget.component.html',
    styleUrls: [],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class RadarChartWidgetComponent implements OnInit {

  @ViewChild('latestChart')
  latestChart: LatestChartComponent;

  @Input()
  ctx: WidgetContext;

  @Input()
  widgetTitlePanel: TemplateRef<any>;

  settings: RadarChartWidgetSettings;

  callbacks: LatestChartComponentCallbacks;

  constructor(private widgetComponent: WidgetComponent,
              private translate: TranslateService) {
  }

  ngOnInit(): void {
    this.ctx.$scope.radarChartWidget = this;
    this.settings = {...radarChartWidgetDefaultSettings, ...this.ctx.settings};
    this.callbacks = {
      createChart: (chartShape, renderer) => {
        const settings = radarChartWidgetRadarChartSettings(this.settings);
        return new TbRadarChart(this.ctx, settings, chartShape.nativeElement, renderer, this.translate, true);
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
