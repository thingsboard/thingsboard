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
package org.thingsboard.server.dao.audit;

import com.datastax.driver.core.utils.UUIDs;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionStatus;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.audit.AuditLog;
import org.thingsboard.server.common.data.id.*;
import org.thingsboard.server.common.data.page.TimePageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;

import java.util.List;

import static org.thingsboard.server.dao.service.Validator.validateEntityId;
import static org.thingsboard.server.dao.service.Validator.validateId;

@Slf4j
@Service
@ConditionalOnProperty(prefix = "audit_log", value = "enabled", havingValue = "true")
public class AuditLogServiceImpl implements AuditLogService {

    private static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    private static final int INSERTS_PER_ENTRY = 3;

    @Autowired
    private AuditLogDao auditLogDao;

    @Override
    public TimePageData<AuditLog> findAuditLogsByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, TimePageLink pageLink) {
        log.trace("Executing findAuditLogsByTenantIdAndCustomerId [{}], [{}], [{}]", tenantId, customerId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, "Incorrect customerId " + customerId);
        List<AuditLog> auditLogs = auditLogDao.findAuditLogsByTenantIdAndCustomerId(tenantId.getId(), customerId, pageLink);
        return new TimePageData<>(auditLogs, pageLink);
    }

    @Override
    public TimePageData<AuditLog> findAuditLogsByTenantIdAndUserId(TenantId tenantId, UserId userId, TimePageLink pageLink) {
        log.trace("Executing findAuditLogsByTenantIdAndUserId [{}], [{}], [{}]", tenantId, userId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(userId, "Incorrect userId" + userId);
        List<AuditLog> auditLogs = auditLogDao.findAuditLogsByTenantIdAndUserId(tenantId.getId(), userId, pageLink);
        return new TimePageData<>(auditLogs, pageLink);
    }

    @Override
    public TimePageData<AuditLog> findAuditLogsByTenantIdAndEntityId(TenantId tenantId, EntityId entityId, TimePageLink pageLink) {
        log.trace("Executing findAuditLogsByTenantIdAndEntityId [{}], [{}], [{}]", tenantId, entityId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateEntityId(entityId, INCORRECT_TENANT_ID + entityId);
        List<AuditLog> auditLogs = auditLogDao.findAuditLogsByTenantIdAndEntityId(tenantId.getId(), entityId, pageLink);
        return new TimePageData<>(auditLogs, pageLink);
    }

    @Override
    public TimePageData<AuditLog> findAuditLogsByTenantId(TenantId tenantId, TimePageLink pageLink) {
        log.trace("Executing findAuditLogs [{}]", pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        List<AuditLog> auditLogs = auditLogDao.findAuditLogsByTenantId(tenantId.getId(), pageLink);
        return new TimePageData<>(auditLogs, pageLink);
    }

    @Override
    public ListenableFuture<List<Void>> logEntityAction(User user,
                                                        EntityId entityId,
                                                        String entityName,
                                                        CustomerId customerId,
                                                        ActionType actionType,
                                                        JsonNode actionData,
                                                        ActionStatus actionStatus,
                                                        String actionFailureDetails) {
        return logAction(
                user.getTenantId(),
                entityId,
                entityName,
                customerId,
                user.getId(),
                user.getName(),
                actionType,
                actionData,
                actionStatus,
                actionFailureDetails);
    }

    private AuditLog createAuditLogEntry(TenantId tenantId,
                                         EntityId entityId,
                                         String entityName,
                                         CustomerId customerId,
                                         UserId userId,
                                         String userName,
                                         ActionType actionType,
                                         JsonNode actionData,
                                         ActionStatus actionStatus,
                                         String actionFailureDetails) {
        AuditLog result = new AuditLog();
        result.setId(new AuditLogId(UUIDs.timeBased()));
        result.setTenantId(tenantId);
        result.setEntityId(entityId);
        result.setEntityName(entityName);
        result.setCustomerId(customerId);
        result.setUserId(userId);
        result.setUserName(userName);
        result.setActionType(actionType);
        result.setActionData(actionData);
        result.setActionStatus(actionStatus);
        result.setActionFailureDetails(actionFailureDetails);
        return result;
    }

    private ListenableFuture<List<Void>> logAction(TenantId tenantId,
                                                   EntityId entityId,
                                                   String entityName,
                                                   CustomerId customerId,
                                                   UserId userId,
                                                   String userName,
                                                   ActionType actionType,
                                                   JsonNode actionData,
                                                   ActionStatus actionStatus,
                                                   String actionFailureDetails) {
        AuditLog auditLogEntry = createAuditLogEntry(tenantId, entityId, entityName, customerId, userId, userName,
                actionType, actionData, actionStatus, actionFailureDetails);
        log.trace("Executing logAction [{}]", auditLogEntry);
        auditLogValidator.validate(auditLogEntry);
        List<ListenableFuture<Void>> futures = Lists.newArrayListWithExpectedSize(INSERTS_PER_ENTRY);
        futures.add(auditLogDao.savePartitionsByTenantId(auditLogEntry));
        futures.add(auditLogDao.saveByTenantId(auditLogEntry));
        futures.add(auditLogDao.saveByTenantIdAndEntityId(auditLogEntry));
        futures.add(auditLogDao.saveByTenantIdAndCustomerId(auditLogEntry));
        futures.add(auditLogDao.saveByTenantIdAndUserId(auditLogEntry));
        return Futures.allAsList(futures);
    }

    private DataValidator<AuditLog> auditLogValidator =
            new DataValidator<AuditLog>() {
                @Override
                protected void validateDataImpl(AuditLog auditLog) {
                    if (auditLog.getEntityId() == null) {
                        throw new DataValidationException("Entity Id should be specified!");
                    }
                    if (auditLog.getTenantId() == null) {
                        throw new DataValidationException("Tenant Id should be specified!");
                    }
                    if (auditLog.getUserId() == null) {
                        throw new DataValidationException("User Id should be specified!");
                    }
                }
            };
}
