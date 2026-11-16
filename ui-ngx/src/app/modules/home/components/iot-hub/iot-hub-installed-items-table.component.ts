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
import { ConverterService } from '@core/http/converter.service';
import { ResourcesService } from '@core/services/resources.service';
import {
  DefaultConverterUpdate,
  differsFromDefaultConverter,
  loadDefaultUplinkConverter,
  loadOwnDefaultConverterIds
} from '@home/components/iot-hub/iot-hub-default-converter';
import { firstValueFrom, forkJoin } from 'rxjs';

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
  // Installed device packages whose default converter differs from ThingsBoard's default converter now
  converterUpdates = new Map<string, DefaultConverterUpdate>();
  private converterCheckRun = 0;
  updatesChecked = false;
  isCheckingUpdates = false;
  // IoT Hub didn't answer Check for updates: the page says so quietly, the list works without it
  updatesCheckFailed = false;
  // After Check for updates the list shows only the items with an update, until the user asks for all of them
  onlyUpdates = false;
  // How many installed items have an update, and how many there are, of all types and with no search; null while not known
  updatesCount: number | null = null;
  // The last load of the list failed: the list is empty, but not because nothing has an update
  loadFailed = false;
  installedCount = 0;
  // A later load (another page, mode, sort or search) wins over an earlier one that answers last
  private loadRun = 0;

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
    private cd: ChangeDetectorRef,
    private converterService: ConverterService,
    private resourcesService: ResourcesService
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
    this.updatesCheckFailed = false;
    this.iotHubApiService.getInstalledItemIds({ ignoreLoading: true }).pipe(
      // An IoT Hub that doesn't answer is not a page error: the page says so next to the button, the list works without it
      switchMap(itemIds => this.iotHubApiService.getItemsPublishedVersions(itemIds, { ignoreLoading: true, ignoreErrors: true }))
    ).subscribe({
      next: (infos) => {
        this.publishedVersionMap.clear();
        infos.forEach(info => this.publishedVersionMap.set(info.itemId, info));
        this.updatesChecked = true;
        this.isCheckingUpdates = false;
        this.showOnlyUpdates(true);
        this.loadUpdatesCount();
      },
      error: () => {
        this.isCheckingUpdates = false;
        this.updatesCheckFailed = true;
      }
    });
  }

  /** Only the items with an update, or all of them; from the first page, as the list changes. */
  showOnlyUpdates(onlyUpdates: boolean): void {
    this.onlyUpdates = onlyUpdates;
    this.pageIndex = 0;
    if (this.paginator) {
      this.paginator.pageIndex = 0;
    }
    this.loadData();
  }

  /** IoT Hub's published version id by item id, of the items it publishes a version of. */
  private publishedVersionIds(): Record<string, string> {
    const ids: Record<string, string> = {};
    this.publishedVersionMap.forEach((info, itemId) => {
      if (info.publishedVersionId) {
        ids[itemId] = info.publishedVersionId;
      }
    });
    return ids;
  }

  /** After an item is updated or removed: the list and, once the updates are checked, the counts in the header. */
  private reloadAfterChange(): void {
    this.loadData();
    if (this.updatesChecked && !this.itemVersion) {
      this.loadUpdatesCount();
    }
  }

  private loadUpdatesCount(): void {
    this.updatesCount = null;
    forkJoin({
      withUpdates: this.iotHubApiService.getInstalledItemsWithUpdates(new PageLink(1), null, this.publishedVersionIds(), { ignoreLoading: true }),
      installed: this.iotHubApiService.getInstalledItemsCount(undefined, { ignoreLoading: true })
    }).subscribe({
      next: ({ withUpdates, installed }) => {
        this.updatesCount = withUpdates.totalElements;
        this.installedCount = installed;
        this.cd.markForCheck();
      },
      // The header then shows only the buttons
      error: () => this.cd.markForCheck()
    });
  }

  updateItem(item: IotHubInstalledItem, publishedInfo: ItemPublishedVersionInfo): void {
    this.iotHubActions.updateItem(item, publishedInfo.publishedVersion, publishedInfo.publishedVersionId).subscribe(result => {
      if (result === 'updated') {
        this.reloadAfterChange();
      }
    });
  }

  hasConverterUpdate(item: IotHubInstalledItem): boolean {
    return this.converterUpdates.has(item.id.id);
  }

  /** Brings the package's default converter to ThingsBoard's default one. Other packages on the converter take it too. */
  updateConverter(item: IotHubInstalledItem): void {
    const update = this.converterUpdates.get(item.id.id);
    if (!update) {
      return;
    }
    this.iotHubActions.updateDefaultConverter(update).subscribe(updated => {
      if (updated) {
        this.loadData();
      }
    });
  }

  /**
   * The device packages on this page whose step with "default": true installed a converter that differs from
   * ThingsBoard's default converter now. ThingsBoard's default changes with ThingsBoard, so the package has no new version for it.
   */
  private async checkConverterUpdates(): Promise<void> {
    // A later load (another page, sort or search) starts another run: an earlier one finishing last mustn't win
    const run = ++this.converterCheckRun;
    // The package's default converter of each DEVICE package on the page
    const checks: { item: IotHubInstalledItem; stateKey: string; converterId: string }[] = [];
    // Over all installed packages, not only this page's, as the update dialog decides
    let ownDefaultConverterIds: Set<string> | undefined;
    for (const item of this.dataSource) {
      const states = item.itemType === 'DEVICE' ? Object.entries((item.descriptor as DeviceInstalledItemDescriptor)?.installState ?? {}) : [];
      for (const [stateKey, state] of states) {
        const id: string = state?.defaultConverter ? state.entityOutput?.id : undefined;
        if (!id) {
          continue;
        }
        // A converter taken as it was counts only when a package installed it as its default converter
        if (state.resolution === 'use-existing') {
          ownDefaultConverterIds ??= await loadOwnDefaultConverterIds(this.iotHubApiService);
          if (!ownDefaultConverterIds.has(id)) {
            continue;
          }
        }
        checks.push({ item, stateKey, converterId: id });
      }
    }
    const changed = await Promise.all(checks.map(async ({ item, stateKey, converterId }): Promise<DefaultConverterUpdate | null> => {
      const converter = await firstValueFrom(this.converterService.getConverter(converterId, {ignoreErrors: true, ignoreLoading: true}))
        .catch(() => null);
      if (!converter) {
        return null;
      }
      const defaultConverter = await loadDefaultUplinkConverter(this.resourcesService, converter.integrationType,
        converter.configuration.scriptLang, converter.name);
      return defaultConverter && differsFromDefaultConverter(converter, defaultConverter) ?
        { installedItem: item, stateKey, converter, defaultConverter } : null;
    }));
    if (run !== this.converterCheckRun) {
      return;
    }
    this.converterUpdates = new Map(changed.filter(update => !!update).map(update => [update.installedItem.id.id, update]));
    this.cd.markForCheck();
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
    // The page of one item's versions has its own published version and lists all of them
    const items$ = this.onlyUpdates && !this.itemVersion
      ? this.iotHubApiService.getInstalledItemsWithUpdates(pageLink, typeFilters, this.publishedVersionIds(), {ignoreLoading: true})
      : this.iotHubApiService.getInstalledItems(pageLink, typeFilters, this.itemVersion?.itemId, {ignoreLoading: true});
    const run = ++this.loadRun;
    items$.subscribe({
      next: (data) => {
        if (run !== this.loadRun) {
          return;
        }
        // The last page emptied (its last item updated or removed): go to the page that is last now
        if (!data.data.length && this.pageIndex > 0 && data.totalElements > 0) {
          this.pageIndex = Math.ceil(data.totalElements / this.pageSize) - 1;
          if (this.paginator) {
            this.paginator.pageIndex = this.pageIndex;
          }
          this.loadData();
          return;
        }
        this.dataSource = data.data;
        this.totalElements = data.totalElements;
        this.loadFailed = false;
        this.isLoading = false;
        // In the list of items with an update their version update shows, not the converter's
        if (this.updatesChecked && !this.itemVersion && !this.onlyUpdates) {
          void this.checkConverterUpdates();
        }
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
