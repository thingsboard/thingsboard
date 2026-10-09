// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import {
  AfterViewInit,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  ElementRef,
  EventEmitter,
  Input,
  NgZone,
  OnChanges,
  OnDestroy,
  OnInit,
  Renderer2,
  SimpleChanges, Type,
  ViewChild,
  ViewContainerRef
} from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { MAX_SAFE_PAGE_SIZE, PageLink, PageQueryParam, TimePageLink } from '@shared/models/page/page-link';
import { MatDialog } from '@angular/material/dialog';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort, SortDirection } from '@angular/material/sort';
import { EntitiesDataSource } from '@home/models/datasource/entity-datasource';
import { catchError, debounceTime, distinctUntilChanged, filter, map, skip, takeUntil } from 'rxjs/operators';
import { Direction, SortOrder } from '@shared/models/page/sort-order';
import { forkJoin, merge, Observable, of, Subject, Subscription } from 'rxjs';
import { TranslateService } from '@ngx-translate/core';
import { BaseData, HasId } from '@shared/models/base-data';
import { ActivatedRoute, QueryParamsHandling, Router } from '@angular/router';
import {
  CellActionDescriptor,
  CellActionDescriptorType,
  ChartEntityTableColumn,
  EntityActionTableColumn,
  EntityChipsEntityTableColumn,
  EntityColumn, EntityColumnsType, EntityColumnType,
  EntityLinkTableColumn,
  EntityTableColumn,
  EntityTableConfig,
  GroupActionDescriptor,
  HeaderActionDescriptor,
  ProgressBarEntityTableColumn
} from '@home/models/entity/entities-table-config.models';
import { baseDetailsPageByEntityType, EntityTypeTranslation } from '@shared/models/entity-type.models';
import { DialogService } from '@core/services/dialog.service';
import { AddEntityDialogComponent } from './add-entity-dialog.component';
import { AddEntityDialogData, EntityAction } from '@home/models/entity/entity-component.models';
import { getTimePageLinkInterval, Timewindow } from '@shared/models/time/time.models';
import { EntityId } from '@shared/models/id/entity-id';
import { AiChatView } from '@shared/models/ai-chat.models';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { TbAnchorComponent } from '@shared/components/tb-anchor.component';
import { isDefined, isDefinedAndNotNull, isEqual, isNotEmptyStr, isNumber, isUndefined } from '@core/utils';
import { HasUUID } from '@shared/models/id/has-uuid';
import { hidePageSizePixelValue } from '@shared/models/constants';
import { EntitiesTableAction, IEntitiesTableComponent } from '@home/models/entity/entity-table-component.models';
import { EntityDetailsPanelComponent } from '@home/components/entity/entity-details-panel.component';
import { FormBuilder } from '@angular/forms';
import { AiAssistantPanelService } from '@core/services/ai-assistant-panel.service';
import { getCurrentAuthState } from '@core/auth/auth.selectors';

