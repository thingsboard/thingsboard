/**
 * Copyright © 2016-2017 The Thingsboard Authors
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
package org.thingsboard.server.extensions.core.plugin.rpc;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.extensions.api.plugins.PluginContext;
import org.thingsboard.server.extensions.api.plugins.msg.*;
import org.thingsboard.server.extensions.core.plugin.rpc.handlers.RpcRestMsgHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author Andrew Shvayka
 */
@Slf4j
public class RpcManager {

    @Setter
    private RpcRestMsgHandler restHandler;

    private Map<UUID, LocalRequestMetaData> localRpcRequests = new HashMap<>();

    public void process(PluginContext ctx, LocalRequestMetaData requestMd) {
        ToDeviceRpcRequest request = requestMd.getRequest();
        log.trace("[{}] Processing local rpc call for device [{}]", request.getId(), request.getDeviceId());
        ctx.sendRpcRequest(request);
        localRpcRequests.put(request.getId(), requestMd);
        ctx.scheduleTimeoutMsg(new TimeoutUUIDMsg(request.getId(), request.getExpirationTime() - System.currentTimeMillis()));
    }

    public void process(PluginContext ctx, FromDeviceRpcResponse response) {
        UUID requestId = response.getId();
        LocalRequestMetaData md = localRpcRequests.remove(requestId);
        if (md != null) {
            log.trace("[{}] Processing local rpc response from device [{}]", requestId, md.getRequest().getDeviceId());
            restHandler.reply(ctx, md.getResponseWriter(), response);
        } else {
            log.trace("[{}] Unknown or stale rpc response received [{}]", requestId, response);
        }
    }

    public void process(PluginContext ctx, TimeoutMsg msg) {
        if (msg instanceof TimeoutUUIDMsg) {
            UUID requestId = ((TimeoutUUIDMsg) msg).getId();
            FromDeviceRpcResponse timeoutReponse = new FromDeviceRpcResponse(requestId, null, RpcError.TIMEOUT);
            LocalRequestMetaData md = localRpcRequests.remove(requestId);
            if (md != null) {
                log.trace("[{}] Processing rpc timeout for local device [{}]", requestId, md.getRequest().getDeviceId());
                restHandler.reply(ctx, md.getResponseWriter(), timeoutReponse);
            }
        }
    }
}
