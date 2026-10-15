// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.edge.rpc.processor.asset;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.edge.rpc.EdgeVersionComparator;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.gen.edge.v1.AssetUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.EdgeMsgConstructorUtils;

import java.util.UUID;

@Slf4j
@Component
@TbCoreComponent
public class AssetEdgeProcessor extends BaseAssetProcessor implements AssetProcessor {

    @Override
    public ListenableFuture<Void> processAssetMsgFromEdge(TenantId tenantId, Edge edge, AssetUpdateMsg assetUpdateMsg, EdgeVersion edgeVersion) {
        log.trace("[{}] executing processAssetMsgFromEdge [{}] from edge [{}]", tenantId, assetUpdateMsg, edge.getId());
        AssetId assetId = new AssetId(new UUID(assetUpdateMsg.getIdMSB(), assetUpdateMsg.getIdLSB()));
        try {
            edgeSynchronizationManager.getEdgeId().set(edge.getId());

            return switch (assetUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE, ENTITY_UPDATED_RPC_MESSAGE -> {
                    saveOrUpdateAsset(tenantId, assetId, assetUpdateMsg, edge);
                    yield Futures.immediateFuture(null);
                }
                case ENTITY_DELETED_RPC_MESSAGE -> {
                    if (assetUpdateMsg.hasEntityGroupIdMSB() && assetUpdateMsg.hasEntityGroupIdLSB()) {
                        EntityGroupId entityGroupId = new EntityGroupId(
                                new UUID(assetUpdateMsg.getEntityGroupIdMSB(), assetUpdateMsg.getEntityGroupIdLSB()));
                        edgeCtx.getEntityGroupService().removeEntityFromEntityGroup(tenantId, entityGroupId, assetId);
                    } else if (EdgeVersionComparator.INSTANCE.compare(edgeVersion, EdgeVersion.V_4_3_0) >= 0) {
                        deleteAsset(tenantId, edge, assetId);
                    } else {
                        removeAssetFromEdgeAllAssetGroup(tenantId, edge, assetId);
                    }
                    yield Futures.immediateFuture(null);
                }
                default -> handleUnsupportedMsgType(assetUpdateMsg.getMsgType());
            };
        } catch (DataValidationException | ThingsboardException e) {
            if (e.getMessage().contains("limit reached")) {
                log.warn("[{}] Number of allowed asset violated {}", tenantId, assetUpdateMsg, e);
                return Futures.immediateFuture(null);
            } else {
                return Futures.immediateFailedFuture(e);
            }
        } finally {
            edgeSynchronizationManager.getEdgeId().remove();
        }
    }

    private void saveOrUpdateAsset(TenantId tenantId, AssetId assetId, AssetUpdateMsg assetUpdateMsg, Edge edge) throws ThingsboardException {
        Pair<Boolean, Boolean> resultPair = super.saveOrUpdateAsset(tenantId, assetId, assetUpdateMsg);
        Boolean created = resultPair.getFirst();
        if (created) {
            createRelationFromEdge(tenantId, edge.getId(), assetId);
            pushAssetCreatedEventToRuleEngine(tenantId, edge, assetId);
        }
        addAssetToEdgeAllAssetGroup(tenantId, edge, assetId);
        Boolean assetNameUpdated = resultPair.getSecond();
        if (assetNameUpdated) {
            saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.ASSET, EdgeEventActionType.UPDATED, assetId, null);
        }
    }

    private void pushAssetCreatedEventToRuleEngine(TenantId tenantId, Edge edge, AssetId assetId) {
        Asset asset = edgeCtx.getAssetService().findAssetById(tenantId, assetId);
        pushEntityEventToRuleEngine(tenantId, edge, asset, TbMsgType.ENTITY_CREATED);
    }

    private void addAssetToEdgeAllAssetGroup(TenantId tenantId, Edge edge, AssetId assetId) {
        Asset asset = edgeCtx.getAssetService().findAssetById(tenantId, assetId);
        addEntityToEdgeAllGroup(tenantId, edge, asset);
    }

    private void removeAssetFromEdgeAllAssetGroup(TenantId tenantId, Edge edge, AssetId assetId) {
        Asset assetToDelete = edgeCtx.getAssetService().findAssetById(tenantId, assetId);
        removeEntityFromEdgeAllGroup(tenantId, edge, assetToDelete);
    }

    @Override
    public DownlinkMsg convertEdgeEventToDownlink(EdgeEvent edgeEvent, EdgeVersion edgeVersion) {
        AssetId assetId = new AssetId(edgeEvent.getEntityId());
        EntityGroupId entityGroupId = edgeEvent.getEntityGroupId() != null ? new EntityGroupId(edgeEvent.getEntityGroupId()) : null;
        switch (edgeEvent.getAction()) {
            case ADDED, ADDED_TO_ENTITY_GROUP, UPDATED, ASSIGNED_TO_EDGE -> {
                Asset asset = edgeCtx.getAssetService().findAssetById(edgeEvent.getTenantId(), assetId);
                if (asset != null) {
                    UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
                    AssetUpdateMsg assetUpdateMsg = EdgeMsgConstructorUtils.constructAssetUpdatedMsg(msgType, asset, entityGroupId);
                    DownlinkMsg.Builder builder = DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addAssetUpdateMsg(assetUpdateMsg);

                    if (UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE.equals(msgType)) {
                        AssetProfile assetProfile = edgeCtx.getAssetProfileService().findAssetProfileById(edgeEvent.getTenantId(), asset.getAssetProfileId());
                        builder.addAssetProfileUpdateMsg(EdgeMsgConstructorUtils.constructAssetProfileUpdatedMsg(msgType, assetProfile));
                    }
                    return builder.build();
                }
            }
            case DELETED, REMOVED_FROM_ENTITY_GROUP, UNASSIGNED_FROM_EDGE, CHANGE_OWNER -> {
                return DownlinkMsg.newBuilder()
                        .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                        .addAssetUpdateMsg(EdgeMsgConstructorUtils.constructAssetDeleteMsg(assetId, entityGroupId))
                        .build();
            }
        }
        return null;
    }

    @Override
    protected void setCustomerId(TenantId tenantId, CustomerId customerId, Asset asset, AssetUpdateMsg assetUpdateMsg) {
        CustomerId customerUUID = asset.getCustomerId() != null ? asset.getCustomerId() : customerId;
        asset.setCustomerId(customerUUID);
    }

    @Override
    public EdgeEventType getEdgeEventType() {
        return EdgeEventType.ASSET;
    }

}
