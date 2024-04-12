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
package org.thingsboard.rule.engine.action;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.Futures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ListeningExecutor;
import org.thingsboard.rule.engine.TestDbCallbackExecutor;
import org.thingsboard.rule.engine.api.RuleEngineAlarmService;
import org.thingsboard.rule.engine.api.ScriptEngine;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmApiCallResult;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.data.script.ScriptLanguage;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class TbClearAlarmNodeTest {

    @Mock
    TbContext ctxMock;
    @Mock
    RuleEngineAlarmService alarmServiceMock;
    @Mock
    ScriptEngine alarmDetailsScriptMock;
    @Mock
    TbMsg alarmActionMsgMock;

    @Captor
    ArgumentCaptor<Runnable> successCaptor;

    @Spy
    TbClearAlarmNode nodeSpy;
    TbClearAlarmNodeConfiguration config;

    final TenantId tenantId = TenantId.fromUUID(Uuids.timeBased());
    final EntityId msgOriginator = new DeviceId(Uuids.timeBased());
    final AlarmId msgAlarmOriginator = new AlarmId(Uuids.timeBased());
    TbMsgMetaData metadata;

    ListeningExecutor dbExecutor;

    @BeforeEach
    void before() {
        config = new TbClearAlarmNodeConfiguration();
        metadata = new TbMsgMetaData();
        dbExecutor = new TestDbCallbackExecutor();
    }

    @Test
    @DisplayName("When defaultConfiguration() is called, then correct values are set.")
    void whenDefaultConfiguration_thenShouldSetCorrectValues() {
        // GIVEN-WHEN
        config = config.defaultConfiguration();

        // THEN
        assertThat(config.getAlarmType()).isEqualTo("General Alarm");
        assertThat(config.getScriptLang()).isEqualTo(ScriptLanguage.TBEL);
        assertThat(config.getAlarmDetailsBuildJs()).isEqualTo("""
                \
                var details = {};
                if (metadata.prevAlarmDetails) {
                    details = JSON.parse(metadata.prevAlarmDetails);
                    //remove prevAlarmDetails from metadata
                    delete metadata.prevAlarmDetails;
                    //now metadata is the same as it comes IN this rule node
                }


                return details;""");
        assertThat(config.getAlarmDetailsBuildTbel()).isEqualTo("""
                \
                var details = {};
                if (metadata.prevAlarmDetails != null) {
                    details = JSON.parse(metadata.prevAlarmDetails);
                    //remove prevAlarmDetails from metadata
                    metadata.remove('prevAlarmDetails');
                    //now metadata is the same as it comes IN this rule node
                }


                return details;""");
    }

    @Test
    @DisplayName(
            "Given message originator type is ALARM and alarm does not exist, " +
                    "when onMsg(), " +
                    "then tell next 'False' with original message."
    )
    void whenMsgOriginatorIsAlarmAndAlarmDoesNotExist_thenShouldTellFalseWithOriginalMsg() throws Exception {
        // GIVEN

        // node configuration
        config = config.defaultConfiguration();

        String alarmType = config.getAlarmType();
        ScriptLanguage scriptLang = config.getScriptLang();
        String alarmDetailsScript = config.getAlarmDetailsBuildTbel();
        JsonNode alarmDetails = JacksonUtil.newObjectNode().put("alarmDetailsProperty", "alarmDetailsValue");
        long clearTs = 500L;

        var alarmId = new AlarmId(Uuids.timeBased());
        var clearedAlarm = Alarm.builder()
                .type(alarmType)
                .tenantId(tenantId)
                .originator(new DeviceId(Uuids.timeBased()))
                .severity(AlarmSeverity.WARNING)
                .cleared(true)
                .clearTs(clearTs)
                .startTs(100L)
                .endTs(200L)
                .details(alarmDetails)
                .build();
        clearedAlarm.setId(alarmId);

        metadata.putValue("metadataKey", "metadataValue");
        var msg = TbMsg.newMsg(TbMsgType.ENTITY_UPDATED, msgAlarmOriginator, metadata, JacksonUtil.toString(clearedAlarm));

        // mocks
        given(ctxMock.getTenantId()).willReturn(tenantId);
        given(ctxMock.getAlarmService()).willReturn(alarmServiceMock);
        given(ctxMock.getDbCallbackExecutor()).willReturn(dbExecutor);
        given(ctxMock.createScriptEngine(scriptLang, alarmDetailsScript)).willReturn(alarmDetailsScriptMock);

        given(alarmServiceMock.findAlarmById(tenantId, msgAlarmOriginator)).willReturn(null);

        // node initialization
        nodeSpy.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        // WHEN
        nodeSpy.onMsg(ctxMock, msg);

        // THEN
        then(alarmServiceMock).should(never()).clearAlarm(any(), any(), anyLong(), any());

        then(ctxMock).should().tellNext(msg, TbNodeConnectionType.FALSE);
        then(ctxMock).should(never()).enqueue(any(), any(), any());
        then(ctxMock).should(never()).tellNext(any(), eq("Created"));
        then(ctxMock).should(never()).tellNext(any(), eq("Updated"));
        then(ctxMock).should(never()).tellNext(any(), eq("Cleared"));
        then(ctxMock).should(never()).tellSuccess(any());
        then(ctxMock).should(never()).tellFailure(any(), any());
    }

    @Test
    @DisplayName(
            "Given message originator type is ALARM and already cleared alarm exists, " +
                    "when onMsg(), " +
                    "then tell next 'False' with original message."
    )
    void whenMsgOriginatorIsAlarmAndClearedAlarmExists_thenShouldTellFalseWithOriginalMsg() throws Exception {
        // GIVEN

        // node configuration
        config = config.defaultConfiguration();

        String alarmType = config.getAlarmType();
        ScriptLanguage scriptLang = config.getScriptLang();
        String alarmDetailsScript = config.getAlarmDetailsBuildTbel();
        JsonNode alarmDetails = JacksonUtil.newObjectNode().put("alarmDetailsProperty", "alarmDetailsValue");
        long clearTs = 500L;

        var alarmId = new AlarmId(Uuids.timeBased());
        var clearedAlarm = Alarm.builder()
                .type(alarmType)
                .tenantId(tenantId)
                .originator(new DeviceId(Uuids.timeBased()))
                .severity(AlarmSeverity.WARNING)
                .cleared(true)
                .clearTs(clearTs)
                .startTs(100L)
                .endTs(200L)
                .details(alarmDetails)
                .build();
        clearedAlarm.setId(alarmId);

        metadata.putValue("metadataKey", "metadataValue");
        var msg = TbMsg.newMsg(TbMsgType.ENTITY_UPDATED, msgAlarmOriginator, metadata, JacksonUtil.toString(clearedAlarm));

        // mocks
        given(ctxMock.getTenantId()).willReturn(tenantId);
        given(ctxMock.getAlarmService()).willReturn(alarmServiceMock);
        given(ctxMock.getDbCallbackExecutor()).willReturn(dbExecutor);
        given(ctxMock.createScriptEngine(scriptLang, alarmDetailsScript)).willReturn(alarmDetailsScriptMock);

        given(alarmServiceMock.findAlarmById(tenantId, msgAlarmOriginator)).willReturn(clearedAlarm);

        // node initialization
        nodeSpy.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        // WHEN
        nodeSpy.onMsg(ctxMock, msg);

        // THEN
        then(alarmServiceMock).should(never()).clearAlarm(any(), any(), anyLong(), any());

        then(ctxMock).should().tellNext(msg, TbNodeConnectionType.FALSE);
        then(ctxMock).should(never()).enqueue(any(), any(), any());
        then(ctxMock).should(never()).tellNext(any(), eq("Created"));
        then(ctxMock).should(never()).tellNext(any(), eq("Updated"));
        then(ctxMock).should(never()).tellNext(any(), eq("Cleared"));
        then(ctxMock).should(never()).tellSuccess(any());
        then(ctxMock).should(never()).tellFailure(any(), any());
    }

    @Test
    @DisplayName(
            "Given message originator type is ALARM and active alarm exists, " +
                    "when onMsg(), " +
                    "then should clear active alarm, update alarm details, enqueue ALARM_CLEAR message, tell next 'Cleared' with cleared alarm in message body and 'isClearedAlarm = true' in metadata"
    )
    void whenMsgOriginatorIsAlarmAndActiveAlarmExists_thenShouldCorrectlyClear() throws Exception {
        // GIVEN

        // node configuration
        config = config.defaultConfiguration();

        String alarmType = "High Temperature";
        ScriptLanguage scriptLang = config.getScriptLang();
        String alarmDetailsScript = config.getAlarmDetailsBuildTbel();
        JsonNode oldAlarmDetails = JacksonUtil.newObjectNode().put("oldAlarmDetailsProperty", "oldAlarmDetailsValue");
        JsonNode newAlarmDetails = JacksonUtil.newObjectNode().put("newAlarmDetailsProperty", "newAlarmDetailsValue");
        long clearTs = 500L;

        var alarmId = new AlarmId(Uuids.timeBased());
        var activeAlarm = Alarm.builder()
                .type(alarmType)
                .tenantId(tenantId)
                .originator(new DeviceId(Uuids.timeBased()))
                .severity(AlarmSeverity.WARNING)
                .startTs(100L)
                .endTs(200L)
                .details(oldAlarmDetails)
                .build();
        activeAlarm.setId(alarmId);

        metadata.putValue("metadataKey", "metadataValue");
        var msg = TbMsg.newMsg(TbMsgType.ENTITY_UPDATED, msgAlarmOriginator, metadata, JacksonUtil.toString(activeAlarm));

        var ruleNodeSelfId = new RuleNodeId(Uuids.timeBased());

        // mocks
        given(ctxMock.getTenantId()).willReturn(tenantId);
        given(ctxMock.getAlarmService()).willReturn(alarmServiceMock);
        given(ctxMock.getDbCallbackExecutor()).willReturn(dbExecutor);
        given(ctxMock.getSelfId()).willReturn(ruleNodeSelfId);
        doReturn(clearTs).when(nodeSpy).currentTimeMillis();
        given(ctxMock.createScriptEngine(scriptLang, alarmDetailsScript)).willReturn(alarmDetailsScriptMock);
        given(alarmDetailsScriptMock.executeJsonAsync(any())).willReturn(Futures.immediateFuture(newAlarmDetails));

        var expectedAlarm = new Alarm(activeAlarm);
        expectedAlarm.setCleared(true);
        expectedAlarm.setDetails(newAlarmDetails);

        var expectedAlarmInfo = new AlarmInfo(expectedAlarm);

        given(alarmServiceMock.findAlarmById(tenantId, msgAlarmOriginator)).willReturn(activeAlarm);
        given(alarmServiceMock.clearAlarm(tenantId, alarmId, clearTs, newAlarmDetails))
                .willReturn(AlarmApiCallResult.builder()
                        .successful(true)
                        .cleared(true)
                        .alarm(expectedAlarmInfo)
                        .build());
        given(ctxMock.alarmActionMsg(expectedAlarmInfo, ruleNodeSelfId, TbMsgType.ALARM_CLEAR)).willReturn(alarmActionMsgMock);
        given(ctxMock.transformMsg(any(TbMsg.class), any(TbMsgType.class), any(EntityId.class), any(TbMsgMetaData.class), anyString()))
                .willAnswer(answer -> TbMsg.transformMsg(
                        answer.getArgument(0, TbMsg.class),
                        answer.getArgument(1, TbMsgType.class),
                        answer.getArgument(2, EntityId.class),
                        answer.getArgument(3, TbMsgMetaData.class),
                        answer.getArgument(4, String.class))
                );

        // node initialization
        nodeSpy.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        // WHEN
        nodeSpy.onMsg(ctxMock, msg);

        // THEN

        // verify alarm details script evaluation
        then(ctxMock).should().logJsEvalRequest();
        var dummyMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        then(alarmDetailsScriptMock).should().executeJsonAsync(dummyMsgCaptor.capture());
        TbMsg actualDummyMsg = dummyMsgCaptor.getValue();
        assertThat(actualDummyMsg.getType()).isEqualTo(msg.getType());
        assertThat(actualDummyMsg.getData()).isEqualTo(msg.getData());
        assertThat(actualDummyMsg.getMetaData().getData()).containsEntry(TbAbstractAlarmNode.PREV_ALARM_DETAILS, JacksonUtil.toString(oldAlarmDetails));
        then(ctxMock).should().logJsEvalResponse();
        then(ctxMock).should(never()).logJsEvalFailure();

        // verify we called clearAlarm() with correct parameters
        then(alarmServiceMock).should().clearAlarm(tenantId, alarmId, clearTs, newAlarmDetails);

        // verify that we created a correct alarm action message and enqueued it
        then(ctxMock).should().alarmActionMsg(expectedAlarmInfo, ruleNodeSelfId, TbMsgType.ALARM_CLEAR);
        then(ctxMock).should().enqueue(eq(alarmActionMsgMock), successCaptor.capture(), any());

        // run success captor to emulate successful enqueueing and to trigger further processing on the success path
        successCaptor.getValue().run();

        // capture and verify an outgoing message
        var outgoingMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        then(ctxMock).should().tellNext(outgoingMsgCaptor.capture(), eq("Cleared"));
        var actualOutgoingMsg = outgoingMsgCaptor.getValue();
        assertThat(actualOutgoingMsg.getType()).isEqualTo(TbMsgType.ALARM.name());
        assertThat(actualOutgoingMsg.getOriginator()).isEqualTo(msgAlarmOriginator);
        assertThat(actualOutgoingMsg.getData()).isEqualTo(JacksonUtil.toString(expectedAlarmInfo));

        Map<String, String> actualOutgoingMsgMetadataContent = actualOutgoingMsg.getMetaData().getData();
        assertThat(actualOutgoingMsgMetadataContent).containsAllEntriesOf(metadata.getData());
        assertThat(actualOutgoingMsgMetadataContent).containsEntry(DataConstants.IS_CLEARED_ALARM, Boolean.TRUE.toString());
        assertThat(actualOutgoingMsgMetadataContent).size().isEqualTo(metadata.getData().size() + 1);

        // verify wrong processing paths were not taken
        then(ctxMock).should(never()).tellNext(any(), eq(TbNodeConnectionType.FALSE));
        then(ctxMock).should(never()).tellNext(any(), eq("Created"));
        then(ctxMock).should(never()).tellNext(any(), eq("Updated"));
        then(ctxMock).should(never()).tellSuccess(any());
        then(ctxMock).should(never()).tellFailure(any(), any());
    }

    @Test
    @DisplayName(
            "Given message originator type is not ALARM and alarm does not exist, " +
                    "when onMsg(), " +
                    "then tell next 'False' with original message."
    )
    void whenMsgOriginatorNotAlarmAndAlarmDoesNotExist_thenShouldTellFalseWithOriginalMsg() throws Exception {
        // GIVEN

        // node configuration
        config = config.defaultConfiguration();

        String alarmType = config.getAlarmType();
        ScriptLanguage scriptLang = config.getScriptLang();
        String alarmDetailsScript = config.getAlarmDetailsBuildTbel();

        metadata.putValue("metadataKey", "metadataValue");
        var msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, msgOriginator, metadata, "{\"temperature\": 50}");

        // mocks
        given(ctxMock.getTenantId()).willReturn(tenantId);
        given(ctxMock.getAlarmService()).willReturn(alarmServiceMock);
        given(ctxMock.getDbCallbackExecutor()).willReturn(dbExecutor);
        given(ctxMock.createScriptEngine(scriptLang, alarmDetailsScript)).willReturn(alarmDetailsScriptMock);

        given(alarmServiceMock.findLatestActiveByOriginatorAndType(tenantId, msgOriginator, alarmType)).willReturn(null);

        // node initialization
        nodeSpy.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        // WHEN
        nodeSpy.onMsg(ctxMock, msg);

        // THEN
        then(alarmServiceMock).should(never()).clearAlarm(any(), any(), anyLong(), any());

        then(ctxMock).should().tellNext(msg, TbNodeConnectionType.FALSE);
        then(ctxMock).should(never()).enqueue(any(), any(), any());
        then(ctxMock).should(never()).tellNext(any(), eq("Created"));
        then(ctxMock).should(never()).tellNext(any(), eq("Updated"));
        then(ctxMock).should(never()).tellNext(any(), eq("Cleared"));
        then(ctxMock).should(never()).tellSuccess(any());
        then(ctxMock).should(never()).tellFailure(any(), any());
    }

    @Test
    @DisplayName(
            "Given message originator type is not ALARM and already cleared alarm exists, " +
                    "when onMsg(), " +
                    "then tell next 'False' with original message."
    )
    void whenMsgOriginatorNotAlarmAndClearedAlarmExists_thenShouldTellFalseWithOriginalMsg() throws Exception {
        // GIVEN

        // node configuration
        config = config.defaultConfiguration();

        String alarmType = config.getAlarmType();
        ScriptLanguage scriptLang = config.getScriptLang();
        String alarmDetailsScript = config.getAlarmDetailsBuildTbel();
        JsonNode alarmDetails = JacksonUtil.newObjectNode().put("alarmDetailsProperty", "alarmDetailsValue");
        long clearTs = 500L;

        metadata.putValue("metadataKey", "metadataValue");
        var msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, msgOriginator, metadata, "{\"temperature\": 50}");

        // mocks
        given(ctxMock.getTenantId()).willReturn(tenantId);
        given(ctxMock.getAlarmService()).willReturn(alarmServiceMock);
        given(ctxMock.getDbCallbackExecutor()).willReturn(dbExecutor);
        given(ctxMock.createScriptEngine(scriptLang, alarmDetailsScript)).willReturn(alarmDetailsScriptMock);

        var alarmId = new AlarmId(Uuids.timeBased());
        var clearedAlarm = Alarm.builder()
                .type(alarmType)
                .tenantId(tenantId)
                .originator(msgOriginator)
                .severity(AlarmSeverity.WARNING)
                .cleared(true)
                .clearTs(clearTs)
                .startTs(100L)
                .endTs(200L)
                .details(alarmDetails)
                .build();
        clearedAlarm.setId(alarmId);

        given(alarmServiceMock.findLatestActiveByOriginatorAndType(tenantId, msgOriginator, alarmType)).willReturn(clearedAlarm);

        // node initialization
        nodeSpy.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        // WHEN
        nodeSpy.onMsg(ctxMock, msg);

        // THEN
        then(alarmServiceMock).should(never()).clearAlarm(any(), any(), anyLong(), any());

        then(ctxMock).should().tellNext(msg, TbNodeConnectionType.FALSE);
        then(ctxMock).should(never()).enqueue(any(), any(), any());
        then(ctxMock).should(never()).tellNext(any(), eq("Created"));
        then(ctxMock).should(never()).tellNext(any(), eq("Updated"));
        then(ctxMock).should(never()).tellNext(any(), eq("Cleared"));
        then(ctxMock).should(never()).tellSuccess(any());
        then(ctxMock).should(never()).tellFailure(any(), any());
    }

    @Test
    @DisplayName(
            "Given message originator type is not ALARM, active alarm exists and alarm type is using a message body pattern, " +
                    "when onMsg(), " +
                    "then should clear active alarm, update alarm details, enqueue ALARM_CLEAR message, tell next 'Cleared' with cleared alarm in message body and 'isClearedAlarm = true' in metadata"
    )
    void whenMsgOriginatorNotAlarmAndActiveAlarmExistsAndAlarmTypeIsMsgBodyPattern_thenShouldCorrectlyClear() throws Exception {
        // GIVEN

        // node configuration
        config = config.defaultConfiguration();
        config.setAlarmType("$[alarmType]");

        String alarmType = "High Temperature";
        ScriptLanguage scriptLang = config.getScriptLang();
        String alarmDetailsScript = config.getAlarmDetailsBuildTbel();
        JsonNode oldAlarmDetails = JacksonUtil.newObjectNode().put("oldAlarmDetailsProperty", "oldAlarmDetailsValue");
        JsonNode newAlarmDetails = JacksonUtil.newObjectNode().put("newAlarmDetailsProperty", "newAlarmDetailsValue");
        long clearTs = 500L;

        metadata.putValue("metadataKey", "metadataValue");
        var msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, msgOriginator, metadata, "{\"temperature\": 50, \"alarmType\": \"" + alarmType + "\"}");

        var ruleNodeSelfId = new RuleNodeId(Uuids.timeBased());

        // mocks
        given(ctxMock.getTenantId()).willReturn(tenantId);
        given(ctxMock.getAlarmService()).willReturn(alarmServiceMock);
        given(ctxMock.getDbCallbackExecutor()).willReturn(dbExecutor);
        given(ctxMock.getSelfId()).willReturn(ruleNodeSelfId);
        doReturn(clearTs).when(nodeSpy).currentTimeMillis();
        given(ctxMock.createScriptEngine(scriptLang, alarmDetailsScript)).willReturn(alarmDetailsScriptMock);
        given(alarmDetailsScriptMock.executeJsonAsync(any())).willReturn(Futures.immediateFuture(newAlarmDetails));

        var alarmId = new AlarmId(Uuids.timeBased());
        var activeAlarm = Alarm.builder()
                .type(alarmType)
                .tenantId(tenantId)
                .originator(msgOriginator)
                .severity(AlarmSeverity.WARNING)
                .startTs(100L)
                .endTs(200L)
                .details(oldAlarmDetails)
                .build();
        activeAlarm.setId(alarmId);

        var expectedAlarm = new Alarm(activeAlarm);
        expectedAlarm.setCleared(true);
        expectedAlarm.setDetails(newAlarmDetails);

        var expectedAlarmInfo = new AlarmInfo(expectedAlarm);

        given(alarmServiceMock.findLatestActiveByOriginatorAndType(tenantId, msgOriginator, alarmType)).willReturn(activeAlarm);
        given(alarmServiceMock.clearAlarm(tenantId, alarmId, clearTs, newAlarmDetails))
                .willReturn(AlarmApiCallResult.builder()
                        .successful(true)
                        .cleared(true)
                        .alarm(expectedAlarmInfo)
                        .build());
        given(ctxMock.alarmActionMsg(expectedAlarmInfo, ruleNodeSelfId, TbMsgType.ALARM_CLEAR)).willReturn(alarmActionMsgMock);
        given(ctxMock.transformMsg(any(TbMsg.class), any(TbMsgType.class), any(EntityId.class), any(TbMsgMetaData.class), anyString()))
                .willAnswer(answer -> TbMsg.transformMsg(
                        answer.getArgument(0, TbMsg.class),
                        answer.getArgument(1, TbMsgType.class),
                        answer.getArgument(2, EntityId.class),
                        answer.getArgument(3, TbMsgMetaData.class),
                        answer.getArgument(4, String.class))
                );

        // node initialization
        nodeSpy.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        // WHEN
        nodeSpy.onMsg(ctxMock, msg);

        // THEN

        // verify alarm details script evaluation
        then(ctxMock).should().logJsEvalRequest();
        var dummyMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        then(alarmDetailsScriptMock).should().executeJsonAsync(dummyMsgCaptor.capture());
        TbMsg actualDummyMsg = dummyMsgCaptor.getValue();
        assertThat(actualDummyMsg.getType()).isEqualTo(msg.getType());
        assertThat(actualDummyMsg.getData()).isEqualTo(msg.getData());
        assertThat(actualDummyMsg.getMetaData().getData()).containsEntry(TbAbstractAlarmNode.PREV_ALARM_DETAILS, JacksonUtil.toString(oldAlarmDetails));
        then(ctxMock).should().logJsEvalResponse();
        then(ctxMock).should(never()).logJsEvalFailure();

        // verify we called clearAlarm() with correct parameters
        then(alarmServiceMock).should().clearAlarm(tenantId, alarmId, clearTs, newAlarmDetails);

        // verify that we created a correct alarm action message and enqueued it
        then(ctxMock).should().alarmActionMsg(expectedAlarmInfo, ruleNodeSelfId, TbMsgType.ALARM_CLEAR);
        then(ctxMock).should().enqueue(eq(alarmActionMsgMock), successCaptor.capture(), any());

        // run success captor to emulate successful enqueueing and to trigger further processing on the success path
        successCaptor.getValue().run();

        // capture and verify an outgoing message
        var outgoingMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        then(ctxMock).should().tellNext(outgoingMsgCaptor.capture(), eq("Cleared"));
        var actualOutgoingMsg = outgoingMsgCaptor.getValue();
        assertThat(actualOutgoingMsg.getType()).isEqualTo(TbMsgType.ALARM.name());
        assertThat(actualOutgoingMsg.getOriginator()).isEqualTo(msgOriginator);
        assertThat(actualOutgoingMsg.getData()).isEqualTo(JacksonUtil.toString(expectedAlarmInfo));

        Map<String, String> actualOutgoingMsgMetadataContent = actualOutgoingMsg.getMetaData().getData();
        assertThat(actualOutgoingMsgMetadataContent).containsAllEntriesOf(metadata.getData());
        assertThat(actualOutgoingMsgMetadataContent).containsEntry(DataConstants.IS_CLEARED_ALARM, Boolean.TRUE.toString());
        assertThat(actualOutgoingMsgMetadataContent).size().isEqualTo(metadata.getData().size() + 1);

        // verify wrong processing paths were not taken
        then(ctxMock).should(never()).tellNext(any(), eq(TbNodeConnectionType.FALSE));
        then(ctxMock).should(never()).tellNext(any(), eq("Created"));
        then(ctxMock).should(never()).tellNext(any(), eq("Updated"));
        then(ctxMock).should(never()).tellSuccess(any());
        then(ctxMock).should(never()).tellFailure(any(), any());
    }

    @Test
    @DisplayName(
            "Given message originator type is not ALARM, active alarm exists and alarm type is using a metadata pattern, " +
                    "when onMsg(), " +
                    "then should clear active alarm, update alarm details, enqueue ALARM_CLEAR message, tell next 'Cleared' with cleared alarm in message body and 'isClearedAlarm = true' in metadata"
    )
    void whenMsgOriginatorNotAlarmAndActiveAlarmExistsAndAlarmTypeIsMetadataPattern_thenShouldCorrectlyClear() throws Exception {
        // GIVEN

        // node configuration
        config = config.defaultConfiguration();
        config.setAlarmType("${alarmType}");

        String alarmType = "High Temperature";
        ScriptLanguage scriptLang = config.getScriptLang();
        String alarmDetailsScript = config.getAlarmDetailsBuildTbel();
        JsonNode oldAlarmDetails = JacksonUtil.newObjectNode().put("oldAlarmDetailsProperty", "oldAlarmDetailsValue");
        JsonNode newAlarmDetails = JacksonUtil.newObjectNode().put("newAlarmDetailsProperty", "newAlarmDetailsValue");
        long clearTs = 500L;

        metadata.putValue("metadataKey", "metadataValue");
        metadata.putValue("alarmType", alarmType);
        var msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, msgOriginator, metadata, "{\"temperature\": 50}");

        var ruleNodeSelfId = new RuleNodeId(Uuids.timeBased());

        // mocks
        given(ctxMock.getTenantId()).willReturn(tenantId);
        given(ctxMock.getAlarmService()).willReturn(alarmServiceMock);
        given(ctxMock.getDbCallbackExecutor()).willReturn(dbExecutor);
        given(ctxMock.getSelfId()).willReturn(ruleNodeSelfId);
        doReturn(clearTs).when(nodeSpy).currentTimeMillis();
        given(ctxMock.createScriptEngine(scriptLang, alarmDetailsScript)).willReturn(alarmDetailsScriptMock);
        given(alarmDetailsScriptMock.executeJsonAsync(any())).willReturn(Futures.immediateFuture(newAlarmDetails));

        var alarmId = new AlarmId(Uuids.timeBased());
        var activeAlarm = Alarm.builder()
                .type(alarmType)
                .tenantId(tenantId)
                .originator(msgOriginator)
                .severity(AlarmSeverity.WARNING)
                .startTs(100L)
                .endTs(200L)
                .details(oldAlarmDetails)
                .build();
        activeAlarm.setId(alarmId);

        var expectedAlarm = new Alarm(activeAlarm);
        expectedAlarm.setCleared(true);
        expectedAlarm.setDetails(newAlarmDetails);

        var expectedAlarmInfo = new AlarmInfo(expectedAlarm);

        given(alarmServiceMock.findLatestActiveByOriginatorAndType(tenantId, msgOriginator, alarmType)).willReturn(activeAlarm);
        given(alarmServiceMock.clearAlarm(tenantId, alarmId, clearTs, newAlarmDetails))
                .willReturn(AlarmApiCallResult.builder()
                        .successful(true)
                        .cleared(true)
                        .alarm(expectedAlarmInfo)
                        .build());
        given(ctxMock.alarmActionMsg(expectedAlarmInfo, ruleNodeSelfId, TbMsgType.ALARM_CLEAR)).willReturn(alarmActionMsgMock);
        given(ctxMock.transformMsg(any(TbMsg.class), any(TbMsgType.class), any(EntityId.class), any(TbMsgMetaData.class), anyString()))
                .willAnswer(answer -> TbMsg.transformMsg(
                        answer.getArgument(0, TbMsg.class),
                        answer.getArgument(1, TbMsgType.class),
                        answer.getArgument(2, EntityId.class),
                        answer.getArgument(3, TbMsgMetaData.class),
                        answer.getArgument(4, String.class))
                );

        // node initialization
        nodeSpy.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        // WHEN
        nodeSpy.onMsg(ctxMock, msg);

        // THEN

        // verify alarm details script evaluation
        then(ctxMock).should().logJsEvalRequest();
        var dummyMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        then(alarmDetailsScriptMock).should().executeJsonAsync(dummyMsgCaptor.capture());
        TbMsg actualDummyMsg = dummyMsgCaptor.getValue();
        assertThat(actualDummyMsg.getType()).isEqualTo(msg.getType());
        assertThat(actualDummyMsg.getData()).isEqualTo(msg.getData());
        assertThat(actualDummyMsg.getMetaData().getData()).containsEntry(TbAbstractAlarmNode.PREV_ALARM_DETAILS, JacksonUtil.toString(oldAlarmDetails));
        then(ctxMock).should().logJsEvalResponse();
        then(ctxMock).should(never()).logJsEvalFailure();

        // verify we called clearAlarm() with correct parameters
        then(alarmServiceMock).should().clearAlarm(tenantId, alarmId, clearTs, newAlarmDetails);

        // verify that we created a correct alarm action message and enqueued it
        then(ctxMock).should().alarmActionMsg(expectedAlarmInfo, ruleNodeSelfId, TbMsgType.ALARM_CLEAR);
        then(ctxMock).should().enqueue(eq(alarmActionMsgMock), successCaptor.capture(), any());

        // run success captor to emulate successful enqueueing and to trigger further processing on the success path
        successCaptor.getValue().run();

        // capture and verify an outgoing message
        var outgoingMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        then(ctxMock).should().tellNext(outgoingMsgCaptor.capture(), eq("Cleared"));
        var actualOutgoingMsg = outgoingMsgCaptor.getValue();
        assertThat(actualOutgoingMsg.getType()).isEqualTo(TbMsgType.ALARM.name());
        assertThat(actualOutgoingMsg.getOriginator()).isEqualTo(msgOriginator);
        assertThat(actualOutgoingMsg.getData()).isEqualTo(JacksonUtil.toString(expectedAlarmInfo));

        Map<String, String> actualOutgoingMsgMetadataContent = actualOutgoingMsg.getMetaData().getData();
        assertThat(actualOutgoingMsgMetadataContent).containsAllEntriesOf(metadata.getData());
        assertThat(actualOutgoingMsgMetadataContent).containsEntry(DataConstants.IS_CLEARED_ALARM, Boolean.TRUE.toString());
        assertThat(actualOutgoingMsgMetadataContent).size().isEqualTo(metadata.getData().size() + 1);

        // verify wrong processing paths were not taken
        then(ctxMock).should(never()).tellNext(any(), eq(TbNodeConnectionType.FALSE));
        then(ctxMock).should(never()).tellNext(any(), eq("Created"));
        then(ctxMock).should(never()).tellNext(any(), eq("Updated"));
        then(ctxMock).should(never()).tellSuccess(any());
        then(ctxMock).should(never()).tellFailure(any(), any());
    }

    @Test
    @DisplayName(
            "When the alarm details script throws an exception, " +
                    "node should tell failure with that exception, and it should neither clear any alarms, nor should it send any other messages."
    )
    void whenAlarmDetailsScriptThrowsException_thenShouldTellFailureAndNoOtherActions() throws Exception {
        // GIVEN
        config = config.defaultConfiguration();

        var msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, msgOriginator, metadata, "{\"temperature\": 50}");

        given(ctxMock.getTenantId()).willReturn(tenantId);
        given(ctxMock.getAlarmService()).willReturn(alarmServiceMock);
        given(ctxMock.getDbCallbackExecutor()).willReturn(dbExecutor);
        given(ctxMock.createScriptEngine(ScriptLanguage.TBEL, config.getAlarmDetailsBuildTbel())).willReturn(alarmDetailsScriptMock);

        var alarmId = new AlarmId(Uuids.timeBased());
        var activeAlarm = Alarm.builder()
                .type(config.getAlarmType())
                .tenantId(tenantId)
                .originator(msgOriginator)
                .severity(AlarmSeverity.WARNING)
                .startTs(100L)
                .endTs(200L)
                .details(JacksonUtil.newObjectNode())
                .build();
        activeAlarm.setId(alarmId);
        given(alarmServiceMock.findLatestActiveByOriginatorAndType(tenantId, msgOriginator, activeAlarm.getType())).willReturn(activeAlarm);

        var expectedException = new ExecutionException("Failed to execute script.", new RuntimeException("Something went wrong!"));
        given(alarmDetailsScriptMock.executeJsonAsync(any())).willReturn(Futures.immediateFailedFuture(expectedException));

        nodeSpy.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        // WHEN
        nodeSpy.onMsg(ctxMock, msg);

        // THEN
        then(ctxMock).should().logJsEvalRequest();
        then(ctxMock).should().logJsEvalFailure();

        var exceptionCaptor = ArgumentCaptor.forClass(Throwable.class);
        then(ctxMock).should().tellFailure(eq(msg), exceptionCaptor.capture());
        Throwable actualException = exceptionCaptor.getValue();
        assertThat(actualException).isEqualTo(expectedException);

        then(alarmServiceMock).should(never()).clearAlarm(any(), any(), anyLong(), any());

        then(ctxMock).should(never()).tellNext(any(), eq(TbNodeConnectionType.FALSE));
        then(ctxMock).should(never()).tellNext(any(), eq("Created"));
        then(ctxMock).should(never()).tellNext(any(), eq("Updated"));
        then(ctxMock).should(never()).tellNext(any(), eq("Cleared"));
        then(ctxMock).should(never()).tellSuccess(any());
    }

}
