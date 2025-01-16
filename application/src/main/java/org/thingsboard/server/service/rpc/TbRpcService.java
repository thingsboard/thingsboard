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
package org.thingsboard.server.service.rpc;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.FutureCallback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.*;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.rpc.Rpc;
import org.thingsboard.server.common.data.rpc.RpcError;
import org.thingsboard.server.common.data.rpc.RpcStatus;
import org.thingsboard.server.common.data.rpc.ToDeviceRpcRequestBody;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.rpc.FromDeviceRpcResponse;
import org.thingsboard.server.common.msg.rpc.ToDeviceRpcRequest;
import org.thingsboard.server.controller.BaseController;
import org.thingsboard.server.dao.audit.AuditLogService;
import org.thingsboard.server.dao.rpc.RpcService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.AccessValidator;
import org.thingsboard.server.service.security.ValidationResult;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.Operation;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@TbCoreComponent
@Service
@RequiredArgsConstructor
@Slf4j
public class TbRpcService {
    private final RpcService rpcService;
    private final TbClusterService tbClusterService;
    private final AuditLogService auditLogService;
    private final AccessValidator accessValidator;
    private final TbCoreDeviceRpcService deviceRpcService;

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
        TbMsg msg = TbMsg.newMsg(TbMsgType.valueOf("RPC_" + rpc.getStatus().name()), rpc.getDeviceId(), TbMsgMetaData.EMPTY, JacksonUtil.toString(rpc));
        tbClusterService.pushMsgToRuleEngine(tenantId, rpc.getDeviceId(), msg, null);
    }

    public Rpc findRpcById(TenantId tenantId, RpcId rpcId) {
        return rpcService.findById(tenantId, rpcId);
    }

    public PageData<Rpc> findAllByDeviceIdAndStatus(TenantId tenantId, DeviceId deviceId, RpcStatus rpcStatus, PageLink pageLink) {
        return rpcService.findAllByDeviceIdAndStatus(tenantId, deviceId, rpcStatus, pageLink);
    }

    public void sendRpcRequest(SecurityUser user,
                               DeviceId entityId,
                               ToDeviceRpcRequest rpcRequest,
                               RpcSuccessCallback onSuccess,
                               RpcFailureCallback onFailure) {

        accessValidator.validate(user, Operation.RPC_CALL, entityId, new FutureCallback<>() {

            @Override
            public void onSuccess(ValidationResult result) {
                deviceRpcService.processRestApiRpcRequest(rpcRequest, fromDeviceRpcResponse -> {
                    onSuccess.handleRpcSuccess(rpcRequest, fromDeviceRpcResponse);
                }, user);
            }

            @Override
            public void onFailure(Throwable throwable) {
                onFailure.handleRpcFailure(rpcRequest, throwable);
            }
        });
    }

    public void logRpcCall(SecurityUser user,
                           EntityId entityId,
                           ToDeviceRpcRequestBody body,
                           boolean oneWay,
                           Optional<RpcError> rpcError,
                           Throwable t) {

        String rpcErrorStr = rpcError.map(err -> "RPC Error: " + err.name()).orElse("");
        auditLogService.logEntityAction(
                user.getTenantId(),
                user.getCustomerId(),
                user.getId(),
                user.getName(),
                (UUIDBased & EntityId) entityId,
                null,
                ActionType.RPC_CALL,
                BaseController.toException(t),
                rpcErrorStr,
                oneWay,
                body.getMethod(),
                body.getParams()
        );
    }

    public void handleRpcResponse(
            FromDeviceRpcResponse response,
            Consumer<RpcError> onError,
            BiConsumer<String, Throwable> onSuccessWithData,
            Runnable onSuccessEmpty,
            Consumer<Throwable> onDecodeError
    ) {
        Optional<RpcError> rpcError = response.getError();
        if (rpcError.isPresent()) {
            onError.accept(rpcError.get());
        } else {
            Optional<String> responseData = response.getResponse();
            if (responseData.isPresent() && !responseData.get().isEmpty()) {
                String data = responseData.get();
                try {
                    onSuccessWithData.accept(data, null);
                } catch (IllegalArgumentException e) {
                    onDecodeError.accept(e);
                }
            } else {
                onSuccessEmpty.run();
            }
        }
    }

    @FunctionalInterface
    public interface RpcSuccessCallback {
        void handleRpcSuccess(ToDeviceRpcRequest rpcRequest, FromDeviceRpcResponse response);
    }

    @FunctionalInterface
    public interface RpcFailureCallback {
        void handleRpcFailure(ToDeviceRpcRequest rpcRequest, Throwable e);
    }
}


