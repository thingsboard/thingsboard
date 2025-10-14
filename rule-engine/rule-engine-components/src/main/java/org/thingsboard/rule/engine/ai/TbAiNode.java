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
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.PdfFileContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.external.TbAbstractExternalNode;
import org.thingsboard.server.common.data.GeneralFileDescriptor;
import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.data.TbResourceDataInfo;
import org.thingsboard.server.common.data.TbResourceInfo;
import org.thingsboard.server.common.data.ai.AiModel;
import org.thingsboard.server.common.data.ai.model.AiModelType;
import org.thingsboard.server.common.data.ai.model.chat.AiChatModelConfig;
import org.thingsboard.server.common.data.id.AiModelId;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.resource.TbResourceDataCache;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static org.thingsboard.rule.engine.ai.TbResponseFormat.TbResponseFormatType;
import static org.thingsboard.server.dao.service.ConstraintValidator.validateFields;

@Slf4j
@RuleNode(
        type = ComponentType.EXTERNAL,
        name = "AI request",
        nodeDescription = "Sends a request to an AI model using system and user prompts. Supports JSON mode.",
        nodeDetails = """
                Interact with large language models (LLMs) by sending dynamic requests from your rule chain.
                You can select a specific <strong>AI model</strong> and define its behavior using a <strong>system prompt</strong> (optional context or role) and a <strong>user prompt</strong> (the main task).
                Both prompts can be populated with data and metadata from the incoming message using patterns.
                For example, the <code>$[*]</code> and <code>${*}</code> patterns allow you to access the all message body and all metadata, respectively.
                <br><br>
                After sending the request, the node waits for a response within a configured <strong>timeout</strong>.
                You can specify the desired <strong>response format</strong> as <strong>Text</strong>, <strong>JSON</strong>, or provide a specific <strong>JSON Schema</strong> to structure the output.
                The AI-generated content is forwarded as the body of the outgoing message; the originator, message type, and metadata from the incoming message remain unchanged.
                <br><br>
                Output connections: <code>Success</code>, <code>Failure</code>.
                """,
        configClazz = TbAiNodeConfiguration.class,
        configDirective = "tbExternalNodeAiConfig",
        iconUrl = "data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDkiIGhlaWdodD0iNDgiIHZpZXdCb3g9IjAgMCA0OSA0OCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPHBhdGggZmlsbC1ydWxlPSJldmVub2RkIiBjbGlwLXJ1bGU9ImV2ZW5vZGQiIGQ9Ik0zOC42MzExIDE3LjA3OTVDNDAuMTcwNSAxNy4wNzk2IDQxLjY1MTggMTcuNjg3MiA0Mi43NDc4IDE4Ljc3NjNDNDMuODQ0OCAxOS44NjYzIDQ0LjQ2NTkgMjEuMzUwMSA0NC40NjU5IDIyLjkwMjlWMzUuNDY1MkM0NC40NjU5IDM2LjM1MDkgNDQuMzU2NyAzNy4wNzY5IDQ0LjA5NzMgMzcuNzUxN0M0My44NDE0IDM4LjQxNjcgNDMuNDY1MSAzOC45NjE0IDQzLjA0NDggMzkuNTAyOEM0Mi40NjY3IDQwLjI0NzIgNDEuNjU2MyA0MC42ODU5IDQwLjg5MTkgNDAuOTM4OEM0MC4xMjExIDQxLjE5MzcgMzkuMzE0MyA0MS4yODg1IDM4LjYzMTEgNDEuMjg4NUgzMS4wMjU5TDIzLjM4MTIgNDUuODQ2NEMyMy4wNDMxIDQ2LjA0NzggMjIuNjI0MSA0Ni4wNTA3IDIyLjI4MzkgNDUuODUyOUMyMS45NDM3IDQ1LjY1NDcgMjEuNzMzOCA0NS4yODU5IDIxLjczMzcgNDQuODg3MlY0MS4yODg1SDE5LjY2NjNDMTguMTI2OSA0MS4yODg0IDE2LjY0NTUgNDAuNjgwOSAxNS41NDk2IDM5LjU5MThDMTQuNDUyNyAzOC41MDE5IDEzLjgzMTUgMzcuMDE3OSAxMy44MzE1IDM1LjQ2NTJWMjIuOTAyOUMxMy44MzE1IDIyLjMyMDIgMTMuOTE4NSAyMS43NDY4IDE0LjA4NTggMjEuMjAwN0wxNi4yODg5IDIxLjgxMDFMMTcuMjA5OSAyNS4yNTAyQzE3Ljk0MTYgMjcuOTg0NSAyMS43NTYyIDI3Ljk4NDQgMjIuNDg4IDI1LjI1MDJMMjMuNDA3OSAyMS44MTAxTDI2Ljc5MTcgMjAuODc0OUMyOC41NzkxIDIwLjM4MDUgMjkuMTc3IDE4LjUwMjYgMjguNTg4OCAxNy4wNzk1SDM4LjYzMTFaTTIyLjU4NDIgMzEuNTM5NUMyMS45OCAzMS41Mzk3IDIxLjQ5MDEgMzIuMDM3NiAyMS40OTAxIDMyLjY1MTlDMjEuNDkwMiAzMy4yNjYgMjEuOTgwMSAzMy43NjQgMjIuNTg0MiAzMy43NjQySDM0LjYxOTFDMzUuMjIzMyAzMy43NjQyIDM1LjcxMzEgMzMuMjY2MSAzNS43MTMyIDMyLjY1MTlDMzUuNzEzMiAzMi4wMzc1IDM1LjIyMzQgMzEuNTM5NSAzNC42MTkxIDMxLjUzOTVIMjIuNTg0MlpNMjQuNzcyMyAyNC44NjU3QzI0LjE2ODIgMjQuODY1OCAyMy42NzgzIDI1LjM2MzggMjMuNjc4MyAyNS45NzhDMjMuNjc4NCAyNi41OTIyIDI0LjE2ODMgMjcuMDkwMiAyNC43NzIzIDI3LjA5MDNIMzcuOTAxNEMzOC41MDU1IDI3LjA5MDMgMzguOTk1MyAyNi41OTIyIDM4Ljk5NTQgMjUuOTc4QzM4Ljk5NTQgMjUuMzYzNyAzOC41MDU2IDI0Ljg2NTcgMzcuOTAxNCAyNC44NjU3SDI0Ljc3MjNaIiBmaWxsPSJibGFjayIgZmlsbC1vcGFjaXR5PSIwLjc2Ii8+CjxwYXRoIGQ9Ik0xOC43ODkxIDExLjI5NzVDMTkuMDY5MSAxMC4xODA4IDIwLjYyOTkgMTAuMTgwOCAyMC45MDk5IDExLjI5NzVMMjEuOTE0MyAxNS4zMDM2QzIyLjAxMTYgMTUuNjkxOCAyMi4zMDY1IDE1Ljk5NzggMjIuNjg2NyAxNi4xMDNMMjYuMzYxMSAxNy4xMTg3QzI3LjQzNyAxNy40MTYyIDI3LjQzNyAxOC45Njc2IDI2LjM2MTEgMTkuMjY1MUwyMi42NzYxIDIwLjI4NEMyMi4zMDE4IDIwLjM4NzQgMjIuMDA4NyAyMC42ODQ1IDIxLjkwNjggMjEuMDY1TDIwLjkwNDYgMjQuODEyNUMyMC42MTE3IDI1LjkwNTggMTkuMDg2MSAyNS45MDU5IDE4Ljc5MzMgMjQuODEyNUwxNy43OTExIDIxLjA2NUMxNy42ODkzIDIwLjY4NDcgMTcuMzk3IDIwLjM4NzUgMTcuMDIyOSAyMC4yODRMMTMuMzM2OCAxOS4yNjUxQzEyLjI2MTQgMTguOTY3MyAxMi4yNjE1IDE3LjQxNjUgMTMuMzM2OCAxNy4xMTg3TDE3LjAxMTIgMTYuMTAzQzE3LjM5MTYgMTUuOTk3OCAxNy42ODc0IDE1LjY5MTkgMTcuNzg0NyAxNS4zMDM2TDE4Ljc4OTEgMTEuMjk3NVoiIGZpbGw9ImJsYWNrIiBmaWxsLW9wYWNpdHk9IjAuNzYiLz4KPHBhdGggZD0iTTEwLjAzNDMgNy4wMjQyNUMxMC4zMDY4IDUuODk0NDQgMTEuODg2OCA1Ljg5NDQ0IDEyLjE1OTQgNy4wMjQyNUwxMi42OTg5IDkuMjYyOThDMTIuNzkyNyA5LjY1MTc0IDEzLjA4NTEgOS45NTg4NyAxMy40NjQgMTAuMDY3OUwxNS41NzczIDEwLjY3NTFDMTYuNjM5MyAxMC45ODAzIDE2LjYzOTMgMTIuNTEwOSAxNS41NzczIDEyLjgxNjFMMTMuNDUzMyAxMy40MjY1QzEzLjA4MDIgMTMuNTMzOCAxMi43OTA4IDEzLjgzMzkgMTIuNjkyNSAxNC4yMTUxTDEyLjE1NTEgMTYuMzA0QzExLjg3IDE3LjQxMTYgMTAuMzIzNiAxNy40MTE2IDEwLjAzODUgMTYuMzA0TDkuNTAwMDMgMTQuMjE1MUM5LjQwMTczIDEzLjgzMzkgOS4xMTIzNSAxMy41MzM3IDguNzM5MyAxMy40MjY1TDYuNjE1MjQgMTIuODE2MUM1LjU1Mzc4IDEyLjUxMDYgNS41NTM2NCAxMC45ODA0IDYuNjE1MjQgMTAuNjc1MUw4LjcyODYyIDEwLjA2NzlDOS4xMDc2IDkuOTU4OTggOS4zOTk3OCA5LjY1MTg0IDkuNDkzNjIgOS4yNjI5OEwxMC4wMzQzIDcuMDI0MjVaIiBmaWxsPSJibGFjayIgZmlsbC1vcGFjaXR5PSIwLjc2Ii8+CjxwYXRoIGQ9Ik0yNS45MDI4IDYuNzMzMTNDMjYuMTg3OCA1LjYyNTQxIDI3LjczNDMgNS42MjU0MSAyOC4wMTkzIDYuNzMzMTNMMjguMjAzMSA3LjQ0Njc5QzI4LjMwMyA3LjgzNDMxIDI4LjYwMDEgOC4xMzcwNSAyOC45ODA5IDguMjM5NzVMMjkuNTM0NCA4LjM4OTY1QzMwLjYxOTIgOC42ODIxMiAzMC42MTkzIDEwLjI0NjkgMjkuNTM0NCAxMC41MzkzTDI4Ljk2OTIgMTAuNjkxNEMyOC41OTQ0IDEwLjc5MjUgMjguMjk5OSAxMS4wODgzIDI4LjE5NTYgMTEuNDY4TDI4LjAxNTEgMTIuMTI4NUMyNy43MTc0IDEzLjIxMjggMjYuMjA0NyAxMy4yMTI4IDI1LjkwNyAxMi4xMjg1TDI1LjcyNTQgMTEuNDY4QzI1LjYyMTEgMTEuMDg4MiAyNS4zMjY4IDEwLjc5MjQgMjQuOTUxOCAxMC42OTE0TDI0LjM4NzcgMTAuNTM5M0MyMy4zMDI2IDEwLjI0NyAyMy4zMDI2IDguNjgxOTggMjQuMzg3NyA4LjM4OTY1TDI0Ljk0MDEgOC4yMzk3NUMyNS4zMjExIDguMTM3MDkgMjUuNjE5MSA3LjgzNDQ2IDI1LjcxOSA3LjQ0Njc5TDI1LjkwMjggNi43MzMxM1oiIGZpbGw9ImJsYWNrIiBmaWxsLW9wYWNpdHk9IjAuNzYiLz4KPC9zdmc+Cg==",
        docUrl = "https://thingsboard.io/docs/user-guide/rule-engine-2-0/nodes/external/ai-request/"
)
public final class TbAiNode extends TbAbstractExternalNode implements TbNode {

