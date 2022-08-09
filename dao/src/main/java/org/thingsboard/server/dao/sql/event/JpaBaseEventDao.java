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
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.event.RuleChainDebugEventFilter;
import org.thingsboard.server.common.data.event.RuleNodeDebugEventFilter;
import org.thingsboard.server.common.data.event.ErrorEventFilter;
import org.thingsboard.server.common.data.event.Event;
import org.thingsboard.server.common.data.event.EventFilter;
import org.thingsboard.server.common.data.event.EventType;
import org.thingsboard.server.common.data.event.LifeCycleEventFilter;
import org.thingsboard.server.common.data.event.StatisticsEventFilter;
import org.thingsboard.server.common.data.id.EventId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.event.EventDao;
import org.thingsboard.server.dao.model.sql.AssetInfoEntity;
import org.thingsboard.server.dao.model.sql.EventEntity;
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
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

/**
 * Created by Valerii Sosliuk on 5/3/2017.
 */
@Slf4j
@Component
public class JpaBaseEventDao implements EventDao {

    private final Map<EventType, Map<Long, SqlPartition>> partitionsByEventType = new ConcurrentHashMap<>();
    private static final ReentrantLock partitionCreationLock = new ReentrantLock();

    @Autowired
    private EventPartitionConfiguration partitionConfiguration;

    @Autowired
    private SqlPartitioningRepository partitioningRepository;

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
    private RuleNodeDebugEventRepository ruleNodeDebugEventRepository;

    @Autowired
    private RuleChainDebugEventRepository ruleChainDebugEventRepository;

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

