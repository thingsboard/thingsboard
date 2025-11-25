///
/// Copyright © 2016-2025 The Thingsboard Authors
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
  Injector,
  Input,
  NgZone,
  OnDestroy,
  OnInit,
  QueryList,
  StaticProvider,
  ViewChild,
  ViewChildren,
  ViewContainerRef
} from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { WidgetAction, WidgetContext } from '@home/models/widget-component.models';
import {
  DataKey,
  Datasource,
  DatasourceData,
  DatasourceType,
  WidgetActionDescriptor,
  WidgetConfig
} from '@shared/models/widget.models';
import { UtilsService } from '@core/services/utils.service';
import { TranslateService } from '@ngx-translate/core';
import {
  formattedDataFormDatasourceData,
  hashCode,
  isDefined,
  isDefinedAndNotNull,
  isObject,
  isUndefined
} from '@core/utils';
import cssjs from '@core/css/css';
import { PageLink } from '@shared/models/page/page-link';
import { Direction, SortOrder, sortOrderFromString } from '@shared/models/page/sort-order';
import { CollectionViewer, DataSource } from '@angular/cdk/collections';
import { BehaviorSubject, fromEvent, merge, Observable, of, Subject, Subscription } from 'rxjs';
import { emptyPageData, PageData } from '@shared/models/page/page-data';
import {
  catchError,
  debounceTime,
  distinctUntilChanged,
  map,
  skip,
  startWith,
  switchMap,
  takeUntil,
  tap
} from 'rxjs/operators';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import {
  CellContentInfo,
  CellStyleInfo,
  checkHasActions,
  constructTableCssString,
  DisplayColumn,
  getCellContentFunctionInfo,
  getCellStyleInfo,
  getColumnDefaultVisibility,
  getColumnSelectionAvailability,
  getRowStyleInfo,
  getTableCellButtonActions,
  isValidPageStepCount,
  isValidPageStepIncrement,
  noDataMessage,
  prepareTableCellButtonActions,
  RowStyleInfo,
  TableCellButtonActionDescriptor,
  TableWidgetDataKeySettings,
  TableWidgetSettings
} from '@home/components/widget/lib/table-widget.models';
import { Overlay, OverlayConfig, OverlayRef } from '@angular/cdk/overlay';
import { SubscriptionEntityInfo } from '@core/api/widget-api.models';
import { DatePipe } from '@angular/common';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { hidePageSizePixelValue } from '@shared/models/constants';
import {
  DISPLAY_COLUMNS_PANEL_DATA,
  DisplayColumnsPanelComponent
} from '@home/components/widget/lib/display-columns-panel.component';
import { ComponentPortal } from '@angular/cdk/portal';
import { FormBuilder } from '@angular/forms';
import { DEFAULT_OVERLAY_POSITIONS } from '@shared/models/overlay.models';
import { DateFormatSettings, ValueFormatProcessor } from '@shared/models/widget-settings.models';
import { entityFields } from '@shared/models/entity.models';

export interface TimeseriesTableWidgetSettings extends TableWidgetSettings {
  showTimestamp: boolean;
  showMilliseconds: boolean;
  hideEmptyLines: boolean;
  dateFormat: DateFormatSettings;
  sortOrder: SortOrder;
}

interface TimeseriesWidgetLatestDataKeySettings extends TableWidgetDataKeySettings {
  show: boolean;
  order: number;
}

interface TimeseriesRow {
  actionCellButtons?: TableCellButtonActionDescriptor[];
  hasActions?: boolean;
  [col: number]: any;
  formattedTs: string;
}

interface TimeseriesHeader {
  index: number;
  dataKey: DataKey;
  sortable: boolean;
  show: boolean;
  columnDefaultVisibility?: boolean;
  columnSelectionAvailability?: boolean;
  styleInfo: Observable<CellStyleInfo>;
  contentInfo: CellContentInfo;
  order?: number;
}

interface TimeseriesTableSource {
  keyStartIndex: number;
  keyEndIndex: number;
  latestKeyStartIndex: number;
  latestKeyEndIndex: number;
  datasource: Datasource;
  rawData: Array<DatasourceData>;
  latestRawData: Array<DatasourceData>;
  data: TimeseriesRow[];
  pageLink: PageLink;
  displayedColumns: string[];
  timeseriesDatasource: TimeseriesDatasource;
  header: TimeseriesHeader[];
  rowDataTemplate: {[key: string]: any};
  displayName: string;
}

@Component({
  selector: 'tb-timeseries-table-widget',
  templateUrl: './timeseries-table-widget.component.html',
  styleUrls: ['./timeseries-table-widget.component.scss', './table-widget.scss']
})
export class TimeseriesTableWidgetComponent extends PageComponent implements OnInit, AfterViewInit, OnDestroy {

