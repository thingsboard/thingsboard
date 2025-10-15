/**
 * Copyright © 2016-2025 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
