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
package org.thingsboard.server.transport.mqtt.adaptors;

import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import org.thingsboard.server.common.transport.TransportAdaptor;
import org.thingsboard.server.common.transport.adaptor.AdaptorException;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.mqtt.session.DeviceSessionCtx;

import java.util.Optional;

/**
 * @author Andrew Shvayka
 */
public interface MqttTransportAdaptor extends TransportAdaptor<DeviceSessionCtx, MqttMessage, MqttMessage> {

    TransportProtos.PostTelemetryMsg convertToPostTelemetry(DeviceSessionCtx ctx, MqttPublishMessage inbound) throws AdaptorException;

    TransportProtos.PostAttributeMsg convertToPostAttributes(DeviceSessionCtx ctx, MqttPublishMessage inbound) throws AdaptorException;

    TransportProtos.GetAttributeRequestMsg convertToGetAttributes(DeviceSessionCtx ctx, MqttPublishMessage inbound) throws AdaptorException;

    Optional<MqttMessage> convertToPublish(DeviceSessionCtx ctx, TransportProtos.GetAttributeResponseMsg responseMsg) throws AdaptorException;
}
