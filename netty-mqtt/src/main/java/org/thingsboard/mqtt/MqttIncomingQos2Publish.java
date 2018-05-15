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

import io.netty.channel.EventLoop;
import io.netty.handler.codec.mqtt.*;

import java.util.function.Consumer;

final class MqttIncomingQos2Publish {

    private final MqttPublishMessage incomingPublish;

    private final RetransmissionHandler<MqttMessage> retransmissionHandler = new RetransmissionHandler<>();

    MqttIncomingQos2Publish(MqttPublishMessage incomingPublish, MqttMessage originalMessage) {
        this.incomingPublish = incomingPublish;

        this.retransmissionHandler.setOriginalMessage(originalMessage);
    }

    MqttPublishMessage getIncomingPublish() {
        return incomingPublish;
    }

    void startPubrecRetransmitTimer(EventLoop eventLoop, Consumer<Object> sendPacket) {
        this.retransmissionHandler.setHandle((fixedHeader, originalMessage) ->
                sendPacket.accept(new MqttMessage(fixedHeader, originalMessage.variableHeader())));
        this.retransmissionHandler.start(eventLoop);
    }

    void onPubrelReceived() {
        this.retransmissionHandler.stop();
    }
}
