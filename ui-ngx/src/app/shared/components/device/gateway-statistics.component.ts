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
import { AttributeScope, DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { PageComponent } from '@shared/components/page.component';
import { DialogService } from '@app/core/services/dialog.service';
import { WidgetContext } from '@home/models/widget-component.models';
import { TbFlot } from '@home/components/widget/lib/flot-widget';
import { ResizeObserver } from "@juggle/resize-observer";


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

  public statisticForm: FormGroup;

  public statisticsKeys = [];
  public commands = [];
  public commandObj: any;
  private resize$: ResizeObserver;


  constructor(protected router: Router,
              protected store: Store<AppState>,
              protected fb: FormBuilder,
              protected translate: TranslateService,
              protected attributeService: AttributeService,
              protected deviceService: DeviceService,
              protected dialogService: DialogService,
              private cd: ChangeDetectorRef,
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
      this.changeSubscription(true);
    })
  }


  ngAfterViewInit() {
    if (this.ctx.defaultSubscription.datasources.length) {
      const gatewayId = this.ctx.defaultSubscription.datasources[0].entity.id;
      this.attributeService.getEntityAttributes(gatewayId, AttributeScope.SHARED_SCOPE, ["general_configuration"]).subscribe(resp => {
        if (resp && resp.length) {
          this.commands = resp[0].value.statistics.commands;
          if (!this.statisticForm.get('statisticKey').value) {
            this.statisticForm.get('statisticKey').setValue(this.commands[0].attributeOnGateway);
            this.changeSubscription(true);
          }
        }
      })
    }
    this.ctx.defaultSubscription.onTimewindowChangeFunction = timeWindow => {
      this.ctx.defaultSubscription.options.timeWindowConfig = timeWindow;
      this.ctx.defaultSubscription.updateTimewindowConfig(timeWindow);
      // this.ctx.defaultSubscription.update();
      this.updateChart();
      return timeWindow;
    }
    this.changeSubscription(true);
    this.resize$ = new ResizeObserver(() => {
      this.resize();
    });
    this.resize$.observe(this.statisticChart.nativeElement)
  }

  initChart = () => {
    if (this.ctx.defaultSubscription.data.length && !this.flot) {
      this.ctx.$container = $(this.statisticChart.nativeElement);
      this.flot = new TbFlot(this.ctx, 'line');
    } else setTimeout(this.initChart, 500);
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


  changeSubscription(init?: boolean) {
    if (this.ctx.datasources[0].entity) {
      if (this.flot && init) {
        this.flot.destroy();
        delete this.flot;
      }
      this.ctx.defaultSubscription.options.datasources[0].dataKeys = [{
        name: this.statisticForm.get('statisticKey').value,
        type: DataKeyType.timeseries,
        settings: {},
        color: "#2196f3"
      }];
      this.ctx.defaultSubscription.unsubscribe();
      this.ctx.defaultSubscription.updateDataSubscriptions();
      this.ctx.defaultSubscription.options.callbacks.dataLoading = () => {
      };
      this.ctx.defaultSubscription.callbacks.onDataUpdated = () => {
        if (init) {
          this.initChart();
        } else {
          this.updateChart();
        }
      }
      this.cd.detectChanges();
    }
  }

}
