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

import { AfterViewInit, Component, ElementRef, Input, OnInit, ViewChild } from '@angular/core';
import { MatDialogRef } from '@angular/material/dialog';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { PageLink } from '@shared/models/page/page-link';
import { Direction, SortOrder } from '@shared/models/page/sort-order';
import { MatSort } from '@angular/material/sort';
import { MatTableDataSource } from '@angular/material/table';
import { WidgetContext } from '@home/models/widget-component.models';
import { MatPaginator } from '@angular/material/paginator';
import { GatewayLogData, GatewayStatus, LogLink } from './gateway-widget.models';

@Component({
  selector: 'tb-gateway-logs',
  templateUrl: './gateway-logs.component.html',
  styleUrls: ['./gateway-logs.component.scss']
})
export class GatewayLogsComponent implements OnInit, AfterViewInit {

  pageLink: PageLink;

  dataSource: MatTableDataSource<GatewayLogData>;

  displayedColumns = ['ts', 'status', 'message'];

  @Input()
  ctx: WidgetContext;

  @Input()
  dialogRef: MatDialogRef<any>;

  @ViewChild('searchInput') searchInputField: ElementRef;
  @ViewChild(MatSort) sort: MatSort;
  @ViewChild(MatPaginator) paginator: MatPaginator;

  textSearchMode: boolean;

  logLinks: Array<LogLink>;

  activeLink: LogLink;

  gatewayLogLinks: Array<LogLink> = [
    {
      name: 'General',
      key: 'LOGS'
    }, {
      name: 'Service',
      key: 'SERVICE_LOGS'
    },
    {
      name: 'Connection',
      key: 'CONNECTION_LOGS'
    }, {
      name: 'Storage',
      key: 'STORAGE_LOGS'
    },
    {
      key: 'EXTENSIONS_LOGS',
      name: 'Extension'
    }];


  constructor() {
    const sortOrder: SortOrder = {property: 'ts', direction: Direction.DESC};
    this.pageLink = new PageLink(10, 0, null, sortOrder);
    this.dataSource = new MatTableDataSource<GatewayLogData>([]);
  }

  ngOnInit(): void {
    this.updateWidgetTitle();
  }

  ngAfterViewInit() {
    this.dataSource.sort = this.sort;
    this.dataSource.paginator = this.paginator;
    this.ctx.defaultSubscription.onTimewindowChangeFunction = timewindow => {
      this.ctx.defaultSubscription.options.timeWindowConfig = timewindow;
      this.ctx.defaultSubscription.updateDataSubscriptions();
      return timewindow;
    };
    if (this.ctx.settings.isConnectorLog && this.ctx.settings.connectorLogState) {
      const connector = this.ctx.stateController.getStateParams()[this.ctx.settings.connectorLogState];
      this.logLinks = [{
        key: `${connector.key}_LOGS`,
        name: 'Connector',
        filterFn: (attrData) => !attrData.message.includes(`_converter.py`)
      }, {
        key: `${connector.key}_LOGS`,
        name: 'Converter',
        filterFn: (attrData) => attrData.message.includes(`_converter.py`)
      }];
    } else {
      this.logLinks = this.gatewayLogLinks;
    }
    this.activeLink = this.logLinks[0];
    this.changeSubscription();
  }

  private updateWidgetTitle(): void {
    if (this.ctx.settings.isConnectorLog && this.ctx.settings.connectorLogState) {
      const widgetTitle = this.ctx.widgetConfig.title;
      const titlePlaceholder = '${connectorName}';
      if (widgetTitle.includes(titlePlaceholder)) {
        const connector = this.ctx.stateController.getStateParams()[this.ctx.settings.connectorLogState];
        this.ctx.widgetTitle = widgetTitle.replace(titlePlaceholder, connector.key);
      }
    }
  }


  private updateData() {
    if (this.ctx.defaultSubscription.data.length && this.ctx.defaultSubscription.data[0]) {
      let attrData = this.ctx.defaultSubscription.data[0].data.map(data => {
        const result = {
          ts: data[0],
          key: this.activeLink.key,
          message: data[1],
          status: 'INVALID LOG FORMAT' as GatewayStatus
        };

        try {
          result.message = /\[(.*)/.exec(data[1])[0];
        } catch (e) {
          result.message = data[1];
        }

        try {
          result.status = data[1].match(/\|(\w+)\|/)[1];
        } catch (e) {
          result.status = 'INVALID LOG FORMAT' as GatewayStatus;
        }

        return result;
      });
      if (this.activeLink.filterFn) {
        attrData = attrData.filter(data => this.activeLink.filterFn(data));
      }
      this.dataSource.data = attrData;
    }
  }

  onTabChanged(link: LogLink) {
    this.activeLink = link;
    this.changeSubscription();
  }

  statusClass(status: GatewayStatus): string {
    switch (status) {
      case GatewayStatus.DEBUG:
        return 'status status-debug';
      case GatewayStatus.WARNING:
        return 'status status-warning';
      case GatewayStatus.ERROR:
      case GatewayStatus.EXCEPTION:
        return 'status status-error';
      default:
        return 'status status-info';
    }
  }

  statusClassMsg(status?: GatewayStatus): string {
    if (status === GatewayStatus.EXCEPTION) {
      return 'msg-status-exception';
    }
  }

  trackByLogTs(_: number, log: GatewayLogData): number {
    return log.ts;
  }

  private changeSubscription() {
    if (this.ctx.datasources && this.ctx.datasources[0].entity && this.ctx.defaultSubscription.options.datasources) {
      this.ctx.defaultSubscription.options.datasources[0].dataKeys = [{
        name: this.activeLink.key,
        type: DataKeyType.timeseries,
        settings: {}
      }];
      this.ctx.defaultSubscription.unsubscribe();
      this.ctx.defaultSubscription.updateDataSubscriptions();
      this.ctx.defaultSubscription.callbacks.onDataUpdated = () => {
        this.updateData();
      };
    }
  }
}
