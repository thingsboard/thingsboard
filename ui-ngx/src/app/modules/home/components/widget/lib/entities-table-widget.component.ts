///
/// Copyright © 2016-2019 The Thingsboard Authors
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

import { AfterViewInit, Component, ElementRef, Input, OnInit, ViewChild, ViewContainerRef } from '@angular/core';
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
import { IWidgetSubscription } from '@core/api/widget-api.models';
import { UtilsService } from '@core/services/utils.service';
import { TranslateService } from '@ngx-translate/core';
import { deepClone, isDefined, isNumber } from '@core/utils';
import * as tinycolor_ from 'tinycolor2';
import cssjs from '@core/css/css';
import { PageLink } from '@shared/models/page/page-link';
import { Direction, SortOrder, sortOrderFromString } from '@shared/models/page/sort-order';
import { DataSource } from '@angular/cdk/typings/collections';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { CollectionViewer } from '@angular/cdk/collections';
import { BehaviorSubject, fromEvent, merge, Observable, of } from 'rxjs';
import { emptyPageData, PageData } from '@shared/models/page/page-data';
import { EntityId } from '@shared/models/id/entity-id';
import { entityTypeTranslations } from '@shared/models/entity-type.models';
import { catchError, debounceTime, distinctUntilChanged, map, tap } from 'rxjs/operators';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import {
  CellContentInfo,
  CellStyleInfo,
  DisplayColumn,
  EntityColumn,
  EntityData,
  getEntityValue
} from '@home/components/widget/lib/table-widget.models';
import { ConnectedPosition, Overlay, OverlayConfig, OverlayRef } from '@angular/cdk/overlay';
import { ComponentPortal, PortalInjector } from '@angular/cdk/portal';
import {
  DISPLAY_COLUMNS_PANEL_DATA,
  DisplayColumnsPanelComponent,
  DisplayColumnsPanelData
} from '@home/components/widget/lib/display-columns-panel.component';

const tinycolor = tinycolor_;

interface EntitiesTableWidgetSettings {
  entitiesTitle: string;
  enableSearch: boolean;
  enableSelectColumnDisplay: boolean;
  displayEntityName: boolean;
  entityNameColumnTitle: string;
  displayEntityType: boolean;
  displayPagination: boolean;
  defaultPageSize: number;
  defaultSortOrder: string;
}

interface EntitiesTableDataKeySettings {
  columnWidth: string;
  useCellStyleFunction: boolean;
  cellStyleFunction: string;
  useCellContentFunction: boolean;
  cellContentFunction: string;
}

@Component({
  selector: 'tb-entities-table-widget',
  templateUrl: './entities-table-widget.component.html',
  styleUrls: ['./entities-table-widget.component.scss']
})
export class EntitiesTableWidgetComponent extends PageComponent implements OnInit, AfterViewInit {

  @Input()
  ctx: WidgetContext;

  @ViewChild('searchInput', {static: false}) searchInputField: ElementRef;
  @ViewChild(MatPaginator, {static: false}) paginator: MatPaginator;
  @ViewChild(MatSort, {static: false}) sort: MatSort;

  public displayPagination = true;
  public pageSizeOptions;
  public pageLink: PageLink;
  public textSearchMode = false;
  public columns: Array<EntityColumn> = [];
  public displayedColumns: string[] = [];
  public actionCellDescriptors: WidgetActionDescriptor[];
  public entityDatasource: EntityDatasource;

  private settings: EntitiesTableWidgetSettings;
  private widgetConfig: WidgetConfig;
  private subscription: IWidgetSubscription;

  private defaultPageSize = 10;
  private defaultSortOrder = 'entityName';

