///
/// Copyright © 2016-2021 The Thingsboard Authors
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
  DateEntityTableColumn,
  EntityActionTableColumn,
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import {
  EdgeEvent,
  edgeEventActionTypeTranslations,
  EdgeEventStatus,
  edgeEventStatusColor,
  EdgeEventType,
  edgeEventTypeTranslations
} from '@shared/models/edge.models';
import { TimePageLink } from '@shared/models/page/page-link';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { MatDialog } from '@angular/material/dialog';
import { EntityId } from '@shared/models/id/entity-id';
import { EntityTypeResource } from '@shared/models/entity-type.models';
import { Observable, of } from 'rxjs';
import { PageData } from '@shared/models/page/page-data';
import { Direction } from '@shared/models/page/sort-order';
import { DialogService } from '@core/services/dialog.service';
import { ContentType } from '@shared/models/constants';
import {
  EventContentDialogComponent,
  EventContentDialogData
} from '@home/components/event/event-content-dialog.component';
import { RuleChainService } from '@core/http/rule-chain.service';
import { AttributeService } from '@core/http/attribute.service';
import { AttributeScope } from '@shared/models/telemetry/telemetry.models';
import { EdgeDownlinkTableHeaderComponent } from '@home/components/edge/edge-downlink-table-header.component';
import { EdgeService } from '@core/http/edge.service';
import { map } from 'rxjs/operators';
import { EntityService } from "@core/http/entity.service";

export class EdgeDownlinkTableConfig extends EntityTableConfig<EdgeEvent, TimePageLink> {

  queueStartTs: number;

  constructor(private edgeService: EdgeService,
              private dialogService: DialogService,
              private translate: TranslateService,
              private entityService: EntityService,
              private ruleChainService: RuleChainService,
              private attributeService: AttributeService,
              private datePipe: DatePipe,
              private dialog: MatDialog,
              public entityId: EntityId,
              public tenantId: string) {
    super();
    this.loadDataOnInit = false;
    this.tableTitle = '';
    this.useTimePageLink = true;
    this.detailsPanelEnabled = false;
    this.selectionEnabled = false;
    this.searchEnabled = false;
    this.addEnabled = false;
    this.entitiesDeleteEnabled = false;

    this.headerComponent = EdgeDownlinkTableHeaderComponent;

    this.entityTranslations = {
      noEntities: 'edge.no-downlinks-prompt'
    };
    this.entityResources = {} as EntityTypeResource<EdgeEvent>;
    this.entitiesFetchFunction = pageLink => this.fetchEvents(pageLink);

    this.defaultSortOrder = {property: 'createdTime', direction: Direction.DESC};

    this.updateColumns();
  }

  fetchEvents(pageLink: TimePageLink): Observable<PageData<EdgeEvent>> {
    this.loadEdgeInfo();
    return this.edgeService.getEdgeEvents(this.entityId, pageLink);
  }

  loadEdgeInfo() {
    this.attributeService.getEntityAttributes(this.entityId, AttributeScope.SERVER_SCOPE, ['queueStartTs'])
      .subscribe(
        attributes => this.onUpdate(attributes)
      );
  }

  onUpdate(attributes) {
    this.queueStartTs = 0;
    let edge = attributes.reduce(function (map, attribute) {
      map[attribute.key] = attribute;
      return map;
    }, {});
    if (edge.queueStartTs) {
      this.queueStartTs = edge.queueStartTs.lastUpdateTs;
    }
  }

  updateColumns(updateTableColumns: boolean = false): void {
    this.columns = [];
    this.columns.push(
      new DateEntityTableColumn<EdgeEvent>('createdTime', 'event.event-time', this.datePipe, '120px'),
      new EntityTableColumn<EdgeEvent>('type', 'event.type', '25%',
        entity => this.translate.instant(edgeEventTypeTranslations.get(entity.type)), entity => ({}), false),
      new EntityTableColumn<EdgeEvent>('action', 'edge.event-action', '25%',
        entity => this.translate.instant(edgeEventActionTypeTranslations.get(entity.action)), entity => ({}), false),
      new EntityTableColumn<EdgeEvent>('entityId', 'edge.entity-id', '40%',
        (entity) => entity.entityId, entity => ({}), false),
      new EntityTableColumn<EdgeEvent>('status', 'event.status', '10%',
        (entity) => this.updateEdgeEventStatus(entity.createdTime),
        entity => ({
          color: this.isPending(entity.createdTime) ? edgeEventStatusColor.get(EdgeEventStatus.PENDING) : edgeEventStatusColor.get(EdgeEventStatus.DEPLOYED)
        }), false),
      new EntityActionTableColumn<EdgeEvent>('data', 'event.data',
        {
          name: this.translate.instant('action.view'),
          icon: 'more_horiz',
          isEnabled: (entity) => this.isEdgeEventHasData(entity.type),
          onAction: ($event, entity) =>
            {
              this.prepareEdgeEventContent(entity).subscribe((content) => {
                this.showEdgeEventContent($event, content,'event.data');
              });
            }
        },
        '40px'),
    );
    if (updateTableColumns) {
      this.table.columnsUpdated(true);
    }
  }

  updateEdgeEventStatus(createdTime) {
    if (this.queueStartTs && createdTime < this.queueStartTs) {
      return this.translate.instant('edge.deployed');
    } else {
      return this.translate.instant('edge.pending');
    }
  }

  isPending(createdTime) {
    return createdTime > this.queueStartTs;
  }

  isEdgeEventHasData(edgeEventType: EdgeEventType) {
    switch (edgeEventType) {
      case EdgeEventType.DEVICE_PROFILE:
      case EdgeEventType.WIDGETS_BUNDLE:
      case EdgeEventType.WIDGET_TYPE:
      case EdgeEventType.ADMIN_SETTINGS:
        return false;
      default:
        return true;
    }
  }

  prepareEdgeEventContent(entity) {
    switch (entity.type) {
      case EdgeEventType.DEVICE:
      case EdgeEventType.ASSET:
      case EdgeEventType.EDGE:
      case EdgeEventType.ENTITY_VIEW:
      case EdgeEventType.TENANT:
      case EdgeEventType.CUSTOMER:
      case EdgeEventType.DASHBOARD:
      case EdgeEventType.USER:
      case EdgeEventType.RULE_CHAIN:
      case EdgeEventType.ALARM:
        return this.entityService.getEntity(entity.type, entity.entityId, { ignoreLoading: true, ignoreErrors: true }).pipe(
          map((entity) => {
            return JSON.stringify(entity);
          })
        );
      case EdgeEventType.RELATION:
        return of(JSON.stringify(entity.body));
      case EdgeEventType.RULE_CHAIN_METADATA:
        return this.ruleChainService.getRuleChainMetadata(entity.entityId, null).pipe(
          map((ruleChainMetaData) => {
            return JSON.stringify(ruleChainMetaData.nodes);
          })
        );
      case EdgeEventType.DEVICE_PROFILE:
      case EdgeEventType.WIDGETS_BUNDLE:
      case EdgeEventType.WIDGET_TYPE:
      case EdgeEventType.ADMIN_SETTINGS:
        return of(null);
    }
  }

  showEdgeEventContent($event: MouseEvent, content: string, title: string): void {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<EventContentDialogComponent, EventContentDialogData>(EventContentDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        content,
        title,
        contentType: ContentType.JSON
      }
    });
  }
}
