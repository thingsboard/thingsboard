/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.rule.engine.debug;

import com.google.common.util.concurrent.Futures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.rule.engine.AbstractRuleNodeUpgradeTest;
import org.thingsboard.rule.engine.api.ScriptEngine;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.data.script.ScriptLanguage;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
public class TbMsgGeneratorNodeTest extends AbstractRuleNodeUpgradeTest {

    private static final RuleNodeId RULE_NODE_ID = new RuleNodeId(UUID.fromString("1c649392-1f53-4377-b12f-1ba172611746"));

    private final ThingsBoardThreadFactory factory = ThingsBoardThreadFactory.forName("msg-generator-node-test");

    private TbMsgGeneratorNode node;
    private TbMsgGeneratorNodeConfiguration config;
    private ScheduledExecutorService executorService;

    @Mock
    private TbContext ctxMock;
    @Mock
    private ScriptEngine scriptEngineMock;

    @BeforeEach
    public void setUp() {
        node = spy(new TbMsgGeneratorNode());
        config = new TbMsgGeneratorNodeConfiguration().defaultConfiguration();
        executorService = Executors.newSingleThreadScheduledExecutor(factory);
    }

    @AfterEach
    public void tearDown() {
        if (executorService != null) {
            executorService.shutdownNow();
        }
        node.destroy();
    }

    @Test
    public void verifyDefaultConfig() {
        assertThat(config.getMsgCount()).isEqualTo(TbMsgGeneratorNodeConfiguration.UNLIMITED_MSG_COUNT);
        assertThat(config.getPeriodInSeconds()).isEqualTo(1);
        assertThat(config.getOriginatorId()).isNull();
        assertThat(config.getOriginatorType()).isEqualTo(EntityType.RULE_NODE);
        assertThat(config.getScriptLang()).isEqualTo(ScriptLanguage.TBEL);
        assertThat(config.getJsScript()).isEqualTo(TbMsgGeneratorNodeConfiguration.DEFAULT_SCRIPT);
        assertThat(config.getTbelScript()).isEqualTo(TbMsgGeneratorNodeConfiguration.DEFAULT_SCRIPT);
    }

