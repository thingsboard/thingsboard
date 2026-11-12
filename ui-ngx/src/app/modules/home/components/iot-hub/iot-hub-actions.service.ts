// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { Injectable } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { Observable, of, EMPTY, forkJoin } from 'rxjs';
import { catchError, map, switchMap } from 'rxjs/operators';
import { TranslateService } from '@ngx-translate/core';
import { DialogService } from '@core/services/dialog.service';
import { MpItemVersionView } from '@shared/models/iot-hub/iot-hub-version.models';
import { ItemType } from '@shared/models/iot-hub/iot-hub-item.models';
import { DeviceInstalledItemDescriptor, IotHubInstalledItem } from '@shared/models/iot-hub/iot-hub-installed-item.models';
import { EntityId } from '@shared/models/id/entity-id';
import { TbIotHubAddItemDialogComponent, IotHubAddItemDialogData, IotHubAddItemDialogResult } from './iot-hub-add-item-dialog.component';
import { TbIotHubItemDetailDialogComponent, IotHubItemDetailDialogData, IotHubItemDetailDialogMode } from './iot-hub-item-detail-dialog.component';
import { TbIotHubInstallDialogComponent, IotHubInstallDialogData } from './iot-hub-install-dialog.component';
import { TbIotHubUpdateDialogComponent, IotHubUpdateDialogData } from './iot-hub-update-dialog.component';
import {
  IotHubDeviceUpdateDialogData,
  TbIotHubDeviceUpdateDialogComponent
} from './iot-hub-device-update-dialog.component';
import { TbIotHubDeleteDialogComponent, IotHubDeleteDialogData } from './iot-hub-delete-dialog.component';
import { TbDeviceInstallDialogComponent, DeviceInstallDialogData } from './device-install-dialog/device-install-dialog.component';
import { TbIotHubInstalledItemsDialogComponent, IotHubInstalledItemsDialogData } from './iot-hub-installed-items-dialog.component';
import { IotHubBuiltInService } from './iot-hub-built-in.service';
import { IotHubApiService } from '@core/http/iot-hub-api.service';
import { DeviceService } from '@core/http/device.service';
import { EntityGroupService } from '@core/http/entity-group.service';
import { EntityType } from '@shared/models/entity-type.models';
import { NULL_UUID } from '@shared/models/id/has-uuid';
import { isBuiltInItem } from './iot-hub-utils';

/**
 * What `openBuiltInOrConfirmInstall` did with a built-in item: 'handled' means nothing is left to do
 * (the local copy was opened, or the user was told why it could not be), while 'install-requested'
 * means the component really is absent and the user asked for it to be installed.
 */
export type IotHubBuiltInAction = 'handled' | 'install-requested';

@Injectable()
export class IotHubActionsService {

  constructor(
    private dialog: MatDialog,
    private dialogService: DialogService,
    private translate: TranslateService,
    private builtInService: IotHubBuiltInService,
    private iotHubApiService: IotHubApiService,
    private deviceService: DeviceService,
    private entityGroupService: EntityGroupService
  ) {}

  openItemDetail(item: MpItemVersionView, installedItem?: IotHubInstalledItem, installedItemsCount?: number,
                 mode?: IotHubItemDetailDialogMode, showCreator?: boolean, preview?: boolean): Observable<any> {
    return this.dialog.open(TbIotHubItemDetailDialogComponent, {
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      disableClose: true,
      autoFocus: false,
      data: { item, installedItem, installedItemsCount, mode, showCreator, preview } as IotHubItemDetailDialogData
    }).afterClosed();
  }

  openInstalledItems(item: MpItemVersionView): Observable<any> {
    return this.dialog.open(TbIotHubInstalledItemsDialogComponent, {
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      disableClose: true,
      autoFocus: false,
      data: { item } as IotHubInstalledItemsDialogData
    }).afterClosed();
  }

  addItem(itemType: ItemType, options?: { itemSubType?: string; entityId?: EntityId; entityGroupId?: string; customerId?: string }): Observable<IotHubAddItemDialogResult> {
    return this.dialog.open(TbIotHubAddItemDialogComponent, {
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog-lt-md'],
      disableClose: true,
      autoFocus: false,
      data: {
        itemType,
        itemSubType: options?.itemSubType,
        entityId: options?.entityId,
        entityGroupId: options?.entityGroupId,
        customerId: options?.customerId
      } as IotHubAddItemDialogData
    }).afterClosed();
  }

