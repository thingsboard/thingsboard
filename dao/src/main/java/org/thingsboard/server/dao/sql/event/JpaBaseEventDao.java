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
package org.thingsboard.server.dao.sql.event;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.Event;
import org.thingsboard.server.common.data.event.DebugRuleNodeEvent;
import org.thingsboard.server.common.data.event.ErrorEvent;
import org.thingsboard.server.common.data.event.EventProvisionConfiguration;
import org.thingsboard.server.common.data.event.LifeCycleEvent;
import org.thingsboard.server.common.data.event.StatisticsEvent;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EventId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.event.EventDao;
import org.thingsboard.server.dao.model.sql.EventEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

/**
 * Created by Valerii Sosliuk on 5/3/2017.
 */
@Slf4j
@Component
public class JpaBaseEventDao extends JpaAbstractDao<EventEntity, Event> implements EventDao {

    private final UUID systemTenantId = NULL_UUID;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventInsertRepository eventInsertRepository;

    @Override
    protected Class<EventEntity> getEntityClass() {
        return EventEntity.class;
    }

    @Override
    protected CrudRepository<EventEntity, UUID> getCrudRepository() {
        return eventRepository;
    }

    @Override
    public Event save(TenantId tenantId, Event event) {
        log.debug("Save event [{}] ", event);
        if (event.getId() == null) {
            UUID timeBased = Uuids.timeBased();
            event.setId(new EventId(timeBased));
            event.setCreatedTime(Uuids.unixTimestamp(timeBased));
        } else if (event.getCreatedTime() == 0L) {
            UUID eventId = event.getId().getId();
            if (eventId.version() == 1) {
                event.setCreatedTime(Uuids.unixTimestamp(eventId));
            } else {
                event.setCreatedTime(System.currentTimeMillis());
            }
        }
        if (StringUtils.isEmpty(event.getUid())) {
            event.setUid(event.getId().toString());
        }
        return save(new EventEntity(event), false).orElse(null);
    }

    @Override
    public ListenableFuture<Event> saveAsync(Event event) {
        log.debug("Save event [{}] ", event);
        if (event.getId() == null) {
            UUID timeBased = Uuids.timeBased();
            event.setId(new EventId(timeBased));
            event.setCreatedTime(Uuids.unixTimestamp(timeBased));
        } else if (event.getCreatedTime() == 0L) {
            UUID eventId = event.getId().getId();
            if (eventId.version() == 1) {
                event.setCreatedTime(Uuids.unixTimestamp(eventId));
            } else {
                event.setCreatedTime(System.currentTimeMillis());
            }
        }
        if (StringUtils.isEmpty(event.getUid())) {
            event.setUid(event.getId().toString());
        }
        return service.submit(() -> save(new EventEntity(event), false).orElse(null));
    }

    @Override
    public Optional<Event> saveIfNotExists(Event event) {
        return save(new EventEntity(event), true);
    }

    @Override
    public Event findEvent(UUID tenantId, EntityId entityId, String eventType, String eventUid) {
        return DaoUtil.getData(eventRepository.findByTenantIdAndEntityTypeAndEntityIdAndEventTypeAndEventUid(
                tenantId, entityId.getEntityType(), entityId.getId(), eventType, eventUid));
    }

