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

import {
  AfterViewInit,
  ChangeDetectorRef,
  Component,
  ElementRef,
  HostBinding,
  Input,
  NgZone,
  OnChanges,
  OnDestroy,
  OnInit,
  SimpleChanges,
  ViewChild
} from '@angular/core';
import { Router } from '@angular/router';
import { MatSort } from '@angular/material/sort';
import { MatPaginator } from '@angular/material/paginator';
import { IotHubApiService } from '@core/http/iot-hub-api.service';
import { TranslateService } from '@ngx-translate/core';
import { switchMap } from 'rxjs/operators';
import { PageLink } from '@shared/models/page/page-link';
import { Direction, SortOrder } from '@shared/models/page/sort-order';
import {
  DeviceInstalledItemDescriptor,
  IotHubInstalledItem,
  ItemPublishedVersionInfo
} from '@shared/models/iot-hub/iot-hub-installed-item.models';
import { MpItemVersionView } from '@shared/models/iot-hub/iot-hub-version.models';
import { ItemType, itemTypeTranslations } from '@shared/models/iot-hub/iot-hub-item.models';
import { EntityType } from '@shared/models/entity-type.models';
import { getEntityDetailsPageURL } from '@core/utils';
import { IotHubActionsService } from '@home/components/iot-hub/iot-hub-actions.service';

@Component({
  selector: 'tb-iot-hub-installed-items-table',
  standalone: false,
  templateUrl: './iot-hub-installed-items-table.component.html',
  styleUrls: ['./iot-hub-installed-items-table.component.scss']
})
export class TbIotHubInstalledItemsTableComponent implements OnInit, OnChanges, AfterViewInit, OnDestroy {

  @Input() textSearch = '';
  @Input() typeFilters: string[] = [];
  @Input() itemVersion: MpItemVersionView;

  @HostBinding('class.tb-installed-with-version') get withVersionClass() {
    return !!this.itemVersion;
  }

  displayedColumns: string[] = ['itemName', 'itemType', 'version', 'createdTime', 'updates', 'actions'];
  dataSource: IotHubInstalledItem[] = [];
  totalElements = 0;
  pageSize = 10;
  pageIndex = 0;
  hidePageSize = false;
  isLoading = false;

  publishedVersionMap = new Map<string, ItemPublishedVersionInfo>();
  updatesChecked = false;
  isCheckingUpdates = false;

  private widgetResize$: ResizeObserver;

  @ViewChild(MatSort, {static: true}) sort: MatSort;
  @ViewChild(MatPaginator, {static: true}) paginator: MatPaginator;

  constructor(
    private iotHubApiService: IotHubApiService,
    private translate: TranslateService,
    private router: Router,
    private iotHubActions: IotHubActionsService,
    private elementRef: ElementRef,
    private zone: NgZone,
    private cd: ChangeDetectorRef
  ) {
    this.widgetResize$ = new ResizeObserver(() => {
      this.zone.run(() => {
        const shouldHide = this.elementRef.nativeElement.offsetWidth < 640;
        if (shouldHide !== this.hidePageSize) {
          this.hidePageSize = shouldHide;
          this.cd.markForCheck();
        }
      });
    });
    this.widgetResize$.observe(this.elementRef.nativeElement);
  }

