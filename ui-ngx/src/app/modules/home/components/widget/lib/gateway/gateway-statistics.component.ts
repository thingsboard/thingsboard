///
/// Copyright © 2016-2024 The Thingsboard Authors
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

import { AfterViewInit, Component, ElementRef, Input, ViewChild } from '@angular/core';
import { FormBuilder, FormGroup } from '@angular/forms';
import { AttributeService } from '@core/http/attribute.service';
import { AttributeData, AttributeScope } from '@shared/models/telemetry/telemetry.models';
import { WidgetContext } from '@home/models/widget-component.models';
import { TbFlot } from '@home/components/widget/lib/flot-widget';
import { IWidgetSubscription, SubscriptionInfo, WidgetSubscriptionOptions } from '@core/api/widget-api.models';
import { UtilsService } from '@core/services/utils.service';
import { DatasourceType, LegendConfig, LegendData, LegendPosition, widgetType } from '@shared/models/widget.models';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityId } from '@shared/models/id/entity-id';
import { BaseData } from '@shared/models/base-data';
import { PageLink } from '@shared/models/page/page-link';
import { Direction, SortOrder } from '@shared/models/page/sort-order';
import { MatTableDataSource } from '@angular/material/table';
import { MatSort } from '@angular/material/sort';
import { NULL_UUID } from '@shared/models/id/has-uuid';
import { deepClone } from '@core/utils';

@Component({
  selector: 'tb-gateway-statistics',
  templateUrl: './gateway-statistics.component.html',
  styleUrls: ['./gateway-statistics.component.scss']
})
export class GatewayStatisticsComponent implements AfterViewInit {

  @ViewChild(MatSort) sort: MatSort;
  @ViewChild('statisticChart') statisticChart: ElementRef;

  @Input()
  ctx: WidgetContext;

  @Input()
  public general: boolean;

  public isNumericData = false;
  public dataTypeDefined: boolean = false;
  public chartInited: boolean;
  private flot: TbFlot;
  private flotCtx: WidgetContext;
  public statisticForm: FormGroup;
  public statisticsKeys = [];
  public commands = [];
  public commandObj: any;
  public dataSource: MatTableDataSource<any>;
  public pageLink: PageLink;
  private resize$: ResizeObserver;
  private subscription: IWidgetSubscription;
  private subscriptionInfo: SubscriptionInfo [];
  public legendData: LegendData;
  public displayedColumns: Array<string>;
  private subscriptionOptions: WidgetSubscriptionOptions = {
    callbacks: {
      onDataUpdated: () => this.ctx.ngZone.run(() => {
        this.onDataUpdated();
      }),
      onDataUpdateError: (subscription, e) => this.ctx.ngZone.run(() => {
        this.onDataUpdateError(e);
      })
    },
    useDashboardTimewindow: false,
    legendConfig: {
      position: LegendPosition.bottom
    } as LegendConfig
  };


  constructor(private fb: FormBuilder,
              private attributeService: AttributeService,
              private utils: UtilsService) {
    const sortOrder: SortOrder = {property: '0', direction: Direction.DESC};
    this.pageLink = new PageLink(Number.POSITIVE_INFINITY, 0, null, sortOrder);
    this.displayedColumns = ['0', '1'];
    this.dataSource = new MatTableDataSource<any>([]);
    this.statisticForm = this.fb.group({
      statisticKey: [null, []]
    });

    this.statisticForm.get('statisticKey').valueChanges.subscribe(value => {
      this.commandObj = null;
      if (this.commands.length) {
        this.commandObj = this.commands.find(command => command.attributeOnGateway === value);
      }
      if (this.subscriptionInfo) {
        this.createChartsSubscription(this.ctx.defaultSubscription.datasources[0].entity, value);
      }
    });
  }


  ngAfterViewInit() {
    this.dataSource.sort = this.sort;
    this.sort.sortChange.subscribe(() => this.sortData());
    this.init();
    if (this.ctx.defaultSubscription.datasources.length) {
      const gateway = this.ctx.defaultSubscription.datasources[0].entity;
      if (gateway.id.id === NULL_UUID) {
        return;
      }
      if (!this.general) {
        this.attributeService.getEntityAttributes(gateway.id, AttributeScope.SHARED_SCOPE, ['general_configuration'])
          .subscribe((resp: AttributeData[]) => {
            if (resp && resp.length) {
              this.commands = resp[0].value.statistics.commands;
              if (!this.statisticForm.get('statisticKey').value && this.commands && this.commands.length) {
                this.statisticForm.get('statisticKey').setValue(this.commands[0].attributeOnGateway);
                this.createChartsSubscription(gateway, this.commands[0].attributeOnGateway);
              }
            }
          });
      } else {
        this.attributeService.getEntityTimeseriesLatest(gateway.id).subscribe(
          data => {
            const connectorsTs = Object.keys(data)
              .filter(el => el.includes('ConnectorEventsProduced') || el.includes('ConnectorEventsSent'));
            this.createGeneralChartsSubscription(gateway, connectorsTs);
          });
      }
    }
  }

