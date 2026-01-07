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
package org.thingsboard.rule.engine.profile;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.thingsboard.server.common.data.device.profile.AlarmConditionFilterKey;
import org.thingsboard.server.common.data.device.profile.AlarmConditionKeyType;
import org.thingsboard.server.common.data.device.profile.SpecificTimeSchedule;
import org.thingsboard.server.common.data.query.ComplexFilterPredicate;
import org.thingsboard.server.common.data.query.DynamicValue;
import org.thingsboard.server.common.data.query.DynamicValueSourceType;
import org.thingsboard.server.common.data.query.FilterPredicateType;
import org.thingsboard.server.common.data.query.SimpleKeyFilterPredicate;

import java.util.HashSet;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willCallRealMethod;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProfileStateTest {

    ProfileState profileState;
    Set<AlarmConditionFilterKey> entityKeys = new HashSet<>();
    Set<AlarmConditionFilterKey> ruleKeys = new HashSet<>();

    @BeforeEach
    void setUp() {
        profileState = mock(ProfileState.class);
    }

    @ParameterizedTest()
    @EnumSource(DynamicValueSourceType.class)
    @NullSource
    void addScheduleDynamicValuesSourceAttribute(DynamicValueSourceType sourceType) {
        willCallRealMethod().given(profileState).addScheduleDynamicValues(any(), any());
        final DynamicValue<String> dynamicValue = new DynamicValue<>(sourceType, "myKey");
        SpecificTimeSchedule schedule = new SpecificTimeSchedule();
        schedule.setDynamicValue(dynamicValue);

        Assertions.assertThat(entityKeys.isEmpty()).isTrue();
        Assertions.assertThat(schedule.getDynamicValue().getSourceAttribute()).isNotNull();

        profileState.addScheduleDynamicValues(schedule, entityKeys);

        Assertions.assertThat(entityKeys).isEqualTo(Set.of(
                new AlarmConditionFilterKey(AlarmConditionKeyType.ATTRIBUTE, "myKey")));
    }

    @ParameterizedTest()
    @EnumSource(DynamicValueSourceType.class)
    @NullSource
    void addScheduleDynamicValuesSourceAttributeIsNull(DynamicValueSourceType sourceType) {
        willCallRealMethod().given(profileState).addScheduleDynamicValues(any(), any());
        DynamicValue<String> dynamicValue = new DynamicValue<>(sourceType, null);
        SpecificTimeSchedule schedule = new SpecificTimeSchedule();
        schedule.setDynamicValue(dynamicValue);

        Assertions.assertThat(entityKeys.isEmpty()).isTrue();
        Assertions.assertThat(schedule.getDynamicValue().getSourceAttribute()).isNull();

        profileState.addScheduleDynamicValues(schedule, entityKeys);

        Assertions.assertThat(entityKeys.isEmpty()).isTrue();
    }

    @ParameterizedTest()
    @EnumSource(value = FilterPredicateType.class,  names = {"COMPLEX"})
    void addDynamicValuesRecursivelySourceAttributeComplexKeyFilter(FilterPredicateType predicateType) {
        willCallRealMethod().given(profileState).addDynamicValuesRecursively(any(), any(), any());
        ComplexFilterPredicate predicate = mock(ComplexFilterPredicate.class, RETURNS_DEEP_STUBS);
        willReturn(predicateType).given(predicate).getType();
        profileState.addDynamicValuesRecursively(predicate, entityKeys, ruleKeys);
        Assertions.assertThat(entityKeys.isEmpty()).isTrue();
        Assertions.assertThat(ruleKeys.isEmpty()).isTrue();
    }

    @ParameterizedTest()
    @EnumSource(value = FilterPredicateType.class,  names = {"STRING", "NUMERIC", "BOOLEAN"})
    void addDynamicValuesRecursivelySourceAttributeIsNull(FilterPredicateType predicateType) {
        willCallRealMethod().given(profileState).addDynamicValuesRecursively(any(), any(), any());
        SimpleKeyFilterPredicate<String> predicate = mock(SimpleKeyFilterPredicate.class, RETURNS_DEEP_STUBS);
        willReturn(predicateType).given(predicate).getType();
        when(predicate.getValue().getDynamicValue().getSourceType()).thenReturn(DynamicValueSourceType.CURRENT_DEVICE);
        when(predicate.getValue().getDynamicValue().getSourceAttribute()).thenReturn(null);
        profileState.addDynamicValuesRecursively(predicate, entityKeys, ruleKeys);
        Assertions.assertThat(entityKeys.isEmpty()).isTrue();
        Assertions.assertThat(ruleKeys.isEmpty()).isTrue();
    }

    @ParameterizedTest()
    @EnumSource(value = FilterPredicateType.class,  names = {"STRING", "NUMERIC", "BOOLEAN"})
    void addDynamicValuesRecursivelySourceAttributeAdded(FilterPredicateType predicateType) {
        willCallRealMethod().given(profileState).addDynamicValuesRecursively(any(), any(), any());
        SimpleKeyFilterPredicate<String> predicate = mock(SimpleKeyFilterPredicate.class, RETURNS_DEEP_STUBS);
        willReturn(predicateType).given(predicate).getType();
        when(predicate.getValue().getDynamicValue().getSourceType()).thenReturn(DynamicValueSourceType.CURRENT_DEVICE);
        when(predicate.getValue().getDynamicValue().getSourceAttribute()).thenReturn("myKey");
        profileState.addDynamicValuesRecursively(predicate, entityKeys, ruleKeys);
        Assertions.assertThat(entityKeys).isEqualTo(Set.of(
                new AlarmConditionFilterKey(AlarmConditionKeyType.ATTRIBUTE, "myKey")));
        Assertions.assertThat(ruleKeys).isEqualTo(Set.of(
                new AlarmConditionFilterKey(AlarmConditionKeyType.ATTRIBUTE, "myKey")));
    }

}
