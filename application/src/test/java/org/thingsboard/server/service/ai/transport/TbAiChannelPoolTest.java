// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ai.transport;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.thingsboard.ai.common.channel.ChannelProtocol;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class TbAiChannelPoolTest {

    static final List<String> MUX = List.of(ChannelProtocol.CAPABILITY_REST, ChannelProtocol.CAPABILITY_OPERATIONS,
            ChannelProtocol.CAPABILITY_API, ChannelProtocol.CAPABILITY_MUX);

    AtomicLong now = new AtomicLong(1_000_000_000L);
    FakeTbAiServer server = FakeTbAiServer.start(MUX, (connection, frame) -> {});
    TbAiChannelPool pool;

    @AfterEach
    void tearDown() {
        if (pool != null) {
            pool.shutdown();
        }
        server.close();
    }

    @Test
    void shouldShareOneHandshake_whenUserMakesConcurrentFirstCalls() {
        // GIVEN
        pool = pool(10);
        TbAiTurnContext context = context(user(), "https://tb.example.com", true);

        // WHEN
        CompletableFuture<TbAiPooledConnection> first = pool.acquire(context, false);
        CompletableFuture<TbAiPooledConnection> second = pool.acquire(context, false);

        // THEN
        assertThat(join(first)).isSameAs(join(second));
        assertThat(server.connections.get()).isEqualTo(1);
        assertThat(pool.pooledConnections()).isEqualTo(1);
    }

    @Test
    void shouldReuseChannel_whenSameUserCallsAgain() {
        // GIVEN
        pool = pool(10);
        TbAiTurnContext context = context(user(), "https://tb.example.com", true);
        TbAiPooledConnection connection = join(pool.acquire(context, false));
        connection.finish(null, now.get());

        // WHEN
        TbAiPooledConnection again = join(pool.acquire(context, false));

        // THEN
        assertThat(again).isSameAs(connection);
        assertThat(server.connections.get()).isEqualTo(1);
    }

    @Test
    void shouldOpenSeparateChannels_whenUsersOrExactOriginsDiffer() {
        // GIVEN
        pool = pool(10);
        SecurityUser user = user();
        TbAiPooledConnection mainOrigin = join(pool.acquire(context(user, "https://tb.example.com", true), false));

        // WHEN
        TbAiPooledConnection otherOrigin = join(pool.acquire(context(user, "https://other.example.com", true), false));
        TbAiPooledConnection anyOrigin = join(pool.acquire(context(user, "https://default.example.com", false), false));
        TbAiPooledConnection otherUser = join(pool.acquire(context(user(), "https://tb.example.com", true), false));

        // THEN
        assertThat(otherOrigin).isNotSameAs(mainOrigin);
        assertThat(anyOrigin).isIn(mainOrigin, otherOrigin);
        assertThat(otherUser).isNotSameAs(mainOrigin).isNotSameAs(otherOrigin);
        assertThat(server.connections.get()).isEqualTo(3);
    }

    @Test
    void shouldCloseChannel_whenIdleLongerThanTimeout() {
        // GIVEN
        pool = pool(10);
        TbAiPooledConnection connection = join(pool.acquire(context(user(), "https://tb.example.com", true), false));
        connection.finish(null, now.get());

        // WHEN
        now.addAndGet(Duration.ofSeconds(30).toNanos());
        pool.sweep();
        int openAfterHalfTimeout = server.openConnections.get();
        now.addAndGet(Duration.ofSeconds(31).toNanos());
        pool.sweep();

        // THEN
        assertThat(openAfterHalfTimeout).isEqualTo(1);
        await().atMost(5, TimeUnit.SECONDS).until(() -> server.openConnections.get() == 0);
        await().atMost(5, TimeUnit.SECONDS).until(() -> pool.pooledConnections() == 0);
    }

    @Test
    void shouldRetireBusyChannel_whenOlderThanMaxAge() {
        // GIVEN
        pool = pool(10);
        TbAiTurnContext context = context(user(), "https://tb.example.com", true);
        TbAiPooledConnection busy = join(pool.acquire(context, false));

        // WHEN
        now.addAndGet(Duration.ofMinutes(11).toNanos());
        pool.sweep();
        TbAiPooledConnection next = join(pool.acquire(context, false));
        busy.finish(null, now.get());

        // THEN
        assertThat(next).isNotSameAs(busy);
        await().atMost(5, TimeUnit.SECONDS).until(() -> server.openConnections.get() == 1);
    }

    @Test
    void shouldEvictIdleChannel_whenPoolIsFull() {
        // GIVEN
        pool = pool(1);
        TbAiPooledConnection idle = join(pool.acquire(context(user(), "https://tb.example.com", true), false));
        idle.finish(null, now.get());

        // WHEN
        TbAiPooledConnection next = join(pool.acquire(context(user(), "https://tb.example.com", true), false));

        // THEN
        assertThat(next.isPooled()).isTrue();
        await().atMost(5, TimeUnit.SECONDS).until(() -> server.openConnections.get() == 1);
    }

    @Test
    void shouldUseTransientChannel_whenPoolIsFullOfBusyChannels() {
        // GIVEN
        pool = pool(1);
        join(pool.acquire(context(user(), "https://tb.example.com", true), false));

        // WHEN
        TbAiPooledConnection overflow = join(pool.acquire(context(user(), "https://tb.example.com", true), false));
        overflow.finish(null, now.get());

        // THEN
        assertThat(overflow.isPooled()).isFalse();
        assertThat(pool.pooledConnections()).isEqualTo(1);
        await().atMost(5, TimeUnit.SECONDS).until(() -> server.openConnections.get() == 1);
    }

    @Test
    void shouldUseTransientChannel_whenCallIsBackgroundWork() {
        // GIVEN
        pool = pool(10);
        TbAiTurnContext context = TbAiTurnContext.background(TenantId.fromUUID(UUID.randomUUID()), new UserId(UUID.randomUUID()),
                () -> "token", TbAiClientRequest.withDefaultOrigin("https://tb.example.com"));

        // WHEN
        TbAiPooledConnection connection = join(pool.acquire(context, false));
        connection.finish(null, now.get());

        // THEN
        assertThat(connection.isPooled()).isFalse();
        assertThat(pool.pooledConnections()).isZero();
        await().atMost(5, TimeUnit.SECONDS).until(() -> server.openConnections.get() == 0);
    }

    @Test
    void shouldNotPoolChannel_whenServerIsNotMultiplexed() {
        // GIVEN
        server.close();
        server = FakeTbAiServer.start(List.of(ChannelProtocol.CAPABILITY_REST, ChannelProtocol.CAPABILITY_OPERATIONS), (connection, frame) -> {});
        pool = pool(10);
        TbAiTurnContext context = context(user(), "https://tb.example.com", true);

        // WHEN
        TbAiPooledConnection first = join(pool.acquire(context, false));
        TbAiPooledConnection second = join(pool.acquire(context, false));

        // THEN
        assertThat(first.isMultiplexed()).isFalse();
        assertThat(second).isNotSameAs(first);
        await().atMost(5, TimeUnit.SECONDS).until(() -> pool.pooledConnections() == 0);
    }

    @Test
    void shouldFailWithRejection_whenTokenIsRefused() {
        // GIVEN
        server.close();
        server = FakeTbAiServer.rejecting(401);
        pool = pool(10);

        // WHEN-THEN
        assertThatThrownBy(() -> join(pool.acquire(context(user(), "https://tb.example.com", true), false)))
                .hasCauseInstanceOf(TbAiChannelRejectedException.class);
        await().atMost(5, TimeUnit.SECONDS).until(() -> pool.pooledConnections() == 0);
    }

    @Test
    void shouldFailAsUnavailable_whenServerHasNoChannelEndpoint() {
        // GIVEN
        server.close();
        server = FakeTbAiServer.rejecting(404);
        pool = pool(10);

        // WHEN-THEN
        assertThatThrownBy(() -> join(pool.acquire(context(user(), "https://tb.example.com", true), false)))
                .hasCauseInstanceOf(TbAiChannelUnavailableException.class);
        await().atMost(5, TimeUnit.SECONDS).until(() -> pool.pooledConnections() == 0);
    }

    TbAiChannelPool pool(int maxConnections) {
        return new TbAiChannelPool(server.client(), maxConnections, 20, Duration.ofSeconds(60), Duration.ofMinutes(10), now::get, false);
    }

    static TbAiPooledConnection join(CompletableFuture<TbAiPooledConnection> future) {
        return future.orTimeout(10, TimeUnit.SECONDS).join();
    }

    static SecurityUser user() {
        SecurityUser user = mock(SecurityUser.class);
        given(user.getTenantId()).willReturn(TenantId.fromUUID(UUID.randomUUID()));
        given(user.getId()).willReturn(new UserId(UUID.randomUUID()));
        return user;
    }

    static TbAiTurnContext context(SecurityUser user, String origin, boolean exactOrigin) {
        return new TbAiTurnContext(user, null, null, () -> "token", new TbAiClientRequest(origin, Map.of(), exactOrigin));
    }

}
