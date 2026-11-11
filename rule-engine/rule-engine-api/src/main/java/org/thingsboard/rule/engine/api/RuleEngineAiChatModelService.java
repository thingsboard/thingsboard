// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.rule.engine.api;

import com.google.common.util.concurrent.FluentFuture;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.thingsboard.server.common.data.ai.model.chat.AiChatModelConfig;
import org.thingsboard.server.common.data.id.TenantId;

public interface RuleEngineAiChatModelService {

    <C extends AiChatModelConfig<C>> FluentFuture<ChatResponse> sendChatRequestAsync(
            TenantId tenantId, AiChatModelConfig<C> chatModelConfig, ChatRequest chatRequest
    );

}
