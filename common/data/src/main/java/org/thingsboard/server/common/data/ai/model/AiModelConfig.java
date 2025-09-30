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
package org.thingsboard.server.common.data.ai.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.thingsboard.server.common.data.ai.model.chat.AmazonBedrockChatModelConfig;
import org.thingsboard.server.common.data.ai.model.chat.AnthropicChatModelConfig;
import org.thingsboard.server.common.data.ai.model.chat.AzureOpenAiChatModelConfig;
import org.thingsboard.server.common.data.ai.model.chat.GitHubModelsChatModelConfig;
import org.thingsboard.server.common.data.ai.model.chat.GoogleAiGeminiChatModelConfig;
import org.thingsboard.server.common.data.ai.model.chat.GoogleVertexAiGeminiChatModelConfig;
import org.thingsboard.server.common.data.ai.model.chat.MistralAiChatModelConfig;
import org.thingsboard.server.common.data.ai.model.chat.OllamaChatModelConfig;
import org.thingsboard.server.common.data.ai.model.chat.OpenAiChatModelConfig;
import org.thingsboard.server.common.data.ai.provider.AiProvider;
import org.thingsboard.server.common.data.ai.provider.AiProviderConfig;
import org.thingsboard.server.common.data.ai.provider.AmazonBedrockProviderConfig;
import org.thingsboard.server.common.data.ai.provider.AnthropicProviderConfig;
import org.thingsboard.server.common.data.ai.provider.AzureOpenAiProviderConfig;
import org.thingsboard.server.common.data.ai.provider.GitHubModelsProviderConfig;
import org.thingsboard.server.common.data.ai.provider.GoogleAiGeminiProviderConfig;
import org.thingsboard.server.common.data.ai.provider.GoogleVertexAiGeminiProviderConfig;
import org.thingsboard.server.common.data.ai.provider.MistralAiProviderConfig;
import org.thingsboard.server.common.data.ai.provider.OllamaProviderConfig;
import org.thingsboard.server.common.data.ai.provider.OpenAiProviderConfig;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
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
public interface AiModelConfig {

    AiProvider provider();

    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
            property = "provider"
    )
    @JsonSubTypes({
            @JsonSubTypes.Type(value = OpenAiProviderConfig.class, name = "OPENAI"),
            @JsonSubTypes.Type(value = AzureOpenAiProviderConfig.class, name = "AZURE_OPENAI"),
            @JsonSubTypes.Type(value = GoogleAiGeminiProviderConfig.class, name = "GOOGLE_AI_GEMINI"),
            @JsonSubTypes.Type(value = GoogleVertexAiGeminiProviderConfig.class, name = "GOOGLE_VERTEX_AI_GEMINI"),
            @JsonSubTypes.Type(value = MistralAiProviderConfig.class, name = "MISTRAL_AI"),
            @JsonSubTypes.Type(value = AnthropicProviderConfig.class, name = "ANTHROPIC"),
            @JsonSubTypes.Type(value = AmazonBedrockProviderConfig.class, name = "AMAZON_BEDROCK"),
            @JsonSubTypes.Type(value = GitHubModelsProviderConfig.class, name = "GITHUB_MODELS"),
            @JsonSubTypes.Type(value = OllamaProviderConfig.class, name = "OLLAMA")
    })
    AiProviderConfig providerConfig();

    AiModelType modelType();

}
