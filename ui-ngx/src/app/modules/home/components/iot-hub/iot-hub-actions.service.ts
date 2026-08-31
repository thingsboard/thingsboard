///
/// Copyright © 2016-2026 The Thingsboard Authors
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
import { MatDialog } from '@angular/material/dialog';
import { Observable, of, EMPTY } from 'rxjs';
import { map, switchMap } from 'rxjs/operators';
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
import { TbIotHubDeleteDialogComponent, IotHubDeleteDialogData } from './iot-hub-delete-dialog.component';
import { TbDeviceInstallDialogComponent, DeviceInstallDialogData } from './device-install-dialog/device-install-dialog.component';
import { TbIotHubInstalledItemsDialogComponent, IotHubInstalledItemsDialogData } from './iot-hub-installed-items-dialog.component';
import { IotHubBuiltInService } from './iot-hub-built-in.service';
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
    private builtInService: IotHubBuiltInService
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

  addItem(itemType: ItemType, options?: { itemSubType?: string; entityId?: EntityId }): Observable<IotHubAddItemDialogResult> {
    return this.dialog.open(TbIotHubAddItemDialogComponent, {
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog-lt-md'],
      disableClose: true,
      autoFocus: false,
      data: {
        itemType,
        itemSubType: options?.itemSubType,
        entityId: options?.entityId
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
    if (item.type === ItemType.ALARM_RULE) {
      this.dialogService.alert(
        this.translate.instant('iot-hub.alarm-rule-install-update-required'),
        this.translate.instant('iot-hub.alarm-rule-install-update-required-text')
      );
      return EMPTY;
    }
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
  installDevice(item: MpItemVersionView): Observable<string> {
    return this.openDeviceInstallDialog(item);
  }

  reviewDevice(item: MpItemVersionView, deviceDescriptor: DeviceInstalledItemDescriptor): Observable<any> {
    return this.openDeviceInstallDialog(item, {
        reviewMode: true,
        selectedInstallMethod: deviceDescriptor.selectedInstallMethod,
        installState: deviceDescriptor.installState
      });
  }

  private openDeviceInstallDialog(item: MpItemVersionView,
                                  options?: { reviewMode?: boolean; selectedInstallMethod?: string; installState?: any }): Observable<any> {
    return this.dialog.open<TbDeviceInstallDialogComponent, DeviceInstallDialogData>(TbDeviceInstallDialogComponent, {
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog-lt-md'],
      disableClose: true,
      autoFocus: false,
      data: { item, ...options }
    }).afterClosed();
  }
}
