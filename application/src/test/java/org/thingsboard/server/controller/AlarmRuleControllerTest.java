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
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.rule.AlarmRule;
import org.thingsboard.server.common.data.alarm.rule.AlarmRuleInfo;
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
import org.thingsboard.server.common.data.alarm.rule.filter.AlarmRuleSingleEntityFilter;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class AlarmRuleControllerTest extends AbstractControllerTest {

    private AbstractWebTest.IdComparator<AlarmRuleInfo> idComparator = new AbstractWebTest.IdComparator<>();

    private Tenant savedTenant;
    private User tenantAdmin;

    @Before
    public void beforeTest() throws Exception {
        loginSysAdmin();

        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        savedTenant = doPost("/api/tenant", tenant, Tenant.class);
        Assert.assertNotNull(savedTenant);

        tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(savedTenant.getId());
        tenantAdmin.setEmail("tenant2@thingsboard.org");
        tenantAdmin.setFirstName("Joe");
        tenantAdmin.setLastName("Downs");

        tenantAdmin = createUserAndLogin(tenantAdmin, "testPassword1");
    }

    @After
    public void afterTest() throws Exception {
        loginSysAdmin();

        doDelete("/api/tenant/" + savedTenant.getId().getId().toString()).andExpect(status().isOk());
    }

    @Test
    public void testSaveAlarmRule() throws Exception {
        Device device = createDevice();
        AlarmRule alarmRule = createAlarmRule(device.getId());

        Mockito.reset(tbClusterService, auditLogService);

        AlarmRule savedAlarmRule = doPost("/api/alarmRule", alarmRule, AlarmRule.class);

        testNotifyEntityAllOneTime(savedAlarmRule, savedAlarmRule.getId(), savedAlarmRule.getId(), savedTenant.getId(),
                tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.ADDED);

        Assert.assertNotNull(savedAlarmRule);
        Assert.assertNotNull(savedAlarmRule.getId());
        Assert.assertTrue(savedAlarmRule.getCreatedTime() > 0);
        Assert.assertEquals(savedTenant.getId(), savedAlarmRule.getTenantId());
        Assert.assertEquals(alarmRule.getName(), savedAlarmRule.getName());

        Mockito.reset(tbClusterService, auditLogService);

        savedAlarmRule.setName("My new alarmRule");
        doPost("/api/alarmRule", savedAlarmRule, Asset.class);

        testNotifyEntityAllOneTime(savedAlarmRule, savedAlarmRule.getId(), savedAlarmRule.getId(), savedTenant.getId(),
                tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.UPDATED);

        AlarmRule foundAlarmRule = doGet("/api/alarmRule/" + savedAlarmRule.getId().getId().toString(), AlarmRule.class);
        Assert.assertEquals(foundAlarmRule.getName(), savedAlarmRule.getName());
    }

    @Test
    public void testUpdateAlarmRuleFromDifferentTenant() throws Exception {
        Device device = createDevice();
        AlarmRule alarmRule = createAlarmRule(device.getId());

        Mockito.reset(tbClusterService, auditLogService);

        AlarmRule savedAlarmRule = doPost("/api/alarmRule", alarmRule, AlarmRule.class);

        loginDifferentTenant();

        Mockito.reset(tbClusterService, auditLogService);

        savedAlarmRule.setName("My new alarmRule");
        doPost("/api/alarmRule", savedAlarmRule)
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermission)));

        testNotifyEntityNever(savedAlarmRule.getId(), savedAlarmRule);

        doDelete("/api/alarmRule/" + savedAlarmRule.getId().getId().toString())
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermission)));

        testNotifyEntityNever(savedAlarmRule.getId(), savedAlarmRule);
    }

    @Test
    public void testSaveAlarmRuleWithDeviceFromDifferentTenant() throws Exception {
        loginDifferentTenant();
        Device device = createDevice();

        AlarmRule alarmRule = createAlarmRule(device.getId());

        loginTenantAdmin();

        doPost("/api/alarmRule", alarmRule).andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Can't use non-existent DEVICE in Alarm Rule SINGLE_ENTITY filter!")));
    }

    @Test
    public void testFindAlarmRuleById() throws Exception {
        Device device = createDevice();
        AlarmRule alarmRule = createAlarmRule(device.getId());

        AlarmRule savedAlarmRule = doPost("/api/alarmRule", alarmRule, AlarmRule.class);
        AlarmRule foundAlarmRule = doGet("/api/alarmRule/" + savedAlarmRule.getId().getId().toString(), AlarmRule.class);
        Assert.assertNotNull(foundAlarmRule);
        Assert.assertEquals(savedAlarmRule, foundAlarmRule);
    }

    @Test
    public void testDeleteAlarmRule() throws Exception {
        Device device = createDevice();
        AlarmRule alarmRule = createAlarmRule(device.getId());

        AlarmRule savedAlarmRule = doPost("/api/alarmRule", alarmRule, AlarmRule.class);

        System.out.println(savedAlarmRule);

        Mockito.reset(tbClusterService, auditLogService);

        doDelete("/api/alarmRule/" + savedAlarmRule.getId().getId().toString())
                .andExpect(status().isOk());

        testNotifyEntityAllOneTime(savedAlarmRule, savedAlarmRule.getId(), savedAlarmRule.getId(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.DELETED, savedAlarmRule.getId().getId().toString());

        String alarmRuleIdStr = savedAlarmRule.getId().getId().toString();
        doGet("/api/alarmRule/" + alarmRuleIdStr)
                .andExpect(status().isNotFound())
                .andExpect(statusReason(containsString(msgErrorNoFound("Alarm rule", alarmRuleIdStr))));
    }

    @Test
    public void testFindAllAlarmRules() throws Exception {
        Device device = createDevice();

        List<AlarmRuleInfo> rules = new ArrayList<>();
        int cntEntity = 178;

        Mockito.reset(tbClusterService, auditLogService);

        for (int i = 0; i < cntEntity; i++) {
            AlarmRule alarmRule = createAlarmRule(device.getId(), " " + i);
            AlarmRule savedAlarmRule = doPost("/api/alarmRule", alarmRule, AlarmRule.class);
            rules.add(new AlarmRuleInfo(savedAlarmRule));
        }
        List<AlarmRuleInfo> loadedRules = new ArrayList<>();
        PageLink pageLink = new PageLink(23);
        PageData<AlarmRuleInfo> pageData;
        do {
            pageData = doGetTypedWithPageLink("/api/alarmRuleInfos?",
                    new TypeReference<>() {
                    }, pageLink);
            loadedRules.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        rules.sort(idComparator);
        loadedRules.sort(idComparator);

        Assert.assertEquals(rules, loadedRules);
    }

    private Device createDevice() {
        Device device = new Device();
        device.setName("My device");
        device.setType("default");
        device = doPost("/api/device", device, Device.class);
        return device;
    }

    private AlarmRule createAlarmRule(EntityId entityId) {
        return createAlarmRule(entityId, "");
    }

    private AlarmRule createAlarmRule(EntityId entityId, String prefix) {
        AlarmRule alarmRule = new AlarmRule();
        alarmRule.setTenantId(tenantId);
        alarmRule.setAlarmType("My Type" + prefix);
        alarmRule.setName("My AlarmRule" + prefix);
        alarmRule.setEnabled(true);

        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setSpec(new SimpleAlarmConditionSpec());

        var boolKey = new FromMessageArgument(AlarmConditionKeyType.ATTRIBUTE, "bool", ArgumentValueType.BOOLEAN);
        var boolConst = new ConstantArgument(ArgumentValueType.BOOLEAN, Boolean.TRUE);

        SimpleAlarmConditionFilter alarmConditionFilter = new SimpleAlarmConditionFilter();
        alarmConditionFilter.setLeftArgId("boolKey");
        alarmConditionFilter.setRightArgId("boolConst");
        alarmConditionFilter.setOperation(ArgumentOperation.EQUAL);
        alarmCondition.setConditionFilter(alarmConditionFilter);

        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setAlarmCondition(alarmCondition);
        AlarmRuleConfiguration alarmRuleConfiguration = new AlarmRuleConfiguration();
        alarmRuleConfiguration.setArguments(Map.of("boolKey", boolKey, "boolConst", boolConst));

        AlarmRuleSingleEntityFilter sourceFilter = new AlarmRuleSingleEntityFilter(entityId);
        alarmRuleConfiguration.setSourceEntityFilters(Collections.singletonList(sourceFilter));

        alarmRuleConfiguration.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRuleCondition)));

        alarmRule.setConfiguration(alarmRuleConfiguration);

        alarmRule.setConfiguration(alarmRuleConfiguration);
        return alarmRule;
    }

}
