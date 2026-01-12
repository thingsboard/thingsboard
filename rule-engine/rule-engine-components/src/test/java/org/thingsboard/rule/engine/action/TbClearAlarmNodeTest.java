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
package org.thingsboard.rule.engine.action;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.Futures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ListeningExecutor;
import org.thingsboard.rule.engine.TestDbCallbackExecutor;
import org.thingsboard.rule.engine.api.RuleEngineAlarmService;
import org.thingsboard.rule.engine.api.ScriptEngine;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmApiCallResult;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.script.ScriptLanguage;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TbClearAlarmNodeTest {

    @Mock
    TbContext ctxMock;
    @Mock
    RuleEngineAlarmService alarmServiceMock;
    @Mock
    ScriptEngine alarmDetailsScriptMock;

    @Captor
    ArgumentCaptor<Runnable> successCaptor;
    @Captor
    ArgumentCaptor<Consumer<Throwable>> failureCaptor;

    TbClearAlarmNode node;

    final TenantId tenantId = TenantId.fromUUID(Uuids.timeBased());
    final EntityId msgOriginator = new DeviceId(Uuids.timeBased());
    final EntityId alarmOriginator = new AlarmId(Uuids.timeBased());
    TbMsgMetaData metadata;

    ListeningExecutor dbExecutor;

    @BeforeEach
    void before() {
        dbExecutor = new TestDbCallbackExecutor();
        metadata = new TbMsgMetaData();
    }

    @Test
    void alarmCanBeCleared() {
        initWithClearAlarmScript();
        metadata.putValue("key", "value");
        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(msgOriginator)
                .copyMetaData(metadata)
                .data("{\"temperature\": 50}")
                .build();

        long oldEndDate = System.currentTimeMillis();
        Alarm activeAlarm = Alarm.builder().type("SomeType").tenantId(tenantId).originator(msgOriginator).severity(AlarmSeverity.WARNING).endTs(oldEndDate).build();

        Alarm expectedAlarm = Alarm.builder()
                .tenantId(tenantId)
                .originator(msgOriginator)
                .cleared(true)
                .severity(AlarmSeverity.WARNING)
                .propagate(false)
                .type("SomeType")
                .details(null)
                .endTs(oldEndDate)
                .build();

        when(alarmDetailsScriptMock.executeJsonAsync(msg)).thenReturn(Futures.immediateFuture(null));
        when(alarmServiceMock.findLatestActiveByOriginatorAndType(tenantId, msgOriginator, "SomeType")).thenReturn(activeAlarm);
        when(alarmServiceMock.clearAlarm(eq(activeAlarm.getTenantId()), eq(activeAlarm.getId()), anyLong(), nullable(JsonNode.class)))
                .thenReturn(AlarmApiCallResult.builder()
                        .successful(true)
                        .cleared(true)
                        .alarm(new AlarmInfo(expectedAlarm))
                        .build());

        node.onMsg(ctxMock, msg);

        verify(ctxMock).enqueue(any(), successCaptor.capture(), failureCaptor.capture());
        successCaptor.getValue().run();
        verify(ctxMock).tellNext(any(), eq("Cleared"));

        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<TbMsgType> typeCaptor = ArgumentCaptor.forClass(TbMsgType.class);
        ArgumentCaptor<EntityId> originatorCaptor = ArgumentCaptor.forClass(EntityId.class);
        ArgumentCaptor<TbMsgMetaData> metadataCaptor = ArgumentCaptor.forClass(TbMsgMetaData.class);
        ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(ctxMock).transformMsg(msgCaptor.capture(), typeCaptor.capture(), originatorCaptor.capture(), metadataCaptor.capture(), dataCaptor.capture());

        assertThat(TbMsgType.ALARM).isEqualTo(typeCaptor.getValue());
        assertThat(msgOriginator).isEqualTo(originatorCaptor.getValue());
        assertThat("value").isEqualTo(metadataCaptor.getValue().getValue("key"));
        assertThat(Boolean.TRUE.toString()).isEqualTo(metadataCaptor.getValue().getValue(DataConstants.IS_CLEARED_ALARM));
        assertThat(metadata).isNotSameAs(metadataCaptor.getValue());

        Alarm actualAlarm = JacksonUtil.fromBytes(dataCaptor.getValue().getBytes(), Alarm.class);
        assertThat(actualAlarm).isEqualTo(expectedAlarm);
    }

    @Test
    void alarmCanBeClearedWithAlarmOriginator() {
        initWithClearAlarmScript();
        metadata.putValue("key", "value");
        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(alarmOriginator)
                .copyMetaData(metadata)
                .data("{\"temperature\": 50}")
                .build();

        long oldEndDate = System.currentTimeMillis();
        AlarmId id = new AlarmId(alarmOriginator.getId());
        Alarm activeAlarm = Alarm.builder().type("SomeType").tenantId(tenantId).originator(msgOriginator).severity(AlarmSeverity.WARNING).endTs(oldEndDate).build();
        activeAlarm.setId(id);

        Alarm expectedAlarm = Alarm.builder()
                .tenantId(tenantId)
                .originator(msgOriginator)
                .cleared(true)
                .severity(AlarmSeverity.WARNING)
                .propagate(false)
                .type("SomeType")
                .details(null)
                .endTs(oldEndDate)
                .build();
        expectedAlarm.setId(id);

        when(alarmDetailsScriptMock.executeJsonAsync(msg)).thenReturn(Futures.immediateFuture(null));
        when(alarmServiceMock.findAlarmById(tenantId, id)).thenReturn(activeAlarm);
        when(alarmServiceMock.clearAlarm(eq(activeAlarm.getTenantId()), eq(activeAlarm.getId()), anyLong(), nullable(JsonNode.class)))
                .thenReturn(AlarmApiCallResult.builder()
                        .successful(true)
                        .cleared(true)
                        .alarm(new AlarmInfo(expectedAlarm))
                        .build());

        node.onMsg(ctxMock, msg);

        verify(ctxMock).enqueue(any(), successCaptor.capture(), failureCaptor.capture());
        successCaptor.getValue().run();
        verify(ctxMock).tellNext(any(), eq("Cleared"));

        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<TbMsgType> typeCaptor = ArgumentCaptor.forClass(TbMsgType.class);
        ArgumentCaptor<EntityId> originatorCaptor = ArgumentCaptor.forClass(EntityId.class);
        ArgumentCaptor<TbMsgMetaData> metadataCaptor = ArgumentCaptor.forClass(TbMsgMetaData.class);
        ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(ctxMock).transformMsg(msgCaptor.capture(), typeCaptor.capture(), originatorCaptor.capture(), metadataCaptor.capture(), dataCaptor.capture());

        assertThat(TbMsgType.ALARM).isEqualTo(typeCaptor.getValue());
        assertThat(alarmOriginator).isEqualTo(originatorCaptor.getValue());
        assertThat("value").isEqualTo(metadataCaptor.getValue().getValue("key"));
        assertThat(Boolean.TRUE.toString()).isEqualTo(metadataCaptor.getValue().getValue(DataConstants.IS_CLEARED_ALARM));
        assertThat(metadata).isNotSameAs(metadataCaptor.getValue());

        Alarm actualAlarm = JacksonUtil.fromBytes(dataCaptor.getValue().getBytes(), Alarm.class);
        assertThat(actualAlarm).isEqualTo(expectedAlarm);
    }

    private void initWithClearAlarmScript() {
        try {
            TbClearAlarmNodeConfiguration config = new TbClearAlarmNodeConfiguration();
            config.setAlarmType("SomeType");
            config.setScriptLang(ScriptLanguage.JS);
            config.setAlarmDetailsBuildJs("DETAILS");
            TbNodeConfiguration nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));

            when(ctxMock.createScriptEngine(ScriptLanguage.JS, "DETAILS")).thenReturn(alarmDetailsScriptMock);

            when(ctxMock.getTenantId()).thenReturn(tenantId);
            when(ctxMock.getAlarmService()).thenReturn(alarmServiceMock);
            when(ctxMock.getDbCallbackExecutor()).thenReturn(dbExecutor);

            node = new TbClearAlarmNode();
            node.init(ctxMock, nodeConfiguration);
        } catch (TbNodeException ex) {
            throw new IllegalStateException(ex);
        }
    }

}