  private contentsInfo: {[key: string]: CellContentInfo} = {};
  private stylesInfo: {[key: string]: CellStyleInfo} = {};
  private columnWidth: {[key: string]: string} = {};

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
              private domSanitizer: DomSanitizer) {
    super(store);

    const sortOrder: SortOrder = sortOrderFromString(this.defaultSortOrder);
    this.pageLink = new PageLink(this.defaultPageSize, 0, null, sortOrder);
  }

  ngOnInit(): void {
    this.ctx.$scope.entitiesTableWidget = this;
    this.settings = this.ctx.settings;
    this.widgetConfig = this.ctx.widgetConfig;
    this.subscription = this.ctx.defaultSubscription;
    this.initializeConfig();
    this.updateDatasources();
    this.ctx.updateWidgetParams();
  }

  ngAfterViewInit(): void {
    fromEvent(this.searchInputField.nativeElement, 'keyup')
      .pipe(
        debounceTime(150),
        distinctUntilChanged(),
        tap(() => {
          if (this.displayPagination) {
            this.paginator.pageIndex = 0;
          }
          this.updateData();
        })
      )
      .subscribe();

    if (this.displayPagination) {
      this.sort.sortChange.subscribe(() => this.paginator.pageIndex = 0);
    }
    (this.displayPagination ? merge(this.sort.sortChange, this.paginator.page) : this.sort.sortChange)
      .pipe(
        tap(() => this.updateData())
      )
      .subscribe();
    this.updateData();
  }

  public onDataUpdated() {
    this.entityDatasource.updateEntitiesData(this.subscription.data);
    this.ctx.detectChanges();
  }

  private initializeConfig() {
    this.ctx.widgetActions = [this.searchAction, this.columnDisplayAction];

    let entitiesTitle: string;

    if (this.settings.entitiesTitle && this.settings.entitiesTitle.length) {
      entitiesTitle = this.utils.customTranslation(this.settings.entitiesTitle, this.settings.entitiesTitle);
    } else {
      entitiesTitle = this.translate.instant('entity.entities');
    }

    const datasource = this.subscription.datasources[0];
    this.ctx.widgetTitle = this.utils.createLabelFromDatasource(datasource, entitiesTitle);

    this.searchAction.show = isDefined(this.settings.enableSearch) ? this.settings.enableSearch : true;
    this.displayPagination = isDefined(this.settings.displayPagination) ? this.settings.displayPagination : true;
    this.columnDisplayAction.show = isDefined(this.settings.enableSelectColumnDisplay) ? this.settings.enableSelectColumnDisplay : true;

    const pageSize = this.settings.defaultPageSize;
    if (isDefined(pageSize) && isNumber(pageSize) && pageSize > 0) {
      this.defaultPageSize = pageSize;
    }
    this.pageSizeOptions = [this.defaultPageSize, this.defaultPageSize*2, this.defaultPageSize*3];

    if (this.settings.defaultSortOrder && this.settings.defaultSortOrder.length) {
      this.defaultSortOrder = this.settings.defaultSortOrder;
    }

    this.pageLink.pageSize = this.displayPagination ? this.defaultPageSize : Number.POSITIVE_INFINITY;
    this.pageLink.sortOrder = sortOrderFromString(this.defaultSortOrder);

    const origColor = this.widgetConfig.color || 'rgba(0, 0, 0, 0.87)';
    const origBackgroundColor = this.widgetConfig.backgroundColor || 'rgb(255, 255, 255)';
    const defaultColor = tinycolor(origColor);
    const mdDark = defaultColor.setAlpha(0.87).toRgbString();
    const mdDarkSecondary = defaultColor.setAlpha(0.54).toRgbString();
    const mdDarkDisabled = defaultColor.setAlpha(0.26).toRgbString();
    const mdDarkDivider = defaultColor.setAlpha(0.12).toRgbString();

    const cssString =
      '.mat-input-element::placeholder {\n' +
      '   color: ' + mdDarkSecondary + ';\n'+
      '}\n' +
      '.mat-input-element::-moz-placeholder {\n' +
      '   color: ' + mdDarkSecondary + ';\n'+
      '}\n' +
      '.mat-input-element::-webkit-input-placeholder {\n' +
      '   color: ' + mdDarkSecondary + ';\n'+
      '}\n' +
      '.mat-input-element:-ms-input-placeholder {\n' +
      '   color: ' + mdDarkSecondary + ';\n'+
      '}\n' +
      'mat-toolbar.mat-table-toolbar {\n'+
      'color: ' + mdDark + ';\n'+
      '}\n'+
      'mat-toolbar.mat-table-toolbar button.mat-icon-button mat-icon {\n'+
      'color: ' + mdDarkSecondary + ';\n'+
      '}\n'+
      '.mat-table .mat-header-row {\n'+
      'background-color: ' + origBackgroundColor + ';\n'+
      '}\n'+
      '.mat-table .mat-header-cell {\n'+
      'color: ' + mdDarkSecondary + ';\n'+
      '}\n'+
      '.mat-table .mat-header-cell .mat-sort-header-arrow {\n'+
      'color: ' + mdDarkDisabled + ';\n'+
      '}\n'+
      '.mat-table .mat-row, .mat-table .mat-header-row {\n'+
      'border-bottom-color: '+mdDarkDivider+';\n'+
      '}\n'+
      '.mat-table .mat-row:not(.tb-current-entity):not(:hover) .mat-cell.mat-table-sticky, .mat-table .mat-header-cell.mat-table-sticky {\n'+
      'background-color: ' + origBackgroundColor + ';\n'+
      '}\n'+
      '.mat-table .mat-cell {\n'+
      'color: ' + mdDark + ';\n'+
      '}\n'+
      '.mat-table .mat-cell button.mat-icon-button mat-icon {\n'+
      'color: ' + mdDarkSecondary + ';\n'+
      '}\n'+
      '.mat-divider {\n'+
      'border-top-color: ' + mdDarkDivider + ';\n'+
      '}\n'+
      '.mat-paginator {\n'+
      'color: ' + mdDarkSecondary + ';\n'+
      '}\n'+
      '.mat-paginator button.mat-icon-button {\n'+
      'color: ' + mdDarkSecondary + ';\n'+
      '}\n'+
      '.mat-paginator button.mat-icon-button[disabled][disabled] {\n'+
      'color: ' + mdDarkDisabled + ';\n'+
      '}\n'+
      '.mat-paginator .mat-select-value {\n'+
      'color: ' + mdDarkSecondary + ';\n'+
      '}';

    const cssParser = new cssjs();
    cssParser.testMode = false;
    const namespace = 'entities-table-' + this.utils.hashCode(cssString);
    cssParser.cssPreviewNamespace = namespace;
    cssParser.createStyleElement(namespace, cssString);
    $(this.elementRef.nativeElement).addClass(namespace);
  }

  private updateDatasources() {

    this.actionCellDescriptors = this.ctx.actionsApi.getActionDescriptors('actionCellButton');

    const displayEntityName = isDefined(this.settings.displayEntityName) ? this.settings.displayEntityName : true;
    let entityNameColumnTitle: string;
    if (this.settings.entityNameColumnTitle && this.settings.entityNameColumnTitle.length) {
      entityNameColumnTitle = this.utils.customTranslation(this.settings.entityNameColumnTitle, this.settings.entityNameColumnTitle);
    } else {
      entityNameColumnTitle = this.translate.instant('entity.entity-name');
    }
    const displayEntityType = isDefined(this.settings.displayEntityType) ? this.settings.displayEntityType : true;

    if (displayEntityName) {
      this.displayedColumns.push('entityName');
      this.columns.push(
        {
          name: 'entityName',
          label: 'entityName',
          title: entityNameColumnTitle
        } as EntityColumn
      );
      this.contentsInfo['entityName'] = {
        useCellContentFunction: false
      };
      this.stylesInfo['entityName'] = {
        useCellStyleFunction: false
      };
      this.columnWidth['entityName'] = '100px';
    }
    if (displayEntityType) {
      this.displayedColumns.push('entityType');
      this.columns.push(
        {
          name: 'entityType',
          label: 'entityType',
          title: this.translate.instant('entity.entity-type'),
        } as EntityColumn
      );
      this.contentsInfo['entityType'] = {
        useCellContentFunction: false
      };
      this.stylesInfo['entityType'] = {
        useCellStyleFunction: false
      };
      this.columnWidth['entityType'] = '100px';
    }

    const dataKeys: Array<DataKey> = [];

    const datasource = this.subscription.datasources[0];

    if (datasource) {
      datasource.dataKeys.forEach((_dataKey) => {
        const dataKey: EntityColumn = deepClone(_dataKey) as EntityColumn;
        if (dataKey.type === DataKeyType.function) {
          dataKey.name = dataKey.label;
        }
        dataKeys.push(dataKey);

        dataKey.title = this.utils.customTranslation(dataKey.label, dataKey.label);
        const keySettings: EntitiesTableDataKeySettings = dataKey.settings;

        let cellStyleFunction: Function = null;
        let useCellStyleFunction = false;

        if (keySettings.useCellStyleFunction === true) {
          if (isDefined(keySettings.cellStyleFunction) && keySettings.cellStyleFunction.length > 0) {
            try {
              cellStyleFunction = new Function('value', keySettings.cellStyleFunction);
              useCellStyleFunction = true;
            } catch (e) {
              cellStyleFunction = null;
              useCellStyleFunction = false;
            }
          }
        }
        this.stylesInfo[dataKey.label] = {
          useCellStyleFunction,
          cellStyleFunction
        };

        let cellContentFunction: Function = null;
        let useCellContentFunction = false;

        if (keySettings.useCellContentFunction === true) {
          if (isDefined(keySettings.cellContentFunction) && keySettings.cellContentFunction.length > 0) {
            try {
              cellContentFunction = new Function('value, entity, ctx', keySettings.cellContentFunction);
              useCellContentFunction = true;
            } catch (e) {
              cellContentFunction = null;
              useCellContentFunction = false;
            }
          }
        }

        this.contentsInfo[dataKey.label] = {
          useCellContentFunction,
          cellContentFunction,
          units: dataKey.units,
          decimals: dataKey.decimals
        };

        const columnWidth = isDefined(keySettings.columnWidth) ? keySettings.columnWidth : '0px';
        this.columnWidth[dataKey.label] = columnWidth;
        this.displayedColumns.push(dataKey.label);
        this.columns.push(dataKey);
      });
    }

    if (this.actionCellDescriptors.length) {
      this.displayedColumns.push('actions');
    }
    this.entityDatasource = new EntityDatasource(
      this.translate, dataKeys, this.subscription.datasources);
  }

  private editColumnsToDisplay($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    const target = $event.target || $event.srcElement || $event.currentTarget;
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

    const overlayRef = this.overlay.create(config);
    overlayRef.backdropClick().subscribe(() => {
      overlayRef.dispose();
    });

    const columns: DisplayColumn[] = this.columns.map(column => {
      return {
        title: column.title,
        label: column.label,
        display: this.displayedColumns.indexOf(column.label) > -1
      }
    });

    const injectionTokens = new WeakMap<any, any>([
      [DISPLAY_COLUMNS_PANEL_DATA, {
        columns,
        columnsUpdated: (newColumns) => {
          this.displayedColumns = newColumns.filter(column => column.display).map(column => column.label);
          this.displayedColumns.push('actions');
        }
      } as DisplayColumnsPanelData],
      [OverlayRef, overlayRef]
    ]);
    const injector = new PortalInjector(this.viewContainerRef.injector, injectionTokens);
    overlayRef.attach(new ComponentPortal(DisplayColumnsPanelComponent,
      this.viewContainerRef, injector));
    this.ctx.detectChanges();
  }

  private enterFilterMode() {
    this.textSearchMode = true;
    this.pageLink.textSearch = '';
    this.ctx.hideTitlePanel = true;
    this.ctx.detectChanges(true);
    setTimeout(() => {
      this.searchInputField.nativeElement.focus();
      this.searchInputField.nativeElement.setSelectionRange(0, 0);
    }, 10);
  }

  exitFilterMode() {
    this.textSearchMode = false;
    this.pageLink.textSearch = null;
    if (this.displayPagination) {
      this.paginator.pageIndex = 0;
    }
    this.updateData();
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
    this.pageLink.sortOrder.property = this.sort.active;
    this.pageLink.sortOrder.direction = Direction[this.sort.direction.toUpperCase()];
    this.entityDatasource.loadEntities(this.pageLink);
    this.ctx.detectChanges();
  }

  public trackByColumnLabel(index, column: EntityColumn) {
    return column.label;
  }

  public headerStyle(key: EntityColumn): any {
    return this.widthStyle(key);
  }

  public cellStyle(entity: EntityData, key: EntityColumn): any {
    let style: any = {};
    if (entity && key) {
      const styleInfo = this.stylesInfo[key.label];
      const value = getEntityValue(entity, key);
      if (styleInfo.useCellStyleFunction && styleInfo.cellStyleFunction) {
        try {
          style = styleInfo.cellStyleFunction(value);
        } catch (e) {
          style = {};
        }
      } else {
        style = this.defaultStyle(key, value);
      }
    }
    const widthStyle = this.widthStyle(key);
    style = {...style, ...widthStyle};
    return style;
  }

  private widthStyle(key: EntityColumn): any {
    let style: any = {};
    const columnWidth = this.columnWidth[key.label];
    if (columnWidth !== "0px") {
      style.minWidth = columnWidth;
      style.width = columnWidth;
    } else {
      style.minWidth = "auto";
      style.width = "auto";
    }
    return style;
  }

  public cellContent(entity: EntityData, key: EntityColumn): SafeHtml {
    let strContent = '';
    if (entity && key) {
      const contentInfo = this.contentsInfo[key.label];
      const value = getEntityValue(entity, key);
      if (contentInfo.useCellContentFunction && contentInfo.cellContentFunction) {
        if (isDefined(value)) {
          strContent = '' + value;
        }
        var content = strContent;
        try {
          content = contentInfo.cellContentFunction(value, entity, this.ctx);
        } catch (e) {
          content = strContent;
        }
      } else {
        const decimals = (contentInfo.decimals || contentInfo.decimals === 0) ? contentInfo.decimals : this.ctx.widgetConfig.decimals;
        const units = contentInfo.units || this.ctx.widgetConfig.units;
        content = this.ctx.utils.formatValue(value, decimals, units, true);
      }
      return this.domSanitizer.bypassSecurityTrustHtml(content);
    } else {
      return strContent;
    }
  }

  public onRowClick($event: Event, entity: EntityData) {
    if ($event) {
      $event.stopPropagation();
    }
    this.entityDatasource.toggleCurrentEntity(entity);
    const descriptors = this.ctx.actionsApi.getActionDescriptors('rowClick');
    if (descriptors.length) {
      let entityId;
      let entityName;
      if (entity) {
        entityId = entity.id;
        entityName = entity.entityName;
      }
      this.ctx.actionsApi.handleWidgetAction($event, descriptors[0], entityId, entityName);
    }
  }

  public onActionButtonClick($event: Event, entity: EntityData, actionDescriptor: WidgetActionDescriptor) {
    if ($event) {
      $event.stopPropagation();
    }
    let entityId;
    let entityName;
    if (entity) {
      entityId = entity.id;
      entityName = entity.entityName;
    }
    this.ctx.actionsApi.handleWidgetAction($event, actionDescriptor, entityId, entityName);
  }

  private defaultStyle(key: EntityColumn, value: any): any {
    return {};
  }

}



