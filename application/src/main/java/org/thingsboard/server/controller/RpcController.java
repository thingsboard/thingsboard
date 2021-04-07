/**
 * Copyright © 2016-2021 The Thingsboard Authors
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.FutureCallback;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.rule.engine.api.RpcError;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.common.data.rpc.RpcRequest;
import org.thingsboard.server.common.data.rpc.ToDeviceRpcRequestBody;
import org.thingsboard.server.common.msg.rpc.ToDeviceRpcRequest;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.rpc.FromDeviceRpcResponse;
import org.thingsboard.server.service.rpc.LocalRequestMetaData;
import org.thingsboard.server.service.rpc.TbCoreDeviceRpcService;
import org.thingsboard.server.service.security.AccessValidator;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.telemetry.exception.ToErrorResponseEntity;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

/**
 * Created by ashvayka on 22.03.18.
 */
@RestController
@TbCoreComponent
@RequestMapping(TbUrlConstants.RPC_URL_PREFIX)
@Slf4j
public class RpcController extends BaseController {

    protected final ObjectMapper jsonMapper = new ObjectMapper();

    @Autowired
    private TbCoreDeviceRpcService deviceRpcService;

    @Autowired
    private AccessValidator accessValidator;

    @Value("${server.rest.server_side_rpc.min_timeout:5000}")
    private long minTimeout;

    @Value("${server.rest.server_side_rpc.default_timeout:10000}")
    private long defaultTimeout;

    @PreAuthorize("hasAnyAuthority('ROOT', 'SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/oneway/{deviceId}", method = RequestMethod.POST)
    @ResponseBody
    public DeferredResult<ResponseEntity> handleOneWayDeviceRPCRequest(@PathVariable("deviceId") String deviceIdStr, @RequestBody String requestBody) throws ThingsboardException {
        return handleDeviceRPCRequest(true, new DeviceId(UUID.fromString(deviceIdStr)), requestBody);
    }

    @PreAuthorize("hasAnyAuthority('ROOT', 'SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/twoway/{deviceId}", method = RequestMethod.POST)
    @ResponseBody
    public DeferredResult<ResponseEntity> handleTwoWayDeviceRPCRequest(@PathVariable("deviceId") String deviceIdStr, @RequestBody String requestBody) throws ThingsboardException {
        return handleDeviceRPCRequest(false, new DeviceId(UUID.fromString(deviceIdStr)), requestBody);
    }

    private DeferredResult<ResponseEntity> handleDeviceRPCRequest(boolean oneWay, DeviceId deviceId, String requestBody) throws ThingsboardException {
        try {
            JsonNode rpcRequestBody = jsonMapper.readTree(requestBody);
            RpcRequest cmd = new RpcRequest(rpcRequestBody.get("method").asText(),
                    jsonMapper.writeValueAsString(rpcRequestBody.get("params")));

            if (rpcRequestBody.has("timeout")) {
                cmd.setTimeout(rpcRequestBody.get("timeout").asLong());
            }
            SecurityUser currentUser = getCurrentUser();
            TenantId tenantId = currentUser.getTenantId();
            final DeferredResult<ResponseEntity> response = new DeferredResult<>();
            long timeout = cmd.getTimeout() != null ? cmd.getTimeout() : defaultTimeout;
            long expTime = System.currentTimeMillis() + Math.max(minTimeout, timeout);
            ToDeviceRpcRequestBody body = new ToDeviceRpcRequestBody(cmd.getMethodName(), cmd.getRequestData());
            accessValidator.validate(currentUser, Operation.RPC_CALL, deviceId, new HttpValidationCallback(response, new FutureCallback<DeferredResult<ResponseEntity>>() {
                @Override
                public void onSuccess(@Nullable DeferredResult<ResponseEntity> result) {
                    ToDeviceRpcRequest rpcRequest = new ToDeviceRpcRequest(UUID.randomUUID(),
                            tenantId,
                            deviceId,
                            oneWay,
                            expTime,
                            body
                    );
                    deviceRpcService.processRestApiRpcRequest(rpcRequest, fromDeviceRpcResponse -> reply(new LocalRequestMetaData(rpcRequest, currentUser, result), fromDeviceRpcResponse));
                }

                @Override
                public void onFailure(Throwable e) {
                    ResponseEntity entity;
                    if (e instanceof ToErrorResponseEntity) {
                        entity = ((ToErrorResponseEntity) e).toErrorResponseEntity();
                    } else {
                        entity = new ResponseEntity(HttpStatus.UNAUTHORIZED);
                    }
                    logRpcCall(currentUser, deviceId, body, oneWay, Optional.empty(), e);
                    response.setResult(entity);
                }
            }));
            return response;
        } catch (IOException ioe) {
            throw new ThingsboardException("Invalid request body", ioe, ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
    }

    public void reply(LocalRequestMetaData rpcRequest, FromDeviceRpcResponse response) {
        Optional<RpcError> rpcError = response.getError();
        DeferredResult<ResponseEntity> responseWriter = rpcRequest.getResponseWriter();
        if (rpcError.isPresent()) {
            logRpcCall(rpcRequest, rpcError, null);
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
                    logRpcCall(rpcRequest, rpcError, null);
                    responseWriter.setResult(new ResponseEntity<>(jsonMapper.readTree(data), HttpStatus.OK));
                } catch (IOException e) {
                    log.debug("Failed to decode device response: {}", data, e);
                    logRpcCall(rpcRequest, rpcError, e);
                    responseWriter.setResult(new ResponseEntity<>(HttpStatus.NOT_ACCEPTABLE));
                }
            } else {
                logRpcCall(rpcRequest, rpcError, null);
                responseWriter.setResult(new ResponseEntity<>(HttpStatus.OK));
            }
        }
    }

    private void logRpcCall(LocalRequestMetaData rpcRequest, Optional<RpcError> rpcError, Throwable e) {
        logRpcCall(rpcRequest.getUser(), rpcRequest.getRequest().getDeviceId(), rpcRequest.getRequest().getBody(), rpcRequest.getRequest().isOneway(), rpcError, null);
    }


    private void logRpcCall(SecurityUser user, EntityId entityId, ToDeviceRpcRequestBody body, boolean oneWay, Optional<RpcError> rpcError, Throwable e) {
        String rpcErrorStr = "";
        if (rpcError.isPresent()) {
            rpcErrorStr = "RPC Error: " + rpcError.get().name();
        }
        String method = body.getMethod();
        String params = body.getParams();

        auditLogService.logEntityAction(
                user.getTenantId(),
                user.getCustomerId(),
                user.getId(),
                user.getName(),
                (UUIDBased & EntityId) entityId,
                null,
                ActionType.RPC_CALL,
                BaseController.toException(e),
                rpcErrorStr,
                oneWay,
                method,
                params);
    }


}
