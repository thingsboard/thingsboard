// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.profile;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.thingsboard.server.common.data.device.profile.AlarmCondition;
import org.thingsboard.server.common.data.device.profile.AlarmConditionFilter;
import org.thingsboard.server.common.data.device.profile.AlarmConditionFilterKey;
import org.thingsboard.server.common.data.device.profile.AlarmConditionKeyType;
import org.thingsboard.server.common.data.device.profile.AlarmRule;
import org.thingsboard.server.common.data.device.profile.SimpleAlarmConditionSpec;
import org.thingsboard.server.common.data.query.EntityKeyValueType;
import org.thingsboard.server.common.data.query.FilterPredicateValue;
import org.thingsboard.server.common.data.query.StringFilterPredicate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class AlarmRuleStateTest {

    private static Stream<Arguments> testEvalCondition() {
        return Stream.of(
                Arguments.of(StringFilterPredicate.StringOperation.IN, "test,value", "test", AlarmEvalResult.TRUE),
                Arguments.of(StringFilterPredicate.StringOperation.IN, "test,value", "teeeeest", AlarmEvalResult.FALSE),
                Arguments.of(StringFilterPredicate.StringOperation.NOT_IN, "test,value", "test", AlarmEvalResult.FALSE),
                Arguments.of(StringFilterPredicate.StringOperation.NOT_IN, "test,value", "teeeeest", AlarmEvalResult.TRUE),
                Arguments.of(StringFilterPredicate.StringOperation.CONTAINS, "test value", "test value", AlarmEvalResult.TRUE),
                Arguments.of(StringFilterPredicate.StringOperation.CONTAINS, "test value", "test", AlarmEvalResult.FALSE),
                Arguments.of(StringFilterPredicate.StringOperation.NOT_CONTAINS, "test value", "test", AlarmEvalResult.TRUE),
                Arguments.of(StringFilterPredicate.StringOperation.NOT_CONTAINS, "test value", "test value", AlarmEvalResult.FALSE),
                Arguments.of(StringFilterPredicate.StringOperation.EQUAL, "test value", "test value", AlarmEvalResult.TRUE),
                Arguments.of(StringFilterPredicate.StringOperation.EQUAL, "test value", "test", AlarmEvalResult.FALSE),
                Arguments.of(StringFilterPredicate.StringOperation.NOT_EQUAL, "test value", "test", AlarmEvalResult.TRUE),
                Arguments.of(StringFilterPredicate.StringOperation.NOT_EQUAL, "test value", "test value", AlarmEvalResult.FALSE),
                Arguments.of(StringFilterPredicate.StringOperation.ENDS_WITH, "test value", "some test value", AlarmEvalResult.TRUE),
                Arguments.of(StringFilterPredicate.StringOperation.ENDS_WITH, "test value", "some test value2", AlarmEvalResult.FALSE),
                Arguments.of(StringFilterPredicate.StringOperation.STARTS_WITH, "test value", "test value attribute", AlarmEvalResult.TRUE),
                Arguments.of(StringFilterPredicate.StringOperation.STARTS_WITH, "test value", "test", AlarmEvalResult.FALSE)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testEvalCondition(StringFilterPredicate.StringOperation operation, String predicateValue, String attributeValue, AlarmEvalResult evalResult) {
            AlarmConditionFilterKey alarmConditionFilterKey = new AlarmConditionFilterKey(AlarmConditionKeyType.ATTRIBUTE, "stringKey");

            StringFilterPredicate predicate = new StringFilterPredicate();
            predicate.setOperation(operation);
            predicate.setValue(new FilterPredicateValue<>(predicateValue));

            AlarmConditionFilter alarmConditionFilter = new AlarmConditionFilter();
            alarmConditionFilter.setKey(alarmConditionFilterKey);
            alarmConditionFilter.setPredicate(predicate);
            alarmConditionFilter.setValueType(EntityKeyValueType.STRING);

            List<AlarmConditionFilter> condition = new ArrayList<>();
            condition.add(alarmConditionFilter);

            AlarmCondition alarmCondition = new AlarmCondition();
            alarmCondition.setSpec(new SimpleAlarmConditionSpec());
            alarmCondition.setCondition(condition);

            AlarmRule alarmRule = new AlarmRule();
            alarmRule.setCondition(alarmCondition);

            AlarmRuleState alarmRuleState = new AlarmRuleState(null, alarmRule, null, null, null);

            Set<AlarmConditionFilterKey> entityKeys = new HashSet<>(List.of(alarmConditionFilterKey));
            DataSnapshot result = new DataSnapshot(entityKeys);
            result.putValue(alarmConditionFilterKey, System.currentTimeMillis(), EntityKeyValue.fromString(attributeValue));
            Assertions.assertEquals(evalResult, alarmRuleState.eval(result));
    }
}
