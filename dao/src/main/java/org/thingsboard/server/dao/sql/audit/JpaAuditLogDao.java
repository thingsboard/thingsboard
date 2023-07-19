/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.server.dao.sql.audit;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.audit.AuditLog;
import org.thingsboard.server.common.data.id.AuditLogId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.audit.AuditLogDao;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.sql.AuditLogEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.sqlts.insert.sql.SqlPartitioningRepository;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@SqlDao
@RequiredArgsConstructor
@Slf4j
public class JpaAuditLogDao extends JpaAbstractDao<AuditLogEntity, AuditLog> implements AuditLogDao {

    private final AuditLogRepository auditLogRepository;
    private final SqlPartitioningRepository partitioningRepository;
    private final JdbcTemplate jdbcTemplate;

    @Value("${sql.audit_logs.partition_size:168}")
    private int partitionSizeInHours;
    @Value("${sql.ttl.audit_logs.ttl:0}")
    private long ttlInSec;

    private static final String TABLE_NAME = ModelConstants.AUDIT_LOG_COLUMN_FAMILY_NAME;

    @Override
    protected Class<AuditLogEntity> getEntityClass() {
        return AuditLogEntity.class;
    }

    @Override
    protected JpaRepository<AuditLogEntity, UUID> getRepository() {
        return auditLogRepository;
    }

    @Override
    public ListenableFuture<Void> saveByTenantId(AuditLog auditLog) {
        return service.submit(() -> {
            save(auditLog.getTenantId(), auditLog);
            return null;
        });
    }

    @Override
    public AuditLog save(TenantId tenantId, AuditLog auditLog) {
        if (auditLog.getId() == null) {
            UUID uuid = Uuids.timeBased();
            auditLog.setId(new AuditLogId(uuid));
            auditLog.setCreatedTime(Uuids.unixTimestamp(uuid));
        }
        partitioningRepository.createPartitionIfNotExists(TABLE_NAME, auditLog.getCreatedTime(), TimeUnit.HOURS.toMillis(partitionSizeInHours));
        return super.save(tenantId, auditLog);
    }

    @Override
    public PageData<AuditLog> findAuditLogsByTenantIdAndEntityId(UUID tenantId, EntityId entityId, List<ActionType> actionTypes, TimePageLink pageLink) {
        return DaoUtil.toPageData(
                auditLogRepository
                        .findAuditLogsByTenantIdAndEntityId(
                                tenantId,
                                entityId.getEntityType(),
                                entityId.getId(),
                                Objects.toString(pageLink.getTextSearch(), ""),
                                pageLink.getStartTime(),
                                pageLink.getEndTime(),
                                actionTypes,
                                DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<AuditLog> findAuditLogsByTenantIdAndCustomerId(UUID tenantId, CustomerId customerId, List<ActionType> actionTypes, TimePageLink pageLink) {
        return DaoUtil.toPageData(
                auditLogRepository
                        .findAuditLogsByTenantIdAndCustomerId(
                                tenantId,
                                customerId.getId(),
                                Objects.toString(pageLink.getTextSearch(), ""),
                                pageLink.getStartTime(),
                                pageLink.getEndTime(),
                                actionTypes,
                                DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<AuditLog> findAuditLogsByTenantIdAndUserId(UUID tenantId, UserId userId, List<ActionType> actionTypes, TimePageLink pageLink) {
        return DaoUtil.toPageData(
                auditLogRepository
                        .findAuditLogsByTenantIdAndUserId(
                                tenantId,
                                userId.getId(),
                                Objects.toString(pageLink.getTextSearch(), ""),
                                pageLink.getStartTime(),
                                pageLink.getEndTime(),
                                actionTypes,
                                DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<AuditLog> findAuditLogsByTenantId(UUID tenantId, List<ActionType> actionTypes, TimePageLink pageLink) {
        return DaoUtil.toPageData(
                auditLogRepository.findByTenantId(
                        tenantId,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        pageLink.getStartTime(),
                        pageLink.getEndTime(),
                        actionTypes,
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public void cleanUpAuditLogs(long expTime) {
        partitioningRepository.dropPartitionsBefore(TABLE_NAME, expTime, TimeUnit.HOURS.toMillis(partitionSizeInHours));
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

}
