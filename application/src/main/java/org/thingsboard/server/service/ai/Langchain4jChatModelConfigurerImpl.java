/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.service.ai;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.azure.AzureOpenAiChatModel;
import dev.langchain4j.model.bedrock.BedrockChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.google.genai.GoogleGenAiChatModel;
import dev.langchain4j.model.mistralai.MistralAiChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openaiofficial.OpenAiOfficialChatModel;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.SsrfProtectionValidator;
import org.thingsboard.server.common.data.ai.model.chat.AmazonBedrockChatModelConfig;
import org.thingsboard.server.common.data.ai.model.chat.AnthropicChatModelConfig;
import org.thingsboard.server.common.data.ai.model.chat.AzureOpenAiChatModelConfig;
import org.thingsboard.server.common.data.ai.model.chat.GitHubModelsChatModelConfig;
import org.thingsboard.server.common.data.ai.model.chat.GoogleAiGeminiChatModelConfig;
import org.thingsboard.server.common.data.ai.model.chat.GoogleVertexAiGeminiChatModelConfig;
import org.thingsboard.server.common.data.ai.model.chat.Langchain4jChatModelConfigurer;
import org.thingsboard.server.common.data.ai.model.chat.MistralAiChatModelConfig;
import org.thingsboard.server.common.data.ai.model.chat.OllamaChatModelConfig;
import org.thingsboard.server.common.data.ai.model.chat.OpenAiChatModelConfig;
import org.thingsboard.server.common.data.ai.provider.AmazonBedrockProviderConfig;
import org.thingsboard.server.common.data.ai.provider.AzureOpenAiProviderConfig;
import org.thingsboard.server.common.data.ai.provider.OllamaProviderConfig;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

import static java.util.Collections.singletonMap;

@Component
class Langchain4jChatModelConfigurerImpl implements Langchain4jChatModelConfigurer {

    @Override
    public ChatModel configureChatModel(OpenAiChatModelConfig chatModelConfig) {
        validateBaseUrl(chatModelConfig.providerConfig().baseUrl());
        return OpenAiChatModel.builder()
                .baseUrl(chatModelConfig.providerConfig().baseUrl())
                .apiKey(chatModelConfig.providerConfig().apiKey())
                .modelName(chatModelConfig.modelId())
                .temperature(chatModelConfig.temperature())
                .topP(chatModelConfig.topP())
                .frequencyPenalty(chatModelConfig.frequencyPenalty())
                .presencePenalty(chatModelConfig.presencePenalty())
                .maxTokens(chatModelConfig.maxOutputTokens())
                .timeout(toDuration(chatModelConfig.timeoutSeconds()))
                .maxRetries(chatModelConfig.maxRetries())
                .build();
    }

    @Override
    public ChatModel configureChatModel(AzureOpenAiChatModelConfig chatModelConfig) {
        AzureOpenAiProviderConfig providerConfig = chatModelConfig.providerConfig();
        validateBaseUrl(providerConfig.endpoint());
        return AzureOpenAiChatModel.builder()
                .endpoint(providerConfig.endpoint())
                .serviceVersion(providerConfig.serviceVersion())
                .apiKey(providerConfig.apiKey())
                .deploymentName(chatModelConfig.modelId())
                .temperature(chatModelConfig.temperature())
                .topP(chatModelConfig.topP())
                .frequencyPenalty(chatModelConfig.frequencyPenalty())
                .presencePenalty(chatModelConfig.presencePenalty())
                .maxTokens(chatModelConfig.maxOutputTokens())
                .timeout(toDuration(chatModelConfig.timeoutSeconds()))
                .maxRetries(chatModelConfig.maxRetries())
                .build();
    }

    @Override
    public ChatModel configureChatModel(GoogleAiGeminiChatModelConfig chatModelConfig) {
        return GoogleGenAiChatModel.builder()
                .apiKey(chatModelConfig.providerConfig().apiKey())
                .modelName(chatModelConfig.modelId())
                .temperature(chatModelConfig.temperature())
                .topP(chatModelConfig.topP())
                .topK(chatModelConfig.topK())
                .frequencyPenalty(chatModelConfig.frequencyPenalty())
                .presencePenalty(chatModelConfig.presencePenalty())
                .maxOutputTokens(chatModelConfig.maxOutputTokens())
                .timeout(toDuration(chatModelConfig.timeoutSeconds()))
                .maxRetries(chatModelConfig.maxRetries())
                .build();
    }

    @Override
    public ChatModel configureChatModel(GoogleVertexAiGeminiChatModelConfig chatModelConfig) {
        GoogleCredentials credentials;
        try {
            credentials = ServiceAccountCredentials
                    .fromStream(new ByteArrayInputStream(chatModelConfig.providerConfig().serviceAccountKey().getBytes(StandardCharsets.UTF_8)))
                    .createScoped("https://www.googleapis.com/auth/cloud-platform");
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse service account key JSON", e);
        }
        return GoogleGenAiChatModel.builder()
                .projectId(chatModelConfig.providerConfig().projectId())
                .location(chatModelConfig.providerConfig().location())
                .googleCredentials(credentials)
                .modelName(chatModelConfig.modelId())
                .temperature(chatModelConfig.temperature())
                .topP(chatModelConfig.topP())
                .topK(chatModelConfig.topK())
                .frequencyPenalty(chatModelConfig.frequencyPenalty())
                .presencePenalty(chatModelConfig.presencePenalty())
                .maxOutputTokens(chatModelConfig.maxOutputTokens())
                .timeout(toDuration(chatModelConfig.timeoutSeconds()))
                .maxRetries(chatModelConfig.maxRetries())
                .build();
    }

