/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.CrudRepository;
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
import org.thingsboard.server.dao.model.sql.AuditLogEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.thingsboard.server.dao.DaoUtil.endTimeToId;
import static org.thingsboard.server.dao.DaoUtil.startTimeToId;

@Component
@SqlDao
public class JpaAuditLogDao extends JpaAbstractDao<AuditLogEntity, AuditLog> implements AuditLogDao {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Override
    protected Class<AuditLogEntity> getEntityClass() {
        return AuditLogEntity.class;
    }

    @Override
    protected CrudRepository<AuditLogEntity, UUID> getCrudRepository() {
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
    public PageData<AuditLog> findAuditLogsByTenantIdAndEntityId(UUID tenantId, EntityId entityId, List<ActionType> actionTypes, TimePageLink pageLink) {
        return DaoUtil.toPageData(
                auditLogRepository
                        .findAuditLogsByTenantIdAndEntityId(
                                tenantId,
                                entityId.getEntityType(),
                                entityId.getId(),
                                Objects.toString(pageLink.getTextSearch(), ""),
                                startTimeToId(pageLink.getStartTime()),
                                endTimeToId(pageLink.getEndTime()),
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
                                startTimeToId(pageLink.getStartTime()),
                                endTimeToId(pageLink.getEndTime()),
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
                                startTimeToId(pageLink.getStartTime()),
                                endTimeToId(pageLink.getEndTime()),
                                actionTypes,
                                DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<AuditLog> findAuditLogsByTenantId(UUID tenantId, List<ActionType> actionTypes, TimePageLink pageLink) {
        return DaoUtil.toPageData(
                auditLogRepository.findByTenantId(
                        tenantId,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        startTimeToId(pageLink.getStartTime()),
                        endTimeToId(pageLink.getEndTime()),
                        actionTypes,
                        DaoUtil.toPageable(pageLink)));
    }
}
