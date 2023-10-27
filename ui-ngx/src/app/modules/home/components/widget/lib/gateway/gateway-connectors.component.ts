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
import { FormBuilder, FormGroup, UntypedFormControl, ValidatorFn, Validators } from '@angular/forms';
import { EntityId } from '@shared/models/id/entity-id';
import { AttributeService } from '@core/http/attribute.service';
import { TranslateService } from '@ngx-translate/core';
import { forkJoin, Observable } from 'rxjs';
import { AttributeData, AttributeScope } from '@shared/models/telemetry/telemetry.models';
import { PageComponent } from '@shared/components/page.component';
import { PageLink } from '@shared/models/page/page-link';
import { AttributeDatasource } from '@home/models/datasource/attribute-datasource';
import { Direction, SortOrder } from '@shared/models/page/sort-order';
import { MatSort } from '@angular/material/sort';
import { TelemetryWebsocketService } from '@core/ws/telemetry-websocket.service';
import { MatTableDataSource } from '@angular/material/table';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { DialogService } from '@core/services/dialog.service';
import { WidgetContext } from '@home/models/widget-component.models';
import { camelCase, deepClone, isString } from '@core/utils';
import { NULL_UUID } from '@shared/models/id/has-uuid';
import { IWidgetSubscription, WidgetSubscriptionOptions } from '@core/api/widget-api.models';
import { DatasourceType, widgetType } from '@shared/models/widget.models';
import { UtilsService } from '@core/services/utils.service';
import { EntityType } from '@shared/models/entity-type.models';
import {
  GatewayConnector,
  GatewayConnectorDefaultTypesTranslates,
  GatewayLogLevel
} from './gateway-widget.models';

@Component({
  selector: 'tb-gateway-connector',
  templateUrl: './gateway-connectors.component.html',
  styleUrls: ['./gateway-connectors.component.scss']
})
export class GatewayConnectorComponent extends PageComponent implements AfterViewInit {

  pageLink: PageLink;

  dataSource: MatTableDataSource<AttributeData>;

  displayedColumns = ['enabled', 'key', 'type', 'syncStatus', 'errors', 'actions'];

  gatewayConnectorDefaultTypes = GatewayConnectorDefaultTypesTranslates;

  @Input()
  ctx: WidgetContext;

  @Input()
  device: EntityId;

  @ViewChild('nameInput') nameInput: ElementRef;
  @ViewChild(MatSort, {static: false}) sort: MatSort;

  connectorForm: FormGroup;

  textSearchMode: boolean;

  activeConnectors: Array<string>;

  gatewayLogLevel = Object.values(GatewayLogLevel);

  private inactiveConnectors: Array<string>;

  private attributeDataSource: AttributeDatasource;

  private inactiveConnectorsDataSource: AttributeDatasource;

  private serverDataSource: AttributeDatasource;

  private activeData: Array<any> = [];

  private inactiveData: Array<any> = [];

  private sharedAttributeData: Array<AttributeData> = [];

  private initialConnector: GatewayConnector;

  private subscriptionOptions: WidgetSubscriptionOptions = {
    callbacks: {
      onDataUpdated: () => this.ctx.ngZone.run(() => {
        this.onDataUpdated();
      }),
      onDataUpdateError: (subscription, e) => this.ctx.ngZone.run(() => {
        this.onDataUpdateError(e);
      })
    }
  };

  private subscription: IWidgetSubscription;

  constructor(protected store: Store<AppState>,
              private fb: FormBuilder,
              private translate: TranslateService,
              private attributeService: AttributeService,
              private dialogService: DialogService,
              private telemetryWsService: TelemetryWebsocketService,
              private zone: NgZone,
              private utils: UtilsService,
              private cd: ChangeDetectorRef) {
    super(store);
    const sortOrder: SortOrder = {property: 'key', direction: Direction.ASC};
    this.pageLink = new PageLink(1000, 0, null, sortOrder);
    this.attributeDataSource = new AttributeDatasource(this.attributeService, this.telemetryWsService, this.zone, this.translate);
    this.inactiveConnectorsDataSource = new AttributeDatasource(this.attributeService, this.telemetryWsService, this.zone, this.translate);
    this.serverDataSource = new AttributeDatasource(this.attributeService, this.telemetryWsService, this.zone, this.translate);
    this.dataSource = new MatTableDataSource<AttributeData>([]);
    this.connectorForm = this.fb.group({
      name: ['', [Validators.required, this.uniqNameRequired()]],
      type: ['', [Validators.required]],
      logLevel: ['', [Validators.required]],
      key: ['auto'],
      class: [''],
      configuration: [''],
      configurationJson: [{}, [Validators.required]]
    });
    this.connectorForm.disable();
  }

