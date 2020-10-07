/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.transport.mqtt.adaptors;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttPublishVariableHeader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.thingsboard.server.common.transport.adaptor.AdaptorException;
import org.thingsboard.server.common.transport.adaptor.JsonConverter;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.common.data.device.profile.MqttTopics;
import org.thingsboard.server.transport.mqtt.session.MqttDeviceAwareSessionContext;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * @author Andrew Shvayka
 */
@Component
@Slf4j
public class JsonMqttAdaptor implements MqttTransportAdaptor {

    protected static final Charset UTF8 = StandardCharsets.UTF_8;

    private static final Gson GSON = new Gson();
    private static final ByteBufAllocator ALLOCATOR = new UnpooledByteBufAllocator(false);

    @Override
    public TransportProtos.PostTelemetryMsg convertToPostTelemetry(MqttDeviceAwareSessionContext ctx, MqttPublishMessage inbound) throws AdaptorException {
        String payload = validatePayload(ctx.getSessionId(), inbound.payload(), false);
        try {
            return JsonConverter.convertToTelemetryProto(new JsonParser().parse(payload));
        } catch (IllegalStateException | JsonSyntaxException ex) {
            throw new AdaptorException(ex);
        }
    }

    @Override
    public TransportProtos.PostAttributeMsg convertToPostAttributes(MqttDeviceAwareSessionContext ctx, MqttPublishMessage inbound) throws AdaptorException {
        String payload = validatePayload(ctx.getSessionId(), inbound.payload(), false);
        try {
            return JsonConverter.convertToAttributesProto(new JsonParser().parse(payload));
        } catch (IllegalStateException | JsonSyntaxException ex) {
            throw new AdaptorException(ex);
        }
    }

    @Override
    public TransportProtos.ClaimDeviceMsg convertToClaimDevice(MqttDeviceAwareSessionContext ctx, MqttPublishMessage inbound) throws AdaptorException {
        String payload = validatePayload(ctx.getSessionId(), inbound.payload(), true);
        try {
            return JsonConverter.convertToClaimDeviceProto(ctx.getDeviceId(), payload);
        } catch (IllegalStateException | JsonSyntaxException ex) {
            throw new AdaptorException(ex);
        }
    }

    @Override
    public TransportProtos.ProvisionDeviceRequestMsg convertToProvisionRequestMsg(MqttDeviceAwareSessionContext ctx, MqttPublishMessage inbound) throws AdaptorException {
        String payload = validatePayload(ctx.getSessionId(), inbound.payload(), false);
        try {
            return JsonConverter.convertToProvisionRequestMsg(payload);
        } catch (IllegalStateException | JsonSyntaxException ex) {
            throw new AdaptorException(ex);
        }
    }

    @Override
    public TransportProtos.GetAttributeRequestMsg convertToGetAttributes(MqttDeviceAwareSessionContext ctx, MqttPublishMessage inbound) throws AdaptorException {
        return processGetAttributeRequestMsg(inbound, MqttTopics.DEVICE_ATTRIBUTES_REQUEST_TOPIC_PREFIX);
    }

    @Override
    public TransportProtos.ToDeviceRpcResponseMsg convertToDeviceRpcResponse(MqttDeviceAwareSessionContext ctx, MqttPublishMessage inbound) throws AdaptorException {
        return processToDeviceRpcResponseMsg(inbound, MqttTopics.DEVICE_RPC_RESPONSE_TOPIC);
    }

    @Override
    public TransportProtos.ToServerRpcRequestMsg convertToServerRpcRequest(MqttDeviceAwareSessionContext ctx, MqttPublishMessage inbound) throws AdaptorException {
        return processToServerRpcRequestMsg(ctx, inbound, MqttTopics.DEVICE_RPC_REQUESTS_TOPIC);
    }

    @Override
    public Optional<MqttMessage> convertToPublish(MqttDeviceAwareSessionContext ctx, TransportProtos.GetAttributeResponseMsg responseMsg) throws AdaptorException {
        return processConvertFromAttributeResponseMsg(ctx, responseMsg, MqttTopics.DEVICE_ATTRIBUTES_RESPONSE_TOPIC_PREFIX);
    }

