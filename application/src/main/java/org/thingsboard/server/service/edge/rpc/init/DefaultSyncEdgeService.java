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
package org.thingsboard.server.service.edge.rpc.init;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.DataType;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.page.TimePageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntityRelationsQuery;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationsSearchParameters;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.edge.EdgeEventService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.gen.edge.AttributesRequestMsg;
import org.thingsboard.server.gen.edge.DeviceCredentialsRequestMsg;
import org.thingsboard.server.gen.edge.RelationRequestMsg;
import org.thingsboard.server.gen.edge.RuleChainMetadataRequestMsg;
import org.thingsboard.server.gen.edge.UserCredentialsRequestMsg;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class DefaultSyncEdgeService implements SyncEdgeService {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private EdgeEventService edgeEventService;

    @Autowired
    private AttributesService attributesService;

    @Autowired
    private RuleChainService ruleChainService;

    @Autowired
    private RelationService relationService;

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private AssetService assetService;

    @Autowired
    private EntityViewService entityViewService;

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private UserService userService;

    @Autowired
    private DbCallbackExecutorService dbCallbackExecutorService;

    @Override
    public void sync(Edge edge) {
        try {
            syncUsers(edge);
            syncRuleChains(edge);
            syncDevices(edge);
            syncAssets(edge);
            syncEntityViews(edge);
            syncDashboards(edge);
        } catch (Exception e) {
            log.error("Exception during sync process", e);
        }
    }

    private void syncRuleChains(Edge edge) {
        try {
            ListenableFuture<TimePageData<RuleChain>> future =
                    ruleChainService.findRuleChainsByTenantIdAndEdgeId(edge.getTenantId(), edge.getId(), new TimePageLink(Integer.MAX_VALUE));
            Futures.addCallback(future, new FutureCallback<TimePageData<RuleChain>>() {
                @Override
                public void onSuccess(@Nullable TimePageData<RuleChain> pageData) {
                    if (pageData != null && pageData.getData() != null && !pageData.getData().isEmpty()) {
                        log.trace("[{}] [{}] rule chains(s) are going to be pushed to edge.", edge.getId(), pageData.getData().size());
                        for (RuleChain ruleChain : pageData.getData()) {
                            saveEdgeEvent(edge.getTenantId(), edge.getId(), EdgeEventType.RULE_CHAIN, ActionType.ADDED, ruleChain.getId(), null);
                        }
                    }
                }

                @Override
                public void onFailure(Throwable t) {
                    log.error("Exception during loading edge rule chain(s) on sync!", t);
                }
            }, dbCallbackExecutorService);
        } catch (Exception e) {
            log.error("Exception during loading edge rule chain(s) on sync!", e);
        }
    }

    private void syncDevices(Edge edge) {
        try {
            ListenableFuture<TimePageData<Device>> future =
                    deviceService.findDevicesByTenantIdAndEdgeId(edge.getTenantId(), edge.getId(), new TimePageLink(Integer.MAX_VALUE));
            Futures.addCallback(future, new FutureCallback<TimePageData<Device>>() {
                @Override
                public void onSuccess(@Nullable TimePageData<Device> pageData) {
                    if (pageData != null && pageData.getData() != null && !pageData.getData().isEmpty()) {
                        log.trace("[{}] [{}] device(s) are going to be pushed to edge.", edge.getId(), pageData.getData().size());
                        for (Device device : pageData.getData()) {
                            saveEdgeEvent(edge.getTenantId(), edge.getId(), EdgeEventType.DEVICE, ActionType.ADDED, device.getId(), null);
                        }
                    }
                }

                @Override
                public void onFailure(Throwable t) {
                    log.error("Exception during loading edge device(s) on sync!", t);
                }
            }, dbCallbackExecutorService);
        } catch (Exception e) {
            log.error("Exception during loading edge device(s) on sync!", e);
        }
    }

    private void syncAssets(Edge edge) {
        try {
            ListenableFuture<TimePageData<Asset>> future = assetService.findAssetsByTenantIdAndEdgeId(edge.getTenantId(), edge.getId(), new TimePageLink(Integer.MAX_VALUE));
            Futures.addCallback(future, new FutureCallback<TimePageData<Asset>>() {
                @Override
                public void onSuccess(@Nullable TimePageData<Asset> pageData) {
                    if (pageData != null && pageData.getData() != null && !pageData.getData().isEmpty()) {
                        log.trace("[{}] [{}] asset(s) are going to be pushed to edge.", edge.getId(), pageData.getData().size());
                        for (Asset asset : pageData.getData()) {
                            saveEdgeEvent(edge.getTenantId(), edge.getId(), EdgeEventType.ASSET, ActionType.ADDED, asset.getId(), null);
                        }
                    }
                }

                @Override
                public void onFailure(Throwable t) {
                    log.error("Exception during loading edge asset(s) on sync!", t);
                }
            }, dbCallbackExecutorService);
        } catch (Exception e) {
            log.error("Exception during loading edge asset(s) on sync!", e);
        }
    }

    private void syncEntityViews(Edge edge) {
        try {
            ListenableFuture<TimePageData<EntityView>> future = entityViewService.findEntityViewsByTenantIdAndEdgeId(edge.getTenantId(), edge.getId(), new TimePageLink(Integer.MAX_VALUE));
            Futures.addCallback(future, new FutureCallback<TimePageData<EntityView>>() {
                @Override
                public void onSuccess(@Nullable TimePageData<EntityView> pageData) {
                    if (pageData != null && pageData.getData() != null && !pageData.getData().isEmpty()) {
                        log.trace("[{}] [{}] entity view(s) are going to be pushed to edge.", edge.getId(), pageData.getData().size());
                        for (EntityView entityView : pageData.getData()) {
                            saveEdgeEvent(edge.getTenantId(), edge.getId(), EdgeEventType.ENTITY_VIEW, ActionType.ADDED, entityView.getId(), null);
                        }
                    }
                }

                @Override
                public void onFailure(Throwable t) {
                    log.error("Exception during loading edge entity view(s) on sync!", t);
                }
            }, dbCallbackExecutorService);
        } catch (Exception e) {
            log.error("Exception during loading edge entity view(s) on sync!", e);
        }
    }

    private void syncDashboards(Edge edge) {
        try {
            ListenableFuture<TimePageData<DashboardInfo>> future = dashboardService.findDashboardsByTenantIdAndEdgeId(edge.getTenantId(), edge.getId(), new TimePageLink(Integer.MAX_VALUE));
            Futures.addCallback(future, new FutureCallback<TimePageData<DashboardInfo>>() {
                @Override
                public void onSuccess(@Nullable TimePageData<DashboardInfo> pageData) {
                    if (pageData != null && pageData.getData() != null && !pageData.getData().isEmpty()) {
                        log.trace("[{}] [{}] dashboard(s) are going to be pushed to edge.", edge.getId(), pageData.getData().size());
                        for (DashboardInfo dashboardInfo : pageData.getData()) {
                            saveEdgeEvent(edge.getTenantId(), edge.getId(), EdgeEventType.DASHBOARD, ActionType.ADDED, dashboardInfo.getId(), null);
                        }
                    }
                }

                @Override
                public void onFailure(Throwable t) {
                    log.error("Exception during loading edge dashboard(s) on sync!", t);
                }
            }, dbCallbackExecutorService);
        } catch (Exception e) {
            log.error("Exception during loading edge dashboard(s) on sync!", e);
        }
    }

    private void syncUsers(Edge edge) {
        try {
            TextPageData<User> pageData = userService.findTenantAdmins(edge.getTenantId(), new TextPageLink(Integer.MAX_VALUE));
            pushUsersToEdge(pageData, edge);
            if (edge.getCustomerId() != null && !EntityId.NULL_UUID.equals(edge.getCustomerId().getId())) {
                pageData = userService.findCustomerUsers(edge.getTenantId(), edge.getCustomerId(), new TextPageLink(Integer.MAX_VALUE));
                pushUsersToEdge(pageData, edge);
            }
        } catch (Exception e) {
            log.error("Exception during loading edge user(s) on sync!", e);
        }
    }

    private void pushUsersToEdge(TextPageData<User> pageData, Edge edge) {
        if (pageData != null && pageData.getData() != null && !pageData.getData().isEmpty()) {
            log.trace("[{}] [{}] user(s) are going to be pushed to edge.", edge.getId(), pageData.getData().size());
            for (User user : pageData.getData()) {
                saveEdgeEvent(edge.getTenantId(), edge.getId(), EdgeEventType.USER, ActionType.ADDED, user.getId(), null);
            }
        }
    }

    @Override
    public void processRuleChainMetadataRequestMsg(Edge edge, RuleChainMetadataRequestMsg ruleChainMetadataRequestMsg) {
        if (ruleChainMetadataRequestMsg.getRuleChainIdMSB() != 0 && ruleChainMetadataRequestMsg.getRuleChainIdLSB() != 0) {
            RuleChainId ruleChainId = new RuleChainId(new UUID(ruleChainMetadataRequestMsg.getRuleChainIdMSB(), ruleChainMetadataRequestMsg.getRuleChainIdLSB()));
            saveEdgeEvent(edge.getTenantId(), edge.getId(), EdgeEventType.RULE_CHAIN_METADATA, ActionType.ADDED, ruleChainId, null);
        }
    }

    @Override
    public void processAttributesRequestMsg(Edge edge, AttributesRequestMsg attributesRequestMsg) {
        EntityId entityId = EntityIdFactory.getByTypeAndUuid(
                EntityType.valueOf(attributesRequestMsg.getEntityType()),
                new UUID(attributesRequestMsg.getEntityIdMSB(), attributesRequestMsg.getEntityIdLSB()));
        final EdgeEventType edgeEventType = getEdgeQueueTypeByEntityType(entityId.getEntityType());
        if (edgeEventType != null) {
            ListenableFuture<List<AttributeKvEntry>> ssAttrFuture = attributesService.findAll(edge.getTenantId(), entityId, DataConstants.SERVER_SCOPE);
            Futures.addCallback(ssAttrFuture, new FutureCallback<List<AttributeKvEntry>>() {
                @Override
                public void onSuccess(@Nullable List<AttributeKvEntry> ssAttributes) {
                    if (ssAttributes != null && !ssAttributes.isEmpty()) {
                        try {
                            ObjectNode entityNode = mapper.createObjectNode();
                            for (AttributeKvEntry attr : ssAttributes) {
                                if (attr.getDataType() == DataType.BOOLEAN && attr.getBooleanValue().isPresent()) {
                                    entityNode.put(attr.getKey(), attr.getBooleanValue().get());
                                } else if (attr.getDataType() == DataType.DOUBLE && attr.getDoubleValue().isPresent()) {
                                    entityNode.put(attr.getKey(), attr.getDoubleValue().get());
                                } else if (attr.getDataType() == DataType.LONG && attr.getLongValue().isPresent()) {
                                    entityNode.put(attr.getKey(), attr.getLongValue().get());
                                } else {
                                    entityNode.put(attr.getKey(), attr.getValueAsString());
                                }
                            }
                            log.debug("Sending attributes data msg, entityId [{}], attributes [{}]", entityId, entityNode);
                            saveEdgeEvent(edge.getTenantId(),
                                    edge.getId(),
                                    edgeEventType,
                                    ActionType.ATTRIBUTES_UPDATED,
                                    entityId,
                                    entityNode);
                        } catch (Exception e) {
                            log.error("[{}] Failed to send attribute updates to the edge", edge.getName(), e);
                        }
                    }
                }

                @Override
                public void onFailure(Throwable t) {

                }
            }, dbCallbackExecutorService);

            // TODO: voba - push shared attributes to edge?
            ListenableFuture<List<AttributeKvEntry>> shAttrFuture = attributesService.findAll(edge.getTenantId(), entityId, DataConstants.SHARED_SCOPE);
            ListenableFuture<List<AttributeKvEntry>> clAttrFuture = attributesService.findAll(edge.getTenantId(), entityId, DataConstants.CLIENT_SCOPE);
        }
    }

    private EdgeEventType getEdgeQueueTypeByEntityType(EntityType entityType) {
        switch (entityType) {
            case DEVICE:
                return EdgeEventType.DEVICE;
            case ASSET:
                return EdgeEventType.ASSET;
            case ENTITY_VIEW:
                return EdgeEventType.ENTITY_VIEW;
            default:
                return null;
        }
    }

    @Override
    public void processRelationRequestMsg(Edge edge, RelationRequestMsg relationRequestMsg) {
        EntityId entityId = EntityIdFactory.getByTypeAndUuid(
                EntityType.valueOf(relationRequestMsg.getEntityType()),
                new UUID(relationRequestMsg.getEntityIdMSB(), relationRequestMsg.getEntityIdLSB()));

        List<ListenableFuture<List<EntityRelation>>> futures = new ArrayList<>();
        futures.add(findRelationByQuery(edge, entityId, EntitySearchDirection.FROM));
        futures.add(findRelationByQuery(edge, entityId, EntitySearchDirection.TO));
        ListenableFuture<List<List<EntityRelation>>> relationsListFuture = Futures.allAsList(futures);
        Futures.addCallback(relationsListFuture, new FutureCallback<List<List<EntityRelation>>>() {
            @Override
            public void onSuccess(@Nullable List<List<EntityRelation>> relationsList) {
                try {
                    if (!relationsList.isEmpty()) {
                        for (List<EntityRelation> entityRelations : relationsList) {
                            log.trace("[{}] [{}] [{}] relation(s) are going to be pushed to edge.", edge.getId(), entityId, entityRelations.size());
                            for (EntityRelation relation : entityRelations) {
                                try {
                                    if (!relation.getFrom().getEntityType().equals(EntityType.EDGE) &&
                                            !relation.getTo().getEntityType().equals(EntityType.EDGE)) {
                                        saveEdgeEvent(edge.getTenantId(),
                                                edge.getId(),
                                                EdgeEventType.RELATION,
                                                ActionType.ADDED,
                                                null,
                                                mapper.valueToTree(relation));
                                    }
                                } catch (Exception e) {
                                    log.error("Exception during loading relation [{}] to edge on sync!", relation, e);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("Exception during loading relation(s) to edge on sync!", e);
                }
            }

            @Override
            public void onFailure(Throwable t) {
                log.error("Exception during loading relation(s) to edge on sync!", t);
            }
        }, dbCallbackExecutorService);
    }

    private ListenableFuture<List<EntityRelation>> findRelationByQuery(Edge edge, EntityId entityId, EntitySearchDirection direction) {
        EntityRelationsQuery query = new EntityRelationsQuery();
        query.setParameters(new RelationsSearchParameters(entityId, direction, -1, false));
        return relationService.findByQuery(edge.getTenantId(), query);
    }

    @Override
    public void processDeviceCredentialsRequestMsg(Edge edge, DeviceCredentialsRequestMsg deviceCredentialsRequestMsg) {
        if (deviceCredentialsRequestMsg.getDeviceIdMSB() != 0 && deviceCredentialsRequestMsg.getDeviceIdLSB() != 0) {
            DeviceId deviceId = new DeviceId(new UUID(deviceCredentialsRequestMsg.getDeviceIdMSB(), deviceCredentialsRequestMsg.getDeviceIdLSB()));
            saveEdgeEvent(edge.getTenantId(), edge.getId(), EdgeEventType.DEVICE, ActionType.CREDENTIALS_UPDATED, deviceId, null);
        }
    }

    @Override
    public void processUserCredentialsRequestMsg(Edge edge, UserCredentialsRequestMsg userCredentialsRequestMsg) {
        if (userCredentialsRequestMsg.getUserIdMSB() != 0 && userCredentialsRequestMsg.getUserIdLSB() != 0) {
            UserId userId = new UserId(new UUID(userCredentialsRequestMsg.getUserIdMSB(), userCredentialsRequestMsg.getUserIdLSB()));
            saveEdgeEvent(edge.getTenantId(), edge.getId(), EdgeEventType.USER, ActionType.CREDENTIALS_UPDATED, userId, null);
        }
    }

    private void saveEdgeEvent(TenantId tenantId,
                               EdgeId edgeId,
                               EdgeEventType edgeEventType,
                               ActionType edgeEventAction,
                               EntityId entityId,
                               JsonNode entityBody) {
        log.debug("Pushing edge event to edge queue. tenantId [{}], edgeId [{}], edgeEventType [{}], edgeEventAction[{}], entityId [{}], entityBody [{}]",
                tenantId, edgeId, edgeEventType, edgeEventAction, entityId, entityBody);

        EdgeEvent edgeEvent = new EdgeEvent();
        edgeEvent.setTenantId(tenantId);
        edgeEvent.setEdgeId(edgeId);
        edgeEvent.setEdgeEventType(edgeEventType);
        edgeEvent.setEdgeEventAction(edgeEventAction.name());
        if (entityId != null) {
            edgeEvent.setEntityId(entityId.getId());
        }
        edgeEvent.setEntityBody(entityBody);
        edgeEventService.saveAsync(edgeEvent);
    }
}