  @Input()
  ctx: WidgetContext;

  @ViewChild('searchInput') searchInputField: ElementRef;
  @ViewChildren(MatPaginator) paginators: QueryList<MatPaginator>;
  @ViewChildren(MatSort) sorts: QueryList<MatSort>;

  textSearch = this.fb.control('', {nonNullable: true});

  public displayPagination = true;
  public enableStickyHeader = true;
  public enableStickyAction = true;
  public showCellActionsMenu = true;
  public pageSizeOptions = [];
  public textSearchMode = false;
  public hidePageSize = false;
  public sources: TimeseriesTableSource[];
  public sourceIndex: number;
  public noDataDisplayMessageText: string;
  public hasRowAction: boolean;
  private setCellButtonAction: boolean;

  private cellContentCache: Array<any> = [];
  private cellStyleCache: Array<any> = [];
  private rowStyleCache: Array<any> = [];

  private settings: TimeseriesTableWidgetSettings;
  private widgetConfig: WidgetConfig;
  private data: Array<DatasourceData>;
  private latestData: Array<DatasourceData>;
  private datasources: Array<Datasource>;

  private defaultPageSize;
  private defaultSortOrder = '-0';
  private hideEmptyLines = false;
  public showTimestamp = true;
  private useEntityLabel = false;
  private dateFormatFilter: string;

  private displayedColumns: Array<DisplayColumn[]> = [];

  private rowStylesInfo: Observable<RowStyleInfo>;

  private subscriptions: Subscription[] = [];
  private widgetTimewindowChanged$: Subscription;
  private widgetResize$: ResizeObserver;
  private destroy$ = new Subject<void>();

  private searchAction: WidgetAction = {
    name: 'action.search',
    show: true,
    icon: 'search',
    onAction: () => {
      this.enterFilterMode();
    }
  };

  private columnDisplayAction: WidgetAction = {
    name: 'entity.columns-to-display',
    show: true,
    icon: 'view_column',
    onAction: ($event) => {
      this.editColumnsToDisplay($event);
    }
  };

  constructor(protected store: Store<AppState>,
              private elementRef: ElementRef,
              private overlay: Overlay,
              private viewContainerRef: ViewContainerRef,
              private utils: UtilsService,
              private translate: TranslateService,
              private domSanitizer: DomSanitizer,
              private datePipe: DatePipe,
              private cd: ChangeDetectorRef,
              private fb: FormBuilder,
              private zone: NgZone) {
    super(store);
  }

  ngOnInit(): void {
    this.ctx.$scope.timeseriesTableWidget = this;
    this.settings = this.ctx.settings;
    this.widgetConfig = this.ctx.widgetConfig;
    this.data = this.ctx.data;
    this.latestData = this.ctx.latestData;
    this.datasources = this.ctx.datasources;
    this.initialize();
    this.ctx.updateWidgetParams();

    if (this.displayPagination) {
      this.widgetTimewindowChanged$ = this.ctx.defaultSubscription.widgetTimewindowChanged$.subscribe(
        () => {
          this.sources.forEach((source) => {
            if (this.displayPagination) {
              source.pageLink.page = 0;
            }
          });
        }
      );
      this.widgetResize$ = new ResizeObserver(() => {
        this.zone.run(() => {
          const showHidePageSize = this.elementRef.nativeElement.offsetWidth < hidePageSizePixelValue;
          if (showHidePageSize !== this.hidePageSize) {
            this.hidePageSize = showHidePageSize;
            this.cd.markForCheck();
          }
        });
      });
      this.widgetResize$.observe(this.elementRef.nativeElement);
    }
  }

  ngOnDestroy(): void {
    if (this.widgetTimewindowChanged$) {
      this.widgetTimewindowChanged$.unsubscribe();
      this.widgetTimewindowChanged$ = null;
    }
    if (this.widgetResize$) {
      this.widgetResize$.disconnect();
    }
  }

  ngAfterViewInit(): void {
    this.textSearch.valueChanges.pipe(
      debounceTime(150),
      startWith(''),
      distinctUntilChanged((a: string, b: string) => a.trim() === b.trim()),
      skip(1),
      takeUntil(this.destroy$)
    ).subscribe((textSearch) => {
      this.sources.forEach((source) => {
        source.pageLink.textSearch = textSearch.trim();
        if (this.displayPagination) {
          source.pageLink.page = 0;
        }
      });
      this.loadCurrentSourceRow();
      this.ctx.detectChanges();
    });

    this.sorts.changes.pipe(takeUntil(this.destroy$)).subscribe(() => {
      this.initSubscriptionsToSortAndPaginator();
    });

    this.initSubscriptionsToSortAndPaginator();
  }

  public onDataUpdated() {
    this.updateCurrentSourceData();
    this.clearCache();
    this.ctx.detectChanges();
  }

