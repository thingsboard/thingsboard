// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.ai.provider;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import lombok.Builder;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

@Builder
public record OpenAiProviderConfig(
        String baseUrl,
        String apiKey
) implements AiProviderConfig {

    public static final String OPENAI_OFFICIAL_BASE_URL = "https://api.openai.com/v1";

    public OpenAiProviderConfig {
        baseUrl = Objects.requireNonNullElse(baseUrl, OPENAI_OFFICIAL_BASE_URL);
    }

    @JsonIgnore
    @AssertTrue(message = "API key is required when using the official OpenAI API")
    public boolean isValid() {
        if (baseUrl.equals(OPENAI_OFFICIAL_BASE_URL)) {
            return StringUtils.isNotBlank(apiKey);
        }
        return true;
    }

}