    @Override
    public Optional<MqttMessage> convertToGatewayPublish(MqttDeviceAwareSessionContext ctx, String deviceName, TransportProtos.GetAttributeResponseMsg responseMsg) throws AdaptorException {
        return processConvertFromGatewayAttributeResponseMsg(ctx, deviceName, responseMsg, MqttTopics.GATEWAY_ATTRIBUTES_RESPONSE_TOPIC);
    }

    @Override
    public Optional<MqttMessage> convertToPublish(MqttDeviceAwareSessionContext ctx, TransportProtos.AttributeUpdateNotificationMsg notificationMsg) {
        return Optional.of(createMqttPublishMsg(ctx, MqttTopics.DEVICE_ATTRIBUTES_TOPIC, JsonConverter.toJson(notificationMsg)));
    }

    @Override
    public Optional<MqttMessage> convertToGatewayPublish(MqttDeviceAwareSessionContext ctx, String deviceName, TransportProtos.AttributeUpdateNotificationMsg notificationMsg) {
        JsonObject result = JsonConverter.getJsonObjectForGateway(deviceName, notificationMsg);
        return Optional.of(createMqttPublishMsg(ctx, MqttTopics.GATEWAY_ATTRIBUTES_TOPIC, result));
    }

    @Override
    public Optional<MqttMessage> convertToPublish(MqttDeviceAwareSessionContext ctx, TransportProtos.ToDeviceRpcRequestMsg rpcRequest) {
        return Optional.of(createMqttPublishMsg(ctx, MqttTopics.DEVICE_RPC_REQUESTS_TOPIC + rpcRequest.getRequestId(), JsonConverter.toJson(rpcRequest, false)));
    }

    @Override
    public Optional<MqttMessage> convertToGatewayPublish(MqttDeviceAwareSessionContext ctx, String deviceName, TransportProtos.ToDeviceRpcRequestMsg rpcRequest) {
        return Optional.of(createMqttPublishMsg(ctx, MqttTopics.GATEWAY_RPC_TOPIC, JsonConverter.toGatewayJson(deviceName, rpcRequest)));
    }

    @Override
    public Optional<MqttMessage> convertToPublish(MqttDeviceAwareSessionContext ctx, TransportProtos.ToServerRpcResponseMsg rpcResponse) {
        return Optional.of(createMqttPublishMsg(ctx, MqttTopics.DEVICE_RPC_RESPONSE_TOPIC + rpcResponse.getRequestId(), JsonConverter.toJson(rpcResponse)));
    }

    @Override
    public Optional<MqttMessage> convertToPublish(MqttDeviceAwareSessionContext ctx, TransportProtos.ProvisionDeviceResponseMsg provisionResponse) {
        return Optional.of(createMqttPublishMsg(ctx, MqttTopics.DEVICE_PROVISION_RESPONSE_TOPIC, JsonConverter.toJson(provisionResponse)));
    }

    @Override
    public Optional<MqttMessage> convertToGatewayPublish(MqttDeviceAwareSessionContext ctx, String deviceName, TransportProtos.ProvisionDeviceResponseMsg responseMsg) {
        return Optional.of(createMqttPublishMsg(ctx,
                MqttTopics.GATEWAY_PROVISION_RESPONSE_TOPIC,
                JsonConverter.toGatewayJson(deviceName, responseMsg)));
    }

    public static JsonElement validateJsonPayload(UUID sessionId, ByteBuf payloadData) throws AdaptorException {
        String payload = validatePayload(sessionId, payloadData, false);
        try {
            return new JsonParser().parse(payload);
        } catch (JsonSyntaxException ex) {
            log.warn("Payload is in incorrect format: {}", payload);
            throw new AdaptorException(ex);
        }
    }

    protected TransportProtos.GetAttributeRequestMsg processGetAttributeRequestMsg(MqttPublishMessage inbound, String topic) throws AdaptorException {
        String topicName = inbound.variableHeader().topicName();
        try {
            TransportProtos.GetAttributeRequestMsg.Builder result = TransportProtos.GetAttributeRequestMsg.newBuilder();
            result.setRequestId(getRequestId(topicName, topic));
            String payload = inbound.payload().toString(UTF8);
            JsonElement requestBody = new JsonParser().parse(payload);
            Set<String> clientKeys = toStringSet(requestBody, "clientKeys");
            Set<String> sharedKeys = toStringSet(requestBody, "sharedKeys");
            if (clientKeys != null) {
                result.addAllClientAttributeNames(clientKeys);
            }
            if (sharedKeys != null) {
                result.addAllSharedAttributeNames(sharedKeys);
            }
            return result.build();
        } catch (RuntimeException e) {
            log.warn("Failed to decode get attributes request", e);
            throw new AdaptorException(e);
        }
    }

