// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ai.transport;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.codec.ServerSentEvent;
import org.thingsboard.ai.common.channel.ChannelError;
import org.thingsboard.ai.common.channel.ChannelFrame;
import org.thingsboard.ai.common.channel.ChannelHello;
import org.thingsboard.ai.common.channel.ChannelProtocol;
import org.thingsboard.ai.common.channel.ChatEventPayload;
import org.thingsboard.ai.common.channel.ChatTurnRequest;
import org.thingsboard.ai.common.channel.TbHttpRequest;
import org.thingsboard.ai.common.channel.TbHttpResponse;
import org.thingsboard.ai.common.channel.TurnEnd;
import org.thingsboard.common.util.JacksonUtil;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;

class ChannelTbAiTransportTest {

    static final UUID EXECUTION_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    static final List<String> MUX = TbAiChannelPoolTest.MUX;

    TbAiRequestExecutor requestExecutor = mock(TbAiRequestExecutor.class);
    TbAiChannelRegistry registry = new TbAiChannelRegistry();
    TbAiTurnContext context = TbAiChannelPoolTest.context(TbAiChannelPoolTest.user(), "https://tb.example.com", true);
    JsonNode request = JacksonUtil.newObjectNode().put("message", "Create a device");
    FakeTbAiServer server;
    TbAiChannelPool pool;

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
    void shouldRelayEventsServeRequestsAndKeepChannel_whenServerIsMultiplexed() {
        // GIVEN
        server = FakeTbAiServer.start(MUX, this::assistantWithToolCall);
        stubExecutor();

        // WHEN
        List<ServerSentEvent<String>> first = turn();
        List<ServerSentEvent<String>> second = turn();

        // THEN
        assertThat(first).extracting(ServerSentEvent::event, ServerSentEvent::comment).containsExactly(
                tuple(null, "heartbeat"), tuple("assistantMessage", null), tuple("toolExecutionRequested", null));
        assertThat(second).hasSize(3);
        assertThat(server.connections.get()).isEqualTo(1);
        ChannelHello hello = server.codec.fromPayload(server.received(ChannelProtocol.HELLO).getFirst().payload(), ChannelHello.class);
        assertThat(hello).isEqualTo(new ChannelHello(1, TbAiPooledConnection.CLIENT_CAPABILITIES, "https://tb.example.com", null));
        ChannelFrame send = server.received(ChannelProtocol.CHAT_SEND).getFirst();
        ChatTurnRequest turnRequest = server.codec.fromPayload(send.payload(), ChatTurnRequest.class);
        assertThat(turnRequest.request()).isEqualTo(request);
        assertThat(turnRequest.acceptLanguage()).isEqualTo("de-DE");
        assertThat(server.received(ChannelProtocol.TB_RESPONSE)).extracting(ChannelFrame::op)
                .containsExactlyElementsOf(server.received(ChannelProtocol.CHAT_SEND).stream().map(ChannelFrame::id).toList());
        await().atMost(Duration.ofSeconds(5)).until(() -> registry.findApprovalRoute(EXECUTION_ID).isEmpty());
    }

    @Test
    void shouldRunTurnOnSingleUseChannel_whenServerIsNotMultiplexed() {
        // GIVEN
        server = FakeTbAiServer.start(List.of(ChannelProtocol.CAPABILITY_REST), this::assistantWithToolCall);
        stubExecutor();

        // WHEN
        List<ServerSentEvent<String>> first = turn();
        List<ServerSentEvent<String>> second = turn();

        // THEN
        assertThat(first).hasSize(3);
        assertThat(second).hasSize(3);
        assertThat(server.connections.get()).isEqualTo(2);
        assertThat(server.received(ChannelProtocol.TB_RESPONSE)).extracting(ChannelFrame::op).containsOnlyNulls();
    }

    @Test
    void shouldRetryOnNewChannel_whenServerIsDraining() {
        // GIVEN
        AtomicBoolean drained = new AtomicBoolean();
        server = FakeTbAiServer.start(MUX, (connection, frame) -> {
            if (ChannelProtocol.CHAT_SEND.equals(frame.type())) {
                if (drained.compareAndSet(false, true)) {
                    connection.refuse(frame, ChannelError.DRAINING);
                } else {
                    connection.sendFor(frame, ChannelProtocol.CHAT_EVENT, new ChatEventPayload("assistantMessage",
                            JacksonUtil.newObjectNode().put("message", "Hi")));
                    connection.sendFor(frame, ChannelProtocol.TURN_END, new TurnEnd(TurnEnd.Status.COMPLETED));
                }
            }
        });

        // WHEN
        List<ServerSentEvent<String>> events = turn();

        // THEN
        assertThat(events).extracting(ServerSentEvent::event).containsExactly("assistantMessage");
        assertThat(server.connections.get()).isEqualTo(2);
    }