  public onLatestDataUpdated() {
    this.updateCurrentSourceLatestData();
    this.clearCache();
    this.ctx.detectChanges();
  }

  public onEditModeChanged() {
    if (this.textSearchMode) {
      this.ctx.hideTitlePanel = !this.ctx.isEdit;
      this.ctx.detectChanges(true);
    }
  }

  private initialize() {
    this.ctx.widgetActions = [this.searchAction, this.columnDisplayAction];

    this.setCellButtonAction = !!this.ctx.actionsApi.getActionDescriptors('actionCellButton').length;
    this.hasRowAction = !!this.ctx.actionsApi.getActionDescriptors('rowClick').length;

    this.searchAction.show = isDefined(this.settings.enableSearch) ? this.settings.enableSearch : true;
    this.columnDisplayAction.show = isDefined(this.settings.enableSelectColumnDisplay) ? this.settings.enableSelectColumnDisplay : true;
    this.displayPagination = isDefined(this.settings.displayPagination) ? this.settings.displayPagination : true;
    this.enableStickyHeader = isDefined(this.settings.enableStickyHeader) ? this.settings.enableStickyHeader : true;
    this.enableStickyAction = isDefined(this.settings.enableStickyAction) ? this.settings.enableStickyAction : true;
    this.showCellActionsMenu = isDefined(this.settings.showCellActionsMenu) ? this.settings.showCellActionsMenu : true;
    this.hideEmptyLines = isDefined(this.settings.hideEmptyLines) ? this.settings.hideEmptyLines : false;
    this.useEntityLabel = isDefined(this.widgetConfig.settings.useEntityLabel) ? this.widgetConfig.settings.useEntityLabel : false;
    this.showTimestamp = this.settings.showTimestamp !== false;
    // For backward compatibility
    if (isDefined(this.settings?.showMilliseconds) && this.settings?.showMilliseconds) {
      this.dateFormatFilter = 'yyyy-MM-dd HH:mm:ss.SSS';
    } else {
      this.dateFormatFilter = isDefined(this.settings.dateFormat?.format) ? this.settings.dateFormat?.format : 'yyyy-MM-dd HH:mm:ss';
    }

    this.rowStylesInfo = getRowStyleInfo(this.ctx, this.settings, 'rowData, ctx');

    const pageSize = this.settings.defaultPageSize;
    let pageStepIncrement = isValidPageStepIncrement(this.settings.pageStepIncrement) ? this.settings.pageStepIncrement : null;
    let pageStepCount = isValidPageStepCount(this.settings.pageStepCount) ? this.settings.pageStepCount : null;

    if (Number.isInteger(pageSize) && pageSize > 0) {
      this.defaultPageSize = pageSize;
    }

    if (!this.defaultPageSize) {
      this.defaultPageSize = pageStepIncrement ?? 10;
    }

    if (!isDefinedAndNotNull(pageStepIncrement) || !isDefinedAndNotNull(pageStepCount)) {
      pageStepIncrement = this.defaultPageSize;
      pageStepCount = 3;
    }

    for (let i = 1; i <= pageStepCount; i++) {
      this.pageSizeOptions.push(pageStepIncrement * i);
    }

    this.noDataDisplayMessageText =
      noDataMessage(this.widgetConfig.noDataDisplayMessage, 'widget.no-data-found', this.utils, this.translate);

    let cssString = constructTableCssString(this.widgetConfig);

    const origBackgroundColor = this.widgetConfig.backgroundColor || 'rgb(255, 255, 255)';
    cssString += '.tb-table-widget mat-toolbar.mat-mdc-table-toolbar:not([color=primary]) {\n' +
    'background-color: ' + origBackgroundColor + ' !important;\n' +
    '}\n';

    const cssParser = new cssjs();
    cssParser.testMode = false;
    const namespace = 'ts-table-' + hashCode(cssString);
    cssParser.cssPreviewNamespace = namespace;
    cssParser.createStyleElement(namespace, cssString);
    $(this.elementRef.nativeElement).addClass(namespace);
    this.updateDatasources();
  }

  private getTabLabel(source: Datasource):string {
    const value = this.useEntityLabel
      ? (source.entityLabel || source.entityName)
      : source.entityName;

    return this.utils.customTranslation(value);
  }

  private sortDatasources(source: TimeseriesTableSource[]) {
    const property = this.settings?.sortOrder?.property;
    const direction = this.settings?.sortOrder?.direction;
    const isAsc = direction === Direction.ASC;

    if (property === entityFields.name.keyName) {
      const collator = new Intl.Collator(undefined, {
        sensitivity: "variant",
        numeric: true,
        ignorePunctuation: false
      });

      source.sort((a, b) => {
        const valueA = a.displayName || '';
        const valueB = b.displayName || '';

        return isAsc
          ? collator.compare(valueA, valueB)
          : collator.compare(valueB, valueA);
      });
    } else if (property === entityFields.createdTime.keyName) {
      if (isAsc) {
        source.reverse();
      }
    }
  }

