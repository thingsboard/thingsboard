// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.sql;

import org.junit.jupiter.api.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.ai.model.AiModelConfig;
import org.thingsboard.server.dao.util.mapping.AiModelConfigConverter;
import org.thingsboard.server.dao.util.mapping.IotHubInstalledItemDescriptorConverter;

import static org.assertj.core.api.Assertions.assertThat;

class LegacyJsonbColumnsTest {

    private static final String LEGACY_AI_MODEL_CONFIG = "{\"topP\": 0.3, \"modelId\": \"gpt-4o\", \"provider\": \"OPENAI\", \"modelType\": \"CHAT\", \"maxRetries\": 2, \"temperature\": 0.5, \"providerConfig\": {\"apiKey\": \"test-api-key\", \"baseUrl\": \"https://api.openai.com/v1\"}, \"timeoutSeconds\": 60, \"maxOutputTokens\": 1000, \"presencePenalty\": 0.2, \"frequencyPenalty\": 0.1}";
    private static final String LEGACY_IOT_HUB_DESCRIPTOR = "{\"type\": \"RULE_CHAIN\", \"ruleChainId\": {\"id\": \"4dda2206-0fd7-4c10-aea1-39535f726360\", \"entityType\": \"RULE_CHAIN\"}, \"targetProfileId\": {\"id\": \"4103fb6e-ce28-4115-a0fc-e4492ffe4d9b\", \"entityType\": \"DEVICE_PROFILE\"}}";

    @Test
    void readsLegacyAiModelConfig() {
        AiModelConfigConverter converter = new AiModelConfigConverter();
        AiModelConfig config = converter.convertToEntityAttribute(LEGACY_AI_MODEL_CONFIG);
        assertThat(JacksonUtil.toJsonNode(converter.convertToDatabaseColumn(config)))
                .isEqualTo(JacksonUtil.toJsonNode(LEGACY_AI_MODEL_CONFIG));
    }

    @Test
    void readsLegacyIotHubDescriptor() {
        IotHubInstalledItemDescriptorConverter converter = new IotHubInstalledItemDescriptorConverter();
        assertThat(JacksonUtil.toJsonNode(converter.convertToDatabaseColumn(converter.convertToEntityAttribute(LEGACY_IOT_HUB_DESCRIPTOR))))
                .isEqualTo(JacksonUtil.toJsonNode(LEGACY_IOT_HUB_DESCRIPTOR));
    }

}
