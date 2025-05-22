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
package org.thingsboard.server.service.ai;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.mistralai.MistralAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.rule.engine.api.RuleEngineAiService;
import org.thingsboard.server.common.data.ai.AiSettings;
import org.thingsboard.server.common.data.ai.model.GoogleAiGeminiChatModelConfig;
import org.thingsboard.server.common.data.ai.model.MistralAiChatModelConfig;
import org.thingsboard.server.common.data.ai.model.OpenAiChatModelConfig;
import org.thingsboard.server.common.data.id.AiSettingsId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.ai.AiSettingsService;

import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
class AiServiceImpl implements RuleEngineAiService {

    private final AiSettingsService aiSettingsService;

    @Override
    public ChatModel configureChatModel(TenantId tenantId, AiSettingsId aiSettingsId) {
        Optional<AiSettings> aiSettingsOpt = aiSettingsService.findAiSettingsById(tenantId, aiSettingsId);
        if (aiSettingsOpt.isEmpty()) {
            throw new NoSuchElementException("AI settings with ID: " + aiSettingsId + " were not found");
        }
        var aiSettings = aiSettingsOpt.get();

        return switch (aiSettings.getProvider()) {
            case OPENAI -> {
                var modelBuilder = OpenAiChatModel.builder()
                        .apiKey(aiSettings.getProviderConfig().getApiKey())
                        .modelName(aiSettings.getModel());

                if (aiSettings.getModelConfig() instanceof OpenAiChatModelConfig config) {
                    modelBuilder.temperature(config.getTemperature());
                }

                yield modelBuilder.build();
            }
            case MISTRAL_AI -> {
                var modelBuilder = MistralAiChatModel.builder()
                        .apiKey(aiSettings.getProviderConfig().getApiKey())
                        .modelName(aiSettings.getModel());

                if (aiSettings.getModelConfig() instanceof MistralAiChatModelConfig config) {
                    modelBuilder.temperature(config.getTemperature());
                }

                yield modelBuilder.build();
            }
            case GOOGLE_AI_GEMINI -> {
                var modelBuilder = GoogleAiGeminiChatModel.builder()
                        .apiKey(aiSettings.getProviderConfig().getApiKey())
                        .modelName(aiSettings.getModel());

                if (aiSettings.getModelConfig() instanceof GoogleAiGeminiChatModelConfig config) {
                    modelBuilder.temperature(config.getTemperature());
                }

                yield modelBuilder.build();
            }
        };
    }

}
