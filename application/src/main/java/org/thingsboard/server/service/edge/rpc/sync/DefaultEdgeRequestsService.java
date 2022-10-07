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
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.id.WidgetsBundleId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.DataType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntityRelationsQuery;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.relation.RelationsSearchParameters;
import org.thingsboard.server.common.data.widget.WidgetType;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.dao.asset.AssetProfileService;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.edge.EdgeEventService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.widget.WidgetTypeService;
import org.thingsboard.server.dao.widget.WidgetsBundleService;
import org.thingsboard.server.gen.edge.v1.AssetProfileAssetsRequestMsg;
import org.thingsboard.server.gen.edge.v1.AttributesRequestMsg;
import org.thingsboard.server.gen.edge.v1.DeviceCredentialsRequestMsg;
import org.thingsboard.server.gen.edge.v1.DeviceProfileDevicesRequestMsg;
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

    private static final int DEFAULT_PAGE_SIZE = 1000;

    @Autowired
    private EdgeEventService edgeEventService;

    @Autowired
    private AttributesService attributesService;

    @Autowired
    private RelationService relationService;

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private AssetService assetService;

    @Lazy
    @Autowired
    private TbEntityViewService entityViewService;

    @Autowired
    private DeviceProfileService deviceProfileService;

    @Autowired
    private AssetProfileService assetProfileService;

    @Autowired
    private WidgetsBundleService widgetsBundleService;

    @Autowired
    private WidgetTypeService widgetTypeService;

    @Autowired
    private DbCallbackExecutorService dbCallbackExecutorService;

    @Autowired
    private TbClusterService tbClusterService;

    @Override
    public ListenableFuture<Void> processRuleChainMetadataRequestMsg(TenantId tenantId, Edge edge, RuleChainMetadataRequestMsg ruleChainMetadataRequestMsg) {
        log.trace("[{}] processRuleChainMetadataRequestMsg [{}][{}]", tenantId, edge.getName(), ruleChainMetadataRequestMsg);
        if (ruleChainMetadataRequestMsg.getRuleChainIdMSB() == 0 || ruleChainMetadataRequestMsg.getRuleChainIdLSB() == 0) {
            return Futures.immediateFuture(null);
        }
        RuleChainId ruleChainId =
                new RuleChainId(new UUID(ruleChainMetadataRequestMsg.getRuleChainIdMSB(), ruleChainMetadataRequestMsg.getRuleChainIdLSB()));
        return saveEdgeEvent(tenantId, edge.getId(),
                EdgeEventType.RULE_CHAIN_METADATA, EdgeEventActionType.ADDED, ruleChainId, null);
    }

    @Override
    public ListenableFuture<Void> processAttributesRequestMsg(TenantId tenantId, Edge edge, AttributesRequestMsg attributesRequestMsg) {
        log.trace("[{}] processAttributesRequestMsg [{}][{}]", tenantId, edge.getName(), attributesRequestMsg);
        EntityId entityId = EntityIdFactory.getByTypeAndUuid(
                EntityType.valueOf(attributesRequestMsg.getEntityType()),
                new UUID(attributesRequestMsg.getEntityIdMSB(), attributesRequestMsg.getEntityIdLSB()));
        final EdgeEventType type = EdgeUtils.getEdgeEventTypeByEntityType(entityId.getEntityType());
        if (type == null) {
            log.warn("[{}] Type doesn't supported {}", tenantId, entityId.getEntityType());
            return Futures.immediateFuture(null);
        }
        SettableFuture<Void> futureToSet = SettableFuture.create();
        String scope = attributesRequestMsg.getScope();
        ListenableFuture<List<AttributeKvEntry>> findAttrFuture = attributesService.findAll(tenantId, entityId, scope);
        Futures.addCallback(findAttrFuture, new FutureCallback<>() {
            @Override
            public void onSuccess(@Nullable List<AttributeKvEntry> ssAttributes) {
                if (ssAttributes == null || ssAttributes.isEmpty()) {
                    log.trace("[{}][{}] No attributes found for entity {} [{}]", tenantId,
                            edge.getName(),
                            entityId.getEntityType(),
                            entityId.getId());
                    futureToSet.set(null);
                    return;
                }

                try {
                    Map<String, Object> entityData = new HashMap<>();
                    ObjectNode attributes = JacksonUtil.OBJECT_MAPPER.createObjectNode();
                    for (AttributeKvEntry attr : ssAttributes) {
                        if (DefaultDeviceStateService.PERSISTENT_ATTRIBUTES.contains(attr.getKey())) {
                            continue;
                        }
                        if (attr.getDataType() == DataType.BOOLEAN && attr.getBooleanValue().isPresent()) {
                            attributes.put(attr.getKey(), attr.getBooleanValue().get());
                        } else if (attr.getDataType() == DataType.DOUBLE && attr.getDoubleValue().isPresent()) {
                            attributes.put(attr.getKey(), attr.getDoubleValue().get());
                        } else if (attr.getDataType() == DataType.LONG && attr.getLongValue().isPresent()) {
                            attributes.put(attr.getKey(), attr.getLongValue().get());
                        } else {
                            attributes.put(attr.getKey(), attr.getValueAsString());
                        }
                    }
                    entityData.put("kv", attributes);
                    entityData.put("scope", scope);
                    JsonNode body = JacksonUtil.OBJECT_MAPPER.valueToTree(entityData);
                    log.debug("Sending attributes data msg, entityId [{}], attributes [{}]", entityId, body);
                    ListenableFuture<Void> future = saveEdgeEvent(tenantId, edge.getId(), type, EdgeEventActionType.ATTRIBUTES_UPDATED, entityId, body);
                    Futures.addCallback(future, new FutureCallback<>() {
                        @Override
                        public void onSuccess(@Nullable Void unused) {
                            futureToSet.set(null);
                        }

                        @Override
                        public void onFailure(Throwable throwable) {
                            String errMsg = String.format("[%s] Failed to save edge event [%s]", edge.getId(), attributesRequestMsg);
                            log.error(errMsg, throwable);
                            futureToSet.setException(new RuntimeException(errMsg, throwable));
                        }
                    }, dbCallbackExecutorService);
                } catch (Exception e) {
                    String errMsg = String.format("[%s] Failed to save attribute updates to the edge [%s]", edge.getId(), attributesRequestMsg);
                    log.error(errMsg, e);
                    futureToSet.setException(new RuntimeException(errMsg, e));
                }
            }

            @Override
            public void onFailure(Throwable t) {
                String errMsg = String.format("[%s] Can't find attributes [%s]", edge.getId(), attributesRequestMsg);
                log.error(errMsg, t);
                futureToSet.setException(new RuntimeException(errMsg, t));
            }
        }, dbCallbackExecutorService);
        return futureToSet;
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
                            log.trace("[{}] [{}] [{}] relation(s) are going to be pushed to edge.", edge.getId(), entityId, entityRelations.size());
                            for (EntityRelation relation : entityRelations) {
                                try {
                                    if (!relation.getFrom().getEntityType().equals(EntityType.EDGE) &&
                                            !relation.getTo().getEntityType().equals(EntityType.EDGE)) {
                                        futures.add(saveEdgeEvent(tenantId,
                                                edge.getId(),
                                                EdgeEventType.RELATION,
                                                EdgeEventActionType.ADDED,
                                                null,
                                                JacksonUtil.OBJECT_MAPPER.valueToTree(relation)));
                                    }
                                } catch (Exception e) {
                                    String errMsg = String.format("[%s] Exception during loading relation [%s] to edge on sync!", edge.getId(), relation);
                                    log.error(errMsg, e);
                                    futureToSet.setException(new RuntimeException(errMsg, e));
                                    return;
                                }
                            }
                        }
                        Futures.addCallback(Futures.allAsList(futures), new FutureCallback<>() {
                            @Override
                            public void onSuccess(@Nullable List<Void> voids) {
                                futureToSet.set(null);
                            }

                            @Override
                            public void onFailure(Throwable throwable) {
                                String errMsg = String.format("[%s] Exception during saving edge events [%s]!", edge.getId(), relationRequestMsg);
                                log.error(errMsg, throwable);
                                futureToSet.setException(new RuntimeException(errMsg, throwable));
                            }
                        }, dbCallbackExecutorService);
                    } else {
                        futureToSet.set(null);
                    }
                } catch (Exception e) {
                    log.error("Exception during loading relation(s) to edge on sync!", e);
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

    private ListenableFuture<List<EntityRelation>> findRelationByQuery(TenantId tenantId, Edge edge,
                                                                       EntityId entityId, EntitySearchDirection direction) {
        EntityRelationsQuery query = new EntityRelationsQuery();
        query.setParameters(new RelationsSearchParameters(entityId, direction, -1, false));
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
    public ListenableFuture<Void> processDeviceProfileDevicesRequestMsg(TenantId tenantId, Edge edge, DeviceProfileDevicesRequestMsg deviceProfileDevicesRequestMsg) {
        log.trace("[{}] processDeviceProfileDevicesRequestMsg [{}][{}]", tenantId, edge.getName(), deviceProfileDevicesRequestMsg);
        if (deviceProfileDevicesRequestMsg.getDeviceProfileIdMSB() == 0 || deviceProfileDevicesRequestMsg.getDeviceProfileIdLSB() == 0) {
            return Futures.immediateFuture(null);
        }
        DeviceProfileId deviceProfileId = new DeviceProfileId(new UUID(deviceProfileDevicesRequestMsg.getDeviceProfileIdMSB(), deviceProfileDevicesRequestMsg.getDeviceProfileIdLSB()));
        DeviceProfile deviceProfileById = deviceProfileService.findDeviceProfileById(tenantId, deviceProfileId);
        if (deviceProfileById == null) {
            return Futures.immediateFuture(null);
        }
        return syncDevices(tenantId, edge, deviceProfileById.getName());
    }

    private ListenableFuture<Void> syncDevices(TenantId tenantId, Edge edge, String deviceType) {
        log.trace("[{}] syncDevices [{}][{}]", tenantId, edge.getName(), deviceType);
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        try {
            PageLink pageLink = new PageLink(DEFAULT_PAGE_SIZE);
            PageData<Device> pageData;
            do {
                pageData = deviceService.findDevicesByTenantIdAndEdgeIdAndType(tenantId, edge.getId(), deviceType, pageLink);
                if (pageData != null && pageData.getData() != null && !pageData.getData().isEmpty()) {
                    log.trace("[{}] [{}] device(s) are going to be pushed to edge.", edge.getId(), pageData.getData().size());
                    for (Device device : pageData.getData()) {
                        futures.add(saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.DEVICE, EdgeEventActionType.ADDED, device.getId(), null));
                    }
                    if (pageData.hasNext()) {
                        pageLink = pageLink.nextPageLink();
                    }
                }
            } while (pageData != null && pageData.hasNext());
        } catch (Exception e) {
            log.error("Exception during loading edge device(s) on sync!", e);
        }
        return Futures.transform(Futures.allAsList(futures), voids -> null, dbCallbackExecutorService);
    }

    @Override
    public ListenableFuture<Void> processAssetProfileAssetsRequestMsg(TenantId tenantId, Edge edge, AssetProfileAssetsRequestMsg assetProfileAssetsRequestMsg) {
        log.trace("[{}] processAssetProfileAssetsRequestMsg [{}][{}]", tenantId, edge.getName(), assetProfileAssetsRequestMsg);
        if (assetProfileAssetsRequestMsg.getAssetProfileIdMSB() == 0 || assetProfileAssetsRequestMsg.getAssetProfileIdLSB() == 0) {
            return Futures.immediateFuture(null);
        }
        AssetProfileId assetProfileId = new AssetProfileId(new UUID(assetProfileAssetsRequestMsg.getAssetProfileIdMSB(), assetProfileAssetsRequestMsg.getAssetProfileIdLSB()));
        AssetProfile assetProfileById = assetProfileService.findAssetProfileById(tenantId, assetProfileId);
        if (assetProfileById == null) {
            return Futures.immediateFuture(null);
        }
        return syncAssets(tenantId, edge, assetProfileById.getName());
    }

    private ListenableFuture<Void> syncAssets(TenantId tenantId, Edge edge, String assetType) {
        log.trace("[{}] syncAssets [{}][{}]", tenantId, edge.getName(), assetType);
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        try {
            PageLink pageLink = new PageLink(DEFAULT_PAGE_SIZE);
            PageData<Asset> pageData;
            do {
                pageData = assetService.findAssetsByTenantIdAndEdgeIdAndType(tenantId, edge.getId(), assetType, pageLink);
                if (pageData != null && pageData.getData() != null && !pageData.getData().isEmpty()) {
                    log.trace("[{}] [{}] asset(s) are going to be pushed to edge.", edge.getId(), pageData.getData().size());
                    for (Asset asset : pageData.getData()) {
                        futures.add(saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.ASSET, EdgeEventActionType.ADDED, asset.getId(), null));
                    }
                    if (pageData.hasNext()) {
                        pageLink = pageLink.nextPageLink();
                    }
                }
            } while (pageData != null && pageData.hasNext());
        } catch (Exception e) {
            log.error("Exception during loading edge asset(s) on sync!", e);
        }
        return Futures.transform(Futures.allAsList(futures), voids -> null, dbCallbackExecutorService);
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
                        widgetTypeService.findWidgetTypesByTenantIdAndBundleAlias(widgetsBundleById.getTenantId(), widgetsBundleById.getAlias());
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
                        log.error("Exception during loading relation to edge on sync!", t);
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

        return Futures.transform(edgeEventService.saveAsync(edgeEvent), unused -> {
            tbClusterService.onEdgeEventUpdate(tenantId, edgeId);
            return null;
        }, dbCallbackExecutorService);
    }

}
