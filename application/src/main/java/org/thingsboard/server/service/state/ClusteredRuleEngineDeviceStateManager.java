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
package org.thingsboard.server.service.state;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.rule.engine.api.RuleEngineDeviceStateManager;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.common.SimpleTbQueueCallback;
import org.thingsboard.server.queue.util.TbRuleEngineComponent;

@Slf4j
@Service
@TbRuleEngineComponent
@RequiredArgsConstructor
public class ClusteredRuleEngineDeviceStateManager implements RuleEngineDeviceStateManager {

    private final TbClusterService clusterService;

    @Override
    public void onDeviceConnect(TenantId tenantId, DeviceId deviceId, long connectTime, TbCallback callback) {
        var tenantUuid = tenantId.getId();
        var deviceUuid = deviceId.getId();
        var deviceConnectMsg = TransportProtos.DeviceConnectProto.newBuilder()
                .setTenantIdMSB(tenantUuid.getMostSignificantBits())
                .setTenantIdLSB(tenantUuid.getLeastSignificantBits())
                .setDeviceIdMSB(deviceUuid.getMostSignificantBits())
                .setDeviceIdLSB(deviceUuid.getLeastSignificantBits())
                .setLastConnectTime(connectTime)
                .build();
        var toCoreMsg = TransportProtos.ToCoreMsg.newBuilder()
                .setDeviceConnectMsg(deviceConnectMsg)
                .build();
        log.trace("[{}][{}] Sending device connect message to core. Connect time: [{}].", tenantUuid, deviceUuid, connectTime);
        clusterService.pushMsgToCore(tenantId, deviceId, toCoreMsg, new SimpleTbQueueCallback(__ -> callback.onSuccess(), callback::onFailure));
    }

    @Override
    public void onDeviceActivity(TenantId tenantId, DeviceId deviceId, long activityTime, TbCallback callback) {
        var tenantUuid = tenantId.getId();
        var deviceUuid = deviceId.getId();
        var deviceActivityMsg = TransportProtos.DeviceActivityProto.newBuilder()
                .setTenantIdMSB(tenantUuid.getMostSignificantBits())
                .setTenantIdLSB(tenantUuid.getLeastSignificantBits())
                .setDeviceIdMSB(deviceUuid.getMostSignificantBits())
                .setDeviceIdLSB(deviceUuid.getLeastSignificantBits())
                .setLastActivityTime(activityTime)
                .build();
        var toCoreMsg = TransportProtos.ToCoreMsg.newBuilder()
                .setDeviceActivityMsg(deviceActivityMsg)
                .build();
        log.trace("[{}][{}] Sending device activity message to core. Activity time: [{}].", tenantUuid, deviceUuid, activityTime);
        clusterService.pushMsgToCore(tenantId, deviceId, toCoreMsg, new SimpleTbQueueCallback(__ -> callback.onSuccess(), callback::onFailure));
    }

    @Override
    public void onDeviceDisconnect(TenantId tenantId, DeviceId deviceId, long disconnectTime, TbCallback callback) {
        var tenantUuid = tenantId.getId();
        var deviceUuid = deviceId.getId();
        var deviceDisconnectMsg = TransportProtos.DeviceDisconnectProto.newBuilder()
                .setTenantIdMSB(tenantUuid.getMostSignificantBits())
                .setTenantIdLSB(tenantUuid.getLeastSignificantBits())
                .setDeviceIdMSB(deviceUuid.getMostSignificantBits())
                .setDeviceIdLSB(deviceUuid.getLeastSignificantBits())
                .setLastDisconnectTime(disconnectTime)
                .build();
        var toCoreMsg = TransportProtos.ToCoreMsg.newBuilder()
                .setDeviceDisconnectMsg(deviceDisconnectMsg)
                .build();
        log.trace("[{}][{}] Sending device disconnect message to core. Disconnect time: [{}].", tenantUuid, deviceUuid, disconnectTime);
        clusterService.pushMsgToCore(tenantId, deviceId, toCoreMsg, new SimpleTbQueueCallback(__ -> callback.onSuccess(), callback::onFailure));
    }

    @Override
    public void onDeviceInactivity(TenantId tenantId, DeviceId deviceId, long inactivityTime, TbCallback callback) {
        var tenantUuid = tenantId.getId();
        var deviceUuid = deviceId.getId();
        var deviceInactivityMsg = TransportProtos.DeviceInactivityProto.newBuilder()
                .setTenantIdMSB(tenantUuid.getMostSignificantBits())
                .setTenantIdLSB(tenantUuid.getLeastSignificantBits())
                .setDeviceIdMSB(deviceUuid.getMostSignificantBits())
                .setDeviceIdLSB(deviceUuid.getLeastSignificantBits())
                .setLastInactivityTime(inactivityTime)
                .build();
        var toCoreMsg = TransportProtos.ToCoreMsg.newBuilder()
                .setDeviceInactivityMsg(deviceInactivityMsg)
                .build();
        log.trace("[{}][{}] Sending device inactivity message to core. Inactivity time: [{}].", tenantUuid, deviceUuid, inactivityTime);
        clusterService.pushMsgToCore(tenantId, deviceId, toCoreMsg, new SimpleTbQueueCallback(__ -> callback.onSuccess(), callback::onFailure));
    }

}
