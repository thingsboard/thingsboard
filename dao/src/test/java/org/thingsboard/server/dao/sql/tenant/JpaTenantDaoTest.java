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
package org.thingsboard.server.dao.sql.tenant;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.service.AbstractServiceTest;
import org.thingsboard.server.dao.service.TenantProfileServiceTest;
import org.thingsboard.server.dao.tenant.TenantDao;
import org.thingsboard.server.dao.tenant.TenantProfileDao;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

/**
 * Created by Valerii Sosliuk on 4/30/2017.
 */
public class JpaTenantDaoTest extends AbstractJpaDaoTest {

    @Autowired
    private TenantDao tenantDao;

    @Autowired
    private TenantProfileDao tenantProfileDao;

    List<Tenant> createdTenants = new ArrayList<>();
    TenantProfile tenantProfile;

    @Before
    public void setUp() throws Exception {
        tenantProfile = tenantProfileDao.save(TenantId.SYS_TENANT_ID, TenantProfileServiceTest.createTenantProfile("default tenant profile"));
        assertThat(tenantProfile).as("tenant profile").isNotNull();
    }

    @After
    public void tearDown() throws Exception {
        createdTenants.forEach((tenant)-> tenantDao.removeById(TenantId.SYS_TENANT_ID, tenant.getUuidId()));
        tenantProfileDao.removeById(TenantId.SYS_TENANT_ID, tenantProfile.getUuidId());
    }

    @Test
    //@DatabaseSetup("classpath:dbunit/empty_dataset.xml")
    public void testFindTenants() {
        createTenants();
        assertEquals(30, tenantDao.find(AbstractServiceTest.SYSTEM_TENANT_ID).size());

        PageLink pageLink = new PageLink(20, 0, "title");
        PageData<Tenant> tenants1 = tenantDao.findTenants(AbstractServiceTest.SYSTEM_TENANT_ID, pageLink);
        assertEquals(20, tenants1.getData().size());

        pageLink = pageLink.nextPageLink();
        PageData<Tenant> tenants2 = tenantDao.findTenants(AbstractServiceTest.SYSTEM_TENANT_ID,
                pageLink);
        assertEquals(10, tenants2.getData().size());

        pageLink = pageLink.nextPageLink();
        PageData<Tenant> tenants3 = tenantDao.findTenants(AbstractServiceTest.SYSTEM_TENANT_ID,
                pageLink);
        assertEquals(0, tenants3.getData().size());
    }

    private void createTenants() {
        for (int i = 0; i < 30; i++) {
            createTenant("TITLE", i);
        }
    }

    void createTenant(String title, int index) {
        Tenant tenant = new Tenant();
        tenant.setId(TenantId.fromUUID(Uuids.timeBased()));
        tenant.setTitle(title + "_" + index);
        tenant.setTenantProfileId(tenantProfile.getId());
        createdTenants.add(tenantDao.save(TenantId.SYS_TENANT_ID, tenant));
    }

    @Test
    //@DatabaseSetup("classpath:dbunit/empty_dataset.xml")
    public void testIsExistsTenantById() {
        final UUID uuid = Uuids.timeBased();
        final TenantId tenantId = new TenantId(uuid);
        assertThat(tenantDao.existsById(tenantId, uuid)).as("Is tenant exists before save").isFalse();

        final Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setTitle("Tenant " + uuid);
        tenant.setTenantProfileId(tenantProfile.getId());

        createdTenants.add(tenantDao.save(TenantId.SYS_TENANT_ID, tenant));

        assertThat(tenantDao.existsById(tenantId, uuid)).as("Is tenant exists after save").isTrue();

    }

}
