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
package org.thingsboard.rule.engine.ai;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.Futures;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.ResponseFormatType;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.TestDbCallbackExecutor;
import org.thingsboard.rule.engine.ai.TbResponseFormat.TbJsonResponseFormat;
import org.thingsboard.rule.engine.ai.TbResponseFormat.TbJsonSchemaResponseFormat;
import org.thingsboard.rule.engine.ai.TbResponseFormat.TbTextResponseFormat;
import org.thingsboard.rule.engine.api.RuleEngineAiChatModelService;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.GeneralFileDescriptor;
import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.TbResourceDataInfo;
import org.thingsboard.server.common.data.ai.AiModel;
import org.thingsboard.server.common.data.ai.model.AiModelConfig;
import org.thingsboard.server.common.data.ai.model.chat.AnthropicChatModelConfig;
import org.thingsboard.server.common.data.ai.model.chat.OpenAiChatModelConfig;
import org.thingsboard.server.common.data.ai.provider.AnthropicProviderConfig;
import org.thingsboard.server.common.data.ai.provider.OpenAiProviderConfig;
import org.thingsboard.server.common.data.id.AiModelId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.ai.AiModelService;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.dao.resource.ResourceService;
import org.thingsboard.server.dao.resource.TbResourceDataCache;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static com.google.common.util.concurrent.Futures.immediateFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.thingsboard.server.common.data.ResourceType.GENERAL;

@ExtendWith(MockitoExtension.class)
class TbAiNodeTest {

    private static final byte[] PNG_IMAGE = Base64.getDecoder().decode("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");

    @Mock
    TbContext ctxMock;
    @Mock
    AiModelService aiModelServiceMock;
    @Mock
    RuleEngineAiChatModelService aiChatModelServiceMock;
    @Mock
    TbResourceDataCache tbResourceDataCacheMock;
    @Mock
    ResourceService resourceServiceMock;

    TbAiNode aiNode;
    TbAiNodeConfiguration config;

    TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
    DeviceId deviceId = new DeviceId(UUID.randomUUID());
    AiModelId modelId = new AiModelId(UUID.randomUUID());
    RuleNodeId ruleNodeId = new RuleNodeId(UUID.randomUUID());

    RuleNode ruleNode;

    AiModel model;
    AiModelConfig modelConfig;

    boolean externalNodeForceAck = false;

    @BeforeEach
    void setup() {
        aiNode = new TbAiNode();
        config = new TbAiNodeConfiguration();

        modelConfig = OpenAiChatModelConfig.builder()
                .providerConfig(OpenAiProviderConfig.builder()
                        .baseUrl(OpenAiProviderConfig.OPENAI_OFFICIAL_BASE_URL)
                        .apiKey("test-api-key")
                        .build())
                .modelId("gpt-4o")
                .temperature(0.5)
                .topP(0.3)
                .frequencyPenalty(0.1)
                .presencePenalty(0.2)
                .maxOutputTokens(1000)
                .timeoutSeconds(100)
                .maxRetries(2)
                .build();

        model = AiModel.builder()
                .tenantId(tenantId)
                .name("Test model")
                .configuration(modelConfig)
                .build();

        model.setId(modelId);
        model.setVersion(1L);
        model.setCreatedTime(123L);
        lenient().when(aiModelServiceMock.findAiModelByTenantIdAndId(tenantId, modelId)).thenReturn(Optional.of(model));
        lenient().when(aiModelServiceMock.findAiModelByTenantIdAndIdAsync(tenantId, modelId)).thenReturn(FluentFuture.from(immediateFuture(Optional.of(model))));

        ruleNode = new RuleNode();
        ruleNode.setId(ruleNodeId);
        ruleNode.setName("Test AI node");
        lenient().when(ctxMock.getSelf()).thenReturn(ruleNode);

        lenient().when(ctxMock.isExternalNodeForceAck()).thenReturn(externalNodeForceAck);
        lenient().when(ctxMock.getTenantId()).thenReturn(tenantId);
        lenient().when(ctxMock.getAiModelService()).thenReturn(aiModelServiceMock);
        lenient().when(ctxMock.getAiChatModelService()).thenReturn(aiChatModelServiceMock);
        lenient().when(ctxMock.getDbCallbackExecutor()).thenReturn(new TestDbCallbackExecutor());
        lenient().when(ctxMock.getTbResourceDataCache()).thenReturn(tbResourceDataCacheMock);
        lenient().when(ctxMock.getResourceService()).thenReturn(resourceServiceMock);
    }

    @Test
    void givenDefaultConfig_whenCalled_thenSetsCorrectValues() {
        // GIVEN-WHEN
        config = config.defaultConfiguration();

        // THEN
        assertThat(config.getModelId()).isNull();
        assertThat(config.getSystemPrompt()).isEqualTo(
                "You are a helpful AI assistant. Your primary function is to process the user's request and respond with a valid JSON object. " +
                        "Do not include any text, explanations, or markdown formatting before or after the JSON output."
        );
        assertThat(config.getUserPrompt()).isNull();
        assertThat(config.getResponseFormat()).isEqualTo(new TbJsonResponseFormat());
        assertThat(config.getTimeoutSeconds()).isEqualTo(60);
        assertThat(config.isForceAck()).isTrue();
        assertThat(config.getResourceIds()).isNull();
    }

    /* -- Node initialization tests -- */

