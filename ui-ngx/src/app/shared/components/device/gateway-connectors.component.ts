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

import { AfterViewInit, ChangeDetectorRef, Component, ElementRef, Input, NgZone, ViewChild } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { EntityId } from '@shared/models/id/entity-id';
import { MatDialog } from '@angular/material/dialog';
import { AttributeService } from '@core/http/attribute.service';
import { DeviceService } from '@core/http/device.service';
import { TranslateService } from '@ngx-translate/core';
import { forkJoin, merge } from 'rxjs';
import { AttributeData, AttributeScope } from '@shared/models/telemetry/telemetry.models';
import { PageComponent } from '@shared/components/page.component';
import { PageLink } from '@shared/models/page/page-link';
import { AttributeDatasource } from '@home/models/datasource/attribute-datasource';
import { Direction, SortOrder } from '@shared/models/page/sort-order';
import { MatSort } from '@angular/material/sort';
import { tap } from 'rxjs/operators';
import { TelemetryWebsocketService } from '@core/ws/telemetry-websocket.service';
import { MatTableDataSource } from '@angular/material/table';
import { GatewayLogLevel } from '@shared/components/device/gateway-configuration.component';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { DialogService } from '@app/core/services/dialog.service';
import { WidgetContext } from '@home/models/widget-component.models';
import { deepClone } from '@core/utils';


export interface gatewayConnector {
  name: string;
  type: string;
  configuration?: string;
  configurationJson: string;
  log_level: string;
  key?: string;
}


export const GatewayConnectorDefaultTypesTranslates = new Map<string, string>([
  ['mqtt', 'MQTT'],
  ['modbus', 'MODBUS'],
  ['grpc', 'GRPC'],
  ['opcua', 'OPCUA'],
  ['opcua_asyncio', 'OPCUA ASYNCIO'],
  ['ble', 'BLE'],
  ['request', 'REQUEST'],
  ['can', 'CAN'],
  ['bacnet', 'BACNET'],
  ['odbc', 'ODBC'],
  ['rest', 'REST'],
  ['snmp', 'SNMP'],
  ['ftp', 'FTP'],
  ['socket', 'SOCKET'],
  ['xmpp', 'XMPP'],
  ['ocpp', 'OCCP'],
  ['custom', 'CUSTOM']
]);

@Component({
  selector: 'tb-gateway-connector',
  templateUrl: './gateway-connectors.component.html',
  styleUrls: ['./gateway-connectors.component.scss']
})
export class GatewayConnectorComponent extends PageComponent implements AfterViewInit {

  pageLink: PageLink;

  attributeDataSource: AttributeDatasource;

  inactiveConnectorsDataSource: AttributeDatasource;

  dataSource: MatTableDataSource<AttributeData>

  displayedColumns = ['enabled', 'key', 'type', 'actions'];

  gatewayConnectorDefaultTypes = GatewayConnectorDefaultTypesTranslates;

  @Input()
  ctx: WidgetContext;

  @Input()
  device: EntityId;

  @ViewChild('searchInput') searchInputField: ElementRef;
  @ViewChild(MatSort) sort: MatSort;

  connectorForm: FormGroup;

  viewsInited = false;

  textSearchMode: boolean;

  activeConnectors: Array<string>;

  inactiveConnectors: Array<string>;

  InitialActiveConnectors: Array<string>;

  gatewayLogLevel = Object.values(GatewayLogLevel);

  activeData: Array<any> = [];

  inactiveData: Array<any> = [];

  constructor(protected router: Router,
              protected store: Store<AppState>,
              protected fb: FormBuilder,
              protected translate: TranslateService,
              protected attributeService: AttributeService,
              protected deviceService: DeviceService,
              protected dialogService: DialogService,
              private telemetryWsService: TelemetryWebsocketService,
              private zone: NgZone,
              private cd: ChangeDetectorRef,
              public dialog: MatDialog) {
    super(store);
    const sortOrder: SortOrder = {property: 'key', direction: Direction.ASC};
    this.pageLink = new PageLink(1000, 0, null, sortOrder);
    this.attributeDataSource = new AttributeDatasource(this.attributeService, this.telemetryWsService, this.zone, this.translate);
    this.inactiveConnectorsDataSource = new AttributeDatasource(this.attributeService, this.telemetryWsService, this.zone, this.translate);
    this.dataSource = new MatTableDataSource<AttributeData>([]);
    this.connectorForm = this.fb.group({
      name: ['', [Validators.required]],
      type: ['', [Validators.required]],
      log_level: ['', [Validators.required]],
      key: ['auto'],
      class: [''],
      configuration: [''],
      configurationJson: [{}, [Validators.required]]
    })
    this.connectorForm.disable();
  }


