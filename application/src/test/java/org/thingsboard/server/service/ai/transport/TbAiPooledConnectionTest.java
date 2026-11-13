// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ai.transport;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.thingsboard.ai.common.channel.ChannelFrame;
import org.thingsboard.ai.common.channel.ChannelFrameCodec;
import org.thingsboard.ai.common.channel.ChannelHello;
import org.thingsboard.ai.common.channel.ChannelProtocol;
import org.thingsboard.ai.common.channel.ChannelSession;
import org.thingsboard.ai.common.channel.TbAiChannelClient;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

class TbAiPooledConnectionTest {

    ChannelFrameCodec codec = new ChannelFrameCodec(new ObjectMapper());
    TbAiChannelPool pool = new TbAiChannelPool(mock(TbAiChannelClient.class), "4.4.0", 10, 20, Duration.ofSeconds(60),
            Duration.ofMinutes(10), System::nanoTime, false);
    TbAiChannelPool.PoolKey key = new TbAiChannelPool.PoolKey(TenantId.fromUUID(UUID.randomUUID()), new UserId(UUID.randomUUID()));

    @Test
    void shouldCloseSessionWithoutHello_whenSessionArrivesAfterHandshakeTimedOut() {
        // GIVEN
        TbAiPooledConnection connection = connection();
        ChannelSession session = openSession();
        connection.failed(new TimeoutException());

        // WHEN
        connection.opened(session);

        // THEN
        then(session).should(never()).send(any());
        then(session).should().close(ChannelProtocol.CLOSE_NORMAL, "Handshake failed");
        assertThat(connection.isRemoved()).isTrue();
        assertThat(pool.pooledConnections()).isZero();
    }

    @Test
    void shouldCloseOpenedSession_whenHandshakeTimesOutBeforeHelloReply() {
        // GIVEN
        TbAiPooledConnection connection = connection();
        ChannelSession session = openSession();
        connection.opened(session);

        // WHEN
        connection.failed(new TimeoutException());
        connection.onFrame(session, ChannelFrame.of(ChannelProtocol.HELLO,
                codec.toPayload(new ChannelHello(ChannelProtocol.VERSION, null, null, null))));

        // THEN
        then(session).should().close(ChannelProtocol.CLOSE_NORMAL, "Handshake failed");
        assertThat(connection.ready()).isCompletedExceptionally();
        assertThat(connection.isRemoved()).isTrue();
        assertThat(pool.pooledConnections()).isZero();
    }

    @Test
    void shouldSendHelloWithoutCapabilities_whenSessionOpens() {
        // GIVEN
        TbAiPooledConnection connection = connection();
        ChannelSession session = openSession();

        // WHEN
        connection.opened(session);
        connection.onFrame(session, ChannelFrame.of(ChannelProtocol.HELLO,
                codec.toPayload(new ChannelHello(ChannelProtocol.VERSION, null, null, 1024L))));

        // THEN
        then(session).should().send(ChannelFrame.of(ChannelProtocol.HELLO,
                codec.toPayload(new ChannelHello(ChannelProtocol.VERSION, List.of(), "https://tb.example.com", null))));
        then(session).should(never()).close(anyInt(), anyString());
        assertThat(connection.ready()).isCompletedWithValue(connection);
        assertThat(connection.maxFrameBytes()).isEqualTo(1024);
    }

    TbAiPooledConnection connection() {
        return new TbAiPooledConnection(key, "https://tb.example.com", false, 20, codec, pool, System.nanoTime());
    }

    static ChannelSession openSession() {
        ChannelSession session = mock(ChannelSession.class);
        given(session.isOpen()).willReturn(true);
        return session;
    }

}
