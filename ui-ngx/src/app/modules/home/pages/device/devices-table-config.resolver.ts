///
/// Copyright © 2016-2025 The Thingsboard Authors
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

import { Injectable } from '@angular/core';

import { ActivatedRoute, ActivatedRouteSnapshot, Router } from '@angular/router';
import {
  CellActionDescriptor,
  checkBoxCell,
  DateEntityTableColumn,
  EntityTableColumn,
  EntityTableConfig,
  GroupActionDescriptor,
  HeaderActionDescriptor
} from '@home/models/entity/entities-table-config.models';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { AddEntityDialogData, EntityAction } from '@home/models/entity/entity-component.models';
import {
  Device,
  DeviceCredentials,
  DeviceInfo,
  DeviceInfoFilter,
  DeviceInfoQuery
} from '@app/shared/models/device.models';
import { DeviceComponent } from '@modules/home/pages/device/device.component';
import { forkJoin, Observable, of, Subject } from 'rxjs';
import { select, Store } from '@ngrx/store';
import { selectAuthUser, selectUserSettingsProperty } from '@core/auth/auth.selectors';
import { map, mergeMap, take, tap } from 'rxjs/operators';
import { AppState } from '@core/core.state';
import { DeviceService } from '@app/core/http/device.service';
import { Authority } from '@app/shared/models/authority.enum';
import { CustomerService } from '@core/http/customer.service';
import { Customer } from '@app/shared/models/customer.model';
import { NULL_UUID } from '@shared/models/id/has-uuid';
import { BroadcastService } from '@core/services/broadcast.service';
import { DeviceTableHeaderComponent } from '@modules/home/pages/device/device-table-header.component';
import { MatDialog } from '@angular/material/dialog';
import {
  DeviceCredentialsDialogComponent,
  DeviceCredentialsDialogData
} from '@modules/home/pages/device/device-credentials-dialog.component';
import { DialogService } from '@core/services/dialog.service';
import {
  AssignToCustomerDialogComponent,
  AssignToCustomerDialogData
} from '@modules/home/dialogs/assign-to-customer-dialog.component';
import { DeviceId } from '@app/shared/models/id/device-id';
import {
  AddEntitiesToCustomerDialogComponent,
  AddEntitiesToCustomerDialogData
} from '../../dialogs/add-entities-to-customer-dialog.component';
import { DeviceTabsComponent } from '@home/pages/device/device-tabs.component';
import { HomeDialogsService } from '@home/dialogs/home-dialogs.service';
import { DeviceWizardDialogComponent } from '@home/components/wizard/device-wizard-dialog.component';
import { BaseData, HasId } from '@shared/models/base-data';
import { deepClone, isDefined, isDefinedAndNotNull } from '@core/utils';
import { EdgeService } from '@core/http/edge.service';
import {
  AddEntitiesToEdgeDialogComponent,
  AddEntitiesToEdgeDialogData
} from '@home/dialogs/add-entities-to-edge-dialog.component';
import { EdgeId } from '@shared/models/id/edge-id';
import { CustomerId } from '@shared/models/id/customer-id';
import { PageLink, PageQueryParam } from '@shared/models/page/page-link';
import { DeviceProfileId } from '@shared/models/id/device-profile-id';
import {
  DeviceCheckConnectivityDialogComponent,
  DeviceCheckConnectivityDialogData
} from '@home/pages/device/device-check-connectivity-dialog.component';
import { EntityId } from '@shared/models/id/entity-id';

interface DevicePageQueryParams extends PageQueryParam {
  deviceProfileId?: string;
  active?: boolean | string;
}

@Injectable()
export class DevicesTableConfigResolver  {

  private readonly config: EntityTableConfig<DeviceInfo> = new EntityTableConfig<DeviceInfo>();

  private customerId: string;