  ngOnInit(): void {
    if (this.itemVersion) {
      this.displayedColumns = ['version', 'createdTime', 'updates', 'actions'];
      this.updatesChecked = true;
      this.publishedVersionMap.set(this.itemVersion.itemId, {
        itemId: this.itemVersion.itemId,
        publishedVersionId: this.itemVersion.id,
        publishedVersion: this.itemVersion.version
      });
    }
    this.loadData();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.textSearch || changes.typeFilters) {
      if (!changes.textSearch?.firstChange || !changes.typeFilters?.firstChange) {
        this.pageIndex = 0;
        if (this.paginator) {
          this.paginator.pageIndex = 0;
        }
        this.loadData();
      }
    }
  }

  ngAfterViewInit(): void {
    this.sort.sortChange.subscribe(() => {
      this.pageIndex = 0;
      this.paginator.pageIndex = 0;
      this.loadData();
    });
  }

  ngOnDestroy(): void {
    this.widgetResize$?.disconnect();
  }

  onPageChange(event: any): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadData();
  }

  getItemTypeIcon(itemType: string): string {
    switch (itemType) {
      case 'WIDGET': return 'widgets';
      case 'DASHBOARD': return 'dashboard';
      case 'SOLUTION_TEMPLATE': return 'integration_instructions';
      case 'CALCULATED_FIELD': return 'functions';
      case 'RULE_CHAIN': return 'settings_ethernet';
      case 'DEVICE': return 'memory';
      default: return 'category';
    }
  }

  deleteItem(item: IotHubInstalledItem): void {
    this.iotHubActions.deleteItem(item).subscribe(confirmed => {
      if (confirmed) {
        this.loadData();
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
      case 'SOLUTION_TEMPLATE': return 'tb-type-solution-template';
      default: return '';
    }
  }

  viewItemDetails(item: IotHubInstalledItem): void {
    if (item.itemType === 'DEVICE') {
      this.openDeviceReviewDialog(item);
      return;
    }
    this.iotHubApiService.getVersionInfo(item.itemVersionId, {ignoreLoading: true}).subscribe(versionView => {
      this.iotHubActions.openItemDetail(versionView, item).subscribe(result => {
        if (result === 'updated' || result === 'deleted') {
          this.loadData();
        }
      });
    });
  }

  private openDeviceReviewDialog(item: IotHubInstalledItem): void {
    const descriptor = item.descriptor as DeviceInstalledItemDescriptor;
    if (!descriptor.installState || !descriptor.selectedInstallMethod) {
      this.iotHubApiService.getVersionInfo(item.itemVersionId, {ignoreLoading: true}).subscribe(versionView => {
        this.iotHubActions.openItemDetail(versionView, item).subscribe();
      });
      return;
    }
    this.iotHubApiService.getVersionInfo(item.itemVersionId, {ignoreLoading: true, ignoreErrors: true}).subscribe(versionView => {
      this.iotHubActions.reviewDevice(versionView, descriptor).subscribe();
    });
  }

  getEntityId(item: IotHubInstalledItem): string | null {
    const descriptor = item.descriptor;
    switch (descriptor.type) {
      case 'WIDGET': return descriptor.widgetTypeId?.id;
      case 'DASHBOARD': return descriptor.dashboardId?.id;
      case 'CALCULATED_FIELD': return descriptor.entityId?.id;
      case 'RULE_CHAIN': return descriptor.ruleChainId?.id;
      case 'DEVICE': return descriptor.dashboardId?.id ?? null;
      case 'SOLUTION_TEMPLATE': return descriptor.dashboardId?.id;
      default: return null;
    }
  }

  getEntityType(item: IotHubInstalledItem): EntityType | null {
    const descriptor = item.descriptor;
    switch (descriptor.type) {
      case 'WIDGET': return EntityType.WIDGET_TYPE;
      case 'DASHBOARD': return EntityType.DASHBOARD;
      case 'CALCULATED_FIELD': return descriptor.entityId?.entityType as EntityType;
      case 'RULE_CHAIN': return EntityType.RULE_CHAIN;
      case 'DEVICE': return descriptor.dashboardId ? EntityType.DASHBOARD : null;
      case 'SOLUTION_TEMPLATE': return EntityType.DASHBOARD;
      default: return null;
    }
  }

  openEntity(item: IotHubInstalledItem): void {
    const entityType = this.getEntityType(item);
    const entityId = this.getEntityId(item);
    if (entityType && entityId) {
      const url = getEntityDetailsPageURL(entityId, entityType);
      if (url) {
        window.open(this.router.serializeUrl(this.router.parseUrl(url)), '_blank');
      }
    }
  }

  checkForUpdates(): void {
    this.isCheckingUpdates = true;
    this.iotHubApiService.getInstalledItemIds({ ignoreLoading: true }).pipe(
      switchMap(itemIds => this.iotHubApiService.getItemsPublishedVersions(itemIds, { ignoreLoading: true }))
    ).subscribe({
      next: (infos) => {
        this.publishedVersionMap.clear();
        infos.forEach(info => this.publishedVersionMap.set(info.itemId, info));
        this.updatesChecked = true;
        this.isCheckingUpdates = false;
      },
      error: () => {
        this.isCheckingUpdates = false;
      }
    });
  }

  viewUpdateDetails(publishedInfo: ItemPublishedVersionInfo, installedItem: IotHubInstalledItem): void {
    this.iotHubApiService.getVersionInfo(publishedInfo.publishedVersionId, {ignoreLoading: true}).subscribe(versionView => {
      this.iotHubActions.openItemDetail(versionView, installedItem).subscribe(result => {
        if (result === 'updated' || result === 'deleted') {
          this.loadData();
        }
      });
    });
  }

  updateItem(item: IotHubInstalledItem, publishedInfo: ItemPublishedVersionInfo): void {
    this.iotHubActions.updateItem(item, publishedInfo.publishedVersion, publishedInfo.publishedVersionId).subscribe(result => {
      if (result === 'updated') {
        this.loadData();
      }
    });
  }

  getPublishedVersionInfo(item: IotHubInstalledItem): ItemPublishedVersionInfo | undefined {
    return this.publishedVersionMap.get(item.itemId);
  }

  private loadData(): void {
    this.isLoading = true;
    const sortOrder: SortOrder = {
      property: this.sort?.active || 'createdTime',
      direction: this.sort?.direction === 'asc' ? Direction.ASC : Direction.DESC
    };
    const pageLink = new PageLink(this.pageSize, this.pageIndex, this.textSearch || null, sortOrder);
    const typeFilters = this.typeFilters?.length ? this.typeFilters : null;
    this.iotHubApiService.getInstalledItems(pageLink, typeFilters, this.itemVersion?.itemId, {ignoreLoading: true}).subscribe({
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
