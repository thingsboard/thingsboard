/*
Author Ahmet Ertuğrul KAYA
*/
package org.thingsboard.server.update.dao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.audit.AuditLog;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.sql.AuditLogEntity;
import org.thingsboard.server.dao.sql.JpaPartitionedAbstractDao;
import org.thingsboard.server.dao.sql.audit.AuditLogRepository;
import org.thingsboard.server.dao.sqlts.insert.sql.SqlPartitioningRepository;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@SqlDao
@RequiredArgsConstructor
@Slf4j
public class JpaUpgradeAuditLogDao extends JpaPartitionedAbstractDao<AuditLogEntity, AuditLog> implements AuditLogDao {

    private final AuditLogRepository auditLogRepository;
    private final SqlPartitioningRepository partitioningRepository;
    private final JdbcTemplate jdbcTemplate;

    @Value("${sql.audit_logs.partition_size:168}")
    private int partitionSizeInHours;
    @Value("${sql.ttl.audit_logs.ttl:0}")
    private long ttlInSec;

    private static final String TABLE_NAME = ModelConstants.AUDIT_LOG_TABLE_NAME;

    @Override
    protected Class<AuditLogEntity> getEntityClass() {
        return AuditLogEntity.class;
    }

    @Override
    protected JpaRepository<AuditLogEntity, UUID> getRepository() {
        return auditLogRepository;
    }

    @Override
    public void migrateAuditLogs() {
        long startTime = ttlInSec > 0 ? System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(ttlInSec) : 1480982400000L;

        long currentTime = System.currentTimeMillis();
        var partitionStepInMs = TimeUnit.HOURS.toMillis(partitionSizeInHours);
        long numberOfPartitions = (currentTime - startTime) / partitionStepInMs;

        if (numberOfPartitions > 1000) {
            String error = "Please adjust your audit logs partitioning configuration. Configuration with partition size " +
                    "of " + partitionSizeInHours + " hours and corresponding TTL will use " + numberOfPartitions + " " +
                    "(> 1000) partitions which is not recommended!";
            log.error(error);
            throw new RuntimeException(error);
        }

        while (startTime < currentTime) {
            var endTime = startTime + partitionStepInMs;
            log.info("Migrating audit logs for time period: {} - {}", startTime, endTime);
            callMigrationFunction(startTime, endTime, partitionStepInMs);
            startTime = endTime;
        }
        log.info("Audit logs migration finished");

        jdbcTemplate.execute("DROP TABLE IF EXISTS old_audit_log");
    }

    private void callMigrationFunction(long startTime, long endTime, long partitionSizeInMs) {
        jdbcTemplate.update("CALL migrate_audit_logs(?, ?, ?)", startTime, endTime, partitionSizeInMs);
    }

    @Override
    public void createPartition(AuditLogEntity entity) {
        partitioningRepository.createPartitionIfNotExists(TABLE_NAME, entity.getCreatedTime(), TimeUnit.HOURS.toMillis(partitionSizeInHours));
    }

}