  constructor(private store: Store<AppState>,
              private broadcast: BroadcastService,
              private deviceService: DeviceService,
              private customerService: CustomerService,
              private dialogService: DialogService,
              private edgeService: EdgeService,
              private homeDialogs: HomeDialogsService,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private router: Router,
              private dialog: MatDialog) {

    this.config.entityType = EntityType.DEVICE;
    this.config.entityComponent = DeviceComponent;
    this.config.entityTabsComponent = DeviceTabsComponent;
    this.config.entityTranslations = entityTypeTranslations.get(EntityType.DEVICE);
    this.config.entityResources = entityTypeResources.get(EntityType.DEVICE);

    this.config.addDialogStyle = {width: '600px'};

    this.config.deleteEntityTitle = device => this.translate.instant('device.delete-device-title', {deviceName: device.name});
    this.config.deleteEntityContent = () => this.translate.instant('device.delete-device-text');
    this.config.deleteEntitiesTitle = count => this.translate.instant('device.delete-devices-title', {count});
    this.config.deleteEntitiesContent = () => this.translate.instant('device.delete-devices-text');

    this.config.loadEntity = id => this.deviceService.getDeviceInfo(id.id);
    this.config.saveEntity = device => this.deviceService.saveDevice(device).pipe(
        tap(() => {
          this.broadcast.broadcast('deviceSaved');
        }),
        mergeMap((savedDevice) => this.deviceService.getDeviceInfo(savedDevice.id.id)
        ));
    this.config.onEntityAction = action => this.onDeviceAction(action, this.config);
    this.config.detailsReadonly = () =>
      (this.config.componentsData.deviceScope === 'customer_user' || this.config.componentsData.deviceScope === 'edge_customer_user');
    this.config.onLoadAction = (route) => this.onLoadAction(route);

    this.config.headerComponent = DeviceTableHeaderComponent;

  }

  resolve(route: ActivatedRouteSnapshot): Observable<EntityTableConfig<DeviceInfo>> {
    const routeParams = route.params;
    this.config.componentsData = {
      deviceScope: route.data.devicesType,
      deviceInfoFilter: {},
      deviceCredentials$: new Subject<DeviceCredentials>(),
      edgeId: routeParams.edgeId
    };
    this.customerId = routeParams.customerId;
    this.config.componentsData.edgeId = routeParams.edgeId;
    return this.store.pipe(select(selectAuthUser), take(1)).pipe(
      tap((authUser) => {
        if (authUser.authority === Authority.CUSTOMER_USER) {
          if (route.data.devicesType === 'edge') {
            this.config.componentsData.deviceScope = 'edge_customer_user';
          } else {
            this.config.componentsData.deviceScope = 'customer_user';
          }
          this.customerId = authUser.customerId;
        }
      }),
      mergeMap(() =>
        this.customerId ? this.customerService.getCustomer(this.customerId) : of(null as Customer)
      ),
      map((parentCustomer) => {
        if (parentCustomer) {
          if (parentCustomer.additionalInfo && parentCustomer.additionalInfo.isPublic) {
            this.config.tableTitle = this.translate.instant('customer.public-devices');
          } else {
            this.config.tableTitle = parentCustomer.title + ': ' + this.translate.instant('device.devices');
          }
        } else if (this.config.componentsData.deviceScope === 'edge') {
          this.edgeService.getEdge(this.config.componentsData.edgeId).subscribe(
            edge => this.config.tableTitle = edge.name + ': ' + this.translate.instant('device.devices')
          );
        } else {
          this.config.tableTitle = this.translate.instant('device.devices');
        }
        this.config.columns = this.configureColumns(this.config.componentsData.deviceScope);
        this.configureEntityFunctions(this.config.componentsData.deviceScope);
        this.config.cellActionDescriptors = this.configureCellActions(this.config.componentsData.deviceScope);
        this.config.groupActionDescriptors = this.configureGroupActions(this.config.componentsData.deviceScope);
        this.config.addActionDescriptors = this.configureAddActions(this.config.componentsData.deviceScope);
        this.config.addEnabled = !(this.config.componentsData.deviceScope === 'customer_user' ||
          this.config.componentsData.deviceScope === 'edge_customer_user');
        this.config.entitiesDeleteEnabled = this.config.componentsData.deviceScope === 'tenant';
        this.config.deleteEnabled = () => this.config.componentsData.deviceScope === 'tenant';
        return this.config;
      })
    );
  }

