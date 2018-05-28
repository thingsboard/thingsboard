/**
 * Copyright © 2016-2018 The Thingsboard Authors
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
package org.thingsboard.server.dao.sql.tenant;

import com.datastax.driver.core.utils.UUIDs;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.tenant.TenantDao;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Created by Valerii Sosliuk on 4/30/2017.
 */
public class JpaTenantDaoTest extends AbstractJpaDaoTest {

    @Autowired
    private TenantDao tenantDao;

    @Test
    @DatabaseSetup("classpath:dbunit/empty_dataset.xml")
    public void testFindTenantsByRegion() {
        createTenants();
        assertEquals(60, tenantDao.find().size());
        List<Tenant> tenants1 = tenantDao.findTenantsByRegion("REGION_1", new TextPageLink(20,"title"));
        assertEquals(20, tenants1.size());
        List<Tenant> tenants2 = tenantDao.findTenantsByRegion("REGION_1",
                new TextPageLink(20,"title", tenants1.get(19).getId().getId(), null));
        assertEquals(10, tenants2.size());
        List<Tenant> tenants3 = tenantDao.findTenantsByRegion("REGION_1",
                new TextPageLink(20,"title", tenants2.get(9).getId().getId(), null));
        assertEquals(0, tenants3.size());
    }

    private void createTenants() {
        for (int i = 0; i < 30; i++) {
            createTenant("REGION_1", "TITLE", i);
            createTenant("REGION_2", "TITLE", i);
        }
    }

    private void createTenant(String region, String title, int index) {
        Tenant tenant = new Tenant();
        tenant.setId(new TenantId(UUIDs.timeBased()));
        tenant.setRegion(region);
        tenant.setTitle(title + "_" + index);
        tenantDao.save(tenant);
    }

}
