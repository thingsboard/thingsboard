// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.client;

import org.junit.Test;
import org.thingsboard.client.api.ThingsboardApi.AssignDashboardToCustomerArgs;
import org.thingsboard.client.api.ThingsboardApi.AssignDashboardToPublicCustomerArgs;
import org.thingsboard.client.api.ThingsboardApi.DeleteDashboardArgs;
import org.thingsboard.client.api.ThingsboardApi.GetCustomerDashboardsArgs;
import org.thingsboard.client.api.ThingsboardApi.GetDashboardInfoByIdArgs;
import org.thingsboard.client.api.ThingsboardApi.GetTenantDashboardsArgs;
import org.thingsboard.client.api.ThingsboardApi.SaveDashboardArgs;
import org.thingsboard.client.api.ThingsboardApi.UnassignDashboardFromCustomerArgs;
import org.thingsboard.client.api.ThingsboardApi.UnassignDashboardFromPublicCustomerArgs;
import org.thingsboard.client.model.Dashboard;
import org.thingsboard.client.model.DashboardInfo;
import org.thingsboard.client.model.PageDataDashboardInfo;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@DaoSqlTest
public class DashboardApiClientTest extends AbstractApiClientTest {

    @Test
    public void testDashboardLifecycle() throws Exception {
        long timestamp = System.currentTimeMillis();

        // create 20 dashboards
        for (int i = 0; i < 20; i++) {
            Dashboard dashboard = new Dashboard();
            String dashboardTitle = ((i % 2 == 0) ? TEST_PREFIX : TEST_PREFIX_2) + timestamp + "_" + i;
            dashboard.setTitle(dashboardTitle);

            client.saveDashboard(SaveDashboardArgs.builder()
                    .dashboard(dashboard)
                    .build());
        }

        // find all, check count
        PageDataDashboardInfo allDashboards = client.getTenantDashboards(GetTenantDashboardsArgs.builder()
                .pageSize(100)
                .page(0)
                .build());
        assertNotNull(allDashboards);
        assertNotNull(allDashboards.getData());
        int initialSize = allDashboards.getData().size();
        assertEquals("Expected 20 dashboards, but got " + initialSize, 20, initialSize);

        List<DashboardInfo> createdDashboards = allDashboards.getData();

        // find all with search text, check count
        PageDataDashboardInfo filteredDashboards = client.getTenantDashboards(GetTenantDashboardsArgs.builder()
                .pageSize(100)
                .page(0)
                .textSearch(TEST_PREFIX_2)
                .build());
        assertEquals("Expected exactly 10 dashboards matching prefix", 10, filteredDashboards.getData().size());

        // find by id
        DashboardInfo searchDashboard = createdDashboards.get(10);
        DashboardInfo fetchedDashboard = client.getDashboardInfoById(GetDashboardInfoByIdArgs.builder()
                .dashboardId(searchDashboard.getId().getId().toString())
                .build());
        assertEquals(searchDashboard.getTitle(), fetchedDashboard.getTitle());

        // update dashboard
        Dashboard dashboardToUpdate = new Dashboard();
        dashboardToUpdate.setId(fetchedDashboard.getId());
        dashboardToUpdate.setTitle(fetchedDashboard.getTitle() + "_updated");
        dashboardToUpdate.setVersion(fetchedDashboard.getVersion());
        client.saveDashboard(SaveDashboardArgs.builder()
                .dashboard(dashboardToUpdate)
                .build());

        DashboardInfo updatedDashboard = client.getDashboardInfoById(GetDashboardInfoByIdArgs.builder()
                .dashboardId(fetchedDashboard.getId().getId().toString())
                .build());
        assertEquals(fetchedDashboard.getTitle() + "_updated", updatedDashboard.getTitle());

        // assign dashboard to customer and verify
        String customerId = savedClientCustomer.getId().getId().toString();
        String dashboardId = createdDashboards.get(0).getId().getId().toString();
        client.assignDashboardToCustomer(AssignDashboardToCustomerArgs.builder()
                .customerId(customerId)
                .dashboardId(dashboardId)
                .build());

        PageDataDashboardInfo customerDashboards = client.getCustomerDashboards(GetCustomerDashboardsArgs.builder()
                .customerId(customerId)
                .pageSize(100)
                .page(0)
                .build());
        assertEquals(1, customerDashboards.getData().size());
        assertEquals(createdDashboards.get(0).getTitle(), customerDashboards.getData().get(0).getTitle());

        // unassign dashboard from customer
        client.unassignDashboardFromCustomer(UnassignDashboardFromCustomerArgs.builder()
                .customerId(customerId)
                .dashboardId(dashboardId)
                .build());
        PageDataDashboardInfo dashboardsAfterUnassign = client.getCustomerDashboards(GetCustomerDashboardsArgs.builder()
                .customerId(customerId)
                .pageSize(100)
                .page(0)
                .build());
        assertEquals(0, dashboardsAfterUnassign.getData().size());

        // make dashboard public and verify
        client.assignDashboardToPublicCustomer(AssignDashboardToPublicCustomerArgs.builder()
                .dashboardId(dashboardId)
                .build());
        DashboardInfo publicDashboard = client.getDashboardInfoById(GetDashboardInfoByIdArgs.builder()
                .dashboardId(dashboardId)
                .build());
        assertNotNull(publicDashboard.getAssignedCustomers());
        assertTrue(publicDashboard.getAssignedCustomers().size() > 0);

        // remove public access
        client.unassignDashboardFromPublicCustomer(UnassignDashboardFromPublicCustomerArgs.builder()
                .dashboardId(dashboardId)
                .build());

        // delete dashboard
        UUID dashboardToDeleteId = createdDashboards.get(0).getId().getId();
        client.deleteDashboard(DeleteDashboardArgs.builder()
                .dashboardId(dashboardToDeleteId.toString())
                .build());

        // verify deletion
        PageDataDashboardInfo dashboardsAfterDelete = client.getTenantDashboards(GetTenantDashboardsArgs.builder()
                .pageSize(100)
                .page(0)
                .build());
        assertEquals(initialSize - 1, dashboardsAfterDelete.getData().size());

        assertReturns404(() ->
                client.getDashboardInfoById(GetDashboardInfoByIdArgs.builder()
                        .dashboardId(dashboardToDeleteId.toString())
                        .build())
        );
    }

    @Test
    public void testGetServerTime() throws Exception {
        Long serverTime = client.getServerTime();
        assertNotNull(serverTime);
    }

    @Test
    public void testGetMaxDatapointsLimit() throws Exception {
        Long maxDatapointsLimit = client.getMaxDatapointsLimit();
        assertNotNull(maxDatapointsLimit);
    }

}
