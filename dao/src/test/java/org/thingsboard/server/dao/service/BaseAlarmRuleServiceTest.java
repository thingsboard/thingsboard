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
package org.thingsboard.server.dao.service;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileType;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.alarm.rule.AlarmRule;
import org.thingsboard.server.common.data.alarm.rule.AlarmRuleInfo;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmCondition;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmConditionKeyType;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmRuleCondition;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmRuleConfiguration;
import org.thingsboard.server.common.data.alarm.rule.condition.ArgumentValueType;
import org.thingsboard.server.common.data.alarm.rule.condition.AttributeArgument;
import org.thingsboard.server.common.data.alarm.rule.condition.ComplexAlarmConditionFilter;
import org.thingsboard.server.common.data.alarm.rule.condition.ConstantArgument;
import org.thingsboard.server.common.data.alarm.rule.condition.FromMessageArgument;
import org.thingsboard.server.common.data.alarm.rule.condition.ArgumentOperation;
import org.thingsboard.server.common.data.alarm.rule.condition.SimpleAlarmConditionFilter;
import org.thingsboard.server.common.data.alarm.rule.condition.ValueSourceType;
import org.thingsboard.server.common.data.alarm.rule.filter.AlarmRuleDeviceTypeEntityFilter;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.exception.DataValidationException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.alarm.AlarmSeverity.CRITICAL;
import static org.thingsboard.server.common.data.alarm.rule.condition.ComplexAlarmConditionFilter.ComplexOperation.AND;

@DaoSqlTest
public class BaseAlarmRuleServiceTest extends AbstractServiceTest {

    private static final String ALARM_RULE_NAME = "Alarm Rule";

    @Autowired
    private DeviceProfileService deviceProfileService;

    private IdComparator<AlarmRule> idComparator = new IdComparator<>();
    private IdComparator<AlarmRuleInfo> alarmRuleInfoIdComparator = new IdComparator<>();

    private TenantId tenantId;
    private DeviceProfileId deviceProfileId;

    @Before
    public void before() {
        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        Tenant savedTenant = tenantService.saveTenant(tenant);
        Assert.assertNotNull(savedTenant);
        tenantId = savedTenant.getId();

        DeviceProfile deviceProfile = new DeviceProfile();
        deviceProfile.setTenantId(tenantId);
        deviceProfile.setName("Test Profile");
        deviceProfile.setType(DeviceProfileType.DEFAULT);
        deviceProfile.setTransportType(DeviceTransportType.DEFAULT);

        DeviceProfileData data = new DeviceProfileData();
        data.setTransportConfiguration(new DefaultDeviceProfileTransportConfiguration());
        deviceProfile.setProfileData(data);

        var savedDeviceProfile = deviceProfileService.saveDeviceProfile(deviceProfile);
        Assert.assertNotNull(savedDeviceProfile);
        deviceProfileId = savedDeviceProfile.getId();
    }

    @After
    public void after() {
        tenantService.deleteTenant(tenantId);
    }

    @Test
    public void testSaveAlarmRule() {
        AlarmRule alarmRule = createAlarmRule(tenantId, ALARM_RULE_NAME);
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
        AlarmRule alarmRule = createAlarmRule(tenantId, ALARM_RULE_NAME);
        AlarmRule savedAlarmRule = alarmRuleService.saveAlarmRule(tenantId, alarmRule);
        AlarmRule foundAlarmRule = alarmRuleService.findAlarmRuleById(tenantId, savedAlarmRule.getId());
        Assert.assertNotNull(foundAlarmRule);
        Assert.assertEquals(savedAlarmRule, foundAlarmRule);
    }

