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

import { AfterViewInit, Component, ElementRef, EventEmitter, Input, NgZone, OnDestroy, OnInit, Output, ViewChild } from '@angular/core';
import { Subject } from 'rxjs';
import { debounceTime, takeUntil } from 'rxjs/operators';
import { PageLink } from '@shared/models/page/page-link';
import { Direction, SortOrder } from '@shared/models/page/sort-order';
import { PageData } from '@shared/models/page/page-data';
import { MpItemVersionQuery, MpItemVersionView } from '@shared/models/iot-hub/iot-hub-version.models';
import { ItemType, FilterParamInfo } from '@shared/models/iot-hub/iot-hub-item.models';
import { widgetTypeTranslations, cfTypeTranslations, ruleChainTypeTranslations } from '@shared/models/iot-hub/iot-hub-version.models';
import { IotHubInstalledItem, DeviceInstalledItemDescriptor } from '@shared/models/iot-hub/iot-hub-installed-item.models';
import { IotHubApiService } from '@core/http/iot-hub-api.service';
import { TranslateService } from '@ngx-translate/core';
import { ActivatedRoute, Router } from '@angular/router';
import { IotHubActionsService } from '@home/components/iot-hub/iot-hub-actions.service';
import { filterIotHubItemsBySearch, groupIotHubFilterItems, IotHubFilterGroup } from '@home/components/iot-hub/iot-hub-utils';

interface SortOption {
  value: string;
  label: string;
  direction: Direction;
}

@Component({
  selector: 'tb-iot-hub-browse',
  standalone: false,
  templateUrl: './iot-hub-browse.component.html',
  styleUrls: ['./iot-hub-browse.component.scss'],
  host: { '[class.embedded]': 'embedded' }
})
export class TbIotHubBrowseComponent implements OnInit, AfterViewInit, OnDestroy {

  readonly ItemType = ItemType;

  @Input() creatorId: string;
  @Input() embedded = false;
  @Input() hideTabs = false;
  @Input() mode: 'default' | 'add' = 'default';
  @Input() fixedSubType: string;
  @Output() addItem = new EventEmitter<MpItemVersionView>();
  @Input() set activeType(value: ItemType) {
    if (value && value !== this._activeType) {
      const wasInit = !!this._activeType;
      this._activeType = value;
      // When set after init (e.g., parent resolves config in its ngOnInit),
      // trigger type-specific loading since our ngOnInit already ran
      if (wasInit) {
        this.loadFilterInfo();
        if (value === ItemType.WIDGET) {
          this.loadInstalledWidgets();
        } else if (value === ItemType.SOLUTION_TEMPLATE) {
          this.loadInstalledSolutionTemplates();
        } else {
          this.loadInstalledItemCounts();
        }
      }
    }
  }
  get activeType(): ItemType {
    return this._activeType;
  }

  get isCompactType(): boolean {
    return this._activeType === ItemType.CALCULATED_FIELD || this._activeType === ItemType.RULE_CHAIN;
  }

  items: MpItemVersionView[] = [];
  totalElements = 0;
  pageSize = 12;
  pageIndex = 0;
  isLoading = false;
  hasError = false;
  filterDrawerOpened = false;

  textSearch = '';
  _activeType: ItemType = ItemType.WIDGET;

  @ViewChild('cardGridProbe', { static: true }) cardGridProbe!: ElementRef<HTMLElement>;
  probeCols = 4;

  get cols(): number {
    return this.isCompactType ? Math.max(1, this.probeCols - 1) : this.probeCols;
  }

  get pageSizeOptions(): number[] {
    return this.cols >= 5 ? [15, 30, 60] : [12, 24, 48];
  }

  private resizeObserver?: ResizeObserver;
  private resize$ = new Subject<void>();
  activeCategories = new Set<string>();
  activeUseCases = new Set<string>();
  activeCfTypes = new Set<string>();
  activeWidgetTypes = new Set<string>();
  activeRuleChainTypes = new Set<string>();
  activeConnectivity = new Set<string>();
  activeHardwareTypes = new Set<string>();
  activeVendors = new Set<string>();

  sortOptions: SortOption[] = [
    { value: 'totalInstallCount', label: 'iot-hub.sort-most-installed', direction: Direction.DESC },
    { value: 'publishedTime', label: 'iot-hub.sort-newest', direction: Direction.DESC },
    { value: 'name', label: 'iot-hub.sort-name', direction: Direction.ASC }
  ];

