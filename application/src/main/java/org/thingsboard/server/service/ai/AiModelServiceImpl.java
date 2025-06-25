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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.rule.engine.api.RuleEngineAiModelService;
import org.thingsboard.server.common.data.ai.model.chat.AiChatModel;
import org.thingsboard.server.common.data.ai.model.chat.AiChatModelConfig;
import org.thingsboard.server.common.data.ai.model.chat.Langchain4jChatModelConfigurer;

@Service
@RequiredArgsConstructor
class AiModelServiceImpl implements RuleEngineAiModelService {

    private final Langchain4jChatModelConfigurer chatModelConfigurer;

    @Override
    public <C extends AiChatModelConfig<C>> ChatModel configureChatModel(AiChatModel<C> chatModel) {
        return chatModel.configure(chatModelConfigurer);
    }

}
