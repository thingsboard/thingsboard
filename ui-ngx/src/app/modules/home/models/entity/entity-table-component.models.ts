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

import { BaseData, HasId } from '@shared/models/base-data';
import { EntityTypeTranslation } from '@shared/models/entity-type.models';
import { SafeHtml } from '@angular/platform-browser';
import { PageLink } from '@shared/models/page/page-link';
import { Timewindow } from '@shared/models/time/time.models';
import { EntitiesDataSource } from '@home/models/datasource/entity-datasource';
import { ElementRef, EventEmitter, Renderer2, ViewContainerRef } from '@angular/core';
import { TbAnchorComponent } from '@shared/components/tb-anchor.component';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { EntityAction } from '@home/models/entity/entity-component.models';
import {
  CellActionDescriptor, EntityActionTableColumn, EntityColumn, EntityTableColumn,
  EntityTableConfig,
  GroupActionDescriptor,
  HeaderActionDescriptor
} from '@home/models/entity/entities-table-config.models';
import { ActivatedRoute } from '@angular/router';

export type EntitiesTableAction = 'add';

export interface IEntitiesTableComponent {
  entitiesTableConfig: EntityTableConfig<BaseData<HasId>>;
  translations: EntityTypeTranslation;
  headerActionDescriptors: Array<HeaderActionDescriptor>;
  groupActionDescriptors: Array<GroupActionDescriptor<BaseData<HasId>>>;
  cellActionDescriptors: Array<CellActionDescriptor<BaseData<HasId>>>;
  actionColumns: Array<EntityActionTableColumn<BaseData<HasId>>>;
  entityColumns: Array<EntityTableColumn<BaseData<HasId>>>;
  displayedColumns: string[];
  headerCellStyleCache: Array<any>;
  cellContentCache: Array<SafeHtml>;
  cellTooltipCache: Array<string>;
  cellStyleCache: Array<any>;
  selectionEnabled: boolean;
  defaultPageSize: number;
  displayPagination: boolean;
  pageSizeOptions: number[];
  pageLink: PageLink;
  pageMode: boolean;
  textSearchMode: boolean;
  timewindow: Timewindow;
  dataSource: EntitiesDataSource<BaseData<HasId>>;
  isDetailsOpen: boolean;
  detailsPanelOpened: EventEmitter<boolean>;
  entityTableHeaderAnchor: TbAnchorComponent;
  searchInputField: ElementRef;
  paginator: MatPaginator;
  sort: MatSort;
  route: ActivatedRoute;
  viewContainerRef: ViewContainerRef;

  addEnabled(): boolean;
  clearSelection(): void;
  updateData(closeDetails?: boolean, reloadEntity?: boolean): void;
  onRowClick($event: Event, entity): void;
  toggleEntityDetails($event: Event, entity);
  addEntity($event: Event): void;
  onEntityUpdated(entity: BaseData<HasId>): void;
  onEntityAction(action: EntityAction<BaseData<HasId>>): void;
  deleteEntity($event: Event, entity: BaseData<HasId>): void;
  deleteEntities($event: Event, entities: BaseData<HasId>[]): void;
  onTimewindowChange(): void;
  enterFilterMode(): void;
  exitFilterMode(): void;
  resetSortAndFilter(update?: boolean, preserveTimewindow?: boolean): void;
  columnsUpdated(resetData?: boolean): void;
  cellActionDescriptorsUpdated(): void;
  headerCellStyle(column: EntityColumn<BaseData<HasId>>): any;
  clearCellCache(col: number, row: number): void;
  cellContent(entity: BaseData<HasId>, column: EntityColumn<BaseData<HasId>>, row: number): any;
  cellTooltip(entity: BaseData<HasId>, column: EntityColumn<BaseData<HasId>>, row: number): string;
  cellStyle(entity: BaseData<HasId>, column: EntityColumn<BaseData<HasId>>, row: number): any;
  trackByColumnKey(index, column: EntityTableColumn<BaseData<HasId>>): string;
  trackByEntityId(index: number, entity: BaseData<HasId>): string;
  detectChanges(): void;
}