  initialConnector: gatewayConnector;

  ngAfterViewInit() {
    this.connectorForm.valueChanges.subscribe(() => {
      this.cd.detectChanges();
    });
    merge(this.sort.sortChange)
      .pipe(
        tap(() => this.updateData())
      )
      .subscribe();

    this.viewsInited = true;
    if (this.device) {
      forkJoin(this.attributeService.getEntityAttributes(this.device, AttributeScope.SHARED_SCOPE, ['active_connectors']),
        this.attributeService.getEntityAttributes(this.device, AttributeScope.SERVER_SCOPE, ['inactive_connectors'])).subscribe(attributes => {
        if (attributes.length) {
          this.activeConnectors = attributes[0].length ? attributes[0][0].value : [];
          this.inactiveConnectors = attributes[1].length ? attributes[1][0].value : [];
          this.updateData(true);
        } else {
          this.activeConnectors = [];
          this.inactiveConnectors = [];
          this.updateData(true);
        }
      })
    }
  }

  saveConnector(): void {
    const value = this.connectorForm.value;
    value.configuration = this.camelize(value.name) + ".json";
    if (value.type !== 'grpc') {
      delete value.key;
    }
    const attributesToSave = [{
      key: value.name,
      value: value
    }];
    const attributesToDelete = [];
    const scope = (this.activeConnectors.includes(value.name) || !this.initialConnector) ? AttributeScope.SHARED_SCOPE : AttributeScope.SERVER_SCOPE;
    let updateActiveConnectors = false;
    if (this.initialConnector && this.initialConnector.name !== value.name) {
      attributesToDelete.push({key: this.initialConnector.name});
      updateActiveConnectors = true;
      const activeIndex = this.activeConnectors.indexOf(this.initialConnector.name);
      const inactiveIndex = this.inactiveConnectors.indexOf(this.initialConnector.name);
      if (activeIndex !== -1) this.activeConnectors.splice(activeIndex, 1);
      if (inactiveIndex !== -1) this.inactiveConnectors.splice(activeIndex, 1);
    }
    if (!this.activeConnectors.includes(value.name) && scope == AttributeScope.SHARED_SCOPE) {
      this.activeConnectors.push(value.name);
      updateActiveConnectors = true;
    }
    if (!this.inactiveConnectors.includes(value.name) && scope == AttributeScope.SERVER_SCOPE) {
      this.inactiveConnectors.push(value.name);
      updateActiveConnectors = true;
    }
    const tasks = [this.attributeService.saveEntityAttributes(this.device, AttributeScope.SHARED_SCOPE, attributesToSave)];
    if (updateActiveConnectors) {
      tasks.push(this.attributeService.saveEntityAttributes(this.device, scope, [{
        key: scope == AttributeScope.SHARED_SCOPE ? 'active_connectors' : 'inactive_connectors',
        value: scope == AttributeScope.SHARED_SCOPE ? this.activeConnectors : this.inactiveConnectors
      }]));
    }

    if (attributesToDelete.length) {
      tasks.push(this.attributeService.deleteEntityAttributes(this.device, AttributeScope.SHARED_SCOPE, attributesToDelete));
    }
    forkJoin(tasks).subscribe(resp => {
      this.showToast("Update Successful")
      this.updateData(true);
    })
  }

  resetSortAndFilter(update: boolean = true) {
    this.textSearchMode = false;
    this.pageLink.textSearch = null;
    if (this.viewsInited) {
      const sortable = this.sort.sortables.get('key');
      this.sort.active = sortable.id;
      this.sort.direction = 'asc';
      if (update) {
        this.updateData(true);
      }
    }
  }

  updateData(reload: boolean = false) {
    this.pageLink.sortOrder.property = this.sort.active;
    this.pageLink.sortOrder.direction = Direction[this.sort.direction.toUpperCase()];
    this.attributeDataSource.loadAttributes(this.device, AttributeScope.CLIENT_SCOPE, this.pageLink, reload).subscribe(data => {
      this.activeData = data.data.filter(value => this.activeConnectors.includes(value.key));
      this.combineData()
    });
    this.inactiveConnectorsDataSource.loadAttributes(this.device, AttributeScope.SHARED_SCOPE, this.pageLink, reload).subscribe(data => {
      this.inactiveData = data.data.filter(value =>this.inactiveConnectors.includes(value.key));
      this.combineData()
    });
  }

  combineData () {
    this.dataSource.data = [...this.activeData, ...this.inactiveData];
  }

