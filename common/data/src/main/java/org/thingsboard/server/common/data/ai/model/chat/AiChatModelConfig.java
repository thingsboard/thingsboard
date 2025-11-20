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

import com.fasterxml.jackson.annotation.*;
import dev.langchain4j.model.chat.ChatModel;
import io.swagger.v3.oas.annotations.media.Schema;
import org.thingsboard.server.common.data.ai.model.AiModelConfig;
import org.thingsboard.server.common.data.ai.model.AiModelType;
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "provider",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = OpenAiChatModelConfig.class, name = "OPENAI"),
        @JsonSubTypes.Type(value = AzureOpenAiChatModelConfig.class, name = "AZURE_OPENAI"),
        @JsonSubTypes.Type(value = GoogleAiGeminiChatModelConfig.class, name = "GOOGLE_AI_GEMINI"),
        @JsonSubTypes.Type(value = GoogleVertexAiGeminiChatModelConfig.class, name = "GOOGLE_VERTEX_AI_GEMINI"),
        @JsonSubTypes.Type(value = MistralAiChatModelConfig.class, name = "MISTRAL_AI"),
        @JsonSubTypes.Type(value = AnthropicChatModelConfig.class, name = "ANTHROPIC"),
        @JsonSubTypes.Type(value = AmazonBedrockChatModelConfig.class, name = "AMAZON_BEDROCK"),
        @JsonSubTypes.Type(value = GitHubModelsChatModelConfig.class, name = "GITHUB_MODELS"),
        @JsonSubTypes.Type(value = OllamaChatModelConfig.class, name = "OLLAMA")
})
@Schema(oneOf  = {
AmazonBedrockChatModelConfig.class,
AnthropicChatModelConfig.class,
AzureOpenAiChatModelConfig.class,
GitHubModelsChatModelConfig.class,
GoogleAiGeminiChatModelConfig.class,
GoogleVertexAiGeminiChatModelConfig.class,
MistralAiChatModelConfig.class,
OllamaChatModelConfig.class,
OpenAiChatModelConfig.class
                })
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
