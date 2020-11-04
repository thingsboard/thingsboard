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

import { Injectable } from '@angular/core';
import { defaultHttpOptionsFromConfig, RequestConfig } from './http-utils';
import { Observable } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { TimePageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';
import { EntityId } from '@shared/models/id/entity-id';
import { DebugEventType, Event, EventType } from '@shared/models/event.models';

@Injectable({
  providedIn: 'root'
})
export class EventService {

  constructor(
    private http: HttpClient
  ) { }

  public getEvents(entityId: EntityId, eventType: EventType | DebugEventType, tenantId: string, pageLink: TimePageLink,
                   config?: RequestConfig): Observable<PageData<Event>> {
    return this.http.get<PageData<Event>>(`/api/events/${entityId.entityType}/${entityId.id}/${eventType}` +
              `${pageLink.toQuery()}&tenantId=${tenantId}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getEdgeEvents(entityId: EntityId, pageLink: TimePageLink,
                       config?: RequestConfig): Observable<PageData<Event>> {
    return this.http.get<PageData<Event>>(`/api/edge/${entityId.id}/events` + `${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config));
  }
}
