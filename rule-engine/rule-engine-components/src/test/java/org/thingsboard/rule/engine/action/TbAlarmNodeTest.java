/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.thingsboard.common.util.ListeningExecutor;
import org.thingsboard.rule.engine.api.RuleEngineAlarmService;
import org.thingsboard.rule.engine.api.ScriptEngine;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.alarm.AlarmOperationResult;
import org.thingsboard.server.dao.alarm.AlarmService;

import javax.script.ScriptException;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.thingsboard.rule.engine.action.TbAbstractAlarmNode.IS_CLEARED_ALARM;
import static org.thingsboard.rule.engine.action.TbAbstractAlarmNode.IS_EXISTING_ALARM;
import static org.thingsboard.rule.engine.action.TbAbstractAlarmNode.IS_NEW_ALARM;
import static org.thingsboard.server.common.data.alarm.AlarmSeverity.CRITICAL;
import static org.thingsboard.server.common.data.alarm.AlarmSeverity.WARNING;
import static org.thingsboard.server.common.data.alarm.AlarmStatus.ACTIVE_UNACK;
import static org.thingsboard.server.common.data.alarm.AlarmStatus.CLEARED_ACK;
import static org.thingsboard.server.common.data.alarm.AlarmStatus.CLEARED_UNACK;

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

    private RuleChainId ruleChainId = new RuleChainId(Uuids.timeBased());
    private RuleNodeId ruleNodeId = new RuleNodeId(Uuids.timeBased());

    private ListeningExecutor dbExecutor;

    private EntityId originator = new DeviceId(Uuids.timeBased());
    private TenantId tenantId = new TenantId(Uuids.timeBased());
    private TbMsgMetaData metaData = new TbMsgMetaData();
    private String rawJson = "{\"name\": \"Vit\", \"passed\": 5}";

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

        when(detailsJs.executeJsonAsync(msg)).thenReturn(Futures.immediateFuture(null));
        when(alarmService.findLatestByOriginatorAndType(tenantId, originator, "SomeType")).thenReturn(Futures.immediateFuture(null));
        doAnswer((Answer<Alarm>) invocationOnMock -> (Alarm) (invocationOnMock.getArguments())[0]).when(alarmService).createOrUpdateAlarm(any(Alarm.class));

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

        Alarm actualAlarm = new ObjectMapper().readValue(dataCaptor.getValue().getBytes(), Alarm.class);
        Alarm expectedAlarm = Alarm.builder()
                .tenantId(tenantId)
                .originator(originator)
                .status(ACTIVE_UNACK)
                .severity(CRITICAL)
                .propagate(true)
                .type("SomeType")
                .details(null)
                .build();

        assertEquals(expectedAlarm, actualAlarm);
    }

    @Test
    public void buildDetailsThrowsException() throws ScriptException, IOException {
        initWithCreateAlarmScript();
        metaData.putValue("key", "value");
        TbMsg msg = TbMsg.newMsg("USER", originator, metaData, TbMsgDataType.JSON, rawJson, ruleChainId, ruleNodeId);

        when(detailsJs.executeJsonAsync(msg)).thenReturn(Futures.immediateFailedFuture(new NotImplementedException("message")));
        when(alarmService.findLatestByOriginatorAndType(tenantId, originator, "SomeType")).thenReturn(Futures.immediateFuture(null));

        node.onMsg(ctx, msg);

        verifyError(msg, "message", NotImplementedException.class);

        verify(ctx).createJsScriptEngine("DETAILS");
        verify(ctx).getAlarmService();
        verify(ctx, times(3)).getDbCallbackExecutor();
        verify(ctx).logJsEvalRequest();
        verify(ctx).getTenantId();
        verify(alarmService).findLatestByOriginatorAndType(tenantId, originator, "SomeType");

        verifyNoMoreInteractions(ctx, alarmService);
    }

    @Test
    public void ifAlarmClearedCreateNew() throws ScriptException, IOException {
        initWithCreateAlarmScript();
        metaData.putValue("key", "value");
        TbMsg msg = TbMsg.newMsg("USER", originator, metaData, TbMsgDataType.JSON, rawJson, ruleChainId, ruleNodeId);

        Alarm clearedAlarm = Alarm.builder().status(CLEARED_ACK).build();

        when(detailsJs.executeJsonAsync(msg)).thenReturn(Futures.immediateFuture(null));
        when(alarmService.findLatestByOriginatorAndType(tenantId, originator, "SomeType")).thenReturn(Futures.immediateFuture(clearedAlarm));

        doAnswer((Answer<Alarm>) invocationOnMock -> (Alarm) (invocationOnMock.getArguments())[0]).when(alarmService).createOrUpdateAlarm(any(Alarm.class));

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


        Alarm actualAlarm = new ObjectMapper().readValue(dataCaptor.getValue().getBytes(), Alarm.class);
        Alarm expectedAlarm = Alarm.builder()
                .tenantId(tenantId)
                .originator(originator)
                .status(ACTIVE_UNACK)
                .severity(CRITICAL)
                .propagate(true)
                .type("SomeType")
                .details(null)
                .build();

        assertEquals(expectedAlarm, actualAlarm);
    }

    @Test
    public void alarmCanBeUpdated() throws ScriptException, IOException {
        initWithCreateAlarmScript();
        metaData.putValue("key", "value");
        TbMsg msg = TbMsg.newMsg("USER", originator, metaData, TbMsgDataType.JSON, rawJson, ruleChainId, ruleNodeId);

        long oldEndDate = System.currentTimeMillis();
        Alarm activeAlarm = Alarm.builder().type("SomeType").tenantId(tenantId).originator(originator).status(ACTIVE_UNACK).severity(WARNING).endTs(oldEndDate).build();

        when(detailsJs.executeJsonAsync(msg)).thenReturn(Futures.immediateFuture(null));
        when(alarmService.findLatestByOriginatorAndType(tenantId, originator, "SomeType")).thenReturn(Futures.immediateFuture(activeAlarm));

        doAnswer((Answer<Alarm>) invocationOnMock -> (Alarm) (invocationOnMock.getArguments())[0]).when(alarmService).createOrUpdateAlarm(activeAlarm);

        node.onMsg(ctx, msg);

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

        Alarm actualAlarm = new ObjectMapper().readValue(dataCaptor.getValue().getBytes(), Alarm.class);
        assertTrue(activeAlarm.getEndTs() > oldEndDate);
        Alarm expectedAlarm = Alarm.builder()
                .tenantId(tenantId)
                .originator(originator)
                .status(ACTIVE_UNACK)
                .severity(CRITICAL)
                .propagate(true)
                .type("SomeType")
                .details(null)
                .endTs(activeAlarm.getEndTs())
                .build();

        assertEquals(expectedAlarm, actualAlarm);
    }

    @Test
    public void alarmCanBeCleared() throws ScriptException, IOException {
        initWithClearAlarmScript();
        metaData.putValue("key", "value");
        TbMsg msg = TbMsg.newMsg( "USER", originator, metaData, TbMsgDataType.JSON, rawJson, ruleChainId, ruleNodeId);

        long oldEndDate = System.currentTimeMillis();
        Alarm activeAlarm = Alarm.builder().type("SomeType").tenantId(tenantId).originator(originator).status(ACTIVE_UNACK).severity(WARNING).endTs(oldEndDate).build();

        when(detailsJs.executeJsonAsync(msg)).thenReturn(Futures.immediateFuture(null));
        when(alarmService.findLatestByOriginatorAndType(tenantId, originator, "SomeType")).thenReturn(Futures.immediateFuture(activeAlarm));
        when(alarmService.clearAlarm(eq(activeAlarm.getTenantId()), eq(activeAlarm.getId()), org.mockito.Mockito.any(JsonNode.class), anyLong()))
                .thenReturn(Futures.immediateFuture( false));
        when(alarmService.findAlarmByIdAsync(eq(activeAlarm.getTenantId()), eq(activeAlarm.getId()))).thenReturn(Futures.immediateFuture(activeAlarm));
//        doAnswer((Answer<Alarm>) invocationOnMock -> (Alarm) (invocationOnMock.getArguments())[0]).when(alarmService).createOrUpdateAlarm(activeAlarm);

        node.onMsg(ctx, msg);

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

        Alarm actualAlarm = new ObjectMapper().readValue(dataCaptor.getValue().getBytes(), Alarm.class);
        Alarm expectedAlarm = Alarm.builder()
                .tenantId(tenantId)
                .originator(originator)
                .status(CLEARED_UNACK)
                .severity(WARNING)
                .propagate(false)
                .type("SomeType")
                .details(null)
                .endTs(oldEndDate)
                .build();

        assertEquals(expectedAlarm, actualAlarm);
    }

    private void initWithCreateAlarmScript() {
        try {
            TbCreateAlarmNodeConfiguration config = new TbCreateAlarmNodeConfiguration();
            config.setPropagate(true);
            config.setSeverity(CRITICAL);
            config.setAlarmType("SomeType");
            config.setAlarmDetailsBuildJs("DETAILS");
            ObjectMapper mapper = new ObjectMapper();
            TbNodeConfiguration nodeConfiguration = new TbNodeConfiguration(mapper.valueToTree(config));

            when(ctx.createJsScriptEngine("DETAILS")).thenReturn(detailsJs);

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
            config.setAlarmDetailsBuildJs("DETAILS");
            ObjectMapper mapper = new ObjectMapper();
            TbNodeConfiguration nodeConfiguration = new TbNodeConfiguration(mapper.valueToTree(config));

            when(ctx.createJsScriptEngine("DETAILS")).thenReturn(detailsJs);

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
