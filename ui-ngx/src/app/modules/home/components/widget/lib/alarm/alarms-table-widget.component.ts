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
  EventEmitter,
  Injector,
  Input,
  NgZone,
  OnDestroy,
  OnInit,
  StaticProvider,
  ViewChild,
  ViewContainerRef
} from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { WidgetAction, WidgetContext } from '@home/models/widget-component.models';
import { DataKey, WidgetActionDescriptor, WidgetConfig } from '@shared/models/widget.models';
import { IWidgetSubscription } from '@core/api/widget-api.models';
import { UtilsService } from '@core/services/utils.service';
import { TranslateService } from '@ngx-translate/core';
import {
  deepClone,
  hashCode,
  isDefined,
  isDefinedAndNotNull,
  isNotEmptyStr,
  isObject,
  isUndefined
} from '@core/utils';
import cssjs from '@core/css/css';
import { sortItems } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';
import { CollectionViewer, DataSource, SelectionModel } from '@angular/cdk/collections';
import { BehaviorSubject, forkJoin, fromEvent, merge, Observable, of, Subject, Subscription } from 'rxjs';
import { emptyPageData, PageData } from '@shared/models/page/page-data';
import { catchError, debounceTime, distinctUntilChanged, map, take, takeUntil, tap } from 'rxjs/operators';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort, SortDirection } from '@angular/material/sort';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import {
  CellContentInfo,
  CellStyleInfo,
  checkHasActions,
  constructTableCssString,
  DisplayColumn,
  EntityColumn,
  entityDataSortOrderFromString,
  findColumnByEntityKey,
  findEntityKeyByColumnDef,
  fromEntityColumnDef,
  getAlarmValue,
  getCellContentFunctionInfo,
  getCellStyleInfo,
  getColumnDefaultVisibility,
  getColumnSelectionAvailability,
  getColumnWidth,
  getHeaderTitle,
  getRowStyleInfo,
  getTableCellButtonActions,
  isValidPageStepCount,
  isValidPageStepIncrement,
  noDataMessage,
  prepareTableCellButtonActions,
  RowStyleInfo,
  TableCellButtonActionDescriptor,
  TableWidgetDataKeySettings,
  TableWidgetSettings,
  widthStyle
} from '@home/components/widget/lib/table-widget.models';
import { ConnectedPosition, Overlay, OverlayConfig, OverlayRef } from '@angular/cdk/overlay';
import { ComponentPortal } from '@angular/cdk/portal';
import {
  DISPLAY_COLUMNS_PANEL_DATA,
  DisplayColumnsPanelComponent,
  DisplayColumnsPanelData
} from '@home/components/widget/lib/display-columns-panel.component';
import {
  AlarmAssignee,
  AlarmDataInfo,
  alarmFields,
  AlarmInfo,
  alarmSeverityColors,
  AlarmStatus,
  getUserDisplayName,
  getUserInitials
} from '@shared/models/alarm.models';
import { DatePipe } from '@angular/common';
import {
  AlarmDetailsDialogComponent,
  AlarmDetailsDialogData
} from '@home/components/alarm/alarm-details-dialog.component';
import { MatDialog } from '@angular/material/dialog';
import { NULL_UUID } from '@shared/models/id/has-uuid';
import { DialogService } from '@core/services/dialog.service';
import { AlarmService } from '@core/http/alarm.service';
import {
  AlarmData,
  AlarmDataPageLink,
  dataKeyToEntityKey,
  dataKeyTypeToEntityKeyType,
  entityDataPageLinkSortDirection,
  KeyFilter
} from '@app/shared/models/query/query.models';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { entityFields } from '@shared/models/entity.models';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { hidePageSizePixelValue } from '@shared/models/constants';
import {
  ALARM_ASSIGNEE_PANEL_DATA,
  AlarmAssigneePanelComponent,
  AlarmAssigneePanelData
} from '@home/components/alarm/alarm-assignee-panel.component';
import {
  AlarmCommentDialogComponent,
  AlarmCommentDialogData
} from '@home/components/alarm/alarm-comment-dialog.component';
import { EntityService } from '@core/http/entity.service';
import {
  ALARM_FILTER_CONFIG_DATA,
  AlarmFilterConfigComponent,
  AlarmFilterConfigData
} from '@home/components/alarm/alarm-filter-config.component';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { FormBuilder } from '@angular/forms';
import { DEFAULT_OVERLAY_POSITIONS } from '@shared/models/overlay.models';
import { ValueFormatProcessor } from '@shared/models/widget-settings.models';

interface AlarmsTableWidgetSettings extends TableWidgetSettings {
  alarmsTitle: string;
  enableSelectColumnDisplay: boolean;
  defaultSortOrder: string;
  enableSelection: boolean;
  enableStatusFilter?: boolean;
  enableFilter: boolean;
  displayActivity: boolean;
  displayDetails: boolean;
  allowAcknowledgment: boolean;
  allowClear: boolean;
  allowAssign: boolean;
}

interface AlarmWidgetActionDescriptor extends TableCellButtonActionDescriptor {
  details?: boolean;
  acknowledge?: boolean;
  clear?: boolean;
  activity?: boolean;
}

@Component({
  selector: 'tb-alarms-table-widget',
  templateUrl: './alarms-table-widget.component.html',
  styleUrls: ['./alarms-table-widget.component.scss', './../table-widget.scss']
})
export class AlarmsTableWidgetComponent extends PageComponent implements OnInit, OnDestroy, AfterViewInit {


  @Input()
  ctx: WidgetContext;

  @ViewChild('searchInput') searchInputField: ElementRef;
  @ViewChild(MatPaginator) paginator: MatPaginator;
  @ViewChild(MatSort) sort: MatSort;

  textSearch = this.fb.control('', {nonNullable: true});

