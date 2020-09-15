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
package org.thingsboard.rule.engine.profile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.AdditionalAnswers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.util.StringUtils;
import org.thingsboard.rule.engine.api.RuleEngineAlarmService;
import org.thingsboard.rule.engine.api.RuleEngineDeviceProfileCache;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.device.profile.AlarmCondition;
import org.thingsboard.server.common.data.device.profile.AlarmRule;
import org.thingsboard.server.common.data.device.profile.DeviceProfileAlarm;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.EntityKeyValueType;
import org.thingsboard.server.common.data.query.FilterPredicateValue;
import org.thingsboard.server.common.data.query.KeyFilter;
import org.thingsboard.server.common.data.query.NumericFilterPredicate;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.session.SessionMsgType;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;

import java.util.Collections;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class TbDeviceProfileNodeTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    private TbDeviceProfileNode node;

    @Mock
    private TbContext ctx;
    @Mock
    private RuleEngineDeviceProfileCache cache;
    @Mock
    private TimeseriesService timeseriesService;
    @Mock
    private RuleEngineAlarmService alarmService;

    private TenantId tenantId = new TenantId(UUID.randomUUID());
    private DeviceId deviceId = new DeviceId(UUID.randomUUID());
    private DeviceProfileId deviceProfileId = new DeviceProfileId(UUID.randomUUID());

    @Test
    public void testRandomMessageType() throws Exception {
        init();

        DeviceProfile deviceProfile = new DeviceProfile();
        DeviceProfileData deviceProfileData = new DeviceProfileData();
        deviceProfileData.setAlarms(Collections.emptyList());
        deviceProfile.setProfileData(deviceProfileData);

        Mockito.when(cache.get(tenantId, deviceId)).thenReturn(deviceProfile);
        ObjectNode data = mapper.createObjectNode();
        data.put("temperature", 42);
        TbMsg msg = TbMsg.newMsg("123456789", deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, mapper.writeValueAsString(data), null, null);
        node.onMsg(ctx, msg);
        verify(ctx).tellSuccess(msg);
        verify(ctx, Mockito.never()).tellFailure(Mockito.any(), Mockito.any());
    }

    @Test
    public void testEmptyProfile() throws Exception {
        init();

        DeviceProfile deviceProfile = new DeviceProfile();
        DeviceProfileData deviceProfileData = new DeviceProfileData();
        deviceProfileData.setAlarms(Collections.emptyList());
        deviceProfile.setProfileData(deviceProfileData);

        Mockito.when(cache.get(tenantId, deviceId)).thenReturn(deviceProfile);
        ObjectNode data = mapper.createObjectNode();
        data.put("temperature", 42);
        TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, mapper.writeValueAsString(data), null, null);
        node.onMsg(ctx, msg);
        verify(ctx).tellSuccess(msg);
        verify(ctx, Mockito.never()).tellFailure(Mockito.any(), Mockito.any());
    }

    @Test
    public void testAlarmCreate() throws Exception {
        init();

        DeviceProfile deviceProfile = new DeviceProfile();
        DeviceProfileData deviceProfileData = new DeviceProfileData();
        KeyFilter highTempFilter = new KeyFilter();
        highTempFilter.setKey(new EntityKey(EntityKeyType.TIME_SERIES, "temperature"));
        highTempFilter.setValueType(EntityKeyValueType.NUMERIC);
        NumericFilterPredicate highTemperaturePredicate = new NumericFilterPredicate();
        highTemperaturePredicate.setOperation(NumericFilterPredicate.NumericOperation.GREATER);
        highTemperaturePredicate.setValue(new FilterPredicateValue<>(30.0));
        highTempFilter.setPredicate(highTemperaturePredicate);
        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setCondition(Collections.singletonList(highTempFilter));
        AlarmRule alarmRule = new AlarmRule();
        alarmRule.setCondition(alarmCondition);
        DeviceProfileAlarm dpa = new DeviceProfileAlarm();
        dpa.setId("highTemperatureAlarmID");
        dpa.setCreateRules(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRule));
        deviceProfileData.setAlarms(Collections.singletonList(dpa));
        deviceProfile.setProfileData(deviceProfileData);

        Mockito.when(cache.get(tenantId, deviceId)).thenReturn(deviceProfile);
        Mockito.when(timeseriesService.findLatest(tenantId, deviceId, Collections.singleton("temperature")))
                .thenReturn(Futures.immediateFuture(Collections.emptyList()));
        Mockito.when(alarmService.createOrUpdateAlarm(Mockito.any())).thenAnswer(AdditionalAnswers.returnsFirstArg());

        TbMsg theMsg = TbMsg.newMsg("ALARM", deviceId, new TbMsgMetaData(), "");
        Mockito.when(ctx.newMsg(Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.anyString())).thenReturn(theMsg);

        ObjectNode data = mapper.createObjectNode();
        data.put("temperature", 42);
        TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, mapper.writeValueAsString(data), null, null);
        node.onMsg(ctx, msg);
        verify(ctx).tellSuccess(msg);
        verify(ctx).tellNext(theMsg, "Alarm Created");
        verify(ctx, Mockito.never()).tellFailure(Mockito.any(), Mockito.any());

        TbMsg theMsg2 = TbMsg.newMsg("ALARM", deviceId, new TbMsgMetaData(), "2");
        Mockito.when(ctx.newMsg(Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.anyString())).thenReturn(theMsg2);


        TbMsg msg2 = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, mapper.writeValueAsString(data), null, null);
        node.onMsg(ctx, msg2);
        verify(ctx).tellSuccess(msg2);
        verify(ctx).tellNext(theMsg2, "Alarm Updated");

    }

    private void init() throws TbNodeException {
        Mockito.when(ctx.getTenantId()).thenReturn(tenantId);
        Mockito.when(ctx.getDeviceProfileCache()).thenReturn(cache);
        Mockito.when(ctx.getTimeseriesService()).thenReturn(timeseriesService);
        Mockito.when(ctx.getAlarmService()).thenReturn(alarmService);
        TbNodeConfiguration nodeConfiguration = new TbNodeConfiguration(mapper.createObjectNode());
        node = new TbDeviceProfileNode();
        node.init(ctx, nodeConfiguration);
    }

}
