// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityTableHeaderComponent } from '../../components/entity/entity-table-header.component';
import { DebugEventType, Event, EventType, eventTypeTranslations } from '@app/shared/models/event.models';
import { EventTableConfig } from '@home/components/event/event-table-config';

@Component({
    selector: 'tb-event-table-header',
    templateUrl: './event-table-header.component.html',
    styleUrls: ['./event-table-header.component.scss'],
    standalone: false
})
export class EventTableHeaderComponent extends EntityTableHeaderComponent<Event> {

  eventTypeTranslationsMap = eventTypeTranslations;

  get eventTableConfig(): EventTableConfig {
    return this.entitiesTableConfig as EventTableConfig;
  }

  constructor(protected store: Store<AppState>) {
    super(store);
  }

  eventTypeChanged(eventType: EventType | DebugEventType) {
    this.eventTableConfig.eventType = eventType;
    this.eventTableConfig.getTable().resetSortAndFilter(true, true);
  }
}
