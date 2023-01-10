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
package org.thingsboard.server.dao.sql.edge;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.id.EdgeEventId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.edge.EdgeEventDao;
import org.thingsboard.server.dao.model.sql.EdgeEventEntity;
import org.thingsboard.server.dao.sql.JpaAbstractSearchTextDao;
import org.thingsboard.server.dao.sql.ScheduledLogExecutorComponent;
import org.thingsboard.server.dao.sql.TbSqlBlockingQueueParams;
import org.thingsboard.server.dao.sql.TbSqlBlockingQueueWrapper;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

@Slf4j
@Component
public class JpaBaseEdgeEventDao extends JpaAbstractSearchTextDao<EdgeEventEntity, EdgeEvent> implements EdgeEventDao {

    private final UUID systemTenantId = NULL_UUID;

    @Autowired
    ScheduledLogExecutorComponent logExecutor;

    @Autowired
    private StatsFactory statsFactory;

    @Value("${sql.edge_events.batch_size:1000}")
    private int batchSize;

    @Value("${sql.edge_events.batch_max_delay:100}")
    private long maxDelay;

    @Value("${sql.edge_events.stats_print_interval_ms:10000}")
    private long statsPrintIntervalMs;

    private TbSqlBlockingQueueWrapper<EdgeEventEntity> queue;

    @Autowired
    private EdgeEventRepository edgeEventRepository;

    @Autowired
    private EdgeEventInsertRepository edgeEventInsertRepository;

    @Override
    protected Class<EdgeEventEntity> getEntityClass() {
        return EdgeEventEntity.class;
    }

    @Override
    protected JpaRepository<EdgeEventEntity, UUID> getRepository() {
        return edgeEventRepository;
    }

    @PostConstruct
    private void init() {
        TbSqlBlockingQueueParams params = TbSqlBlockingQueueParams.builder()
                .logName("Edge Events")
                .batchSize(batchSize)
                .maxDelay(maxDelay)
                .statsPrintIntervalMs(statsPrintIntervalMs)
                .statsNamePrefix("edge.events")
                .batchSortEnabled(true)
                .build();
        Function<EdgeEventEntity, Integer> hashcodeFunction = entity -> {
            if (entity.getEntityId() != null) {
                return entity.getEntityId().hashCode();
            } else {
                return NULL_UUID.hashCode();
            }
        };
        queue = new TbSqlBlockingQueueWrapper<>(params, hashcodeFunction, 1, statsFactory);
        queue.init(logExecutor, v -> edgeEventInsertRepository.save(v),
                Comparator.comparing(EdgeEventEntity::getTs)
        );
    }

    @PreDestroy
    private void destroy() {
        if (queue != null) {
            queue.destroy();
        }
    }

    @Override
    public ListenableFuture<Void> saveAsync(EdgeEvent edgeEvent) {
        log.debug("Save edge event [{}] ", edgeEvent);
        if (edgeEvent.getId() == null) {
            UUID timeBased = Uuids.timeBased();
            edgeEvent.setId(new EdgeEventId(timeBased));
            edgeEvent.setCreatedTime(Uuids.unixTimestamp(timeBased));
        } else if (edgeEvent.getCreatedTime() == 0L) {
            UUID eventId = edgeEvent.getId().getId();
            if (eventId.version() == 1) {
                edgeEvent.setCreatedTime(Uuids.unixTimestamp(eventId));
            } else {
                edgeEvent.setCreatedTime(System.currentTimeMillis());
            }
        }
        if (StringUtils.isEmpty(edgeEvent.getUid())) {
            edgeEvent.setUid(edgeEvent.getId().toString());
        }
        return save(new EdgeEventEntity(edgeEvent));
    }

    private ListenableFuture<Void> save(EdgeEventEntity entity) {
        log.debug("Save edge event [{}] ", entity);
        if (entity.getTenantId() == null) {
            log.trace("Save system edge event with predefined id {}", systemTenantId);
            entity.setTenantId(systemTenantId);
        }
        if (entity.getUuid() == null) {
            entity.setUuid(Uuids.timeBased());
        }

        return addToQueue(entity);
    }

    private ListenableFuture<Void> addToQueue(EdgeEventEntity entity) {
        return queue.add(entity);
    }


    @Override
    public PageData<EdgeEvent> findEdgeEvents(UUID tenantId, EdgeId edgeId, TimePageLink pageLink, boolean withTsUpdate) {
        if (withTsUpdate) {
            return DaoUtil.toPageData(
                    edgeEventRepository
                            .findEdgeEventsByTenantIdAndEdgeId(
                                    tenantId,
                                    edgeId.getId(),
                                    Objects.toString(pageLink.getTextSearch(), ""),
                                    pageLink.getStartTime(),
                                    pageLink.getEndTime(),
                                    DaoUtil.toPageable(pageLink)));
        } else {
            return DaoUtil.toPageData(
                    edgeEventRepository
                            .findEdgeEventsByTenantIdAndEdgeIdWithoutTimeseriesUpdated(
                                    tenantId,
                                    edgeId.getId(),
                                    Objects.toString(pageLink.getTextSearch(), ""),
                                    pageLink.getStartTime(),
                                    pageLink.getEndTime(),
                                    DaoUtil.toPageable(pageLink)));

        }
    }

    @Override
    public void cleanupEvents(long ttl) {
        log.info("Going to cleanup old edge events using ttl: {}s", ttl);
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement("call cleanup_edge_events_by_ttl(?,?)")) {
            stmt.setLong(1, ttl);
            stmt.setLong(2, 0);
            stmt.setQueryTimeout((int) TimeUnit.HOURS.toSeconds(1));
            stmt.execute();
            printWarnings(stmt);
            try (ResultSet resultSet = stmt.getResultSet()) {
                resultSet.next();
                log.info("Total edge events removed by TTL: [{}]", resultSet.getLong(1));
            }
        } catch (SQLException e) {
            log.error("SQLException occurred during edge events TTL task execution ", e);
        }
    }
}
