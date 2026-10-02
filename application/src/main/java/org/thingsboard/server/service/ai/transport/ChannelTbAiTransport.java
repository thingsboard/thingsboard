// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ai.transport;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.thingsboard.ai.common.channel.ChannelError;
import org.thingsboard.ai.common.channel.ChannelException;
import org.thingsboard.ai.common.channel.ChannelFrame;
import org.thingsboard.ai.common.channel.ChannelProtocol;
import org.thingsboard.ai.common.channel.ChatEventPayload;
import org.thingsboard.ai.common.channel.ChatTurnRequest;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.queue.util.TbCoreComponent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Runs a chat turn on a pooled channel and re-emits its events as SSE. A turn the channel did not start is retried
 * once on a new channel; a channel that cannot be opened fails the turn with "Service unavailable".
 */
@Slf4j
@Component
@TbCoreComponent
@RequiredArgsConstructor
class ChannelTbAiTransport implements TbAiTransport {

    private static final String HEARTBEAT = "heartbeat";
    private static final String TOOL_EXECUTION_REQUESTED = "toolExecutionRequested";
    private static final String TOOL_EXECUTION_RESULT = "toolExecutionResult";

    private final TbAiChannelPool pool;
    private final TbAiRequestExecutor requestExecutor;
    private final TbAiChannelRegistry registry;

    @Override
    public Flux<ServerSentEvent<String>> sendChatMessage(UUID chatId, JsonNode request, TbAiTurnContext context) {
        return Flux.create(sink -> {
            var turn = new ChannelTurn(chatId, request, context, sink);
            sink.onDispose(turn::abandon);
            turn.start(false);
        });
    }

    private static ServerSentEvent<String> errorEvent(String message) {
        return ServerSentEvent.<String>builder()
                .event("error")
                .data(JacksonUtil.toString(JacksonUtil.newObjectNode().put("message", message)))
                .build();
    }

    private final class ChannelTurn implements TbAiChannelExchange {

        private final UUID chatId;
        private final JsonNode request;
        private final TbAiTurnContext context;
        private final FluxSink<ServerSentEvent<String>> sink;
        private final AtomicBoolean finished = new AtomicBoolean();
        private final Set<UUID> approvals = ConcurrentHashMap.newKeySet();
        private volatile TbAiPooledConnection connection;
        private volatile String op;
        private volatile boolean frameSeen;
        private volatile boolean retried;

        private ChannelTurn(UUID chatId, JsonNode request, TbAiTurnContext context, FluxSink<ServerSentEvent<String>> sink) {
            this.chatId = chatId;
            this.request = request;
            this.context = context;
            this.sink = sink;
        }

        void start(boolean fresh) {
            pool.acquire(context, fresh).whenComplete((acquired, error) -> {
                if (error != null) {
                    connectFailed(error instanceof CompletionException && error.getCause() != null ? error.getCause() : error);
                } else {
                    attach(acquired);
                }
            });
        }

        void abandon() {
            if (finished.get()) {
                return;
            }
            TbAiPooledConnection current = connection;
            if (current != null) {
                if (op != null) {
                    try {
                        current.send(new ChannelFrame(ChannelProtocol.CANCEL, null, null, null, op));
                    } catch (ChannelException e) {
                        log.debug("[{}] Failed to cancel the AI turn: {}", chatId, e.getMessage());
                    }
                } else {
                    current.close(ChannelProtocol.CLOSE_NORMAL, "Turn abandoned");
                }
            }
            end(null);
        }

        @Override
        public void onFrame(TbAiPooledConnection frameConnection, ChannelFrame frame) {
            try {
                switch (frame.type()) {
                    case ChannelProtocol.CHAT_EVENT -> {
                        frameSeen = true;
                        onChatEvent(frameConnection, frame);
                    }
                    case ChannelProtocol.TB_REQUEST -> {
                        frameSeen = true;
                        requestExecutor.serve(frameConnection.session(), frame, frameConnection.codec(), context, frameConnection.maxFrameBytes());
                    }
                    case ChannelProtocol.TURN_END -> end(null);
                    case ChannelProtocol.ERROR -> onError(frameConnection, frame);
                    default -> log.debug("[{}] Ignoring AI channel frame '{}'", chatId, frame.type());
                }
            } catch (ChannelException e) {
                log.warn("[{}] Malformed AI channel frame '{}': {}", chatId, frame.type(), e.getMessage());
                end(errorEvent("Internal error"));
            }
        }

