/**
 * Copyright © 2016-2026 The Thingsboard Authors
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.alarm.AlarmComment;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.audit.ActionStatus;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.audit.AuditLog;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.dao.audit.sink.AuditLogSink;
import org.thingsboard.server.dao.device.provision.ProvisionRequest;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.sql.JpaExecutorService;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.stream.Collectors;

import static org.thingsboard.server.dao.service.Validator.validateEntityId;
import static org.thingsboard.server.dao.service.Validator.validateId;

@Slf4j
@Service
@ConditionalOnProperty(prefix = "audit-log", value = "enabled", havingValue = "true")
public class AuditLogServiceImpl implements AuditLogService {

    private static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";

    @Autowired
    private AuditLogLevelFilter auditLogLevelFilter;

    @Autowired
    private AuditLogDao auditLogDao;

    @Autowired
    private EntityService entityService;

    @Autowired
    private AuditLogSink auditLogSink;

    @Autowired
    private JpaExecutorService executor;

    @Autowired
    private DataValidator<AuditLog> auditLogValidator;

    @Override
    public PageData<AuditLog> findAuditLogsByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, List<ActionType> actionTypes, TimePageLink pageLink) {
        log.trace("Executing findAuditLogsByTenantIdAndCustomerId [{}], [{}], [{}]", tenantId, customerId, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(customerId, id -> "Incorrect customerId " + id);
        return auditLogDao.findAuditLogsByTenantIdAndCustomerId(tenantId.getId(), customerId, actionTypes, pageLink);
    }

    @Override
    public PageData<AuditLog> findAuditLogsByTenantIdAndUserId(TenantId tenantId, UserId userId, List<ActionType> actionTypes, TimePageLink pageLink) {
        log.trace("Executing findAuditLogsByTenantIdAndUserId [{}], [{}], [{}]", tenantId, userId, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(userId, id -> "Incorrect userId" + id);
        return auditLogDao.findAuditLogsByTenantIdAndUserId(tenantId.getId(), userId, actionTypes, pageLink);
    }

    @Override
    public PageData<AuditLog> findAuditLogsByTenantIdAndEntityId(TenantId tenantId, EntityId entityId, List<ActionType> actionTypes, TimePageLink pageLink) {
        log.trace("Executing findAuditLogsByTenantIdAndEntityId [{}], [{}], [{}]", tenantId, entityId, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateEntityId(entityId, id -> "Incorrect entityId" + id);
        return auditLogDao.findAuditLogsByTenantIdAndEntityId(tenantId.getId(), entityId, actionTypes, pageLink);
    }

    @Override
    public PageData<AuditLog> findAuditLogsByTenantId(TenantId tenantId, List<ActionType> actionTypes, TimePageLink pageLink) {
        log.trace("Executing findAuditLogs [{}]", pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        return auditLogDao.findAuditLogsByTenantId(tenantId.getId(), actionTypes, pageLink);
    }

    @Override
    public <E extends HasName, I extends EntityId> ListenableFuture<Void>
    logEntityAction(TenantId tenantId, CustomerId customerId, UserId userId, String userName, I entityId, E entity,
                    ActionType actionType, Exception e, Object... additionalInfo) {
        if (canLog(entityId.getEntityType(), actionType) || (tenantId != null && tenantId.isSysTenantId())) {
            JsonNode actionData = constructActionData(entityId, entity, actionType, additionalInfo);
            ActionStatus actionStatus = ActionStatus.SUCCESS;
            String failureDetails = "";
            String entityName = getEntityName(tenantId, entityId, entity, actionType);
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

    private <E extends HasName, I extends EntityId> String getEntityName(TenantId tenantId, I entityId, E entity, ActionType actionType) {
        if (entity == null) {
            return fetchEntityName(tenantId, entityId);
        }
        if (!actionType.isAlarmAction()) {
            return entity.getName();
        }
        if (entity instanceof AlarmInfo alarmInfo) {
            return alarmInfo.getOriginatorName();
        }
        return fetchEntityName(tenantId, entityId);
    }

    private <I extends EntityId> String fetchEntityName(TenantId tenantId, I entityId) {
        try {
            return entityService.fetchEntityName(tenantId, entityId).orElse("N/A");
        } catch (Exception ignored) {
            return "N/A";
        }
    }

    private <E extends HasName, I extends EntityId> JsonNode constructActionData(I entityId, E entity,
                                                                                 ActionType actionType,
                                                                                 Object... additionalInfo) {
        ObjectNode actionData = JacksonUtil.newObjectNode();
        switch (actionType) {
            case ADDED:
            case UPDATED:
            case ALARM_ACK:
            case ALARM_CLEAR:
            case ALARM_ASSIGNED:
            case ALARM_UNASSIGNED:
            case RELATIONS_DELETED:
            case ASSIGNED_TO_TENANT:
                if (entity != null) {
                    ObjectNode entityNode = (ObjectNode) JacksonUtil.valueToTree(entity);
                    if (entityId.getEntityType() == EntityType.DASHBOARD) {
                        entityNode.put("configuration", "");
                    }
                    actionData.set("entity", entityNode);
                }
                if (entityId.getEntityType() == EntityType.RULE_CHAIN) {
                    RuleChainMetaData ruleChainMetaData = extractParameter(RuleChainMetaData.class, additionalInfo);
                    if (ruleChainMetaData != null) {
                        ObjectNode ruleChainMetaDataNode = (ObjectNode) JacksonUtil.valueToTree(ruleChainMetaData);
                        actionData.set("metadata", ruleChainMetaDataNode);
                    }
                }
                break;
            case ADDED_COMMENT:
            case UPDATED_COMMENT:
            case DELETED_COMMENT:
                AlarmComment comment = extractParameter(AlarmComment.class, additionalInfo);
                actionData.set("comment", comment.getComment());
                break;
            case ALARM_DELETE:
                EntityId alarmId = extractParameter(EntityId.class, additionalInfo);
                actionData.put("alarmId", alarmId != null ? alarmId.toString() : null);
                actionData.put("originatorId", entityId.toString());
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
                AttributeScope scope = extractParameter(AttributeScope.class, 0, additionalInfo);
                @SuppressWarnings("unchecked")
                List<AttributeKvEntry> attributes = extractParameter(List.class, 1, additionalInfo);
                actionData.put("scope", scope.name());
                ObjectNode attrsNode = JacksonUtil.newObjectNode();
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
                scope = extractParameter(AttributeScope.class, 0, additionalInfo);
                actionData.put("scope", scope.name());
                @SuppressWarnings("unchecked")
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
                actionData.set("credentials", JacksonUtil.valueToTree(deviceCredentials));
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
                actionData.set("relation", JacksonUtil.valueToTree(relation));
                break;
            case LOGIN:
            case LOGOUT:
            case LOCKOUT:
                String clientAddress = extractParameter(String.class, 0, additionalInfo);
                String browser = extractParameter(String.class, 1, additionalInfo);
                String os = extractParameter(String.class, 2, additionalInfo);
                String device = extractParameter(String.class, 3, additionalInfo);
                String provider = extractParameter(String.class, 4, additionalInfo);
                actionData.put("clientAddress", clientAddress);
                actionData.put("browser", browser);
                actionData.put("os", os);
                actionData.put("device", device);
                if (StringUtils.hasText(provider)) {
                    actionData.put("provider", provider);
                }
                break;
            case PROVISION_SUCCESS:
            case PROVISION_FAILURE:
                ProvisionRequest request = extractParameter(ProvisionRequest.class, additionalInfo);
                if (request != null) {
                    actionData.set("provisionRequest", JacksonUtil.valueToTree(request));
                }
                break;
            case TIMESERIES_UPDATED:
                actionData.put("entityId", entityId.toString());
                @SuppressWarnings("unchecked")
                List<TsKvEntry> updatedTimeseries = extractParameter(List.class, 0, additionalInfo);
                if (updatedTimeseries != null) {
                    ArrayNode result = actionData.putArray("timeseries");
                    updatedTimeseries.stream()
                            .collect(Collectors.groupingBy(TsKvEntry::getTs))
                            .forEach((k, v) -> {
                                ObjectNode element = JacksonUtil.newObjectNode();
                                element.put("ts", k);
                                ObjectNode values = element.putObject("values");
                                v.forEach(kvEntry -> values.put(kvEntry.getKey(), kvEntry.getValueAsString()));
                                result.add(element);
                            });
                }
                break;
            case TIMESERIES_DELETED:
                actionData.put("entityId", entityId.toString());
                @SuppressWarnings("unchecked")
                List<String> timeseriesKeys = extractParameter(List.class, 0, additionalInfo);
                if (timeseriesKeys != null) {
                    ArrayNode timeseriesArrayNode = actionData.putArray("timeseries");
                    timeseriesKeys.forEach(timeseriesArrayNode::add);
                }
                actionData.put("startTs", extractParameter(Long.class, 1, additionalInfo));
                actionData.put("endTs", extractParameter(Long.class, 2, additionalInfo));
                break;
            case ASSIGNED_TO_EDGE:
                strEntityId = extractParameter(String.class, 0, additionalInfo);
                String strEdgeId = extractParameter(String.class, 1, additionalInfo);
                String strEdgeName = extractParameter(String.class, 2, additionalInfo);
                actionData.put("entityId", strEntityId);
                actionData.put("assignedEdgeId", strEdgeId);
                actionData.put("assignedEdgeName", strEdgeName);
                break;
            case UNASSIGNED_FROM_EDGE:
                strEntityId = extractParameter(String.class, 0, additionalInfo);
                strEdgeId = extractParameter(String.class, 1, additionalInfo);
                strEdgeName = extractParameter(String.class, 2, additionalInfo);
                actionData.put("entityId", strEntityId);
                actionData.put("unassignedEdgeId", strEdgeId);
                actionData.put("unassignedEdgeName", strEdgeName);
                break;
            case SMS_SENT:
                String number = extractParameter(String.class, 0, additionalInfo);
                actionData.put("recipientNumber", number);
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

    private ListenableFuture<Void> logAction(TenantId tenantId,
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
        try {
            auditLogValidator.validate(auditLogEntry, AuditLog::getTenantId);
        } catch (Exception e) {
            if (StringUtils.contains(e.getMessage(), "is malformed")) {
                auditLogEntry.setEntityName("MALFORMED");
            } else {
                return Futures.immediateFailedFuture(e);
            }
        }

        return executor.submit(() -> {
            try {
                AuditLog auditLog = auditLogDao.save(tenantId, auditLogEntry);
                auditLogSink.logAction(auditLog);
            } catch (Throwable e) {
                log.error("[{}] Failed to save audit log: {}", tenantId, auditLogEntry, e);
            }
            return null;
        });
    }

}
