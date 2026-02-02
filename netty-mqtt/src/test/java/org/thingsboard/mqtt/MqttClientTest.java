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

import com.google.common.util.concurrent.Futures;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.hivemq.HiveMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.thingsboard.common.util.AbstractListeningExecutor;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@Testcontainers
class MqttClientTest {

    final int randomPort = 0;

    @Container
    HiveMQContainer broker = new HiveMQContainer(DockerImageName.parse("hivemq/hivemq-ce").withTag("2025.2"));

    MqttTestProxy proxy;

    MqttClient client;

    AbstractListeningExecutor handlerExecutor;

    @BeforeAll
    static void init() {
        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.PARANOID);
    }

    @BeforeEach
    void setup() {
        handlerExecutor = new AbstractListeningExecutor() {
            @Override
            protected int getThreadPollSize() {
                return 1;
            }
        };
        handlerExecutor.init();
    }

    @AfterEach
    void cleanup() {
        if (client != null) {
            client.disconnect();
            client = null;
        }
        if (proxy != null) {
            proxy.stop();
            proxy = null;
        }
        handlerExecutor.destroy();
        handlerExecutor = null;
    }

    @Test
    void testConnectToBroker() {
        // GIVEN
        var clientConfig = new MqttClientConfig();
        clientConfig.setOwnerId("Test[ConnectToBroker]");
        clientConfig.setClientId("connect");

        client = MqttClient.create(clientConfig, null, handlerExecutor);

        // WHEN
        Promise<MqttConnectResult> connectFuture = client.connect(broker.getHost(), broker.getMqttPort());

        // THEN
        assertThat(connectFuture).isNotNull();

        Awaitility.await("waiting for client to connect")
                .atMost(Duration.ofSeconds(10L))
                .until(connectFuture::isDone);

        assertThat(connectFuture.isSuccess()).isTrue();

        MqttConnectResult actualConnectResult = connectFuture.getNow();
        assertThat(actualConnectResult).isNotNull();
        assertThat(actualConnectResult.isSuccess()).isTrue();
        assertThat(actualConnectResult.getReturnCode()).isEqualTo(MqttConnectReturnCode.CONNECTION_ACCEPTED);

        assertThat(client.isConnected()).isTrue();
    }

    @Test
    void testDisconnectFromBroker() {
        // GIVEN
        var clientConfig = new MqttClientConfig();
        clientConfig.setOwnerId("Test[Disconnect]");
        clientConfig.setClientId("disconnect");

        client = MqttClient.create(clientConfig, null, handlerExecutor);

        connect(broker.getHost(), broker.getMqttPort());

        // WHEN
        client.disconnect();

        // THEN
        Awaitility.await("waiting for client to disconnect")
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(client.isConnected()).isFalse());
    }

    @Test
    void testDisconnectDueToKeepAliveIfNoActivity() {
        // GIVEN
        proxy = MqttTestProxy.builder()
                .localPort(randomPort)
                .brokerHost(broker.getHost())
                .brokerPort(broker.getMqttPort())
                .brokerToClientInterceptor(msg -> msg.fixedHeader().messageType() != MqttMessageType.PINGRESP) // drop all ping responses to simulate broker down
                .build();

        int idleTimeoutSeconds = 2;

        var clientConfig = new MqttClientConfig();
        clientConfig.setOwnerId("Test[KeepAliveDisconnect]");
        clientConfig.setClientId("no-activity-disconnect");
        clientConfig.setTimeoutSeconds(idleTimeoutSeconds);
        clientConfig.setReconnect(false); // disable auto reconnect
        client = MqttClient.create(clientConfig, null, handlerExecutor);

        // WHEN-THEN
        connect(broker.getHost(), proxy.getPort());

        // no activity...

        Awaitility.await("waiting for client to disconnect")
                .pollDelay(Duration.ofSeconds(idleTimeoutSeconds * 2)) // 2 seconds to wait for the first idle event and then 2 seconds for scheduled disconnect to fire
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(client.isConnected()).isFalse());
    }

    @Test
    void testRetransmission() {
        // GIVEN
        proxy = MqttTestProxy.builder()
                .localPort(randomPort)
                .brokerHost(broker.getHost())
                .brokerPort(broker.getMqttPort())
                .brokerToClientInterceptor(msg -> msg.fixedHeader().messageType() != MqttMessageType.PUBACK) // drop all pubacks to allow retransmission to happen
                .build();

        // create client
        var clientConfig = new MqttClientConfig();
        clientConfig.setOwnerId("Test[Retransmission]");
        clientConfig.setClientId("retransmission");
        clientConfig.setRetransmissionConfig(new MqttClientConfig.RetransmissionConfig(1, 1000L, 0d));
        client = MqttClient.create(clientConfig, null, handlerExecutor);

        // connect to a broker
        connect(broker.getHost(), proxy.getPort());

        // subscribe to a topic
        String topic = "test-topic";
        List<ByteBuf> receivedMessages = Collections.synchronizedList(new ArrayList<>(2));
        Future<Void> subscribeFuture = client.on(topic, (__, payload) -> {
            receivedMessages.add(payload);
            return Futures.immediateVoidFuture();
        });
        Awaitility.await("waiting for client to subscribe to a topic")
                .atMost(Duration.ofSeconds(10L))
                .until(subscribeFuture::isDone);

        // WHEN
        // publish a message
        ByteBuf message = PooledByteBufAllocator.DEFAULT.buffer().writeBytes("test message".getBytes(StandardCharsets.UTF_8));
        client.publish(topic, message, MqttQoS.AT_LEAST_ONCE);

        // THEN
        // wait enough time so that retransmission happens and stops
        // if retransmission works incorrectly waiting 10 seconds allows for additional retransmissions to happen
        try {
            Awaitility.await("wait up to 10s, stop early if too many messages")
                    .atMost(Duration.ofSeconds(10L))
                    .pollInterval(Duration.ofMillis(100))
                    .until(() -> receivedMessages.size() > 2);
        } catch (ConditionTimeoutException __) {
            // didn't exceed 2 messages
        }

        assertThat(receivedMessages).size().describedAs("incorrect number of messages received, expected 2 (original plus one retransmitted)").isEqualTo(2);
    }

    private void connect(String host, int port) {
        Promise<MqttConnectResult> connectFuture = client.connect(host, port);
        Awaitility.await("waiting for client to connect")
                .atMost(Duration.ofSeconds(10L))
                .until(connectFuture::isSuccess);
    }

}
