// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import {
  AfterViewInit,
  ChangeDetectorRef,
  Component,
  ElementRef,
  EventEmitter,
  HostBinding,
  Input,
  NgZone,
  OnChanges,
  OnDestroy,
  OnInit,
  Output,
  SimpleChanges,
  ViewChild
} from '@angular/core';
import { Router, UrlTree } from '@angular/router';
import { MatSort } from '@angular/material/sort';
import { MatPaginator } from '@angular/material/paginator';
import { IotHubApiService } from '@core/http/iot-hub-api.service';
import { TranslateService } from '@ngx-translate/core';
import { switchMap } from 'rxjs/operators';
import { PageLink } from '@shared/models/page/page-link';
import { Direction, SortOrder } from '@shared/models/page/sort-order';
import {
  DeviceInstalledItemDescriptor,
  getInstalledItemTarget,
  getInstalledItemUrl,
  IotHubInstalledItem,
  ItemPublishedVersionInfo
} from '@shared/models/iot-hub/iot-hub-installed-item.models';
import { entityTypeTranslations } from '@shared/models/entity-type.models';
import { MpItemVersionView } from '@shared/models/iot-hub/iot-hub-version.models';
import { getItemTypeIcon, ItemType, itemTypeTranslations } from '@shared/models/iot-hub/iot-hub-item.models';
import { IotHubActionsService } from '@home/components/iot-hub/iot-hub-actions.service';
import { forkJoin } from 'rxjs';

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

  @Output() clearFilters = new EventEmitter<void>();

  hasActiveFilters(): boolean {
    return !!this.textSearch || (this.typeFilters?.length ?? 0) > 0;
  }

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
  updatesCheckFailed = false;
  onlyUpdates = false;
  // Totals, ignoring type filters and search
  updatesCount: number | null = null;
  installedCount = 0;
  loadFailed = false;
  private loadRun = 0;
  private countRun = 0;

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
        this.setPageIndex(0);
        this.loadData();
      }
    }
  }

  ngAfterViewInit(): void {
    this.sort.sortChange.subscribe(() => {
      this.setPageIndex(0);
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
    return getItemTypeIcon(itemType);
  }

  deleteItem(item: IotHubInstalledItem): void {
    this.iotHubActions.deleteItem(item).subscribe(deleted => {
      if (deleted) {
        this.reloadAfterChange();
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
      case 'CALCULATED_FIELD': return 'tb-type-calc-field';
      case 'ALARM_RULE': return 'tb-type-alarm-rule';
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
          this.reloadAfterChange();
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

  canOpen(item: IotHubInstalledItem): boolean {
    return !!getInstalledItemTarget(item?.descriptor);
  }

  /** Names what Open opens: for a device package, its dashboard, device or integration. */
  openLabel(item: IotHubInstalledItem): string {
    if (!this.canOpen(item)) {
      return this.translate.instant('iot-hub.nothing-to-open');
    }
    if (item.itemType === 'SOLUTION_TEMPLATE') {
      return this.translate.instant('iot-hub.goto-main-dashboard');
    }
    let type = this.getItemTypeLabel(item.itemType);
    const target = item.itemType === 'DEVICE' ? getInstalledItemTarget(item.descriptor) : null;
    const typeKey = target && entityTypeTranslations.get(target.entityType)?.type;
    if (typeKey) {
      type = this.translate.instant(typeKey);
    }
    return this.translate.instant('iot-hub.open-item-type', { type });
  }

  openEntity(item: IotHubInstalledItem): void {
    const url = getInstalledItemUrl(item?.descriptor);
    if (url) {
      const urlTree = this.router.parseUrl(url);
      window.open(this.router.serializeUrl(urlTree), '_blank');
    }
  }

  checkForUpdates(): void {
    this.isCheckingUpdates = true;
    if (this.updatesChecked) {
      // Only the count failed: recount, keeping the list as it is
      this.loadUpdatesCount();
      return;
    }
    this.updatesCheckFailed = false;
    this.iotHubApiService.getInstalledItemIds({ ignoreLoading: true }).pipe(
      // Failure is shown next to the button, not as an error toast
      switchMap(itemIds => this.iotHubApiService.getItemsPublishedVersions(itemIds, { ignoreLoading: true, ignoreErrors: true }))
    ).subscribe({
      next: (infos) => {
        this.publishedVersionMap.clear();
        infos.forEach(info => this.publishedVersionMap.set(info.itemId, info));
        this.loadUpdatesCount(() => {
          this.updatesChecked = true;
          // No updates: stay on the full list
          this.showOnlyUpdates(this.updatesCount > 0);
        });
      },
      error: () => {
        this.isCheckingUpdates = false;
        this.updatesCheckFailed = true;
      }
    });
  }

  showOnlyUpdates(onlyUpdates: boolean): void {
    this.onlyUpdates = onlyUpdates;
    this.setPageIndex(0);
    this.loadData();
  }

  private setPageIndex(pageIndex: number): void {
    this.pageIndex = pageIndex;
    if (this.paginator) {
      this.paginator.pageIndex = pageIndex;
    }
  }

  private publishedVersionIds(): Record<string, string> {
    const ids: Record<string, string> = {};
    this.publishedVersionMap.forEach((info, itemId) => {
      if (info.publishedVersionId) {
        ids[itemId] = info.publishedVersionId;
      }
    });
    return ids;
  }

  retryLoad(): void {
    this.loadData();
    if (this.updatesChecked && !this.itemVersion && this.updatesCount === null) {
      this.loadUpdatesCount();
    }
  }

  private reloadAfterChange(): void {
    if (!this.updatesChecked || this.itemVersion) {
      this.loadData();
      return;
    }
    this.loadUpdatesCount(() => {
      if (this.onlyUpdates && this.updatesCount === 0) {
        this.showOnlyUpdates(false);
      } else {
        this.loadData();
      }
    }, () => {
      // Count failed: fall back to the full list
      if (this.onlyUpdates) {
        this.showOnlyUpdates(false);
      } else {
        this.loadData();
      }
    });
  }

  // Latest call wins; previous counts stay until it returns
  private loadUpdatesCount(onCounts?: () => void, onError?: () => void): void {
    const run = ++this.countRun;
    const config = { ignoreLoading: true, ignoreErrors: true };
    forkJoin({
      withUpdates: this.iotHubApiService.getInstalledItemsWithUpdates(new PageLink(1), null, this.publishedVersionIds(), config),
      installed: this.iotHubApiService.getInstalledItemsCount(undefined, config)
    }).subscribe({
      next: ({ withUpdates, installed }) => {
        if (run !== this.countRun) {
          return;
        }
        this.updatesCount = withUpdates.totalElements;
        this.installedCount = installed;
        this.updatesCheckFailed = false;
        this.isCheckingUpdates = false;
        this.cd.markForCheck();
        onCounts?.();
      },
      error: () => {
        if (run !== this.countRun) {
          return;
        }
        this.updatesCount = null;
        this.updatesCheckFailed = true;
        this.isCheckingUpdates = false;
        this.cd.markForCheck();
        onError?.();
      }
    });
  }

  updateItem(item: IotHubInstalledItem, publishedInfo: ItemPublishedVersionInfo): void {
    this.iotHubActions.updateItem(item, publishedInfo.publishedVersion, publishedInfo.publishedVersionId).subscribe(result => {
      if (result === 'updated') {
        this.reloadAfterChange();
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
    // The empty state shows a failure of the only-updates list; one item's versions page lists them all
    const items$ = this.onlyUpdates && !this.itemVersion
      ? this.iotHubApiService.getInstalledItemsWithUpdates(pageLink, typeFilters, this.publishedVersionIds(), {ignoreLoading: true, ignoreErrors: true})
      : this.iotHubApiService.getInstalledItems(pageLink, typeFilters, this.itemVersion?.itemId, {ignoreLoading: true});
    const run = ++this.loadRun;
    items$.subscribe({
      next: (data) => {
        if (run !== this.loadRun) {
          return;
        }
        // The last page emptied after an update or a removal
        if (!data.data.length && this.pageIndex > 0 && data.totalElements > 0) {
          this.setPageIndex(Math.ceil(data.totalElements / this.pageSize) - 1);
          this.loadData();
          return;
        }
        this.dataSource = data.data;
        this.totalElements = data.totalElements;
        this.loadFailed = false;
        this.isLoading = false;
      },
      error: () => {
        if (run !== this.loadRun) {
          return;
        }
        this.dataSource = [];
        this.totalElements = 0;
        this.loadFailed = true;
        this.isLoading = false;
      }
    });
  }
}
