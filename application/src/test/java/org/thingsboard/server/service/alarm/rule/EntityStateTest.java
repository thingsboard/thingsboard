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
package org.thingsboard.server.service.alarm.rule;

import com.google.common.util.concurrent.Futures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.RuleEngineAlarmService;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmApiCallResult;
import org.thingsboard.server.common.data.alarm.AlarmCreateOrUpdateActiveRequest;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.rule.AlarmRule;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmCondition;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmConditionKeyType;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmRuleCondition;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmRuleConfiguration;
import org.thingsboard.server.common.data.alarm.rule.condition.ArgumentValueType;
import org.thingsboard.server.common.data.alarm.rule.condition.ConstantArgument;
import org.thingsboard.server.common.data.alarm.rule.condition.FromMessageArgument;
import org.thingsboard.server.common.data.alarm.rule.condition.ArgumentOperation;
import org.thingsboard.server.common.data.alarm.rule.condition.SimpleAlarmConditionFilter;
import org.thingsboard.server.common.data.alarm.rule.condition.SimpleAlarmConditionSpec;
import org.thingsboard.server.common.data.alarm.rule.filter.AlarmRuleAllDevicesEntityFilter;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.AlarmRuleId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.device.DeviceService;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class EntityStateTest {

    private TbAlarmRuleContext ctx;
    private TbClusterService clusterService;
    private TenantId tenantId;

    @BeforeEach
    public void beforeEach() {
        ctx = mock(TbAlarmRuleContext.class);
        clusterService = mock(TbClusterService.class);

        when(ctx.getDeviceService()).thenReturn(mock(DeviceService.class));
        when(ctx.getClusterService()).thenReturn(clusterService);

        AttributesService attributesService = mock(AttributesService.class);
        when(attributesService.find(any(), any(), any(AttributeScope.class), anyCollection())).thenReturn(Futures.immediateFuture(Collections.emptyList()));
        when(ctx.getAttributesService()).thenReturn(attributesService);

        RuleEngineAlarmService alarmService = mock(RuleEngineAlarmService.class);
        when(alarmService.findLatestActiveByOriginatorAndType(any(), any(), any())).thenReturn(null);
        when(alarmService.createAlarm(any())).thenAnswer(invocationOnMock -> {
            AlarmCreateOrUpdateActiveRequest request = invocationOnMock.getArgument(0);
            return AlarmApiCallResult.builder()
                    .successful(true)
                    .created(true)
                    .modified(true)
                    .alarm(new AlarmInfo(new Alarm(new AlarmId(UUID.randomUUID()))))
                    .build();
        });
        when(ctx.getAlarmService()).thenReturn(alarmService);

        tenantId = new TenantId(UUID.randomUUID());
    }

    @Test
    public void whenAttributeIsDeleted_thenUnneededAlarmRulesAreNotReevaluated() throws Exception {
        AlarmRule alarmRule = createAlarmRule(createAlarmConfigWithBoolAttrCondition("enabled", false));
        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        EntityState deviceState = createEntityState(deviceId, alarmRule);

        TbMsg attributeUpdateMsg = TbMsg.newMsg(TbMsgType.POST_ATTRIBUTES_REQUEST,
                deviceId, TbMsgMetaData.EMPTY, "{ \"enabled\": false }");

        deviceState.process(null, attributeUpdateMsg);

        ArgumentCaptor<TbMsg> resultMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        Mockito.verify(clusterService).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), resultMsgCaptor.capture(), eq(Collections.singleton("Alarm Created")), any());
        Alarm alarm = JacksonUtil.fromString(resultMsgCaptor.getValue().getData(), Alarm.class);

        deviceState.process(null, TbMsg.newMsg(TbMsgType.ALARM_CLEAR, deviceId, TbMsgMetaData.EMPTY, JacksonUtil.toString(alarm)));
        reset(clusterService);

        String deletedAttributes = "{ \"attributes\": [ \"other\" ] }";
        deviceState.process(null, TbMsg.newMsg(TbMsgType.ATTRIBUTES_DELETED, deviceId, TbMsgMetaData.EMPTY, deletedAttributes));
        verify(clusterService, never()).pushMsgToRuleEngine(any(), any(), any(), any(), any());
    }

    @Test
    public void whenDeletingClearedAlarm_thenNoError() throws Exception {
        AlarmRule alarmRule = createAlarmRule(createAlarmConfigWithBoolAttrCondition("enabled", false));
        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        EntityState deviceState = createEntityState(deviceId, alarmRule);

        TbMsg attributeUpdateMsg = TbMsg.newMsg(TbMsgType.POST_ATTRIBUTES_REQUEST,
                deviceId, TbMsgMetaData.EMPTY, "{ \"enabled\": false }");

        deviceState.process(null, attributeUpdateMsg);
        ArgumentCaptor<TbMsg> resultMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        Mockito.verify(clusterService).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), resultMsgCaptor.capture(), eq(Collections.singleton("Alarm Created")), any());
        Alarm alarm = JacksonUtil.fromString(resultMsgCaptor.getValue().getData(), Alarm.class);

        deviceState.process(null, TbMsg.newMsg(TbMsgType.ALARM_CLEAR, deviceId, TbMsgMetaData.EMPTY, JacksonUtil.toString(alarm)));

        TbMsg alarmDeleteNotification = TbMsg.newMsg(TbMsgType.ALARM_DELETE, deviceId, TbMsgMetaData.EMPTY, JacksonUtil.toString(alarm));
        assertDoesNotThrow(() -> {
            deviceState.process(null, alarmDeleteNotification);
        });
    }

    private EntityState createEntityState(DeviceId deviceId, AlarmRule... rules) {
        EntityRulesState deviceRuleState = new EntityRulesState(List.of(rules));

        return new EntityState(tenantId, deviceId, null, ctx, deviceRuleState, null);
    }

    private AlarmRule createAlarmRule(AlarmRuleConfiguration alarmRuleConfiguration) {
        AlarmRule alarmRule = new AlarmRule(new AlarmRuleId(UUID.randomUUID()));
        alarmRule.setAlarmType("MyAlarm");
        alarmRule.setConfiguration(alarmRuleConfiguration);
        return alarmRule;
    }

    private AlarmRuleConfiguration createAlarmConfigWithBoolAttrCondition(String key, boolean value) {
        var enabledKey = new FromMessageArgument(AlarmConditionKeyType.ATTRIBUTE, key, ArgumentValueType.BOOLEAN);
        var enabledConst = new ConstantArgument(ArgumentValueType.BOOLEAN, value);

        SimpleAlarmConditionFilter condition = new SimpleAlarmConditionFilter();
        condition.setLeftArgId("enabledKey");
        condition.setRightArgId("enabledConst");
        condition.setOperation(ArgumentOperation.EQUAL);

        AlarmRuleConfiguration alarmConfig = new AlarmRuleConfiguration();
        AlarmRuleCondition alarmRule = new AlarmRuleCondition();
        AlarmCondition alarmCondition = new AlarmCondition();
        alarmConfig.setArguments(Map.of("enabledKey", enabledKey, "enabledConst", enabledConst));
        alarmCondition.setSpec(new SimpleAlarmConditionSpec());
        alarmCondition.setConditionFilter(condition);
        alarmRule.setAlarmCondition(alarmCondition);
        alarmConfig.setCreateRules(new TreeMap<>(Map.of(AlarmSeverity.CRITICAL, alarmRule)));

        alarmConfig.setSourceEntityFilters(Collections.singletonList(new AlarmRuleAllDevicesEntityFilter()));

        return alarmConfig;
    }

}