  ngAfterViewInit() {
    this.connectorForm.valueChanges.subscribe(() => {
      this.cd.detectChanges();
    });

    this.dataSource.sort = this.sort;
    this.dataSource.sortingDataAccessor = (data: AttributeData, sortHeaderId: string) => {
      if (sortHeaderId === 'syncStatus') {
        return this.isConnectorSynced(data) ? 1 : 0;
      } else if (sortHeaderId === 'enabled') {
        return this.activeConnectors.includes(data.key) ? 1 : 0;
      }
      return data[sortHeaderId] || data.value[sortHeaderId];
    };

    if (this.device) {
      if (this.device.id === NULL_UUID) {
        return;
      }
      forkJoin([
        this.attributeService.getEntityAttributes(this.device, AttributeScope.SHARED_SCOPE, ['active_connectors']),
        this.attributeService.getEntityAttributes(this.device, AttributeScope.SERVER_SCOPE, ['inactive_connectors'])
      ]).subscribe(attributes => {
        if (attributes.length) {
          this.activeConnectors = attributes[0].length ? attributes[0][0].value : [];
          this.activeConnectors = isString(this.activeConnectors) ? JSON.parse(this.activeConnectors as any) : this.activeConnectors;
          this.inactiveConnectors = attributes[1].length ? attributes[1][0].value : [];
          this.inactiveConnectors = isString(this.inactiveConnectors)
                                      ? JSON.parse(this.inactiveConnectors as any)
                                      : this.inactiveConnectors;
          this.updateData(true);
        } else {
          this.activeConnectors = [];
          this.inactiveConnectors = [];
          this.updateData(true);
        }
      });
    }
  }

  private uniqNameRequired(): ValidatorFn {
    return (c: UntypedFormControl) => {
      const newName = c.value.trim().toLowerCase();
      const found = this.dataSource.data.find((connectorAttr) => {
        const connectorData = connectorAttr.value;
        return connectorData.name.toLowerCase() === newName;
      });
      if (found) {
        if (this.initialConnector && this.initialConnector.name.toLowerCase() === newName) {
          return null;
        }
        return {
          duplicateName: {
            valid: false
          }
        };
      }
      return null;
    };
  }

  saveConnector(): void {
    const value = this.connectorForm.value;
    value.configuration = camelCase(value.name) + '.json';
    if (value.type !== 'grpc') {
      delete value.key;
    }
    if (value.type !== 'custom') {
      delete value.class;
    }
    value.ts = new Date().getTime();
    const attributesToSave = [{
      key: value.name,
      value
    }];
    const attributesToDelete = [];
    const scope = (this.initialConnector && this.activeConnectors.includes(this.initialConnector.name))
                  ? AttributeScope.SHARED_SCOPE
                  : AttributeScope.SERVER_SCOPE;
    let updateActiveConnectors = false;
    if (this.initialConnector && this.initialConnector.name !== value.name) {
      attributesToDelete.push({key: this.initialConnector.name});
      updateActiveConnectors = true;
      const activeIndex = this.activeConnectors.indexOf(this.initialConnector.name);
      const inactiveIndex = this.inactiveConnectors.indexOf(this.initialConnector.name);
      if (activeIndex !== -1) {
        this.activeConnectors.splice(activeIndex, 1);
      }
      if (inactiveIndex !== -1) {
        this.inactiveConnectors.splice(inactiveIndex, 1);
      }
    }
    if (!this.activeConnectors.includes(value.name) && scope === AttributeScope.SHARED_SCOPE) {
      this.activeConnectors.push(value.name);
      updateActiveConnectors = true;
    }
    if (!this.inactiveConnectors.includes(value.name) && scope === AttributeScope.SERVER_SCOPE) {
      this.inactiveConnectors.push(value.name);
      updateActiveConnectors = true;
    }
    const tasks = [this.attributeService.saveEntityAttributes(this.device, scope, attributesToSave)];
    if (updateActiveConnectors) {
      tasks.push(this.attributeService.saveEntityAttributes(this.device, scope, [{
        key: scope === AttributeScope.SHARED_SCOPE ? 'active_connectors' : 'inactive_connectors',
        value: scope === AttributeScope.SHARED_SCOPE ? this.activeConnectors : this.inactiveConnectors
      }]));
    }

    if (attributesToDelete.length) {
      tasks.push(this.attributeService.deleteEntityAttributes(this.device, scope, attributesToDelete));
    }
    forkJoin(tasks).subscribe(_ => {
      this.initialConnector = value;
      this.showToast('Update Successful');
      this.updateData(true);
    });
  }

