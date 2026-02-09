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
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPublishVariableHeader;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.util.concurrent.ScheduledFuture;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

@Slf4j
@RequiredArgsConstructor
final class RetransmissionHandler<T extends MqttMessage> {

    private final MqttClientConfig.RetransmissionConfig config;
    private final PendingOperation pendingOperation;

    private volatile boolean stopped;
    private ScheduledFuture<?> timer;
    private int attemptCount = 0;

    @Setter
    private BiConsumer<MqttFixedHeader, T> handler;

    // the three fields below are used for logging only
    private final String ownerId;
    private String originalMessageId;
    private long totalWaitingTimeMillis;

    private T originalMessage;

    void setOriginalMessage(T originalMessage) {
        this.originalMessage = originalMessage;
        var variableHeader = originalMessage.variableHeader();
        if (variableHeader instanceof MqttMessageIdVariableHeader messageIdVariableHeader) {
            originalMessageId = String.valueOf(messageIdVariableHeader.messageId());
        } else if (variableHeader instanceof MqttPublishVariableHeader publishVariableHeader) {
            originalMessageId = String.valueOf(publishVariableHeader.packetId());
        } else {
            originalMessageId = "N/A";
        }
    }

    void start(EventLoop eventLoop) {
        if (eventLoop == null) {
            throw new NullPointerException("eventLoop");
        }
        if (handler == null) {
            throw new NullPointerException("handler");
        }
        log.debug("{}MessageID[{}] Starting retransmission handler", ownerId, originalMessageId);
        startTimer(eventLoop);
    }

    private void startTimer(EventLoop eventLoop) {
        if (stopped || pendingOperation.isCancelled()) {
            return;
        }

        // Calculate the base delay using exponential backoff.
        // For attemptCount == 0, delay = initial delay; for each subsequent attempt, the base delay doubles.
        long baseDelay = config.initialDelayMillis() * (long) Math.pow(2, attemptCount);
        // Apply jitter: random factor between (1 - jitterFactor) and (1 + jitterFactor).
        double minFactor = 1.0 - config.jitterFactor();
        double maxFactor = 1.0 + config.jitterFactor();
        double randomFactor = config.jitterFactor() == 0 ? 1 : ThreadLocalRandom.current().nextDouble(minFactor, maxFactor);
        long delayMillisWithJitter = (long) (baseDelay * randomFactor);
        totalWaitingTimeMillis += delayMillisWithJitter;

        timer = eventLoop.schedule(() -> {
            if (stopped || pendingOperation.isCancelled()) {
                return;
            }

            attemptCount++;
            if (attemptCount > config.maxAttempts()) {
                log.debug(
                        "{}MessageID[{}] Gave up after {} retransmission attempts; waited a total of {} ms without receiving acknowledgement",
                        ownerId, originalMessageId, config.maxAttempts(), totalWaitingTimeMillis
                );
                stop();
                pendingOperation.onMaxRetransmissionAttemptsReached();
                return;
            }

            log.debug("{}MessageID[{}] Retransmission attempt #{} out of {}", ownerId, originalMessageId, attemptCount, config.maxAttempts());

            var originalFixedHeader = originalMessage.fixedHeader();
            var newFixedHeader = new MqttFixedHeader(
                    originalFixedHeader.messageType(),
                    isDup(originalFixedHeader),
                    originalFixedHeader.qosLevel(),
                    originalFixedHeader.isRetain(),
                    originalFixedHeader.remainingLength()
            );
            handler.accept(newFixedHeader, originalMessage);
            startTimer(eventLoop);
        }, delayMillisWithJitter, TimeUnit.MILLISECONDS);
    }

    private static boolean isDup(MqttFixedHeader originalFixedHeader) {
        return originalFixedHeader.isDup() || (originalFixedHeader.messageType() == MqttMessageType.PUBLISH && originalFixedHeader.qosLevel() != MqttQoS.AT_MOST_ONCE);
    }

    void stop() {
        log.debug("{}MessageID[{}] Stopping retransmission handler", ownerId, originalMessageId);
        stopped = true;
        if (timer != null) {
            timer.cancel(true);
        }
    }

}
