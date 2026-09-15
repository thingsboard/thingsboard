// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, OnInit, OnDestroy, Input, Output, EventEmitter, ViewChild, ElementRef } from '@angular/core';
import { Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { forkJoin, Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { PageLink } from '@shared/models/page/page-link';
import { Direction, SortOrder } from '@shared/models/page/sort-order';
import { MpItemVersionQuery, MpItemVersionView } from '@shared/models/iot-hub/iot-hub-version.models';
import { FilterParamInfo, ItemType, itemTypeTranslations } from '@shared/models/iot-hub/iot-hub-item.models';
import { IotHubInstalledItem } from '@shared/models/iot-hub/iot-hub-installed-item.models';
import { IotHubApiService } from '@core/http/iot-hub-api.service';
import { IotHubActionsService } from './iot-hub-actions.service';

interface SortOption {
  value: string;
  label: string;
  direction: Direction;
}

/** Sort property served by relevance ranking. With no text the backend substitutes the
 *  install count, so it is a safe default in both states. */
const RELEVANCE = 'relevance';

/**
 * Item types the Type facet offers, in the order every other cross-type surface uses (the hero
 * popup's sections, the site's). Fixed rather than sorted by count: the counts move with the
 * catalogue and a facet whose options reshuffle under the pointer is hard to use.
 */
const FACET_ITEM_TYPES: ItemType[] = [
  ItemType.DEVICE, ItemType.SOLUTION_TEMPLATE, ItemType.WIDGET,
  ItemType.CALCULATED_FIELD, ItemType.ALARM_RULE, ItemType.RULE_CHAIN
];

/**
 * Rows a page holds, as a multiple of the column count, so a page always ends on a complete row.
 * The floor keeps a one- or two-column phone layout from falling to three cards a page.
 */
const ROWS_PER_PAGE = 3;
const MIN_PAGE_SIZE = 12;

@Component({
  selector: 'tb-iot-hub-search',
  standalone: false,
  templateUrl: './iot-hub-search.component.html',
  styleUrls: ['./iot-hub-search.component.scss']
})
export class TbIotHubSearchComponent implements OnInit, OnDestroy {

  @Input() searchText = '';
  @Input() creatorId: string;
  @Input() showCreator = true;
  /**
   * Render the filter panel. The search page does; the creator profile does not - it is already
   * scoped to one creator, and the site's profile has no panel either.
   */
  @Input() showFilters = true;
  @Output() searchTextChange = new EventEmitter<string>();

  get searchPlaceholderKey(): string {
    return this.creatorId ? 'iot-hub.search-published-items' : 'iot-hub.search';
  }

  results: MpItemVersionView[] = [];
  totalElements = 0;
  isLoading = false;
  hasError = false;
  private retryTimer: any = null;

  pageIndex = 0;

  /**
   * Read from a hidden probe that carries the real grid's classes, not guessed from breakpoints:
   * the grid's column count is a CSS fact, and duplicating its media queries in TypeScript is a
   * second source of truth that drifts the first time someone edits the stylesheet.
   */
  @ViewChild('cardGridProbe', { static: true }) cardGridProbe!: ElementRef<HTMLElement>;
  cols = 5;
  pageSize = MIN_PAGE_SIZE;

  get pageSizeOptions(): number[] {
    const base = Math.max(MIN_PAGE_SIZE, this.cols * ROWS_PER_PAGE);
    return [base, base * 2, base * 4];
  }

  /** Filter panel state. Empty sets mean "no filter", which is what the query object expects. */
  /** Narrow widths only: the facet panel is a block above the results, not a sidebar. */
  filtersOpen = false;
  typeOptions: FilterParamInfo[] = [];
  categoryOptions: FilterParamInfo[] = [];
  useCaseOptions: FilterParamInfo[] = [];
  activeTypes = new Set<string>();
  activeCategories = new Set<string>();
  activeUseCases = new Set<string>();

  sortOptions: SortOption[] = [
    { value: RELEVANCE, label: 'iot-hub.sort-most-relevant', direction: Direction.DESC },
    { value: 'totalInstallCount', label: 'iot-hub.sort-most-installed', direction: Direction.DESC },
    { value: 'publishedTime', label: 'iot-hub.sort-newest', direction: Direction.DESC },
    { value: 'name', label: 'iot-hub.sort-name', direction: Direction.ASC }
  ];
  /**
   * Relevance is the default in BOTH states, which is why nothing here switches on whether the
   * search field has text. With text it ranks the answer; without it the backend substitutes the
   * install count, so a user who never opens this menu sees the order they always saw while
   * browsing and the best matches once they type.
   */
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
    this.measureCols();
    this.observeGridWidth();
    if (this.showFilters) {
      this.loadFilterInfo();
    }
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
    this.resizeSubscription?.unsubscribe();
    this.resizeObserver?.disconnect();
  }

  // Filters

  getTypeLabel = (key: string): string => {
    const translationKey = itemTypeTranslations.get(key as ItemType);
    return translationKey ? this.translate.instant(translationKey + '-plural') : key;
  };

  onTypeToggle(key: string): void {
    this.toggle(this.activeTypes, key);
  }

  onCategoryToggle(key: string): void {
    this.toggle(this.activeCategories, key);
  }

  onUseCaseToggle(key: string): void {
    this.toggle(this.activeUseCases, key);
  }

  get activeFilterCount(): number {
    return this.activeTypes.size + this.activeCategories.size + this.activeUseCases.size;
  }

  clearFilters(): void {
    if (this.activeFilterCount === 0) {
      return;
    }
    this.activeTypes.clear();
    this.activeCategories.clear();
    this.activeUseCases.clear();
    this.pageIndex = 0;
    this.loadResults();
  }

  private toggle(set: Set<string>, key: string): void {
    if (set.has(key)) {
      set.delete(key);
    } else {
      set.add(key);
    }
    this.pageIndex = 0;
    this.loadResults();
  }

  /**
   * There is no catalogue-wide filterInfo endpoint - it answers per item type - so the six
   * answers are merged here, the way the site merges the same six at build time. Counts are
   * deliberately not rendered: filterInfo takes no text query, so beside a searched result they
   * would be the whole catalogue's numbers pretending to describe this answer.
   */
  private loadFilterInfo(): void {
    const config = { ignoreLoading: true, ignoreErrors: true };
    forkJoin(
      FACET_ITEM_TYPES.map(type => this.iotHubApiService.getFilterInfo(type, config))
    ).subscribe({
      next: infos => {
        // Every item type is offered, not only the ones filterInfo reports facets for: the
        // endpoint describes a type's categories and vendors, never how many items it has, and
        // a type whose items carry no categories would vanish from its own facet.
        this.typeOptions = FACET_ITEM_TYPES.map(type => ({
          key: type as string,
          totalItems: 0,
          totalInstallCount: 0
        }));
        this.categoryOptions = this.mergeFacet(infos.map(i => i?.categories));
        this.useCaseOptions = this.mergeFacet(infos.map(i => i?.useCases));
      },
      // A facet panel that failed to load is an empty panel, never a broken page: the grid
      // beside it answers the query perfectly well without it.
      error: () => {}
    });
  }

  private mergeFacet(lists: (FilterParamInfo[] | undefined)[]): FilterParamInfo[] {
    const byKey = new Map<string, FilterParamInfo>();
    for (const list of lists) {
      for (const option of list || []) {
        const seen = byKey.get(option.key);
        if (seen) {
          seen.totalItems += option.totalItems;
          seen.totalInstallCount += option.totalInstallCount;
        } else {
          byKey.set(option.key, { ...option });
        }
      }
    }
    return [...byKey.values()]
      .filter(o => o.totalItems > 0)
      .sort((a, b) => a.key.localeCompare(b.key));
  }

  // Column count -> page size

  private resizeObserver?: ResizeObserver;
  private resizeSubject = new Subject<void>();
  private resizeSubscription?: Subscription;

  private observeGridWidth(): void {
    const el = this.cardGridProbe?.nativeElement;
    if (!el || typeof ResizeObserver === 'undefined') {
      return;
    }
    this.resizeSubscription = this.resizeSubject.pipe(debounceTime(150)).subscribe(() => {
      const before = this.pageSizeOptions[0];
      this.measureCols();
      if (this.pageSizeOptions[0] === before) {
        return;
      }
      // Keep the user roughly where they were in the list rather than on the same page number:
      // page 4 of 12-card pages is a different place from page 4 of 18-card pages.
      const firstItem = this.pageIndex * this.pageSize;
      this.pageSize = this.pageSizeOptions[0];
      this.pageIndex = Math.floor(firstItem / this.pageSize);
      this.loadResults();
    });
    this.resizeObserver = new ResizeObserver(() => this.resizeSubject.next());
    this.resizeObserver.observe(el);
  }

  private measureCols(): void {
    const el = this.cardGridProbe?.nativeElement;
    if (!el) {
      return;
    }
    // Force layout so grid-template-columns resolves to pixel tracks rather than `repeat(...)`.
    void el.offsetWidth;
    const tracks = getComputedStyle(el).gridTemplateColumns;
    if (!tracks || tracks === 'none') {
      this.cols = 1;
    } else if (tracks.startsWith('repeat(')) {
      const match = tracks.match(/^repeat\(\s*(\d+)\s*,/);
      this.cols = match ? parseInt(match[1], 10) : 1;
    } else {
      this.cols = Math.max(1, tracks.trim().split(/\s+/).filter(t => t.length > 0).length);
    }
    if (!this.pageSizeOptions.includes(this.pageSize)) {
      this.pageSize = this.pageSizeOptions[0];
    }
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
        this.results = [];
        this.totalElements = 0;
      }
    });
  }

  private fetchResults(text: string) {
    const sort = this.sortOptions[this.selectedSortIndex];
    const sortOrder: SortOrder = { property: sort.value, direction: sort.direction };
    const pageLink = new PageLink(this.pageSize, this.pageIndex, text.trim() || null, sortOrder);
    const query = new MpItemVersionQuery(pageLink, {
      creatorId: this.creatorId || undefined,
      // Empty sets are left off the query entirely - an empty array would narrow to nothing.
      types: this.activeTypes.size ? [...this.activeTypes] : undefined,
      categories: this.activeCategories.size ? [...this.activeCategories] : undefined,
      useCases: this.activeUseCases.size ? [...this.activeUseCases] : undefined
    });
    return this.iotHubApiService.getPublishedVersions(query, { ignoreLoading: true, ignoreErrors: true });
  }

  private applyResults(data: MpItemVersionView[], totalElements: number): void {
    this.totalElements = totalElements;
    this.results = data;
    this.isLoading = false;
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
