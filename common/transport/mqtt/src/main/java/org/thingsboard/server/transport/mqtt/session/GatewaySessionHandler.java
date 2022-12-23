/**
 * Copyright © 2016-2022 The Thingsboard Authors
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


import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import io.micrometer.core.lang.Nullable;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.transport.adaptor.AdaptorException;
import org.thingsboard.server.common.transport.adaptor.JsonConverter;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.mqtt.adaptors.JsonMqttAdaptor;

import java.util.Map;
import java.util.UUID;

/**
 * Created by ashvayka on 19.01.17.
 */
@Slf4j
public class GatewaySessionHandler extends AbstractGatewaySessionHandler {
    public GatewaySessionHandler(DeviceSessionCtx deviceSessionCtx, UUID sessionId) {
        super(deviceSessionCtx, sessionId);
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

    private void onDeviceTelemetryJson(int msgId, ByteBuf payload) throws AdaptorException {
        JsonElement json = JsonMqttAdaptor.validateJsonPayload(sessionId, payload);
        if (json.isJsonObject()) {
            JsonObject jsonObj = json.getAsJsonObject();
            for (Map.Entry<String, JsonElement> deviceEntry : jsonObj.entrySet()) {
                String deviceName = deviceEntry.getKey();
                Futures.addCallback(checkDeviceConnected(deviceName),
                        new FutureCallback<>() {
                            @Override
                            public void onSuccess(@Nullable MqttDeviceAwareSessionContext deviceCtx) {
                                if (!deviceEntry.getValue().isJsonArray()) {
                                    throw new JsonSyntaxException(CAN_T_PARSE_VALUE + json);
                                }
                                try {
                                    TransportProtos.PostTelemetryMsg postTelemetryMsg = JsonConverter.convertToTelemetryProto(deviceEntry.getValue().getAsJsonArray());
                                    processPostTelemetryMsg(deviceCtx, postTelemetryMsg, deviceName, msgId);
                                } catch (Throwable e) {
                                    log.warn("[{}][{}] Failed to convert telemetry: {}", transportDeviceInfo.getDeviceId(), deviceName, deviceEntry.getValue(), e);
                                    channel.close();
                                }
                            }

                            @Override
                            public void onFailure(Throwable t) {
                                log.debug("[{}] Failed to process device telemetry command: {}", sessionId, deviceName, t);
                            }
                        }, context.getExecutor());
            }
        } else {
            throw new JsonSyntaxException(CAN_T_PARSE_VALUE + json);
        }
    }

}
