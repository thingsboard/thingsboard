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

    boolean supportsJsonMode();

}
