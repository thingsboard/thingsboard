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
package org.thingsboard.rule.engine.action;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.FluentFuture;
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
import org.thingsboard.server.common.data.alarm.AlarmCreateOrUpdateActiveRequest;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmPropagationInfo;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmUpdateRequest;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.data.script.ScriptLanguage;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static com.google.common.util.concurrent.Futures.immediateFailedFuture;
import static com.google.common.util.concurrent.Futures.immediateFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class TbCreateAlarmNodeTest {

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
    TbCreateAlarmNode nodeSpy;
    TbCreateAlarmNodeConfiguration config;

    final TenantId tenantId = TenantId.fromUUID(Uuids.timeBased());
    final EntityId msgOriginator = new DeviceId(Uuids.timeBased());
    TbMsgMetaData metadata;

    ListeningExecutor dbExecutor;

    @BeforeEach
    void before() {
        dbExecutor = new TestDbCallbackExecutor();
        metadata = new TbMsgMetaData();
        config = new TbCreateAlarmNodeConfiguration();

        lenient().when(ctxMock.getDbCallbackExecutor()).thenReturn(dbExecutor);
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
        assertThat(config.getSeverity()).isEqualTo(AlarmSeverity.CRITICAL.name());
        assertThat(config.isPropagate()).isFalse();
        assertThat(config.isPropagateToOwner()).isFalse();
        assertThat(config.isPropagateToTenant()).isFalse();
        assertThat(config.isUseMessageAlarmData()).isFalse();
        assertThat(config.isOverwriteAlarmDetails()).isFalse();
        assertThat(config.isDynamicSeverity()).isFalse();
        assertThat(config.getRelationTypes()).isEmpty();
    }

    @Test
    @DisplayName("When node is taking alarm info from default node config and alarm does not exist, then should create new alarm using info from default config.")
    void whenAlarmDataIsTakenFromDefaultNodeConfigAndAlarmDoesNotExist_thenNewAlarmIsCreated() throws Exception {
        // GIVEN

        // node configuration
        config = config.defaultConfiguration();

        // other values
        String alarmType = config.getAlarmType();
        AlarmSeverity alarmSeverity = AlarmSeverity.valueOf(config.getSeverity());
        JsonNode alarmDetails = JacksonUtil.newObjectNode();

        long metadataTs = 1711631716127L;
        metadata.putValue("ts", Long.toString(metadataTs));
        metadata.putValue("location", "Company office");

        var ruleNodeSelfId = new RuleNodeId(Uuids.timeBased());

        var incomingMsg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(msgOriginator)
                .copyMetaData(metadata)
                .data("{\"temperature\": 50}")
                .build();

        Alarm existingAlarm = null;

        // expected values
        var expectedAlarm = Alarm.builder()
                .tenantId(tenantId)
                .originator(msgOriginator)
                .cleared(false)
                .acknowledged(false)
                .severity(alarmSeverity)
                .propagate(false)
                .propagateToOwner(false)
                .propagateToTenant(false)
                .propagateRelationTypes(Collections.emptyList())
                .type(alarmType)
                .startTs(metadataTs)
                .endTs(metadataTs)
                .details(alarmDetails)
                .build();
        var expectedCreatedAlarmInfo = new AlarmInfo(expectedAlarm);
        expectedCreatedAlarmInfo.setId(new AlarmId(Uuids.timeBased()));

        var expectedCreateAlarmRequest = AlarmCreateOrUpdateActiveRequest.builder()
                .tenantId(tenantId)
                .customerId(null)
                .type(alarmType)
                .originator(msgOriginator)
                .severity(alarmSeverity)
                .startTs(metadataTs)
                .endTs(metadataTs)
                .details(alarmDetails)
                .propagation(AlarmPropagationInfo.builder()
                        .propagate(false)
                        .propagateToOwner(false)
                        .propagateToTenant(false)
                        .propagateRelationTypes(Collections.emptyList()).build())
                .userId(null)
                .edgeAlarmId(null)
                .build();

        // mocks
        given(ctxMock.getTenantId()).willReturn(tenantId);
        given(ctxMock.getAlarmService()).willReturn(alarmServiceMock);
        given(ctxMock.getSelfId()).willReturn(ruleNodeSelfId);
        given(alarmServiceMock.findLatestActiveByOriginatorAndTypeAsync(tenantId, msgOriginator, alarmType)).willReturn(FluentFuture.from(immediateFuture(existingAlarm)));
        given(alarmDetailsScriptMock.executeJsonAsync(incomingMsg)).willReturn(immediateFuture(alarmDetails));
        var apiCallResult = AlarmApiCallResult.builder()
                .successful(true)
                .created(true)
                .modified(false)
                .cleared(false)
                .deleted(false)
                .alarm(expectedCreatedAlarmInfo)
                .old(null)
                .propagatedEntitiesList(Collections.emptyList())
                .build();
        given(alarmServiceMock.createAlarm(expectedCreateAlarmRequest)).willReturn(apiCallResult);
        given(ctxMock.alarmActionMsg(expectedCreatedAlarmInfo, ruleNodeSelfId, TbMsgType.ENTITY_CREATED)).willReturn(alarmActionMsgMock);
        given(ctxMock.transformMsg(any(TbMsg.class), any(TbMsgType.class), any(EntityId.class), any(TbMsgMetaData.class), anyString()))
                .willAnswer(answer -> answer.getArgument(0, TbMsg.class).transform()
                        .type(answer.getArgument(1, TbMsgType.class))
                        .originator(answer.getArgument(2, EntityId.class))
                        .metaData(answer.getArgument(3, TbMsgMetaData.class))
                        .data(answer.getArgument(4, String.class))
                        .build()
                );
        given(ctxMock.createScriptEngine(ScriptLanguage.TBEL, TbAbstractAlarmNodeConfiguration.ALARM_DETAILS_BUILD_TBEL_TEMPLATE)).willReturn(alarmDetailsScriptMock);

        // node initialization
        nodeSpy.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        // WHEN
        nodeSpy.onMsg(ctxMock, incomingMsg);

        // THEN

        // verify alarm details script evaluation
        then(alarmDetailsScriptMock).should().executeJsonAsync(incomingMsg);

        // verify we called createAlarm() with correct AlarmCreateOrUpdateActiveRequest
        then(alarmServiceMock).should().createAlarm(expectedCreateAlarmRequest);
        then(alarmServiceMock).should(never()).updateAlarm(any());

        // verify that we created a correct alarm action message and enqueued it
        then(ctxMock).should().alarmActionMsg(expectedCreatedAlarmInfo, ruleNodeSelfId, TbMsgType.ENTITY_CREATED);
        then(ctxMock).should().enqueue(eq(alarmActionMsgMock), successCaptor.capture(), any());

        // run success captor to emulate successful sending and to trigger further processing on the success path
        successCaptor.getValue().run();

        // capture and verify an outgoing message
        var outgoingMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        then(ctxMock).should().tellNext(outgoingMsgCaptor.capture(), eq("Created"));
        var actualOutgoingMsg = outgoingMsgCaptor.getValue();
        assertThat(actualOutgoingMsg.getType()).isEqualTo(TbMsgType.ALARM.name());
        assertThat(actualOutgoingMsg.getOriginator()).isEqualTo(msgOriginator);
        assertThat(actualOutgoingMsg.getData()).isEqualTo(JacksonUtil.valueToTree(expectedCreatedAlarmInfo).toString());

        Map<String, String> actualOutgoingMsgMetadataContent = actualOutgoingMsg.getMetaData().getData();
        assertThat(actualOutgoingMsgMetadataContent).containsAllEntriesOf(metadata.getData());
        assertThat(actualOutgoingMsgMetadataContent).containsEntry(DataConstants.IS_NEW_ALARM, Boolean.TRUE.toString());
        assertThat(actualOutgoingMsgMetadataContent).size().isEqualTo(metadata.getData().size() + 1);

        // verify wrong processing paths were not taken
        then(ctxMock).should(never()).tellNext(any(), eq(TbNodeConnectionType.FALSE));
        then(ctxMock).should(never()).tellNext(any(), eq("Updated"));
        then(ctxMock).should(never()).tellNext(any(), eq("Cleared"));
        then(ctxMock).should(never()).tellSuccess(any());
        then(ctxMock).should(never()).tellFailure(any(), any());
    }

    @Test
    @DisplayName("When node is taking alarm info from node config and cleared alarm exists, then should create new alarm using info from config.")
    void whenAlarmDataIsTakenFromNodeConfigAndClearedAlarmExists_thenNewAlarmIsCreated() throws Exception {
        // GIVEN

        // node configuration
        config.setAlarmType("$[alarmType]");
        config.setScriptLang(ScriptLanguage.JS);
        config.setAlarmDetailsBuildJs("""
                return {
                    alarmDetails: "Some alarm details"
                };
                """);
        config.setAlarmDetailsBuildTbel("""
                return {
                    alarmDetails: "Some alarm details"
                };
                """);
        config.setDynamicSeverity(true);
        config.setSeverity("${alarmSeverity}");
        config.setPropagate(true);
        config.setPropagateToOwner(true);
        config.setPropagateToTenant(true);
        config.setRelationTypes(List.of("RELATION_TYPE_1", "RELATION_TYPE_2", "RELATION_TYPE_3"));
        config.setUseMessageAlarmData(false);
        config.setOverwriteAlarmDetails(false);

        // other values
        String alarmType = "High Temperature";
        AlarmSeverity alarmSeverity = AlarmSeverity.MAJOR;
        JsonNode alarmDetails = JacksonUtil.newObjectNode().put("alarmDetails", "Some alarm details");

        long metadataTs = 1711631716127L;
        metadata.putValue("ts", Long.toString(metadataTs));
        metadata.putValue("alarmSeverity", alarmSeverity.name());
        metadata.putValue("location", "Company office");

        var ruleNodeSelfId = new RuleNodeId(Uuids.timeBased());

        var incomingMsg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(msgOriginator)
                .copyMetaData(metadata)
                .data("{\"temperature\": 50, \"alarmType\": \"" + alarmType + "\"}")
                .build();

        var existingClearedAlarm = Alarm.builder()
                .tenantId(tenantId)
                .originator(msgOriginator)
                .cleared(true)
                .acknowledged(false)
                .severity(AlarmSeverity.WARNING)
                .propagate(false)
                .propagateToOwner(false)
                .propagateToTenant(false)
                .propagateRelationTypes(Collections.emptyList())
                .type(alarmType)
                .startTs(100L)
                .endTs(200L)
                .details(JacksonUtil.newObjectNode().put("oldAlarmDetailsProperty", "oldAlarmDetailsPropertyValue"))
                .build();
        existingClearedAlarm.setId(new AlarmId(Uuids.timeBased()));

        // expected values
        var expectedCreatedAlarm = Alarm.builder()
                .tenantId(tenantId)
                .originator(msgOriginator)
                .cleared(false)
                .acknowledged(false)
                .severity(alarmSeverity)
                .propagate(true)
                .propagateToOwner(true)
                .propagateToTenant(true)
                .propagateRelationTypes(config.getRelationTypes())
                .type(alarmType)
                .startTs(metadataTs)
                .endTs(metadataTs)
                .details(alarmDetails)
                .build();
        var expectedCreatedAlarmInfo = new AlarmInfo(expectedCreatedAlarm);
        expectedCreatedAlarmInfo.setId(new AlarmId(Uuids.timeBased()));

        var expectedCreateAlarmRequest = AlarmCreateOrUpdateActiveRequest.builder()
                .tenantId(tenantId)
                .customerId(null)
                .type(alarmType)
                .originator(msgOriginator)
                .severity(alarmSeverity)
                .startTs(metadataTs)
                .endTs(metadataTs)
                .details(alarmDetails)
                .propagation(AlarmPropagationInfo.builder()
                        .propagate(true)
                        .propagateToOwner(true)
                        .propagateToTenant(true)
                        .propagateRelationTypes(config.getRelationTypes()).build())
                .userId(null)
                .edgeAlarmId(null)
                .build();

        // mocks
        given(ctxMock.getTenantId()).willReturn(tenantId);
        given(ctxMock.getAlarmService()).willReturn(alarmServiceMock);
        given(ctxMock.getSelfId()).willReturn(ruleNodeSelfId);
        given(alarmServiceMock.findLatestActiveByOriginatorAndTypeAsync(tenantId, msgOriginator, alarmType)).willReturn(FluentFuture.from(immediateFuture(existingClearedAlarm)));
        given(alarmDetailsScriptMock.executeJsonAsync(incomingMsg)).willReturn(immediateFuture(alarmDetails));
        var apiCallResult = AlarmApiCallResult.builder()
                .successful(true)
                .created(true)
                .modified(false)
                .cleared(false)
                .deleted(false)
                .alarm(expectedCreatedAlarmInfo)
                .old(null)
                .propagatedEntitiesList(List.of(TenantId.fromUUID(Uuids.timeBased()), new CustomerId(Uuids.timeBased()), new AssetId(Uuids.timeBased())))
                .build();
        given(alarmServiceMock.createAlarm(expectedCreateAlarmRequest)).willReturn(apiCallResult);
        given(ctxMock.alarmActionMsg(expectedCreatedAlarmInfo, ruleNodeSelfId, TbMsgType.ENTITY_CREATED)).willReturn(alarmActionMsgMock);
        given(ctxMock.transformMsg(any(TbMsg.class), any(TbMsgType.class), any(EntityId.class), any(TbMsgMetaData.class), anyString()))
                .willAnswer(answer -> answer.getArgument(0, TbMsg.class).transform()
                        .type(answer.getArgument(1, TbMsgType.class))
                        .originator(answer.getArgument(2, EntityId.class))
                        .metaData(answer.getArgument(3, TbMsgMetaData.class))
                        .data(answer.getArgument(4, String.class))
                        .build()
                );
        given(ctxMock.createScriptEngine(ScriptLanguage.JS, config.getAlarmDetailsBuildJs())).willReturn(alarmDetailsScriptMock);

        // node initialization
        nodeSpy.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        // WHEN
        nodeSpy.onMsg(ctxMock, incomingMsg);

        // THEN

        // verify alarm details script evaluation
        then(alarmDetailsScriptMock).should().executeJsonAsync(incomingMsg);

        // verify we called createAlarm() with correct AlarmCreateOrUpdateActiveRequest
        then(alarmServiceMock).should().createAlarm(expectedCreateAlarmRequest);
        then(alarmServiceMock).should(never()).updateAlarm(any());

        // verify that we created a correct alarm action message and enqueued it
        then(ctxMock).should().alarmActionMsg(expectedCreatedAlarmInfo, ruleNodeSelfId, TbMsgType.ENTITY_CREATED);
        then(ctxMock).should().enqueue(eq(alarmActionMsgMock), successCaptor.capture(), any());

        // run success captor to emulate successful sending and to trigger further processing on the success path
        successCaptor.getValue().run();

        // capture and verify an outgoing message
        var outgoingMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        then(ctxMock).should().tellNext(outgoingMsgCaptor.capture(), eq("Created"));
        var actualOutgoingMsg = outgoingMsgCaptor.getValue();
        assertThat(actualOutgoingMsg.getType()).isEqualTo(TbMsgType.ALARM.name());
        assertThat(actualOutgoingMsg.getOriginator()).isEqualTo(msgOriginator);
        assertThat(actualOutgoingMsg.getData()).isEqualTo(JacksonUtil.valueToTree(expectedCreatedAlarmInfo).toString());

        Map<String, String> actualOutgoingMsgMetadataContent = actualOutgoingMsg.getMetaData().getData();
        assertThat(actualOutgoingMsgMetadataContent).containsAllEntriesOf(metadata.getData());
        assertThat(actualOutgoingMsgMetadataContent).containsEntry(DataConstants.IS_NEW_ALARM, Boolean.TRUE.toString());
        assertThat(actualOutgoingMsgMetadataContent).size().isEqualTo(metadata.getData().size() + 1);

        // verify wrong processing paths were not taken
        then(ctxMock).should(never()).tellNext(any(), eq(TbNodeConnectionType.FALSE));
        then(ctxMock).should(never()).tellNext(any(), eq("Updated"));
        then(ctxMock).should(never()).tellNext(any(), eq("Cleared"));
        then(ctxMock).should(never()).tellSuccess(any());
        then(ctxMock).should(never()).tellFailure(any(), any());
    }

    @Test
    @DisplayName("When node is taking alarm info from node config and active alarm exists, then should update existing alarm using info from config.")
    void whenAlarmDataIsTakenFromNodeConfigAndActiveAlarmExists_thenExistingAlarmIsUpdated() throws Exception {
        // GIVEN

        // values that changed between existing alarm and updated alarm
        AlarmSeverity oldAlarmSeverity = AlarmSeverity.WARNING;
        AlarmSeverity newAlarmSeverity = AlarmSeverity.MAJOR;

        boolean oldPropagate = true;
        boolean newPropagate = false;

        boolean oldPropagateToOwner = false;
        boolean newPropagateToOwner = true;

        boolean oldPropagateToTenant = false;
        boolean newPropagateToTenant = true;

        List<String> oldPropagateRelationTypes = List.of("RELATION_TYPE_1", "RELATION_TYPE_2", "RELATION_TYPE_3");
        List<String> newPropagateRelationTypes = Collections.emptyList();

        JsonNode oldAlarmDetails = JacksonUtil.newObjectNode().put("oldAlarmDetailsKey", "oldAlarmDetailsValue");
        JsonNode newAlarmDetails = JacksonUtil.newObjectNode().put("newAlarmDetails", "Some alarm details TBEL").set("oldAlarmDetails", oldAlarmDetails);

        long oldEndTs = 200L;
        long newEndTs = 300L;

        // node configuration
        config.setAlarmType("${alarmType}");
        config.setScriptLang(ScriptLanguage.TBEL);
        config.setAlarmDetailsBuildJs("""
                return {
                    oldAlarmDetails: metadata.prevAlarmDetails,
                    newAlarmDetails: "Some alarm details JS"
                };
                """);
        config.setAlarmDetailsBuildTbel("""
                return {
                    oldAlarmDetails: metadata.prevAlarmDetails,
                    newAlarmDetails: "Some alarm details TBEL"
                };
                """);
        config.setDynamicSeverity(true);
        config.setSeverity("$[alarmSeverity]");
        config.setPropagate(newPropagate);
        config.setPropagateToOwner(newPropagateToOwner);
        config.setPropagateToTenant(newPropagateToTenant);
        config.setRelationTypes(newPropagateRelationTypes);
        config.setUseMessageAlarmData(false);
        config.setOverwriteAlarmDetails(false);

        // other values
        String alarmType = "High Temperature";

        long metadataTs = 1711631716127L;
        metadata.putValue("ts", Long.toString(metadataTs));
        metadata.putValue("alarmType", alarmType);
        metadata.putValue("location", "Company office");

        var ruleNodeSelfId = new RuleNodeId(Uuids.timeBased());

        var incomingMsg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(msgOriginator)
                .copyMetaData(metadata)
                .data("{\"temperature\": 50, \"alarmSeverity\": \"" + newAlarmSeverity.name() + "\"}")
                .build();

        var existingAlarmId = new AlarmId(Uuids.timeBased());
        var existingActiveAlarm = Alarm.builder()
                .tenantId(tenantId)
                .originator(msgOriginator)
                .cleared(false)
                .acknowledged(false)
                .severity(oldAlarmSeverity)
                .propagate(oldPropagate)
                .propagateToOwner(oldPropagateToOwner)
                .propagateToTenant(oldPropagateToTenant)
                .propagateRelationTypes(oldPropagateRelationTypes)
                .type(alarmType)
                .startTs(100L)
                .endTs(oldEndTs)
                .details(oldAlarmDetails)
                .build();
        existingActiveAlarm.setId(existingAlarmId);

        // expected values
        var expectedUpdatedAlarm = Alarm.builder()
                .tenantId(tenantId)
                .originator(msgOriginator)
                .cleared(false)
                .acknowledged(false)
                .severity(newAlarmSeverity)
                .propagate(newPropagate)
                .propagateToOwner(newPropagateToOwner)
                .propagateToTenant(newPropagateToTenant)
                .propagateRelationTypes(newPropagateRelationTypes)
                .type(alarmType)
                .startTs(100L)
                .endTs(newEndTs)
                .details(newAlarmDetails)
                .build();
        expectedUpdatedAlarm.setId(existingAlarmId);
        var expectedUpdatedAlarmInfo = new AlarmInfo(expectedUpdatedAlarm);

        var expectedUpdateAlarmRequest = AlarmUpdateRequest.builder()
                .tenantId(tenantId)
                .alarmId(existingAlarmId)
                .severity(newAlarmSeverity)
                .startTs(100L)
                .endTs(newEndTs)
                .details(newAlarmDetails)
                .propagation(AlarmPropagationInfo.builder()
                        .propagate(newPropagate)
                        .propagateToOwner(newPropagateToOwner)
                        .propagateToTenant(newPropagateToTenant)
                        .propagateRelationTypes(newPropagateRelationTypes).build())
                .userId(null)
                .build();

        // mocks
        given(ctxMock.getTenantId()).willReturn(tenantId);
        given(ctxMock.getAlarmService()).willReturn(alarmServiceMock);
        given(ctxMock.getSelfId()).willReturn(ruleNodeSelfId);
        given(alarmServiceMock.findLatestActiveByOriginatorAndTypeAsync(tenantId, msgOriginator, alarmType)).willReturn(FluentFuture.from(immediateFuture(existingActiveAlarm)));
        given(alarmDetailsScriptMock.executeJsonAsync(any())).willReturn(immediateFuture(newAlarmDetails));
        doReturn(newEndTs).when(nodeSpy).currentTimeMillis();
        var apiCallResult = AlarmApiCallResult.builder()
                .successful(true)
                .created(false)
                .modified(true)
                .cleared(false)
                .deleted(false)
                .alarm(expectedUpdatedAlarmInfo)
                .old(new Alarm(existingActiveAlarm))
                .propagatedEntitiesList(List.of(tenantId))
                .build();
        given(alarmServiceMock.updateAlarm(expectedUpdateAlarmRequest)).willReturn(apiCallResult);
        given(ctxMock.alarmActionMsg(expectedUpdatedAlarmInfo, ruleNodeSelfId, TbMsgType.ENTITY_UPDATED)).willReturn(alarmActionMsgMock);
        given(ctxMock.transformMsg(any(TbMsg.class), any(TbMsgType.class), any(EntityId.class), any(TbMsgMetaData.class), anyString()))
                .willAnswer(answer -> answer.getArgument(0, TbMsg.class).transform()
                        .type(answer.getArgument(1, TbMsgType.class))
                        .originator(answer.getArgument(2, EntityId.class))
                        .metaData(answer.getArgument(3, TbMsgMetaData.class))
                        .data(answer.getArgument(4, String.class))
                        .build()
                );
        given(ctxMock.createScriptEngine(ScriptLanguage.TBEL, config.getAlarmDetailsBuildTbel())).willReturn(alarmDetailsScriptMock);

        // node initialization
        nodeSpy.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        // WHEN
        nodeSpy.onMsg(ctxMock, incomingMsg);

        // THEN

        // verify alarm details script evaluation
        var dummyMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        then(alarmDetailsScriptMock).should().executeJsonAsync(dummyMsgCaptor.capture());
        TbMsg actualDummyMsg = dummyMsgCaptor.getValue();
        assertThat(actualDummyMsg.getType()).isEqualTo(incomingMsg.getType());
        assertThat(actualDummyMsg.getData()).isEqualTo(incomingMsg.getData());
        assertThat(actualDummyMsg.getMetaData().getData()).containsEntry("prevAlarmDetails", JacksonUtil.toString(oldAlarmDetails));

        // verify we called updateAlarm() with correct AlarmUpdateRequest
        then(alarmServiceMock).should().updateAlarm(expectedUpdateAlarmRequest);
        then(alarmServiceMock).should(never()).createAlarm(any());

        // verify that we created a correct alarm action message and enqueued it
        then(ctxMock).should().alarmActionMsg(expectedUpdatedAlarmInfo, ruleNodeSelfId, TbMsgType.ENTITY_UPDATED);
        then(ctxMock).should().enqueue(eq(alarmActionMsgMock), successCaptor.capture(), any());

        // run success captor to emulate successful queueing of an alarm action message and to trigger further processing on the success path
        successCaptor.getValue().run();

        // capture and verify an outgoing message
        var outgoingMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        then(ctxMock).should().tellNext(outgoingMsgCaptor.capture(), eq("Updated"));
        var actualOutgoingMsg = outgoingMsgCaptor.getValue();
        assertThat(actualOutgoingMsg.getType()).isEqualTo(TbMsgType.ALARM.name());
        assertThat(actualOutgoingMsg.getOriginator()).isEqualTo(msgOriginator);
        assertThat(actualOutgoingMsg.getData()).isEqualTo(JacksonUtil.valueToTree(expectedUpdatedAlarmInfo).toString());

        Map<String, String> actualOutgoingMsgMetadataContent = actualOutgoingMsg.getMetaData().getData();
        assertThat(actualOutgoingMsgMetadataContent).containsAllEntriesOf(metadata.getData());
        assertThat(actualOutgoingMsgMetadataContent).containsEntry(DataConstants.IS_EXISTING_ALARM, Boolean.TRUE.toString());
        assertThat(actualOutgoingMsgMetadataContent).size().isEqualTo(metadata.getData().size() + 1);

        // verify wrong processing paths were not taken
        then(ctxMock).should(never()).tellNext(any(), eq(TbNodeConnectionType.FALSE));
        then(ctxMock).should(never()).tellNext(any(), eq("Created"));
        then(ctxMock).should(never()).tellNext(any(), eq("Cleared"));
        then(ctxMock).should(never()).tellSuccess(any());
        then(ctxMock).should(never()).tellFailure(any(), any());
    }

    @Test
    @DisplayName("When node is taking alarm data from incoming message and cleared alarm exists, then should create new alarm using info from incoming message.")
    void whenAlarmDataIsTakenFromMsgAndClearedAlarmExists_thenNewAlarmIsCreated() throws Exception {
        // GIVEN

        // node configuration
        config = config.defaultConfiguration();
        config.setUseMessageAlarmData(true);
        config.setOverwriteAlarmDetails(false);

        // other values
        String alarmType = "High Temperature";
        AlarmSeverity alarmSeverity = AlarmSeverity.MAJOR;
        JsonNode alarmDetails = JacksonUtil.newObjectNode().put("alarmDetails", "Some alarm details");

        // alarm that is inside an incoming message
        var alarmFromIncomingMessage = Alarm.builder()
                .tenantId(tenantId)
                .originator(msgOriginator)
                .cleared(false)
                .acknowledged(false)
                .severity(alarmSeverity)
                .propagate(true)
                .propagateToOwner(true)
                .propagateToTenant(true)
                .propagateRelationTypes(Collections.emptyList())
                .type(alarmType)
                .startTs(100L)
                .endTs(300L)
                .details(alarmDetails)
                .build();

        long metadataTs = 1711631716127L;
        metadata.putValue("ts", Long.toString(metadataTs));
        metadata.putValue("location", "Company office");

        var ruleNodeSelfId = new RuleNodeId(Uuids.timeBased());

        var incomingMsg = TbMsg.newMsg()
                .type(TbMsgType.ALARM)
                .originator(msgOriginator)
                .copyMetaData(metadata)
                .data(JacksonUtil.toString(alarmFromIncomingMessage))
                .build();

        var existingClearedAlarm = Alarm.builder()
                .tenantId(tenantId)
                .originator(msgOriginator)
                .cleared(true)
                .acknowledged(false)
                .severity(AlarmSeverity.WARNING)
                .propagate(false)
                .propagateToOwner(true)
                .propagateToTenant(true)
                .propagateRelationTypes(Collections.emptyList())
                .type(alarmType)
                .startTs(100L)
                .endTs(200L)
                .details(JacksonUtil.newObjectNode())
                .build();
        existingClearedAlarm.setId(new AlarmId(Uuids.timeBased()));

        // expected values
        var expectedCreatedAlarm = Alarm.builder()
                .tenantId(tenantId)
                .originator(msgOriginator)
                .cleared(false)
                .acknowledged(false)
                .severity(alarmSeverity)
                .propagate(true)
                .propagateToOwner(true)
                .propagateToTenant(true)
                .propagateRelationTypes(Collections.emptyList())
                .type(alarmType)
                .startTs(100L)
                .endTs(300L)
                .details(alarmDetails)
                .build();
        var expectedCreatedAlarmInfo = new AlarmInfo(expectedCreatedAlarm);
        expectedCreatedAlarmInfo.setId(new AlarmId(Uuids.timeBased()));

        var expectedCreateAlarmRequest = AlarmCreateOrUpdateActiveRequest.builder()
                .tenantId(tenantId)
                .customerId(null)
                .type(alarmType)
                .originator(msgOriginator)
                .severity(alarmSeverity)
                .startTs(100L)
                .endTs(300L)
                .details(alarmDetails)
                .propagation(AlarmPropagationInfo.builder()
                        .propagate(true)
                        .propagateToOwner(true)
                        .propagateToTenant(true)
                        .propagateRelationTypes(Collections.emptyList()).build())
                .userId(null)
                .edgeAlarmId(null)
                .build();

        // mocks
        given(ctxMock.getTenantId()).willReturn(tenantId);
        given(ctxMock.getAlarmService()).willReturn(alarmServiceMock);
        given(ctxMock.getSelfId()).willReturn(ruleNodeSelfId);
        given(alarmServiceMock.findLatestActiveByOriginatorAndTypeAsync(tenantId, msgOriginator, alarmType)).willReturn(FluentFuture.from(immediateFuture(existingClearedAlarm)));
        var apiCallResult = AlarmApiCallResult.builder()
                .successful(true)
                .created(true)
                .modified(false)
                .cleared(false)
                .deleted(false)
                .alarm(expectedCreatedAlarmInfo)
                .old(null)
                .propagatedEntitiesList(List.of(TenantId.fromUUID(Uuids.timeBased()), new CustomerId(Uuids.timeBased()), new AssetId(Uuids.timeBased())))
                .build();
        given(alarmServiceMock.createAlarm(expectedCreateAlarmRequest)).willReturn(apiCallResult);
        given(ctxMock.alarmActionMsg(expectedCreatedAlarmInfo, ruleNodeSelfId, TbMsgType.ENTITY_CREATED)).willReturn(alarmActionMsgMock);
        given(ctxMock.transformMsg(any(TbMsg.class), any(TbMsgType.class), any(EntityId.class), any(TbMsgMetaData.class), anyString()))
                .willAnswer(answer -> answer.getArgument(0, TbMsg.class).transform()
                        .type(answer.getArgument(1, TbMsgType.class))
                        .originator(answer.getArgument(2, EntityId.class))
                        .metaData(answer.getArgument(3, TbMsgMetaData.class))
                        .data(answer.getArgument(4, String.class))
                        .build()
                );
        given(ctxMock.createScriptEngine(ScriptLanguage.TBEL, config.getAlarmDetailsBuildTbel())).willReturn(alarmDetailsScriptMock);

        // node initialization
        nodeSpy.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        // WHEN
        nodeSpy.onMsg(ctxMock, incomingMsg);

        // THEN

        // verify alarm details script was not evaluated
        then(alarmDetailsScriptMock).should(never()).executeJsonAsync(any());

        // verify we called createAlarm() with correct AlarmCreateOrUpdateActiveRequest
        then(alarmServiceMock).should().createAlarm(expectedCreateAlarmRequest);
        then(alarmServiceMock).should(never()).updateAlarm(any());

        // verify that we created a correct alarm action message and enqueued it
        then(ctxMock).should().alarmActionMsg(expectedCreatedAlarmInfo, ruleNodeSelfId, TbMsgType.ENTITY_CREATED);
        then(ctxMock).should().enqueue(eq(alarmActionMsgMock), successCaptor.capture(), any());

        // run success captor to emulate successful sending and to trigger further processing on the success path
        successCaptor.getValue().run();

        // capture and verify an outgoing message
        var outgoingMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        then(ctxMock).should().tellNext(outgoingMsgCaptor.capture(), eq("Created"));
        var actualOutgoingMsg = outgoingMsgCaptor.getValue();
        assertThat(actualOutgoingMsg.getType()).isEqualTo(TbMsgType.ALARM.name());
        assertThat(actualOutgoingMsg.getOriginator()).isEqualTo(msgOriginator);
        assertThat(actualOutgoingMsg.getData()).isEqualTo(JacksonUtil.valueToTree(expectedCreatedAlarmInfo).toString());

        Map<String, String> actualOutgoingMsgMetadataContent = actualOutgoingMsg.getMetaData().getData();
        assertThat(actualOutgoingMsgMetadataContent).containsAllEntriesOf(metadata.getData());
        assertThat(actualOutgoingMsgMetadataContent).containsEntry(DataConstants.IS_NEW_ALARM, Boolean.TRUE.toString());
        assertThat(actualOutgoingMsgMetadataContent).size().isEqualTo(metadata.getData().size() + 1);

        // verify wrong processing paths were not taken
        then(ctxMock).should(never()).tellNext(any(), eq(TbNodeConnectionType.FALSE));
        then(ctxMock).should(never()).tellNext(any(), eq("Updated"));
        then(ctxMock).should(never()).tellNext(any(), eq("Cleared"));
        then(ctxMock).should(never()).tellSuccess(any());
        then(ctxMock).should(never()).tellFailure(any(), any());
    }

    @Test
    @DisplayName("When node is taking alarm data from incoming message and active alarm exists, then should update existing alarm using info from incoming message.")
    void whenAlarmDataIsTakenFromMsgAndActiveAlarmExists_thenExistingAlarmIsUpdated() throws Exception {
        // GIVEN

        // values that changed between existing alarm and updated alarm
        AlarmSeverity oldAlarmSeverity = AlarmSeverity.WARNING;
        AlarmSeverity newAlarmSeverity = AlarmSeverity.MAJOR;

        boolean oldPropagate = true;
        boolean newPropagate = false;

        boolean oldPropagateToOwner = false;
        boolean newPropagateToOwner = true;

        boolean oldPropagateToTenant = false;
        boolean newPropagateToTenant = true;

        List<String> oldPropagateRelationTypes = List.of("RELATION_TYPE_1", "RELATION_TYPE_2", "RELATION_TYPE_3");
        List<String> newPropagateRelationTypes = Collections.emptyList();

        JsonNode oldAlarmDetails = JacksonUtil.newObjectNode().put("oldAlarmDetailsKey", "oldAlarmDetailsValue");
        JsonNode newAlarmDetails = JacksonUtil.newObjectNode().put("newAlarmDetails", "Some alarm details TBEL").set("oldAlarmDetails", oldAlarmDetails);

        long oldEndTs = 200L;
        long newEndTs = 300L;

        // node configuration
        config = config.defaultConfiguration();
        config.setUseMessageAlarmData(true);
        config.setOverwriteAlarmDetails(true);

        // other values
        String alarmType = "High Temperature";

        long metadataTs = 1711631716127L;
        metadata.putValue("ts", Long.toString(metadataTs));
        metadata.putValue("location", "Company office");

        var ruleNodeSelfId = new RuleNodeId(Uuids.timeBased());

        // alarm that is inside an incoming message
        var alarmFromIncomingMessage = Alarm.builder()
                .tenantId(tenantId)
                .originator(msgOriginator)
                .cleared(false)
                .acknowledged(false)
                .severity(newAlarmSeverity)
                .propagate(newPropagate)
                .propagateToOwner(newPropagateToOwner)
                .propagateToTenant(newPropagateToTenant)
                .propagateRelationTypes(newPropagateRelationTypes)
                .type(alarmType)
                .startTs(100L)
                .endTs(newEndTs)
                .details(newAlarmDetails)
                .build();

        var incomingMsg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(msgOriginator)
                .copyMetaData(metadata)
                .data(JacksonUtil.toString(alarmFromIncomingMessage))
                .build();

        var existingAlarmId = new AlarmId(Uuids.timeBased());
        var existingActiveAlarm = Alarm.builder()
                .tenantId(tenantId)
                .originator(msgOriginator)
                .cleared(false)
                .acknowledged(false)
                .severity(oldAlarmSeverity)
                .propagate(oldPropagate)
                .propagateToOwner(oldPropagateToOwner)
                .propagateToTenant(oldPropagateToTenant)
                .propagateRelationTypes(oldPropagateRelationTypes)
                .type(alarmType)
                .startTs(100L)
                .endTs(oldEndTs)
                .details(oldAlarmDetails)
                .build();
        existingActiveAlarm.setId(existingAlarmId);

        // expected values
        var expectedUpdatedAlarm = Alarm.builder()
                .tenantId(tenantId)
                .originator(msgOriginator)
                .cleared(false)
                .acknowledged(false)
                .severity(newAlarmSeverity)
                .propagate(newPropagate)
                .propagateToOwner(newPropagateToOwner)
                .propagateToTenant(newPropagateToTenant)
                .propagateRelationTypes(newPropagateRelationTypes)
                .type(alarmType)
                .startTs(100L)
                .endTs(newEndTs)
                .details(newAlarmDetails)
                .build();
        expectedUpdatedAlarm.setId(existingAlarmId);
        var expectedUpdatedAlarmInfo = new AlarmInfo(expectedUpdatedAlarm);

        var expectedUpdateAlarmRequest = AlarmUpdateRequest.builder()
                .tenantId(tenantId)
                .alarmId(existingAlarmId)
                .severity(newAlarmSeverity)
                .startTs(100L)
                .endTs(newEndTs)
                .details(newAlarmDetails)
                .propagation(AlarmPropagationInfo.builder()
                        .propagate(newPropagate)
                        .propagateToOwner(newPropagateToOwner)
                        .propagateToTenant(newPropagateToTenant)
                        .propagateRelationTypes(newPropagateRelationTypes).build())
                .userId(null)
                .build();

        // mocks
        given(ctxMock.getTenantId()).willReturn(tenantId);
        given(ctxMock.getAlarmService()).willReturn(alarmServiceMock);
        given(ctxMock.getSelfId()).willReturn(ruleNodeSelfId);
        given(alarmServiceMock.findLatestActiveByOriginatorAndTypeAsync(tenantId, msgOriginator, alarmType)).willReturn(FluentFuture.from(immediateFuture(existingActiveAlarm)));
        given(alarmDetailsScriptMock.executeJsonAsync(any())).willReturn(immediateFuture(newAlarmDetails));
        doReturn(newEndTs).when(nodeSpy).currentTimeMillis();
        var apiCallResult = AlarmApiCallResult.builder()
                .successful(true)
                .created(false)
                .modified(true)
                .cleared(false)
                .deleted(false)
                .alarm(expectedUpdatedAlarmInfo)
                .old(new Alarm(existingActiveAlarm))
                .propagatedEntitiesList(List.of(tenantId))
                .build();
        given(alarmServiceMock.updateAlarm(expectedUpdateAlarmRequest)).willReturn(apiCallResult);
        given(ctxMock.alarmActionMsg(expectedUpdatedAlarmInfo, ruleNodeSelfId, TbMsgType.ENTITY_UPDATED)).willReturn(alarmActionMsgMock);
        given(ctxMock.transformMsg(any(TbMsg.class), any(TbMsgType.class), any(EntityId.class), any(TbMsgMetaData.class), anyString()))
                .willAnswer(answer -> answer.getArgument(0, TbMsg.class).transform()
                        .type(answer.getArgument(1, TbMsgType.class))
                        .originator(answer.getArgument(2, EntityId.class))
                        .metaData(answer.getArgument(3, TbMsgMetaData.class))
                        .data(answer.getArgument(4, String.class))
                        .build()
                );
        given(ctxMock.createScriptEngine(ScriptLanguage.TBEL, config.getAlarmDetailsBuildTbel())).willReturn(alarmDetailsScriptMock);

        // node initialization
        nodeSpy.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        // WHEN
        nodeSpy.onMsg(ctxMock, incomingMsg);

        // THEN

        // verify alarm details script evaluation
        var dummyMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        then(alarmDetailsScriptMock).should().executeJsonAsync(dummyMsgCaptor.capture());
        TbMsg actualDummyMsg = dummyMsgCaptor.getValue();
        assertThat(actualDummyMsg.getType()).isEqualTo(incomingMsg.getType());
        assertThat(actualDummyMsg.getData()).isEqualTo(incomingMsg.getData());
        assertThat(actualDummyMsg.getMetaData().getData()).containsEntry("prevAlarmDetails", JacksonUtil.toString(oldAlarmDetails));

        // verify we called updateAlarm() with correct AlarmUpdateRequest
        then(alarmServiceMock).should().updateAlarm(expectedUpdateAlarmRequest);
        then(alarmServiceMock).should(never()).createAlarm(any());

        // verify that we created a correct alarm action message and enqueued it
        then(ctxMock).should().alarmActionMsg(expectedUpdatedAlarmInfo, ruleNodeSelfId, TbMsgType.ENTITY_UPDATED);
        then(ctxMock).should().enqueue(eq(alarmActionMsgMock), successCaptor.capture(), any());

        // run success captor to emulate successful queueing of an alarm action message and to trigger further processing on the success path
        successCaptor.getValue().run();

        // capture and verify an outgoing message
        var outgoingMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        then(ctxMock).should().tellNext(outgoingMsgCaptor.capture(), eq("Updated"));
        var actualOutgoingMsg = outgoingMsgCaptor.getValue();
        assertThat(actualOutgoingMsg.getType()).isEqualTo(TbMsgType.ALARM.name());
        assertThat(actualOutgoingMsg.getOriginator()).isEqualTo(msgOriginator);
        assertThat(actualOutgoingMsg.getData()).isEqualTo(JacksonUtil.valueToTree(expectedUpdatedAlarmInfo).toString());

        Map<String, String> actualOutgoingMsgMetadataContent = actualOutgoingMsg.getMetaData().getData();
        assertThat(actualOutgoingMsgMetadataContent).containsAllEntriesOf(metadata.getData());
        assertThat(actualOutgoingMsgMetadataContent).containsEntry(DataConstants.IS_EXISTING_ALARM, Boolean.TRUE.toString());
        assertThat(actualOutgoingMsgMetadataContent).size().isEqualTo(metadata.getData().size() + 1);

        // verify wrong processing paths were not taken
        then(ctxMock).should(never()).tellNext(any(), eq(TbNodeConnectionType.FALSE));
        then(ctxMock).should(never()).tellNext(any(), eq("Created"));
        then(ctxMock).should(never()).tellNext(any(), eq("Cleared"));
        then(ctxMock).should(never()).tellSuccess(any());
        then(ctxMock).should(never()).tellFailure(any(), any());
    }

    @Test
    @DisplayName("When only severity was updated (other fields the same), then should consider this as an alarm update and take update processing path.")
    void whenOnlySeverityWasUpdated_thenShouldTakeAlarmUpdatedPath() throws Exception {
        // GIVEN

        // values that changed between existing alarm and updated alarm
        AlarmSeverity oldAlarmSeverity = AlarmSeverity.WARNING;
        AlarmSeverity newAlarmSeverity = AlarmSeverity.MAJOR;

        boolean propagate = true;
        boolean propagateToOwner = false;
        boolean propagateToTenant = false;
        List<String> propagateRelationTypes = List.of("RELATION_TYPE_1", "RELATION_TYPE_2", "RELATION_TYPE_3");
        JsonNode alarmDetails = JacksonUtil.newObjectNode().put("oldAlarmDetailsKey", "oldAlarmDetailsValue");
        long endTs = 200L;

        // node configuration
        config = config.defaultConfiguration();
        config.setUseMessageAlarmData(true);
        config.setOverwriteAlarmDetails(true);

        // other values
        String alarmType = "High Temperature";

        long metadataTs = 1711631716127L;
        metadata.putValue("ts", Long.toString(metadataTs));
        metadata.putValue("location", "Company office");

        var ruleNodeSelfId = new RuleNodeId(Uuids.timeBased());

        // alarm that is inside an incoming message
        var alarmFromIncomingMessage = Alarm.builder()
                .tenantId(tenantId)
                .originator(msgOriginator)
                .cleared(false)
                .acknowledged(false)
                .severity(newAlarmSeverity)
                .propagate(propagate)
                .propagateToOwner(propagateToOwner)
                .propagateToTenant(propagateToTenant)
                .propagateRelationTypes(propagateRelationTypes)
                .type(alarmType)
                .startTs(100L)
                .endTs(endTs)
                .details(alarmDetails)
                .build();

        var incomingMsg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(msgOriginator)
                .copyMetaData(metadata)
                .data(JacksonUtil.toString(alarmFromIncomingMessage))
                .build();

        var existingAlarmId = new AlarmId(Uuids.timeBased());
        var existingActiveAlarm = Alarm.builder()
                .tenantId(tenantId)
                .originator(msgOriginator)
                .cleared(false)
                .acknowledged(false)
                .severity(oldAlarmSeverity)
                .propagate(propagate)
                .propagateToOwner(propagateToOwner)
                .propagateToTenant(propagateToTenant)
                .propagateRelationTypes(propagateRelationTypes)
                .type(alarmType)
                .startTs(100L)
                .endTs(endTs)
                .details(alarmDetails)
                .build();
        existingActiveAlarm.setId(existingAlarmId);

        // expected values
        var expectedUpdatedAlarm = Alarm.builder()
                .tenantId(tenantId)
                .originator(msgOriginator)
                .cleared(false)
                .acknowledged(false)
                .severity(newAlarmSeverity)
                .propagate(propagate)
                .propagateToOwner(propagateToOwner)
                .propagateToTenant(propagateToTenant)
                .propagateRelationTypes(propagateRelationTypes)
                .type(alarmType)
                .startTs(100L)
                .endTs(endTs)
                .details(alarmDetails)
                .build();
        expectedUpdatedAlarm.setId(existingAlarmId);
        var expectedUpdatedAlarmInfo = new AlarmInfo(expectedUpdatedAlarm);

        var expectedUpdateAlarmRequest = AlarmUpdateRequest.builder()
                .tenantId(tenantId)
                .alarmId(existingAlarmId)
                .severity(newAlarmSeverity)
                .startTs(100L)
                .endTs(endTs)
                .details(alarmDetails)
                .propagation(AlarmPropagationInfo.builder()
                        .propagate(propagate)
                        .propagateToOwner(propagateToOwner)
                        .propagateToTenant(propagateToTenant)
                        .propagateRelationTypes(propagateRelationTypes).build())
                .userId(null)
                .build();

        // mocks
        given(ctxMock.getTenantId()).willReturn(tenantId);
        given(ctxMock.getAlarmService()).willReturn(alarmServiceMock);
        given(ctxMock.getSelfId()).willReturn(ruleNodeSelfId);
        given(alarmServiceMock.findLatestActiveByOriginatorAndTypeAsync(tenantId, msgOriginator, alarmType)).willReturn(FluentFuture.from(immediateFuture(existingActiveAlarm)));
        given(alarmDetailsScriptMock.executeJsonAsync(any())).willReturn(immediateFuture(alarmDetails));
        doReturn(endTs).when(nodeSpy).currentTimeMillis();
        var apiCallResult = AlarmApiCallResult.builder()
                .successful(true)
                .created(false)
                .modified(true)
                .cleared(false)
                .deleted(false)
                .alarm(expectedUpdatedAlarmInfo)
                .old(new Alarm(existingActiveAlarm))
                .propagatedEntitiesList(List.of(tenantId))
                .build();
        given(alarmServiceMock.updateAlarm(expectedUpdateAlarmRequest)).willReturn(apiCallResult);
        given(ctxMock.alarmActionMsg(expectedUpdatedAlarmInfo, ruleNodeSelfId, TbMsgType.ENTITY_UPDATED)).willReturn(alarmActionMsgMock);
        given(ctxMock.transformMsg(any(TbMsg.class), any(TbMsgType.class), any(EntityId.class), any(TbMsgMetaData.class), anyString()))
                .willAnswer(answer -> answer.getArgument(0, TbMsg.class).transform()
                        .type(answer.getArgument(1, TbMsgType.class))
                        .originator(answer.getArgument(2, EntityId.class))
                        .metaData(answer.getArgument(3, TbMsgMetaData.class))
                        .data(answer.getArgument(4, String.class))
                        .build()
                );
        given(ctxMock.createScriptEngine(ScriptLanguage.TBEL, config.getAlarmDetailsBuildTbel())).willReturn(alarmDetailsScriptMock);

        // node initialization
        nodeSpy.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        // WHEN
        nodeSpy.onMsg(ctxMock, incomingMsg);

        // THEN

        // verify alarm details script evaluation
        var dummyMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        then(alarmDetailsScriptMock).should().executeJsonAsync(dummyMsgCaptor.capture());
        TbMsg actualDummyMsg = dummyMsgCaptor.getValue();
        assertThat(actualDummyMsg.getType()).isEqualTo(incomingMsg.getType());
        assertThat(actualDummyMsg.getData()).isEqualTo(incomingMsg.getData());
        assertThat(actualDummyMsg.getMetaData().getData()).containsEntry("prevAlarmDetails", JacksonUtil.toString(alarmDetails));

        // verify we called updateAlarm() with correct AlarmUpdateRequest
        then(alarmServiceMock).should().updateAlarm(expectedUpdateAlarmRequest);
        then(alarmServiceMock).should(never()).createAlarm(any());

        // verify that we created a correct alarm action message and enqueued it
        then(ctxMock).should().alarmActionMsg(expectedUpdatedAlarmInfo, ruleNodeSelfId, TbMsgType.ENTITY_UPDATED);
        then(ctxMock).should().enqueue(eq(alarmActionMsgMock), successCaptor.capture(), any());

        // run success captor to emulate successful queueing of an alarm action message and to trigger further processing on the success path
        successCaptor.getValue().run();

        // capture and verify an outgoing message
        var outgoingMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        then(ctxMock).should().tellNext(outgoingMsgCaptor.capture(), eq("Updated"));
        var actualOutgoingMsg = outgoingMsgCaptor.getValue();
        assertThat(actualOutgoingMsg.getType()).isEqualTo(TbMsgType.ALARM.name());
        assertThat(actualOutgoingMsg.getOriginator()).isEqualTo(msgOriginator);
        assertThat(actualOutgoingMsg.getData()).isEqualTo(JacksonUtil.valueToTree(expectedUpdatedAlarmInfo).toString());

        Map<String, String> actualOutgoingMsgMetadataContent = actualOutgoingMsg.getMetaData().getData();
        assertThat(actualOutgoingMsgMetadataContent).containsAllEntriesOf(metadata.getData());
        assertThat(actualOutgoingMsgMetadataContent).containsEntry(DataConstants.IS_EXISTING_ALARM, Boolean.TRUE.toString());
        assertThat(actualOutgoingMsgMetadataContent).size().isEqualTo(metadata.getData().size() + 1);

        // verify wrong processing paths were not taken
        then(ctxMock).should(never()).tellNext(any(), eq(TbNodeConnectionType.FALSE));
        then(ctxMock).should(never()).tellNext(any(), eq("Created"));
        then(ctxMock).should(never()).tellNext(any(), eq("Cleared"));
        then(ctxMock).should(never()).tellSuccess(any());
        then(ctxMock).should(never()).tellFailure(any(), any());
    }

    @Test
    @DisplayName("When the alarm details script throws an exception, " +
            "node should tell failure with that exception, and it should neither create nor update any alarms, nor should it send any other messages.")
    void whenAlarmDetailsScriptThrowsException_thenShouldTellFailureAndNoOtherActions() throws Exception {
        // GIVEN
        config = config.defaultConfiguration();

        var incomingMsg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(msgOriginator)
                .copyMetaData(metadata)
                .data("{\"temperature\": 50}")
                .build();

        given(ctxMock.getTenantId()).willReturn(tenantId);
        given(ctxMock.getAlarmService()).willReturn(alarmServiceMock);
        given(ctxMock.createScriptEngine(ScriptLanguage.TBEL, config.getAlarmDetailsBuildTbel())).willReturn(alarmDetailsScriptMock);
        given(alarmServiceMock.findLatestActiveByOriginatorAndTypeAsync(tenantId, msgOriginator, config.getAlarmType())).willReturn(FluentFuture.from(immediateFuture(null)));

        var expectedException = new ExecutionException("Failed to execute script.", new RuntimeException("Something went wrong!"));
        given(alarmDetailsScriptMock.executeJsonAsync(incomingMsg)).willReturn(immediateFailedFuture(expectedException));

        nodeSpy.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        // WHEN
        nodeSpy.onMsg(ctxMock, incomingMsg);

        // THEN
        var exceptionCaptor = ArgumentCaptor.forClass(Throwable.class);
        then(ctxMock).should().tellFailure(eq(incomingMsg), exceptionCaptor.capture());
        Throwable actualException = exceptionCaptor.getValue();
        assertThat(actualException).isEqualTo(expectedException);

        then(alarmServiceMock).should(never()).createAlarm(any());
        then(alarmServiceMock).should(never()).updateAlarm(any());

        then(ctxMock).should(never()).tellNext(any(), eq(TbNodeConnectionType.FALSE));
        then(ctxMock).should(never()).tellNext(any(), eq("Created"));
        then(ctxMock).should(never()).tellNext(any(), eq("Updated"));
        then(ctxMock).should(never()).tellNext(any(), eq("Cleared"));
        then(ctxMock).should(never()).tellSuccess(any());
    }

}
