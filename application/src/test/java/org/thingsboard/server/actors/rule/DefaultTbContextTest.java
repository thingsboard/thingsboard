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
package org.thingsboard.server.actors.rule;

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
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.TbActorRef;
import org.thingsboard.server.actors.ruleChain.DefaultTbContext;
import org.thingsboard.server.actors.ruleChain.RuleChainOutputMsg;
import org.thingsboard.server.actors.ruleChain.RuleNodeCtx;
import org.thingsboard.server.actors.ruleChain.RuleNodeToRuleChainTellNextMsg;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.data.rule.DebugStrategy;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileConfiguration;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileData;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.TbMsgProcessingStackItem;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TbMsgCallback;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineMsg;
import org.thingsboard.server.queue.common.SimpleTbQueueCallback;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@SuppressWarnings("ResultOfMethodCallIgnored")
@ExtendWith(MockitoExtension.class)
class DefaultTbContextTest {

    private final String EXCEPTION_MSG = "Some runtime exception!";
    private final RuntimeException EXCEPTION = new RuntimeException(EXCEPTION_MSG);

    private final TenantId TENANT_ID = TenantId.fromUUID(UUID.fromString("c7bf4c85-923c-4688-a4b5-0f8a0feb7cd5"));
    private final RuleNodeId RULE_NODE_ID = new RuleNodeId(UUID.fromString("1ca5e2ef-1309-41d9-bafa-709e9df0e2a6"));
    private final RuleChainId RULE_CHAIN_ID = new RuleChainId(UUID.fromString("b87c4123-f9f2-41a6-9a09-e3a5b6580b11"));

    @Mock
    private ActorSystemContext mainCtxMock;
    @Mock
    private RuleNodeCtx nodeCtxMock;
    @Mock
    private TbActorRef chainActorMock;

    private DefaultTbContext defaultTbContext;

    @BeforeEach
    public void setUp() {
        defaultTbContext = new DefaultTbContext(mainCtxMock, "Test rule chain name", nodeCtxMock);
    }

    @Test
    public void givenDebugStrategyOnlyFailureEvents_whenTellSuccess_thenVerifyDebugOutputNotPersisted() {
        // GIVEN
        var callbackMock = mock(TbMsgCallback.class);
        var msg = getTbMsgWithCallback(callbackMock);
        var ruleNode = new RuleNode(RULE_NODE_ID);
        ruleNode.setRuleChainId(RULE_CHAIN_ID);
        ruleNode.setDebugStrategy(DebugStrategy.ONLY_FAILURE_EVENTS);
        given(nodeCtxMock.getSelf()).willReturn(ruleNode);
        given(nodeCtxMock.getChainActor()).willReturn(chainActorMock);

        // WHEN
        defaultTbContext.tellSuccess(msg);

        // THEN
        then(nodeCtxMock).should().getChainActor();
        then(nodeCtxMock).shouldHaveNoMoreInteractions();
        then(mainCtxMock).shouldHaveNoInteractions();
        checkTellNextCommonLogic(callbackMock, TbNodeConnectionType.SUCCESS, msg);
    }

    @Test
    public void givenDebugStrategyOnlyFailureEventsAndSuccessConnection_whenTellNext_thenVerifyDebugOutputNotPersisted() {
        // GIVEN
        var callbackMock = mock(TbMsgCallback.class);
        var msg = getTbMsgWithCallback(callbackMock);
        var ruleNode = new RuleNode(RULE_NODE_ID);
        ruleNode.setRuleChainId(RULE_CHAIN_ID);
        ruleNode.setDebugStrategy(DebugStrategy.ONLY_FAILURE_EVENTS);
        given(nodeCtxMock.getSelf()).willReturn(ruleNode);
        given(nodeCtxMock.getChainActor()).willReturn(chainActorMock);

        // WHEN
        defaultTbContext.tellNext(msg, TbNodeConnectionType.SUCCESS);

        // THEN
        then(nodeCtxMock).should().getChainActor();
        then(nodeCtxMock).shouldHaveNoMoreInteractions();
        then(mainCtxMock).shouldHaveNoInteractions();
        checkTellNextCommonLogic(callbackMock, TbNodeConnectionType.SUCCESS, msg);
    }

    @MethodSource
    @ParameterizedTest
    void givenDebugStrategyOnlyFailureEventsAndConnections_whenTellNext_thenVerifyDebugOutputPersisted(Set<String> connections) {
        // GIVEN
        var callbackMock = mock(TbMsgCallback.class);
        var msg = getTbMsgWithCallback(callbackMock);
        var ruleNode = new RuleNode(RULE_NODE_ID);
        ruleNode.setRuleChainId(RULE_CHAIN_ID);
        ruleNode.setDebugStrategy(DebugStrategy.ONLY_FAILURE_EVENTS);
        given(nodeCtxMock.getTenantId()).willReturn(TENANT_ID);
        given(nodeCtxMock.getSelf()).willReturn(ruleNode);
        given(nodeCtxMock.getChainActor()).willReturn(chainActorMock);

        // WHEN
        defaultTbContext.tellNext(msg, connections);

        // THEN
        then(nodeCtxMock).should().getChainActor();
        then(nodeCtxMock).shouldHaveNoMoreInteractions();
        then(mainCtxMock).should().persistDebugOutput(TENANT_ID, RULE_NODE_ID, msg, TbNodeConnectionType.FAILURE, null, null);
        then(mainCtxMock).shouldHaveNoMoreInteractions();
        checkTellNextCommonLogic(callbackMock, connections, msg);
    }

    private static Stream<Set<String>> givenDebugStrategyOnlyFailureEventsAndConnections_whenTellNext_thenVerifyDebugOutputPersisted() {
        return Stream.of(
                Collections.singleton(TbNodeConnectionType.FAILURE),
                Set.of(TbNodeConnectionType.FAILURE, TbNodeConnectionType.SUCCESS)
        );
    }

