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

import { ChangeDetectorRef, Component, EventEmitter, Input, OnInit, Output, ViewEncapsulation } from '@angular/core';
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
import { debounceTime, distinctUntilChanged, map, skip } from 'rxjs/operators';
import { BehaviorSubject, combineLatest } from 'rxjs';
import { isObject } from '@core/utils';
import { PageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';
import { GridEntitiesFetchFunction, ScrollGridColumns } from '@shared/components/grid/scroll-grid-datasource';
import { ItemSizeStrategy } from '@shared/components/grid/scroll-grid.component';
import { coerceBoolean } from '@shared/decorators/coercion';

type selectWidgetMode = 'bundles' | 'allWidgets';

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
  encapsulation: ViewEncapsulation.None
})
export class DashboardWidgetSelectComponent implements OnInit {

  private searchSubject = new BehaviorSubject<string>('');
  private search$ = this.searchSubject.asObservable().pipe(
    debounceTime(150));

  private filterWidgetTypes$ = new BehaviorSubject<Array<widgetType>>(null);
  private deprecatedFilter$ = new BehaviorSubject<DeprecatedFilter>(DeprecatedFilter.ACTUAL);
  private selectWidgetMode$ = new BehaviorSubject<selectWidgetMode>('bundles');
  private widgetsBundle$ = new BehaviorSubject<WidgetsBundle>(null);

  widgetTypes = new Set<widgetType>();
  hasDeprecated = false;

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

  @Input()
  set filterWidgetTypes(widgetTypes: Array<widgetType>) {
    this.filterWidgetTypes$.next(widgetTypes);
  }

  get filterWidgetTypes(): Array<widgetType> {
    return this.filterWidgetTypes$.value;
  }

  @Input()
  set selectWidgetMode(mode: selectWidgetMode) {
    if (this.selectWidgetMode$.value !== mode) {
      if (mode === 'bundles' && this.widgetsBundle$.value === null) {
        this.widgetTypes.clear();
        this.hasDeprecated = false;
      } else {
        this.widgetTypes = new Set<widgetType>(Object.keys(widgetType).map(t => t as widgetType));
        this.hasDeprecated = true;
      }
      this.filterWidgetTypes$.next(null);
      this.deprecatedFilter$.next(DeprecatedFilter.ACTUAL);
      this.selectWidgetMode$.next(mode);
    }
  }

  get selectWidgetMode(): selectWidgetMode {
    return this.selectWidgetMode$.value;
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
      if (widgetBundle === null && this.selectWidgetMode$.value !== 'allWidgets') {
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

  @Output()
  widgetSelected: EventEmitter<WidgetInfo> = new EventEmitter<WidgetInfo>();

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
  allWidgetsFetchFunction: GridEntitiesFetchFunction<WidgetTypeInfo, WidgetsFilter>;
  widgetsFetchFunction: GridEntitiesFetchFunction<WidgetTypeInfo, BundleWidgetsFilter>;

  widgetsBundleFilter = '';
  allWidgetsFilter: WidgetsFilter = {search: '', filter: null, deprecatedFilter: DeprecatedFilter.ACTUAL};
  widgetsFilter: BundleWidgetsFilter = {search: '', filter: null, deprecatedFilter: DeprecatedFilter.ACTUAL, widgetsBundleId: null};

  constructor(private widgetsService: WidgetService,
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

    this.search$.pipe(
      distinctUntilChanged(),
      skip(1)
    ).subscribe(
      (search) => {
        this.widgetsBundleFilter = search;
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

  ngOnInit(): void {
  }

  onWidgetClicked($event: Event, widget: WidgetTypeInfo): void {
    this.widgetSelected.emit(this.toWidgetInfo(widget));
  }

  isSystem(item: WidgetsBundle): boolean {
    return item && item.tenantId.id === NULL_UUID;
  }

  selectBundle($event: Event, bundle: WidgetsBundle) {
    $event.preventDefault();
    this.widgetsBundle = bundle;
    if (bundle.title?.toLowerCase().includes(this.search.toLowerCase()) ||
      bundle.description?.toLowerCase().includes(this.search.toLowerCase())) {
      this.searchSubject.next('');
    }
  }

  isObject(value: any): boolean {
    return isObject(value);
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
