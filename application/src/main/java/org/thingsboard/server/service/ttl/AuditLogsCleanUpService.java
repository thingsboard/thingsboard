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
package org.thingsboard.server.service.ttl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.server.dao.audit.AuditLogDao;
import org.thingsboard.server.dao.sqlts.insert.sql.SqlPartitioningRepository;
import org.thingsboard.server.queue.discovery.PartitionService;

import java.util.concurrent.TimeUnit;

import static org.thingsboard.server.dao.model.ModelConstants.AUDIT_LOG_COLUMN_FAMILY_NAME;

@Service
@ConditionalOnProperty(name = "sql.ttl.audit_logs.enabled", havingValue = "true")
@ConditionalOnExpression("${sql.ttl.audit_logs.ttl:0} > 0")
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
        long auditLogsExpTime = System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(ttlInSec);
        if (isSystemTenantPartitionMine()) {
            auditLogDao.cleanUpAuditLogs(auditLogsExpTime);
        } else {
            partitioningRepository.cleanupPartitionsCache(AUDIT_LOG_COLUMN_FAMILY_NAME, auditLogsExpTime, TimeUnit.HOURS.toMillis(partitionSizeInHours));
        }
    }

}