  public navigateToStatistics() {
    const params = deepClone(this.ctx.stateController.getStateParams());
    this.ctx.stateController.openState('configuration', params);
  }

  public sortData() {
    this.dataSource.sortData(this.dataSource.data, this.sort);
  }

  public onLegendKeyHiddenChange(index: number) {
    this.legendData.keys[index].dataKey.hidden = !this.legendData.keys[index].dataKey.hidden;
    this.subscription.updateDataVisibility(index);
  }

  private createChartsSubscription(gateway: BaseData<EntityId>, attr: string) {
    const subscriptionInfo = [{
      type: DatasourceType.entity,
      entityType: EntityType.DEVICE,
      entityId: gateway.id.id,
      entityName: gateway.name,
      timeseries: []
    }];

    subscriptionInfo[0].timeseries = [{name: attr, label: attr}];
    this.subscriptionInfo = subscriptionInfo;
    this.changeSubscription(subscriptionInfo);
    this.ctx.defaultSubscription.unsubscribe();
  }

  private createGeneralChartsSubscription(gateway: BaseData<EntityId>, attrData: string[]) {
    const subscriptionInfo = [{
      type: DatasourceType.entity,
      entityType: EntityType.DEVICE,
      entityId: gateway.id.id,
      entityName: gateway.name,
      timeseries: []
    }];
    subscriptionInfo[0].timeseries = [];
    if (attrData?.length) {
      attrData.forEach(attr => {
        subscriptionInfo[0].timeseries.push({name: attr, label: attr});
      });
    }
    this.ctx.defaultSubscription.datasources[0].dataKeys.forEach(dataKey => {
      subscriptionInfo[0].timeseries.push({name: dataKey.name, label: dataKey.label});
    });

    this.changeSubscription(subscriptionInfo);
    this.ctx.defaultSubscription.unsubscribe();
  }

  private init = () => {
    this.flotCtx = {
      $scope: this.ctx.$scope,
      $injector: this.ctx.$injector,
      utils: this.ctx.utils,
      isMobile: this.ctx.isMobile,
      isEdit: this.ctx.isEdit,
      subscriptionApi: this.ctx.subscriptionApi,
      detectChanges: this.ctx.detectChanges,
      settings: this.ctx.settings
    } as WidgetContext;
  };

  private updateChart = () => {
    if (this.flot && this.ctx.defaultSubscription.data.length) {
      this.flot.update();
    }
  };

  private resize = () => {
    if (this.flot) {
      this.flot.resize();
    }
  };

  private reset() {
    if (this.resize$) {
      this.resize$.disconnect();
    }
    if (this.subscription) {
      this.subscription.unsubscribe();
    }
    if (this.flot) {
      this.flot.destroy();
    }
  }

  private onDataUpdateError(e: any) {
    const exceptionData = this.utils.parseException(e);
    let errorText = exceptionData.name;
    if (exceptionData.message) {
      errorText += ': ' + exceptionData.message;
    }
    console.error(errorText);
  }

  private onDataUpdated() {
    this.isDataOnlyNumbers();
    if (this.isNumericData) {
      if (this.chartInited) {
        if (this.flot) {
          this.flot.update();
        }
      } else {
        this.initChart();
      }
    }
  }

  private initChart() {
    this.chartInited = true;
    this.flotCtx.$container = $(this.statisticChart.nativeElement);
    this.resize$.observe(this.statisticChart.nativeElement);
    this.flot = new TbFlot(this.flotCtx as WidgetContext, 'line');
    this.flot.update();
  }

  private isDataOnlyNumbers() {
    if (this.general) {
      this.isNumericData = true;
      return;
    }
    this.dataSource.data = this.subscription.data.length ? this.subscription.data[0].data : [];
    if (this.dataSource.data.length && !this.dataTypeDefined) {
      this.dataTypeDefined = true;
      this.isNumericData = this.dataSource.data.every(data => !isNaN(+data[1]));
    }
  }


  private changeSubscription(subscriptionInfo: SubscriptionInfo[]) {
    if (this.subscription) {
      this.reset();
    }
    if (this.ctx.datasources[0].entity) {
      this.ctx.subscriptionApi.createSubscriptionFromInfo(widgetType.timeseries, subscriptionInfo, this.subscriptionOptions,
        false, true).subscribe(subscription => {
        this.dataTypeDefined = false;
        this.subscription = subscription;
        this.isDataOnlyNumbers();
        this.legendData = this.subscription.legendData;
        this.flotCtx.defaultSubscription = subscription;
        this.resize$ = new ResizeObserver(() => {
          this.resize();
        });
        this.ctx.detectChanges();
        if (this.isNumericData) {
          this.initChart();
        }
      });
    }
  }
}
