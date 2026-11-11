// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.ai.model.chat.OpenAiChatModelConfig;
import org.thingsboard.server.common.data.ai.provider.OpenAiProviderConfig;
import org.thingsboard.server.common.data.id.AiModelId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AiModelTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void infoJsonMatchesModelJsonExceptForNullProviderConfig() {
        AiModel model = openAiModel();

        JsonNode infoJson = mapper.valueToTree(AiModelInfo.from(model));
        ObjectNode expected = mapper.valueToTree(model);
        ((ObjectNode) expected.get("configuration")).putNull("providerConfig");

        assertThat(infoJson).isEqualTo(expected);
        assertThat(infoJson.toString()).doesNotContain("test-api-key");
    }

    @Test
    void modelJsonWritesProviderOnce() throws Exception {
        String json = mapper.writeValueAsString(openAiModel());

        assertThat(json.split("\"provider\"", -1)).hasSize(2);
        assertThat(mapper.readValue(json, AiModel.class).getConfiguration()).isEqualTo(openAiModel().getConfiguration());
    }

    @Test
    void infoJsonDeserializesAsInfoAndAsModel() throws Exception {
        AiModel model = openAiModel();
        AiModelInfo info = AiModelInfo.from(model);
        String json = mapper.writeValueAsString(info);

        assertThat(mapper.readValue(json, AiModelInfo.class)).isEqualTo(info);

        AiModel modelFromInfo = mapper.readValue(json, AiModel.class);
        assertThat(modelFromInfo.getConfiguration().providerConfig()).isNull();
        assertThat(modelFromInfo.getConfiguration()).isEqualTo(info.configuration());
        assertThat(modelFromInfo.getName()).isEqualTo(model.getName());
        assertThat(modelFromInfo.isCustomerAccessEnabled()).isTrue();
    }

    private static AiModel openAiModel() {
        AiModel model = AiModel.builder()
                .tenantId(TenantId.fromUUID(UUID.randomUUID()))
                .version(3L)
                .name("Test model")
                .customerAccessEnabled(true)
                .configuration(OpenAiChatModelConfig.builder()
                        .providerConfig(OpenAiProviderConfig.builder()
                                .baseUrl(OpenAiProviderConfig.OPENAI_OFFICIAL_BASE_URL)
                                .apiKey("test-api-key")
                                .build())
                        .modelId("gpt-4o")
                        .build())
                .build();
        model.setId(new AiModelId(UUID.randomUUID()));
        model.setCreatedTime(1_700_000_000_000L);
        return model;
    }

}
