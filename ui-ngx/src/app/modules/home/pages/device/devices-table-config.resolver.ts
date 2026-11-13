// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { DestroyRef, Injectable } from '@angular/core';
import { ActivatedRoute, ActivatedRouteSnapshot, Router } from '@angular/router';
import {
  CellActionDescriptor,
  checkBoxCell,
  DateEntityTableColumn,
  EntityChipsEntityTableColumn,
  EntityColumn,
  EntityTableColumn,
  EntityTableConfig,
  GroupActionDescriptor,
  HeaderActionDescriptor
} from '@home/models/entity/entities-table-config.models';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { EntityAction } from '@home/models/entity/entity-component.models';
import {
  Device,
  deviceAiAssistantConfig,
  DeviceCredentials,
  DeviceInfo,
  DeviceInfoFilter,
  DeviceInfoQuery
} from '@app/shared/models/device.models';
import { AiAssistantViewType } from '@shared/models/ai-chat.models';
import { Observable, of, Subject } from 'rxjs';
import { select, Store } from '@ngrx/store';
import { getCurrentAuthUser, selectUserSettingsProperty } from '@core/auth/auth.selectors';
import { map, mergeMap, take, tap } from 'rxjs/operators';
import { AppState } from '@core/core.state';
import { DeviceService } from '@app/core/http/device.service';
import { Authority } from '@app/shared/models/authority.enum';
import { CustomerService } from '@core/http/customer.service';
import { Customer } from '@app/shared/models/customer.model';
import { BroadcastService } from '@core/services/broadcast.service';
import { DeviceTableHeaderComponent } from '@modules/home/pages/device/device-table-header.component';
import { MatDialog } from '@angular/material/dialog';
import {
  DeviceCredentialsDialogComponent,
  DeviceCredentialsDialogData
} from '@modules/home/pages/device/device-credentials-dialog.component';
import { HomeDialogsService } from '@home/dialogs/home-dialogs.service';
import { UtilsService } from '@core/services/utils.service';
import { deepClone, isDefined, isDefinedAndNotNull } from '@core/utils';
import { DeviceComponent } from './device.component';
import { AllEntitiesTableConfigService } from '@home/components/entity/all-entities-table-config.service';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { resolveGroupParams } from '@shared/models/entity-group.models';
import { AuthUser } from '@shared/models/user.model';
import { Operation, Resource } from '@shared/models/security.models';
import { CustomerId } from '@shared/models/id/customer-id';
import {
  DeviceWizardDialogComponent,
  DeviceWizardDialogData
} from '@home/components/wizard/device-wizard-dialog.component';
import { GroupEntityTabsComponent } from '@home/components/group/group-entity-tabs.component';
import { PageLink, PageQueryParam } from '@shared/models/page/page-link';
import { DeviceProfileId } from '@shared/models/id/device-profile-id';
import {
  DeviceCheckConnectivityDialogComponent,
  DeviceCheckConnectivityDialogData
} from '@home/pages/device/device-check-connectivity-dialog.component';
import { EntityId } from '@shared/models/id/entity-id';
import { WhiteLabelingService } from '@core/http/white-labeling.service';
import { AiDashboardGenerationService } from '@home/components/ai/ai-dashboard-generation.service';
import { IotHubActionsService } from '@home/components/iot-hub/iot-hub-actions.service';
import { ItemType } from '@shared/models/iot-hub/iot-hub-item.models';

interface DevicePageQueryParams extends PageQueryParam {
  deviceProfileId?: string;
  active?: boolean | string;
}

@Injectable()
export class DevicesTableConfigResolver  {

  constructor(private allEntitiesTableConfigService: AllEntitiesTableConfigService<DeviceInfo>,
              private store: Store<AppState>,
              private userPermissionsService: UserPermissionsService,
              private broadcast: BroadcastService,
              private deviceService: DeviceService,
              private customerService: CustomerService,
              private homeDialogs: HomeDialogsService,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private utils: UtilsService,
              private router: Router,
              private dialog: MatDialog,
              private wl: WhiteLabelingService,
              private destroyRef: DestroyRef,
              private iotHubActions: IotHubActionsService,
              private aiDashboardGenerationService: AiDashboardGenerationService) {
  }

