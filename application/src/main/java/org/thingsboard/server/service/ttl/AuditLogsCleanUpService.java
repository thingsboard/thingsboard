// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.ttl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.server.dao.audit.AuditLogDao;
import org.thingsboard.server.dao.sqlts.insert.sql.SqlPartitioningRepository;
import org.thingsboard.server.queue.discovery.PartitionService;

import java.util.concurrent.TimeUnit;

import static org.thingsboard.server.dao.model.ModelConstants.AUDIT_LOG_TABLE_NAME;

@Service
@ConditionalOnExpression("${sql.ttl.audit_logs.enabled:true} && ${sql.ttl.audit_logs.ttl:0} > 0")
@Slf4j
public class AuditLogsCleanUpService extends AbstractCleanUpService {

    private final AuditLogDao auditLogDao;
    private final SqlPartitioningRepository partitioningRepository;

    @Value("${sql.ttl.audit_logs.ttl:0}")
    private long ttlInSec;
    @Value("${sql.audit_logs.partition_size:168}")
    private int partitionSizeInHours;

    public AuditLogsCleanUpService(PartitionService partitionService, AuditLogDao auditLogDao, SqlPartitioningRepository partitioningRepository) {
        super(partitionService);
        this.auditLogDao = auditLogDao;
        this.partitioningRepository = partitioningRepository;
    }

    @Scheduled(initialDelayString = "#{T(org.apache.commons.lang3.RandomUtils).nextLong(0, ${sql.ttl.audit_logs.checking_interval_ms})}",
            fixedDelayString = "${sql.ttl.audit_logs.checking_interval_ms}")
    public void cleanUp() {
        long auditLogsExpTime = getCurrentTimeMillis() - TimeUnit.SECONDS.toMillis(ttlInSec);
        log.debug("cleanup {}", auditLogsExpTime);
        if (isSystemTenantPartitionMine()) {
            auditLogDao.cleanUpAuditLogs(auditLogsExpTime);
        } else {
            partitioningRepository.cleanupPartitionsCache(AUDIT_LOG_TABLE_NAME, auditLogsExpTime, TimeUnit.HOURS.toMillis(partitionSizeInHours));
        }
    }

    public long getCurrentTimeMillis() {
        return System.currentTimeMillis();
    }

}
