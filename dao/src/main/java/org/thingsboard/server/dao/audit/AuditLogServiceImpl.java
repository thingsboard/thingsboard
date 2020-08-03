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
package org.thingsboard.server.dao.audit;

import com.datastax.driver.core.utils.UUIDs;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.audit.ActionStatus;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.audit.AuditLog;
import org.thingsboard.server.common.data.id.AuditLogId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.page.TimePageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.dao.audit.sink.AuditLogSink;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import static org.thingsboard.server.dao.service.Validator.validateEntityId;
import static org.thingsboard.server.dao.service.Validator.validateId;

@Slf4j
@Service
@ConditionalOnProperty(prefix = "audit-log", value = "enabled", havingValue = "true")
public class AuditLogServiceImpl implements AuditLogService {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    private static final int INSERTS_PER_ENTRY = 3;

    @Autowired
    private AuditLogLevelFilter auditLogLevelFilter;

    @Autowired
    private AuditLogDao auditLogDao;

    @Autowired
    private EntityService entityService;

    @Autowired
    private AuditLogSink auditLogSink;

    @Override
    public TimePageData<AuditLog> findAuditLogsByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, List<ActionType> actionTypes, TimePageLink pageLink) {
        log.trace("Executing findAuditLogsByTenantIdAndCustomerId [{}], [{}], [{}]", tenantId, customerId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, "Incorrect customerId " + customerId);
        List<AuditLog> auditLogs = auditLogDao.findAuditLogsByTenantIdAndCustomerId(tenantId.getId(), customerId, actionTypes, pageLink);
        return new TimePageData<>(auditLogs, pageLink);
    }

    @Override
    public TimePageData<AuditLog> findAuditLogsByTenantIdAndUserId(TenantId tenantId, UserId userId, List<ActionType> actionTypes, TimePageLink pageLink) {
        log.trace("Executing findAuditLogsByTenantIdAndUserId [{}], [{}], [{}]", tenantId, userId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(userId, "Incorrect userId" + userId);
        List<AuditLog> auditLogs = auditLogDao.findAuditLogsByTenantIdAndUserId(tenantId.getId(), userId, actionTypes, pageLink);
        return new TimePageData<>(auditLogs, pageLink);
    }

    @Override
    public TimePageData<AuditLog> findAuditLogsByTenantIdAndEntityId(TenantId tenantId, EntityId entityId, List<ActionType> actionTypes, TimePageLink pageLink) {
        log.trace("Executing findAuditLogsByTenantIdAndEntityId [{}], [{}], [{}]", tenantId, entityId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateEntityId(entityId, INCORRECT_TENANT_ID + entityId);
        List<AuditLog> auditLogs = auditLogDao.findAuditLogsByTenantIdAndEntityId(tenantId.getId(), entityId, actionTypes, pageLink);
        return new TimePageData<>(auditLogs, pageLink);
    }

    @Override
    public TimePageData<AuditLog> findAuditLogsByTenantId(TenantId tenantId, List<ActionType> actionTypes, TimePageLink pageLink) {
        log.trace("Executing findAuditLogs [{}]", pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        List<AuditLog> auditLogs = auditLogDao.findAuditLogsByTenantId(tenantId.getId(), actionTypes, pageLink);
        return new TimePageData<>(auditLogs, pageLink);
    }

    @Override
    public <E extends HasName, I extends EntityId> ListenableFuture<List<Void>>
    logEntityAction(TenantId tenantId, CustomerId customerId, UserId userId, String userName, I entityId, E entity,
                    ActionType actionType, Exception e, Object... additionalInfo) {
        if (canLog(entityId.getEntityType(), actionType)) {
            JsonNode actionData = constructActionData(entityId, entity, actionType, additionalInfo);
            ActionStatus actionStatus = ActionStatus.SUCCESS;
            String failureDetails = "";
            String entityName = "";
            if (entity != null) {
                entityName = entity.getName();
            } else {
                try {
                    entityName = entityService.fetchEntityNameAsync(tenantId, entityId).get();
                } catch (Exception ex) {
                }
            }
            if (e != null) {
                actionStatus = ActionStatus.FAILURE;
                failureDetails = getFailureStack(e);
            }
            if (actionType == ActionType.RPC_CALL) {
                String rpcErrorString = extractParameter(String.class, additionalInfo);
                if (!StringUtils.isEmpty(rpcErrorString)) {
                    actionStatus = ActionStatus.FAILURE;
                    failureDetails = rpcErrorString;
                }
            }
            return logAction(tenantId,
                    entityId,
                    entityName,
                    customerId,
                    userId,
                    userName,
                    actionType,
                    actionData,
                    actionStatus,
                    failureDetails);
        } else {
            return null;
        }
    }

    private <E extends HasName, I extends EntityId> JsonNode constructActionData(I entityId, E entity,
                                                                                 ActionType actionType,
                                                                                 Object... additionalInfo) {
        ObjectNode actionData = objectMapper.createObjectNode();
        switch (actionType) {
            case ADDED:
            case UPDATED:
            case ALARM_ACK:
            case ALARM_CLEAR:
            case RELATIONS_DELETED:
            case ASSIGNED_TO_TENANT:
                if (entity != null) {
                    ObjectNode entityNode = objectMapper.valueToTree(entity);
                    if (entityId.getEntityType() == EntityType.DASHBOARD) {
                        entityNode.put("configuration", "");
                    }
                    actionData.set("entity", entityNode);
                }
                if (entityId.getEntityType() == EntityType.RULE_CHAIN) {
                    RuleChainMetaData ruleChainMetaData = extractParameter(RuleChainMetaData.class, additionalInfo);
                    if (ruleChainMetaData != null) {
                        ObjectNode ruleChainMetaDataNode = objectMapper.valueToTree(ruleChainMetaData);
                        actionData.set("metadata", ruleChainMetaDataNode);
                    }
                }
                break;
            case DELETED:
            case ACTIVATED:
            case SUSPENDED:
            case CREDENTIALS_READ:
                String strEntityId = extractParameter(String.class, additionalInfo);
                actionData.put("entityId", strEntityId);
                break;
            case ATTRIBUTES_UPDATED:
                actionData.put("entityId", entityId.toString());
                String scope = extractParameter(String.class, 0, additionalInfo);
                List<AttributeKvEntry> attributes = extractParameter(List.class, 1, additionalInfo);
                actionData.put("scope", scope);
                ObjectNode attrsNode = objectMapper.createObjectNode();
                if (attributes != null) {
                    for (AttributeKvEntry attr : attributes) {
                        attrsNode.put(attr.getKey(), attr.getValueAsString());
                    }
                }
                actionData.set("attributes", attrsNode);
                break;
            case ATTRIBUTES_DELETED:
            case ATTRIBUTES_READ:
                actionData.put("entityId", entityId.toString());
                scope = extractParameter(String.class, 0, additionalInfo);
                actionData.put("scope", scope);
                List<String> keys = extractParameter(List.class, 1, additionalInfo);
                ArrayNode attrsArrayNode = actionData.putArray("attributes");
                if (keys != null) {
                    keys.forEach(attrsArrayNode::add);
                }
                break;
            case RPC_CALL:
                actionData.put("entityId", entityId.toString());
                Boolean oneWay = extractParameter(Boolean.class, 1, additionalInfo);
                String method = extractParameter(String.class, 2, additionalInfo);
                String params = extractParameter(String.class, 3, additionalInfo);
                actionData.put("oneWay", oneWay);
                actionData.put("method", method);
                actionData.put("params", params);
                break;
            case CREDENTIALS_UPDATED:
                actionData.put("entityId", entityId.toString());
                DeviceCredentials deviceCredentials = extractParameter(DeviceCredentials.class, additionalInfo);
                actionData.set("credentials", objectMapper.valueToTree(deviceCredentials));
                break;
            case ASSIGNED_TO_CUSTOMER:
                strEntityId = extractParameter(String.class, 0, additionalInfo);
                String strCustomerId = extractParameter(String.class, 1, additionalInfo);
                String strCustomerName = extractParameter(String.class, 2, additionalInfo);
                actionData.put("entityId", strEntityId);
                actionData.put("assignedCustomerId", strCustomerId);
                actionData.put("assignedCustomerName", strCustomerName);
                break;
            case UNASSIGNED_FROM_CUSTOMER:
                strEntityId = extractParameter(String.class, 0, additionalInfo);
                strCustomerId = extractParameter(String.class, 1, additionalInfo);
                strCustomerName = extractParameter(String.class, 2, additionalInfo);
                actionData.put("entityId", strEntityId);
                actionData.put("unassignedCustomerId", strCustomerId);
                actionData.put("unassignedCustomerName", strCustomerName);
                break;
            case RELATION_ADD_OR_UPDATE:
            case RELATION_DELETED:
                EntityRelation relation = extractParameter(EntityRelation.class, 0, additionalInfo);
                actionData.set("relation", objectMapper.valueToTree(relation));
                break;
            case LOGIN:
            case LOGOUT:
            case LOCKOUT:
                String clientAddress = extractParameter(String.class, 0, additionalInfo);
                String browser = extractParameter(String.class, 1, additionalInfo);
                String os = extractParameter(String.class, 2, additionalInfo);
                String device = extractParameter(String.class, 3, additionalInfo);
                actionData.put("clientAddress", clientAddress);
                actionData.put("browser", browser);
                actionData.put("os", os);
                actionData.put("device", device);
                break;
        }
        return actionData;
    }

    private <T> T extractParameter(Class<T> clazz, Object... additionalInfo) {
        return extractParameter(clazz, 0, additionalInfo);
    }

    private <T> T extractParameter(Class<T> clazz, int index, Object... additionalInfo) {
        T result = null;
        if (additionalInfo != null && additionalInfo.length > index) {
            Object paramObject = additionalInfo[index];
            if (clazz.isInstance(paramObject)) {
                result = clazz.cast(paramObject);
            }
        }
        return result;
    }

    private String getFailureStack(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    private boolean canLog(EntityType entityType, ActionType actionType) {
        return auditLogLevelFilter.logEnabled(entityType, actionType);
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
        auditLogValidator.validate(auditLogEntry, AuditLog::getTenantId);
        List<ListenableFuture<Void>> futures = Lists.newArrayListWithExpectedSize(INSERTS_PER_ENTRY);
        futures.add(auditLogDao.savePartitionsByTenantId(auditLogEntry));
        futures.add(auditLogDao.saveByTenantId(auditLogEntry));
        futures.add(auditLogDao.saveByTenantIdAndEntityId(auditLogEntry));
        futures.add(auditLogDao.saveByTenantIdAndCustomerId(auditLogEntry));
        futures.add(auditLogDao.saveByTenantIdAndUserId(auditLogEntry));

        auditLogSink.logAction(auditLogEntry);

        return Futures.allAsList(futures);
    }

    private DataValidator<AuditLog> auditLogValidator =
            new DataValidator<AuditLog>() {
                @Override
                protected void validateDataImpl(TenantId tenantId, AuditLog auditLog) {
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
