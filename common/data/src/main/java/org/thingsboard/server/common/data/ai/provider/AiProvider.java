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

import org.thingsboard.server.common.data.ai.model.chat.AmazonBedrockChatModel;
import org.thingsboard.server.common.data.ai.model.chat.AnthropicChatModel;
import org.thingsboard.server.common.data.ai.model.chat.AzureOpenAiChatModel;
import org.thingsboard.server.common.data.ai.model.chat.GitHubModelsChatModel;
import org.thingsboard.server.common.data.ai.model.chat.GoogleAiGeminiChatModel;
import org.thingsboard.server.common.data.ai.model.chat.GoogleVertexAiGeminiChatModel;
import org.thingsboard.server.common.data.ai.model.chat.MistralAiChatModel;
import org.thingsboard.server.common.data.ai.model.chat.OpenAiChatModel;

public enum AiProvider {

    OPENAI(OpenAiChatModel.class),
    AZURE_OPENAI(AzureOpenAiChatModel.class),
    GOOGLE_AI_GEMINI(GoogleAiGeminiChatModel.class),
    GOOGLE_VERTEX_AI_GEMINI(GoogleVertexAiGeminiChatModel.class),
    MISTRAL_AI(MistralAiChatModel.class),
    ANTHROPIC(AnthropicChatModel.class),
    AMAZON_BEDROCK(AmazonBedrockChatModel.class),
    GITHUB_MODELS(GitHubModelsChatModel.class);

    private final Class<?> defaultModelClass;

    AiProvider(Class<?> defaultModelClass) {
        this.defaultModelClass = defaultModelClass;
    }

    public Class<?> getDefaultModelClass() {
        return defaultModelClass;
    }

}
