// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.ai;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.Futures;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.ai.model.chat.AiChatModelConfig;
import org.thingsboard.server.common.data.ai.model.chat.Langchain4jChatModelConfigurer;

@Service
@RequiredArgsConstructor
class AiChatModelServiceImpl implements AiChatModelService {

    private final Langchain4jChatModelConfigurer chatModelConfigurer;
    private final AiRequestsExecutor aiRequestsExecutor;

    @Override
    public <C extends AiChatModelConfig<C>> FluentFuture<ChatResponse> sendChatRequestAsync(AiChatModelConfig<C> chatModelConfig, ChatRequest chatRequest) {
        ChatModel langChainChatModel;
        try {
            langChainChatModel = chatModelConfig.configure(chatModelConfigurer);
        } catch (Throwable t) {
            return FluentFuture.from(Futures.immediateFailedFuture(t));
        }
        return aiRequestsExecutor.sendChatRequestAsync(langChainChatModel, chatRequest);
    }

}
