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
import org.thingsboard.server.common.msg.edge.EdgeHighPriorityMsg;
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

    public void deleteRpc(TenantId tenantId, RpcId rpcId) {
        Rpc rpc = rpcService.findById(tenantId, rpcId);
        rpcService.deleteRpc(tenantId, rpcId);
        // RPC v2 (persistent) delete/abort propagation Cloud -> Edge for ANY status.
        // Skipped when the delete originated from an edge (edge-sync context) so it is not echoed back.
        if (rpc != null && edgeSynchronizationManager.getEdgeId().get() == null) {
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
            EdgeEvent edgeEvent = EdgeUtils.constructEdgeEvent(tenantId, edgeId, EdgeEventType.DEVICE,
                    EdgeEventActionType.RPC_CALL, rpc.getDeviceId(), body);
            tbClusterService.onEdgeHighPriorityMsg(new EdgeHighPriorityMsg(tenantId, edgeEvent));
        }
    }

}
