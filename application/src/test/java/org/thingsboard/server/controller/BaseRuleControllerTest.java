/**
 * Copyright © 2016-2017 The Thingsboard Authors
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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.plugin.PluginMetaData;
import org.thingsboard.server.common.data.rule.RuleMetaData;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.extensions.core.plugin.telemetry.TelemetryStoragePlugin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public abstract class BaseRuleControllerTest extends AbstractControllerTest {

    private IdComparator<RuleMetaData> idComparator = new IdComparator<>();

    private static final ObjectMapper mapper = new ObjectMapper();
    private Tenant savedTenant;
    private User tenantAdmin;
    private PluginMetaData sysPlugin;
    private PluginMetaData tenantPlugin;

    @Before
    public void beforeTest() throws Exception {
        loginSysAdmin();

        sysPlugin = new PluginMetaData();
        sysPlugin.setName("Sys plugin");
        sysPlugin.setApiToken("sysplugin");
        sysPlugin.setConfiguration(mapper.readTree("{}"));
        sysPlugin.setClazz(TelemetryStoragePlugin.class.getName());
        sysPlugin = doPost("/api/plugin", sysPlugin, PluginMetaData.class);

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

        tenantPlugin = new PluginMetaData();
        tenantPlugin.setName("My plugin");
        tenantPlugin.setApiToken("myplugin");
        tenantPlugin.setConfiguration(mapper.readTree("{}"));
        tenantPlugin.setClazz(TelemetryStoragePlugin.class.getName());
        tenantPlugin = doPost("/api/plugin", tenantPlugin, PluginMetaData.class);
    }

    @After
    public void afterTest() throws Exception {
        loginSysAdmin();

        doDelete("/api/tenant/" + savedTenant.getId().getId().toString())
                .andExpect(status().isOk());

        doDelete("/api/plugin/" + sysPlugin.getId().getId()).andExpect(status().isOk());
    }

    @Test
    public void testSaveRule() throws Exception {
        RuleMetaData rule = new RuleMetaData();
        doPost("/api/rule", rule).andExpect(status().isBadRequest());
        rule.setName("My Rule");
        doPost("/api/rule", rule).andExpect(status().isBadRequest());
        rule.setPluginToken(tenantPlugin.getApiToken());
        doPost("/api/rule", rule).andExpect(status().isBadRequest());
        rule.setFilters(mapper.readTree("[{\"clazz\":\"org.thingsboard.server.extensions.core.filter.MsgTypeFilter\", " +
                "\"name\":\"TelemetryFilter\", " +
                "\"configuration\": {\"messageTypes\":[\"POST_TELEMETRY\",\"POST_ATTRIBUTES\",\"GET_ATTRIBUTES\"]}}]"));
        doPost("/api/rule", rule).andExpect(status().isBadRequest());
        rule.setAction(mapper.readTree("{\"clazz\":\"org.thingsboard.server.extensions.core.action.telemetry.TelemetryPluginAction\", \"name\":\"TelemetryMsgConverterAction\", \"configuration\":{\"timeUnit\":\"DAYS\", \"ttlValue\":1}}"));

        RuleMetaData savedRule = doPost("/api/rule", rule, RuleMetaData.class);
        Assert.assertNotNull(savedRule);
        Assert.assertNotNull(savedRule.getId());
        Assert.assertTrue(savedRule.getCreatedTime() > 0);
        Assert.assertEquals(savedTenant.getId(), savedRule.getTenantId());
    }

    @Test
    public void testFindRuleById() throws Exception {
        RuleMetaData rule = createRuleMetaData(tenantPlugin);
        RuleMetaData savedRule = doPost("/api/rule", rule, RuleMetaData.class);

        RuleMetaData foundRule = doGet("/api/rule/" + savedRule.getId().getId().toString(), RuleMetaData.class);
        Assert.assertNotNull(foundRule);
        Assert.assertEquals(savedRule, foundRule);
    }

    @Test
    public void testFindRuleByPluginToken() throws Exception {
        RuleMetaData rule = createRuleMetaData(tenantPlugin);
        RuleMetaData savedRule = doPost("/api/rule", rule, RuleMetaData.class);

        List<RuleMetaData> foundRules = doGetTyped("/api/rule/token/" + savedRule.getPluginToken(),
                new TypeReference<List<RuleMetaData>>() {
                });
        Assert.assertNotNull(foundRules);
        Assert.assertEquals(1, foundRules.size());
        Assert.assertEquals(savedRule, foundRules.get(0));
    }

    @Test
    public void testActivateRule() throws Exception {
        RuleMetaData rule = createRuleMetaData(tenantPlugin);
        RuleMetaData savedRule = doPost("/api/rule", rule, RuleMetaData.class);

        doPost("/api/rule/" + savedRule.getId().getId().toString() + "/activate").andExpect(status().isBadRequest());

        doPost("/api/plugin/" + tenantPlugin.getId().getId().toString() + "/activate").andExpect(status().isOk());

        doPost("/api/rule/" + savedRule.getId().getId().toString() + "/activate").andExpect(status().isOk());
    }

    @Test
    public void testSuspendRule() throws Exception {
        RuleMetaData rule = createRuleMetaData(tenantPlugin);
        RuleMetaData savedRule = doPost("/api/rule", rule, RuleMetaData.class);

        doPost("/api/plugin/" + tenantPlugin.getId().getId().toString() + "/activate").andExpect(status().isOk());
        doPost("/api/rule/" + savedRule.getId().getId().toString() + "/activate").andExpect(status().isOk());
        doPost("/api/rule/" + savedRule.getId().getId().toString() + "/suspend").andExpect(status().isOk());
    }

    @Test
    public void testFindSystemRules() throws Exception {
        loginSysAdmin();
        List<RuleMetaData> rules = testRulesCreation("/api/rule/system", sysPlugin);
        for (RuleMetaData rule : rules) {
            doDelete("/api/rule/" + rule.getId().getId()).andExpect(status().isOk());
        }
        loginTenantAdmin();
    }

    @Test
    public void testFindCurrentTenantPlugins() throws Exception {
        List<RuleMetaData> rules = testRulesCreation("/api/rule", tenantPlugin);
        for (RuleMetaData rule : rules) {
            doDelete("/api/rule/" + rule.getId().getId()).andExpect(status().isOk());
        }
    }

    @Test
    public void testFindTenantPlugins() throws Exception {
        List<RuleMetaData> rules = testRulesCreation("/api/rule", tenantPlugin);
        loginSysAdmin();
        List<RuleMetaData> loadedRules = new ArrayList<>();
        TextPageLink pageLink = new TextPageLink(3);
        TextPageData<RuleMetaData> pageData;
        do {
            pageData = doGetTypedWithPageLink("/api/rule/tenant/" + savedTenant.getId().getId().toString() + "?",
                    new TypeReference<TextPageData<RuleMetaData>>() {
                    }, pageLink);
            loadedRules.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageData.getNextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(rules, idComparator);
        Collections.sort(loadedRules, idComparator);

        Assert.assertEquals(rules, loadedRules);

        for (RuleMetaData rule : rules) {
            doDelete("/api/rule/" + rule.getId().getId()).andExpect(status().isOk());
        }
    }

    private List<RuleMetaData> testRulesCreation(String url, PluginMetaData plugin) throws Exception {
        List<RuleMetaData> rules = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            RuleMetaData rule = createRuleMetaData(plugin);
            rule.setPluginToken(plugin.getApiToken());
            rule.setName(rule.getName() + i);
            rules.add(doPost("/api/rule", rule, RuleMetaData.class));
        }

        List<RuleMetaData> loadedRules = new ArrayList<>();
        TextPageLink pageLink = new TextPageLink(3);
        TextPageData<RuleMetaData> pageData;
        do {
            pageData = doGetTypedWithPageLink(url + "?",
                    new TypeReference<TextPageData<RuleMetaData>>() {
                    }, pageLink);
            loadedRules.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageData.getNextPageLink();
            }
        } while (pageData.hasNext());

        loadedRules = loadedRules.stream().filter(p -> !p.getName().equals("System Telemetry Rule")).collect(Collectors.toList());

        Collections.sort(rules, idComparator);
        Collections.sort(loadedRules, idComparator);

        Assert.assertEquals(rules, loadedRules);
        return loadedRules;
    }

    public static RuleMetaData createRuleMetaData(PluginMetaData plugin) throws IOException {
        RuleMetaData rule = new RuleMetaData();
        rule.setName("My Rule");
        rule.setPluginToken(plugin.getApiToken());
        rule.setFilters(mapper.readTree("[{\"clazz\":\"org.thingsboard.server.extensions.core.filter.MsgTypeFilter\", " +
                "\"name\":\"TelemetryFilter\", " +
                "\"configuration\": {\"messageTypes\":[\"POST_TELEMETRY\",\"POST_ATTRIBUTES\",\"GET_ATTRIBUTES\"]}}]"));
        rule.setAction(mapper.readTree("{\"clazz\":\"org.thingsboard.server.extensions.core.action.telemetry.TelemetryPluginAction\", \"name\":\"TelemetryMsgConverterAction\", " +
                "\"configuration\":{\"timeUnit\":\"DAYS\", \"ttlValue\":1}}"));
        return rule;
    }
}
