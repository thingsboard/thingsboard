// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.api;

import com.google.common.util.concurrent.FluentFuture;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.thingsboard.server.common.data.ai.model.chat.AiChatModelConfig;

public interface RuleEngineAiChatModelService {

    <C extends AiChatModelConfig<C>> FluentFuture<ChatResponse> sendChatRequestAsync(AiChatModelConfig<C> chatModelConfig, ChatRequest chatRequest);

}
