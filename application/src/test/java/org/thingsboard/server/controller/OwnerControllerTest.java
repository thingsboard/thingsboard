// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import org.junit.Test;
import org.thingsboard.server.dao.service.DaoSqlTest;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class OwnerControllerTest extends AbstractControllerTest {

    @Test
    public void testShouldNotChangeOwnerForTheOnlyTenantAdmin() throws Exception {
        loginTenantAdmin();

        // try to change owner for last tenant admin
        doPost("/api/owner/CUSTOMER/" + customerId.getId() + "/USER/" + tenantAdminUser.getId().getId())
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("At least one tenant administrator must remain!")));
    }
}
