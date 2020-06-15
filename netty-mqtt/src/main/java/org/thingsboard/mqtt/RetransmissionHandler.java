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
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.util.concurrent.ScheduledFuture;

import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

final class RetransmissionHandler<T extends MqttMessage> {

    private ScheduledFuture<?> timer;
    private int timeout = 10;
    private BiConsumer<MqttFixedHeader, T> handler;
    private T originalMessage;

    void start(EventLoop eventLoop){
        if(eventLoop == null){
            throw new NullPointerException("eventLoop");
        }
        if(this.handler == null){
            throw new NullPointerException("handler");
        }
        this.timeout = 10;
        this.startTimer(eventLoop);
    }

    private void startTimer(EventLoop eventLoop){
        this.timer = eventLoop.schedule(() -> {
            this.timeout += 5;
            boolean isDup = this.originalMessage.fixedHeader().isDup();
            if(this.originalMessage.fixedHeader().messageType() == MqttMessageType.PUBLISH && this.originalMessage.fixedHeader().qosLevel() != MqttQoS.AT_MOST_ONCE){
                isDup = true;
            }
            MqttFixedHeader fixedHeader = new MqttFixedHeader(this.originalMessage.fixedHeader().messageType(), isDup, this.originalMessage.fixedHeader().qosLevel(), this.originalMessage.fixedHeader().isRetain(), this.originalMessage.fixedHeader().remainingLength());
            handler.accept(fixedHeader, originalMessage);
            startTimer(eventLoop);
        }, timeout, TimeUnit.SECONDS);
    }

    void stop(){
        if(this.timer != null){
            this.timer.cancel(true);
        }
    }

    void setHandle(BiConsumer<MqttFixedHeader, T> runnable) {
        this.handler = runnable;
    }

    void setOriginalMessage(T originalMessage) {
        this.originalMessage = originalMessage;
    }
}
