// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ai.transport;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.thingsboard.ai.common.channel.ChannelError;
import org.thingsboard.ai.common.channel.ChannelException;
import org.thingsboard.ai.common.channel.ChannelFrame;
import org.thingsboard.ai.common.channel.ChannelProtocol;
import org.thingsboard.ai.common.channel.OperationResult;
import org.thingsboard.ai.common.client.TbAiClient.TbAiResponse;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Runs one operation on a pooled channel and waits for its {@code operation.result}. An operation the channel did not
 * start (a {@code draining} or {@code too_many_operations} refusal, or a 1001 close before any frame for it) is
 * retried once on a new channel; any other failure after it was sent is an error, so an operation never runs twice.
 */
@Slf4j
@Component
@TbCoreComponent
@RequiredArgsConstructor
class ChannelTbAiOperations {

    static final Duration OPERATION_TIMEOUT = Duration.ofMinutes(30);
    // Operations during which TB AI calls ThingsBoard back; the rest are api.v1 operations.
    static final Set<String> CALLBACK_OPERATIONS = Set.of(
            ChannelProtocol.SOLUTION_INSTALL, ChannelProtocol.SOLUTION_UNINSTALL, ChannelProtocol.DASHBOARD_GENERATE);

    private final TbAiChannelPool pool;
    private final TbAiRequestExecutor requestExecutor;

    TbAiResponse execute(TbAiOperation operation, TbAiTurnContext context) {
        String capability = CALLBACK_OPERATIONS.contains(operation.type())
                ? ChannelProtocol.CAPABILITY_OPERATIONS : ChannelProtocol.CAPABILITY_API;
        boolean fresh = false;
        for (int attempt = 1; ; attempt++) {
            TbAiPooledConnection connection = acquire(context, fresh);
            if (!connection.supports(capability)) {
                connection.finish(null, System.nanoTime());
                throw new TbAiOperationsUnsupportedException("TB AI does not support '" + operation.type() + "' over the channel");
            }
            var call = new OperationCall(operation, context, connection);
            try {
                return call.run();
            } catch (NotStartedException e) {
                if (attempt >= 2) {
                    log.warn("AI channel operation '{}' was refused twice: {}", operation.type(), e.getMessage());
                    return failure("Service unavailable");
                }
                log.debug("AI channel operation '{}' was not started ({}), retrying on a new channel", operation.type(), e.getMessage());
                fresh = true;
            }
        }
    }

    private TbAiPooledConnection acquire(TbAiTurnContext context, boolean fresh) {
        try {
            return pool.acquire(context, fresh).get(OPERATION_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TbAiChannelUnavailableException("Interrupted while opening the AI channel", e);
        } catch (TimeoutException e) {
            throw new TbAiChannelUnavailableException("Timed out opening the AI channel", e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new TbAiChannelUnavailableException("Cannot open the AI channel: " + ExceptionUtils.getRootCauseMessage(e), e);
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

    private static final class NotStartedException extends RuntimeException {

        NotStartedException(String message) {
            super(message, null, false, false);
        }

    }

    private final class OperationCall implements TbAiChannelExchange {

        private final TbAiOperation operation;
        private final TbAiTurnContext context;
        private final TbAiPooledConnection connection;
        private final CompletableFuture<TbAiResponse> result = new CompletableFuture<>();
        private final AtomicBoolean finished = new AtomicBoolean();
        private volatile boolean frameSeen;
        private volatile String op;

        private OperationCall(TbAiOperation operation, TbAiTurnContext context, TbAiPooledConnection connection) {
            this.operation = operation;
            this.context = context;
            this.connection = connection;
        }

        TbAiResponse run() {
            ChannelFrame start = ChannelFrame.request(operation.type(), connection.codec().toPayload(operation.payload()));
            try {
                op = connection.start(this, start);
            } catch (ChannelException e) {
                finish();
                throw new NotStartedException("channel closed before the operation was sent");
            }
            try {
                return result.get(OPERATION_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return failure("Request interrupted");
            } catch (TimeoutException e) {
                return failure("Request timed out");
            } catch (ExecutionException e) {
                if (e.getCause() instanceof NotStartedException notStarted) {
                    throw notStarted;
                }
                log.warn("AI channel operation '{}' failed: {}", operation.type(), ExceptionUtils.getRootCauseMessage(e));
                return failure("Service unavailable");
            } finally {
                finish();
            }
        }

        @Override
        public void onFrame(TbAiPooledConnection frameConnection, ChannelFrame frame) {
            try {
                switch (frame.type()) {
                    case ChannelProtocol.TB_REQUEST -> {
                        frameSeen = true;
                        requestExecutor.serve(frameConnection.session(), frame, frameConnection.codec(), context, frameConnection.maxFrameBytes());
                    }
                    case ChannelProtocol.OPERATION_RESULT -> result.complete(toResponse(
                            frameConnection.codec().fromPayload(frame.payload(), OperationResult.class)));
                    case ChannelProtocol.ERROR -> onError(frameConnection, frame);
                    default -> {
                        frameSeen = true;
                        log.debug("Ignoring AI channel frame '{}' during operation '{}'", frame.type(), operation.type());
                    }
                }
            } catch (ChannelException e) {
                result.completeExceptionally(e);
            }
        }

        @Override
        public void onClose(int code, String reason) {
            if (!frameSeen && code == ChannelProtocol.CLOSE_GOING_AWAY) {
                result.completeExceptionally(new NotStartedException("channel closed [" + code + "] " + reason));
            } else {
                result.completeExceptionally(new ChannelException("AI channel closed [" + code + "] " + reason));
            }
        }

        private void onError(TbAiPooledConnection frameConnection, ChannelFrame frame) {
            ChannelError error = frameConnection.codec().fromPayload(frame.payload(), ChannelError.class);
            if (ChannelError.DRAINING.equals(error.code())) {
                frameConnection.retire();
                result.completeExceptionally(new NotStartedException(error.message()));
            } else if (ChannelError.TOO_MANY_OPERATIONS.equals(error.code())) {
                result.completeExceptionally(new NotStartedException(error.message()));
            } else {
                log.warn("AI channel error during operation '{}' {}: {}", operation.type(), error.code(), error.message());
                result.completeExceptionally(new CompletionException(new ChannelException(error.message())));
            }
        }

        private void finish() {
            if (finished.compareAndSet(false, true)) {
                connection.finish(op, System.nanoTime());
            }
        }

    }

}
