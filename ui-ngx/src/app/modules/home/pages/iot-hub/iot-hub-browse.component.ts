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

import { Component, OnInit, OnDestroy, Input } from '@angular/core';
import { Subject } from 'rxjs';
import { debounceTime, takeUntil } from 'rxjs/operators';
import { PageLink } from '@shared/models/page/page-link';
import { Direction, SortOrder } from '@shared/models/page/sort-order';
import { PageData } from '@shared/models/page/page-data';
import { MpItemVersionQuery, MpItemVersionView } from '@shared/models/iot-hub/iot-hub-version.models';
import {
  ItemType,
  getCategoriesForType, useCaseTranslations
} from '@shared/models/iot-hub/iot-hub-item.models';
import { cfTypeTranslations, widgetTypeTranslations, ruleChainTypeTranslations } from '@shared/models/iot-hub/iot-hub-version.models';
import { IotHubApiService } from '@core/http/iot-hub-api.service';
import { IotHubInstalledItemInfo } from '@shared/models/iot-hub/iot-hub-installed-item.models';
import { MatDialog } from '@angular/material/dialog';
import { TranslateService } from '@ngx-translate/core';
import { ActivatedRoute, Router } from '@angular/router';
import { TbIotHubItemDetailDialogComponent, IotHubItemDetailDialogData } from './iot-hub-item-detail-dialog.component';
import { TbIotHubInstallDialogComponent, IotHubInstallDialogData } from './iot-hub-install-dialog.component';
import { TbIotHubUpdateDialogComponent, IotHubUpdateDialogData } from './iot-hub-update-dialog.component';

interface SortOption {
  value: string;
  label: string;
  direction: Direction;
}

const TYPE_TABS: ItemType[] = [
  ItemType.WIDGET,
  ItemType.DASHBOARD,
  ItemType.CALCULATED_FIELD,
  ItemType.RULE_CHAIN,
  ItemType.DEVICE
];

@Component({
  selector: 'tb-iot-hub-browse',
  standalone: false,
  templateUrl: './iot-hub-browse.component.html',
  styleUrls: ['./iot-hub-browse.component.scss'],
  host: { '[class.embedded]': 'embedded' }
})
export class TbIotHubBrowseComponent implements OnInit, OnDestroy {

  readonly ItemType = ItemType;

  @Input() creatorId: string;
  @Input() embedded = false;

  items: MpItemVersionView[] = [];
  totalElements = 0;
  pageSize = 12;
  pageIndex = 0;
  isLoading = false;
  hasError = false;

  textSearch = '';
  activeType: ItemType = ItemType.WIDGET;
  activeCategories = new Set<string>();
  activeUseCases = new Set<string>();
  activeCfTypes = new Set<string>();
  activeWidgetTypes = new Set<string>();
  activeRuleChainTypes = new Set<string>();

  sortOptions: SortOption[] = [
    { value: 'totalInstallCount', label: 'iot-hub.sort-most-installed', direction: Direction.DESC },
    { value: 'publishedTime', label: 'iot-hub.sort-newest', direction: Direction.DESC },
    { value: 'name', label: 'iot-hub.sort-name', direction: Direction.ASC }
  ];
  selectedSortIndex = 0;

  categories = new Map<string, string>();
  useCases: Map<string, string> = useCaseTranslations as Map<string, string>;
  cfTypes: Map<string, string> = cfTypeTranslations;
  widgetTypes: Map<string, string> = widgetTypeTranslations;
  ruleChainTypes: Map<string, string> = ruleChainTypeTranslations;

  installedItemsMap = new Map<string, IotHubInstalledItemInfo>();

  private searchSubject = new Subject<string>();
  private destroy$ = new Subject<void>();

  constructor(
    private iotHubApiService: IotHubApiService,
    private dialog: MatDialog,
    private translate: TranslateService,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.searchSubject.pipe(
      debounceTime(300),
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.pageIndex = 0;
      this.loadItems();
    });
    this.updateCategories();
    this.initInstalledItemInfos();
    this.loadItems();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  onSearchChange(value: string): void {
    this.textSearch = value;
    this.searchSubject.next(value);
  }

  getTypeTabIndex(): number {
    return TYPE_TABS.indexOf(this.activeType);
  }

  onTypeTabIndexChange(index: number): void {
    this.onTypeTabChange(TYPE_TABS[index]);
  }

  onTypeTabChange(type: ItemType): void {
    this.activeType = type;
    this.activeCategories.clear();
    this.activeUseCases.clear();
    this.activeCfTypes.clear();
    this.activeWidgetTypes.clear();
    this.activeRuleChainTypes.clear();
    this.updateCategories();
    this.pageIndex = 0;
    this.loadItems();
  }

