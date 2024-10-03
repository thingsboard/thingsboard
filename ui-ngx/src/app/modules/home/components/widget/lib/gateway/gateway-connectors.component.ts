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

import {
  AfterViewInit,
  ChangeDetectorRef,
  Component,
  ElementRef,
  Input,
  NgZone,
  OnDestroy,
  ViewChild
} from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormBuilder, FormControl, FormGroup, UntypedFormControl, ValidatorFn, Validators } from '@angular/forms';
import { EntityId } from '@shared/models/id/entity-id';
import { AttributeService } from '@core/http/attribute.service';
import { TranslateService } from '@ngx-translate/core';
import { forkJoin, Observable, of, Subject, Subscription } from 'rxjs';
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
import { camelCase, deepClone, isEqual, isString } from '@core/utils';
import { NULL_UUID } from '@shared/models/id/has-uuid';
import { IWidgetSubscription, WidgetSubscriptionOptions } from '@core/api/widget-api.models';
import { DatasourceType, widgetType } from '@shared/models/widget.models';
import { UtilsService } from '@core/services/utils.service';
import { EntityType } from '@shared/models/entity-type.models';
import {
  AddConnectorConfigData,
  ConnectorBaseConfig,
  ConnectorBaseInfo,
  ConfigurationModes,
  ConnectorType,
  GatewayAttributeData,
  GatewayConnector,
  GatewayConnectorDefaultTypesTranslatesMap,
  GatewayLogLevel,
  noLeadTrailSpacesRegex,
  ReportStrategyDefaultValue,
  ReportStrategyType,
} from './gateway-widget.models';
import { MatDialog } from '@angular/material/dialog';
import { AddConnectorDialogComponent } from '@home/components/widget/lib/gateway/dialog/add-connector-dialog.component';
import { debounceTime, filter, switchMap, take, takeUntil, tap } from 'rxjs/operators';
import { ErrorStateMatcher } from '@angular/material/core';
import { PageData } from '@shared/models/page/page-data';
import {
  GatewayConnectorVersionMappingUtil
} from '@home/components/widget/lib/gateway/utils/gateway-connector-version-mapping.util';
import { LatestVersionConfigPipe } from '@home/components/widget/lib/gateway/pipes/latest-version-config.pipe';

export class ForceErrorStateMatcher implements ErrorStateMatcher {
  isErrorState(control: FormControl | null): boolean {
    return (control && control.invalid);
  }
}

@Component({
  selector: 'tb-gateway-connector',
  templateUrl: './gateway-connectors.component.html',
  providers: [{ provide: ErrorStateMatcher, useClass: ForceErrorStateMatcher }],
  styleUrls: ['./gateway-connectors.component.scss']
})
export class GatewayConnectorComponent extends PageComponent implements AfterViewInit, OnDestroy {

  @Input()
  ctx: WidgetContext;
  @Input()
  device: EntityId;

  @ViewChild('nameInput') nameInput: ElementRef;
  @ViewChild(MatSort, {static: false}) sort: MatSort;

  readonly ConnectorType = ConnectorType;
  readonly allowBasicConfig = new Set<ConnectorType>([
    ConnectorType.MQTT,
    ConnectorType.OPCUA,
    ConnectorType.MODBUS,
  ]);
  readonly gatewayLogLevel = Object.values(GatewayLogLevel);
  readonly displayedColumns = ['enabled', 'key', 'type', 'syncStatus', 'errors', 'actions'];
  readonly GatewayConnectorTypesTranslatesMap = GatewayConnectorDefaultTypesTranslatesMap;
  readonly ConnectorConfigurationModes = ConfigurationModes;
  readonly ReportStrategyDefaultValue = ReportStrategyDefaultValue;

  pageLink: PageLink;
  dataSource: MatTableDataSource<GatewayAttributeData>;
  connectorForm: FormGroup;
  activeConnectors: Array<string>;
  mode: ConfigurationModes = this.ConnectorConfigurationModes.BASIC;
  initialConnector: GatewayConnector;
  basicConfigInitSubject = new Subject<void>();