class EntityDatasource implements DataSource<EntityData> {

  private entitiesSubject = new BehaviorSubject<EntityData[]>([]);
  private pageDataSubject = new BehaviorSubject<PageData<EntityData>>(emptyPageData<EntityData>());

  private allEntities: Array<EntityData> = [];
  private allEntitiesSubject = new BehaviorSubject<EntityData[]>([]);
  private allEntities$: Observable<Array<EntityData>> = this.allEntitiesSubject.asObservable();

  private currentEntity: EntityData = null;

  constructor(
       private translate: TranslateService,
       private dataKeys: Array<DataKey>,
       datasources: Array<Datasource>
    ) {

    for (const datasource of datasources) {
      if (datasource.type === DatasourceType.entity && !datasource.entityId) {
        continue;
      }
      const entity: EntityData = {
        id: {} as EntityId,
        entityName: datasource.entityName
      };
      if (datasource.entityId) {
        entity.id.id = datasource.entityId;
      }
      if (datasource.entityType) {
        entity.id.entityType = datasource.entityType;
        entity.entityType = this.translate.instant(entityTypeTranslations.get(datasource.entityType).type);
      } else {
        entity.entityType = '';
      }
      this.dataKeys.forEach((dataKey) => {
        entity[dataKey.label] = '';
      });
      this.allEntities.push(entity);
    }
    this.allEntitiesSubject.next(this.allEntities);
  }