  onLoadAction(route: ActivatedRoute): void {
    const routerQueryParams: DevicePageQueryParams = route.snapshot.queryParams;
    if (routerQueryParams) {
      const queryParams = deepClone(routerQueryParams);
      let replaceUrl = false;
      if (routerQueryParams?.deviceProfileId) {
        this.config.componentsData.deviceInfoFilter.deviceProfileId = new DeviceProfileId(routerQueryParams?.deviceProfileId);
        delete queryParams.deviceProfileId;
        replaceUrl = true;
      }
      if (isDefined(routerQueryParams?.active)) {
        this.config.componentsData.deviceInfoFilter.active = (routerQueryParams?.active === true || routerQueryParams?.active === 'true');
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

  configureColumns(deviceScope: string): Array<EntityTableColumn<DeviceInfo>> {
    const columns: Array<EntityTableColumn<DeviceInfo>> = [
      new DateEntityTableColumn<DeviceInfo>('createdTime', 'common.created-time', this.datePipe, '150px'),
      new EntityTableColumn<DeviceInfo>('name', 'device.name', '25%'),
      new EntityTableColumn<DeviceInfo>('deviceProfileName', 'device-profile.device-profile', '25%'),
      new EntityTableColumn<DeviceInfo>('label', 'device.label', '25%'),
      new EntityTableColumn<DeviceInfo>('active', 'device.state', '80px',
        entity => this.deviceState(entity), entity => this.deviceStateStyle(entity))
    ];
    if (deviceScope === 'tenant') {
      columns.push(
        new EntityTableColumn<DeviceInfo>('customerTitle', 'customer.customer', '25%'),
        new EntityTableColumn<DeviceInfo>('customerIsPublic', 'device.public', '60px',
          entity => checkBoxCell(entity.customerIsPublic), () => ({})),
      );
    }
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

  configureEntityFunctions(deviceScope: string): void {
    this.config.entitiesFetchFunction = pageLink => this.deviceService.getDeviceInfosByQuery(this.prepareDeviceInfoQuery(pageLink));
    if (deviceScope === 'tenant') {
      this.config.deleteEntity = id => this.deviceService.deleteDevice(id.id);
    } else {
      this.config.deleteEntity = () => of();
    }
  }

  prepareDeviceInfoQuery(pageLink: PageLink): DeviceInfoQuery {
    const deviceInfoFilter: DeviceInfoFilter = deepClone(this.config.componentsData.deviceInfoFilter);
    if (this.config.componentsData.deviceScope === 'edge' || this.config.componentsData.deviceScope === 'edge_customer_user') {
      deviceInfoFilter.edgeId = new EdgeId(this.config.componentsData.edgeId);
    } else if (this.config.componentsData.deviceScope !== 'tenant') {
      deviceInfoFilter.customerId = new CustomerId(this.customerId);
    }
    return new DeviceInfoQuery(pageLink, deviceInfoFilter);
  }

  configureCellActions(deviceScope: string): Array<CellActionDescriptor<DeviceInfo>> {
    const actions: Array<CellActionDescriptor<DeviceInfo>> = [];
    if (deviceScope === 'tenant') {
      actions.push(
        {
          name: this.translate.instant('device.make-public'),
          icon: 'share',
          isEnabled: (entity) => (!entity.customerId || entity.customerId.id === NULL_UUID),
          onAction: ($event, entity) => this.makePublic($event, entity)
        },
        {
          name: this.translate.instant('device.assign-to-customer'),
          icon: 'assignment_ind',
          isEnabled: (entity) => (!entity.customerId || entity.customerId.id === NULL_UUID),
          onAction: ($event, entity) => this.assignToCustomer($event, [entity.id])
        },
        {
          name: this.translate.instant('device.unassign-from-customer'),
          icon: 'assignment_return',
          isEnabled: (entity) => (entity.customerId && entity.customerId.id !== NULL_UUID && !entity.customerIsPublic),
          onAction: ($event, entity) => this.unassignFromCustomer($event, entity)
        },
        {
          name: this.translate.instant('device.make-private'),
          icon: 'reply',
          isEnabled: (entity) => (entity.customerId && entity.customerId.id !== NULL_UUID && entity.customerIsPublic),
          onAction: ($event, entity) => this.unassignFromCustomer($event, entity)
        },
        {
          name: this.translate.instant('device.manage-credentials'),
          icon: 'security',
          isEnabled: () => true,
          onAction: ($event, entity) => this.manageCredentials($event, entity)
        }
      );
    }
    if (deviceScope === 'customer') {
      actions.push(
        {
          name: this.translate.instant('device.unassign-from-customer'),
          icon: 'assignment_return',
          isEnabled: (entity) => (entity.customerId && entity.customerId.id !== NULL_UUID && !entity.customerIsPublic),
          onAction: ($event, entity) => this.unassignFromCustomer($event, entity)
        },
        {
          name: this.translate.instant('device.make-private'),
          icon: 'reply',
          isEnabled: (entity) => (entity.customerId && entity.customerId.id !== NULL_UUID && entity.customerIsPublic),
          onAction: ($event, entity) => this.unassignFromCustomer($event, entity)
        },
        {
          name: this.translate.instant('device.manage-credentials'),
          icon: 'security',
          isEnabled: () => true,
          onAction: ($event, entity) => this.manageCredentials($event, entity)
        }
      );
    }
    if (deviceScope === 'customer_user' || deviceScope === 'edge_customer_user') {
      actions.push(
        {
          name: this.translate.instant('device.view-credentials'),
          icon: 'security',
          isEnabled: () => true,
          onAction: ($event, entity) => this.manageCredentials($event, entity)
        }
      );
    }
    if (deviceScope === 'edge') {
      actions.push(
        {
          name: this.translate.instant('edge.unassign-from-edge'),
          icon: 'assignment_return',
          isEnabled: (entity) => true,
          onAction: ($event, entity) => this.unassignFromEdge($event, entity)
        }
      );
    }
    return actions;
  }

  configureGroupActions(deviceScope: string): Array<GroupActionDescriptor<DeviceInfo>> {
    const actions: Array<GroupActionDescriptor<DeviceInfo>> = [];
    if (deviceScope === 'tenant') {
      actions.push(
        {
          name: this.translate.instant('device.assign-devices'),
          icon: 'assignment_ind',
          isEnabled: true,
          onAction: ($event, entities) => this.assignToCustomer($event, entities.map((entity) => entity.id))
        }
      );
    }
    if (deviceScope === 'customer') {
      actions.push(
        {
          name: this.translate.instant('device.unassign-devices'),
          icon: 'assignment_return',
          isEnabled: true,
          onAction: ($event, entities) => this.unassignDevicesFromCustomer($event, entities)
        }
      );
    }
    if (deviceScope === 'edge') {
      actions.push(
        {
          name: this.translate.instant('device.unassign-devices-from-edge'),
          icon: 'assignment_return',
          isEnabled: true,
          onAction: ($event, entities) => this.unassignDevicesFromEdge($event, entities)
        }
      );
    }
    return actions;
  }

  configureAddActions(deviceScope: string): Array<HeaderActionDescriptor> {
    this.config.addEntity = null;
    const actions: Array<HeaderActionDescriptor> = [];
    if (deviceScope === 'tenant') {
      actions.push(
        {
          name: this.translate.instant('device.add-device-text'),
          icon: 'insert_drive_file',
          isEnabled: () => true,
          onAction: ($event) => this.deviceWizard($event)
        },
        {
          name: this.translate.instant('device.import'),
          icon: 'file_upload',
          isEnabled: () => true,
          onAction: ($event) => this.importDevices($event)
        },
      );
      this.config.addEntity = () => {this.deviceWizard(null); return of(null); };
    }
    if (deviceScope === 'customer') {
      actions.push(
        {
          name: this.translate.instant('device.assign-new-device'),
          icon: 'add',
          isEnabled: () => true,
          onAction: ($event) => this.addDevicesToCustomer($event)
        }
      );
    }
    if (deviceScope === 'edge') {
      actions.push(
        {
          name: this.translate.instant('device.assign-new-device'),
          icon: 'add',
          isEnabled: () => true,
          onAction: ($event) => this.addDevicesToEdge($event)
        }
      );
    }
    return actions;
  }

  private openDevice($event: Event, device: Device, config: EntityTableConfig<DeviceInfo>) {
    if ($event) {
      $event.stopPropagation();
    }
    const url = this.router.createUrlTree([device.id.id], {relativeTo: config.getActivatedRoute()});
    this.router.navigateByUrl(url);
  }

  importDevices($event: Event) {
    this.homeDialogs.importEntities(EntityType.DEVICE).subscribe((res) => {
      if (res) {
        this.broadcast.broadcast('deviceSaved');
        this.config.updateData();
      }
    });
  }

  deviceWizard($event: Event) {
    this.dialog.open<DeviceWizardDialogComponent, AddEntityDialogData<BaseData<HasId>>,
      Device>(DeviceWizardDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog']
    }).afterClosed().subscribe(
      (res) => {
        if (res) {
          this.store.pipe(select(selectUserSettingsProperty( 'notDisplayConnectivityAfterAddDevice'))).pipe(
            take(1)
          ).subscribe((settings: boolean) => {
            if(!settings) {
              this.checkConnectivity(null, res.id, true);
            } else {
              this.config.updateData();
            }
          });
        }
      }
    );
  }

  addDevicesToCustomer($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<AddEntitiesToCustomerDialogComponent, AddEntitiesToCustomerDialogData,
      boolean>(AddEntitiesToCustomerDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        customerId: this.customerId,
        entityType: EntityType.DEVICE
      }
    }).afterClosed()
      .subscribe((res) => {
        if (res) {
          this.config.updateData();
        }
      });
  }

  makePublic($event: Event, device: Device) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialogService.confirm(
      this.translate.instant('device.make-public-device-title', {deviceName: device.name}),
      this.translate.instant('device.make-public-device-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe((res) => {
        if (res) {
          this.deviceService.makeDevicePublic(device.id.id).subscribe(
            () => {
              this.config.updateData();
            }
          );
        }
      }
    );
  }

