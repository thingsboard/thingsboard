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

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Schema(
        name = "MistralAiChatModelConfig",
        description = "Configuration for Mistral AI chat models"
)
public final class MistralAiChatModelConfig extends AiModelConfig {

    @Schema(
            requiredMode = Schema.RequiredMode.REQUIRED,
            accessMode = Schema.AccessMode.READ_WRITE,
            description = "Identifier of the AI model",
            allowableValues = "mistral-medium-latest",
            example = "mistral-medium-latest"
    )
    public String getModel() {
        return super.getModel();
    }

    @Schema(
            accessMode = Schema.AccessMode.READ_WRITE,
            description = "Sampling temperature to control randomness: 0.0 (most deterministic) to 1.0 (most creative)",
            example = "0.7"
    )
    private Double temperature;

    @Schema(
            accessMode = Schema.AccessMode.READ_WRITE,
            description = "Timeout (in seconds) for the entire HTTP call: applied to connect, read, and write operations"
    )
    private Integer timeoutSeconds;

    @Schema(
            accessMode = Schema.AccessMode.READ_WRITE,
            description = "Maximum number of times to retry an LLM call upon exception (except for non-retriable ones like authentication or invalid request errors)"
    )
    private Integer maxRetries;

}
