/**
 * Copyright © 2016-2025 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.ai;

import com.google.common.util.concurrent.FluentFuture;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.ModelProvider;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.ai.model.chat.AiChatModelConfig;
import org.thingsboard.server.common.data.ai.model.chat.Langchain4jChatModelConfigurer;

import java.util.List;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.StringUtils.escapeJson;

@Service
@RequiredArgsConstructor
class AiChatModelServiceImpl implements AiChatModelService {

    private final Langchain4jChatModelConfigurer chatModelConfigurer;
    private final AiRequestsExecutor aiRequestsExecutor;

    @Override
    public <C extends AiChatModelConfig<C>> FluentFuture<ChatResponse> sendChatRequestAsync(AiChatModelConfig<C> chatModelConfig, ChatRequest chatRequest) {
        ChatModel langChainChatModel = chatModelConfig.configure(chatModelConfigurer);
        if (langChainChatModel.provider() == ModelProvider.GITHUB_MODELS) {
            chatRequest = prepareGithubChatRequest(chatRequest);
        }
        return aiRequestsExecutor.sendChatRequestAsync(langChainChatModel, chatRequest);
    }

    private ChatRequest prepareGithubChatRequest(ChatRequest chatRequest) {
        List<ChatMessage> messages = chatRequest.messages().stream()
                        .map(this::prepareUserMessage)
                        .collect(Collectors.toList());

        return ChatRequest.builder()
                .messages(messages)
                .responseFormat(chatRequest.responseFormat())
                .build();
    }

    private ChatMessage prepareUserMessage(ChatMessage message) {
        if (message instanceof UserMessage userMessage) {
            List<Content> newContents = userMessage.contents().stream()
                    .map(this::prepareContent)
                    .collect(Collectors.toList());

            return UserMessage.from(newContents);
        }
        return message;
    }

    private Content prepareContent(Content content) {
        if (content instanceof TextContent txt) {
            return new TextContent(escapeJson(txt.text()));
        }
        return content;
    }

}