    @Override
    public ChatModel configureChatModel(MistralAiChatModelConfig chatModelConfig) {
        return MistralAiChatModel.builder()
                .apiKey(chatModelConfig.providerConfig().apiKey())
                .modelName(chatModelConfig.modelId())
                .temperature(chatModelConfig.temperature())
                .topP(chatModelConfig.topP())
                .frequencyPenalty(chatModelConfig.frequencyPenalty())
                .presencePenalty(chatModelConfig.presencePenalty())
                .maxTokens(chatModelConfig.maxOutputTokens())
                .timeout(toDuration(chatModelConfig.timeoutSeconds()))
                .maxRetries(chatModelConfig.maxRetries())
                .build();
    }

    @Override
    public ChatModel configureChatModel(AnthropicChatModelConfig chatModelConfig) {
        return AnthropicChatModel.builder()
                .apiKey(chatModelConfig.providerConfig().apiKey())
                .modelName(chatModelConfig.modelId())
                .temperature(chatModelConfig.temperature())
                .topP(chatModelConfig.topP())
                .topK(chatModelConfig.topK())
                .maxTokens(chatModelConfig.maxOutputTokens())
                .timeout(toDuration(chatModelConfig.timeoutSeconds()))
                .maxRetries(chatModelConfig.maxRetries())
                .build();
    }

    @Override
    public ChatModel configureChatModel(AmazonBedrockChatModelConfig chatModelConfig) {
        AmazonBedrockProviderConfig providerConfig = chatModelConfig.providerConfig();

        var credentialsProvider = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(providerConfig.accessKeyId(), providerConfig.secretAccessKey())
        );

        var bedrockClient = BedrockRuntimeClient.builder()
                .region(Region.of(providerConfig.region()))
                .credentialsProvider(credentialsProvider)
                .build();

        var defaultChatRequestParams = ChatRequestParameters.builder()
                .temperature(chatModelConfig.temperature())
                .topP(chatModelConfig.topP())
                .maxOutputTokens(chatModelConfig.maxOutputTokens())
                .build();

        return BedrockChatModel.builder()
                .client(bedrockClient)
                .modelId(chatModelConfig.modelId())
                .defaultRequestParameters(defaultChatRequestParams)
                .timeout(toDuration(chatModelConfig.timeoutSeconds()))
                .maxRetries(chatModelConfig.maxRetries())
                .build();
    }

    @Override
    public ChatModel configureChatModel(GitHubModelsChatModelConfig chatModelConfig) {
        return OpenAiOfficialChatModel.builder()
                .isGitHubModels(true)
                .strictJsonSchema(true)
                .apiKey(chatModelConfig.providerConfig().personalAccessToken())
                .modelName(chatModelConfig.modelId())
                .temperature(chatModelConfig.temperature())
                .topP(chatModelConfig.topP())
                .frequencyPenalty(chatModelConfig.frequencyPenalty())
                .presencePenalty(chatModelConfig.presencePenalty())
                .maxCompletionTokens(chatModelConfig.maxOutputTokens())
                .timeout(toDuration(chatModelConfig.timeoutSeconds()))
                .maxRetries(chatModelConfig.maxRetries())
                .build();
    }

    @Override
    public ChatModel configureChatModel(OllamaChatModelConfig chatModelConfig) {
        validateBaseUrl(chatModelConfig.providerConfig().baseUrl());
        var builder = OllamaChatModel.builder()
                .baseUrl(chatModelConfig.providerConfig().baseUrl())
                .modelName(chatModelConfig.modelId())
                .temperature(chatModelConfig.temperature())
                .topP(chatModelConfig.topP())
                .topK(chatModelConfig.topK())
                .numCtx(chatModelConfig.contextLength())
                .numPredict(chatModelConfig.maxOutputTokens())
                .timeout(toDuration(chatModelConfig.timeoutSeconds()))
                .maxRetries(chatModelConfig.maxRetries());

        var auth = chatModelConfig.providerConfig().auth();
        if (auth instanceof OllamaProviderConfig.OllamaAuth.Basic basicAuth) {
            String credentials = basicAuth.username() + ":" + basicAuth.password();
            String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
            builder.customHeaders(singletonMap(HttpHeaders.AUTHORIZATION, "Basic " + encodedCredentials));
        } else if (auth instanceof OllamaProviderConfig.OllamaAuth.Token tokenAuth) {
            builder.customHeaders(singletonMap(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAuth.token()));
        } else if (auth instanceof OllamaProviderConfig.OllamaAuth.None) {
            // do nothing
        } else {
            throw new UnsupportedOperationException("Unknown authentication type: " + auth.getClass().getSimpleName());
        }

        return builder.build();
    }

    private static void validateBaseUrl(String url) {
        SsrfProtectionValidator.validateUri(URI.create(url));
    }

    private static Duration toDuration(Integer timeoutSeconds) {
        return timeoutSeconds != null ? Duration.ofSeconds(timeoutSeconds) : null;
    }

}
