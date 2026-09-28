// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.rpc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.RpcId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.rpc.Rpc;
import org.thingsboard.server.common.data.rpc.RpcStatus;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.edge.EdgeEventService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.edge.EdgeSynchronizationManager;
import org.thingsboard.server.dao.rpc.RpcService;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.List;

@TbCoreComponent
@Service
@RequiredArgsConstructor
@Slf4j
public class TbRpcService {
    private final RpcService rpcService;
    private final TbClusterService tbClusterService;
    private final EdgeService edgeService;
    private final EdgeEventService edgeEventService;
    private final EdgeSynchronizationManager edgeSynchronizationManager;

    public Rpc save(TenantId tenantId, Rpc rpc) {
        Rpc saved = rpcService.save(rpc);
        pushRpcMsgToRuleEngine(tenantId, saved);
        return saved;
    }

    public void save(TenantId tenantId, RpcId rpcId, RpcStatus newStatus, JsonNode response) {
        Rpc foundRpc = rpcService.findById(tenantId, rpcId);
        if (foundRpc != null) {
            foundRpc.setStatus(newStatus);
            if (response != null) {
                foundRpc.setResponse(response);
            }
            Rpc saved = rpcService.save(foundRpc);
            pushRpcMsgToRuleEngine(tenantId, saved);
        } else {
            log.warn("[{}] Failed to update RPC status because RPC was already deleted", rpcId);
        }
    }

    private void pushRpcMsgToRuleEngine(TenantId tenantId, Rpc rpc) {
        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.valueOf("RPC_" + rpc.getStatus().name()))
                .originator(rpc.getDeviceId())
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(JacksonUtil.toString(rpc))
                .build();
        tbClusterService.pushMsgToRuleEngine(tenantId, rpc.getDeviceId(), msg, null);
    }

    public Rpc findRpcById(TenantId tenantId, RpcId rpcId) {
        return rpcService.findById(tenantId, rpcId);
    }

    public PageData<Rpc> findAllByDeviceIdAndStatus(TenantId tenantId, DeviceId deviceId, RpcStatus rpcStatus, PageLink pageLink) {
        return rpcService.findAllByDeviceIdAndStatus(tenantId, deviceId, rpcStatus, pageLink);
    }

    /**
     * The only edge-aware RPC delete: propagates the delete to related edges (Cloud -> Edge) for ANY status.
     * Other delete paths (RpcService.deleteRpc, TTL clean-up) intentionally do not sync, so each side expires its own copies.
     */
    public void deleteRpc(TenantId tenantId, Rpc rpc) {
        rpcService.deleteRpc(tenantId, rpc.getId());
        // skipped when the delete originated from an edge (edge-sync context) so it is not echoed back
        if (edgeSynchronizationManager.getEdgeId().get() == null) {
            pushRpcDeleteToEdges(tenantId, rpc);
        }
    }

    private void pushRpcDeleteToEdges(TenantId tenantId, Rpc rpc) {
        List<EdgeId> relatedEdgeIds = edgeService.findAllRelatedEdgeIds(tenantId, rpc.getDeviceId());
        if (relatedEdgeIds == null || relatedEdgeIds.isEmpty()) {
            return;
        }
        ObjectNode body = JacksonUtil.newObjectNode();
        body.put("requestUUID", rpc.getId().getId().toString());
        body.put("rpcStatus", RpcStatus.DELETED.name());
        for (EdgeId edgeId : relatedEdgeIds) {
            // persisted (not high-priority) so the delete is replayed when the edge reconnects
            EdgeEvent edgeEvent = EdgeUtils.constructEdgeEvent(tenantId, edgeId, EdgeEventType.DEVICE,
                    EdgeEventActionType.RPC_CALL, rpc.getDeviceId(), body);
            edgeEventService.saveAsync(edgeEvent);
        }
    }

}
