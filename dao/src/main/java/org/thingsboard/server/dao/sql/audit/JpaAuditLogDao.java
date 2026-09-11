// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.sql.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.audit.AuditLog;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.audit.AuditLogDao;
import org.thingsboard.server.dao.config.DefaultDataSource;
import org.thingsboard.server.dao.model.sql.AuditLogEntity;
import org.thingsboard.server.dao.sql.JpaPartitionedAbstractDao;
import org.thingsboard.server.dao.sqlts.insert.sql.SqlPartitioningRepository;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.thingsboard.server.dao.model.ModelConstants.AUDIT_LOG_TABLE_NAME;

@DefaultDataSource
@Component
@SqlDao
@RequiredArgsConstructor
@Slf4j
public class JpaAuditLogDao extends JpaPartitionedAbstractDao<AuditLogEntity, AuditLog> implements AuditLogDao {

    private final AuditLogRepository auditLogRepository;
    private final SqlPartitioningRepository partitioningRepository;

    @Value("${sql.audit_logs.partition_size:168}")
    private int partitionSizeInHours;

    @Override
    public PageData<AuditLog> findAuditLogsByTenantIdAndEntityId(UUID tenantId, EntityId entityId, List<ActionType> actionTypes, TimePageLink pageLink) {
        return DaoUtil.toPageData(
                auditLogRepository
                        .findAuditLogsByTenantIdAndEntityId(
                                tenantId,
                                entityId.getEntityType(),
                                entityId.getId(),
                                pageLink.getTextSearch(),
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
                                pageLink.getTextSearch(),
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
                                pageLink.getTextSearch(),
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
                        pageLink.getTextSearch(),
                        pageLink.getStartTime(),
                        pageLink.getEndTime(),
                        actionTypes,
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public void cleanUpAuditLogs(long expTime) {
        partitioningRepository.dropPartitionsBefore(AUDIT_LOG_TABLE_NAME, expTime, TimeUnit.HOURS.toMillis(partitionSizeInHours));
    }

    @Override
    public void createPartition(AuditLogEntity entity) {
        partitioningRepository.createPartitionIfNotExists(AUDIT_LOG_TABLE_NAME, entity.getCreatedTime(), TimeUnit.HOURS.toMillis(partitionSizeInHours));
    }

    @Override
    protected Class<AuditLogEntity> getEntityClass() {
        return AuditLogEntity.class;
    }

    @Override
    protected JpaRepository<AuditLogEntity, UUID> getRepository() {
        return auditLogRepository;
    }

}
