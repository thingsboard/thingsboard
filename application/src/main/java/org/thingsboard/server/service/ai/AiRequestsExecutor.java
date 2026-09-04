// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.ai;

import com.google.common.util.concurrent.FluentFuture;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;

public interface AiRequestsExecutor {

    FluentFuture<ChatResponse> sendChatRequestAsync(ChatModel chatModel, ChatRequest chatRequest);

}
