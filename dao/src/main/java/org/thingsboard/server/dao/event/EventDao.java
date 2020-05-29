/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.Dao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The Interface EventDao.
 */
public interface EventDao extends Dao<Event> {

    /**
     * Save or update event object
     *
     * @param event the event object
     * @return saved event object
     */
    Event save(TenantId tenantId, Event event);

    /**
     * Save or update event object async
     *
     * @param event the event object
     * @return saved event object future
     */
    ListenableFuture<Event> saveAsync(Event event);

    /**
     * Save event object if it is not yet saved
     *
     * @param event the event object
     * @return saved event object
     */
    Optional<Event> saveIfNotExists(Event event);

    /**
     * Find event by tenantId, entityId and eventUid.
     *
     * @param tenantId the tenantId
     * @param entityId the entityId
     * @param eventType the eventType
     * @param eventUid the eventUid
     * @return the event
     */
    Event findEvent(UUID tenantId, EntityId entityId, String eventType, String eventUid);

    /**
     * Find events by tenantId, entityId and pageLink.
     *
     * @param tenantId the tenantId
     * @param entityId the entityId
     * @param pageLink the pageLink
     * @return the event list
     */
    List<Event> findEvents(UUID tenantId, EntityId entityId, TimePageLink pageLink);

    /**
     * Find events by tenantId, entityId, eventType and pageLink.
     *
     * @param tenantId the tenantId
     * @param entityId the entityId
     * @param eventType the eventType
     * @param pageLink the pageLink
     * @return the event list
     */
    List<Event> findEvents(UUID tenantId, EntityId entityId, String eventType, TimePageLink pageLink);

    /**
     * Find latest events by tenantId, entityId and eventType.
     *
     * @param tenantId the tenantId
     * @param entityId the entityId
     * @param eventType the eventType
     * @param limit the limit
     * @return the event list
     */
    List<Event> findLatestEvents(UUID tenantId, EntityId entityId, String eventType, int limit);

}
