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

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.page.TimePageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntityRelationsQuery;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationsSearchParameters;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.gen.edge.AssetUpdateMsg;
import org.thingsboard.server.gen.edge.DashboardUpdateMsg;
import org.thingsboard.server.gen.edge.DeviceUpdateMsg;
import org.thingsboard.server.gen.edge.EntityUpdateMsg;
import org.thingsboard.server.gen.edge.EntityViewUpdateMsg;
import org.thingsboard.server.gen.edge.RelationUpdateMsg;
import org.thingsboard.server.gen.edge.ResponseMsg;
import org.thingsboard.server.gen.edge.RuleChainMetadataRequestMsg;
import org.thingsboard.server.gen.edge.RuleChainMetadataUpdateMsg;
import org.thingsboard.server.gen.edge.RuleChainUpdateMsg;
import org.thingsboard.server.gen.edge.UpdateMsgType;
import org.thingsboard.server.service.edge.rpc.constructor.AssetUpdateMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.DashboardUpdateMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.DeviceUpdateMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.EntityViewUpdateMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.RelationUpdateMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.RuleChainUpdateMsgConstructor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class DefaultSyncEdgeService implements SyncEdgeService {

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
    private RuleChainUpdateMsgConstructor ruleChainUpdateMsgConstructor;

    @Autowired
    private RelationUpdateMsgConstructor relationUpdateMsgConstructor;

    @Autowired
    private DeviceUpdateMsgConstructor deviceUpdateMsgConstructor;

    @Autowired
    private AssetUpdateMsgConstructor assetUpdateMsgConstructor;

    @Autowired
    private EntityViewUpdateMsgConstructor entityViewUpdateMsgConstructor;

    @Autowired
    private DashboardUpdateMsgConstructor dashboardUpdateMsgConstructor;

    @Override
    public void sync(Edge edge, StreamObserver<ResponseMsg> outputStream) {
        Set<EntityId> pushedEntityIds = new HashSet<>();
        syncRuleChains(edge, pushedEntityIds, outputStream);
        syncDevices(edge, pushedEntityIds, outputStream);
        syncAssets(edge, pushedEntityIds, outputStream);
        syncEntityViews(edge, pushedEntityIds, outputStream);
        syncDashboards(edge, pushedEntityIds, outputStream);
        syncRelations(edge, pushedEntityIds, outputStream);
    }

    private void syncRelations(Edge edge, Set<EntityId> pushedEntityIds, StreamObserver<ResponseMsg> outputStream) {
        if (!pushedEntityIds.isEmpty()) {
            List<ListenableFuture<List<EntityRelation>>> futures = new ArrayList<>();
            for (EntityId entityId : pushedEntityIds) {
                futures.add(syncRelations(edge, entityId, EntitySearchDirection.FROM));
                futures.add(syncRelations(edge, entityId, EntitySearchDirection.TO));
            }
            ListenableFuture<List<List<EntityRelation>>> relationsListFuture = Futures.allAsList(futures);
            Futures.transform(relationsListFuture, relationsList -> {
                try {
                    Set<EntityRelation> uniqueEntityRelations = new HashSet<>();
                    if (!relationsList.isEmpty()) {
                        for (List<EntityRelation> entityRelations : relationsList) {
                            if (!entityRelations.isEmpty()) {
                                uniqueEntityRelations.addAll(entityRelations);
                            }
                        }
                    }
                    if (!uniqueEntityRelations.isEmpty()) {
                        log.trace("[{}] [{}] relation(s) are going to be pushed to edge.", edge.getId(), uniqueEntityRelations.size());
                        for (EntityRelation relation : uniqueEntityRelations) {
                            try {
                                RelationUpdateMsg relationUpdateMsg =
                                        relationUpdateMsgConstructor.constructRelationUpdatedMsg(
                                                UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE,
                                                relation);
                                EntityUpdateMsg entityUpdateMsg = EntityUpdateMsg.newBuilder()
                                        .setRelationUpdateMsg(relationUpdateMsg)
                                        .build();
                                outputStream.onNext(ResponseMsg.newBuilder()
                                        .setEntityUpdateMsg(entityUpdateMsg)
                                        .build());
                            } catch (Exception e) {
                                log.error("Exception during loading relation [{}] to edge on init!", relation, e);
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("Exception during loading relation(s) to edge on init!", e);
                }
                return null;
            }, MoreExecutors.directExecutor());
        }
    }

    private ListenableFuture<List<EntityRelation>> syncRelations(Edge edge, EntityId entityId, EntitySearchDirection direction) {
        EntityRelationsQuery query = new EntityRelationsQuery();
        query.setParameters(new RelationsSearchParameters(entityId, direction, -1, false));
        return relationService.findByQuery(edge.getTenantId(), query);
    }

    private void syncDevices(Edge edge, Set<EntityId> pushedEntityIds, StreamObserver<ResponseMsg> outputStream) {
        try {
            TimePageLink pageLink = new TimePageLink(100);
            TimePageData<Device> pageData;
            do {
                pageData = deviceService.findDevicesByTenantIdAndEdgeId(edge.getTenantId(), edge.getId(), pageLink).get();
                if (!pageData.getData().isEmpty()) {
                    log.trace("[{}] [{}] device(s) are going to be pushed to edge.", edge.getId(), pageData.getData().size());
                    for (Device device : pageData.getData()) {
                        DeviceUpdateMsg deviceUpdateMsg =
                                deviceUpdateMsgConstructor.constructDeviceUpdatedMsg(
                                        UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE,
                                        device);
                        EntityUpdateMsg entityUpdateMsg = EntityUpdateMsg.newBuilder()
                                .setDeviceUpdateMsg(deviceUpdateMsg)
                                .build();
                        outputStream.onNext(ResponseMsg.newBuilder()
                                .setEntityUpdateMsg(entityUpdateMsg)
                                .build());
                        pushedEntityIds.add(device.getId());
                    }
                }
                if (pageData.hasNext()) {
                    pageLink = pageData.getNextPageLink();
                }
            } while (pageData.hasNext());
        } catch (Exception e) {
            log.error("Exception during loading edge device(s) on init!", e);
        }
    }

    private void syncAssets(Edge edge, Set<EntityId> pushedEntityIds, StreamObserver<ResponseMsg> outputStream) {
        try {
            TimePageLink pageLink = new TimePageLink(100);
            TimePageData<Asset> pageData;
            do {
                pageData = assetService.findAssetsByTenantIdAndEdgeId(edge.getTenantId(), edge.getId(), pageLink).get();
                if (!pageData.getData().isEmpty()) {
                    log.trace("[{}] [{}] asset(s) are going to be pushed to edge.", edge.getId(), pageData.getData().size());
                    for (Asset asset : pageData.getData()) {
                        AssetUpdateMsg assetUpdateMsg =
                                assetUpdateMsgConstructor.constructAssetUpdatedMsg(
                                        UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE,
                                        asset);
                        EntityUpdateMsg entityUpdateMsg = EntityUpdateMsg.newBuilder()
                                .setAssetUpdateMsg(assetUpdateMsg)
                                .build();
                        outputStream.onNext(ResponseMsg.newBuilder()
                                .setEntityUpdateMsg(entityUpdateMsg)
                                .build());
                        pushedEntityIds.add(asset.getId());
                    }
                }
                if (pageData.hasNext()) {
                    pageLink = pageData.getNextPageLink();
                }
            } while (pageData.hasNext());
        } catch (Exception e) {
            log.error("Exception during loading edge asset(s) on init!", e);
        }
    }

    private void syncEntityViews(Edge edge, Set<EntityId> pushedEntityIds, StreamObserver<ResponseMsg> outputStream) {
        try {
            TimePageLink pageLink = new TimePageLink(100);
            TimePageData<EntityView> pageData;
            do {
                pageData = entityViewService.findEntityViewsByTenantIdAndEdgeId(edge.getTenantId(), edge.getId(), pageLink).get();
                if (!pageData.getData().isEmpty()) {
                    log.trace("[{}] [{}] entity view(s) are going to be pushed to edge.", edge.getId(), pageData.getData().size());
                    for (EntityView entityView : pageData.getData()) {
                        EntityViewUpdateMsg entityViewUpdateMsg =
                                entityViewUpdateMsgConstructor.constructEntityViewUpdatedMsg(
                                        UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE,
                                        entityView);
                        EntityUpdateMsg entityUpdateMsg = EntityUpdateMsg.newBuilder()
                                .setEntityViewUpdateMsg(entityViewUpdateMsg)
                                .build();
                        outputStream.onNext(ResponseMsg.newBuilder()
                                .setEntityUpdateMsg(entityUpdateMsg)
                                .build());
                        pushedEntityIds.add(entityView.getId());
                    }
                }
                if (pageData.hasNext()) {
                    pageLink = pageData.getNextPageLink();
                }
            } while (pageData.hasNext());
        } catch (Exception e) {
            log.error("Exception during loading edge entity view(s) on init!", e);
        }
    }

    private void syncDashboards(Edge edge, Set<EntityId> pushedEntityIds, StreamObserver<ResponseMsg> outputStream) {
        try {
            TimePageLink pageLink = new TimePageLink(100);
            TimePageData<DashboardInfo> pageData;
            do {
                pageData = dashboardService.findDashboardsByTenantIdAndEdgeId(edge.getTenantId(), edge.getId(), pageLink).get();
                if (!pageData.getData().isEmpty()) {
                    log.trace("[{}] [{}] dashboard(s) are going to be pushed to edge.", edge.getId(), pageData.getData().size());
                    for (DashboardInfo dashboardInfo : pageData.getData()) {
                        Dashboard dashboard = dashboardService.findDashboardById(edge.getTenantId(), dashboardInfo.getId());
                        DashboardUpdateMsg dashboardUpdateMsg =
                                dashboardUpdateMsgConstructor.constructDashboardUpdatedMsg(
                                        UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE,
                                        dashboard);
                        EntityUpdateMsg entityUpdateMsg = EntityUpdateMsg.newBuilder()
                                .setDashboardUpdateMsg(dashboardUpdateMsg)
                                .build();
                        outputStream.onNext(ResponseMsg.newBuilder()
                                .setEntityUpdateMsg(entityUpdateMsg)
                                .build());
                        pushedEntityIds.add(dashboard.getId());
                    }
                }
                if (pageData.hasNext()) {
                    pageLink = pageData.getNextPageLink();
                }
            } while (pageData.hasNext());
        } catch (Exception e) {
            log.error("Exception during loading edge dashboard(s) on init!", e);
        }
    }

    private void syncRuleChains(Edge edge, Set<EntityId> pushedEntityIds, StreamObserver<ResponseMsg> outputStream) {
        try {
            TimePageLink pageLink = new TimePageLink(100);
            TimePageData<RuleChain> pageData;
            do {
                pageData = ruleChainService.findRuleChainsByTenantIdAndEdgeId(edge.getTenantId(), edge.getId(), pageLink).get();
                if (!pageData.getData().isEmpty()) {
                    log.trace("[{}] [{}] rule chains(s) are going to be pushed to edge.", edge.getId(), pageData.getData().size());
                    for (RuleChain ruleChain : pageData.getData()) {
                        RuleChainUpdateMsg ruleChainUpdateMsg =
                                ruleChainUpdateMsgConstructor.constructRuleChainUpdatedMsg(
                                        edge.getRootRuleChainId(),
                                        UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE,
                                        ruleChain);
                        EntityUpdateMsg entityUpdateMsg = EntityUpdateMsg.newBuilder()
                                .setRuleChainUpdateMsg(ruleChainUpdateMsg)
                                .build();
                        outputStream.onNext(ResponseMsg.newBuilder()
                                .setEntityUpdateMsg(entityUpdateMsg)
                                .build());
                        pushedEntityIds.add(ruleChain.getId());
                    }
                }
                if (pageData.hasNext()) {
                    pageLink = pageData.getNextPageLink();
                }
            } while (pageData.hasNext());
        } catch (Exception e) {
            log.error("Exception during loading edge rule chain(s) on init!", e);
        }
    }

    @Override
    public void syncRuleChainMetadata(Edge edge, RuleChainMetadataRequestMsg ruleChainMetadataRequestMsg, StreamObserver<ResponseMsg> outputStream) {
        if (ruleChainMetadataRequestMsg.getRuleChainIdMSB() != 0 && ruleChainMetadataRequestMsg.getRuleChainIdLSB() != 0) {
            RuleChainId ruleChainId = new RuleChainId(new UUID(ruleChainMetadataRequestMsg.getRuleChainIdMSB(), ruleChainMetadataRequestMsg.getRuleChainIdLSB()));
            RuleChainMetaData ruleChainMetaData = ruleChainService.loadRuleChainMetaData(edge.getTenantId(), ruleChainId);
            RuleChainMetadataUpdateMsg ruleChainMetadataUpdateMsg =
                    ruleChainUpdateMsgConstructor.constructRuleChainMetadataUpdatedMsg(
                            UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE,
                            ruleChainMetaData);
            if (ruleChainMetadataUpdateMsg != null) {
                EntityUpdateMsg entityUpdateMsg = EntityUpdateMsg.newBuilder()
                        .setRuleChainMetadataUpdateMsg(ruleChainMetadataUpdateMsg)
                        .build();
                outputStream.onNext(ResponseMsg.newBuilder()
                        .setEntityUpdateMsg(entityUpdateMsg)
                        .build());
            }
        }
    }
}
