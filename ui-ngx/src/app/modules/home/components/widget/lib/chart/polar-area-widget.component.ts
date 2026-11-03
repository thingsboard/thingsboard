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
import { TbBarsChart } from '@home/components/widget/lib/chart/bars-chart';
import {
  polarAreaChartWidgetBarsChartSettings,
  polarAreaChartWidgetDefaultSettings,
  PolarAreaChartWidgetSettings
} from '@home/components/widget/lib/chart/polar-area-widget.models';

@Component({
    selector: 'tb-polar-area-chart-widget',
    templateUrl: './latest-chart-widget.component.html',
    styleUrls: [],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class PolarAreaWidgetComponent implements OnInit {

  @ViewChild('latestChart')
  latestChart: LatestChartComponent;

  @Input()
  ctx: WidgetContext;

  @Input()
  widgetTitlePanel: TemplateRef<any>;

  settings: PolarAreaChartWidgetSettings;

  callbacks: LatestChartComponentCallbacks;

  constructor(private widgetComponent: WidgetComponent,
              private translate: TranslateService) {
  }

  ngOnInit(): void {
    this.ctx.$scope.polarAreaChartWidget = this;
    this.settings = {...polarAreaChartWidgetDefaultSettings, ...this.ctx.settings};
    this.callbacks = {
      createChart: (chartShape, renderer) => {
        const settings = polarAreaChartWidgetBarsChartSettings(this.settings);
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