    @MethodSource
    @ParameterizedTest
    void givenDebugStrategyDisabledAndConnections_whenTellNext_thenVerifyDebugOutputNotPersisted(Set<String> connections) {
        // GIVEN
        var callbackMock = mock(TbMsgCallback.class);
        var msg = getTbMsgWithCallback(callbackMock);
        var ruleNode = new RuleNode(RULE_NODE_ID);
        ruleNode.setRuleChainId(RULE_CHAIN_ID);
        ruleNode.setDebugStrategy(DebugStrategy.DISABLED);
        given(nodeCtxMock.getSelf()).willReturn(ruleNode);
        given(nodeCtxMock.getChainActor()).willReturn(chainActorMock);

        // WHEN
        defaultTbContext.tellNext(msg, connections);

        // THEN
        then(nodeCtxMock).should().getChainActor();
        then(nodeCtxMock).shouldHaveNoMoreInteractions();
        then(mainCtxMock).shouldHaveNoInteractions();
        checkTellNextCommonLogic(callbackMock, connections, msg);
    }

    private static Stream<Set<String>> givenDebugStrategyDisabledAndConnections_whenTellNext_thenVerifyDebugOutputNotPersisted() {
        return Stream.of(
                Collections.singleton(TbNodeConnectionType.FAILURE),
                Collections.singleton(TbNodeConnectionType.SUCCESS),
                Set.of(TbNodeConnectionType.FAILURE, TbNodeConnectionType.SUCCESS)
        );
    }

    @MethodSource
    @ParameterizedTest
    void givenDebugStrategyAllEventsAndConnection_whenTellNext_thenVerifyDebugOutputPersisted(String connection) {
        // GIVEN
        var callbackMock = mock(TbMsgCallback.class);
        var msg = getTbMsgWithCallback(callbackMock);
        var ruleNode = new RuleNode(RULE_NODE_ID);
        ruleNode.setRuleChainId(RULE_CHAIN_ID);
        ruleNode.setLastUpdateTs(System.currentTimeMillis());
        ruleNode.setDebugStrategy(DebugStrategy.ALL_EVENTS);
        given(nodeCtxMock.getTenantId()).willReturn(TENANT_ID);
        given(nodeCtxMock.getSelf()).willReturn(ruleNode);
        given(nodeCtxMock.getChainActor()).willReturn(chainActorMock);
        mockGetMaxRuleNodeDebugModeDurationMinutes();

        // WHEN
        defaultTbContext.tellNext(msg, connection);

        // THEN
        then(nodeCtxMock).should().getChainActor();
        then(nodeCtxMock).shouldHaveNoMoreInteractions();
        then(mainCtxMock).should().getTenantProfileCache();
        then(mainCtxMock).should().getMaxRuleNodeDebugModeDurationMinutes();
        then(mainCtxMock).should().persistDebugOutput(TENANT_ID, RULE_NODE_ID, msg, connection, null, null);
        then(mainCtxMock).shouldHaveNoMoreInteractions();
        checkTellNextCommonLogic(callbackMock, connection, msg);
    }

    private static Stream<String> givenDebugStrategyAllEventsAndConnection_whenTellNext_thenVerifyDebugOutputPersisted() {
        return failureAndSuccessConnection();
    }

    @Test
    public void givenDebugStrategyAllEventsAndFailureAndSuccessConnection_whenTellNext_thenVerifyDebugOutputPersistedForAllEvents() {
        // GIVEN
        var callbackMock = mock(TbMsgCallback.class);
        var msg = getTbMsgWithCallback(callbackMock);
        var ruleNode = new RuleNode(RULE_NODE_ID);
        ruleNode.setRuleChainId(RULE_CHAIN_ID);
        ruleNode.setLastUpdateTs(System.currentTimeMillis());
        ruleNode.setDebugStrategy(DebugStrategy.ALL_EVENTS);
        given(nodeCtxMock.getTenantId()).willReturn(TENANT_ID);
        given(nodeCtxMock.getSelf()).willReturn(ruleNode);
        given(nodeCtxMock.getChainActor()).willReturn(chainActorMock);
        mockGetMaxRuleNodeDebugModeDurationMinutes();

        // WHEN
        Set<String> connections = failureAndSuccessConnection().collect(Collectors.toSet());
        defaultTbContext.tellNext(msg, connections);

        // THEN
        then(nodeCtxMock).should().getChainActor();
        then(nodeCtxMock).shouldHaveNoMoreInteractions();
        then(mainCtxMock).should().getTenantProfileCache();
        then(mainCtxMock).should().getMaxRuleNodeDebugModeDurationMinutes();
        var nodeConnectionsCaptor = ArgumentCaptor.forClass(String.class);
        int wantedNumberOfInvocations = connections.size();
        then(mainCtxMock).should(times(wantedNumberOfInvocations)).persistDebugOutput(eq(TENANT_ID), eq(RULE_NODE_ID), eq(msg), nodeConnectionsCaptor.capture(), nullable(Throwable.class), nullable(String.class));
        then(mainCtxMock).shouldHaveNoMoreInteractions();
        assertThat(nodeConnectionsCaptor.getAllValues()).hasSize(wantedNumberOfInvocations);
        assertThat(nodeConnectionsCaptor.getAllValues()).containsExactlyInAnyOrderElementsOf(connections);
        checkTellNextCommonLogic(callbackMock, connections, msg);
    }

    private static Stream<String> failureAndSuccessConnection() {
        return Stream.of(TbNodeConnectionType.FAILURE, TbNodeConnectionType.SUCCESS);
    }

