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

import { BaseData, HasId } from '@shared/models/base-data';
import { EntitiesDataSource, EntitiesFetchFunction } from '@home/models/datasource/entity-datasource';
import { Observable, of } from 'rxjs';
import { emptyPageData } from '@shared/models/page/page-data';
import { DatePipe } from '@angular/common';
import { Direction, SortOrder } from '@shared/models/page/sort-order';
import { EntityType, EntityTypeResource, EntityTypeTranslation } from '@shared/models/entity-type.models';
import { EntityComponent } from '@home/components/entity/entity.component';
import { Type } from '@angular/core';
import { EntityAction } from './entity-component.models';
import { HasUUID } from '@shared/models/id/has-uuid';
import { PageLink } from '@shared/models/page/page-link';
import { EntityTableHeaderComponent } from '@home/components/entity/entity-table-header.component';
import { ActivatedRoute } from '@angular/router';
import { EntityTabsComponent } from '../../components/entity/entity-tabs.component';
import { DAY, historyInterval } from '@shared/models/time/time.models';
import { IEntitiesTableComponent } from '@home/models/entity/entity-table-component.models';
import { IEntityDetailsPageComponent } from '@home/models/entity/entity-details-page-component.models';

export type EntityBooleanFunction<T extends BaseData<HasId>> = (entity: T) => boolean;
export type EntityStringFunction<T extends BaseData<HasId>> = (entity: T) => string;
export type EntityVoidFunction<T extends BaseData<HasId>> = (entity: T) => void;
export type EntityIdsVoidFunction<T extends BaseData<HasId>> = (ids: HasUUID[]) => void;
export type EntityCountStringFunction = (count: number) => string;
export type EntityTwoWayOperation<T extends BaseData<HasId>> = (entity: T, originalEntity?: T) => Observable<T>;
export type EntityByIdOperation<T extends BaseData<HasId>> = (id: HasUUID) => Observable<T>;
export type EntityIdOneWayOperation = (id: HasUUID) => Observable<any>;
export type EntityActionFunction<T extends BaseData<HasId>> = (action: EntityAction<T>) => boolean;
export type CreateEntityOperation<T extends BaseData<HasId>> = () => Observable<T>;
export type EntityRowClickFunction<T extends BaseData<HasId>> = (event: Event, entity: T) => boolean;

export type CellContentFunction<T extends BaseData<HasId>> = (entity: T, key: string) => string;
export type CellTooltipFunction<T extends BaseData<HasId>> = (entity: T, key: string) => string | undefined;
export type HeaderCellStyleFunction<T extends BaseData<HasId>> = (key: string) => object;
export type CellStyleFunction<T extends BaseData<HasId>> = (entity: T, key: string) => object;
export type CopyCellContent<T extends BaseData<HasId>> = (entity: T, key: string, length: number) => object;

export enum CellActionDescriptorType { 'DEFAULT', 'COPY_BUTTON'}

export interface CellActionDescriptor<T extends BaseData<HasId>> {
  name: string;
  nameFunction?: (entity: T) => string;
  icon?: string;
  iconFunction?: (entity: T) => string;
  style?: any;
  isEnabled: (entity: T) => boolean;
  onAction: ($event: MouseEvent, entity: T) => any;
  type?: CellActionDescriptorType;
}

export interface GroupActionDescriptor<T extends BaseData<HasId>> {
  name: string;
  icon: string;
  isEnabled: boolean;
  onAction: ($event: MouseEvent, entities: T[]) => void;
}

export interface HeaderActionDescriptor {
  name: string;
  icon: string;
  isEnabled: () => boolean;
  onAction: ($event: MouseEvent) => void;
}

export type EntityTableColumnType = 'content' | 'action' | 'link' | 'entityChips';

export class BaseEntityTableColumn<T extends BaseData<HasId>> {
  constructor(public type: EntityTableColumnType,
              public key: string,
              public title: string,
              public width: string = '0px',
              public sortable: boolean = true,
              public ignoreTranslate: boolean = false,
              public mobileHide: boolean = false) {
  }
}

export class EntityTableColumn<T extends BaseData<HasId>> extends BaseEntityTableColumn<T> {
  constructor(public key: string,
              public title: string,
              public width: string = '0px',
              public cellContentFunction: CellContentFunction<T> = (entity, property) => entity[property] ? entity[property] : '',
              public cellStyleFunction: CellStyleFunction<T> = () => ({}),
              public sortable: boolean = true,
              public headerCellStyleFunction: HeaderCellStyleFunction<T> = () => ({}),
              public cellTooltipFunction: CellTooltipFunction<T> = () => undefined,
              public isNumberColumn: boolean = false,
              public actionCell: CellActionDescriptor<T> = null) {
    super('content', key, title, width, sortable);
  }
}

export class EntityActionTableColumn<T extends BaseData<HasId>> extends BaseEntityTableColumn<T> {
  constructor(public key: string,
              public title: string,
              public actionDescriptor: CellActionDescriptor<T>,
              public width: string = '0px') {
    super('action', key, title, width, false);
  }
}

export class EntityLinkTableColumn<T extends BaseData<HasId>> extends BaseEntityTableColumn<T> {
  constructor(public key: string,
              public title: string,
              public width: string = '0px',
              public cellContentFunction: CellContentFunction<T> = (entity, property) => entity[property] ? entity[property] : '',
              public entityURL: (entity) => string,
              public sortable: boolean = true,
              public cellStyleFunction: CellStyleFunction<T> = () => ({}),
              public headerCellStyleFunction: HeaderCellStyleFunction<T> = () => ({}),
              public cellTooltipFunction: CellTooltipFunction<T> = () => undefined,
              public actionCell: CellActionDescriptor<T> = null) {
    super('link', key, title, width, sortable);
  }
}

