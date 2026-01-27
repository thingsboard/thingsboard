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
package org.thingsboard.server.service.edge.rpc.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasCustomerId;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasVersion;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.data.page.PageDataIterableByTenantIdEntityId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainConnectionInfo;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.edge.EdgeSynchronizationManager;
import org.thingsboard.server.dao.entity.EntityDaoRegistry;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueMsgMetadata;
import org.thingsboard.server.service.edge.EdgeContextComponent;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;
import org.thingsboard.server.service.state.DefaultDeviceStateService;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.thingsboard.server.dao.edge.BaseRelatedEdgesService.RELATED_EDGES_CACHE_ITEMS;

@Slf4j
public abstract class BaseEdgeProcessor implements EdgeProcessor {

    protected static final Lock deviceCreationLock = new ReentrantLock();
    protected static final Lock assetCreationLock = new ReentrantLock();

    @Lazy
    @Autowired
    protected EdgeContextComponent edgeCtx;

    @Autowired
    protected EntityDaoRegistry entityDaoRegistry;

    @Autowired
    protected EdgeSynchronizationManager edgeSynchronizationManager;

    @Autowired
    protected DbCallbackExecutorService dbCallbackExecutorService;

    protected ListenableFuture<Void> saveEdgeEvent(TenantId tenantId,
                                                   EdgeId edgeId,
                                                   EdgeEventType type,
                                                   EdgeEventActionType action,
                                                   EntityId entityId,
                                                   JsonNode body) {
        return saveEdgeEvent(tenantId, edgeId, type, action, entityId, body, true);
    }

    protected ListenableFuture<Void> saveEdgeEvent(TenantId tenantId,
                                                   EdgeId edgeId,
                                                   EdgeEventType type,
                                                   EdgeEventActionType action,
                                                   EntityId entityId,
                                                   JsonNode body,
                                                   boolean doValidate) {
        if (doValidate) {
            ListenableFuture<Optional<AttributeKvEntry>> future =
                    edgeCtx.getAttributesService().find(tenantId, edgeId, AttributeScope.SERVER_SCOPE, DefaultDeviceStateService.ACTIVITY_STATE);
            return Futures.transformAsync(future, activeOpt -> {
                if (activeOpt.isEmpty()) {
                    log.trace("Edge is not activated. Skipping event. tenantId [{}], edgeId [{}], type[{}], " +
                                    "action [{}], entityId [{}], body [{}]",
                            tenantId, edgeId, type, action, entityId, body);
                    return Futures.immediateFuture(null);
                }
                if (activeOpt.get().getBooleanValue().isPresent() && activeOpt.get().getBooleanValue().get()) {
                    return doSaveEdgeEvent(tenantId, edgeId, type, action, entityId, body);
                } else {
                    if (doSaveIfEdgeIsOffline(type, action)) {
                        return doSaveEdgeEvent(tenantId, edgeId, type, action, entityId, body);
                    } else {
                        log.trace("Edge is not active at the moment. Skipping event. tenantId [{}], edgeId [{}], type[{}], " +
                                        "action [{}], entityId [{}], body [{}]",
                                tenantId, edgeId, type, action, entityId, body);
                        return Futures.immediateFuture(null);
                    }
                }
            }, dbCallbackExecutorService);
        } else {
            return doSaveEdgeEvent(tenantId, edgeId, type, action, entityId, body);
        }
    }

    private boolean doSaveIfEdgeIsOffline(EdgeEventType type, EdgeEventActionType action) {
        return switch (action) {
            case TIMESERIES_UPDATED, ALARM_ACK, ALARM_CLEAR, ALARM_ASSIGNED, ALARM_UNASSIGNED, ADDED_COMMENT,
                 UPDATED_COMMENT, DELETED -> true;
            default -> switch (type) {
                case ALARM, ALARM_COMMENT, RULE_CHAIN, RULE_CHAIN_METADATA, USER, CUSTOMER, TENANT, TENANT_PROFILE,
                     WIDGETS_BUNDLE, WIDGET_TYPE, ADMIN_SETTINGS, OTA_PACKAGE, QUEUE, RELATION, CALCULATED_FIELD, AI_MODEL, NOTIFICATION_TEMPLATE,
                     NOTIFICATION_TARGET, NOTIFICATION_RULE -> true;
                default -> false;
            };
        };
    }