  public enableSelection = true;
  public displayPagination = true;
  public enableStickyHeader = true;
  public enableStickyAction = false;
  public showCellActionsMenu = true;
  public pageSizeOptions = [];
  public pageLink: AlarmDataPageLink;
  public sortOrderProperty: string;
  public textSearchMode = false;
  public hidePageSize = false;
  public columns: Array<EntityColumn> = [];
  public displayedColumns: string[] = [];
  public alarmsDatasource: AlarmsDatasource;
  public noDataDisplayMessageText: string;
  public hasRowAction: boolean;
  private setCellButtonAction: boolean;

  private cellContentCache: Array<any> = [];
  private cellStyleCache: Array<any> = [];
  private rowStyleCache: Array<any> = [];

  private settings: AlarmsTableWidgetSettings;
  private widgetConfig: WidgetConfig;
  private subscription: IWidgetSubscription;
  private widgetResize$: ResizeObserver;
  private destroy$ = new Subject<void>();

  private displayActivity = false;
  private displayDetails = true;
  public allowAcknowledgment = true;
  private allowClear = true;
  public allowAssign = true;

  private defaultPageSize;
  private defaultSortOrder = '-' + alarmFields.createdTime.value;

  private contentsInfo: {[key: string]: CellContentInfo} = {};
  private stylesInfo: {[key: string]: Observable<CellStyleInfo>} = {};
  private columnWidth: {[key: string]: string} = {};
  private columnDefaultVisibility: {[key: string]: boolean} = {};
  private columnSelectionAvailability: {[key: string]: boolean} = {};
  private columnsWithCellClick: Array<number> = [];

  private rowStylesInfo: Observable<RowStyleInfo>;

  private widgetTimewindowChanged$: Subscription;

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

  private alarmFilterAction: WidgetAction = {
    name: 'alarm.alarm-filter',
    show: true,
    onAction: ($event) => {
      this.editAlarmFilter($event);
    },
    icon: 'filter_list'
  };

  constructor(protected store: Store<AppState>,
              private elementRef: ElementRef,
              private ngZone: NgZone,
              private overlay: Overlay,
              private viewContainerRef: ViewContainerRef,
              private utils: UtilsService,
              public translate: TranslateService,
              private domSanitizer: DomSanitizer,
              private datePipe: DatePipe,
              private dialog: MatDialog,
              private dialogService: DialogService,
              private entityService: EntityService,
              private alarmService: AlarmService,
              private cd: ChangeDetectorRef,
              private fb: FormBuilder) {
    super(store);
    this.pageLink = {
      page: 0,
      pageSize: this.defaultPageSize,
      textSearch: null
    };
  }

