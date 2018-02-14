/**
 * Copyright © 2016-2017 The Thingsboard Authors
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

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.audit.AuditLog;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.audit.AuditLogDao;
import org.thingsboard.server.dao.model.sql.AuditLogEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import javax.annotation.PreDestroy;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;

import static org.thingsboard.server.common.data.UUIDConverter.fromTimeUUID;
import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID_STR;

@Component
@SqlDao
public class JpaAuditLogDao extends JpaAbstractDao<AuditLogEntity, AuditLog> implements AuditLogDao {

    private ListeningExecutorService insertService = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Override
    protected Class<AuditLogEntity> getEntityClass() {
        return AuditLogEntity.class;
    }

    @Override
    protected CrudRepository<AuditLogEntity, String> getCrudRepository() {
        return auditLogRepository;
    }

    @PreDestroy
    void onDestroy() {
        insertService.shutdown();
    }

    @Override
    public ListenableFuture<Void> saveByTenantId(AuditLog auditLog) {
        return insertService.submit(() -> {
            save(auditLog);
            return null;
        });
    }

    @Override
    public ListenableFuture<Void> saveByTenantIdAndEntityId(AuditLog auditLog) {
        return insertService.submit(() -> null);
    }

    @Override
    public ListenableFuture<Void> saveByTenantIdAndCustomerId(AuditLog auditLog) {
        return insertService.submit(() -> null);
    }

    @Override
    public ListenableFuture<Void> saveByTenantIdAndUserId(AuditLog auditLog) {
        return insertService.submit(() -> null);
    }

    @Override
    public ListenableFuture<Void> savePartitionsByTenantId(AuditLog auditLog) {
        return insertService.submit(() -> null);
    }

    @Override
    public List<AuditLog> findAuditLogsByTenantIdAndEntityId(UUID tenantId, EntityId entityId, TimePageLink pageLink) {
        return DaoUtil.convertDataList(
                auditLogRepository.findByTenantIdAndEntityId(
                        fromTimeUUID(tenantId),
                        fromTimeUUID(entityId.getId()),
                        entityId.getEntityType(),
                        pageLink.getIdOffset() == null ? NULL_UUID_STR : fromTimeUUID(pageLink.getIdOffset()),
                        new PageRequest(0, pageLink.getLimit())));
    }

    @Override
    public List<AuditLog> findAuditLogsByTenantIdAndCustomerId(UUID tenantId, CustomerId customerId, TimePageLink pageLink) {
        return DaoUtil.convertDataList(
                auditLogRepository.findByTenantIdAndCustomerId(
                        fromTimeUUID(tenantId),
                        fromTimeUUID(customerId.getId()),
                        pageLink.getIdOffset() == null ? NULL_UUID_STR : fromTimeUUID(pageLink.getIdOffset()),
                        new PageRequest(0, pageLink.getLimit())));
    }

    @Override
    public List<AuditLog> findAuditLogsByTenantIdAndUserId(UUID tenantId, UserId userId, TimePageLink pageLink) {
        return DaoUtil.convertDataList(
                auditLogRepository.findByTenantIdAndUserId(
                        fromTimeUUID(tenantId),
                        fromTimeUUID(userId.getId()),
                        pageLink.getIdOffset() == null ? NULL_UUID_STR : fromTimeUUID(pageLink.getIdOffset()),
                        new PageRequest(0, pageLink.getLimit())));
    }

    @Override
    public List<AuditLog> findAuditLogsByTenantId(UUID tenantId, TimePageLink pageLink) {
        return DaoUtil.convertDataList(
                auditLogRepository.findByTenantId(
                        fromTimeUUID(tenantId),
                        pageLink.getIdOffset() == null ? NULL_UUID_STR : fromTimeUUID(pageLink.getIdOffset()),
                        new PageRequest(0, pageLink.getLimit())));
    }
}
