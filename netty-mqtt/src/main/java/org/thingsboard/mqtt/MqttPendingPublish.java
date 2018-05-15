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
package org.thingsboard.mqtt;

import io.netty.buffer.ByteBuf;
import io.netty.channel.EventLoop;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.util.concurrent.Promise;

import java.util.function.Consumer;

final class MqttPendingPublish {

    private final int messageId;
    private final Promise<Void> future;
    private final ByteBuf payload;
    private final MqttPublishMessage message;
    private final MqttQoS qos;

    private final RetransmissionHandler<MqttPublishMessage> publishRetransmissionHandler = new RetransmissionHandler<>();
    private final RetransmissionHandler<MqttMessage> pubrelRetransmissionHandler = new RetransmissionHandler<>();

    private boolean sent = false;

    MqttPendingPublish(int messageId, Promise<Void> future, ByteBuf payload, MqttPublishMessage message, MqttQoS qos) {
        this.messageId = messageId;
        this.future = future;
        this.payload = payload;
        this.message = message;
        this.qos = qos;

        this.publishRetransmissionHandler.setOriginalMessage(message);
    }

    int getMessageId() {
        return messageId;
    }

    Promise<Void> getFuture() {
        return future;
    }

    ByteBuf getPayload() {
        return payload;
    }

    boolean isSent() {
        return sent;
    }

    void setSent(boolean sent) {
        this.sent = sent;
    }

    MqttPublishMessage getMessage() {
        return message;
    }

    MqttQoS getQos() {
        return qos;
    }

    void startPublishRetransmissionTimer(EventLoop eventLoop, Consumer<Object> sendPacket) {
        this.publishRetransmissionHandler.setHandle(((fixedHeader, originalMessage) ->
                sendPacket.accept(new MqttPublishMessage(fixedHeader, originalMessage.variableHeader(), this.payload.retain()))));
        this.publishRetransmissionHandler.start(eventLoop);
    }

    void onPubackReceived() {
        this.publishRetransmissionHandler.stop();
    }

    void setPubrelMessage(MqttMessage pubrelMessage) {
        this.pubrelRetransmissionHandler.setOriginalMessage(pubrelMessage);
    }

    void startPubrelRetransmissionTimer(EventLoop eventLoop, Consumer<Object> sendPacket) {
        this.pubrelRetransmissionHandler.setHandle((fixedHeader, originalMessage) ->
                sendPacket.accept(new MqttMessage(fixedHeader, originalMessage.variableHeader())));
        this.pubrelRetransmissionHandler.start(eventLoop);
    }

    void onPubcompReceived() {
        this.pubrelRetransmissionHandler.stop();
    }
}
