// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.ai.model.chat.OpenAiChatModelConfig;
import org.thingsboard.server.common.data.ai.provider.AiProvider;
import org.thingsboard.server.common.data.ai.provider.OpenAiProviderConfig;

import static org.assertj.core.api.Assertions.assertThat;

class AiModelTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void modelJsonCarriesProviderAndModelIdOnlyInsideConfiguration() {
        JsonNode json = mapper.valueToTree(openAiModel());

        assertThat(json.has("provider")).isFalse();
        assertThat(json.has("modelId")).isFalse();
        assertThat(json.get("configuration").get("provider").asText()).isEqualTo("OPENAI");
        assertThat(json.get("configuration").get("modelId").asText()).isEqualTo("gpt-4o");
    }

    @Test
    void infoCopiedFromModelCarriesProviderAndModelId() {
        AiModelInfo info = new AiModelInfo(openAiModel());

        assertThat(info.getProvider()).isEqualTo(AiProvider.OPENAI);
        assertThat(info.getModelId()).isEqualTo("gpt-4o");

        JsonNode json = mapper.valueToTree(info);
        assertThat(json.get("provider").asText()).isEqualTo("OPENAI");
        assertThat(json.get("modelId").asText()).isEqualTo("gpt-4o");
        assertThat(json.has("configuration")).isFalse();
    }

    private static AiModel openAiModel() {
        return AiModel.builder()
                .name("Test model")
                .configuration(OpenAiChatModelConfig.builder()
                        .providerConfig(OpenAiProviderConfig.builder()
                                .baseUrl(OpenAiProviderConfig.OPENAI_OFFICIAL_BASE_URL)
                                .apiKey("test-api-key")
                                .build())
                        .modelId("gpt-4o")
                        .build())
                .build();
    }

}
