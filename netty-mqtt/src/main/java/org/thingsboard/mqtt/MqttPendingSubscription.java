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
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import io.netty.util.concurrent.Promise;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNullElseGet;

@Getter(AccessLevel.PACKAGE)
final class MqttPendingSubscription {

    private final Promise<Void> future;
    private final String topic;
    private final Set<MqttPendingHandler> handlers;
    private final MqttSubscribeMessage subscribeMessage;

    @Getter(AccessLevel.NONE)
    private final RetransmissionHandler<MqttSubscribeMessage> retransmissionHandler;

    @Setter(AccessLevel.PACKAGE)
    private boolean sent = false;

    private MqttPendingSubscription(
            Promise<Void> future,
            String topic,
            Set<MqttPendingHandler> handlers,
            MqttSubscribeMessage subscribeMessage,
            String ownerId,
            MqttClientConfig.RetransmissionConfig retransmissionConfig,
            PendingOperation operation
    ) {
        this.future = future;
        this.topic = topic;
        this.handlers = requireNonNullElseGet(handlers, HashSet::new);
        this.subscribeMessage = subscribeMessage;

        retransmissionHandler = new RetransmissionHandler<>(retransmissionConfig, operation, ownerId);
        retransmissionHandler.setOriginalMessage(subscribeMessage);
    }

    record MqttPendingHandler(MqttHandler handler, boolean once) {}

    void addHandler(MqttHandler handler, boolean once) {
        handlers.add(new MqttPendingHandler(handler, once));
    }

    void startRetransmitTimer(EventLoop eventLoop, Consumer<Object> sendPacket) {
        if (sent) { // If the packet is sent, we can start the retransmission timer
            retransmissionHandler.setHandler((fixedHeader, originalMessage) ->
                    sendPacket.accept(new MqttSubscribeMessage(fixedHeader, originalMessage.variableHeader(), originalMessage.payload())));
            retransmissionHandler.start(eventLoop);
        }
    }

    void onSubackReceived() {
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
        private Set<MqttPendingHandler> handlers;
        private MqttSubscribeMessage subscribeMessage;
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

        Builder handlers(Set<MqttPendingHandler> handlers) {
            this.handlers = handlers;
            return this;
        }

        Builder subscribeMessage(MqttSubscribeMessage subscribeMessage) {
            this.subscribeMessage = subscribeMessage;
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

        MqttPendingSubscription build() {
            return new MqttPendingSubscription(future, topic, handlers, subscribeMessage, ownerId, retransmissionConfig, pendingOperation);
        }

    }

}