  connect(collectionViewer: CollectionViewer): Observable<EntityData[] | ReadonlyArray<EntityData>> {
    return this.entitiesSubject.asObservable();
  }

  disconnect(collectionViewer: CollectionViewer): void {
    this.entitiesSubject.complete();
    this.pageDataSubject.complete();
  }

  loadEntities(pageLink: PageLink) {
    this.fetchEntities(pageLink).pipe(
      catchError(() => of(emptyPageData<EntityData>())),
    ).subscribe(
      (pageData) => {
        this.entitiesSubject.next(pageData.data);
        this.pageDataSubject.next(pageData);
      }
    );
  }

  updateEntitiesData(data: DatasourceData[]) {
    for (let i = 0; i < this.allEntities.length; i++) {
      const entity = this.allEntities[i];
      for (let a = 0; a < this.dataKeys.length; a++) {
        const dataKey = this.dataKeys[a];
        const index = i * this.dataKeys.length + a;
        const keyData = data[index].data;
        if (keyData && keyData.length && keyData[0].length > 1) {
          const value = keyData[0][1];
          entity[dataKey.label] = value;
        } else {
          entity[dataKey.label] = '';
        }
      }
    }
    this.allEntitiesSubject.next(this.allEntities);
  }

  isEmpty(): Observable<boolean> {
    return this.entitiesSubject.pipe(
      map((entities) => !entities.length)
    );
  }

  total(): Observable<number> {
    return this.pageDataSubject.pipe(
      map((pageData) => pageData.totalElements)
    );
  }

  public toggleCurrentEntity(entity: EntityData): boolean {
    if (this.currentEntity !== entity) {
      this.currentEntity = entity;
      return true;
    } else {
      return false;
    }
  }

  public isCurrentEntity(entity: EntityData): boolean {
    return (this.currentEntity && entity && this.currentEntity.id && entity.id) &&
      (this.currentEntity.id.id === entity.id.id);
  }

  private fetchEntities(pageLink: PageLink): Observable<PageData<EntityData>> {
    return this.allEntities$.pipe(
      map((data) => pageLink.filterData(data))
    );
  }
}
