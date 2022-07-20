/**
 * Copyright © 2016-2022 The Thingsboard Authors
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.*;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.DataType;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.audit.AuditLogService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.cluster.TbClusterService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@TbCoreComponent
@Service
@RequiredArgsConstructor
@Slf4j
public class EntityActionService {
    private final TbClusterService tbClusterService;
    private final AuditLogService auditLogService;

    private static final ObjectMapper json = new ObjectMapper();

    public void pushEntityActionToRuleEngine(EntityId entityId, HasName entity, TenantId tenantId, CustomerId customerId,
                                             ActionType actionType, User user, Object... additionalInfo) {
        String msgType = null;
        switch (actionType) {
            case ADDED:
                msgType = DataConstants.ENTITY_CREATED;
                break;
            case DELETED:
                msgType = DataConstants.ENTITY_DELETED;
                break;
            case UPDATED:
                msgType = DataConstants.ENTITY_UPDATED;
                break;
            case ASSIGNED_TO_CUSTOMER:
                msgType = DataConstants.ENTITY_ASSIGNED;
                break;
            case UNASSIGNED_FROM_CUSTOMER:
                msgType = DataConstants.ENTITY_UNASSIGNED;
                break;
            case ATTRIBUTES_UPDATED:
                msgType = DataConstants.ATTRIBUTES_UPDATED;
                break;
            case ATTRIBUTES_DELETED:
                msgType = DataConstants.ATTRIBUTES_DELETED;
                break;
            case ALARM_ACK:
                msgType = DataConstants.ALARM_ACK;
                break;
            case ALARM_CLEAR:
                msgType = DataConstants.ALARM_CLEAR;
                break;
            case ALARM_DELETE:
                msgType = DataConstants.ALARM_DELETE;
                break;
            case ASSIGNED_FROM_TENANT:
                msgType = DataConstants.ENTITY_ASSIGNED_FROM_TENANT;
                break;
            case ASSIGNED_TO_TENANT:
                msgType = DataConstants.ENTITY_ASSIGNED_TO_TENANT;
                break;
            case PROVISION_SUCCESS:
                msgType = DataConstants.PROVISION_SUCCESS;
                break;
            case PROVISION_FAILURE:
                msgType = DataConstants.PROVISION_FAILURE;
                break;
            case TIMESERIES_UPDATED:
                msgType = DataConstants.TIMESERIES_UPDATED;
                break;
            case TIMESERIES_DELETED:
                msgType = DataConstants.TIMESERIES_DELETED;
                break;
            case ASSIGNED_TO_EDGE:
                msgType = DataConstants.ENTITY_ASSIGNED_TO_EDGE;
                break;
            case UNASSIGNED_FROM_EDGE:
                msgType = DataConstants.ENTITY_UNASSIGNED_FROM_EDGE;
                break;
        }
        if (!StringUtils.isEmpty(msgType)) {
            try {
                TbMsgMetaData metaData = new TbMsgMetaData();
                if (user != null) {
                    metaData.putValue("userId", user.getId().toString());
                    metaData.putValue("userName", user.getName());
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
                }
                ObjectNode entityNode;
                if (entity != null) {
                    if (entity instanceof OtaPackage) {
                        OtaPackage otaPackage = (OtaPackage) entity;
                        otaPackage.setData(null);
                        entityNode = json.valueToTree(otaPackage); //todo
                    } else {
                        entityNode = json.valueToTree(entity);
                        if (entityId.getEntityType() == EntityType.DASHBOARD) {
                            entityNode.put("configuration", "");
                        }
                    }
                } else {
                    entityNode = json.createObjectNode();
                    if (actionType == ActionType.ATTRIBUTES_UPDATED) {
                        String scope = extractParameter(String.class, 0, additionalInfo);
                        @SuppressWarnings("unchecked")
                        List<AttributeKvEntry> attributes = extractParameter(List.class, 1, additionalInfo);
                        metaData.putValue(DataConstants.SCOPE, scope);
                        if (attributes != null) {
                            for (AttributeKvEntry attr : attributes) {
                                addKvEntry(entityNode, attr);
                            }
                        }
                    } else if (actionType == ActionType.ATTRIBUTES_DELETED) {
                        String scope = extractParameter(String.class, 0, additionalInfo);
                        @SuppressWarnings("unchecked")
                        List<String> keys = extractParameter(List.class, 1, additionalInfo);
                        metaData.putValue(DataConstants.SCOPE, scope);
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
                    }
                }
                TbMsg tbMsg = TbMsg.newMsg(msgType, entityId, customerId, metaData, TbMsgDataType.JSON, json.writeValueAsString(entityNode));
                if (tenantId.isNullUid()) {
                    if (entity instanceof HasTenantId) {
                        tenantId = ((HasTenantId) entity).getTenantId();
                    }
                }
                tbClusterService.pushMsgToRuleEngine(tenantId, entityId, tbMsg, null);
            } catch (Exception e) {
                log.warn("[{}] Failed to push entity action to rule engine: {}", entityId, actionType, e);
            }
        }
    }

    public  <E extends HasName, I extends EntityId> void logEntityAction(User user, I entityId, E entity, CustomerId customerId,
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

    private void addTimeseries(ObjectNode entityNode, List<TsKvEntry> timeseries) throws Exception {
        if (timeseries != null && !timeseries.isEmpty()) {
            ArrayNode result = entityNode.putArray("timeseries");
            Map<Long, List<TsKvEntry>> groupedTelemetry = timeseries.stream()
                    .collect(Collectors.groupingBy(TsKvEntry::getTs));
            for (Map.Entry<Long, List<TsKvEntry>> entry : groupedTelemetry.entrySet()) {
                ObjectNode element = json.createObjectNode();
                element.put("ts", entry.getKey());
                ObjectNode values = element.putObject("values");
                for (TsKvEntry tsKvEntry : entry.getValue()) {
                    addKvEntry(values, tsKvEntry);
                }
                result.add(element);
            }
        }
    }

    private void addKvEntry(ObjectNode entityNode, KvEntry kvEntry) throws Exception {
        if (kvEntry.getDataType() == DataType.BOOLEAN) {
            kvEntry.getBooleanValue().ifPresent(value -> entityNode.put(kvEntry.getKey(), value));
        } else if (kvEntry.getDataType() == DataType.DOUBLE) {
            kvEntry.getDoubleValue().ifPresent(value -> entityNode.put(kvEntry.getKey(), value));
        } else if (kvEntry.getDataType() == DataType.LONG) {
            kvEntry.getLongValue().ifPresent(value -> entityNode.put(kvEntry.getKey(), value));
        } else if (kvEntry.getDataType() == DataType.JSON) {
            if (kvEntry.getJsonValue().isPresent()) {
                entityNode.set(kvEntry.getKey(), json.readTree(kvEntry.getJsonValue().get()));
            }
        } else {
            entityNode.put(kvEntry.getKey(), kvEntry.getValueAsString());
        }
    }
}