    @Test
    public void givenDebugStrategyOnlyFailureEventsAndFailureConnection_whenOutput_thenVerifyDebugOutputPersisted() {
        // GIVEN
        var msgMock = mock(TbMsg.class);
        var ruleNode = new RuleNode(RULE_NODE_ID);
        ruleNode.setRuleChainId(RULE_CHAIN_ID);
        ruleNode.setDebugStrategy(DebugStrategy.ONLY_FAILURE_EVENTS);
        given(msgMock.popFormStack()).willReturn(new TbMsgProcessingStackItem(RULE_CHAIN_ID, RULE_NODE_ID));
        given(nodeCtxMock.getTenantId()).willReturn(TENANT_ID);
        given(nodeCtxMock.getSelf()).willReturn(ruleNode);
        given(nodeCtxMock.getChainActor()).willReturn(chainActorMock);

        // WHEN
        defaultTbContext.output(msgMock, TbNodeConnectionType.FAILURE);

        // THEN
        checkOutputCommonLogic(msgMock, TbNodeConnectionType.FAILURE);
        then(mainCtxMock).should().persistDebugOutput(TENANT_ID, RULE_NODE_ID, msgMock, TbNodeConnectionType.FAILURE, null, null);
        then(mainCtxMock).shouldHaveNoMoreInteractions();
        then(nodeCtxMock).shouldHaveNoMoreInteractions();
    }

    @Test
    public void givenDebugStrategyOnlyFailureEventsAndSuccessConnection_whenOutput_thenVerifyDebugOutputNotPersisted() {
        // GIVEN
        var msgMock = mock(TbMsg.class);
        var ruleNode = new RuleNode(RULE_NODE_ID);
        ruleNode.setRuleChainId(RULE_CHAIN_ID);
        ruleNode.setDebugStrategy(DebugStrategy.ONLY_FAILURE_EVENTS);
        given(msgMock.popFormStack()).willReturn(new TbMsgProcessingStackItem(RULE_CHAIN_ID, RULE_NODE_ID));
        given(nodeCtxMock.getSelf()).willReturn(ruleNode);
        given(nodeCtxMock.getChainActor()).willReturn(chainActorMock);

        // WHEN
        defaultTbContext.output(msgMock, TbNodeConnectionType.SUCCESS);

        // THEN
        checkOutputCommonLogic(msgMock, TbNodeConnectionType.SUCCESS);
        then(mainCtxMock).shouldHaveNoMoreInteractions();
        then(nodeCtxMock).shouldHaveNoMoreInteractions();
    }

    @ParameterizedTest
    @ValueSource(strings = {TbNodeConnectionType.SUCCESS, TbNodeConnectionType.FAILURE})
    void givenDebugStrategyDisabled_whenOutput_thenVerifyDebugOutputNotPersisted(String nodeConnection) {
        // GIVEN
        var msgMock = mock(TbMsg.class);
        var ruleNode = new RuleNode(RULE_NODE_ID);
        ruleNode.setRuleChainId(RULE_CHAIN_ID);
        ruleNode.setDebugStrategy(DebugStrategy.DISABLED);
        given(msgMock.popFormStack()).willReturn(new TbMsgProcessingStackItem(RULE_CHAIN_ID, RULE_NODE_ID));
        given(nodeCtxMock.getSelf()).willReturn(ruleNode);
        given(nodeCtxMock.getChainActor()).willReturn(chainActorMock);

        // WHEN
        defaultTbContext.output(msgMock, nodeConnection);

        // THEN
        checkOutputCommonLogic(msgMock, nodeConnection);
        then(mainCtxMock).shouldHaveNoMoreInteractions();
        then(nodeCtxMock).shouldHaveNoMoreInteractions();
    }

    @ParameterizedTest
    @ValueSource(strings = {TbNodeConnectionType.SUCCESS, TbNodeConnectionType.FAILURE})
    void givenDebugStrategyAllEvents_whenOutput_thenVerifyDebugOutputPersisted(String nodeConnection) {
        // GIVEN
        var msgMock = mock(TbMsg.class);
        var ruleNode = new RuleNode(RULE_NODE_ID);
        ruleNode.setRuleChainId(RULE_CHAIN_ID);
        ruleNode.setDebugStrategy(DebugStrategy.ALL_EVENTS);
        given(msgMock.popFormStack()).willReturn(new TbMsgProcessingStackItem(RULE_CHAIN_ID, RULE_NODE_ID));
        given(nodeCtxMock.getTenantId()).willReturn(TENANT_ID);
        given(nodeCtxMock.getSelf()).willReturn(ruleNode);
        given(nodeCtxMock.getChainActor()).willReturn(chainActorMock);
        mockGetMaxRuleNodeDebugModeDurationMinutes();

        // WHEN
        defaultTbContext.output(msgMock, nodeConnection);

        // THEN
        checkOutputCommonLogic(msgMock, nodeConnection);
        then(mainCtxMock).should().getTenantProfileCache();
        then(mainCtxMock).should().getMaxRuleNodeDebugModeDurationMinutes();
        then(mainCtxMock).should().persistDebugOutput(TENANT_ID, RULE_NODE_ID, msgMock, nodeConnection, null, null);
        then(mainCtxMock).shouldHaveNoMoreInteractions();
        then(nodeCtxMock).shouldHaveNoMoreInteractions();
    }

    @Test
    public void givenEmptyStack_whenOutput_thenVerifyMsgAck() {
        // GIVEN
        var msgMock = mock(TbMsg.class);
        var ruleNode = new RuleNode(RULE_NODE_ID);
        ruleNode.setRuleChainId(RULE_CHAIN_ID);
        ruleNode.setDebugStrategy(DebugStrategy.DISABLED);
        given(msgMock.popFormStack()).willReturn(null);
        TbMsgCallback callbackMock = mock(TbMsgCallback.class);
        given(msgMock.getCallback()).willReturn(callbackMock);
        given(nodeCtxMock.getSelf()).willReturn(ruleNode);

        // WHEN
        defaultTbContext.output(msgMock, TbNodeConnectionType.SUCCESS);

        // THEN
        then(msgMock).should().popFormStack();
        then(callbackMock).should().onProcessingEnd(RULE_NODE_ID);
        then(callbackMock).should().onSuccess();
        then(nodeCtxMock).should(never()).getChainActor();
    }