    @Test
    void givenNullModelId_whenInit_thenThrowsUnrecoverableTbNodeException() {
        // GIVEN
        config = constructValidConfig();
        config.setModelId(null);

        // WHEN-THEN
        assertThatThrownBy(() -> aiNode.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config))))
                .isInstanceOf(TbNodeException.class)
                .hasRootCauseInstanceOf(DataValidationException.class)
                .hasRootCauseMessage("'" + ruleNode.getName() + "' node configuration is invalid: modelId must not be null")
                .matches(e -> ((TbNodeException) e).isUnrecoverable());
    }

    @ParameterizedTest
    @MethodSource("invalidSystemPrompts")
    void givenInvalidSystemPrompt_whenInit_thenThrowsUnrecoverableTbNodeException(String invalidSystemPrompt) {
        // GIVEN
        config = constructValidConfig();
        config.setSystemPrompt(invalidSystemPrompt);

        // WHEN-THEN
        assertThatThrownBy(() -> aiNode.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config))))
                .isInstanceOf(TbNodeException.class)
                .matches(e -> ((TbNodeException) e).isUnrecoverable())
                .rootCause()
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("'" + ruleNode.getName() + "' node configuration is invalid: systemPrompt");
    }

    static Stream<Arguments> invalidSystemPrompts() {
        String tooLongString = "a".repeat(500_001);
        return Stream.of(
                Arguments.of(""),
                Arguments.of(tooLongString)
        );
    }

    @ParameterizedTest
    @MethodSource("validSystemPrompts")
    void givenValidSystemPrompt_whenInit_thenInitializesSuccessfully(String validSystemPrompt) {
        // GIVEN
        config = constructValidConfig();
        config.setSystemPrompt(validSystemPrompt);

        // WHEN-THEN
        assertThatNoException().isThrownBy(() -> aiNode.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config))));
    }

    static Stream<Arguments> validSystemPrompts() {
        String longString = "a".repeat(500_000);
        return Stream.of(
                Arguments.of((String) null),
                Arguments.of("a"),
                Arguments.of("Test system prompt"),
                Arguments.of(longString),
                Arguments.of("""
                        first sentence
                        
                        second sentence
                        """)
        );
    }

    @ParameterizedTest
    @MethodSource("invalidUserPrompts")
    void givenInvalidUserPrompt_whenInit_thenThrowsUnrecoverableTbNodeException(String invalidUserPrompt) {
        // GIVEN
        config = constructValidConfig();
        config.setUserPrompt(invalidUserPrompt);

        // WHEN-THEN
        assertThatThrownBy(() -> aiNode.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config))))
                .isInstanceOf(TbNodeException.class)
                .matches(e -> ((TbNodeException) e).isUnrecoverable())
                .rootCause()
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("'" + ruleNode.getName() + "' node configuration is invalid: userPrompt");
    }

    static Stream<Arguments> invalidUserPrompts() {
        String tooLongString = "a".repeat(500_001);
        return Stream.of(
                Arguments.of((String) null),
                Arguments.of(""),
                Arguments.of("   "),
                Arguments.of(tooLongString)
        );
    }

    @ParameterizedTest
    @MethodSource("validUserPrompts")
    void givenValidUserPrompt_whenInit_thenInitializesSuccessfully(String validUserPrompt) {
        // GIVEN
        config = constructValidConfig();
        config.setUserPrompt(validUserPrompt);

        // WHEN-THEN
        assertThatNoException().isThrownBy(() -> aiNode.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config))));
    }

    static Stream<Arguments> validUserPrompts() {
        String longString = "a".repeat(500_000);
        return Stream.of(
                Arguments.of("a"),
                Arguments.of("Test user prompt"),
                Arguments.of(longString),
                Arguments.of("""
                        first sentence
                        
                        second sentence
                        """)
        );
    }

    @Test
    void givenNullResponseFormat_whenInit_thenThrowsUnrecoverableTbNodeException() {
        // GIVEN
        config = constructValidConfig();
        config.setResponseFormat(null);

        // WHEN-THEN
        assertThatThrownBy(() -> aiNode.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config))))
                .isInstanceOf(TbNodeException.class)
                .hasRootCauseInstanceOf(DataValidationException.class)
                .hasRootCauseMessage("'" + ruleNode.getName() + "' node configuration is invalid: responseFormat must not be null")
                .matches(e -> ((TbNodeException) e).isUnrecoverable());
    }

    @ParameterizedTest
    @ValueSource(ints = {Integer.MIN_VALUE, 0, 601, Integer.MAX_VALUE})
    void givenInvalidTimeoutSeconds_whenInit_thenThrowsUnrecoverableTbNodeException(int invalidTimeoutSeconds) {
        // GIVEN
        config = constructValidConfig();
        config.setTimeoutSeconds(invalidTimeoutSeconds);

        // WHEN-THEN
        assertThatThrownBy(() -> aiNode.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config))))
                .isInstanceOf(TbNodeException.class)
                .matches(e -> ((TbNodeException) e).isUnrecoverable())
                .rootCause()
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("'" + ruleNode.getName() + "' node configuration is invalid: timeoutSeconds");
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 60, 600})
    void givenValidTimeoutSeconds_whenInit_thenInitializesSuccessfully(int validTimeoutSeconds) {
        // GIVEN
        config = constructValidConfig();
        config.setTimeoutSeconds(validTimeoutSeconds);

        // WHEN-THEN
        assertThatNoException().isThrownBy(() -> aiNode.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config))));
    }

    @Test
    void givenAiModelNotFound_whenInit_thenThrowsUnrecoverableTbNodeException() {
        // GIVEN
        config = constructValidConfig();
        given(aiModelServiceMock.findAiModelByTenantIdAndId(tenantId, modelId)).willReturn(Optional.empty());

        // WHEN-THEN
        assertThatThrownBy(() -> aiNode.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config))))
                .isInstanceOf(TbNodeException.class)
                .hasMessage("[" + tenantId + "] AI model with ID: [" + modelId + "] was not found")
                .matches(e -> ((TbNodeException) e).isUnrecoverable());
    }

    TbAiNodeConfiguration constructValidConfig() {
        var config = new TbAiNodeConfiguration();
        config.setModelId(modelId);
        config.setSystemPrompt("Test system prompt");
        config.setUserPrompt("Test user prompt");
        config.setResponseFormat(new TbJsonResponseFormat());
        config.setTimeoutSeconds(60);
        config.setForceAck(true);
        return config;
    }


    @Test
    void givenJsonModeConfiguredButModelDoesNotSupportIt_whenInit_thenThrowsUnrecoverableTbNodeException() {
        // GIVEN
        config = constructValidConfig();
        config.setResponseFormat(new TbJsonResponseFormat());

        modelConfig = AnthropicChatModelConfig.builder()
                .providerConfig(new AnthropicProviderConfig("test-api-key"))
                .modelId("claude-sonnet-4-0")
                .build();

        model = AiModel.builder()
                .tenantId(tenantId)
                .name("Test model")
                .configuration(modelConfig)
                .build();

        model.setId(modelId);
        model.setVersion(1L);
        model.setCreatedTime(123L);

        given(aiModelServiceMock.findAiModelByTenantIdAndId(tenantId, modelId)).willReturn(Optional.of(model));

        // WHEN-THEN
        assertThatThrownBy(() -> aiNode.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config))))
                .isInstanceOf(TbNodeException.class)
                .hasMessage("[" + tenantId + "] AI model with ID: [" + modelId + "] does not support 'JSON' response format")
                .matches(e -> ((TbNodeException) e).isUnrecoverable());
    }

    @Test
    void givenNotExistingResources_whenInit_thenThrowsException() {
        // GIVEN
        config = constructValidConfig();
        UUID resourceId = UUID.randomUUID();
        config.setResourceIds(Set.of(resourceId));

        // WHEN-THEN
        assertThatThrownBy(() -> aiNode.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config))))
                .isInstanceOf(TbNodeException.class)
                .hasMessageContaining("[" + tenantId + "] Resource with ID: [" + resourceId + "] was not found");
    }

    @Test
    void givenResourceOfWrongType_whenInit_thenThrowsException() {
        // GIVEN
        config = constructValidConfig();
        UUID resourceId = UUID.randomUUID();
        config.setResourceIds(Set.of(resourceId));

        // WHEN-THEN
        TbResource tbResource = new TbResource();
        tbResource.setResourceType(ResourceType.DASHBOARD);
        given(resourceServiceMock.findResourceInfoById(any(), any())).willReturn(tbResource);

        assertThatThrownBy(() -> aiNode.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config))))
                .isInstanceOf(TbNodeException.class)
                .hasMessageContaining("[" + tenantId + "] Resource with ID: [" + resourceId + "] has unsupported resource type: " + ResourceType.DASHBOARD);
    }

    /* -- Message processing tests -- */

    @Test
    void givenForceAckIsFalse_whenOnMsg_thenTellSuccessIsCalled() throws TbNodeException {
        // GIVEN
        config.setModelId(modelId);
        config.setSystemPrompt("Respond with valid JSON");
        config.setUserPrompt("Tell me a joke");
        config.setResponseFormat(new TbJsonResponseFormat());
        config.setTimeoutSeconds(10);
        config.setForceAck(false);

        aiNode.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        var msg = TbMsg.newMsg()
                .originator(deviceId)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .metaData(TbMsgMetaData.EMPTY)
                .build();

        var chatResponse = ChatResponse.builder()
                .aiMessage(AiMessage.from("{\"type\":\"joke\",\"setup\":\"Why did the scarecrow win an award?\",\"punchline\":\"Because he was outstanding in his field.\"}"))
                .build();

        given(aiChatModelServiceMock.sendChatRequestAsync(any(), any())).willReturn(FluentFuture.from(immediateFuture(chatResponse)));

        // WHEN
        aiNode.onMsg(ctxMock, msg);

        // THEN
        then(ctxMock).should().tellSuccess(any());

        then(ctxMock).should(never()).enqueueForTellNext(any(), any(String.class));
        then(ctxMock).should(never()).enqueueForTellFailure(any(), any(Throwable.class));
        then(ctxMock).should(never()).tellNext(any(), any(String.class));
        then(ctxMock).should(never()).tellFailure(any(), any());
    }

    @Test
    void givenLocalForceAckIsFalseButExternalIsTold_whenOnMsg_thenEnqueuesForTellNext() throws TbNodeException {
        // GIVEN
        config.setModelId(modelId);
        config.setSystemPrompt("Respond with valid JSON");
        config.setUserPrompt("Tell me a joke");
        config.setResponseFormat(new TbJsonResponseFormat());
        config.setTimeoutSeconds(10);
        config.setForceAck(false);

        given(ctxMock.isExternalNodeForceAck()).willReturn(true);

        aiNode.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        var msg = TbMsg.newMsg()
                .originator(deviceId)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .metaData(TbMsgMetaData.EMPTY)
                .build();

        var chatResponse = ChatResponse.builder()
                .aiMessage(AiMessage.from("{\"type\":\"joke\",\"setup\":\"Why did the scarecrow win an award?\",\"punchline\":\"Because he was outstanding in his field.\"}"))
                .build();

        given(aiChatModelServiceMock.sendChatRequestAsync(any(), any())).willReturn(FluentFuture.from(immediateFuture(chatResponse)));

        // WHEN
        aiNode.onMsg(ctxMock, msg);

        // THEN
        then(ctxMock).should().enqueueForTellNext(any(), eq(TbNodeConnectionType.SUCCESS));

        then(ctxMock).should(never()).tellSuccess(any());
        then(ctxMock).should(never()).enqueueForTellFailure(any(), any(Throwable.class));
        then(ctxMock).should(never()).tellNext(any(), any(String.class));
        then(ctxMock).should(never()).tellFailure(any(), any());
    }

    @Test
    void givenForceAckIsTrue_whenOnMsg_thenEnqueuesForTellNext() throws TbNodeException {
        // GIVEN
        config.setModelId(modelId);
        config.setSystemPrompt("Respond with valid JSON");
        config.setUserPrompt("Tell me a joke");
        config.setResponseFormat(new TbJsonResponseFormat());
        config.setTimeoutSeconds(10);
        config.setForceAck(true);

        aiNode.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        var msg = TbMsg.newMsg()
                .originator(deviceId)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .metaData(TbMsgMetaData.EMPTY)
                .build();

        var chatResponse = ChatResponse.builder()
                .aiMessage(AiMessage.from("{\"type\":\"joke\",\"setup\":\"Why did the scarecrow win an award?\",\"punchline\":\"Because he was outstanding in his field.\"}"))
                .build();

        given(aiChatModelServiceMock.sendChatRequestAsync(any(), any())).willReturn(FluentFuture.from(immediateFuture(chatResponse)));

        // WHEN
        aiNode.onMsg(ctxMock, msg);

        // THEN
        then(ctxMock).should().enqueueForTellNext(any(), eq(TbNodeConnectionType.SUCCESS));

        then(ctxMock).should(never()).tellSuccess(any());
        then(ctxMock).should(never()).enqueueForTellFailure(any(), any(Throwable.class));
        then(ctxMock).should(never()).tellNext(any(), any(String.class));
        then(ctxMock).should(never()).tellFailure(any(), any());
    }

    @Test
    void givenOnlyUserPromptConfigured_whenOnMsg_thenRequestContainsOnlyUserMessage() throws TbNodeException {
        // GIVEN
        config.setModelId(modelId);
        config.setSystemPrompt(null);
        config.setUserPrompt("Tell me a joke");
        config.setResponseFormat(new TbJsonResponseFormat());
        config.setTimeoutSeconds(10);
        config.setForceAck(true);

        aiNode.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        var msg = TbMsg.newMsg()
                .originator(deviceId)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .metaData(TbMsgMetaData.EMPTY)
                .build();

        var chatResponse = ChatResponse.builder()
                .aiMessage(AiMessage.from("{\"type\":\"joke\",\"setup\":\"Why did the scarecrow win an award?\",\"punchline\":\"Because he was outstanding in his field.\"}"))
                .build();

        given(aiChatModelServiceMock.sendChatRequestAsync(any(), any())).willReturn(FluentFuture.from(immediateFuture(chatResponse)));

        // WHEN
        aiNode.onMsg(ctxMock, msg);

        // THEN
        then(aiChatModelServiceMock).should().sendChatRequestAsync(any(),
                argThat(actualChatRequest -> {
                    assertThat(actualChatRequest.messages()).hasSize(1);
                    assertThat(actualChatRequest.messages().get(0)).isEqualTo(UserMessage.from("Tell me a joke"));
                    return true;
                })
        );
    }

    @Test
    void givenSystemAndUserPromptsConfigured_whenOnMsg_thenRequestContainsBothSystemAndUserMessages() throws TbNodeException {
        // GIVEN
        config.setModelId(modelId);
        config.setSystemPrompt("Respond with valid JSON");
        config.setUserPrompt("Tell me a joke");
        config.setResponseFormat(new TbJsonResponseFormat());
        config.setTimeoutSeconds(10);
        config.setForceAck(true);

        aiNode.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        var msg = TbMsg.newMsg()
                .originator(deviceId)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .metaData(TbMsgMetaData.EMPTY)
                .build();

        var chatResponse = ChatResponse.builder()
                .aiMessage(AiMessage.from("{\"type\":\"joke\",\"setup\":\"Why did the scarecrow win an award?\",\"punchline\":\"Because he was outstanding in his field.\"}"))
                .build();

        given(aiChatModelServiceMock.sendChatRequestAsync(any(), any())).willReturn(FluentFuture.from(immediateFuture(chatResponse)));

        // WHEN
        aiNode.onMsg(ctxMock, msg);

        // THEN
        then(aiChatModelServiceMock).should().sendChatRequestAsync(any(),
                argThat(actualChatRequest -> {
                    assertThat(actualChatRequest.messages()).hasSize(2);
                    assertThat(actualChatRequest.messages().get(0)).isEqualTo(SystemMessage.from("Respond with valid JSON"));
                    assertThat(actualChatRequest.messages().get(1)).isEqualTo(UserMessage.from("Tell me a joke"));
                    return true;
                })
        );
    }

    @Test
    void givenSystemPromptAndUserPromptAndResourcesConfigured_whenOnMsg_thenRequestContainsSystemAndUserAndResourceContent() throws TbNodeException {
        String systemPrompt = "Respond with valid JSON";
        String userPrompt = "Tell me a joke";
        String textData = "Text resource content for AI request.";
        String xmlData = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><test></test>";

        // GIVEN
        config = constructValidConfig();
        config.setSystemPrompt(systemPrompt);
        config.setUserPrompt(userPrompt);
        UUID resourceId = UUID.randomUUID();
        UUID resourceId2 = UUID.randomUUID();
        UUID resourceId3 = UUID.randomUUID();

        config.setResourceIds(Set.of(resourceId, resourceId2, resourceId3));

        // WHEN-THEN
        TbResource textResource = buildGeneralResource(textData.getBytes(), "text/plain");
        TbResource xmlResource = buildGeneralResource(xmlData.getBytes(), "application/xml");
        TbResource imageResource = buildGeneralResource(PNG_IMAGE, "image/png");

        given(resourceServiceMock.findResourceInfoById(any(), eq(new TbResourceId(resourceId)))).willReturn(textResource);
        given(resourceServiceMock.findResourceInfoById(any(), eq(new TbResourceId(resourceId2)))).willReturn(xmlResource);
        given(resourceServiceMock.findResourceInfoById(any(), eq(new TbResourceId(resourceId3)))).willReturn(imageResource);

        given(tbResourceDataCacheMock.getResourceDataInfoAsync(any(), eq(new TbResourceId(resourceId)))).willReturn(FluentFuture.from(Futures.immediateFuture(textResource.toResourceDataInfo())));
        given(tbResourceDataCacheMock.getResourceDataInfoAsync(any(), eq(new TbResourceId(resourceId2)))).willReturn(FluentFuture.from(Futures.immediateFuture(xmlResource.toResourceDataInfo())));
        given(tbResourceDataCacheMock.getResourceDataInfoAsync(any(), eq(new TbResourceId(resourceId3)))).willReturn(FluentFuture.from(Futures.immediateFuture(imageResource.toResourceDataInfo())));

        aiNode.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        var msg = TbMsg.newMsg()
                .originator(deviceId)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .metaData(TbMsgMetaData.EMPTY)
                .build();

        var chatResponse = ChatResponse.builder()
                .aiMessage(AiMessage.from("{\"type\":\"joke\",\"setup\":\"Why did the scarecrow win an award?\",\"punchline\":\"Because he was outstanding in his field.\"}"))
                .build();

        given(aiChatModelServiceMock.sendChatRequestAsync(any(), any())).willReturn(FluentFuture.from(immediateFuture(chatResponse)));

        // WHEN
        aiNode.onMsg(ctxMock, msg);

        // THEN
        then(aiChatModelServiceMock).should().sendChatRequestAsync(any(),
                argThat(actualChatRequest -> {
                    assertThat(actualChatRequest.messages()).hasSize(2);
                    assertThat(actualChatRequest.messages().get(0)).isEqualTo(SystemMessage.from(systemPrompt));
                    assertThat(((UserMessage)actualChatRequest.messages().get(1)).contents())
                            .containsAll(List.of(new TextContent(userPrompt), new TextContent(textData),
                                    new TextContent(xmlData), new ImageContent(Base64.getEncoder().encodeToString(PNG_IMAGE), "image/png")));
                    return true;
                })
        );
    }

    @Test
    void givenNullResource_whenOnMsg_thenRequestContainsSystemAndUserPrompt() throws TbNodeException {
        // GIVEN
        config = constructValidConfig();
        UUID resourceId = UUID.randomUUID();
        config.setResourceIds(Set.of(resourceId));

        // WHEN-THEN
        TbResource tbResource = buildGeneralResource("Text resource content for AI request.".getBytes(), "text/plain");

        given(resourceServiceMock.findResourceInfoById(any(), eq(new TbResourceId(resourceId)))).willReturn(tbResource);
        given(tbResourceDataCacheMock.getResourceDataInfoAsync(any(), eq(new TbResourceId(resourceId)))).willReturn(FluentFuture.from(Futures.immediateFuture(null)));

        aiNode.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        var msg = TbMsg.newMsg()
                .originator(deviceId)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .metaData(TbMsgMetaData.EMPTY)
                .build();

        // WHEN
        aiNode.onMsg(ctxMock, msg);

        // THEN
        then(aiChatModelServiceMock).should().sendChatRequestAsync(any(),
                argThat(actualChatRequest -> {
                    assertThat(actualChatRequest.messages()).hasSize(2);
                    assertThat(actualChatRequest.messages().get(0)).isEqualTo(SystemMessage.from(config.getSystemPrompt()));
                    assertThat(((UserMessage)actualChatRequest.messages().get(1)).contents())
                            .containsAll(List.of(new TextContent(config.getUserPrompt())));
                    return true;
                })
        );
    }

    @Test
    void givenResourceWithNoDescriptor_whenOnMsg_thenEnqueueForTellFailure() throws TbNodeException {
        // GIVEN
        config = constructValidConfig();
        UUID resourceId = UUID.randomUUID();
        config.setResourceIds(Set.of(resourceId));

        // WHEN-THEN
        TbResource tbResource = buildGeneralResource("Text resource content for AI request.".getBytes(), "text/plain");
        TbResourceDataInfo resourceDataInfo = new TbResourceDataInfo(tbResource.getData(), null);

        given(resourceServiceMock.findResourceInfoById(any(), eq(new TbResourceId(resourceId)))).willReturn(tbResource);
        given(tbResourceDataCacheMock.getResourceDataInfoAsync(any(), eq(new TbResourceId(resourceId)))).willReturn(FluentFuture.from(Futures.immediateFuture(resourceDataInfo)));

        aiNode.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        var msg = TbMsg.newMsg()
                .originator(deviceId)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .metaData(TbMsgMetaData.EMPTY)
                .build();

        // WHEN
        aiNode.onMsg(ctxMock, msg);

        // THEN
        var exceptionCaptor = ArgumentCaptor.forClass(Throwable.class);
        then(ctxMock).should().enqueueForTellFailure(any(), exceptionCaptor.capture());
        Throwable actualException = exceptionCaptor.getValue();
        assertThat(actualException.getMessage()).isEqualTo("Missing descriptor for resource");
    }

    @Test
    void givenResourceWithNoMediaType_whenOnMsg_thenEnqueueForTellFailure() throws TbNodeException {
        // GIVEN
        config = constructValidConfig();
        UUID resourceId = UUID.randomUUID();
        config.setResourceIds(Set.of(resourceId));

        // WHEN-THEN
        TbResource tbResource = buildGeneralResource("Text resource content for AI request.".getBytes(), "text/plain");
        TbResourceDataInfo resourceDataInfo = new TbResourceDataInfo(tbResource.getData(), JacksonUtil.newObjectNode());

        given(resourceServiceMock.findResourceInfoById(any(), eq(new TbResourceId(resourceId)))).willReturn(tbResource);
        given(tbResourceDataCacheMock.getResourceDataInfoAsync(any(), eq(new TbResourceId(resourceId)))).willReturn(FluentFuture.from(Futures.immediateFuture(resourceDataInfo)));

        aiNode.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        var msg = TbMsg.newMsg()
                .originator(deviceId)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .metaData(TbMsgMetaData.EMPTY)
                .build();

        // WHEN
        aiNode.onMsg(ctxMock, msg);

        // THEN
        var exceptionCaptor = ArgumentCaptor.forClass(Throwable.class);
        then(ctxMock).should().enqueueForTellFailure(any(), exceptionCaptor.capture());
        Throwable actualException = exceptionCaptor.getValue();
        assertThat(actualException.getMessage()).isEqualTo("Missing mediaType in resource descriptor {}");
    }

    @Test
    void givenTemplatedPrompts_whenOnMsg_thenRequestContainsSubstitutedMessages() throws TbNodeException {
        // GIVEN
        config.setModelId(modelId);
        config.setSystemPrompt("Respond with $[responseFormat]");
        config.setUserPrompt("Tell me a joke about ${jokeIdea}");
        config.setResponseFormat(new TbJsonResponseFormat());
        config.setTimeoutSeconds(10);
        config.setForceAck(true);

        aiNode.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        var msg = TbMsg.newMsg()
                .originator(deviceId)
                .data("{\"responseFormat\":\"valid JSON\"}")
                .metaData(new TbMsgMetaData(Map.of("jokeIdea", "JSON")))
                .build();

        var chatResponse = ChatResponse.builder()
                .aiMessage(AiMessage.from("{\"joke\":\"Why did the JSON go to therapy?\",\"punchline\":\"Because it had too many unresolved references!\"}"))
                .build();

        given(aiChatModelServiceMock.sendChatRequestAsync(any(), any())).willReturn(FluentFuture.from(immediateFuture(chatResponse)));

        // WHEN
        aiNode.onMsg(ctxMock, msg);

        // THEN
        then(aiChatModelServiceMock).should().sendChatRequestAsync(any(),
                argThat(actualChatRequest -> {
                    assertThat(actualChatRequest.messages()).hasSize(2);
                    assertThat(actualChatRequest.messages().get(0)).isEqualTo(SystemMessage.from("Respond with valid JSON"));
                    assertThat(actualChatRequest.messages().get(1)).isEqualTo(UserMessage.from("Tell me a joke about JSON"));
                    return true;
                })
        );
    }

    @Test
    void givenNodeTimeoutIsConfigured_whenOnMsg_thenRequestUsesNodeTimeout() throws TbNodeException {
        // GIVEN
        config.setModelId(modelId);
        config.setSystemPrompt("Respond with valid JSON");
        config.setUserPrompt("Tell me a joke");
        config.setResponseFormat(new TbJsonResponseFormat());
        config.setTimeoutSeconds(10);
        config.setForceAck(true);

        aiNode.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        var msg = TbMsg.newMsg()
                .originator(deviceId)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .metaData(TbMsgMetaData.EMPTY)
                .build();

        var chatResponse = ChatResponse.builder()
                .aiMessage(AiMessage.from("{\"type\":\"joke\",\"setup\":\"Why did the scarecrow win an award?\",\"punchline\":\"Because he was outstanding in his field.\"}"))
                .build();

        given(aiChatModelServiceMock.sendChatRequestAsync(any(), any())).willReturn(FluentFuture.from(immediateFuture(chatResponse)));

        // WHEN
        aiNode.onMsg(ctxMock, msg);

        // THEN
        then(aiChatModelServiceMock).should().sendChatRequestAsync(
                argThat(actualChatModelConfig -> {
                    assertThat(actualChatModelConfig.timeoutSeconds()).isEqualTo(config.getTimeoutSeconds());
                    return true;
                }), any()
        );
    }

    @Test
    void givenAnyConfig_whenOnMsg_thenRequestHasRetriesDisabled() throws TbNodeException {
        // GIVEN
        config.setModelId(modelId);
        config.setSystemPrompt("Respond with valid JSON");
        config.setUserPrompt("Tell me a joke");
        config.setResponseFormat(new TbJsonResponseFormat());
        config.setTimeoutSeconds(10);
        config.setForceAck(true);

        aiNode.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        var msg = TbMsg.newMsg()
                .originator(deviceId)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .metaData(TbMsgMetaData.EMPTY)
                .build();

        var chatResponse = ChatResponse.builder()
                .aiMessage(AiMessage.from("{\"type\":\"joke\",\"setup\":\"Why did the scarecrow win an award?\",\"punchline\":\"Because he was outstanding in his field.\"}"))
                .build();

        given(aiChatModelServiceMock.sendChatRequestAsync(any(), any())).willReturn(FluentFuture.from(immediateFuture(chatResponse)));

        // WHEN
        aiNode.onMsg(ctxMock, msg);

        // THEN
        then(aiChatModelServiceMock).should().sendChatRequestAsync(
                argThat(actualChatModelConfig -> {
                    assertThat(actualChatModelConfig.maxRetries()).isZero();
                    return true;
                }), any()
        );
    }

    @Test
    void givenTextResponseFormatAndNonJsonResponse_whenOnMsg_thenWrapsResponseInJsonObject() throws TbNodeException {
        // GIVEN
        config.setModelId(modelId);
        config.setUserPrompt("Tell me a joke about JSON");
        config.setResponseFormat(new TbTextResponseFormat());
        config.setTimeoutSeconds(10);
        config.setForceAck(false);

        aiNode.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        var msg = TbMsg.newMsg()
                .originator(deviceId)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .metaData(TbMsgMetaData.EMPTY)
                .build();

        var chatResponse = ChatResponse.builder()
                .aiMessage(AiMessage.from("""
                        Why did the JSON file break up with the XML file?
                        Because it found someone less complicated and more flexible!"""))
                .build();

        given(aiChatModelServiceMock.sendChatRequestAsync(any(), any())).willReturn(FluentFuture.from(immediateFuture(chatResponse)));

        // WHEN
        aiNode.onMsg(ctxMock, msg);

        // THEN
        then(ctxMock).should().tellSuccess(argThat(
                resultMsg -> resultMsg.getData().equals(JacksonUtil.newObjectNode().put("response", chatResponse.aiMessage().text()).toString()))
        );
    }

    @Test
    void givenModelIsConfigured_whenOnMsg_thenRequestUsesCorrectModelConfig() throws TbNodeException {
        // GIVEN
        config.setModelId(modelId);
        config.setSystemPrompt("Respond with valid JSON");
        config.setUserPrompt("Tell me a joke");
        config.setResponseFormat(new TbJsonResponseFormat());
        config.setTimeoutSeconds(10);
        config.setForceAck(true);

        aiNode.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        var msg = TbMsg.newMsg()
                .originator(deviceId)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .metaData(TbMsgMetaData.EMPTY)
                .build();

        var chatResponse = ChatResponse.builder()
                .aiMessage(AiMessage.from("{\"type\":\"joke\",\"setup\":\"Why did the scarecrow win an award?\",\"punchline\":\"Because he was outstanding in his field.\"}"))
                .build();

        given(aiChatModelServiceMock.sendChatRequestAsync(any(), any())).willReturn(FluentFuture.from(immediateFuture(chatResponse)));

        // WHEN
        aiNode.onMsg(ctxMock, msg);

        // THEN
        then(aiChatModelServiceMock).should().sendChatRequestAsync(
                argThat(actualChatModelConfig -> {
                    assertThat(actualChatModelConfig)
                            .usingRecursiveComparison()
                            .ignoringFields("timeoutSeconds", "maxRetries")
                            .isEqualTo(modelConfig);
                    return true;
                }),
                any()
        );
    }

    @Test
    void givenTextResponseFormat_whenOnMsg_thenRequestResponseFormatIsNull() throws TbNodeException {
        // GIVEN
        config.setModelId(modelId);
        config.setUserPrompt("Tell me a joke");
        config.setResponseFormat(new TbTextResponseFormat());
        config.setTimeoutSeconds(10);
        config.setForceAck(true);

        aiNode.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        var msg = TbMsg.newMsg()
                .originator(deviceId)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .metaData(TbMsgMetaData.EMPTY)
                .build();

        var chatResponse = ChatResponse.builder()
                .aiMessage(AiMessage.from("""
                        Why did the JSON file break up with the XML file?
                        Because it found someone less complicated and more flexible!"""))
                .build();

        given(aiChatModelServiceMock.sendChatRequestAsync(any(), any())).willReturn(FluentFuture.from(immediateFuture(chatResponse)));

        // WHEN
        aiNode.onMsg(ctxMock, msg);

        // THEN
        then(aiChatModelServiceMock).should().sendChatRequestAsync(
                any(),
                argThat(actualChatRequest -> {
                    assertThat(actualChatRequest.responseFormat()).isNull();
                    return true;
                })
        );
    }

    @Test
    void givenJsonResponseFormat_whenOnMsg_thenRequestResponseFormatIsJson() throws TbNodeException {
        // GIVEN
        config.setModelId(modelId);
        config.setUserPrompt("Tell me a joke");
        config.setResponseFormat(new TbJsonResponseFormat());
        config.setTimeoutSeconds(10);
        config.setForceAck(true);

        aiNode.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        var msg = TbMsg.newMsg()
                .originator(deviceId)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .metaData(TbMsgMetaData.EMPTY)
                .build();

        var chatResponse = ChatResponse.builder()
                .aiMessage(AiMessage.from("""
                        Why did the JSON file break up with the XML file?
                        Because it found someone less complicated and more flexible!"""))
                .build();

        given(aiChatModelServiceMock.sendChatRequestAsync(any(), any())).willReturn(FluentFuture.from(immediateFuture(chatResponse)));

        // WHEN
        aiNode.onMsg(ctxMock, msg);

        // THEN
        then(aiChatModelServiceMock).should().sendChatRequestAsync(
                any(),
                argThat(actualChatRequest -> {
                    assertThat(actualChatRequest.responseFormat()).isEqualTo(ResponseFormat.builder().type(ResponseFormatType.JSON).build());
                    return true;
                })
        );
    }

    @Test
    void givenJsonSchemaResponseFormat_whenOnMsg_thenRequestResponseFormatIsJsonWithSchema() throws TbNodeException {
        // GIVEN
        var jsonSchema = """
                {
                    "title": "Joke",
                    "type": "object",
                    "properties": {
                        "joke": {
                            "type": "string"
                        },
                        "punchline": {
                            "type": "string"
                        }
                    },
                    "required": [
                        "joke",
                        "punchline"
                    ]
                }
                """;

        config.setModelId(modelId);
        config.setSystemPrompt("Respond with valid JSON");
        config.setUserPrompt("Tell me a joke");
        config.setResponseFormat(new TbJsonSchemaResponseFormat((ObjectNode) JacksonUtil.toJsonNode(jsonSchema)));
        config.setTimeoutSeconds(10);
        config.setForceAck(true);

        aiNode.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        var msg = TbMsg.newMsg()
                .originator(deviceId)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .metaData(TbMsgMetaData.EMPTY)
                .build();

        var chatResponse = ChatResponse.builder()
                .aiMessage(AiMessage.from("""
                        {
                          "joke": "Why do programmers prefer JSON over XML?",
                          "punchline": "Because it’s less taxing to read!"
                        }"""))
                .build();

        given(aiChatModelServiceMock.sendChatRequestAsync(any(), any())).willReturn(FluentFuture.from(immediateFuture(chatResponse)));

        // WHEN
        aiNode.onMsg(ctxMock, msg);

        // THEN
        var expectedJsonSchema = JsonSchema.builder()
                .name("Joke")
                .rootElement(JsonObjectSchema.builder()
                        .addStringProperty("joke")
                        .addStringProperty("punchline")
                        .required("joke", "punchline")
                        .additionalProperties(true)
                        .build())
                .build();

        then(aiChatModelServiceMock).should().sendChatRequestAsync(
                any(),
                argThat(actualChatRequest -> {
                    assertThat(actualChatRequest.responseFormat()).isEqualTo(ResponseFormat.builder().type(ResponseFormatType.JSON).jsonSchema(expectedJsonSchema).build());
                    return true;
                })
        );
    }

    @Test
    void givenComprehensiveConfig_whenOnMsg_thenProcessesMessageAndTellsSuccessCorrectly() throws TbNodeException {
        // GIVEN
        config.setModelId(modelId);
        config.setSystemPrompt("Respond with valid JSON");
        config.setUserPrompt("Tell me a joke");
        config.setResponseFormat(new TbJsonResponseFormat());
        config.setTimeoutSeconds(10);
        config.setForceAck(false);

        aiNode.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        var msg = TbMsg.newMsg()
                .originator(deviceId)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .metaData(TbMsgMetaData.EMPTY)
                .build();

        var chatResponse = ChatResponse.builder()
                .aiMessage(AiMessage.from("{\"type\":\"joke\",\"setup\":\"Why did the scarecrow win an award?\",\"punchline\":\"Because he was outstanding in his field.\"}"))
                .build();

        given(aiChatModelServiceMock.sendChatRequestAsync(any(), any())).willReturn(FluentFuture.from(immediateFuture(chatResponse)));

        // WHEN
        aiNode.onMsg(ctxMock, msg);

        // THEN
        then(aiChatModelServiceMock).should().sendChatRequestAsync(
                argThat(actualChatModelConfig -> {
                    assertThat(actualChatModelConfig)
                            .usingRecursiveComparison()
                            .ignoringFields("timeoutSeconds", "maxRetries")
                            .isEqualTo(modelConfig);
                    assertThat(actualChatModelConfig.timeoutSeconds()).isEqualTo(config.getTimeoutSeconds());
                    assertThat(actualChatModelConfig.maxRetries()).isEqualTo(0);
                    return true;
                }),
                argThat(actualChatRequest -> {
                    assertThat(actualChatRequest.messages()).hasSize(2);
                    assertThat(actualChatRequest.messages().get(0)).isEqualTo(SystemMessage.from("Respond with valid JSON"));
                    assertThat(actualChatRequest.messages().get(1)).isEqualTo(UserMessage.from("Tell me a joke"));
                    assertThat(actualChatRequest.responseFormat()).isEqualTo(ResponseFormat.builder().type(ResponseFormatType.JSON).build());
                    return true;
                })
        );

        then(ctxMock).should().tellSuccess(argThat(resultMsg ->
                resultMsg.getData().equals(chatResponse.aiMessage().text()) &&
                        resultMsg.getMetaData().equals(msg.getMetaData()) &&
                        resultMsg.getType().equals(msg.getType()) &&
                        resultMsg.getOriginator().equals(msg.getOriginator()))
        );

        then(ctxMock).should(never()).enqueueForTellNext(any(), any(String.class));
        then(ctxMock).should(never()).enqueueForTellFailure(any(), any(Throwable.class));
        then(ctxMock).should(never()).tellNext(any(), any(String.class));
        then(ctxMock).should(never()).tellFailure(any(), any());
    }

    private TbResource buildGeneralResource(byte[] data, String mediaType) {
        TbResource tbResource = new TbResource();
        tbResource.setResourceType(GENERAL);
        GeneralFileDescriptor descriptor = new GeneralFileDescriptor(mediaType);
        tbResource.setDescriptorValue(descriptor);
        tbResource.setData(data);
        return tbResource;
    }

}