    private String systemPrompt;
    private String userPrompt;
    private Set<TbResourceId> resourceIds;
    private ResponseFormat responseFormat;
    private int timeoutSeconds;
    private AiModelId modelId;

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

        modelId = config.getModelId();
        Optional<AiModel> modelOpt = ctx.getAiModelService().findAiModelByTenantIdAndId(ctx.getTenantId(), modelId);
        if (modelOpt.isEmpty()) {
            throw new TbNodeException("[" + ctx.getTenantId() + "] AI model with ID: [" + modelId + "] was not found", true);
        }
        AiModel model = modelOpt.get();
        AiModelType modelType = model.getConfiguration().modelType();
        if (modelType != AiModelType.CHAT) {
            throw new TbNodeException("[" + ctx.getTenantId() + "] AI model with ID: [" + modelId + "] must be of type CHAT, but was " + modelType, true);
        }
        AiChatModelConfig<?> chatModelConfig = (AiChatModelConfig<?>) model.getConfiguration();
        if (isJsonModeConfigured(config)) {
            if (!chatModelConfig.supportsJsonMode()) {
                throw new TbNodeException("[" + ctx.getTenantId() + "] AI model with ID: [" + modelId + "] does not support '" + config.getResponseFormat().type() + "' response format", true);
            }
            // LangChain4j AnthropicChatModel rejects requests with non-null ResponseFormat even if ResponseFormatType is TEXT
            responseFormat = config.getResponseFormat().toLangChainResponseFormat();
        }
        if (config.getResourceIds() != null && !config.getResourceIds().isEmpty()) {
            resourceIds = new HashSet<>(config.getResourceIds().size());
            for (UUID resourceId : config.getResourceIds()) {
                TbResourceId tbResourceId = new TbResourceId(resourceId);
                validateResource(ctx, tbResourceId);
                resourceIds.add(tbResourceId);
            }
        }

