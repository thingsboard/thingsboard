// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ttl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.sqlts.insert.sql.SqlPartitioningRepository;
import org.thingsboard.server.queue.discovery.PartitionService;

import java.util.concurrent.TimeUnit;

@Service
@ConditionalOnExpression("${sql.ttl.reports.enabled:true} && ${sql.ttl.reports.ttl:0} > 0")
public class ReportsCleanUpService extends AbstractCleanUpService {

    @Autowired
    private SqlPartitioningRepository partitioningRepository;

    @Value("${sql.ttl.reports.ttl:0}")
    private long ttlInSec;
    @Value("${sql.reports.partition_size:168}")
    private int partitionSizeInHours;

    public ReportsCleanUpService(PartitionService partitionService) {
        super(partitionService);
    }

    @Scheduled(initialDelayString = "#{T(org.apache.commons.lang3.RandomUtils).nextLong(0, ${sql.ttl.reports.checking_interval_ms})}",
            fixedDelayString = "${sql.ttl.reports.checking_interval_ms}")
    public void cleanUp() {
        long reportsExpTime = System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(ttlInSec);
        if (isSystemTenantPartitionMine()) {
            partitioningRepository.dropPartitionsBefore(ModelConstants.REPORT_TABLE_NAME, reportsExpTime, TimeUnit.HOURS.toMillis(partitionSizeInHours));
        } else {
            partitioningRepository.cleanupPartitionsCache(ModelConstants.REPORT_TABLE_NAME, reportsExpTime, TimeUnit.HOURS.toMillis(partitionSizeInHours));
        }
    }

}
