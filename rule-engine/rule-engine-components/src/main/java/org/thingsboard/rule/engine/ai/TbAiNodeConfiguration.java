// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.ai;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;
import org.thingsboard.server.common.data.id.AiModelId;
import org.thingsboard.server.common.data.validation.Length;

import java.util.Set;
import java.util.UUID;

import static org.thingsboard.rule.engine.ai.TbResponseFormat.TbJsonResponseFormat;

@Data
public class TbAiNodeConfiguration implements NodeConfiguration<TbAiNodeConfiguration> {

    @NotNull
    private AiModelId modelId;

    @Length(min = 1, max = 500_000)
    private String systemPrompt;

    @NotBlank
    @Length(min = 1, max = 500_000)
    private String userPrompt;

    private Set<@NotNull(message = "references to resources cannot be null") UUID> resourceIds;

    @NotNull
    @Valid
    private TbResponseFormat responseFormat;

    @Min(value = 1, message = "must be at least 1 second")
    @Max(value = 600, message = "cannot exceed 600 seconds (10 minutes)")
    private int timeoutSeconds;

    private boolean forceAck;

    @Override
    public TbAiNodeConfiguration defaultConfiguration() {
        var configuration = new TbAiNodeConfiguration();
        configuration.setSystemPrompt(
                "You are a helpful AI assistant. Your primary function is to process the user's request and respond with a valid JSON object. " +
                        "Do not include any text, explanations, or markdown formatting before or after the JSON output."
        );
        configuration.setResponseFormat(new TbJsonResponseFormat());
        configuration.setTimeoutSeconds(60);
        configuration.setForceAck(true);
        return configuration;
    }

}
