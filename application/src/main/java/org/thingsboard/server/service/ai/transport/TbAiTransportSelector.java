// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ai.transport;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.thingsboard.server.queue.util.TbCoreComponent;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Slf4j
@Primary
@Component
@TbCoreComponent
@RequiredArgsConstructor
class TbAiTransportSelector implements TbAiTransport {

    private final SseTbAiTransport sseTransport;
    private final ChannelTbAiTransport channelTransport;
    private final TbAiChannelAvailability availability;

    @Override
    public Flux<ServerSentEvent<String>> sendChatMessage(UUID chatId, JsonNode request, TbAiTurnContext context) {
        if (!availability.isUsable()) {
            return sseTransport.sendChatMessage(chatId, request, context);
        }
        return channelTransport.sendChatMessage(chatId, request, context)
                .onErrorResume(TbAiChannelUnavailableException.class, e -> {
                    log.warn("[{}] AI channel unavailable, using SSE for {}: {}", chatId, availability.fallbackBackoff(), e.getMessage());
                    availability.markUnavailable();
                    return sseTransport.sendChatMessage(chatId, request, context);
                });
    }

}
