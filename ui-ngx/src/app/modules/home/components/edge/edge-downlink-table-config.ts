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

import {
  DateEntityTableColumn,
  EntityActionTableColumn,
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import {
  EdgeEvent,
  EdgeEventActionType,
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
import { Observable } from 'rxjs';
import { PageData } from '@shared/models/page/page-data';
import { Direction } from '@shared/models/page/sort-order';
import { DialogService } from '@core/services/dialog.service';
import { ContentType } from '@shared/models/constants';
import {
  EventContentDialogComponent,
  EventContentDialogData
} from '@home/components/event/event-content-dialog.component';
import { AttributeService } from '@core/http/attribute.service';
import { AttributeScope } from '@shared/models/telemetry/telemetry.models';
import { EdgeDownlinkTableHeaderComponent } from '@home/components/edge/edge-downlink-table-header.component';
import { EdgeService } from '@core/http/edge.service';
import { concatMap, map } from 'rxjs/operators';
import { EntityService } from '@core/http/entity.service';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { ActionNotificationShow } from '@core/notification/notification.actions';

export class EdgeDownlinkTableConfig extends EntityTableConfig<EdgeEvent, TimePageLink> {

  private queueStartTs: number;

  constructor(private attributeService: AttributeService,
              private datePipe: DatePipe,
              private dialogService: DialogService,
              private dialog: MatDialog,
              private edgeService: EdgeService,
              private entityService: EntityService,
              private translate: TranslateService,
              private store: Store<AppState>,
              public entityId: EntityId) {
    super();

    this.tableTitle = '';
    this.loadDataOnInit = false;
    this.useTimePageLink = true;
    this.detailsPanelEnabled = false;
    this.selectionEnabled = false;
    this.searchEnabled = false;
    this.addEnabled = false;
    this.entitiesDeleteEnabled = false;
    this.pageMode = false;

    this.headerComponent = EdgeDownlinkTableHeaderComponent;
    this.entityTranslations = { noEntities: 'edge.no-downlinks-prompt' };
    this.entitiesFetchFunction = pageLink => this.fetchEvents(pageLink);
    this.defaultSortOrder = { property: 'createdTime', direction: Direction.DESC };

    this.updateColumns();
  }

  private fetchEvents(pageLink: TimePageLink): Observable<PageData<EdgeEvent>> {
    return this.attributeService.getEntityAttributes(this.entityId, AttributeScope.SERVER_SCOPE, ['queueStartTs']).pipe(
      map((attributes) => this.onUpdate(attributes)),
      concatMap(() => this.edgeService.getEdgeEvents(this.entityId, pageLink))
    );
  }

  private onUpdate(attributes: any): void {
    this.queueStartTs = 0;
    const edge = attributes.reduce((attrMap, attribute) => {
      attrMap[attribute.key] = attribute;
      return attrMap;
    }, {});
    this.queueStartTs = edge.queueStartTs && edge.queueStartTs.value ? edge.queueStartTs.value : 0;
  }

  private updateColumns(updateTableColumns: boolean = false): void {
    this.columns = [];
    this.columns.push(
      new DateEntityTableColumn<EdgeEvent>('createdTime', 'event.event-time', this.datePipe, '120px'),
      new EntityTableColumn<EdgeEvent>('type', 'event.type', '25%',
        entity => {
          let key = edgeEventTypeTranslations.get(entity.type);
          return key ? this.translate.instant(key) : entity.type;
        }, entity => ({}), false),
      new EntityTableColumn<EdgeEvent>('action', 'edge.event-action', '25%',
        entity => {
          let key = edgeEventActionTypeTranslations.get(entity.action);
          return key ? this.translate.instant(key) : entity.action;
        }, entity => ({}), false),
      new EntityTableColumn<EdgeEvent>('entityId', 'edge.entity-id', '40%',
        (entity) => entity.entityId ? entity.entityId : '', () => ({}), false),
      new EntityTableColumn<EdgeEvent>('status', 'event.status', '10%',
        (entity) => this.updateEdgeEventStatus(entity.createdTime),
        entity => ({
          color: this.isPending(entity.createdTime) ? edgeEventStatusColor.get(EdgeEventStatus.PENDING) :
            edgeEventStatusColor.get(EdgeEventStatus.DEPLOYED)
        }), false),
      new EntityActionTableColumn<EdgeEvent>('data', 'event.data',
        {
          name: this.translate.instant('action.view'),
          icon: 'more_horiz',
          isEnabled: (entity) => this.isEdgeEventHasData(entity),
          onAction: ($event, entity) =>
            {
              this.prepareEdgeEventContent(entity).subscribe(
                (content) => this.showEdgeEventContent($event, content, 'event.data'),
                () => this.showEntityNotFoundError()
              );
            }
        },
        '48px'),
    );
    if (updateTableColumns) {
      this.getTable().columnsUpdated(true);
    }
  }

  private updateEdgeEventStatus(createdTime: number): string {
    if (this.queueStartTs && createdTime <= this.queueStartTs) {
      return this.translate.instant('edge.deployed');
    } else {
      return this.translate.instant('edge.pending');
    }
  }

  private isPending(createdTime: number): boolean {
    return createdTime > this.queueStartTs;
  }

  private isEdgeEventHasData(entity: EdgeEvent): boolean {
    return !(entity.type === EdgeEventType.ADMIN_SETTINGS ||
             entity.action === EdgeEventActionType.DELETED);
  }

  private prepareEdgeEventContent(entity: EdgeEvent): Observable<string> {
    return this.entityService.getEdgeEventContent(entity).pipe(
      map((result) => JSON.stringify(result))
    );
  }

  private showEdgeEventContent($event: MouseEvent, content: string, title: string): void {
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

  private showEntityNotFoundError(): void {
    this.store.dispatch(new ActionNotificationShow(
      {
        message: this.translate.instant('edge.load-entity-error'),
        type: 'error',
        verticalPosition: 'top',
        horizontalPosition: 'left'
      }
    ));
  }
}
