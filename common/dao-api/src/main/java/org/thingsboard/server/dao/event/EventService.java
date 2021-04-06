/**
 * Copyright © 2016-2021 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.dao.event;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.Event;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;

import java.util.List;
import java.util.Optional;

public interface EventService {

    Event save(Event event);

    ListenableFuture<Event> saveAsync(Event event);

    Optional<Event> saveIfNotExists(Event event);

    Optional<Event> findEvent(TenantId tenantId, EntityId entityId, String eventType, String eventUid);

    PageData<Event> findEvents(TenantId tenantId, EntityId entityId, TimePageLink pageLink);

    PageData<Event> findEvents(TenantId tenantId, EntityId entityId, String eventType, TimePageLink pageLink);

    List<Event> findLatestEvents(TenantId tenantId, EntityId entityId, String eventType, int limit);

    PageData<Event> findEvents(TenantId tenantId, EntityId entityId, String eventType, String bodyFilter, String dataSearch, String metadataSearch, Boolean isError, TimePageLink pageLink);

    void removeEvents(TenantId tenantId, EntityId entityId);

}
