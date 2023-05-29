///
/// Copyright © 2016-2023 The Thingsboard Authors
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

import { AfterViewInit, ChangeDetectorRef, Component, ElementRef, Input, ViewChild } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { FormBuilder, FormGroup } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { AttributeService } from '@core/http/attribute.service';
import { DeviceService } from '@core/http/device.service';
import { TranslateService } from '@ngx-translate/core';
import { AttributeData, AttributeScope } from '@shared/models/telemetry/telemetry.models';
import { PageComponent } from '@shared/components/page.component';
import { DialogService } from '@app/core/services/dialog.service';
import { WidgetContext } from '@home/models/widget-component.models';
import { TbFlot } from '@home/components/widget/lib/flot-widget';
import { ResizeObserver } from '@juggle/resize-observer';
import { IWidgetSubscription, SubscriptionInfo, WidgetSubscriptionOptions } from '@core/api/widget-api.models';
import { UtilsService } from '@core/services/utils.service';
import { DatasourceType, LegendConfig, LegendData, LegendPosition, widgetType } from '@shared/models/widget.models';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityId } from '@shared/models/id/entity-id';
import { BaseData } from '@shared/models/base-data';


@Component({
  selector: 'tb-gateway-statistics',
  templateUrl: './gateway-statistics.component.html',
  styleUrls: ['./gateway-statistics.component.scss']
})
export class GatewayStatisticsComponent extends PageComponent implements AfterViewInit {

  @ViewChild('statisticChart') statisticChart: ElementRef;

  @Input()
  ctx: WidgetContext;

  private flot: TbFlot;
  private flotCtx;
  public statisticForm: FormGroup;
  public statisticsKeys = [];
  public commands = [];
  public commandObj: any;
  private resize$: ResizeObserver;
  private subscription: IWidgetSubscription;
  private subscriptionOptions: WidgetSubscriptionOptions = {
    callbacks: {
      onDataUpdated: (subscription, detectChanges) => this.ctx.ngZone.run(() => {
        this.onDataUpdated();
      }),
      onDataUpdateError: (subscription, e) => this.ctx.ngZone.run(() => {
        this.onDataUpdateError(subscription, e);
      })
    },
    useDashboardTimewindow: false,
    legendConfig : {
      position: LegendPosition.bottom
    } as LegendConfig
  };
  private subscriptionInfo: SubscriptionInfo [];
  public legendData: LegendData;


  constructor(protected router: Router,
              protected store: Store<AppState>,
              protected fb: FormBuilder,
              protected translate: TranslateService,
              protected attributeService: AttributeService,
              protected deviceService: DeviceService,
              protected dialogService: DialogService,
              private cd: ChangeDetectorRef,
              private utils: UtilsService,
              public dialog: MatDialog) {
    super(store);
    this.statisticForm = this.fb.group({
      statisticKey: [null, []]
    })

    this.statisticForm.get('statisticKey').valueChanges.subscribe(value => {
      this.commandObj = null;
      if (this.commands.length) {
        this.commandObj = this.commands.find(command => command.attributeOnGateway === value);
      }
      if (this.subscriptionInfo) this.createChartsSubscription(this.ctx.defaultSubscription.datasources[0].entity, value);
    })
  }


  ngAfterViewInit() {
    this.init();
    if (this.ctx.defaultSubscription.datasources.length) {

      const gatewayId = this.ctx.defaultSubscription.datasources[0].entity.id;
      this.attributeService.getEntityAttributes(gatewayId, AttributeScope.SHARED_SCOPE, ["general_configuration"]).subscribe((resp: AttributeData[]) => {
        if (resp && resp.length) {
          this.commands = resp[0].value.statistics.commands;
          if (!this.statisticForm.get('statisticKey').value) {
            this.statisticForm.get('statisticKey').setValue(this.commands[0].attributeOnGateway);
            this.createChartsSubscription(this.ctx.defaultSubscription.datasources[0].entity, this.commands[0].attributeOnGateway);
          }
        }
      })
    }

  }

  public onLegendKeyHiddenChange(index: number) {
    this.legendData.keys[index].dataKey.hidden = !this.legendData.keys[index].dataKey.hidden;
    this.subscription.updateDataVisibility(index);
  }

  private createChartsSubscription(gateway: BaseData<EntityId>, attr: string) {
    let subscriptionInfo = [{
      type: DatasourceType.entity,
      entityType: EntityType.DEVICE,
      entityId: gateway.id.id,
      entityName: gateway.name,
      timeseries: []
    }];

    subscriptionInfo[0].timeseries = [{name: attr, label: attr}];
    this.subscriptionInfo = subscriptionInfo;
    this.changeSubscription(subscriptionInfo);

  }

  init = () => {
    this.flotCtx = {
      $scope: this.ctx.$scope,
      $injector: this.ctx.$injector,
      utils: this.ctx.utils,
      isMobile: this.ctx.isMobile,
      isEdit: this.ctx.isEdit,
      subscriptionApi: this.ctx.subscriptionApi,
      detectChanges: this.ctx.detectChanges,
      settings: this.ctx.settings
    };
  }

  updateChart = () => {
    if (this.flot && this.ctx.defaultSubscription.data.length) {
      this.flot.update();
    }
  }

  resize = () => {
    if (this.flot) {
      this.flot.resize();
    }
  }

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

  private onDataUpdateError(subscription: IWidgetSubscription, e: any) {
    const exceptionData = this.utils.parseException(e);
    let errorText = exceptionData.name;
    if (exceptionData.message) {
      errorText += ': ' + exceptionData.message;
    }
  }

  private onDataUpdated() {
    if (this.flot) {
      this.flot.update();
    }
  }


  changeSubscription(subscriptionInfo: SubscriptionInfo[]) {
    if (this.subscription) {
      this.reset();
    }
    if (this.ctx.datasources[0].entity) {
      this.ctx.subscriptionApi.createSubscriptionFromInfo(widgetType.timeseries, subscriptionInfo, this.subscriptionOptions, false, true).subscribe(subscription => {
        this.subscription = subscription;
        this.legendData = this.subscription.legendData;
        this.flotCtx.defaultSubscription = subscription;
        this.flotCtx.$container = $(this.statisticChart.nativeElement);
        this.resize$ = new ResizeObserver(() => {
          this.resize();
        });
        this.resize$.observe(this.statisticChart.nativeElement);
        this.ctx.detectChanges();
        this.flot = new TbFlot(this.flotCtx as WidgetContext, "line");
        this.flot.update();
      })

    }
  }

}