  resolve(route: ActivatedRouteSnapshot): Observable<EntityTableConfig<DeviceInfo>> {
    const groupParams = resolveGroupParams(route);
    const config = new EntityTableConfig<DeviceInfo>(groupParams);
    this.configDefaults(config);
    const authUser = getCurrentAuthUser(this.store);
    config.componentsData = {
      deviceInfoFilter: {},
      includeCustomers: true,
      deviceCredentials$: new Subject<DeviceCredentials>(),
      includeCustomersChanged: (includeCustomers: boolean) => {
        config.componentsData.includeCustomers = includeCustomers;
        config.columns = this.configureColumns(authUser, config);
        config.getTable().columnsUpdated();
        config.getTable().resetSortAndFilter(true);
      }
    };
    return (config.customerId ?
      this.customerService.getCustomer(config.customerId) : of(null as Customer)).pipe(
      map((parentCustomer) => {
        if (parentCustomer) {
          config.tableTitle = parentCustomer.title + ': ' + this.translate.instant('device.devices');
        } else {
          config.tableTitle = this.translate.instant('device.devices');
        }
        config.columns = this.configureColumns(authUser, config);
        this.configureEntityFunctions(config);
        config.cellActionDescriptors = this.configureCellActions(config);
        config.groupActionDescriptors = this.configureGroupActions(config);
        config.addActionDescriptors = this.configureAddActions(config);
        config.onLoadAction = (activatedRoute) => this.onLoadAction(config, activatedRoute);
        return this.allEntitiesTableConfigService.prepareConfiguration(config);
      })
    );
  }

  configDefaults(config: EntityTableConfig<DeviceInfo>) {
    config.entityType = EntityType.DEVICE;
    config.entityComponent = DeviceComponent;
    config.entityTabsComponent = GroupEntityTabsComponent<DeviceInfo>;
    config.entityTranslations = entityTypeTranslations.get(EntityType.DEVICE);
    config.entityResources = entityTypeResources.get(EntityType.DEVICE);

    config.entityTitle = (device) => device ?
      this.utils.customTranslation(device.name, device.name) : '';

    config.rowPointer = true;

    config.deleteEntityTitle = device => this.translate.instant('device.delete-device-title', {deviceName: device.name});
    config.deleteEntityContent = () => this.translate.instant('device.delete-device-text');
    config.deleteEntitiesTitle = count => this.translate.instant('device.delete-devices-title', {count});
    config.deleteEntitiesContent = () => this.translate.instant('device.delete-devices-text');

    config.loadEntity = id => this.deviceService.getDeviceInfo(id.id);
    config.saveEntity = device => this.deviceService.saveDevice(device).pipe(
        tap(() => {
          this.broadcast.broadcast('deviceSaved');
        }),
      mergeMap((savedDevice) => this.deviceService.getDeviceInfo(savedDevice.id.id)
      ));
    config.onEntityAction = action => this.onDeviceAction(action, config);
    config.headerComponent = DeviceTableHeaderComponent;
    config.aiAssistantConfig = deviceAiAssistantConfig(this.store, this.userPermissionsService, this.translate,
      this.userPermissionsService.hasGenericPermission(Resource.DEVICE, Operation.WRITE), AiAssistantViewType.DEVICE);
  }

  onLoadAction(config: EntityTableConfig<DeviceInfo>, route: ActivatedRoute): void {
    const routerQueryParams: DevicePageQueryParams = route.snapshot.queryParams;
    if (routerQueryParams) {
      const queryParams = deepClone(routerQueryParams);
      let replaceUrl = false;
      if (routerQueryParams?.deviceProfileId) {
        config.componentsData.deviceInfoFilter.deviceProfileId = new DeviceProfileId(routerQueryParams?.deviceProfileId);
        delete queryParams.deviceProfileId;
        replaceUrl = true;
      }
      if (isDefined(routerQueryParams?.active)) {
        config.componentsData.deviceInfoFilter.active = (routerQueryParams?.active === true || routerQueryParams?.active === 'true');
        delete queryParams.active;
        replaceUrl = true;
      }
      if (replaceUrl) {
        this.router.navigate([], {
          relativeTo: route,
          queryParams,
          queryParamsHandling: '',
          replaceUrl: true
        });
      }
    }
  }

  configureColumns(authUser: AuthUser, config: EntityTableConfig<DeviceInfo>): Array<EntityColumn<DeviceInfo>> {
    const columns: Array<EntityColumn<DeviceInfo>> = [
      new DateEntityTableColumn<DeviceInfo>('createdTime', 'common.created-time', this.datePipe, '150px'),
      new EntityTableColumn<DeviceInfo>('name', 'device.name', '20%', config.entityTitle),
      new EntityTableColumn<DeviceInfo>('type', 'device-profile.device-profile', '20%'),
      new EntityTableColumn<DeviceInfo>('label', 'device.label', '15%'),
      new EntityTableColumn<DeviceInfo>('active', 'device.state', '80px',
        entity => this.deviceState(entity), entity => this.deviceStateStyle(entity))
    ];
    if (config.componentsData.includeCustomers) {
      const title = (authUser.authority === Authority.CUSTOMER_USER || config.customerId)
        ? 'entity.sub-customer-name' : 'entity.customer-name';
      columns.push(new EntityTableColumn<DeviceInfo>('ownerName', title, '20%'));
    }
    columns.push(
      new EntityChipsEntityTableColumn<DeviceInfo>( 'groups', 'entity.groups', '25%')
    );
    columns.push(
      new EntityTableColumn<DeviceInfo>('gateway', 'device.is-gateway', '60px',
        entity => checkBoxCell(entity.additionalInfo && entity.additionalInfo.gateway), () => ({}), false)
    );
    return columns;
  }

