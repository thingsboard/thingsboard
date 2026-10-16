// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ai.transport;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface TbAiTransport {

    Flux<ServerSentEvent<String>> sendChatMessage(UUID chatId, JsonNode request, TbAiTurnContext context);

}