  assignToCustomer($event: Event, deviceIds: Array<DeviceId>) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<AssignToCustomerDialogComponent, AssignToCustomerDialogData,
      boolean>(AssignToCustomerDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        entityIds: deviceIds,
        entityType: EntityType.DEVICE
      }
    }).afterClosed()
      .subscribe((res) => {
        if (res) {
          this.config.updateData();
        }
      });
  }

  unassignFromCustomer($event: Event, device: DeviceInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    const isPublic = device.customerIsPublic;
    let title;
    let content;
    if (isPublic) {
      title = this.translate.instant('device.make-private-device-title', {deviceName: device.name});
      content = this.translate.instant('device.make-private-device-text');
    } else {
      title = this.translate.instant('device.unassign-device-title', {deviceName: device.name});
      content = this.translate.instant('device.unassign-device-text');
    }
    this.dialogService.confirm(
      title,
      content,
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe((res) => {
        if (res) {
          this.deviceService.unassignDeviceFromCustomer(device.id.id).subscribe(
            () => {
              this.config.updateData(this.config.componentsData.deviceScope !== 'tenant');
            }
          );
        }
      }
    );
  }

  unassignDevicesFromCustomer($event: Event, devices: Array<DeviceInfo>) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialogService.confirm(
      this.translate.instant('device.unassign-devices-title', {count: devices.length}),
      this.translate.instant('device.unassign-devices-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe((res) => {
        if (res) {
          const tasks: Observable<any>[] = [];
          devices.forEach(
            (device) => {
              tasks.push(this.deviceService.unassignDeviceFromCustomer(device.id.id));
            }
          );
          forkJoin(tasks).subscribe(
            () => {
              this.config.updateData();
            }
          );
        }
      }
    );
  }

  manageCredentials($event: Event, device: Device) {
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
        isReadOnly: this.config.componentsData.deviceScope === 'customer_user' ||
          this.config.componentsData.deviceScope === 'edge_customer_user'
      }
    }).afterClosed().subscribe(deviceCredentials => {
      if (isDefinedAndNotNull(deviceCredentials)) {
        this.config.componentsData.deviceCredentials$.next(deviceCredentials);
      }
    });
  }

  onDeviceAction(action: EntityAction<DeviceInfo>, config: EntityTableConfig<DeviceInfo>): boolean {
    switch (action.action) {
      case 'open':
        this.openDevice(action.event, action.entity, config);
        return true;
      case 'makePublic':
        this.makePublic(action.event, action.entity);
        return true;
      case 'assignToCustomer':
        this.assignToCustomer(action.event, [action.entity.id]);
        return true;
      case 'unassignFromCustomer':
        this.unassignFromCustomer(action.event, action.entity);
        return true;
      case 'unassignFromEdge':
        this.unassignFromEdge(action.event, action.entity);
        return true;
      case 'manageCredentials':
        this.manageCredentials(action.event, action.entity);
        return true;
      case 'checkConnectivity':
        this.checkConnectivity(action.event, action.entity.id);
        return true;
    }
    return false;
  }

  addDevicesToEdge($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<AddEntitiesToEdgeDialogComponent, AddEntitiesToEdgeDialogData,
      boolean>(AddEntitiesToEdgeDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        edgeId: this.config.componentsData.edgeId,
        entityType: EntityType.DEVICE
      }
    }).afterClosed()
      .subscribe((res) => {
        if (res) {
          this.config.updateData();
        }
      });
  }

  unassignFromEdge($event: Event, device: DeviceInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialogService.confirm(
      this.translate.instant('device.unassign-device-from-edge-title', {deviceName: device.name}),
      this.translate.instant('device.unassign-device-from-edge-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe((res) => {
        if (res) {
          this.deviceService.unassignDeviceFromEdge(this.config.componentsData.edgeId, device.id.id).subscribe(
            () => {
              this.config.updateData(this.config.componentsData.deviceScope !== 'tenant');
            }
          );
        }
      }
    );
  }

  unassignDevicesFromEdge($event: Event, devices: Array<DeviceInfo>) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialogService.confirm(
      this.translate.instant('device.unassign-devices-from-edge-title', {count: devices.length}),
      this.translate.instant('device.unassign-devices-from-edge-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe((res) => {
        if (res) {
          const tasks: Observable<any>[] = [];
          devices.forEach(
            (device) => {
              tasks.push(this.deviceService.unassignDeviceFromEdge(this.config.componentsData.edgeId, device.id.id));
            }
          );
          forkJoin(tasks).subscribe(
            () => {
              this.config.updateData();
            }
          );
        }
      }
    );
  }

  checkConnectivity($event: Event, deviceId: EntityId, afterAdd = false) {
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
          this.config.updateData();
        }
      });
  }
}
