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

import { Component, OnInit, OnDestroy, Input, Output, EventEmitter } from '@angular/core';
import { Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { forkJoin, Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { PageLink } from '@shared/models/page/page-link';
import { Direction, SortOrder } from '@shared/models/page/sort-order';
import { MpItemVersionQuery, MpItemVersionView } from '@shared/models/iot-hub/iot-hub-version.models';
import { ItemType, itemTypeTranslations } from '@shared/models/iot-hub/iot-hub-item.models';
import { IotHubInstalledItem } from '@shared/models/iot-hub/iot-hub-installed-item.models';
import { IotHubApiService } from '@core/http/iot-hub-api.service';
import { IotHubActionsService } from './iot-hub-actions.service';

interface SearchResultGroup {
  type: ItemType;
  items: MpItemVersionView[];
}

interface SortOption {
  value: string;
  label: string;
  direction: Direction;
}

const TYPE_ORDER: ItemType[] = [
  ItemType.DEVICE, ItemType.SOLUTION_TEMPLATE, ItemType.WIDGET,
  ItemType.CALCULATED_FIELD, ItemType.ALARM_RULE, ItemType.RULE_CHAIN
];

@Component({
  selector: 'tb-iot-hub-search',
  standalone: false,
  templateUrl: './iot-hub-search.component.html',
  styleUrls: ['./iot-hub-search.component.scss']
})
export class TbIotHubSearchComponent implements OnInit, OnDestroy {

  readonly ItemType = ItemType;

  @Input() searchText = '';
  @Input() creatorId: string;
  @Input() showCreator = true;
  @Output() searchTextChange = new EventEmitter<string>();

  get searchPlaceholderKey(): string {
    return this.creatorId ? 'iot-hub.search-published-items' : 'iot-hub.search';
  }

  resultGroups: SearchResultGroup[] = [];
  totalElements = 0;
  isLoading = false;
  hasError = false;
  private retryTimer: any = null;

  pageSize = 15;
  pageIndex = 0;
  pageSizeOptions = [15, 30, 60];

  sortOptions: SortOption[] = [
    { value: 'totalInstallCount', label: 'iot-hub.sort-most-installed', direction: Direction.DESC },
    { value: 'publishedTime', label: 'iot-hub.sort-newest', direction: Direction.DESC },
    { value: 'name', label: 'iot-hub.sort-name', direction: Direction.ASC }
  ];
  selectedSortIndex = 0;

  installedWidgets: IotHubInstalledItem[] = [];
  installedSolutionTemplates: IotHubInstalledItem[] = [];
  installedDeviceCounts: Record<string, number> = {};
  installedCalcFieldCounts: Record<string, number> = {};
  installedAlarmRuleCounts: Record<string, number> = {};
  installedRuleChainCounts: Record<string, number> = {};

  private searchSubject = new Subject<string>();
  private searchSubscription: Subscription;

  constructor(
    private router: Router,
    private translate: TranslateService,
    private iotHubApiService: IotHubApiService,
    private iotHubActions: IotHubActionsService
  ) {}

  ngOnInit(): void {
    this.loadInstalledItems();
    this.searchSubscription = this.searchSubject.pipe(
      debounceTime(300),
      distinctUntilChanged()
    ).subscribe(() => {
      this.pageIndex = 0;
      this.loadResults();
    });
    this.loadResults();
  }

  ngOnDestroy(): void {
    this.searchSubscription?.unsubscribe();
  }

  onSearchInput(): void {
    this.searchTextChange.emit(this.searchText);
    this.searchSubject.next(this.searchText || '');
  }

  clearSearch(): void {
    this.searchText = '';
    this.searchTextChange.emit(this.searchText);
    this.loadResults();
  }

  onSearchEnter(): void {
    this.loadResults();
  }

  onSortChange(index: number): void {
    this.selectedSortIndex = index;
    this.pageIndex = 0;
    this.loadResults();
  }

  // Pagination
  get totalPages(): number {
    return Math.ceil(this.totalElements / this.pageSize) || 0;
  }

  getPageNumbers(): number[] {
    const total = this.totalPages;
    if (total <= 5) {
      return Array.from({length: total}, (_, i) => i);
    }
    const pages: number[] = [];
    const start = Math.max(0, this.pageIndex - 2);
    const end = Math.min(total - 1, start + 4);
    if (end - start < 4) {
      const adjustedStart = Math.max(0, end - 4);
      for (let i = adjustedStart; i <= end; i++) {
        pages.push(i);
      }
    } else {
      for (let i = start; i <= end; i++) {
        pages.push(i);
      }
    }
    return pages;
  }

  goToPage(page: number): void {
    if (page >= 0 && page < this.totalPages) {
      this.pageIndex = page;
      this.loadResults();
    }
  }

  onPageSizeChange(size: number): void {
    this.pageSize = size;
    this.pageIndex = 0;
    this.loadResults();
  }

  // Type helpers
  isCompactType(type: ItemType): boolean {
    return type === ItemType.CALCULATED_FIELD
      || type === ItemType.ALARM_RULE
      || type === ItemType.RULE_CHAIN;
  }

  getTypeLabel(type: ItemType): string {
    const key = itemTypeTranslations.get(type);
    return key ? this.translate.instant(key + '-plural') : type;
  }

  getTypeRoute(type: ItemType): string {
    switch (type) {
      case ItemType.WIDGET: return 'widgets';
      case ItemType.SOLUTION_TEMPLATE: return 'solution-templates';
      case ItemType.CALCULATED_FIELD: return 'calculated-fields';
      case ItemType.ALARM_RULE: return 'alarm-rules';
      case ItemType.RULE_CHAIN: return 'rule-chains';
      case ItemType.DEVICE: return 'devices';
      default: return 'widgets';
    }
  }

  navigateToType(type: ItemType): void {
    const search = this.searchText?.trim() || undefined;
    void this.router.navigate(['/iot-hub', this.getTypeRoute(type)], { queryParams: { search } });
  }

  // Installed items
  getInstalledItem(item: MpItemVersionView): IotHubInstalledItem | undefined {
    switch (item.type) {
      case ItemType.WIDGET:
        return this.installedWidgets.find(i => i.itemId === item.itemId);
      case ItemType.SOLUTION_TEMPLATE:
        return this.installedSolutionTemplates.find(i => i.itemId === item.itemId);
      default:
        return undefined;
    }
  }

  getInstalledItemsCount(item: MpItemVersionView): number {
    switch (item.type) {
      case ItemType.DEVICE:
        return this.installedDeviceCounts[item.itemId] || 0;
      case ItemType.CALCULATED_FIELD:
        return this.installedCalcFieldCounts[item.itemId] || 0;
      case ItemType.ALARM_RULE:
        return this.installedAlarmRuleCounts[item.itemId] || 0;
      case ItemType.RULE_CHAIN:
        return this.installedRuleChainCounts[item.itemId] || 0;
      default:
        return 0;
    }
  }

  // Dialogs
  openItemDetail(item: MpItemVersionView): void {
    this.iotHubActions.openItemDetail(item, this.getInstalledItem(item), this.getInstalledItemsCount(item), undefined, this.showCreator).subscribe(result => {
      if (result === 'installed' || result === 'deleted' || result === 'updated') {
        this.reloadInstalledItems();
      }
    });
  }

  installItem(item: MpItemVersionView): void {
    this.iotHubActions.installItem(item).subscribe(result => {
      if (result === 'installed') {
        this.reloadInstalledItems();
      }
    });
  }

  updateItem(item: MpItemVersionView): void {
    const installedItem = this.getInstalledItem(item);
    this.iotHubActions.updateItem(installedItem, item.version, item.id as string).subscribe(result => {
      if (result === 'updated') {
        this.reloadInstalledItems();
      }
    });
  }

  deleteInstalledItem(item: MpItemVersionView): void {
    const installedItem = this.getInstalledItem(item);
    this.iotHubActions.deleteItem(installedItem).subscribe((deleted) => {
      if (deleted) {
        this.reloadInstalledItems();
      }
    });
  }

  navigateToCreator(creatorId: string): void {
    void this.router.navigate(['/iot-hub/creator', creatorId]);
  }

  retryLoadResults(): void {
    if (this.retryTimer != null) {
      clearTimeout(this.retryTimer);
    }
    this.isLoading = true;
    this.retryTimer = setTimeout(() => {
      this.retryTimer = null;
      this.loadResults();
    }, 350);
  }

  // Data loading
  private loadResults(): void {
    if (this.retryTimer != null) {
      clearTimeout(this.retryTimer);
      this.retryTimer = null;
    }
    this.isLoading = true;
    // hasError stays as-is until the request actually succeeds
    // (cleared in the `next` callback below).
    this.fetchResults(this.searchText || '').subscribe({
      next: result => {
        this.applyResults(result.data, result.totalElements);
        this.hasError = false;
      },
      error: () => {
        this.isLoading = false;
        this.hasError = true;
        this.resultGroups = [];
        this.totalElements = 0;
      }
    });
  }

  private fetchResults(text: string) {
    const sort = this.sortOptions[this.selectedSortIndex];
    const sortOrder: SortOrder = { property: sort.value, direction: sort.direction };
    const pageLink = new PageLink(this.pageSize, this.pageIndex, text.trim() || null, sortOrder);
    const query = new MpItemVersionQuery(pageLink, { creatorId: this.creatorId || undefined });
    return this.iotHubApiService.getPublishedVersions(query, { ignoreLoading: true, ignoreErrors: true });
  }

  private applyResults(data: MpItemVersionView[], totalElements: number): void {
    this.totalElements = totalElements;
    this.resultGroups = this.groupResults(data);
    this.isLoading = false;
  }

  private groupResults(items: MpItemVersionView[]): SearchResultGroup[] {
    const groupMap = new Map<ItemType, MpItemVersionView[]>();
    for (const item of items) {
      let list = groupMap.get(item.type);
      if (!list) {
        list = [];
        groupMap.set(item.type, list);
      }
      list.push(item);
    }
    return TYPE_ORDER
      .filter(type => groupMap.has(type))
      .map(type => ({ type, items: groupMap.get(type) }));
  }

  private loadInstalledItems(): void {
    const config = { ignoreLoading: true };
    const pageLink = new PageLink(10000, 0);
    forkJoin({
      widgets: this.iotHubApiService.getInstalledItems(pageLink, ItemType.WIDGET, undefined, config),
      solutionTemplates: this.iotHubApiService.getInstalledItems(pageLink, ItemType.SOLUTION_TEMPLATE, undefined, config),
      deviceCounts: this.iotHubApiService.getInstalledItemCounts(ItemType.DEVICE, config),
      calcFieldCounts: this.iotHubApiService.getInstalledItemCounts(ItemType.CALCULATED_FIELD, config),
      alarmRuleCounts: this.iotHubApiService.getInstalledItemCounts(ItemType.ALARM_RULE, config),
      ruleChainCounts: this.iotHubApiService.getInstalledItemCounts(ItemType.RULE_CHAIN, config)
    }).subscribe(results => {
      this.installedWidgets = results.widgets.data;
      this.installedSolutionTemplates = results.solutionTemplates.data;
      this.installedDeviceCounts = results.deviceCounts;
      this.installedCalcFieldCounts = results.calcFieldCounts;
      this.installedAlarmRuleCounts = results.alarmRuleCounts;
      this.installedRuleChainCounts = results.ruleChainCounts;
    });
  }

  private reloadInstalledItems(): void {
    this.loadInstalledItems();
  }
}