    @Test
    public void givenEmptyStackAndDebugStrategyAllEvents_whenOutput_thenVerifyMsgAckAndDebugOutputPersisted() {
        // GIVEN
        var msgMock = mock(TbMsg.class);
        var ruleNode = new RuleNode(RULE_NODE_ID);
        ruleNode.setRuleChainId(RULE_CHAIN_ID);
        ruleNode.setDebugStrategy(DebugStrategy.ALL_EVENTS);
        ruleNode.setLastUpdateTs(System.currentTimeMillis());
        given(msgMock.popFormStack()).willReturn(null);
        TbMsgCallback callbackMock = mock(TbMsgCallback.class);
        given(msgMock.getCallback()).willReturn(callbackMock);
        given(nodeCtxMock.getSelf()).willReturn(ruleNode);
        given(nodeCtxMock.getTenantId()).willReturn(TENANT_ID);
        mockGetMaxRuleNodeDebugModeDurationMinutes();

        // WHEN
        defaultTbContext.output(msgMock, TbNodeConnectionType.SUCCESS);

        // THEN
        then(msgMock).should().popFormStack();
        then(callbackMock).should().onProcessingEnd(RULE_NODE_ID);
        then(callbackMock).should().onSuccess();
        then(nodeCtxMock).should(never()).getChainActor();
        then(mainCtxMock).should().persistDebugOutput(TENANT_ID, RULE_NODE_ID, msgMock, TbNodeConnectionType.ACK, null, null);
    }

    @Test
    public void givenDebugStrategyOnlyFailureEvents_whenEnqueueForTellFailure_thenVerifyDebugOutputPersisted() {
        // GIVEN
        var msg = getTbMsgWithQueueName();
        var tpi = new TopicPartitionInfo(DataConstants.MAIN_QUEUE_TOPIC, TENANT_ID, 0, true);
        var ruleNode = new RuleNode(RULE_NODE_ID);
        ruleNode.setRuleChainId(RULE_CHAIN_ID);
        ruleNode.setDebugStrategy(DebugStrategy.ONLY_FAILURE_EVENTS);
        var tbClusterServiceMock = mock(TbClusterService.class);

        given(nodeCtxMock.getTenantId()).willReturn(TENANT_ID);
        given(nodeCtxMock.getSelf()).willReturn(ruleNode);
        given(mainCtxMock.resolve(any(ServiceType.class), anyString(), any(TenantId.class), any(EntityId.class))).willReturn(tpi);
        given(mainCtxMock.getClusterService()).willReturn(tbClusterServiceMock);

        // WHEN
        defaultTbContext.enqueueForTellFailure(msg, EXCEPTION);

        // THEN
        then(mainCtxMock).should().resolve(ServiceType.TB_RULE_ENGINE, DataConstants.MAIN_QUEUE_NAME, TENANT_ID, TENANT_ID);
        TbMsg expectedTbMsg = TbMsg.newMsg(msg, msg.getQueueName(), RULE_CHAIN_ID, RULE_NODE_ID);
        checkEnqueueForTellFailurePushMsgToRuleEngine(tbClusterServiceMock, tpi, expectedTbMsg);
        ArgumentCaptor<TbMsg> tbMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        then(mainCtxMock).should().persistDebugOutput(eq(TENANT_ID), eq(RULE_NODE_ID), tbMsgCaptor.capture(), eq(TbNodeConnectionType.FAILURE), isNull(), eq(EXCEPTION_MSG));
        TbMsg actualTbMsg = tbMsgCaptor.getValue();
        assertThat(actualTbMsg).usingRecursiveComparison()
                .ignoringFields("id", "ctx")
                .isEqualTo(expectedTbMsg);
        then(mainCtxMock).should().getClusterService();
        then(mainCtxMock).shouldHaveNoMoreInteractions();
        then(tbClusterServiceMock).shouldHaveNoMoreInteractions();
    }

    @Test
    public void givenDebugStrategyDisabled_whenEnqueueForTellFailure_thenVerifyDebugOutputNotPersisted() {
        // GIVEN
        var msg = getTbMsgWithQueueName();
        var tpi = new TopicPartitionInfo(DataConstants.MAIN_QUEUE_TOPIC, TENANT_ID, 0, true);
        var ruleNode = new RuleNode(RULE_NODE_ID);
        ruleNode.setRuleChainId(RULE_CHAIN_ID);
        ruleNode.setDebugStrategy(DebugStrategy.DISABLED);
        var tbClusterServiceMock = mock(TbClusterService.class);

        given(nodeCtxMock.getTenantId()).willReturn(TENANT_ID);
        given(nodeCtxMock.getSelf()).willReturn(ruleNode);
        given(mainCtxMock.resolve(any(ServiceType.class), anyString(), any(TenantId.class), any(EntityId.class))).willReturn(tpi);
        given(mainCtxMock.getClusterService()).willReturn(tbClusterServiceMock);

        // WHEN
        defaultTbContext.enqueueForTellFailure(msg, EXCEPTION);

        // THEN
        then(mainCtxMock).should().resolve(ServiceType.TB_RULE_ENGINE, DataConstants.MAIN_QUEUE_NAME, TENANT_ID, TENANT_ID);
        TbMsg expectedTbMsg = TbMsg.newMsg(msg, msg.getQueueName(), RULE_CHAIN_ID, RULE_NODE_ID);
        checkEnqueueForTellFailurePushMsgToRuleEngine(tbClusterServiceMock, tpi, expectedTbMsg);
        then(mainCtxMock).should().getClusterService();
        then(mainCtxMock).shouldHaveNoMoreInteractions();
        then(tbClusterServiceMock).shouldHaveNoMoreInteractions();
    }