    @Test
    public void testFindAlarmRules() {
        List<AlarmRule> alarmRules = new ArrayList<>();

        for (int i = 0; i < 28; i++) {
            AlarmRule alarmRule = createAlarmRule(tenantId, ALARM_RULE_NAME + i);
            alarmRules.add(alarmRuleService.saveAlarmRule(tenantId, alarmRule));
        }

        List<AlarmRule> loadedAlarmRules = new ArrayList<>();
        PageLink pageLink = new PageLink(17);
        PageData<AlarmRule> pageData;
        do {
            pageData = alarmRuleService.findAlarmRules(tenantId, pageLink);
            loadedAlarmRules.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        alarmRules.sort(idComparator);
        loadedAlarmRules.sort(idComparator);

        Assert.assertEquals(alarmRules, loadedAlarmRules);
    }

    @Test
    public void testFindAlarmRuleInfos() {
        List<AlarmRule> alarmRules = new ArrayList<>();

        for (int i = 0; i < 28; i++) {
            AlarmRule alarmRule = createAlarmRule(tenantId, ALARM_RULE_NAME + i);
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

    @Test
    public void testFindAlarmRuleByName() {
        AlarmRule alarmRule = createAlarmRule(tenantId, ALARM_RULE_NAME);
        AlarmRule savedAlarmRule = alarmRuleService.saveAlarmRule(tenantId, alarmRule);
        AlarmRule foundAlarmRule = alarmRuleService.findAlarmRuleByName(tenantId, savedAlarmRule.getName());
        Assert.assertNotNull(foundAlarmRule);
        Assert.assertEquals(savedAlarmRule, foundAlarmRule);
    }

    @Test()
    public void testSaveAlarmRuleWithEmptyName() {
        AlarmRule alarmRule = createAlarmRule(tenantId, "");
        Assertions.assertThrows(
                DataValidationException.class,
                () -> alarmRuleService.saveAlarmRule(tenantId, alarmRule)
        );
    }

    @Test
    public void testSaveAlarmRuleWithEmptyAlarmType() {
        AlarmRule alarmRule = createAlarmRule(tenantId, ALARM_RULE_NAME);
        alarmRule.setAlarmType("");
        Assertions.assertThrows(
                DataValidationException.class,
                () -> alarmRuleService.saveAlarmRule(tenantId, alarmRule)
        );
    }

    @Test
    public void testSaveAlarmRuleWithEmptyTenantId() {
        AlarmRule alarmRule = createAlarmRule(null, ALARM_RULE_NAME);
        Assertions.assertThrows(
                DataValidationException.class,
                () -> alarmRuleService.saveAlarmRule(tenantId, alarmRule)
        );
    }

    @Test
    public void testSaveAlarmRuleWithEmptyConfiguration() {
        AlarmRule alarmRule = createAlarmRule(tenantId, ALARM_RULE_NAME);
        alarmRule.setConfiguration(null);
        Assertions.assertThrows(
                DataValidationException.class,
                () -> alarmRuleService.saveAlarmRule(tenantId, alarmRule)
        );
    }

    @Test
    public void testSaveAlarmRuleWithEmptySourceFilter() {
        AlarmRule alarmRule = createAlarmRule(tenantId, ALARM_RULE_NAME);
        var configuration = alarmRule.getConfiguration();
        configuration.setSourceEntityFilters(Collections.emptyList());
        Assertions.assertThrows(
                DataValidationException.class,
                () -> alarmRuleService.saveAlarmRule(tenantId, alarmRule)
        );
    }

    @Test
    public void testSaveAlarmRuleWithEmptyEntityIdInSourceFilter() {
        AlarmRule alarmRule = createAlarmRule(tenantId, ALARM_RULE_NAME);
        var configuration = alarmRule.getConfiguration();
        configuration.setSourceEntityFilters(List.of(new AlarmRuleDeviceTypeEntityFilter(null)));
        Assertions.assertThrows(
                DataValidationException.class,
                () -> alarmRuleService.saveAlarmRule(tenantId, alarmRule)
        );
    }

    @Test
    public void testSaveAlarmRuleWithEmptyCreateRules() {
        AlarmRule alarmRule = createAlarmRule(tenantId, ALARM_RULE_NAME);
        var configuration = alarmRule.getConfiguration();
        configuration.setCreateRules(null);
        Assertions.assertThrows(
                DataValidationException.class,
                () -> alarmRuleService.saveAlarmRule(tenantId, alarmRule)
        );
    }

    @Test
    public void testMaxAlarmRuleConditionDepth() {
        AlarmRule alarmRule = new AlarmRule();
        alarmRule.setTenantId(tenantId);
        alarmRule.setAlarmType(ALARM_RULE_NAME + "Alarm");
        alarmRule.setName(ALARM_RULE_NAME);

        var complexFilterRoot = new ComplexAlarmConditionFilter(
                List.of(new ComplexAlarmConditionFilter(
                        List.of(new ComplexAlarmConditionFilter(
                                List.of(new ComplexAlarmConditionFilter(
                                        List.of(new ComplexAlarmConditionFilter(
                                                List.of(new ComplexAlarmConditionFilter(
                                                        List.of(new SimpleAlarmConditionFilter()), AND)), AND)), AND)), AND)), AND)), AND);

        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setConditionFilter(complexFilterRoot);
        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();

        alarmRuleCondition.setAlarmCondition(alarmCondition);
        AlarmRuleConfiguration alarmRuleConfiguration = new AlarmRuleConfiguration();

        AlarmRuleDeviceTypeEntityFilter sourceFilter = new AlarmRuleDeviceTypeEntityFilter(List.of(deviceProfileId));
        alarmRuleConfiguration.setSourceEntityFilters(Collections.singletonList(sourceFilter));

        alarmRuleConfiguration.setCreateRules(new TreeMap<>(Collections.singletonMap(CRITICAL, alarmRuleCondition)));

        alarmRule.setConfiguration(alarmRuleConfiguration);

        Assertions.assertThrows(
                DataValidationException.class,
                () -> alarmRuleService.saveAlarmRule(tenantId, alarmRule)
        );
    }

    @Test
    public void testSaveAlarmRuleWithEmptyArgument() {
        AlarmRule alarmRule = createAlarmRule(tenantId, ALARM_RULE_NAME);
        var configuration = alarmRule.getConfiguration();
        var complex = (ComplexAlarmConditionFilter) configuration.getCreateRules().get(CRITICAL).getAlarmCondition().getConditionFilter();
        var simple = (SimpleAlarmConditionFilter) complex.getConditions().get(0);
        simple.setRightArgId(null);

        Assertions.assertThrows(
                DataValidationException.class,
                () -> alarmRuleService.saveAlarmRule(tenantId, alarmRule)
        );
    }

    @Test
    public void testSaveAlarmRuleWithUnknownArgument() {
        AlarmRule alarmRule = createAlarmRule(tenantId, ALARM_RULE_NAME);
        var configuration = alarmRule.getConfiguration();
        var complex = (ComplexAlarmConditionFilter) configuration.getCreateRules().get(CRITICAL).getAlarmCondition().getConditionFilter();
        var simple = (SimpleAlarmConditionFilter) complex.getConditions().get(0);
        simple.setRightArgId("unknownArgumentId");

        Assertions.assertThrows(
                DataValidationException.class,
                () -> alarmRuleService.saveAlarmRule(tenantId, alarmRule)
        );
    }

    @Test
    public void testSaveAlarmRuleWithArgumentDifferentValueTypes() {
        AlarmRule alarmRule = createAlarmRule(tenantId, ALARM_RULE_NAME);
        var temperatureKey = new FromMessageArgument(AlarmConditionKeyType.TIME_SERIES, "temperature", ArgumentValueType.STRING);
        var configuration = alarmRule.getConfiguration();
        var arguments = new HashMap<>(configuration.getArguments());
        arguments.put("temperatureKey", temperatureKey);
        configuration.setArguments(arguments);

        Assertions.assertThrows(
                DataValidationException.class,
                () -> alarmRuleService.saveAlarmRule(tenantId, alarmRule)
        );
    }

    @Test
    public void testSaveAlarmRuleWithEmptyOperation() {
        AlarmRule alarmRule = createAlarmRule(tenantId, ALARM_RULE_NAME);
        var configuration = alarmRule.getConfiguration();
        var complex = (ComplexAlarmConditionFilter) configuration.getCreateRules().get(CRITICAL).getAlarmCondition().getConditionFilter();
        var simple = (SimpleAlarmConditionFilter) complex.getConditions().get(0);
        simple.setOperation(null);

        Assertions.assertThrows(
                DataValidationException.class,
                () -> alarmRuleService.saveAlarmRule(tenantId, alarmRule)
        );
    }

    @Test
    public void testSaveAlarmRuleWithNotSupportedOperation() {
        AlarmRule alarmRule = createAlarmRule(tenantId, ALARM_RULE_NAME);
        var configuration = alarmRule.getConfiguration();
        var complex = (ComplexAlarmConditionFilter) configuration.getCreateRules().get(CRITICAL).getAlarmCondition().getConditionFilter();
        var simple = (SimpleAlarmConditionFilter) complex.getConditions().get(0);
        simple.setOperation(ArgumentOperation.CONTAINS);

        Assertions.assertThrows(
                DataValidationException.class,
                () -> alarmRuleService.saveAlarmRule(tenantId, alarmRule)
        );
    }

    private AlarmRule createAlarmRule(TenantId tenantId, String name) {
        AlarmRule alarmRule = new AlarmRule();
        alarmRule.setTenantId(tenantId);
        alarmRule.setAlarmType(name + "Alarm");
        alarmRule.setName(name);

        var alarmEnabledConst = new ConstantArgument(ArgumentValueType.BOOLEAN, Boolean.TRUE);
        var alarmEnabledKey = new AttributeArgument("alarmEnabled", ArgumentValueType.BOOLEAN, ValueSourceType.CURRENT_ENTITY, Boolean.FALSE, false);

        SimpleAlarmConditionFilter alarmEnabledFilter = new SimpleAlarmConditionFilter();
        alarmEnabledFilter.setLeftArgId("alarmEnabledConst");
        alarmEnabledFilter.setRightArgId("alarmEnabledKey");
        alarmEnabledFilter.setOperation(ArgumentOperation.EQUAL);

        var temperatureKey = new FromMessageArgument(AlarmConditionKeyType.TIME_SERIES, "temperature", ArgumentValueType.NUMERIC);
        var temperatureConst = new ConstantArgument(ArgumentValueType.NUMERIC, 20.0);

        SimpleAlarmConditionFilter temperatureFilter = new SimpleAlarmConditionFilter();
        temperatureFilter.setLeftArgId("temperatureKey");
        temperatureFilter.setRightArgId("temperatureConst");
        temperatureFilter.setOperation(ArgumentOperation.GREATER);

        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setConditionFilter(new ComplexAlarmConditionFilter(Arrays.asList(alarmEnabledFilter, temperatureFilter), ComplexAlarmConditionFilter.ComplexOperation.AND));
        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();

        alarmRuleCondition.setAlarmCondition(alarmCondition);
        AlarmRuleConfiguration alarmRuleConfiguration = new AlarmRuleConfiguration();
        alarmRuleConfiguration.setArguments(Map.of(
                "alarmEnabledConst", alarmEnabledConst,
                "alarmEnabledKey", alarmEnabledKey,
                "temperatureKey", temperatureKey,
                "temperatureConst", temperatureConst));

        AlarmRuleDeviceTypeEntityFilter sourceFilter = new AlarmRuleDeviceTypeEntityFilter(List.of(deviceProfileId));
        alarmRuleConfiguration.setSourceEntityFilters(Collections.singletonList(sourceFilter));

        alarmRuleConfiguration.setCreateRules(new TreeMap<>(Collections.singletonMap(CRITICAL, alarmRuleCondition)));

        alarmRule.setConfiguration(alarmRuleConfiguration);
        return alarmRule;
    }
}
