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
package org.thingsboard.server.service.edge.rpc.sync;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.id.WidgetsBundleId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.DataType;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntityRelationsQuery;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.relation.RelationsSearchParameters;
import org.thingsboard.server.common.data.widget.WidgetType;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.cf.CalculatedFieldService;
import org.thingsboard.server.dao.edge.EdgeEventService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.dao.widget.WidgetTypeService;
import org.thingsboard.server.dao.widget.WidgetsBundleService;
import org.thingsboard.server.gen.edge.v1.AttributesRequestMsg;
import org.thingsboard.server.gen.edge.v1.CalculatedFieldRequestMsg;
import org.thingsboard.server.gen.edge.v1.DeviceCredentialsRequestMsg;
import org.thingsboard.server.gen.edge.v1.EntityViewsRequestMsg;
import org.thingsboard.server.gen.edge.v1.RelationRequestMsg;
import org.thingsboard.server.gen.edge.v1.RuleChainMetadataRequestMsg;
import org.thingsboard.server.gen.edge.v1.UserCredentialsRequestMsg;
import org.thingsboard.server.gen.edge.v1.WidgetBundleTypesRequestMsg;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.entityview.TbEntityViewService;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;
import org.thingsboard.server.service.state.DefaultDeviceStateService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@TbCoreComponent
@Slf4j
public class DefaultEdgeRequestsService implements EdgeRequestsService {

    @Autowired
    private EdgeEventService edgeEventService;

    @Autowired
    private AttributesService attributesService;

    @Autowired
    private TimeseriesService timeseriesService;

    @Autowired
    private RelationService relationService;

    @Lazy
    @Autowired
    private TbEntityViewService entityViewService;

    @Autowired
    private WidgetsBundleService widgetsBundleService;

    @Autowired
    private WidgetTypeService widgetTypeService;

    @Autowired
    private CalculatedFieldService calculatedFieldService;

    @Autowired
    private DbCallbackExecutorService dbCallbackExecutorService;

