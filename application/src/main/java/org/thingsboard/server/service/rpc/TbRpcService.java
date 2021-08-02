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
package org.thingsboard.server.service.rpc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.FutureCallback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.RpcId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.rpc.Rpc;
import org.thingsboard.server.common.data.rpc.RpcStatus;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.rpc.ToDeviceRpcRequest;
import org.thingsboard.server.dao.rpc.RpcService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.queue.TbClusterService;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import javax.annotation.Nullable;
import java.util.Collections;

@TbCoreComponent
@Service
@RequiredArgsConstructor
@Slf4j
public class TbRpcService {
    private final RpcService rpcService;
    private final TbClusterService tbClusterService;
    private final TelemetrySubscriptionService tsSubService;

    public Rpc save(TenantId tenantId, Rpc rpc, boolean persistedRpcTelemetry) {
        Rpc saved = rpcService.save(rpc);
        pushRpcMsgToRuleEngine(tenantId, saved);
        if (persistedRpcTelemetry) {
            saveRpcToTelemetry(tenantId, saved);
        }
        return saved;
    }

    public void save(TenantId tenantId, RpcId rpcId, RpcStatus newStatus, JsonNode response, boolean persistedRpcTelemetry) {
        Rpc foundRpc = findRpcById(tenantId, rpcId);
        if (foundRpc != null) {
            foundRpc.setStatus(newStatus);
            if (response != null) {
                foundRpc.setResponse(response);
            }
            Rpc saved = rpcService.save(foundRpc);
            pushRpcMsgToRuleEngine(tenantId, saved);
            if (persistedRpcTelemetry) {
                saveRpcToTelemetry(tenantId, saved);
            }
        } else {
            log.warn("[{}] Failed to update RPC status because RPC was already deleted", rpcId);
        }
    }

    public void save(TenantId tenantId, RpcId rpcId, ToDeviceRpcRequest newRequest) {
        Rpc foundRpc = findRpcById(tenantId, rpcId);
        if (foundRpc != null) {
            foundRpc.setStatus(RpcStatus.QUEUED);
            foundRpc.setExpirationTime(System.currentTimeMillis() + newRequest.getTimeout());
            foundRpc.setResponse(null);
            foundRpc.setRequest(JacksonUtil.valueToTree(newRequest));
            Rpc saved = rpcService.save(foundRpc);
            pushRpcMsgToRuleEngine(tenantId, saved);
            if (newRequest.isPersistedRpcTelemetry()) {
                saveRpcToTelemetry(tenantId, saved);
            }
        } else {
            log.warn("[{}] Failed to update RPC status because RPC was already deleted", rpcId);
        }
    }

    private void saveRpcToTelemetry(TenantId tenantId, Rpc savedRpc) {
        // TODO: 7/30/21 consider to add prefix for telemetry key: rpc_
        ObjectNode rpcTelemetryData = JacksonUtil.newObjectNode();
        rpcTelemetryData.put("status", savedRpc.getStatus().name());
        rpcTelemetryData.set("request", savedRpc.getRequest());
        rpcTelemetryData.set("response", savedRpc.getResponse());
        RpcId rpcId = savedRpc.getId();
        DeviceId deviceId = savedRpc.getDeviceId();
        JsonDataEntry kv = new JsonDataEntry(rpcId.toString(), JacksonUtil.toString(rpcTelemetryData));
        tsSubService.saveAndNotify(tenantId, deviceId, Collections.singletonList(new BasicTsKvEntry(savedRpc.getCreatedTime(), kv)), new FutureCallback<>() {
            @Override
            public void onSuccess(@Nullable Void result) {
                log.trace("[{}] Successfully saved rpc telemetry [{}] to device [{}]", tenantId, rpcId, deviceId);
            }

            @Override
            public void onFailure(Throwable t) {
                log.error("[{}] Failed to save rpc telemetry [{}] to device [{}]", tenantId, rpcId, deviceId, t);
            }
        });
    }

    private void pushRpcMsgToRuleEngine(TenantId tenantId, Rpc rpc) {
        TbMsg msg = TbMsg.newMsg("RPC_" + rpc.getStatus().name(), rpc.getDeviceId(), TbMsgMetaData.EMPTY, JacksonUtil.toString(rpc));
        tbClusterService.pushMsgToRuleEngine(tenantId, rpc.getDeviceId(), msg, null);
    }

    public PageData<Rpc> findAllByDeviceIdAndStatus(TenantId tenantId, DeviceId deviceId, RpcStatus rpcStatus, PageLink pageLink) {
        return rpcService.findAllByDeviceIdAndStatus(tenantId, deviceId, rpcStatus, pageLink);
    }

    private Rpc findRpcById(TenantId tenantId, RpcId rpcId) {
        return rpcService.findById(tenantId, rpcId);
    }

}
