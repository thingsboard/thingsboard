/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EventInfo;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.event.DebugEvent;
import org.thingsboard.server.common.data.event.ErrorEvent;
import org.thingsboard.server.common.data.event.ErrorEventFilter;
import org.thingsboard.server.common.data.event.Event;
import org.thingsboard.server.common.data.event.EventFilter;
import org.thingsboard.server.common.data.event.EventType;
import org.thingsboard.server.common.data.event.LifeCycleEventFilter;
import org.thingsboard.server.common.data.event.LifecycleEvent;
import org.thingsboard.server.common.data.event.StatisticsEvent;
import org.thingsboard.server.common.data.event.StatisticsEventFilter;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EventId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.event.EventDao;
import org.thingsboard.server.dao.sql.ScheduledLogExecutorComponent;
import org.thingsboard.server.dao.sql.TbSqlBlockingQueueParams;
import org.thingsboard.server.dao.sql.TbSqlBlockingQueueWrapper;
import org.thingsboard.server.dao.sqlts.insert.sql.SqlPartitioningRepository;
import org.thingsboard.server.dao.timeseries.SqlPartition;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

/**
 * Created by Valerii Sosliuk on 5/3/2017.
 */
@Slf4j
@Component
public class JpaBaseEventDao implements EventDao {

    private static final long PARTITION_DURATION = TimeUnit.HOURS.toMillis(1);
    private final Map<EventType, Map<Long, SqlPartition>> partitionsByEventType = new ConcurrentHashMap<>();
    private static final ReentrantLock partitionCreationLock = new ReentrantLock();

    @Autowired
    private SqlPartitioningRepository partitioningRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private LifecycleEventRepository lcEventRepository;

    @Autowired
    private StatisticsEventRepository statsEventRepository;

    @Autowired
    private ErrorEventRepository errorEventRepository;

    @Autowired
    private EventInsertRepository eventInsertRepository;

    @Autowired
    private EventCleanupRepository eventCleanupRepository;

    @Autowired
    ScheduledLogExecutorComponent logExecutor;

    @Autowired
    private StatsFactory statsFactory;

    @Value("${sql.events.batch_size:10000}")
    private int batchSize;

    @Value("${sql.events.batch_max_delay:100}")
    private long maxDelay;

    @Value("${sql.events.stats_print_interval_ms:10000}")
    private long statsPrintIntervalMs;

    @Value("${sql.events.batch_threads:3}")
    private int batchThreads;

    @Value("${sql.batch_sort:false}")
    private boolean batchSortEnabled;

    private TbSqlBlockingQueueWrapper<Event> queue;

    @PostConstruct
    private void init() {
        for (EventType eventType : EventType.values()) {
            partitionsByEventType.put(eventType, new ConcurrentHashMap<>());
        }
        TbSqlBlockingQueueParams params = TbSqlBlockingQueueParams.builder()
                .logName("Events")
                .batchSize(batchSize)
                .maxDelay(maxDelay)
                .statsPrintIntervalMs(statsPrintIntervalMs)
                .statsNamePrefix("events")
                .batchSortEnabled(batchSortEnabled)
                .build();
        Function<Event, Integer> hashcodeFunction = entity -> Objects.hash(super.hashCode(), entity.getTenantId(), entity.getEntityId());
        queue = new TbSqlBlockingQueueWrapper<>(params, hashcodeFunction, batchThreads, statsFactory);
        queue.init(logExecutor, v -> eventInsertRepository.save(v), Comparator.comparing(Event::getCreatedTime));
    }

    @PreDestroy
    private void destroy() {
        if (queue != null) {
            queue.destroy();
        }
    }

    @Override
    public ListenableFuture<Void> saveAsync(Event event) {
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
        savePartitionIfNotExist(event);
        return queue.add(event);
    }

    private void savePartitionIfNotExist(Event event) {
        var partitionsMap = partitionsByEventType.get(event.getType());
        long partitionStartTs = event.getCreatedTime() - (event.getCreatedTime() % PARTITION_DURATION);
        if (partitionsMap.get(partitionStartTs) == null) {
            long partitionEndTs = partitionStartTs + PARTITION_DURATION;
            savePartition(partitionsMap, new SqlPartition(event.getType().getTable(), partitionStartTs, partitionEndTs, Long.toString(partitionStartTs)));
        }
    }