    @Override
    public ListenableFuture<Void> processRuleChainMetadataRequestMsg(TenantId tenantId, Edge edge, RuleChainMetadataRequestMsg ruleChainMetadataRequestMsg) {
        log.trace("[{}] processRuleChainMetadataRequestMsg [{}][{}]", tenantId, edge.getName(), ruleChainMetadataRequestMsg);
        if (ruleChainMetadataRequestMsg.getRuleChainIdMSB() == 0 || ruleChainMetadataRequestMsg.getRuleChainIdLSB() == 0) {
            return Futures.immediateFuture(null);
        }
        RuleChainId ruleChainId = new RuleChainId(new UUID(ruleChainMetadataRequestMsg.getRuleChainIdMSB(), ruleChainMetadataRequestMsg.getRuleChainIdLSB()));
        return saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.RULE_CHAIN_METADATA, EdgeEventActionType.ADDED, ruleChainId, null);
    }

    @Override
    public ListenableFuture<Void> processAttributesRequestMsg(TenantId tenantId, Edge edge, AttributesRequestMsg attributesRequestMsg) {
        log.trace("[{}] processAttributesRequestMsg [{}][{}]", tenantId, edge.getName(), attributesRequestMsg);
        EntityId entityId = EntityIdFactory.getByTypeAndUuid(
                EntityType.valueOf(attributesRequestMsg.getEntityType()),
                new UUID(attributesRequestMsg.getEntityIdMSB(), attributesRequestMsg.getEntityIdLSB()));
        final EdgeEventType entityType = EdgeUtils.getEdgeEventTypeByEntityType(entityId.getEntityType());
        if (entityType == null) {
            log.warn("[{}] Type doesn't supported {}", tenantId, entityId.getEntityType());
            return Futures.immediateFuture(null);
        }
        String scope = attributesRequestMsg.getScope();
        ListenableFuture<List<AttributeKvEntry>> findAttrFuture = attributesService.findAll(tenantId, entityId, AttributeScope.valueOf(scope));
        return Futures.transformAsync(findAttrFuture, ssAttributes
                        -> processEntityAttributesAndAddToEdgeQueue(tenantId, entityId, edge, entityType, scope, ssAttributes, attributesRequestMsg),
                dbCallbackExecutorService);
    }

    private ListenableFuture<Void> processEntityAttributesAndAddToEdgeQueue(TenantId tenantId, EntityId entityId, Edge edge,
                                                                            EdgeEventType entityType, String scope, List<AttributeKvEntry> ssAttributes,
                                                                            AttributesRequestMsg attributesRequestMsg) {
        Map<String, Object> entityData = null;
        ObjectNode attributes = null;
        ListenableFuture<Void> future;
        try {
            if (ssAttributes == null || ssAttributes.isEmpty()) {
                log.trace("[{}][{}] No attributes found for entity {} [{}]", tenantId,
                        edge.getName(),
                        entityId.getEntityType(),
                        entityId.getId());
                future = Futures.immediateFuture(null);
            } else {
                entityData = new HashMap<>();
                attributes = JacksonUtil.newObjectNode();
                for (AttributeKvEntry attr : ssAttributes) {
                    if (DefaultDeviceStateService.ACTIVITY_KEYS_WITHOUT_INACTIVITY_TIMEOUT.contains(attr.getKey())) {
                        continue;
                    }
                    if (attr.getDataType() == DataType.BOOLEAN && attr.getBooleanValue().isPresent()) {
                        attributes.put(attr.getKey(), attr.getBooleanValue().get());
                    } else if (attr.getDataType() == DataType.DOUBLE && attr.getDoubleValue().isPresent()) {
                        attributes.put(attr.getKey(), attr.getDoubleValue().get());
                    } else if (attr.getDataType() == DataType.LONG && attr.getLongValue().isPresent()) {
                        attributes.put(attr.getKey(), attr.getLongValue().get());
                    } else if (attr.getDataType() == DataType.JSON && attr.getJsonValue().isPresent()) {
                        attributes.set(attr.getKey(), JacksonUtil.toJsonNode(attr.getJsonValue().get()));
                    } else {
                        attributes.put(attr.getKey(), attr.getValueAsString());
                    }
                }
                if (!attributes.isEmpty()) {
                    entityData.put("kv", attributes);
                    entityData.put("scope", scope);
                    JsonNode body = JacksonUtil.valueToTree(entityData);
                    log.debug("[{}] Sending attributes data msg, entityId [{}], attributes [{}]", tenantId, entityId, body);
                    future = saveEdgeEvent(tenantId, edge.getId(), entityType, EdgeEventActionType.ATTRIBUTES_UPDATED, entityId, body);
                } else {
                    future = Futures.immediateFuture(null);
                }
            }
            return Futures.transformAsync(future, v -> processLatestTimeseriesAndAddToEdgeQueue(tenantId, entityId, edge, entityType), dbCallbackExecutorService);
        } catch (Exception e) {
            String errMsg = String.format("[%s][%s] Failed to save attribute updates to the edge [%s], scope = %s, entityData = %s, attributes = %s",
                    tenantId, edge.getId(), attributesRequestMsg, scope, entityData, attributes);
            log.error(errMsg, e);
            return Futures.immediateFailedFuture(new RuntimeException(errMsg, e));
        }
    }

    private ListenableFuture<Void> processLatestTimeseriesAndAddToEdgeQueue(TenantId tenantId, EntityId entityId, Edge edge,
                                                                            EdgeEventType entityType) {
        ListenableFuture<List<TsKvEntry>> getAllLatestFuture = timeseriesService.findAllLatest(tenantId, entityId);
        return Futures.transformAsync(getAllLatestFuture, tsKvEntries -> {
            if (tsKvEntries == null || tsKvEntries.isEmpty()) {
                log.trace("[{}][{}] No timeseries found for entity {} [{}]", tenantId,
                        edge.getName(),
                        entityId.getEntityType(),
                        entityId.getId());
                return Futures.immediateFuture(null);
            }
            Map<Long, Map<String, Object>> tsData = new HashMap<>();
            for (TsKvEntry tsKvEntry : tsKvEntries) {
                if (DefaultDeviceStateService.ACTIVITY_KEYS_WITH_INACTIVITY_TIMEOUT.contains(tsKvEntry.getKey())) {
                    continue;
                }
                tsData.computeIfAbsent(tsKvEntry.getTs(), k -> new HashMap<>()).put(tsKvEntry.getKey(), tsKvEntry.getValue());
            }
            List<ListenableFuture<Void>> futures = new ArrayList<>();
            for (Map.Entry<Long, Map<String, Object>> entry : tsData.entrySet()) {
                Map<String, Object> entityBody = new HashMap<>();
                entityBody.put("data", entry.getValue());
                entityBody.put("ts", entry.getKey());
                futures.add(saveEdgeEvent(tenantId, edge.getId(), entityType, EdgeEventActionType.TIMESERIES_UPDATED, entityId, JacksonUtil.valueToTree(entityBody)));
            }
            return Futures.transform(Futures.allAsList(futures), v -> null, dbCallbackExecutorService);
        }, dbCallbackExecutorService);
    }

    @Override
    public ListenableFuture<Void> processRelationRequestMsg(TenantId tenantId, Edge edge, RelationRequestMsg relationRequestMsg) {
        log.trace("[{}] processRelationRequestMsg [{}][{}]", tenantId, edge.getName(), relationRequestMsg);
        EntityId entityId = EntityIdFactory.getByTypeAndUuid(
                EntityType.valueOf(relationRequestMsg.getEntityType()),
                new UUID(relationRequestMsg.getEntityIdMSB(), relationRequestMsg.getEntityIdLSB()));

        List<ListenableFuture<List<EntityRelation>>> futures = new ArrayList<>();
        futures.add(findRelationByQuery(tenantId, edge, entityId, EntitySearchDirection.FROM));
        futures.add(findRelationByQuery(tenantId, edge, entityId, EntitySearchDirection.TO));
        ListenableFuture<List<List<EntityRelation>>> relationsListFuture = Futures.allAsList(futures);
        SettableFuture<Void> futureToSet = SettableFuture.create();
        Futures.addCallback(relationsListFuture, new FutureCallback<>() {
            @Override
            public void onSuccess(@Nullable List<List<EntityRelation>> relationsList) {
                try {
                    if (relationsList != null && !relationsList.isEmpty()) {
                        List<ListenableFuture<Void>> futures = new ArrayList<>();
                        for (List<EntityRelation> entityRelations : relationsList) {
                            if (entityRelations.isEmpty()) {
                                continue;
                            }
                            log.trace("[{}][{}][{}][{}] relation(s) are going to be pushed to edge.", tenantId, edge.getId(), entityId, entityRelations.size());
                            for (EntityRelation relation : entityRelations) {
                                try {
                                    if (!relation.getFrom().getEntityType().equals(EntityType.EDGE) &&
                                            !relation.getTo().getEntityType().equals(EntityType.EDGE)) {
                                        futures.add(saveEdgeEvent(tenantId,
                                                edge.getId(),
                                                EdgeEventType.RELATION,
                                                EdgeEventActionType.ADDED,
                                                null,
                                                JacksonUtil.valueToTree(relation)));
                                    }
                                } catch (Exception e) {
                                    String errMsg = String.format("[%s][%s] Exception during loading relation [%s] to edge on sync!", tenantId, edge.getId(), relation);
                                    log.error(errMsg, e);
                                    futureToSet.setException(new RuntimeException(errMsg, e));
                                    return;
                                }
                            }
                        }
                        if (futures.isEmpty()) {
                            futureToSet.set(null);
                        } else {
                            Futures.addCallback(Futures.allAsList(futures), new FutureCallback<>() {
                                @Override
                                public void onSuccess(@Nullable List<Void> voids) {
                                    futureToSet.set(null);
                                }

                                @Override
                                public void onFailure(Throwable throwable) {
                                    String errMsg = String.format("[%s][%s] Exception during saving edge events [%s]!", tenantId, edge.getId(), relationRequestMsg);
                                    log.error(errMsg, throwable);
                                    futureToSet.setException(new RuntimeException(errMsg, throwable));
                                }
                            }, dbCallbackExecutorService);
                        }
                    } else {
                        futureToSet.set(null);
                    }
                } catch (Exception e) {
                    log.error("[{}] Exception during loading relation(s) to edge on sync!", tenantId, e);
                    futureToSet.setException(e);
                }
            }

            @Override
            public void onFailure(Throwable t) {
                String errMsg = String.format("[%s] Can't find relation by query. Entity id [%s]!", tenantId, entityId);
                log.error(errMsg, t);
                futureToSet.setException(new RuntimeException(errMsg, t));
            }
        }, dbCallbackExecutorService);
        return futureToSet;
    }

    @Override
    public ListenableFuture<Void> processCalculatedFieldRequestMsg(TenantId tenantId, Edge edge, CalculatedFieldRequestMsg calculatedFieldRequestMsg) {
        log.trace("[{}] processCalculatedFieldRequestMsg [{}][{}]", tenantId, edge.getName(), calculatedFieldRequestMsg);

        EntityId entityId = EntityIdFactory.getByTypeAndUuid(
                EntityType.valueOf(calculatedFieldRequestMsg.getEntityType()),
                new UUID(calculatedFieldRequestMsg.getEntityIdMSB(), calculatedFieldRequestMsg.getEntityIdLSB()));

        log.trace("[{}] processCalculatedField [{}][{}] for entity [{}][{}]", tenantId, edge.getName(), calculatedFieldRequestMsg, entityId.getEntityType(), entityId.getId());
        return saveCalculatedFieldsToEdge(tenantId, edge.getId(), entityId);
    }

    private ListenableFuture<Void> saveCalculatedFieldsToEdge(TenantId tenantId, EdgeId edgeId, EntityId entityId) {
        return Futures.transformAsync(
                dbCallbackExecutorService.submit(() -> calculatedFieldService.findCalculatedFieldsByEntityId(tenantId, entityId)),
                calculatedFields -> {
                    log.trace("[{}][{}][{}][{}] calculatedField(s) are going to be pushed to edge.", tenantId, edgeId, entityId, calculatedFields.size());

                    List<ListenableFuture<?>> futures = calculatedFields.stream().map(calculatedField -> {
                        try {
                            return saveEdgeEvent(tenantId, edgeId, EdgeEventType.CALCULATED_FIELD,
                                    EdgeEventActionType.ADDED, calculatedField.getId(), JacksonUtil.valueToTree(calculatedField));
                        } catch (Exception e) {
                            log.error("[{}][{}] Exception during loading calculatedField [{}] to edge on sync!", tenantId, edgeId, calculatedField, e);
                            return Futures.immediateFailedFuture(e);
                        }
                    }).toList();

                    return Futures.transform(
                            Futures.allAsList(futures),
                            voids -> null,
                            dbCallbackExecutorService
                    );
                },
                dbCallbackExecutorService
        );
    }

    private ListenableFuture<List<EntityRelation>> findRelationByQuery(TenantId tenantId, Edge edge, EntityId entityId, EntitySearchDirection direction) {
        EntityRelationsQuery query = new EntityRelationsQuery();
        query.setParameters(new RelationsSearchParameters(entityId, direction, 1, false));
        return relationService.findByQuery(tenantId, query);
    }

    @Override
    public ListenableFuture<Void> processDeviceCredentialsRequestMsg(TenantId tenantId, Edge edge, DeviceCredentialsRequestMsg deviceCredentialsRequestMsg) {
        log.trace("[{}] processDeviceCredentialsRequestMsg [{}][{}]", tenantId, edge.getName(), deviceCredentialsRequestMsg);
        if (deviceCredentialsRequestMsg.getDeviceIdMSB() == 0 || deviceCredentialsRequestMsg.getDeviceIdLSB() == 0) {
            return Futures.immediateFuture(null);
        }
        DeviceId deviceId = new DeviceId(new UUID(deviceCredentialsRequestMsg.getDeviceIdMSB(), deviceCredentialsRequestMsg.getDeviceIdLSB()));
        return saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.DEVICE,
                EdgeEventActionType.CREDENTIALS_UPDATED, deviceId, null);
    }

    @Override
    public ListenableFuture<Void> processUserCredentialsRequestMsg(TenantId tenantId, Edge edge, UserCredentialsRequestMsg userCredentialsRequestMsg) {
        log.trace("[{}] processUserCredentialsRequestMsg [{}][{}]", tenantId, edge.getName(), userCredentialsRequestMsg);
        if (userCredentialsRequestMsg.getUserIdMSB() == 0 || userCredentialsRequestMsg.getUserIdLSB() == 0) {
            return Futures.immediateFuture(null);
        }
        UserId userId = new UserId(new UUID(userCredentialsRequestMsg.getUserIdMSB(), userCredentialsRequestMsg.getUserIdLSB()));
        return saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.USER,
                EdgeEventActionType.CREDENTIALS_UPDATED, userId, null);
    }

    @Override
    public ListenableFuture<Void> processWidgetBundleTypesRequestMsg(TenantId tenantId, Edge edge,
                                                                     WidgetBundleTypesRequestMsg widgetBundleTypesRequestMsg) {
        log.trace("[{}] processWidgetBundleTypesRequestMsg [{}][{}]", tenantId, edge.getName(), widgetBundleTypesRequestMsg);
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        if (widgetBundleTypesRequestMsg.getWidgetBundleIdMSB() != 0 && widgetBundleTypesRequestMsg.getWidgetBundleIdLSB() != 0) {
            WidgetsBundleId widgetsBundleId = new WidgetsBundleId(new UUID(widgetBundleTypesRequestMsg.getWidgetBundleIdMSB(), widgetBundleTypesRequestMsg.getWidgetBundleIdLSB()));
            WidgetsBundle widgetsBundleById = widgetsBundleService.findWidgetsBundleById(tenantId, widgetsBundleId);
            if (widgetsBundleById != null) {
                List<WidgetType> widgetTypesToPush =
                        widgetTypeService.findWidgetTypesByWidgetsBundleId(widgetsBundleById.getTenantId(), widgetsBundleId);
                for (WidgetType widgetType : widgetTypesToPush) {
                    futures.add(saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.WIDGET_TYPE, EdgeEventActionType.ADDED, widgetType.getId(), null));
                }
            }
        }
        return Futures.transform(Futures.allAsList(futures), voids -> null, dbCallbackExecutorService);
    }

    @Override
    public ListenableFuture<Void> processEntityViewsRequestMsg(TenantId tenantId, Edge edge, EntityViewsRequestMsg entityViewsRequestMsg) {
        log.trace("[{}] processEntityViewsRequestMsg [{}][{}]", tenantId, edge.getName(), entityViewsRequestMsg);
        EntityId entityId = EntityIdFactory.getByTypeAndUuid(
                EntityType.valueOf(entityViewsRequestMsg.getEntityType()),
                new UUID(entityViewsRequestMsg.getEntityIdMSB(), entityViewsRequestMsg.getEntityIdLSB()));
        SettableFuture<Void> futureToSet = SettableFuture.create();
        Futures.addCallback(entityViewService.findEntityViewsByTenantIdAndEntityIdAsync(tenantId, entityId), new FutureCallback<>() {
            @Override
            public void onSuccess(@Nullable List<EntityView> entityViews) {
                if (entityViews == null || entityViews.isEmpty()) {
                    futureToSet.set(null);
                    return;
                }
                List<ListenableFuture<Void>> futures = new ArrayList<>();
                for (EntityView entityView : entityViews) {
                    ListenableFuture<Boolean> future = relationService.checkRelationAsync(tenantId, edge.getId(), entityView.getId(),
                            EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE);
                    futures.add(Futures.transformAsync(future, result -> {
                        if (Boolean.TRUE.equals(result)) {
                            return saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.ENTITY_VIEW,
                                    EdgeEventActionType.ADDED, entityView.getId(), null);
                        } else {
                            return Futures.immediateFuture(null);
                        }
                    }, dbCallbackExecutorService));
                }
                Futures.addCallback(Futures.allAsList(futures), new FutureCallback<>() {
                    @Override
                    public void onSuccess(@Nullable List<Void> result) {
                        futureToSet.set(null);
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        log.error("[{}] Exception during loading relation to edge on sync!", tenantId, t);
                        futureToSet.setException(t);
                    }
                }, dbCallbackExecutorService);
            }

            @Override
            public void onFailure(Throwable t) {
                log.error("[{}] Can't find entity views by entity id [{}]", tenantId, entityId, t);
                futureToSet.setException(t);
            }
        }, dbCallbackExecutorService);
        return futureToSet;
    }

    private ListenableFuture<Void> saveEdgeEvent(TenantId tenantId,
                                                 EdgeId edgeId,
                                                 EdgeEventType type,
                                                 EdgeEventActionType action,
                                                 EntityId entityId,
                                                 JsonNode body) {
        log.trace("Pushing edge event to edge queue. tenantId [{}], edgeId [{}], type [{}], action[{}], entityId [{}], body [{}]",
                tenantId, edgeId, type, action, entityId, body);

        EdgeEvent edgeEvent = EdgeUtils.constructEdgeEvent(tenantId, edgeId, type, action, entityId, body);
        return edgeEventService.saveAsync(edgeEvent);
    }

}