    @Test
    public void givenDebugStrategyAllEvents_whenEnqueueForTellFailure_thenVerifyDebugOutputPersisted() {
        // GIVEN
        var msg = getTbMsgWithQueueName();
        var tpi = new TopicPartitionInfo(DataConstants.MAIN_QUEUE_TOPIC, TENANT_ID, 0, true);
        var ruleNode = new RuleNode(RULE_NODE_ID);
        ruleNode.setRuleChainId(RULE_CHAIN_ID);
        ruleNode.setDebugStrategy(DebugStrategy.ALL_EVENTS);
        ruleNode.setLastUpdateTs(System.currentTimeMillis());
        var tbClusterServiceMock = mock(TbClusterService.class);

        given(nodeCtxMock.getTenantId()).willReturn(TENANT_ID);
        given(nodeCtxMock.getSelf()).willReturn(ruleNode);
        given(mainCtxMock.resolve(any(ServiceType.class), anyString(), any(TenantId.class), any(EntityId.class))).willReturn(tpi);
        given(mainCtxMock.getClusterService()).willReturn(tbClusterServiceMock);
        mockGetMaxRuleNodeDebugModeDurationMinutes();

        // WHEN
        defaultTbContext.enqueueForTellFailure(msg, EXCEPTION);

        // THEN
        then(mainCtxMock).should().resolve(ServiceType.TB_RULE_ENGINE, DataConstants.MAIN_QUEUE_NAME, TENANT_ID, TENANT_ID);
        TbMsg expectedTbMsg = TbMsg.newMsg(msg, msg.getQueueName(), RULE_CHAIN_ID, RULE_NODE_ID);
        checkEnqueueForTellFailurePushMsgToRuleEngine(tbClusterServiceMock, tpi, expectedTbMsg);
        ArgumentCaptor<TbMsg> tbMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        then(mainCtxMock).should().persistDebugOutput(eq(TENANT_ID), eq(RULE_NODE_ID), tbMsgCaptor.capture(), eq(TbNodeConnectionType.FAILURE), isNull(), eq(EXCEPTION_MSG));
        TbMsg actualTbMsg = tbMsgCaptor.getValue();
        assertThat(actualTbMsg).usingRecursiveComparison()
                .ignoringFields("id", "ctx")
                .isEqualTo(expectedTbMsg);
        then(mainCtxMock).should().getClusterService();
        then(mainCtxMock).should().getTenantProfileCache();
        then(mainCtxMock).should().getMaxRuleNodeDebugModeDurationMinutes();
        then(mainCtxMock).shouldHaveNoMoreInteractions();
        then(tbClusterServiceMock).shouldHaveNoMoreInteractions();
    }

    @Test
    public void givenInvalidMsg_whenEnqueueForTellFailure_thenDoNothing() {
        // GIVEN
        var msgMock = mock(TbMsg.class);
        var tpi = new TopicPartitionInfo(DataConstants.MAIN_QUEUE_TOPIC, TENANT_ID, 0, true);

        given(msgMock.getOriginator()).willReturn(TENANT_ID);
        given(msgMock.getQueueName()).willReturn(DataConstants.MAIN_QUEUE_NAME);
        given(msgMock.isValid()).willReturn(false);
        given(nodeCtxMock.getTenantId()).willReturn(TENANT_ID);
        given(mainCtxMock.resolve(any(ServiceType.class), anyString(), any(TenantId.class), any(EntityId.class))).willReturn(tpi);

        // WHEN
        defaultTbContext.enqueueForTellFailure(msgMock, EXCEPTION);

        // THEN
        then(msgMock).should(times(2)).getQueueName();
        then(msgMock).should().getOriginator();
        then(msgMock).should().isValid();
        then(msgMock).shouldHaveNoMoreInteractions();

        then(mainCtxMock).should().resolve(ServiceType.TB_RULE_ENGINE, DataConstants.MAIN_QUEUE_NAME, TENANT_ID, TENANT_ID);
        then(mainCtxMock).shouldHaveNoMoreInteractions();

        then(nodeCtxMock).should(times(2)).getTenantId();
        then(nodeCtxMock).shouldHaveNoMoreInteractions();
        then(chainActorMock).shouldHaveNoInteractions();
    }

