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

import {
  DateEntityTableColumn,
  EntityActionTableColumn,
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import { DebugEventType, Event, EventType } from '@shared/models/event.models';
import { TimePageLink } from '@shared/models/page/page-link';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { MatDialog } from '@angular/material/dialog';
import { EntityId } from '@shared/models/id/entity-id';
import { EventService } from '@app/core/http/event.service';
import { EventTableHeaderComponent } from '@home/components/event/event-table-header.component';
import { EntityTypeResource } from '@shared/models/entity-type.models';
import { Observable } from 'rxjs';
import { PageData } from '@shared/models/page/page-data';
import { Direction } from '@shared/models/page/sort-order';
import { MsgDataType } from '@shared/models/rule-node.models';
import { DialogService } from '@core/services/dialog.service';

export class EventTableConfig extends EntityTableConfig<Event, TimePageLink> {

  eventTypeValue: EventType | DebugEventType;

  set eventType(eventType: EventType | DebugEventType) {
    if (this.eventTypeValue !== eventType) {
      this.eventTypeValue = eventType;
      this.updateColumns(true);
    }
  }

  get eventType(): EventType | DebugEventType {
    return this.eventTypeValue;
  }

  eventTypes: Array<EventType | DebugEventType>;

  constructor(private eventService: EventService,
              private dialogService: DialogService,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private dialog: MatDialog,
              public entityId: EntityId,
              public tenantId: string,
              private defaultEventType: EventType | DebugEventType,
              private disabledEventTypes: Array<EventType | DebugEventType> = null,
              private debugEventTypes: Array<DebugEventType> = null) {
    super();
    this.loadDataOnInit = false;
    this.tableTitle = '';
    this.useTimePageLink = true;
    this.detailsPanelEnabled = false;
    this.selectionEnabled = false;
    this.searchEnabled = false;
    this.addEnabled = false;
    this.entitiesDeleteEnabled = false;

    this.headerComponent = EventTableHeaderComponent;

    this.eventTypes = Object.keys(EventType).map(type => EventType[type]);

    if (disabledEventTypes && disabledEventTypes.length) {
      this.eventTypes = this.eventTypes.filter(type => disabledEventTypes.indexOf(type) === -1);
    }

    if (debugEventTypes && debugEventTypes.length) {
      this.eventTypes = [...this.eventTypes, ...debugEventTypes];
    }

    this.eventTypeValue = defaultEventType;

    this.entityTranslations = {
      noEntities: 'event.no-events-prompt'
    };
    this.entityResources = {
    } as EntityTypeResource;
    this.entitiesFetchFunction = pageLink => this.fetchEvents(pageLink);

    this.defaultSortOrder = {property: 'createdTime', direction: Direction.DESC};

    this.updateColumns();
  }

  fetchEvents(pageLink: TimePageLink): Observable<PageData<Event>> {
    return this.eventService.getEvents(this.entityId, this.eventType, this.tenantId, pageLink);
  }

  updateColumns(updateTableColumns: boolean = false): void {
    this.columns = [];
    this.columns.push(
      new DateEntityTableColumn<Event>('createdTime', 'event.event-time', this.datePipe, '150px'),
      new EntityTableColumn<Event>('server', 'event.server', '150px',
        (entity) => entity.body.server, entity => ({}), false));
    switch (this.eventType) {
      case EventType.ERROR:
        this.columns.push(
        new EntityTableColumn<Event>('method', 'event.method', '100%',
          (entity) => entity.body.method, entity => ({}), false),
          new EntityActionTableColumn<Event>('error', 'event.error',
            {
              name: this.translate.instant('action.view'),
              icon: 'more_horiz',
              isEnabled: (entity) => entity.body.error && entity.body.error.length > 0,
              onAction: ($event, entity) => this.showContent($event, entity.body.error, 'event.error')
            },
            '100px')
        );
        break;
      case EventType.LC_EVENT:
        this.columns.push(
          new EntityTableColumn<Event>('method', 'event.event', '100%',
            (entity) => entity.body.event, entity => ({}), false),
          new EntityTableColumn<Event>('status', 'event.status', '100%',
            (entity) =>
              this.translate.instant(entity.body.success ? 'event.success' : 'event.failed'), entity => ({}), false),
          new EntityActionTableColumn<Event>('error', 'event.error',
            {
              name: this.translate.instant('action.view'),
              icon: 'more_horiz',
              isEnabled: (entity) => entity.body.error && entity.body.error.length > 0,
              onAction: ($event, entity) => this.showContent($event, entity.body.error, 'event.error')
            },
            '100px')
        );
        break;
      case EventType.STATS:
        this.columns.push(
          new EntityTableColumn<Event>('messagesProcessed', 'event.messages-processed', '100%',
            (entity) => entity.body.messagesProcessed + '',
              entity => ({justifyContent: 'flex-end'}),
            false,
            key => ({justifyContent: 'flex-end'})),
          new EntityTableColumn<Event>('errorsOccurred', 'event.errors-occurred', '100%',
            (entity) => entity.body.errorsOccurred + '',
              entity => ({justifyContent: 'flex-end'}),
            false,
            key => ({justifyContent: 'flex-end'}))
        );
        break;
      case DebugEventType.DEBUG_RULE_NODE:
      case DebugEventType.DEBUG_RULE_CHAIN:
        this.columns.push(
          new EntityTableColumn<Event>('type', 'event.type', '100%',
            (entity) => entity.body.type, entity => ({}), false),
          new EntityTableColumn<Event>('entity', 'event.entity', '100%',
            (entity) => entity.body.entityName, entity => ({}), false),
          new EntityTableColumn<Event>('msgId', 'event.message-id', '100%',
            (entity) => entity.body.msgId, entity => ({}), false),
          new EntityTableColumn<Event>('msgType', 'event.message-type', '100%',
            (entity) => entity.body.msgType, entity => ({}), false),
          new EntityTableColumn<Event>('relationType', 'event.relation-type', '100%',
            (entity) => entity.body.relationType, entity => ({}), false),
          new EntityActionTableColumn<Event>('data', 'event.data',
            {
              name: this.translate.instant('action.view'),
              icon: 'more_horiz',
              isEnabled: (entity) => entity.body.data && entity.body.data.length > 0,
              onAction: ($event, entity) => this.showContent($event, entity.body.data,
                'event.data', entity.body.dataType)
            },
            '60px'),
          new EntityActionTableColumn<Event>('metadata', 'event.metadata',
            {
              name: this.translate.instant('action.view'),
              icon: 'more_horiz',
              isEnabled: (entity) => entity.body.metadata && entity.body.metadata.length > 0,
              onAction: ($event, entity) => this.showContent($event, entity.body.metadata,
                'event.metadata', MsgDataType.JSON)
            },
            '60px'),
          new EntityActionTableColumn<Event>('error', 'event.error',
            {
              name: this.translate.instant('action.view'),
              icon: 'more_horiz',
              isEnabled: (entity) => entity.body.error && entity.body.error.length > 0,
              onAction: ($event, entity) => this.showContent($event, entity.body.error,
                'event.error')
            },
            '60px')
        );
        break;
    }
    if (updateTableColumns) {
      this.table.columnsUpdated(true);
    }
  }

  showContent($event: MouseEvent, content: string, title: string, contentType: MsgDataType = null): void {
    if ($event) {
      $event.stopPropagation();
    }
    // TODO:
    this.dialogService.todo();
  }
}
