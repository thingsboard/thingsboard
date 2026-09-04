// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.profile;

import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.device.profile.AlarmCondition;
import org.thingsboard.server.common.data.device.profile.AlarmConditionSpec;
import org.thingsboard.server.common.data.device.profile.AlarmConditionSpecType;
import org.thingsboard.server.common.data.device.profile.AlarmRule;
import org.thingsboard.server.common.data.device.profile.DeviceProfileAlarm;
import org.thingsboard.server.common.data.device.profile.DurationAlarmConditionSpec;
import org.thingsboard.server.common.data.device.profile.RepeatingAlarmConditionSpec;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

public class AlarmStateTest {

    @Test
    public void testSetAlarmConditionMetadata_repeatingCondition() {
        AlarmRuleState ruleState = createMockAlarmRuleState(new RepeatingAlarmConditionSpec());
        int eventCount = 3;
        ruleState.getState().setEventCount(eventCount);

        AlarmState alarmState = createMockAlarmState();
        TbMsgMetaData metaData = new TbMsgMetaData();

        alarmState.setAlarmConditionMetadata(ruleState, metaData);

        assertEquals(AlarmConditionSpecType.REPEATING, ruleState.getSpec().getType());
        assertNotNull(metaData.getValue(DataConstants.ALARM_CONDITION_REPEATS));
        assertNull(metaData.getValue(DataConstants.ALARM_CONDITION_DURATION));
        assertEquals(String.valueOf(eventCount), metaData.getValue(DataConstants.ALARM_CONDITION_REPEATS));
    }

    @Test
    public void testSetAlarmConditionMetadata_durationCondition() {
        DurationAlarmConditionSpec spec = new DurationAlarmConditionSpec();
        spec.setUnit(TimeUnit.SECONDS);
        AlarmRuleState ruleState = createMockAlarmRuleState(spec);
        int duration = 12;
        ruleState.getState().setDuration(duration);

        AlarmState alarmState = createMockAlarmState();
        TbMsgMetaData metaData = new TbMsgMetaData();

        alarmState.setAlarmConditionMetadata(ruleState, metaData);

        assertEquals(AlarmConditionSpecType.DURATION, ruleState.getSpec().getType());
        assertNotNull(metaData.getValue(DataConstants.ALARM_CONDITION_DURATION));
        assertNull(metaData.getValue(DataConstants.ALARM_CONDITION_REPEATS));
        assertEquals(String.valueOf(duration), metaData.getValue(DataConstants.ALARM_CONDITION_DURATION));
    }

    private AlarmRuleState createMockAlarmRuleState(AlarmConditionSpec spec) {
        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setSpec(spec);

        AlarmRule alarmRule = new AlarmRule();
        alarmRule.setCondition(alarmCondition);

        return new AlarmRuleState(null, alarmRule, null, null, null);
    }

    private AlarmState createMockAlarmState() {
        return new AlarmState(null, null, mock(DeviceProfileAlarm.class), null, null);
    }
}
