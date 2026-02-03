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