  ngOnInit(): void {
    this.ctx.$scope.alarmsTableWidget = this;
    this.settings = this.ctx.settings;
    this.widgetConfig = this.ctx.widgetConfig;
    this.subscription = this.ctx.defaultSubscription;
    this.initializeConfig();
    this.updateAlarmSource();
    this.ctx.updateWidgetParams();

    if (this.displayPagination) {
      this.widgetTimewindowChanged$ = this.ctx.defaultSubscription.widgetTimewindowChanged$.subscribe(
        () => this.pageLink.page = 0
      );
      this.widgetResize$ = new ResizeObserver(() => {
        this.ngZone.run(() => {
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
    this.destroy$.next();
    this.destroy$.complete();
  }

  ngAfterViewInit(): void {
    this.textSearch.valueChanges.pipe(
      debounceTime(150),
      distinctUntilChanged((prev, current) => (this.pageLink.textSearch ?? '') === current.trim()),
      takeUntil(this.destroy$)
    ).subscribe((value) => {
      this.resetPageIndex();
      this.pageLink.textSearch = value.trim();
      this.updateData();
    });

    if (this.displayPagination) {
      this.sort.sortChange.pipe(takeUntil(this.destroy$)).subscribe(() => this.paginator.pageIndex = 0);

      this.ctx.aliasController?.filtersChanged.pipe(
        takeUntil(this.destroy$)
      ).subscribe((filters) => {
        let currentFilterId = this.ctx.defaultSubscription.options.alarmSource?.filterId;
        if (currentFilterId && filters.includes(currentFilterId)) {
          this.paginator.pageIndex = 0;
        }
        this.updateData();
      });
    }
    ((this.displayPagination ? merge(this.sort.sortChange, this.paginator.page) : this.sort.sortChange) as Observable<any>).pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => this.updateData());
    this.updateData();
  }

  public onDataUpdated() {
    this.alarmsDatasource.updateAlarms();
    this.clearCache();
    this.ctx.detectChanges();
  }

  public onEditModeChanged() {
    if (this.textSearchMode || this.enableSelection && this.alarmsDatasource.selection.hasValue()) {
      this.ctx.hideTitlePanel = !this.ctx.isEdit;
      this.ctx.detectChanges(true);
    }
  }

  public pageLinkSortDirection(): SortDirection {
    return entityDataPageLinkSortDirection(this.pageLink);
  }

  private initializeConfig() {
    this.ctx.widgetActions = [this.searchAction, this.alarmFilterAction, this.columnDisplayAction];

    this.displayActivity = isDefined(this.settings.displayActivity) ? this.settings.displayActivity : false;
    this.displayDetails = isDefined(this.settings.displayDetails) ? this.settings.displayDetails : true;
    this.allowAcknowledgment = isDefined(this.settings.allowAcknowledgment) ? this.settings.allowAcknowledgment : true;
    this.allowClear = isDefined(this.settings.allowClear) ? this.settings.allowClear : true;
    this.allowAssign = isDefined(this.settings.allowAssign) ? this.settings.allowAssign : true;

    if (this.settings.alarmsTitle && this.settings.alarmsTitle.length) {
      this.ctx.widgetTitle = this.settings.alarmsTitle;
    } else {
      this.ctx.widgetTitle = this.translate.instant('alarm.alarms');
    }

    this.enableSelection = isDefined(this.settings.enableSelection) ? this.settings.enableSelection : true;
    if (!this.allowAcknowledgment && !this.allowClear) {
      this.enableSelection = false;
    }

    this.searchAction.show = isDefined(this.settings.enableSearch) ? this.settings.enableSearch : true;
    this.displayPagination = isDefined(this.settings.displayPagination) ? this.settings.displayPagination : true;
    this.enableStickyHeader = isDefined(this.settings.enableStickyHeader) ? this.settings.enableStickyHeader : true;
    this.enableStickyAction = isDefined(this.settings.enableStickyAction) ? this.settings.enableStickyAction : false;
    this.showCellActionsMenu = isDefined(this.settings.showCellActionsMenu) ? this.settings.showCellActionsMenu : true;
    this.columnDisplayAction.show = isDefined(this.settings.enableSelectColumnDisplay) ? this.settings.enableSelectColumnDisplay : true;
    this.columnsWithCellClick = this.ctx.actionsApi.getActionDescriptors('cellClick').map(action => action.columnIndex);
    let enableFilter;
    if (isDefined(this.settings.enableFilter)) {
      enableFilter = this.settings.enableFilter;
    } else if (isDefined(this.settings.enableStatusFilter)) {
      enableFilter = this.settings.enableStatusFilter;
    } else {
      enableFilter = true;
    }
    this.alarmFilterAction.show = enableFilter;

    this.rowStylesInfo = getRowStyleInfo(this.ctx, this.settings, 'alarm, ctx');

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
    this.pageLink.pageSize = this.displayPagination ? this.defaultPageSize : 1024;

    const alarmFilter = this.entityService.resolveAlarmFilter(this.widgetConfig.alarmFilterConfig, false);
    this.pageLink = {...this.pageLink, ...alarmFilter};

    this.noDataDisplayMessageText =
      noDataMessage(this.widgetConfig.noDataDisplayMessage, 'alarm.no-alarms-prompt', this.utils, this.translate);

    const cssString = constructTableCssString(this.widgetConfig);
    const cssParser = new cssjs();
    cssParser.testMode = false;
    const namespace = 'alarms-table-' + hashCode(cssString);
    cssParser.cssPreviewNamespace = namespace;
    cssParser.createStyleElement(namespace, cssString);
    $(this.elementRef.nativeElement).addClass(namespace);
  }

  private updateAlarmSource() {

    if (this.enableSelection) {
      this.displayedColumns.push('select');
    }

    const latestDataKeys: Array<DataKey> = [];

    if (this.subscription.alarmSource) {
      this.subscription.alarmSource.dataKeys.forEach((alarmDataKey) => {
        const dataKey: EntityColumn = deepClone(alarmDataKey) as EntityColumn;
        const keySettings: TableWidgetDataKeySettings = dataKey.settings;
        dataKey.entityKey = dataKeyToEntityKey(alarmDataKey);
        dataKey.label = this.utils.customTranslation(dataKey.label, dataKey.label);
        dataKey.title = getHeaderTitle(dataKey, keySettings, this.utils);
        dataKey.def = 'def' + this.columns.length;
        dataKey.sortable = !keySettings.disableSorting && !(dataKey.type === DataKeyType.alarm && dataKey.name.startsWith('details.'));
        if (dataKey.type === DataKeyType.alarm && !isDefined(keySettings.columnWidth)) {
          const alarmField = alarmFields[dataKey.name];
          if (alarmField && alarmField.time) {
            keySettings.columnWidth = '120px';
          }
          if (alarmField && alarmField.keyName === alarmFields.assignee.keyName) {
            keySettings.columnWidth = '120px';
          }
        }
        this.stylesInfo[dataKey.def] = getCellStyleInfo(this.ctx, keySettings, 'value, alarm, ctx');
        const contentFunctionInfo = getCellContentFunctionInfo(this.ctx, keySettings, 'value, alarm, ctx');
        const decimals = (dataKey.decimals || dataKey.decimals === 0) ? dataKey.decimals : this.ctx.widgetConfig.decimals;
        const units = dataKey.units || this.ctx.widgetConfig.units;
        const valueFormat = ValueFormatProcessor.fromSettings(this.ctx.$injector, {units, decimals, showZeroDecimals: true});
        this.contentsInfo[dataKey.def] = {
          contentFunction: contentFunctionInfo,
          valueFormat
        };
        this.columnWidth[dataKey.def] = getColumnWidth(keySettings);
        this.columnDefaultVisibility[dataKey.def] = getColumnDefaultVisibility(keySettings, this.ctx);
        this.columnSelectionAvailability[dataKey.def] = getColumnSelectionAvailability(keySettings);
        this.columns.push(dataKey);

        if (dataKey.type !== DataKeyType.alarm) {
          latestDataKeys.push(dataKey);
        }
      });
      this.displayedColumns.push(...this.columns.filter(column => this.columnDefaultVisibility[column.def])
        .map(column => column.def));
    }
    if (this.settings.defaultSortOrder && this.settings.defaultSortOrder.length) {
      this.defaultSortOrder = this.utils.customTranslation(this.settings.defaultSortOrder, this.settings.defaultSortOrder);
    }
    this.pageLink.sortOrder = entityDataSortOrderFromString(this.defaultSortOrder, this.columns);
    let sortColumn: EntityColumn;
    if (this.pageLink.sortOrder) {
      sortColumn = findColumnByEntityKey(this.pageLink.sortOrder.key, this.columns);
    }
    this.sortOrderProperty = sortColumn ? sortColumn.def : null;

    const actionCellDescriptors: AlarmWidgetActionDescriptor[] = [];
    if (this.displayActivity) {
      actionCellDescriptors.push(
        {
          displayName: this.translate.instant('alarm-activity.activity'),
          icon: 'comment',
          activity: true
        } as AlarmWidgetActionDescriptor
      );
    }

    if (this.displayDetails) {
      actionCellDescriptors.push(
        {
          displayName: this.translate.instant('alarm.details'),
          icon: 'more_horiz',
          details: true
        } as AlarmWidgetActionDescriptor
      );
    }

    if (this.allowAcknowledgment) {
      actionCellDescriptors.push(
        {
          displayName: this.translate.instant('alarm.acknowledge'),
          icon: 'done',
          acknowledge: true
        } as AlarmWidgetActionDescriptor
      );
    }

    if (this.allowClear) {
      actionCellDescriptors.push(
        {
          displayName: this.translate.instant('alarm.clear'),
          icon: 'clear',
          clear: true
        } as AlarmWidgetActionDescriptor
      );
    }

    this.setCellButtonAction = !!(actionCellDescriptors.length + this.ctx.actionsApi.getActionDescriptors('actionCellButton').length);
    this.hasRowAction = !!this.ctx.actionsApi.getActionDescriptors('rowClick').length;

    if (this.setCellButtonAction) {
      this.displayedColumns.push('actions');
    }

    this.alarmsDatasource = new AlarmsDatasource(this.subscription, latestDataKeys, this.ngZone, this.ctx, actionCellDescriptors);
    if (this.enableSelection) {
      this.alarmsDatasource.selectionModeChanged$.subscribe((selectionMode) => {
        const hideTitlePanel = selectionMode || this.textSearchMode && !this.ctx.isEdit;
        if (this.ctx.hideTitlePanel !== hideTitlePanel) {
          this.ctx.hideTitlePanel = hideTitlePanel;
          this.ctx.detectChanges(true);
        } else {
          this.ctx.detectChanges();
        }
      });
    }
  }

  private editColumnsToDisplay($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
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

    const columns: DisplayColumn[] = this.columns.map(column => ({
        title: column.title,
        def: column.def,
        display: this.displayedColumns.indexOf(column.def) > -1,
        selectable: this.columnSelectionAvailability[column.def]
      }));

    const providers: StaticProvider[] = [
      {
        provide: DISPLAY_COLUMNS_PANEL_DATA,
        useValue: {
          columns,
          columnsUpdated: (newColumns) => {
            this.displayedColumns = newColumns.filter(column => column.display).map(column => column.def);
            if (this.enableSelection) {
              this.displayedColumns.unshift('select');
            }
            if (this.setCellButtonAction) {
              this.displayedColumns.push('actions');
            }
            this.clearCache();
          }
        } as DisplayColumnsPanelData
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

  private resetPageIndex(): void {
    if (this.displayPagination) {
      this.paginator.pageIndex = 0;
    }
  }

  private editAlarmFilter($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    const target = $event.target || $event.currentTarget;
    const config = new OverlayConfig({
      panelClass: 'tb-filter-panel',
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
    const authUser = getCurrentAuthUser(this.store);
    const assignedToCurrentUser = isDefinedAndNotNull(this.pageLink.assigneeId) && this.pageLink.assigneeId.id === authUser.userId;
    const assigneeId = assignedToCurrentUser ? null : this.pageLink.assigneeId;
    const providers: StaticProvider[] = [
      {
        provide: ALARM_FILTER_CONFIG_DATA,
        useValue: {
          panelMode: true,
          userMode: true,
          alarmFilterConfig: {
            statusList: deepClone(this.pageLink.statusList),
            severityList: deepClone(this.pageLink.severityList),
            typeList: deepClone(this.pageLink.typeList),
            searchPropagatedAlarms: this.pageLink.searchPropagatedAlarms,
            assignedToCurrentUser,
            assigneeId
          },
          initialAlarmFilterConfig: deepClone(this.widgetConfig.alarmFilterConfig)
        } as AlarmFilterConfigData
      },
      {
        provide: OverlayRef,
        useValue: overlayRef
      }
    ];
    const injector = Injector.create({parent: this.viewContainerRef.injector, providers});
    const componentRef = overlayRef.attach(new ComponentPortal(AlarmFilterConfigComponent,
      this.viewContainerRef, injector));

    const resizeWindows$ = fromEvent(window, 'resize').subscribe(() => {
      overlayRef.updatePosition();
    });

    componentRef.onDestroy(() => {
      resizeWindows$.unsubscribe();
      if (componentRef.instance.panelResult) {
        const result = componentRef.instance.panelResult;
        const alarmFilter = this.entityService.resolveAlarmFilter(result, false);
        this.pageLink = {...this.pageLink, ...alarmFilter};
        this.resetPageIndex();
        this.updateData();
      }
    });
    this.ctx.detectChanges();
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
    this.ctx.hideTitlePanel = false;
    this.ctx.detectChanges(true);
  }

  private updateData() {
    if (this.displayPagination) {
      this.pageLink.page = this.paginator.pageIndex;
      this.pageLink.pageSize = this.paginator.pageSize;
    } else {
      this.pageLink.page = 0;
    }
    const key = findEntityKeyByColumnDef(this.sort.active, this.columns);
    if (key) {
      this.pageLink.sortOrder = {
        key,
        direction: Direction[this.sort.direction.toUpperCase()]
      };
    } else {
      this.pageLink.sortOrder = null;
    }
    const sortOrderLabel = fromEntityColumnDef(this.sort.active, this.columns);
    const keyFilters: KeyFilter[] = null; // TODO:
    this.alarmsDatasource.loadAlarms(this.pageLink, sortOrderLabel, keyFilters);
    this.ctx.detectChanges();
  }

  public trackByColumnDef(index, column: EntityColumn) {
    return column.def;
  }

  public trackByAlarmId(index: number, alarm: AlarmData) {
    return alarm.id.id;
  }

  public trackByActionCellDescriptionId(index: number, action: WidgetActionDescriptor) {
    return action.id;
  }

  public headerStyle(key: EntityColumn): any {
    const columnWidth = this.columnWidth[key.def];
    return widthStyle(columnWidth);
  }

  public rowStyle(alarm: AlarmDataInfo, row: number): Observable<any> {
    let style$: Observable<any>;
    const res = this.rowStyleCache[row];
    if (!res) {
      style$ = this.rowStylesInfo.pipe(
        map(styleInfo => {
          if (styleInfo.useRowStyleFunction && styleInfo.rowStyleFunction) {
            const style = styleInfo.rowStyleFunction.execute(alarm, this.ctx);
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
          console.warn(`Row style function in widget '${this.ctx.widgetTitle}' ` +
            `returns '${e}'. Please check your row style function.`);
          return of({});
        })
      );
      style$ = style$.pipe(
        tap((style) => {
          this.rowStyleCache[row] = style;
        })
      );
    } else {
      style$ = of(res);
    }
    return style$;
  }

  public cellStyle(alarm: AlarmDataInfo, key: EntityColumn, row: number): Observable<any> {
    let style$: Observable<any>;
    const col = this.columns.indexOf(key);
    const index = row * this.columns.length + col;
    const res = this.cellStyleCache[index];
    if (!res) {
      if (alarm && key) {
        style$ = this.stylesInfo[key.def].pipe(
          map(styleInfo => {
            const value = getAlarmValue(alarm, key);
            if (styleInfo.useCellStyleFunction && styleInfo.cellStyleFunction) {
              const style = styleInfo.cellStyleFunction.execute(value, alarm, this.ctx);
              if (!isObject(style)) {
                throw new TypeError(`${style === null ? 'null' : typeof style} instead of style object`);
              }
              if (Array.isArray(style)) {
                throw new TypeError(`Array instead of style object`);
              }
              return style;
            } else {
              return this.defaultStyle(key, value);
            }
          }),
          catchError(e => {
            console.warn(`Cell style function for data key '${key.label}' in widget '${this.ctx.widgetTitle}' ` +
              `returns '${e}'. Please check your cell style function.`);
            return of({});
          })
        );
      } else {
        style$ = of({});
      }
      style$ = style$.pipe(
        map((style) => {
          if (!style.width) {
            const columnWidth = this.columnWidth[key.def];
            style = Object.assign(style, widthStyle(columnWidth));
          }
          return style;
        }),
        tap((style) => {
          this.cellStyleCache[index] = style;
        })
      );
    } else {
      style$ = of(res);
    }
    return style$;
  }

  public cellContent(alarm: AlarmDataInfo, key: EntityColumn, row: number): Observable<SafeHtml> {
    let content$: Observable<SafeHtml>;
    const col = this.columns.indexOf(key);
    const index = row * this.columns.length + col;
    const res = this.cellContentCache[index];
    if (isUndefined(res)) {
      const contentInfo = this.contentsInfo[key.def];
      content$ = contentInfo.contentFunction.pipe(
        map((contentFunction) => {
          let content: any = '';
          if (alarm && key) {
            const contentInfo = this.contentsInfo[key.def];
            const value = getAlarmValue(alarm, key);
            if (contentFunction.useCellContentFunction && contentFunction.cellContentFunction) {
              try {
                content = contentFunction.cellContentFunction.execute(value, alarm, this.ctx);
              } catch (e) {
                content = '' + value;
              }
            } else {
              content = this.defaultContent(key, contentInfo, value);
            }
            if (isDefined(content)) {
              content = this.utils.customTranslation(content, content);
              switch (typeof content) {
                case 'string':
                  content = this.domSanitizer.bypassSecurityTrustHtml(content);
                  break;
              }
            }
          }
          return content;
        })
      );
      content$ = content$.pipe(
        tap((content) => {
          this.cellContentCache[index] = content;
        })
      );
    } else {
      content$ = of(res);
    }
    return content$;
  }

  public onCellClick($event: Event, alarm: AlarmDataInfo, key: EntityColumn, columnIndex: number) {
    this.alarmsDatasource.toggleCurrentAlarm(alarm);
    const descriptors = this.ctx.actionsApi.getActionDescriptors('cellClick');
    let descriptor;
    if (descriptors.length) {
      descriptor = descriptors.find(desc => desc.columnIndex === columnIndex);
    }
    if ($event && descriptor) {
      $event.stopPropagation();
      let entityId;
      let entityName;
      let entityLabel;
      if (alarm && alarm.originator) {
        entityId = alarm.originator;
        entityName = alarm.entityName;
        entityLabel = alarm.entityLabel;
      }
      this.ctx.actionsApi.handleWidgetAction($event, descriptor, entityId, entityName, {alarm, key}, entityLabel);
    }
  }

  public columnHasCellClick(columnIndex: number) {
    if (this.columnsWithCellClick.length) {
      return this.columnsWithCellClick.includes(columnIndex);
    }
  }

  public onRowClick($event: Event, alarm: AlarmDataInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    this.alarmsDatasource.toggleCurrentAlarm(alarm);
    const descriptors = this.ctx.actionsApi.getActionDescriptors('rowClick');
    if (descriptors.length) {
      let entityId;
      let entityName;
      let entityLabel;
      if (alarm && alarm.originator) {
        entityId = alarm.originator;
        entityName = alarm.originatorName;
        entityLabel = alarm.originatorLabel;
      }
      this.ctx.actionsApi.handleWidgetAction($event, descriptors[0], entityId, entityName, {alarm}, entityLabel);
    }
  }

  public onActionButtonClick($event: Event, alarm: AlarmDataInfo, actionDescriptor: AlarmWidgetActionDescriptor) {
    if (actionDescriptor.details) {
      this.openAlarmDetails($event, alarm);
    } else if (actionDescriptor.acknowledge) {
      this.ackAlarm($event, alarm);
    } else if (actionDescriptor.clear) {
      this.clearAlarm($event, alarm);
    } else if (actionDescriptor.activity) {
      this.openAlarmActivity($event, alarm);
    } else {
      if ($event) {
        $event.stopPropagation();
      }
      let entityId;
      let entityName;
      let entityLabel;
      if (alarm && alarm.originator) {
        entityId = alarm.originator;
        entityName = alarm.originatorName;
        entityLabel = alarm.originatorLabel;
      }
      this.ctx.actionsApi.handleWidgetAction($event, actionDescriptor, entityId, entityName, {alarm}, entityLabel);
    }
  }

  public actionEnabled(alarm: AlarmDataInfo, actionDescriptor: AlarmWidgetActionDescriptor): boolean {
    if (actionDescriptor.acknowledge) {
      return (alarm.status === AlarmStatus.ACTIVE_UNACK ||
        alarm.status === AlarmStatus.CLEARED_UNACK);
    } else if (actionDescriptor.clear) {
      return (alarm.status === AlarmStatus.ACTIVE_ACK ||
        alarm.status === AlarmStatus.ACTIVE_UNACK);
    }
    return true;
  }

  private openAlarmDetails($event: Event, alarm: AlarmDataInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    if (alarm && alarm.id && alarm.id.id !== NULL_UUID) {
      this.dialog.open<AlarmDetailsDialogComponent, AlarmDetailsDialogData, boolean>
      (AlarmDetailsDialogComponent,
        {
          disableClose: true,
          panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
          data: {
            alarmId: alarm.id.id,
            allowAcknowledgment: this.allowAcknowledgment,
            allowClear: this.allowClear,
            displayDetails: true,
            allowAssign: this.allowAssign
          }
        }).afterClosed().subscribe(
        (res) => {
          if (res) {
            this.subscription.update();
          }
        }
      );
    }
  }

  private ackAlarm($event: Event, alarm: AlarmDataInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    if (alarm && alarm.id && alarm.id.id !== NULL_UUID) {
      this.dialogService.confirm(
        this.translate.instant('alarm.aknowledge-alarm-title'),
        this.translate.instant('alarm.aknowledge-alarm-text'),
        this.translate.instant('action.no'),
        this.translate.instant('action.yes')
      ).subscribe((res) => {
        if (res) {
          if (res) {
            this.alarmService.ackAlarm(alarm.id.id).subscribe(() => {
              this.subscription.update();
            });
          }
        }
      });
    }
  }

  public ackAlarms($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    if (this.alarmsDatasource.selection.hasValue()) {
      const unacknowledgedAlarms = this.alarmsDatasource.selection.selected.filter(
        alarm => alarm.id.id !== NULL_UUID && !alarm.acknowledged
      );
      let title = '';
      let content = '';
      if (!unacknowledgedAlarms.length) {
        title = this.translate.instant('alarm.selected-alarms', {count: unacknowledgedAlarms.length});
        content = this.translate.instant('alarm.selected-alarms-are-acknowledged');
        this.dialogService.alert(
          title,
          content
        ).subscribe();
      } else {
        title = this.translate.instant('alarm.aknowledge-alarms-title', {count: unacknowledgedAlarms.length});
        content = this.translate.instant('alarm.aknowledge-alarms-text', {count: unacknowledgedAlarms.length});
        this.dialogService.confirm(
          title,
          content,
          this.translate.instant('action.no'),
          this.translate.instant('action.yes')
        ).subscribe((res) => {
          if (res) {
            const tasks: Observable<AlarmInfo>[] = [];
            for (const alarm of unacknowledgedAlarms) {
              tasks.push(this.alarmService.ackAlarm(alarm.id.id));
            }
            forkJoin(tasks).subscribe(() => {
              this.alarmsDatasource.clearSelection();
              this.subscription.update();
            });
          }
        });
      }
    }
  }

  private clearAlarm($event: Event, alarm: AlarmDataInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    if (alarm && alarm.id && alarm.id.id !== NULL_UUID) {
      this.dialogService.confirm(
        this.translate.instant('alarm.clear-alarm-title'),
        this.translate.instant('alarm.clear-alarm-text'),
        this.translate.instant('action.no'),
        this.translate.instant('action.yes')
      ).subscribe((res) => {
        if (res) {
          if (res) {
            this.alarmService.clearAlarm(alarm.id.id).subscribe(() => {
              this.subscription.update();
            });
          }
        }
      });
    }
  }

  public clearAlarms($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    if (this.alarmsDatasource.selection.hasValue()) {
      const activeAlarms = this.alarmsDatasource.selection.selected.filter(
        alarm => alarm.id.id !== NULL_UUID && !alarm.cleared
      );
      let title = '';
      let content = '';
      if (!activeAlarms.length) {
        title = this.translate.instant('alarm.selected-alarms', {count: activeAlarms.length});
        content = this.translate.instant('alarm.selected-alarms-are-cleared');
        this.dialogService.alert(
          title,
          content
        ).subscribe();
      } else {
        title = this.translate.instant('alarm.clear-alarms-title', {count: activeAlarms.length});
        content = this.translate.instant('alarm.clear-alarms-text', {count: activeAlarms.length});
        this.dialogService.confirm(
          title,
          content,
          this.translate.instant('action.no'),
          this.translate.instant('action.yes')
        ).subscribe((res) => {
          if (res) {
            const tasks: Observable<AlarmInfo>[] = [];
            for (const alarm of activeAlarms) {
              tasks.push(this.alarmService.clearAlarm(alarm.id.id));
            }
            forkJoin(tasks).subscribe(() => {
              this.alarmsDatasource.clearSelection();
              this.subscription.update();
            });
          }
        });
      }
    }
  }

  private openAlarmActivity($event: Event, alarm: AlarmDataInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    if (alarm && alarm.id && alarm.id.id !== NULL_UUID) {
      this.dialog.open<AlarmCommentDialogComponent, AlarmCommentDialogData, void>
      (AlarmCommentDialogComponent,
        {
          disableClose: true,
          panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
          data: {
            alarmId: alarm.id.id
          }
        }).afterClosed();
    }
  }

  private defaultContent(key: EntityColumn, contentInfo: CellContentInfo, value: any): any {
    if (isDefined(value)) {
      const alarmField = alarmFields[key.name];
      if (alarmField) {
        return this.utils.defaultAlarmFieldContent(key, value);
      }
      const entityField = entityFields[key.name];
      if (entityField) {
        if (entityField.time) {
          return this.datePipe.transform(value, 'yyyy-MM-dd HH:mm:ss');
        }
      }
      return contentInfo.valueFormat.format(value);
    } else {
      return '';
    }
  }

  private defaultStyle(key: EntityColumn, value: any): any {
    if (isDefined(value)) {
      const alarmField = alarmFields[key.name];
      if (alarmField) {
        if (alarmField.value === alarmFields.severity.value) {
          return {
            fontWeight: 'bold',
            color: alarmSeverityColors.get(value)
          };
        } else {
          return {};
        }
      } else {
        return {};
      }
    } else {
      return {};
    }
  }

  private clearCache() {
    this.cellContentCache.length = 0;
    this.cellStyleCache.length = 0;
    this.rowStyleCache.length = 0;
  }

  checkAssigneeHasName(alarmAssignee: AlarmAssignee): boolean {
    return (isNotEmptyStr(alarmAssignee?.firstName) || isNotEmptyStr(alarmAssignee?.lastName)) ||
      isNotEmptyStr(alarmAssignee?.email);
  }

  getUserDisplayName(alarmAssignee: AlarmAssignee) {
    return getUserDisplayName(alarmAssignee);
  }

  getUserInitials(alarmAssignee: AlarmAssignee): string {
    return getUserInitials(alarmAssignee);
  }

  getAvatarBgColor(alarmAssignee: AlarmAssignee) {
    return this.utils.stringToHslColor(this.getUserDisplayName(alarmAssignee), 40, 60);
  }

  openAlarmAssigneePanel($event: Event, entity: AlarmInfo) {
    $event?.stopPropagation();
    if (entity.id.id === NULL_UUID) {
      return
    }
    const target = $event.target || $event.currentTarget;
    const config = new OverlayConfig();
    config.backdropClass = 'cdk-overlay-transparent-backdrop';
    config.hasBackdrop = true;
    const connectedPosition: ConnectedPosition = {
      originX: 'end',
      originY: 'bottom',
      overlayX: 'end',
      overlayY: 'top'
    };
    config.positionStrategy = this.overlay.position().flexibleConnectedTo(target as HTMLElement)
      .withPositions([connectedPosition]);
    config.minWidth = '260px';
    const overlayRef = this.overlay.create(config);
    overlayRef.backdropClick().subscribe(() => {
      overlayRef.dispose();
    });
    const providers: StaticProvider[] = [
      {
        provide: ALARM_ASSIGNEE_PANEL_DATA,
        useValue: {
          alarmId: entity.id.id,
          assigneeId: entity.assigneeId?.id
        } as AlarmAssigneePanelData
      },
      {
        provide: OverlayRef,
        useValue: overlayRef
      }
    ];
    const injector = Injector.create({parent: this.viewContainerRef.injector, providers});
    overlayRef.attach(new ComponentPortal(AlarmAssigneePanelComponent,
      this.viewContainerRef, injector));
  }
}

class AlarmsDatasource implements DataSource<AlarmDataInfo> {

  private alarmsSubject = new BehaviorSubject<AlarmDataInfo[]>([]);
  private pageDataSubject = new BehaviorSubject<PageData<AlarmDataInfo>>(emptyPageData<AlarmDataInfo>());

  public selection = new SelectionModel<AlarmDataInfo>(true, [], false,
    (alarm1: AlarmDataInfo, alarm2: AlarmDataInfo) => alarm1.id.id === alarm2.id.id);

  private selectionModeChanged = new EventEmitter<boolean>();

  public selectionModeChanged$ = this.selectionModeChanged.asObservable();

  private currentAlarm: AlarmDataInfo = null;

  public dataLoading = true;
  public countCellButtonAction = 0;

  private appliedPageLink: AlarmDataPageLink;
  private appliedSortOrderLabel: string;

  private reserveSpaceForHiddenAction = true;
  private cellButtonActions: TableCellButtonActionDescriptor[];
  private usedShowCellActionFunction: boolean;
  private inited = false;

  constructor(private subscription: IWidgetSubscription,
              private dataKeys: Array<DataKey>,
              private ngZone: NgZone,
              private widgetContext: WidgetContext,
              private actionCellDescriptors: AlarmWidgetActionDescriptor[]) {
    if (this.widgetContext.settings.reserveSpaceForHiddenAction) {
      this.reserveSpaceForHiddenAction = coerceBooleanProperty(this.widgetContext.settings.reserveSpaceForHiddenAction);
    }
  }

  private init(): Observable<any> {
    if (this.inited) {
      return of(null);
    }
    return getTableCellButtonActions(this.widgetContext).pipe(
      tap(actions => {
        this.cellButtonActions = this.actionCellDescriptors.concat(actions);
        this.usedShowCellActionFunction = this.cellButtonActions.some(action => action.useShowActionCellButtonFunction);
        this.inited = true;
      })
    );
  }

  connect(collectionViewer: CollectionViewer): Observable<AlarmDataInfo[] | ReadonlyArray<AlarmDataInfo>> {
    return this.alarmsSubject.asObservable();
  }

  disconnect(collectionViewer: CollectionViewer): void {
    this.alarmsSubject.complete();
    this.pageDataSubject.complete();
  }

  loadAlarms(pageLink: AlarmDataPageLink, sortOrderLabel: string, keyFilters: KeyFilter[]) {
    this.dataLoading = true;
    // this.clear();
    this.appliedPageLink = pageLink;
    this.appliedSortOrderLabel = sortOrderLabel;
    this.subscription.subscribeForAlarms(pageLink, keyFilters);
  }

  private clear() {
    if (this.selection.hasValue()) {
      this.selection.clear();
      this.onSelectionModeChanged(false);
    }
    this.alarmsSubject.next([]);
    this.pageDataSubject.next(emptyPageData<AlarmDataInfo>());
  }

  updateAlarms() {
    this.init().subscribe(() => {
      const subscriptionAlarms = this.subscription.alarms;
      let alarms = new Array<AlarmDataInfo>();
      let maxCellButtonAction = 0;
      let isEmptySelection = false;
      const dynamicWidthCellButtonActions = this.usedShowCellActionFunction && !this.reserveSpaceForHiddenAction;
      subscriptionAlarms.data.forEach((alarmData) => {
        const alarm = this.alarmDataToInfo(alarmData);
        alarms.push(alarm);
        if (dynamicWidthCellButtonActions && alarm.actionCellButtons.length > maxCellButtonAction) {
          maxCellButtonAction = alarm.actionCellButtons.length;
        }
      });
      if (!dynamicWidthCellButtonActions && this.cellButtonActions.length && alarms.length) {
        maxCellButtonAction = alarms[0].actionCellButtons.length;
      }
      if (this.appliedSortOrderLabel && this.appliedSortOrderLabel.length) {
        const asc = this.appliedPageLink.sortOrder.direction === Direction.ASC;
        alarms = alarms.sort((a, b) => sortItems(a, b, this.appliedSortOrderLabel, asc));
      }
      if (this.selection.hasValue()) {
        const alarmIds = alarms.map((alarm) => alarm.id.id);
        const toRemove = this.selection.selected.filter(alarm => alarmIds.indexOf(alarm.id.id) === -1);
        this.selection.deselect(...toRemove);
        if (this.selection.isEmpty()) {
          isEmptySelection = true;
        }
      }
      const alarmsPageData: PageData<AlarmDataInfo> = {
        data: alarms,
        totalPages: subscriptionAlarms.totalPages,
        totalElements: subscriptionAlarms.totalElements,
        hasNext: subscriptionAlarms.hasNext
      };
      this.ngZone.run(() => {
        if (isEmptySelection) {
          this.onSelectionModeChanged(false);
        }
        this.alarmsSubject.next(alarms);
        this.pageDataSubject.next(alarmsPageData);
        this.countCellButtonAction = maxCellButtonAction;
        this.dataLoading = false;
      });
    });
  }

  private alarmDataToInfo(alarmData: AlarmData): AlarmDataInfo {
    const alarm: AlarmDataInfo = deepClone(alarmData);
    delete alarm.latest;
    const latest = alarmData.latest;
    this.dataKeys.forEach((dataKey) => {
      const type = dataKeyTypeToEntityKeyType(dataKey.type);
      let value = '';
      if (type) {
        if (latest && latest[type]) {
          const tsVal = latest[type][dataKey.name];
          if (tsVal) {
            value = tsVal.value;
          }
        }
      }
      alarm[dataKey.label] = value;
    });
    if (this.cellButtonActions.length) {
      if (this.usedShowCellActionFunction) {
        alarm.actionCellButtons = prepareTableCellButtonActions(this.widgetContext, this.cellButtonActions,
                                                                alarm, this.reserveSpaceForHiddenAction);
        alarm.hasActions = checkHasActions(alarm.actionCellButtons);
      } else {
        alarm.actionCellButtons = this.cellButtonActions;
        alarm.hasActions = true;
      }
    }
    return alarm;
  }

  isAllSelected(): Observable<boolean> {
    const numSelected = this.selection.selected.length;
    return this.alarmsSubject.pipe(
      map((alarms) => numSelected === alarms.length)
    );
  }

  isEmpty(): Observable<boolean> {
    return this.alarmsSubject.pipe(
      map((alarms) => !alarms.length)
    );
  }

  total(): Observable<number> {
    return this.pageDataSubject.pipe(
      map((pageData) => pageData.totalElements)
    );
  }

  toggleSelection(alarm: AlarmDataInfo) {
    const hasValue = this.selection.hasValue();
    this.selection.toggle(alarm);
    if (hasValue !== this.selection.hasValue()) {
      this.onSelectionModeChanged(this.selection.hasValue());
    }
  }

  isSelected(alarm: AlarmDataInfo): boolean {
    return this.selection.isSelected(alarm);
  }

  clearSelection() {
    if (this.selection.hasValue()) {
      this.selection.clear();
      this.onSelectionModeChanged(false);
    }
  }

  masterToggle() {
    this.alarmsSubject.pipe(
      tap((alarms) => {
        const numSelected = this.selection.selected.length;
        if (numSelected === alarms.length) {
          this.selection.clear();
          if (numSelected > 0) {
            this.onSelectionModeChanged(false);
          }
        } else {
          alarms.forEach(row => {
            this.selection.select(row);
          });
          if (numSelected === 0) {
            this.onSelectionModeChanged(true);
          }
        }
      }),
      take(1)
    ).subscribe();
  }

  public toggleCurrentAlarm(alarm: AlarmDataInfo): boolean {
    if (this.currentAlarm !== alarm) {
      this.currentAlarm = alarm;
      return true;
    } else {
      return false;
    }
  }

  public isCurrentAlarm(alarm: AlarmDataInfo): boolean {
    return (this.currentAlarm && alarm && this.currentAlarm.id && alarm.id) &&
      (this.currentAlarm.id.id === alarm.id.id);
  }

  private onSelectionModeChanged(selectionMode: boolean) {
    this.selectionModeChanged.emit(selectionMode);
  }
}