  private gatewayVersion: string;
  private isGatewayActive: boolean;
  private inactiveConnectors: Array<string>;
  private attributeDataSource: AttributeDatasource;
  private inactiveConnectorsDataSource: AttributeDatasource;
  private serverDataSource: AttributeDatasource;
  private activeData: Array<any> = [];
  private inactiveData: Array<any> = [];
  private sharedAttributeData: Array<GatewayAttributeData> = [];
  private basicConfigSub: Subscription;
  private jsonConfigSub: Subscription;
  private subscriptionOptions: WidgetSubscriptionOptions = {
    callbacks: {
      onDataUpdated: () => this.ctx.ngZone.run(() => {
        this.onErrorsUpdated();
      }),
      onDataUpdateError: (_, e) => this.ctx.ngZone.run(() => {
        this.onDataUpdateError(e);
      })
    }
  };
  private destroy$ = new Subject<void>();
  private subscription: IWidgetSubscription;
  private attributeUpdateSubject = new Subject<GatewayAttributeData>();

  constructor(protected store: Store<AppState>,
              private fb: FormBuilder,
              private translate: TranslateService,
              private attributeService: AttributeService,
              private dialogService: DialogService,
              private dialog: MatDialog,
              private telemetryWsService: TelemetryWebsocketService,
              private zone: NgZone,
              private utils: UtilsService,
              private isLatestVersionConfig: LatestVersionConfigPipe,
              private cd: ChangeDetectorRef) {
    super(store);

    this.initDataSources();
    this.initConnectorForm();
    this.observeAttributeChange();
  }

  ngAfterViewInit(): void {
    this.dataSource.sort = this.sort;
    this.dataSource.sortingDataAccessor = this.getSortingDataAccessor();
    this.ctx.$scope.gatewayConnectors = this;

    this.loadConnectors();
    this.loadGatewayState();
    this.observeModeChange();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    super.ngOnDestroy();
  }

  onSaveConnector(): void {
    this.saveConnector(this.getUpdatedConnectorData(this.connectorForm.value), false);
  }

  private saveConnector(connector: GatewayConnector, isNew = true): void {
    const scope = (isNew || this.activeConnectors.includes(this.initialConnector.name))
      ? AttributeScope.SHARED_SCOPE
      : AttributeScope.SERVER_SCOPE;

    forkJoin(this.getEntityAttributeTasks(connector, scope)).pipe(take(1)).subscribe(_ => {
      this.showToast(isNew
        ? this.translate.instant('gateway.connector-created')
        : this.translate.instant('gateway.connector-updated')
      );
      this.initialConnector = connector;
      this.updateData(true);
      this.connectorForm.markAsPristine();
    });
  }

  private getEntityAttributeTasks(value: GatewayConnector, scope: AttributeScope): Observable<any>[] {
    const tasks = [];
    const attributesToSave = [{ key: value.name, value }];
    const attributesToDelete = [];
    const shouldAddToConnectorsList = !this.activeConnectors.includes(value.name) && scope === AttributeScope.SHARED_SCOPE
      || !this.inactiveConnectors.includes(value.name) && scope === AttributeScope.SERVER_SCOPE;
    const isNewConnector = this.initialConnector && this.initialConnector.name !== value.name;

    if (isNewConnector) {
      attributesToDelete.push({ key: this.initialConnector.name });
      this.removeConnectorFromList(this.initialConnector.name, true);
      this.removeConnectorFromList(this.initialConnector.name, false);
    }

    if (shouldAddToConnectorsList) {
      if (scope === AttributeScope.SHARED_SCOPE) {
        this.activeConnectors.push(value.name);
      } else {
        this.inactiveConnectors.push(value.name);
      }
    }

    if (isNewConnector || shouldAddToConnectorsList) {
      tasks.push(this.getSaveEntityAttributesTask(scope));
    }

    tasks.push(this.attributeService.saveEntityAttributes(this.device, scope, attributesToSave));

    if (attributesToDelete.length) {
      tasks.push(this.attributeService.deleteEntityAttributes(this.device, scope, attributesToDelete));
    }

    return tasks;
  }

