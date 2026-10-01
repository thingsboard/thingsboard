// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ai.transport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.codec.ServerSentEvent;
import org.thingsboard.ai.common.channel.ChannelFrame;
import org.thingsboard.ai.common.channel.ChannelFrameCodec;
import org.thingsboard.ai.common.channel.ChannelHello;
import org.thingsboard.ai.common.channel.ChannelProtocol;
import org.thingsboard.ai.common.channel.ChatEventPayload;
import org.thingsboard.ai.common.channel.ChatTurnRequest;
import org.thingsboard.ai.common.channel.TbAiChannelClient;
import org.thingsboard.ai.common.channel.TbHttpRequest;
import org.thingsboard.ai.common.channel.TbHttpResponse;
import org.thingsboard.ai.common.channel.TurnEnd;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.service.security.model.SecurityUser;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

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
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class ChannelTbAiTransportTest {

    static final UUID EXECUTION_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    ChannelFrameCodec codec = new ChannelFrameCodec(new ObjectMapper());
    List<ChannelFrame> receivedByAi = new CopyOnWriteArrayList<>();
    TbAiRequestExecutor requestExecutor = mock(TbAiRequestExecutor.class);
    TbAiChannelRegistry registry = spy(new TbAiChannelRegistry());
    TbAiTurnContext context = new TbAiTurnContext(mock(SecurityUser.class), "Bearer user-jwt", "de-DE", () -> "ai-token",
            new TbAiClientRequest("https://tb.example.com", Map.of()));
    DisposableServer aiServer;

    @AfterEach
    void tearDown() {
        if (aiServer != null) {
            aiServer.disposeNow();
        }
    }

    @Test
    void shouldRelayEventsAndServeThingsBoardRequests_whenTurnRuns() {
        // GIVEN
        startScriptedAi();
        UUID chatId = UUID.randomUUID();
        JsonNode request = JacksonUtil.newObjectNode().put("message", "Create a device");
        given(requestExecutor.execute(any(), any(), same(context), anyInt())).willAnswer(invocation -> {
            ChannelFrame frame = invocation.getArgument(0);
            var response = new TbHttpResponse(200, Map.of("Content-Type", List.of("application/json")), "{\"id\":\"d1\"}", "utf8");
            return Mono.just(ChannelFrame.reply(ChannelProtocol.TB_RESPONSE, frame, codec.toPayload(response)));
        });
        doCallRealMethod().when(requestExecutor).serve(any(), any(), any(), same(context), anyInt());

        // WHEN
        List<ServerSentEvent<String>> events = transport().sendChatMessage(chatId, request, context)
                .collectList()
                .block(Duration.ofSeconds(10));

        // THEN
        assertThat(events).extracting(ServerSentEvent::event, ServerSentEvent::comment)
                .containsExactly(
                        tuple(null, "heartbeat"),
                        tuple("assistantMessage", null),
                        tuple("toolExecutionRequested", null));
        assertThat(JacksonUtil.toJsonNode(events.get(1).data())).isEqualTo(JacksonUtil.toJsonNode("{\"message\":\"Working on it\"}"));

        ChannelHello hello = codec.fromPayload(frame(ChannelProtocol.HELLO).payload(), ChannelHello.class);
        assertThat(hello).isEqualTo(new ChannelHello(1, List.of("rest.v1"), "https://tb.example.com", null));
        ChatTurnRequest turn = codec.fromPayload(frame(ChannelProtocol.CHAT_SEND).payload(), ChatTurnRequest.class);
        assertThat(turn.chatId()).isEqualTo(chatId);
        assertThat(turn.request()).isEqualTo(request);
        TbHttpResponse tbResponse = codec.fromPayload(frame(ChannelProtocol.TB_RESPONSE).payload(), TbHttpResponse.class);
        assertThat(tbResponse.body()).isEqualTo("{\"id\":\"d1\"}");
        verify(registry).registerApproval(any(), any());
        await().atMost(Duration.ofSeconds(5)).until(() -> registry.findApprovalSession(EXECUTION_ID).isEmpty());
    }

    @Test
    void shouldSignalChannelUnavailable_whenServerHasNoChannelEndpoint() {
        // GIVEN
        aiServer = HttpServer.create().host("127.0.0.1").port(0)
                .route(routes -> routes.get(ChannelProtocol.WS_PATH, (req, res) -> res.status(404).send()))
                .bindNow();

        // WHEN-THEN
        assertThatThrownBy(() -> transport().sendChatMessage(UUID.randomUUID(), JacksonUtil.newObjectNode(), context)
                .collectList()
                .block(Duration.ofSeconds(10)))
                .isInstanceOf(TbAiChannelUnavailableException.class);
    }

    @Test
    void shouldEmitErrorEvent_whenServerRejectsToken() {
        // GIVEN
        aiServer = HttpServer.create().host("127.0.0.1").port(0)
                .route(routes -> routes.get(ChannelProtocol.WS_PATH, (req, res) -> res.status(401).send()))
                .bindNow();

        // WHEN
        List<ServerSentEvent<String>> events = transport().sendChatMessage(UUID.randomUUID(), JacksonUtil.newObjectNode(), context)
                .collectList()
                .block(Duration.ofSeconds(10));

        // THEN
        assertThat(events).hasSize(1);
        assertThat(events.getFirst().event()).isEqualTo("error");
        assertThat(JacksonUtil.toJsonNode(events.getFirst().data())).isEqualTo(JacksonUtil.toJsonNode("{\"message\":\"Unauthorized\"}"));
    }

    ChannelTbAiTransport transport() {
        return new ChannelTbAiTransport(new TbAiChannelClient("http://127.0.0.1:" + aiServer.port()), requestExecutor, registry);
    }

    ChannelFrame frame(String type) {
        return receivedByAi.stream().filter(frame -> type.equals(frame.type())).findFirst().orElseThrow();
    }

    void startScriptedAi() {
        aiServer = HttpServer.create().host("127.0.0.1").port(0)
                .route(routes -> routes.ws(ChannelProtocol.WS_PATH, (inbound, outbound) -> {
                    Sinks.Many<String> out = Sinks.many().unicast().onBackpressureBuffer();
                    inbound.receive().asString().subscribe(text -> {
                        ChannelFrame frame = codec.decode(text);
                        receivedByAi.add(frame);
                        switch (frame.type()) {
                            case ChannelProtocol.HELLO -> out.tryEmitNext(codec.encode(ChannelFrame.of(ChannelProtocol.HELLO,
                                    codec.toPayload(new ChannelHello(1, List.of("rest.v1"), null, 10_485_760L)))));
                            case ChannelProtocol.CHAT_SEND -> {
                                out.tryEmitNext(event("heartbeat", null));
                                out.tryEmitNext(event("assistantMessage", JacksonUtil.newObjectNode().put("message", "Working on it")));
                                out.tryEmitNext(codec.encode(new ChannelFrame(ChannelProtocol.TB_REQUEST, "r1", null, codec.toPayload(
                                        new TbHttpRequest("POST", "/api/device", null, Map.of(), "{}", "utf8", 5000L)))));
                            }
                            case ChannelProtocol.TB_RESPONSE -> {
                                out.tryEmitNext(event("toolExecutionRequested", JacksonUtil.newObjectNode()
                                        .put("executionId", EXECUTION_ID.toString()).put("needsApproval", true).put("message", "Save device")));
                                out.tryEmitNext(codec.encode(ChannelFrame.of(ChannelProtocol.TURN_END,
                                        codec.toPayload(new TurnEnd(TurnEnd.Status.COMPLETED)))));
                                out.tryEmitComplete();
                            }
                            default -> {}
                        }
                    });
                    return outbound.sendString(out.asFlux()).then().then(outbound.sendClose(1000, "Turn completed"));
                }))
                .bindNow();
    }

    String event(String name, JsonNode data) {
        return codec.encode(ChannelFrame.of(ChannelProtocol.CHAT_EVENT, codec.toPayload(new ChatEventPayload(name, data))));
    }

}