    private void savePartition(Map<Long, SqlPartition> partitionsMap, SqlPartition sqlPartition) {
        if (!partitionsMap.containsKey(sqlPartition.getStart())) {
            partitionCreationLock.lock();
            try {
                log.trace("Saving partition: {}", sqlPartition);
                partitioningRepository.save(sqlPartition);
                log.trace("Adding partition to map: {}", sqlPartition);
                partitionsMap.put(sqlPartition.getStart(), sqlPartition);
            } catch (DataIntegrityViolationException ex) {
                log.trace("Error occurred during partition save:", ex);
                if (ex.getCause() instanceof ConstraintViolationException) {
                    log.warn("Saving partition [{}] rejected. Event data will save to the DEFAULT partition.", sqlPartition.getPartitionDate());
                    partitionsMap.put(sqlPartition.getStart(), sqlPartition);
                } else {
                    throw new RuntimeException(ex);
                }
            } finally {
                partitionCreationLock.unlock();
            }
        }
    }

    @Override
    public PageData<EventInfo> findEvents(UUID tenantId, EntityId entityId, TimePageLink pageLink) {
        return null;
//        return DaoUtil.toPageData(
//                eventRepository
//                        .findEventsByTenantIdAndEntityId(
//                                tenantId,
//                                entityId.getEntityType(),
//                                entityId.getId(),
//                                Objects.toString(pageLink.getTextSearch(), ""),
//                                pageLink.getStartTime(),
//                                pageLink.getEndTime(),
//                                DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<? extends Event> findEvents(UUID tenantId, UUID entityId, EventType eventType, TimePageLink pageLink) {
        switch (eventType) {
            case LC_EVENT:
                return findLcEventsWithoutFilter(tenantId, entityId, pageLink);
            case STATS:
                return findStatsEventsWithoutFilter(tenantId, entityId, pageLink);
            case ERROR:
                return findErrorEventsWithoutFilter(tenantId, entityId, pageLink);
            default:
                throw new RuntimeException("Event type: " + eventType + " is not supported!");
        }
    }

    private PageData<LifecycleEvent> findLcEventsWithoutFilter(UUID tenantId, UUID entityId, TimePageLink pageLink) {
        return DaoUtil.toPageData(
                lcEventRepository.findEvents(tenantId, entityId, pageLink.getStartTime(), pageLink.getEndTime(), DaoUtil.toPageable(pageLink)));
    }

    private PageData<StatisticsEvent> findStatsEventsWithoutFilter(UUID tenantId, UUID entityId, TimePageLink pageLink) {
        return DaoUtil.toPageData(
                statsEventRepository.findEventsWithoutFilter(tenantId, entityId, pageLink.getStartTime(), pageLink.getEndTime(), DaoUtil.toPageable(pageLink)));
    }

    private PageData<ErrorEvent> findErrorEventsWithoutFilter(UUID tenantId, UUID entityId, TimePageLink pageLink) {
        return DaoUtil.toPageData(
                errorEventRepository.findEvents(tenantId, entityId, pageLink.getStartTime(), pageLink.getEndTime(), DaoUtil.toPageable(pageLink)));
    }


    @Override
    public PageData<? extends Event> findEventByFilter(UUID tenantId, UUID entityId, EventFilter eventFilter, TimePageLink pageLink) {
        if (eventFilter.hasFilterForJsonBody()) {
            switch (eventFilter.getEventType()) {
                case DEBUG_RULE_NODE:
                case DEBUG_RULE_CHAIN:
                    return findEventByFilter(tenantId, entityId, (DebugEvent) eventFilter, pageLink);
                case LC_EVENT:
                    return findEventByFilter(tenantId, entityId, (LifeCycleEventFilter) eventFilter, pageLink);
                case ERROR:
                    return findEventByFilter(tenantId, entityId, (ErrorEventFilter) eventFilter, pageLink);
                case STATS:
                    return findEventByFilter(tenantId, entityId, (StatisticsEventFilter) eventFilter, pageLink);
                default:
                    throw new RuntimeException("Not supported event type: " + eventFilter.getEventType());
            }
        } else {
            return findEvents(tenantId, entityId, eventFilter.getEventType(), pageLink);
        }
    }