  private getSaveEntityAttributesTask(scope: AttributeScope): Observable<any> {
    const key = scope === AttributeScope.SHARED_SCOPE ? 'active_connectors' : 'inactive_connectors';
    const value = scope === AttributeScope.SHARED_SCOPE ? this.activeConnectors : this.inactiveConnectors;

    return this.attributeService.saveEntityAttributes(this.device, scope, [{ key, value }]);
  }

  private removeConnectorFromList(connectorName: string, isActive: boolean): void {
    const list = isActive? this.activeConnectors : this.inactiveConnectors;
    const index = list.indexOf(connectorName);
    if (index !== -1) {
      list.splice(index, 1);
    }
  }

  private getUpdatedConnectorData(connector: GatewayConnector): GatewayConnector {
    const value = {...connector };
    value.configuration = `${camelCase(value.name)}.json`;
    delete value.basicConfig;

    if (value.type !== ConnectorType.GRPC) {
      delete value.key;
    }
    if (value.type !== ConnectorType.CUSTOM) {
      delete value.class;
    }

    if (value.type === ConnectorType.MODBUS && this.isLatestVersionConfig.transform(value.configVersion)) {
      if (!value.reportStrategy) {
        value.reportStrategy = {
          type: ReportStrategyType.OnReportPeriod,
          reportPeriod: ReportStrategyDefaultValue.Connector
        };
        delete value.sendDataOnlyOnChange;
      }
    }

    if (this.gatewayVersion && !value.configVersion) {
      value.configVersion = this.gatewayVersion;
    }

    value.ts = Date.now();

    return value;
  }