@Component({
    selector: 'tb-entities-table',
    templateUrl: './entities-table.component.html',
    styleUrls: ['./entities-table.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class EntitiesTableComponent extends PageComponent implements IEntitiesTableComponent, AfterViewInit, OnInit, OnChanges, OnDestroy {

  @Input()
  entitiesTableConfig: EntityTableConfig<BaseData<HasId>>;

  translations: EntityTypeTranslation;

  headerActionDescriptors: Array<HeaderActionDescriptor>;
  groupActionDescriptors: Array<GroupActionDescriptor<BaseData<HasId>>>;
  cellActionDescriptors: Array<CellActionDescriptor<BaseData<HasId>>>;

  actionColumns: Array<EntityActionTableColumn<BaseData<HasId>>>;
  entityColumns: EntityColumnsType;
  displayedColumns: string[];

  headerCellStyleCache: Array<any> = [];

  cellContentCache: Array<SafeHtml> = [];
  cellTooltipCache: Array<string> = [];

  cellStyleCache: Array<any> = [];

  selectionEnabled;

  defaultPageSize;
  displayPagination = true;
  hidePageSize = false;
  pageSizeOptions = [];
  pageLink: PageLink;
  pageMode = true;
  textSearchMode = false;
  timewindow: Timewindow;
  dataSource: EntitiesDataSource<BaseData<HasId>>;

  cellActionType = CellActionDescriptorType;

  isDetailsOpen = false;
  detailsPanelOpened = new EventEmitter<boolean>();

  get isAiAssistantOpen() { return this.panelService.open(); }
  aiEnabled = getCurrentAuthState(this.store).aiEnabled;

  configureWithAiButton: HeaderActionDescriptor;

  replaceComponent: Type<any>;

  @ViewChild('replaceComponentAnchor', {static: true}) replaceComponentAnchor: TbAnchorComponent;

  @ViewChild('entityTableHeader', {static: true}) entityTableHeaderAnchor: TbAnchorComponent;

  @ViewChild('searchInput') searchInputField: ElementRef;

  @ViewChild(MatPaginator) paginator: MatPaginator;
  @ViewChild(MatSort) sort: MatSort;

  @ViewChild('entityDetailsPanel') entityDetailsPanel: EntityDetailsPanelComponent;

  textSearch = this.fb.control('', {nonNullable: true});

  private updateDataSubscription: Subscription;
  private viewInited = false;

  private widgetResize$: ResizeObserver;
  private destroy$ = new Subject<void>();
  private aiUpdatedData$: Subscription;

  constructor(protected store: Store<AppState>,
              public route: ActivatedRoute,
              public translate: TranslateService,
              public dialog: MatDialog,
              private dialogService: DialogService,
              private domSanitizer: DomSanitizer,
              private cd: ChangeDetectorRef,
              private router: Router,
              private elementRef: ElementRef,
              private fb: FormBuilder,
              private zone: NgZone,
              public viewContainerRef: ViewContainerRef,
              public renderer: Renderer2,
              public panelService: AiAssistantPanelService) {
    super(store);
  }

  ngOnInit() {
    if (this.entitiesTableConfig) {
      this.init(this.entitiesTableConfig);
    } else {
      this.route.data.pipe(
        takeUntil(this.destroy$)
      ).subscribe((data) => {
        this.init(data.entitiesTableConfig);
      });
    }
    // Observed outside the zone so a width change does not trigger change detection on every frame.
    this.zone.runOutsideAngular(() => {
      this.widgetResize$ = new ResizeObserver(() => {
        const showHidePageSize = this.elementRef.nativeElement.offsetWidth < hidePageSizePixelValue;
        if (showHidePageSize !== this.hidePageSize) {
          this.zone.run(() => {
            this.hidePageSize = showHidePageSize;
            this.cd.markForCheck();
          });
        }
      });
      this.widgetResize$.observe(this.elementRef.nativeElement);
    });
  }

  ngOnDestroy() {
    if (this.widgetResize$) {
      this.widgetResize$.disconnect();
    }
    if (this.entitiesTableConfig?.aiAssistantConfig) {
      this.panelService.teardown();
    }
    this.entitiesTableConfig?.onDestroy();
    this.destroy$.next();
    this.destroy$.complete();
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (propName === 'entitiesTableConfig' && change.currentValue) {
          this.init(change.currentValue);
        }
      }
    }
  }

  goBack(): void {
    this.router.navigate(this.entitiesTableConfig.backNavigationCommands, { relativeTo: this.route });
  }

  private init(entitiesTableConfig: EntityTableConfig<BaseData<HasId>>) {

    if (this.route.snapshot.data.replaceComponent) {
      this.replaceComponent = this.route.snapshot.data.replaceComponent(this.store);
    }

    const viewContainerRef = this.replaceComponentAnchor.viewContainerRef;
    viewContainerRef.clear();
    if (this.replaceComponent) {
      viewContainerRef.createComponent(this.replaceComponent);
    }

    this.isDetailsOpen = false;
    this.entitiesTableConfig = entitiesTableConfig;
    const aiConfig = entitiesTableConfig.aiAssistantConfig;
    if (aiConfig) {
      this.panelService.setEnabled(true);
      this.panelService.setConfig(aiConfig);
      this.configureWithAiButton = {
        name: this.translate.instant('ai-assistant.configure-with-ai'),
        icon: 'mdi:creation',
        isEnabled: () => this.aiEnabled && aiConfig.showButton !== false && !this.isAiAssistantOpen,
        onAction: ($event) => this.toggleAiAssistant($event)
      };
      this.updateAiClientContext();
      this.aiUpdatedData$?.unsubscribe();
      if (entitiesTableConfig.entityType) {
        this.aiUpdatedData$ = this.panelService.updatedData$.pipe(
          filter(affectedEntities =>
            affectedEntities.some(entityId => entityId.entityType === entitiesTableConfig.entityType)),
          takeUntil(this.destroy$)
        ).subscribe(() => {
          this.updateData();
        });
      }
    }

    this.pageMode = this.entitiesTableConfig.pageMode;
    if (this.entitiesTableConfig.headerComponent) {
      const viewContainerRef = this.entityTableHeaderAnchor.viewContainerRef;
      viewContainerRef.clear();
      const componentRef = viewContainerRef.createComponent(this.entitiesTableConfig.headerComponent);
      const headerComponent = componentRef.instance;
      headerComponent.entitiesTableConfig = this.entitiesTableConfig;
    }

    this.entitiesTableConfig.setTable(this);
    this.translations = this.entitiesTableConfig.entityTranslations;

    this.headerActionDescriptors = [...this.entitiesTableConfig.headerActionDescriptors];
    this.groupActionDescriptors = [...this.entitiesTableConfig.groupActionDescriptors];
    this.cellActionDescriptors = [...this.entitiesTableConfig.cellActionDescriptors];

    if (this.entitiesTableConfig.entitiesDeleteEnabled) {
      this.cellActionDescriptors.push(
        {
          name: this.translate.instant('action.delete'),
          icon: 'delete',
          isEnabled: entity => this.entitiesTableConfig.deleteEnabled(entity),
          onAction: ($event, entity) => this.deleteEntity($event, entity)
        }
      );
      this.groupActionDescriptors.push(
        {
          name: this.translate.instant('action.delete'),
          icon: 'delete',
          isEnabled: true,
          onAction: ($event, entities) => this.deleteEntities($event, entities)
        }
      );
    }

    const enabledGroupActionDescriptors =
      this.groupActionDescriptors.filter((descriptor) => descriptor.isEnabled);

    this.selectionEnabled = this.entitiesTableConfig.selectionEnabled && enabledGroupActionDescriptors.length;

    this.columnsUpdated();

    const routerQueryParams: PageQueryParam = this.route.snapshot.queryParams;

    let sortOrder: SortOrder = null;
    let initialAction: EntitiesTableAction = null;
    if (this.pageMode) {
      initialAction = routerQueryParams?.action;
      if (this.entitiesTableConfig.defaultSortOrder || routerQueryParams.hasOwnProperty('direction')
        || routerQueryParams.hasOwnProperty('property')) {
        sortOrder = {
          property: routerQueryParams?.property || this.entitiesTableConfig.defaultSortOrder.property,
          direction: routerQueryParams?.direction || this.entitiesTableConfig.defaultSortOrder.direction
        };
      }
    } else if (this.entitiesTableConfig.defaultSortOrder){
      sortOrder = {
        property: this.entitiesTableConfig.defaultSortOrder.property,
        direction: this.entitiesTableConfig.defaultSortOrder.direction
      };
    }

    this.displayPagination = this.entitiesTableConfig.displayPagination;
    const pageSize = this.entitiesTableConfig.defaultPageSize;
    let pageStepIncrement = this.entitiesTableConfig.pageStepIncrement;
    let pageStepCount = this.entitiesTableConfig.pageStepCount;

    if (isDefined(pageSize) && isNumber(pageSize) && pageSize > 0) {
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

    if (this.entitiesTableConfig.useTimePageLink) {
      this.timewindow = this.entitiesTableConfig.defaultTimewindowInterval;
      const interval = getTimePageLinkInterval(this.timewindow);
      this.pageLink = new TimePageLink(10, 0, null, sortOrder,
        interval.startTime, interval.endTime);
    } else {
      this.pageLink = new PageLink(10, 0, null, sortOrder);
    }
    this.pageLink.pageSize = this.displayPagination ? this.defaultPageSize : MAX_SAFE_PAGE_SIZE;
    if (this.pageMode) {
      if (routerQueryParams.hasOwnProperty('page')) {
        this.pageLink.page = Number(routerQueryParams.page);
      }
      if (routerQueryParams.hasOwnProperty('pageSize')) {
        this.pageLink.pageSize = Number(routerQueryParams.pageSize);
      }
      const textSearchParam = routerQueryParams.textSearch;
      if (isNotEmptyStr(textSearchParam)) {
        const decodedTextSearch = decodeURI(textSearchParam);
        this.textSearchMode = true;
        this.pageLink.textSearch = decodedTextSearch.trim();
        this.textSearch.setValue(decodedTextSearch, {emitEvent: false});
      }
    }
    this.dataSource = this.entitiesTableConfig.dataSource(this.dataLoaded.bind(this));
    if (this.entitiesTableConfig.onLoadAction) {
      this.entitiesTableConfig.onLoadAction(this.route);
    }
    if (this.entitiesTableConfig.loadDataOnInit) {
      this.dataSource.loadEntities(this.pageLink);
    }
    if (this.viewInited) {
      setTimeout(() => {
        this.updatePaginationSubscriptions();
      }, 0);
    }
    if (this.pageMode) {
      if (initialAction) {
        const queryParams: PageQueryParam = {};
        this.router.navigate([], {
          relativeTo: this.route,
          queryParams,
          queryParamsHandling: '',
          replaceUrl: true
        });
      }
      if (initialAction === 'add') {
        setTimeout(() => {
          this.addEntity(null);
        }, 0);
      }
      if (initialAction === 'aiAssistant' && this.entitiesTableConfig.aiAssistantConfig) {
        setTimeout(() => {
          this.toggleAiAssistant(null);
        }, 0);
      }
    }
  }

  ngAfterViewInit() {
    this.textSearch.valueChanges.pipe(
      debounceTime(150),
      distinctUntilChanged((prev, current) => (this.pageLink.textSearch ?? '') === current.trim()),
      takeUntil(this.destroy$)
    ).subscribe(value => {
      if (this.pageMode) {
        const queryParams: PageQueryParam = {
          textSearch: isNotEmptyStr(value) ? encodeURI(value) : null,
          page: null
        };
        this.updatedRouterParamsAndData(queryParams);
      } else {
        this.pageLink.textSearch = isNotEmptyStr(value) ? value.trim() : null;
        if (this.displayPagination) {
          this.paginator.pageIndex = 0;
        }
        this.updateData();
      }
    });

    if (this.pageMode) {
      this.route.queryParams.pipe(
        skip(1),
        takeUntil(this.destroy$)
      ).subscribe((params: PageQueryParam) => {
        if (this.displayPagination) {
          this.paginator.pageIndex = Number(params.page) || 0;
          this.paginator.pageSize = Number(params.pageSize) || this.defaultPageSize;
        }
        this.sort.active = params.property || this.entitiesTableConfig.defaultSortOrder.property;
        this.sort.direction = (params.direction || this.entitiesTableConfig.defaultSortOrder.direction).toLowerCase() as SortDirection;
        const textSearchParam = params.textSearch;
        if (isNotEmptyStr(textSearchParam)) {
          const decodedTextSearch = decodeURI(textSearchParam);
          this.textSearchMode = true;
          this.pageLink.textSearch = decodedTextSearch.trim();
          this.textSearch.setValue(decodedTextSearch, {emitEvent: false});
        } else {
          this.pageLink.textSearch = null;
          this.textSearch.reset('', {emitEvent: false});
        }
        this.updateData();
      });
    }

    this.updatePaginationSubscriptions();
    this.viewInited = true;
  }

  private updatePaginationSubscriptions() {
    if (this.updateDataSubscription) {
      this.updateDataSubscription.unsubscribe();
      this.updateDataSubscription = null;
    }
    let paginatorSubscription$: Observable<object>;
    const sortSubscription$: Observable<object> = this.sort.sortChange.asObservable().pipe(
      map((data) => {
        const direction = data.direction.toUpperCase();
        const queryParams: PageQueryParam = {
          direction: (this.entitiesTableConfig?.defaultSortOrder?.direction === direction ? null : direction) as Direction,
          property: this.entitiesTableConfig?.defaultSortOrder?.property === data.active ? null : data.active
        };
        if (this.displayPagination) {
          queryParams.page = null;
          this.paginator.pageIndex = 0;
        }
        return queryParams;
      })
    );
    if (this.displayPagination) {
      paginatorSubscription$ = this.paginator.page.asObservable().pipe(
        map((data) => ({
          page: data.pageIndex === 0 ? null : data.pageIndex,
          pageSize: data.pageSize === this.defaultPageSize ? null : data.pageSize
        }))
      );
    }
    this.updateDataSubscription = ((this.displayPagination ? merge(sortSubscription$, paginatorSubscription$)
      : sortSubscription$) as Observable<PageQueryParam>).pipe(
      takeUntil(this.destroy$)
    ).subscribe(queryParams => this.updatedRouterParamsAndData(queryParams));
  }

  addEnabled() {
    return this.entitiesTableConfig.addEnabled;
  }

  clearSelection() {
    this.dataSource.selection.clear();
    this.cd.detectChanges();
  }

  updateData(closeDetails: boolean = true, reloadEntity: boolean = true) {
    if (closeDetails) {
      this.setEntityDetailsOpen(false);
    }
    if (this.displayPagination) {
      this.pageLink.page = this.paginator.pageIndex;
      this.pageLink.pageSize = this.paginator.pageSize;
    } else {
      this.pageLink.page = 0;
    }
    if (this.sort.active) {
      this.pageLink.sortOrder = {
        property: this.sort.active,
        direction: Direction[this.sort.direction.toUpperCase()]
      };
    } else {
      this.pageLink.sortOrder = null;
    }
    if (this.entitiesTableConfig.useTimePageLink) {
      const timePageLink = this.pageLink as TimePageLink;
      const interval = getTimePageLinkInterval(this.timewindow);
      timePageLink.startTime = interval.startTime;
      timePageLink.endTime = interval.endTime;
    }
    this.dataSource.loadEntities(this.pageLink);
    if (reloadEntity && this.isDetailsOpen && this.entityDetailsPanel) {
      this.entityDetailsPanel.reloadEntity();
    }
  }

  private dataLoaded(col?: number, row?: number) {
    if (isFinite(col) && isFinite(row)) {
      this.clearCellCache(col, row);
    } else {
      this.headerCellStyleCache.length = 0;
      this.cellContentCache.length = 0;
      this.cellTooltipCache.length = 0;
      this.cellStyleCache.length = 0;
    }
  }

  onRowClick($event: Event, entity) {
    if (!this.entitiesTableConfig.handleRowClick($event, entity)) {
      this.toggleEntityDetails($event, entity);
    }
  }

  toggleEntityDetails($event: Event, entity) {
    if ($event) {
      $event.stopPropagation();
      if (($event as MouseEvent).detail > 1) {
        return;
      }
    }
    const open = this.dataSource.toggleCurrentEntity(entity) ? true : !this.isDetailsOpen;
    this.setEntityDetailsOpen(open);
  }

  onCloseEntityDetails(): void {
    this.setEntityDetailsOpen(false);
  }

  onDetailsDrawerOpenedChange(opened: boolean): void {
    if (opened !== this.isDetailsOpen) {
      this.setEntityDetailsOpen(opened);
    }
  }

  private setEntityDetailsOpen(open: boolean): void {
    this.isDetailsOpen = open;
    if (this.entitiesTableConfig.detailsPanelEnabled) {
      this.panelService.setDetailsOpen(open);
    }
    this.updateAiClientContext();
    this.detailsPanelOpened.emit(open);
  }

  private updateAiClientContext(): void {
    const viewConfig = this.entitiesTableConfig?.aiAssistantConfig?.view;
    if (!viewConfig) {
      return;
    }
    const entity = this.isDetailsOpen ? this.dataSource.currentEntity : null;
    const view: AiChatView = (viewConfig.entityView && entity?.id)
      ? {type: viewConfig.entityView, entityId: entity.id as EntityId}
      : {type: viewConfig.listView, entityId: viewConfig.listEntityId};
    this.panelService.setClientContextForView(view);
  }

  toggleAiAssistant($event: Event) {
    $event?.stopPropagation();
    if (this.pageMode) {
      this.panelService.toggle();
    } else if (this.entitiesTableConfig.aiAssistantConfig?.pageUrl) {
      window.open(`${this.entitiesTableConfig.aiAssistantConfig.pageUrl}?action=aiAssistant`, '_blank');
    } else {
      window.open(`${baseDetailsPageByEntityType.get(this.entitiesTableConfig.entityType)}?action=aiAssistant`, '_blank');
    }
  }

  addEntity($event: Event) {
    let entity$: Observable<BaseData<HasId>>;
    if (this.entitiesTableConfig.addEntity) {
      entity$ = this.entitiesTableConfig.addEntity();
    } else {
      entity$ = this.dialog.open<AddEntityDialogComponent, AddEntityDialogData<BaseData<HasId>>,
                                 BaseData<HasId>>(AddEntityDialogComponent, {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: {
          entitiesTableConfig: this.entitiesTableConfig
        }
      }).afterClosed();
    }
    entity$.subscribe(
      (entity) => {
        if (entity) {
          this.updateData();
          this.entitiesTableConfig.entityAdded(entity);
        }
      }
    );
  }

  onEntityUpdated(entity: BaseData<HasId>) {
    this.updateData(false, false);
    this.entitiesTableConfig.entityUpdated(entity);
  }

  onEntityAction(action: EntityAction<BaseData<HasId>>) {
    if (action.action === 'delete') {
      this.deleteEntity(action.event, action.entity);
    }
  }

  deleteEntity($event: Event, entity: BaseData<HasId>) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialogService.confirm(
      this.entitiesTableConfig.deleteEntityTitle(entity),
      this.entitiesTableConfig.deleteEntityContent(entity),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe((result) => {
      if (result) {
        this.entitiesTableConfig.deleteEntity(entity.id).subscribe(
          () => {
            this.updateData();
            this.entitiesTableConfig.entitiesDeleted([entity.id]);
          }
        );
      }
    });
  }

  deleteEntities($event: Event, entities: BaseData<HasId>[]) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialogService.confirm(
      this.entitiesTableConfig.deleteEntitiesTitle(entities.length),
      this.entitiesTableConfig.deleteEntitiesContent(entities.length),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe((result) => {
      if (result) {
        const tasks: Observable<HasUUID>[] = [];
        entities.forEach((entity) => {
          if (this.entitiesTableConfig.deleteEnabled(entity)) {
            tasks.push(this.entitiesTableConfig.deleteEntity(entity.id).pipe(
              map(() => entity.id),
              catchError(() => of(null)
            )));
          }
        });
        forkJoin(tasks).subscribe(
          (ids) => {
            this.updateData();
            this.entitiesTableConfig.entitiesDeleted(ids.filter(id => id !== null));
          }
        );
      }
    });
  }

  onTimewindowChange() {
    if (this.displayPagination) {
      this.paginator.pageIndex = 0;
    }
    this.updateData();
  }

  enterFilterMode() {
    this.textSearchMode = true;
    setTimeout(() => {
      this.searchInputField.nativeElement.focus();
      this.searchInputField.nativeElement.setSelectionRange(0, 0);
    }, 10);
  }

  exitFilterMode() {
    this.textSearchMode = false;
    this.textSearch.reset();
  }

  resetSortAndFilter(update: boolean = true, preserveTimewindow: boolean = false) {
    this.textSearchMode = false;
    this.pageLink.textSearch = null;
    this.textSearch.reset('', {emitEvent: false});
    if (this.entitiesTableConfig.useTimePageLink && !preserveTimewindow) {
      this.timewindow = this.entitiesTableConfig.defaultTimewindowInterval;
    }
    if (this.displayPagination) {
      this.paginator.pageIndex = 0;
    }
    const sortable = this.sort.sortables.get(this.entitiesTableConfig.defaultSortOrder.property);
    this.sort.active = sortable.id;
    this.sort.direction = this.entitiesTableConfig.defaultSortOrder.direction === Direction.ASC ? 'asc' : 'desc';
    if (update) {
      this.updatedRouterParamsAndData({}, '');
    }
  }

  columnsUpdated(resetData: boolean = false) {
    this.entityColumns = this.entitiesTableConfig.columns.filter(
      (column) => column instanceof EntityTableColumn || column instanceof EntityLinkTableColumn ||
        column instanceof ChartEntityTableColumn || column instanceof ProgressBarEntityTableColumn || column instanceof EntityChipsEntityTableColumn);
    this.actionColumns = this.entitiesTableConfig.columns.filter(
      (column) => column instanceof EntityActionTableColumn)
      .map(column => column as EntityActionTableColumn<BaseData<HasId>>);

    this.displayedColumns = [];

    if (this.selectionEnabled) {
      this.displayedColumns.push('select');
    }
    this.entitiesTableConfig.columns.forEach(
      (column) => {
        this.displayedColumns.push(column.key);
      }
    );
    this.displayedColumns.push('actions');
    this.headerCellStyleCache.length = 0;
    this.cellContentCache.length = 0;
    this.cellTooltipCache.length = 0;
    this.cellStyleCache.length = 0;
    if (resetData) {
      this.dataSource.reset();
    }
  }

  cellActionDescriptorsUpdated() {
    this.cellActionDescriptors = [...this.entitiesTableConfig.cellActionDescriptors];
  }

  headerCellStyle(column: EntityColumnType) {
    const index = this.entitiesTableConfig.columns.indexOf(column as EntityColumn<BaseData<HasId>>);
    let res = this.headerCellStyleCache[index];
    if (!res) {
      const widthStyle: any = {width: column.width};
      if (column.width !== '0px') {
        widthStyle.minWidth = column.width;
        widthStyle.maxWidth = column.width;
      }
      if (column instanceof EntityTableColumn) {
        res = {...column.headerCellStyleFunction(column.key), ...widthStyle};
      } else {
        res = widthStyle;
      }
      this.headerCellStyleCache[index] = res;
    }
    return res;
  }

  clearCellCache(col: number, row: number) {
    const index = row * this.entitiesTableConfig.columns.length + col;
    this.cellContentCache[index] = undefined;
    this.cellTooltipCache[index] = undefined;
    this.cellStyleCache[index] = undefined;
  }

  cellContent(entity: BaseData<HasId>, column: EntityColumnType, row: number): any {
    if (column instanceof EntityTableColumn || column instanceof EntityLinkTableColumn) {
      const col = this.entitiesTableConfig.columns.indexOf(column);
      const index = row * this.entitiesTableConfig.columns.length + col;
      let res = this.cellContentCache[index];
      if (isUndefined(res)) {
        res = this.domSanitizer.bypassSecurityTrustHtml(column.cellContentFunction(entity, column.key));
        this.cellContentCache[index] = res;
      }
      return res;
    } else if (column instanceof ChartEntityTableColumn) {
      return column.cellContentFunction(entity, column.key);
    } else if (column instanceof ProgressBarEntityTableColumn) {
      return column.cellContentFunction(entity, column.key);
    }
    return '';
  }

  cellTooltip(entity: BaseData<HasId>, column: EntityColumnType, row: number) {
    if (column instanceof EntityTableColumn || column instanceof EntityLinkTableColumn) {
      const col = this.entitiesTableConfig.columns.indexOf(column);
      const index = row * this.entitiesTableConfig.columns.length + col;
      let res = this.cellTooltipCache[index];
      if (isUndefined(res)) {
        res = column.cellTooltipFunction(entity, column.key);
        res = isDefined(res) ? res : null;
        this.cellTooltipCache[index] = res;
      } else {
        return res !== null ? res : undefined;
      }
    } else {
      return undefined;
    }
  }

  cellStyle(entity: BaseData<HasId>, column: EntityColumnType, row: number) {
    const col = this.entitiesTableConfig.columns.indexOf(column as EntityColumn<BaseData<HasId>>);
    const index = row * this.entitiesTableConfig.columns.length + col;
    let res = this.cellStyleCache[index];
    if (!res) {
      const widthStyle: any = {width: column.width};
      if (column.width !== '0px') {
        widthStyle.minWidth = column.width;
        widthStyle.maxWidth = column.width;
      }
      if (column instanceof EntityTableColumn || column instanceof ProgressBarEntityTableColumn) {
        res = {...column.cellStyleFunction(entity, column.key), ...widthStyle};
      } else {
        res = widthStyle;
      }
      this.cellStyleCache[index] = res;
    }
    return res;
  }

  cellChartStyle(entity: BaseData<HasId>, column: EntityColumnType, row: number) {
    let res;
    if (column instanceof ChartEntityTableColumn) {
      res = column.chartStyleFunction(entity, column.key);
    }
    if (column instanceof ProgressBarEntityTableColumn) {
      res = column.progressBarStyleFunction(entity, column.key);
    }
    return res;
  }

  trackByEntityId(index: number, entity: BaseData<HasId>) {
    return entity.id.id;
  }

  protected updatedRouterParamsAndData(queryParams: object, queryParamsHandling: QueryParamsHandling = 'merge') {
    if (this.pageMode) {
      this.router.navigate([], {
        relativeTo: this.route,
        queryParams,
        queryParamsHandling
      });
      if (queryParamsHandling === '' && isEqual(this.route.snapshot.queryParams, queryParams)) {
        this.updateData();
      }
    } else {
      this.updateData();
    }
  }

  detectChanges() {
    this.cd.markForCheck();
  }
}
