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

import { Component, OnInit, AfterViewInit, ViewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { MatSort } from '@angular/material/sort';
import { MatPaginator } from '@angular/material/paginator';
import { IotHubApiService } from '@core/http/iot-hub-api.service';
import { DialogService } from '@core/services/dialog.service';
import { TranslateService } from '@ngx-translate/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { Subject } from 'rxjs';
import { debounceTime } from 'rxjs/operators';
import { PageLink } from '@shared/models/page/page-link';
import { Direction, SortOrder } from '@shared/models/page/sort-order';
import { IotHubInstalledItem, IotHubInstalledItemInfo, ItemUpdateInfo } from '@shared/models/iot-hub/iot-hub-installed-item.models';
import { ItemType, itemTypeTranslations } from '@shared/models/iot-hub/iot-hub-item.models';
import { EntityType } from '@shared/models/entity-type.models';
import { getEntityDetailsPageURL } from '@core/utils';
import { TbIotHubItemDetailDialogComponent, IotHubItemDetailDialogData } from './iot-hub-item-detail-dialog.component';
import { TbIotHubUpdateDialogComponent, IotHubUpdateDialogData } from './iot-hub-update-dialog.component';

@Component({
  selector: 'tb-iot-hub-installed-items',
  standalone: false,
  templateUrl: './iot-hub-installed-items.component.html',
  styleUrls: ['./iot-hub-installed-items.component.scss']
})
export class TbIotHubInstalledItemsComponent implements OnInit, AfterViewInit {

  displayedColumns: string[] = ['itemName', 'itemType', 'version', 'createdTime', 'updates', 'actions'];
  dataSource: IotHubInstalledItem[] = [];
  totalElements = 0;
  pageSize = 10;
  pageIndex = 0;
  isLoading = false;
  textSearch = '';

  installedItemInfos: IotHubInstalledItemInfo[] = [];
  updateInfoMap = new Map<string, ItemUpdateInfo>();
  updatesChecked = false;
  isCheckingUpdates = false;

  private searchSubject = new Subject<string>();

  @ViewChild(MatSort, {static: true}) sort: MatSort;
  @ViewChild(MatPaginator, {static: true}) paginator: MatPaginator;

  private static readonly ITEM_TYPE_TO_ENTITY_TYPE: Record<string, EntityType> = {
    'WIDGET': EntityType.WIDGET_TYPE,
    'DASHBOARD': EntityType.DASHBOARD,
    'CALCULATED_FIELD': EntityType.CALCULATED_FIELD,
    'RULE_CHAIN': EntityType.RULE_CHAIN,
    'DEVICE': EntityType.DEVICE_PROFILE
  };

  constructor(
    private iotHubApiService: IotHubApiService,
    private dialogService: DialogService,
    private translate: TranslateService,
    private store: Store<AppState>,
    private router: Router,
    private dialog: MatDialog,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.installedItemInfos = this.route.snapshot.data['installedItemInfos'] || [];
    this.searchSubject.pipe(
      debounceTime(300)
    ).subscribe(() => {
      this.pageIndex = 0;
      this.paginator.pageIndex = 0;
      this.loadData();
    });
    this.loadData();
  }

  ngAfterViewInit(): void {
    this.sort.sortChange.subscribe(() => {
      this.pageIndex = 0;
      this.paginator.pageIndex = 0;
      this.loadData();
    });
  }

  navigateToMarketplace(): void {
    this.router.navigate(['/iot-hub']);
  }

  onSearchChange(value: string): void {
    this.textSearch = value;
    this.searchSubject.next(value);
  }

  onPageChange(event: any): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadData();
  }

  deleteItem(item: IotHubInstalledItem): void {
    this.dialogService.confirm(
      this.translate.instant('iot-hub.delete-installed-item-title'),
      this.translate.instant('iot-hub.delete-installed-item-text', {name: item.itemName}),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes')
    ).subscribe(result => {
      if (result) {
        this.iotHubApiService.deleteInstalledItem(item.itemId).subscribe({
          next: () => {
            this.store.dispatch(new ActionNotificationShow({
              message: this.translate.instant('iot-hub.installed-item-deleted', {name: item.itemName}),
              type: 'success',
              duration: 3000
            }));
            this.loadData();
          },
          error: () => {
            this.store.dispatch(new ActionNotificationShow({
              message: this.translate.instant('iot-hub.installed-item-delete-error', {name: item.itemName}),
              type: 'error',
              duration: 5000
            }));
          }
        });
      }
    });
  }

  getItemTypeLabel(itemType: string): string {
    const key = itemTypeTranslations.get(itemType as ItemType);
    return key ? this.translate.instant(key) : itemType;
  }

  getItemTypeChipClass(itemType: string): string {
    switch (itemType) {
      case 'WIDGET': return 'tb-type-widget';
      case 'DASHBOARD': return 'tb-type-dashboard';
      case 'CALCULATED_FIELD': return 'tb-type-calc-field';
      case 'RULE_CHAIN': return 'tb-type-rule-chain';
      case 'DEVICE': return 'tb-type-device';
      default: return '';
    }
  }

  viewItemDetails(item: IotHubInstalledItem): void {
    this.iotHubApiService.getVersionInfo(item.itemVersionId, {ignoreLoading: true}).subscribe(versionView => {
      this.dialog.open(TbIotHubItemDetailDialogComponent, {
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: {
          item: versionView,
          iotHubApiService: this.iotHubApiService,
          installedDescriptor: item.descriptor
        } as IotHubItemDetailDialogData
      });
    });
  }

  getEntityId(item: IotHubInstalledItem): string | null {
    const descriptor = item.descriptor;
    switch (descriptor.type) {
      case 'WIDGET': return descriptor.widgetTypeId?.id;
      case 'DASHBOARD': return descriptor.dashboardId?.id;
      case 'CALCULATED_FIELD': return descriptor.calculatedFieldId?.id;
      case 'RULE_CHAIN': return descriptor.ruleChainId?.id;
      default: return null;
    }
  }

  openEntity(item: IotHubInstalledItem): void {
    const entityType = TbIotHubInstalledItemsComponent.ITEM_TYPE_TO_ENTITY_TYPE[item.itemType];
    const entityId = this.getEntityId(item);
    if (entityType && entityId) {
      const url = getEntityDetailsPageURL(entityId, entityType);
      if (url) {
        this.router.navigateByUrl(url);
      }
    }
  }

  checkForUpdates(): void {
    this.isCheckingUpdates = true;
    this.iotHubApiService.checkForUpdates(this.installedItemInfos, { ignoreLoading: true }).subscribe({
      next: (updates) => {
        this.updateInfoMap.clear();
        updates.forEach(info => this.updateInfoMap.set(info.itemId, info));
        this.updatesChecked = true;
        this.isCheckingUpdates = false;
      },
      error: () => {
        this.isCheckingUpdates = false;
      }
    });
  }

  viewUpdateDetails(updateInfo: ItemUpdateInfo, installedItem: IotHubInstalledItem): void {
    this.iotHubApiService.getVersionInfo(updateInfo.latestItemVersionId, {ignoreLoading: true}).subscribe(versionView => {
      this.dialog.open(TbIotHubItemDetailDialogComponent, {
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: {
          item: versionView,
          iotHubApiService: this.iotHubApiService,
          installedDescriptor: installedItem.descriptor,
          installedItemInfo: { itemId: installedItem.itemId, itemVersionId: installedItem.itemVersionId }
        } as IotHubItemDetailDialogData
      });
    });
  }

  updateItem(item: IotHubInstalledItem, updateInfo: ItemUpdateInfo): void {
    const dialogRef = this.dialog.open(TbIotHubUpdateDialogComponent, {
      panelClass: ['tb-dialog'],
      data: {
        itemId: item.itemId,
        itemName: item.itemName,
        itemType: item.itemType as ItemType,
        version: updateInfo.latestVersion,
        versionId: updateInfo.latestItemVersionId,
        iotHubApiService: this.iotHubApiService
      } as IotHubUpdateDialogData
    });
    dialogRef.afterClosed().subscribe(result => {
      if (result === 'updated') {
        this.loadData();
      }
    });
  }

  getUpdateInfo(item: IotHubInstalledItem): ItemUpdateInfo | undefined {
    return this.updateInfoMap.get(item.itemId);
  }

  private loadData(): void {
    this.isLoading = true;
    const sortOrder: SortOrder = {
      property: this.sort?.active || 'createdTime',
      direction: this.sort?.direction === 'asc' ? Direction.ASC : Direction.DESC
    };
    const pageLink = new PageLink(this.pageSize, this.pageIndex, this.textSearch || null, sortOrder);
    this.iotHubApiService.getInstalledItems(pageLink, {ignoreLoading: true}).subscribe({
      next: (data) => {
        this.dataSource = data.data;
        this.totalElements = data.totalElements;
        this.isLoading = false;
      },
      error: () => {
        this.dataSource = [];
        this.totalElements = 0;
        this.isLoading = false;
      }
    });
  }
}
