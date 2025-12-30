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
  CellActionDescriptorType,
  DateEntityTableColumn,
  EntityActionTableColumn,
  EntityLinkTableColumn,
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import {
  DebugEventType,
  Event as DebugEvent,
  Event,
  EventBody,
  EventType,
  FilterEventBody
} from '@shared/models/event.models';
import { TimePageLink } from '@shared/models/page/page-link';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { MatDialog } from '@angular/material/dialog';
import { EntityId } from '@shared/models/id/entity-id';
import { EventService } from '@app/core/http/event.service';
import { EventTableHeaderComponent } from '@home/components/event/event-table-header.component';
import { EntityType, EntityTypeResource } from '@shared/models/entity-type.models';
import { fromEvent, Observable } from 'rxjs';
import { PageData } from '@shared/models/page/page-data';
import { Direction } from '@shared/models/page/sort-order';
import { DialogService } from '@core/services/dialog.service';
import { ContentType } from '@shared/models/constants';
import {
  EventContentDialogComponent,
  EventContentDialogData
} from '@home/components/event/event-content-dialog.component';
import { getEntityDetailsPageURL, isEqual, sortObjectKeys } from '@core/utils';
import { DAY, historyInterval, MINUTE } from '@shared/models/time/time.models';
import { Overlay, OverlayConfig, OverlayRef } from '@angular/cdk/overlay';
import { ChangeDetectorRef, EventEmitter, Injector, StaticProvider, ViewContainerRef } from '@angular/core';
import { ComponentPortal } from '@angular/cdk/portal';
import {
  EVENT_FILTER_PANEL_DATA,
  EventFilterPanelComponent,
  EventFilterPanelData,
  FilterEntityColumn
} from '@home/components/event/event-filter-panel.component';
import { DEFAULT_OVERLAY_POSITIONS } from '@shared/models/overlay.models';
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';

export class EventTableConfig extends EntityTableConfig<Event, TimePageLink> {

  eventTypeValue: EventType | DebugEventType;

  private filterParams: FilterEventBody = {};
  private filterColumns: FilterEntityColumn[] = [];
  private readonly maxDebugModeDurationMinutes = getCurrentAuthState(this.store).maxDebugModeDurationMinutes;

  set eventType(eventType: EventType | DebugEventType) {
    if (this.eventTypeValue !== eventType) {
      this.eventTypeValue = eventType;
      this.updateCellAction();
      this.updateColumns(true);
      this.updateFilterColumns();
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
              private debugEventTypes: Array<DebugEventType> = null,
              private overlay: Overlay,
              private viewContainerRef: ViewContainerRef,
              private cd: ChangeDetectorRef,
              private store: Store<AppState>,
              public testButtonLabel?: string,
              private debugEventSelected?: EventEmitter<EventBody>,
              public hideClearEventAction = false,
              public disableDebugEventAction = false) {
    super();
    this.loadDataOnInit = false;
    this.tableTitle = '';
    this.useTimePageLink = true;
    const defaultInterval = this.maxDebugModeDurationMinutes ? Math.min(this.maxDebugModeDurationMinutes * MINUTE, DAY) : DAY;
    this.defaultTimewindowInterval = historyInterval(defaultInterval);
    this.detailsPanelEnabled = false;
    this.selectionEnabled = false;
    this.searchEnabled = false;
    this.addEnabled = false;
    this.entitiesDeleteEnabled = false;
    this.pageMode = false;

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
    } as EntityTypeResource<Event>;
    this.entitiesFetchFunction = pageLink => this.fetchEvents(pageLink);

    this.defaultSortOrder = {property: 'createdTime', direction: Direction.DESC};

    this.updateColumns();
    this.updateCellAction();
    this.updateFilterColumns();

    this.headerActionDescriptors.push({
      name: this.translate.instant('event.clear-filter'),
      icon: 'mdi:filter-variant-remove',
      isEnabled: () => !isEqual(this.filterParams, {}),
      onAction: ($event) => {
        this.clearFiter($event);
      }
    },
    {
      name: this.translate.instant('event.events-filter'),
      icon: 'filter_list',
      isEnabled: () => true,
      onAction: ($event) => {
        this.editEventFilter($event);
      }
    },
    {
      name: this.translate.instant('event.clean-events'),
      icon: 'delete',
      isEnabled: () => !this.hideClearEventAction,
      onAction: $event => this.clearEvents($event)
    });
  }

