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

import dev.langchain4j.model.chat.ChatModel;
import lombok.With;
import org.thingsboard.server.common.data.ai.model.AiModelType;
import org.thingsboard.server.common.data.ai.provider.AzureOpenAiProviderConfig;

import java.util.List;

public record AzureOpenAiChatModel(
        AiModelType modelType,
        AzureOpenAiProviderConfig providerConfig,
        @With Config modelConfig
) implements AiChatModel<AzureOpenAiChatModel.Config> {

    @With
    public record Config(
            String modelId,
            Double temperature,
            Double topP,
            Double frequencyPenalty,
            Double presencePenalty,
            Integer maxOutputTokens,
            List<String> stopSequences,
            Integer timeoutSeconds,
            Integer maxRetries
    ) implements AiChatModelConfig<AzureOpenAiChatModel.Config> {}

    @Override
    public ChatModel configure(Langchain4jChatModelConfigurer configurer) {
        return configurer.configureChatModel(this);
    }

}
