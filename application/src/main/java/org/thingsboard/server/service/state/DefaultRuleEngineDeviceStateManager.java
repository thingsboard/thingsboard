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

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.rule.engine.api.RuleEngineDeviceStateManager;
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
import org.thingsboard.server.queue.util.TbRuleEngineComponent;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@TbRuleEngineComponent
public class DefaultRuleEngineDeviceStateManager implements RuleEngineDeviceStateManager {

    private final TbServiceInfoProvider serviceInfoProvider;
    private final PartitionService partitionService;

    private final Optional<DeviceStateService> deviceStateService;
    private final TbClusterService clusterService;

    public DefaultRuleEngineDeviceStateManager(
            TbServiceInfoProvider serviceInfoProvider, PartitionService partitionService,
            Optional<DeviceStateService> deviceStateServiceOptional, TbClusterService clusterService
    ) {
        this.serviceInfoProvider = serviceInfoProvider;
        this.partitionService = partitionService;
        this.deviceStateService = deviceStateServiceOptional;
        this.clusterService = clusterService;
    }

    @Getter
    private abstract static class ConnectivityEventInfo {

        private final TenantId tenantId;
        private final DeviceId deviceId;
        private final long eventTime;

        private ConnectivityEventInfo(TenantId tenantId, DeviceId deviceId, long eventTime) {
            this.tenantId = tenantId;
            this.deviceId = deviceId;
            this.eventTime = eventTime;
        }

        abstract void forwardToLocalService();

        abstract TransportProtos.ToCoreMsg toQueueMsg();

    }

    @Override
    public void onDeviceConnect(TenantId tenantId, DeviceId deviceId, long connectTime, TbCallback callback) {
        routeEvent(new ConnectivityEventInfo(tenantId, deviceId, connectTime) {
            @Override
            void forwardToLocalService() {
                deviceStateService.ifPresent(service -> service.onDeviceConnect(tenantId, deviceId, connectTime));
            }

            @Override
            TransportProtos.ToCoreMsg toQueueMsg() {
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
            }
        }, callback);
    }

    @Override
    public void onDeviceActivity(TenantId tenantId, DeviceId deviceId, long activityTime, TbCallback callback) {
        routeEvent(new ConnectivityEventInfo(tenantId, deviceId, activityTime) {
            @Override
            void forwardToLocalService() {
                deviceStateService.ifPresent(service -> service.onDeviceActivity(tenantId, deviceId, activityTime));
            }

            @Override
            TransportProtos.ToCoreMsg toQueueMsg() {
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
            }
        }, callback);
    }

    @Override
    public void onDeviceDisconnect(TenantId tenantId, DeviceId deviceId, long disconnectTime, TbCallback callback) {
        routeEvent(new ConnectivityEventInfo(tenantId, deviceId, disconnectTime) {
            @Override
            void forwardToLocalService() {
                deviceStateService.ifPresent(service -> service.onDeviceDisconnect(tenantId, deviceId, disconnectTime));
            }

            @Override
            TransportProtos.ToCoreMsg toQueueMsg() {
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
            }
        }, callback);
    }

    @Override
    public void onDeviceInactivity(TenantId tenantId, DeviceId deviceId, long inactivityTime, TbCallback callback) {
        routeEvent(new ConnectivityEventInfo(tenantId, deviceId, inactivityTime) {
            @Override
            void forwardToLocalService() {
                deviceStateService.ifPresent(service -> service.onDeviceInactivity(tenantId, deviceId, inactivityTime));
            }

            @Override
            TransportProtos.ToCoreMsg toQueueMsg() {
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
            }
        }, callback);
    }

    private void routeEvent(ConnectivityEventInfo eventInfo, TbCallback callback) {
        var tenantId = eventInfo.getTenantId();
        var deviceId = eventInfo.getDeviceId();
        long eventTime = eventInfo.getEventTime();

        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, tenantId, deviceId);
        if (serviceInfoProvider.isService(ServiceType.TB_CORE) && tpi.isMyPartition() && deviceStateService.isPresent()) {
            log.debug("[{}][{}] Forwarding device connectivity event to local service. Event time: [{}].", tenantId.getId(), deviceId.getId(), eventTime);
            try {
                eventInfo.forwardToLocalService();
            } catch (Exception e) {
                log.error("[{}][{}] Failed to process device connectivity event. Event time: [{}].", tenantId.getId(), deviceId.getId(), eventTime, e);
                callback.onFailure(e);
                return;
            }
            callback.onSuccess();
        } else {
            TransportProtos.ToCoreMsg msg = eventInfo.toQueueMsg();
            log.debug("[{}][{}] Sending device connectivity message to core. Event time: [{}].", tenantId.getId(), deviceId.getId(), eventTime);
            clusterService.pushMsgToCore(tpi, UUID.randomUUID(), msg, new SimpleTbQueueCallback(__ -> callback.onSuccess(), callback::onFailure));
        }
    }

}
