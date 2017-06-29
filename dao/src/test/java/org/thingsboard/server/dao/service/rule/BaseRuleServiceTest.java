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
package org.thingsboard.server.dao.service.rule;

import com.datastax.driver.core.utils.UUIDs;
import org.junit.Assert;
import org.junit.Test;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.plugin.PluginMetaData;
import org.thingsboard.server.common.data.rule.RuleMetaData;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.service.AbstractServiceTest;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public abstract class BaseRuleServiceTest extends AbstractServiceTest {

    @Test
    public void saveRule() throws Exception {
        PluginMetaData plugin = generatePlugin(null, "testPluginToken" + ThreadLocalRandom.current().nextInt());
        pluginService.savePlugin(plugin);
        RuleMetaData ruleMetaData = ruleService.saveRule(generateRule(plugin.getTenantId(), null, plugin.getApiToken()));
        Assert.assertNotNull(ruleMetaData.getId());
        Assert.assertNotNull(ruleMetaData.getAdditionalInfo());
        ruleMetaData.setAdditionalInfo(mapper.readTree("{\"description\":\"test\"}"));
        RuleMetaData newRuleMetaData = ruleService.saveRule(ruleMetaData);
        Assert.assertEquals(ruleMetaData.getAdditionalInfo(), newRuleMetaData.getAdditionalInfo());
    }

    @Test
    public void findRuleById() throws Exception {
        PluginMetaData plugin = generatePlugin(null, "testPluginToken" + ThreadLocalRandom.current().nextInt());
        pluginService.savePlugin(plugin);

        RuleMetaData expected = ruleService.saveRule(generateRule(plugin.getTenantId(), null, plugin.getApiToken()));
        Assert.assertNotNull(expected.getId());
        RuleMetaData found = ruleService.findRuleById(expected.getId());
        Assert.assertEquals(expected, found);
    }

    @Test
    public void findPluginRules() throws Exception {
        TenantId tenantIdA = new TenantId(UUIDs.timeBased());
        TenantId tenantIdB = new TenantId(UUIDs.timeBased());

        PluginMetaData pluginA = generatePlugin(tenantIdA, "testPluginToken" + ThreadLocalRandom.current().nextInt());
        PluginMetaData pluginB = generatePlugin(tenantIdB, "testPluginToken" + ThreadLocalRandom.current().nextInt());
        pluginService.savePlugin(pluginA);
        pluginService.savePlugin(pluginB);

        ruleService.saveRule(generateRule(tenantIdA, null, pluginA.getApiToken()));
        ruleService.saveRule(generateRule(tenantIdA, null, pluginA.getApiToken()));
        ruleService.saveRule(generateRule(tenantIdA, null, pluginA.getApiToken()));

        ruleService.saveRule(generateRule(tenantIdB, null, pluginB.getApiToken()));
        ruleService.saveRule(generateRule(tenantIdB, null, pluginB.getApiToken()));

        List<RuleMetaData> foundA = ruleService.findPluginRules(pluginA.getApiToken());
        Assert.assertEquals(3, foundA.size());

        List<RuleMetaData> foundB = ruleService.findPluginRules(pluginB.getApiToken());
        Assert.assertEquals(2, foundB.size());
    }

    @Test
    public void findSystemRules() throws Exception {
        TenantId systemTenant = new TenantId(ModelConstants.NULL_UUID); // system tenant id

        PluginMetaData plugin = generatePlugin(systemTenant, "testPluginToken" + ThreadLocalRandom.current().nextInt());
        pluginService.savePlugin(plugin);
        ruleService.saveRule(generateRule(systemTenant, null, plugin.getApiToken()));
        ruleService.saveRule(generateRule(systemTenant, null, plugin.getApiToken()));
        ruleService.saveRule(generateRule(systemTenant, null, plugin.getApiToken()));
        TextPageData<RuleMetaData> found = ruleService.findSystemRules(new TextPageLink(100));
        Assert.assertEquals(3, found.getData().size());
    }

    @Test
    public void findTenantRules() throws Exception {
        TenantId tenantIdA = new TenantId(UUIDs.timeBased());
        TenantId tenantIdB = new TenantId(UUIDs.timeBased());

        PluginMetaData pluginA = generatePlugin(tenantIdA, "testPluginToken" + ThreadLocalRandom.current().nextInt());
        PluginMetaData pluginB = generatePlugin(tenantIdB, "testPluginToken" + ThreadLocalRandom.current().nextInt());
        pluginService.savePlugin(pluginA);
        pluginService.savePlugin(pluginB);

        ruleService.saveRule(generateRule(tenantIdA, null, pluginA.getApiToken()));
        ruleService.saveRule(generateRule(tenantIdA, null, pluginA.getApiToken()));
        ruleService.saveRule(generateRule(tenantIdA, null, pluginA.getApiToken()));

        ruleService.saveRule(generateRule(tenantIdB, null, pluginB.getApiToken()));
        ruleService.saveRule(generateRule(tenantIdB, null, pluginB.getApiToken()));

        TextPageData<RuleMetaData> foundA = ruleService.findTenantRules(tenantIdA, new TextPageLink(100));
        Assert.assertEquals(3, foundA.getData().size());

        TextPageData<RuleMetaData> foundB = ruleService.findTenantRules(tenantIdB, new TextPageLink(100));
        Assert.assertEquals(2, foundB.getData().size());
    }

    @Test
    public void deleteRuleById() throws Exception {
        PluginMetaData plugin = generatePlugin(null, "testPluginToken" + ThreadLocalRandom.current().nextInt());
        pluginService.savePlugin(plugin);

        RuleMetaData expected = ruleService.saveRule(generateRule(plugin.getTenantId(), null, plugin.getApiToken()));
        Assert.assertNotNull(expected.getId());
        RuleMetaData found = ruleService.findRuleById(expected.getId());
        Assert.assertEquals(expected, found);
        ruleService.deleteRuleById(expected.getId());
        found = ruleService.findRuleById(expected.getId());
        Assert.assertNull(found);
    }

    @Test
    public void deleteRulesByTenantId() throws Exception {
        TenantId tenantIdA = new TenantId(UUIDs.timeBased());
        TenantId tenantIdB = new TenantId(UUIDs.timeBased());

        PluginMetaData pluginA = generatePlugin(tenantIdA, "testPluginToken" + ThreadLocalRandom.current().nextInt());
        PluginMetaData pluginB = generatePlugin(tenantIdB, "testPluginToken" + ThreadLocalRandom.current().nextInt());
        pluginService.savePlugin(pluginA);
        pluginService.savePlugin(pluginB);

        ruleService.saveRule(generateRule(tenantIdA, null, pluginA.getApiToken()));
        ruleService.saveRule(generateRule(tenantIdA, null, pluginA.getApiToken()));
        ruleService.saveRule(generateRule(tenantIdA, null, pluginA.getApiToken()));

        ruleService.saveRule(generateRule(tenantIdB, null, pluginB.getApiToken()));
        ruleService.saveRule(generateRule(tenantIdB, null, pluginB.getApiToken()));

        TextPageData<RuleMetaData> foundA = ruleService.findTenantRules(tenantIdA, new TextPageLink(100));
        Assert.assertEquals(3, foundA.getData().size());

        TextPageData<RuleMetaData> foundB = ruleService.findTenantRules(tenantIdB, new TextPageLink(100));
        Assert.assertEquals(2, foundB.getData().size());

        ruleService.deleteRulesByTenantId(tenantIdA);

        foundA = ruleService.findTenantRules(tenantIdA, new TextPageLink(100));
        Assert.assertEquals(0, foundA.getData().size());

        foundB = ruleService.findTenantRules(tenantIdB, new TextPageLink(100));
        Assert.assertEquals(2, foundB.getData().size());
    }
}