  typeTabs: { type: ItemType; label: string }[] = [
    { type: ItemType.WIDGET, label: 'item.type-widget-plural' },
    { type: ItemType.DASHBOARD, label: 'item.type-dashboard-plural' },
    { type: ItemType.SOLUTION_TEMPLATE, label: 'item.type-solution-template-plural' },
    { type: ItemType.CALCULATED_FIELD, label: 'item.type-calculated-field-plural' },
    { type: ItemType.RULE_CHAIN, label: 'item.type-rule-chain-plural' },
    { type: ItemType.DEVICE, label: 'item.type-device-plural' }
  ];
  selectedSortIndex = 0;

  subtypeOptions: FilterParamInfo[] = [];
  categoryOptions: FilterParamInfo[] = [];
  useCaseOptions: FilterParamInfo[] = [];
  vendorOptions: FilterParamInfo[] = [];
  hardwareTypeOptions: FilterParamInfo[] = [];
  connectivityGroups: { group: string; values: FilterParamInfo[] }[] = [];

  filterSearch: Record<string, string> = {};
  filterItemsHovered = false;

  installedWidgets: IotHubInstalledItem[] = null;
  installedSolutionTemplates: IotHubInstalledItem[] = null;
  installedItemCounts: Record<string, number> = {};

  private searchSubject = new Subject<string>();
  private destroy$ = new Subject<void>();

  constructor(
    private iotHubApiService: IotHubApiService,
    private iotHubActions: IotHubActionsService,
    private translate: TranslateService,
    private router: Router,
    private route: ActivatedRoute,
    private zone: NgZone
  ) {}

