// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ai.chat;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.codec.ServerSentEvent;
import org.thingsboard.server.service.security.model.SecurityUser;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface AiChatService {

    JsonNode createChat(JsonNode request, SecurityUser user);

    void updateChat(UUID chatId, JsonNode request, SecurityUser user);

    JsonNode listChats(SecurityUser user);

    JsonNode getChatMessages(UUID chatId, SecurityUser user);

    void deleteChat(UUID chatId, SecurityUser user);

    Flux<ServerSentEvent<String>> sendChatMessage(
            UUID chatId, JsonNode request, String tbAccessToken, String acceptLanguage, SecurityUser user
    );

}