  clearEvents($event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialogService.confirm(
      this.translate.instant('event.clear-request-title'),
      this.translate.instant('event.clear-request-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes')
    ).subscribe((res) => {
      if (res) {
        this.eventService.clearEvents(this.entityId, this.eventType, this.filterParams,
          this.tenantId, this.getTable().pageLink as TimePageLink).subscribe(
          () => {
            this.getTable().paginator.pageIndex = 0;
            this.updateData();
          }
        );
      }
    });
  }

  fetchEvents(pageLink: TimePageLink): Observable<PageData<Event>> {
    return this.eventService.getFilterEvents(this.entityId, this.eventType, this.tenantId, this.filterParams, pageLink);
  }

  updateColumns(updateTableColumns: boolean = false): void {
    this.columns = [];
    this.columns.push(
      new DateEntityTableColumn<Event>('createdTime', 'event.event-time', this.datePipe, '120px', 'yyyy-MM-dd HH:mm:ss.SSS'),
      new EntityTableColumn<Event>('server', 'event.server', '100px',
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
          new EntityTableColumn<Event>('messagesProcessed', 'event.messages-processed', '50%',
            (entity) => entity.body.messagesProcessed + '',
            () => ({}),
            false,
            () => ({}), () => undefined, true),
          new EntityTableColumn<Event>('errorsOccurred', 'event.errors-occurred', '50%',
            (entity) => entity.body.errorsOccurred + '',
            () => ({}),
            false,
            () => ({}), () => undefined, true)
        );
        break;
      case DebugEventType.DEBUG_RULE_NODE:
        this.columns[0].width = '80px';
        (this.columns[1] as EntityTableColumn<Event>).headerCellStyleFunction = () => ({padding: '0 12px 0 0'});
        (this.columns[1] as EntityTableColumn<Event>).cellStyleFunction = () => ({padding: '0 12px 0 0'});
        this.columns.push(
          new EntityTableColumn<Event>('type', 'event.type', '40px',
            (entity) => entity.body.type, entity => ({
              padding: '0 12px 0 0',
            }), false, key => ({
              padding: '0 12px 0 0'
            })),
          new EntityTableColumn<Event>('entityType', 'event.entity-type', '75px',
            (entity) => entity.body.entityType, entity => ({
              padding: '0 12px 0 0',
            }), false, key => ({
              padding: '0 12px 0 0'
            })),
          new EntityTableColumn<Event>('entityId', 'event.entity-id', '85px',
            (entity) => `<span style="display: inline-block; width: 7ch">${entity.body.entityId.substring(0, 6)}…</span>`,
            () => ({
              padding: '0 12px 0 0'
            }), false, () => ({
              padding: '0 12px 0 0'
            }),
            () => undefined, false, {
              name: this.translate.instant('event.copy-entity-id'),
              icon: 'content_paste',
              style: {
                padding: '4px',
                'font-size': '16px',
                color: 'rgba(0,0,0,.87)'
              },
              isEnabled: () => true,
              onAction: ($event, entity) => entity.body.entityId,
              type: CellActionDescriptorType.COPY_BUTTON
            }),
          new EntityTableColumn<Event>('msgId', 'event.message-id', '85px',
            (entity) => `<span style="display: inline-block; width: 7ch">${entity.body.msgId.substring(0, 6)}…</span>`,
            () => ({
              padding: '0 12px 0 0'
            }), false, () => ({
              padding: '0 12px 0 0'
            }), () => undefined, false, {
              name: this.translate.instant('event.copy-message-id'),
              icon: 'content_paste',
              style: {
                padding: '4px',
                'font-size': '16px',
                color: 'rgba(0,0,0,.87)'
              },
              isEnabled: () => true,
              onAction: ($event, entity) => entity.body.msgId,
              type: CellActionDescriptorType.COPY_BUTTON
            }),
          new EntityTableColumn<Event>('msgType', 'event.message-type', '100px',
            (entity) => entity.body.msgType, entity => ({
              whiteSpace: 'nowrap',
              padding: '0 12px 0 0'
            }), false, key => ({
              padding: '0 12px 0 0'
            }),
            entity => entity.body.msgType),
          new EntityTableColumn<Event>('relationType', 'event.relation-type', '85px',
            (entity) => entity.body.relationType, () => ({padding: '0 12px 0 0'}), false, () => ({
              padding: '0 12px 0 0'
            })),
          new EntityActionTableColumn<Event>('data', 'event.data',
            {
              name: this.translate.instant('action.view'),
              icon: 'more_horiz',
              isEnabled: (entity) => entity.body.data ? entity.body.data.length > 0 : false,
              onAction: ($event, entity) => this.showContent($event, entity.body.data,
                'event.data', entity.body.dataType)
            },
            '48px'),
          new EntityActionTableColumn<Event>('metadata', 'event.metadata',
            {
              name: this.translate.instant('action.view'),
              icon: 'more_horiz',
              isEnabled: (entity) => entity.body.metadata ? entity.body.metadata.length > 0 : false,
              onAction: ($event, entity) => this.showContent($event, entity.body.metadata,
                'event.metadata', ContentType.JSON, true)
            },
            '48px'),
          new EntityActionTableColumn<Event>('error', 'event.error',
            {
              name: this.translate.instant('action.view'),
              icon: 'more_horiz',
              isEnabled: (entity) => entity.body.error && entity.body.error.length > 0,
              onAction: ($event, entity) => this.showContent($event, entity.body.error,
                'event.error')
            },
            '48px')
        );
        break;
      case DebugEventType.DEBUG_RULE_CHAIN:
        this.columns[0].width = '100px';
        this.columns.push(
          new EntityActionTableColumn<Event>('message', 'event.message',
            {
              name: this.translate.instant('action.view'),
              icon: 'more_horiz',
              isEnabled: (entity) => entity.body.message ? entity.body.message.length > 0 : false,
              onAction: ($event, entity) => this.showContent($event, entity.body.message,
                'event.message')
            },
            '48px'),
          new EntityActionTableColumn<Event>('error', 'event.error',
            {
              name: this.translate.instant('action.view'),
              icon: 'more_horiz',
              isEnabled: (entity) => entity.body.error && entity.body.error.length > 0,
              onAction: ($event, entity) => this.showContent($event, entity.body.error,
                'event.error')
            },
            '48px')
        );
        break;
      case DebugEventType.DEBUG_CALCULATED_FIELD:
        this.columns[0].width = '80px';
        this.columns[1].width = '100px';
        this.columns.push(
          new EntityLinkTableColumn<Event>('entityId', 'event.entity-id', '100px',
            (entity) => `<span style="display: inline-block; width: 9ch">${entity.body.entityId.substring(0, 8)}…</span>`,
            (entity) => getEntityDetailsPageURL(entity.body.entityId, entity.body.entityType as EntityType),
            false,
            () => ({padding: '0 12px 0 0'}),
            () => ({padding: '0 12px 0 0'}),
            (entity) => entity.body.entityId,
            {
              name: this.translate.instant('event.copy-entity-id'),
              icon: 'content_copy',
              style: {
                padding: '4px',
                'font-size': '16px',
                color: 'rgba(0,0,0,.87)'
              },
              isEnabled: () => true,
              onAction: ($event, entity) => entity.body.entityId,
              type: CellActionDescriptorType.COPY_BUTTON
            }
          ),
          new EntityTableColumn<Event>('messageId', 'event.message-id', '100px',
            (entity) => entity.body.msgId ? `<span style="display: inline-block; width: 9ch">${entity.body.msgId?.substring(0, 8)}…</span>` : '-',
            () => ({padding: '0 12px 0 0'}),
            false,
            () => ({padding: '0 12px 0 0'}),
            (entity) => entity.body.msgId,
            false,
            {
              name: this.translate.instant('event.copy-message-id'),
              icon: 'content_copy',
              style: {
                padding: '4px',
                'font-size': '16px',
                color: 'rgba(0,0,0,.87)'
              },
              isEnabled: (entity) => !!entity.body.msgId,
              onAction: (_, entity) => entity.body.msgId,
              type: CellActionDescriptorType.COPY_BUTTON
            }
          ),
          new EntityTableColumn<Event>('messageType', 'event.message-type', '100px',
            (entity) => entity.body.msgType ?? '-',
            () => ({padding: '0 12px 0 0'}),
            false,
            () => ({padding: '0 12px 0 0'}),
            (entity) => entity.body.msgType,
          ),
          new EntityActionTableColumn<Event>('arguments', 'event.arguments',
            {
              name: this.translate.instant('action.view'),
              icon: 'more_horiz',
              isEnabled: (entity) => entity.body.arguments !== undefined,
              onAction: ($event, entity) => this.showContent($event, entity.body.arguments,
                'event.arguments', ContentType.JSON, true)
            },
            '48px'
          ),
          new EntityActionTableColumn<Event>('result', 'event.result',
            {
              name: this.translate.instant('action.view'),
              icon: 'more_horiz',
              isEnabled: (entity) => entity.body.result !== undefined,
              onAction: ($event, entity) => this.showContent($event, entity.body.result,
                'event.result', ContentType.JSON, true)
            },
            '48px'
          ),
          new EntityActionTableColumn<Event>('error', 'event.error',
            {
              name: this.translate.instant('action.view'),
              icon: 'more_horiz',
              isEnabled: (entity) => entity.body.error && entity.body.error.length > 0,
              onAction: ($event, entity) => this.showContent($event, entity.body.error,
                'event.error')
            },
            '48px'
          )
        );
        break;
    }
    if (updateTableColumns) {
      this.getTable().columnsUpdated(true);
    }
  }

