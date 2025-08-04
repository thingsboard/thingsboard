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
package org.thingsboard.server.service.ai.tbel;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.ai.dto.OpenAiTbelChatModelConfigProperties;
import org.thingsboard.server.common.data.ai.model.chat.OpenAiTbelChatModelConfig;
import org.thingsboard.server.common.data.ai.provider.OpenAiTbelProviderConfig;

@Component
@RequiredArgsConstructor
public class DefaultOpenAiTbelChatModelConfigFactory {

    public OpenAiTbelChatModelConfig create() {
        OpenAiTbelChatModelConfigProperties props = new OpenAiTbelChatModelConfigProperties();

        // Use env variable only if not set in the entity
        if (props.getApiKey() == null) {
            props.setApiKey(System.getenv("OPENAI_API_TBEL_KEY"));
        }
        if (props.getApiKey() == null) {
//            props.setApiKey("you OPENAI_API_TBEL_KEY"); // NB: test-only key!
            // TODO: REMOVE before production
            props.setApiKey("sk-..."); // NB: test-only key!
        }
        if (props.getApiKey() != null) {
            OpenAiTbelProviderConfig provider = new OpenAiTbelProviderConfig(
                    props.getApiKey(),
                    props.getBaseUrl());
            return OpenAiTbelChatModelConfig.builder()
                    .providerConfig(provider)
                    .modelId(props.getModelId())
                    .temperature(props.getTemperature())
                    .topP(props.getTopP())
                    .frequencyPenalty(props.getFrequencyPenalty())
                    .presencePenalty(props.getPresencePenalty())
                    .maxOutputTokens(props.getMaxOutputTokens())
                    .timeoutSeconds(props.getTimeoutSeconds())
                    .maxRetries(props.getMaxRetries())
                    .build();

        } else {
            return null;
        }
    }
}
