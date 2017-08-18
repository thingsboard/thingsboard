/**
 * Copyright © 2016-2017 The Thingsboard Authors
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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.SessionId;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.msg.core.*;
import org.thingsboard.server.common.msg.session.BasicAdaptorToSessionActorMsg;
import org.thingsboard.server.common.msg.session.BasicToDeviceActorSessionMsg;
import org.thingsboard.server.common.msg.session.ctrl.SessionCloseMsg;
import org.thingsboard.server.common.transport.SessionMsgProcessor;
import org.thingsboard.server.common.transport.adaptor.AdaptorException;
import org.thingsboard.server.common.transport.adaptor.JsonConverter;
import org.thingsboard.server.common.transport.auth.DeviceAuthService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.transport.mqtt.MqttTransportHandler;
import org.thingsboard.server.transport.mqtt.adaptors.JsonMqttAdaptor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.thingsboard.server.transport.mqtt.adaptors.JsonMqttAdaptor.validateJsonPayload;

/**
 * Created by ashvayka on 19.01.17.
 */
@Slf4j
public class GatewaySessionCtx {

    private static final String DEFAULT_DEVICE_TYPE = "default";
    private final Device gateway;
    private final SessionId gatewaySessionId;
    private final SessionMsgProcessor processor;
    private final DeviceService deviceService;
    private final DeviceAuthService authService;
    private final RelationService relationService;
    private final Map<String, GatewayDeviceSessionCtx> devices;
    private ChannelHandlerContext channel;

    public GatewaySessionCtx(SessionMsgProcessor processor, DeviceService deviceService, DeviceAuthService authService, RelationService relationService, DeviceSessionCtx gatewaySessionCtx) {
        this.processor = processor;
        this.deviceService = deviceService;
        this.authService = authService;
        this.relationService = relationService;
        this.gateway = gatewaySessionCtx.getDevice();
        this.gatewaySessionId = gatewaySessionCtx.getSessionId();
        this.devices = new HashMap<>();
    }

    public void onDeviceConnect(MqttPublishMessage msg) throws AdaptorException {
        JsonElement json = getJson(msg);
        String deviceName = checkDeviceName(getDeviceName(json));
        String deviceType = getDeviceType(json);
        onDeviceConnect(deviceName, deviceType);
        ack(msg);
    }

    private void onDeviceConnect(String deviceName, String deviceType) {
        if (!devices.containsKey(deviceName)) {
            Optional<Device> deviceOpt = deviceService.findDeviceByTenantIdAndName(gateway.getTenantId(), deviceName);
            Device device = deviceOpt.orElseGet(() -> {
                Device newDevice = new Device();
                newDevice.setTenantId(gateway.getTenantId());
                newDevice.setName(deviceName);
                newDevice.setType(deviceType);
                newDevice = deviceService.saveDevice(newDevice);
                relationService.saveRelation(new EntityRelation(gateway.getId(), newDevice.getId(), "Created"));
                return newDevice;
            });
            GatewayDeviceSessionCtx ctx = new GatewayDeviceSessionCtx(this, device);
            devices.put(deviceName, ctx);
            log.debug("[{}] Added device [{}] to the gateway session", gatewaySessionId, deviceName);
            processor.process(new BasicToDeviceActorSessionMsg(device, new BasicAdaptorToSessionActorMsg(ctx, new AttributesSubscribeMsg())));
            processor.process(new BasicToDeviceActorSessionMsg(device, new BasicAdaptorToSessionActorMsg(ctx, new RpcSubscribeMsg())));
        }
    }

    public void onDeviceDisconnect(MqttPublishMessage msg) throws AdaptorException {
        String deviceName = checkDeviceName(getDeviceName(getJson(msg)));
        GatewayDeviceSessionCtx deviceSessionCtx = devices.remove(deviceName);
        if (deviceSessionCtx != null) {
            processor.process(SessionCloseMsg.onDisconnect(deviceSessionCtx.getSessionId()));
            deviceSessionCtx.setClosed(true);
            log.debug("[{}] Removed device [{}] from the gateway session", gatewaySessionId, deviceName);
        } else {
            log.debug("[{}] Device [{}] was already removed from the gateway session", gatewaySessionId, deviceName);
        }
        ack(msg);
    }

    public void onGatewayDisconnect() {
        devices.forEach((k, v) -> {
            processor.process(SessionCloseMsg.onDisconnect(v.getSessionId()));
        });
    }

    public void onDeviceTelemetry(MqttPublishMessage mqttMsg) throws AdaptorException {
        JsonElement json = validateJsonPayload(gatewaySessionId, mqttMsg.payload());
        int requestId = mqttMsg.variableHeader().messageId();
        if (json.isJsonObject()) {
            JsonObject jsonObj = json.getAsJsonObject();
            for (Map.Entry<String, JsonElement> deviceEntry : jsonObj.entrySet()) {
                String deviceName = checkDeviceConnected(deviceEntry.getKey());
                if (!deviceEntry.getValue().isJsonArray()) {
                    throw new JsonSyntaxException("Can't parse value: " + json);
                }
                BasicTelemetryUploadRequest request = new BasicTelemetryUploadRequest(requestId);
                JsonArray deviceData = deviceEntry.getValue().getAsJsonArray();
                for (JsonElement element : deviceData) {
                    JsonConverter.parseWithTs(request, element.getAsJsonObject());
                }
                GatewayDeviceSessionCtx deviceSessionCtx = devices.get(deviceName);
                processor.process(new BasicToDeviceActorSessionMsg(deviceSessionCtx.getDevice(),
                        new BasicAdaptorToSessionActorMsg(deviceSessionCtx, request)));
            }
        } else {
            throw new JsonSyntaxException("Can't parse value: " + json);
        }
    }