    private ListenableFuture<Void> doSaveEdgeEvent(TenantId tenantId, EdgeId edgeId, EdgeEventType type, EdgeEventActionType action, EntityId entityId, JsonNode body) {
        log.debug("Pushing event to edge queue. tenantId [{}], edgeId [{}], type[{}], action [{}], entityId [{}], body [{}]",
                tenantId, edgeId, type, action, entityId, body);

        EdgeEvent edgeEvent = EdgeUtils.constructEdgeEvent(tenantId, edgeId, type, action, entityId, body);
        return edgeCtx.getEdgeEventService().saveAsync(edgeEvent);
    }

    protected ListenableFuture<Void> processActionForAllEdges(TenantId tenantId, EdgeEventType type,
                                                              EdgeEventActionType actionType, EntityId entityId,
                                                              JsonNode body, EdgeId sourceEdgeId) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        if (TenantId.SYS_TENANT_ID.equals(tenantId)) {
            PageDataIterable<Edge> edges = new PageDataIterable<>(link -> edgeCtx.getEdgeService().findActiveEdges(link), 1024);
            for (Edge edge : edges) {
                futures.add(saveEdgeEvent(edge.getTenantId(), edge.getId(), type, actionType, entityId, body, false));
            }
        } else {
            futures = processActionForAllEdgesByTenantId(tenantId, type, actionType, entityId, null, sourceEdgeId);
        }
        return Futures.transform(Futures.allAsList(futures), voids -> null, dbCallbackExecutorService);
    }

    private List<ListenableFuture<Void>> processActionForAllEdgesByTenantId(TenantId tenantId,
                                                                            EdgeEventType type,
                                                                            EdgeEventActionType actionType,
                                                                            EntityId entityId,
                                                                            JsonNode body,
                                                                            EdgeId sourceEdgeId) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        PageDataIterable<Edge> edges = new PageDataIterable<>(link -> edgeCtx.getEdgeService().findEdgesByTenantId(tenantId, link), 1024);
        for (Edge edge : edges) {
            if (!edge.getId().equals(sourceEdgeId)) {
                futures.add(saveEdgeEvent(tenantId, edge.getId(), type, actionType, entityId, body));
            }
        }
        return futures;
    }

    protected ListenableFuture<Void> handleUnsupportedMsgType(UpdateMsgType msgType) {
        String errMsg = String.format("Unsupported msg type %s", msgType);
        log.error(errMsg);
        return Futures.immediateFailedFuture(new RuntimeException(errMsg));
    }

    protected UpdateMsgType getUpdateMsgType(EdgeEventActionType actionType) {
        return switch (actionType) {
            case UPDATED, CREDENTIALS_UPDATED, ASSIGNED_TO_CUSTOMER, UNASSIGNED_FROM_CUSTOMER, UPDATED_COMMENT ->
                    UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE;
            case ADDED, ASSIGNED_TO_EDGE, RELATION_ADD_OR_UPDATE, ADDED_COMMENT -> UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE;
            case DELETED, UNASSIGNED_FROM_EDGE, RELATION_DELETED, DELETED_COMMENT, ALARM_DELETE -> UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE;
            case ALARM_ACK -> UpdateMsgType.ALARM_ACK_RPC_MESSAGE;
            case ALARM_CLEAR -> UpdateMsgType.ALARM_CLEAR_RPC_MESSAGE;
            default -> throw new RuntimeException("Unsupported actionType [" + actionType + "]");
        };
    }

    @Override
    public ListenableFuture<Void> processEntityNotification(TenantId tenantId, TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg) {
        EdgeEventType type = EdgeEventType.valueOf(edgeNotificationMsg.getType());
        EdgeEventActionType actionType = EdgeEventActionType.valueOf(edgeNotificationMsg.getAction());
        EntityId entityId = EntityIdFactory.getByEdgeEventTypeAndUuid(type, new UUID(edgeNotificationMsg.getEntityIdMSB(), edgeNotificationMsg.getEntityIdLSB()));
        EdgeId originatorEdgeId = safeGetEdgeId(edgeNotificationMsg.getOriginatorEdgeIdMSB(), edgeNotificationMsg.getOriginatorEdgeIdLSB());
        if (type.isAllEdgesRelated()) {
            return processEntityNotificationForAllEdges(tenantId, type, actionType, entityId, originatorEdgeId);
        } else {
            JsonNode body = JacksonUtil.toJsonNode(edgeNotificationMsg.getBody());
            EdgeId edgeId = safeGetEdgeId(edgeNotificationMsg.getEdgeIdMSB(), edgeNotificationMsg.getEdgeIdLSB());
            switch (actionType) {
                case UPDATED:
                case CREDENTIALS_UPDATED:
                case ASSIGNED_TO_CUSTOMER:
                case UNASSIGNED_FROM_CUSTOMER:
                    if (edgeId != null && !edgeId.equals(originatorEdgeId)) {
                        return saveEdgeEvent(tenantId, edgeId, type, actionType, entityId, body);
                    } else {
                        return processNotificationToRelatedEdges(tenantId, entityId, entityId, type, actionType, originatorEdgeId);
                    }
                case DELETED:
                    EdgeEventActionType deleted = EdgeEventActionType.DELETED;
                    if (edgeId != null) {
                        return saveEdgeEvent(tenantId, edgeId, type, deleted, entityId, body);
                    } else {
                        return Futures.transform(Futures.allAsList(processActionForAllEdgesByTenantId(tenantId, type, deleted, entityId, body, originatorEdgeId)),
                                voids -> null, dbCallbackExecutorService);
                    }
                case ASSIGNED_TO_EDGE:
                case UNASSIGNED_FROM_EDGE:
                    if (originatorEdgeId == null) {
                        ListenableFuture<Void> future = saveEdgeEvent(tenantId, edgeId, type, actionType, entityId, body);
                        return Futures.transformAsync(future, unused -> {
                            if (type.equals(EdgeEventType.RULE_CHAIN)) {
                                return updateDependentRuleChains(tenantId, new RuleChainId(entityId.getId()), edgeId);
                            } else {
                                return Futures.immediateFuture(null);
                            }
                        }, dbCallbackExecutorService);
                    } else {
                        return Futures.immediateFuture(null);
                    }
                default:
                    return Futures.immediateFuture(null);
            }
        }
    }

    protected EdgeId safeGetEdgeId(long edgeIdMSB, long edgeIdLSB) {
        if (edgeIdMSB != 0 && edgeIdLSB != 0) {
            return new EdgeId(new UUID(edgeIdMSB, edgeIdLSB));
        } else {
            return null;
        }
    }

    protected ListenableFuture<Void> processNotificationToRelatedEdges(TenantId tenantId, EntityId ownerEntityId, EntityId entityId, EdgeEventType type,
                                                                       EdgeEventActionType actionType, EdgeId sourceEdgeId) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        PageDataIterableByTenantIdEntityId<EdgeId> edgeIds =
                new PageDataIterableByTenantIdEntityId<>(edgeCtx.getEdgeService()::findRelatedEdgeIdsByEntityId, tenantId, ownerEntityId, RELATED_EDGES_CACHE_ITEMS);
        for (EdgeId relatedEdgeId : edgeIds) {
            if (!relatedEdgeId.equals(sourceEdgeId)) {
                futures.add(saveEdgeEvent(tenantId, relatedEdgeId, type, actionType, entityId, null));
            }
        }
        return Futures.transform(Futures.allAsList(futures), voids -> null, dbCallbackExecutorService);
    }

    private ListenableFuture<Void> updateDependentRuleChains(TenantId tenantId, RuleChainId processingRuleChainId, EdgeId edgeId) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        PageDataIterable<RuleChain> ruleChains = new PageDataIterable<>(link -> edgeCtx.getRuleChainService().findRuleChainsByTenantIdAndEdgeId(tenantId, edgeId, link), 1024);
        for (RuleChain ruleChain : ruleChains) {
            List<RuleChainConnectionInfo> connectionInfos =
                    edgeCtx.getRuleChainService().loadRuleChainMetaData(ruleChain.getTenantId(), ruleChain.getId()).getRuleChainConnections();
            if (connectionInfos != null && !connectionInfos.isEmpty()) {
                for (RuleChainConnectionInfo connectionInfo : connectionInfos) {
                    if (connectionInfo.getTargetRuleChainId().equals(processingRuleChainId)) {
                        futures.add(saveEdgeEvent(tenantId,
                                edgeId,
                                EdgeEventType.RULE_CHAIN_METADATA,
                                EdgeEventActionType.UPDATED,
                                ruleChain.getId(),
                                null));
                    }
                }
            }
        }
        return Futures.transform(Futures.allAsList(futures), voids -> null, dbCallbackExecutorService);
    }

    private ListenableFuture<Void> processEntityNotificationForAllEdges(TenantId tenantId, EdgeEventType type, EdgeEventActionType actionType, EntityId entityId, EdgeId sourceEdgeId) {
        return switch (actionType) {
            case ADDED, UPDATED, DELETED, CREDENTIALS_UPDATED -> // used by USER entity
                    processActionForAllEdges(tenantId, type, actionType, entityId, null, sourceEdgeId);
            default -> Futures.immediateFuture(null);
        };
    }

    protected EntityId constructEntityId(String entityTypeStr, long entityIdMSB, long entityIdLSB) {
        EntityType entityType = EntityType.valueOf(entityTypeStr);
        return switch (entityType) {
            case DEVICE -> new DeviceId(new UUID(entityIdMSB, entityIdLSB));
            case ASSET -> new AssetId(new UUID(entityIdMSB, entityIdLSB));
            case ENTITY_VIEW -> new EntityViewId(new UUID(entityIdMSB, entityIdLSB));
            case DASHBOARD -> new DashboardId(new UUID(entityIdMSB, entityIdLSB));
            case TENANT -> TenantId.fromUUID(new UUID(entityIdMSB, entityIdLSB));
            case CUSTOMER -> new CustomerId(new UUID(entityIdMSB, entityIdLSB));
            case USER -> new UserId(new UUID(entityIdMSB, entityIdLSB));
            case EDGE -> new EdgeId(new UUID(entityIdMSB, entityIdLSB));
            default -> {
                log.warn("Unsupported entity type [{}] during construct of entity id. entityIdMSB [{}], entityIdLSB [{}]",
                        entityTypeStr, entityIdMSB, entityIdLSB);
                yield null;
            }
        };
    }

    protected UUID safeGetUUID(long mSB, long lSB) {
        return mSB != 0 && lSB != 0 ? new UUID(mSB, lSB) : null;
    }

    protected CustomerId safeGetCustomerId(long mSB, long lSB) {
        CustomerId customerId = null;
        UUID customerUUID = safeGetUUID(mSB, lSB);
        if (customerUUID != null) {
            customerId = new CustomerId(customerUUID);
        }
        return customerId;
    }

    protected boolean isEntityExists(TenantId tenantId, EntityId entityId) {
        return entityDaoRegistry.getDao(entityId.getEntityType()).existsById(tenantId, entityId.getId());
    }

    protected void createRelationFromEdge(TenantId tenantId, EdgeId edgeId, EntityId entityId) {
        EntityRelation relation = new EntityRelation();
        relation.setFrom(edgeId);
        relation.setTo(entityId);
        relation.setTypeGroup(RelationTypeGroup.COMMON);
        relation.setType(EntityRelation.EDGE_TYPE);
        edgeCtx.getRelationService().saveRelation(tenantId, relation);
    }

    protected <T extends HasId<? extends EntityId>> void pushEntityEventToRuleEngine(TenantId tenantId, T entity, TbMsgType msgType) {
        pushEntityEventToRuleEngine(tenantId, null, entity, msgType);
    }

    protected <T extends HasId<? extends EntityId>> void pushEntityEventToRuleEngine(TenantId tenantId, Edge edge, T entity, TbMsgType msgType) {
        try {
            String entityAsString = JacksonUtil.toString(entity);
            CustomerId customerId = getCustomerId(entity);
            TbMsgMetaData tbMsgMetaData = edge == null ? TbMsgMetaData.EMPTY : getEdgeActionTbMsgMetaData(edge, customerId);

            pushEntityEventToRuleEngine(tenantId, entity.getId(), customerId, msgType, entityAsString, tbMsgMetaData);
        } catch (Exception e) {
            log.warn("[{}][{}] Failed to push entity action for {} to rule engine: {}", tenantId, entity.getId(), entity.getId().getEntityType(), msgType.name(), e);
        }
    }

    private <T extends HasId<? extends EntityId>> CustomerId getCustomerId(T entity) {
        if (entity instanceof HasCustomerId hasCustomer) {
            return hasCustomer.getCustomerId();
        }
        return null;
    }

    protected TbMsgMetaData getEdgeActionTbMsgMetaData(Edge edge, CustomerId customerId) {
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("edgeId", edge.getId().toString());
        metaData.putValue("edgeName", edge.getName());
        if (customerId != null && !customerId.isNullUid()) {
            metaData.putValue("customerId", customerId.toString());
        }
        return metaData;
    }

    protected void pushEntityEventToRuleEngine(TenantId tenantId, EntityId entityId, CustomerId customerId,
                                               TbMsgType msgType, String msgData, TbMsgMetaData metaData) {
        TbMsg tbMsg = TbMsg.newMsg()
                .type(msgType)
                .originator(entityId)
                .customerId(customerId)
                .copyMetaData(metaData)
                .dataType(TbMsgDataType.JSON)
                .data(msgData)
                .build();
        edgeCtx.getClusterService().pushMsgToRuleEngine(tenantId, entityId, tbMsg, new TbQueueCallback() {
            @Override
            public void onSuccess(TbQueueMsgMetadata metadata) {
                log.debug("[{}] Successfully send ENTITY_CREATED EVENT to rule engine [{}]", tenantId, msgData);
            }

            @Override
            public void onFailure(Throwable t) {
                log.warn("[{}] Failed to send ENTITY_CREATED EVENT to rule engine [{}]", tenantId, msgData, t);
            }
        });
    }

    protected boolean isSaveRequired(HasVersion current, HasVersion updated) {
        updated.setVersion(null);
        return !updated.equals(current);
    }

    protected <I extends EntityId, E extends HasName & HasId<I>> Optional<String> generateUniqueNameIfDuplicateExists(
            TenantId tenantId, I entityId, E entity, @Nullable E entityWithSameName) {

        if (entityWithSameName == null || entityWithSameName.getId().equals(entityId)) {
            return Optional.empty();
        }
        String currentName = entity.getName();
        String newEntityName = generateRandomAlphabeticString(currentName);

        log.warn("[{}] Entity with name '{}' already exists (id={}). Renaming to '{}'", tenantId, currentName, entityWithSameName.getId(), newEntityName);
        return Optional.of(newEntityName);
    }

    protected static String generateRandomAlphabeticString(String prefix) {
        return prefix + "_" + StringUtils.randomAlphabetic(15);
    }

}
