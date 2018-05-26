/**
 * Copyright © 2016-2018 The Thingsboard Authors
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

import com.datastax.driver.core.utils.UUIDs;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.thingsboard.rule.engine.api.*;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.alarm.AlarmService;

import javax.script.ScriptException;
import java.io.IOException;
import java.util.concurrent.Callable;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.thingsboard.rule.engine.action.TbAbstractAlarmNode.*;
import static org.thingsboard.server.common.data.alarm.AlarmSeverity.CRITICAL;
import static org.thingsboard.server.common.data.alarm.AlarmSeverity.WARNING;
import static org.thingsboard.server.common.data.alarm.AlarmStatus.*;

@RunWith(MockitoJUnitRunner.class)
public class TbAlarmNodeTest {

    private TbAbstractAlarmNode node;

    @Mock
    private TbContext ctx;
    @Mock
    private ListeningExecutor executor;
    @Mock
    private AlarmService alarmService;

    @Mock
    private ScriptEngine detailsJs;

    private RuleChainId ruleChainId = new RuleChainId(UUIDs.timeBased());
    private RuleNodeId ruleNodeId = new RuleNodeId(UUIDs.timeBased());

    private ListeningExecutor dbExecutor;

    private EntityId originator = new DeviceId(UUIDs.timeBased());
    private TenantId tenantId = new TenantId(UUIDs.timeBased());
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
        TbMsg msg = new TbMsg(UUIDs.timeBased(), "USER", originator, metaData, rawJson, ruleChainId, ruleNodeId, 0L);

        when(detailsJs.executeJson(msg)).thenReturn(null);
        when(alarmService.findLatestByOriginatorAndType(tenantId, originator, "SomeType")).thenReturn(Futures.immediateFuture(null));

        doAnswer((Answer<Alarm>) invocationOnMock -> (Alarm) (invocationOnMock.getArguments())[0]).when(alarmService).createOrUpdateAlarm(any(Alarm.class));

        node.onMsg(ctx, msg);

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

        verify(executor, times(1)).executeAsync(any(Callable.class));
    }

    @Test
    public void buildDetailsThrowsException() throws ScriptException, IOException {
        initWithCreateAlarmScript();
        metaData.putValue("key", "value");
        TbMsg msg = new TbMsg(UUIDs.timeBased(), "USER", originator, metaData, rawJson, ruleChainId, ruleNodeId, 0L);

        when(detailsJs.executeJson(msg)).thenThrow(new NotImplementedException("message"));
        when(alarmService.findLatestByOriginatorAndType(tenantId, originator, "SomeType")).thenReturn(Futures.immediateFuture(null));

        node.onMsg(ctx, msg);

        verifyError(msg, "message", NotImplementedException.class);

        verify(ctx).createJsScriptEngine("DETAILS");
        verify(ctx, times(1)).getJsExecutor();
        verify(ctx).getAlarmService();
        verify(ctx, times(3)).getDbCallbackExecutor();
        verify(ctx).getTenantId();
        verify(alarmService).findLatestByOriginatorAndType(tenantId, originator, "SomeType");

        verifyNoMoreInteractions(ctx, alarmService);
    }

    @Test
    public void ifAlarmClearedCreateNew() throws ScriptException, IOException {
        initWithCreateAlarmScript();
        metaData.putValue("key", "value");
        TbMsg msg = new TbMsg(UUIDs.timeBased(), "USER", originator, metaData, rawJson, ruleChainId, ruleNodeId, 0L);

        Alarm clearedAlarm = Alarm.builder().status(CLEARED_ACK).build();

        when(detailsJs.executeJson(msg)).thenReturn(null);
        when(alarmService.findLatestByOriginatorAndType(tenantId, originator, "SomeType")).thenReturn(Futures.immediateFuture(clearedAlarm));

        doAnswer((Answer<Alarm>) invocationOnMock -> (Alarm) (invocationOnMock.getArguments())[0]).when(alarmService).createOrUpdateAlarm(any(Alarm.class));

        node.onMsg(ctx, msg);

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

        verify(executor, times(1)).executeAsync(any(Callable.class));
    }

    @Test
    public void alarmCanBeUpdated() throws ScriptException, IOException {
        initWithCreateAlarmScript();
        metaData.putValue("key", "value");
        TbMsg msg = new TbMsg(UUIDs.timeBased(), "USER", originator, metaData, rawJson, ruleChainId, ruleNodeId, 0L);

        long oldEndDate = System.currentTimeMillis();
        Alarm activeAlarm = Alarm.builder().type("SomeType").tenantId(tenantId).originator(originator).status(ACTIVE_UNACK).severity(WARNING).endTs(oldEndDate).build();

        when(detailsJs.executeJson(msg)).thenReturn(null);
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

        verify(executor, times(1)).executeAsync(any(Callable.class));
    }

    @Test
    public void alarmCanBeCleared() throws ScriptException, IOException {
        initWithClearAlarmScript();
        metaData.putValue("key", "value");
        TbMsg msg = new TbMsg(UUIDs.timeBased(), "USER", originator, metaData, rawJson, ruleChainId, ruleNodeId, 0L);

        long oldEndDate = System.currentTimeMillis();
        Alarm activeAlarm = Alarm.builder().type("SomeType").tenantId(tenantId).originator(originator).status(ACTIVE_UNACK).severity(WARNING).endTs(oldEndDate).build();

//        when(detailsJs.executeJson(msg)).thenReturn(null);
        when(alarmService.findLatestByOriginatorAndType(tenantId, originator, "SomeType")).thenReturn(Futures.immediateFuture(activeAlarm));
        when(alarmService.clearAlarm(eq(activeAlarm.getId()), org.mockito.Mockito.any(JsonNode.class), anyLong())).thenReturn(Futures.immediateFuture(true));
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
            when(ctx.getJsExecutor()).thenReturn(executor);
            when(ctx.getAlarmService()).thenReturn(alarmService);
            when(ctx.getDbCallbackExecutor()).thenReturn(dbExecutor);

            mockJsExecutor();

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
            when(ctx.getJsExecutor()).thenReturn(executor);
            when(ctx.getAlarmService()).thenReturn(alarmService);
            when(ctx.getDbCallbackExecutor()).thenReturn(dbExecutor);

            mockJsExecutor();

            node = new TbClearAlarmNode();
            node.init(ctx, nodeConfiguration);
        } catch (TbNodeException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private void mockJsExecutor() {
        when(ctx.getJsExecutor()).thenReturn(executor);
        doAnswer((Answer<ListenableFuture<Boolean>>) invocationOnMock -> {
            try {
                Callable task = (Callable) (invocationOnMock.getArguments())[0];
                return Futures.immediateFuture((Boolean) task.call());
            } catch (Throwable th) {
                return Futures.immediateFailedFuture(th);
            }
        }).when(executor).executeAsync(any(Callable.class));
    }

    private void verifyError(TbMsg msg, String message, Class expectedClass) {
        ArgumentCaptor<Throwable> captor = ArgumentCaptor.forClass(Throwable.class);
        verify(ctx).tellFailure(same(msg), captor.capture());

        Throwable value = captor.getValue();
        assertEquals(expectedClass, value.getClass());
        assertEquals(message, value.getMessage());
    }

}