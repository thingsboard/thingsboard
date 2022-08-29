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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.server.dao.audit.AuditLogService;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.util.TbCoreComponent;

@TbCoreComponent
@Slf4j
@Service
public class AuditLogCleanUpService extends AbstractCleanUpService{

    public static final String RANDOM_DELAY_INTERVAL_MS_EXPRESSION =
            "#{T(org.apache.commons.lang3.RandomUtils).nextLong(0, ${sql.ttl.audit_log.execution_interval_ms})}";

    @Value("${sql.ttl.audit_log.enabled:false}")
    private boolean ttlTaskExecutionEnabled;

    @Value("${sql.ttl.audit_log.events_ttl:0}")
    private long ttl;

    private final AuditLogService auditLogService;

    public AuditLogCleanUpService(PartitionService partitionService, AuditLogService auditLogService) {
        super(partitionService);
        this.auditLogService = auditLogService;
    }

    @Scheduled(initialDelayString = RANDOM_DELAY_INTERVAL_MS_EXPRESSION, fixedDelayString = "${sql.ttl.audit_log.execution_interval_ms}")
    public void cleanUp() {
        if (ttlTaskExecutionEnabled && isSystemTenantPartitionMine()) {
            auditLogService.cleanUp(ttl);
        }
    }

}