  onCategoryToggle(category: string): void {
    if (this.activeCategories.has(category)) {
      this.activeCategories.delete(category);
    } else {
      this.activeCategories.add(category);
    }
    this.pageIndex = 0;
    this.loadItems();
  }

  onSortChange(index: number): void {
    this.selectedSortIndex = index;
    this.pageIndex = 0;
    this.loadItems();
  }

  onPageChange(event: { pageIndex: number; pageSize: number }): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadItems();
  }

  clearCategories(): void {
    this.activeCategories.clear();
    this.pageIndex = 0;
    this.loadItems();
  }

  onUseCaseToggle(useCase: string): void {
    if (this.activeUseCases.has(useCase)) {
      this.activeUseCases.delete(useCase);
    } else {
      this.activeUseCases.add(useCase);
    }
    this.pageIndex = 0;
    this.loadItems();
  }

  clearUseCases(): void {
    this.activeUseCases.clear();
    this.pageIndex = 0;
    this.loadItems();
  }

  getSubtypeMap(): Map<string, string> | null {
    switch (this.activeType) {
      case ItemType.WIDGET: return this.widgetTypes;
      case ItemType.CALCULATED_FIELD: return this.cfTypes;
      case ItemType.RULE_CHAIN: return this.ruleChainTypes;
      default: return null;
    }
  }

  getActiveSubtypes(): Set<string> {
    switch (this.activeType) {
      case ItemType.WIDGET: return this.activeWidgetTypes;
      case ItemType.CALCULATED_FIELD: return this.activeCfTypes;
      case ItemType.RULE_CHAIN: return this.activeRuleChainTypes;
      default: return new Set();
    }
  }

  onSubtypeToggle(subtype: string): void {
    const active = this.getActiveSubtypes();
    if (active.has(subtype)) {
      active.delete(subtype);
    } else {
      active.add(subtype);
    }
    this.pageIndex = 0;
    this.loadItems();
  }

  clearSubtypes(): void {
    this.getActiveSubtypes().clear();
    this.pageIndex = 0;
    this.loadItems();
  }

  getActiveSubtypesArray(): string[] {
    return Array.from(this.getActiveSubtypes());
  }

  getActiveCategoriesArray(): string[] {
    return Array.from(this.activeCategories);
  }

  getActiveUseCasesArray(): string[] {
    return Array.from(this.activeUseCases);
  }

  onSubtypesChange(values: string[]): void {
    const active = this.getActiveSubtypes();
    active.clear();
    values.forEach(v => active.add(v));
    this.pageIndex = 0;
    this.loadItems();
  }

  onCategoriesChange(values: string[]): void {
    this.activeCategories.clear();
    values.forEach(v => this.activeCategories.add(v));
    this.pageIndex = 0;
    this.loadItems();
  }

  onUseCasesChange(values: string[]): void {
    this.activeUseCases.clear();
    values.forEach(v => this.activeUseCases.add(v));
    this.pageIndex = 0;
    this.loadItems();
  }

  getSubtypeLabel(key: string): string {
    const map = this.getSubtypeMap();
    const translationKey = map?.get(key);
    return translationKey ? this.translate.instant(translationKey) : key;
  }

  getCategoryLabel(key: string): string {
    const translationKey = this.categories.get(key);
    return translationKey ? this.translate.instant(translationKey) : key;
  }

  getUseCaseLabel(key: string): string {
    const translationKey = this.useCases.get(key);
    return translationKey ? this.translate.instant(translationKey) : key;
  }

  clearAllFilters(): void {
    this.activeCategories.clear();
    this.activeUseCases.clear();
    this.activeCfTypes.clear();
    this.activeWidgetTypes.clear();
    this.activeRuleChainTypes.clear();
    this.textSearch = '';
    this.updateCategories();
    this.pageIndex = 0;
    this.loadItems();
  }

  hasActiveDropdownFilters(): boolean {
    return this.activeCategories.size > 0 ||
           this.activeUseCases.size > 0 || this.activeCfTypes.size > 0 ||
           this.activeWidgetTypes.size > 0 || this.activeRuleChainTypes.size > 0;
  }

  hasActiveFilters(): boolean {
    return this.hasActiveDropdownFilters() || this.textSearch.length > 0;
  }

  getTitle(): string {
    switch (this.activeType) {
      case ItemType.WIDGET: return 'iot-hub.title-widgets';
      case ItemType.DASHBOARD: return 'iot-hub.title-dashboards';
      case ItemType.CALCULATED_FIELD: return 'iot-hub.title-calculated-fields';
      case ItemType.RULE_CHAIN: return 'iot-hub.title-rule-chains';
      case ItemType.DEVICE: return 'iot-hub.title-devices';
    }
  }

  getSubtitle(): string {
    switch (this.activeType) {
      case ItemType.WIDGET: return 'iot-hub.subtitle-widgets';
      case ItemType.DASHBOARD: return 'iot-hub.subtitle-dashboards';
      case ItemType.CALCULATED_FIELD: return 'iot-hub.subtitle-calculated-fields';
      case ItemType.RULE_CHAIN: return 'iot-hub.subtitle-rule-chains';
      case ItemType.DEVICE: return 'iot-hub.subtitle-devices';
    }
  }

  getSearchPlaceholder(): string {
    switch (this.activeType) {
      case ItemType.WIDGET: return 'iot-hub.search-widgets';
      case ItemType.DASHBOARD: return 'iot-hub.search-dashboards';
      case ItemType.CALCULATED_FIELD: return 'iot-hub.search-calculated-fields';
      case ItemType.RULE_CHAIN: return 'iot-hub.search-rule-chains';
      case ItemType.DEVICE: return 'iot-hub.search-devices';
    }
  }

  isInstalled(item: MpItemVersionView): boolean {
    return this.installedItemsMap.has(item.itemId);
  }

  openItemDetail(item: MpItemVersionView): void {
    const installedInfo = this.installedItemsMap.get(item.itemId);
    const dialogRef = this.dialog.open(TbIotHubItemDetailDialogComponent, {
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      autoFocus: false,
      data: {
        item,
        iotHubApiService: this.iotHubApiService,
        installedItemInfo: installedInfo
      } as IotHubItemDetailDialogData
    });
    dialogRef.afterClosed().subscribe(result => {
      if (result === 'installed' || result === 'updated') {
        this.loadInstalledItemInfos();
      }
    });
  }

  installItem(item: MpItemVersionView): void {
    const dialogRef = this.dialog.open(TbIotHubInstallDialogComponent, {
      panelClass: ['tb-dialog'],
      data: {
        item,
        iotHubApiService: this.iotHubApiService
      } as IotHubInstallDialogData
    });
    dialogRef.afterClosed().subscribe(result => {
      if (result === 'installed') {
        this.loadInstalledItemInfos();
      }
    });
  }

  updateItem(item: MpItemVersionView): void {
    const installedInfo = this.installedItemsMap.get(item.itemId);
    if (!installedInfo) {
      return;
    }
    const dialogRef = this.dialog.open(TbIotHubUpdateDialogComponent, {
      panelClass: ['tb-dialog'],
      data: {
        itemId: item.itemId,
        itemName: item.name,
        itemType: item.type,
        version: item.version,
        versionId: item.id,
        iotHubApiService: this.iotHubApiService
      } as IotHubUpdateDialogData
    });
    dialogRef.afterClosed().subscribe(result => {
      if (result === 'updated') {
        this.loadInstalledItemInfos();
      }
    });
  }

  navigateToCreator(creatorId: string): void {
    this.router.navigate(['/iot-hub/creator', creatorId]);
  }

  navigateToInstalledItems(): void {
    this.router.navigate(['/iot-hub/installed']);
  }

  private initInstalledItemInfos(): void {
    const infos: IotHubInstalledItemInfo[] = this.route.snapshot.data['installedItemInfos'] || [];
    this.installedItemsMap.clear();
    infos.forEach(info => this.installedItemsMap.set(info.itemId, info));
  }

  private loadInstalledItemInfos(): void {
    this.iotHubApiService.getInstalledItemInfos({ignoreLoading: true}).subscribe(infos => {
      this.installedItemsMap.clear();
      infos.forEach(info => this.installedItemsMap.set(info.itemId, info));
    });
  }

  private updateCategories(): void {
    this.categories = getCategoriesForType(this.activeType);
  }

  loadItems(): void {
    this.isLoading = true;
    this.hasError = false;
    const sort = this.sortOptions[this.selectedSortIndex];
    const sortOrder: SortOrder = { property: sort.value, direction: sort.direction };
    const pageLink = new PageLink(this.pageSize, this.pageIndex, this.textSearch || null, sortOrder);
    const query = new MpItemVersionQuery(
      pageLink,
      this.activeType,
      undefined,
      this.creatorId || undefined,
      this.activeCategories.size > 0 ? Array.from(this.activeCategories) : undefined,
      this.activeUseCases.size > 0 ? Array.from(this.activeUseCases) : undefined,
      this.activeCfTypes.size > 0 ? Array.from(this.activeCfTypes) : undefined,
      this.activeWidgetTypes.size > 0 ? Array.from(this.activeWidgetTypes) : undefined,
      this.activeRuleChainTypes.size > 0 ? Array.from(this.activeRuleChainTypes) : undefined
    );
    this.iotHubApiService.getPublishedVersions(
      query,
      { ignoreLoading: true, ignoreErrors: true }
    ).subscribe({
      next: (data: PageData<MpItemVersionView>) => {
        this.items = data.data;
        this.totalElements = data.totalElements;
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
        this.hasError = true;
        this.items = [];
        this.totalElements = 0;
      }
    });
  }
}
