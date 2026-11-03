// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, Input, OnInit, TemplateRef, ViewChild, ViewEncapsulation } from '@angular/core';
import { WidgetContext } from '@home/models/widget-component.models';
import { WidgetComponent } from '@home/components/widget/widget.component';
import { TranslateService } from '@ngx-translate/core';
import { TbPieChart } from '@home/components/widget/lib/chart/pie-chart';
import {
  LatestChartComponent,
  LatestChartComponentCallbacks
} from '@home/components/widget/lib/chart/latest-chart.component';
import {
  pieChartWidgetDefaultSettings,
  pieChartWidgetPieChartSettings,
  PieChartWidgetSettings
} from '@home/components/widget/lib/chart/pie-chart-widget.models';

@Component({
    selector: 'tb-pie-chart-widget',
    templateUrl: './latest-chart-widget.component.html',
    styleUrls: [],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class PieChartWidgetComponent implements OnInit {

  @ViewChild('latestChart')
  latestChart: LatestChartComponent;

  @Input()
  ctx: WidgetContext;

  @Input()
  widgetTitlePanel: TemplateRef<any>;

  settings: PieChartWidgetSettings;

  callbacks: LatestChartComponentCallbacks;

  constructor(private widgetComponent: WidgetComponent,
              private translate: TranslateService) {
  }

  ngOnInit(): void {
    this.ctx.$scope.pieChartWidget = this;
    this.settings = {...pieChartWidgetDefaultSettings, ...this.ctx.settings};
    this.callbacks = {
      createChart: (chartShape, renderer) => {
        const settings = pieChartWidgetPieChartSettings(this.settings);
        return new TbPieChart(this.ctx, settings, chartShape.nativeElement, renderer, this.translate, true);
      },
      onItemClick: ($event: Event, item) => {
        const descriptors = this.ctx.actionsApi.getActionDescriptors('sliceClick');
        if ($event && descriptors.length) {
          $event.stopPropagation();
          const datasource = item.datasource;
          const entityId = datasource ? datasource.entity?.id : null;
          const entityName = datasource ? datasource.entityName : null;
          const entityLabel = datasource ? datasource.entityLabel : null;
          this.ctx.actionsApi.handleWidgetAction($event, descriptors[0], entityId, entityName, item, entityLabel);
        }
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
