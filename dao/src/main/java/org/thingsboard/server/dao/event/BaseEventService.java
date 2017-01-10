/**
 * Copyright © 2016-2017 The Thingsboard Authors
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

import com.datastax.driver.core.utils.UUIDs;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.Event;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EventId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TimePageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.model.EventEntity;
import org.thingsboard.server.dao.service.DataValidator;

import java.util.List;
import java.util.Optional;

import static org.thingsboard.server.dao.DaoUtil.convertDataList;
import static org.thingsboard.server.dao.DaoUtil.getData;
import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

@Service
@Slf4j
public class BaseEventService implements EventService {

    private final TenantId systemTenantId = new TenantId(NULL_UUID);

    @Autowired
    public EventDao eventDao;

    @Override
    public Event save(Event event) {
        eventValidator.validate(event);
        if (event.getTenantId() == null) {
            log.trace("Save system event with predefined id {}", systemTenantId);
            event.setTenantId(systemTenantId);
        }
        if (event.getId() == null) {
            event.setId(new EventId(UUIDs.timeBased()));
        }
        if (StringUtils.isEmpty(event.getUid())) {
            event.setUid(event.getId().toString());
        }
        return getData(eventDao.save(event));
    }

    @Override
    public Optional<Event> saveIfNotExists(Event event) {
        eventValidator.validate(event);
        if (StringUtils.isEmpty(event.getUid())) {
            throw new DataValidationException("Event uid should be specified!.");
        }
        if (event.getTenantId() == null) {
            log.trace("Save system event with predefined id {}", systemTenantId);
            event.setTenantId(systemTenantId);
        }
        if (event.getId() == null) {
            event.setId(new EventId(UUIDs.timeBased()));
        }
        Optional<EventEntity> result = eventDao.saveIfNotExists(event);
        return result.isPresent() ? Optional.of(getData(result.get())) : Optional.empty();
    }

    @Override
    public Optional<Event> findEvent(TenantId tenantId, EntityId entityId, String eventType, String eventUid) {
        if (tenantId == null) {
            throw new DataValidationException("Tenant id should be specified!.");
        }
        if (entityId == null) {
            throw new DataValidationException("Entity id should be specified!.");
        }
        if (StringUtils.isEmpty(eventType)) {
            throw new DataValidationException("Event type should be specified!.");
        }
        if (StringUtils.isEmpty(eventUid)) {
            throw new DataValidationException("Event uid should be specified!.");
        }
        EventEntity entity = eventDao.findEvent(tenantId.getId(), entityId, eventType, eventUid);
        return entity != null ? Optional.of(getData(entity)) : Optional.empty();
    }

    @Override
    public TimePageData<Event> findEvents(TenantId tenantId, EntityId entityId, TimePageLink pageLink) {
        List<EventEntity> entities = eventDao.findEvents(tenantId.getId(), entityId, pageLink);
        List<Event> events = convertDataList(entities);
        return new TimePageData<Event>(events, pageLink);
    }


    @Override
    public TimePageData<Event> findEvents(TenantId tenantId, EntityId entityId, String eventType, TimePageLink pageLink) {
        List<EventEntity> entities = eventDao.findEvents(tenantId.getId(), entityId, eventType, pageLink);
        List<Event> events = convertDataList(entities);
        return new TimePageData<Event>(events, pageLink);
    }

    private DataValidator<Event> eventValidator =
            new DataValidator<Event>() {
                @Override
                protected void validateDataImpl(Event event) {
                    if (event.getEntityId() == null) {
                        throw new DataValidationException("Entity id should be specified!.");
                    }
                    if (StringUtils.isEmpty(event.getType())) {
                        throw new DataValidationException("Event type should be specified!.");
                    }
                    if (event.getBody() == null) {
                        throw new DataValidationException("Event body should be specified!.");
                    }
                }
            };
}
