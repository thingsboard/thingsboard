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
package org.thingsboard.server.service.state;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.rule.engine.api.DeviceStateManager;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.common.SimpleTbQueueCallback;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultDeviceStateManager implements DeviceStateManager {

    private final TbServiceInfoProvider serviceInfoProvider;
    private final PartitionService partitionService;

    private final Optional<DeviceStateService> deviceStateService;
    private final TbClusterService clusterService;

    @Override
    public void onDeviceConnect(TenantId tenantId, DeviceId deviceId, long connectTime, TbCallback callback) {
        forwardToDeviceStateService(tenantId, deviceId,
                deviceStateService -> {
                    log.debug("[{}][{}] Forwarding device connect event to local service. Connect time: [{}].", tenantId.getId(), deviceId.getId(), connectTime);
                    deviceStateService.onDeviceConnect(tenantId, deviceId, connectTime);
                },
                () -> {
                    log.debug("[{}][{}] Sending device connect message to core. Connect time: [{}].", tenantId.getId(), deviceId.getId(), connectTime);
                    var deviceConnectMsg = TransportProtos.DeviceConnectProto.newBuilder()
                            .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                            .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                            .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                            .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                            .setLastConnectTime(connectTime)
                            .build();
                    return TransportProtos.ToCoreMsg.newBuilder()
                            .setDeviceConnectMsg(deviceConnectMsg)
                            .build();
                }, callback);
    }

    @Override
    public void onDeviceActivity(TenantId tenantId, DeviceId deviceId, long activityTime, TbCallback callback) {
        forwardToDeviceStateService(tenantId, deviceId,
                deviceStateService -> {
                    log.debug("[{}][{}] Forwarding device activity event to local service. Activity time: [{}].", tenantId.getId(), deviceId.getId(), activityTime);
                    deviceStateService.onDeviceActivity(tenantId, deviceId, activityTime);
                },
                () -> {
                    log.debug("[{}][{}] Sending device activity message to core. Activity time: [{}].", tenantId.getId(), deviceId.getId(), activityTime);
                    var deviceActivityMsg = TransportProtos.DeviceActivityProto.newBuilder()
                            .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                            .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                            .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                            .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                            .setLastActivityTime(activityTime)
                            .build();
                    return TransportProtos.ToCoreMsg.newBuilder()
                            .setDeviceActivityMsg(deviceActivityMsg)
                            .build();
                }, callback);
    }

    @Override
    public void onDeviceDisconnect(TenantId tenantId, DeviceId deviceId, long disconnectTime, TbCallback callback) {
        forwardToDeviceStateService(tenantId, deviceId,
                deviceStateService -> {
                    log.debug("[{}][{}] Forwarding device disconnect event to local service. Disconnect time: [{}].", tenantId.getId(), deviceId.getId(), disconnectTime);
                    deviceStateService.onDeviceDisconnect(tenantId, deviceId, disconnectTime);
                },
                () -> {
                    log.debug("[{}][{}] Sending device disconnect message to core. Disconnect time: [{}].", tenantId.getId(), deviceId.getId(), disconnectTime);
                    var deviceDisconnectMsg = TransportProtos.DeviceDisconnectProto.newBuilder()
                            .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                            .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                            .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                            .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                            .setLastDisconnectTime(disconnectTime)
                            .build();
                    return TransportProtos.ToCoreMsg.newBuilder()
                            .setDeviceDisconnectMsg(deviceDisconnectMsg)
                            .build();
                }, callback);
    }

    @Override
    public void onDeviceInactivity(TenantId tenantId, DeviceId deviceId, long inactivityTime, TbCallback callback) {
        forwardToDeviceStateService(tenantId, deviceId,
                deviceStateService -> {
                    log.debug("[{}][{}] Forwarding device inactivity event to local service. Inactivity time: [{}].", tenantId.getId(), deviceId.getId(), inactivityTime);
                    deviceStateService.onDeviceInactivity(tenantId, deviceId, inactivityTime);
                },
                () -> {
                    log.debug("[{}][{}] Sending device inactivity message to core. Inactivity time: [{}].", tenantId.getId(), deviceId.getId(), inactivityTime);
                    var deviceInactivityMsg = TransportProtos.DeviceInactivityProto.newBuilder()
                            .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                            .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                            .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                            .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                            .setLastInactivityTime(inactivityTime)
                            .build();
                    return TransportProtos.ToCoreMsg.newBuilder()
                            .setDeviceInactivityMsg(deviceInactivityMsg)
                            .build();
                }, callback);
    }

    @Override
    public void onDeviceInactivityTimeoutUpdate(TenantId tenantId, DeviceId deviceId, long inactivityTimeout, TbCallback callback) {
        forwardToDeviceStateService(tenantId, deviceId,
                deviceStateService -> {
                    log.debug("[{}][{}] Forwarding device inactivity timeout update to local service. Updated inactivity timeout: [{}].", tenantId.getId(), deviceId.getId(), inactivityTimeout);
                    deviceStateService.onDeviceInactivityTimeoutUpdate(tenantId, deviceId, inactivityTimeout);
                },
                () -> {
                    log.debug("[{}][{}] Sending device inactivity timeout update message to core. Updated inactivity timeout: [{}].", tenantId.getId(), deviceId.getId(), inactivityTimeout);
                    var deviceInactivityTimeoutUpdateMsg = TransportProtos.DeviceInactivityTimeoutUpdateProto.newBuilder()
                            .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                            .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                            .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                            .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                            .setInactivityTimeout(inactivityTimeout)
                            .build();
                    return TransportProtos.ToCoreMsg.newBuilder()
                            .setDeviceInactivityTimeoutUpdateMsg(deviceInactivityTimeoutUpdateMsg)
                            .build();
                }, callback);
    }

    private void forwardToDeviceStateService(
            TenantId tenantId, DeviceId deviceId,
            Consumer<DeviceStateService> toDeviceStateService,
            Supplier<TransportProtos.ToCoreMsg> toCore,
            TbCallback callback
    ) {
        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, tenantId, deviceId);
        if (serviceInfoProvider.isService(ServiceType.TB_CORE) && tpi.isMyPartition() && deviceStateService.isPresent()) {
            try {
                toDeviceStateService.accept(deviceStateService.get());
            } catch (Exception e) {
                log.error("[{}][{}] Failed to process device connectivity event.", tenantId.getId(), deviceId.getId(), e);
                callback.onFailure(e);
                return;
            }
            callback.onSuccess();
        } else {
            TransportProtos.ToCoreMsg toCoreMsg = toCore.get();
            clusterService.pushMsgToCore(tpi, deviceId.getId(), toCoreMsg, new SimpleTbQueueCallback(__ -> callback.onSuccess(), callback::onFailure));
        }
    }

}
