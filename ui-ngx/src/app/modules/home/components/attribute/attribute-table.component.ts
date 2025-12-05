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
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  ElementRef,
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
import { PageLink } from '@shared/models/page/page-link';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { MatDialog } from '@angular/material/dialog';
import { DialogService } from '@core/services/dialog.service';
import { Direction, SortOrder } from '@shared/models/page/sort-order';
import { forkJoin, merge, Observable, Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';
import { EntityId } from '@shared/models/id/entity-id';
import {
  AttributeData,
  AttributeScope,
  DataKeyType,
  isClientSideTelemetryType,
  LatestTelemetry,
  TelemetryType,
  telemetryTypeTranslations,
  TimeseriesDeleteStrategy,
  toTelemetryType
} from '@shared/models/telemetry/telemetry.models';
import { AttributeDatasource } from '@home/models/datasource/attribute-datasource';
import { AttributeService } from '@app/core/http/attribute.service';
import { EntityType } from '@shared/models/entity-type.models';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import {
  AddAttributeDialogComponent,
  AddAttributeDialogData
} from '@home/components/attribute/add-attribute-dialog.component';
import { ConnectedPosition, Overlay, OverlayConfig, OverlayRef } from '@angular/cdk/overlay';
import {
  EDIT_ATTRIBUTE_VALUE_PANEL_DATA,
  EditAttributeValuePanelComponent,
  EditAttributeValuePanelData
} from './edit-attribute-value-panel.component';
import { ComponentPortal } from '@angular/cdk/portal';
import { TelemetryWebsocketService } from '@core/ws/telemetry-websocket.service';
import { WidgetsBundle } from '@shared/models/widgets-bundle.model';
import { DataKey, Datasource, DatasourceType, Widget, widgetType } from '@shared/models/widget.models';
import { IAliasController, IStateController, StateParams } from '@core/api/widget-api.models';
import { AliasController } from '@core/api/alias-controller';
import { EntityAlias, EntityAliases } from '@shared/models/alias.models';
import { UtilsService } from '@core/services/utils.service';
import { DashboardUtilsService } from '@core/services/dashboard-utils.service';
import { WidgetService } from '@core/http/widget.service';
import { toWidgetInfo } from '../../models/widget-component.models';
import { EntityService } from '@core/http/entity.service';
import {
  AddWidgetToDashboardDialogComponent,
  AddWidgetToDashboardDialogData
} from '@home/components/attribute/add-widget-to-dashboard-dialog.component';
import { deepClone } from '@core/utils';
import { Filters } from '@shared/models/query/query.models';
import { hidePageSizePixelValue } from '@shared/models/constants';
import { DeleteTimeseriesPanelComponent } from '@home/components/attribute/delete-timeseries-panel.component';
import { FormBuilder } from '@angular/forms';
import { AggregationType, defaultTimewindow } from '@shared/models/time/time.models';
import { TimeService } from '@core/services/time.service';


@Component({
  selector: 'tb-attribute-table',
  templateUrl: './attribute-table.component.html',
  styleUrls: ['./attribute-table.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AttributeTableComponent extends PageComponent implements AfterViewInit, OnInit, OnDestroy {

  telemetryTypeTranslationsMap = telemetryTypeTranslations;
  isClientSideTelemetryTypeMap = isClientSideTelemetryType;

  latestTelemetryTypes = LatestTelemetry;
  attributeScopeTypes = AttributeScope;

  mode: 'default' | 'widget' = 'default';

  attributeScopes: Array<string> = [];
  attributeScope: TelemetryType;
  toTelemetryTypeFunc = toTelemetryType;

  displayedColumns = ['select', 'lastUpdateTs', 'key', 'value'];
  pageLink: PageLink;
  textSearchMode = false;
  dataSource: AttributeDatasource;
  hidePageSize = false;

  activeValue = false;
  dirtyValue = false;
  entityIdValue: EntityId;

  attributeScopeSelectionReadonly = false;

  viewsInited = false;

  selectedWidgetsBundleAlias: string = null;
  widgetBundleSet = false;
  widgetsLoaded = false;
  widgetsCarouselIndex = 0;
  widgetsList: Array<Array<Widget>> = [];
  widgetsListCache: Array<Array<Widget>> = [];
  aliasController: IAliasController;
  private widgetDatasource: Datasource;

  private widgetResize$: ResizeObserver;

  private disableAttributeScopeSelectionValue: boolean;

  get disableAttributeScopeSelection(): boolean {
    return this.disableAttributeScopeSelectionValue;
  }

  @Input()
  set disableAttributeScopeSelection(value: boolean) {
    this.disableAttributeScopeSelectionValue = coerceBooleanProperty(value);
  }

  @Input()
  defaultAttributeScope: TelemetryType;

  @Input()
  set active(active: boolean) {
    if (this.activeValue !== active) {
      this.activeValue = active;
      if (this.activeValue && this.dirtyValue) {
        this.dirtyValue = false;
        if (this.viewsInited) {
          this.updateData(true);
        }
      }
    }
  }

  @Input()
  set entityId(entityId: EntityId) {
    if (this.entityIdValue !== entityId) {
      this.entityIdValue = entityId;
      this.resetSortAndFilter(this.activeValue);
      if (!this.activeValue) {
        this.dirtyValue = true;
      }
    }
  }

  @Input()
  entityName: string;

  @ViewChild('searchInput') searchInputField: ElementRef;

  @ViewChild(MatPaginator) paginator: MatPaginator;
  @ViewChild(MatSort) sort: MatSort;

  textSearch = this.fb.control('', {nonNullable: true});

  private destroy$ = new Subject<void>();

  constructor(protected store: Store<AppState>,
              private attributeService: AttributeService,
              private telemetryWsService: TelemetryWebsocketService,
              public translate: TranslateService,
              public dialog: MatDialog,
              private overlay: Overlay,
              private viewContainerRef: ViewContainerRef,
              private dialogService: DialogService,
              private entityService: EntityService,
              private utils: UtilsService,
              private dashboardUtils: DashboardUtilsService,
              private widgetService: WidgetService,
              private zone: NgZone,
              private cd: ChangeDetectorRef,
              private elementRef: ElementRef,
              private fb: FormBuilder,
              private timeService: TimeService) {
    super(store);
    this.dirtyValue = !this.activeValue;
    const sortOrder: SortOrder = { property: 'key', direction: Direction.ASC };
    this.pageLink = new PageLink(10, 0, null, sortOrder);
    this.dataSource = new AttributeDatasource(this.attributeService, this.telemetryWsService, this.zone, this.translate);
  }

  ngOnInit() {
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

  ngOnDestroy() {
    if (this.widgetResize$) {
      this.widgetResize$.disconnect();
    }
    this.destroy$.next();
    this.destroy$.complete();
  }

  attributeScopeChanged(attributeScope: TelemetryType) {
    this.attributeScope = attributeScope;
    this.mode = 'default';
    this.paginator.pageIndex = 0;
    this.updateData(true);
  }

  ngAfterViewInit() {
    this.textSearch.valueChanges.pipe(
      debounceTime(150),
      distinctUntilChanged((prev, current) => (this.pageLink.textSearch ?? '') === current.trim()),
      takeUntil(this.destroy$)
    ).subscribe((value) => {
      this.paginator.pageIndex = 0;
      this.pageLink.textSearch = value.trim();
      this.updateData();
    });

    this.sort.sortChange.pipe(takeUntil(this.destroy$)).subscribe(() => this.paginator.pageIndex = 0);

    merge(this.sort.sortChange, this.paginator.page).pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => this.updateData());

    this.viewsInited = true;
    if (this.activeValue && this.entityIdValue) {
      this.updateData(true);
    }
  }

  updateData(reload: boolean = false) {
    this.pageLink.page = this.paginator.pageIndex;
    this.pageLink.pageSize = this.paginator.pageSize;
    this.pageLink.sortOrder.property = this.sort.active;
    this.pageLink.sortOrder.direction = Direction[this.sort.direction.toUpperCase()];
    this.dataSource.loadAttributes(this.entityIdValue, this.attributeScope, this.pageLink, reload);
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

  resetSortAndFilter(update: boolean = true) {
    const entityType = this.entityIdValue.entityType;
    if (entityType === EntityType.DEVICE || entityType === EntityType.ENTITY_VIEW) {
      this.attributeScopes = Object.keys(AttributeScope);
      this.attributeScopeSelectionReadonly = false;
    } else {
      this.attributeScopes = [AttributeScope.SERVER_SCOPE];
      this.attributeScopeSelectionReadonly = true;
    }
    this.mode = 'default';
    this.textSearchMode = false;
    this.selectedWidgetsBundleAlias = null;
    this.attributeScope = this.defaultAttributeScope;
    this.pageLink.textSearch = null;
    this.textSearch.reset('', {emitEvent: false});
    if (this.viewsInited) {
      this.paginator.pageIndex = 0;
      const sortable = this.sort.sortables.get('key');
      this.sort.active = sortable.id;
      this.sort.direction = 'asc';
      if (update) {
        this.updateData(true);
      }
    }
  }

  reloadAttributes() {
    this.updateData(true);
  }

  addAttribute($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    const data: AddAttributeDialogData = {
      entityId: this.entityIdValue,
      attributeScope: this.attributeScope,
    };

    if(this.attributeScope === LatestTelemetry.LATEST_TELEMETRY) {
      data.datasource = this.dataSource;
    }

    this.dialog.open<AddAttributeDialogComponent, AddAttributeDialogData, boolean>(AddAttributeDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data
    }).afterClosed().subscribe(
      (res) => {
        if (res) {
          this.reloadAttributes();
        }
      }
    );
  }

  editAttribute($event: Event, attribute: AttributeData) {
    if ($event) {
      $event.stopPropagation();
    }
    if (this.isClientSideTelemetryTypeMap.get(this.attributeScope)) {
      return;
    }
    const target = $event.target || $event.currentTarget;
    const config = new OverlayConfig();
    config.backdropClass = 'cdk-overlay-transparent-backdrop';
    config.hasBackdrop = true;
    const connectedPosition: ConnectedPosition = {
      originX: 'end',
      originY: 'center',
      overlayX: 'end',
      overlayY: 'center'
    };
    config.positionStrategy = this.overlay.position().flexibleConnectedTo(target as HTMLElement)
      .withPositions([connectedPosition]);

    const overlayRef = this.overlay.create(config);
    overlayRef.backdropClick().subscribe(() => {
      overlayRef.dispose();
    });
    const providers: StaticProvider[] = [
      {
        provide: EDIT_ATTRIBUTE_VALUE_PANEL_DATA,
        useValue: {
          attributeValue: attribute.value
        } as EditAttributeValuePanelData
      },
      {
        provide: OverlayRef,
        useValue: overlayRef
      }
    ];
    const injector = Injector.create({parent: this.viewContainerRef.injector, providers});
    const componentRef = overlayRef.attach(new ComponentPortal(EditAttributeValuePanelComponent,
      this.viewContainerRef, injector));
    componentRef.onDestroy(() => {
      if (componentRef.instance.result !== null) {
        const attributeValue = componentRef.instance.result;
        const updatedAttribute = {...attribute};
        updatedAttribute.value = attributeValue;
        this.attributeService.saveEntityAttributes(this.entityIdValue,
          this.attributeScope as AttributeScope, [updatedAttribute]).subscribe(
          () => {
            this.reloadAttributes();
          }
        );
      }
    });
  }

  deleteTimeseries($event: Event, telemetry?: AttributeData) {
    if ($event) {
      $event.stopPropagation();
    }
    const target = $event.target || $event.currentTarget;
    const config = new OverlayConfig({
      panelClass: 'tb-filter-panel',
      backdropClass: 'cdk-overlay-transparent-backdrop',
      hasBackdrop: true,
      maxWidth: 488,
      width: '100%'
    });
    const connectedPosition: ConnectedPosition = {
      originX: 'start',
      originY: 'top',
      overlayX: 'end',
      overlayY: 'top'
    };
    config.positionStrategy = this.overlay.position().flexibleConnectedTo(target as HTMLElement)
      .withPositions([connectedPosition]);
    const overlayRef = this.overlay.create(config);
    overlayRef.backdropClick().subscribe(() => {
      overlayRef.dispose();
    });

    const providers: StaticProvider[] = [
      {
        provide: OverlayRef,
        useValue: overlayRef
      }
    ];
    const injector = Injector.create({parent: this.viewContainerRef.injector, providers});
    const componentRef = overlayRef.attach(new ComponentPortal(DeleteTimeseriesPanelComponent,
      this.viewContainerRef, injector));
    componentRef.onDestroy(() => {
      if (componentRef.instance.result !== null) {
        const result = componentRef.instance.result;
        const deleteTimeseries = telemetry ? [telemetry]: this.dataSource.selection.selected;
        const tasks: Observable<any>[] = [];
        let deleteAllDataForKeys = false;
        let rewriteLatestIfDeleted = false;
        let startTs = null;
        let endTs = null;
        let deleteLatest = true;
        switch (result.strategy) {
          case TimeseriesDeleteStrategy.DELETE_ALL_DATA:
            deleteAllDataForKeys = true;
            break;
          case TimeseriesDeleteStrategy.DELETE_ALL_DATA_EXCEPT_LATEST_VALUE:
            deleteAllDataForKeys = true;
            deleteLatest = false;
            break;
          case TimeseriesDeleteStrategy.DELETE_LATEST_VALUE:
            rewriteLatestIfDeleted = result.rewriteLatest;
            for (const ts of deleteTimeseries) {
              startTs = ts.lastUpdateTs;
              endTs = startTs + 1;
              tasks.push(this.attributeService.deleteEntityTimeseries(this.entityIdValue, [ts],
                deleteAllDataForKeys, startTs, endTs, rewriteLatestIfDeleted, deleteLatest));
            }
            break;
          case TimeseriesDeleteStrategy.DELETE_ALL_DATA_FOR_TIME_PERIOD:
            startTs = result.startDateTime.getTime();
            endTs = result.endDateTime.getTime();
            rewriteLatestIfDeleted = result.rewriteLatest;
            break;
        }
        if (tasks.length) {
          forkJoin(tasks).subscribe(() => this.reloadAttributes());
        } else {
          this.attributeService.deleteEntityTimeseries(this.entityIdValue, deleteTimeseries, deleteAllDataForKeys,
                                                        startTs, endTs, rewriteLatestIfDeleted, deleteLatest)
            .subscribe(() => this.reloadAttributes());
        }
      }
    });
  }

  deleteAttributes($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    if (this.dataSource.selection.selected.length > 0) {
      this.dialogService.confirm(
        this.translate.instant('attribute.delete-attributes-title', {count: this.dataSource.selection.selected.length}),
        this.translate.instant('attribute.delete-attributes-text'),
        this.translate.instant('action.no'),
        this.translate.instant('action.yes'),
        true
      ).subscribe((result) => {
        if (result) {
          this.attributeService.deleteEntityAttributes(this.entityIdValue,
            this.attributeScope as AttributeScope, this.dataSource.selection.selected).subscribe(
            () => {
              this.reloadAttributes();
            }
          );
        }
      });
    }
  }

  deleteTelemetry($event: Event) {
    if (this.attributeScope === this.latestTelemetryTypes.LATEST_TELEMETRY) {
      this.deleteTimeseries($event);
    } else {
      this.deleteAttributes($event);
    }
  }

  enterWidgetMode() {
    this.mode = 'widget';
    this.widgetsList = [];
    this.widgetsListCache = [];
    this.widgetsLoaded = false;
    this.widgetBundleSet = false;
    this.widgetsCarouselIndex = 0;
    this.selectedWidgetsBundleAlias = 'tables';

    const entityAlias: EntityAlias = {
      id: this.utils.guid(),
      alias: this.entityName,
      filter: this.dashboardUtils.createSingleEntityFilter(this.entityIdValue)
    };
    const entitiAliases: EntityAliases = {};
    entitiAliases[entityAlias.id] = entityAlias;

    // @ts-ignore
    const stateController: IStateController = {
      getStateParams: (): StateParams => ({})
    };

    const filters: Filters = {};

    this.aliasController = new AliasController(this.utils,
      this.entityService,
      this.translate,
      () => stateController, entitiAliases, filters);

    const dataKeyType: DataKeyType = this.attributeScope === LatestTelemetry.LATEST_TELEMETRY ?
      DataKeyType.timeseries : DataKeyType.attribute;

    this.widgetDatasource = {
      type: DatasourceType.entity,
      entityAliasId: entityAlias.id,
      dataKeys: []
    };

    for (let i = 0; i < this.dataSource.selection.selected.length; i++) {
      const attribute = this.dataSource.selection.selected[i];
      const dataKey: DataKey = {
        name: attribute.key,
        label: attribute.key,
        type: dataKeyType,
        color: this.utils.getMaterialColor(i),
        settings: {},
      };
      this.widgetDatasource.dataKeys.push(dataKey);
    }
  }

  onWidgetsCarouselIndexChanged() {
    if (this.mode === 'widget') {
      for (let i = 0; i < this.widgetsList.length; i++) {
        this.widgetsList[i].splice(0, this.widgetsList[i].length);
        if (i === this.widgetsCarouselIndex) {
          this.widgetsList[i].push(this.widgetsListCache[i][0]);
        }
      }
    }
  }

  onWidgetsBundleChanged(widgetsBundle: WidgetsBundle) {
    this.widgetBundleSet = !!widgetsBundle;
    if (this.mode === 'widget') {
      this.widgetsList = [];
      this.widgetsListCache = [];
      this.widgetsCarouselIndex = 0;
      if (widgetsBundle) {
        this.widgetsLoaded = false;
        this.widgetService.getBundleWidgetTypes(widgetsBundle.id.id).subscribe(
          (widgetTypes) => {
            widgetTypes = widgetTypes.filter(widget => !widget.deprecated).sort((a, b) => {
              let result = widgetType[b.descriptor.type].localeCompare(widgetType[a.descriptor.type]);
              if (result === 0) {
                result = b.createdTime - a.createdTime;
              }
              return result;
            });
            for (const type of widgetTypes) {
              const widgetInfo = toWidgetInfo(type);
              if (widgetInfo.type !== widgetType.static) {
                const sizeX = widgetInfo.sizeX * 2;
                const sizeY = widgetInfo.sizeY * 2;
                const col = Math.floor(Math.max(0, (20 - sizeX) / 2));
                const widget: Widget = {
                  typeFullFqn: widgetInfo.fullFqn,
                  type: widgetInfo.type,
                  sizeX,
                  sizeY,
                  row: 0,
                  col,
                  config: JSON.parse(widgetInfo.defaultConfig)
                };
                widget.config.title = widgetInfo.widgetName;
                widget.config.datasources = [this.widgetDatasource];
                if (widget.type === widgetType.timeseries && widget.config.useDashboardTimewindow) {
                  widget.config.useDashboardTimewindow = false;
                  widget.config.timewindow = defaultTimewindow(this.timeService);
                  widget.config.timewindow.aggregation.type = AggregationType.NONE;
                }
                if ((this.attributeScope === LatestTelemetry.LATEST_TELEMETRY && widgetInfo.type !== widgetType.rpc) ||
                      widgetInfo.type === widgetType.latest) {
                  const length = this.widgetsListCache.push([widget]);
                  this.widgetsList.push(length === 1 ? [widget] : []);
                }
              }
            }
            this.widgetsLoaded = true;
            this.cd.markForCheck();
          }
        );
      }
    }
  }

  addWidgetToDashboard() {
    if (this.mode === 'widget' && this.widgetsListCache.length > 0) {
      const widget = this.widgetsListCache[this.widgetsCarouselIndex][0];
      this.dialog.open<AddWidgetToDashboardDialogComponent, AddWidgetToDashboardDialogData>
        (AddWidgetToDashboardDialogComponent, {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: {
          entityId: this.entityIdValue,
          entityName: this.entityName,
          widget: deepClone(widget)
        }
      }).afterClosed();
    }
  }

  exitWidgetMode() {
    this.selectedWidgetsBundleAlias = null;
    this.mode = 'default';
  }

}
