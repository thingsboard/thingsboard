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
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "model",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = OpenAiChatModelConfig.class, name = "gpt-4o"),
        @JsonSubTypes.Type(value = OpenAiChatModelConfig.class, name = "gpt-4o-mini"),
        @JsonSubTypes.Type(value = GoogleAiGeminiChatModelConfig.class, name = "gemini-2.0-flash"),
        @JsonSubTypes.Type(value = MistralAiChatModelConfig.class, name = "mistral-medium-latest")
})
public abstract class AiModelConfig {

    @Schema(
            requiredMode = Schema.RequiredMode.REQUIRED,
            accessMode = Schema.AccessMode.READ_WRITE,
            description = "Identifier of the AI model"
    )
    private String model;

}
