// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ai.transport;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.thingsboard.ai.common.client.TbAiClient;
import org.thingsboard.server.queue.util.TbCoreComponent;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Component
@TbCoreComponent
@RequiredArgsConstructor
class SseTbAiTransport implements TbAiTransport {

    private final TbAiClient tbAiClient;

    @Override
    public Flux<ServerSentEvent<String>> sendChatMessage(UUID chatId, JsonNode request, TbAiTurnContext context) {
        return tbAiClient.sendChatMessage(chatId, request, context.tbAccessToken(), context.acceptLanguage(),
                context.tokenProvider());
    }

}