    private PageData<EventInfo> findEventByFilter(UUID tenantId, EntityId entityId, DebugEvent eventFilter, TimePageLink pageLink) {
        return null;
//        return DaoUtil.toPageData(
//                eventRepository.findDebugRuleNodeEvents(
//                        tenantId,
//                        entityId.getId(),
//                        entityId.getEntityType().name(),
//                        eventFilter.getEventType().name(),
//                        notNull(pageLink.getStartTime()),
//                        notNull(pageLink.getEndTime()),
//                        eventFilter.getMsgDirectionType(),
//                        eventFilter.getServer(),
//                        eventFilter.getEntityName(),
//                        eventFilter.getRelationType(),
//                        eventFilter.getEntityId(),
//                        eventFilter.getMsgType(),
//                        eventFilter.isError(),
//                        eventFilter.getErrorStr(),
//                        eventFilter.getDataSearch(),
//                        eventFilter.getMetadataSearch(),
//                        DaoUtil.toPageable(pageLink)));
    }

    private PageData<? extends Event> findEventByFilter(UUID tenantId, UUID entityId, ErrorEventFilter eventFilter, TimePageLink pageLink) {
        return DaoUtil.toPageData(
                errorEventRepository.findEvents(
                        tenantId,
                        entityId,
                        pageLink.getStartTime(),
                        pageLink.getEndTime(),
                        eventFilter.getServer(),
                        eventFilter.getMethod(),
                        eventFilter.getErrorStr(),
                        DaoUtil.toPageable(pageLink))
        );
    }

    private PageData<? extends Event> findEventByFilter(UUID tenantId, UUID entityId, LifeCycleEventFilter eventFilter, TimePageLink pageLink) {
        boolean statusFilterEnabled = !StringUtils.isEmpty(eventFilter.getStatus());
        boolean statusFilter = statusFilterEnabled && eventFilter.getStatus().equalsIgnoreCase("Success");
        return DaoUtil.toPageData(
                lcEventRepository.findEvents(
                        tenantId,
                        entityId,
                        pageLink.getStartTime(),
                        pageLink.getEndTime(),
                        eventFilter.getServer(),
                        eventFilter.getEvent(),
                        statusFilterEnabled,
                        statusFilter,
                        eventFilter.getErrorStr(),
                        DaoUtil.toPageable(pageLink))
        );
    }

    private PageData<EventInfo> findEventByFilter(UUID tenantId, EntityId entityId, StatisticsEventFilter eventFilter, TimePageLink pageLink) {
        return null;
//        return DaoUtil.toPageData(
//                eventRepository.findStatisticsEvents(
//                        tenantId,
//                        entityId.getId(),
//                        entityId.getEntityType().name(),
//                        notNull(pageLink.getStartTime()),
//                        notNull(pageLink.getEndTime()),
//                        eventFilter.getServer(),
//                        notNull(eventFilter.getMessagesProcessed()),
//                        notNull(eventFilter.getErrorsOccurred()),
//                        DaoUtil.toPageable(pageLink))
//        );
    }

    @Override
    public List<EventInfo> findLatestEvents(UUID tenantId, UUID entityId, EventType eventType, int limit) {
        return null;
//        List<EventEntity> latest = eventRepository.findLatestByTenantIdAndEntityTypeAndEntityIdAndEventType(
//                tenantId,
//                entityId.getEntityType(),
//                entityId.getId(),
//                eventType,
//                PageRequest.of(0, limit));
//        return DaoUtil.convertDataList(latest);
    }

    @Override
    public void cleanupEvents(long regularEventStartTs, long regularEventEndTs, long debugEventStartTs, long debugEventEndTs) {
        log.info("Going to cleanup old events. Interval for regular events: [{}:{}], for debug events: [{}:{}]", regularEventStartTs, regularEventEndTs, debugEventStartTs, debugEventEndTs);
        eventCleanupRepository.cleanupEvents(regularEventStartTs, regularEventEndTs, debugEventStartTs, debugEventEndTs);
    }

    private long notNull(Long value) {
        return value != null ? value : 0;
    }

    private int notNull(Integer value) {
        return value != null ? value : 0;
    }

}
