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
package org.thingsboard.server.transport.mqtt.session;

import com.fasterxml.jackson.core.type.TypeReference;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttReasonCodes;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.adaptor.AdaptorException;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.transport.auth.GetOrCreateDeviceFromGatewayResponse;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.mqtt.gateway.GatewayMetricsService;

import java.util.Optional;
import java.util.UUID;

import static com.amazonaws.util.StringUtils.UTF8;

/**
 * Created by nickAS21 on 26.12.22
 */
@Slf4j
public class GatewaySessionHandler extends AbstractGatewaySessionHandler<GatewayDeviceSessionContext> {

    private final GatewayMetricsService gatewayMetricsService;

    public GatewaySessionHandler(DeviceSessionCtx deviceSessionCtx, UUID sessionId, boolean overwriteDevicesActivity) {
        super(deviceSessionCtx, sessionId, overwriteDevicesActivity);
        this.gatewayMetricsService = deviceSessionCtx.getContext().getGatewayMetricsService();
    }

    public void onDeviceConnect(MqttPublishMessage mqttMsg) throws AdaptorException {
        if (isJsonPayloadType()) {
            onDeviceConnectJson(mqttMsg);
        } else {
            onDeviceConnectProto(mqttMsg);
        }
    }

    public void onDeviceTelemetry(MqttPublishMessage mqttMsg) throws AdaptorException {
        int msgId = getMsgId(mqttMsg);
        ByteBuf payload = mqttMsg.payload();
        if (isJsonPayloadType()) {
            onDeviceTelemetryJson(msgId, payload);
        } else {
            onDeviceTelemetryProto(msgId, payload);
        }
    }

    @Override
    protected GatewayDeviceSessionContext newDeviceSessionCtx(GetOrCreateDeviceFromGatewayResponse msg) {
        return new GatewayDeviceSessionContext(this, msg.getDeviceInfo(), msg.getDeviceProfile(), mqttQoSMap, transportService);
    }

    public void onGatewayDisconnect() {
        this.onDevicesDisconnect();
        gatewayMetricsService.onDeviceDisconnect(gateway.getDeviceId());
    }

    public void onGatewayUpdate(TransportProtos.SessionInfoProto sessionInfo, Device device, Optional<DeviceProfile> deviceProfileOpt) {
        this.onDeviceUpdate(sessionInfo, device, deviceProfileOpt);
        gatewayMetricsService.onDeviceUpdate(sessionInfo, gateway.getDeviceId());
    }

    public void onGatewayDelete(DeviceId deviceId) {
        gatewayMetricsService.onDeviceDelete(deviceId);
    }

    public void onGatewayMetrics(MqttPublishMessage mqttMsg) throws AdaptorException {
        int msgId = getMsgId(mqttMsg);
        ByteBuf payloadData = mqttMsg.payload();
        String payload = payloadData.toString(UTF8);
        if (payload == null) {
            log.debug("[{}][{}][{}] Payload is empty!", gateway.getTenantId(), gateway.getDeviceId(), sessionId);
            throw new AdaptorException(new IllegalArgumentException("Payload is empty!"));
        }
        long ts = System.currentTimeMillis();
        try {
            gatewayMetricsService.process(deviceSessionCtx.getSessionInfo(), gateway.getDeviceId(), JacksonUtil.fromString(payload, new TypeReference<>() {}), ts);
            ack(msgId, MqttReasonCodes.PubAck.SUCCESS);
        } catch (Throwable t) {
            ackOrClose(msgId);
        }
    }

}
