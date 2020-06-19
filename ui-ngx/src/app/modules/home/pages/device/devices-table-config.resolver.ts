///
/// Copyright © 2016-2020 The Thingsboard Authors
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

import { ActivatedRouteSnapshot, Resolve, Router } from '@angular/router';
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
import { EntityAction } from '@home/models/entity/entity-component.models';
import { Device, DeviceCredentials, DeviceInfo } from '@app/shared/models/device.models';
import { DeviceComponent } from '@modules/home/pages/device/device.component';
import { forkJoin, Observable, of } from 'rxjs';
import { select, Store } from '@ngrx/store';
import { selectAuthUser } from '@core/auth/auth.selectors';
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

@Injectable()
export class DevicesTableConfigResolver implements Resolve<EntityTableConfig<DeviceInfo>> {

  private readonly config: EntityTableConfig<DeviceInfo> = new EntityTableConfig<DeviceInfo>();

  private customerId: string;

  constructor(private store: Store<AppState>,
              private broadcast: BroadcastService,
              private deviceService: DeviceService,
              private customerService: CustomerService,
              private dialogService: DialogService,
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

    this.config.deleteEntityTitle = device => this.translate.instant('device.delete-device-title', { deviceName: device.name });
    this.config.deleteEntityContent = () => this.translate.instant('device.delete-device-text');
    this.config.deleteEntitiesTitle = count => this.translate.instant('device.delete-devices-title', {count});
    this.config.deleteEntitiesContent = () => this.translate.instant('device.delete-devices-text');

    this.config.loadEntity = id => this.deviceService.getDeviceInfo(id.id);
    this.config.saveEntity = device => {
      return this.deviceService.saveDevice(device).pipe(
        tap(() => {
          this.broadcast.broadcast('deviceSaved');
        }),
        mergeMap((savedDevice) => this.deviceService.getDeviceInfo(savedDevice.id.id)
      ));
    };
    this.config.onEntityAction = action => this.onDeviceAction(action);
    this.config.detailsReadonly = () => this.config.componentsData.deviceScope === 'customer_user';

    this.config.headerComponent = DeviceTableHeaderComponent;

  }

  resolve(route: ActivatedRouteSnapshot): Observable<EntityTableConfig<DeviceInfo>> {
    const routeParams = route.params;
    this.config.componentsData = {
      deviceScope: route.data.devicesType,
      deviceType: ''
    };
    this.customerId = routeParams.customerId;
    return this.store.pipe(select(selectAuthUser), take(1)).pipe(
      tap((authUser) => {
        if (authUser.authority === Authority.CUSTOMER_USER) {
          this.config.componentsData.deviceScope = 'customer_user';
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
        } else {
          this.config.tableTitle = this.translate.instant('device.devices');
        }
        this.config.columns = this.configureColumns(this.config.componentsData.deviceScope);
        this.configureEntityFunctions(this.config.componentsData.deviceScope);
        this.config.cellActionDescriptors = this.configureCellActions(this.config.componentsData.deviceScope);
        this.config.groupActionDescriptors = this.configureGroupActions(this.config.componentsData.deviceScope);
        this.config.addActionDescriptors = this.configureAddActions(this.config.componentsData.deviceScope);
        this.config.addEnabled = this.config.componentsData.deviceScope !== 'customer_user';
        this.config.entitiesDeleteEnabled = this.config.componentsData.deviceScope === 'tenant';
        this.config.deleteEnabled = () => this.config.componentsData.deviceScope === 'tenant';
        return this.config;
      })
    );
  }

  configureColumns(deviceScope: string): Array<EntityTableColumn<DeviceInfo>> {
    const columns: Array<EntityTableColumn<DeviceInfo>> = [
      new DateEntityTableColumn<DeviceInfo>('createdTime', 'common.created-time', this.datePipe, '150px'),
      new EntityTableColumn<DeviceInfo>('name', 'device.name', '25%'),
      new EntityTableColumn<DeviceInfo>('type', 'device.device-type', '25%'),
      new EntityTableColumn<DeviceInfo>('label', 'device.label', '25%')
    ];
    if (deviceScope === 'tenant') {
      columns.push(
        new EntityTableColumn<DeviceInfo>('customerTitle', 'customer.customer', '25%'),
        new EntityTableColumn<DeviceInfo>('customerIsPublic', 'device.public', '60px',
          entity => {
            return checkBoxCell(entity.customerIsPublic);
          }, () => ({}), false),
      );
    }
    columns.push(
      new EntityTableColumn<DeviceInfo>('gateway', 'device.is-gateway', '60px',
        entity => {
          return checkBoxCell(entity.additionalInfo && entity.additionalInfo.gateway);
        }, () => ({}), false)
    );
    return columns;
  }

  configureEntityFunctions(deviceScope: string): void {
    if (deviceScope === 'tenant') {
      this.config.entitiesFetchFunction = pageLink =>
        this.deviceService.getTenantDeviceInfos(pageLink, this.config.componentsData.deviceType);
      this.config.deleteEntity = id => this.deviceService.deleteDevice(id.id);
    } else {
      this.config.entitiesFetchFunction = pageLink =>
        this.deviceService.getCustomerDeviceInfos(this.customerId, pageLink, this.config.componentsData.deviceType);
      this.config.deleteEntity = id => this.deviceService.unassignDeviceFromCustomer(id.id);
    }
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
          isEnabled: (entity) => true,
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
            isEnabled: (entity) => true,
            onAction: ($event, entity) => this.manageCredentials($event, entity)
          }
        );
    }
    if (deviceScope === 'customer_user') {
      actions.push(
        {
          name: this.translate.instant('device.view-credentials'),
          icon: 'security',
          isEnabled: (entity) => true,
          onAction: ($event, entity) => this.manageCredentials($event, entity)
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
    return actions;
  }

  configureAddActions(deviceScope: string): Array<HeaderActionDescriptor> {
    const actions: Array<HeaderActionDescriptor> = [];
    if (deviceScope === 'tenant') {
      actions.push(
        {
          name: this.translate.instant('device.add-device-text'),
          icon: 'insert_drive_file',
          isEnabled: () => true,
          onAction: ($event) => this.config.table.addEntity($event)
        },
        {
          name: this.translate.instant('device.import'),
          icon: 'file_upload',
          isEnabled: () => true,
          onAction: ($event) => this.importDevices($event)
        }
      );
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
    return actions;
  }

  importDevices($event: Event) {
    this.homeDialogs.importEntities(EntityType.DEVICE).subscribe((res) => {
      if (res) {
        this.broadcast.broadcast('deviceSaved');
        this.config.table.updateData();
      }
    });
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
          this.config.table.updateData();
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
              this.config.table.updateData();
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
        this.config.table.updateData();
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
              this.config.table.updateData();
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
              this.config.table.updateData();
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
        isReadOnly: this.config.componentsData.deviceScope === 'customer_user'
      }
    });
  }

  onDeviceAction(action: EntityAction<DeviceInfo>): boolean {
    switch (action.action) {
      case 'makePublic':
        this.makePublic(action.event, action.entity);
        return true;
      case 'assignToCustomer':
        this.assignToCustomer(action.event, [action.entity.id]);
        return true;
      case 'unassignFromCustomer':
        this.unassignFromCustomer(action.event, action.entity);
        return true;
      case 'manageCredentials':
        this.manageCredentials(action.event, action.entity);
        return true;
    }
    return false;
  }

}
