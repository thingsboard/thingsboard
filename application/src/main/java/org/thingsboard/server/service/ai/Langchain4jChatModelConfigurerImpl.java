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
import dev.langchain4j.model.bedrock.BedrockChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.vertexai.gemini.VertexAiGeminiChatModel;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.ai.model.chat.AmazonBedrockChatModel;
import org.thingsboard.server.common.data.ai.model.chat.AnthropicChatModel;
import org.thingsboard.server.common.data.ai.model.chat.AzureOpenAiChatModel;
import org.thingsboard.server.common.data.ai.model.chat.GitHubModelsChatModel;
import org.thingsboard.server.common.data.ai.model.chat.GoogleAiGeminiChatModel;
import org.thingsboard.server.common.data.ai.model.chat.GoogleVertexAiGeminiChatModel;
import org.thingsboard.server.common.data.ai.model.chat.Langchain4jChatModelConfigurer;
import org.thingsboard.server.common.data.ai.model.chat.MistralAiChatModel;
import org.thingsboard.server.common.data.ai.model.chat.OpenAiChatModel;
import org.thingsboard.server.common.data.ai.provider.AmazonBedrockProviderConfig;
import org.thingsboard.server.common.data.ai.provider.GoogleVertexAiGeminiProviderConfig;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Duration;

@Component
class Langchain4jChatModelConfigurerImpl implements Langchain4jChatModelConfigurer {

    @Override
    public ChatModel configureChatModel(OpenAiChatModel chatModel) {
        OpenAiChatModel.Config modelConfig = chatModel.modelConfig();
        return dev.langchain4j.model.openai.OpenAiChatModel.builder()
                .apiKey(chatModel.providerConfig().apiKey())
                .modelName(modelConfig.modelId())
                .temperature(modelConfig.temperature())
                .topP(modelConfig.topP())
                .frequencyPenalty(modelConfig.frequencyPenalty())
                .timeout(toDuration(modelConfig.timeoutSeconds()))
                .maxRetries(modelConfig.maxRetries())
                .build();
    }

    @Override
    public ChatModel configureChatModel(AzureOpenAiChatModel chatModel) {
        AzureOpenAiChatModel.Config modelConfig = chatModel.modelConfig();
        return dev.langchain4j.model.azure.AzureOpenAiChatModel.builder()
                .apiKey(chatModel.providerConfig().apiKey())
                .deploymentName(modelConfig.modelId())
                .temperature(modelConfig.temperature())
                .topP(modelConfig.topP())
                .frequencyPenalty(modelConfig.frequencyPenalty())
                .timeout(toDuration(modelConfig.timeoutSeconds()))
                .maxRetries(modelConfig.maxRetries())
                .build();
    }

    @Override
    public ChatModel configureChatModel(GoogleAiGeminiChatModel chatModel) {
        GoogleAiGeminiChatModel.Config modelConfig = chatModel.modelConfig();
        return dev.langchain4j.model.googleai.GoogleAiGeminiChatModel.builder()
                .apiKey(chatModel.providerConfig().apiKey())
                .modelName(modelConfig.modelId())
                .temperature(modelConfig.temperature())
                .topP(modelConfig.topP())
                .topK(modelConfig.topK())
                .frequencyPenalty(modelConfig.frequencyPenalty())
                .timeout(toDuration(modelConfig.timeoutSeconds()))
                .maxRetries(modelConfig.maxRetries())
                .build();
    }