    @MethodSource
    @ParameterizedTest
    void givenDebugStrategyOptions_whenEnqueueForTellNext_thenVerifyDebugOutputPersistedOnlyForAllEventsDebugStrategy(DebugStrategy debugStrategy, String connectionType) {
        // GIVEN
        var msg = getTbMsgWithQueueName();
        var tpi = new TopicPartitionInfo(DataConstants.MAIN_QUEUE_TOPIC, TENANT_ID, 0, true);
        var ruleNode = new RuleNode(RULE_NODE_ID);
        ruleNode.setRuleChainId(RULE_CHAIN_ID);
        ruleNode.setDebugStrategy(debugStrategy);
        ruleNode.setLastUpdateTs(System.currentTimeMillis());
        var tbClusterServiceMock = mock(TbClusterService.class);

        given(nodeCtxMock.getTenantId()).willReturn(TENANT_ID);
        given(nodeCtxMock.getSelf()).willReturn(ruleNode);
        given(mainCtxMock.resolve(any(ServiceType.class), anyString(), any(TenantId.class), any(EntityId.class))).willReturn(tpi);
        given(mainCtxMock.getClusterService()).willReturn(tbClusterServiceMock);
        if (DebugStrategy.ALL_EVENTS.equals(debugStrategy)) {
            mockGetMaxRuleNodeDebugModeDurationMinutes();
        }

        // WHEN
        defaultTbContext.enqueueForTellNext(msg, connectionType);

        // THEN
        then(mainCtxMock).should().resolve(ServiceType.TB_RULE_ENGINE, DataConstants.MAIN_QUEUE_NAME, TENANT_ID, TENANT_ID);
        TbMsg expectedTbMsg = TbMsg.newMsg(msg, msg.getQueueName(), RULE_CHAIN_ID, RULE_NODE_ID);

        ArgumentCaptor<ToRuleEngineMsg> toRuleEngineMsgCaptor = ArgumentCaptor.forClass(ToRuleEngineMsg.class);
        ArgumentCaptor<SimpleTbQueueCallback> simpleTbQueueCallbackCaptor = ArgumentCaptor.forClass(SimpleTbQueueCallback.class);
        then(tbClusterServiceMock).should().pushMsgToRuleEngine(eq(tpi), notNull(UUID.class), toRuleEngineMsgCaptor.capture(), simpleTbQueueCallbackCaptor.capture());

        ToRuleEngineMsg actualToRuleEngineMsg = toRuleEngineMsgCaptor.getValue();
        assertThat(actualToRuleEngineMsg).usingRecursiveComparison()
                .ignoringFields("tbMsg_")
                .isEqualTo(ToRuleEngineMsg.newBuilder()
                        .setTenantIdMSB(TENANT_ID.getId().getMostSignificantBits())
                        .setTenantIdLSB(TENANT_ID.getId().getLeastSignificantBits())
                        .setTbMsg(TbMsg.toByteString(expectedTbMsg))
                        .addAllRelationTypes(List.of(connectionType)).build());

        var simpleTbQueueCallback = simpleTbQueueCallbackCaptor.getValue();
        assertThat(simpleTbQueueCallback).isNotNull();
        simpleTbQueueCallback.onSuccess(null);

        if (DebugStrategy.ALL_EVENTS.equals(debugStrategy)) {
            then(mainCtxMock).should().getTenantProfileCache();
            then(mainCtxMock).should().getMaxRuleNodeDebugModeDurationMinutes();
            ArgumentCaptor<TbMsg> tbMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
            then(mainCtxMock).should().persistDebugOutput(eq(TENANT_ID), eq(RULE_NODE_ID), tbMsgCaptor.capture(), eq(connectionType), isNull(), isNull());
            TbMsg actualTbMsg = tbMsgCaptor.getValue();
            assertThat(actualTbMsg).usingRecursiveComparison()
                    .ignoringFields("id", "ctx")
                    .isEqualTo(expectedTbMsg);
        }
        then(mainCtxMock).should().getClusterService();
        then(mainCtxMock).shouldHaveNoMoreInteractions();
        then(tbClusterServiceMock).shouldHaveNoMoreInteractions();
    }

    @MethodSource
    @ParameterizedTest
    void givenDebugStrategyOptions_whenEnqueue_thenVerifyDebugOutputPersistedOnlyForAllEventsDebugStrategy(DebugStrategy debugStrategy) {
        // GIVEN
        var msg = getTbMsgWithQueueName();
        var tpi = new TopicPartitionInfo(DataConstants.MAIN_QUEUE_TOPIC, TENANT_ID, 0, true);
        var ruleNode = new RuleNode(RULE_NODE_ID);
        ruleNode.setQueueName(DataConstants.MAIN_QUEUE_NAME);
        ruleNode.setRuleChainId(RULE_CHAIN_ID);
        ruleNode.setDebugStrategy(debugStrategy);
        ruleNode.setLastUpdateTs(System.currentTimeMillis());
        var tbClusterServiceMock = mock(TbClusterService.class);

        given(nodeCtxMock.getTenantId()).willReturn(TENANT_ID);
        given(nodeCtxMock.getSelf()).willReturn(ruleNode);
        given(mainCtxMock.resolve(any(ServiceType.class), anyString(), any(TenantId.class), any(EntityId.class))).willReturn(tpi);
        given(mainCtxMock.getClusterService()).willReturn(tbClusterServiceMock);
        if (DebugStrategy.ALL_EVENTS.equals(debugStrategy)) {
            mockGetMaxRuleNodeDebugModeDurationMinutes();
        }

        Consumer<Throwable> onFailure = mock(Consumer.class);
        Runnable onSuccess = mock(Runnable.class);

        // WHEN
        defaultTbContext.enqueue(msg, onSuccess, onFailure);

        // THEN
        then(mainCtxMock).should().resolve(ServiceType.TB_RULE_ENGINE, DataConstants.MAIN_QUEUE_NAME, TENANT_ID, TENANT_ID);
        TbMsg expectedTbMsg = TbMsg.newMsg(msg, msg.getQueueName(), RULE_CHAIN_ID, RULE_NODE_ID);

        ArgumentCaptor<ToRuleEngineMsg> toRuleEngineMsgCaptor = ArgumentCaptor.forClass(ToRuleEngineMsg.class);
        ArgumentCaptor<SimpleTbQueueCallback> simpleTbQueueCallbackCaptor = ArgumentCaptor.forClass(SimpleTbQueueCallback.class);
        then(tbClusterServiceMock).should().pushMsgToRuleEngine(eq(tpi), notNull(UUID.class), toRuleEngineMsgCaptor.capture(), simpleTbQueueCallbackCaptor.capture());

        ToRuleEngineMsg actualToRuleEngineMsg = toRuleEngineMsgCaptor.getValue();
        assertThat(actualToRuleEngineMsg).usingRecursiveComparison()
                .ignoringFields("tbMsg_")
                .isEqualTo(ToRuleEngineMsg.newBuilder()
                        .setTenantIdMSB(TENANT_ID.getId().getMostSignificantBits())
                        .setTenantIdLSB(TENANT_ID.getId().getLeastSignificantBits())
                        .setTbMsg(TbMsg.toByteString(expectedTbMsg))
                        .build());

        var simpleTbQueueCallback = simpleTbQueueCallbackCaptor.getValue();
        assertThat(simpleTbQueueCallback).isNotNull();
        simpleTbQueueCallback.onSuccess(null);

        if (DebugStrategy.ALL_EVENTS.equals(debugStrategy)) {
            then(mainCtxMock).should().getTenantProfileCache();
            then(mainCtxMock).should().getMaxRuleNodeDebugModeDurationMinutes();
            then(mainCtxMock).should().persistDebugOutput(eq(TENANT_ID), eq(RULE_NODE_ID), eq(msg), eq(TbNodeConnectionType.TO_ROOT_RULE_CHAIN), nullable(Throwable.class), nullable(String.class));
        }
        then(mainCtxMock).should().getClusterService();
        then(mainCtxMock).shouldHaveNoMoreInteractions();
        then(tbClusterServiceMock).shouldHaveNoMoreInteractions();
    }

