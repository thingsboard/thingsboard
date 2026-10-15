// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.edge.rpc.processor.secret;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.SecretId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.data.secret.Secret;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.SecretUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.EdgeMsgConstructorUtils;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.utils.EdgeVersionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@TbCoreComponent
public class SecretEdgeProcessor extends BaseEdgeProcessor {

    @Override
    public DownlinkMsg convertEdgeEventToDownlink(EdgeEvent edgeEvent, EdgeVersion edgeVersion) {
        SecretId secretId = new SecretId(edgeEvent.getEntityId());

        switch (edgeEvent.getAction()) {
            case ADDED:
            case UPDATED:
                Secret secret = edgeCtx.getSecretService().findSecretById(edgeEvent.getTenantId(), secretId);
                if (secret != null && isSupportedByEdge(secret, edgeVersion)) {
                    UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
                    SecretUpdateMsg secretUpdateMsg = EdgeMsgConstructorUtils.constructSecretUpdatedMsg(msgType, secret);

                    return DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addSecretUpdateMsg(secretUpdateMsg)
                            .build();
                }
                break;

            case DELETED:
                SecretUpdateMsg deleteMsg = EdgeMsgConstructorUtils.constructSecretDeleteMsg(secretId);
                return DownlinkMsg.newBuilder()
                        .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                        .addSecretUpdateMsg(deleteMsg)
                        .build();
        }

        return null;
    }

    @Override
    public ListenableFuture<Void> processEntityNotification(TenantId tenantId, TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg) {
        EdgeEventType type = EdgeEventType.valueOf(edgeNotificationMsg.getType());
        EdgeEventActionType actionType = EdgeEventActionType.valueOf(edgeNotificationMsg.getAction());
        SecretId secretId = new SecretId(new UUID(edgeNotificationMsg.getEntityIdMSB(), edgeNotificationMsg.getEntityIdLSB()));
        EdgeId originatorEdgeId = safeGetEdgeId(edgeNotificationMsg.getOriginatorEdgeIdMSB(), edgeNotificationMsg.getOriginatorEdgeIdLSB());
        return switch (actionType) {
            case ADDED, UPDATED -> {
                Secret secret = edgeCtx.getSecretService().findSecretById(tenantId, secretId);
                if (secret == null) {
                    yield Futures.immediateFuture(null);
                }
                if (!EntityType.CUSTOMER.equals(secret.getOwnerId().getEntityType())) {
                    yield processActionForAllEdges(tenantId, type, actionType, secretId, null, originatorEdgeId);
                }
                CustomerId customerId = secret.getCustomerId();
                // edges of the customer and its sub-customers, read from the DB: the in-memory edge hierarchy is per node and goes stale
                List<ListenableFuture<Void>> futures = new ArrayList<>();
                for (EntityId ownerId : ownersCacheService.getChildOwners(tenantId, customerId)) {
                    PageDataIterable<Edge> edges = new PageDataIterable<>(
                            link -> edgeCtx.getEdgeService().findEdgesByTenantIdAndCustomerId(tenantId, new CustomerId(ownerId.getId()), link), 1024);
                    for (Edge edge : edges) {
                        if (!edge.getId().equals(originatorEdgeId)) {
                            futures.add(saveEdgeEvent(tenantId, edge.getId(), type, actionType, secretId, null));
                        }
                    }
                }
                yield Futures.transform(Futures.allAsList(futures), voids -> null, dbCallbackExecutorService);
            }
            // the delete event carries only the secret id, so like a role delete it goes to every edge
            case DELETED -> processActionForAllEdges(tenantId, type, actionType, secretId, null, originatorEdgeId);
            default -> Futures.immediateFuture(null);
        };
    }

    // older edges drop customerId and would store a customer secret as a tenant one
    private boolean isSupportedByEdge(Secret secret, EdgeVersion edgeVersion) {
        return !EntityType.CUSTOMER.equals(secret.getOwnerId().getEntityType())
                || !EdgeVersionUtils.isEdgeVersionOlderThan(edgeVersion, EdgeVersion.V_4_4_0_1);
    }

    @Override
    public EdgeEventType getEdgeEventType() {
        return EdgeEventType.SECRET;
    }

}