    private final Map<EventType, EventRepository<?, ?>> repositories = new ConcurrentHashMap<>();

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
        repositories.put(EventType.LC_EVENT, lcEventRepository);
        repositories.put(EventType.STATS, statsEventRepository);
        repositories.put(EventType.ERROR, errorEventRepository);
        repositories.put(EventType.DEBUG_RULE_NODE, ruleNodeDebugEventRepository);
        repositories.put(EventType.DEBUG_RULE_CHAIN, ruleChainDebugEventRepository);
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
        EventType type = event.getType();
        var partitionsMap = partitionsByEventType.get(type);
        var partitionDuration = partitionConfiguration.getPartitionSizeInMs(type);
        long partitionStartTs = event.getCreatedTime() - (event.getCreatedTime() % partitionDuration);
        if (partitionsMap.get(partitionStartTs) == null) {
            savePartition(partitionsMap, new SqlPartition(type.getTable(), partitionStartTs, partitionStartTs + partitionDuration, Long.toString(partitionStartTs)));
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
    public PageData<? extends Event> findEvents(UUID tenantId, UUID entityId, EventType eventType, TimePageLink pageLink) {
        return DaoUtil.toPageData(getEventRepository(eventType).findEvents(tenantId, entityId, pageLink.getStartTime(), pageLink.getEndTime(), DaoUtil.toPageable(pageLink, EventEntity.eventColumnMap)));
    }

    @Override
    public PageData<? extends Event> findEventByFilter(UUID tenantId, UUID entityId, EventFilter eventFilter, TimePageLink pageLink) {
        if (eventFilter.isNotEmpty()) {
            switch (eventFilter.getEventType()) {
                case DEBUG_RULE_NODE:
                    return findEventByFilter(tenantId, entityId, (RuleNodeDebugEventFilter) eventFilter, pageLink);
                case DEBUG_RULE_CHAIN:
                    return findEventByFilter(tenantId, entityId, (RuleChainDebugEventFilter) eventFilter, pageLink);
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

    @Override
    public void removeEvents(UUID tenantId, UUID entityId, Long startTime, Long endTime) {
        log.debug("[{}][{}] Remove events [{}-{}] ", tenantId, entityId, startTime, endTime);
        for (EventType eventType : EventType.values()) {
            getEventRepository(eventType).removeEvents(tenantId, entityId, startTime, endTime);
        }
    }

    @Override
    public void removeEvents(UUID tenantId, UUID entityId, EventFilter eventFilter, Long startTime, Long endTime) {
        if (eventFilter.isNotEmpty()) {
            switch (eventFilter.getEventType()) {
                case DEBUG_RULE_NODE:
                    removeEventsByFilter(tenantId, entityId, (RuleNodeDebugEventFilter) eventFilter, startTime, endTime);
                    break;
                case DEBUG_RULE_CHAIN:
                    removeEventsByFilter(tenantId, entityId, (RuleChainDebugEventFilter) eventFilter, startTime, endTime);
                    break;
                case LC_EVENT:
                    removeEventsByFilter(tenantId, entityId, (LifeCycleEventFilter) eventFilter, startTime, endTime);
                    break;
                case ERROR:
                    removeEventsByFilter(tenantId, entityId, (ErrorEventFilter) eventFilter, startTime, endTime);
                    break;
                case STATS:
                    removeEventsByFilter(tenantId, entityId, (StatisticsEventFilter) eventFilter, startTime, endTime);
                    break;
                default:
                    throw new RuntimeException("Not supported event type: " + eventFilter.getEventType());
            }
        } else {
            getEventRepository(eventFilter.getEventType()).removeEvents(tenantId, entityId, startTime, endTime);
        }
    }

    @Override
    public void migrateEvents(long regularEventTs, long debugEventTs) {
        eventCleanupRepository.migrateEvents(regularEventTs, debugEventTs);
    }

    private PageData<? extends Event> findEventByFilter(UUID tenantId, UUID entityId, RuleChainDebugEventFilter eventFilter, TimePageLink pageLink) {
        return DaoUtil.toPageData(
                ruleChainDebugEventRepository.findEvents(
                        tenantId,
                        entityId,
                        pageLink.getStartTime(),
                        pageLink.getEndTime(),
                        eventFilter.getServer(),
                        eventFilter.getMessage(),
                        eventFilter.isError(),
                        eventFilter.getErrorStr(),
                        DaoUtil.toPageable(pageLink, EventEntity.eventColumnMap)));
    }

    private PageData<? extends Event> findEventByFilter(UUID tenantId, UUID entityId, RuleNodeDebugEventFilter eventFilter, TimePageLink pageLink) {
        parseUUID(eventFilter.getEntityId(), "Entity Id");
        parseUUID(eventFilter.getMsgId(), "Message Id");
        return DaoUtil.toPageData(
                ruleNodeDebugEventRepository.findEvents(
                        tenantId,
                        entityId,
                        pageLink.getStartTime(),
                        pageLink.getEndTime(),
                        eventFilter.getServer(),
                        eventFilter.getMsgDirectionType(),
                        eventFilter.getEntityId(),
                        eventFilter.getEntityType(),
                        eventFilter.getMsgId(),
                        eventFilter.getMsgType(),
                        eventFilter.getRelationType(),
                        eventFilter.getDataSearch(),
                        eventFilter.getMetadataSearch(),
                        eventFilter.isError(),
                        eventFilter.getErrorStr(),
                        DaoUtil.toPageable(pageLink, EventEntity.eventColumnMap)));
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
                        DaoUtil.toPageable(pageLink, EventEntity.eventColumnMap))
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
                        DaoUtil.toPageable(pageLink, EventEntity.eventColumnMap))
        );
    }

    private PageData<? extends Event> findEventByFilter(UUID tenantId, UUID entityId, StatisticsEventFilter eventFilter, TimePageLink pageLink) {
        return DaoUtil.toPageData(
                statsEventRepository.findEvents(
                        tenantId,
                        entityId,
                        pageLink.getStartTime(),
                        pageLink.getEndTime(),
                        eventFilter.getServer(),
                        eventFilter.getMinMessagesProcessed(),
                        eventFilter.getMaxMessagesProcessed(),
                        eventFilter.getMinErrorsOccurred(),
                        eventFilter.getMaxErrorsOccurred(),
                        DaoUtil.toPageable(pageLink, EventEntity.eventColumnMap))
        );
    }

    private void removeEventsByFilter(UUID tenantId, UUID entityId, RuleChainDebugEventFilter eventFilter, Long startTime, Long endTime) {
        ruleChainDebugEventRepository.removeEvents(
                tenantId,
                entityId,
                startTime,
                endTime,
                eventFilter.getServer(),
                eventFilter.getMessage(),
                eventFilter.isError(),
                eventFilter.getErrorStr());
    }

    private void removeEventsByFilter(UUID tenantId, UUID entityId, RuleNodeDebugEventFilter eventFilter, Long startTime, Long endTime) {
        parseUUID(eventFilter.getEntityId(), "Entity Id");
        parseUUID(eventFilter.getMsgId(), "Message Id");
        ruleNodeDebugEventRepository.removeEvents(
                tenantId,
                entityId,
                startTime,
                endTime,
                eventFilter.getServer(),
                eventFilter.getMsgDirectionType(),
                eventFilter.getEntityId(),
                eventFilter.getEntityType(),
                eventFilter.getMsgId(),
                eventFilter.getMsgType(),
                eventFilter.getRelationType(),
                eventFilter.getDataSearch(),
                eventFilter.getMetadataSearch(),
                eventFilter.isError(),
                eventFilter.getErrorStr());
    }

    private void removeEventsByFilter(UUID tenantId, UUID entityId, ErrorEventFilter eventFilter, Long startTime, Long endTime) {
        errorEventRepository.removeEvents(
                tenantId,
                entityId,
                startTime,
                endTime,
                eventFilter.getServer(),
                eventFilter.getMethod(),
                eventFilter.getErrorStr());

    }

    private void removeEventsByFilter(UUID tenantId, UUID entityId, LifeCycleEventFilter eventFilter, Long startTime, Long endTime) {
        boolean statusFilterEnabled = !StringUtils.isEmpty(eventFilter.getStatus());
        boolean statusFilter = statusFilterEnabled && eventFilter.getStatus().equalsIgnoreCase("Success");
        lcEventRepository.removeEvents(
                tenantId,
                entityId,
                startTime,
                endTime,
                eventFilter.getServer(),
                eventFilter.getEvent(),
                statusFilterEnabled,
                statusFilter,
                eventFilter.getErrorStr());
    }

    private void removeEventsByFilter(UUID tenantId, UUID entityId, StatisticsEventFilter eventFilter, Long startTime, Long endTime) {
        statsEventRepository.removeEvents(
                tenantId,
                entityId,
                startTime,
                endTime,
                eventFilter.getServer(),
                eventFilter.getMinMessagesProcessed(),
                eventFilter.getMaxMessagesProcessed(),
                eventFilter.getMinErrorsOccurred(),
                eventFilter.getMaxErrorsOccurred()
        );
    }

    @Override
    public List<? extends Event> findLatestEvents(UUID tenantId, UUID entityId, EventType eventType, int limit) {
        return DaoUtil.convertDataList(getEventRepository(eventType).findLatestEvents(tenantId, entityId, limit));
    }

    @Override
    public void cleanupEvents(long regularEventExpTs, long debugEventExpTs, boolean cleanupDb) {
        if (regularEventExpTs > 0) {
            log.info("Going to cleanup regular events with exp time: {}", regularEventExpTs);
            if (cleanupDb) {
                eventCleanupRepository.cleanupEvents(regularEventExpTs, false);
            }
            cleanupPartitions(regularEventExpTs, false);
        }
        if (debugEventExpTs > 0) {
            log.info("Going to cleanup debug events with exp time: {}", debugEventExpTs);
            if (cleanupDb) {
                eventCleanupRepository.cleanupEvents(debugEventExpTs, true);
            }
            cleanupPartitions(debugEventExpTs, true);
        }
    }

    private void cleanupPartitions(long expTime, boolean isDebug) {
        for (EventType eventType : EventType.values()) {
            if (eventType.isDebug() == isDebug) {
                Map<Long, SqlPartition> partitions = partitionsByEventType.get(eventType);
                partitions.keySet().removeIf(startTs -> startTs + partitionConfiguration.getPartitionSizeInMs(eventType) < expTime);
            }
        }
    }

    private void parseUUID(String src, String paramName) {
        if (!StringUtils.isEmpty(src)) {
            try {
                UUID.fromString(src);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Failed to convert " + paramName + " to UUID!");
            }
        }
    }

    private EventRepository<? extends EventEntity<?>, ?> getEventRepository(EventType eventType) {
        var repository = repositories.get(eventType);
        if (repository == null) {
            throw new RuntimeException("Event type: " + eventType + " is not supported!");
        }
        return repository;
    }


}
