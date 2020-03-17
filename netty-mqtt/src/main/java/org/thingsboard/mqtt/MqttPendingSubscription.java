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
package org.thingsboard.mqtt;

import io.netty.channel.EventLoop;
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import io.netty.util.concurrent.Promise;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

final class MqttPendingSubscription {

    private final Promise<Void> future;
    private final String topic;
    private final Set<MqttPendingHandler> handlers = new HashSet<>();
    private final MqttSubscribeMessage subscribeMessage;

    private final RetransmissionHandler<MqttSubscribeMessage> retransmissionHandler = new RetransmissionHandler<>();

    private boolean sent = false;

    MqttPendingSubscription(Promise<Void> future, String topic, MqttSubscribeMessage message) {
        this.future = future;
        this.topic = topic;
        this.subscribeMessage = message;

        this.retransmissionHandler.setOriginalMessage(message);
    }

    Promise<Void> getFuture() {
        return future;
    }

    String getTopic() {
        return topic;
    }

    boolean isSent() {
        return sent;
    }

    void setSent(boolean sent) {
        this.sent = sent;
    }

    MqttSubscribeMessage getSubscribeMessage() {
        return subscribeMessage;
    }

    void addHandler(MqttHandler handler, boolean once){
        this.handlers.add(new MqttPendingHandler(handler, once));
    }

    Set<MqttPendingHandler> getHandlers() {
        return handlers;
    }

    void startRetransmitTimer(EventLoop eventLoop, Consumer<Object> sendPacket) {
        if(this.sent){ //If the packet is sent, we can start the retransmit timer
            this.retransmissionHandler.setHandle((fixedHeader, originalMessage) ->
                    sendPacket.accept(new MqttSubscribeMessage(fixedHeader, originalMessage.variableHeader(), originalMessage.payload())));
            this.retransmissionHandler.start(eventLoop);
        }
    }

    void onSubackReceived(){
        this.retransmissionHandler.stop();
    }

    final class MqttPendingHandler {
        private final MqttHandler handler;
        private final boolean once;

        MqttPendingHandler(MqttHandler handler, boolean once) {
            this.handler = handler;
            this.once = once;
        }

        MqttHandler getHandler() {
            return handler;
        }

        boolean isOnce() {
            return once;
        }
    }
}
