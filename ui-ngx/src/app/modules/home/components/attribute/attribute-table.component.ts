///
/// Copyright © 2016-2020 The Thingsboard Authors
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
  Component,
  ElementRef,
  Input,
  NgZone,
  OnInit,
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
import { fromEvent, merge } from 'rxjs';
import { debounceTime, distinctUntilChanged, tap } from 'rxjs/operators';
import { EntityId } from '@shared/models/id/entity-id';
import {
  AttributeData,
  AttributeScope,
  DataKeyType,
  isClientSideTelemetryType,
  LatestTelemetry,
  TelemetryType,
  telemetryTypeTranslations,
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
import { ComponentPortal, PortalInjector } from '@angular/cdk/portal';
import { TelemetryWebsocketService } from '@core/ws/telemetry-websocket.service';
import { WidgetsBundle } from '@shared/models/widgets-bundle.model';
import { DataKey, Datasource, DatasourceType, Widget, widgetType } from '@shared/models/widget.models';
import { IAliasController, IStateController, StateParams } from '@core/api/widget-api.models';
import { AliasController } from '@core/api/alias-controller';
import { EntityAlias, EntityAliases } from '@shared/models/alias.models';
import { UtilsService } from '@core/services/utils.service';
import { DashboardUtilsService } from '@core/services/dashboard-utils.service';
import { NULL_UUID } from '@shared/models/id/has-uuid';
import { WidgetService } from '@core/http/widget.service';
import { toWidgetInfo } from '../../models/widget-component.models';
import { EntityService } from '@core/http/entity.service';
import {
  AddWidgetToDashboardDialogComponent,
  AddWidgetToDashboardDialogData
} from '@home/components/attribute/add-widget-to-dashboard-dialog.component';
import { deepClone } from '@core/utils';
import { Filters } from '@shared/models/query/query.models';


@Component({
  selector: 'tb-attribute-table',
  templateUrl: './attribute-table.component.html',
  styleUrls: ['./attribute-table.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AttributeTableComponent extends PageComponent implements AfterViewInit, OnInit {

  telemetryTypeTranslationsMap = telemetryTypeTranslations;
  isClientSideTelemetryTypeMap = isClientSideTelemetryType;

  latestTelemetryTypes = LatestTelemetry;

  mode: 'default' | 'widget' = 'default';

  attributeScopes: Array<string> = [];
  attributeScope: TelemetryType;
  toTelemetryTypeFunc = toTelemetryType;

  displayedColumns = ['select', 'lastUpdateTs', 'key', 'value'];
  pageLink: PageLink;
  textSearchMode = false;
  dataSource: AttributeDatasource;

  activeValue = false;
  dirtyValue = false;
  entityIdValue: EntityId;

  attributeScopeSelectionReadonly = false;

  viewsInited = false;

  selectedWidgetsBundleAlias: string = null;
  widgetsBundle: WidgetsBundle = null;
  widgetsLoaded = false;
  widgetsCarouselIndex = 0;
  widgetsList: Array<Array<Widget>> = [];
  widgetsListCache: Array<Array<Widget>> = [];
  aliasController: IAliasController;
  private widgetDatasource: Datasource;

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
              private zone: NgZone) {
    super(store);
    this.dirtyValue = !this.activeValue;
    const sortOrder: SortOrder = { property: 'key', direction: Direction.ASC };
    this.pageLink = new PageLink(10, 0, null, sortOrder);
    this.dataSource = new AttributeDatasource(this.attributeService, this.telemetryWsService, this.zone, this.translate);
  }

  ngOnInit() {
  }

  attributeScopeChanged(attributeScope: TelemetryType) {
    this.attributeScope = attributeScope;
    this.mode = 'default';
    this.updateData(true);
  }

  ngAfterViewInit() {

    fromEvent(this.searchInputField.nativeElement, 'keyup')
      .pipe(
        debounceTime(150),
        distinctUntilChanged(),
        tap(() => {
          this.paginator.pageIndex = 0;
          this.updateData();
        })
      )
      .subscribe();

    this.sort.sortChange.subscribe(() => this.paginator.pageIndex = 0);

    merge(this.sort.sortChange, this.paginator.page)
      .pipe(
        tap(() => this.updateData())
      )
      .subscribe();

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
    this.pageLink.textSearch = '';
    setTimeout(() => {
      this.searchInputField.nativeElement.focus();
      this.searchInputField.nativeElement.setSelectionRange(0, 0);
    }, 10);
  }

  exitFilterMode() {
    this.textSearchMode = false;
    this.pageLink.textSearch = null;
    this.paginator.pageIndex = 0;
    this.updateData();
  }

  resetSortAndFilter(update: boolean = true) {
    const entityType = this.entityIdValue.entityType;
    if (entityType === EntityType.DEVICE || entityType === EntityType.ENTITY_VIEW || entityType === EntityType.EDGE) {
      this.attributeScopes = Object.keys(AttributeScope);
      this.attributeScopeSelectionReadonly = false;
    } else {
      this.attributeScopes = [AttributeScope.SERVER_SCOPE];
      this.attributeScopeSelectionReadonly = true;
    }
    this.mode = 'default';
    this.attributeScope = this.defaultAttributeScope;
    this.pageLink.textSearch = null;
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
    this.dialog.open<AddAttributeDialogComponent, AddAttributeDialogData, boolean>(AddAttributeDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        entityId: this.entityIdValue,
        attributeScope: this.attributeScope as AttributeScope
      }
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
    const target = $event.target || $event.srcElement || $event.currentTarget;
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
    const injectionTokens = new WeakMap<any, any>([
      [EDIT_ATTRIBUTE_VALUE_PANEL_DATA, {
        attributeValue: attribute.value
      } as EditAttributeValuePanelData],
      [OverlayRef, overlayRef]
    ]);
    const injector = new PortalInjector(this.viewContainerRef.injector, injectionTokens);
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

  enterWidgetMode() {
    this.mode = 'widget';
    this.widgetsList = [];
    this.widgetsListCache = [];
    this.widgetsLoaded = false;
    this.widgetsCarouselIndex = 0;
    this.widgetsBundle = null;
    this.selectedWidgetsBundleAlias = 'cards';

    const entityAlias: EntityAlias = {
      id: this.utils.guid(),
      alias: this.entityName,
      filter: this.dashboardUtils.createSingleEntityFilter(this.entityIdValue)
    };
    const entitiAliases: EntityAliases = {};
    entitiAliases[entityAlias.id] = entityAlias;

    // @ts-ignore
    const stateController: IStateController = {
      getStateParams(): StateParams {
        return {};
      }
    };

    const filters: Filters = {};

    this.aliasController = new AliasController(this.utils,
      this.entityService,
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
        _hash: Math.random()
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

  onWidgetsBundleChanged() {
    if (this.mode === 'widget') {
      this.widgetsList = [];
      this.widgetsListCache = [];
      this.widgetsCarouselIndex = 0;
      if (this.widgetsBundle) {
        this.widgetsLoaded = false;
        const bundleAlias = this.widgetsBundle.alias;
        const isSystem = this.widgetsBundle.tenantId.id === NULL_UUID;
        this.widgetService.getBundleWidgetTypes(bundleAlias, isSystem).subscribe(
          (widgetTypes) => {
            widgetTypes = widgetTypes.sort((a, b) => {
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
                  isSystemType: isSystem,
                  bundleAlias,
                  typeAlias: widgetInfo.alias,
                  type: widgetInfo.type,
                  title: widgetInfo.widgetName,
                  sizeX,
                  sizeY,
                  row: 0,
                  col,
                  config: JSON.parse(widgetInfo.defaultConfig)
                };
                widget.config.title = widgetInfo.widgetName;
                widget.config.datasources = [this.widgetDatasource];
                if ((this.attributeScope === LatestTelemetry.LATEST_TELEMETRY && widgetInfo.type !== widgetType.rpc) ||
                      widgetInfo.type === widgetType.latest) {
                  const length = this.widgetsListCache.push([widget]);
                  this.widgetsList.push(length === 1 ? [widget] : []);
                }
              }
            }
            this.widgetsLoaded = true;
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
