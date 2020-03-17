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
import io.netty.handler.codec.mqtt.MqttUnsubscribeMessage;
import io.netty.util.concurrent.Promise;

import java.util.function.Consumer;

final class MqttPendingUnsubscription {

    private final Promise<Void> future;
    private final String topic;

    private final RetransmissionHandler<MqttUnsubscribeMessage> retransmissionHandler = new RetransmissionHandler<>();

    MqttPendingUnsubscription(Promise<Void> future, String topic, MqttUnsubscribeMessage unsubscribeMessage) {
        this.future = future;
        this.topic = topic;

        this.retransmissionHandler.setOriginalMessage(unsubscribeMessage);
    }

    Promise<Void> getFuture() {
        return future;
    }

    String getTopic() {
        return topic;
    }

    void startRetransmissionTimer(EventLoop eventLoop, Consumer<Object> sendPacket) {
        this.retransmissionHandler.setHandle((fixedHeader, originalMessage) ->
                sendPacket.accept(new MqttUnsubscribeMessage(fixedHeader, originalMessage.variableHeader(), originalMessage.payload())));
        this.retransmissionHandler.start(eventLoop);
    }

    void onUnsubackReceived(){
        this.retransmissionHandler.stop();
    }
}