    @Override
    public ChatModel configureChatModel(GoogleVertexAiGeminiChatModel chatModel) {
        GoogleVertexAiGeminiProviderConfig providerConfig = chatModel.providerConfig();
        GoogleVertexAiGeminiChatModel.Config modelConfig = chatModel.modelConfig();

        // construct service account credentials using service account key JSON
        ServiceAccountCredentials serviceAccountCredentials;
        try {
            serviceAccountCredentials = ServiceAccountCredentials.fromStream(
                    new ByteArrayInputStream(JacksonUtil.writeValueAsBytes(providerConfig.serviceAccountKey()))
            );
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
            if (modelConfig.timeoutSeconds() != null) {
                retrySettings.setTotalTimeout(org.threeten.bp.Duration.ofSeconds(modelConfig.timeoutSeconds()));
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
        if (modelConfig.temperature() != null) {
            generationConfigBuilder.setTemperature(modelConfig.temperature().floatValue());
        }
        if (modelConfig.topP() != null) {
            generationConfigBuilder.setTopP(modelConfig.topP().floatValue());
        }
        if (modelConfig.topK() != null) {
            generationConfigBuilder.setTopK(modelConfig.topK());
        }
        if (modelConfig.frequencyPenalty() != null) {
            generationConfigBuilder.setFrequencyPenalty(modelConfig.frequencyPenalty().floatValue());
        }
        var generationConfig = generationConfigBuilder.build();

        // construct generative model instance
        var generativeModel = new GenerativeModel(modelConfig.modelId(), vertexAI).withGenerationConfig(generationConfig);

        return new VertexAiGeminiChatModel(generativeModel, generationConfig, modelConfig.maxRetries());
    }

    private static PredictionServiceClient createPredictionServiceClient(PredictionServiceSettings settings) {
        try {
            return PredictionServiceClient.create(settings);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create prediction service client", e);
        }
    }

    @Override
    public ChatModel configureChatModel(MistralAiChatModel chatModel) {
        MistralAiChatModel.Config modelConfig = chatModel.modelConfig();
        return dev.langchain4j.model.mistralai.MistralAiChatModel.builder()
                .apiKey(chatModel.providerConfig().apiKey())
                .modelName(modelConfig.modelId())
                .temperature(modelConfig.temperature())
                .topP(modelConfig.topP())
                .frequencyPenalty(modelConfig.frequencyPenalty())
                .timeout(toDuration(modelConfig.timeoutSeconds()))
                .maxRetries(modelConfig.maxRetries())
                .build();
    }

    @Override
    public ChatModel configureChatModel(AnthropicChatModel chatModel) {
        AnthropicChatModel.Config modelConfig = chatModel.modelConfig();
        return dev.langchain4j.model.anthropic.AnthropicChatModel.builder()
                .apiKey(chatModel.providerConfig().apiKey())
                .modelName(modelConfig.modelId())
                .temperature(modelConfig.temperature())
                .topP(modelConfig.topP())
                .topK(modelConfig.topK())
                .timeout(toDuration(modelConfig.timeoutSeconds()))
                .maxRetries(modelConfig.maxRetries())
                .build();
    }

    @Override
    public ChatModel configureChatModel(AmazonBedrockChatModel chatModel) {
        AmazonBedrockProviderConfig providerConfig = chatModel.providerConfig();
        AmazonBedrockChatModel.Config modelConfig = chatModel.modelConfig();

        var credentialsProvider = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(providerConfig.accessKeyId(), providerConfig.secretAccessKey())
        );

        var bedrockClient = BedrockRuntimeClient.builder()
                .region(Region.of(providerConfig.region()))
                .credentialsProvider(credentialsProvider)
                .build();

        var defaultChatRequestParams = ChatRequestParameters.builder()
                .temperature(modelConfig.temperature())
                .topP(modelConfig.topP())
                .build();

        return BedrockChatModel.builder()
                .client(bedrockClient)
                .modelId(modelConfig.modelId())
                .defaultRequestParameters(defaultChatRequestParams)
                .timeout(toDuration(modelConfig.timeoutSeconds()))
                .maxRetries(modelConfig.maxRetries())
                .build();
    }

    @Override
    public ChatModel configureChatModel(GitHubModelsChatModel chatModel) {
        GitHubModelsChatModel.Config modelConfig = chatModel.modelConfig();
        return dev.langchain4j.model.github.GitHubModelsChatModel.builder()
                .gitHubToken(chatModel.providerConfig().personalAccessToken())
                .modelName(modelConfig.modelId())
                .temperature(modelConfig.temperature())
                .topP(modelConfig.topP())
                .frequencyPenalty(modelConfig.frequencyPenalty())
                .timeout(toDuration(modelConfig.timeoutSeconds()))
                .maxRetries(modelConfig.maxRetries())
                .build();
    }

    private static Duration toDuration(Integer timeoutSeconds) {
        return timeoutSeconds != null ? Duration.ofSeconds(timeoutSeconds) : null;
    }

}
