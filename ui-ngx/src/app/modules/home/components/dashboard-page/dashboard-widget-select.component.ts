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

import { ChangeDetectorRef, Component, EventEmitter, Input, Output, TemplateRef, ViewChild, ViewEncapsulation } from '@angular/core';
import { WidgetsBundle } from '@shared/models/widgets-bundle.model';
import { IAliasController } from '@core/api/widget-api.models';
import { NULL_UUID } from '@shared/models/id/has-uuid';
import { WidgetService } from '@core/http/widget.service';
import {
  DeprecatedFilter,
  fullWidgetTypeFqn,
  WidgetInfo,
  widgetType,
  WidgetTypeInfo
} from '@shared/models/widget.models';
import { debounceTime, distinctUntilChanged, map, skip, switchMap } from 'rxjs/operators';
import { BehaviorSubject, combineLatest, forkJoin, of } from 'rxjs';
import { PageLink } from '@shared/models/page/page-link';
import { Direction, SortOrder } from '@shared/models/page/sort-order';
import { GridEntitiesFetchFunction, ScrollGridColumns } from '@shared/components/grid/scroll-grid-datasource';
import { ItemSizeStrategy } from '@shared/components/grid/scroll-grid.component';
import { coerceBoolean } from '@shared/decorators/coercion';
import { TranslateService } from '@ngx-translate/core';
import { MpItemVersionQuery, MpItemVersionView, widgetTypeTranslations } from '@shared/models/iot-hub/iot-hub-version.models';
import { ItemType, FilterParamInfo, WidgetCategory } from '@shared/models/iot-hub/iot-hub-item.models';
import { IotHubInstalledItem } from '@shared/models/iot-hub/iot-hub-installed-item.models';
import { IotHubApiService } from '@core/http/iot-hub-api.service';
import { IotHubActionsService } from '@home/components/iot-hub/iot-hub-actions.service';
import { filterIotHubItemsBySearch, groupIotHubFilterItems, IotHubFilterGroup, resolveIotHubItemImageUrl } from '@home/components/iot-hub/iot-hub-utils';

type selectWidgetMode = 'installed' | 'iotHub';
type installedSubMode = 'default' | 'allWidgets';
type iotHubSubMode = 'default' | 'allWidgets' | 'installed' | 'category';

interface WidgetSelectSortOption {
  value: string;
  label: string;
  direction: Direction;
}

const LOGICAL_ALL_WIDGETS = '__logical_all_widgets__';
const LOGICAL_INSTALLED_FROM_IOT_HUB = '__logical_installed_from_iot_hub__';

interface LogicalCard {
  __logical: typeof LOGICAL_ALL_WIDGETS | typeof LOGICAL_INSTALLED_FROM_IOT_HUB;
}

interface WidgetsFilter {
  search: string;
  filter: widgetType[];
  deprecatedFilter: DeprecatedFilter;
}

interface BundleWidgetsFilter extends WidgetsFilter {
  widgetsBundleId: string;
}

