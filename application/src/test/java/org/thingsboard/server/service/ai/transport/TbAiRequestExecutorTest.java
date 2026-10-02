// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ai.transport;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.codec.http.HttpHeaders;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.thingsboard.ai.common.channel.ChannelError;
import org.thingsboard.ai.common.channel.ChannelFrame;
import org.thingsboard.ai.common.channel.ChannelFrameCodec;
import org.thingsboard.ai.common.channel.ChannelProtocol;
import org.thingsboard.ai.common.channel.TbHttpRequest;
import org.thingsboard.ai.common.channel.TbHttpResponse;
import org.thingsboard.server.service.security.model.SecurityUser;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class TbAiRequestExecutorTest {

    ChannelFrameCodec codec = new ChannelFrameCodec(new ObjectMapper());
    AtomicReference<HttpHeaders> receivedHeaders = new AtomicReference<>();
    AtomicReference<String> receivedUri = new AtomicReference<>();
    AtomicReference<String> receivedBody = new AtomicReference<>();
    DisposableServer tbServer;
    TbAiRequestExecutor executor;
    TbAiTurnContext context = new TbAiTurnContext(mock(SecurityUser.class), "Bearer user-jwt", "en-US", () -> "ai-token",
            new TbAiClientRequest("https://tb.example.com", Map.of("Host", "tb.example.com", "X-Forwarded-Proto", "https")));

    @BeforeEach
    void setUp() {
        tbServer = HttpServer.create().host("127.0.0.1").port(0).route(routes -> routes
                .get("/api/tenant/devices", (request, response) -> {
                    receivedHeaders.set(request.requestHeaders());
                    receivedUri.set(request.uri());
                    return response.header("Content-Type", "application/json").sendString(Mono.just("{\"data\":[]}"));
                })
                .post("/api/device", (request, response) -> request.receive().aggregate().asString()
                        .flatMap(body -> {
                            receivedBody.set(body);
                            return response.status(201).header("Content-Type", "application/json")
                                    .sendString(Mono.just("{\"id\":\"saved\"}")).then();
                        }))
                .get("/api/large", (request, response) -> response.header("Content-Type", "application/json")
                        .sendString(Mono.just("{\"payload\":\"" + "x".repeat(200) + "\"}"))))
                .bindNow();
        executor = new TbAiRequestExecutor("http://127.0.0.1:" + tbServer.port(), null);
    }

    @AfterEach
    void tearDown() {
        tbServer.disposeNow();
    }

    @Test
    void shouldCallThingsBoardAsUserWithForwardedHeaders_whenRequestIsValid() {
        // GIVEN
        var request = new TbHttpRequest("GET", "/api/tenant/devices", "pageSize=10&page=0",
                Map.of("Accept", List.of("application/json"), "Authorization", List.of("Bearer ai-token")), null, "base64", 5000L);

        // WHEN
        ChannelFrame reply = execute(request, 10_000);

        // THEN
        assertThat(reply.type()).isEqualTo(ChannelProtocol.TB_RESPONSE);
        TbHttpResponse response = codec.fromPayload(reply.payload(), TbHttpResponse.class);
        assertThat(response.status()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("{\"data\":[]}");
        assertThat(response.bodyEncoding()).isEqualTo("utf8");
        assertThat(receivedUri.get()).isEqualTo("/api/tenant/devices?pageSize=10&page=0");
        HttpHeaders headers = receivedHeaders.get();
        assertThat(headers.get("X-Authorization")).isEqualTo("Bearer user-jwt");
        assertThat(headers.get("Host")).isEqualTo("tb.example.com");
        assertThat(headers.get("X-Forwarded-Proto")).isEqualTo("https");
        assertThat(headers.get("Accept")).isEqualTo("application/json");
        assertThat(headers.contains("Authorization")).isFalse();
    }

    @Test
    void shouldForwardBodyAndStatus_whenRequestHasBody() {
        // GIVEN
        var request = new TbHttpRequest("POST", "/api/device", null,
                Map.of("Content-Type", List.of("application/json")), "{\"name\":\"Pump\"}", "utf8", 5000L);

        // WHEN
        ChannelFrame reply = execute(request, 10_000);

        // THEN
        TbHttpResponse response = codec.fromPayload(reply.payload(), TbHttpResponse.class);
        assertThat(response.status()).isEqualTo(201);
        assertThat(response.body()).isEqualTo("{\"id\":\"saved\"}");
        assertThat(receivedBody.get()).isEqualTo("{\"name\":\"Pump\"}");
    }

    @Test
    void shouldPassThroughErrorStatus_whenThingsBoardRejectsRequest() {
        // GIVEN
        var request = new TbHttpRequest("GET", "/api/missing", null, Map.of(), null, "base64", 5000L);

        // WHEN
        ChannelFrame reply = execute(request, 10_000);

        // THEN
        assertThat(reply.type()).isEqualTo(ChannelProtocol.TB_RESPONSE);
        assertThat(codec.fromPayload(reply.payload(), TbHttpResponse.class).status()).isEqualTo(404);
    }

    @Test
    void shouldReplyWithError_whenResponseExceedsFrameLimit() {
        // GIVEN
        var request = new TbHttpRequest("GET", "/api/large", null, Map.of(), null, "base64", 5000L);

        // WHEN
        ChannelFrame reply = execute(request, 100);

        // THEN
        assertThat(reply.type()).isEqualTo(ChannelProtocol.ERROR);
        assertThat(codec.fromPayload(reply.payload(), ChannelError.class))
                .isEqualTo(new ChannelError("too_large", "Response is larger than the channel frame limit"));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            GET   | /oauth2/authorization | Path must start with /api/
            GET   | http://evil/api/x     | Path must start with /api/
            GET   | /api/../admin         | Path must be a normalized relative path
            GET   | /api//device          | Path must be a normalized relative path
            GET   | /api/device?x=1       | Path must be a normalized relative path
            TRACE | /api/device           | Method is not allowed: TRACE
            """)
    void shouldRejectRequest_whenMethodOrPathIsNotAllowed(String method, String path, String expectedMessage) {
        // GIVEN
        var request = new TbHttpRequest(method, path, null, Map.of(), null, "base64", 5000L);

        // WHEN
        ChannelFrame reply = execute(request, 10_000);

        // THEN
        assertThat(reply.type()).isEqualTo(ChannelProtocol.ERROR);
        assertThat(codec.fromPayload(reply.payload(), ChannelError.class)).isEqualTo(new ChannelError("invalid_request", expectedMessage));
        assertThat(receivedUri.get()).isNull();
    }

    ChannelFrame execute(TbHttpRequest request, int maxBodyBytes) {
        var frame = new ChannelFrame(ChannelProtocol.TB_REQUEST, UUID.randomUUID().toString(), null, codec.toPayload(request));
        ChannelFrame reply = executor.execute(frame, codec, context, maxBodyBytes).block(Duration.ofSeconds(10));
        assertThat(reply.replyTo()).isEqualTo(frame.id());
        return reply;
    }

}