    @Test
    void shouldCancelOnlyItsTurn_whenBrowserLeaves() {
        // GIVEN
        server = FakeTbAiServer.start(MUX, (connection, frame) -> {
            if (ChannelProtocol.CHAT_SEND.equals(frame.type())) {
                connection.sendFor(frame, ChannelProtocol.CHAT_EVENT, new ChatEventPayload("heartbeat", null));
            }
        });
        pool = pool();
        var transport = new ChannelTbAiTransport(pool, requestExecutor, registry);
        AtomicBoolean heartbeat = new AtomicBoolean();

        // WHEN
        Disposable subscription = transport.sendChatMessage(UUID.randomUUID(), request, context).subscribe(event -> heartbeat.set(true));
        await().atMost(Duration.ofSeconds(5)).untilTrue(heartbeat);
        subscription.dispose();

        // THEN
        await().atMost(Duration.ofSeconds(5)).until(() -> !server.received(ChannelProtocol.CANCEL).isEmpty());
        ChannelFrame cancel = server.received(ChannelProtocol.CANCEL).getFirst();
        assertThat(cancel.op()).isEqualTo(server.received(ChannelProtocol.CHAT_SEND).getFirst().id());
        assertThat(server.openConnections.get()).isEqualTo(1);
    }

    @Test
    void shouldSignalChannelUnavailable_whenServerHasNoChannelEndpoint() {
        // GIVEN
        server = FakeTbAiServer.rejecting(404);

        // WHEN-THEN
        assertThatThrownBy(this::turn).isInstanceOf(TbAiChannelUnavailableException.class);
    }

    @Test
    void shouldEmitErrorEvent_whenServerRejectsToken() {
        // GIVEN
        server = FakeTbAiServer.rejecting(401);

        // WHEN
        List<ServerSentEvent<String>> events = turn();

        // THEN
        assertThat(events).hasSize(1);
        assertThat(events.getFirst().event()).isEqualTo("error");
        assertThat(JacksonUtil.toJsonNode(events.getFirst().data())).isEqualTo(JacksonUtil.toJsonNode("{\"message\":\"Unauthorized\"}"));
    }

    List<ServerSentEvent<String>> turn() {
        if (pool == null) {
            pool = pool();
        }
        TbAiTurnContext turnContext = new TbAiTurnContext(context.tenantId(), context.userId(), context.user(), "Bearer user-jwt",
                "de-DE", context.tokenProvider(), new TbAiClientRequest("https://tb.example.com", Map.of()));
        return new ChannelTbAiTransport(pool, requestExecutor, registry)
                .sendChatMessage(UUID.randomUUID(), request, turnContext)
                .collectList()
                .block(Duration.ofSeconds(10));
    }

    TbAiChannelPool pool() {
        return new TbAiChannelPool(server.client(), 10, 20, Duration.ofSeconds(60), Duration.ofMinutes(10), System::nanoTime, false);
    }

    void stubExecutor() {
        given(requestExecutor.execute(any(), any(), any(), anyInt())).willAnswer(invocation -> {
            ChannelFrame frame = invocation.getArgument(0);
            var response = new TbHttpResponse(200, Map.of("Content-Type", List.of("application/json")), "{\"id\":\"d1\"}", "utf8");
            return Mono.just(ChannelFrame.reply(ChannelProtocol.TB_RESPONSE, frame, server.codec.toPayload(response)));
        });
        doCallRealMethod().when(requestExecutor).serve(any(), any(), any(), any(), anyInt());
    }

    void assistantWithToolCall(FakeTbAiServer.Connection connection, ChannelFrame frame) {
        switch (frame.type()) {
            case ChannelProtocol.CHAT_SEND -> {
                connection.sendFor(frame, ChannelProtocol.CHAT_EVENT, new ChatEventPayload("heartbeat", null));
                connection.sendFor(frame, ChannelProtocol.CHAT_EVENT, new ChatEventPayload("assistantMessage",
                        JacksonUtil.newObjectNode().put("message", "Working on it")));
                connection.request(frame, ChannelProtocol.TB_REQUEST, "r-" + frame.id(),
                        new TbHttpRequest("POST", "/api/device", null, Map.of(), "{}", "utf8", 5000L));
            }
            case ChannelProtocol.TB_RESPONSE -> {
                ChannelFrame start = server.received(ChannelProtocol.CHAT_SEND).stream()
                        .filter(send -> frame.replyTo().equals("r-" + send.id())).findFirst().orElseThrow();
                connection.sendFor(start, ChannelProtocol.CHAT_EVENT, new ChatEventPayload("toolExecutionRequested",
                        JacksonUtil.newObjectNode().put("executionId", EXECUTION_ID.toString()).put("needsApproval", true)));
                connection.sendFor(start, ChannelProtocol.TURN_END, new TurnEnd(TurnEnd.Status.COMPLETED));
                if (!connection.isMultiplexed()) {
                    connection.close(ChannelProtocol.CLOSE_NORMAL, "Turn completed");
                }
            }
            default -> {}
        }
    }

}
