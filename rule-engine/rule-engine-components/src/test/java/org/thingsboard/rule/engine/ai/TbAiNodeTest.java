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

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.FluentFuture;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
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
import org.thingsboard.server.common.data.ai.AiModel;
import org.thingsboard.server.common.data.ai.model.AiModelConfig;
import org.thingsboard.server.common.data.ai.model.chat.AnthropicChatModelConfig;
import org.thingsboard.server.common.data.ai.model.chat.OpenAiChatModelConfig;
import org.thingsboard.server.common.data.ai.provider.AnthropicProviderConfig;
import org.thingsboard.server.common.data.ai.provider.OpenAiProviderConfig;
import org.thingsboard.server.common.data.id.AiModelId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.ai.AiModelService;
import org.thingsboard.server.dao.exception.DataValidationException;

import java.util.Map;
import java.util.Optional;
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

@ExtendWith(MockitoExtension.class)
class TbAiNodeTest {

    @Mock
    TbContext ctxMock;
    @Mock
    AiModelService aiModelServiceMock;
    @Mock
    RuleEngineAiChatModelService aiChatModelServiceMock;

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
                .providerConfig(new OpenAiProviderConfig("test-api-key"))
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
        String tooLongString = "a".repeat(10_001);
        return Stream.of(
                Arguments.of(""),
                Arguments.of("   "),
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
        String longString = "a".repeat(10_000);
        return Stream.of(
                Arguments.of((String) null),
                Arguments.of("a"),
                Arguments.of("Test system prompt"),
                Arguments.of(longString)
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
        String tooLongString = "a".repeat(10_001);
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
        String longString = "a".repeat(10_000);
        return Stream.of(
                Arguments.of("a"),
                Arguments.of("Test user prompt"),
                Arguments.of(longString)
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

}
