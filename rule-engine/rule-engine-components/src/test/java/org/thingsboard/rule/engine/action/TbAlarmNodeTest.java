/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
import com.google.common.util.concurrent.ListenableFuture;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ListeningExecutor;
import org.thingsboard.rule.engine.api.RuleEngineAlarmService;
import org.thingsboard.rule.engine.api.ScriptEngine;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmApiCallResult;
import org.thingsboard.server.common.data.alarm.AlarmCreateOrUpdateActiveRequest;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmUpdateRequest;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.script.ScriptLanguage;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import javax.script.ScriptException;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.thingsboard.server.common.data.DataConstants.IS_CLEARED_ALARM;
import static org.thingsboard.server.common.data.DataConstants.IS_EXISTING_ALARM;
import static org.thingsboard.server.common.data.DataConstants.IS_NEW_ALARM;
import static org.thingsboard.server.common.data.alarm.AlarmSeverity.CRITICAL;
import static org.thingsboard.server.common.data.alarm.AlarmSeverity.WARNING;

@RunWith(MockitoJUnitRunner.class)
public class TbAlarmNodeTest {

    private TbAbstractAlarmNode node;

    @Mock
    private TbContext ctx;
    @Mock
    private RuleEngineAlarmService alarmService;

    @Mock
    private ScriptEngine detailsJs;

    @Captor
    private ArgumentCaptor<Runnable> successCaptor;
    @Captor
    private ArgumentCaptor<Consumer<Throwable>> failureCaptor;

    private final RuleChainId ruleChainId = new RuleChainId(Uuids.timeBased());
    private final RuleNodeId ruleNodeId = new RuleNodeId(Uuids.timeBased());

    private ListeningExecutor dbExecutor;

    private final EntityId originator = new DeviceId(Uuids.timeBased());
    private final EntityId alarmOriginator = new AlarmId(Uuids.timeBased());
    private final TenantId tenantId = TenantId.fromUUID(Uuids.timeBased());
    private final TbMsgMetaData metaData = new TbMsgMetaData();
    private final String rawJson = "{\"name\": \"Vit\", \"passed\": 5}";