    @Test
    public void givenUnsupportedEntityType_whenInit_thenThrowsException() {
        // GIVEN
        config.setOriginatorType(EntityType.NOTIFICATION);

        // WHEN-THEN
        assertThatThrownBy(() -> node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config))))
                .isInstanceOf(TbNodeException.class)
                .hasMessage("Originator type 'NOTIFICATION' is not supported.");
    }

    @ParameterizedTest
    @MethodSource
    public void givenOriginatorEntityType_whenInit_thenVerifyOriginatorId(EntityType entityType,
                                                                          String originatorId,
                                                                          EntityId expectedOriginatorId,
                                                                          Consumer<TbContext> mockCtx) throws TbNodeException {
        // GIVEN
        config.setOriginatorType(entityType);
        config.setOriginatorId(originatorId);

        mockCtx.accept(ctxMock);

        // WHEN
        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        // THEN
        then(ctxMock).should().isLocalEntity(expectedOriginatorId);
    }

    private static Stream<Arguments> givenOriginatorEntityType_whenInit_thenVerifyOriginatorId() {
        return Stream.of(
                Arguments.of(EntityType.RULE_NODE, null, RULE_NODE_ID,
                        (Consumer<TbContext>) ctxMock -> given(ctxMock.getSelfId()).willReturn(RULE_NODE_ID)),
                Arguments.of(EntityType.TENANT, null, TenantId.fromUUID(UUID.fromString("c7f7b865-3e4c-40d3-b333-a7ec2fd871ee")),
                        (Consumer<TbContext>) ctxMock -> given(ctxMock.getTenantId()).willReturn(TenantId.fromUUID(UUID.fromString("c7f7b865-3e4c-40d3-b333-a7ec2fd871ee")))),
                Arguments.of(EntityType.ASSET, "cbb9a3d3-02f1-482b-90ab-2417dcd35f20", new AssetId(UUID.fromString("cbb9a3d3-02f1-482b-90ab-2417dcd35f20")),
                        (Consumer<TbContext>) ctxMock -> given(ctxMock.getQueueName()).willReturn("Main"))
        );
    }

    @Test
    public void givenMsgCountAndDelay_whenInit_thenVerifyInvocationOfOnMsgMethod() throws TbNodeException, InterruptedException {
        // GIVEN
        var awaitTellSelfLatch = new CountDownLatch(5);
        config.setMsgCount(5);

        given(ctxMock.getSelfId()).willReturn(RULE_NODE_ID);
        given(ctxMock.isLocalEntity(any())).willReturn(true);
        given(ctxMock.createScriptEngine(any(), any(), any(), any(), any())).willReturn(scriptEngineMock);

        // creation of tickMsg
        TbMsg tickMsg = TbMsg.newMsg(TbMsgType.GENERATOR_NODE_SELF_MSG, RULE_NODE_ID, TbMsgMetaData.EMPTY, TbMsg.EMPTY_STRING);
        given(ctxMock.newMsg(null, TbMsgType.GENERATOR_NODE_SELF_MSG, RULE_NODE_ID, null, TbMsgMetaData.EMPTY, TbMsg.EMPTY_STRING)).willReturn(tickMsg);

        // invocation of tellSelf() method
        willAnswer(invocationOnMock -> {
            executorService.execute(() -> {
                node.onMsg(ctxMock, invocationOnMock.getArgument(0));
                awaitTellSelfLatch.countDown();
            });
            return null;
        }).given(ctxMock).tellSelf(any(), any(Long.class));

        // creation of first message
        TbMsg firstMsg = TbMsg.newMsg(TbMsg.EMPTY_STRING, RULE_NODE_ID, TbMsgMetaData.EMPTY, TbMsg.EMPTY_JSON_OBJECT);
        given(ctxMock.newMsg(null, TbMsg.EMPTY_STRING, RULE_NODE_ID, null, TbMsgMetaData.EMPTY, TbMsg.EMPTY_JSON_OBJECT)).willReturn(firstMsg);

        // creation of generated message
        TbMsgMetaData metaData = new TbMsgMetaData(Map.of("data", "40"));
        TbMsg generatedMsg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, RULE_NODE_ID, metaData, "{ \"temp\": 42, \"humidity\": 77 }");
        given(scriptEngineMock.executeGenerateAsync(any())).willReturn(Futures.immediateFuture(generatedMsg));

        // creation of prev message
        TbMsg prevMsg = TbMsg.newMsg(generatedMsg.getType(), RULE_NODE_ID, generatedMsg.getMetaData(), generatedMsg.getData());
        given(ctxMock.newMsg(null, generatedMsg.getType(), RULE_NODE_ID, null, generatedMsg.getMetaData(), generatedMsg.getData())).willReturn(prevMsg);

        // WHEN
        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        awaitTellSelfLatch.await();

        // THEN

        // verify invocation of tellSelf() method
        ArgumentCaptor<TbMsg> actualTickMsg = ArgumentCaptor.forClass(TbMsg.class);
        then(ctxMock).should(times(6)).tellSelf(actualTickMsg.capture(), any(Long.class));
        assertThat(actualTickMsg.getValue()).usingRecursiveComparison().ignoringFields("ctx").isEqualTo(tickMsg);

        // verify invocation of enqueueForTellNext() method
        ArgumentCaptor<TbMsg> actualGeneratedMsg = ArgumentCaptor.forClass(TbMsg.class);
        then(ctxMock).should(times(5)).enqueueForTellNext(actualGeneratedMsg.capture(), eq(TbNodeConnectionType.SUCCESS));
        assertThat(actualGeneratedMsg.getValue()).usingRecursiveComparison().ignoringFields("ctx", "ts", "id").isEqualTo(generatedMsg);
    }

    @Test
    public void givenOriginatorIsNotLocalEntity_whenInit_thenDestroy() throws TbNodeException {
        // GIVEN
        config.setOriginatorType(EntityType.DEVICE);
        config.setOriginatorId("2e8b77f1-ee33-4207-a3d7-556fb16e0151");
        ReflectionTestUtils.setField(node, "initialized", new AtomicBoolean(true));

        given(ctxMock.isLocalEntity(any())).willReturn(false);

        // WHEN
        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        // THEN
        then(node).should().destroy();
    }

    // Rule nodes upgrade
    private static Stream<Arguments> givenFromVersionAndConfig_whenUpgrade_thenVerifyHasChangesAndConfig() {
        return Stream.of(
                // default config for version 0
                Arguments.of(0,
                        "{\"msgCount\":0,\"periodInSeconds\":1,\"originatorId\":null,\"originatorType\":null, \"queueName\":null, \"scriptLang\":\"TBEL\",\"jsScript\":\"var msg = { temp: 42, humidity: 77 };\\nvar metadata = { data: 40 };\\nvar msgType = \\\"POST_TELEMETRY_REQUEST\\\";\\n\\nreturn { msg: msg, metadata: metadata, msgType: msgType };\",\"tbelScript\":\"var msg = { temp: 42, humidity: 77 };\\nvar metadata = { data: 40 };\\nvar msgType = \\\"POST_TELEMETRY_REQUEST\\\";\\n\\nreturn { msg: msg, metadata: metadata, msgType: msgType };\"}",
                        true,
                        "{\"msgCount\":0,\"periodInSeconds\":1,\"originatorId\":null,\"originatorType\":\"RULE_NODE\", \"scriptLang\":\"TBEL\",\"jsScript\":\"var msg = { temp: 42, humidity: 77 };\\nvar metadata = { data: 40 };\\nvar msgType = \\\"POST_TELEMETRY_REQUEST\\\";\\n\\nreturn { msg: msg, metadata: metadata, msgType: msgType };\",\"tbelScript\":\"var msg = { temp: 42, humidity: 77 };\\nvar metadata = { data: 40 };\\nvar msgType = \\\"POST_TELEMETRY_REQUEST\\\";\\n\\nreturn { msg: msg, metadata: metadata, msgType: msgType };\"}"),
                // default config for version 0 with queueName
                Arguments.of(0,
                        "{\"msgCount\":0,\"periodInSeconds\":1,\"originatorId\":null,\"originatorType\":null, \"queueName\":\"Main\", \"scriptLang\":\"TBEL\",\"jsScript\":\"var msg = { temp: 42, humidity: 77 };\\nvar metadata = { data: 40 };\\nvar msgType = \\\"POST_TELEMETRY_REQUEST\\\";\\n\\nreturn { msg: msg, metadata: metadata, msgType: msgType };\",\"tbelScript\":\"var msg = { temp: 42, humidity: 77 };\\nvar metadata = { data: 40 };\\nvar msgType = \\\"POST_TELEMETRY_REQUEST\\\";\\n\\nreturn { msg: msg, metadata: metadata, msgType: msgType };\"}",
                        true,
                        "{\"msgCount\":0,\"periodInSeconds\":1,\"originatorId\":null,\"originatorType\":\"RULE_NODE\", \"scriptLang\":\"TBEL\",\"jsScript\":\"var msg = { temp: 42, humidity: 77 };\\nvar metadata = { data: 40 };\\nvar msgType = \\\"POST_TELEMETRY_REQUEST\\\";\\n\\nreturn { msg: msg, metadata: metadata, msgType: msgType };\",\"tbelScript\":\"var msg = { temp: 42, humidity: 77 };\\nvar metadata = { data: 40 };\\nvar msgType = \\\"POST_TELEMETRY_REQUEST\\\";\\n\\nreturn { msg: msg, metadata: metadata, msgType: msgType };\"}"),
                // default config for version 1 with upgrade from version 0
                Arguments.of(0,
                        "{\"msgCount\":0,\"periodInSeconds\":1,\"originatorId\":null,\"originatorType\":null, \"scriptLang\":\"TBEL\",\"jsScript\":\"var msg = { temp: 42, humidity: 77 };\\nvar metadata = { data: 40 };\\nvar msgType = \\\"POST_TELEMETRY_REQUEST\\\";\\n\\nreturn { msg: msg, metadata: metadata, msgType: msgType };\",\"tbelScript\":\"var msg = { temp: 42, humidity: 77 };\\nvar metadata = { data: 40 };\\nvar msgType = \\\"POST_TELEMETRY_REQUEST\\\";\\n\\nreturn { msg: msg, metadata: metadata, msgType: msgType };\"}",
                        true,
                        "{\"msgCount\":0,\"periodInSeconds\":1,\"originatorId\":null,\"originatorType\":\"RULE_NODE\", \"scriptLang\":\"TBEL\",\"jsScript\":\"var msg = { temp: 42, humidity: 77 };\\nvar metadata = { data: 40 };\\nvar msgType = \\\"POST_TELEMETRY_REQUEST\\\";\\n\\nreturn { msg: msg, metadata: metadata, msgType: msgType };\",\"tbelScript\":\"var msg = { temp: 42, humidity: 77 };\\nvar metadata = { data: 40 };\\nvar msgType = \\\"POST_TELEMETRY_REQUEST\\\";\\n\\nreturn { msg: msg, metadata: metadata, msgType: msgType };\"}"),
                // config for version 2 with upgrade from version 1 (originatorType is not selected)
                Arguments.of(1,
                        "{\"msgCount\":0,\"periodInSeconds\":1,\"originatorId\":null,\"originatorType\":null,\"scriptLang\":\"TBEL\",\"jsScript\":\"var msg = { temp: 42, humidity: 77 };\\nvar metadata = { data: 40 };\\nvar msgType = \\\"POST_TELEMETRY_REQUEST\\\";\\n\\nreturn { msg: msg, metadata: metadata, msgType: msgType };\",\"tbelScript\": \"var msg = { temp: 42, humidity: 77 };\\nvar metadata = { data: 40 };\\nvar msgType = \\\"POST_TELEMETRY_REQUEST\\\";\\n\\nreturn { msg: msg, metadata: metadata, msgType: msgType };\"}",
                        true,
                        "{\"msgCount\":0,\"periodInSeconds\":1,\"originatorId\":null,\"originatorType\":\"RULE_NODE\",\"scriptLang\":\"TBEL\",\"jsScript\":\"var msg = { temp: 42, humidity: 77 };\\nvar metadata = { data: 40 };\\nvar msgType = \\\"POST_TELEMETRY_REQUEST\\\";\\n\\nreturn { msg: msg, metadata: metadata, msgType: msgType };\",\"tbelScript\": \"var msg = { temp: 42, humidity: 77 };\\nvar metadata = { data: 40 };\\nvar msgType = \\\"POST_TELEMETRY_REQUEST\\\";\\n\\nreturn { msg: msg, metadata: metadata, msgType: msgType };\"}"),
                // config for version 2 with upgrade from version 1 (originatorType is TENANT)
                Arguments.of(1,
                        "{\"msgCount\":0,\"periodInSeconds\":1,\"originatorId\":\"ae540d15-7ef6-41d4-9176-bf788324a5c3\",\"originatorType\":\"TENANT\",\"scriptLang\":\"TBEL\",\"jsScript\":\"var msg = { temp: 42, humidity: 77 };\\nvar metadata = { data: 40 };\\nvar msgType = \\\"POST_TELEMETRY_REQUEST\\\";\\n\\nreturn { msg: msg, metadata: metadata, msgType: msgType };\",\"tbelScript\": \"var msg = { temp: 42, humidity: 77 };\\nvar metadata = { data: 40 };\\nvar msgType = \\\"POST_TELEMETRY_REQUEST\\\";\\n\\nreturn { msg: msg, metadata: metadata, msgType: msgType };\"}",
                        true,
                        "{\"msgCount\":0,\"periodInSeconds\":1,\"originatorId\":null,\"originatorType\":\"TENANT\",\"scriptLang\":\"TBEL\",\"jsScript\":\"var msg = { temp: 42, humidity: 77 };\\nvar metadata = { data: 40 };\\nvar msgType = \\\"POST_TELEMETRY_REQUEST\\\";\\n\\nreturn { msg: msg, metadata: metadata, msgType: msgType };\",\"tbelScript\": \"var msg = { temp: 42, humidity: 77 };\\nvar metadata = { data: 40 };\\nvar msgType = \\\"POST_TELEMETRY_REQUEST\\\";\\n\\nreturn { msg: msg, metadata: metadata, msgType: msgType };\"}")
        );
    }

    @Override
    protected TbNode getTestNode() {
        return node;
    }

}
