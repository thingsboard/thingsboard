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
package org.thingsboard.server.extensions.core.plugin.rpc.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.extensions.api.plugins.PluginApiCallSecurityContext;
import org.thingsboard.server.extensions.api.plugins.PluginCallback;
import org.thingsboard.server.extensions.api.plugins.PluginContext;
import org.thingsboard.server.extensions.api.plugins.handlers.DefaultRestMsgHandler;
import org.thingsboard.server.extensions.api.plugins.msg.FromDeviceRpcResponse;
import org.thingsboard.server.extensions.api.plugins.msg.RpcError;
import org.thingsboard.server.extensions.api.plugins.msg.ToDeviceRpcRequest;
import org.thingsboard.server.extensions.api.plugins.msg.ToDeviceRpcRequestBody;
import org.thingsboard.server.extensions.api.plugins.rest.PluginRestMsg;
import org.thingsboard.server.extensions.api.plugins.rest.RestRequest;
import org.thingsboard.server.extensions.core.plugin.rpc.LocalRequestMetaData;
import org.thingsboard.server.extensions.core.plugin.rpc.RpcManager;
import org.thingsboard.server.extensions.core.plugin.rpc.cmd.RpcRequest;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

/**
 * @author Andrew Shvayka
 */
@Slf4j
@RequiredArgsConstructor
public class RpcRestMsgHandler extends DefaultRestMsgHandler {

    private final RpcManager rpcManager;
    @Setter
    private long defaultTimeout;

    @Override
    public void handleHttpPostRequest(PluginContext ctx, PluginRestMsg msg) throws ServletException {
        boolean valid = false;
        RestRequest request = msg.getRequest();
        try {
            String[] pathParams = request.getPathParams();
            if (pathParams.length == 2) {
                String method = pathParams[0].toUpperCase();
                if (DataConstants.ONEWAY.equals(method) || DataConstants.TWOWAY.equals(method)) {
                    DeviceId deviceId = DeviceId.fromString(pathParams[1]);
                    JsonNode rpcRequestBody = jsonMapper.readTree(request.getRequestBody());

                    RpcRequest cmd = new RpcRequest(rpcRequestBody.get("method").asText(),
                            jsonMapper.writeValueAsString(rpcRequestBody.get("params")));
                    if (rpcRequestBody.has("timeout")) {
                        cmd.setTimeout(rpcRequestBody.get("timeout").asLong());
                    }

                    final TenantId tenantId = ctx.getSecurityCtx().orElseThrow(() -> new IllegalStateException("Security context is empty!")).getTenantId();

                    ctx.checkAccess(deviceId, new PluginCallback<Void>() {
                        @Override
                        public void onSuccess(PluginContext ctx, Void value) {
                            long timeout = cmd.getTimeout() != null ? cmd.getTimeout() : defaultTimeout;
                            ToDeviceRpcRequestBody body = new ToDeviceRpcRequestBody(cmd.getMethodName(), cmd.getRequestData());
                            ToDeviceRpcRequest rpcRequest = new ToDeviceRpcRequest(UUID.randomUUID(),
                                    tenantId,
                                    deviceId,
                                    DataConstants.ONEWAY.equals(method),
                                    System.currentTimeMillis() + timeout,
                                    body
                            );
                            rpcManager.process(ctx, new LocalRequestMetaData(rpcRequest, msg.getResponseHolder()));
                        }

                        @Override
                        public void onFailure(PluginContext ctx, Exception e) {
                            msg.getResponseHolder().setResult(new ResponseEntity<>(HttpStatus.UNAUTHORIZED));
                        }
                    });
                    valid = true;
                }
            }
        } catch (IOException e) {
            log.debug("Failed to process POST request due to IO exception", e);
        } catch (RuntimeException e) {
            log.debug("Failed to process POST request due to Runtime exception", e);
        }
        if (!valid) {
            msg.getResponseHolder().setResult(new ResponseEntity<>(HttpStatus.BAD_REQUEST));
        }
    }

    public void reply(PluginContext ctx, DeferredResult<ResponseEntity> responseWriter, FromDeviceRpcResponse response) {
        if (response.getError().isPresent()) {
            RpcError error = response.getError().get();
            switch (error) {
                case TIMEOUT:
                    responseWriter.setResult(new ResponseEntity<>(HttpStatus.REQUEST_TIMEOUT));
                    break;
                case NO_ACTIVE_CONNECTION:
                    responseWriter.setResult(new ResponseEntity<>(HttpStatus.CONFLICT));
                    break;
                default:
                    responseWriter.setResult(new ResponseEntity<>(HttpStatus.REQUEST_TIMEOUT));
                    break;
            }
        } else {
            if (response.getResponse().isPresent() && !StringUtils.isEmpty(response.getResponse().get())) {
                String data = response.getResponse().get();
                try {
                    responseWriter.setResult(new ResponseEntity<>(jsonMapper.readTree(data), HttpStatus.OK));
                } catch (IOException e) {
                    log.debug("Failed to decode device response: {}", data, e);
                    responseWriter.setResult(new ResponseEntity<>(HttpStatus.NOT_ACCEPTABLE));
                }
            } else {
                responseWriter.setResult(new ResponseEntity<>(HttpStatus.OK));
            }
        }
    }
}