/**
 * Copyright © 2016-2018 The Thingsboard Authors
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

import com.fasterxml.jackson.core.JsonProcessingException;
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
import org.thingsboard.server.extensions.api.exception.ToErrorResponseEntity;
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
                    final TenantId tenantId = ctx.getSecurityCtx().orElseThrow(() -> new IllegalStateException("Security context is empty!")).getTenantId();
                    JsonNode rpcRequestBody = jsonMapper.readTree(request.getRequestBody());

                    RpcRequest cmd = new RpcRequest(rpcRequestBody.get("method").asText(),
                            jsonMapper.writeValueAsString(rpcRequestBody.get("params")));
                    if (rpcRequestBody.has("timeout")) {
                        cmd.setTimeout(rpcRequestBody.get("timeout").asLong());
                    }

                    boolean oneWay = DataConstants.ONEWAY.equals(method);

                    DeviceId deviceId = DeviceId.fromString(pathParams[1]);
                    valid = handleDeviceRPCRequest(ctx, msg, tenantId, deviceId, cmd, oneWay);
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

    private boolean handleDeviceRPCRequest(PluginContext ctx, final PluginRestMsg msg, TenantId tenantId, DeviceId deviceId, RpcRequest cmd, boolean oneWay) throws JsonProcessingException {
        long timeout = System.currentTimeMillis() + (cmd.getTimeout() != null ? cmd.getTimeout() : defaultTimeout);
        ToDeviceRpcRequestBody body = new ToDeviceRpcRequestBody(cmd.getMethodName(), cmd.getRequestData());
        ctx.checkAccess(deviceId, new PluginCallback<Void>() {
            @Override
            public void onSuccess(PluginContext ctx, Void value) {
                ToDeviceRpcRequest rpcRequest = new ToDeviceRpcRequest(UUID.randomUUID(),
                        msg.getSecurityCtx(),
                        tenantId,
                        deviceId,
                        oneWay,
                        timeout,
                        body
                );
                rpcManager.process(ctx, new LocalRequestMetaData(rpcRequest, msg.getResponseHolder()));
            }

            @Override
            public void onFailure(PluginContext ctx, Exception e) {
                ResponseEntity response;
                if (e instanceof ToErrorResponseEntity) {
                    response = ((ToErrorResponseEntity)e).toErrorResponseEntity();
                } else {
                    response = new ResponseEntity(HttpStatus.UNAUTHORIZED);
                }
                ctx.logRpcRequest(msg.getSecurityCtx(), deviceId, body, oneWay, Optional.empty(), e);
                msg.getResponseHolder().setResult(response);
            }
        });
        return true;
    }

    public void reply(PluginContext ctx, ToDeviceRpcRequest rpcRequest, DeferredResult<ResponseEntity> responseWriter, FromDeviceRpcResponse response) {
        Optional<RpcError> rpcError = response.getError();
        if (rpcError.isPresent()) {
            ctx.logRpcRequest(rpcRequest.getSecurityCtx(), rpcRequest.getDeviceId(), rpcRequest.getBody(), rpcRequest.isOneway(), rpcError, null);
            RpcError error = rpcError.get();
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
            Optional<String> responseData = response.getResponse();
            if (responseData.isPresent() && !StringUtils.isEmpty(responseData.get())) {
                String data = responseData.get();
                try {
                    ctx.logRpcRequest(rpcRequest.getSecurityCtx(), rpcRequest.getDeviceId(), rpcRequest.getBody(), rpcRequest.isOneway(), rpcError, null);
                    responseWriter.setResult(new ResponseEntity<>(jsonMapper.readTree(data), HttpStatus.OK));
                } catch (IOException e) {
                    log.debug("Failed to decode device response: {}", data, e);
                    ctx.logRpcRequest(rpcRequest.getSecurityCtx(), rpcRequest.getDeviceId(), rpcRequest.getBody(), rpcRequest.isOneway(), rpcError, e);
                    responseWriter.setResult(new ResponseEntity<>(HttpStatus.NOT_ACCEPTABLE));
                }
            } else {
                ctx.logRpcRequest(rpcRequest.getSecurityCtx(), rpcRequest.getDeviceId(), rpcRequest.getBody(), rpcRequest.isOneway(), rpcError, null);
                responseWriter.setResult(new ResponseEntity<>(HttpStatus.OK));
            }
        }
    }
}