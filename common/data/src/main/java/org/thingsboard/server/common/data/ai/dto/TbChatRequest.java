/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.common.data.ai.dto;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.thingsboard.server.common.data.ai.model.chat.AiChatModelConfig;

import java.util.ArrayList;
import java.util.List;

public record TbChatRequest(
        @Schema(
                requiredMode = Schema.RequiredMode.NOT_REQUIRED,
                accessMode = Schema.AccessMode.READ_WRITE,
                description = "A system-level instruction that frames the user's input, setting the persona, tone, and constraints for the generated response",
                example = "You are a helpful assistant. Only output valid JSON."
        )
        String systemMessage,

        @Schema(
                requiredMode = Schema.RequiredMode.REQUIRED,
                accessMode = Schema.AccessMode.READ_WRITE,
                description = "The actual user prompt that will be answered by the AI model"
        )
        @NotNull @Valid
        TbUserMessage userMessage,

        @Schema(
                requiredMode = Schema.RequiredMode.REQUIRED,
                accessMode = Schema.AccessMode.READ_WRITE,
                description = "Configuration of the AI chat model that should execute the request"
        )
        @NotNull @Valid
        AiChatModelConfig<?> chatModelConfig
) {

    public ChatRequest toLangChainChatRequest() {
        return ChatRequest.builder()
                .messages(getLangChainMessages())
                .build();
    }

    private List<ChatMessage> getLangChainMessages() {
        List<ChatMessage> messages = new ArrayList<>(2);

        if (systemMessage != null) {
            messages.add(SystemMessage.from(systemMessage));
        }

        List<Content> langChainContents = userMessage.contents().stream()
                .map(TbContent::toLangChainContent)
                .toList();

        messages.add(UserMessage.from(langChainContents));

        return messages;
    }

}
