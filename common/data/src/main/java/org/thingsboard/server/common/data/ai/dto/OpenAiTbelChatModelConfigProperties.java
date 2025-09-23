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
package org.thingsboard.server.common.data.ai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
public class OpenAiTbelChatModelConfigProperties {

    @NotBlank
    private String apiKey;

    private String baseUrl = "https://api.openai.com/v1";

    @NotBlank
//    private String modelId = "gpt-3.5-turbo";
    private String modelId = "gpt-4o";

    @PositiveOrZero
    private Double temperature = 0.0;

    @Positive
    @Max(1)
    private Double topP = 1.0;

    private Double frequencyPenalty = 0.0;
    private Double presencePenalty = 0.0;

    @Positive
    private Integer maxOutputTokens = 1024;

    @Positive
    private Integer timeoutSeconds = 15;

    @PositiveOrZero
    private Integer maxRetries = 2;
}