    @Test
    public void givenDebugStrategyOnlyFailures_whenTellFailure_thenVerifyDebugOutputPersisted() {
        // GIVEN
        var msg = getTbMsg();
        var ruleNode = new RuleNode(RULE_NODE_ID);
        ruleNode.setRuleChainId(RULE_CHAIN_ID);
        ruleNode.setDebugStrategy(DebugStrategy.ONLY_FAILURE_EVENTS);
        given(nodeCtxMock.getTenantId()).willReturn(TENANT_ID);
        given(nodeCtxMock.getSelf()).willReturn(ruleNode);
        given(nodeCtxMock.getChainActor()).willReturn(chainActorMock);

        // WHEN
        defaultTbContext.tellFailure(msg, EXCEPTION);

        // THEN
        var expectedRuleNodeToRuleChainTellNextMsg = new RuleNodeToRuleChainTellNextMsg(
                RULE_CHAIN_ID,
                RULE_NODE_ID,
                Collections.singleton(TbNodeConnectionType.FAILURE),
                msg,
                EXCEPTION_MSG
        );
        then(chainActorMock).should().tell(expectedRuleNodeToRuleChainTellNextMsg);
        then(chainActorMock).shouldHaveNoMoreInteractions();
        then(nodeCtxMock).should().getChainActor();
        then(mainCtxMock).should().persistDebugOutput(TENANT_ID, RULE_NODE_ID, msg, TbNodeConnectionType.FAILURE, EXCEPTION, null);
        then(mainCtxMock).shouldHaveNoMoreInteractions();
        then(nodeCtxMock).shouldHaveNoMoreInteractions();
    }

    @Test
    public void givenDebugStrategyDisabled_whenTellFailure_thenVerifyDebugOutputNotPersisted() {
        // GIVEN
        var msg = getTbMsg();
        var ruleNode = new RuleNode(RULE_NODE_ID);
        ruleNode.setRuleChainId(RULE_CHAIN_ID);
        ruleNode.setDebugStrategy(DebugStrategy.DISABLED);
        given(nodeCtxMock.getSelf()).willReturn(ruleNode);
        given(nodeCtxMock.getChainActor()).willReturn(chainActorMock);

        // WHEN
        defaultTbContext.tellFailure(msg, EXCEPTION);

        // THEN
        var expectedRuleNodeToRuleChainTellNextMsg = new RuleNodeToRuleChainTellNextMsg(
                RULE_CHAIN_ID,
                RULE_NODE_ID,
                Collections.singleton(TbNodeConnectionType.FAILURE),
                msg,
                EXCEPTION_MSG
        );
        then(chainActorMock).should().tell(expectedRuleNodeToRuleChainTellNextMsg);
        then(chainActorMock).shouldHaveNoMoreInteractions();
        then(nodeCtxMock).should().getChainActor();
        then(mainCtxMock).shouldHaveNoInteractions();
        then(nodeCtxMock).shouldHaveNoMoreInteractions();
    }

    @Test
    public void givenDebugStrategyAllEvents_whenTellFailure_thenVerifyDebugOutputPersisted() {
        // GIVEN
        var msg = getTbMsg();
        var ruleNode = new RuleNode(RULE_NODE_ID);
        ruleNode.setRuleChainId(RULE_CHAIN_ID);
        ruleNode.setDebugStrategy(DebugStrategy.ALL_EVENTS);
        ruleNode.setLastUpdateTs(System.currentTimeMillis());
        given(nodeCtxMock.getTenantId()).willReturn(TENANT_ID);
        given(nodeCtxMock.getSelf()).willReturn(ruleNode);
        given(nodeCtxMock.getChainActor()).willReturn(chainActorMock);
        mockGetMaxRuleNodeDebugModeDurationMinutes();

        // WHEN
        defaultTbContext.tellFailure(msg, EXCEPTION);

        // THEN
        var expectedRuleNodeToRuleChainTellNextMsg = new RuleNodeToRuleChainTellNextMsg(
                RULE_CHAIN_ID,
                RULE_NODE_ID,
                Collections.singleton(TbNodeConnectionType.FAILURE),
                msg,
                EXCEPTION_MSG
        );
        then(chainActorMock).should().tell(expectedRuleNodeToRuleChainTellNextMsg);
        then(chainActorMock).shouldHaveNoMoreInteractions();
        then(nodeCtxMock).should().getChainActor();
        then(mainCtxMock).should().persistDebugOutput(TENANT_ID, RULE_NODE_ID, msg, TbNodeConnectionType.FAILURE, EXCEPTION, null);
        then(mainCtxMock).should().getTenantProfileCache();
        then(mainCtxMock).should().getMaxRuleNodeDebugModeDurationMinutes();
        then(mainCtxMock).shouldHaveNoMoreInteractions();
        then(nodeCtxMock).shouldHaveNoMoreInteractions();
    }

