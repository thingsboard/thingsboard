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
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.ai.model.chat.GoogleAiGeminiChatModel;
import org.thingsboard.server.common.data.ai.model.chat.Langchain4jChatModelConfigurer;
import org.thingsboard.server.common.data.ai.model.chat.MistralAiChatModel;
import org.thingsboard.server.common.data.ai.model.chat.OpenAiChatModel;

import java.time.Duration;

@Component
public class Langchain4jChatModelConfigurerImpl implements Langchain4jChatModelConfigurer {

    @Override
    public ChatModel configureChatModel(OpenAiChatModel chatModel) {
        OpenAiChatModel.Config modelConfig = chatModel.modelConfig();
        return dev.langchain4j.model.openai.OpenAiChatModel.builder()
                .apiKey(chatModel.providerConfig().apiKey())
                .modelName(chatModel.modelId())
                .temperature(modelConfig.temperature())
                .timeout(toDuration(modelConfig.timeoutSeconds()))
                .maxRetries(modelConfig.maxRetries())
                .build();
    }

    @Override
    public ChatModel configureChatModel(GoogleAiGeminiChatModel chatModel) {
        GoogleAiGeminiChatModel.Config modelConfig = chatModel.modelConfig();
        return dev.langchain4j.model.googleai.GoogleAiGeminiChatModel.builder()
                .apiKey(chatModel.providerConfig().apiKey())
                .modelName(chatModel.modelId())
                .temperature(modelConfig.temperature())
                .timeout(toDuration(modelConfig.timeoutSeconds()))
                .maxRetries(modelConfig.maxRetries())
                .build();
    }

    @Override
    public ChatModel configureChatModel(MistralAiChatModel chatModel) {
        MistralAiChatModel.Config modelConfig = chatModel.modelConfig();
        return dev.langchain4j.model.mistralai.MistralAiChatModel.builder()
                .apiKey(chatModel.providerConfig().apiKey())
                .modelName(chatModel.modelId())
                .temperature(modelConfig.temperature())
                .timeout(toDuration(modelConfig.timeoutSeconds()))
                .maxRetries(modelConfig.maxRetries())
                .build();
    }

    private static Duration toDuration(Integer timeoutSeconds) {
        return timeoutSeconds != null ? Duration.ofSeconds(timeoutSeconds) : null;
    }

}
