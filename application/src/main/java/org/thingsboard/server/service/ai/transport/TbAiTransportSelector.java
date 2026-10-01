// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ai.transport;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.thingsboard.server.queue.util.TbCoreComponent;
import reactor.core.publisher.Flux;

import java.time.Clock;
import java.time.Duration;
import java.util.UUID;

@Slf4j
@Primary
@Component
@TbCoreComponent
class TbAiTransportSelector implements TbAiTransport {

    private final SseTbAiTransport sseTransport;
    private final ChannelTbAiTransport channelTransport;
    private final boolean channelEnabled;
    private final Duration fallbackBackoff;
    private final Clock clock;

    private volatile long channelUnavailableUntil;

    @Autowired
    TbAiTransportSelector(SseTbAiTransport sseTransport,
                          ChannelTbAiTransport channelTransport,
                          @Value("${TB_AI_CHANNEL_ENABLED:true}") boolean channelEnabled,
                          @Value("${TB_AI_CHANNEL_FALLBACK_BACKOFF_MINUTES:10}") long fallbackBackoffMinutes) {
        this(sseTransport, channelTransport, channelEnabled, Duration.ofMinutes(fallbackBackoffMinutes), Clock.systemUTC());
    }

    TbAiTransportSelector(SseTbAiTransport sseTransport, ChannelTbAiTransport channelTransport,
                          boolean channelEnabled, Duration fallbackBackoff, Clock clock) {
        this.sseTransport = sseTransport;
        this.channelTransport = channelTransport;
        this.channelEnabled = channelEnabled;
        this.fallbackBackoff = fallbackBackoff;
        this.clock = clock;
    }

    @Override
    public Flux<ServerSentEvent<String>> sendChatMessage(UUID chatId, JsonNode request, TbAiTurnContext context) {
        if (!channelEnabled || clock.millis() < channelUnavailableUntil) {
            return sseTransport.sendChatMessage(chatId, request, context);
        }
        return channelTransport.sendChatMessage(chatId, request, context)
                .onErrorResume(TbAiChannelUnavailableException.class, e -> {
                    log.warn("[{}] AI channel unavailable, using SSE for {}: {}", chatId, fallbackBackoff, e.getMessage());
                    channelUnavailableUntil = clock.millis() + fallbackBackoff.toMillis();
                    return sseTransport.sendChatMessage(chatId, request, context);
                });
    }

}
