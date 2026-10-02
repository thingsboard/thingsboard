// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ai.transport;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.thingsboard.ai.common.channel.ChannelError;
import org.thingsboard.ai.common.channel.ChannelFrame;
import org.thingsboard.ai.common.channel.ChannelProtocol;
import org.thingsboard.ai.common.channel.ChannelSession;
import org.thingsboard.ai.common.channel.OperationResult;
import org.thingsboard.ai.common.channel.SolutionOperationRequest;
import org.thingsboard.ai.common.channel.TbHttpRequest;
import org.thingsboard.ai.common.channel.TbHttpResponse;
import org.thingsboard.ai.common.client.TbAiClient.TbAiResponse;
import org.thingsboard.common.util.JacksonUtil;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

class ChannelTbAiOperationsTest {

    static final List<String> MUX = TbAiChannelPoolTest.MUX;

    UUID solutionId = UUID.randomUUID();
    TbAiRequestExecutor requestExecutor = mock(TbAiRequestExecutor.class);
    TbAiTurnContext context = TbAiChannelPoolTest.context(TbAiChannelPoolTest.user(), "https://tb.example.com", true);
    TbAiOperation install = new TbAiOperation(ChannelProtocol.SOLUTION_INSTALL, new SolutionOperationRequest(solutionId),
            () -> { throw new AssertionError("HTTP must not be used"); });
    FakeTbAiServer server;
    TbAiChannelPool pool;
    int maxConnections = 10;

    @AfterEach
    void tearDown() {
        if (pool != null) {
            pool.shutdown();
        }
        if (server != null) {
            server.close();
        }
    }

    @Test
    void shouldRunOperationAndServeThingsBoardRequests_whenServerIsMultiplexed() {
        // GIVEN
        server = FakeTbAiServer.start(MUX, (connection, frame) -> {
            switch (frame.type()) {
                case ChannelProtocol.SOLUTION_INSTALL -> connection.request(frame, ChannelProtocol.TB_REQUEST, "r1",
                        new TbHttpRequest("POST", "/api/dashboard", null, Map.of(), "{}", "utf8", 5000L));
                case ChannelProtocol.TB_RESPONSE -> connection.result(server.received(ChannelProtocol.SOLUTION_INSTALL).getFirst(),
                        new OperationResult(200, JacksonUtil.newObjectNode().put("mainDashboardId", "d1"), 3));
                default -> {}
            }
        });
        doAnswer(invocation -> {
            ChannelSession session = invocation.getArgument(0);
            ChannelFrame frame = invocation.getArgument(1);
            session.send(ChannelFrame.reply(ChannelProtocol.TB_RESPONSE, frame, server.codec.toPayload(new TbHttpResponse(200, Map.of(), "{}", "utf8"))));
            return null;
        }).when(requestExecutor).serve(any(), any(), any(), any(), anyInt());

        // WHEN
        TbAiResponse first = operations().execute(install, context);

        // THEN
        assertThat(first.isSuccess()).isTrue();
        assertThat(first.getValue()).isEqualTo(JacksonUtil.newObjectNode().put("mainDashboardId", "d1"));
        assertThat(first.getCreditsUsed()).isEqualTo(3);
        ChannelFrame start = server.received(ChannelProtocol.SOLUTION_INSTALL).getFirst();
        assertThat(server.received(ChannelProtocol.TB_RESPONSE).getFirst().op()).isEqualTo(start.id());
        assertThat(server.codec.fromPayload(start.payload(), SolutionOperationRequest.class)).isEqualTo(new SolutionOperationRequest(solutionId));
        assertThat(server.connections.get()).isEqualTo(1);
        assertThat(server.openConnections.get()).isEqualTo(1);
    }

    @Test
    void shouldRunConcurrentOperationsOnOneChannel_whenServerIsMultiplexed() throws Exception {
        // GIVEN
        CountDownLatch bothStarted = new CountDownLatch(2);
        server = FakeTbAiServer.start(MUX, (connection, frame) -> {
            bothStarted.countDown();
            CompletableFuture.runAsync(() -> {
                await(bothStarted);
                connection.result(frame, new OperationResult(200, JacksonUtil.toJsonNode("[]"), 0));
            });
        });
        ChannelTbAiOperations operations = operations();
        TbAiOperation list = new TbAiOperation(ChannelProtocol.CHAT_LIST, null, null);

        // WHEN
        CompletableFuture<TbAiResponse> first = CompletableFuture.supplyAsync(() -> operations.execute(list, context));
        CompletableFuture<TbAiResponse> second = CompletableFuture.supplyAsync(() -> operations.execute(list, context));

        // THEN
        assertThat(first.get(10, TimeUnit.SECONDS).isSuccess()).isTrue();
        assertThat(second.get(10, TimeUnit.SECONDS).isSuccess()).isTrue();
        assertThat(server.connections.get()).isEqualTo(1);
    }

