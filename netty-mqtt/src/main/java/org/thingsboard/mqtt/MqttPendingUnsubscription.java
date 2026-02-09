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

import io.netty.channel.EventLoop;
import io.netty.handler.codec.mqtt.MqttUnsubscribeMessage;
import io.netty.util.concurrent.Promise;
import lombok.AccessLevel;
import lombok.Getter;

import java.util.function.Consumer;

@Getter(AccessLevel.PACKAGE)
final class MqttPendingUnsubscription {

    private final Promise<Void> future;
    private final String topic;

    @Getter(AccessLevel.NONE)
    private final RetransmissionHandler<MqttUnsubscribeMessage> retransmissionHandler;

    private MqttPendingUnsubscription(
            Promise<Void> future,
            String topic,
            MqttUnsubscribeMessage unsubscribeMessage,
            String ownerId,
            MqttClientConfig.RetransmissionConfig retransmissionConfig,
            PendingOperation operation
    ) {
        this.future = future;
        this.topic = topic;

        retransmissionHandler = new RetransmissionHandler<>(retransmissionConfig, operation, ownerId);
        retransmissionHandler.setOriginalMessage(unsubscribeMessage);
    }

    void startRetransmissionTimer(EventLoop eventLoop, Consumer<Object> sendPacket) {
        retransmissionHandler.setHandler((fixedHeader, originalMessage) ->
                sendPacket.accept(new MqttUnsubscribeMessage(fixedHeader, originalMessage.variableHeader(), originalMessage.payload())));
        retransmissionHandler.start(eventLoop);
    }

    void onUnsubackReceived() {
        retransmissionHandler.stop();
    }

    void onChannelClosed() {
        retransmissionHandler.stop();
    }

    static Builder builder() {
        return new Builder();
    }

    static class Builder {

        private Promise<Void> future;
        private String topic;
        private MqttUnsubscribeMessage unsubscribeMessage;
        private String ownerId;
        private PendingOperation pendingOperation;
        private MqttClientConfig.RetransmissionConfig retransmissionConfig;

        Builder future(Promise<Void> future) {
            this.future = future;
            return this;
        }

        Builder topic(String topic) {
            this.topic = topic;
            return this;
        }

        Builder unsubscribeMessage(MqttUnsubscribeMessage unsubscribeMessage) {
            this.unsubscribeMessage = unsubscribeMessage;
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

        MqttPendingUnsubscription build() {
            return new MqttPendingUnsubscription(future, topic, unsubscribeMessage, ownerId, retransmissionConfig, pendingOperation);
        }

    }

}