    @Before
    public void before() {
        dbExecutor = new ListeningExecutor() {
            @Override
            public <T> ListenableFuture<T> executeAsync(Callable<T> task) {
                try {
                    return Futures.immediateFuture(task.call());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void execute(Runnable command) {
                command.run();
            }
        };
    }

    @Test
    public void newAlarmCanBeCreated() throws ScriptException, IOException {
        initWithCreateAlarmScript();
        metaData.putValue("key", "value");
        TbMsg msg = TbMsg.newMsg("USER", originator, metaData, TbMsgDataType.JSON, rawJson, ruleChainId, ruleNodeId);
        long ts = msg.getTs();

        when(detailsJs.executeJsonAsync(msg)).thenReturn(Futures.immediateFuture(null));
        when(alarmService.findLatestActiveByOriginatorAndType(tenantId, originator, "SomeType")).thenReturn(null);
        Alarm expectedAlarm = Alarm.builder()
                .startTs(ts)
                .endTs(ts)
                .tenantId(tenantId)
                .originator(originator)
                .severity(CRITICAL)
                .propagate(true)
                .type("SomeType")
                .details(null)
                .build();
        when(alarmService.createAlarm(any(AlarmCreateOrUpdateActiveRequest.class))).thenReturn(
                AlarmApiCallResult.builder()
                        .created(true)
                        .alarm(new AlarmInfo(expectedAlarm))
                        .build());

        node.onMsg(ctx, msg);

        verify(ctx).enqueue(any(), successCaptor.capture(), failureCaptor.capture());
        successCaptor.getValue().run();
        verify(ctx).tellNext(any(), eq("Created"));

        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<String> typeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<EntityId> originatorCaptor = ArgumentCaptor.forClass(EntityId.class);
        ArgumentCaptor<TbMsgMetaData> metadataCaptor = ArgumentCaptor.forClass(TbMsgMetaData.class);
        ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(ctx).transformMsg(msgCaptor.capture(), typeCaptor.capture(), originatorCaptor.capture(), metadataCaptor.capture(), dataCaptor.capture());

        assertEquals("ALARM", typeCaptor.getValue());
        assertEquals(originator, originatorCaptor.getValue());
        assertEquals("value", metadataCaptor.getValue().getValue("key"));
        assertEquals(Boolean.TRUE.toString(), metadataCaptor.getValue().getValue(IS_NEW_ALARM));
        assertNotSame(metaData, metadataCaptor.getValue());

        Alarm actualAlarm = JacksonUtil.fromBytes(dataCaptor.getValue().getBytes(), Alarm.class);
        assertEquals(expectedAlarm, actualAlarm);
    }

    @Test
    public void buildDetailsThrowsException() throws ScriptException, IOException {
        initWithCreateAlarmScript();
        metaData.putValue("key", "value");
        TbMsg msg = TbMsg.newMsg("USER", originator, metaData, TbMsgDataType.JSON, rawJson, ruleChainId, ruleNodeId);

        when(detailsJs.executeJsonAsync(msg)).thenReturn(Futures.immediateFailedFuture(new NotImplementedException("message")));
        when(alarmService.findLatestActiveByOriginatorAndType(tenantId, originator, "SomeType")).thenReturn(null);

        node.onMsg(ctx, msg);

        verifyError(msg, "message", NotImplementedException.class);

        verify(ctx).createScriptEngine(ScriptLanguage.JS, "DETAILS");
        verify(ctx).getAlarmService();
        verify(ctx, times(2)).getDbCallbackExecutor();
        verify(ctx).logJsEvalRequest();
        verify(ctx).getTenantId();
        verify(alarmService).findLatestActiveByOriginatorAndType(tenantId, originator, "SomeType");

        verifyNoMoreInteractions(ctx, alarmService);
    }

    @Test
    public void ifAlarmClearedCreateNew() throws ScriptException, IOException {
        initWithCreateAlarmScript();
        metaData.putValue("key", "value");
        TbMsg msg = TbMsg.newMsg("USER", originator, metaData, TbMsgDataType.JSON, rawJson, ruleChainId, ruleNodeId);
        long ts = msg.getTs();
        Alarm clearedAlarm = Alarm.builder().cleared(true).acknowledged(true).build();

        when(detailsJs.executeJsonAsync(msg)).thenReturn(Futures.immediateFuture(null));
        when(alarmService.findLatestActiveByOriginatorAndType(tenantId, originator, "SomeType")).thenReturn(clearedAlarm);

        Alarm expectedAlarm = Alarm.builder()
                .startTs(ts)
                .endTs(ts)
                .tenantId(tenantId)
                .originator(originator)
                .severity(CRITICAL)
                .propagate(true)
                .type("SomeType")
                .details(null)
                .build();
        when(alarmService.createAlarm(any(AlarmCreateOrUpdateActiveRequest.class))).thenReturn(
                AlarmApiCallResult.builder()
                        .successful(true)
                        .created(true)
                        .alarm(new AlarmInfo(expectedAlarm))
                        .build());

        node.onMsg(ctx, msg);

        verify(ctx).enqueue(any(), successCaptor.capture(), failureCaptor.capture());
        successCaptor.getValue().run();
        verify(ctx).tellNext(any(), eq("Created"));

        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<String> typeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<EntityId> originatorCaptor = ArgumentCaptor.forClass(EntityId.class);
        ArgumentCaptor<TbMsgMetaData> metadataCaptor = ArgumentCaptor.forClass(TbMsgMetaData.class);
        ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(ctx).transformMsg(msgCaptor.capture(), typeCaptor.capture(), originatorCaptor.capture(), metadataCaptor.capture(), dataCaptor.capture());

        assertEquals("ALARM", typeCaptor.getValue());
        assertEquals(originator, originatorCaptor.getValue());
        assertEquals("value", metadataCaptor.getValue().getValue("key"));
        assertEquals(Boolean.TRUE.toString(), metadataCaptor.getValue().getValue(IS_NEW_ALARM));
        assertNotSame(metaData, metadataCaptor.getValue());


        Alarm actualAlarm = JacksonUtil.fromBytes(dataCaptor.getValue().getBytes(), Alarm.class);
        assertEquals(expectedAlarm, actualAlarm);
    }

    @Test
    public void alarmCanBeUpdated() throws IOException {
        initWithCreateAlarmScript();
        metaData.putValue("key", "value");
        TbMsg msg = TbMsg.newMsg("USER", originator, metaData, TbMsgDataType.JSON, rawJson, ruleChainId, ruleNodeId);

        long oldEndDate = System.currentTimeMillis();
        Alarm activeAlarm = Alarm.builder().type("SomeType").tenantId(tenantId).originator(originator).severity(WARNING).endTs(oldEndDate).build();

        when(detailsJs.executeJsonAsync(msg)).thenReturn(Futures.immediateFuture(null));
        when(alarmService.findLatestActiveByOriginatorAndType(tenantId, originator, "SomeType")).thenReturn(activeAlarm);

        Alarm expectedAlarm = Alarm.builder()
                .tenantId(tenantId)
                .originator(originator)
                .severity(CRITICAL)
                .propagate(true)
                .type("SomeType")
                .details(null)
                .endTs(activeAlarm.getEndTs())
                .build();
        when(alarmService.updateAlarm(any(AlarmUpdateRequest.class))).thenReturn(
                AlarmApiCallResult.builder()
                        .successful(true)
                        .modified(true)
                        .alarm(new AlarmInfo(expectedAlarm))
                        .build());
        node.onMsg(ctx, msg);

        verify(ctx).enqueue(any(), successCaptor.capture(), failureCaptor.capture());
        successCaptor.getValue().run();
        verify(ctx).tellNext(any(), eq("Updated"));

        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<String> typeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<EntityId> originatorCaptor = ArgumentCaptor.forClass(EntityId.class);
        ArgumentCaptor<TbMsgMetaData> metadataCaptor = ArgumentCaptor.forClass(TbMsgMetaData.class);
        ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(ctx).transformMsg(msgCaptor.capture(), typeCaptor.capture(), originatorCaptor.capture(), metadataCaptor.capture(), dataCaptor.capture());

        assertEquals("ALARM", typeCaptor.getValue());
        assertEquals(originator, originatorCaptor.getValue());
        assertEquals("value", metadataCaptor.getValue().getValue("key"));
        assertEquals(Boolean.TRUE.toString(), metadataCaptor.getValue().getValue(IS_EXISTING_ALARM));
        assertNotSame(metaData, metadataCaptor.getValue());

        Alarm actualAlarm = JacksonUtil.fromBytes(dataCaptor.getValue().getBytes(), Alarm.class);
        assertTrue(activeAlarm.getEndTs() >= oldEndDate);
        assertEquals(expectedAlarm, actualAlarm);
    }

    @Test
    public void alarmCanBeCleared() throws ScriptException, IOException {
        initWithClearAlarmScript();
        metaData.putValue("key", "value");
        TbMsg msg = TbMsg.newMsg("USER", originator, metaData, TbMsgDataType.JSON, rawJson, ruleChainId, ruleNodeId);

        long oldEndDate = System.currentTimeMillis();
        Alarm activeAlarm = Alarm.builder().type("SomeType").tenantId(tenantId).originator(originator).severity(WARNING).endTs(oldEndDate).build();

        Alarm expectedAlarm = Alarm.builder()
                .tenantId(tenantId)
                .originator(originator)
                .cleared(true)
                .severity(WARNING)
                .propagate(false)
                .type("SomeType")
                .details(null)
                .endTs(oldEndDate)
                .build();

        when(detailsJs.executeJsonAsync(msg)).thenReturn(Futures.immediateFuture(null));
        when(alarmService.findLatestActiveByOriginatorAndType(tenantId, originator, "SomeType")).thenReturn(activeAlarm);
        when(alarmService.clearAlarm(eq(activeAlarm.getTenantId()), eq(activeAlarm.getId()), anyLong(), nullable(JsonNode.class)))
                .thenReturn(AlarmApiCallResult.builder()
                        .successful(true)
                        .cleared(true)
                        .alarm(new AlarmInfo(expectedAlarm))
                        .build());

        node.onMsg(ctx, msg);

        verify(ctx).enqueue(any(), successCaptor.capture(), failureCaptor.capture());
        successCaptor.getValue().run();
        verify(ctx).tellNext(any(), eq("Cleared"));

        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<String> typeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<EntityId> originatorCaptor = ArgumentCaptor.forClass(EntityId.class);
        ArgumentCaptor<TbMsgMetaData> metadataCaptor = ArgumentCaptor.forClass(TbMsgMetaData.class);
        ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(ctx).transformMsg(msgCaptor.capture(), typeCaptor.capture(), originatorCaptor.capture(), metadataCaptor.capture(), dataCaptor.capture());

        assertEquals("ALARM", typeCaptor.getValue());
        assertEquals(originator, originatorCaptor.getValue());
        assertEquals("value", metadataCaptor.getValue().getValue("key"));
        assertEquals(Boolean.TRUE.toString(), metadataCaptor.getValue().getValue(IS_CLEARED_ALARM));
        assertNotSame(metaData, metadataCaptor.getValue());

        Alarm actualAlarm = JacksonUtil.fromBytes(dataCaptor.getValue().getBytes(), Alarm.class);
        assertEquals(expectedAlarm, actualAlarm);
    }

    @Test
    public void alarmCanBeClearedWithAlarmOriginator() throws ScriptException, IOException {
        initWithClearAlarmScript();
        metaData.putValue("key", "value");
        TbMsg msg = TbMsg.newMsg("USER", alarmOriginator, metaData, TbMsgDataType.JSON, rawJson, ruleChainId, ruleNodeId);

        long oldEndDate = System.currentTimeMillis();
        AlarmId id = new AlarmId(alarmOriginator.getId());
        Alarm activeAlarm = Alarm.builder().type("SomeType").tenantId(tenantId).originator(originator).severity(WARNING).endTs(oldEndDate).build();
        activeAlarm.setId(id);

        Alarm expectedAlarm = Alarm.builder()
                .tenantId(tenantId)
                .originator(originator)
                .cleared(true)
                .severity(WARNING)
                .propagate(false)
                .type("SomeType")
                .details(null)
                .endTs(oldEndDate)
                .build();
        expectedAlarm.setId(id);

        when(detailsJs.executeJsonAsync(msg)).thenReturn(Futures.immediateFuture(null));
        when(alarmService.findAlarmById(tenantId, id)).thenReturn(activeAlarm);
        when(alarmService.clearAlarm(eq(activeAlarm.getTenantId()), eq(activeAlarm.getId()), anyLong(), nullable(JsonNode.class)))
                .thenReturn(AlarmApiCallResult.builder()
                        .successful(true)
                        .cleared(true)
                        .alarm(new AlarmInfo(expectedAlarm))
                        .build());

        node.onMsg(ctx, msg);

        verify(ctx).enqueue(any(), successCaptor.capture(), failureCaptor.capture());
        successCaptor.getValue().run();
        verify(ctx).tellNext(any(), eq("Cleared"));

        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<String> typeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<EntityId> originatorCaptor = ArgumentCaptor.forClass(EntityId.class);
        ArgumentCaptor<TbMsgMetaData> metadataCaptor = ArgumentCaptor.forClass(TbMsgMetaData.class);
        ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(ctx).transformMsg(msgCaptor.capture(), typeCaptor.capture(), originatorCaptor.capture(), metadataCaptor.capture(), dataCaptor.capture());

        assertEquals("ALARM", typeCaptor.getValue());
        assertEquals(alarmOriginator, originatorCaptor.getValue());
        assertEquals("value", metadataCaptor.getValue().getValue("key"));
        assertEquals(Boolean.TRUE.toString(), metadataCaptor.getValue().getValue(IS_CLEARED_ALARM));
        assertNotSame(metaData, metadataCaptor.getValue());

        Alarm actualAlarm = JacksonUtil.fromBytes(dataCaptor.getValue().getBytes(), Alarm.class);
        assertEquals(expectedAlarm, actualAlarm);
    }

    @Test
    public void testCreateAlarmWithDynamicSeverityFromMessageBody() throws Exception {
        TbCreateAlarmNodeConfiguration config = new TbCreateAlarmNodeConfiguration();
        config.setPropagate(true);
        config.setSeverity("$[alarmSeverity]");
        config.setAlarmType("SomeType");
        config.setScriptLang(ScriptLanguage.JS);
        config.setAlarmDetailsBuildJs("DETAILS");
        config.setDynamicSeverity(true);
        TbNodeConfiguration nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));

        when(ctx.createScriptEngine(ScriptLanguage.JS, "DETAILS")).thenReturn(detailsJs);

        when(ctx.getTenantId()).thenReturn(tenantId);
        when(ctx.getAlarmService()).thenReturn(alarmService);
        when(ctx.getDbCallbackExecutor()).thenReturn(dbExecutor);

        node = new TbCreateAlarmNode();
        node.init(ctx, nodeConfiguration);

        String rawJson = "{\"alarmSeverity\": \"WARNING\", \"passed\": 5}";
        metaData.putValue("key", "value");
        TbMsg msg = TbMsg.newMsg("USER", originator, metaData, TbMsgDataType.JSON, rawJson, ruleChainId, ruleNodeId);
        long ts = msg.getTs();
        Alarm expectedAlarm = Alarm.builder()
                .startTs(ts)
                .endTs(ts)
                .tenantId(tenantId)
                .originator(originator)
                .severity(WARNING)
                .propagate(true)
                .type("SomeType")
                .details(null)
                .build();

        when(detailsJs.executeJsonAsync(msg)).thenReturn(Futures.immediateFuture(null));
        when(alarmService.findLatestActiveByOriginatorAndType(tenantId, originator, "SomeType")).thenReturn(null);
        when(alarmService.createAlarm(any(AlarmCreateOrUpdateActiveRequest.class))).thenReturn(
                AlarmApiCallResult.builder()
                        .successful(true)
                        .created(true)
                        .alarm(new AlarmInfo(expectedAlarm))
                        .build());

        node.onMsg(ctx, msg);

        verify(ctx).enqueue(any(), successCaptor.capture(), failureCaptor.capture());
        successCaptor.getValue().run();
        verify(ctx).tellNext(any(), eq("Created"));

        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<String> typeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<EntityId> originatorCaptor = ArgumentCaptor.forClass(EntityId.class);
        ArgumentCaptor<TbMsgMetaData> metadataCaptor = ArgumentCaptor.forClass(TbMsgMetaData.class);
        ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(ctx).transformMsg(msgCaptor.capture(), typeCaptor.capture(), originatorCaptor.capture(), metadataCaptor.capture(), dataCaptor.capture());

        assertEquals("ALARM", typeCaptor.getValue());
        assertEquals(originator, originatorCaptor.getValue());
        assertEquals("value", metadataCaptor.getValue().getValue("key"));
        assertEquals(Boolean.TRUE.toString(), metadataCaptor.getValue().getValue(IS_NEW_ALARM));
        assertNotSame(metaData, metadataCaptor.getValue());

        Alarm actualAlarm = JacksonUtil.fromBytes(dataCaptor.getValue().getBytes(), Alarm.class);
        assertEquals(expectedAlarm, actualAlarm);
    }

