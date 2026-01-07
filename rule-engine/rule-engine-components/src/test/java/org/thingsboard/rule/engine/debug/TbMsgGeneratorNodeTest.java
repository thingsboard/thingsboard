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
package org.thingsboard.rule.engine.debug;

import com.google.common.util.concurrent.Futures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.rule.engine.AbstractRuleNodeUpgradeTest;
import org.thingsboard.rule.engine.api.ScriptEngine;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.data.script.ScriptLanguage;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.PartitionChangeMsg;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
public class TbMsgGeneratorNodeTest extends AbstractRuleNodeUpgradeTest {

    private static final Set<EntityType> supportedEntityTypes = EnumSet.of(EntityType.DEVICE, EntityType.ASSET, EntityType.ENTITY_VIEW,
            EntityType.TENANT, EntityType.CUSTOMER, EntityType.USER, EntityType.DASHBOARD, EntityType.EDGE, EntityType.RULE_NODE);

    private final RuleNodeId RULE_NODE_ID = new RuleNodeId(UUID.fromString("1c649392-1f53-4377-b12f-1ba172611746"));
    private final TenantId TENANT_ID = TenantId.fromUUID(UUID.fromString("4470dfc2-f621-42b2-b82c-b5776d424140"));

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
        executorService = ThingsBoardExecutors.newSingleThreadScheduledExecutor("msg-generator-node-test");
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