    public void onDeviceRpcResponse(MqttPublishMessage mqttMsg) throws AdaptorException {
        JsonElement json = validateJsonPayload(gatewaySessionId, mqttMsg.payload());
        if (json.isJsonObject()) {
            JsonObject jsonObj = json.getAsJsonObject();
            String deviceName = checkDeviceConnected(jsonObj.get("device").getAsString());
            Integer requestId = jsonObj.get("id").getAsInt();
            String data = jsonObj.get("data").toString();
            GatewayDeviceSessionCtx deviceSessionCtx = devices.get(deviceName);
            processor.process(new BasicToDeviceActorSessionMsg(deviceSessionCtx.getDevice(),
                    new BasicAdaptorToSessionActorMsg(deviceSessionCtx, new ToDeviceRpcResponseMsg(requestId, data))));
        } else {
            throw new JsonSyntaxException("Can't parse value: " + json);
        }
    }

    public void onDeviceAttributes(MqttPublishMessage mqttMsg) throws AdaptorException {
        JsonElement json = validateJsonPayload(gatewaySessionId, mqttMsg.payload());
        int requestId = mqttMsg.variableHeader().messageId();
        if (json.isJsonObject()) {
            JsonObject jsonObj = json.getAsJsonObject();
            for (Map.Entry<String, JsonElement> deviceEntry : jsonObj.entrySet()) {
                String deviceName = checkDeviceConnected(deviceEntry.getKey());
                if (!deviceEntry.getValue().isJsonObject()) {
                    throw new JsonSyntaxException("Can't parse value: " + json);
                }
                long ts = System.currentTimeMillis();
                BasicUpdateAttributesRequest request = new BasicUpdateAttributesRequest(requestId);
                JsonObject deviceData = deviceEntry.getValue().getAsJsonObject();
                request.add(JsonConverter.parseValues(deviceData).stream().map(kv -> new BaseAttributeKvEntry(kv, ts)).collect(Collectors.toList()));
                GatewayDeviceSessionCtx deviceSessionCtx = devices.get(deviceName);
                processor.process(new BasicToDeviceActorSessionMsg(deviceSessionCtx.getDevice(),
                        new BasicAdaptorToSessionActorMsg(deviceSessionCtx, request)));
            }
        } else {
            throw new JsonSyntaxException("Can't parse value: " + json);
        }
    }

    public void onDeviceAttributesRequest(MqttPublishMessage mqttMsg) throws AdaptorException {
        JsonElement json = validateJsonPayload(gatewaySessionId, mqttMsg.payload());
        if (json.isJsonObject()) {
            JsonObject jsonObj = json.getAsJsonObject();
            int requestId = jsonObj.get("id").getAsInt();
            String deviceName = jsonObj.get("device").getAsString();
            boolean clientScope = jsonObj.get("client").getAsBoolean();
            String key = jsonObj.get("key").getAsString();

            BasicGetAttributesRequest request;
            if (clientScope) {
                request = new BasicGetAttributesRequest(requestId, Collections.singleton(key), null);
            } else {
                request = new BasicGetAttributesRequest(requestId, null, Collections.singleton(key));
            }
            GatewayDeviceSessionCtx deviceSessionCtx = devices.get(deviceName);
            processor.process(new BasicToDeviceActorSessionMsg(deviceSessionCtx.getDevice(),
                    new BasicAdaptorToSessionActorMsg(deviceSessionCtx, request)));
        } else {
            throw new JsonSyntaxException("Can't parse value: " + json);
        }
    }

    private String checkDeviceConnected(String deviceName) {
        if (!devices.containsKey(deviceName)) {
            log.debug("[{}] Missing device [{}] for the gateway session", gatewaySessionId, deviceName);
            onDeviceConnect(deviceName, DEFAULT_DEVICE_TYPE);
        }
        return deviceName;
    }

    private String checkDeviceName(String deviceName) {
        if (StringUtils.isEmpty(deviceName)) {
            throw new RuntimeException("Device name is empty!");
        } else {
            return deviceName;
        }
    }

    private String getDeviceName(JsonElement json) throws AdaptorException {
        return json.getAsJsonObject().get("device").getAsString();
    }

    private String getDeviceType(JsonElement json) throws AdaptorException {
        JsonElement type = json.getAsJsonObject().get("type");
        return type == null ? DEFAULT_DEVICE_TYPE : type.getAsString();
    }

    private JsonElement getJson(MqttPublishMessage mqttMsg) throws AdaptorException {
        return JsonMqttAdaptor.validateJsonPayload(gatewaySessionId, mqttMsg.payload());
    }

    protected SessionMsgProcessor getProcessor() {
        return processor;
    }

    DeviceAuthService getAuthService() {
        return authService;
    }

    public void setChannel(ChannelHandlerContext channel) {
        this.channel = channel;
    }

    private void ack(MqttPublishMessage msg) {
        if(msg.variableHeader().messageId() > 0) {
            writeAndFlush(MqttTransportHandler.createMqttPubAckMsg(msg.variableHeader().messageId()));
        }
    }

    void writeAndFlush(MqttMessage mqttMessage) {
        channel.writeAndFlush(mqttMessage);
    }

}