  private deviceState(device: DeviceInfo): string {
    let translateKey = 'device.active';
    let backgroundColor = 'rgba(25, 128, 56, 0.08)';
    if (!device.active) {
      translateKey = 'device.inactive';
      backgroundColor = 'rgba(209, 39, 48, 0.08)';
    }
    return `<div class="status" style="border-radius: 16px; height: 32px;
                line-height: 32px; padding: 0 12px; width: fit-content; background-color: ${backgroundColor}">
                ${this.translate.instant(translateKey)}
            </div>`;
  }

  private deviceStateStyle(device: DeviceInfo): object {
    const styleObj = {
      fontSize: '14px',
      color: '#198038',
      cursor: 'pointer'
    };
    if (!device.active) {
      styleObj.color = '#d12730';
    }
    return styleObj;
  }

  configureEntityFunctions(config: EntityTableConfig<DeviceInfo>): void {
    config.entitiesFetchFunction = pageLink => this.deviceService.getDeviceInfosByQuery(this.prepareDeviceInfoQuery(config, pageLink));
    config.deleteEntity = id => this.deviceService.deleteDevice(id.id);
  }

  prepareDeviceInfoQuery(config: EntityTableConfig<DeviceInfo>, pageLink: PageLink): DeviceInfoQuery {
    const deviceInfoFilter: DeviceInfoFilter = deepClone(config.componentsData.deviceInfoFilter);
    deviceInfoFilter.includeCustomers = config.componentsData.includeCustomers;
    if (config.customerId) {
      deviceInfoFilter.customerId = new CustomerId(config.customerId);
    }
    return new DeviceInfoQuery(pageLink, deviceInfoFilter);
  }

  configureCellActions(config: EntityTableConfig<DeviceInfo>): Array<CellActionDescriptor<DeviceInfo>> {
    const actions: Array<CellActionDescriptor<DeviceInfo>> = [];
    if (this.userPermissionsService.hasGenericPermission(Resource.DEVICE, Operation.READ_CREDENTIALS) &&
      !this.userPermissionsService.hasGenericPermission(Resource.DEVICE, Operation.WRITE_CREDENTIALS)) {
      actions.push(
        {
          name: this.translate.instant('device.view-credentials'),
          icon: 'security',
          isEnabled: () => true,
          onAction: ($event, entity) => this.manageCredentials($event, entity, true, config)
        }
      );
    }

    if (this.userPermissionsService.hasGenericPermission(Resource.DEVICE, Operation.WRITE_CREDENTIALS)) {
      actions.push(
        {
          name: this.translate.instant('device.manage-credentials'),
          icon: 'security',
          isEnabled: () => true,
          onAction: ($event, entity) => this.manageCredentials($event, entity, false, config)
        }
      );
    }

    if (this.aiDashboardGenerationService.isAllowedDashboardGenerate({ requireTelemetry: true })) {
      actions.push({
          name: this.translate.instant('solution-creator.generate-dashboard-with-ai'),
          icon: 'mdi:creation',
          isEnabled: () => true,
          onAction: ($event, entity) => this.generateDashboard($event, entity)
        });
    }
    return actions;
  }

  configureGroupActions(config: EntityTableConfig<DeviceInfo>): Array<GroupActionDescriptor<DeviceInfo>> {
    const actions: Array<GroupActionDescriptor<DeviceInfo>> = [];
    return actions;
  }

  configureAddActions(config: EntityTableConfig<DeviceInfo>): Array<HeaderActionDescriptor> {
    const actions: Array<HeaderActionDescriptor> = [];
    const authUser = getCurrentAuthUser(this.store);
    actions.push(
      {
        name: this.translate.instant('device.add-device-text'),
        icon: 'insert_drive_file',
        isEnabled: () => true,
        onAction: ($event) => this.deviceWizard($event, config)
      },
      {
        name: this.translate.instant('device.import'),
        icon: 'file_upload',
        isEnabled: () => true,
        onAction: ($event) => this.importDevices($event, config)
      }
    );
    if (authUser.authority === Authority.TENANT_ADMIN && this.userPermissionsService.hasGenericPermission(Resource.ALL, Operation.ALL)) {
      actions.push(
        {
          name: this.translate.instant('iot-hub.add-from-iot-hub'),
          icon: 'hub',
          isEnabled: () => true,
          onAction: (_$event) => this.addDeviceFromIotHub(config)
        }
      );
    }
    config.addEntity = () => {this.deviceWizard(null, config); return of(null); };
    return actions;
  }

