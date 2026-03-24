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
package org.thingsboard.server.client;

import org.junit.Test;
import org.thingsboard.client.ApiException;
import org.thingsboard.client.model.Authority;
import org.thingsboard.client.model.PageDataTenant;
import org.thingsboard.client.model.PageDataUser;
import org.thingsboard.client.model.Tenant;
import org.thingsboard.client.model.User;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@DaoSqlTest
public class TenantApiClientTest extends AbstractApiClientTest {

    @Test
    public void testTenantLifecycle() throws Exception {
        long timestamp = System.currentTimeMillis();
        List<Tenant> createdTenants = new ArrayList<>();

        // authenticate as sysadmin for tenant management
        client.login("sysadmin@thingsboard.org", "sysadmin");

        // create 20 tenants
        for (int i = 0; i < 20; i++) {
            Tenant tenant = new Tenant();
            String tenantTitle = ((i % 2 == 0) ? TEST_PREFIX : TEST_PREFIX_2) + timestamp + "_" + i;
            tenant.setTitle(tenantTitle);
            tenant.setEmail("tenant_" + timestamp + "_" + i + "@test.com");
            tenant.setCountry("US");
            tenant.setCity("City" + i);

            Tenant createdTenant = client.saveTenant(tenant);
            assertNotNull(createdTenant);
            assertNotNull(createdTenant.getId());
            assertEquals(tenantTitle, createdTenant.getTitle());

            createdTenants.add(createdTenant);
        }

        try {
            // find all with search text, check count
            PageDataTenant filteredTenants = client.getTenants(100, 0, TEST_PREFIX_2, null, null);
            assertEquals("Expected exactly 10 tenants matching prefix", 10, filteredTenants.getData().size());

            // find by id
            Tenant searchTenant = createdTenants.get(10);
            Tenant fetchedTenant = client.getTenantById(searchTenant.getId().getId().toString());
            assertEquals(searchTenant.getTitle(), fetchedTenant.getTitle());
            assertEquals(searchTenant.getEmail(), fetchedTenant.getEmail());

            // update tenant
            fetchedTenant.setCity("Updated City");
            fetchedTenant.setCountry("DE");
            Tenant updatedTenant = client.saveTenant(fetchedTenant);
            assertEquals("Updated City", updatedTenant.getCity());
            assertEquals("DE", updatedTenant.getCountry());

            // create a tenant admin for one of the tenants and verify listing
            Tenant tenantForAdmin = createdTenants.get(0);
            User adminUser = new User();
            adminUser.setEmail("tenanttest_admin_" + timestamp + "@test.com");
            adminUser.setAuthority(Authority.TENANT_ADMIN);
            adminUser.setTenantId(tenantForAdmin.getId());
            adminUser.setFirstName("TestAdmin");
            User savedAdmin = client.saveUser(adminUser, "false");
            assertNotNull(savedAdmin);

            PageDataUser tenantAdmins = client.getTenantAdmins(
                    tenantForAdmin.getId().getId().toString(), 100, 0, null, null, null);
            assertEquals(1, tenantAdmins.getData().size());
            assertEquals(savedAdmin.getEmail(), tenantAdmins.getData().get(0).getEmail());

            // delete tenant
            UUID tenantToDeleteId = createdTenants.get(0).getId().getId();
            client.deleteTenant(tenantToDeleteId.toString());
            createdTenants.remove(0);

            // verify deletion
            PageDataTenant tenantsAfterDelete = client.getTenants(100, 0, TEST_PREFIX_2, null, null);
            assertEquals(10, tenantsAfterDelete.getData().size());

            assertReturns404(() ->
                    client.getTenantById(tenantToDeleteId.toString())
            );
        } finally {
            // clean up all created tenants (deleting tenant cascades to users)
            client.login("sysadmin@thingsboard.org", "sysadmin");
            for (Tenant tenant : createdTenants) {
                try {
                    client.deleteTenant(tenant.getId().getId().toString());
                } catch (ApiException ignored) {
                }
            }
        }
    }

}
