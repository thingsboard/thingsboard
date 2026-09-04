// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.ai.provider;

public sealed interface AiProviderConfig
        permits
        OpenAiProviderConfig, AzureOpenAiProviderConfig, GoogleAiGeminiProviderConfig,
        GoogleVertexAiGeminiProviderConfig, MistralAiProviderConfig, AnthropicProviderConfig,
        AmazonBedrockProviderConfig, GitHubModelsProviderConfig, OllamaProviderConfig {}
