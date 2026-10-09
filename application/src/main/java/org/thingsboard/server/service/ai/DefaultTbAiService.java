// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.thingsboard.ai.common.channel.ChannelProtocol;
import org.thingsboard.ai.common.client.TbAiClient;
import org.thingsboard.ai.common.client.TbAiClient.TbAiResponse;
import org.thingsboard.ai.common.data.usage.ApiUsageInfo;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.stats.TbApiUsageReportClient;
import org.thingsboard.server.exception.ThingsboardRuntimeException;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.ai.transport.TbAiChannelRejectedException;
import org.thingsboard.server.service.ai.transport.TbAiChannelUnavailableException;
import org.thingsboard.server.service.ai.transport.TbAiOperation;
import org.thingsboard.server.service.ai.transport.TbAiOperations;
import org.thingsboard.server.service.apiusage.TbApiUsageStateService;
import org.thingsboard.server.service.security.model.SecurityUser;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
@TbCoreComponent
@RequiredArgsConstructor
class DefaultTbAiService implements TbAiService {

    private final TbApiUsageStateService apiUsageStateService;
    private final TbApiUsageReportClient apiUsageReportClient;
    private final Optional<TbAiTokenProvider> tokenProvider;
    private final TbAiSettings aiSettings;
    private final TbAiOperations operations;

    @Override
    public JsonNode process(TbAiCall<TbAiResponse> call, SecurityUser user, boolean checkCredits) {
        checkEnabled();
        if (checkCredits) {
            checkApiUsage(user);
        }
        TbAiResponse response = call.apply(createTokenProvider(user));
        return processResponse(response, user);
    }

    @Override
    public Flux<ServerSentEvent<String>> processStream(UUID chatId, TbAiCall<Flux<ServerSentEvent<String>>> call, SecurityUser user) {
        checkEnabled();
        checkApiUsage(user);
        return Flux.defer(() -> {
            // Eager subscribe so client cancel does not drop the trailing creditsUsed event.
            Sinks.Many<ServerSentEvent<String>> clientSink = Sinks.many().unicast().onBackpressureBuffer();

            call.apply(createTokenProvider(user))
                    .timeout(Duration.ofSeconds(aiSettings.getSseInactivityTimeoutSeconds()))
                    .subscribe(
                            event -> {
                                if (log.isTraceEnabled()) {
                                    log.trace("[{}] Received SSE event '{}'{}: {}", chatId, event.event(),
                                            event.comment() != null ? " (comment: '" + event.comment() + "')" : "", truncate(event.data()));
                                }
                                var processed = processEvent(event, user);
                                if (processed != null) {
                                    clientSink.tryEmitNext(processed);
                                }
                            },
                            error -> {
                                clientSink.tryEmitNext(errorEvent(toUserFriendlyMessage(error)));
                                clientSink.tryEmitComplete();
                            },
                            clientSink::tryEmitComplete
                    );

            return clientSink.asFlux();
        });
    }

    @Override
    public boolean isEnabled() {
        return aiSettings.isEnabled();
    }

    @Override
    public ApiUsageInfo getApiUsageInfo(SecurityUser user) {
        if (!isEnabled()) {
            return new ApiUsageInfo(Map.of(), 0);
        }
        try {
            return JacksonUtil.IGNORE_UNKNOWN_PROPERTIES_AND_ENUMS_JSON_MAPPER.treeToValue(
                    process(tokenProvider -> operations.execute(new TbAiOperation(ChannelProtocol.USAGE_GET, null), user, tokenProvider), user, false), ApiUsageInfo.class);
        } catch (Exception e) {
            log.warn("[{}][{}] Couldn't get AI API usage info", user.getTenantId(), user.getId(), e);
            return new ApiUsageInfo(Map.of(), 0);
        }
    }

    private void checkEnabled() {
        if (!isEnabled()) {
            throw new ThingsboardRuntimeException("AI feature is disabled", ThingsboardErrorCode.PERMISSION_DENIED);
        }
    }

    private TbAiClient.TokenProvider createTokenProvider(SecurityUser user) {
        TbAiTokenProvider provider = tokenProvider.orElseThrow(() -> new ThingsboardRuntimeException("AI feature is not available", ThingsboardErrorCode.BAD_REQUEST_PARAMS));
        return TbAiTokenProviderFactory.userScoped(provider, user);
    }

    private ServerSentEvent<String> processEvent(ServerSentEvent<String> event, SecurityUser user) {
        if ("creditsUsed".equals(event.event())) {
            JsonNode node = JacksonUtil.toJsonNode(event.data());
            if (node == null || !node.isObject() || !node.has("creditsUsed") || !node.get("creditsUsed").isIntegralNumber()) {
                log.error("Malformed creditsUsed event: {}", event);
                throw new IllegalStateException("Internal error");
            }
            reportCreditsUsed(user, node.get("creditsUsed").asInt());
            return null;
        }
        return event;
    }

    private static ServerSentEvent<String> errorEvent(String message) {
        return ServerSentEvent.<String>builder()
                .event("error")
                .data(JacksonUtil.toString(JacksonUtil.newObjectNode().put("message", message)))
                .build();
    }

    private static String toUserFriendlyMessage(Throwable e) {
        if (e instanceof TimeoutException) {
            return "Request timed out";
        } else if (e instanceof TbAiChannelUnavailableException) {
            return "Service unavailable";
        } else if (e instanceof TbAiChannelRejectedException || e instanceof ThingsboardRuntimeException) {
            return e.getMessage();
        }
        return "Internal error";
    }

    private JsonNode processResponse(TbAiResponse response, SecurityUser user) {
        reportCreditsUsed(user, response.getCreditsUsed());
        if (response.isSuccess()) {
            return response.getValue();
        } else {
            throw new ThingsboardRuntimeException(response.getError(), ThingsboardErrorCode.GENERAL);
        }
    }

    private void checkApiUsage(SecurityUser user) {
        if (!apiUsageStateService.getApiUsageState(user.getTenantId()).isAiEnabled()) {
            throw new ThingsboardRuntimeException("Out of AI credits", ThingsboardErrorCode.TOO_MANY_REQUESTS);
        }
    }

    private void reportCreditsUsed(User user, int creditsUsed) {
        if (creditsUsed > 0) {
            apiUsageReportClient.report(user.getTenantId(), user.getCustomerId(), ApiUsageRecordKey.AI_CREDITS_COUNT, creditsUsed);
        }
    }

    private static final String TRUNCATE_MARKER = "... (truncated)";

    private String truncate(String data) {
        return StringUtils.abbreviate(data, TRUNCATE_MARKER, aiSettings.getSseLogMaxDataLength());
    }

}
