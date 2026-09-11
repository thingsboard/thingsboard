// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.ttl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.server.dao.edge.EdgeEventService;
import org.thingsboard.server.dao.sqlts.insert.sql.SqlPartitioningRepository;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.concurrent.TimeUnit;

import static org.thingsboard.server.dao.model.ModelConstants.EDGE_EVENT_TABLE_NAME;

@TbCoreComponent
@Slf4j
@Service
@ConditionalOnExpression("${edges.enabled:true} && ${sql.ttl.edge_events.edge_events_ttl:0} > 0")
public class EdgeEventsCleanUpService extends AbstractCleanUpService {

    public static final String RANDOM_DELAY_INTERVAL_MS_EXPRESSION =
            "#{T(org.apache.commons.lang3.RandomUtils).nextLong(0, ${sql.ttl.edge_events.execution_interval_ms})}";

    @Value("${sql.ttl.edge_events.edge_events_ttl}")
    private long ttl;

    @Value("${sql.edge_events.partition_size:168}")
    private int partitionSizeInHours;

    @Value("${sql.ttl.edge_events.enabled:true}")
    private boolean ttlTaskExecutionEnabled;

    private final EdgeEventService edgeEventService;

    private final SqlPartitioningRepository partitioningRepository;

    public EdgeEventsCleanUpService(PartitionService partitionService, EdgeEventService edgeEventService, SqlPartitioningRepository partitioningRepository) {
        super(partitionService);
        this.edgeEventService = edgeEventService;
        this.partitioningRepository = partitioningRepository;
    }

    @Scheduled(initialDelayString = RANDOM_DELAY_INTERVAL_MS_EXPRESSION, fixedDelayString = "${sql.ttl.edge_events.execution_interval_ms}")
    public void cleanUp() {
        long edgeEventsExpTime = System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(ttl);
        if (ttlTaskExecutionEnabled && isSystemTenantPartitionMine()) {
            edgeEventService.cleanupEvents(edgeEventsExpTime);
        } else {
            partitioningRepository.cleanupPartitionsCache(EDGE_EVENT_TABLE_NAME, edgeEventsExpTime, TimeUnit.HOURS.toMillis(partitionSizeInHours));
        }
    }

}
