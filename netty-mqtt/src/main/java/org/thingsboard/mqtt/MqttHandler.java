// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
