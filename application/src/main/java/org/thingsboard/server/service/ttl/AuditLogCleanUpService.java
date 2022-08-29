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
