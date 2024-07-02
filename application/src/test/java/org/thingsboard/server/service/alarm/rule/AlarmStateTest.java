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

import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.alarm.rule.AlarmRule;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmCondition;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmConditionSpec;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmConditionSpecType;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmRuleCondition;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmRuleConfiguration;
import org.thingsboard.server.common.data.alarm.rule.condition.DurationAlarmConditionSpec;
import org.thingsboard.server.common.data.alarm.rule.condition.RepeatingAlarmConditionSpec;
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

        AlarmRuleCondition alarmRule = new AlarmRuleCondition();
        alarmRule.setAlarmCondition(alarmCondition);

        return new AlarmRuleState(null, alarmRule, null, null, null, null);
    }

    private AlarmState createMockAlarmState() {
        AlarmRule alarmRule = new AlarmRule();
        alarmRule.setConfiguration(mock(AlarmRuleConfiguration.class));
        return new AlarmState(null, null, null, alarmRule, null, null);
    }
}
