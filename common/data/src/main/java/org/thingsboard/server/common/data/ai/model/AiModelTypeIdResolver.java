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
import org.thingsboard.server.common.data.ai.model.chat.GoogleAiGeminiChatModel;
import org.thingsboard.server.common.data.ai.model.chat.GoogleVertexAiGeminiChatModel;
import org.thingsboard.server.common.data.ai.model.chat.MistralAiChatModel;
import org.thingsboard.server.common.data.ai.model.chat.OpenAiChatModel;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class AiModelTypeIdResolver extends TypeIdResolverBase {

    private static final Map<String, Class<?>> typeIdToModelClass;

    static {
        Map<String, Class<?>> map = new HashMap<>();

        // OpenAI models
        map.put("OPENAI::o4-mini", OpenAiChatModel.class);
        // map.put("OPENAI::o3-pro", OpenAiChatModel.class); // needs verification with Gov ID :)
        // map.put("OPENAI::o3", OpenAiChatModel.class);     // needs verification with Gov ID :)
        map.put("OPENAI::o3-mini", OpenAiChatModel.class);
        // map.put("OPENAI::o1-pro", OpenAiChatModel.class); // LC4j sends requests to v1/chat/completions, but o1-pro is only supported in v1/responses
        map.put("OPENAI::o1", OpenAiChatModel.class);
        map.put("OPENAI::gpt-4.1", OpenAiChatModel.class);
        map.put("OPENAI::gpt-4.1-mini", OpenAiChatModel.class);
        map.put("OPENAI::gpt-4.1-nano", OpenAiChatModel.class);
        map.put("OPENAI::gpt-4o", OpenAiChatModel.class);
        map.put("OPENAI::gpt-4o-mini", OpenAiChatModel.class);

        // Google AI Gemini models
        map.put("GOOGLE_AI_GEMINI::gemini-2.5-pro", GoogleAiGeminiChatModel.class);
        map.put("GOOGLE_AI_GEMINI::gemini-2.5-flash", GoogleAiGeminiChatModel.class);
        map.put("GOOGLE_AI_GEMINI::gemini-2.0-flash", GoogleAiGeminiChatModel.class);
        map.put("GOOGLE_AI_GEMINI::gemini-2.0-flash-lite", GoogleAiGeminiChatModel.class);
        map.put("GOOGLE_AI_GEMINI::gemini-1.5-pro", GoogleAiGeminiChatModel.class);
        map.put("GOOGLE_AI_GEMINI::gemini-1.5-flash", GoogleAiGeminiChatModel.class);
        map.put("GOOGLE_AI_GEMINI::gemini-1.5-flash-8b", GoogleAiGeminiChatModel.class);

        // Google Vertex AI Gemini models
        map.put("GOOGLE_VERTEX_AI_GEMINI::gemini-2.5-pro", GoogleVertexAiGeminiChatModel.class);
        map.put("GOOGLE_VERTEX_AI_GEMINI::gemini-2.5-flash", GoogleVertexAiGeminiChatModel.class);
        map.put("GOOGLE_VERTEX_AI_GEMINI::gemini-2.0-flash", GoogleVertexAiGeminiChatModel.class);
        map.put("GOOGLE_VERTEX_AI_GEMINI::gemini-2.0-flash-lite", GoogleVertexAiGeminiChatModel.class);
        map.put("GOOGLE_VERTEX_AI_GEMINI::gemini-1.5-pro", GoogleVertexAiGeminiChatModel.class);
        map.put("GOOGLE_VERTEX_AI_GEMINI::gemini-1.5-flash", GoogleVertexAiGeminiChatModel.class);
        map.put("GOOGLE_VERTEX_AI_GEMINI::gemini-1.5-flash-8b", GoogleVertexAiGeminiChatModel.class);

        // Mistral AI models
        map.put("MISTRAL_AI::magistral-medium-latest", MistralAiChatModel.class);
        map.put("MISTRAL_AI::magistral-small-latest", MistralAiChatModel.class);
        map.put("MISTRAL_AI::mistral-large-latest", MistralAiChatModel.class);
        map.put("MISTRAL_AI::mistral-medium-latest", MistralAiChatModel.class);
        map.put("MISTRAL_AI::mistral-small-latest", MistralAiChatModel.class);
        map.put("MISTRAL_AI::pixtral-large-latest", MistralAiChatModel.class);
        map.put("MISTRAL_AI::ministral-8b-latest", MistralAiChatModel.class);
        map.put("MISTRAL_AI::ministral-3b-latest", MistralAiChatModel.class);
        map.put("MISTRAL_AI::open-mistral-nemo", MistralAiChatModel.class);

        typeIdToModelClass = Collections.unmodifiableMap(map);
    }

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
        if (modelClass == null) {
            throw new IllegalArgumentException("Unknown model type ID: " + id);
        }
        return context.constructSpecializedType(baseType, modelClass);
    }

    @Override
    public JsonTypeInfo.Id getMechanism() {
        return JsonTypeInfo.Id.CUSTOM;
    }

    private static String generateId(AiModel<?> model) {
        String provider = model.providerConfig().provider().name();
        String modelId = model.modelConfig().modelId();
        return provider + "::" + modelId;
    }

}
