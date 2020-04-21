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
import { EntitiesTableComponent } from '@home/components/entity/entities-table.component';
import { EntityTableHeaderComponent } from '@home/components/entity/entity-table-header.component';
import { ActivatedRoute } from '@angular/router';
import { EntityTabsComponent } from '../../components/entity/entity-tabs.component';

export type EntityBooleanFunction<T extends BaseData<HasId>> = (entity: T) => boolean;
export type EntityStringFunction<T extends BaseData<HasId>> = (entity: T) => string;
export type EntityVoidFunction<T extends BaseData<HasId>> = (entity: T) => void;
export type EntityIdsVoidFunction<T extends BaseData<HasId>> = (ids: HasUUID[]) => void;
export type EntityCountStringFunction = (count: number) => string;
export type EntityTwoWayOperation<T extends BaseData<HasId>> = (entity: T) => Observable<T>;
export type EntityByIdOperation<T extends BaseData<HasId>> = (id: HasUUID) => Observable<T>;
export type EntityIdOneWayOperation = (id: HasUUID) => Observable<any>;
export type EntityActionFunction<T extends BaseData<HasId>> = (action: EntityAction<T>) => boolean;
export type CreateEntityOperation<T extends BaseData<HasId>> = () => Observable<T>;
export type EntityRowClickFunction<T extends BaseData<HasId>> = (event: Event, entity: T) => boolean;

export type CellContentFunction<T extends BaseData<HasId>> = (entity: T, key: string) => string;
export type CellTooltipFunction<T extends BaseData<HasId>> = (entity: T, key: string) => string | undefined;
export type HeaderCellStyleFunction<T extends BaseData<HasId>> = (key: string) => object;
export type CellStyleFunction<T extends BaseData<HasId>> = (entity: T, key: string) => object;

export interface CellActionDescriptor<T extends BaseData<HasId>> {
  name: string;
  nameFunction?: (entity: T) => string;
  icon?: string;
  mdiIcon?: string;
  style?: any;
  isEnabled: (entity: T) => boolean;
  onAction: ($event: MouseEvent, entity: T) => void;
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

export type EntityTableColumnType = 'content' | 'action';

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
              public isNumberColumn: boolean = false) {
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

export type EntityColumn<T extends BaseData<HasId>> = EntityTableColumn<T> | EntityActionTableColumn<T>;

export class EntityTableConfig<T extends BaseData<HasId>, P extends PageLink = PageLink, L extends BaseData<HasId> = T> {

  constructor() {}

  componentsData: any = null;

  loadDataOnInit = true;
  onLoadAction: (route: ActivatedRoute) => void = null;
  table: EntitiesTableComponent = null;
  useTimePageLink = false;
  entityType: EntityType = null;
  tableTitle = '';
  selectionEnabled = true;
  searchEnabled = true;
  addEnabled = true;
  entitiesDeleteEnabled = true;
  detailsPanelEnabled = true;
  hideDetailsTabsOnEdit = true;
  actionsColumnTitle = null;
  entityTranslations: EntityTypeTranslation;
  entityResources: EntityTypeResource<T>;
  entityComponent: Type<EntityComponent<T, P, L>>;
  entityTabsComponent: Type<EntityTabsComponent<T, P, L>>;
  addDialogStyle = {};
  defaultSortOrder: SortOrder = {property: 'createdTime', direction: Direction.ASC};
  displayPagination = true;
  defaultPageSize = 10;
  columns: Array<EntityColumn<L>> = [];
  cellActionDescriptors: Array<CellActionDescriptor<L>> = [];
  groupActionDescriptors: Array<GroupActionDescriptor<L>> = [];
  headerActionDescriptors: Array<HeaderActionDescriptor> = [];
  addActionDescriptors: Array<HeaderActionDescriptor> = [];
  headerComponent: Type<EntityTableHeaderComponent<T, P, L>>;
  addEntity: CreateEntityOperation<T> = null;
  dataSource: (dataLoadedFunction: (col?: number, row?: number) => void)
    => EntitiesDataSource<L> = (dataLoadedFunction: (col?: number, row?: number) => void) => {
    return new EntitiesDataSource(this.entitiesFetchFunction, this.entitySelectionEnabled, dataLoadedFunction);
  };
  detailsReadonly: EntityBooleanFunction<T> = () => false;
  entitySelectionEnabled: EntityBooleanFunction<L> = () => true;
  deleteEnabled: EntityBooleanFunction<T | L> = () => true;
  deleteEntityTitle: EntityStringFunction<L> = () => '';
  deleteEntityContent: EntityStringFunction<L> = () => '';
  deleteEntitiesTitle: EntityCountStringFunction = () => '';
  deleteEntitiesContent: EntityCountStringFunction = () => '';
  loadEntity: EntityByIdOperation<T> = () => of();
  saveEntity: EntityTwoWayOperation<T> = (entity) => of(entity);
  deleteEntity: EntityIdOneWayOperation = () => of();
  entitiesFetchFunction: EntitiesFetchFunction<L, P> = () => of(emptyPageData<L>());
  onEntityAction: EntityActionFunction<T> = () => false;
  handleRowClick: EntityRowClickFunction<L> = () => false;
  entityTitle: EntityStringFunction<T> = (entity) => entity?.name;
  entityAdded: EntityVoidFunction<T> = () => {};
  entityUpdated: EntityVoidFunction<T> = () => {};
  entitiesDeleted: EntityIdsVoidFunction<T> = () => {};
}

export function checkBoxCell(value: boolean): string {
  return `<mat-icon class="material-icons mat-icon">${value ? 'check_box' : 'check_box_outline_blank'}</mat-icon>`;
}