        systemPrompt = config.getSystemPrompt();
        userPrompt = config.getUserPrompt();
        timeoutSeconds = config.getTimeoutSeconds();
        super.forceAck = config.isForceAck() || super.forceAck; // force ack if node config says so, or if env variable (super.forceAck) says so
    }

    private static boolean isJsonModeConfigured(TbAiNodeConfiguration config) {
        var responseFormatType = config.getResponseFormat().type();
        return responseFormatType == TbResponseFormatType.JSON || responseFormatType == TbResponseFormatType.JSON_SCHEMA;
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        var ackedMsg = ackIfNeeded(ctx, msg);
        final String processedUserPrompt = TbNodeUtils.processPattern(this.userPrompt, ackedMsg);

        final ListenableFuture<UserMessage> userMessageFuture =
                resourceIds == null
                        ? Futures.immediateFuture(UserMessage.from(processedUserPrompt))
                        : Futures.transform(
                        loadResources(ctx),
                        resources -> UserMessage.from(buildContents(processedUserPrompt, resources)),
                        ctx.getDbCallbackExecutor()
                );

        Futures.addCallback(
                userMessageFuture,
                new FutureCallback<>() {
                    @Override
                    public void onSuccess(UserMessage userMessage) {
                        buildAndSendRequest(ctx, ackedMsg, userMessage);
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        tellFailure(ctx, ackedMsg, t);
                    }
                },
                MoreExecutors.directExecutor()
        );
    }

    private void buildAndSendRequest(TbContext ctx, TbMsg ackedMsg, UserMessage userMessage) {
        List<ChatMessage> chatMessages = new ArrayList<>(2);

        if (systemPrompt != null && !systemPrompt.isBlank()) {
            chatMessages.add(SystemMessage.from(TbNodeUtils.processPattern(systemPrompt, ackedMsg)));
        }

        chatMessages.add(userMessage);

        var chatRequest = ChatRequest.builder()
                .messages(chatMessages)
                .responseFormat(responseFormat)
                .build();

        sendChatRequestAsync(ctx, chatRequest).addCallback(new FutureCallback<>() {
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

    private <C extends AiChatModelConfig<C>> FluentFuture<ChatResponse> sendChatRequestAsync(TbContext ctx, ChatRequest chatRequest) {
        return ctx.getAiModelService().findAiModelByTenantIdAndIdAsync(ctx.getTenantId(), modelId).transformAsync(modelOpt -> {
            if (modelOpt.isEmpty()) {
                throw new NoSuchElementException("[" + ctx.getTenantId() + "] AI model with ID: [" + modelId + "] was not found");
            }
            AiModel model = modelOpt.get();
            AiModelType modelType = model.getConfiguration().modelType();
            if (modelType != AiModelType.CHAT) {
                throw new IllegalStateException("[" + ctx.getTenantId() + "] AI model with ID: [" + modelId + "] must be of type CHAT, but was " + modelType);
            }

            @SuppressWarnings("unchecked")
            AiChatModelConfig<C> chatModelConfig = (AiChatModelConfig<C>) model.getConfiguration();

            chatModelConfig = chatModelConfig
                    .withTimeoutSeconds(timeoutSeconds)
                    .withMaxRetries(0); // disable retries to respect timeout set in rule node config

            return ctx.getAiChatModelService().sendChatRequestAsync(chatModelConfig, chatRequest);
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

    private void validateResource(TbContext ctx, TbResourceId tbResourceId) throws TbNodeException {
        TbResourceInfo resource = ctx.getResourceService().findResourceInfoById(ctx.getTenantId(), tbResourceId);
        if (resource == null) {
            throw new TbNodeException("[" + ctx.getTenantId() + "] Resource with ID: [" + tbResourceId + "] was not found", true);
        }
        if (!ResourceType.GENERAL.equals(resource.getResourceType())) {
            throw new TbNodeException("[" + ctx.getTenantId() + "] Resource with ID: [" + tbResourceId + "] has unsupported resource type: " + resource.getResourceType(), true);
        }
        ctx.checkTenantOrSystemEntity(resource);
    }

    private ListenableFuture<List<TbResourceDataInfo>> loadResources(TbContext ctx) {
        final TenantId tenantId = ctx.getTenantId();
        final TbResourceDataCache cache = ctx.getTbResourceDataCache();
        List<? extends ListenableFuture<TbResourceDataInfo>> futures = resourceIds.stream()
                .map(id -> cache.getResourceDataInfoAsync(tenantId, id))
                .toList();
        return Futures.allAsList(futures);
    }

    private List<Content> buildContents(String userPrompt, List<TbResourceDataInfo> resources) {
        List<Content> contents = new ArrayList<>(1 + resources.size());
        contents.add(new TextContent(userPrompt)); // user prompt first

        resources.stream()
                .filter(Objects::nonNull)
                .map(this::toContent)
                .forEach(contents::add);

        return contents;
    }

    private Content toContent(TbResourceDataInfo resource) {
        if (resource.getDescriptor() == null) {
            throw new RuntimeException("Missing descriptor for resource");
        }
        GeneralFileDescriptor descriptor = JacksonUtil.treeToValue(resource.getDescriptor(), GeneralFileDescriptor.class);
        String mediaType = descriptor.getMediaType();
        if (mediaType == null) {
            throw new RuntimeException("Missing mediaType in resource descriptor " + resource.getDescriptor());
        }
        byte[] data = resource.getData();
        if (mediaType.startsWith("text/")) {
            return new TextContent(new String(data, StandardCharsets.UTF_8));
        }
        if (mediaType.equals("application/pdf")) {
            return new PdfFileContent(Base64.getEncoder().encodeToString(data), mediaType);
        }
        if (mediaType.startsWith("image/")) {
            return new ImageContent(Base64.getEncoder().encodeToString(data), mediaType);
        }
        log.debug("Trying to create text content for {}", resource.getDescriptor());
        return new TextContent(new String(data, StandardCharsets.UTF_8));
    }

    @Override
    public void destroy() {
        super.destroy();
        systemPrompt = null;
        userPrompt = null;
        resourceIds = null;
        responseFormat = null;
        modelId = null;
    }

}