    private void checkTellNextCommonLogic(TbMsgCallback callbackMock, String nodeConnection, TbMsg msg) {
        checkTellNextCommonLogic(callbackMock, Collections.singleton(nodeConnection), msg);
    }

    private void checkTellNextCommonLogic(TbMsgCallback callbackMock, Set<String> nodeConnections, TbMsg msg) {
        then(callbackMock).should().onProcessingEnd(RULE_NODE_ID);
        then(callbackMock).shouldHaveNoMoreInteractions();
        var expectedRuleNodeToRuleChainTellNextMsg = new RuleNodeToRuleChainTellNextMsg(
                RULE_CHAIN_ID,
                RULE_NODE_ID,
                nodeConnections,
                msg,
                null);
        then(chainActorMock).should().tell(expectedRuleNodeToRuleChainTellNextMsg);
        then(chainActorMock).shouldHaveNoMoreInteractions();
    }

    private void checkOutputCommonLogic(TbMsg msg, String nodeConnection) {
        then(msg).should().popFormStack();
        var expectedRuleChainOutputMsg = new RuleChainOutputMsg(
                RULE_CHAIN_ID,
                RULE_NODE_ID,
                nodeConnection,
                msg);
        then(chainActorMock).should().tell(expectedRuleChainOutputMsg);
        then(chainActorMock).shouldHaveNoMoreInteractions();
        then(nodeCtxMock).should().getChainActor();
    }

    private void checkEnqueueForTellFailurePushMsgToRuleEngine(TbClusterService tbClusterService, TopicPartitionInfo tpi, TbMsg expectedTbMsg) {
        ArgumentCaptor<ToRuleEngineMsg> toRuleEngineMsgCaptor = ArgumentCaptor.forClass(ToRuleEngineMsg.class);
        ArgumentCaptor<SimpleTbQueueCallback> simpleTbQueueCallbackCaptor = ArgumentCaptor.forClass(SimpleTbQueueCallback.class);
        then(tbClusterService).should().pushMsgToRuleEngine(eq(tpi), notNull(UUID.class), toRuleEngineMsgCaptor.capture(), simpleTbQueueCallbackCaptor.capture());

        ToRuleEngineMsg actualToRuleEngineMsg = toRuleEngineMsgCaptor.getValue();
        assertThat(actualToRuleEngineMsg).usingRecursiveComparison()
                .ignoringFields("tbMsg_")
                .isEqualTo(ToRuleEngineMsg.newBuilder()
                        .setTenantIdMSB(TENANT_ID.getId().getMostSignificantBits())
                        .setTenantIdLSB(TENANT_ID.getId().getLeastSignificantBits())
                        .setTbMsg(TbMsg.toByteString(expectedTbMsg))
                        .setFailureMessage(EXCEPTION_MSG)
                        .addAllRelationTypes(List.of(TbNodeConnectionType.FAILURE)).build());

        var simpleTbQueueCallback = simpleTbQueueCallbackCaptor.getValue();
        assertThat(simpleTbQueueCallback).isNotNull();
        simpleTbQueueCallback.onSuccess(null);
    }

    private static Stream<Arguments> givenDebugStrategyOptions_whenEnqueueForTellNext_thenVerifyDebugOutputPersistedOnlyForAllEventsDebugStrategy() {
        return Stream.of(
                Arguments.of(DebugStrategy.ALL_EVENTS, TbNodeConnectionType.OTHER),
                Arguments.of(DebugStrategy.ONLY_FAILURE_EVENTS, TbNodeConnectionType.TRUE),
                Arguments.of(DebugStrategy.DISABLED, TbNodeConnectionType.FALSE)
        );
    }

    private static Stream<Arguments> givenDebugStrategyOptions_whenEnqueue_thenVerifyDebugOutputPersistedOnlyForAllEventsDebugStrategy() {
        return Stream.of(
                Arguments.of(DebugStrategy.ALL_EVENTS),
                Arguments.of(DebugStrategy.ONLY_FAILURE_EVENTS),
                Arguments.of(DebugStrategy.DISABLED)
        );
    }

    private TbMsg getTbMsgWithCallback(TbMsgCallback callback) {
        return TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, TENANT_ID, TbMsgMetaData.EMPTY, TbMsg.EMPTY_STRING, callback);
    }

    private TbMsg getTbMsgWithQueueName() {
        return TbMsg.newMsg(DataConstants.MAIN_QUEUE_NAME, TbMsgType.POST_TELEMETRY_REQUEST, TENANT_ID, TbMsgMetaData.EMPTY, TbMsg.EMPTY_STRING);
    }

    private TbMsg getTbMsg() {
        return TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, TENANT_ID, TbMsgMetaData.EMPTY, TbMsg.EMPTY_STRING);
    }

    private void mockGetMaxRuleNodeDebugModeDurationMinutes() {
        var tbTenantProfileCacheMock = mock(TbTenantProfileCache.class);
        var tenantProfileMock = mock(TenantProfile.class);
        var tenantProfileDataMock = mock(TenantProfileData.class);
        var tenantProfileConfigurationMock = mock(TenantProfileConfiguration.class);

        given(mainCtxMock.getTenantProfileCache()).willReturn(tbTenantProfileCacheMock);
        given(tbTenantProfileCacheMock.get(TENANT_ID)).willReturn(tenantProfileMock);
        given(tenantProfileMock.getProfileData()).willReturn(tenantProfileDataMock);
        given(tenantProfileDataMock.getConfiguration()).willReturn(tenantProfileConfigurationMock);
        given(tenantProfileConfigurationMock.getMaxRuleNodeDebugModeDurationMinutes(anyInt())).willReturn(15);
    }

}
