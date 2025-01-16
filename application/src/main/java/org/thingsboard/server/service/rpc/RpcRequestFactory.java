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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.rpc.ToDeviceRpcRequestBody;
import org.thingsboard.server.common.msg.rpc.ToDeviceRpcRequest;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.ws.WebSocketSessionRef;
import org.thingsboard.server.service.ws.telemetry.cmd.v1.RpcCmd;

import java.util.UUID;

@Service
@Slf4j
public class RpcRequestFactory {

    @Value("${server.rest.server_side_rpc.min_timeout:5000}")
    private long minTimeout;

    @Value("${server.rest.server_side_rpc.default_timeout:10000}")
    private long defaultTimeout;

    public ToDeviceRpcRequest buildRpcRequestFromWs(WebSocketSessionRef sessionRef, RpcCmd cmd) {
        SecurityUser currentUser = sessionRef.getSecurityCtx();
        TenantId tenantId = currentUser.getTenantId();
        DeviceId deviceId = DeviceId.fromString(cmd.getEntityId());

        long expTime = calcExpirationTime(cmd.getExpTime(), cmd.getTimeout());

        ToDeviceRpcRequestBody body = buildRequestBody(JacksonUtil.toJsonNode(cmd.getRpcJson()));

        return ToDeviceRpcRequest.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .deviceId(deviceId)
                .oneway(cmd.isOneway())
                .expirationTime(expTime)
                .body(body)
                .persisted(cmd.isPersisted())
                .retries(Math.max(cmd.getRetries(), 0))
                .additionalInfo(null)
                .build();
    }

    public ToDeviceRpcRequest buildRpcRequestFromHttp(TenantId tenantId, DeviceId deviceId, String requestBody, boolean oneWay) {
        JsonNode rpcRequestBody = JacksonUtil.toJsonNode(requestBody);
        ToDeviceRpcRequestBody body = buildRequestBody(rpcRequestBody);

        long timeout = rpcRequestBody.has(DataConstants.TIMEOUT)
                ? rpcRequestBody.get(DataConstants.TIMEOUT).asLong()
                : defaultTimeout;
        long expTime = rpcRequestBody.has(DataConstants.EXPIRATION_TIME)
                ? rpcRequestBody.get(DataConstants.EXPIRATION_TIME).asLong()
                : System.currentTimeMillis() + Math.max(minTimeout, timeout);

        UUID rpcRequestUUID = rpcRequestBody.has("requestUUID")
                ? UUID.fromString(rpcRequestBody.get("requestUUID").asText())
                : UUID.randomUUID();

        boolean persisted = rpcRequestBody.has(DataConstants.PERSISTENT)
                && rpcRequestBody.get(DataConstants.PERSISTENT).asBoolean();
        String additionalInfo = JacksonUtil.toString(rpcRequestBody.get(DataConstants.ADDITIONAL_INFO));
        Integer retries = rpcRequestBody.has(DataConstants.RETRIES)
                ? rpcRequestBody.get(DataConstants.RETRIES).asInt()
                : null;

        return ToDeviceRpcRequest.builder()
                .id(rpcRequestUUID)
                .tenantId(tenantId)
                .deviceId(deviceId)
                .oneway(oneWay)
                .expirationTime(expTime)
                .body(body)
                .persisted(persisted)
                .retries(retries)
                .additionalInfo(additionalInfo)
                .build();
    }

    private long calcExpirationTime(long providedExpTime, long providedTimeout) {
        long actualTimeout = (providedTimeout > 0) ? providedTimeout : defaultTimeout;
        return (providedExpTime > 0)
                ? providedExpTime
                : System.currentTimeMillis() + Math.max(minTimeout, actualTimeout);
    }

    private ToDeviceRpcRequestBody buildRequestBody(JsonNode rpcRequestBody) {
        return new ToDeviceRpcRequestBody(
                rpcRequestBody.get("method").asText(),
                JacksonUtil.toString(rpcRequestBody.get("params"))
        );
    }
}
