// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.client;

import org.junit.Test;
import org.thingsboard.client.ApiException;
import org.thingsboard.client.api.ThingsboardApi.DeleteTenantArgs;
import org.thingsboard.client.api.ThingsboardApi.GetTenantAdminsArgs;
import org.thingsboard.client.api.ThingsboardApi.GetTenantByIdArgs;
import org.thingsboard.client.api.ThingsboardApi.GetTenantsArgs;
import org.thingsboard.client.api.ThingsboardApi.SaveTenantArgs;
import org.thingsboard.client.api.ThingsboardApi.SaveUserArgs;
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

            Tenant createdTenant = client.saveTenant(SaveTenantArgs.builder()
                    .tenant(tenant)
                    .build());
            assertNotNull(createdTenant);
            assertNotNull(createdTenant.getId());
            assertEquals(tenantTitle, createdTenant.getTitle());

            createdTenants.add(createdTenant);
        }

        try {
            // find all with search text, check count
            PageDataTenant filteredTenants = client.getTenants(GetTenantsArgs.builder()
                    .pageSize(100)
                    .page(0)
                    .textSearch(TEST_PREFIX_2)
                    .build());
            assertEquals("Expected exactly 10 tenants matching prefix", 10, filteredTenants.getData().size());

            // find by id
            Tenant searchTenant = createdTenants.get(10);
            Tenant fetchedTenant = client.getTenantById(GetTenantByIdArgs.builder()
                    .tenantId(searchTenant.getId().getId().toString())
                    .build());
            assertEquals(searchTenant.getTitle(), fetchedTenant.getTitle());
            assertEquals(searchTenant.getEmail(), fetchedTenant.getEmail());

            // update tenant
            fetchedTenant.setCity("Updated City");
            fetchedTenant.setCountry("DE");
            Tenant updatedTenant = client.saveTenant(SaveTenantArgs.builder()
                    .tenant(fetchedTenant)
                    .build());
            assertEquals("Updated City", updatedTenant.getCity());
            assertEquals("DE", updatedTenant.getCountry());

            // create a tenant admin for one of the tenants and verify listing
            Tenant tenantForAdmin = createdTenants.get(0);
            User adminUser = new User();
            adminUser.setEmail("tenanttest_admin_" + timestamp + "@test.com");
            adminUser.setAuthority(Authority.TENANT_ADMIN);
            adminUser.setTenantId(tenantForAdmin.getId());
            adminUser.setFirstName("TestAdmin");
            User savedAdmin = client.saveUser(SaveUserArgs.builder()
                    .user(adminUser)
                    .sendActivationMail("false")
                    .build());
            assertNotNull(savedAdmin);

            PageDataUser tenantAdmins = client.getTenantAdmins(GetTenantAdminsArgs.builder()
                    .tenantId(tenantForAdmin.getId().getId().toString())
                    .pageSize(100)
                    .page(0)
                    .build());
            assertEquals(1, tenantAdmins.getData().size());
            assertEquals(savedAdmin.getEmail(), tenantAdmins.getData().get(0).getEmail());

            // delete tenant
            UUID tenantToDeleteId = createdTenants.get(0).getId().getId();
            client.deleteTenant(DeleteTenantArgs.builder()
                    .tenantId(tenantToDeleteId.toString())
                    .build());
            createdTenants.remove(0);

            // verify deletion
            PageDataTenant tenantsAfterDelete = client.getTenants(GetTenantsArgs.builder()
                    .pageSize(100)
                    .page(0)
                    .textSearch(TEST_PREFIX_2)
                    .build());
            assertEquals(10, tenantsAfterDelete.getData().size());

            assertReturns404(() ->
                    client.getTenantById(GetTenantByIdArgs.builder()
                            .tenantId(tenantToDeleteId.toString())
                            .build())
            );
        } finally {
            // clean up all created tenants (deleting tenant cascades to users)
            client.login("sysadmin@thingsboard.org", "sysadmin");
            for (Tenant tenant : createdTenants) {
                try {
                    client.deleteTenant(DeleteTenantArgs.builder()
                            .tenantId(tenant.getId().getId().toString())
                            .build());
                } catch (ApiException ignored) {
                }
            }
        }
    }

}
