// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.service.security.permission.OwnersCacheService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class OwnerControllerTest extends AbstractControllerTest {

    @Autowired
    private OwnersCacheService ownersCacheService;

    @Test
    public void testShouldNotChangeOwnerForTheOnlyTenantAdmin() throws Exception {
        loginTenantAdmin();

        // try to change owner for last tenant admin
        doPost("/api/owner/CUSTOMER/" + customerId.getId() + "/USER/" + tenantAdminUser.getId().getId())
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("At least one tenant administrator must remain!")));
    }

    @Test
    public void testChangeCustomerOwnerEvictsCachedOwnersOfItsEntityGroups() throws Exception {
        loginTenantAdmin();

        Customer customerA = new Customer();
        customerA.setTitle("Customer A");
        customerA = doPost("/api/customer", customerA, Customer.class);

        Customer customerB = new Customer();
        customerB.setTitle("Customer B");
        customerB = doPost("/api/customer", customerB, Customer.class);

        EntityGroup assetGroup = new EntityGroup();
        assetGroup.setName("Sites");
        assetGroup.setType(EntityType.ASSET);
        assetGroup.setOwnerId(customerB.getId());
        assetGroup = doPost("/api/entityGroup", assetGroup, EntityGroup.class);

        // a permission check performed before the owner is changed caches the owner chain of the group
        assertThat(ownersCacheService.getOwners(tenantId, assetGroup.getId()))
                .contains(customerB.getId())
                .doesNotContain(customerA.getId());

        doPost("/api/owner/CUSTOMER/" + customerA.getId().getId() + "/CUSTOMER/" + customerB.getId().getId())
                .andExpect(status().isOk());

        assertThat(ownersCacheService.getOwners(tenantId, assetGroup.getId()))
                .contains(customerA.getId());
    }
}
