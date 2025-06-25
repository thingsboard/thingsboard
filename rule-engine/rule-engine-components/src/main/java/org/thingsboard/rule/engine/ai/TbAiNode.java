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
package org.thingsboard.rule.engine.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.FutureCallback;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.ResponseFormatType;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.external.TbAbstractExternalNode;
import org.thingsboard.server.common.data.ai.AiModelSettings;
import org.thingsboard.server.common.data.ai.model.AiModelType;
import org.thingsboard.server.common.data.ai.model.chat.AiChatModel;
import org.thingsboard.server.common.data.ai.model.chat.AiChatModelConfig;
import org.thingsboard.server.common.data.id.AiModelSettingsId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.dao.exception.DataValidationException;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static org.thingsboard.server.dao.service.ConstraintValidator.validateFields;

@RuleNode(
        type = ComponentType.EXTERNAL,
        name = "AI",
        nodeDescription = "Interact with AI",
        nodeDetails = "This node makes requests to AI based on a prompt and a input message and returns a response in a form of output message",
        configClazz = TbAiNodeConfiguration.class,
        ruleChainTypes = RuleChainType.CORE
)
public final class TbAiNode extends TbAbstractExternalNode implements TbNode {

    private String systemPrompt;
    private String userPrompt;
    private ResponseFormat responseFormat;
    private int timeoutSeconds;
    private AiModelSettingsId modelSettingsId;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        super.init(ctx);

        var config = TbNodeUtils.convert(configuration, TbAiNodeConfiguration.class);
        String errorPrefix = "'" + ctx.getSelf().getName() + "' node configuration is invalid: ";
        try {
            validateFields(config, errorPrefix);
        } catch (DataValidationException e) {
            throw new TbNodeException(e, true);
        }

        responseFormat = ResponseFormat.builder()
                .type(config.getResponseFormatType())
                .jsonSchema(getJsonSchema(config.getResponseFormatType(), config.getJsonSchema()))
                .build();

        systemPrompt = config.getSystemPrompt();
        userPrompt = config.getUserPrompt();
        timeoutSeconds = config.getTimeoutSeconds();
        modelSettingsId = config.getAiModelSettingsId();

        Optional<AiModelSettings> modelSettings = ctx.getAiModelSettingsService().findAiModelSettingsByTenantIdAndId(ctx.getTenantId(), modelSettingsId);
        if (modelSettings.isEmpty()) {
            throw new TbNodeException("[" + ctx.getTenantId() + "] AI model settings with ID: [" + modelSettingsId + "] were not found", true);
        }
        AiModelType modelType = modelSettings.get().getConfiguration().modelType();
        if (modelType != AiModelType.CHAT) {
            throw new TbNodeException("[" + ctx.getTenantId() + "] AI model settings with ID: [" + modelSettingsId + "] must be of type CHAT, but was " + modelType, true);
        }
    }

    private static JsonSchema getJsonSchema(ResponseFormatType responseFormatType, ObjectNode jsonSchema) {
        if (responseFormatType == ResponseFormatType.TEXT) {
            return null;
        }
        return responseFormatType == ResponseFormatType.JSON && jsonSchema != null ? Langchain4jJsonSchemaAdapter.fromJsonNode(jsonSchema) : null;
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        var ackedMsg = ackIfNeeded(ctx, msg);

        var systemMessage = SystemMessage.from(TbNodeUtils.processPattern(systemPrompt, ackedMsg));
        var userMessage = UserMessage.from(TbNodeUtils.processPattern(userPrompt, ackedMsg));

        var chatRequest = ChatRequest.builder()
                .messages(List.of(systemMessage, userMessage))
                .responseFormat(responseFormat)
                .build();

        configureChatModelAsync(ctx)
                .transformAsync(chatModel -> ctx.getAiRequestsExecutor().sendChatRequestAsync(chatModel, chatRequest), directExecutor())
                .addCallback(new FutureCallback<>() {
                    @Override
                    public void onSuccess(ChatResponse chatResponse) {
                        String response = chatResponse.aiMessage().text();
                        if (!isValidJsonObject(response)) {
                            response = wrapInJsonObject(response);
                        }
                        tellSuccess(ctx, ackedMsg.transform()
                                .data(response)
                                .build());
                    }

                    @Override
                    public void onFailure(@NonNull Throwable t) {
                        tellFailure(ctx, ackedMsg, t);
                    }
                }, directExecutor());
    }

    private <C extends AiChatModelConfig<C>> FluentFuture<ChatModel> configureChatModelAsync(TbContext ctx) {
        return ctx.getAiModelSettingsService().findAiModelSettingsByTenantIdAndIdAsync(ctx.getTenantId(), modelSettingsId).transform(settingsOpt -> {
            if (settingsOpt.isEmpty()) {
                throw new NoSuchElementException("[" + ctx.getTenantId() + "] AI model settings with ID: [" + modelSettingsId + "] were not found");
            }
            AiModelSettings settings = settingsOpt.get();
            AiModelType modelType = settings.getConfiguration().modelType();
            if (modelType != AiModelType.CHAT) {
                throw new IllegalStateException("[" + ctx.getTenantId() + "] AI model settings with ID: [" + modelSettingsId + "] must be of type CHAT, but was " + modelType);
            }

            @SuppressWarnings("unchecked")
            AiChatModel<C> chatModel = (AiChatModel<C>) settingsOpt.get().getConfiguration();

            chatModel = chatModel.withModelConfig(chatModel.modelConfig()
                    .withTimeoutSeconds(timeoutSeconds)
                    .withMaxRetries(0)); // disable retries to respect timeout set in rule node config

            return ctx.getAiModelService().configureChatModel(chatModel);
        }, ctx.getDbCallbackExecutor());
    }

    private static boolean isValidJsonObject(String jsonString) {
        try {
            JsonNode result = JacksonUtil.toJsonNode(jsonString);
            return result != null && result.isObject();
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static String wrapInJsonObject(String response) {
        return JacksonUtil.newObjectNode().put("response", response).toString();
    }

    @Override
    public void destroy() {
        super.destroy();
        systemPrompt = null;
        userPrompt = null;
        responseFormat = null;
        modelSettingsId = null;
    }

}
