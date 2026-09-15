// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.ai.model.chat;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.langchain4j.model.chat.ChatModel;
import org.thingsboard.server.common.data.ai.model.AiModelConfig;
import org.thingsboard.server.common.data.ai.model.AiModelType;

public sealed interface AiChatModelConfig<C extends AiChatModelConfig<C>> extends AiModelConfig
        permits
        OpenAiChatModelConfig, AzureOpenAiChatModelConfig, GoogleAiGeminiChatModelConfig,
        GoogleVertexAiGeminiChatModelConfig, MistralAiChatModelConfig, AnthropicChatModelConfig,
        AmazonBedrockChatModelConfig, GitHubModelsChatModelConfig, OllamaChatModelConfig {

    ChatModel configure(Langchain4jChatModelConfigurer configurer);

    @Override
    @JsonProperty(value = "modelType", access = JsonProperty.Access.READ_ONLY)
    default AiModelType modelType() {
        return AiModelType.CHAT;
    }

    Integer timeoutSeconds();

    Integer maxRetries();

    C withTimeoutSeconds(Integer timeoutSeconds);

    C withMaxRetries(Integer maxRetries);

    boolean supportsSchemalessJsonOutput();

    boolean supportsJsonSchemaOutput();

}