  ngOnInit(): void {
    this.searchSubject.pipe(
      debounceTime(300),
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.pageIndex = 0;
      this.loadItems();
    });
    const params = this.route.snapshot.queryParams;
    if (params['search']) {
      this.textSearch = params['search'];
    }
    if (this.fixedSubType) {
      const subtypes = this.getActiveSubtypes();
      if (subtypes) {
        subtypes.add(this.fixedSubType);
      }
    }
    this.loadFilterInfo();
    if (this.activeType === ItemType.WIDGET) {
      this.loadInstalledWidgets();
    } else if (this.activeType === ItemType.SOLUTION_TEMPLATE) {
      this.loadInstalledSolutionTemplates();
    } else {
      this.loadInstalledItemCounts();
    }
  }

  ngAfterViewInit(): void {
    this.probeCols = this.measureProbeCols();
    this.adjustPageSize();
    this.resize$.pipe(
      debounceTime(150),
      takeUntil(this.destroy$)
    ).subscribe(() => this.handleResize());
    this.resizeObserver = new ResizeObserver(() => {
      this.zone.run(() => this.resize$.next());
    });
    this.resizeObserver.observe(this.cardGridProbe.nativeElement);
    this.loadItems();
  }

  ngOnDestroy(): void {
    this.resizeObserver?.disconnect();
    this.destroy$.next();
    this.destroy$.complete();
  }

  private measureProbeCols(): number {
    const el = this.cardGridProbe?.nativeElement;
    if (!el) {
      return this.probeCols;
    }
    // Force layout so `grid-template-columns` resolves to pixel tracks, not `repeat(...)`.
    void el.offsetWidth;
    const tracks = getComputedStyle(el).gridTemplateColumns;
    if (!tracks || tracks === 'none') {
      return 1;
    }
    if (tracks.startsWith('repeat(')) {
      const match = tracks.match(/^repeat\(\s*(\d+)\s*,/);
      return match ? parseInt(match[1], 10) : 1;
    }
    return Math.max(1, tracks.trim().split(/\s+/).filter(t => t.length > 0).length);
  }

  private adjustPageSize(): void {
    const options = this.pageSizeOptions;
    if (options.includes(this.pageSize)) {
      return;
    }
    const oldPageSize = this.pageSize;
    const oldPageIndex = this.pageIndex;
    this.pageSize = options.reduce((best, cur) =>
      Math.abs(cur - oldPageSize) < Math.abs(best - oldPageSize) ? cur : best);
    this.pageIndex = Math.floor((oldPageIndex * oldPageSize) / this.pageSize);
  }

  private handleResize(): void {
    const newProbeCols = this.measureProbeCols();
    if (newProbeCols === this.probeCols) {
      return;
    }
    const oldOptions = this.pageSizeOptions;
    this.probeCols = newProbeCols;
    const newOptions = this.pageSizeOptions;
    if (oldOptions[0] === newOptions[0]) {
      return;
    }
    this.adjustPageSize();
    this.loadItems();
  }

  onSearchChange(value: string): void {
    this.textSearch = value;
    this.searchSubject.next(value);
  }

  onTypeTabChange(type: ItemType): void {
    this.activeType = type;
    this.clearActiveFilterSets();
    this.filterSearch = {};
    this.loadFilterInfo();
    this.pageIndex = 0;
    this.adjustPageSize();
    if (type === ItemType.WIDGET) {
      this.loadInstalledWidgets();
    } else if (type === ItemType.SOLUTION_TEMPLATE) {
      this.loadInstalledSolutionTemplates();
    } else {
      this.loadInstalledItemCounts();
    }
    this.loadItems();
  }

  isSubtypeActive(key: string): boolean {
    return this.getActiveSubtypes().has(key);
  }

  isCategoryActive(key: string): boolean {
    return this.activeCategories.has(key);
  }

  isUseCaseActive(key: string): boolean {
    return this.activeUseCases.has(key);
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

  getTotalPages(): number {
    return Math.ceil(this.totalElements / this.pageSize);
  }

  getPageNumbers(): number[] {
    const total = this.getTotalPages();
    const pages: number[] = [];
    const maxVisible = 5;
    let start = Math.max(0, this.pageIndex - Math.floor(maxVisible / 2));
    const end = Math.min(total, start + maxVisible);
    if (end - start < maxVisible) {
      start = Math.max(0, end - maxVisible);
    }
    for (let i = start; i < end; i++) {
      pages.push(i);
    }
    return pages;
  }

  goToPage(page: number): void {
    if (page >= 0 && page < this.getTotalPages()) {
      this.pageIndex = page;
      this.loadItems();
    }
  }

  onPageSizeChange(size: number): void {
    this.pageSize = size;
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

  onConnectivityToggle(value: string): void {
    if (this.activeConnectivity.has(value)) {
      this.activeConnectivity.delete(value);
    } else {
      this.activeConnectivity.add(value);
    }
    this.pageIndex = 0;
    this.loadItems();
  }

  isConnectivityActive(value: string): boolean {
    return this.activeConnectivity.has(value);
  }

  onHardwareTypeToggle(value: string): void {
    if (this.activeHardwareTypes.has(value)) {
      this.activeHardwareTypes.delete(value);
    } else {
      this.activeHardwareTypes.add(value);
    }
    this.pageIndex = 0;
    this.loadItems();
  }

  isHardwareTypeActive(value: string): boolean {
    return this.activeHardwareTypes.has(value);
  }

  onVendorToggle(vendor: string): void {
    if (this.activeVendors.has(vendor)) {
      this.activeVendors.delete(vendor);
    } else {
      this.activeVendors.add(vendor);
    }
    this.pageIndex = 0;
    this.loadItems();
  }

  isVendorActive(vendor: string): boolean {
    return this.activeVendors.has(vendor);
  }

  getActiveVendorsArray(): string[] {
    return Array.from(this.activeVendors);
  }

  getActiveConnectivityArray(): string[] {
    return Array.from(this.activeConnectivity);
  }

  getActiveHardwareTypesArray(): string[] {
    return Array.from(this.activeHardwareTypes);
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

  getActiveSubtypesArray(): string[] {
    if (this.fixedSubType) {
      return Array.from(this.getActiveSubtypes()).filter(s => s !== this.fixedSubType);
    }
    return Array.from(this.getActiveSubtypes());
  }

  getActiveCategoriesArray(): string[] {
    return Array.from(this.activeCategories);
  }

  getActiveUseCasesArray(): string[] {
    return Array.from(this.activeUseCases);
  }

  getFilteredItems(items: FilterParamInfo[], searchKey: string): FilterParamInfo[] {
    return filterIotHubItemsBySearch(items, this.filterSearch[searchKey]);
  }

  getGroupedFilterItems(items: FilterParamInfo[], searchKey: string): IotHubFilterGroup[] {
    return groupIotHubFilterItems(items, this.filterSearch[searchKey]);
  }

  getFilteredConnectivityGroups(searchKey: string): { group: string; values: FilterParamInfo[] }[] {
    const search = (this.filterSearch[searchKey] || '').toLowerCase();
    if (!search) { return this.connectivityGroups; }
    return this.connectivityGroups
      .map(g => ({ group: g.group, values: g.values.filter(v => v.key.toLowerCase().includes(search)) }))
      .filter(g => g.values.length > 0);
  }

  get allConnectivityCount(): number {
    return this.connectivityGroups.reduce((sum, g) => sum + g.values.length, 0);
  }

  getSubtypeLabel(key: string): string {
    let translationKey: string;
    switch (this.activeType) {
      case ItemType.WIDGET: translationKey = widgetTypeTranslations.get(key); break;
      case ItemType.CALCULATED_FIELD: translationKey = cfTypeTranslations.get(key); break;
      case ItemType.RULE_CHAIN: translationKey = ruleChainTypeTranslations.get(key); break;
      default: return key;
    }
    return translationKey ? this.translate.instant(translationKey) : key;
  }

  clearAllFilters(): void {
    this.clearActiveFilterSets();
    if (this.fixedSubType) {
      this.getActiveSubtypes()?.add(this.fixedSubType);
    }
    this.textSearch = '';
    this.filterSearch = {};
    this.loadFilterInfo();
    this.pageIndex = 0;
    this.loadItems();
  }

  private clearActiveFilterSets(): void {
    this.activeCategories.clear();
    this.activeUseCases.clear();
    this.activeCfTypes.clear();
    this.activeWidgetTypes.clear();
    this.activeRuleChainTypes.clear();
    this.activeConnectivity.clear();
    this.activeHardwareTypes.clear();
    this.activeVendors.clear();
  }

  get activeFilterCount(): number {
    const subtypeCount = this.fixedSubType ? this.getActiveSubtypesArray().length : (this.getActiveSubtypes()?.size || 0);
    return subtypeCount + this.activeCategories.size + this.activeUseCases.size +
           this.activeConnectivity.size + this.activeHardwareTypes.size + this.activeVendors.size;
  }

  hasActiveDropdownFilters(): boolean {
    const subtypeCount = this.fixedSubType ? this.getActiveSubtypesArray().length : (this.getActiveSubtypes()?.size || 0);
    return this.activeCategories.size > 0 ||
           this.activeUseCases.size > 0 || subtypeCount > 0 ||
           this.activeConnectivity.size > 0 || this.activeHardwareTypes.size > 0 ||
           this.activeVendors.size > 0;
  }

  hasActiveFilters(): boolean {
    return this.hasActiveDropdownFilters() || this.textSearch.length > 0;
  }

  getTitle(): string {
    switch (this.activeType) {
      case ItemType.WIDGET: return 'iot-hub.title-widgets';
      case ItemType.DASHBOARD: return 'iot-hub.title-dashboards';
      case ItemType.SOLUTION_TEMPLATE: return 'iot-hub.title-solution-templates';
      case ItemType.CALCULATED_FIELD: return 'iot-hub.title-calculated-fields';
      case ItemType.RULE_CHAIN: return 'iot-hub.title-rule-chains';
      case ItemType.DEVICE: return 'iot-hub.title-devices';
    }
  }

  getInstalledItem(item: MpItemVersionView): IotHubInstalledItem | undefined {
    if (this.activeType === ItemType.WIDGET && this.installedWidgets) {
      return this.installedWidgets.find(i => i.itemId === item.itemId);
    }
    if (this.activeType === ItemType.SOLUTION_TEMPLATE && this.installedSolutionTemplates) {
      return this.installedSolutionTemplates.find(i => i.itemId === item.itemId);
    }
    return undefined;
  }

  getInstalledItemsCount(item: MpItemVersionView): number {
    return this.installedItemCounts[item.itemId] || 0;
  }

  openItemDetail(item: MpItemVersionView): void {
    this.iotHubActions.openItemDetail(item, this.getInstalledItem(item), this.getInstalledItemsCount(item), this.mode).subscribe(result => {
      if (result?.action === 'add') {
        this.addItem.emit(result.item);
      } else if (result === 'installed' || result === 'updated' || result === 'deleted') {
        this.reloadInstalledItems();
      }
    });
  }

  onItemAdd(item: MpItemVersionView): void {
    this.addItem.emit(item);
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
    if (!installedItem) { return; }
    this.iotHubActions.updateItem(installedItem, item.version, item.id as string).subscribe(result => {
      if (result === 'updated') {
        this.reloadInstalledItems();
      }
    });
  }

  deleteInstalledItem(item: MpItemVersionView): void {
    const installedItem = this.getInstalledItem(item);
    if (!installedItem) { return; }
    this.iotHubActions.deleteItem(installedItem).subscribe(() => {
      this.reloadInstalledItems();
    });
  }

  openInstallGuide(item: MpItemVersionView): void {
    const installedItem = this.getInstalledItem(item);
    if (!installedItem) { return; }
    const descriptor = installedItem.descriptor as DeviceInstalledItemDescriptor;
    this.iotHubApiService.getVersionInfo(installedItem.itemVersionId, {ignoreLoading: true, ignoreErrors: true}).subscribe(versionView => {
      this.iotHubActions.reviewDevice(versionView, descriptor).subscribe();
    });
  }

  navigateToCreator(creatorId: string): void {
    void this.router.navigate(['/iot-hub/creator', creatorId]);
  }

  private loadInstalledWidgets(): void {
    if (this.installedWidgets !== null) {
      return;
    }
    const pageLink = new PageLink(10000, 0);
    this.iotHubApiService.getInstalledItems(pageLink, ItemType.WIDGET, undefined, {ignoreLoading: true}).subscribe({
      next: (data) => {
        this.installedWidgets = data.data;
      }
    });
  }

  private loadInstalledItemCounts(): void {
    this.iotHubApiService.getInstalledItemCounts(this.activeType, {ignoreLoading: true}).subscribe({
      next: (counts) => {
        this.installedItemCounts = counts;
      }
    });
  }

  private loadInstalledSolutionTemplates(): void {
    if (this.installedSolutionTemplates !== null) {
      return;
    }
    const pageLink = new PageLink(10000, 0);
    this.iotHubApiService.getInstalledItems(pageLink, ItemType.SOLUTION_TEMPLATE, undefined, {ignoreLoading: true}).subscribe({
      next: (data) => {
        this.installedSolutionTemplates = data.data;
      }
    });
  }

  private reloadInstalledItems(): void {
    const config = {ignoreLoading: true};
    const pageLink = new PageLink(10000, 0);
    if (this.activeType === ItemType.WIDGET) {
      this.iotHubApiService.getInstalledItems(pageLink, ItemType.WIDGET, undefined, config).subscribe(data => {
        this.installedWidgets = data.data;
      });
    } else if (this.activeType === ItemType.SOLUTION_TEMPLATE) {
      this.iotHubApiService.getInstalledItems(pageLink, ItemType.SOLUTION_TEMPLATE, undefined, config).subscribe(data => {
        this.installedSolutionTemplates = data.data;
      });
    } else {
      this.iotHubApiService.getInstalledItemCounts(this.activeType, config).subscribe(counts => {
        this.installedItemCounts = counts;
      });
    }
  }

  private loadFilterInfo(): void {
    const hasItems = (i: FilterParamInfo) => i.totalItems > 0;
    this.iotHubApiService.getFilterInfo(this.activeType, { ignoreLoading: true }).subscribe(info => {
      this.subtypeOptions = (info.types || []).filter(hasItems);
      this.categoryOptions = (info.categories || []).filter(hasItems);
      this.useCaseOptions = (info.useCases || []).filter(hasItems);
      this.vendorOptions = (info.vendors || []).filter(hasItems);
      this.hardwareTypeOptions = (info.hardwareTypes || []).filter(hasItems);
      if (info.connectivities) {
        this.connectivityGroups = Object.entries(info.connectivities)
          .map(([group, values]) => ({ group, values: values.filter(hasItems) }))
          .filter(g => g.values.length > 0);
      } else {
        this.connectivityGroups = [];
      }
    });
  }

  loadItems(): void {
    this.isLoading = true;
    this.hasError = false;
    const sort = this.sortOptions[this.selectedSortIndex];
    const sortOrder: SortOrder = { property: sort.value, direction: sort.direction };
    const pageLink = new PageLink(this.pageSize, this.pageIndex, this.textSearch || null, sortOrder);
    const query = new MpItemVersionQuery(pageLink, {
      type: this.activeType,
      creatorId: this.creatorId || undefined,
      categories: this.activeCategories.size > 0 ? Array.from(this.activeCategories) : undefined,
      useCases: this.activeUseCases.size > 0 ? Array.from(this.activeUseCases) : undefined,
      cfTypes: this.activeCfTypes.size > 0 ? Array.from(this.activeCfTypes) : undefined,
      widgetTypes: this.activeWidgetTypes.size > 0 ? Array.from(this.activeWidgetTypes) : undefined,
      ruleChainTypes: this.activeRuleChainTypes.size > 0 ? Array.from(this.activeRuleChainTypes) : undefined,
      hardwareTypes: this.activeHardwareTypes.size > 0 ? Array.from(this.activeHardwareTypes) : undefined,
      connectivity: this.activeConnectivity.size > 0 ? Array.from(this.activeConnectivity) : undefined,
      vendors: this.activeVendors.size > 0 ? Array.from(this.activeVendors) : undefined
    });
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
