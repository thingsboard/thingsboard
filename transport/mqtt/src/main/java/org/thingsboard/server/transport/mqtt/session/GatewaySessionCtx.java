/**
 * Copyright © 2016-2018 The Thingsboard Authors
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

import com.google.gson.*;
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
import org.thingsboard.server.common.msg.session.BasicTransportToDeviceSessionActorMsg;
import org.thingsboard.server.common.msg.session.ctrl.SessionCloseMsg;
import org.thingsboard.server.common.transport.SessionMsgProcessor;
import org.thingsboard.server.common.transport.adaptor.AdaptorException;
import org.thingsboard.server.common.transport.adaptor.JsonConverter;
import org.thingsboard.server.common.transport.auth.DeviceAuthService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.transport.mqtt.MqttTopicMatcher;
import org.thingsboard.server.transport.mqtt.MqttTransportHandler;
import org.thingsboard.server.transport.mqtt.adaptors.JsonMqttAdaptor;

import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import static org.thingsboard.server.transport.mqtt.adaptors.JsonMqttAdaptor.validateJsonPayload;

/**
 * Created by ashvayka on 19.01.17.
 */
@Slf4j
public class GatewaySessionCtx {

    private static final String DEFAULT_DEVICE_TYPE = "default";
    public static final String CAN_T_PARSE_VALUE = "Can't parse value: ";
    public static final String DEVICE_PROPERTY = "device";
    private final Device gateway;
    private final SessionId gatewaySessionId;
    private final SessionMsgProcessor processor;
    private final DeviceService deviceService;
    private final DeviceAuthService authService;
    private final RelationService relationService;
    private final Map<String, GatewayDeviceSessionCtx> devices;
    private final ConcurrentMap<MqttTopicMatcher, Integer> mqttQoSMap;
    private ChannelHandlerContext channel;

