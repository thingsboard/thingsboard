// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { EntityBooleanFunction } from '@home/models/entity/entities-table-config.models';
import {
  entityDataToShortEntityView,
  EntityGroupColumn,
  entityGroupColumnToEntityKey,
  groupEntitiesPageLinkToEntityDataPageLink,
  prepareEntityDataColumnMap,
  ShortEntityView
} from '@shared/models/entity-group.models';
import { EntitiesDataSource } from '@home/models/datasource/entity-datasource';
import { isDefined } from '@core/utils';
import { PageLink } from '@shared/models/page/page-link';
import { EntityDataCmd, TelemetrySubscriber } from '@shared/models/telemetry/telemetry.models';
import { NgZone } from '@angular/core';
import { CollectionViewer } from '@angular/cdk/collections';
import { EntityData, EntityKey, EntityKeyType } from '@shared/models/query/query.models';
import { AliasFilterType } from '@shared/models/alias.models';
import { EntityType } from '@shared/models/entity-type.models';
import { PageData } from '@shared/models/page/page-data';
import { Observable, ReplaySubject, Subject } from 'rxjs';
import { TelemetryWebsocketService } from '@core/ws/telemetry-websocket.service';

interface EntitiesUpdate {
  allEntities: Array<ShortEntityView>;
  updatedRows: Array<{ row: number; columns: number[] }>;
}

class GroupEntitiesSubscription {

  private pageDataSubject: Subject<PageData<ShortEntityView>>;
  private entitiesSubject: Subject<EntitiesUpdate> = new ReplaySubject();
  public entities$ = this.entitiesSubject.asObservable();

  private readonly columnsMap: {[entityKeyType: string]: EntityGroupColumn[]};
  private readonly columnKeyToEntityKeyMap: {[columnKey: string]: EntityKey};
  private readonly propertyToColumnIndexMap: {[property: string]: number[]};
  private readonly dataCommand: EntityDataCmd;
  private subscriber: TelemetrySubscriber;
  private entityIdToDataIndex: {[id: string]: number};
  private data: PageData<ShortEntityView>;

  constructor(private columns: EntityGroupColumn[],
              private entityGroupId: string,
              private groupType: EntityType,
              private telemetryService: TelemetryWebsocketService,
              private zone: NgZone) {
    this.columnsMap = prepareEntityDataColumnMap(columns);
    this.columnKeyToEntityKeyMap = {};
    this.propertyToColumnIndexMap = {};
    this.columns.forEach((column, index) => {
      this.columnKeyToEntityKeyMap[column.columnKey] = entityGroupColumnToEntityKey(column);
      let columnIndexes = this.propertyToColumnIndexMap[column.property];
      if (!columnIndexes) {
        columnIndexes = [];
        this.propertyToColumnIndexMap[column.property] = columnIndexes;
      }
      columnIndexes.push(index);
    });
    let entityFields: Array<EntityKey> = [];
    const entityFieldsColumns = this.columnsMap[EntityKeyType.ENTITY_FIELD];
    if (entityFieldsColumns) {
      entityFields = entityFieldsColumns.map(c => (entityGroupColumnToEntityKey(c)));
    }
    if (!entityFields.find(key => key.key === 'name')) {
      entityFields.push({
        type: EntityKeyType.ENTITY_FIELD,
        key: 'name'
      });
    }
    let latestValues: Array<EntityKey> = [];
    for (const entityKeyType of Object.keys(this.columnsMap)) {
      if (entityKeyType !== EntityKeyType.ENTITY_FIELD) {
        const typeColumns = this.columnsMap[entityKeyType];
        const entityKeys: Array<EntityKey> = typeColumns.map(c => (entityGroupColumnToEntityKey(c)));
        latestValues = latestValues.concat(entityKeys);
      }
    }
    this.dataCommand = new EntityDataCmd();
    this.dataCommand.query = {
      entityFilter: {
        type: AliasFilterType.entityGroup,
        groupType: this.groupType,
        entityGroup: this.entityGroupId
      },
      entityFields,
      latestValues,
      keyFilters: [],
      pageLink: null
    };
    this.dataCommand.latestCmd = {
      keys: latestValues
    };
  }

