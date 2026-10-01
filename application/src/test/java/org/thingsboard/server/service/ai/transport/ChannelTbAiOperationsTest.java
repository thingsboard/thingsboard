// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ai.transport;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.thingsboard.ai.common.channel.ChannelFrame;
import org.thingsboard.ai.common.channel.ChannelFrameCodec;
import org.thingsboard.ai.common.channel.ChannelHello;
import org.thingsboard.ai.common.channel.ChannelProtocol;
import org.thingsboard.ai.common.channel.ChannelSession;
import org.thingsboard.ai.common.channel.OperationResult;
import org.thingsboard.ai.common.channel.SolutionOperationRequest;
import org.thingsboard.ai.common.channel.TbAiChannelClient;
import org.thingsboard.ai.common.channel.TbHttpRequest;
import org.thingsboard.ai.common.channel.TbHttpResponse;
import org.thingsboard.ai.common.client.TbAiClient.TbAiResponse;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.service.security.model.SecurityUser;
import reactor.core.publisher.Sinks;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ChannelTbAiOperationsTest {

    UUID solutionId = UUID.randomUUID();
    ChannelFrameCodec codec = new ChannelFrameCodec(new ObjectMapper());
    List<ChannelFrame> receivedByAi = new CopyOnWriteArrayList<>();
    TbAiRequestExecutor requestExecutor = mock(TbAiRequestExecutor.class);
    TbAiTurnContext context = new TbAiTurnContext(mock(SecurityUser.class), "Bearer user-jwt", null, () -> "ai-token",
            new TbAiClientRequest("https://tb.example.com", Map.of()));
    TbAiOperation operation = new TbAiOperation(ChannelProtocol.SOLUTION_INSTALL, new SolutionOperationRequest(solutionId),
            () -> { throw new AssertionError("HTTP must not be used"); });
    DisposableServer aiServer;

    @AfterEach
    void tearDown() {
        if (aiServer != null) {
            aiServer.disposeNow();
        }
    }

    @Test
    void shouldRunOperationAndServeThingsBoardRequests_whenServerSupportsOperations() {
        // GIVEN
        startScriptedAi(List.of(ChannelProtocol.CAPABILITY_REST, ChannelProtocol.CAPABILITY_OPERATIONS),
                request -> new OperationResult(200, JacksonUtil.newObjectNode().put("mainDashboardId", "d1"), 3));
        doAnswer(invocation -> {
            ChannelSession session = invocation.getArgument(0);
            ChannelFrame frame = invocation.getArgument(1);
            session.send(ChannelFrame.reply(ChannelProtocol.TB_RESPONSE, frame,
                    codec.toPayload(new TbHttpResponse(200, Map.of(), "{}", "utf8"))));
            return null;
        }).when(requestExecutor).serve(any(), any(), any(), same(context), anyInt());

        // WHEN
        TbAiResponse response = operations().execute(operation, context);

        // THEN
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getValue()).isEqualTo(JacksonUtil.newObjectNode().put("mainDashboardId", "d1"));
        assertThat(response.getCreditsUsed()).isEqualTo(3);
        ChannelHello hello = codec.fromPayload(frame(ChannelProtocol.HELLO).payload(), ChannelHello.class);
        assertThat(hello.capabilities()).containsExactly(ChannelProtocol.CAPABILITY_REST, ChannelProtocol.CAPABILITY_OPERATIONS);
        assertThat(hello.clientOrigin()).isEqualTo("https://tb.example.com");
        assertThat(codec.fromPayload(frame(ChannelProtocol.SOLUTION_INSTALL).payload(), SolutionOperationRequest.class))
                .isEqualTo(new SolutionOperationRequest(solutionId));
        assertThat(frame(ChannelProtocol.TB_RESPONSE)).isNotNull();
    }

    @Test
    void shouldReturnErrorMessageFromBody_whenOperationFails() {
        // GIVEN
        startScriptedAi(List.of(ChannelProtocol.CAPABILITY_REST, ChannelProtocol.CAPABILITY_OPERATIONS),
                request -> new OperationResult(404, JacksonUtil.newObjectNode().put("message", "Solution not found"), 0));

        // WHEN
        TbAiResponse response = operations().execute(operation, context);

        // THEN
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getError()).isEqualTo("Solution not found");
    }

    @Test
    void shouldSignalOperationsUnsupported_whenServerLacksCapability() {
        // GIVEN
        startScriptedAi(List.of(ChannelProtocol.CAPABILITY_REST), request -> {
            throw new AssertionError("Operation must not be sent");
        });

        // WHEN-THEN
        assertThatThrownBy(() -> operations().execute(operation, context))
                .isInstanceOf(TbAiOperationsUnsupportedException.class);
        assertThat(receivedByAi).extracting(ChannelFrame::type).doesNotContain(ChannelProtocol.SOLUTION_INSTALL);
        verify(requestExecutor, never()).serve(any(), any(), any(), any(), anyInt());
    }

    @Test
    void shouldSignalChannelUnavailable_whenServerHasNoChannelEndpoint() {
        // GIVEN
        aiServer = HttpServer.create().host("127.0.0.1").port(0)
                .route(routes -> routes.get(ChannelProtocol.WS_PATH, (req, res) -> res.status(404).send()))
                .bindNow();

        // WHEN-THEN
        assertThatThrownBy(() -> operations().execute(operation, context))
                .isInstanceOf(TbAiChannelUnavailableException.class);
    }

    @Test
    void shouldReturnUnauthorized_whenServerRejectsToken() {
        // GIVEN
        aiServer = HttpServer.create().host("127.0.0.1").port(0)
                .route(routes -> routes.get(ChannelProtocol.WS_PATH, (req, res) -> res.status(401).send()))
                .bindNow();

        // WHEN
        TbAiResponse response = operations().execute(operation, context);

        // THEN
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getError()).isEqualTo("Unauthorized");
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
        return new ChannelTbAiOperations(new TbAiChannelClient("http://127.0.0.1:" + aiServer.port()), requestExecutor);
    }

    ChannelFrame frame(String type) {
        return receivedByAi.stream().filter(frame -> type.equals(frame.type())).findFirst().orElseThrow();
    }

    void startScriptedAi(List<String> capabilities, Function<ChannelFrame, OperationResult> operationHandler) {
        aiServer = HttpServer.create().host("127.0.0.1").port(0)
                .route(routes -> routes.ws(ChannelProtocol.WS_PATH, (inbound, outbound) -> {
                    Sinks.Many<String> out = Sinks.many().unicast().onBackpressureBuffer();
                    ChannelFrame[] pendingOperation = new ChannelFrame[1];
                    inbound.receive().asString().subscribe(text -> {
                        ChannelFrame frame = codec.decode(text);
                        receivedByAi.add(frame);
                        switch (frame.type()) {
                            case ChannelProtocol.HELLO -> out.tryEmitNext(codec.encode(ChannelFrame.of(ChannelProtocol.HELLO,
                                    codec.toPayload(new ChannelHello(1, capabilities, null, 10_485_760L)))));
                            case ChannelProtocol.SOLUTION_INSTALL -> {
                                OperationResult result = operationHandler.apply(frame);
                                if (result.status() == 200) {
                                    pendingOperation[0] = frame;
                                    out.tryEmitNext(codec.encode(new ChannelFrame(ChannelProtocol.TB_REQUEST, "r1", null, codec.toPayload(
                                            new TbHttpRequest("POST", "/api/dashboard", null, Map.of(), "{}", "utf8", 5000L)))));
                                } else {
                                    reply(out, frame, result);
                                }
                            }
                            case ChannelProtocol.TB_RESPONSE -> reply(out, pendingOperation[0], operationHandler.apply(pendingOperation[0]));
                            default -> {}
                        }
                    });
                    return outbound.sendString(out.asFlux()).then().then(outbound.sendClose(1000, "Operation completed"));
                }))
                .bindNow();
    }

    void reply(Sinks.Many<String> out, ChannelFrame request, OperationResult result) {
        out.tryEmitNext(codec.encode(ChannelFrame.reply(ChannelProtocol.OPERATION_RESULT, request, codec.toPayload(result))));
        out.tryEmitComplete();
    }

}