  private updateData(reload: boolean = false) {
    this.pageLink.sortOrder.property = this.sort.active;
    this.pageLink.sortOrder.direction = Direction[this.sort.direction.toUpperCase()];
    this.attributeDataSource.loadAttributes(this.device, AttributeScope.CLIENT_SCOPE, this.pageLink, reload).subscribe(data => {
      this.activeData = data.data.filter(value => this.activeConnectors.includes(value.key));
      this.combineData();
      this.generateSubscription();
    });
    this.inactiveConnectorsDataSource.loadAttributes(this.device, AttributeScope.SHARED_SCOPE, this.pageLink, reload).subscribe(data => {
      this.sharedAttributeData = data.data.filter(value => this.activeConnectors.includes(value.key));
      this.combineData();
    });
    this.serverDataSource.loadAttributes(this.device, AttributeScope.SERVER_SCOPE, this.pageLink, reload).subscribe(data => {
      this.inactiveData = data.data.filter(value => this.inactiveConnectors.includes(value.key));
      this.combineData();
    });
  }

  isConnectorSynced(attribute: AttributeData) {
    const connectorData = attribute.value;
    if (!connectorData.ts) {
      return false;
    }
    const clientIndex = this.activeData.findIndex(data => {
      const sharedData = data.value;
      return sharedData.name === connectorData.name;
    });
    if (clientIndex === -1) {
      return false;
    }
    const sharedIndex = this.sharedAttributeData.findIndex(data => {
      const sharedData = data.value;
      return sharedData.name === connectorData.name && sharedData.ts && sharedData.ts <= connectorData.ts;
    });
    return sharedIndex !== -1;
  }

  private combineData() {
    this.dataSource.data = [...this.activeData, ...this.inactiveData, ...this.sharedAttributeData].filter((item, index, self) =>
      index === self.findIndex((t) => t.key === item.key)
    ).map(attribute => {
      attribute.value = typeof attribute.value === 'string' ? JSON.parse(attribute.value) : attribute.value;
      return attribute;
    });
  }

  addAttribute(): void {
    if (this.connectorForm.disabled) {
      this.connectorForm.enable();
    }
    this.nameInput.nativeElement.focus();
    this.clearOutConnectorForm();
  }

  private clearOutConnectorForm(): void {
    this.connectorForm.setValue({
      name: '',
      type: 'mqtt',
      logLevel: GatewayLogLevel.INFO,
      key: 'auto',
      class: '',
      configuration: '',
      configurationJson: {}
    });
    this.initialConnector = null;
    this.connectorForm.markAsPristine();
  }

  selectConnector(attribute: AttributeData): void {
    if (this.connectorForm.disabled) {
      this.connectorForm.enable();
    }
    const connector = attribute.value;
    if (!connector.configuration) {
      connector.configuration = '';
    }
    if (!connector.key) {
      connector.key = 'auto';
    }
    this.initialConnector = connector;
    this.connectorForm.patchValue(connector);
    this.connectorForm.markAsPristine();
  }