    public GatewaySessionCtx(SessionMsgProcessor processor, DeviceService deviceService, DeviceAuthService authService, RelationService relationService, DeviceSessionCtx gatewaySessionCtx) {
        this.processor = processor;
        this.deviceService = deviceService;
        this.authService = authService;
        this.relationService = relationService;
        this.gateway = gatewaySessionCtx.getDevice();
        this.gatewaySessionId = gatewaySessionCtx.getSessionId();
        this.devices = new HashMap<>();
        this.mqttQoSMap = gatewaySessionCtx.getMqttQoSMap();
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
            Device device = deviceService.findDeviceByTenantIdAndName(gateway.getTenantId(), deviceName);
            if (device == null) {
                device = new Device();
                device.setTenantId(gateway.getTenantId());
                device.setName(deviceName);
                device.setType(deviceType);
                device.setAssignedCustomers(gateway.getAssignedCustomers());
                device = deviceService.saveDevice(device);
                relationService.saveRelationAsync(new EntityRelation(gateway.getId(), device.getId(), "Created"));
                processor.onDeviceAdded(device);
            }
            GatewayDeviceSessionCtx ctx = new GatewayDeviceSessionCtx(this, device, mqttQoSMap);
            devices.put(deviceName, ctx);
            log.debug("[{}] Added device [{}] to the gateway session", gatewaySessionId, deviceName);
            processor.process(new BasicTransportToDeviceSessionActorMsg(device, new BasicAdaptorToSessionActorMsg(ctx, new AttributesSubscribeMsg())));
            processor.process(new BasicTransportToDeviceSessionActorMsg(device, new BasicAdaptorToSessionActorMsg(ctx, new RpcSubscribeMsg())));
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
                    throw new JsonSyntaxException(CAN_T_PARSE_VALUE + json);
                }
                BasicTelemetryUploadRequest request = new BasicTelemetryUploadRequest(requestId);
                JsonArray deviceData = deviceEntry.getValue().getAsJsonArray();
                for (JsonElement element : deviceData) {
                    JsonConverter.parseWithTs(request, element.getAsJsonObject());
                }
                GatewayDeviceSessionCtx deviceSessionCtx = devices.get(deviceName);
                processor.process(new BasicTransportToDeviceSessionActorMsg(deviceSessionCtx.getDevice(),
                        new BasicAdaptorToSessionActorMsg(deviceSessionCtx, request)));
            }
        } else {
            throw new JsonSyntaxException(CAN_T_PARSE_VALUE + json);
        }
    }

    public void onDeviceRpcResponse(MqttPublishMessage mqttMsg) throws AdaptorException {
        JsonElement json = validateJsonPayload(gatewaySessionId, mqttMsg.payload());
        if (json.isJsonObject()) {
            JsonObject jsonObj = json.getAsJsonObject();
            String deviceName = checkDeviceConnected(jsonObj.get(DEVICE_PROPERTY).getAsString());
            Integer requestId = jsonObj.get("id").getAsInt();
            String data = jsonObj.get("data").toString();
            GatewayDeviceSessionCtx deviceSessionCtx = devices.get(deviceName);
            processor.process(new BasicTransportToDeviceSessionActorMsg(deviceSessionCtx.getDevice(),
                    new BasicAdaptorToSessionActorMsg(deviceSessionCtx, new ToDeviceRpcResponseMsg(requestId, data))));
            ack(mqttMsg);
        } else {
            throw new JsonSyntaxException(CAN_T_PARSE_VALUE + json);
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
                    throw new JsonSyntaxException(CAN_T_PARSE_VALUE + json);
                }
                long ts = System.currentTimeMillis();
                BasicAttributesUpdateRequest request = new BasicAttributesUpdateRequest(requestId);
                JsonObject deviceData = deviceEntry.getValue().getAsJsonObject();
                request.add(JsonConverter.parseValues(deviceData).stream().map(kv -> new BaseAttributeKvEntry(kv, ts)).collect(Collectors.toList()));
                GatewayDeviceSessionCtx deviceSessionCtx = devices.get(deviceName);
                processor.process(new BasicTransportToDeviceSessionActorMsg(deviceSessionCtx.getDevice(),
                        new BasicAdaptorToSessionActorMsg(deviceSessionCtx, request)));
            }
        } else {
            throw new JsonSyntaxException(CAN_T_PARSE_VALUE + json);
        }
    }

    public void onDeviceAttributesRequest(MqttPublishMessage msg) throws AdaptorException {
        JsonElement json = validateJsonPayload(gatewaySessionId, msg.payload());
        if (json.isJsonObject()) {
            JsonObject jsonObj = json.getAsJsonObject();
            int requestId = jsonObj.get("id").getAsInt();
            String deviceName = jsonObj.get(DEVICE_PROPERTY).getAsString();
            boolean clientScope = jsonObj.get("client").getAsBoolean();
            Set<String> keys;
            if (jsonObj.has("key")) {
                keys = Collections.singleton(jsonObj.get("key").getAsString());
            } else {
                JsonArray keysArray = jsonObj.get("keys").getAsJsonArray();
                keys = new HashSet<>();
                for (JsonElement keyObj : keysArray) {
                    keys.add(keyObj.getAsString());
                }
            }

            BasicGetAttributesRequest request;
            if (clientScope) {
                request = new BasicGetAttributesRequest(requestId, keys, null);
            } else {
                request = new BasicGetAttributesRequest(requestId, null, keys);
            }
            GatewayDeviceSessionCtx deviceSessionCtx = devices.get(deviceName);
            processor.process(new BasicTransportToDeviceSessionActorMsg(deviceSessionCtx.getDevice(),
                    new BasicAdaptorToSessionActorMsg(deviceSessionCtx, request)));
            ack(msg);
        } else {
            throw new JsonSyntaxException(CAN_T_PARSE_VALUE + json);
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
        return json.getAsJsonObject().get(DEVICE_PROPERTY).getAsString();
    }

    private String getDeviceType(JsonElement json) throws AdaptorException {
        JsonElement type = json.getAsJsonObject().get("type");
        return type == null || type instanceof JsonNull ? DEFAULT_DEVICE_TYPE : type.getAsString();
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
        if (msg.variableHeader().messageId() > 0) {
            writeAndFlush(MqttTransportHandler.createMqttPubAckMsg(msg.variableHeader().messageId()));
        }
    }

    void writeAndFlush(MqttMessage mqttMessage) {
        channel.writeAndFlush(mqttMessage);
    }

}