  private updateDatasources() {
    this.sources = [];
    this.sourceIndex = 0;
    let keyOffset = 0;
    let latestKeyOffset = 0;
    const pageSize = this.displayPagination ? this.defaultPageSize : Number.POSITIVE_INFINITY;
    if (this.datasources) {
      for (const datasource of this.datasources) {
        const sortOrder: SortOrder = sortOrderFromString(this.defaultSortOrder);
        const source = {} as TimeseriesTableSource;
        source.header = this.prepareHeader(datasource);
        source.keyStartIndex = keyOffset;
        keyOffset += datasource.dataKeys.length;
        source.keyEndIndex = keyOffset;
        source.latestKeyStartIndex = latestKeyOffset;
        latestKeyOffset += datasource.latestDataKeys ? datasource.latestDataKeys.length : 0;
        source.latestKeyEndIndex = latestKeyOffset;
        source.datasource = datasource;
        source.data = [];
        source.rawData = [];
        source.displayedColumns = [];
        source.pageLink = new PageLink(pageSize, 0, null, sortOrder);
        source.rowDataTemplate = {};
        source.rowDataTemplate.Timestamp = null;
        source.displayName = this.getTabLabel(datasource);
        if (this.showTimestamp) {
          source.displayedColumns.push('0');
        }
        source.header.forEach(header => {
          const dataKey = header.dataKey;
          if (header.show) {
            source.displayedColumns.push(header.index + '');
          }
          source.rowDataTemplate[dataKey.label] = null;
        });
        if (this.setCellButtonAction) {
          source.displayedColumns.push('actions');
        }
        const tsDatasource = new TimeseriesDatasource(source, this.hideEmptyLines, this.dateFormatFilter, this.datePipe, this.ctx);
        tsDatasource.allDataUpdated(this.data, this.latestData);
        this.sources.push(source);
      }
    }
    if (this.sources.length) {
      this.sortDatasources(this.sources);
      this.sources.forEach((source, index) => {
        this.prepareDisplayedColumn(index);
        source.displayedColumns = this.displayedColumns[index].filter(value => value.display).map(value => value.def);
      });
    }
    this.updateActiveEntityInfo();
  }

  private editColumnsToDisplay($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    if (this.sources.length) {
      const target = $event.target || $event.currentTarget;
      const config = new OverlayConfig({
        panelClass: 'tb-panel-container',
        backdropClass: 'cdk-overlay-transparent-backdrop',
        hasBackdrop: true,
        height: 'fit-content',
        maxHeight: '75vh'
      });
      config.positionStrategy = this.overlay.position()
        .flexibleConnectedTo(target as HTMLElement)
        .withPositions(DEFAULT_OVERLAY_POSITIONS);

      const overlayRef = this.overlay.create(config);
      overlayRef.backdropClick().subscribe(() => {
        overlayRef.dispose();
      });

      const source = this.sources[this.sourceIndex];

      const providers: StaticProvider[] = [
        {
          provide: DISPLAY_COLUMNS_PANEL_DATA,
          useValue: {
            columns: this.displayedColumns[this.sourceIndex],
            columnsUpdated: (newColumns) => {
              source.displayedColumns = newColumns.filter(value => value.display).map(value => value.def);
              this.clearCache();
            }
          }
        },
        {
          provide: OverlayRef,
          useValue: overlayRef
        }
      ];

      const injector = Injector.create({parent: this.viewContainerRef.injector, providers});
      const componentRef = overlayRef.attach(new ComponentPortal(DisplayColumnsPanelComponent,
        this.viewContainerRef, injector));

      const resizeWindows$ = fromEvent(window, 'resize').subscribe(() => {
        overlayRef.updatePosition();
      });
      componentRef.onDestroy(() => {
        resizeWindows$.unsubscribe();
      });

      this.ctx.detectChanges();
    }
  }

  private prepareDisplayedColumn(index: number) {
    if (!this.displayedColumns[index]) {
      this.displayedColumns[index] = this.sources[index].displayedColumns.map(value => {
        let title = '';
        const header = this.sources[index].header.find(column => column.index.toString() === value);
        if (value === '0') {
          title = this.translate.instant('widgets.table.timestamp-column-name');
        } else if (value === 'actions') {
          title = 'Actions';
        } else {
          title = header.dataKey.label;
        }
        return {
          title,
          def: value,
          display: header?.columnDefaultVisibility ?? true,
          selectable: header?.columnSelectionAvailability ?? true
        };
      });
    }
  }

