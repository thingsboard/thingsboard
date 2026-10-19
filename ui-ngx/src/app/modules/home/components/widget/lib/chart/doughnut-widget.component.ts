// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, Input, OnInit, TemplateRef, ViewChild, ViewEncapsulation } from '@angular/core';
import {
  doughnutDefaultSettings,
  doughnutPieChartSettings,
  DoughnutWidgetSettings
} from '@home/components/widget/lib/chart/doughnut-widget.models';
import { WidgetContext } from '@home/models/widget-component.models';
import { WidgetComponent } from '@home/components/widget/widget.component';
import { TranslateService } from '@ngx-translate/core';
import { isDefinedAndNotNull } from '@core/utils';
import { TbPieChart } from '@home/components/widget/lib/chart/pie-chart';
import {
  LatestChartComponent,
  LatestChartComponentCallbacks
} from '@home/components/widget/lib/chart/latest-chart.component';

@Component({
    selector: 'tb-doughnut-widget',
    templateUrl: './latest-chart-widget.component.html',
    styleUrls: [],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class DoughnutWidgetComponent implements OnInit {

  @ViewChild('latestChart')
  latestChart: LatestChartComponent;

  @Input()
  ctx: WidgetContext;

  @Input()
  widgetTitlePanel: TemplateRef<any>;

  settings: DoughnutWidgetSettings;

  callbacks: LatestChartComponentCallbacks;

  constructor(private widgetComponent: WidgetComponent,
              private translate: TranslateService) {
  }

  ngOnInit(): void {
    const params = this.widgetComponent.typeParameters as any;
    const horizontal  = isDefinedAndNotNull(params.horizontal) ? params.horizontal : false;
    this.ctx.$scope.doughnutWidget = this;
    this.settings = {...doughnutDefaultSettings(horizontal), ...this.ctx.settings};
    this.callbacks = {
      createChart: (chartShape, renderer) => {
        const settings = doughnutPieChartSettings(this.settings);
        return new TbPieChart(this.ctx, settings, chartShape.nativeElement, renderer, this.translate, true);
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