  /**
   * Runs the primary action of an item: built-in content is opened locally (never installed),
   * device packages are connected, everything else goes through the install dialog.
   */
  installItem(item: MpItemVersionView): Observable<string> {
    if (isBuiltInItem(item)) {
      return this.openOrInstallBuiltIn(item);
    }
    return this.runInstall(item);
  }

  /** Opens the local copy of a built-in item, or asks to install it when it really is absent. */
  openBuiltInOrConfirmInstall(item: MpItemVersionView): Observable<IotHubBuiltInAction> {
    return this.builtInService.openLocalComponent(item).pipe(
      switchMap(outcome => {
        switch (outcome) {
          case 'missing':
            return this.confirmInstallMissingBuiltIn(item).pipe(
              map((confirmed): IotHubBuiltInAction => confirmed ? 'install-requested' : 'handled')
            );
          case 'failed':
            this.showBuiltInLookupFailed(item);
            return of<IotHubBuiltInAction>('handled');
          default:
            // 'opened' or 'cancelled' — the component is in place either way, so never install.
            return of<IotHubBuiltInAction>('handled');
        }
      })
    );
  }

  /**
   * Asks whether to install a built-in item whose local copy is gone — the component the Hub entry
   * mirrors was deleted from this instance, so installing is the only way to get it back.
   */
  confirmInstallMissingBuiltIn(item: MpItemVersionView): Observable<boolean> {
    return this.dialogService.confirm(
      this.translate.instant('iot-hub.built-in-missing-title'),
      this.translate.instant('iot-hub.built-in-missing-text', { name: item?.name }),
      this.translate.instant('action.cancel'),
      this.translate.instant('iot-hub.install')
    );
  }

  /** Reports that the local copy could not be checked — nothing was opened and nothing installed. */
  showBuiltInLookupFailed(item: MpItemVersionView): void {
    this.dialogService.alert(
      this.translate.instant('iot-hub.built-in-lookup-failed-title'),
      this.translate.instant('iot-hub.built-in-lookup-failed-text', { name: item?.name })
    );
  }

  private openOrInstallBuiltIn(item: MpItemVersionView): Observable<string> {
    return this.openBuiltInOrConfirmInstall(item).pipe(
      // The confirmation inside is the only one the user gets: install starts immediately,
      // and opening the component is a separate click once it is back in the library.
      switchMap(action => action === 'install-requested' ? this.runInstall(item, true) : EMPTY)
    );
  }

  private runInstall(item: MpItemVersionView, skipConfirm = false): Observable<string> {
    if (item.type === ItemType.DEVICE) {
      return this.openDeviceInstallDialog(item);
    }
    return this.dialog.open(TbIotHubInstallDialogComponent, {
      panelClass: ['tb-dialog'],
      disableClose: true,
      autoFocus: false,
      data: { item, skipConfirm } as IotHubInstallDialogData
    }).afterClosed();
  }

  updateItem(installedItem: IotHubInstalledItem, version: string, versionId: string): Observable<string | boolean> {
    if (!installedItem) {
      return of(false);
    }
    if (installedItem.itemType === ItemType.DEVICE) {
      // This version can only reinstall a device package: the dialog leads to the upgrade first.
      return this.dialog.open<TbIotHubDeviceUpdateDialogComponent, IotHubDeviceUpdateDialogData, string | boolean>(
        TbIotHubDeviceUpdateDialogComponent, {
          panelClass: ['tb-dialog'],
          disableClose: true,
          autoFocus: false,
          data: { itemName: installedItem.itemName }
        }).afterClosed().pipe(
        switchMap(result => result === 'reinstall' ? this.reinstallDevice(installedItem, versionId) : of(result))
      );
    }
    return this.dialog.open<TbIotHubUpdateDialogComponent, IotHubUpdateDialogData, string | boolean>(TbIotHubUpdateDialogComponent, {
      panelClass: ['tb-dialog'],
      disableClose: true,
      autoFocus: false,
      data: {
        installedItemId: installedItem.id.id,
        itemName: installedItem.itemName,
        itemType: installedItem.itemType as ItemType,
        version,
        versionId
      }
    }).afterClosed();
  }

