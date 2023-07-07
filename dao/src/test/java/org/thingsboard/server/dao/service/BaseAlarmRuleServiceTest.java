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
package org.thingsboard.server.dao.service;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.rule.AlarmRule;
import org.thingsboard.server.common.data.alarm.rule.AlarmRuleInfo;
import org.thingsboard.server.common.data.alarm.rule.AlarmRuleOriginatorTargetEntity;
import org.thingsboard.server.common.data.alarm.rule.filter.AlarmRuleDeviceTypeEntityFilter;
import org.thingsboard.server.common.data.device.profile.AlarmCondition;
import org.thingsboard.server.common.data.device.profile.AlarmConditionFilter;
import org.thingsboard.server.common.data.device.profile.AlarmConditionFilterKey;
import org.thingsboard.server.common.data.device.profile.AlarmConditionKeyType;
import org.thingsboard.server.common.data.device.profile.AlarmRuleCondition;
import org.thingsboard.server.common.data.device.profile.AlarmRuleConfiguration;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.query.BooleanFilterPredicate;
import org.thingsboard.server.common.data.query.DynamicValue;
import org.thingsboard.server.common.data.query.DynamicValueSourceType;
import org.thingsboard.server.common.data.query.EntityKeyValueType;
import org.thingsboard.server.common.data.query.FilterPredicateValue;
import org.thingsboard.server.common.data.query.NumericFilterPredicate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;

@DaoSqlTest
public class BaseAlarmRuleServiceTest extends AbstractServiceTest {

    private IdComparator<AlarmRule> idComparator = new IdComparator<>();
    private IdComparator<AlarmRuleInfo> alarmRuleInfoIdComparator = new IdComparator<>();

    private TenantId tenantId;

    @Before
    public void before() {
        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        Tenant savedTenant = tenantService.saveTenant(tenant);
        Assert.assertNotNull(savedTenant);
        tenantId = savedTenant.getId();
    }

    @After
    public void after() {
        tenantService.deleteTenant(tenantId);
    }

    @Test
    public void testSaveAlarmRule() {
        AlarmRule alarmRule = createAlarmRule(tenantId, "Alarm Rule");
        AlarmRule savedAlarmRule = alarmRuleService.saveAlarmRule(tenantId, alarmRule);
        Assert.assertNotNull(savedAlarmRule);
        Assert.assertNotNull(savedAlarmRule.getId());
        Assert.assertTrue(savedAlarmRule.getCreatedTime() > 0);
        Assert.assertEquals(alarmRule.getName(), savedAlarmRule.getName());
        Assert.assertEquals(alarmRule.getDescription(), savedAlarmRule.getDescription());
        Assert.assertEquals(alarmRule.getConfiguration(), savedAlarmRule.getConfiguration());
        Assert.assertEquals(alarmRule.isEnabled(), savedAlarmRule.isEnabled());
        savedAlarmRule.setName("New Alarm Rule");
        alarmRuleService.saveAlarmRule(tenantId, savedAlarmRule);
        AlarmRule foundAlarmRule = alarmRuleService.findAlarmRuleById(tenantId, savedAlarmRule.getId());
        Assert.assertEquals(savedAlarmRule.getName(), foundAlarmRule.getName());
    }

    @Test
    public void testFindAlarmRuleById() {
        AlarmRule alarmRule = createAlarmRule(tenantId, "Alarm Rule");
        AlarmRule savedAlarmRule = alarmRuleService.saveAlarmRule(tenantId, alarmRule);
        AlarmRule foundAlarmRule = alarmRuleService.findAlarmRuleById(tenantId, savedAlarmRule.getId());
        Assert.assertNotNull(foundAlarmRule);
        Assert.assertEquals(savedAlarmRule, foundAlarmRule);
    }

    @Test
    public void testFindDeviceProfileInfos() {
        List<AlarmRule> alarmRules = new ArrayList<>();

        for (int i = 0; i < 28; i++) {
            AlarmRule alarmRule = createAlarmRule(tenantId, "Alarm Rule" + i);
            alarmRules.add(alarmRuleService.saveAlarmRule(tenantId, alarmRule));
        }

        List<AlarmRuleInfo> loadedAlarmRuleInfos = new ArrayList<>();
        PageLink pageLink = new PageLink(17);
        PageData<AlarmRuleInfo> pageData;
        do {
            pageData = alarmRuleService.findAlarmRuleInfos(tenantId, pageLink);
            loadedAlarmRuleInfos.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(alarmRules, idComparator);
        Collections.sort(loadedAlarmRuleInfos, alarmRuleInfoIdComparator);

        List<AlarmRuleInfo> alarmRuleInfos = alarmRules.stream()
                .map(AlarmRuleInfo::new).collect(Collectors.toList());

        Assert.assertEquals(alarmRuleInfos, loadedAlarmRuleInfos);
    }

    private AlarmRule createAlarmRule(TenantId tenantId, String name) {
        AlarmRule alarmRule = new AlarmRule();
        alarmRule.setTenantId(tenantId);
        alarmRule.setAlarmType(name + "Alarm");
        alarmRule.setName(name);

        AlarmConditionFilter alarmEnabledFilter = new AlarmConditionFilter();
        alarmEnabledFilter.setKey(new AlarmConditionFilterKey(AlarmConditionKeyType.CONSTANT, "alarmEnabled"));
        alarmEnabledFilter.setValue(Boolean.TRUE);
        alarmEnabledFilter.setValueType(EntityKeyValueType.BOOLEAN);
        BooleanFilterPredicate alarmEnabledPredicate = new BooleanFilterPredicate();
        alarmEnabledPredicate.setOperation(BooleanFilterPredicate.BooleanOperation.EQUAL);
        alarmEnabledPredicate.setValue(new FilterPredicateValue<>(
                Boolean.FALSE,
                null,
                new DynamicValue<>(DynamicValueSourceType.CURRENT_DEVICE, "alarmEnabled")
        ));
        alarmEnabledFilter.setPredicate(alarmEnabledPredicate);

        AlarmConditionFilter temperatureFilter = new AlarmConditionFilter();
        temperatureFilter.setKey(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"));
        temperatureFilter.setValueType(EntityKeyValueType.NUMERIC);
        NumericFilterPredicate temperaturePredicate = new NumericFilterPredicate();
        temperaturePredicate.setOperation(NumericFilterPredicate.NumericOperation.GREATER);
        temperaturePredicate.setValue(new FilterPredicateValue<>(20.0, null, null));
        temperatureFilter.setPredicate(temperaturePredicate);

        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setCondition(Arrays.asList(alarmEnabledFilter, temperatureFilter));
        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setCondition(alarmCondition);
        AlarmRuleConfiguration alarmRuleConfiguration = new AlarmRuleConfiguration();

        AlarmRuleDeviceTypeEntityFilter sourceFilter = new AlarmRuleDeviceTypeEntityFilter(null);
        alarmRuleConfiguration.setSourceEntityFilters(Collections.singletonList(sourceFilter));
        alarmRuleConfiguration.setAlarmTargetEntity(new AlarmRuleOriginatorTargetEntity());

        alarmRuleConfiguration.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRuleCondition)));

        alarmRule.setConfiguration(alarmRuleConfiguration);
        return alarmRule;
    }

}
