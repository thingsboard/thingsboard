// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.common.data.ai.dto;

import dev.langchain4j.model.chat.request.ChatRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.thingsboard.server.common.data.ai.model.chat.AiChatModelConfig;

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
        return new TbChatPrompt(systemMessage, userMessage).toLangChainChatRequest();
    }

}
