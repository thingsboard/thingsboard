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
package org.thingsboard.server.common.data.ai.model;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.jsontype.impl.TypeIdResolverBase;
import org.thingsboard.server.common.data.ai.model.chat.AmazonBedrockChatModel;
import org.thingsboard.server.common.data.ai.model.chat.AnthropicChatModel;
import org.thingsboard.server.common.data.ai.model.chat.GitHubModelsChatModel;
import org.thingsboard.server.common.data.ai.model.chat.GoogleAiGeminiChatModel;
import org.thingsboard.server.common.data.ai.model.chat.GoogleVertexAiGeminiChatModel;
import org.thingsboard.server.common.data.ai.model.chat.MistralAiChatModel;
import org.thingsboard.server.common.data.ai.model.chat.OpenAiChatModel;
import org.thingsboard.server.common.data.ai.provider.AiProvider;

import java.util.Map;

public final class AiModelTypeIdResolver extends TypeIdResolverBase {

    private static final Map<String, Class<?>> typeIdToModelClass = Map.ofEntries(
            // OpenAI models
            Map.entry("OPENAI::o4-mini", OpenAiChatModel.class),
            // Map.entry("OPENAI::o3-pro", OpenAiChatModel.class); // needs verification with Gov ID :)
            // Map.entry("OPENAI::o3", OpenAiChatModel.class);     // needs verification with Gov ID :)
            Map.entry("OPENAI::o3-mini", OpenAiChatModel.class),
            // Map.entry("OPENAI::o1-pro", OpenAiChatModel.class); // LC4j sends requests to v1/chat/completions, but o1-pro is only supported in v1/responses
            Map.entry("OPENAI::o1", OpenAiChatModel.class),
            Map.entry("OPENAI::gpt-4.1", OpenAiChatModel.class),
            Map.entry("OPENAI::gpt-4.1-mini", OpenAiChatModel.class),
            Map.entry("OPENAI::gpt-4.1-nano", OpenAiChatModel.class),
            Map.entry("OPENAI::gpt-4o", OpenAiChatModel.class),
            Map.entry("OPENAI::gpt-4o-mini", OpenAiChatModel.class),

            // Google AI Gemini models
            Map.entry("GOOGLE_AI_GEMINI::gemini-2.5-pro", GoogleAiGeminiChatModel.class),
            Map.entry("GOOGLE_AI_GEMINI::gemini-2.5-flash", GoogleAiGeminiChatModel.class),
            Map.entry("GOOGLE_AI_GEMINI::gemini-2.0-flash", GoogleAiGeminiChatModel.class),
            Map.entry("GOOGLE_AI_GEMINI::gemini-2.0-flash-lite", GoogleAiGeminiChatModel.class),
            Map.entry("GOOGLE_AI_GEMINI::gemini-1.5-pro", GoogleAiGeminiChatModel.class),
            Map.entry("GOOGLE_AI_GEMINI::gemini-1.5-flash", GoogleAiGeminiChatModel.class),
            Map.entry("GOOGLE_AI_GEMINI::gemini-1.5-flash-8b", GoogleAiGeminiChatModel.class),

            // Google Vertex AI Gemini models
            Map.entry("GOOGLE_VERTEX_AI_GEMINI::gemini-2.5-pro", GoogleVertexAiGeminiChatModel.class),
            Map.entry("GOOGLE_VERTEX_AI_GEMINI::gemini-2.5-flash", GoogleVertexAiGeminiChatModel.class),
            Map.entry("GOOGLE_VERTEX_AI_GEMINI::gemini-2.0-flash", GoogleVertexAiGeminiChatModel.class),
            Map.entry("GOOGLE_VERTEX_AI_GEMINI::gemini-2.0-flash-lite", GoogleVertexAiGeminiChatModel.class),
            Map.entry("GOOGLE_VERTEX_AI_GEMINI::gemini-1.5-pro", GoogleVertexAiGeminiChatModel.class),
            Map.entry("GOOGLE_VERTEX_AI_GEMINI::gemini-1.5-flash", GoogleVertexAiGeminiChatModel.class),
            Map.entry("GOOGLE_VERTEX_AI_GEMINI::gemini-1.5-flash-8b", GoogleVertexAiGeminiChatModel.class),

            // Mistral AI models
            Map.entry("MISTRAL_AI::magistral-medium-latest", MistralAiChatModel.class),
            Map.entry("MISTRAL_AI::magistral-small-latest", MistralAiChatModel.class),
            Map.entry("MISTRAL_AI::mistral-large-latest", MistralAiChatModel.class),
            Map.entry("MISTRAL_AI::mistral-medium-latest", MistralAiChatModel.class),
            Map.entry("MISTRAL_AI::mistral-small-latest", MistralAiChatModel.class),
            Map.entry("MISTRAL_AI::pixtral-large-latest", MistralAiChatModel.class),
            Map.entry("MISTRAL_AI::ministral-8b-latest", MistralAiChatModel.class),
            Map.entry("MISTRAL_AI::ministral-3b-latest", MistralAiChatModel.class),
            Map.entry("MISTRAL_AI::open-mistral-nemo", MistralAiChatModel.class),

            // Anthropic models
            Map.entry("ANTHROPIC::claude-opus-4-0", AnthropicChatModel.class),
            Map.entry("ANTHROPIC::claude-sonnet-4-0", AnthropicChatModel.class),
            Map.entry("ANTHROPIC::claude-3-7-sonnet-latest", AnthropicChatModel.class),
            Map.entry("ANTHROPIC::claude-3-5-sonnet-latest", AnthropicChatModel.class),
            Map.entry("ANTHROPIC::claude-3-5-haiku-latest", AnthropicChatModel.class),
            Map.entry("ANTHROPIC::claude-3-opus-latest", AnthropicChatModel.class),

            // Amazon Bedrock models
            Map.entry("AMAZON_BEDROCK::amazon.nova-lite-v1:0", AmazonBedrockChatModel.class),

            // GitHub Models models
            Map.entry("GITHUB_MODELS::gpt-4o", GitHubModelsChatModel.class)
    );

    private static final String PROVIDER_MODEL_SEPARATOR = "::";

    private JavaType baseType;

    @Override
    public void init(JavaType baseType) {
        this.baseType = baseType;
    }

    @Override
    public String idFromValue(Object value) {
        return generateId((AiModel<?>) value);
    }

    @Override
    public String idFromValueAndType(Object value, Class<?> suggestedType) {
        return generateId((AiModel<?>) value);
    }

    @Override
    public JavaType typeFromId(DatabindContext context, String id) {
        Class<?> modelClass = typeIdToModelClass.get(id);
        if (modelClass != null) { // known model
            return context.constructSpecializedType(baseType, modelClass);
        }

        String[] parts = id.split(PROVIDER_MODEL_SEPARATOR, 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid model type ID format: " + id + ". Expected format: PROVIDER::MODEL_ID");
        }

        String providerName = parts[0];

        // Check if the provider exists
        AiProvider provider;
        try {
            provider = AiProvider.valueOf(providerName);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown AI provider: " + providerName);
        }

        // Provider is valid but model is unknown - fallback to default model class
        modelClass = provider.getDefaultModelClass();

        return context.constructSpecializedType(baseType, modelClass);
    }

    @Override
    public JsonTypeInfo.Id getMechanism() {
        return JsonTypeInfo.Id.CUSTOM;
    }

    private static String generateId(AiModel<?> model) {
        String provider = model.providerConfig().provider().name();
        String modelId = model.modelConfig().modelId();
        return provider + PROVIDER_MODEL_SEPARATOR + modelId;
    }

}
