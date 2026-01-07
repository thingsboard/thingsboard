/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Builder;
import lombok.With;
import org.thingsboard.server.common.data.ai.provider.AiProvider;
import org.thingsboard.server.common.data.ai.provider.GitHubModelsProviderConfig;

@Builder
public record GitHubModelsChatModelConfig(
        @NotNull @Valid GitHubModelsProviderConfig providerConfig,
        @NotBlank String modelId,
        @PositiveOrZero Double temperature,
        @Positive @Max(1) Double topP,
        Double frequencyPenalty,
        Double presencePenalty,
        Integer maxOutputTokens,
        @With @Positive Integer timeoutSeconds,
        @With @PositiveOrZero Integer maxRetries
) implements AiChatModelConfig<GitHubModelsChatModelConfig> {

    @Override
    public AiProvider provider() {
        return AiProvider.GITHUB_MODELS;
    }

    @Override
    public ChatModel configure(Langchain4jChatModelConfigurer configurer) {
        return configurer.configureChatModel(this);
    }

    @Override
    public boolean supportsJsonMode() {
        return false;
    }

}
