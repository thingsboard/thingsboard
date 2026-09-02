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
package org.thingsboard.server.service.action;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmComment;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.notification.rule.trigger.AlarmAssignmentTrigger;
import org.thingsboard.server.common.data.notification.rule.trigger.AlarmCommentTrigger;
import org.thingsboard.server.common.data.notification.rule.trigger.EntitiesLimitTrigger;
import org.thingsboard.server.common.data.notification.rule.trigger.EntityActionTrigger;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.notification.NotificationRuleProcessor;
import org.thingsboard.server.dao.audit.AuditLogService;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EntityActionService {
    private final TbClusterService tbClusterService;
    private final AuditLogService auditLogService;
    private final NotificationRuleProcessor notificationRuleProcessor;

    public void pushEntityActionToRuleEngine(EntityId entityId, HasName entity, TenantId tenantId, CustomerId customerId,
                                             ActionType actionType, User user, Object... additionalInfo) {
        Optional<TbMsgType> msgType = actionType.getRuleEngineMsgType();
        if (msgType.isPresent()) {
            try {
                TbMsgMetaData metaData = new TbMsgMetaData();
                if (user != null) {
                    metaData.putValue("userId", user.getId().toString());
                    metaData.putValue("userName", user.getName());
                    metaData.putValue("userEmail", user.getEmail());
                    if (user.getFirstName() != null) {
                        metaData.putValue("userFirstName", user.getFirstName());
                    }
                    if (user.getLastName() != null) {
                        metaData.putValue("userLastName", user.getLastName());
                    }
                }
                if (customerId != null && !customerId.isNullUid()) {
                    metaData.putValue("customerId", customerId.toString());
                }
                if (actionType == ActionType.ASSIGNED_TO_CUSTOMER) {
                    String strCustomerId = extractParameter(String.class, 1, additionalInfo);
                    String strCustomerName = extractParameter(String.class, 2, additionalInfo);
                    metaData.putValue("assignedCustomerId", strCustomerId);
                    metaData.putValue("assignedCustomerName", strCustomerName);
                } else if (actionType == ActionType.UNASSIGNED_FROM_CUSTOMER) {
                    String strCustomerId = extractParameter(String.class, 1, additionalInfo);
                    String strCustomerName = extractParameter(String.class, 2, additionalInfo);
                    metaData.putValue("unassignedCustomerId", strCustomerId);
                    metaData.putValue("unassignedCustomerName", strCustomerName);
                } else if (actionType == ActionType.ASSIGNED_FROM_TENANT) {
                    String strTenantId = extractParameter(String.class, 0, additionalInfo);
                    String strTenantName = extractParameter(String.class, 1, additionalInfo);
                    metaData.putValue("assignedFromTenantId", strTenantId);
                    metaData.putValue("assignedFromTenantName", strTenantName);
                } else if (actionType == ActionType.ASSIGNED_TO_TENANT) {
                    String strTenantId = extractParameter(String.class, 0, additionalInfo);
                    String strTenantName = extractParameter(String.class, 1, additionalInfo);
                    metaData.putValue("assignedToTenantId", strTenantId);
                    metaData.putValue("assignedToTenantName", strTenantName);
                } else if (actionType == ActionType.ASSIGNED_TO_EDGE) {
                    String strEdgeId = extractParameter(String.class, 1, additionalInfo);
                    String strEdgeName = extractParameter(String.class, 2, additionalInfo);
                    metaData.putValue("assignedEdgeId", strEdgeId);
                    metaData.putValue("assignedEdgeName", strEdgeName);
                } else if (actionType == ActionType.UNASSIGNED_FROM_EDGE) {
                    String strEdgeId = extractParameter(String.class, 1, additionalInfo);
                    String strEdgeName = extractParameter(String.class, 2, additionalInfo);
                    metaData.putValue("unassignedEdgeId", strEdgeId);
                    metaData.putValue("unassignedEdgeName", strEdgeName);
                } else if (actionType == ActionType.ADDED_COMMENT || actionType == ActionType.UPDATED_COMMENT) {
                    AlarmComment comment = extractParameter(AlarmComment.class, 0, additionalInfo);
                    metaData.putValue("comment", JacksonUtil.toString(comment));
                }
                ObjectNode entityNode;
                if (entity != null) {
                    entityNode = JacksonUtil.OBJECT_MAPPER.valueToTree(entity);
                    if (entityId.getEntityType() == EntityType.DASHBOARD) {
                        entityNode.put("configuration", "");
                    }
                    metaData.putValue("entityName", entity.getName());
                    metaData.putValue("entityType", entityId.getEntityType().toString());
                } else {
                    entityNode = JacksonUtil.newObjectNode();
                    if (actionType == ActionType.ATTRIBUTES_UPDATED) {
                        AttributeScope scope = extractParameter(AttributeScope.class, 0, additionalInfo);
                        @SuppressWarnings("unchecked")
                        List<AttributeKvEntry> attributes = extractParameter(List.class, 1, additionalInfo);
                        metaData.putValue(DataConstants.SCOPE, scope.name());
                        if (attributes != null) {
                            for (AttributeKvEntry attr : attributes) {
                                JacksonUtil.addKvEntry(entityNode, attr);
                            }
                        }
                    } else if (actionType == ActionType.ATTRIBUTES_DELETED) {
                        AttributeScope scope = extractParameter(AttributeScope.class, 0, additionalInfo);
                        @SuppressWarnings("unchecked")
                        List<String> keys = extractParameter(List.class, 1, additionalInfo);
                        metaData.putValue(DataConstants.SCOPE, scope.name());
                        ArrayNode attrsArrayNode = entityNode.putArray("attributes");
                        if (keys != null) {
                            keys.forEach(attrsArrayNode::add);
                        }
                    } else if (actionType == ActionType.TIMESERIES_UPDATED) {
                        @SuppressWarnings("unchecked")
                        List<TsKvEntry> timeseries = extractParameter(List.class, 0, additionalInfo);
                        addTimeseries(entityNode, timeseries);
                    } else if (actionType == ActionType.TIMESERIES_DELETED) {
                        @SuppressWarnings("unchecked")
                        List<String> keys = extractParameter(List.class, 0, additionalInfo);
                        if (keys != null) {
                            ArrayNode timeseriesArrayNode = entityNode.putArray("timeseries");
                            keys.forEach(timeseriesArrayNode::add);
                        }
                        entityNode.put("startTs", extractParameter(Long.class, 1, additionalInfo));
                        entityNode.put("endTs", extractParameter(Long.class, 2, additionalInfo));
                    } else if (ActionType.RELATION_ADD_OR_UPDATE.equals(actionType) || ActionType.RELATION_DELETED.equals(actionType)) {
                        entityNode = JacksonUtil.OBJECT_MAPPER.valueToTree(extractParameter(EntityRelation.class, 0, additionalInfo));
                    }
                }

                if (tenantId == null || tenantId.isNullUid()) {
                    if (entity instanceof HasTenantId) {
                        tenantId = ((HasTenantId) entity).getTenantId();
                    }
                }
                if (tenantId != null && !tenantId.isSysTenantId()) {
                    processNotificationRules(tenantId, entityId, entity, actionType, user, additionalInfo);
                }
                TbMsg tbMsg = TbMsg.newMsg()
                        .type(msgType.get())
                        .originator(entityId)
                        .customerId(customerId)
                        .copyMetaData(metaData)
                        .dataType(TbMsgDataType.JSON)
                        .data(JacksonUtil.toString(entityNode))
                        .build();
                tbClusterService.pushMsgToRuleEngine(tenantId, entityId, tbMsg, null);
            } catch (Exception e) {
                log.warn("[{}] Failed to push entity action to rule engine: {}", entityId, actionType, e);
            }
        }
    }

    private void processNotificationRules(TenantId tenantId, EntityId originatorId, HasName entity, ActionType actionType, User user, Object... additionalInfo) {
        EntityId entityId = entity instanceof HasId ? ((HasId<? extends EntityId>) entity).getId() : originatorId;
        switch (actionType) {
            case ADDED:
                notificationRuleProcessor.process(EntitiesLimitTrigger.builder()
                        .tenantId(tenantId)
                        .entityType(entityId.getEntityType())
                        .build());
            case UPDATED:
            case DELETED:
                notificationRuleProcessor.process(EntityActionTrigger.builder()
                        .tenantId(tenantId)
                        .entityId(entityId)
                        .entity(entity)
                        .actionType(actionType)
                        .user(user)
                        .build());
                break;
            case ALARM_ASSIGNED:
            case ALARM_UNASSIGNED:
                if (!(entity instanceof AlarmInfo)) { // should not normally happen
                    log.warn("Invalid alarm assignment event: entity is not instance of AlarmInfo");
                    break;
                }
                notificationRuleProcessor.process(AlarmAssignmentTrigger.builder()
                        .tenantId(tenantId)
                        .alarmInfo((AlarmInfo) entity)
                        .actionType(actionType)
                        .user(user)
                        .build());
                break;
            case ADDED_COMMENT:
            case UPDATED_COMMENT:
                if (!(entity instanceof Alarm)) { // should not normally happen
                    log.warn("Invalid alarm comment event: entity is not instance of Alarm");
                    break;
                }
                notificationRuleProcessor.process(AlarmCommentTrigger.builder()
                        .tenantId(tenantId)
                        .comment(extractParameter(AlarmComment.class, 0, additionalInfo))
                        .alarm((Alarm) entity)
                        .actionType(actionType)
                        .user(user)
                        .build());
                break;
        }
    }

    public <E extends HasName, I extends EntityId> void logEntityAction(User user, @NotNull I entityId, E entity, CustomerId customerId,
                                                                        ActionType actionType, Exception e, Object... additionalInfo) {
        if (customerId == null || customerId.isNullUid()) {
            customerId = user.getCustomerId();
        }
        if (e == null) {
            pushEntityActionToRuleEngine(entityId, entity, user.getTenantId(), customerId, actionType, user, additionalInfo);
        }
        auditLogService.logEntityAction(user.getTenantId(), customerId, user.getId(), user.getName(), entityId, entity, actionType, e, additionalInfo);
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

    private void addTimeseries(ObjectNode entityNode, List<TsKvEntry> timeseries) {
        if (timeseries != null && !timeseries.isEmpty()) {
            ArrayNode result = entityNode.putArray("timeseries");
            Map<Long, List<TsKvEntry>> groupedTelemetry = timeseries.stream()
                    .collect(Collectors.groupingBy(TsKvEntry::getTs));
            for (Map.Entry<Long, List<TsKvEntry>> entry : groupedTelemetry.entrySet()) {
                ObjectNode element = JacksonUtil.newObjectNode();
                element.put("ts", entry.getKey());
                ObjectNode values = element.putObject("values");
                for (TsKvEntry tsKvEntry : entry.getValue()) {
                    JacksonUtil.addKvEntry(values, tsKvEntry);
                }
                result.add(element);
            }
        }
    }

}