  public getEntityGroupEntities(pageLink: PageLink): Observable<PageData<ShortEntityView>> {
    this.doUnsubscribe();
    this.subscriber = new TelemetrySubscriber(this.telemetryService, this.zone);
    this.pageDataSubject = new ReplaySubject<PageData<ShortEntityView>>();
    this.dataCommand.query.pageLink = groupEntitiesPageLinkToEntityDataPageLink(pageLink, this.columnKeyToEntityKeyMap);
    this.subscriber.subscriptionCommands.push(this.dataCommand);
    this.subscriber.entityData$.subscribe(
      (entityDataUpdate) => {
        if (entityDataUpdate.data) {
          this.onPageData(entityDataUpdate.data);
        } else if (entityDataUpdate.update) {
          this.onDataUpdate(entityDataUpdate.update);
        }
      }
    );
    this.subscriber.subscribe();
    return this.pageDataSubject.asObservable();
  }

  public unsubscribe() {
    this.doUnsubscribe();
    this.entitiesSubject.complete();
  }

  private doUnsubscribe() {
    if (this.subscriber) {
      this.subscriber.unsubscribe();
    }
    if (this.pageDataSubject) {
      this.pageDataSubject.complete();
    }
    this.subscriber = null;
    this.pageDataSubject = null;
  }

  private onPageData(pageData: PageData<EntityData>) {
    const entities = pageData.data.map(e => entityDataToShortEntityView(e, this.columnsMap));
    this.data = {
      data: entities,
      totalElements: pageData.totalElements,
      hasNext: pageData.hasNext,
      totalPages: pageData.totalPages
    };
    this.entityIdToDataIndex = {};
    for (let dataIndex = 0; dataIndex < this.data.data.length; dataIndex++) {
      const entityData = this.data.data[dataIndex];
      this.entityIdToDataIndex[entityData.id.id] = dataIndex;
    }
    this.pageDataSubject.next(this.data);
  }

  private onDataUpdate(update: Array<EntityData>) {
    const updatedRows: Array<{ row: number; columns: number[] }> = [];
    for (const entityData of update) {
      const dataIndex = this.entityIdToDataIndex[entityData.entityId.id];
      if (isDefined(dataIndex) && dataIndex >= 0) {
        const entityUpdate = entityDataToShortEntityView(entityData, this.columnsMap, true);
        const existingEntity = this.data.data[dataIndex];
        const updatedColumns: number[] = [];
        for (const property of Object.keys(entityUpdate)) {
          if (property !== 'id' && property !== 'name') {
            const value = entityUpdate[property];
            if (existingEntity[property] !== value) {
              existingEntity[property] = value;
              updatedColumns.push(...this.propertyToColumnIndexMap[property]);
            }
          }
        }
        if (updatedColumns.length) {
          updatedRows.push({
            row: dataIndex,
            columns: updatedColumns
          });
        }
      }
    }
    this.entitiesSubject.next({allEntities: this.data.data, updatedRows});
  }

}

export class GroupEntitiesDataSource extends EntitiesDataSource<ShortEntityView> {

  private groupEntitiesSubscription: GroupEntitiesSubscription;

  constructor(private columns: EntityGroupColumn[],
              private entityGroupId: string,
              private groupType: EntityType,
              private telemetryService: TelemetryWebsocketService,
              private zone: NgZone,
              protected selectionEnabledFunction: EntityBooleanFunction<ShortEntityView>,
              protected dataLoadedFunction: (col?: number, row?: number) => void) {
    super(
      (pageLink => this.groupEntitiesSubscription.getEntityGroupEntities(pageLink)),
      selectionEnabledFunction,
      dataLoadedFunction
    );
    this.groupEntitiesSubscription = new GroupEntitiesSubscription(this.columns,
      this.entityGroupId, this.groupType, this.telemetryService, this.zone);

    this.groupEntitiesSubscription.entities$.subscribe(
      (entitiesUpdate) => {
        for (const row of entitiesUpdate.updatedRows) {
          row.columns.forEach((column) => {
            this.dataLoadedFunction(column, row.row);
          });
        }
        super.onEntities(entitiesUpdate.allEntities);
      }
    );
  }

  disconnect(collectionViewer: CollectionViewer): void {
    super.disconnect(collectionViewer);
    this.groupEntitiesSubscription.unsubscribe();
  }

  protected onEntities(entities: ShortEntityView[]) {
    super.onEntities(entities);
  }

}
