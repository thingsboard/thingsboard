// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.ttl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.util.TbCoreComponent;

@TbCoreComponent
@Slf4j
@Service
public class TimeseriesCleanUpService extends AbstractCleanUpService {

    @Value("${sql.ttl.ts.ts_key_value_ttl}")
    protected long systemTtl;

    @Value("${sql.ttl.ts.enabled}")
    private boolean ttlTaskExecutionEnabled;

    private final TimeseriesService timeseriesService;

    public TimeseriesCleanUpService(PartitionService partitionService, TimeseriesService timeseriesService) {
        super(partitionService);
        this.timeseriesService = timeseriesService;
    }

    @Scheduled(initialDelayString = "${sql.ttl.ts.execution_interval_ms}", fixedDelayString = "${sql.ttl.ts.execution_interval_ms}")
    public void cleanUp() {
        if (ttlTaskExecutionEnabled && isSystemTenantPartitionMine()) {
            timeseriesService.cleanup(systemTtl);
        }
    }

}