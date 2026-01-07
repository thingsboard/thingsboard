/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.mqtt;

import io.netty.buffer.ByteBuf;
import io.netty.channel.EventLoop;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.util.concurrent.Promise;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.function.Consumer;

@Getter(AccessLevel.PACKAGE)
final class MqttPendingPublish {

    private final int messageId;
    private final Promise<Void> future;
    private final ByteBuf payload;
    private final MqttPublishMessage message;
    private final MqttQoS qos;

    @Getter(AccessLevel.NONE)
    private final RetransmissionHandler<MqttPublishMessage> publishRetransmissionHandler;
    @Getter(AccessLevel.NONE)
    private final RetransmissionHandler<MqttMessage> pubrelRetransmissionHandler;

    @Setter(AccessLevel.PACKAGE)
    private boolean sent = false;

    private MqttPendingPublish(
            int messageId,
            Promise<Void> future,
            ByteBuf payload,
            MqttPublishMessage message,
            MqttQoS qos,
            String ownerId,
            MqttClientConfig.RetransmissionConfig retransmissionConfig,
            PendingOperation pendingOperation
    ) {
        this.messageId = messageId;
        this.future = future;
        this.payload = payload;
        this.message = message;
        this.qos = qos;

        publishRetransmissionHandler = new RetransmissionHandler<>(retransmissionConfig, pendingOperation, ownerId);
        publishRetransmissionHandler.setOriginalMessage(message);
        pubrelRetransmissionHandler = new RetransmissionHandler<>(retransmissionConfig, pendingOperation, ownerId);
    }

    void startPublishRetransmissionTimer(EventLoop eventLoop, Consumer<Object> sendPacket) {
        publishRetransmissionHandler.setHandler(((fixedHeader, originalMessage) ->
                sendPacket.accept(new MqttPublishMessage(fixedHeader, originalMessage.variableHeader(), payload.retain()))));
        publishRetransmissionHandler.start(eventLoop);
    }

    void onPubackReceived() {
        publishRetransmissionHandler.stop();
    }

    void setPubrelMessage(MqttMessage pubrelMessage) {
        pubrelRetransmissionHandler.setOriginalMessage(pubrelMessage);
    }

    void startPubrelRetransmissionTimer(EventLoop eventLoop, Consumer<Object> sendPacket) {
        pubrelRetransmissionHandler.setHandler((fixedHeader, originalMessage) ->
                sendPacket.accept(new MqttMessage(fixedHeader, originalMessage.variableHeader())));
        pubrelRetransmissionHandler.start(eventLoop);
    }

    void onPubcompReceived() {
        pubrelRetransmissionHandler.stop();
    }

    void onChannelClosed() {
        publishRetransmissionHandler.stop();
        pubrelRetransmissionHandler.stop();
        if (payload != null) {
            payload.release();
        }
    }

    static Builder builder() {
        return new Builder();
    }

    static class Builder {

        private int messageId;
        private Promise<Void> future;
        private ByteBuf payload;
        private MqttPublishMessage message;
        private MqttQoS qos;
        private String ownerId;
        private MqttClientConfig.RetransmissionConfig retransmissionConfig;
        private PendingOperation pendingOperation;

        Builder messageId(int messageId) {
            this.messageId = messageId;
            return this;
        }

        Builder future(Promise<Void> future) {
            this.future = future;
            return this;
        }

        Builder payload(ByteBuf payload) {
            this.payload = payload;
            return this;
        }

        Builder message(MqttPublishMessage message) {
            this.message = message;
            return this;
        }

        Builder qos(MqttQoS qos) {
            this.qos = qos;
            return this;
        }

        Builder ownerId(String ownerId) {
            this.ownerId = ownerId;
            return this;
        }

        Builder retransmissionConfig(MqttClientConfig.RetransmissionConfig retransmissionConfig) {
            this.retransmissionConfig = retransmissionConfig;
            return this;
        }

        Builder pendingOperation(PendingOperation pendingOperation) {
            this.pendingOperation = pendingOperation;
            return this;
        }

        MqttPendingPublish build() {
            return new MqttPendingPublish(messageId, future, payload, message, qos, ownerId, retransmissionConfig, pendingOperation);
        }

    }

}
