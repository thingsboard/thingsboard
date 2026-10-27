// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ai.transport;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface TbAiTransport {

    Flux<ServerSentEvent<String>> sendChatMessage(UUID chatId, JsonNode request, TbAiTurnContext context, TbAiTurnLink link);

    /**
     * Follows the turn running in the chat: a {@code turnResumed} event, every event of the turn so far, then the live
     * ones. Completes at once when no turn is running or when TB AI cannot resume turns.
     */
    Flux<ServerSentEvent<String>> resumeChat(UUID chatId, TbAiTurnContext context, TbAiTurnLink link);

}
