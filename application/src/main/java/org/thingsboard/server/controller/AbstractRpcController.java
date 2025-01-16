/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.rpc.FromDeviceRpcResponse;
import org.thingsboard.server.common.msg.rpc.ToDeviceRpcRequest;
import org.thingsboard.server.exception.ToErrorResponseEntity;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.rpc.LocalRequestMetaData;
import org.thingsboard.server.service.rpc.RpcRequestFactory;
import org.thingsboard.server.service.rpc.TbRpcService;
import org.thingsboard.server.service.security.AccessValidator;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.Optional;

/**
 * Created by ashvayka on 22.03.18.
 */
@TbCoreComponent
@Slf4j
public abstract class AbstractRpcController extends BaseController {

    @Autowired
    protected AccessValidator accessValidator;

    @Autowired
    protected RpcRequestFactory rpcRequestFactory;

    @Autowired
    protected TbRpcService tbRpcService;

    protected DeferredResult<ResponseEntity> handleDeviceRPCRequest(boolean oneWay, DeviceId deviceId, String requestBody, HttpStatus timeoutStatus, HttpStatus noActiveConnectionStatus) throws ThingsboardException {
        SecurityUser currentUser = getCurrentUser();
        TenantId tenantId = currentUser.getTenantId();

        final DeferredResult<ResponseEntity> response = new DeferredResult<>();

        ToDeviceRpcRequest rpcRequest = rpcRequestFactory.buildRpcRequestFromHttp(tenantId, deviceId, requestBody, oneWay);

        tbRpcService.sendRpcRequest(
                currentUser,
                deviceId,
                rpcRequest,
                (request, fromDeviceRpcResponse) ->
                        reply(new LocalRequestMetaData(request, currentUser, response),
                        fromDeviceRpcResponse,
                        timeoutStatus,
                        noActiveConnectionStatus),
                (request, e) -> {
                        ResponseEntity<?> entity;
                        if (e instanceof ToErrorResponseEntity) {
                            entity = ((ToErrorResponseEntity) e).toErrorResponseEntity();
                        } else {
                            entity = new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
                        }
                        tbRpcService.logRpcCall(currentUser, deviceId, rpcRequest.getBody(), oneWay, Optional.empty(), e);
                        response.setResult(entity);
                }
            );

            return response;
    }

    public void reply(LocalRequestMetaData rpcRequest, FromDeviceRpcResponse response, HttpStatus timeoutStatus, HttpStatus noActiveConnectionStatus) {
        DeferredResult<ResponseEntity> responseWriter = rpcRequest.getResponseWriter();

        tbRpcService.handleRpcResponse(
                response,
                (rpcError) -> {
                    tbRpcService.logRpcCall(
                            rpcRequest.getUser(),
                            rpcRequest.getRequest().getDeviceId(),
                            rpcRequest.getRequest().getBody(),
                            rpcRequest.getRequest().isOneway(),
                            Optional.of(rpcError),
                            null);
                    switch (rpcError) {
                        case TIMEOUT ->
                                responseWriter.setResult(new ResponseEntity<>(timeoutStatus));
                        case NO_ACTIVE_CONNECTION ->
                                responseWriter.setResult(new ResponseEntity<>(noActiveConnectionStatus));
                        default ->
                                responseWriter.setResult(new ResponseEntity<>(timeoutStatus));
                    }
                },
                (data, unusedThrowable) -> {
                    tbRpcService.logRpcCall(
                            rpcRequest.getUser(),
                            rpcRequest.getRequest().getDeviceId(),
                            rpcRequest.getRequest().getBody(),
                            rpcRequest.getRequest().isOneway(),
                            Optional.empty(),
                            null
                    );
                    try {
                        responseWriter.setResult(new ResponseEntity<>(
                                JacksonUtil.toJsonNode(data),
                                HttpStatus.OK
                        ));
                    } catch (IllegalArgumentException e) {
                        log.debug("Failed to decode device response: {}", data, e);
                        tbRpcService.logRpcCall(
                                rpcRequest.getUser(),
                                rpcRequest.getRequest().getDeviceId(),
                                rpcRequest.getRequest().getBody(),
                                rpcRequest.getRequest().isOneway(),
                                Optional.empty(),
                                e
                        );
                        responseWriter.setResult(new ResponseEntity<>(HttpStatus.NOT_ACCEPTABLE));
                    }
                },
                () -> {
                    tbRpcService.logRpcCall(
                            rpcRequest.getUser(),
                            rpcRequest.getRequest().getDeviceId(),
                            rpcRequest.getRequest().getBody(),
                            rpcRequest.getRequest().isOneway(),
                            Optional.empty(),
                            null
                    );
                    responseWriter.setResult(new ResponseEntity<>(HttpStatus.OK));
                },
                (decodeEx) -> {
                    log.debug("Failed to decode device response: {}", decodeEx.getMessage());
                    tbRpcService.logRpcCall(
                            rpcRequest.getUser(),
                            rpcRequest.getRequest().getDeviceId(),
                            rpcRequest.getRequest().getBody(),
                            rpcRequest.getRequest().isOneway(),
                            Optional.empty(),
                            decodeEx
                    );
                    responseWriter.setResult(new ResponseEntity<>(HttpStatus.NOT_ACCEPTABLE));
                }
        );
    }
}