  addDeviceFromIotHub(config: EntityTableConfig<DeviceInfo>) {
    const customerId = config.customerId || undefined;
    this.iotHubActions.addItem(ItemType.DEVICE, { customerId }).subscribe(result => {
      if (result?.descriptor) {
        this.broadcast.broadcast('deviceSaved');
        config.updateData();
      }
    });
  }

  private openDevice($event: Event, device: DeviceInfo, config: EntityTableConfig<DeviceInfo>) {
    if ($event) {
      $event.stopPropagation();
    }
    const url = this.router.createUrlTree([device.id.id], {relativeTo: config.getActivatedRoute()});
    this.router.navigateByUrl(url);
  }

  importDevices($event: Event, config: EntityTableConfig<DeviceInfo>) {
    const customerId = config.customerId ? new CustomerId(config.customerId) : null;
    this.homeDialogs.importEntities(customerId, EntityType.DEVICE, null).subscribe((res) => {
      if (res) {
        this.broadcast.broadcast('deviceSaved');
        config.updateData();
      }
    });
  }

  deviceWizard($event: Event, config: EntityTableConfig<DeviceInfo>) {
    this.dialog.open<DeviceWizardDialogComponent, DeviceWizardDialogData,
      Device>(DeviceWizardDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        customerId: config.customerId
      }
    }).afterClosed().subscribe(
      (res) => {
        if (res) {
          if (this.wl.getHideConnectivityDialog()) {
            config.updateData();
          } else {
            this.store.pipe(select(selectUserSettingsProperty('notDisplayConnectivityAfterAddDevice'))).pipe(
              take(1)
            ).subscribe((settings: boolean) => {
              if (!settings) {
                this.checkConnectivity(null, res.id, true, config);
              } else {
                config.updateData();
              }
            });
          }
        }
      }
    );
  }

  manageCredentials($event: Event, device: DeviceInfo, isReadOnly: boolean, config: EntityTableConfig<DeviceInfo>) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<DeviceCredentialsDialogComponent, DeviceCredentialsDialogData,
      DeviceCredentials>(DeviceCredentialsDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        deviceId: device.id.id,
        deviceProfileId: device.deviceProfileId.id,
        isReadOnly
      }
    }).afterClosed().subscribe(deviceCredentials => {
      if (isDefinedAndNotNull(deviceCredentials)) {
        config.componentsData.deviceCredentials$.next(deviceCredentials);
      }
    });
  }

  manageOwnerAndGroups($event: Event, device: DeviceInfo, config: EntityTableConfig<DeviceInfo>) {
    this.homeDialogs.manageOwnerAndGroups($event, device).subscribe(
      (res) => {
        if (res) {
          config.updateData();
        }
      }
    );
  }

  checkConnectivity($event: Event, deviceId: EntityId, afterAdd = false, config?: EntityTableConfig<DeviceInfo>) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<DeviceCheckConnectivityDialogComponent, DeviceCheckConnectivityDialogData>
    (DeviceCheckConnectivityDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        deviceId,
        afterAdd
      }
    })
      .afterClosed()
      .subscribe(() => {
        if (afterAdd ) {
          config.updateData();
        }
      });
  }

  onDeviceAction(action: EntityAction<DeviceInfo>, config: EntityTableConfig<DeviceInfo>): boolean {
    switch (action.action) {
      case 'open':
        this.openDevice(action.event, action.entity, config);
        return true;
      case 'manageCredentials':
        this.manageCredentials(action.event, action.entity, false, config);
        return true;
      case 'viewCredentials':
        this.manageCredentials(action.event, action.entity, true, config);
        return true;
      case 'manageOwnerAndGroups':
        this.manageOwnerAndGroups(action.event, action.entity, config);
        return true;
      case 'checkConnectivity':
        this.checkConnectivity(action.event, action.entity.id);
        return true;
    }
    return false;
  }

  generateDashboard($event: Event, device: DeviceInfo) {
    $event.stopPropagation();
    this.aiDashboardGenerationService.generateWithTelemetryCheck({
      deviceId: device.id.id,
      destroyRef: this.destroyRef,
      noTelemetry: { checkConnectivity: () => this.checkConnectivity($event, device.id) }
    });
  }
}
