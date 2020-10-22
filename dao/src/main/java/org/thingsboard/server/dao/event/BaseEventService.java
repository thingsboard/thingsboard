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

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.Event;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TimePageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class BaseEventService implements EventService {

    private static final int MAX_DEBUG_EVENT_SYMBOLS = 4 * 1024;

    @Autowired
    public EventDao eventDao;

    @Override
    public Event save(Event event) {
        eventValidator.validate(event, Event::getTenantId);
        return eventDao.save(event.getTenantId(), event);
    }

    @Override
    public ListenableFuture<Event> saveAsync(Event event) {
        eventValidator.validate(event, Event::getTenantId);
        checkAndTruncateDebugEvent(event);
        return eventDao.saveAsync(event);
    }

    @Override
    public Optional<Event> saveIfNotExists(Event event) {
        eventValidator.validate(event, Event::getTenantId);
        if (StringUtils.isEmpty(event.getUid())) {
            throw new DataValidationException("Event uid should be specified!");
        }
        checkAndTruncateDebugEvent(event);
        return eventDao.saveIfNotExists(event);
    }

    private void checkAndTruncateDebugEvent(Event event) {
        if (event.getType().startsWith("DEBUG") && event.getBody() != null && event.getBody().has("data")) {
            String dataStr = event.getBody().get("data").asText();
            int length = dataStr.length();
            if (length > MAX_DEBUG_EVENT_SYMBOLS) {
                ((ObjectNode) event.getBody()).put("data", dataStr.substring(0, MAX_DEBUG_EVENT_SYMBOLS) + "...[truncated " + (length - MAX_DEBUG_EVENT_SYMBOLS) + " symbols]");
                log.trace("[{}] Event was truncated: {}", event.getId(), dataStr);
            }
        }
    }

    @Override
    public Optional<Event> findEvent(TenantId tenantId, EntityId entityId, String eventType, String eventUid) {
        if (tenantId == null) {
            throw new DataValidationException("Tenant id should be specified!");
        }
        if (entityId == null) {
            throw new DataValidationException("Entity id should be specified!");
        }
        if (StringUtils.isEmpty(eventType)) {
            throw new DataValidationException("Event type should be specified!");
        }
        if (StringUtils.isEmpty(eventUid)) {
            throw new DataValidationException("Event uid should be specified!");
        }
        Event event = eventDao.findEvent(tenantId.getId(), entityId, eventType, eventUid);
        return event != null ? Optional.of(event) : Optional.empty();
    }

    @Override
    public TimePageData<Event> findEvents(TenantId tenantId, EntityId entityId, TimePageLink pageLink) {
        List<Event> events = eventDao.findEvents(tenantId.getId(), entityId, pageLink);
        return new TimePageData<>(events, pageLink);
    }

    @Override
    public TimePageData<Event> findEvents(TenantId tenantId, EntityId entityId, String eventType, TimePageLink pageLink) {
        List<Event> events = eventDao.findEvents(tenantId.getId(), entityId, eventType, pageLink);
        return new TimePageData<>(events, pageLink);
    }

    @Override
    public List<Event> findLatestEvents(TenantId tenantId, EntityId entityId, String eventType, int limit) {
        return eventDao.findLatestEvents(tenantId.getId(), entityId, eventType, limit);
    }

    @Override
    public void removeEvents(TenantId tenantId, EntityId entityId) {
        TimePageData<Event> eventPageData;
        TimePageLink eventPageLink = new TimePageLink(1000);
        do {
            eventPageData = findEvents(tenantId, entityId, eventPageLink);
            for (Event event : eventPageData.getData()) {
                eventDao.removeById(tenantId, event.getUuidId());
            }
            if (eventPageData.hasNext()) {
                eventPageLink = eventPageData.getNextPageLink();
            }
        } while (eventPageData.hasNext());
    }

    private DataValidator<Event> eventValidator =
            new DataValidator<Event>() {
                @Override
                protected void validateDataImpl(TenantId tenantId, Event event) {
                    if (event.getEntityId() == null) {
                        throw new DataValidationException("Entity id should be specified!");
                    }
                    if (StringUtils.isEmpty(event.getType())) {
                        throw new DataValidationException("Event type should be specified!");
                    }
                    if (event.getBody() == null) {
                        throw new DataValidationException("Event body should be specified!");
                    }
                }
            };
}