  /** The install wizard of the new version updates a device package: it needs the user's input. */
  private reinstallDevice(installedItem: IotHubInstalledItem, versionId: string): Observable<string | boolean> {
    const descriptor = installedItem.descriptor as DeviceInstalledItemDescriptor;
    return forkJoin({
      item: this.iotHubApiService.getVersionInfo(versionId),
      placement: this.findDevicePlacement(descriptor)
    }).pipe(
      switchMap(({ item, placement }) => this.openDeviceInstallDialog(item, {
        replaceInstalledItemId: installedItem.id.id,
        selectedInstallMethod: descriptor?.selectedInstallMethod,
        installState: descriptor?.installState,
        ...placement
      })),
      map(result => result === 'installed' ? 'updated' : false),
      // The error itself is shown by the HTTP interceptor; nothing is changed yet.
      catchError(() => of(false))
    );
  }

  deleteItem(installedItem: IotHubInstalledItem): Observable<boolean> {
    if (!installedItem) {
      return of(false);
    }
    return this.dialog.open<TbIotHubDeleteDialogComponent, IotHubDeleteDialogData, boolean>(TbIotHubDeleteDialogComponent, {
      panelClass: ['tb-dialog'],
      disableClose: true,
      autoFocus: false,
      data: { installedItemId: installedItem.id.id, itemName: installedItem.itemName, itemType: installedItem.itemType }
    }).afterClosed();
  }

  // Reached only for items whose action mode is 'connect', i.e. never for built-in content:
  // that decision is made once, at the public entry points above.
  installDevice(item: MpItemVersionView, options?: { entityGroupId?: string; customerId?: string }): Observable<string> {
    return this.openDeviceInstallDialog(item, {
      entityGroupId: options?.entityGroupId,
      customerId: options?.customerId
    });
  }

  reviewDevice(item: MpItemVersionView, deviceDescriptor: DeviceInstalledItemDescriptor): Observable<any> {
    return this.openDeviceInstallDialog(item, {
        reviewMode: true,
        selectedInstallMethod: deviceDescriptor.selectedInstallMethod,
        installState: deviceDescriptor.installState
      });
  }

  /**
   * The customer and the entity group of the installed version's device: the new version's device is created there
   * too, as the wizard creates it in the group and for the customer it was installed from. With several groups the
   * first one other than "All" is taken; nothing when the device cannot be read. When an earlier update already removed
   * the device, the placement the server kept for it ($placement in the install state).
   */
  private findDevicePlacement(descriptor: DeviceInstalledItemDescriptor): Observable<{ entityGroupId?: string; customerId?: string }> {
    const deviceId = descriptor?.createdEntityIds?.find(id => id.entityType === EntityType.DEVICE)?.id;
    if (!deviceId) {
      const kept = descriptor?.installState?.['$placement'];
      return of({ entityGroupId: kept?.entityGroupId, customerId: kept?.customerId });
    }
    const config = { ignoreLoading: true, ignoreErrors: true };
    // Each lookup failing on its own (a deleted device, no access to its groups) only leaves its part out.
    return forkJoin({
      customerId: this.deviceService.getDevice(deviceId, config).pipe(
        map(device => device.customerId && device.customerId.id !== NULL_UUID ? device.customerId.id : undefined),
        catchError(() => of(undefined))),
      entityGroupId: this.entityGroupService.getEntityGroupIdsForEntityId(EntityType.DEVICE, deviceId, config).pipe(
        switchMap(groupIds => groupIds.length
          ? this.entityGroupService.getEntityGroupsByIds(groupIds.map(groupId => groupId.id), config) : of([])),
        map(groups => groups.find(group => !group.groupAll)?.id.id),
        catchError(() => of(undefined)))
    });
  }

  private openDeviceInstallDialog(item: MpItemVersionView,
                                  options?: { reviewMode?: boolean; selectedInstallMethod?: string; installState?: any;
                                  entityGroupId?: string; customerId?: string; replaceInstalledItemId?: string}): Observable<any> {
    return this.dialog.open<TbDeviceInstallDialogComponent, DeviceInstallDialogData>(TbDeviceInstallDialogComponent, {
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog-lt-md'],
      disableClose: true,
      autoFocus: false,
      data: { item, ...options }
    }).afterClosed();
  }
}
