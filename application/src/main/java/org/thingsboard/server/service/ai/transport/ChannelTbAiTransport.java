// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ai.transport;

import com.fasterxml.jackson.databind.JsonNode;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.thingsboard.ai.common.channel.ChannelError;
import org.thingsboard.ai.common.channel.ChannelException;
import org.thingsboard.ai.common.channel.ChannelFrame;
import org.thingsboard.ai.common.channel.ChannelFrameCodec;
import org.thingsboard.ai.common.channel.ChannelFrameListener;
import org.thingsboard.ai.common.channel.ChannelHello;
import org.thingsboard.ai.common.channel.ChannelProtocol;
import org.thingsboard.ai.common.channel.ChannelSession;
import org.thingsboard.ai.common.channel.ChatEventPayload;
import org.thingsboard.ai.common.channel.ChatTurnRequest;
import org.thingsboard.ai.common.channel.TbAiChannelClient;
import org.thingsboard.ai.common.client.TbAiClient;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.queue.util.TbCoreComponent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@TbCoreComponent
@RequiredArgsConstructor
class ChannelTbAiTransport implements TbAiTransport {

    private static final String HEARTBEAT = "heartbeat";
    private static final String TOOL_EXECUTION_REQUESTED = "toolExecutionRequested";
    private static final String TOOL_EXECUTION_RESULT = "toolExecutionResult";

    private final TbAiChannelClient channelClient;
    private final TbAiRequestExecutor requestExecutor;
    private final TbAiChannelRegistry registry;

    @Override
    public Flux<ServerSentEvent<String>> sendChatMessage(UUID chatId, JsonNode request, TbAiTurnContext context) {
        return Flux.create(sink -> {
            var turn = new ChannelTurn(chatId, request, context, sink);
            sink.onDispose(turn::abandon);
            channelClient.connect(withAcceptLanguage(context), turn).subscribe(turn::opened, turn::connectFailed);
        });
    }

    private static TbAiClient.TokenProvider withAcceptLanguage(TbAiTurnContext context) {
        TbAiClient.TokenProvider delegate = context.tokenProvider();
        return new TbAiClient.TokenProvider() {
            @Override
            public String getToken() {
                return delegate.getToken();
            }

            @Override
            public Map<String, String> getAdditionalInfo() {
                var headers = new HashMap<>(delegate.getAdditionalInfo());
                if (context.acceptLanguage() != null && !context.acceptLanguage().isBlank()) {
                    headers.put(HttpHeaders.ACCEPT_LANGUAGE, context.acceptLanguage());
                }
                return headers;
            }
        };
    }

    private static ServerSentEvent<String> errorEvent(String message) {
        return ServerSentEvent.<String>builder()
                .event("error")
                .data(JacksonUtil.toString(JacksonUtil.newObjectNode().put("message", message)))
                .build();
    }

    private static Integer handshakeStatus(Throwable error) {
        for (Throwable cause : ExceptionUtils.getThrowableList(error)) {
            if (cause instanceof WebSocketClientHandshakeException handshake && handshake.response() != null) {
                return handshake.response().status().code();
            }
        }
        return null;
    }

    private final class ChannelTurn implements ChannelFrameListener {

        private final UUID chatId;
        private final JsonNode request;
        private final TbAiTurnContext context;
        private final FluxSink<ServerSentEvent<String>> sink;
        private final ChannelFrameCodec codec = channelClient.codec();
        private final AtomicBoolean finished = new AtomicBoolean();
        private volatile ChannelSession session;
        private volatile boolean started;
        private volatile int maxFrameBytes = TbAiChannelClient.DEFAULT_MAX_FRAME_BYTES;

        private ChannelTurn(UUID chatId, JsonNode request, TbAiTurnContext context, FluxSink<ServerSentEvent<String>> sink) {
            this.chatId = chatId;
            this.request = request;
            this.context = context;
            this.sink = sink;
        }

        void opened(ChannelSession openedSession) {
            session = openedSession;
            if (sink.isCancelled()) {
                openedSession.close(ChannelProtocol.CLOSE_NORMAL, "Turn abandoned");
                return;
            }
            openedSession.send(ChannelFrame.of(ChannelProtocol.HELLO, codec.toPayload(new ChannelHello(
                    ChannelProtocol.VERSION, List.of(ChannelProtocol.CAPABILITY_REST), context.clientRequest().clientOrigin(), null))));
        }

