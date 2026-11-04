// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ai.chat;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.thingsboard.ai.common.channel.ChannelProtocol;
import org.thingsboard.ai.common.channel.ChatOperationRequest;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.ai.TbAiService;
import org.thingsboard.server.service.ai.transport.TbAiOperation;
import org.thingsboard.server.service.ai.transport.TbAiOperations;
import org.thingsboard.server.service.ai.transport.TbAiTransport;
import org.thingsboard.server.service.ai.transport.TbAiTurnContext;
import org.thingsboard.server.service.ai.transport.TbAiTurnLink;
import org.thingsboard.server.service.security.model.SecurityUser;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Service
@TbCoreComponent
@RequiredArgsConstructor
class DefaultAiChatService implements AiChatService {

    private static final JsonNode TOOL_CARDS = JacksonUtil.newObjectNode().put("toolCards", true);

    private final TbAiService tbAiService;
    private final TbAiTransport tbAiTransport;
    private final TbAiOperations operations;

    @Override
    public JsonNode createChat(JsonNode request, SecurityUser user) {
        return tbAiService.process(tokenProvider -> operations.execute(
                new TbAiOperation(ChannelProtocol.CHAT_CREATE, new ChatOperationRequest(null, request)), user, tokenProvider), user, false);
    }

    @Override
    public void updateChat(UUID chatId, JsonNode request, SecurityUser user) {
        tbAiService.process(tokenProvider -> operations.execute(
                new TbAiOperation(ChannelProtocol.CHAT_UPDATE, new ChatOperationRequest(chatId, request)), user, tokenProvider), user, false);
    }

    @Override
    public JsonNode listChats(SecurityUser user) {
        return tbAiService.process(tokenProvider -> operations.execute(
                new TbAiOperation(ChannelProtocol.CHAT_LIST, null), user, tokenProvider), user, false);
    }

    @Override
    public JsonNode getChatMessages(UUID chatId, SecurityUser user) {
        return tbAiService.process(tokenProvider -> operations.execute(
                new TbAiOperation(ChannelProtocol.CHAT_MESSAGES, new ChatOperationRequest(chatId, TOOL_CARDS)), user, tokenProvider), user, false);
    }

    @Override
    public void deleteChat(UUID chatId, SecurityUser user) {
        tbAiService.process(tokenProvider -> operations.execute(
                new TbAiOperation(ChannelProtocol.CHAT_DELETE, new ChatOperationRequest(chatId, null)), user, tokenProvider), user, false);
    }

    @Override
    public Flux<ServerSentEvent<String>> sendChatMessage(
            UUID chatId, JsonNode request, String tbAccessToken, String acceptLanguage, String clientOrigin,
            SecurityUser user
    ) {
        var link = new TbAiTurnLink();
        return tbAiService.processStream(chatId, tokenProvider -> {
            var context = new TbAiTurnContext(user, tbAccessToken, acceptLanguage, tokenProvider, clientOrigin);
            return tbAiTransport.sendChatMessage(chatId, request, context, link);
        }, user, link, true);
    }

    @Override
    public Flux<ServerSentEvent<String>> resumeChat(UUID chatId, String tbAccessToken, String acceptLanguage, String clientOrigin, SecurityUser user) {
        var link = new TbAiTurnLink();
        return tbAiService.processStream(chatId, tokenProvider -> {
            var context = new TbAiTurnContext(user, tbAccessToken, acceptLanguage, tokenProvider, clientOrigin);
            return tbAiTransport.resumeChat(chatId, context, link);
        }, user, link, false);
    }

}