export class DateEntityTableColumn<T extends BaseData<HasId>> extends EntityTableColumn<T> {
  constructor(key: string,
              title: string,
              datePipe: DatePipe,
              width: string = '0px',
              dateFormat: string = 'yyyy-MM-dd HH:mm:ss',
              cellStyleFunction: CellStyleFunction<T> = () => ({})) {
    super(key,
          title,
          width,
          (entity, property) => datePipe.transform(entity[property], dateFormat),
          cellStyleFunction);
  }
}

export class EntityChipsEntityTableColumn<T extends BaseData<HasId>> extends BaseEntityTableColumn<T> {
  constructor(public key: string,
              public title: string,
              public width: string = '0px',
              public entityURL?: (entity) => string) {
    super('entityChips', key, title, width, false);
  }
}

export type EntityColumn<T extends BaseData<HasId>> = EntityTableColumn<T> | EntityActionTableColumn<T> | EntityLinkTableColumn<T> | EntityChipsEntityTableColumn<T>;

export class EntityTableConfig<T extends BaseData<HasId>, P extends PageLink = PageLink, L extends BaseData<HasId> = T> {

  constructor() {}

  private table: IEntitiesTableComponent = null;
  private entityDetailsPage: IEntityDetailsPageComponent = null;

  componentsData: any = null;

  loadDataOnInit = true;
  onLoadAction: (route: ActivatedRoute) => void = null;
  useTimePageLink = false;
  forAllTimeEnabled = false;
  defaultTimewindowInterval = historyInterval(DAY);
  entityType: EntityType = null;
  tableTitle = '';
  selectionEnabled = true;
  searchEnabled = true;
  addEnabled = true;
  addAsTextButton = true;
  entitiesDeleteEnabled = true;
  detailsPanelEnabled = true;
  hideDetailsTabsOnEdit = true;
  rowPointer = false;
  actionsColumnTitle = null;
  entityTranslations: EntityTypeTranslation;
  entityResources: EntityTypeResource<T>;
  entityComponent: Type<EntityComponent<T, P, L>>;
  entityTabsComponent: Type<EntityTabsComponent<T, P, L>>;
  addDialogStyle = {};
  defaultSortOrder: SortOrder = {property: 'createdTime', direction: Direction.DESC};
  displayPagination = true;
  pageMode = true;
  defaultPageSize = 10;
  columns: Array<EntityColumn<L>> = [];
  cellActionDescriptors: Array<CellActionDescriptor<L>> = [];
  groupActionDescriptors: Array<GroupActionDescriptor<L>> = [];
  headerActionDescriptors: Array<HeaderActionDescriptor> = [];
  addActionDescriptors: Array<HeaderActionDescriptor> = [];
  headerComponent: Type<EntityTableHeaderComponent<T, P, L>>;
  addEntity: CreateEntityOperation<T> = null;
  dataSource: (dataLoadedFunction: (col?: number, row?: number) => void)
    => EntitiesDataSource<L> = (dataLoadedFunction: (col?: number, row?: number) => void) =>
    new EntitiesDataSource(this.entitiesFetchFunction, this.entitySelectionEnabled, dataLoadedFunction);
  detailsReadonly: EntityBooleanFunction<T> = () => false;
  entitySelectionEnabled: EntityBooleanFunction<L> = () => true;
  deleteEnabled: EntityBooleanFunction<T | L> = () => true;
  deleteEntityTitle: EntityStringFunction<L> = () => '';
  deleteEntityContent: EntityStringFunction<L> = () => '';
  deleteEntitiesTitle: EntityCountStringFunction = () => '';
  deleteEntitiesContent: EntityCountStringFunction = () => '';
  loadEntity: EntityByIdOperation<T | L> = () => of();
  saveEntity: EntityTwoWayOperation<T> = (entity, originalEntity) => of(entity);
  deleteEntity: EntityIdOneWayOperation = () => of();
  entitiesFetchFunction: EntitiesFetchFunction<L, P> = () => of(emptyPageData<L>());
  onEntityAction: EntityActionFunction<T> = () => false;
  handleRowClick: EntityRowClickFunction<L> = () => false;
  entityTitle: EntityStringFunction<T> = (entity) => entity?.name;
  entityAdded: EntityVoidFunction<T> = () => {};
  entityUpdated: EntityVoidFunction<T> = () => {};
  entitiesDeleted: EntityIdsVoidFunction<T> = () => {};

  getTable(): IEntitiesTableComponent {
    return this.table;
  }

  setTable(table: IEntitiesTableComponent) {
    this.table = table;
    this.entityDetailsPage = null;
  }

  getEntityDetailsPage(): IEntityDetailsPageComponent {
    return this.entityDetailsPage;
  }

  setEntityDetailsPage(entityDetailsPage: IEntityDetailsPageComponent) {
    this.entityDetailsPage = entityDetailsPage;
    this.table = null;
  }

  updateData(closeDetails = false, reloadEntity = true) {
    if (this.table) {
      this.table.updateData(closeDetails, reloadEntity);
    } else if (this.entityDetailsPage) {
      this.entityDetailsPage.reload();
    }
  }

  toggleEntityDetails($event: Event, entity: T) {
    if (this.table) {
      this.table.toggleEntityDetails($event, entity);
    }
  }

  isDetailsOpen(): boolean {
    if (this.table) {
      return this.table.isDetailsOpen;
    } else {
      return false;
    }
  }

  getActivatedRoute(): ActivatedRoute {
    if (this.table) {
      return this.table.route;
    } else {
      return null;
    }
  }
}

export const checkBoxCell =
  (value: boolean): string => `<mat-icon class="material-icons mat-icon">${value ? 'check_box' : 'check_box_outline_blank'}</mat-icon>`;