        @Override
        public void onClose(int code, String reason) {
            if (finished.get()) {
                return;
            }
            if (!frameSeen && code == ChannelProtocol.CLOSE_GOING_AWAY && retry()) {
                return;
            }
            if (!frameSeen) {
                fail(new TbAiChannelUnavailableException("AI channel closed before the turn started [" + code + "] " + reason, null));
            } else if (code == ChannelProtocol.CLOSE_NORMAL) {
                end(null);
            } else {
                log.warn("[{}] AI channel closed during the turn [{}] {}", chatId, code, reason);
                end(errorEvent("Service unavailable"));
            }
        }

        private void connectFailed(Throwable error) {
            if (error instanceof TbAiChannelRejectedException rejected) {
                end(errorEvent(rejected.getMessage()));
            } else if (error instanceof TbAiChannelUnavailableException unavailable) {
                fail(unavailable);
            } else {
                fail(new TbAiChannelUnavailableException("Cannot open the AI channel: " + error.getMessage(), error));
            }
        }

        private void attach(TbAiPooledConnection acquired) {
            if (sink.isCancelled() || finished.get()) {
                acquired.finish(null, System.nanoTime());
                return;
            }
            connection = acquired;
            ChannelFrame start = ChannelFrame.request(ChannelProtocol.CHAT_SEND,
                    acquired.codec().toPayload(new ChatTurnRequest(chatId, request, context.acceptLanguage())));
            try {
                op = acquired.start(this, start);
            } catch (ChannelException e) {
                connection = null;
                acquired.finish(null, System.nanoTime());
                if (!retry()) {
                    fail(new TbAiChannelUnavailableException("AI channel closed before the turn started", e));
                }
            }
        }

        private void onChatEvent(TbAiPooledConnection frameConnection, ChannelFrame frame) {
            ChatEventPayload event = frameConnection.codec().fromPayload(frame.payload(), ChatEventPayload.class);
            if (HEARTBEAT.equals(event.name())) {
                sink.next(ServerSentEvent.<String>builder().comment(HEARTBEAT).build());
                return;
            }
            trackApproval(frameConnection, event);
            sink.next(ServerSentEvent.<String>builder()
                    .event(event.name())
                    .data(event.data() != null ? event.data().toString() : null)
                    .build());
        }

        private void trackApproval(TbAiPooledConnection frameConnection, ChatEventPayload event) {
            JsonNode executionId = event.data() != null ? event.data().get("executionId") : null;
            if (executionId == null || !executionId.isTextual()) {
                return;
            }
            UUID id = UUID.fromString(executionId.asText());
            if (TOOL_EXECUTION_REQUESTED.equals(event.name()) && event.data().path("needsApproval").asBoolean()) {
                approvals.add(id);
                registry.registerApproval(id, frameConnection.session(), op);
            } else if (TOOL_EXECUTION_RESULT.equals(event.name())) {
                approvals.remove(id);
                registry.unregisterApproval(id);
            }
        }

        private void onError(TbAiPooledConnection frameConnection, ChannelFrame frame) {
            ChannelError error = frameConnection.codec().fromPayload(frame.payload(), ChannelError.class);
            boolean notStarted = ChannelError.DRAINING.equals(error.code()) || ChannelError.TOO_MANY_OPERATIONS.equals(error.code());
            if (ChannelError.DRAINING.equals(error.code())) {
                frameConnection.retire();
            }
            if (notStarted && !frameSeen) {
                release();
                if (retry()) {
                    return;
                }
                fail(new TbAiChannelUnavailableException("AI channel refused the turn: " + error.message(), null));
                return;
            }
            log.warn("[{}] AI channel error {}: {}", chatId, error.code(), error.message());
            end(errorEvent("Internal error"));
        }

        private boolean retry() {
            if (retried || finished.get()) {
                return false;
            }
            retried = true;
            log.debug("[{}] AI turn was not started, retrying on a new channel", chatId);
            op = null;
            connection = null;
            start(true);
            return true;
        }

        private void fail(RuntimeException error) {
            if (finished.compareAndSet(false, true)) {
                release();
                sink.error(error);
            }
        }

        private void end(ServerSentEvent<String> lastEvent) {
            if (finished.compareAndSet(false, true)) {
                release();
                if (lastEvent != null) {
                    sink.next(lastEvent);
                }
                sink.complete();
            }
        }

        private void release() {
            approvals.forEach(registry::unregisterApproval);
            approvals.clear();
            TbAiPooledConnection current = connection;
            if (current != null) {
                connection = null;
                current.finish(op, System.nanoTime());
            }
        }

    }

}
