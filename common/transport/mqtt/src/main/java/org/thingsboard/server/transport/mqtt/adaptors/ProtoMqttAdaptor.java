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

import com.google.protobuf.InvalidProtocolBufferException;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.transport.adaptor.AdaptorException;
import org.thingsboard.server.common.transport.adaptor.ProtoConverter;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.mqtt.session.MqttDeviceAwareSessionContext;

import java.util.Optional;
import java.util.UUID;

/**
 * @author Andrew Shvayka
 */
@Component
@Slf4j
public class ProtoMqttAdaptor implements MqttTransportAdaptor {

    @Override
    public TransportProtos.PostTelemetryMsg convertToPostTelemetry(MqttDeviceAwareSessionContext ctx, MqttPublishMessage inbound) throws AdaptorException {
        byte[] bytes = getBytesToProto(inbound);
        try {
            return ProtoConverter.convertToTelemetryProto(bytes);
        } catch (InvalidProtocolBufferException | IllegalArgumentException e) {
            throw new AdaptorException(e);
        }
    }

    public TransportProtos.PostTelemetryMsg convertToPostTelemetryArray(MqttDeviceAwareSessionContext ctx, MqttPublishMessage inbound) throws AdaptorException {
        byte[] bytes = getBytesToProto(inbound);
        try {
            return ProtoConverter.convertToTelemetryArrayProto(bytes);
        } catch (InvalidProtocolBufferException | IllegalArgumentException e) {
            throw new AdaptorException(e);
        }
    }

    @Override
    public TransportProtos.PostAttributeMsg convertToPostAttributes(MqttDeviceAwareSessionContext ctx, MqttPublishMessage inbound) throws AdaptorException {
        byte[] bytes = getBytesToProto(inbound);
        try {
            return ProtoConverter.convertToAttributesProto(bytes);
        } catch (InvalidProtocolBufferException | IllegalArgumentException e) {
            throw new AdaptorException(e);
        }
    }

    @Override
    public TransportProtos.GetAttributeRequestMsg convertToGetAttributes(MqttDeviceAwareSessionContext ctx, MqttPublishMessage inbound) throws AdaptorException {
        byte[] bytes = getBytesToProto(inbound);
        return null;
    }

    @Override
    public TransportProtos.ToDeviceRpcResponseMsg convertToDeviceRpcResponse(MqttDeviceAwareSessionContext ctx, MqttPublishMessage mqttMsg) throws AdaptorException {
        byte[] bytes = getBytesToProto(mqttMsg);
        return null;
    }

    @Override
    public TransportProtos.ToServerRpcRequestMsg convertToServerRpcRequest(MqttDeviceAwareSessionContext ctx, MqttPublishMessage mqttMsg) throws AdaptorException {
        byte[] bytes = getBytesToProto(mqttMsg);
        return null;
    }

    @Override
    public TransportProtos.ClaimDeviceMsg convertToClaimDevice(MqttDeviceAwareSessionContext ctx, MqttPublishMessage inbound) throws AdaptorException {
        byte[] bytes = getBytesToProto(inbound);
        try {
            return ProtoConverter.convertToClaimDeviceProto(ctx.getDeviceId(), bytes);
        } catch (InvalidProtocolBufferException e) {
            throw new AdaptorException(e);
        }
    }

    @Override
    public Optional<MqttMessage> convertToPublish(MqttDeviceAwareSessionContext ctx, TransportProtos.GetAttributeResponseMsg responseMsg) throws AdaptorException {
        return Optional.empty();
    }

    @Override
    public Optional<MqttMessage> convertToGatewayPublish(MqttDeviceAwareSessionContext ctx, String deviceName, TransportProtos.GetAttributeResponseMsg responseMsg) throws AdaptorException {
        return Optional.empty();
    }

    @Override
    public Optional<MqttMessage> convertToPublish(MqttDeviceAwareSessionContext ctx, TransportProtos.AttributeUpdateNotificationMsg notificationMsg) throws AdaptorException {
        return Optional.empty();
    }

    @Override
    public Optional<MqttMessage> convertToGatewayPublish(MqttDeviceAwareSessionContext ctx, String deviceName, TransportProtos.AttributeUpdateNotificationMsg notificationMsg) throws AdaptorException {
        return Optional.empty();
    }

    @Override
    public Optional<MqttMessage> convertToPublish(MqttDeviceAwareSessionContext ctx, TransportProtos.ToDeviceRpcRequestMsg rpcRequest) throws AdaptorException {
        return Optional.empty();
    }

    @Override
    public Optional<MqttMessage> convertToGatewayPublish(MqttDeviceAwareSessionContext ctx, String deviceName, TransportProtos.ToDeviceRpcRequestMsg rpcRequest) throws AdaptorException {
        return Optional.empty();
    }

    @Override
    public Optional<MqttMessage> convertToPublish(MqttDeviceAwareSessionContext ctx, TransportProtos.ToServerRpcResponseMsg rpcResponse) throws AdaptorException {
        return Optional.empty();
    }

    private byte[] getBytesToProto(MqttPublishMessage inbound) {
        ByteBuf byteBuf = inbound.payload();
        byte[] bytes = new byte[byteBuf.readableBytes()];
        int readerIndex = byteBuf.readerIndex();
        byteBuf.getBytes(readerIndex, bytes);
        return bytes;
    }


}
