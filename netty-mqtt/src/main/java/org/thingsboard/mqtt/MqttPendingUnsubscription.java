// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
