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
package org.thingsboard.server.service.ai;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.gax.retrying.RetrySettings;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.vertexai.Transport;
import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.GenerationConfig;
import com.google.cloud.vertexai.api.PredictionServiceClient;
import com.google.cloud.vertexai.api.PredictionServiceSettings;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.azure.AzureOpenAiChatModel;
import dev.langchain4j.model.bedrock.BedrockChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.github.GitHubModelsChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.mistralai.MistralAiChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.vertexai.gemini.VertexAiGeminiChatModel;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
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
import org.thingsboard.server.common.data.ai.provider.GoogleVertexAiGeminiProviderConfig;
import org.thingsboard.server.common.data.ai.provider.OllamaProviderConfig;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

import static java.util.Collections.singletonMap;

@Component
class Langchain4jChatModelConfigurerImpl implements Langchain4jChatModelConfigurer {

    @Override
    public ChatModel configureChatModel(OpenAiChatModelConfig chatModelConfig) {
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
        return GoogleAiGeminiChatModel.builder()
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
        GoogleVertexAiGeminiProviderConfig providerConfig = chatModelConfig.providerConfig();

        // construct service account credentials using service account key JSON
        ServiceAccountCredentials serviceAccountCredentials;
        try {
            serviceAccountCredentials = ServiceAccountCredentials.fromStream(new ByteArrayInputStream(providerConfig.serviceAccountKey().getBytes()));
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse service account key JSON", e);
        }

        PredictionServiceSettings predictionServiceClientSettings;
        try {
            // create prediction service settings for REST transport with service account key credentials
            PredictionServiceSettings.Builder settingsBuilder = PredictionServiceSettings.newHttpJsonBuilder()
                    .setCredentialsProvider(FixedCredentialsProvider.create(serviceAccountCredentials));

            // get the retry settings that control request timeout for generateContent RPC
            RetrySettings.Builder retrySettings = settingsBuilder
                    .generateContentSettings()
                    .getRetrySettings()
                    .toBuilder();

            // set request timeout from model config
            if (chatModelConfig.timeoutSeconds() != null) {
                retrySettings.setTotalTimeoutDuration(Duration.ofSeconds(chatModelConfig.timeoutSeconds()));
            }

            // set updated retry settings
            settingsBuilder.generateContentSettings().setRetrySettings(retrySettings.build());

            // build the client settings
            predictionServiceClientSettings = settingsBuilder.build();
        } catch (IOException e) {
            throw new RuntimeException("Failed to create prediction service client settings", e);
        }

        // construct Vertex AI instance
        var vertexAI = new VertexAI.Builder()
                .setProjectId(providerConfig.projectId())
                .setLocation(providerConfig.location())
                .setPredictionClientSupplier(() -> createPredictionServiceClient(predictionServiceClientSettings))
                .setTransport(Transport.REST) // GRPC also possible, but likely does not work with service account keys
                .build();

        // map model config to generation config
        var generationConfigBuilder = GenerationConfig.newBuilder();
        if (chatModelConfig.temperature() != null) {
            generationConfigBuilder.setTemperature(chatModelConfig.temperature().floatValue());
        }
        if (chatModelConfig.topP() != null) {
            generationConfigBuilder.setTopP(chatModelConfig.topP().floatValue());
        }
        if (chatModelConfig.topK() != null) {
            generationConfigBuilder.setTopK(chatModelConfig.topK());
        }
        if (chatModelConfig.frequencyPenalty() != null) {
            generationConfigBuilder.setFrequencyPenalty(chatModelConfig.frequencyPenalty().floatValue());
        }
        if (chatModelConfig.frequencyPenalty() != null) {
            generationConfigBuilder.setPresencePenalty(chatModelConfig.frequencyPenalty().floatValue());
        }
        if (chatModelConfig.maxOutputTokens() != null) {
            generationConfigBuilder.setMaxOutputTokens(chatModelConfig.maxOutputTokens());
        }
        var generationConfig = generationConfigBuilder.build();

        // construct generative model instance
        var generativeModel = new GenerativeModel(chatModelConfig.modelId(), vertexAI).withGenerationConfig(generationConfig);

        return new VertexAiGeminiChatModel(generativeModel, generationConfig, chatModelConfig.maxRetries());
    }

    private static PredictionServiceClient createPredictionServiceClient(PredictionServiceSettings settings) {
        try {
            return PredictionServiceClient.create(settings);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create prediction service client", e);
        }
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
        return GitHubModelsChatModel.builder()
                .gitHubToken(chatModelConfig.providerConfig().personalAccessToken())
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
    public ChatModel configureChatModel(OllamaChatModelConfig chatModelConfig) {
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

    private static Duration toDuration(Integer timeoutSeconds) {
        return timeoutSeconds != null ? Duration.ofSeconds(timeoutSeconds) : null;
    }

}