    @ParameterizedTest
    @EnumSource(EntityType.class)
    public void givenEntityType_whenInit_thenVerifyException(EntityType entityType) {
        // GIVEN
        config.setOriginatorType(entityType);
        var configuration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));

        // WHEN-THEN
        if (entityType == EntityType.RULE_NODE || entityType == EntityType.TENANT) {
            assertThatNoException().isThrownBy(() -> node.init(ctxMock, configuration));
        } else {
            String errorMsg = supportedEntityTypes.contains(entityType) ? "Originator entity must be selected."
                    : "Originator type '" + entityType + "' is not supported.";
            assertThatThrownBy(() -> node.init(ctxMock, configuration))
                    .isInstanceOf(TbNodeException.class)
                    .hasMessage(errorMsg)
                    .extracting(e -> ((TbNodeException) e).isUnrecoverable())
                    .isEqualTo(true);
        }
    }

    @Test
    public void givenOriginatorEntityTypeIsRuleNode_whenInit_thenVerifyOriginatorId() throws TbNodeException {
        // GIVEN
        config.setOriginatorType(EntityType.RULE_NODE);

        given(ctxMock.getSelfId()).willReturn(RULE_NODE_ID);

        // WHEN
        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        // THEN
        then(ctxMock).should().isLocalEntity(RULE_NODE_ID);
    }

    @Test
    public void givenOriginatorEntityTypeIsTenant_whenInit_thenVerifyOriginatorId() throws TbNodeException {
        // GIVEN
        config.setOriginatorType(EntityType.TENANT);

        given(ctxMock.getTenantId()).willReturn(TENANT_ID);

        // WHEN
        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        // THEN
        then(ctxMock).should().isLocalEntity(TENANT_ID);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ASSET", "DEVICE", "ENTITY_VIEW", "CUSTOMER", "USER", "DASHBOARD", "EDGE"})
    public void givenOriginatorEntityType_whenInit_thenVerifyOriginatorId(String entityTypeStr) throws TbNodeException {
        // GIVEN
        EntityType entityType = EntityType.valueOf(entityTypeStr);
        config.setOriginatorType(entityType);
        String entityIdStr = "5751f55e-089e-4be0-b10a-dd942053dcf0";
        config.setOriginatorId(entityIdStr);
        EntityId entityId = EntityIdFactory.getByTypeAndUuid(entityType, entityIdStr);

        // WHEN
        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        // THEN
        then(ctxMock).should().isLocalEntity(entityId);
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
        TbMsg tickMsg = TbMsg.newMsg()
                .type(TbMsgType.GENERATOR_NODE_SELF_MSG)
                .originator(RULE_NODE_ID)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_STRING)
                .build();
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
        TbMsg firstMsg = TbMsg.newMsg()
                .type(TbMsg.EMPTY_STRING)
                .originator(RULE_NODE_ID)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .build();
        given(ctxMock.newMsg(null, TbMsg.EMPTY_STRING, RULE_NODE_ID, null, TbMsgMetaData.EMPTY, TbMsg.EMPTY_JSON_OBJECT)).willReturn(firstMsg);

        // creation of generated message
        TbMsgMetaData metaData = new TbMsgMetaData(Map.of("data", "40"));
        TbMsg generatedMsg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(RULE_NODE_ID)
                .copyMetaData(metaData)
                .data("{ \"temp\": 42, \"humidity\": 77 }")
                .build();
        given(scriptEngineMock.executeGenerateAsync(any())).willReturn(Futures.immediateFuture(generatedMsg));

        // creation of prev message
        TbMsg prevMsg = TbMsg.newMsg()
                .type(generatedMsg.getType())
                .originator(RULE_NODE_ID)
                .copyMetaData(generatedMsg.getMetaData())
                .data(generatedMsg.getData())
                .build();
        given(ctxMock.newMsg(null, generatedMsg.getType(), RULE_NODE_ID, null, generatedMsg.getMetaData(), generatedMsg.getData())).willReturn(prevMsg);

        // WHEN
        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        awaitTellSelfLatch.await();

        // THEN

        //verify creation of prev message
        then(ctxMock).should(times(1)).newMsg(null, TbMsg.EMPTY_STRING, RULE_NODE_ID, null, TbMsgMetaData.EMPTY, TbMsg.EMPTY_JSON_OBJECT);

        // verify invocation of tellSelf() method
        ArgumentCaptor<TbMsg> actualTickMsg = ArgumentCaptor.forClass(TbMsg.class);
        then(ctxMock).should(times(6)).tellSelf(actualTickMsg.capture(), any(Long.class));
        assertThat(actualTickMsg.getValue()).usingRecursiveComparison().ignoringFields("ctx").isEqualTo(tickMsg);

        // verify invocation of enqueueForTellNext() method
        ArgumentCaptor<TbMsg> actualGeneratedMsg = ArgumentCaptor.forClass(TbMsg.class);
        then(ctxMock).should(times(5)).enqueueForTellNext(actualGeneratedMsg.capture(), eq(TbNodeConnectionType.SUCCESS));
        assertThat(actualGeneratedMsg.getValue()).usingRecursiveComparison().ignoringFields("ctx").isEqualTo(prevMsg);
    }

    @Test
    public void givenOriginatorIsNotLocalEntity_whenOnPartitionChangeMsg_thenDestroy() {
        // GIVEN
        config.setOriginatorType(EntityType.DEVICE);
        config.setOriginatorId("2e8b77f1-ee33-4207-a3d7-556fb16e0151");
        ReflectionTestUtils.setField(node, "initialized", new AtomicBoolean(true));

        given(ctxMock.isLocalEntity(any())).willReturn(false);

        // WHEN
        var partitionChangeMsgMock = mock(PartitionChangeMsg.class);
        node.onPartitionChangeMsg(ctxMock, partitionChangeMsgMock);

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
                // config for version 2 with upgrade from version 1 (originatorId is not selected)
                Arguments.of(1,
                        "{\"msgCount\":0,\"periodInSeconds\":1,\"originatorId\":null,\"originatorType\":\"DEVICE\",\"scriptLang\":\"TBEL\",\"jsScript\":\"var msg = { temp: 42, humidity: 77 };\\nvar metadata = { data: 40 };\\nvar msgType = \\\"POST_TELEMETRY_REQUEST\\\";\\n\\nreturn { msg: msg, metadata: metadata, msgType: msgType };\",\"tbelScript\": \"var msg = { temp: 42, humidity: 77 };\\nvar metadata = { data: 40 };\\nvar msgType = \\\"POST_TELEMETRY_REQUEST\\\";\\n\\nreturn { msg: msg, metadata: metadata, msgType: msgType };\"}",
                        true,
                        "{\"msgCount\":0,\"periodInSeconds\":1,\"originatorId\":null,\"originatorType\":\"RULE_NODE\",\"scriptLang\":\"TBEL\",\"jsScript\":\"var msg = { temp: 42, humidity: 77 };\\nvar metadata = { data: 40 };\\nvar msgType = \\\"POST_TELEMETRY_REQUEST\\\";\\n\\nreturn { msg: msg, metadata: metadata, msgType: msgType };\",\"tbelScript\": \"var msg = { temp: 42, humidity: 77 };\\nvar metadata = { data: 40 };\\nvar msgType = \\\"POST_TELEMETRY_REQUEST\\\";\\n\\nreturn { msg: msg, metadata: metadata, msgType: msgType };\"}"),
                // config for version 2 with upgrade from version 1 (originatorType and originatorId are selected)
                Arguments.of(1,
                        "{\"msgCount\":0,\"periodInSeconds\":1,\"originatorId\":\"92b8e1ce-1b58-4f23-b127-7a0a031b0677\",\"originatorType\":\"DEVICE\",\"scriptLang\":\"TBEL\",\"jsScript\":\"var msg = { temp: 42, humidity: 77 };\\nvar metadata = { data: 40 };\\nvar msgType = \\\"POST_TELEMETRY_REQUEST\\\";\\n\\nreturn { msg: msg, metadata: metadata, msgType: msgType };\",\"tbelScript\": \"var msg = { temp: 42, humidity: 77 };\\nvar metadata = { data: 40 };\\nvar msgType = \\\"POST_TELEMETRY_REQUEST\\\";\\n\\nreturn { msg: msg, metadata: metadata, msgType: msgType };\"}",
                        false,
                        "{\"msgCount\":0,\"periodInSeconds\":1,\"originatorId\":\"92b8e1ce-1b58-4f23-b127-7a0a031b0677\",\"originatorType\":\"DEVICE\",\"scriptLang\":\"TBEL\",\"jsScript\":\"var msg = { temp: 42, humidity: 77 };\\nvar metadata = { data: 40 };\\nvar msgType = \\\"POST_TELEMETRY_REQUEST\\\";\\n\\nreturn { msg: msg, metadata: metadata, msgType: msgType };\",\"tbelScript\": \"var msg = { temp: 42, humidity: 77 };\\nvar metadata = { data: 40 };\\nvar msgType = \\\"POST_TELEMETRY_REQUEST\\\";\\n\\nreturn { msg: msg, metadata: metadata, msgType: msgType };\"}")

        );
    }

    @Override
    protected TbNode getTestNode() {
        return node;
    }

}