  isSameConnector(attribute: AttributeData): boolean {
    if (!this.initialConnector) {
      return false;
    }
    const connector = attribute.value;
    return this.initialConnector.name === connector.name;
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
        forceDismiss: true
      }));
  }

  returnType(attribute: AttributeData): string {
    const value = attribute.value;
    return this.gatewayConnectorDefaultTypes.get(value.type);
  }

  deleteConnector(attribute: AttributeData, $event: Event): void {
    if ($event) {
      $event.stopPropagation();
    }
    const title = `Delete connector ${attribute.key}?`;
    const content = `All connector data will be deleted.`;
    this.dialogService.confirm(title, content, 'Cancel', 'Delete').subscribe(result => {
      if (result) {
        const tasks: Array<Observable<any>> = [];
        const scope = (this.initialConnector && this.activeConnectors.includes(this.initialConnector.name))
                      ? AttributeScope.SHARED_SCOPE
                      : AttributeScope.SERVER_SCOPE;
        tasks.push(this.attributeService.deleteEntityAttributes(this.device, scope, [attribute]));
        const activeIndex = this.activeConnectors.indexOf(attribute.key);
        const inactiveIndex = this.inactiveConnectors.indexOf(attribute.key);
        if (activeIndex !== -1) {
          this.activeConnectors.splice(activeIndex, 1);
        }
        if (inactiveIndex !== -1) {
          this.inactiveConnectors.splice(inactiveIndex, 1);
        }
        tasks.push(this.attributeService.saveEntityAttributes(this.device, scope, [{
          key: scope === AttributeScope.SHARED_SCOPE ? 'active_connectors' : 'inactive_connectors',
          value: scope === AttributeScope.SHARED_SCOPE ? this.activeConnectors : this.inactiveConnectors
        }]));
        forkJoin(tasks).subscribe(() => {
          if (this.initialConnector ? this.initialConnector.name === attribute.key : true) {
            this.clearOutConnectorForm();
            this.cd.detectChanges();
            this.connectorForm.disable();
          }
          this.updateData(true);
        });
      }
    });
  }

  connectorLogs(attribute: AttributeData, $event: Event): void {
    if ($event) {
      $event.stopPropagation();
    }
    const params = deepClone(this.ctx.stateController.getStateParams());
    params.connector_logs = attribute;
    params.targetEntityParamName = 'connector_logs';
    this.ctx.stateController.openState('connector_logs', params);
  }

  connectorRpc(attribute: AttributeData, $event: Event): void {
    if ($event) {
      $event.stopPropagation();
    }
    const params = deepClone(this.ctx.stateController.getStateParams());
    params.connector_rpc = attribute;
    params.targetEntityParamName = 'connector_rpc';
    this.ctx.stateController.openState('connector_rpc', params);
  }


  enableConnector(attribute: AttributeData): void {
    const wasEnabled = this.activeConnectors.includes(attribute.key);
    const scopeOld = wasEnabled ? AttributeScope.SHARED_SCOPE : AttributeScope.SERVER_SCOPE;
    const scopeNew = !wasEnabled ? AttributeScope.SHARED_SCOPE : AttributeScope.SERVER_SCOPE;
    attribute.value.ts = new Date().getTime();
    const tasks = [this.attributeService.saveEntityAttributes(this.device, AttributeScope.SHARED_SCOPE, [{
      key: 'active_connectors',
      value: this.activeConnectors
    }]), this.attributeService.saveEntityAttributes(this.device, AttributeScope.SERVER_SCOPE, [{
      key: 'inactive_connectors',
      value: this.inactiveConnectors
    }]), this.attributeService.deleteEntityAttributes(this.device, scopeOld, [attribute]),
      this.attributeService.saveEntityAttributes(this.device, scopeNew, [attribute])];
    if (wasEnabled) {
      const index = this.activeConnectors.indexOf(attribute.key);
      this.activeConnectors.splice(index, 1);
      this.inactiveConnectors.push(attribute.key);
    } else {
      const index = this.inactiveConnectors.indexOf(attribute.key);
      this.inactiveConnectors.splice(index, 1);
      this.activeConnectors.push(attribute.key);
    }
    forkJoin(tasks).subscribe(_ => {
      this.updateData(true);
    });
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
    this.cd.detectChanges();
  }

  private generateSubscription() {
    if (this.subscription) {
      this.subscription.unsubscribe();
    }
    if (this.device) {
      const subscriptionInfo = [{
        type: DatasourceType.entity,
        entityType: EntityType.DEVICE,
        entityId: this.device.id,
        entityName: 'Gateway',
        timeseries: []
      }];
      this.dataSource.data.forEach(value => {
        subscriptionInfo[0].timeseries.push({name: `${value.key}_ERRORS_COUNT`, label: `${value.key}_ERRORS_COUNT`});
      });
      this.ctx.subscriptionApi.createSubscriptionFromInfo(widgetType.latest, subscriptionInfo, this.subscriptionOptions,
        false, true).subscribe(subscription => {
        this.subscription = subscription;
      });
    }
  }

  getErrorsCount(attribute: AttributeData): string {
    const connectorName = attribute.key;
    const connector = this.subscription && this.subscription.data
      .find(data => data && data.dataKey.name === `${connectorName}_ERRORS_COUNT`);
    return (connector && this.activeConnectors.includes(connectorName)) ? (connector.data[0][1] || 0) : 'Inactive';
  }
}
