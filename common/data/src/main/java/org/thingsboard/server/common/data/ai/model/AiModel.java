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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.thingsboard.server.common.data.ai.model.chat.AmazonBedrockChatModel;
import org.thingsboard.server.common.data.ai.model.chat.AnthropicChatModel;
import org.thingsboard.server.common.data.ai.model.chat.AzureOpenAiChatModel;
import org.thingsboard.server.common.data.ai.model.chat.GitHubModelsChatModel;
import org.thingsboard.server.common.data.ai.model.chat.GoogleAiGeminiChatModel;
import org.thingsboard.server.common.data.ai.model.chat.GoogleVertexAiGeminiChatModel;
import org.thingsboard.server.common.data.ai.model.chat.MistralAiChatModel;
import org.thingsboard.server.common.data.ai.model.chat.OpenAiChatModel;
import org.thingsboard.server.common.data.ai.provider.AiProvider;
import org.thingsboard.server.common.data.ai.provider.AiProviderConfig;
import org.thingsboard.server.common.data.ai.provider.AmazonBedrockProviderConfig;
import org.thingsboard.server.common.data.ai.provider.AnthropicProviderConfig;
import org.thingsboard.server.common.data.ai.provider.AzureOpenAiProviderConfig;
import org.thingsboard.server.common.data.ai.provider.GitHubModelsProviderConfig;
import org.thingsboard.server.common.data.ai.provider.GoogleAiGeminiProviderConfig;
import org.thingsboard.server.common.data.ai.provider.GoogleVertexAiGeminiProviderConfig;
import org.thingsboard.server.common.data.ai.provider.MistralAiProviderConfig;
import org.thingsboard.server.common.data.ai.provider.OpenAiProviderConfig;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "provider",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = OpenAiChatModel.class, name = "OPENAI"),
        @JsonSubTypes.Type(value = AzureOpenAiChatModel.class, name = "AZURE_OPENAI"),
        @JsonSubTypes.Type(value = GoogleAiGeminiChatModel.class, name = "GOOGLE_AI_GEMINI"),
        @JsonSubTypes.Type(value = GoogleVertexAiGeminiChatModel.class, name = "GOOGLE_VERTEX_AI_GEMINI"),
        @JsonSubTypes.Type(value = MistralAiChatModel.class, name = "MISTRAL_AI"),
        @JsonSubTypes.Type(value = AnthropicChatModel.class, name = "ANTHROPIC"),
        @JsonSubTypes.Type(value = AmazonBedrockChatModel.class, name = "AMAZON_BEDROCK"),
        @JsonSubTypes.Type(value = GitHubModelsChatModel.class, name = "GITHUB_MODELS")
})
public interface AiModel<C extends AiModelConfig> {

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
            @JsonSubTypes.Type(value = GitHubModelsProviderConfig.class, name = "GITHUB_MODELS")
    })
    AiProviderConfig providerConfig();

    @JsonProperty("modelType")
    AiModelType modelType();

    C modelConfig();

    AiModel<C> withModelConfig(C config);

}
