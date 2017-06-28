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
package org.thingsboard.server.dao.sql.plugin;

import com.datastax.driver.core.utils.UUIDs;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.id.PluginId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.plugin.PluginMetaData;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.plugin.PluginDao;

import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

/**
 * Created by Valerii Sosliuk on 5/1/2017.
 */
public class JpaBasePluginDaoTest extends AbstractJpaDaoTest {

    @Autowired
    private PluginDao pluginDao;

    @Test
    @DatabaseSetup("classpath:dbunit/empty_dataset.xml")
    public void testFindByTenantIdAndPageLink() {
        UUID tenantId1 = UUIDs.timeBased();
        UUID tenantId2 = UUIDs.timeBased();
        createPluginsTwoTenants(tenantId1, tenantId2, "plugin_");
        List<PluginMetaData> rules1 = pluginDao.findByTenantIdAndPageLink(
                new TenantId(tenantId1), new TextPageLink(20, "plugin_"));
        assertEquals(20, rules1.size());

        List<PluginMetaData> rules2 = pluginDao.findByTenantIdAndPageLink(new TenantId(tenantId1),
                new TextPageLink(20, "plugin_", rules1.get(19).getId().getId(), null));
        assertEquals(10, rules2.size());

        List<PluginMetaData> rules3 = pluginDao.findByTenantIdAndPageLink(new TenantId(tenantId1),
                new TextPageLink(20, "plugin_", rules2.get(9).getId().getId(), null));
        assertEquals(0, rules3.size());
    }

    @Test
    @DatabaseSetup(value = "classpath:dbunit/empty_dataset.xml")
    public void testFindAllTenantRulesByTenantId() {
        UUID tenantId1 = UUIDs.timeBased();
        UUID tenantId2 = UUIDs.timeBased();
        createTenantsAndSystemPlugins(tenantId1, tenantId2, "name_");
        List<PluginMetaData> rules1 = pluginDao.findAllTenantPluginsByTenantId(
                tenantId1, new TextPageLink(40, "name_"));
        assertEquals(40, rules1.size());

        List<PluginMetaData> rules2 = pluginDao.findAllTenantPluginsByTenantId(tenantId1,
                new TextPageLink(40, "name_", rules1.get(19).getId().getId(), null));
        assertEquals(20, rules2.size());

        List<PluginMetaData> rules3 = pluginDao.findAllTenantPluginsByTenantId(tenantId1,
                new TextPageLink(40, "name_", rules2.get(19).getId().getId(), null));
        assertEquals(0, rules3.size());
    }

    private void createTenantsAndSystemPlugins(UUID tenantId1, UUID tenantId2, String namePrefix) {
        for (int i = 0; i < 40; i++) {
            createPlugin(tenantId1, namePrefix, i);
            createPlugin(tenantId2, namePrefix, i);
            createPlugin(null, namePrefix, i);
        }
    }

    private void createPluginsTwoTenants(UUID tenantId1, UUID tenantId2, String namePrefix) {
        for (int i = 0; i < 30; i++) {
            createPlugin(tenantId1, namePrefix, i);
            createPlugin(tenantId2, namePrefix, i);
        }
    }

    private void createPlugin(UUID tenantId, String namePrefix, int i) {
        PluginMetaData plugin = new PluginMetaData();
        plugin.setId(new PluginId(UUIDs.timeBased()));
        plugin.setTenantId(new TenantId(tenantId));
        plugin.setName(namePrefix + i);
        pluginDao.save(plugin);
    }
}