@Component({
    selector: 'tb-dashboard-widget-select',
    templateUrl: './dashboard-widget-select.component.html',
    styleUrls: ['./dashboard-widget-select.component.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class DashboardWidgetSelectComponent {

  private searchSubject = new BehaviorSubject<string>('');
  private search$ = this.searchSubject.asObservable().pipe(
    debounceTime(150));

  private filterWidgetTypes$ = new BehaviorSubject<Array<widgetType>>(null);
  private deprecatedFilter$ = new BehaviorSubject<DeprecatedFilter>(DeprecatedFilter.ACTUAL);
  private selectWidgetMode$ = new BehaviorSubject<selectWidgetMode>('installed');
  private installedSubMode$ = new BehaviorSubject<installedSubMode>('default');
  private iotHubSubMode$ = new BehaviorSubject<iotHubSubMode>('default');
  private widgetsBundle$ = new BehaviorSubject<WidgetsBundle>(null);

  widgetTypes = new Set<widgetType>();
  hasDeprecated = false;

  iotHubSelectedCategory: string = null;
  includeDeprecated = false;
  searchFocused = false;

  iotHubSortOptions: WidgetSelectSortOption[] = [
    { value: 'totalInstallCount', label: 'iot-hub.sort-most-installed', direction: Direction.DESC },
    { value: 'publishedTime', label: 'iot-hub.sort-newest', direction: Direction.DESC },
    { value: 'name', label: 'iot-hub.sort-name', direction: Direction.ASC }
  ];
  iotHubSelectedSortIndex = 0;

  @Input()
  aliasController: IAliasController;

  @Input()
  @coerceBoolean()
  scadaFirst = false;

  @Input()
  set search(search: string) {
    this.searchSubject.next(search);
  }

  get search(): string {
    return this.searchSubject.value;
  }

  set filterWidgetTypes(widgetTypes: Array<widgetType>) {
    this.filterWidgetTypes$.next(widgetTypes);
  }

  get filterWidgetTypes(): Array<widgetType> {
    return this.filterWidgetTypes$.value;
  }

  @Input()
  set selectWidgetMode(mode: selectWidgetMode) {
    if (this.selectWidgetMode$.value !== mode) {
      this.resetSubModes();
      this.filterWidgetTypes$.next(null);
      this.deprecatedFilter$.next(DeprecatedFilter.ACTUAL);
      this.includeDeprecated = false;
      this.selectWidgetMode$.next(mode);
      this.updateWidgetTypesForCurrentState();
      if (mode === 'iotHub') {
        this.loadInstalledWidgets();
        this.loadIotHubFilterInfo();
        this.loadWidgetCategories();
      } else {
        this.installedWidgetVersions = null;
        this.iotHubAppliedWidgetTypes.clear();
        this.iotHubAppliedCategories.clear();
        this.iotHubAppliedUseCases.clear();
        this.iotHubFilterCount = 0;
      }
    }
  }

  get selectWidgetMode(): selectWidgetMode {
    return this.selectWidgetMode$.value;
  }

  set installedSubMode(sub: installedSubMode) {
    if (this.installedSubMode$.value !== sub) {
      this.installedSubMode$.next(sub);
      this.filterWidgetTypes$.next(null);
      this.deprecatedFilter$.next(DeprecatedFilter.ACTUAL);
      this.includeDeprecated = false;
      this.updateWidgetTypesForCurrentState();
    }
  }

  get installedSubMode(): installedSubMode {
    return this.installedSubMode$.value;
  }

  set iotHubSubMode(sub: iotHubSubMode) {
    if (this.iotHubSubMode$.value !== sub) {
      this.iotHubSubMode$.next(sub);
      if (sub !== 'category') {
        this.iotHubSelectedCategory = null;
      }
      this.installedWidgetVersions = null;
      if (sub === 'default') {
        this.widgetCategoriesFilter = (this.searchSubject.value || '') + '|' + Date.now();
        this.cd.markForCheck();
      } else {
        this.reloadIotHubWidgets();
      }
    }
  }

  get iotHubSubMode(): iotHubSubMode {
    return this.iotHubSubMode$.value;
  }

  @Input()
  set deprecatedFilter(filter: DeprecatedFilter) {
    this.deprecatedFilter$.next(filter);
  }

  get deprecatedFilter(): DeprecatedFilter {
    return this.deprecatedFilter$.value;
  }

  set widgetsBundle(widgetBundle: WidgetsBundle) {
    if (this.widgetsBundle$.value !== widgetBundle) {
      if (widgetBundle === null) {
        this.widgetTypes.clear();
        this.hasDeprecated = false;
      } else {
        this.widgetTypes = new Set<widgetType>(Object.keys(widgetType).map(t => t as widgetType));
        this.hasDeprecated = true;
      }
      this.filterWidgetTypes$.next(null);
      this.deprecatedFilter$.next(DeprecatedFilter.ACTUAL);
      this.widgetsBundle$.next(widgetBundle);
    }
  }

  get widgetsBundle(): WidgetsBundle {
    return this.widgetsBundle$.value;
  }

  @ViewChild('iotHubFilterPanel', {static: true}) iotHubFilterPanel: TemplateRef<void>;

  @Output()
  widgetSelected: EventEmitter<WidgetInfo> = new EventEmitter<WidgetInfo>();

  @Output()
  closeClick: EventEmitter<void> = new EventEmitter<void>();

  @Output()
  importClick: EventEmitter<Event> = new EventEmitter<Event>();

  columns: ScrollGridColumns = {
    columns: 2,
    breakpoints: {
      'screen and (min-width: 2000px)': 5,
      'screen and (min-width: 1097px)': 4,
      'gt-sm': 3,
      'screen and (min-width: 721px)': 4,
      'screen and (min-width: 485px)': 3
    }
  };

  gridWidgetsItemSizeStrategy: ItemSizeStrategy = {
    defaultItemSize: 160,
    itemSizeFunction: itemWidth => (itemWidth - 24) * 0.8 + 76
  };

  widgetBundlesFetchFunction: GridEntitiesFetchFunction<WidgetsBundle, string>;
  installedDefaultFetchFunction: GridEntitiesFetchFunction<any, string>;
  allWidgetsFetchFunction: GridEntitiesFetchFunction<WidgetTypeInfo, WidgetsFilter>;
  widgetsFetchFunction: GridEntitiesFetchFunction<WidgetTypeInfo, BundleWidgetsFilter>;
  iotHubWidgetsFetchFunction: GridEntitiesFetchFunction<MpItemVersionView, string>;
  iotHubInstalledWidgetsFetchFunction: GridEntitiesFetchFunction<MpItemVersionView, string>;
  iotHubDefaultFetchFunction: GridEntitiesFetchFunction<any, string>;

  readonly LOGICAL_ALL_WIDGETS = LOGICAL_ALL_WIDGETS;
  readonly LOGICAL_INSTALLED_FROM_IOT_HUB = LOGICAL_INSTALLED_FROM_IOT_HUB;

  widgetsBundleFilter = '';
  allWidgetsFilter: WidgetsFilter = {search: '', filter: null, deprecatedFilter: DeprecatedFilter.ACTUAL};
  widgetsFilter: BundleWidgetsFilter = {search: '', filter: null, deprecatedFilter: DeprecatedFilter.ACTUAL, widgetsBundleId: null};
  iotHubWidgetsFilter = '';
  iotHubInstalledWidgetsFilter = '';
  widgetCategoriesFilter = '';

  private installedWidgets: IotHubInstalledItem[] = null;
  private installedWidgetVersions: MpItemVersionView[] = null;

  // IoT Hub filter model — applied state (used by fetch functions + panel UI)
  iotHubAppliedWidgetTypes = new Set<string>();
  iotHubAppliedCategories = new Set<string>();
  iotHubAppliedUseCases = new Set<string>();

  iotHubWidgetTypeOptions: FilterParamInfo[] = [];
  iotHubCategoryOptions: FilterParamInfo[] = [];
  iotHubUseCaseOptions: FilterParamInfo[] = [];
  iotHubFilterCount = 0;
  iotHubFilterSearch: Record<string, string> = {};
  iotHubFilterItemsHovered = false;

  constructor(private widgetsService: WidgetService,
              private iotHubApiService: IotHubApiService,
              private iotHubActions: IotHubActionsService,
              private translate: TranslateService,
              private cd: ChangeDetectorRef) {

    this.widgetBundlesFetchFunction = (pageSize, page, filter) => {
      const pageLink = new PageLink(pageSize, page, filter, {
        property: 'title',
        direction: Direction.ASC
      });
      return this.widgetsService.getWidgetBundles(pageLink, true, false, this.scadaFirst);
    };

    this.allWidgetsFetchFunction = (pageSize, page, filter) => {
      const pageLink = new PageLink(pageSize, page, filter.search, {
        property: 'name',
        direction: Direction.ASC
      });
      return this.widgetsService.getWidgetTypes(pageLink, false, true, this.scadaFirst,
        filter.deprecatedFilter, filter.filter);
    };

    this.widgetsFetchFunction = (pageSize, page, filter) => {
      const pageLink = new PageLink(pageSize, page, filter.search, {
        property: 'name',
        direction: Direction.ASC
      });
      return this.widgetsService.getBundleWidgetTypeInfos(pageLink, filter.widgetsBundleId,
        true, filter.deprecatedFilter, filter.filter);
    };

    this.iotHubWidgetsFetchFunction = (pageSize, page, filter) => {
      const search = typeof filter === 'string' ? filter.split('|')[0] : filter;
      const sort = this.iotHubSortOptions[this.iotHubSelectedSortIndex];
      const sortOrder: SortOrder = { property: sort.value, direction: sort.direction };
      const pageLink = new PageLink(pageSize, page, search || null, sortOrder);
      const effectiveCategories = this.iotHubSelectedCategory
        ? [this.iotHubSelectedCategory]
        : (this.iotHubAppliedCategories.size > 0 ? Array.from(this.iotHubAppliedCategories) : undefined);
      const query = new MpItemVersionQuery(pageLink, {
        type: ItemType.WIDGET,
        categories: effectiveCategories,
        useCases: this.iotHubAppliedUseCases.size > 0 ? Array.from(this.iotHubAppliedUseCases) : undefined,
        widgetTypes: this.iotHubAppliedWidgetTypes.size > 0 ? Array.from(this.iotHubAppliedWidgetTypes) : undefined,
        scadaFirst: this.scadaFirst ? true : undefined
      });
      return this.iotHubApiService.getPublishedVersions(query, { ignoreLoading: true });
    };

    this.iotHubInstalledWidgetsFetchFunction = (pageSize, page, filter) => {
      if (this.installedWidgetVersions === null) {
        return this.fetchInstalledWidgetVersions().pipe(
          map(versions => this.filterAndPaginateInstalledVersions(versions, pageSize, page, filter))
        );
      }
      return of(this.filterAndPaginateInstalledVersions(this.installedWidgetVersions, pageSize, page, filter));
    };

    this.installedDefaultFetchFunction = (pageSize, page, filter) => {
      return this.widgetBundlesFetchFunction(pageSize, page, filter).pipe(
        map(pd => this.prependLogicalCards(pd, page, [{ __logical: LOGICAL_ALL_WIDGETS }]))
      );
    };

    this.iotHubDefaultFetchFunction = (pageSize, page, filter) => {
      const search = typeof filter === 'string' ? filter.split('|')[0] : filter;
      return this.iotHubApiService.getWidgetCategories(search || undefined,
        this.scadaFirst ? true : undefined, { ignoreLoading: true }).pipe(
        map(categories => ({
          data: categories.slice(page * pageSize, page * pageSize + pageSize),
          totalPages: Math.ceil(categories.length / pageSize),
          totalElements: categories.length,
          hasNext: (page + 1) * pageSize < categories.length
        })),
        map(pd => this.prependLogicalCards(pd, page, [
          { __logical: LOGICAL_ALL_WIDGETS },
          { __logical: LOGICAL_INSTALLED_FROM_IOT_HUB }
        ]))
      );
    };

    this.search$.pipe(
      distinctUntilChanged(),
      skip(1)
    ).subscribe(
      (search) => {
        this.widgetsBundleFilter = search;
        if (this.selectWidgetMode$.value === 'iotHub') {
          if (this.iotHubSubMode$.value === 'installed') {
            this.iotHubInstalledWidgetsFilter = search;
          } else if (this.iotHubSubMode$.value === 'default') {
            this.widgetCategoriesFilter = search;
          } else {
            this.iotHubWidgetsFilter = search;
          }
        }
        this.cd.markForCheck();
      }
    );

    combineLatest({search: this.search$, filter: this.filterWidgetTypes$.asObservable(),
      deprecatedFilter: this.deprecatedFilter$.asObservable()}).pipe(
      distinctUntilChanged((oldValue, newValue) => JSON.stringify(oldValue) === JSON.stringify(newValue)),
      skip(1)
    ).subscribe(
      (filter) => {
        this.allWidgetsFilter = filter;
        this.cd.markForCheck();
      }
    );

    combineLatest({search: this.search$, widgetsBundleId: this.widgetsBundle$.pipe(map(wb => wb !== null ? wb.id.id : null)),
      filter: this.filterWidgetTypes$.asObservable(), deprecatedFilter: this.deprecatedFilter$.asObservable()}).pipe(
      distinctUntilChanged((oldValue, newValue) => JSON.stringify(oldValue) === JSON.stringify(newValue)),
      skip(1)
    ).subscribe(
      (filter) => {
        if (filter.widgetsBundleId) {
          this.widgetsFilter = filter;
          this.cd.markForCheck();
        }
      }
    );
  }

  private prependLogicalCards(pageData: any, page: number, cards: LogicalCard[]): any {
    if (page === 0 && cards.length) {
      return {
        ...pageData,
        data: [...cards, ...(pageData.data || [])],
        totalElements: (pageData.totalElements || 0) + cards.length
      };
    }
    return pageData;
  }

  private resetSubModes(): void {
    this.widgetsBundle$.next(null);
    this.installedSubMode$.next('default');
    this.iotHubSubMode$.next('default');
    this.iotHubSelectedCategory = null;
  }

  private updateWidgetTypesForCurrentState(): void {
    const mode = this.selectWidgetMode$.value;
    const showTypeFilter =
      (mode === 'installed' && (this.installedSubMode$.value === 'allWidgets' || this.widgetsBundle$.value !== null)) ||
      (mode === 'iotHub' && this.iotHubSubMode$.value !== 'default');
    if (showTypeFilter) {
      this.widgetTypes = new Set<widgetType>(Object.keys(widgetType).map(t => t as widgetType));
      this.hasDeprecated = mode === 'installed';
    } else {
      this.widgetTypes.clear();
      this.hasDeprecated = false;
    }
  }

  onWidgetClicked(widget: WidgetTypeInfo): void {
    this.widgetSelected.emit(this.toWidgetInfo(widget));
  }

  isSystem(item: WidgetsBundle): boolean {
    return item && item.tenantId.id === NULL_UUID;
  }

  selectBundle($event: Event, bundle: WidgetsBundle) {
    $event.preventDefault();
    this.widgetsBundle = bundle;
  }

  onAllWidgetsCardClicked($event: Event): void {
    $event.preventDefault();
    if (this.selectWidgetMode$.value === 'installed') {
      this.installedSubMode = 'allWidgets';
    } else {
      this.iotHubSubMode = 'allWidgets';
    }
  }

  onInstalledFromIotHubCardClicked($event: Event): void {
    $event.preventDefault();
    this.iotHubSubMode = 'installed';
  }

  onCategoryCardClicked($event: Event, category: WidgetCategory): void {
    $event.preventDefault();
    this.iotHubSelectedCategory = category.name;
    this.iotHubSubMode = 'category';
  }

  getCategoryImageUrl(category: WidgetCategory): string | null {
    if (!category?.image) {
      return null;
    }
    return this.iotHubApiService.resolveResourceUrl(category.image);
  }

  onIotHubWidgetClicked($event: Event, item: MpItemVersionView): void {
    $event.preventDefault();
    const installedItem = this.installedWidgets?.find(i => i.itemId === item.itemId);
    if (installedItem) {
      const widgetTypeId = installedItem.descriptor?.type === 'WIDGET' ? installedItem.descriptor.widgetTypeId?.id : null;
      if (widgetTypeId) {
        this.widgetsService.getWidgetTypeInfoById(widgetTypeId).subscribe(wt => {
          if (wt) {
            this.widgetSelected.emit({
              typeFullFqn: fullWidgetTypeFqn(wt),
              type: wt.widgetType,
              title: wt.name,
              image: wt.image,
              description: wt.description,
              deprecated: wt.deprecated
            });
          }
        });
      }
      return;
    }
    this.iotHubActions.openItemDetail(item, undefined, undefined, 'add').subscribe(result => {
      if (result?.action === 'add') {
        this.installAndAddWidget(result.item);
      }
    });
  }

  isIotHubWidgetInstalled(item: MpItemVersionView): boolean {
    return this.installedWidgets?.some(i => i.itemId === item.itemId) ?? false;
  }

  getIotHubItemImage(item: MpItemVersionView): string | null {
    return resolveIotHubItemImageUrl(item, this.iotHubApiService);
  }

  getIotHubWidgetTypeLabel(key: string): string {
    const translationKey = widgetTypeTranslations.get(key);
    return translationKey ? this.translate.instant(translationKey) : key;
  }

  getFilteredIotHubItems(items: FilterParamInfo[], searchKey: string): FilterParamInfo[] {
    return filterIotHubItemsBySearch(items, this.iotHubFilterSearch[searchKey]);
  }

  getGroupedIotHubFilterItems(items: FilterParamInfo[], searchKey: string): IotHubFilterGroup[] {
    return groupIotHubFilterItems(items, this.iotHubFilterSearch[searchKey]);
  }

  toggleIotHubWidgetType(key: string): void {
    if (this.iotHubAppliedWidgetTypes.has(key)) {
      this.iotHubAppliedWidgetTypes.delete(key);
    } else {
      this.iotHubAppliedWidgetTypes.add(key);
    }
    this.onIotHubFiltersChanged();
  }

  toggleIotHubCategory(key: string): void {
    if (this.iotHubAppliedCategories.has(key)) {
      this.iotHubAppliedCategories.delete(key);
    } else {
      this.iotHubAppliedCategories.add(key);
    }
    this.onIotHubFiltersChanged();
  }

  toggleIotHubUseCase(key: string): void {
    if (this.iotHubAppliedUseCases.has(key)) {
      this.iotHubAppliedUseCases.delete(key);
    } else {
      this.iotHubAppliedUseCases.add(key);
    }
    this.onIotHubFiltersChanged();
  }

  clearIotHubFilters(): void {
    this.iotHubAppliedWidgetTypes.clear();
    this.iotHubAppliedCategories.clear();
    this.iotHubAppliedUseCases.clear();
    this.iotHubFilterSearch = {};
    this.onIotHubFiltersChanged();
  }

  private onIotHubFiltersChanged(): void {
    this.iotHubFilterCount =
      this.iotHubAppliedWidgetTypes.size +
      (this.iotHubSelectedCategory ? 0 : this.iotHubAppliedCategories.size) +
      this.iotHubAppliedUseCases.size;
    this.reloadIotHubWidgets();
  }

  onIotHubSortChange(index: number): void {
    if (this.iotHubSelectedSortIndex !== index) {
      this.iotHubSelectedSortIndex = index;
      this.installedWidgetVersions = null;
      this.reloadIotHubWidgets();
    }
  }

  get totalFilterCount(): number {
    if (this.selectWidgetMode === 'installed') {
      return (this.filterWidgetTypes?.length ?? 0) + (this.includeDeprecated ? 1 : 0);
    }
    return this.iotHubFilterCount;
  }

  hasActiveFilters(): boolean {
    if (this.selectWidgetMode === 'installed') {
      return (this.filterWidgetTypes?.length ?? 0) > 0 || this.includeDeprecated;
    }
    return this.iotHubAppliedWidgetTypes.size > 0
      || this.iotHubAppliedCategories.size > 0
      || this.iotHubAppliedUseCases.size > 0;
  }

  clearAllFilters(): void {
    if (this.selectWidgetMode === 'installed') {
      this.filterWidgetTypes = null;
      this.onIncludeDeprecatedChange(false);
    } else {
      this.clearIotHubFilters();
    }
  }

  hasActiveSearchOrFilters(): boolean {
    return !!this.searchSubject.value?.trim() || this.hasActiveFilters();
  }

  clearSearchAndFilters(): void {
    this.searchSubject.next('');
    if (this.hasActiveFilters()) {
      this.clearAllFilters();
    }
  }

  navigateToIotHubAllWidgets(): void {
    this.selectWidgetMode = 'iotHub';
    this.iotHubSubMode = 'allWidgets';
  }

  get hideCategoriesFilterSection(): boolean {
    return this.selectWidgetMode === 'iotHub' && this.iotHubSubMode === 'category';
  }

  onIncludeDeprecatedChange(value: boolean): void {
    this.includeDeprecated = value;
    this.deprecatedFilter = value ? DeprecatedFilter.ALL : DeprecatedFilter.ACTUAL;
  }

  isFilterVisible(): boolean {
    if (this.selectWidgetMode === 'iotHub') {
      return this.iotHubSubMode !== 'default';
    }
    return this.installedSubMode === 'allWidgets' || this.widgetsBundle !== null;
  }

  showTopLevelWidgetModeToggle(): boolean {
    if (this.selectWidgetMode === 'installed') {
      return !this.widgetsBundle && this.installedSubMode === 'default';
    }
    return this.iotHubSubMode === 'default';
  }

  showBackButton(): boolean {
    if (this.selectWidgetMode === 'installed') {
      return !!this.widgetsBundle || this.installedSubMode === 'allWidgets';
    }
    return this.iotHubSubMode !== 'default';
  }

  getHeaderTitle(): string {
    if (this.selectWidgetMode === 'iotHub') {
      if (this.iotHubSubMode === 'category' && this.iotHubSelectedCategory) {
        return this.iotHubSelectedCategory;
      }
      if (this.iotHubSubMode === 'installed') {
        return this.translate.instant('iot-hub.installed-from-iot-hub');
      }
      if (this.iotHubSubMode === 'allWidgets') {
        return this.translate.instant('iot-hub.all-iot-hub-widgets');
      }
      return '';
    }
    if (this.widgetsBundle) {
      return this.widgetsBundle.title;
    }
    if (this.installedSubMode === 'allWidgets') {
      return this.translate.instant('widget.all-widgets');
    }
    return '';
  }

  getSearchPlaceholder(): string {
    if (this.selectWidgetMode === 'iotHub') {
      if (this.iotHubSubMode === 'default') {
        return this.translate.instant('iot-hub.search-categories');
      }
      return this.translate.instant('iot-hub.search-widgets');
    }
    if (this.installedSubMode === 'default' && !this.widgetsBundle) {
      return this.translate.instant('iot-hub.search-bundles');
    }
    return this.translate.instant('iot-hub.search-widgets');
  }

  onBackClick(): void {
    if (this.selectWidgetMode === 'installed') {
      if (this.widgetsBundle) {
        this.widgetsBundle = null;
      } else if (this.installedSubMode === 'allWidgets') {
        this.installedSubMode = 'default';
      }
    } else if (this.selectWidgetMode === 'iotHub') {
      if (this.iotHubSubMode !== 'default') {
        this.iotHubSubMode = 'default';
      }
    }
  }

  onClose(): void {
    this.closeClick.emit();
  }

  clearSearch($event: Event): void {
    $event.preventDefault();
    $event.stopPropagation();
    this.search = '';
  }

  onImport($event: Event): void {
    this.importClick.emit($event);
  }

  get widgetTypesList(): widgetType[] {
    return Array.from(this.widgetTypes.values());
  }

  getWidgetTypeLabel(type: widgetType): string {
    return this.translate.instant('widget.type-' + type);
  }

  isWidgetTypeChecked(type: widgetType): boolean {
    return !!this.filterWidgetTypes && this.filterWidgetTypes.includes(type);
  }

  toggleWidgetTypeFilter(type: widgetType): void {
    const current = this.filterWidgetTypes ?? [];
    const next = current.includes(type)
      ? current.filter(t => t !== type)
      : [...current, type];
    this.filterWidgetTypes = next.length ? next : null;
  }

  private reloadIotHubWidgets(): void {
    if (this.iotHubSubMode === 'installed') {
      this.installedWidgetVersions = null;
      this.iotHubInstalledWidgetsFilter = this.searchSubject.value + '|' + Date.now();
    } else {
      this.iotHubWidgetsFilter = this.searchSubject.value + '|' + Date.now();
    }
    this.cd.markForCheck();
  }

  private installAndAddWidget(item: MpItemVersionView): void {
    const versionId = item.id as string;
    this.iotHubApiService.installItemVersion(versionId, { ignoreLoading: true }).subscribe({
      next: (result) => {
        if (result.success && result.descriptor?.type === 'WIDGET') {
          this.loadInstalledWidgets();
          const widgetTypeId = result.descriptor.widgetTypeId?.id;
          if (widgetTypeId) {
            this.widgetsService.getWidgetTypeInfoById(widgetTypeId).subscribe(wt => {
              if (wt) {
                this.widgetSelected.emit(this.toWidgetInfo(wt));
              }
            });
          }
        }
      }
    });
  }

  private loadIotHubFilterInfo(): void {
    const hasItems = (i: FilterParamInfo) => i.totalItems > 0;
    this.iotHubApiService.getFilterInfo(ItemType.WIDGET, { ignoreLoading: true }).subscribe(info => {
      this.iotHubWidgetTypeOptions = (info.types || []).filter(hasItems);
      this.iotHubCategoryOptions = (info.categories || []).filter(hasItems);
      this.iotHubUseCaseOptions = (info.useCases || []).filter(hasItems);
    });
  }

  private loadWidgetCategories(): void {
    this.widgetCategoriesFilter = this.searchSubject.value + '|' + Date.now();
  }

  private loadInstalledWidgets(): void {
    if (this.installedWidgets === null) {
      this.installedWidgets = [];
    }
    const pageLink = new PageLink(10000, 0);
    this.iotHubApiService.getInstalledItems(pageLink, ItemType.WIDGET, undefined, { ignoreLoading: true }).subscribe(data => {
      this.installedWidgets = data.data;
    });
  }

  private fetchInstalledWidgetVersions() {
    const itemIds = (this.installedWidgets || []).map(i => i.itemId);
    if (itemIds.length === 0) {
      this.installedWidgetVersions = [];
      return of([]);
    }
    return this.iotHubApiService.getItemsPublishedVersions(itemIds, { ignoreLoading: true }).pipe(
      switchMap(infos => {
        if (infos.length === 0) {
          return of([]);
        }
        const versionRequests = infos.map(info =>
          this.iotHubApiService.getVersionInfo(info.publishedVersionId, { ignoreLoading: true })
        );
        return forkJoin(versionRequests);
      }),
      map(versions => {
        this.installedWidgetVersions = versions.sort((a, b) => b.totalInstallCount - a.totalInstallCount);
        return this.installedWidgetVersions;
      })
    );
  }

  private filterAndPaginateInstalledVersions(versions: MpItemVersionView[], pageSize: number, page: number, filter: string) {
    let filtered = versions;
    const search = typeof filter === 'string' ? filter.split('|')[0] : '';
    if (search) {
      filtered = filtered.filter(v => v.name.toLowerCase().includes(search.toLowerCase()));
    }
    if (this.iotHubAppliedWidgetTypes.size > 0) {
      filtered = filtered.filter(v => this.iotHubAppliedWidgetTypes.has(v.dataDescriptor?.widgetType));
    }
    if (this.iotHubAppliedCategories.size > 0) {
      filtered = filtered.filter(v => v.categories?.some(c => this.iotHubAppliedCategories.has(c)));
    }
    if (this.iotHubAppliedUseCases.size > 0) {
      filtered = filtered.filter(v => v.useCases?.some(u => this.iotHubAppliedUseCases.has(u)));
    }
    filtered = this.sortInstalledVersions(filtered);
    const start = page * pageSize;
    const data = filtered.slice(start, start + pageSize);
    return { data, totalPages: Math.ceil(filtered.length / pageSize), totalElements: filtered.length, hasNext: start + pageSize < filtered.length };
  }

  private sortInstalledVersions(versions: MpItemVersionView[]): MpItemVersionView[] {
    const sort = this.iotHubSortOptions[this.iotHubSelectedSortIndex];
    const sign = sort.direction === Direction.ASC ? 1 : -1;
    const copy = [...versions];
    copy.sort((a, b) => {
      if (this.scadaFirst) {
        const aScada = !!a.dataDescriptor?.scada;
        const bScada = !!b.dataDescriptor?.scada;
        if (aScada !== bScada) {
          return aScada ? -1 : 1;
        }
      }
      const av = (a as any)[sort.value];
      const bv = (b as any)[sort.value];
      if (av == null && bv == null) { return 0; }
      if (av == null) { return 1; }
      if (bv == null) { return -1; }
      if (typeof av === 'string' && typeof bv === 'string') {
        return sign * av.localeCompare(bv);
      }
      return sign * (av < bv ? -1 : av > bv ? 1 : 0);
    });
    return copy;
  }

  private toWidgetInfo(widgetTypeInfo: WidgetTypeInfo): WidgetInfo {
    return {
      typeFullFqn: fullWidgetTypeFqn(widgetTypeInfo),
      type: widgetTypeInfo.widgetType,
      title: widgetTypeInfo.name,
      image: widgetTypeInfo.image,
      description: widgetTypeInfo.description,
      deprecated: widgetTypeInfo.deprecated
    };
  }
}
