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

import { Component, Input, OnInit, ViewChild } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { MatDialog } from '@angular/material/dialog';
import { EntityId } from '@shared/models/id/entity-id';
import { EntitiesTableComponent } from '@home/components/entity/entities-table.component';
import { EventTableConfig } from './event-table-config';
import { EventService } from '@core/http/event.service';
import { DialogService } from '@core/services/dialog.service';
import { DebugEventType, EventType } from '@shared/models/event.models';

@Component({
  selector: 'tb-event-table',
  templateUrl: './event-table.component.html',
  styleUrls: ['./event-table.component.scss']
})
export class EventTableComponent implements OnInit {

  @Input()
  tenantId: string;

  @Input()
  defaultEventType: EventType | DebugEventType;

  @Input()
  disabledEventTypes: Array<EventType | DebugEventType>;

  @Input()
  debugEventTypes: Array<DebugEventType>;

  activeValue = false;
  dirtyValue = false;
  entityIdValue: EntityId;

  @Input()
  set active(active: boolean) {
    if (this.activeValue !== active) {
      this.activeValue = active;
      if (this.activeValue && this.dirtyValue) {
        this.dirtyValue = false;
        this.entitiesTable.updateData();
      }
    }
  }

  @Input()
  set entityId(entityId: EntityId) {
    this.entityIdValue = entityId;
    if (this.eventTableConfig && this.eventTableConfig.entityId !== entityId) {
      this.eventTableConfig.eventType = this.defaultEventType;
      this.eventTableConfig.entityId = entityId;
      this.entitiesTable.resetSortAndFilter(this.activeValue);
      if (!this.activeValue) {
        this.dirtyValue = true;
      }
    }
  }

  @ViewChild(EntitiesTableComponent, {static: true}) entitiesTable: EntitiesTableComponent;

  eventTableConfig: EventTableConfig;

  constructor(private eventService: EventService,
              private dialogService: DialogService,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private dialog: MatDialog) {
  }

  ngOnInit() {
    this.dirtyValue = !this.activeValue;
    this.eventTableConfig = new EventTableConfig(
      this.eventService,
      this.dialogService,
      this.translate,
      this.datePipe,
      this.dialog,
      this.entityIdValue,
      this.tenantId,
      this.defaultEventType,
      this.disabledEventTypes,
      this.debugEventTypes
    );
  }

}