    @Test
    public void testCreateAlarmWithDynamicSeverityFromMetadata() throws Exception {
        TbCreateAlarmNodeConfiguration config = new TbCreateAlarmNodeConfiguration();
        config.setPropagate(true);
        config.setScriptLang(ScriptLanguage.JS);
        config.setSeverity("${alarmSeverity}");
        config.setAlarmType("SomeType");
        config.setAlarmDetailsBuildJs("DETAILS");
        config.setDynamicSeverity(true);
        TbNodeConfiguration nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));

        when(ctx.createScriptEngine(ScriptLanguage.JS, "DETAILS")).thenReturn(detailsJs);

        when(ctx.getTenantId()).thenReturn(tenantId);
        when(ctx.getAlarmService()).thenReturn(alarmService);
        when(ctx.getDbCallbackExecutor()).thenReturn(dbExecutor);

        node = new TbCreateAlarmNode();
        node.init(ctx, nodeConfiguration);

        metaData.putValue("alarmSeverity", "WARNING");
        TbMsg msg = TbMsg.newMsg("USER", originator, metaData, TbMsgDataType.JSON, rawJson, ruleChainId, ruleNodeId);
        long ts = msg.getTs();
        Alarm expectedAlarm = Alarm.builder()
                .startTs(ts)
                .endTs(ts)
                .tenantId(tenantId)
                .originator(originator)
                .severity(WARNING)
                .propagate(true)
                .type("SomeType")
                .details(null)
                .build();

        when(detailsJs.executeJsonAsync(msg)).thenReturn(Futures.immediateFuture(null));
        when(alarmService.findLatestActiveByOriginatorAndType(tenantId, originator, "SomeType")).thenReturn(null);
        when(alarmService.createAlarm(any(AlarmCreateOrUpdateActiveRequest.class))).thenReturn(
                AlarmApiCallResult.builder()
                        .successful(true)
                        .created(true)
                        .alarm(new AlarmInfo(expectedAlarm))
                        .build());

        node.onMsg(ctx, msg);

        verify(ctx).enqueue(any(), successCaptor.capture(), failureCaptor.capture());
        successCaptor.getValue().run();
        verify(ctx).tellNext(any(), eq("Created"));

        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<String> typeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<EntityId> originatorCaptor = ArgumentCaptor.forClass(EntityId.class);
        ArgumentCaptor<TbMsgMetaData> metadataCaptor = ArgumentCaptor.forClass(TbMsgMetaData.class);
        ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(ctx).transformMsg(msgCaptor.capture(), typeCaptor.capture(), originatorCaptor.capture(), metadataCaptor.capture(), dataCaptor.capture());

        assertEquals("ALARM", typeCaptor.getValue());
        assertEquals(originator, originatorCaptor.getValue());
        assertEquals(Boolean.TRUE.toString(), metadataCaptor.getValue().getValue(IS_NEW_ALARM));
        assertNotSame(metaData, metadataCaptor.getValue());

        Alarm actualAlarm = JacksonUtil.fromBytes(dataCaptor.getValue().getBytes(), Alarm.class);
        assertEquals(expectedAlarm, actualAlarm);
    }

    @Test
    public void testCreateAlarmsWithPropagationToTenantWithDynamicTypes() throws Exception {
        for (int i = 0; i < 10; i++) {
            var config = new TbCreateAlarmNodeConfiguration();
            config.setPropagateToTenant(true);
            config.setSeverity(CRITICAL.name());
            config.setAlarmType("SomeType" + i);
            config.setScriptLang(ScriptLanguage.JS);
            config.setAlarmDetailsBuildJs("DETAILS");
            config.setDynamicSeverity(true);
            TbNodeConfiguration nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));

            when(ctx.createScriptEngine(ScriptLanguage.JS, "DETAILS")).thenReturn(detailsJs);

            when(ctx.getTenantId()).thenReturn(tenantId);
            when(ctx.getAlarmService()).thenReturn(alarmService);
            when(ctx.getDbCallbackExecutor()).thenReturn(dbExecutor);

            node = new TbCreateAlarmNode();
            node.init(ctx, nodeConfiguration);

            metaData.putValue("key", "value");
            TbMsg msg = TbMsg.newMsg("USER", originator, metaData, TbMsgDataType.JSON, rawJson, ruleChainId, ruleNodeId);
            long ts = msg.getTs();
            Alarm expectedAlarm = Alarm.builder()
                    .startTs(ts)
                    .endTs(ts)
                    .tenantId(tenantId)
                    .originator(originator)
                    .severity(CRITICAL)
                    .propagateToTenant(true)
                    .type("SomeType" + i)
                    .details(null)
                    .build();

            when(detailsJs.executeJsonAsync(msg)).thenReturn(Futures.immediateFuture(null));
            when(alarmService.findLatestActiveByOriginatorAndType(tenantId, originator, "SomeType" + i)).thenReturn(null);
            when(alarmService.createAlarm(any(AlarmCreateOrUpdateActiveRequest.class))).thenReturn(
                    AlarmApiCallResult.builder()
                            .successful(true)
                            .created(true)
                            .alarm(new AlarmInfo(expectedAlarm))
                            .build());
            node.onMsg(ctx, msg);

            verify(ctx, atMost(10)).enqueue(any(), successCaptor.capture(), failureCaptor.capture());
            successCaptor.getValue().run();
            verify(ctx, atMost(10)).tellNext(any(), eq("Created"));

            ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
            ArgumentCaptor<String> typeCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<EntityId> originatorCaptor = ArgumentCaptor.forClass(EntityId.class);
            ArgumentCaptor<TbMsgMetaData> metadataCaptor = ArgumentCaptor.forClass(TbMsgMetaData.class);
            ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
            verify(ctx, atMost(10)).transformMsg(msgCaptor.capture(), typeCaptor.capture(), originatorCaptor.capture(), metadataCaptor.capture(), dataCaptor.capture());

            assertEquals("ALARM", typeCaptor.getValue());
            assertEquals(originator, originatorCaptor.getValue());
            assertEquals("value", metadataCaptor.getValue().getValue("key"));
            assertEquals(Boolean.TRUE.toString(), metadataCaptor.getValue().getValue(IS_NEW_ALARM));
            assertNotSame(metaData, metadataCaptor.getValue());

            Alarm actualAlarm = JacksonUtil.fromBytes(dataCaptor.getValue().getBytes(), Alarm.class);
            assertEquals(expectedAlarm, actualAlarm);
        }
    }

    private void initWithCreateAlarmScript() {
        try {
            TbCreateAlarmNodeConfiguration config = new TbCreateAlarmNodeConfiguration();
            config.setPropagate(true);
            config.setSeverity(CRITICAL.name());
            config.setAlarmType("SomeType");
            config.setScriptLang(ScriptLanguage.JS);
            config.setAlarmDetailsBuildJs("DETAILS");
            TbNodeConfiguration nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));

            when(ctx.createScriptEngine(ScriptLanguage.JS, "DETAILS")).thenReturn(detailsJs);

            when(ctx.getTenantId()).thenReturn(tenantId);
            when(ctx.getAlarmService()).thenReturn(alarmService);
            when(ctx.getDbCallbackExecutor()).thenReturn(dbExecutor);

            node = new TbCreateAlarmNode();
            node.init(ctx, nodeConfiguration);
        } catch (TbNodeException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private void initWithClearAlarmScript() {
        try {
            TbClearAlarmNodeConfiguration config = new TbClearAlarmNodeConfiguration();
            config.setAlarmType("SomeType");
            config.setScriptLang(ScriptLanguage.JS);
            config.setAlarmDetailsBuildJs("DETAILS");
            TbNodeConfiguration nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));

            when(ctx.createScriptEngine(ScriptLanguage.JS, "DETAILS")).thenReturn(detailsJs);

            when(ctx.getTenantId()).thenReturn(tenantId);
            when(ctx.getAlarmService()).thenReturn(alarmService);
            when(ctx.getDbCallbackExecutor()).thenReturn(dbExecutor);

            node = new TbClearAlarmNode();
            node.init(ctx, nodeConfiguration);
        } catch (TbNodeException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private void verifyError(TbMsg msg, String message, Class expectedClass) {
        ArgumentCaptor<Throwable> captor = ArgumentCaptor.forClass(Throwable.class);
        verify(ctx).tellFailure(same(msg), captor.capture());

        Throwable value = captor.getValue();
        assertEquals(expectedClass, value.getClass());
        assertEquals(message, value.getMessage());
    }

}