    protected TransportProtos.ToDeviceRpcResponseMsg processToDeviceRpcResponseMsg(MqttPublishMessage inbound, String topic) throws AdaptorException {
        String topicName = inbound.variableHeader().topicName();
        try {
            int requestId = getRequestId(topicName, topic);
            String payload = inbound.payload().toString(UTF8);
            return TransportProtos.ToDeviceRpcResponseMsg.newBuilder().setRequestId(requestId).setPayload(payload).build();
        } catch (RuntimeException e) {
            log.warn("Failed to decode Rpc response", e);
            throw new AdaptorException(e);
        }
    }

    protected TransportProtos.ToServerRpcRequestMsg processToServerRpcRequestMsg(MqttDeviceAwareSessionContext ctx, MqttPublishMessage inbound, String topic) throws AdaptorException {
        String topicName = inbound.variableHeader().topicName();
        String payload = validatePayload(ctx.getSessionId(), inbound.payload(), false);
        try {
            int requestId = getRequestId(topicName, topic);
            return JsonConverter.convertToServerRpcRequest(new JsonParser().parse(payload), requestId);
        } catch (IllegalStateException | JsonSyntaxException ex) {
            throw new AdaptorException(ex);
        }
    }

    protected Optional<MqttMessage> processConvertFromAttributeResponseMsg(MqttDeviceAwareSessionContext ctx, TransportProtos.GetAttributeResponseMsg responseMsg, String topic) throws AdaptorException {
        if (!StringUtils.isEmpty(responseMsg.getError())) {
            throw new AdaptorException(responseMsg.getError());
        } else {
            int requestId = responseMsg.getRequestId();
            if (requestId >= 0) {
                return Optional.of(createMqttPublishMsg(ctx,
                        topic + requestId,
                        JsonConverter.toJson(responseMsg)));
            }
            return Optional.empty();
        }
    }

    protected Optional<MqttMessage> processConvertFromGatewayAttributeResponseMsg(MqttDeviceAwareSessionContext ctx, String deviceName, TransportProtos.GetAttributeResponseMsg responseMsg, String topic) throws AdaptorException {
        if (!StringUtils.isEmpty(responseMsg.getError())) {
            throw new AdaptorException(responseMsg.getError());
        } else {
            JsonObject result = JsonConverter.getJsonObjectForGateway(deviceName, responseMsg);
            return Optional.of(createMqttPublishMsg(ctx, topic, result));
        }
    }

    protected MqttPublishMessage createMqttPublishMsg(MqttDeviceAwareSessionContext ctx, String topic, JsonElement json) {
        MqttFixedHeader mqttFixedHeader =
                new MqttFixedHeader(MqttMessageType.PUBLISH, false, ctx.getQoSForTopic(topic), false, 0);
        MqttPublishVariableHeader header = new MqttPublishVariableHeader(topic, ctx.nextMsgId());
        ByteBuf payload = ALLOCATOR.buffer();
        payload.writeBytes(GSON.toJson(json).getBytes(UTF8));
        return new MqttPublishMessage(mqttFixedHeader, header, payload);
    }

    private Set<String> toStringSet(JsonElement requestBody, String name) {
        JsonElement element = requestBody.getAsJsonObject().get(name);
        if (element != null) {
            return new HashSet<>(Arrays.asList(element.getAsString().split(",")));
        } else {
            return null;
        }
    }

    private static String validatePayload(UUID sessionId, ByteBuf payloadData, boolean isEmptyPayloadAllowed) throws AdaptorException {
        String payload = payloadData.toString(UTF8);
        if (payload == null) {
            log.warn("[{}] Payload is empty!", sessionId);
            if (!isEmptyPayloadAllowed) {
                throw new AdaptorException(new IllegalArgumentException("Payload is empty!"));
            }
        }
        return payload;
    }

    private int getRequestId(String topicName, String topic) {
        return Integer.parseInt(topicName.substring(topic.length()));
    }

}
