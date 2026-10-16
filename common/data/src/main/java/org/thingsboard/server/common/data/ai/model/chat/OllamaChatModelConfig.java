// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.ai.model.chat;

import dev.langchain4j.model.chat.ChatModel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Builder;
import lombok.With;
import org.thingsboard.server.common.data.ai.provider.AiProvider;
import org.thingsboard.server.common.data.ai.provider.OllamaProviderConfig;

@Builder
public record OllamaChatModelConfig(
        @NotNull @Valid OllamaProviderConfig providerConfig,
        @NotBlank String modelId,
        @PositiveOrZero Double temperature,
        @Positive @Max(1) Double topP,
        @PositiveOrZero Integer topK,
        Integer contextLength,
        Integer maxOutputTokens,
        @With @Positive Integer timeoutSeconds,
        @With @PositiveOrZero Integer maxRetries
) implements AiChatModelConfig<OllamaChatModelConfig> {

    @Override
    public AiProvider provider() {
        return AiProvider.OLLAMA;
    }

    @Override
    public ChatModel configure(Langchain4jChatModelConfigurer configurer) {
        return configurer.configureChatModel(this);
    }

    @Override
    public boolean supportsSchemalessJsonOutput() {
        return true;
    }

    @Override
    public boolean supportsJsonSchemaOutput() {
        return true;
    }

}