  addAttribute(): void {
    if (this.connectorForm.disabled) {
      this.connectorForm.enable();
    }
    this.clearOutConnectorForm();
  }

  clearOutConnectorForm(): void {
    this.connectorForm.setValue({
      name: '',
      type: 'mqtt',
      log_level: GatewayLogLevel.info,
      key: 'auto',
      class: '',
      configuration: '',
      configurationJson: {}
    })
    this.initialConnector = null;
    this.connectorForm.markAsPristine();
  }

  selectConnector(attribute): void {
    if (this.connectorForm.disabled) {
      this.connectorForm.enable();
    }
    const connector = typeof attribute.value === 'string' ? JSON.parse(attribute.value): attribute.value;
    if (!connector.configuration) {
      connector.configuration = "";
    }
    if (!connector.key) {
      connector.key = 'auto';
    }
    this.initialConnector = connector;
    this.connectorForm.setValue(connector);
    this.connectorForm.markAsPristine();
  }

  showToast(message: string) {
    this.store.dispatch(new ActionNotificationShow(
      {
        message,
        type: 'success',
        duration: 1000,
        verticalPosition: 'top',
        horizontalPosition: 'right',
        target: 'dashboardRoot',
        // panelClass: this.widgetNamespace,
        forceDismiss: true
      }));
  }

  deleteConnector(attribute: AttributeData, $event: Event): void {
    if ($event) {
      $event.stopPropagation();
    }
    const title = `Delete connector ${attribute.key}?`;
    const content = `All connector data will be deleted.`;
    this.dialogService.confirm(title, content, 'Cancel', 'Delete').subscribe(result => {
      if (result) {
        const tasks = [];
        const scope = (this.activeConnectors.includes(attribute.key) || !this.initialConnector) ? AttributeScope.SHARED_SCOPE : AttributeScope.SERVER_SCOPE;
        tasks.push(this.attributeService.deleteEntityAttributes(this.device, AttributeScope.SHARED_SCOPE, [attribute]));
        const activeIndex = this.activeConnectors.indexOf(attribute.key);
        const inactiveIndex = this.inactiveConnectors.indexOf(attribute.key);
        if (activeIndex !== -1) this.activeConnectors.splice(activeIndex, 1);
        if (inactiveIndex !== -1) this.inactiveConnectors.splice(activeIndex, 1);
        tasks.push(this.attributeService.saveEntityAttributes(this.device, scope, [{
          key: scope == AttributeScope.SHARED_SCOPE ? 'active_connectors' : 'inactive_connectors',
          value: scope == AttributeScope.SHARED_SCOPE ? this.activeConnectors : this.inactiveConnectors
        }]));
        forkJoin(tasks).subscribe(resp => {
          if (this.initialConnector ? this.initialConnector.name === attribute.key : true) {
            this.clearOutConnectorForm();
            this.cd.detectChanges();
            this.connectorForm.disable();
          }
          this.updateData()
        })
      }
    })
  }

  camelize(str): string {
    return str.toLowerCase().replace(/\s+/g, '_');
  }

  connectorLogs(attribute: AttributeData, $event: Event): void {
    if ($event) {
      $event.stopPropagation();
    }
    const params = deepClone(this.ctx.stateController.getStateParams());
    params.connector_logs = attribute;
    params.targetEntityParamName = "connector_logs";
    this.ctx.stateController.openState("connector_logs", params);
  }

  connectorRpc(attribute: AttributeData, $event: Event): void {
    if ($event) {
      $event.stopPropagation();
    }
    const params = deepClone(this.ctx.stateController.getStateParams());
    params.connector_logs = attribute;
    params.targetEntityParamName = "connector_rpc";
    this.ctx.stateController.openState("connector_rpc", params);
  }


  enableConnector(attribute): void {
    const wasEnabled = this.activeConnectors.includes(attribute.key);
    if (wasEnabled) {
      let index = this.activeConnectors.indexOf(attribute.key);
      this.activeConnectors.splice(index, 1);
      this.inactiveConnectors.push(attribute.key);
    } else {
      let index = this.inactiveConnectors.indexOf(attribute.key);
      this.inactiveConnectors.splice(index, 1);
      this.activeConnectors.push(attribute.key);
    }
    forkJoin([this.attributeService.saveEntityAttributes(this.device, AttributeScope.SHARED_SCOPE, [{
      key: 'active_connectors',
      value: this.activeConnectors
    }]), this.attributeService.saveEntityAttributes(this.device, AttributeScope.SERVER_SCOPE, [{
      key: 'inactive_connectors',
      value: this.inactiveConnectors
    }]),]).subscribe(resp => {
      this.updateData();
    })
  }

}
