// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ai.transport;

import com.fasterxml.jackson.databind.JsonNode;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.thingsboard.ai.common.channel.ChannelError;
import org.thingsboard.ai.common.channel.ChannelException;
import org.thingsboard.ai.common.channel.ChannelFrame;
import org.thingsboard.ai.common.channel.ChannelFrameCodec;
import org.thingsboard.ai.common.channel.ChannelFrameListener;
import org.thingsboard.ai.common.channel.ChannelHello;
import org.thingsboard.ai.common.channel.ChannelProtocol;
import org.thingsboard.ai.common.channel.ChannelSession;
import org.thingsboard.ai.common.channel.OperationResult;
import org.thingsboard.ai.common.channel.TbAiChannelClient;
import org.thingsboard.ai.common.client.TbAiClient.TbAiResponse;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@TbCoreComponent
@RequiredArgsConstructor
class ChannelTbAiOperations {

    static final Duration OPERATION_TIMEOUT = Duration.ofMinutes(30);

    private final TbAiChannelClient channelClient;
    private final TbAiRequestExecutor requestExecutor;

    TbAiResponse execute(TbAiOperation operation, TbAiTurnContext context) {
        var call = new OperationCall(operation, context);
        channelClient.connect(context.tokenProvider(), call).subscribe(call::opened, call::connectFailed);
        try {
            return call.result.get(OPERATION_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            call.abandon();
            return failure("Request interrupted");
        } catch (TimeoutException e) {
            call.abandon();
            return failure("Request timed out");
        } catch (ExecutionException e) {
            if (e.getCause() instanceof RuntimeException runtime
                    && (runtime instanceof TbAiChannelUnavailableException || runtime instanceof TbAiOperationsUnsupportedException)) {
                throw runtime;
            }
            log.warn("AI channel operation '{}' failed: {}", operation.type(), ExceptionUtils.getRootCauseMessage(e));
            return failure("Service unavailable");
        }
    }

    static TbAiResponse failure(String message) {
        return TbAiResponse.builder().success(false).error(message).build();
    }

    static TbAiResponse toResponse(OperationResult result) {
        TbAiResponse response;
        if (result.isSuccess()) {
            response = TbAiResponse.builder().success(true).value(result.body()).build();
        } else {
            JsonNode message = result.body() != null ? result.body().get("message") : null;
            String error = message != null && message.isTextual() ? message.asText() : reasonPhrase(result.status());
            response = TbAiResponse.builder().success(false).error(error).build();
        }
        response.setCreditsUsed(result.creditsUsed());
        return response;
    }

    private static String reasonPhrase(int status) {
        HttpStatus resolved = HttpStatus.resolve(status);
        return resolved != null ? resolved.getReasonPhrase() : "Internal error";
    }

    private static Integer handshakeStatus(Throwable error) {
        for (Throwable cause : ExceptionUtils.getThrowableList(error)) {
            if (cause instanceof WebSocketClientHandshakeException handshake && handshake.response() != null) {
                return handshake.response().status().code();
            }
        }
        return null;
    }

    private final class OperationCall implements ChannelFrameListener {

        private final TbAiOperation operation;
        private final TbAiTurnContext context;
        private final ChannelFrameCodec codec = channelClient.codec();
        private final CompletableFuture<TbAiResponse> result = new CompletableFuture<>();
        private final AtomicBoolean sent = new AtomicBoolean();
        private volatile ChannelSession session;
        private volatile int maxFrameBytes = TbAiChannelClient.DEFAULT_MAX_FRAME_BYTES;

        private OperationCall(TbAiOperation operation, TbAiTurnContext context) {
            this.operation = operation;
            this.context = context;
        }

        void opened(ChannelSession openedSession) {
            session = openedSession;
            if (result.isDone()) {
                openedSession.close(ChannelProtocol.CLOSE_NORMAL, "Operation abandoned");
                return;
            }
            openedSession.send(ChannelFrame.of(ChannelProtocol.HELLO, codec.toPayload(new ChannelHello(ChannelProtocol.VERSION,
                    List.of(ChannelProtocol.CAPABILITY_REST, ChannelProtocol.CAPABILITY_OPERATIONS),
                    context.clientRequest().clientOrigin(), null))));
        }

        void connectFailed(Throwable error) {
            Integer status = handshakeStatus(error);
            if (status != null && (status == HttpStatus.UNAUTHORIZED.value() || status == HttpStatus.FORBIDDEN.value())) {
                result.complete(failure(HttpStatus.valueOf(status).getReasonPhrase()));
                return;
            }
            result.completeExceptionally(new TbAiChannelUnavailableException(
                    "Cannot open the AI channel: " + ExceptionUtils.getRootCauseMessage(error), error));
        }

        void abandon() {
            ChannelSession current = session;
            if (current != null && current.isOpen()) {
                current.close(ChannelProtocol.CLOSE_NORMAL, "Operation abandoned");
            }
        }

        @Override
        public void onFrame(ChannelSession frameSession, ChannelFrame frame) {
            try {
                switch (frame.type()) {
                    case ChannelProtocol.HELLO -> onHello(frameSession, frame);
                    case ChannelProtocol.TB_REQUEST -> requestExecutor.serve(frameSession, frame, codec, context, maxFrameBytes);
                    case ChannelProtocol.ERROR -> onError(frameSession, frame);
                    default -> log.debug("Ignoring AI channel frame '{}' during operation '{}'", frame.type(), operation.type());
                }
            } catch (ChannelException e) {
                log.warn("Malformed AI channel frame '{}': {}", frame.type(), e.getMessage());
                frameSession.close(ChannelProtocol.CLOSE_PROTOCOL_ERROR, e.getMessage());
            }
        }

        @Override
        public void onClose(ChannelSession closedSession, int code, String reason) {
            if (!sent.get()) {
                result.completeExceptionally(new TbAiChannelUnavailableException(
                        "AI channel closed before the operation started [" + code + "] " + reason, null));
            }
        }

        private void onHello(ChannelSession helloSession, ChannelFrame frame) {
            ChannelHello hello = codec.fromPayload(frame.payload(), ChannelHello.class);
            if (hello.capabilities() == null || !hello.capabilities().contains(ChannelProtocol.CAPABILITY_OPERATIONS)) {
                result.completeExceptionally(new TbAiOperationsUnsupportedException("TB AI does not support channel operations"));
                helloSession.close(ChannelProtocol.CLOSE_NORMAL, "Operations not supported");
                return;
            }
            if (hello.maxFrameBytes() != null && hello.maxFrameBytes() > 0 && hello.maxFrameBytes() < Integer.MAX_VALUE) {
                maxFrameBytes = hello.maxFrameBytes().intValue();
            }
            sent.set(true);
            helloSession.request(ChannelFrame.request(operation.type(), codec.toPayload(operation.payload())), OPERATION_TIMEOUT)
                    .whenComplete((reply, error) -> {
                        if (error != null) {
                            result.completeExceptionally(error);
                        } else if (ChannelProtocol.OPERATION_RESULT.equals(reply.type())) {
                            result.complete(toResponse(codec.fromPayload(reply.payload(), OperationResult.class)));
                        } else {
                            result.completeExceptionally(new ChannelException("Unexpected reply '" + reply.type() + "'"));
                        }
                    });
        }

        private void onError(ChannelSession errorSession, ChannelFrame frame) {
            ChannelError error = codec.fromPayload(frame.payload(), ChannelError.class);
            log.warn("AI channel error during operation '{}' {}: {}", operation.type(), error.code(), error.message());
            if (!sent.get()) {
                result.completeExceptionally(new TbAiChannelUnavailableException("AI channel refused: " + error.message(), null));
            } else {
                result.completeExceptionally(new ChannelException(error.message()));
            }
            errorSession.close(ChannelProtocol.CLOSE_NORMAL, "Error received");
        }

    }

}
