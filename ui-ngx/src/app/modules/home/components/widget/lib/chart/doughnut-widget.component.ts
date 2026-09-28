// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { Component, Input, OnInit, Optional, TemplateRef, ViewChild, ViewEncapsulation } from '@angular/core';
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
import { coerceBoolean } from '@shared/decorators/coercion';
import { ChartWidgetComponent } from '@home/components/widget/lib/chart/chart.models';

@Component({
    selector: 'tb-doughnut-widget',
    templateUrl: './latest-chart-widget.component.html',
    styleUrls: [],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class DoughnutWidgetComponent implements OnInit, ChartWidgetComponent {

  @ViewChild('latestChart')
  latestChart: LatestChartComponent;

  @Input()
  ctx: WidgetContext;

  @Input()
  @coerceBoolean()
  reportMode = false;

  @Input()
  widgetTitlePanel: TemplateRef<any>;

  settings: DoughnutWidgetSettings;

  callbacks: LatestChartComponentCallbacks;

  constructor(@Optional() private widgetComponent: WidgetComponent,
              private translate: TranslateService) {
  }

  ngOnInit(): void {
    const params = this.widgetComponent?.typeParameters as any;
    const horizontal  = isDefinedAndNotNull(params?.horizontal) ? params?.horizontal : false;
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
