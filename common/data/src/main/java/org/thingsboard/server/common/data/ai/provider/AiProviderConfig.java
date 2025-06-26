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
package org.thingsboard.server.common.data.ai.provider;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "provider"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = OpenAiProviderConfig.class, name = "OPENAI"),
        @JsonSubTypes.Type(value = AzureOpenAiProviderConfig.class, name = "AZURE_OPENAI"),
        @JsonSubTypes.Type(value = GoogleAiGeminiProviderConfig.class, name = "GOOGLE_AI_GEMINI"),
        @JsonSubTypes.Type(value = GoogleVertexAiGeminiProviderConfig.class, name = "GOOGLE_VERTEX_AI_GEMINI"),
        @JsonSubTypes.Type(value = MistralAiProviderConfig.class, name = "MISTRAL_AI"),
        @JsonSubTypes.Type(value = AnthropicProviderConfig.class, name = "ANTHROPIC"),
        @JsonSubTypes.Type(value = AmazonBedrockProviderConfig.class, name = "AMAZON_BEDROCK")
})
public sealed interface AiProviderConfig
        permits
        OpenAiProviderConfig, AzureOpenAiProviderConfig, GoogleAiGeminiProviderConfig,
        GoogleVertexAiGeminiProviderConfig, MistralAiProviderConfig, AnthropicProviderConfig,
        AmazonBedrockProviderConfig {

    AiProvider provider();

}