  private updateData(reload: boolean = false): void {
    this.pageLink.sortOrder.property = this.sort.active;
    this.pageLink.sortOrder.direction = Direction[this.sort.direction.toUpperCase()];
    this.attributeDataSource.loadAttributes(this.device, AttributeScope.CLIENT_SCOPE, this.pageLink, reload).subscribe(data => {
      this.activeData = data.data.filter(value => this.activeConnectors.includes(value.key));
      this.combineData();
      this.generateSubscription();
      this.setClientData(data);
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

  isConnectorSynced(attribute: GatewayAttributeData): boolean {
    const connectorData = attribute.value;
    if (!connectorData.ts || attribute.skipSync || !this.isGatewayActive) {
      return false;
    }
    const clientIndex = this.activeData.findIndex(data => {
      const sharedData = typeof data.value === 'string' ? JSON.parse(data.value) : data.value;
      return sharedData.name === connectorData.name;
    });
    if (clientIndex === -1) {
      return false;
    }
    const sharedIndex = this.sharedAttributeData.findIndex(data => {
      const sharedData = data.value;
      const hasSameName = sharedData.name === connectorData.name;
      const hasEmptyConfig = isEqual(sharedData.configurationJson, {}) && hasSameName;
      const hasSameConfig = this.hasSameConfig(sharedData.configurationJson, connectorData.configurationJson);
      const isRecentlyCreated = sharedData.ts && sharedData.ts <= connectorData.ts;
      return hasSameName && isRecentlyCreated && (hasSameConfig || hasEmptyConfig);
    });
    return sharedIndex !== -1;
  }

  private hasSameConfig(sharedDataConfigJson: ConnectorBaseInfo, connectorDataConfigJson: ConnectorBaseInfo): boolean {
    const { name, id, enableRemoteLogging, logLevel, reportStrategy, configVersion, ...sharedDataConfig } = sharedDataConfigJson;
    const {
      name: connectorName,
      id: connectorId,
      enableRemoteLogging: connectorEnableRemoteLogging,
      logLevel: connectorLogLevel,
      reportStrategy: connectorReportStrategy,
      configVersion: connectorConfigVersion,
      ...connectorConfig
    } = connectorDataConfigJson;

    return isEqual(sharedDataConfig, connectorConfig);
  }

  private combineData(): void {
    const combinedData = [
      ...this.activeData,
      ...this.inactiveData,
      ...this.sharedAttributeData
    ];

    const latestData = combinedData.reduce((acc, attribute) => {
      const existingItemIndex = acc.findIndex(item => item.key === attribute.key);

      if (existingItemIndex === -1) {
        acc.push(attribute);
      } else if (
        attribute.lastUpdateTs > acc[existingItemIndex].lastUpdateTs &&
        !this.isConnectorSynced(acc[existingItemIndex])
      ) {
        acc[existingItemIndex] = { ...attribute, skipSync: true };
      }

      return acc;
    }, []);

    this.dataSource.data = latestData.map(attribute => ({
      ...attribute,
      value: typeof attribute.value === 'string' ? JSON.parse(attribute.value) : attribute.value
    }));
  }

  private clearOutConnectorForm(): void {
    this.initialConnector = null;
    this.connectorForm.setValue({
      mode: ConfigurationModes.BASIC,
      name: '',
      type: ConnectorType.MQTT,
      sendDataOnlyOnChange: false,
      enableRemoteLogging: false,
      logLevel: GatewayLogLevel.INFO,
      key: 'auto',
      class: '',
      configuration: '',
      configurationJson: {},
      basicConfig: {},
      configVersion: '',
      reportStrategy: [{ value: {}, disabled: true }],
    }, {emitEvent: false});
    this.connectorForm.markAsPristine();
  }

  selectConnector($event: Event, attribute: GatewayAttributeData): void {
    if ($event) {
      $event.stopPropagation();
    }
    const connector = attribute.value;
    if (connector?.name !== this.initialConnector?.name) {
      this.confirmConnectorChange().subscribe((result) => {
        if (result) {
          this.setFormValue(connector);
        }
      });
    }
  }

  isSameConnector(attribute: GatewayAttributeData): boolean {
    if (!this.initialConnector) {
      return false;
    }
    const connector = attribute.value;
    return this.initialConnector.name === connector.name;
  }

  showToast(message: string): void {
    this.store.dispatch(new ActionNotificationShow(
      {
        message,
        type: 'success',
        duration: 1000,
        verticalPosition: 'top',
        horizontalPosition: 'left',
        target: 'dashboardRoot',
        forceDismiss: true
      }));
  }

  returnType(attribute: GatewayAttributeData): string {
    const value = attribute.value;
    return this.GatewayConnectorTypesTranslatesMap.get(value.type);
  }

  deleteConnector(attribute: GatewayAttributeData, $event: Event): void {
    $event?.stopPropagation();

    const title = `Delete connector \"${attribute.key}\"?`;
    const content = `All connector data will be deleted.`;

    this.dialogService.confirm(title, content, 'Cancel', 'Delete').pipe(
      take(1),
      switchMap((result) => {
        if (!result) {
          return;
        }
        const tasks: Array<Observable<any>> = [];
        const scope = this.activeConnectors.includes(attribute.value?.name) ?
          AttributeScope.SHARED_SCOPE :
          AttributeScope.SERVER_SCOPE;
        tasks.push(this.attributeService.deleteEntityAttributes(this.device, scope, [attribute]));
        this.removeConnectorFromList(attribute.key, true);
        this.removeConnectorFromList(attribute.key, false);
        tasks.push(this.getSaveEntityAttributesTask(scope));

        return forkJoin(tasks);
      })
    ).subscribe(() => {
      if (this.initialConnector ? this.initialConnector.name === attribute.key : true) {
        this.clearOutConnectorForm();
        this.cd.detectChanges();
        this.connectorForm.disable();
      }
      this.updateData(true);
    });
  }

  connectorLogs(attribute: GatewayAttributeData, $event: Event): void {
    if ($event) {
      $event.stopPropagation();
    }
    const params = deepClone(this.ctx.stateController.getStateParams());
    params.connector_logs = attribute;
    params.targetEntityParamName = 'connector_logs';
    this.ctx.stateController.openState('connector_logs', params);
  }

  connectorRpc(attribute: GatewayAttributeData, $event: Event): void {
    if ($event) {
      $event.stopPropagation();
    }
    const params = deepClone(this.ctx.stateController.getStateParams());
    params.connector_rpc = attribute;
    params.targetEntityParamName = 'connector_rpc';
    this.ctx.stateController.openState('connector_rpc', params);
  }


  onEnableConnector(attribute: GatewayAttributeData): void {
    attribute.value.ts = new Date().getTime();

    this.updateActiveConnectorKeys(attribute.key);

    this.attributeUpdateSubject.next(attribute);
  }

  getErrorsCount(attribute: GatewayAttributeData): string {
    const connectorName = attribute.key;
    const connector = this.subscription && this.subscription.data
      .find(data => data && data.dataKey.name === `${connectorName}_ERRORS_COUNT`);
    return (connector && this.activeConnectors.includes(connectorName)) ? (connector.data[0][1] || 0) : 'Inactive';
  }

  onAddConnector(event?: Event): void {
    event?.stopPropagation();

    this.confirmConnectorChange()
      .pipe(
        take(1),
        filter(Boolean),
        switchMap(() => this.openAddConnectorDialog()),
        filter(Boolean),
      )
      .subscribe(connector => this.addConnector(connector));
  }

  private addConnector(connector: GatewayConnector): void {
    if (this.connectorForm.disabled) {
      this.connectorForm.enable();
    }
    if (!connector.configurationJson) {
      connector.configurationJson = {} as ConnectorBaseConfig;
    }
    if (this.gatewayVersion && !connector.configVersion) {
      connector.configVersion = this.gatewayVersion;
    }
    connector.basicConfig = connector.configurationJson;
    this.initialConnector = connector;

    const previousType = this.connectorForm.get('type').value;

    this.setInitialConnectorValues(connector);

    this.saveConnector(this.getUpdatedConnectorData(connector));

    if (previousType === connector.type || !this.allowBasicConfig.has(connector.type)) {
      this.patchBasicConfigConnector(connector);
    } else {
      this.basicConfigInitSubject.pipe(take(1)).subscribe(() => {
        this.patchBasicConfigConnector(connector);
      });
    }
  }

  private setInitialConnectorValues(connector: GatewayConnector): void {
    const {basicConfig, mode, ...initialConnector} = connector;
    this.toggleReportStrategy(connector.type);
    this.connectorForm.get('mode').setValue(this.allowBasicConfig.has(connector.type)
      ? connector.mode ?? ConfigurationModes.BASIC
      : null, {emitEvent: false}
    );
    this.connectorForm.patchValue(initialConnector, {emitEvent: false});
  }

  private openAddConnectorDialog(): Observable<GatewayConnector> {
    return this.ctx.ngZone.run(() =>
      this.dialog.open<AddConnectorDialogComponent, AddConnectorConfigData>(AddConnectorDialogComponent, {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: {
          dataSourceData: this.dataSource.data,
          gatewayVersion: this.gatewayVersion,
        }
      }).afterClosed()
    );
  }

  uniqNameRequired(): ValidatorFn {
    return (control: UntypedFormControl) => {
      const newName = control.value?.trim().toLowerCase();
      const isDuplicate = this.dataSource.data.some(connectorAttr => connectorAttr.value.name.toLowerCase() === newName);
      const isSameAsInitial = this.initialConnector?.name.toLowerCase() === newName;

      if (isDuplicate && !isSameAsInitial) {
        return { duplicateName: { valid: false } };
      }

      return null;
    };
  }

  private initDataSources(): void {
    const sortOrder: SortOrder = {property: 'key', direction: Direction.ASC};
    this.pageLink = new PageLink(1000, 0, null, sortOrder);
    this.attributeDataSource = new AttributeDatasource(this.attributeService, this.telemetryWsService, this.zone, this.translate);
    this.inactiveConnectorsDataSource = new AttributeDatasource(this.attributeService, this.telemetryWsService, this.zone, this.translate);
    this.serverDataSource = new AttributeDatasource(this.attributeService, this.telemetryWsService, this.zone, this.translate);
    this.dataSource = new MatTableDataSource<GatewayAttributeData>([]);
  }

  private initConnectorForm(): void {
    this.connectorForm = this.fb.group({
      mode: [ConfigurationModes.BASIC],
      name: ['', [Validators.required, this.uniqNameRequired(), Validators.pattern(noLeadTrailSpacesRegex)]],
      type: ['', [Validators.required]],
      enableRemoteLogging: [false],
      logLevel: ['', [Validators.required]],
      sendDataOnlyOnChange: [false],
      key: ['auto'],
      class: [''],
      configuration: [''],
      configurationJson: [{}, [Validators.required]],
      basicConfig: [{}],
      configVersion: [''],
      reportStrategy: [{ value: {}, disabled: true }],
    });
    this.connectorForm.disable();
  }

  private getSortingDataAccessor(): (data: GatewayAttributeData, sortHeaderId: string) => string | number {
    return (data: GatewayAttributeData, sortHeaderId: string) => {
      switch (sortHeaderId) {
        case 'syncStatus':
          return this.isConnectorSynced(data) ? 1 : 0;

        case 'enabled':
          return this.activeConnectors.includes(data.key) ? 1 : 0;

        case 'errors':
          const errors = this.getErrorsCount(data);
          if (typeof errors === 'string') {
            return this.sort.direction.toUpperCase() === Direction.DESC ? -1 : Infinity;
          }
          return errors;

        default:
          return data[sortHeaderId] || data.value[sortHeaderId];
      }
    };
  }

  private loadConnectors(): void {
    if (!this.device || this.device.id === NULL_UUID) {
      return;
    }

    forkJoin([
      this.attributeService.getEntityAttributes(this.device, AttributeScope.SHARED_SCOPE, ['active_connectors']),
      this.attributeService.getEntityAttributes(this.device, AttributeScope.SERVER_SCOPE, ['inactive_connectors']),
      this.attributeService.getEntityAttributes(this.device, AttributeScope.CLIENT_SCOPE, ['Version'])
    ]).pipe(takeUntil(this.destroy$)).subscribe(attributes => {
      this.activeConnectors = this.parseConnectors(attributes[0]);
      this.inactiveConnectors = this.parseConnectors(attributes[1]);
      this.gatewayVersion = attributes[2][0]?.value;

      this.updateData(true);
    });
  }

  private loadGatewayState(): void {
    this.attributeService.getEntityAttributes(this.device, AttributeScope.SERVER_SCOPE)
      .pipe(takeUntil(this.destroy$))
      .subscribe((attributes: AttributeData[]) => {

        const active = attributes.find(data => data.key === 'active').value;
        const lastDisconnectedTime = attributes.find(data => data.key === 'lastDisconnectTime')?.value;
        const lastConnectedTime = attributes.find(data => data.key === 'lastConnectTime')?.value;

        this.isGatewayActive = this.getGatewayStatus(active, lastConnectedTime, lastDisconnectedTime);
      });
  }

  private parseConnectors(attribute: GatewayAttributeData[]): string[] {
    const connectors = attribute?.[0]?.value || [];
    return isString(connectors) ? JSON.parse(connectors) : connectors;
  }

  private observeModeChange(): void {
    this.connectorForm.get('mode').valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.connectorForm.get('mode').markAsPristine();
      });
  }

  private observeAttributeChange(): void {
    this.attributeUpdateSubject.pipe(
      debounceTime(300),
      tap((attribute: GatewayAttributeData) => this.executeAttributeUpdates(attribute)),
      takeUntil(this.destroy$),
    ).subscribe();
  }

  private updateActiveConnectorKeys(key: string): void {
    const wasEnabled = this.activeConnectors.includes(key);
    if (wasEnabled) {
      const index = this.activeConnectors.indexOf(key);
      if (index !== -1) {
        this.activeConnectors.splice(index, 1);
      }
      this.inactiveConnectors.push(key);
    } else {
      const index = this.inactiveConnectors.indexOf(key);
      if (index !== -1) {
        this.inactiveConnectors.splice(index, 1);
      }
      this.activeConnectors.push(key);
    }
  }

  private executeAttributeUpdates(attribute: GatewayAttributeData): void {
    forkJoin(this.getAttributeExecutionTasks(attribute))
      .pipe(
        take(1),
        tap(() => this.updateData(true)),
        takeUntil(this.destroy$),
      )
      .subscribe();
  }

  private getAttributeExecutionTasks(attribute: GatewayAttributeData): Observable<any>[] {
    const isActive = this.activeConnectors.includes(attribute.key);
    const scopeOld =  isActive ? AttributeScope.SERVER_SCOPE : AttributeScope.SHARED_SCOPE;
    const scopeNew = isActive ? AttributeScope.SHARED_SCOPE : AttributeScope.SERVER_SCOPE;

    return [
      this.attributeService.saveEntityAttributes(this.device, AttributeScope.SHARED_SCOPE, [{
        key: 'active_connectors',
        value: this.activeConnectors
      }]),
      this.attributeService.saveEntityAttributes(this.device, AttributeScope.SERVER_SCOPE, [{
        key: 'inactive_connectors',
        value: this.inactiveConnectors
      }]),
      this.attributeService.deleteEntityAttributes(this.device, scopeOld, [attribute]),
      this.attributeService.saveEntityAttributes(this.device, scopeNew, [attribute])
    ];
  }

  private onDataUpdateError(e: any): void {
    const exceptionData = this.utils.parseException(e);
    let errorText = exceptionData.name;
    if (exceptionData.message) {
      errorText += ': ' + exceptionData.message;
    }
    console.error(errorText);
  }

  private onErrorsUpdated(): void {
    this.cd.detectChanges();
  }

  private onDataUpdated(): void {
    const dataSources = this.ctx.defaultSubscription.data;

    const active = dataSources.find(data => data.dataKey.name === 'active').data[0][1];
    const lastDisconnectedTime = dataSources.find(data => data.dataKey.name === 'lastDisconnectTime').data[0][1];
    const lastConnectedTime = dataSources.find(data => data.dataKey.name === 'lastConnectTime').data[0][1];

    this.isGatewayActive = this.getGatewayStatus(active, lastConnectedTime, lastDisconnectedTime);

    this.cd.detectChanges();
  }

  private getGatewayStatus(active: boolean, lastConnectedTime: number, lastDisconnectedTime: number): boolean {
    if (!active) {
      return false;
    }
    return !lastDisconnectedTime || lastConnectedTime > lastDisconnectedTime;
  }

  private generateSubscription(): void {
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

  private createBasicConfigWatcher(): void {
    if (this.basicConfigSub) {
      this.basicConfigSub.unsubscribe();
    }
    this.basicConfigSub = this.connectorForm.get('basicConfig').valueChanges.pipe(
      filter(() => !!this.initialConnector),
      takeUntil(this.destroy$)
    ).subscribe((config) => {
      const configJson = this.connectorForm.get('configurationJson');
      const type = this.connectorForm.get('type').value;
      const mode = this.connectorForm.get('mode').value;
      if (!isEqual(config, configJson?.value) && this.allowBasicConfig.has(type) && mode === ConfigurationModes.BASIC) {
        const newConfig = {...configJson.value, ...config};
        this.connectorForm.get('configurationJson').patchValue(newConfig, {emitEvent: false});
      }
    });
  }

  private createJsonConfigWatcher(): void {
    if (this.jsonConfigSub) {
      this.jsonConfigSub.unsubscribe();
    }
    this.jsonConfigSub = this.connectorForm.get('configurationJson').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((config) => {
      const basicConfig = this.connectorForm.get('basicConfig');
      const type = this.connectorForm.get('type').value;
      const mode = this.connectorForm.get('mode').value;
      if (!isEqual(config, basicConfig?.value) && this.allowBasicConfig.has(type) && mode === ConfigurationModes.ADVANCED) {
        this.connectorForm.get('basicConfig').patchValue(config, {emitEvent: false});
      }
    });
  }

  private confirmConnectorChange(): Observable<boolean> {
    if (this.initialConnector && this.connectorForm.dirty) {
      return this.dialogService.confirm(
        this.translate.instant('gateway.change-connector-title'),
        this.translate.instant('gateway.change-connector-text'),
        this.translate.instant('action.no'),
        this.translate.instant('action.yes'),
        true
      );
    }
    return of(true);
  }

  private setFormValue(connector: GatewayConnector): void {
    if (this.connectorForm.disabled) {
      this.connectorForm.enable();
    }

    const connectorState = GatewayConnectorVersionMappingUtil.getConfig({
      configuration: '',
      key: 'auto',
      configurationJson: {} as ConnectorBaseConfig,
      ...connector,
    }, this.gatewayVersion);

    if (this.gatewayVersion && !connectorState.configVersion) {
      connectorState.configVersion = this.gatewayVersion;
    }

    connectorState.basicConfig = connectorState.configurationJson;
    this.initialConnector = connectorState;
    this.updateConnector(connectorState);
  }

  private updateConnector(connector: GatewayConnector): void {
    this.jsonConfigSub?.unsubscribe();
    switch (connector.type) {
      case ConnectorType.MQTT:
      case ConnectorType.OPCUA:
      case ConnectorType.MODBUS:
        this.updateBasicConfigConnector(connector);
        break;
      default:
        this.connectorForm.patchValue({...connector, mode: null});
        this.connectorForm.markAsPristine();
        this.createJsonConfigWatcher();
    }
  }

  private updateBasicConfigConnector(connector: GatewayConnector): void {
    this.basicConfigSub?.unsubscribe();
    const previousType = this.connectorForm.get('type').value;
    this.setInitialConnectorValues(connector);

    if (previousType === connector.type || !this.allowBasicConfig.has(connector.type)) {
      this.patchBasicConfigConnector(connector);
    } else {
      this.basicConfigInitSubject.asObservable().pipe(take(1)).subscribe(() => {
        this.patchBasicConfigConnector(connector);
      });
    }
  }

  private patchBasicConfigConnector(connector: GatewayConnector): void {
    this.connectorForm.patchValue(connector, {emitEvent: false});
    this.connectorForm.markAsPristine();
    this.createBasicConfigWatcher();
    this.createJsonConfigWatcher();
  }

  private toggleReportStrategy(type: ConnectorType): void {
    const reportStrategyControl = this.connectorForm.get('reportStrategy');
    if (type === ConnectorType.MODBUS) {
      reportStrategyControl.enable({emitEvent: false});
    } else {
      reportStrategyControl.disable({emitEvent: false});
    }
  }

  private setClientData(data: PageData<GatewayAttributeData>): void {
    if (this.initialConnector) {
      const clientConnectorData = data.data.find(attr => attr.key === this.initialConnector.name);
      if (clientConnectorData) {
        clientConnectorData.value = typeof clientConnectorData.value === 'string' ?
          JSON.parse(clientConnectorData.value) : clientConnectorData.value;

        if (this.isConnectorSynced(clientConnectorData) && clientConnectorData.value.configurationJson) {
          this.setFormValue({...clientConnectorData.value, mode: this.connectorForm.get('mode').value ?? clientConnectorData.value.mode});
        }
      }
    }
  }
}
