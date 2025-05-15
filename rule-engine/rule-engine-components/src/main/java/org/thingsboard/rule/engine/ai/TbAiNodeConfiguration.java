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

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
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

    @Override
    public TbAiNodeConfiguration defaultConfiguration() {
        var configuration = new TbAiNodeConfiguration();
        configuration.setSystemPrompt("""
            Take a deep breath and work on this step by step.
            You are an industry-leading IoT domain expert with deep experience in telemetry data analysis.
            Your task is to complete the user-provided task or answer a question.
            You may use additional context information called "Rule engine message payload", "Rule engine message metadata" and "Rule engine message type".
            Your response must be in JSON format.""");
        configuration.setUserPrompt("Tell me a joke");
        return configuration;
    }

}
