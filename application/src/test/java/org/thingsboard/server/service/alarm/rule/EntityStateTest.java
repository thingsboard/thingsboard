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
package org.thingsboard.server.service.alarm.rule;

import com.google.common.util.concurrent.Futures;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.RuleEngineAlarmService;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmCreateOrUpdateActiveRequest;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.rule.AlarmRule;
import org.thingsboard.server.common.data.alarm.rule.AlarmRuleOriginatorTargetEntity;
import org.thingsboard.server.common.data.alarm.rule.filter.AlarmRuleAllDevicesEntityFilter;
import org.thingsboard.server.common.data.device.profile.AlarmCondition;
import org.thingsboard.server.common.data.device.profile.AlarmConditionFilter;
import org.thingsboard.server.common.data.device.profile.AlarmConditionFilterKey;
import org.thingsboard.server.common.data.device.profile.AlarmConditionKeyType;
import org.thingsboard.server.common.data.device.profile.AlarmRuleCondition;
import org.thingsboard.server.common.data.device.profile.AlarmRuleConfiguration;
import org.thingsboard.server.common.data.device.profile.SimpleAlarmConditionSpec;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.AlarmRuleId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.query.BooleanFilterPredicate;
import org.thingsboard.server.common.data.query.EntityKeyValueType;
import org.thingsboard.server.common.data.query.FilterPredicateValue;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.session.SessionMsgType;
import org.thingsboard.server.common.data.alarm.AlarmApiCallResult;
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

    @Before
    public void beforeEach() {
        ctx = mock(TbAlarmRuleContext.class);
        clusterService = mock(TbClusterService.class);

        when(ctx.getDeviceService()).thenReturn(mock(DeviceService.class));
        when(ctx.getClusterService()).thenReturn(clusterService);

        AttributesService attributesService = mock(AttributesService.class);
        when(attributesService.find(any(), any(), any(), anyCollection())).thenReturn(Futures.immediateFuture(Collections.emptyList()));
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

        TbMsg attributeUpdateMsg = TbMsg.newMsg(SessionMsgType.POST_ATTRIBUTES_REQUEST.name(),
                deviceId, new TbMsgMetaData(), "{ \"enabled\": false }");

        deviceState.process(null, attributeUpdateMsg);

        ArgumentCaptor<TbMsg> resultMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        Mockito.verify(clusterService).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), resultMsgCaptor.capture(), eq(Collections.singleton("Alarm Created")), any());
        Alarm alarm = JacksonUtil.fromString(resultMsgCaptor.getValue().getData(), Alarm.class);

        deviceState.process(null, TbMsg.newMsg(DataConstants.ALARM_CLEAR, deviceId, new TbMsgMetaData(), JacksonUtil.toString(alarm)));
        reset(clusterService);

        String deletedAttributes = "{ \"attributes\": [ \"other\" ] }";
        deviceState.process(null, TbMsg.newMsg(DataConstants.ATTRIBUTES_DELETED, deviceId, new TbMsgMetaData(), deletedAttributes));
        verify(clusterService, never()).pushMsgToRuleEngine(any(), any(), any(), any(), any());
    }

    @Test
    public void whenDeletingClearedAlarm_thenNoError() throws Exception {
        AlarmRule alarmRule = createAlarmRule(createAlarmConfigWithBoolAttrCondition("enabled", false));
        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        EntityState deviceState = createEntityState(deviceId, alarmRule);

        TbMsg attributeUpdateMsg = TbMsg.newMsg(SessionMsgType.POST_ATTRIBUTES_REQUEST.name(),
                deviceId, new TbMsgMetaData(), "{ \"enabled\": false }");

        deviceState.process(null, attributeUpdateMsg);
        ArgumentCaptor<TbMsg> resultMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        Mockito.verify(clusterService).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), resultMsgCaptor.capture(), eq(Collections.singleton("Alarm Created")), any());
        Alarm alarm = JacksonUtil.fromString(resultMsgCaptor.getValue().getData(), Alarm.class);

        deviceState.process(null, TbMsg.newMsg(DataConstants.ALARM_CLEAR, deviceId, new TbMsgMetaData(), JacksonUtil.toString(alarm)));

        TbMsg alarmDeleteNotification = TbMsg.newMsg(DataConstants.ALARM_DELETE, deviceId, new TbMsgMetaData(), JacksonUtil.toString(alarm));
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

        AlarmConditionFilter condition = new AlarmConditionFilter();
        condition.setKey(new AlarmConditionFilterKey(AlarmConditionKeyType.ATTRIBUTE, key));
        condition.setValueType(EntityKeyValueType.BOOLEAN);
        BooleanFilterPredicate predicate = new BooleanFilterPredicate();
        predicate.setOperation(BooleanFilterPredicate.BooleanOperation.EQUAL);
        predicate.setValue(new FilterPredicateValue<>(value));
        condition.setPredicate(predicate);

        AlarmRuleConfiguration alarmConfig = new AlarmRuleConfiguration();
        AlarmRuleCondition alarmRule = new AlarmRuleCondition();
        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setSpec(new SimpleAlarmConditionSpec());
        alarmCondition.setCondition(List.of(condition));
        alarmRule.setCondition(alarmCondition);
        alarmConfig.setCreateRules(new TreeMap<>(Map.of(AlarmSeverity.CRITICAL, alarmRule)));

        alarmConfig.setSourceEntityFilters(Collections.singletonList(new AlarmRuleAllDevicesEntityFilter()));
        alarmConfig.setAlarmTargetEntity(new AlarmRuleOriginatorTargetEntity());

        return alarmConfig;
    }

}
