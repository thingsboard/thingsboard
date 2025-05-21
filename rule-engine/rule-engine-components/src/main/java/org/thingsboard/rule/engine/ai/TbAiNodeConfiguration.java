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
package org.thingsboard.rule.engine.ai;

import com.fasterxml.jackson.databind.JsonNode;
import dev.langchain4j.model.chat.request.ResponseFormatType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.thingsboard.common.util.JsonSchemaUtils;
import org.thingsboard.rule.engine.api.NodeConfiguration;
import org.thingsboard.server.common.data.id.AiSettingsId;
import org.thingsboard.server.common.data.validation.Length;

@Data
public class TbAiNodeConfiguration implements NodeConfiguration<TbAiNodeConfiguration> {

    @NotNull
    private AiSettingsId aiSettingsId;

    @NotBlank
    @Length(min = 1, max = 1000)
    private String systemPrompt;

    @NotBlank
    @Length(min = 1, max = 1000)
    private String userPrompt;

    @NotNull
    private ResponseFormatType responseFormatType;

    private JsonNode jsonSchema;

    @AssertTrue(message = "provided JSON Schema must conform to the Draft 2020-12 meta-schema")
    public boolean isJsonSchemaValid() {
        return jsonSchema == null || JsonSchemaUtils.isValidJsonSchema(jsonSchema);
    }

    @Override
    public TbAiNodeConfiguration defaultConfiguration() {
        var configuration = new TbAiNodeConfiguration();
        configuration.setSystemPrompt("You are helpful assistant. Your response must be in JSON format.");
        configuration.setUserPrompt("Tell me a joke.");
        configuration.setResponseFormatType(ResponseFormatType.JSON);
        return configuration;
    }

}
