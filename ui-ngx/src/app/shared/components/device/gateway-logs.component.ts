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
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { AttributeService } from '@core/http/attribute.service';
import { DeviceService } from '@core/http/device.service';
import { TranslateService } from '@ngx-translate/core';
import { AttributeData, DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { PageComponent } from "@shared/components/page.component";
import { PageLink } from "@shared/models/page/page-link";
import { AttributeDatasource } from "@home/models/datasource/attribute-datasource";
import { Direction, SortOrder } from "@shared/models/page/sort-order";
import { MatSort } from "@angular/material/sort";
import { MatTableDataSource } from "@angular/material/table";
import { GatewayLogLevel } from "@shared/components/device/gateway-configuration.component";
import { DialogService } from '@app/core/services/dialog.service';
import { WidgetContext } from "@home/models/widget-component.models";


export interface gatewayConnector {
  name: string;
  type: string;
  configuration?: string;
  configurationJson: string;
  log_level: string;
  key?: string;
}

export interface LogLink {
  name: string;
  key: string;
  filterFn?: Function;
}

@Component({
  selector: 'tb-gateway-logs',
  templateUrl: './gateway-logs.component.html',
  styleUrls: ['./gateway-logs.component.scss']
})
export class GatewayLogsComponent extends PageComponent implements AfterViewInit {

  pageLink: PageLink;

  attributeDataSource: AttributeDatasource;

  dataSource: MatTableDataSource<any>

  displayedColumns = ['ts', 'status', 'message'];

  @Input()
  ctx: WidgetContext;

  @Input()
  dialogRef: MatDialogRef<any>;

  @ViewChild('searchInput') searchInputField: ElementRef;
  @ViewChild(MatSort) sort: MatSort;

  connectorForm: FormGroup;

  viewsInited = false;

  textSearchMode: boolean;

  activeConnectors: Array<string>;

  inactiveConnectors: Array<string>;

  InitialActiveConnectors: Array<string>;

  gatewayLogLevel = Object.values(GatewayLogLevel);

  logLinks: Array<LogLink>;

  initialConnector: gatewayConnector;

  activeLink: LogLink;

  gatewayLogLinks: Array<LogLink> = [
    {
      name: "General",
      key: "LOGS"
    }, {
      name: "Service",
      key: "SERVICE_LOGS"
    },
    {
      name: "Connection",
      key: "CONNECTION_LOGS"
    }, {
      name: "Storage",
      key: "STORAGE_LOGS"
    },
    {
      key: 'EXTENSIONS_LOGS',
      name: "Extension"
    }]


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
    const sortOrder: SortOrder = {property: 'key', direction: Direction.ASC};
    this.pageLink = new PageLink(1000, 0, null, sortOrder);
    this.dataSource = new MatTableDataSource<AttributeData>([]);

  }


  ngAfterViewInit() {
    this.dataSource.sort = this.sort;
    this.ctx.defaultSubscription.onTimewindowChangeFunction = timewindow => {
      this.ctx.defaultSubscription.options.timeWindowConfig = timewindow;
      this.ctx.defaultSubscription.updateDataSubscriptions();
      return timewindow;
    }
    if (this.ctx.settings.isConnectorLog && this.ctx.settings.connectorLogState) {
      console.log(this.ctx.settings.connectorLogState)

      const connector = this.ctx.stateController.getStateParams()[this.ctx.settings.connectorLogState];
      console.log(connector)
      this.logLinks = [{
        key: `${connector.key}_LOGS`,
        name: "Connector",
        filterFn: (attrData)=>{
          return !attrData.message.includes(`_converter.py`)
        }
      },{
        key: `${connector.key}_LOGS`,
        name: "Converter",
        filterFn: (attrData)=>{
          return attrData.message.includes(`_converter.py`)
        }
      }]
    } else {
      this.logLinks = this.gatewayLogLinks;
    }
    this.activeLink = this.logLinks[0];
    this.changeSubscription();
  }


  updateData(sort?) {
    if (this.ctx.defaultSubscription.data.length) {
      console.log(this.ctx.defaultSubscription.data[0].dataKey.name === "LOGS")
      let attrData = this.ctx.defaultSubscription.data[0].data.map(data => {
        let result =  {
          ts: data[0],
          key: this.activeLink.key,
          message: /\[(.*)/.exec(data[1])[0],
          status: 'INVALID LOG FORMAT'
        };

        try {
          result.status= data[1].match(/\|(\w+)\|/)[1];
        } catch (e) {
          result.status = 'INVALID LOG FORMAT'
        }

        return result;
      });
      if (this.activeLink.filterFn) {
        attrData = attrData.filter(data => this.activeLink.filterFn(data));
      }
      this.dataSource.data = attrData;
      if (sort) {
        this.dataSource.sortData(this.dataSource.data, this.sort);
      }
    }
  }

  onTabChanged(link) {
    this.activeLink = link;
    this.changeSubscription();
  }

  statusClass(status) {
    switch (status) {
      case GatewayLogLevel.debug:
        return "status status-debug";
      case GatewayLogLevel.warning:
        return "status status-warning";
      case GatewayLogLevel.error:
      case "EXCEPTION":
        return "status status-error";
      case GatewayLogLevel.info:
      default:
        return "status status-info"
        return "status status-info"
    }
  }

  statusClassMsg(status) {
    if (status === "EXCEPTION") {
      return 'msg-status-exception';
    }
  }

  changeSubscription() {
    if (this.ctx.datasources[0].entity) {
      this.ctx.defaultSubscription.options.datasources[0].dataKeys = [{
        name: this.activeLink.key,
        type: DataKeyType.timeseries,
        settings: {}
      }];
      this.ctx.defaultSubscription.unsubscribe();
      this.ctx.defaultSubscription.updateDataSubscriptions();
      this.ctx.defaultSubscription.callbacks.onDataUpdated = () => {
        this.updateData();
      }

    }
  }

}