  private prepareHeader(datasource: Datasource): TimeseriesHeader[] {
    const dataKeys = datasource.dataKeys;
    const latestDataKeys = datasource.latestDataKeys;
    let header: TimeseriesHeader[] = [];
    dataKeys.forEach((dataKey, index) => {
      const keySettings: TableWidgetDataKeySettings = dataKey.settings;
      const sortable = !keySettings.disableSorting && !dataKey.usePostProcessing;
      const styleInfo = getCellStyleInfo(this.ctx, keySettings, 'value, rowData, ctx');
      const contentFunctionInfo = getCellContentFunctionInfo(this.ctx, keySettings, 'value, rowData, ctx');
      const columnDefaultVisibility = getColumnDefaultVisibility(keySettings, this.ctx);
      const columnSelectionAvailability = getColumnSelectionAvailability(keySettings);
      const decimals = (dataKey.decimals || dataKey.decimals === 0) ? dataKey.decimals : this.ctx.widgetConfig.decimals;
      const units = dataKey.units || this.ctx.widgetConfig.units;
      const valueFormat = ValueFormatProcessor.fromSettings(this.ctx.$injector, {units, decimals, showZeroDecimals: true});
      const contentInfo: CellContentInfo = {
        contentFunction: contentFunctionInfo,
        valueFormat
      };
      header.push({
        index: index + 1,
        dataKey,
        sortable,
        styleInfo,
        contentInfo,
        show: true,
        columnDefaultVisibility,
        columnSelectionAvailability,
        order: index + 2
      });
    });
    if (latestDataKeys) {
      latestDataKeys.forEach((dataKey, latestIndex) => {
        const index = dataKeys.length + latestIndex;
        const keySettings: TimeseriesWidgetLatestDataKeySettings = dataKey.settings;
        const sortable = !keySettings.disableSorting && !dataKey.usePostProcessing;
        const styleInfo = getCellStyleInfo(this.ctx, keySettings, 'value, rowData, ctx');
        const contentFunctionInfo = getCellContentFunctionInfo(this.ctx, keySettings, 'value, rowData, ctx');
        const columnDefaultVisibility = getColumnDefaultVisibility(keySettings, this.ctx);
        const columnSelectionAvailability = getColumnSelectionAvailability(keySettings);
        const decimals = (dataKey.decimals || dataKey.decimals === 0) ? dataKey.decimals : this.ctx.widgetConfig.decimals;
        const units = dataKey.units || this.ctx.widgetConfig.units;
        const valueFormat = ValueFormatProcessor.fromSettings(this.ctx.$injector, {units, decimals, showZeroDecimals: true});
        const contentInfo: CellContentInfo = {
          contentFunction: contentFunctionInfo,
          valueFormat
        };
        header.push({
          index: index + 1,
          dataKey,
          sortable,
          styleInfo,
          contentInfo,
          show: isDefinedAndNotNull(keySettings.show) ? keySettings.show : true,
          columnDefaultVisibility,
          columnSelectionAvailability,
          order: isDefinedAndNotNull(keySettings.order) ? keySettings.order : (index + 2)
        });
      });
    }
    header = header.sort((a, b) => a.order - b.order);
    return header;
  }

  private updateActiveEntityInfo() {
    const source = this.sources[this.sourceIndex];
    let activeEntityInfo: SubscriptionEntityInfo = null;
    if (source) {
      const datasource = source.datasource;
      if (datasource.type === DatasourceType.entity &&
        datasource.entityType && datasource.entityId) {
        activeEntityInfo = {
          entityId: {
            entityType: datasource.entityType,
            id: datasource.entityId
          },
          entityName: datasource.entityName,
          entityLabel: datasource.entityLabel,
          entityDescription: datasource.entityDescription
        };
      }
    }
    this.ctx.activeEntityInfo = activeEntityInfo;
  }

  private initSubscriptionsToSortAndPaginator() {
    this.subscriptions.forEach(subscription => subscription.unsubscribe());
    this.sorts.forEach((sort, index) => {
      let paginator = null;
      const observables = [sort.sortChange];
      if (this.displayPagination) {
        paginator = this.paginators.toArray()[index];
        this.subscriptions.push(
          sort.sortChange.subscribe(() => paginator.pageIndex = 0)
        );
        observables.push(paginator.page);
      }
      this.updateData(sort, paginator);
      this.subscriptions.push(merge(...observables).subscribe(() => this.updateData(sort, paginator)));
    });
  }

  onSourceIndexChanged() {
    this.updateCurrentSourceAllData();
    this.updateActiveEntityInfo();
    this.clearCache();
  }

  private enterFilterMode() {
    this.textSearchMode = true;
    this.ctx.hideTitlePanel = true;
    this.ctx.detectChanges(true);
    setTimeout(() => {
      this.searchInputField.nativeElement.focus();
      this.searchInputField.nativeElement.setSelectionRange(0, 0);
    }, 10);
  }

