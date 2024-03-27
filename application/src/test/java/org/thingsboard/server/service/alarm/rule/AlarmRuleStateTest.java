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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmCondition;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmConditionFilterKey;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmConditionKeyType;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmRuleArgument;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmRuleCondition;
import org.thingsboard.server.common.data.alarm.rule.condition.ArgumentOperation;
import org.thingsboard.server.common.data.alarm.rule.condition.ArgumentValueType;
import org.thingsboard.server.common.data.alarm.rule.condition.ConstantArgument;
import org.thingsboard.server.common.data.alarm.rule.condition.FromMessageArgument;
import org.thingsboard.server.common.data.alarm.rule.condition.SimpleAlarmConditionFilter;
import org.thingsboard.server.common.data.alarm.rule.condition.SimpleAlarmConditionSpec;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public class AlarmRuleStateTest {

    private static Stream<Arguments> testEvalCondition() {
        return Stream.of(
                Arguments.of(ArgumentOperation.IN, "test,value", "test", AlarmEvalResult.TRUE),
                Arguments.of(ArgumentOperation.IN, "test,value", "teeeeest", AlarmEvalResult.FALSE),
                Arguments.of(ArgumentOperation.NOT_IN, "test,value", "test", AlarmEvalResult.FALSE),
                Arguments.of(ArgumentOperation.NOT_IN, "test,value", "teeeeest", AlarmEvalResult.TRUE),
                Arguments.of(ArgumentOperation.CONTAINS, "test value", "test value", AlarmEvalResult.TRUE),
                Arguments.of(ArgumentOperation.CONTAINS, "test value", "test", AlarmEvalResult.FALSE),
                Arguments.of(ArgumentOperation.NOT_CONTAINS, "test value", "test", AlarmEvalResult.TRUE),
                Arguments.of(ArgumentOperation.NOT_CONTAINS, "test value", "test value", AlarmEvalResult.FALSE),
                Arguments.of(ArgumentOperation.EQUAL, "test value", "test value", AlarmEvalResult.TRUE),
                Arguments.of(ArgumentOperation.EQUAL, "test value", "test", AlarmEvalResult.FALSE),
                Arguments.of(ArgumentOperation.NOT_EQUAL, "test value", "test", AlarmEvalResult.TRUE),
                Arguments.of(ArgumentOperation.NOT_EQUAL, "test value", "test value", AlarmEvalResult.FALSE),
                Arguments.of(ArgumentOperation.ENDS_WITH, "test value", "some test value", AlarmEvalResult.TRUE),
                Arguments.of(ArgumentOperation.ENDS_WITH, "test value", "some test value2", AlarmEvalResult.FALSE),
                Arguments.of(ArgumentOperation.STARTS_WITH, "test value", "test value attribute", AlarmEvalResult.TRUE),
                Arguments.of(ArgumentOperation.STARTS_WITH, "test value", "test", AlarmEvalResult.FALSE)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testEvalCondition(ArgumentOperation operation, String constValue, String attributeValue, AlarmEvalResult evalResult) {
        AlarmConditionFilterKey alarmConditionFilterKey = new AlarmConditionFilterKey(AlarmConditionKeyType.ATTRIBUTE, "stringKey");
        var left = new FromMessageArgument(alarmConditionFilterKey, ArgumentValueType.STRING);
        var right = new ConstantArgument(ArgumentValueType.STRING, constValue);

        Map<String, AlarmRuleArgument> arguments = Map.of("leftArgId", left, "rightArgId", right);

        SimpleAlarmConditionFilter alarmConditionFilter = new SimpleAlarmConditionFilter();
        alarmConditionFilter.setLeftArgId("leftArgId");
        alarmConditionFilter.setRightArgId("rightArgId");
        alarmConditionFilter.setOperation(operation);

        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setSpec(new SimpleAlarmConditionSpec());
        alarmCondition.setConditionFilter(alarmConditionFilter);

        AlarmRuleCondition alarmRule = new AlarmRuleCondition();
        alarmRule.setAlarmCondition(alarmCondition);

        AlarmRuleState alarmRuleState = new AlarmRuleState(null, alarmRule, null, null, null, arguments);

        Set<AlarmConditionFilterKey> entityKeys = new HashSet<>(List.of(alarmConditionFilterKey));
        DataSnapshot result = new DataSnapshot(entityKeys);
        result.putValue(alarmConditionFilterKey, System.currentTimeMillis(), EntityKeyValue.fromString(attributeValue));
        Assertions.assertEquals(evalResult, alarmRuleState.eval(result));
    }
}