    @Override
    public PageData<Event> findEvents(UUID tenantId, EntityId entityId, TimePageLink pageLink) {
        return DaoUtil.toPageData(
                eventRepository
                        .findEventsByTenantIdAndEntityId(
                                tenantId,
                                entityId.getEntityType(),
                                entityId.getId(),
                                Objects.toString(pageLink.getTextSearch(), ""),
                                pageLink.getStartTime(),
                                pageLink.getEndTime(),
                                DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<Event> findEvents(UUID tenantId, EntityId entityId, String eventType, TimePageLink pageLink) {
        return DaoUtil.toPageData(
                eventRepository
                        .findEventsByTenantIdAndEntityIdAndEventType(
                                tenantId,
                                entityId.getEntityType(),
                                entityId.getId(),
                                eventType,
                                pageLink.getStartTime(),
                                pageLink.getEndTime(),
                                DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<Event> findEventByFilter(UUID tenantId, EntityId entityId, EventProvisionConfiguration eventProvisionConfiguration, TimePageLink pageLink) {
        switch (eventProvisionConfiguration.getEventType()) {
            case DEBUG_RULE_NODE:
                return DaoUtil.toPageData(
                    eventRepository.findDebugRuleNodeEvents(
                            tenantId,
                            entityId.getId(),
                            entityId.getEntityType().name(),
                            pageLink.getStartTime(),
                            pageLink.getEndTime(),
                            ((DebugRuleNodeEvent) eventProvisionConfiguration).getMsgDirectionType(),
                            ((DebugRuleNodeEvent) eventProvisionConfiguration).getServer(),
                            ((DebugRuleNodeEvent) eventProvisionConfiguration).getEntityName(),
                            ((DebugRuleNodeEvent) eventProvisionConfiguration).getRelationType(),
                            ((DebugRuleNodeEvent) eventProvisionConfiguration).getEntityId(),
                            ((DebugRuleNodeEvent) eventProvisionConfiguration).getMsgType(),
                            ((DebugRuleNodeEvent) eventProvisionConfiguration).isError(),
                            ((DebugRuleNodeEvent) eventProvisionConfiguration).getDataSearch(),
                            ((DebugRuleNodeEvent) eventProvisionConfiguration).getMetadataSearch(),
                            DaoUtil.toPageable(pageLink))
            );
            case LC_EVENT:
                return DaoUtil.toPageData(
                    eventRepository.findLifeCycleEvents(
                            tenantId,
                            entityId.getId(),
                            entityId.getEntityType().name(),
                            pageLink.getStartTime(),
                            pageLink.getEndTime(),
                            ((LifeCycleEvent) eventProvisionConfiguration).isError(),
                            ((LifeCycleEvent) eventProvisionConfiguration).getStatus(),
                            ((LifeCycleEvent) eventProvisionConfiguration).getServer(),
                            DaoUtil.toPageable(pageLink))
                    );

            case ERROR:
                return DaoUtil.toPageData(
                        eventRepository.findErrorEvents(
                                tenantId,
                                entityId.getId(),
                                entityId.getEntityType().name(),
                                pageLink.getStartTime(),
                                pageLink.getEndTime(),
                                ((ErrorEvent) eventProvisionConfiguration).getServer(),
                                ((ErrorEvent) eventProvisionConfiguration).getMethod(),
                                DaoUtil.toPageable(pageLink))
                );
            case STATS:
                return DaoUtil.toPageData(
                    eventRepository.findStatisticsEvents(
                            tenantId,
                            entityId.getId(),
                            entityId.getEntityType().name(),
                            pageLink.getStartTime(),
                            pageLink.getEndTime(),
                            ((StatisticsEvent) eventProvisionConfiguration).getServer(),
                            ((StatisticsEvent) eventProvisionConfiguration).getMessagesProcessed(),
                            ((StatisticsEvent) eventProvisionConfiguration).getErrorsOccured(),
                            DaoUtil.toPageable(pageLink))
            );
            default: throw new RuntimeException("Not supported event type: " + eventProvisionConfiguration.getEventType());
        }
    }

    @Override
    public List<Event> findLatestEvents(UUID tenantId, EntityId entityId, String eventType, int limit) {
        List<EventEntity> latest = eventRepository.findLatestByTenantIdAndEntityTypeAndEntityIdAndEventType(
                tenantId,
                entityId.getEntityType(),
                entityId.getId(),
                eventType,
                PageRequest.of(0, limit));
        return DaoUtil.convertDataList(latest);
    }

    public Optional<Event> save(EventEntity entity, boolean ifNotExists) {
        log.debug("Save event [{}] ", entity);
        if (entity.getTenantId() == null) {
            log.trace("Save system event with predefined id {}", systemTenantId);
            entity.setTenantId(systemTenantId);
        }
        if (entity.getUuid() == null) {
            entity.setUuid(Uuids.timeBased());
        }
        if (StringUtils.isEmpty(entity.getEventUid())) {
            entity.setEventUid(entity.getUuid().toString());
        }
        if (ifNotExists &&
                eventRepository.findByTenantIdAndEntityTypeAndEntityId(entity.getTenantId(), entity.getEntityType(), entity.getEntityId()) != null) {
            return Optional.empty();
        }
        return Optional.of(DaoUtil.getData(eventInsertRepository.saveOrUpdate(entity)));
    }

}
