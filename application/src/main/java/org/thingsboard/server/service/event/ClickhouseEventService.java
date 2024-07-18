/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.service.event;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EventInfo;
import org.thingsboard.server.common.data.event.Event;
import org.thingsboard.server.common.data.event.EventFilter;
import org.thingsboard.server.common.data.event.EventType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EventId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.common.stats.DefaultCounter;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.dao.event.EventDao;
import org.thingsboard.server.dao.event.EventService;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.queue.TbQueueMsg;
import org.thingsboard.server.queue.TbQueueMsgHeaders;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.DefaultTbQueueMsg;
import org.thingsboard.server.queue.common.DefaultTbQueueMsgHeaders;
import org.thingsboard.server.queue.provider.TbQueueProducerProvider;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author baigod
 * @version : ClickhouseEventService.java, v 0.1 2024年05月30日 23:02 baigod Exp $
 */
@Service
@Slf4j
@ConditionalOnProperty(prefix = "event.debug", value = "type", havingValue = "clickhouse")
@RequiredArgsConstructor
public class ClickhouseEventService implements EventService {

    final TbQueueProducerProvider producerProvider;
    final DataValidator<Event> eventValidator;
    final EventDao eventDao;
    final StatsFactory statsFactory;

    private final Map<EventType, DefaultCounter> ckEventCounterMap = new ConcurrentHashMap<>();

    private TbQueueProducer<DefaultTbQueueMsg> tbEventClickhouseProducer;

    @PostConstruct
    public void init() {
        this.tbEventClickhouseProducer = producerProvider.getTbEventClickhouseMsgProducer();
    }

    @PreDestroy
    public void destroy() {
        this.tbEventClickhouseProducer.stop();
    }

    @Override
    public ListenableFuture<Void> saveAsync(Event event) {
        eventValidator.validate(event, Event::getTenantId);

        log.trace("Save event [{}] ", event);
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

        TopicPartitionInfo tpi = new TopicPartitionInfo(tbEventClickhouseProducer.getDefaultTopic(), null, null, false);

        TbQueueMsg tbQueueMsg = new TbQueueMsg() {
            @Override
            public UUID getKey() {
                return event.getId().getId();
            }

            @Override
            public TbQueueMsgHeaders getHeaders() {
                return new DefaultTbQueueMsgHeaders();
            }

            @Override
            public byte[] getData() {
                return JacksonUtil.writeValueAsBytes(EventConverter.convert(event));
            }
        };


        tbEventClickhouseProducer.send(tpi, new DefaultTbQueueMsg(tbQueueMsg), null);

        ckEventCounterMap.computeIfAbsent(event.getType(),
                        k -> statsFactory.createDefaultCounter("clickhouse_event_counter", "eventType", event.getType().getTable()))
                .increment();

        return Futures.immediateFuture(null);
    }

    @Override
    public PageData<EventInfo> findEvents(TenantId tenantId, EntityId entityId, EventType eventType, TimePageLink pageLink) {
        return convert(entityId.getEntityType(), eventDao.findEvents(tenantId.getId(), entityId.getId(), eventType, pageLink));
    }

    @Override
    public List<EventInfo> findLatestEvents(TenantId tenantId, EntityId entityId, EventType eventType, int limit) {
        return convert(entityId.getEntityType(), eventDao.findLatestEvents(tenantId.getId(), entityId.getId(), eventType, limit));
    }

    @Override
    public EventInfo findLatestDebugRuleNodeInEvent(TenantId tenantId, EntityId entityId) {
        return convert(entityId.getEntityType(), eventDao.findLatestDebugRuleNodeInEvent(tenantId.getId(), entityId.getId()));
    }

    @Override
    public PageData<EventInfo> findEventsByFilter(TenantId tenantId, EntityId entityId, EventFilter eventFilter, TimePageLink pageLink) {
        return convert(entityId.getEntityType(), eventDao.findEventByFilter(tenantId.getId(), entityId.getId(), eventFilter, pageLink));
    }

    @Override
    public void removeEvents(TenantId tenantId, EntityId entityId) {
        removeEvents(tenantId, entityId, null, null, null);
    }

    @Override
    public void removeEvents(TenantId tenantId, EntityId entityId, EventFilter eventFilter, Long startTime, Long endTime) {
        if (eventFilter == null) {
            eventDao.removeEvents(tenantId.getId(), entityId.getId(), startTime, endTime);
        } else {
            eventDao.removeEvents(tenantId.getId(), entityId.getId(), eventFilter, startTime, endTime);
        }
    }

    @Override
    public void cleanupEvents(long regularEventExpTs, long debugEventExpTs, boolean cleanupDb) {

    }

    private PageData<EventInfo> convert(EntityType entityType, PageData<? extends Event> pd) {
        return new PageData<>(pd.getData() == null ? null :
                pd.getData().stream().map(e -> e.toInfo(entityType)).collect(Collectors.toList())
                , pd.getTotalPages(), pd.getTotalElements(), pd.hasNext());
    }

    private List<EventInfo> convert(EntityType entityType, List<? extends Event> list) {
        return list == null ? null : list.stream().map(e -> e.toInfo(entityType)).collect(Collectors.toList());
    }

    private EventInfo convert(EntityType entityType, Event event) {
        return Optional.ofNullable(event).map(e -> e.toInfo(entityType)).orElse(null);
    }

}
