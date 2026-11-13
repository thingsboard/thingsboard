// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.Futures;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.ai.model.chat.AiChatModelConfig;
import org.thingsboard.server.common.data.ai.model.chat.Langchain4jChatModelConfigurer;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.secret.SecretConfigurationService;

@Service
@RequiredArgsConstructor
class AiChatModelServiceImpl implements AiChatModelService {

    private final Langchain4jChatModelConfigurer chatModelConfigurer;
    private final AiRequestsExecutor aiRequestsExecutor;
    private final SecretConfigurationService secretConfigurationService;

    @Override
    public <C extends AiChatModelConfig<C>> FluentFuture<ChatResponse> sendChatRequestAsync(TenantId tenantId, AiChatModelConfig<C> chatModelConfig, ChatRequest chatRequest) {
        AiChatModelConfig<C> modelConfigWithSecretsReplaced = replaceSecrets(tenantId, chatModelConfig);
        ChatModel langChainChatModel;
        try {
            langChainChatModel = modelConfigWithSecretsReplaced.configure(chatModelConfigurer);
        } catch (Throwable t) {
            return FluentFuture.from(Futures.immediateFailedFuture(t));
        }
        return aiRequestsExecutor.sendChatRequestAsync(langChainChatModel, chatRequest);
    }

    private <C extends AiChatModelConfig<C>> AiChatModelConfig<C> replaceSecrets(TenantId tenantId, AiChatModelConfig<C> chatModelConfig) {
        JsonNode modelConfigJson = JacksonUtil.valueToTree(chatModelConfig);
        secretConfigurationService.replaceSecretUsages(tenantId, modelConfigJson);
        return JacksonUtil.convertValue(modelConfigJson, new TypeReference<>() {});
    }

}
