// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.ai.model.chat;

import dev.langchain4j.model.chat.ChatModel;

public interface Langchain4jChatModelConfigurer {

    ChatModel configureChatModel(OpenAiChatModelConfig chatModelConfig);

    ChatModel configureChatModel(AzureOpenAiChatModelConfig chatModelConfig);

    ChatModel configureChatModel(GoogleAiGeminiChatModelConfig chatModelConfig);

    ChatModel configureChatModel(GoogleVertexAiGeminiChatModelConfig chatModelConfig);

    ChatModel configureChatModel(MistralAiChatModelConfig chatModelConfig);

    ChatModel configureChatModel(AnthropicChatModelConfig chatModelConfig);

    ChatModel configureChatModel(AmazonBedrockChatModelConfig chatModelConfig);

    ChatModel configureChatModel(GitHubModelsChatModelConfig chatModelConfig);

    ChatModel configureChatModel(OllamaChatModelConfig chatModelConfig);

}
