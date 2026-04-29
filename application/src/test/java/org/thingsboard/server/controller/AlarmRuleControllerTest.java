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
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.rule.AlarmRule;
import org.thingsboard.server.common.data.alarm.rule.condition.SimpleAlarmCondition;
import org.thingsboard.server.common.data.alarm.rule.condition.expression.TbelAlarmConditionExpression;
import org.thingsboard.server.common.data.cf.AlarmRuleDefinition;
import org.thingsboard.server.common.data.cf.AlarmRuleDefinitionInfo;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.AlarmCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.ArgumentType;
import org.thingsboard.server.common.data.cf.configuration.ReferencedEntityKey;
import org.thingsboard.server.common.data.cf.configuration.SimpleCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.TimeSeriesOutput;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class AlarmRuleControllerTest extends AbstractControllerTest {

    private Tenant savedTenant;

    @Before
    public void beforeTest() throws Exception {
        loginSysAdmin();

        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        savedTenant = saveTenant(tenant);
        assertThat(savedTenant).isNotNull();

        User tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(savedTenant.getId());
        tenantAdmin.setEmail("tenant2@thingsboard.org");
        tenantAdmin.setFirstName("Joe");
        tenantAdmin.setLastName("Downs");

        createUserAndLogin(tenantAdmin, "testPassword1");
    }

    @After
    public void afterTest() throws Exception {
        loginSysAdmin();
        deleteTenant(savedTenant.getId());
    }

    @Test
    public void testSaveAlarmRule() throws Exception {
        Device testDevice = createDevice("Test device", "1234567890");
        AlarmRuleDefinition alarmRule = createTestAlarmRule(testDevice.getId(), "High Temperature");

        AlarmRuleDefinition saved = saveAlarmRule(alarmRule);

        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedTime()).isGreaterThan(0);
        assertThat(saved.getTenantId()).isEqualTo(savedTenant.getId());
        assertThat(saved.getEntityId()).isEqualTo(testDevice.getId());
        assertThat(saved.getName()).isEqualTo("High Temperature");
        assertThat(saved.getConfiguration()).isNotNull();
        assertThat(saved.getConfiguration().getCreateRules()).containsKey(AlarmSeverity.CRITICAL);

        saved.setName("Updated Alarm Rule");
        AlarmRuleDefinition updated = saveAlarmRule(saved);

        assertThat(updated.getName()).isEqualTo("Updated Alarm Rule");
        assertThat(updated.getVersion()).isEqualTo(saved.getVersion() + 1);

        doDelete("/api/alarm/rule/" + saved.getId().getId())
                .andExpect(status().isOk());
    }

    @Test
    public void testGetAlarmRuleById() throws Exception {
        Device testDevice = createDevice("Test device", "1234567890");
        AlarmRuleDefinition alarmRule = createTestAlarmRule(testDevice.getId(), "Test Alarm");

        AlarmRuleDefinition saved = saveAlarmRule(alarmRule);
        AlarmRuleDefinition fetched = doGet("/api/alarm/rule/" + saved.getId().getId(), AlarmRuleDefinition.class);

        assertThat(fetched).isNotNull();
        assertThat(fetched).isEqualTo(saved);

        doDelete("/api/alarm/rule/" + saved.getId().getId())
                .andExpect(status().isOk());
    }

    @Test
    public void testGetAlarmRuleById_notFound() throws Exception {
        doGet("/api/alarm/rule/" + UUID.randomUUID())
                .andExpect(status().isNotFound());
    }

    @Test
    public void testGetAlarmRuleById_calculatedFieldNotAlarm() throws Exception {
        Device testDevice = createDevice("Test device", "1234567890");
        CalculatedField cf = createSimpleCalculatedField(testDevice.getId());
        CalculatedField savedCf = doPost("/api/calculatedField", cf, CalculatedField.class);

        doGet("/api/alarm/rule/" + savedCf.getId().getId())
                .andExpect(status().isNotFound());

        doDelete("/api/calculatedField/" + savedCf.getId().getId())
                .andExpect(status().isOk());
    }

    @Test
    public void testGetAlarmRulesByEntityId() throws Exception {
        Device device1 = createDevice("Device 1", "1234567890");
        Device device2 = createDevice("Device 2", "0987654321");
        AlarmRuleDefinition rule1 = saveAlarmRule(createTestAlarmRule(device1.getId(), "Rule 1"));
        saveAlarmRule(createTestAlarmRule(device2.getId(), "Rule 2"));

        PageData<AlarmRuleDefinition> result = doGetTypedWithPageLink(
                "/api/alarm/rules/" + EntityType.DEVICE + "/" + device1.getUuidId() + "?",
                new TypeReference<>() {}, new PageLink(10));

        assertThat(result.getData()).hasSize(1);
        assertThat(result.getData().get(0).getId()).isEqualTo(rule1.getId());
        assertThat(result.getData().get(0).getName()).isEqualTo("Rule 1");
    }

    @Test
    public void testGetAlarmRules() throws Exception {
        Device device = createDevice("Device A", "1234567890");
        AlarmRuleDefinition deviceRule = saveAlarmRule(createTestAlarmRule(device.getId(), "Device Alarm"));

        DeviceProfile profile = doPost("/api/deviceProfile", createDeviceProfile("Profile A"), DeviceProfile.class);
        AlarmRuleDefinition profileRule = saveAlarmRule(createTestAlarmRule(profile.getId(), "Profile Alarm"));

        // All alarm rules
        List<AlarmRuleDefinitionInfo> all = getAlarmRules(null, null);
        assertThat(all).extracting(AlarmRuleDefinition::getName)
                .contains("Device Alarm", "Profile Alarm");

        // Filter by entity type: DEVICE
        List<AlarmRuleDefinitionInfo> deviceRules = getAlarmRules(EntityType.DEVICE, null);
        assertThat(deviceRules).extracting(AlarmRuleDefinition::getName)
                .containsOnly("Device Alarm");

        // Filter by entity type: DEVICE_PROFILE
        List<AlarmRuleDefinitionInfo> profileRules = getAlarmRules(EntityType.DEVICE_PROFILE, null);
        assertThat(profileRules).extracting(AlarmRuleDefinition::getName)
                .containsOnly("Profile Alarm");

        // Filter by specific entity IDs
        List<AlarmRuleDefinitionInfo> specificRules = getAlarmRules(EntityType.DEVICE, List.of(device.getUuidId()));
        assertThat(specificRules).extracting(AlarmRuleDefinition::getName)
                .containsOnly("Device Alarm");

        // Verify entity names are populated
        AlarmRuleDefinitionInfo deviceInfo = all.stream()
                .filter(r -> r.getName().equals("Device Alarm")).findFirst().orElseThrow();
        assertThat(deviceInfo.getEntityName()).isEqualTo("Device A");

        AlarmRuleDefinitionInfo profileInfo = all.stream()
                .filter(r -> r.getName().equals("Profile Alarm")).findFirst().orElseThrow();
        assertThat(profileInfo.getEntityName()).isEqualTo("Profile A");
    }

    @Test
    public void testGetAlarmRules_textSearch() throws Exception {
        Device device = createDevice("Device A", "1234567890");
        saveAlarmRule(createTestAlarmRule(device.getId(), "Temperature Alarm"));
        saveAlarmRule(createTestAlarmRule(device.getId(), "Humidity Alarm"));

        PageData<AlarmRuleDefinitionInfo> result = doGetTypedWithPageLink(
                "/api/alarm/rules?textSearch=Temp&",
                new TypeReference<>() {}, new PageLink(10));

        assertThat(result.getData()).hasSize(1);
        assertThat(result.getData().get(0).getName()).isEqualTo("Temperature Alarm");
    }

    @Test
    public void testGetAlarmRuleNames() throws Exception {
        Device device = createDevice("Device A", "1234567890");
        saveAlarmRule(createTestAlarmRule(device.getId(), "Alpha Alarm"));
        saveAlarmRule(createTestAlarmRule(device.getId(), "Beta Alarm"));

        PageData<String> names = getAlarmRuleNames(new PageLink(10, 0,
                null, new SortOrder("", SortOrder.Direction.ASC)));
        assertThat(names.getTotalElements()).isEqualTo(2);
        assertThat(names.getData()).isSortedAccordingTo(Comparator.naturalOrder());
        assertThat(names.getData()).contains("Alpha Alarm", "Beta Alarm");

        names = getAlarmRuleNames(new PageLink(10, 0,
                null, new SortOrder("", SortOrder.Direction.DESC)));
        assertThat(names.getData()).isSortedAccordingTo(Comparator.reverseOrder());

        names = getAlarmRuleNames(new PageLink(10, 0,
                "Alpha", new SortOrder("", SortOrder.Direction.ASC)));
        assertThat(names.getTotalElements()).isEqualTo(1);
        assertThat(names.getData()).containsOnly("Alpha Alarm");
    }

    @Test
    public void testDeleteAlarmRule() throws Exception {
        Device testDevice = createDevice("Test device", "1234567890");
        AlarmRuleDefinition saved = saveAlarmRule(createTestAlarmRule(testDevice.getId(), "To Delete"));

        assertThat(saved).isNotNull();

        doDelete("/api/alarm/rule/" + saved.getId().getId())
                .andExpect(status().isOk());
        doGet("/api/alarm/rule/" + saved.getId().getId())
                .andExpect(status().isNotFound());
    }

    @Test
    public void testDeleteAlarmRule_notFound() throws Exception {
        doDelete("/api/alarm/rule/" + UUID.randomUUID())
                .andExpect(status().isNotFound());
    }

    @Test
    public void testDeleteAlarmRule_calculatedFieldNotAlarm() throws Exception {
        Device testDevice = createDevice("Test device", "1234567890");
        CalculatedField cf = createSimpleCalculatedField(testDevice.getId());
        CalculatedField savedCf = doPost("/api/calculatedField", cf, CalculatedField.class);

        doDelete("/api/alarm/rule/" + savedCf.getId().getId())
                .andExpect(status().isNotFound());

        doDelete("/api/calculatedField/" + savedCf.getId().getId())
                .andExpect(status().isOk());
    }

    @Test
    public void testGetLatestAlarmRuleDebugEvent() throws Exception {
        Device testDevice = createDevice("Test device", "1234567890");
        AlarmRuleDefinition saved = saveAlarmRule(createTestAlarmRule(testDevice.getId(), "Debug Test"));

        doGet("/api/alarm/rule/" + saved.getId().getId() + "/debug")
                .andExpect(status().isOk());

        doDelete("/api/alarm/rule/" + saved.getId().getId())
                .andExpect(status().isOk());
    }

    @Test
    public void testGetLatestAlarmRuleDebugEvent_notFound() throws Exception {
        doGet("/api/alarm/rule/" + UUID.randomUUID() + "/debug")
                .andExpect(status().isNotFound());
    }

    @Test
    public void testTestAlarmRuleScript() throws Exception {
        JsonNode request = JacksonUtil.toJsonNode("""
                {
                  "expression": "return temperature > 50;",
                  "arguments": {
                    "temperature": { "type": "SINGLE_VALUE", "ts": 1739776478057, "value": 55 }
                  }
                }
                """);

        JsonNode result = doPost("/api/alarm/rule/testScript", request, JsonNode.class);

        assertThat(result).isNotNull();
        assertThat(result.has("output")).isTrue();
        assertThat(result.has("error")).isTrue();
        assertThat(result.get("error").asText()).isEmpty();
        assertThat(result.get("output").asText()).isEqualTo("true");
    }

    @Test
    public void testTestAlarmRuleScript_returnsFalse() throws Exception {
        JsonNode request = JacksonUtil.toJsonNode("""
                {
                  "expression": "return temperature > 50;",
                  "arguments": {
                    "temperature": { "type": "SINGLE_VALUE", "ts": 1739776478057, "value": 30 }
                  }
                }
                """);

        JsonNode result = doPost("/api/alarm/rule/testScript", request, JsonNode.class);

        assertThat(result).isNotNull();
        assertThat(result.get("error").asText()).isEmpty();
        assertThat(result.get("output").asText()).isEqualTo("false");
    }

    @Test
    public void testTestAlarmRuleScript_missingExpression() throws Exception {
        JsonNode request = JacksonUtil.toJsonNode("""
                {
                  "arguments": {}
                }
                """);

        doPost("/api/alarm/rule/testScript", request)
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testTestAlarmRuleScript_invalidExpression() throws Exception {
        JsonNode request = JacksonUtil.toJsonNode("""
                {
                  "expression": "invalid syntax {{{{",
                  "arguments": {}
                }
                """);

        JsonNode result = doPost("/api/alarm/rule/testScript", request, JsonNode.class);

        assertThat(result).isNotNull();
        assertThat(result.get("error").asText()).isNotEmpty();
    }

    // --- Helper methods ---

    private AlarmRuleDefinition createTestAlarmRule(EntityId entityId, String name) {
        AlarmRuleDefinition alarmRule = new AlarmRuleDefinition();
        alarmRule.setEntityId(entityId);
        alarmRule.setName(name);
        alarmRule.setConfigurationVersion(1);
        alarmRule.setAdditionalInfo(JacksonUtil.newObjectNode());

        AlarmCalculatedFieldConfiguration configuration = new AlarmCalculatedFieldConfiguration();

        Argument argument = new Argument();
        argument.setRefEntityKey(new ReferencedEntityKey("temperature", ArgumentType.TS_LATEST, null));
        argument.setDefaultValue("0");
        configuration.setArguments(Map.of("temperature", argument));

        AlarmRule rule = new AlarmRule();
        TbelAlarmConditionExpression expression = new TbelAlarmConditionExpression();
        expression.setExpression("return temperature >= 50;");
        SimpleAlarmCondition condition = new SimpleAlarmCondition();
        condition.setExpression(expression);
        rule.setCondition(condition);
        configuration.setCreateRules(Map.of(AlarmSeverity.CRITICAL, rule));

        alarmRule.setConfiguration(configuration);
        return alarmRule;
    }

    private CalculatedField createSimpleCalculatedField(EntityId entityId) {
        CalculatedField cf = new CalculatedField();
        cf.setEntityId(entityId);
        cf.setType(CalculatedFieldType.SIMPLE);
        cf.setName("Simple CF");
        cf.setConfigurationVersion(1);
        cf.setAdditionalInfo(JacksonUtil.newObjectNode());

        SimpleCalculatedFieldConfiguration config = new SimpleCalculatedFieldConfiguration();
        Argument arg = new Argument();
        arg.setRefEntityKey(new ReferencedEntityKey("temperature", ArgumentType.TS_LATEST, null));
        config.setArguments(Map.of("T", arg));
        config.setExpression("T * 2");
        TimeSeriesOutput output = new TimeSeriesOutput();
        output.setName("result");
        config.setOutput(output);
        cf.setConfiguration(config);

        return cf;
    }

    private List<AlarmRuleDefinitionInfo> getAlarmRules(EntityType entityType, List<UUID> entities) throws Exception {
        StringBuilder url = new StringBuilder("/api/alarm/rules?");
        if (entityType != null) {
            url.append("entityType=").append(entityType).append("&");
        }
        if (entities != null) {
            url.append("entities=").append(String.join(",",
                    entities.stream().map(UUID::toString).toList())).append("&");
        }
        return doGetTypedWithPageLink(url.toString(),
                new TypeReference<PageData<AlarmRuleDefinitionInfo>>() {}, new PageLink(10)).getData();
    }

    private PageData<String> getAlarmRuleNames(PageLink pageLink) throws Exception {
        return doGetTypedWithPageLink("/api/alarm/rules/names?",
                new TypeReference<PageData<String>>() {}, pageLink);
    }

}