  exitFilterMode() {
    this.textSearchMode = false;
    this.textSearch.reset();
    this.loadCurrentSourceRow();
    this.ctx.hideTitlePanel = false;
    this.ctx.detectChanges(true);
  }

  private updateData(sort: MatSort, paginator: MatPaginator) {
    const source = this.sources[this.sourceIndex];
    if (this.displayPagination) {
      source.pageLink.page = paginator.pageIndex;
      source.pageLink.pageSize = paginator.pageSize;
    } else {
      source.pageLink.page = 0;
    }
    source.pageLink.sortOrder.property = sort.active;
    source.pageLink.sortOrder.direction = Direction[sort.direction.toUpperCase()];
    source.timeseriesDatasource.loadRows();
    this.clearCache();
    this.ctx.detectChanges();
  }

  public trackByColumnIndex(index, header: TimeseriesHeader) {
    return header.index;
  }

  public trackByRowTimestamp(index: number) {
    return index;
  }

  public trackByActionCellDescriptionId(index: number, action: WidgetActionDescriptor) {
    return action.id;
  }

  public trackBySourcesIndex(index: number, source: TimeseriesTableSource) {
    return source.datasource.entityId;
  }

  public rowStyle(source: TimeseriesTableSource, row: TimeseriesRow, index: number): Observable<any> {
    let style$: Observable<any>;
    const res = this.rowStyleCache[index];
    if (!res) {
      style$ = this.rowStylesInfo.pipe(
        map(styleInfo => {
          if (styleInfo.useRowStyleFunction && styleInfo.rowStyleFunction) {
            const rowData = source.rowDataTemplate;
            rowData.Timestamp = row[0];
            source.header.forEach((headerInfo) => {
              rowData[headerInfo.dataKey.label] = row[headerInfo.index];
            });
            const style = styleInfo.rowStyleFunction.execute(rowData, this.ctx);
            if (!isObject(style)) {
              throw new TypeError(`${style === null ? 'null' : typeof style} instead of style object`);
            }
            if (Array.isArray(style)) {
              throw new TypeError(`Array instead of style object`);
            }
            return style;
          } else {
            return {};
          }
        }),
        catchError(e => {
          console.warn(`Row style function in widget ` +
            `'${this.ctx.widgetConfig.title}' returns '${e}'. Please check your row style function.`);
          return of({});
        })
      );
      style$ = style$.pipe(
        tap((style) => {
          this.rowStyleCache[index] = style;
        })
      );
    } else {
      style$ = of(res);
    }
    return style$;
  }

  public cellStyle(source: TimeseriesTableSource, header: TimeseriesHeader,
                   index: number, row: TimeseriesRow, value: any, rowIndex: number): Observable<any> {
    let style$: Observable<any>;
    const cacheIndex = rowIndex * (source.header.length + 1) + index;
    const res = this.cellStyleCache[cacheIndex];
    if (!res) {
      if (index > 0) {
        style$ = header.styleInfo.pipe(
          map(styleInfo => {
            if (styleInfo.useCellStyleFunction && styleInfo.cellStyleFunction) {
              const rowData = source.rowDataTemplate;
              rowData.Timestamp = row[0];
              source.header.forEach((headerInfo) => {
                rowData[headerInfo.dataKey.label] = row[headerInfo.index];
              });
              const style = styleInfo.cellStyleFunction.execute(value, rowData, this.ctx);
              if (!isObject(style)) {
                throw new TypeError(`${style === null ? 'null' : typeof style} instead of style object`);
              }
              if (Array.isArray(style)) {
                throw new TypeError(`Array instead of style object`);
              }
              return style;
            } else {
              return {};
            }
          }),
          catchError(e => {
            console.warn(`Cell style function for data key '${source.header[index - 1].dataKey.label}' in widget ` +
              `'${this.ctx.widgetConfig.title}' returns '${e}'. Please check your cell style function.`);
            return of({});
          })
        );
      } else {
        style$ = of({});
      }
      style$ = style$.pipe(
        tap((style) => {
          this.cellStyleCache[cacheIndex] = style;
        })
      );
    } else {
      style$ = of(res);
    }
    return style$;
  }