    @Test
    void shouldServeManyUsersConcurrently_withOneChannelPerUser() throws Exception {
        // GIVEN
        maxConnections = 100;
        server = FakeTbAiServer.start(MUX, (connection, frame) -> CompletableFuture.runAsync(() ->
                connection.result(frame, new OperationResult(200, JacksonUtil.toJsonNode("[]"), 0))));
        ChannelTbAiOperations operations = operations();
        TbAiOperation list = new TbAiOperation(ChannelProtocol.CHAT_LIST, null, null);
        List<TbAiTurnContext> users = java.util.stream.IntStream.range(0, 50)
                .mapToObj(i -> TbAiChannelPoolTest.context(TbAiChannelPoolTest.user(), "https://tb.example.com", false))
                .toList();
        var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor();

        // WHEN
        List<CompletableFuture<TbAiResponse>> calls = java.util.stream.IntStream.range(0, 400)
                .mapToObj(i -> CompletableFuture.supplyAsync(() -> operations.execute(list, users.get(i % users.size())), executor))
                .toList();
        CompletableFuture.allOf(calls.toArray(CompletableFuture[]::new)).get(30, TimeUnit.SECONDS);

        // THEN
        assertThat(calls).allSatisfy(call -> assertThat(call.join().isSuccess()).isTrue());
        assertThat(server.connections.get()).isEqualTo(users.size());
        assertThat(pool.pooledConnections()).isEqualTo(users.size());
        executor.close();
    }

    @Test
    void shouldServeEveryCall_whenUsersOutnumberThePool() throws Exception {
        // GIVEN
        maxConnections = 5;
        server = FakeTbAiServer.start(MUX, (connection, frame) -> CompletableFuture.runAsync(() ->
                connection.result(frame, new OperationResult(200, JacksonUtil.toJsonNode("[]"), 0))));
        ChannelTbAiOperations operations = operations();
        TbAiOperation list = new TbAiOperation(ChannelProtocol.CHAT_LIST, null, null);
        var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor();

        // WHEN
        List<CompletableFuture<TbAiResponse>> calls = java.util.stream.IntStream.range(0, 100)
                .mapToObj(i -> CompletableFuture.supplyAsync(() -> operations.execute(list,
                        TbAiChannelPoolTest.context(TbAiChannelPoolTest.user(), "https://tb.example.com", false)), executor))
                .toList();
        CompletableFuture.allOf(calls.toArray(CompletableFuture[]::new)).get(30, TimeUnit.SECONDS);

        // THEN
        assertThat(calls).allSatisfy(call -> assertThat(call.join().isSuccess()).isTrue());
        assertThat(pool.pooledConnections()).isLessThanOrEqualTo(maxConnections);
        executor.close();
    }

    @Test
    void shouldRetryOnNewChannel_whenServerHasTooManyOperations() {
        // GIVEN
        AtomicBoolean refused = new AtomicBoolean();
        server = FakeTbAiServer.start(MUX, (connection, frame) -> {
            if (refused.compareAndSet(false, true)) {
                connection.refuse(frame, ChannelError.TOO_MANY_OPERATIONS);
            } else {
                connection.result(frame, new OperationResult(200, JacksonUtil.toJsonNode("[]"), 0));
            }
        });

        // WHEN
        TbAiResponse response = operations().execute(new TbAiOperation(ChannelProtocol.CHAT_LIST, null, null), context);

        // THEN
        assertThat(response.isSuccess()).isTrue();
        assertThat(server.connections.get()).isEqualTo(2);
    }