        void connectFailed(Throwable error) {
            Integer status = handshakeStatus(error);
            if (status != null && (status == HttpStatus.UNAUTHORIZED.value() || status == HttpStatus.FORBIDDEN.value())) {
                finish(errorEvent(HttpStatus.valueOf(status).getReasonPhrase()));
                return;
            }
            sink.error(new TbAiChannelUnavailableException("Cannot open the AI channel: " + ExceptionUtils.getRootCauseMessage(error), error));
        }

        void abandon() {
            ChannelSession current = session;
            if (current != null && current.isOpen() && !finished.get()) {
                current.close(ChannelProtocol.CLOSE_NORMAL, "Turn abandoned");
            }
        }

        @Override
        public void onFrame(ChannelSession frameSession, ChannelFrame frame) {
            try {
                switch (frame.type()) {
                    case ChannelProtocol.HELLO -> onHello(frameSession, frame);
                    case ChannelProtocol.CHAT_EVENT -> onChatEvent(frameSession, frame);
                    case ChannelProtocol.TB_REQUEST -> requestExecutor.serve(frameSession, frame, codec, context, maxFrameBytes);
                    case ChannelProtocol.TURN_END -> finish(null);
                    case ChannelProtocol.ERROR -> onError(frameSession, frame);
                    default -> log.debug("[{}] Ignoring AI channel frame '{}'", chatId, frame.type());
                }
            } catch (ChannelException e) {
                log.warn("[{}] Malformed AI channel frame '{}': {}", chatId, frame.type(), e.getMessage());
                frameSession.close(ChannelProtocol.CLOSE_PROTOCOL_ERROR, e.getMessage());
            }
        }

        @Override
        public void onClose(ChannelSession closedSession, int code, String reason) {
            registry.unregisterSession(closedSession);
            if (finished.get()) {
                return;
            }
            if (!started) {
                sink.error(new TbAiChannelUnavailableException("AI channel closed before the turn started [" + code + "] " + reason, null));
            } else if (code == ChannelProtocol.CLOSE_NORMAL) {
                finish(null);
            } else {
                log.warn("[{}] AI channel closed during the turn [{}] {}", chatId, code, reason);
                finish(errorEvent("Service unavailable"));
            }
        }

        private void onHello(ChannelSession helloSession, ChannelFrame frame) {
            ChannelHello hello = codec.fromPayload(frame.payload(), ChannelHello.class);
            if (hello.maxFrameBytes() != null && hello.maxFrameBytes() > 0 && hello.maxFrameBytes() < Integer.MAX_VALUE) {
                maxFrameBytes = hello.maxFrameBytes().intValue();
            }
            started = true;
            helloSession.send(ChannelFrame.of(ChannelProtocol.CHAT_SEND, codec.toPayload(new ChatTurnRequest(chatId, request))));
        }

        private void onChatEvent(ChannelSession eventSession, ChannelFrame frame) {
            ChatEventPayload event = codec.fromPayload(frame.payload(), ChatEventPayload.class);
            if (HEARTBEAT.equals(event.name())) {
                sink.next(ServerSentEvent.<String>builder().comment(HEARTBEAT).build());
                return;
            }
            trackApproval(eventSession, event);
            sink.next(ServerSentEvent.<String>builder()
                    .event(event.name())
                    .data(event.data() != null ? event.data().toString() : null)
                    .build());
        }

        private void trackApproval(ChannelSession eventSession, ChatEventPayload event) {
            JsonNode executionId = event.data() != null ? event.data().get("executionId") : null;
            if (executionId == null || !executionId.isTextual()) {
                return;
            }
            UUID id = UUID.fromString(executionId.asText());
            if (TOOL_EXECUTION_REQUESTED.equals(event.name()) && event.data().path("needsApproval").asBoolean()) {
                registry.registerApproval(id, eventSession);
            } else if (TOOL_EXECUTION_RESULT.equals(event.name())) {
                registry.unregisterApproval(id);
            }
        }

        private void onError(ChannelSession errorSession, ChannelFrame frame) {
            ChannelError error = codec.fromPayload(frame.payload(), ChannelError.class);
            log.warn("[{}] AI channel error {}: {}", chatId, error.code(), error.message());
            if (!started) {
                finished.set(true);
                sink.error(new TbAiChannelUnavailableException("AI channel refused: " + error.message(), null));
            } else {
                finish(errorEvent("Internal error"));
            }
            errorSession.close(ChannelProtocol.CLOSE_NORMAL, "Error received");
        }

        private void finish(ServerSentEvent<String> lastEvent) {
            if (finished.compareAndSet(false, true)) {
                if (lastEvent != null) {
                    sink.next(lastEvent);
                }
                sink.complete();
            }
        }

    }

}