  public cellContent(source: TimeseriesTableSource, header: TimeseriesHeader,
                     index: number, row: TimeseriesRow, value: any, rowIndex: number): Observable<SafeHtml> {
    let content$: Observable<SafeHtml>;
    const cacheIndex = rowIndex * (source.header.length + 1) + index ;
    const res = this.cellContentCache[cacheIndex];
    if (isUndefined(res)) {
      if (index === 0) {
        content$ = of(row.formattedTs);
      } else {
        content$ = header.contentInfo.contentFunction.pipe(
          map((contentFunction) => {
            let content: any;
            if (contentFunction.useCellContentFunction && contentFunction.cellContentFunction) {
              try {
                const rowData = source.rowDataTemplate;
                rowData.Timestamp = row[0];
                source.header.forEach((headerInfo) => {
                  rowData[headerInfo.dataKey.label] = row[headerInfo.index];
                });
                content = contentFunction.cellContentFunction.execute(value, rowData, this.ctx);
              } catch (e) {
                content = '' + value;
              }
            } else {
              content = header.contentInfo.valueFormat.format(value);
            }
            if (isDefined(content)) {
              content = this.utils.customTranslation(content, content);
              switch (typeof content) {
                case 'string':
                  content = this.domSanitizer.bypassSecurityTrustHtml(content);
                  break;
              }
            }
            return content;
          })
        );
      }
      content$ = content$.pipe(
        tap((content) => {
          this.cellContentCache[cacheIndex] = content;
        })
      );
    } else {
      content$ = of(res);
    }
    return content$;
  }

  public onRowClick($event: Event, row: TimeseriesRow) {
    const descriptors = this.ctx.actionsApi.getActionDescriptors('rowClick');
    if (descriptors.length) {
      if ($event) {
        $event.stopPropagation();
      }
      let entityId;
      let entityName;
      let entityLabel;
      if (this.ctx.activeEntityInfo) {
        entityId = this.ctx.activeEntityInfo.entityId;
        entityName = this.ctx.activeEntityInfo.entityName;
        entityLabel = this.ctx.activeEntityInfo.entityLabel;
      }
      this.ctx.actionsApi.handleWidgetAction($event, descriptors[0], entityId, entityName, row, entityLabel);
    }
  }

  public onActionButtonClick($event: Event, row: TimeseriesRow, actionDescriptor: WidgetActionDescriptor) {
    if ($event) {
      $event.stopPropagation();
    }
    let entityId;
    let entityName;
    let entityLabel;
    if (this.ctx.activeEntityInfo) {
      entityId = this.ctx.activeEntityInfo.entityId;
      entityName = this.ctx.activeEntityInfo.entityName;
      entityLabel = this.ctx.activeEntityInfo.entityLabel;
    }
    this.ctx.actionsApi.handleWidgetAction($event, actionDescriptor, entityId, entityName, row, entityLabel);
  }

  public isActiveTab(index: number): boolean {
    return index === this.sourceIndex;
  }

  private updateCurrentSourceAllData() {
    this.sources[this.sourceIndex].timeseriesDatasource.allDataUpdated(this.data, this.latestData);
  }

  private updateCurrentSourceData() {
    this.sources[this.sourceIndex].timeseriesDatasource.dataUpdated(this.data);
  }

  private updateCurrentSourceLatestData() {
    this.sources[this.sourceIndex].timeseriesDatasource.latestDataUpdated(this.latestData);
  }

  private loadCurrentSourceRow() {
    this.sources[this.sourceIndex].timeseriesDatasource.loadRows();
    this.clearCache();
  }

  private clearCache() {
    this.cellContentCache.length = 0;
    this.cellStyleCache.length = 0;
    this.rowStyleCache.length = 0;
  }
}

class TimeseriesDatasource implements DataSource<TimeseriesRow> {

  private rowsSubject = new BehaviorSubject<TimeseriesRow[]>([]);
  private pageDataSubject = new BehaviorSubject<PageData<TimeseriesRow>>(emptyPageData<TimeseriesRow>());

  private allRowsSubject = new BehaviorSubject<TimeseriesRow[]>([]);
  private allRows$: Observable<Array<TimeseriesRow>> = this.allRowsSubject.asObservable();

  public countCellButtonAction = 0;

  private reserveSpaceForHiddenAction = true;
  private cellButtonActions: TableCellButtonActionDescriptor[];
  private usedShowCellActionFunction: boolean;
  private inited = false;

  constructor(
    private source: TimeseriesTableSource,
    private hideEmptyLines: boolean,
    private dateFormatFilter: string,
    private datePipe: DatePipe,
    private widgetContext: WidgetContext
  ) {
    if (this.widgetContext.settings.reserveSpaceForHiddenAction) {
      this.reserveSpaceForHiddenAction = coerceBooleanProperty(this.widgetContext.settings.reserveSpaceForHiddenAction);
    }
    this.source.timeseriesDatasource = this;
  }

  private init(): Observable<any> {
    if (this.inited) {
      return of(null);
    }
    return getTableCellButtonActions(this.widgetContext).pipe(
      tap(actions => {
        this.cellButtonActions = actions
        this.usedShowCellActionFunction = this.cellButtonActions.some(action => action.useShowActionCellButtonFunction);
        this.inited = true;
      })
    );
  }

