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
package org.thingsboard.server.transport.mqtt;

import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;

public class MqttTransportServiceTest {

    private static final String HOST = "127.0.0.1";

    private MqttTransportService service;
    private ServerSocket occupiedSocket;
    private int occupiedPort;

    @BeforeEach
    public void setUp() throws Exception {
        occupiedSocket = new ServerSocket(0, 50, InetAddress.getByName(HOST));
        occupiedPort = occupiedSocket.getLocalPort();

        service = new MqttTransportService();
        ReflectionTestUtils.setField(service, "host", HOST);
        ReflectionTestUtils.setField(service, "port", occupiedPort);
        ReflectionTestUtils.setField(service, "sslEnabled", false);
        ReflectionTestUtils.setField(service, "sslHost", HOST);
        ReflectionTestUtils.setField(service, "sslPort", 0);
        ReflectionTestUtils.setField(service, "leakDetectorLevel", "DISABLED");
        ReflectionTestUtils.setField(service, "bossGroupThreadCount", 1);
        ReflectionTestUtils.setField(service, "workerGroupThreadCount", 1);
        ReflectionTestUtils.setField(service, "keepAlive", true);
        ReflectionTestUtils.setField(service, "context", mock(MqttTransportContext.class));
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (occupiedSocket != null && !occupiedSocket.isClosed()) {
            occupiedSocket.close();
        }
    }

    @Test
    public void whenPlainBindFails_thenInitThrowsAndReleasesNettyResources() {
        assertThatThrownBy(() -> service.init())
                .isInstanceOf(BindException.class);

        EventLoopGroup boss = (EventLoopGroup) ReflectionTestUtils.getField(service, "bossGroup");
        EventLoopGroup worker = (EventLoopGroup) ReflectionTestUtils.getField(service, "workerGroup");

        assertThat(boss).isNotNull();
        assertThat(worker).isNotNull();
        assertThat(boss.isShuttingDown()).isTrue();
        assertThat(worker.isShuttingDown()).isTrue();

        await().atMost(30, TimeUnit.SECONDS).until(boss::isTerminated);
        await().atMost(30, TimeUnit.SECONDS).until(worker::isTerminated);
    }

    @Test
    public void whenSslBindFailsAfterPlainBound_thenInitThrowsAndClosesPlainChannelAndReleasesNettyResources() {
        ReflectionTestUtils.setField(service, "port", 0);
        ReflectionTestUtils.setField(service, "sslEnabled", true);
        ReflectionTestUtils.setField(service, "sslPort", occupiedPort);

        assertThatThrownBy(() -> service.init())
                .isInstanceOf(BindException.class);

        Channel serverChannel = (Channel) ReflectionTestUtils.getField(service, "serverChannel");
        Channel sslServerChannel = (Channel) ReflectionTestUtils.getField(service, "sslServerChannel");
        EventLoopGroup boss = (EventLoopGroup) ReflectionTestUtils.getField(service, "bossGroup");
        EventLoopGroup worker = (EventLoopGroup) ReflectionTestUtils.getField(service, "workerGroup");

        assertThat(serverChannel).isNotNull();
        assertThat(sslServerChannel).isNull();
        assertThat(boss).isNotNull();
        assertThat(worker).isNotNull();

        await().atMost(10, TimeUnit.SECONDS).until(() -> !serverChannel.isOpen());

        assertThat(boss.isShuttingDown()).isTrue();
        assertThat(worker.isShuttingDown()).isTrue();
        await().atMost(30, TimeUnit.SECONDS).until(boss::isTerminated);
        await().atMost(30, TimeUnit.SECONDS).until(worker::isTerminated);
    }
}
