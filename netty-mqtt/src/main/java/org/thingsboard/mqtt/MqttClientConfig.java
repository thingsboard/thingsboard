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

import io.netty.channel.Channel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.mqtt.MqttVersion;
import io.netty.handler.ssl.SslContext;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import lombok.Getter;
import lombok.Setter;

import java.util.Random;

@SuppressWarnings({"WeakerAccess", "unused"})
public final class MqttClientConfig {

    @Getter
    private final SslContext sslContext;
    private final String randomClientId;

    @Getter
    @Setter
    private String ownerId; // [TenantId][IntegrationId] or [TenantId][RuleNodeId] for exceptions logging purposes
    @Nonnull
    @Getter
    private String clientId;
    @Getter
    private int timeoutSeconds = 60;
    @Getter
    private MqttVersion protocolVersion = MqttVersion.MQTT_3_1;
    @Nullable
    @Getter
    @Setter
    private String username = null;
    @Nullable
    @Getter
    @Setter
    private String password = null;
    @Getter
    @Setter
    private boolean cleanSession = true;
    @Nullable
    @Getter
    @Setter
    private MqttLastWill lastWill;
    @Setter
    @Getter
    private Class<? extends Channel> channelClass = NioSocketChannel.class;

    @Getter
    @Setter
    private boolean reconnect = true;
    @Getter
    private long reconnectDelay = 1L;
    @Getter
    private int maxBytesInMessage = 8092;

    @Getter
    @Setter
    private RetransmissionConfig retransmissionConfig;

    public record RetransmissionConfig(int maxAttempts, long initialDelayMillis, double jitterFactor) {

        public RetransmissionConfig {
            if (maxAttempts < 0) {
                throw new IllegalArgumentException("Max retransmission attempts (maxAttempts) must be zero or greater, but was " + maxAttempts);
            }
            if (initialDelayMillis < 0) {
                throw new IllegalArgumentException("Initial retransmission delay (initialDelayMillis) must be zero or greater, but was " + initialDelayMillis);
            }
            if (jitterFactor < 0) {
                throw new IllegalArgumentException("Jitter factor (jitterFactor) must be zero or greater, but was " + jitterFactor);
            }
        }

    }

    public MqttClientConfig() {
        this(null);
    }

    public MqttClientConfig(SslContext sslContext) {
        this.sslContext = sslContext;
        Random random = new Random();
        StringBuilder id = new StringBuilder("netty-mqtt/");
        String[] options = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".split("");
        for (int i = 0; i < 8; i++) {
            id.append(options[random.nextInt(options.length)]);
        }
        this.clientId = id.toString();
        this.randomClientId = id.toString();
    }

    public void setClientId(@Nullable String clientId) {
        if (clientId == null) {
            this.clientId = randomClientId;
        } else {
            this.clientId = clientId;
        }
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        if (timeoutSeconds != -1 && timeoutSeconds <= 0) {
            throw new IllegalArgumentException("timeoutSeconds must be > 0 or -1");
        }
        this.timeoutSeconds = timeoutSeconds;
    }

    public void setProtocolVersion(MqttVersion protocolVersion) {
        if (protocolVersion == null) {
            throw new NullPointerException("protocolVersion");
        }
        this.protocolVersion = protocolVersion;
    }

    /**
     * Sets the reconnect delay in seconds. Defaults to 1 second.
     * @param reconnectDelay
     * @throws IllegalArgumentException if reconnectDelay is smaller than 1.
     */
    public void setReconnectDelay(long reconnectDelay) {
        if (reconnectDelay <= 0) {
            throw new IllegalArgumentException("reconnectDelay must be > 0");
        }
        this.reconnectDelay = reconnectDelay;
    }

    /**
     * Sets the maximum number of bytes in the message for the {@link io.netty.handler.codec.mqtt.MqttDecoder}.
     * Default value is 8092 as specified by Netty. The absolute maximum size is 256MB as set by the MQTT spec.
     *
     * @param maxBytesInMessage
     * @throws IllegalArgumentException if maxBytesInMessage is smaller than 1 or greater than 256_000_000.
     */
    public void setMaxBytesInMessage(int maxBytesInMessage) {
        if (maxBytesInMessage <= 0 || maxBytesInMessage > 256_000_000) {
            throw new IllegalArgumentException("maxBytesInMessage must be > 0 or < 256_000_000");
        }
        this.maxBytesInMessage = maxBytesInMessage;
    }

}