  connect(collectionViewer: CollectionViewer): Observable<TimeseriesRow[] | ReadonlyArray<TimeseriesRow>> {
    if (this.rowsSubject.isStopped) {
      this.rowsSubject.isStopped = false;
      this.pageDataSubject.isStopped = false;
    }
    return this.rowsSubject.asObservable();
  }

  disconnect(collectionViewer: CollectionViewer): void {
    this.rowsSubject.complete();
    this.pageDataSubject.complete();
  }

  loadRows() {
    this.fetchRows(this.source.pageLink).pipe(
      catchError(() => of(emptyPageData<TimeseriesRow>())),
    ).subscribe(
      (pageData) => {
        this.rowsSubject.next(pageData.data);
        this.pageDataSubject.next(pageData);
      }
    );
  }

  allDataUpdated(data: DatasourceData[], latestData: DatasourceData[]) {
    this.source.rawData = data.slice(this.source.keyStartIndex, this.source.keyEndIndex);
    this.source.latestRawData = latestData.slice(this.source.latestKeyStartIndex, this.source.latestKeyEndIndex);
    this.updateSourceData();
  }

  dataUpdated(data: DatasourceData[]) {
    this.source.rawData = data.slice(this.source.keyStartIndex, this.source.keyEndIndex);
    this.updateSourceData();
  }

  latestDataUpdated(latestData: DatasourceData[]) {
    this.source.latestRawData = latestData.slice(this.source.latestKeyStartIndex, this.source.latestKeyEndIndex);
    this.updateSourceData();
  }

  private updateSourceData() {
    this.convertData(this.source.rawData, this.source.latestRawData).subscribe((data) => {
      this.source.data = data;
      this.allRowsSubject.next(this.source.data);
    });
  }

  private convertData(data: DatasourceData[], latestData: DatasourceData[]): Observable<TimeseriesRow[]> {
    return this.init().pipe(
      map(() => {
        const rowsMap: {[timestamp: number]: TimeseriesRow} = {};
        for (let d = 0; d < data.length; d++) {
          const columnData = data[d].data;
          columnData.forEach((cellData) => {
            const timestamp = cellData[0];
            let row = rowsMap[timestamp];
            if (!row) {
              row = {
                formattedTs: this.datePipe.transform(timestamp, this.dateFormatFilter)
              };
              if (this.cellButtonActions.length) {
                if (this.usedShowCellActionFunction) {
                  const parsedData = formattedDataFormDatasourceData(data, undefined, timestamp);
                  row.actionCellButtons = prepareTableCellButtonActions(this.widgetContext, this.cellButtonActions,
                    parsedData[0], this.reserveSpaceForHiddenAction);
                  row.hasActions = checkHasActions(row.actionCellButtons);
                } else {
                  row.hasActions = true;
                  row.actionCellButtons = this.cellButtonActions;
                }
              }
              row[0] = timestamp;
              for (let c = 0; c < (data.length + latestData.length); c++) {
                row[c + 1] = undefined;
              }
              rowsMap[timestamp] = row;
            }
            row[d + 1] = cellData[1];
          });
        }

        let rows: TimeseriesRow[]  = [];
        if (this.hideEmptyLines) {
          for (const t of Object.keys(rowsMap)) {
            let hideLine = true;
            for (let c = 0; (c < data.length) && hideLine; c++) {
              if (rowsMap[t][c + 1]) {
                hideLine = false;
              }
            }
            if (!hideLine) {
              rows.push(rowsMap[t]);
            }
          }
        } else {
          rows = Object.keys(rowsMap).map(itm => rowsMap[itm]);
        }
        for (let d = 0; d < latestData.length; d++) {
          const columnData = latestData[d].data;
          if (columnData.length) {
            const value = columnData[0][1];
            rows.forEach((row) => {
              row[data.length + d + 1] = value;
            });
          }
        }
        return rows;
      })
    );
  }

  isEmpty(): Observable<boolean> {
    return this.rowsSubject.pipe(
      map((rows) => !rows.length)
    );
  }

  total(): Observable<number> {
    return this.pageDataSubject.pipe(
      map((pageData) => pageData.totalElements)
    );
  }

  private fetchRows(pageLink: PageLink): Observable<PageData<TimeseriesRow>> {
    return this.init().pipe(
      switchMap(() => {
        return this.allRows$.pipe(
          map((data) => {
            const fetchData = pageLink.filterData(data);
            if (this.cellButtonActions.length) {
              let maxCellButtonAction: number;
              if (this.usedShowCellActionFunction && !this.reserveSpaceForHiddenAction) {
                maxCellButtonAction = Math.max(...fetchData.data.map(tsRow => tsRow.actionCellButtons.length));
              } else {
                maxCellButtonAction = this.cellButtonActions.length;
              }
              this.countCellButtonAction = maxCellButtonAction;
            }
            return fetchData;
          })
        );
      })
    );
  }
}
