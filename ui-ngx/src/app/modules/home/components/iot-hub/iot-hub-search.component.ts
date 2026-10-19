// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, OnInit, OnDestroy, Input, Output, EventEmitter, ViewChild, ElementRef, NgZone } from '@angular/core';
import { Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { forkJoin, of, Subject, Subscription } from 'rxjs';
import { catchError, debounceTime, distinctUntilChanged, switchMap } from 'rxjs/operators';
import { PageLink } from '@shared/models/page/page-link';
import { SortOrder } from '@shared/models/page/sort-order';
import { MpItemVersionQuery, MpItemVersionView } from '@shared/models/iot-hub/iot-hub-version.models';
import { PageData } from '@shared/models/page/page-data';
import {
  CROSS_TYPE_ITEM_TYPES,
  FilterParamInfo,
  IOT_HUB_SORT_OPTIONS,
  ItemType,
  itemTypeTranslations
} from '@shared/models/iot-hub/iot-hub-item.models';
import { IotHubInstalledItem } from '@shared/models/iot-hub/iot-hub-installed-item.models';
import { IotHubApiService } from '@core/http/iot-hub-api.service';
import { IotHubActionsService } from './iot-hub-actions.service';
import { measureGridColumns } from './iot-hub-utils';

/** A page is whole rows of the grid, and never fewer than MIN_PAGE_SIZE cards. */
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
  /** Off on the creator profile, which is already scoped to one creator. */
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

  /** A hidden element carrying the real grid's classes, so the column count comes from the CSS. */
  @ViewChild('cardGridProbe', { static: true }) cardGridProbe!: ElementRef<HTMLElement>;
  cols = 5;
  pageSize = MIN_PAGE_SIZE;
  private resizeObserver?: ResizeObserver;
  private resizeSubject = new Subject<void>();
  private resizeSubscription?: Subscription;

  get pageSizeOptions(): number[] {
    const base = Math.max(MIN_PAGE_SIZE, this.cols * ROWS_PER_PAGE);
    return [base, base * 2, base * 4];
  }

  // Filter panel state. An empty set means "no filter".

  /** Narrow widths only: the facet panel is a block above the results, not a sidebar. */
  filtersOpen = false;
  readonly typeOptions: string[] = CROSS_TYPE_ITEM_TYPES;
  categoryOptions: string[] = [];
  useCaseOptions: string[] = [];
  activeTypes = new Set<string>();
  activeCategories = new Set<string>();
  activeUseCases = new Set<string>();

  readonly sortOptions = IOT_HUB_SORT_OPTIONS;
  selectedSortIndex = 0;

  installedWidgets: IotHubInstalledItem[] = [];
  installedSolutionTemplates: IotHubInstalledItem[] = [];
  installedDeviceCounts: Record<string, number> = {};
  installedCalcFieldCounts: Record<string, number> = {};
  installedAlarmRuleCounts: Record<string, number> = {};
  installedRuleChainCounts: Record<string, number> = {};

  private searchSubject = new Subject<string>();
  private searchSubscription: Subscription;
  /** Every reload goes through here, so a newer request cancels an older one still in flight. */
  private loadSubject = new Subject<void>();
  private loadSubscription: Subscription;

  constructor(
    private router: Router,
    private translate: TranslateService,
    private iotHubApiService: IotHubApiService,
    private iotHubActions: IotHubActionsService,
    private zone: NgZone
  ) {}

  ngOnInit(): void {
    this.measureCols();
    this.observeGridWidth();
    if (this.showFilters) {
      this.loadFilterInfo();
    }
    this.loadInstalledItems();
    this.loadSubscription = this.loadSubject.pipe(
      switchMap(() => this.fetchResults(this.searchText || '').pipe(
        catchError(() => of(null))
      ))
    ).subscribe(result => this.applyResult(result));
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
    this.loadSubscription?.unsubscribe();
    this.resizeSubscription?.unsubscribe();
    this.resizeObserver?.disconnect();
  }

  // Filters

  getTypeLabel = (key: string): string => {
    const translationKey = itemTypeTranslations.get(key as ItemType);
    return translationKey ? this.translate.instant(translationKey + '-plural') : key;
  };

  onTypeToggle(key: string): void {
    this.activeTypes = this.toggled(this.activeTypes, key);
    this.reloadFromFirstPage();
  }

  onCategoryToggle(key: string): void {
    this.activeCategories = this.toggled(this.activeCategories, key);
    this.reloadFromFirstPage();
  }

  onUseCaseToggle(key: string): void {
    this.activeUseCases = this.toggled(this.activeUseCases, key);
    this.reloadFromFirstPage();
  }

  get activeFilterCount(): number {
    return this.activeTypes.size + this.activeCategories.size + this.activeUseCases.size;
  }

  clearFilters(): void {
    if (this.activeFilterCount === 0) {
      return;
    }
    this.activeTypes = new Set<string>();
    this.activeCategories = new Set<string>();
    this.activeUseCases = new Set<string>();
    this.reloadFromFirstPage();
  }

  /** A new Set rather than a mutation, so the facet list's @Input sees the change. */
  private toggled(set: Set<string>, key: string): Set<string> {
    const next = new Set(set);
    if (!next.delete(key)) {
      next.add(key);
    }
    return next;
  }

  private reloadFromFirstPage(): void {
    this.pageIndex = 0;
    this.loadResults();
  }

  /**
   * filterInfo answers per item type, so the six answers are merged. Each request has its own
   * catchError: one type failing costs that type's options, not the panel.
   */
  private loadFilterInfo(): void {
    const config = { ignoreLoading: true, ignoreErrors: true };
    forkJoin(
      CROSS_TYPE_ITEM_TYPES.map(type =>
        this.iotHubApiService.getFilterInfo(type, config).pipe(catchError(() => of(null)))
      )
    ).subscribe({
      next: infos => {
        this.categoryOptions = this.mergeFacet(infos.map(i => i?.categories));
        this.useCaseOptions = this.mergeFacet(infos.map(i => i?.useCases));
      }
    });
  }

  /** Keys that have items in any of the per-type answers, sorted. */
  private mergeFacet(lists: (FilterParamInfo[] | undefined)[]): string[] {
    const keys = new Set<string>();
    for (const list of lists) {
      for (const option of list || []) {
        if (option.totalItems > 0) {
          keys.add(option.key);
        }
      }
    }
    return [...keys].sort((a, b) => a.localeCompare(b));
  }

  // Column count -> page size

  private observeGridWidth(): void {
    const el = this.cardGridProbe?.nativeElement;
    if (!el || typeof ResizeObserver === 'undefined') {
      return;
    }
    this.resizeSubscription = this.resizeSubject.pipe(debounceTime(150)).subscribe(() => {
      const before = this.pageSizeOptions[0];
      // Keep the same first item in view, not the same page number.
      const firstItem = this.pageIndex * this.pageSize;
      this.measureCols();
      if (this.pageSizeOptions[0] === before) {
        return;
      }
      this.pageSize = this.pageSizeOptions[0];
      this.pageIndex = Math.floor(firstItem / this.pageSize);
      this.loadResults();
    });
    // zone.js does not patch ResizeObserver; without zone.run the reload would not render.
    this.resizeObserver = new ResizeObserver(() => this.zone.run(() => this.resizeSubject.next()));
    this.resizeObserver.observe(el);
  }

  private measureCols(): void {
    const el = this.cardGridProbe?.nativeElement;
    if (!el) {
      return;
    }
    this.cols = measureGridColumns(el);
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
    // hasError stays as-is until a request succeeds.
    this.isLoading = true;
    this.loadSubject.next();
  }

  /** A null result is a failed request. */
  private applyResult(result: PageData<MpItemVersionView> | null): void {
    this.isLoading = false;
    if (result) {
      this.totalElements = result.totalElements;
      this.results = result.data;
      this.hasError = false;
    } else {
      this.hasError = true;
      this.results = [];
      this.totalElements = 0;
    }
  }

  private fetchResults(text: string) {
    const sort = this.sortOptions[this.selectedSortIndex];
    const sortOrder: SortOrder = { property: sort.value, direction: sort.direction };
    const pageLink = new PageLink(this.pageSize, this.pageIndex, text.trim() || null, sortOrder);
    const query = new MpItemVersionQuery(pageLink, {
      creatorId: this.creatorId || undefined,
      // No type selected means every type the platform surfaces (the Hub also holds dashboards).
      types: this.activeTypes.size ? [...this.activeTypes] : CROSS_TYPE_ITEM_TYPES,
      categories: this.activeCategories.size ? [...this.activeCategories] : undefined,
      useCases: this.activeUseCases.size ? [...this.activeUseCases] : undefined
    });
    return this.iotHubApiService.getPublishedVersions(query, { ignoreLoading: true, ignoreErrors: true });
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
