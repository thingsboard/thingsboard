/**
 * Copyright © 2016-2021 The Thingsboard Authors
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

import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.transport.adaptor.AdaptorException;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.mqtt.session.MqttDeviceAwareSessionContext;

import java.util.Optional;

@Data
@AllArgsConstructor
@Slf4j
public class BackwardCompatibilityAdaptor implements MqttTransportAdaptor {

    private static final String BACKWARD_COMPATIBILITY_ENABLED = "Other payload formats compatibility enabled! Trying to convert ";

    private MqttTransportAdaptor main;
    private MqttTransportAdaptor backup;

    @Override
    public TransportProtos.PostTelemetryMsg convertToPostTelemetry(MqttDeviceAwareSessionContext ctx, MqttPublishMessage inbound) throws AdaptorException {
        try {
            return main.convertToPostTelemetry(ctx, inbound);
        } catch (AdaptorException e) {
            log.trace(BACKWARD_COMPATIBILITY_ENABLED + "post telemetry request msg using {} ...", backup.getClass().getSimpleName());
            return backup.convertToPostTelemetry(ctx, inbound);
        }
    }

    @Override
    public TransportProtos.PostAttributeMsg convertToPostAttributes(MqttDeviceAwareSessionContext ctx, MqttPublishMessage inbound) throws AdaptorException {
        try {
            return main.convertToPostAttributes(ctx, inbound);
        } catch (AdaptorException e) {
            log.trace(BACKWARD_COMPATIBILITY_ENABLED + "post attributes request msg using {} ...", backup.getClass().getSimpleName());
            return backup.convertToPostAttributes(ctx, inbound);
        }
    }

    @Override
    public TransportProtos.GetAttributeRequestMsg convertToGetAttributes(MqttDeviceAwareSessionContext ctx, MqttPublishMessage inbound, String topicBase) throws AdaptorException {
        try {
            return main.convertToGetAttributes(ctx, inbound, topicBase);
        } catch (AdaptorException e) {
            log.trace(BACKWARD_COMPATIBILITY_ENABLED + "get attributes request msg using {} ...", backup.getClass().getSimpleName());
            return backup.convertToGetAttributes(ctx, inbound, topicBase);
        }
    }

    @Override
    public TransportProtos.ToDeviceRpcResponseMsg convertToDeviceRpcResponse(MqttDeviceAwareSessionContext ctx, MqttPublishMessage mqttMsg, String topicBase) throws AdaptorException {
        try {
            return main.convertToDeviceRpcResponse(ctx, mqttMsg, topicBase);
        } catch (AdaptorException e) {
            log.trace(BACKWARD_COMPATIBILITY_ENABLED + "to device rpc response msg using {} ...", backup.getClass().getSimpleName());
            return backup.convertToDeviceRpcResponse(ctx, mqttMsg, topicBase);
        }
    }

    @Override
    public TransportProtos.ToServerRpcRequestMsg convertToServerRpcRequest(MqttDeviceAwareSessionContext ctx, MqttPublishMessage mqttMsg, String topicBase) throws AdaptorException {
        try {
            return main.convertToServerRpcRequest(ctx, mqttMsg, topicBase);
        } catch (AdaptorException e) {
            log.trace(BACKWARD_COMPATIBILITY_ENABLED + "to server rpc request msg using {} ...", backup.getClass().getSimpleName());
            return backup.convertToServerRpcRequest(ctx, mqttMsg, topicBase);
        }
    }

    @Override
    public TransportProtos.ClaimDeviceMsg convertToClaimDevice(MqttDeviceAwareSessionContext ctx, MqttPublishMessage inbound) throws AdaptorException {
        try {
            return main.convertToClaimDevice(ctx, inbound);
        } catch (AdaptorException e) {
            log.trace(BACKWARD_COMPATIBILITY_ENABLED + "claim device request msg using {} ...", backup.getClass().getSimpleName());
            return backup.convertToClaimDevice(ctx, inbound);
        }
    }

    @Override
    public Optional<MqttMessage> convertToPublish(MqttDeviceAwareSessionContext ctx, TransportProtos.GetAttributeResponseMsg responseMsg, String topicBase, boolean useBackupAdaptorByDefault) throws AdaptorException {
        return useBackupAdaptorByDefault ? backup.convertToPublish(ctx, responseMsg, topicBase, false) : main.convertToPublish(ctx, responseMsg, topicBase, false);
    }

    @Override
    public Optional<MqttMessage> convertToGatewayPublish(MqttDeviceAwareSessionContext ctx, String deviceName, TransportProtos.GetAttributeResponseMsg responseMsg) throws AdaptorException {
        return main.convertToGatewayPublish(ctx, deviceName, responseMsg);
    }

    @Override
    public Optional<MqttMessage> convertToPublish(MqttDeviceAwareSessionContext ctx, TransportProtos.AttributeUpdateNotificationMsg notificationMsg, String topic, boolean useBackupAdaptorByDefault) throws AdaptorException {
        return useBackupAdaptorByDefault ? backup.convertToPublish(ctx, notificationMsg, topic, false) : main.convertToPublish(ctx, notificationMsg, topic, false);
    }

    @Override
    public Optional<MqttMessage> convertToGatewayPublish(MqttDeviceAwareSessionContext ctx, String deviceName, TransportProtos.AttributeUpdateNotificationMsg notificationMsg) throws AdaptorException {
        return main.convertToGatewayPublish(ctx, deviceName, notificationMsg);
    }

    @Override
    public Optional<MqttMessage> convertToPublish(MqttDeviceAwareSessionContext ctx, TransportProtos.ToDeviceRpcRequestMsg rpcRequest, String topicBase, boolean useBackupAdaptorByDefault) throws AdaptorException {
        return useBackupAdaptorByDefault ? backup.convertToPublish(ctx, rpcRequest, topicBase, false) : main.convertToPublish(ctx, rpcRequest, topicBase, false);
    }

    @Override
    public Optional<MqttMessage> convertToGatewayPublish(MqttDeviceAwareSessionContext ctx, String deviceName, TransportProtos.ToDeviceRpcRequestMsg rpcRequest) throws AdaptorException {
        return main.convertToGatewayPublish(ctx, deviceName, rpcRequest);
    }

    @Override
    public Optional<MqttMessage> convertToPublish(MqttDeviceAwareSessionContext ctx, TransportProtos.ToServerRpcResponseMsg rpcResponse, String topicBase, boolean useBackupAdaptorByDefault) throws AdaptorException {
        return useBackupAdaptorByDefault ? backup.convertToPublish(ctx, rpcResponse, topicBase, false) : main.convertToPublish(ctx, rpcResponse, topicBase, false);
    }

    @Override
    public Optional<MqttMessage> convertToPublish(MqttDeviceAwareSessionContext ctx, byte[] firmwareChunk, String requestId, int chunk, OtaPackageType firmwareType) throws AdaptorException {
        return main.convertToPublish(ctx, firmwareChunk, requestId, chunk, firmwareType);
    }
}
