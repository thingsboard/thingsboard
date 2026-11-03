// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
