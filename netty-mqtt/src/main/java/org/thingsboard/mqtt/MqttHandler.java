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

import java.util.concurrent.Future;
import io.netty.buffer.ByteBuf;

public interface MqttHandler {
    /**
    * Changing ListenableFuture to Future allows you to choose CompletableFuture,
    * which gives developers the freedom to choose the orchestration method. 
    * CompletableFuture is a newer, more evolved version that eliminates callback hell,
    * is easier to use, and comes with the JDK. jdk 1.8 was previously used with the Before JDK1.8, 
    * use ListenableFuture, after that, it is recommended to use CompletableFuture.
    * ListenableFuture It's still written that way.{@link MqttMessageListener#onMessage(topic, payload)}
    * public ListenableFuture<Void> onMessage(String topic, ByteBuf message) {
    *        log.info("MQTT message [{}], topic [{}]", message.toString(StandardCharsets.UTF_8), topic);
    *        events.add(new MqttEvent(topic, message.toString(StandardCharsets.UTF_8)));
    *       return Futures.immediateVoidFuture();
    *    }
    * CompletableFuture It's like this.
    * public CompletableFuture<Void> onMessage(String topic, ByteBuf message) {
    *        log.info("MQTT message [{}], topic [{}]", message.toString(StandardCharsets.UTF_8), topic);
    *       events.add(new MqttEvent(topic, message.toString(StandardCharsets.UTF_8)));
    *       return CompletableFuture.completedFuture(null);
    *    }
    * This change does not affect the system's current use of ListenableFuture so that it is free to choose between ListenableFuture or 
    * CompletableFuture in new development.
    */
    Future<Void> onMessage(String topic, ByteBuf payload);
}
