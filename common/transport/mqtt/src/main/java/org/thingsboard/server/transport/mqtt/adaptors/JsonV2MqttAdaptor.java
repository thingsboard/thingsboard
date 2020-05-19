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

import com.google.gson.JsonObject;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.transport.adaptor.AdaptorException;
import org.thingsboard.server.common.transport.adaptor.JsonConverter;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.mqtt.MqttTopics;
import org.thingsboard.server.transport.mqtt.session.MqttDeviceAwareSessionContext;

import java.util.Optional;

@Component
@Slf4j
public class JsonV2MqttAdaptor extends JsonV1MqttAdaptor implements MqttTransportAdaptor {

    @Override
    public TransportProtos.GetAttributeRequestMsg convertToGetAttributes(MqttDeviceAwareSessionContext ctx, MqttPublishMessage inbound) throws AdaptorException {
        return processGetAttributeRequestMsg(inbound, MqttTopics.DEVICE_ATTRIBUTES_REQUEST_TOPIC_PREFIX_V2_JSON);
    }

    @Override
    public TransportProtos.ToDeviceRpcResponseMsg convertToDeviceRpcResponse(MqttDeviceAwareSessionContext ctx, MqttPublishMessage inbound) throws AdaptorException {
        return processToDeviceRpcResponseMsg(inbound, MqttTopics.DEVICE_RPC_RESPONSE_TOPIC_V2_JSON);
    }

    @Override
    public TransportProtos.ToServerRpcRequestMsg convertToServerRpcRequest(MqttDeviceAwareSessionContext ctx, MqttPublishMessage inbound) throws AdaptorException {
        return processToServerRpcRequestMsg(ctx, inbound, MqttTopics.DEVICE_RPC_REQUESTS_TOPIC_V2_JSON);
    }

    @Override
    public Optional<MqttMessage> convertToPublish(MqttDeviceAwareSessionContext ctx, TransportProtos.GetAttributeResponseMsg responseMsg) throws AdaptorException {
        return processConvertFromAttributeResponseMsg(ctx, responseMsg, MqttTopics.DEVICE_ATTRIBUTES_RESPONSE_TOPIC_PREFIX_V2_JSON);
    }

    @Override
    public Optional<MqttMessage> convertToGatewayPublish(MqttDeviceAwareSessionContext ctx, String deviceName, TransportProtos.GetAttributeResponseMsg responseMsg) throws AdaptorException {
        return processConvertFromGatewayAttributeResponseMsg(ctx, deviceName, responseMsg, MqttTopics.GATEWAY_ATTRIBUTES_RESPONSE_TOPIC_V2_JSON);
    }

    @Override
    public Optional<MqttMessage> convertToPublish(MqttDeviceAwareSessionContext ctx, TransportProtos.AttributeUpdateNotificationMsg notificationMsg) throws AdaptorException {
        return Optional.of(createMqttPublishMsg(ctx, MqttTopics.DEVICE_ATTRIBUTES_TOPIC_V2_JSON, JsonConverter.toJson(notificationMsg)));
    }

    @Override
    public Optional<MqttMessage> convertToGatewayPublish(MqttDeviceAwareSessionContext ctx, String deviceName, TransportProtos.AttributeUpdateNotificationMsg notificationMsg) throws AdaptorException {
        JsonObject result = JsonConverter.getJsonObjectForGateway(deviceName, notificationMsg);
        return Optional.of(createMqttPublishMsg(ctx, MqttTopics.GATEWAY_ATTRIBUTES_TOPIC_V2_JSON, result));
    }

    @Override
    public Optional<MqttMessage> convertToPublish(MqttDeviceAwareSessionContext ctx, TransportProtos.ToDeviceRpcRequestMsg rpcRequest) throws AdaptorException {
        return Optional.of(createMqttPublishMsg(ctx, MqttTopics.DEVICE_RPC_REQUESTS_TOPIC_V2_JSON + rpcRequest.getRequestId(), JsonConverter.toJson(rpcRequest, false)));
    }

    @Override
    public Optional<MqttMessage> convertToGatewayPublish(MqttDeviceAwareSessionContext ctx, String deviceName, TransportProtos.ToDeviceRpcRequestMsg rpcRequest) {
        return Optional.of(createMqttPublishMsg(ctx, MqttTopics.GATEWAY_RPC_TOPIC_V2_JSON, JsonConverter.toGatewayJson(deviceName, rpcRequest)));
    }

    @Override
    public Optional<MqttMessage> convertToPublish(MqttDeviceAwareSessionContext ctx, TransportProtos.ToServerRpcResponseMsg rpcResponse) {
        return Optional.of(createMqttPublishMsg(ctx, MqttTopics.DEVICE_RPC_RESPONSE_TOPIC_V2_JSON + rpcResponse.getRequestId(), JsonConverter.toJson(rpcResponse)));
    }


}