    @Test
    void shouldFailWithoutRetry_whenChannelDropsAfterOperationWasSent() {
        // GIVEN
        server = FakeTbAiServer.start(MUX, (connection, frame) -> connection.close(1011, "Crashed"));

        // WHEN
        TbAiResponse response = operations().execute(install, context);

        // THEN
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getError()).isEqualTo("Service unavailable");
        assertThat(server.received(ChannelProtocol.SOLUTION_INSTALL)).hasSize(1);
    }

    @Test
    void shouldRetryOnNewChannel_whenChannelGoesAwayBeforeAnyFrame() {
        // GIVEN
        AtomicBoolean wentAway = new AtomicBoolean();
        server = FakeTbAiServer.start(MUX, (connection, frame) -> {
            if (wentAway.compareAndSet(false, true)) {
                connection.close(ChannelProtocol.CLOSE_GOING_AWAY, "Server is shutting down");
            } else {
                connection.result(frame, new OperationResult(200, null, 0));
            }
        });

        // WHEN
        TbAiResponse response = operations().execute(install, context);

        // THEN
        assertThat(response.isSuccess()).isTrue();
        assertThat(server.connections.get()).isEqualTo(2);
    }

    @Test
    void shouldReturnErrorMessageFromBody_whenOperationFails() {
        // GIVEN
        server = FakeTbAiServer.start(MUX, (connection, frame) -> connection.result(frame,
                new OperationResult(404, JacksonUtil.newObjectNode().put("message", "Solution not found"), 0)));

        // WHEN
        TbAiResponse response = operations().execute(install, context);

        // THEN
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getError()).isEqualTo("Solution not found");
    }

    @Test
    void shouldRunOnSingleUseChannel_whenServerIsNotMultiplexed() {
        // GIVEN
        server = FakeTbAiServer.start(List.of(ChannelProtocol.CAPABILITY_REST, ChannelProtocol.CAPABILITY_OPERATIONS),
                (connection, frame) -> connection.result(frame, new OperationResult(200, null, 0)));

        // WHEN
        TbAiResponse first = operations().execute(install, context);
        TbAiResponse second = operations().execute(install, context);

        // THEN
        assertThat(first.isSuccess()).isTrue();
        assertThat(second.isSuccess()).isTrue();
        assertThat(server.connections.get()).isEqualTo(2);
        assertThat(server.received(ChannelProtocol.SOLUTION_INSTALL)).extracting(ChannelFrame::op).containsOnlyNulls();
    }

    @Test
    void shouldSignalUnsupported_whenServerLacksCapability() {
        // GIVEN
        server = FakeTbAiServer.start(List.of(ChannelProtocol.CAPABILITY_REST, ChannelProtocol.CAPABILITY_OPERATIONS,
                ChannelProtocol.CAPABILITY_MUX), (connection, frame) -> {});

        // WHEN-THEN
        assertThatThrownBy(() -> operations().execute(new TbAiOperation(ChannelProtocol.CHAT_LIST, null, null), context))
                .isInstanceOf(TbAiOperationsUnsupportedException.class);
        assertThat(server.received(ChannelProtocol.CHAT_LIST)).isEmpty();
    }

    @Test
    void shouldSignalChannelUnavailable_whenServerHasNoChannelEndpoint() {
        // GIVEN
        server = FakeTbAiServer.rejecting(404);

        // WHEN-THEN
        assertThatThrownBy(() -> operations().execute(install, context)).isInstanceOf(TbAiChannelUnavailableException.class);
    }

    @Test
    void shouldSignalRejection_whenServerRejectsToken() {
        // GIVEN
        server = FakeTbAiServer.rejecting(401);

        // WHEN-THEN
        assertThatThrownBy(() -> operations().execute(install, context))
                .isInstanceOf(TbAiChannelRejectedException.class)
                .hasMessage("Unauthorized");
    }

    @Test
    void shouldMapResultWithoutMessage_toReasonPhrase() {
        // WHEN
        TbAiResponse response = ChannelTbAiOperations.toResponse(new OperationResult(403, null, 0));

        // THEN
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getError()).isEqualTo("Forbidden");
    }

    ChannelTbAiOperations operations() {
        if (pool == null) {
            pool = new TbAiChannelPool(server.client(), maxConnections, 20, Duration.ofSeconds(60), Duration.ofMinutes(10), System::nanoTime, false);
        }
        return new ChannelTbAiOperations(pool, requestExecutor);
    }

    static void await(CountDownLatch latch) {
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