  updateCellAction() {
    this.cellActionDescriptors = [];
    switch (this.eventType) {
      case DebugEventType.DEBUG_RULE_NODE:
        if (this.testButtonLabel) {
          this.cellActionDescriptors.push({
            name: this.translate.instant('rulenode.test-with-this-message', {test: this.translate.instant(this.testButtonLabel)}),
            icon: 'bug_report',
            isEnabled: (entity) => (entity.body.type === 'IN' || entity.body.error !== undefined) && !this.disableDebugEventAction,
            onAction: ($event, entity) => {
              this.debugEventSelected.next(entity.body);
            }
          });
        }
        break;
      case DebugEventType.DEBUG_CALCULATED_FIELD:
        this.cellActionDescriptors.push({
          name: this.translate.instant('calculated-fields.test-with-this-message'),
          icon: 'bug_report',
          isEnabled: (event) => !this.disableDebugEventAction && !!(event as DebugEvent).body.arguments,
          onAction: (_, entity) => this.debugEventSelected.next(entity.body)
        });
        break;
    }
    this.getTable()?.cellActionDescriptorsUpdated();
  }

  showContent($event: MouseEvent, content: string, title: string, contentType: ContentType = null, sortKeys = false): void {
    if ($event) {
      $event.stopPropagation();
    }
    if (contentType === ContentType.JSON && sortKeys) {
      try {
        const parsedContent = JSON.parse(content);
        if (Array.isArray(parsedContent)) {
          content = JSON.stringify(parsedContent.map(item => item && typeof item === 'object' ? sortObjectKeys(item) : item));
        } else {
          content = JSON.stringify(sortObjectKeys(parsedContent));
        }
      } catch (e) {}
    }
    this.dialog.open<EventContentDialogComponent, EventContentDialogData>(EventContentDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        content,
        title,
        contentType
      }
    });
  }

  private updateFilterColumns() {
    this.filterParams = {};
    this.filterColumns = [{key: 'server', title: 'event.server'}];
    switch (this.eventType) {
      case EventType.ERROR:
        this.filterColumns.push(
          {key: 'method', title: 'event.method'},
          {key: 'errorStr', title: 'event.error'}
        );
        break;
      case EventType.LC_EVENT:
        this.filterColumns.push(
          {key: 'event', title: 'event.event'},
          {key: 'status', title: 'event.status'},
          {key: 'errorStr', title: 'event.error'}
        );
        break;
      case EventType.STATS:
        this.filterColumns.push(
          {key: 'minMessagesProcessed', title: 'event.min-messages-processed'},
          {key: 'maxMessagesProcessed', title: 'event.max-messages-processed'},
          {key: 'minErrorsOccurred', title: 'event.min-errors-occurred'},
          {key: 'maxErrorsOccurred', title: 'event.max-errors-occurred'}
        );
        break;
      case DebugEventType.DEBUG_RULE_NODE:
        this.filterColumns.push(
          {key: 'msgDirectionType', title: 'event.type'},
          {key: 'entityId', title: 'event.entity-id'},
          {key: 'entityType', title: 'event.entity-type'},
          {key: 'msgId', title: 'event.message-id'},
          {key: 'msgType', title: 'event.message-type'},
          {key: 'relationType', title: 'event.relation-type'},
          {key: 'dataSearch', title: 'event.data'},
          {key: 'metadataSearch', title: 'event.metadata'},
          {key: 'isError', title: 'event.error'},
          {key: 'errorStr', title: 'event.error'}
        );
        break;
      case DebugEventType.DEBUG_RULE_CHAIN:
        this.filterColumns.push(
          {key: 'message', title: 'event.message'},
          {key: 'isError', title: 'event.error'},
          {key: 'errorStr', title: 'event.error'}
        );
        break;
      case DebugEventType.DEBUG_CALCULATED_FIELD:
        this.filterColumns.push(
          {key: 'entityId', title: 'event.entity-id'},
          {key: 'msgId', title: 'event.message-id'},
          {key: 'msgType', title: 'event.message-type'},
          {key: 'arguments', title: 'event.arguments'},
          {key: 'result', title: 'event.result'},
          {key: 'isError', title: 'event.error'},
          {key: 'errorStr', title: 'event.error'}
        );
        break;
    }
  }

  private clearFiter($event) {
    if ($event) {
      $event.stopPropagation();
    }

    this.filterParams = {};
    this.getTable().paginator.pageIndex = 0;
    this.updateData();
  }

  private editEventFilter($event: MouseEvent) {
    if ($event) {
      $event.stopPropagation();
    }
    const target = $event.target || $event.currentTarget;
    const config = new OverlayConfig({
      panelClass: 'tb-panel-container',
      backdropClass: 'cdk-overlay-transparent-backdrop',
      hasBackdrop: true,
      height: 'fit-content',
      maxHeight: '65vh'
    });
    config.positionStrategy = this.overlay.position()
      .flexibleConnectedTo(target as HTMLElement)
      .withPositions(DEFAULT_OVERLAY_POSITIONS);

    const overlayRef = this.overlay.create(config);
    overlayRef.backdropClick().subscribe(() => {
      overlayRef.dispose();
    });
    const providers: StaticProvider[] = [
      {
        provide: EVENT_FILTER_PANEL_DATA,
        useValue: {
          columns: this.filterColumns,
          filterParams: this.filterParams
        } as EventFilterPanelData
      },
      {
        provide: OverlayRef,
        useValue: overlayRef
      }
    ];
    const injector = Injector.create({parent: this.viewContainerRef.injector, providers});
    const componentRef = overlayRef.attach(new ComponentPortal(EventFilterPanelComponent,
      this.viewContainerRef, injector));
    const resizeWindows$ = fromEvent(window, 'resize').subscribe(() => {
      overlayRef.updatePosition();
    });
    componentRef.onDestroy(() => {
      resizeWindows$.unsubscribe();
      if (componentRef.instance.result && !isEqual(this.filterParams, componentRef.instance.result.filterParams)) {
        this.filterParams = componentRef.instance.result.filterParams;
        this.getTable().paginator.pageIndex = 0;
        this.updateData();
      }
    });
    this.cd.detectChanges();
  }
}

