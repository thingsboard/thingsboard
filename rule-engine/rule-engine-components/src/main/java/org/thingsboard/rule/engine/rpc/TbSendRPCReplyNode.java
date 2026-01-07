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
package org.thingsboard.rule.engine.rpc;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.UUID;

@RuleNode(
        type = ComponentType.ACTION,
        name = "rpc call reply",
        configClazz = TbSendRpcReplyNodeConfiguration.class,
        nodeDescription = "Sends reply to RPC call from device",
        nodeDetails = "Expects messages with any message type. Will forward message body to the device.",
        configDirective = "tbActionNodeRpcReplyConfig",
        icon = "call_merge",
        docUrl = "https://thingsboard.io/docs/user-guide/rule-engine-2-0/nodes/action/rpc-call-reply/"
)
public class TbSendRPCReplyNode implements TbNode {

    private TbSendRpcReplyNodeConfiguration config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        config = TbNodeUtils.convert(configuration, TbSendRpcReplyNodeConfiguration.class);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        String serviceIdStr = msg.getMetaData().getValue(config.getServiceIdMetaDataAttribute());
        String sessionIdStr = msg.getMetaData().getValue(config.getSessionIdMetaDataAttribute());
        String requestIdStr = msg.getMetaData().getValue(config.getRequestIdMetaDataAttribute());
        if (msg.getOriginator().getEntityType() != EntityType.DEVICE) {
            ctx.tellFailure(msg, new RuntimeException("Message originator is not a device entity!"));
        } else if (StringUtils.isEmpty(requestIdStr)) {
            ctx.tellFailure(msg, new RuntimeException("Request id is not present in the metadata!"));
        } else if (StringUtils.isEmpty(serviceIdStr)) {
            ctx.tellFailure(msg, new RuntimeException("Service id is not present in the metadata!"));
        } else if (StringUtils.isEmpty(sessionIdStr)) {
            ctx.tellFailure(msg, new RuntimeException("Session id is not present in the metadata!"));
        } else if (StringUtils.isEmpty(msg.getData())) {
            ctx.tellFailure(msg, new RuntimeException("Request body is empty!"));
        } else {
            if (StringUtils.isNotBlank(msg.getMetaData().getValue(DataConstants.EDGE_ID))) {
                saveRpcResponseToEdgeQueue(ctx, msg, serviceIdStr, sessionIdStr, requestIdStr);
            } else {
                ctx.getRpcService().sendRpcReplyToDevice(serviceIdStr, UUID.fromString(sessionIdStr), Integer.parseInt(requestIdStr), msg.getData());
                ctx.tellSuccess(msg);
            }
        }
    }

    private void saveRpcResponseToEdgeQueue(TbContext ctx, TbMsg msg, String serviceIdStr, String sessionIdStr, String requestIdStr) {
        EdgeId edgeId;
        DeviceId deviceId;
        try {
            edgeId = new EdgeId(UUID.fromString(msg.getMetaData().getValue(DataConstants.EDGE_ID)));
            deviceId = new DeviceId(UUID.fromString(msg.getMetaData().getValue(DataConstants.DEVICE_ID)));
        } catch (Exception e) {
            String errMsg = String.format("[%s] Failed to parse edgeId or deviceId from metadata %s!", ctx.getTenantId(), msg.getMetaData());
            ctx.tellFailure(msg, new RuntimeException(errMsg));
            return;
        }

        ObjectNode body = JacksonUtil.newObjectNode();
        body.put("serviceId", serviceIdStr);
        body.put("sessionId", sessionIdStr);
        body.put("requestId", requestIdStr);
        body.put("response", msg.getData());
        EdgeEvent edgeEvent = EdgeUtils.constructEdgeEvent(ctx.getTenantId(), edgeId, EdgeEventType.DEVICE,
                EdgeEventActionType.RPC_CALL, deviceId, JacksonUtil.valueToTree(body));
        ListenableFuture<Void> future = ctx.getEdgeEventService().saveAsync(edgeEvent);
        Futures.addCallback(future, new FutureCallback<>() {
            @Override
            public void onSuccess(Void result) {
                ctx.tellSuccess(msg);
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                ctx.tellFailure(msg, t);
            }
        }, ctx.getDbCallbackExecutor());
    }

}
