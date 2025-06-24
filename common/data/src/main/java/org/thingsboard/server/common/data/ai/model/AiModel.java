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
import org.thingsboard.server.common.data.ai.model.chat.GoogleAiGeminiChatModel;
import org.thingsboard.server.common.data.ai.model.chat.MistralAiChatModel;
import org.thingsboard.server.common.data.ai.model.chat.OpenAiChatModel;
import org.thingsboard.server.common.data.ai.provider.AiProviderConfig;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "modelId",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = OpenAiChatModel.class, name = "gpt-4o"),
        @JsonSubTypes.Type(value = OpenAiChatModel.class, name = "gpt-4o-mini"),
        @JsonSubTypes.Type(value = OpenAiChatModel.class, name = "gpt-4.1"),
        @JsonSubTypes.Type(value = OpenAiChatModel.class, name = "gpt-4.1-mini"),
        @JsonSubTypes.Type(value = OpenAiChatModel.class, name = "gpt-4.1-nano"),
        @JsonSubTypes.Type(value = OpenAiChatModel.class, name = "o4-mini"),
        // @JsonSubTypes.Type(value = OpenAiChatModel.class, name = "o3-pro"), needs verification with Gov ID :)
        // @JsonSubTypes.Type(value = OpenAiChatModel.class, name = "o3"),     needs verification with Gov ID :)
        @JsonSubTypes.Type(value = OpenAiChatModel.class, name = "o3-mini"),
        // @JsonSubTypes.Type(value = OpenAiChatModel.class, name = "o1-pro"), LC4j sends requests to v1/chat/completions, but o1-pro is only supported in v1/responses
        @JsonSubTypes.Type(value = OpenAiChatModel.class, name = "o1"),
        @JsonSubTypes.Type(value = GoogleAiGeminiChatModel.class, name = "gemini-2.5-flash"),
        @JsonSubTypes.Type(value = MistralAiChatModel.class, name = "mistral-medium-latest")
})
public interface AiModel<C extends AiModelConfig<C>> {

    AiProviderConfig providerConfig();

    AiModelType modelType();

    String modelId();

    C modelConfig();

    AiModel<C> withModelConfig(C config);

}
