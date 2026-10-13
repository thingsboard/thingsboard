// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.client;

import org.junit.Test;
import org.thingsboard.client.api.ThingsboardApi.ExportGroupDashboardsArgs;
import org.thingsboard.client.api.ThingsboardApi.GetAllDashboardsArgs;
import org.thingsboard.client.api.ThingsboardApi.GetCustomerDashboardsArgs;
import org.thingsboard.client.api.ThingsboardApi.GetDashboardInfoByIdArgs;
import org.thingsboard.client.api.ThingsboardApi.GetDashboardsByEntityGroupIdArgs;
import org.thingsboard.client.api.ThingsboardApi.GetDashboardsByIdsArgs;
import org.thingsboard.client.api.ThingsboardApi.GetTenantDashboardsArgs;
import org.thingsboard.client.api.ThingsboardApi.GetTenantDashboardsByTenantIdArgs;
import org.thingsboard.client.api.ThingsboardApi.GetUserDashboardsArgs;
import org.thingsboard.client.api.ThingsboardApi.ImportGroupDashboardsArgs;
import org.thingsboard.client.api.ThingsboardApi.SaveDashboardArgs;
import org.thingsboard.client.api.ThingsboardApi.SaveEntityGroupArgs;
import org.thingsboard.client.api.ThingsboardApi.SetCustomerHomeDashboardInfoArgs;
import org.thingsboard.client.api.ThingsboardApi.SetTenantHomeDashboardInfoArgs;
import org.thingsboard.client.model.Dashboard;
import org.thingsboard.client.model.DashboardId;
import org.thingsboard.client.model.DashboardInfo;
import org.thingsboard.client.model.EntityGroup;
import org.thingsboard.client.model.EntityGroupInfo;
import org.thingsboard.client.model.HomeDashboardInfo;
import org.thingsboard.client.model.PageDataDashboardInfo;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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

    @Test
    public void testGetTenantDashboards() throws Exception {
        Dashboard dashboard = new Dashboard();
        dashboard.setTitle(TEST_PREFIX + System.currentTimeMillis());
        client.saveDashboard(SaveDashboardArgs.builder()
                .dashboard(dashboard)
                .build());

        // tenant admin variant
        PageDataDashboardInfo tenantAdminResult = client.getTenantDashboards(GetTenantDashboardsArgs.builder()
                .pageSize(100)
                .page(0)
                .build());
        assertNotNull(tenantAdminResult);
        assertEquals(1, tenantAdminResult.getData().size());

        // get user dashboards
        PageDataDashboardInfo userDashboards = client.getUserDashboards(GetUserDashboardsArgs.builder()
                .pageSize(100)
                .page(0)
                .build());
        assertEquals(1, userDashboards.getData().size());

        // get all dashboards
        PageDataDashboardInfo allDashboards = client.getAllDashboards(GetAllDashboardsArgs.builder()
                .pageSize(100)
                .page(0)
                .includeCustomers(true)
                .build());
        assertEquals(1, allDashboards.getData().size());

        // system administrator variant (requires tenantId)
        client.login("sysadmin@thingsboard.org", "sysadmin");
        PageDataDashboardInfo sysAdminResult = client.getTenantDashboardsByTenantId(GetTenantDashboardsByTenantIdArgs.builder()
                .tenantId(savedClientTenant.getId().getId().toString())
                .pageSize(100)
                .page(0)
                .build());
        assertNotNull(sysAdminResult);
        assertEquals("Expected at least one dashboard from sysadmin query", 1, sysAdminResult.getData().size());
    }

    @Test
    public void testGetCustomerDashboards() throws Exception {
        String customerId = savedClientCustomer.getId().getId().toString();

        // no dashboards are shared with the customer in test setup; call must succeed with empty result
        PageDataDashboardInfo result = client.getCustomerDashboards(GetCustomerDashboardsArgs.builder()
                .customerId(customerId)
                .pageSize(100)
                .page(0)
                .includeCustomers(true)
                .build());
        assertNotNull(result);
        assertNotNull(result.getData());
        assertTrue("Expected no dashboards assigned to the test customer", result.getData().isEmpty());
    }

    @Test
    public void testGetDashboardsByIds() throws Exception {
        Dashboard dashboard = new Dashboard();
        dashboard.setTitle(TEST_PREFIX + System.currentTimeMillis());
        client.saveDashboard(SaveDashboardArgs.builder()
                .dashboard(dashboard)
                .build());

        PageDataDashboardInfo all = client.getTenantDashboards(GetTenantDashboardsArgs.builder()
                .pageSize(100)
                .page(0)
                .build());
        assertFalse(all.getData().isEmpty());
        DashboardInfo first = all.getData().get(0);

        List<DashboardInfo> result = client.getDashboardsByIds(GetDashboardsByIdsArgs.builder()
                .dashboardIds(List.of(first.getId().getId().toString()))
                .build());
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(first.getId().getId(), result.get(0).getId().getId());
    }

    @Test
    public void testEntityGroupDashboards() throws Exception {
        // create a DASHBOARD entity group
        EntityGroup entityGroup = new EntityGroup();
        entityGroup.setType(EntityGroup.TypeEnum.DASHBOARD);
        entityGroup.setName("Test Dashboard Group");
        EntityGroupInfo savedGroup = client.saveEntityGroup(SaveEntityGroupArgs.builder()
                .entityGroup(entityGroup)
                .build());
        assertNotNull(savedGroup.getId());
        String groupId = savedGroup.getId().getId().toString();

        // importGroupDashboards: import a new dashboard into the group
        Dashboard dashboard = new Dashboard();
        dashboard.setTitle(TEST_PREFIX + System.currentTimeMillis());
        dashboard.setConfiguration(OBJECT_MAPPER.createObjectNode());
        client.importGroupDashboards(ImportGroupDashboardsArgs.builder()
                .entityGroupId(groupId)
                .dashboard(List.of(dashboard))
                .overwrite(false)
                .build());

        // getDashboardsByEntityGroupId: verify the imported dashboard is present
        PageDataDashboardInfo groupDashboards = client.getDashboardsByEntityGroupId(GetDashboardsByEntityGroupIdArgs.builder()
                .entityGroupId(groupId)
                .pageSize(100)
                .page(0)
                .build());
        assertNotNull(groupDashboards);
        assertEquals("Expected exactly one dashboard in the entity group", 1, groupDashboards.getData().size());

        List<Dashboard> dashboards = client.exportGroupDashboards(ExportGroupDashboardsArgs.builder()
                .entityGroupId(groupId)
                .limit(10)
                .build());
        assertEquals(1, dashboards.size());
        assertEquals(dashboard.getTitle(), dashboards.get(0).getTitle());
    }

    @Test
    public void testTenantHomeDashboard() throws Exception {
        // create a dashboard and resolve its ID via getTenantDashboards (saveDashboard returns void)
        Dashboard dashboard = new Dashboard();
        dashboard.setTitle(TEST_PREFIX + System.currentTimeMillis());
        client.saveDashboard(SaveDashboardArgs.builder()
                .dashboard(dashboard)
                .build());

        PageDataDashboardInfo all = client.getTenantDashboards(GetTenantDashboardsArgs.builder()
                .pageSize(100)
                .page(0)
                .build());
        DashboardId dashboardId = all.getData().get(0).getId();

        // getTenantHomeDashboardInfo: no home dashboard set for a freshly created tenant
        HomeDashboardInfo initialInfo = client.getTenantHomeDashboardInfo();
        assertNotNull(initialInfo);
        assertNull("No home dashboard should be set for a new tenant", initialInfo.getDashboardId());

        // setTenantHomeDashboardInfo
        HomeDashboardInfo homeDashboardInfo = new HomeDashboardInfo();
        homeDashboardInfo.setDashboardId(dashboardId);
        homeDashboardInfo.setHideDashboardToolbar(false);
        client.setTenantHomeDashboardInfo(SetTenantHomeDashboardInfoArgs.builder()
                .homeDashboardInfo(homeDashboardInfo)
                .build());

        // getTenantHomeDashboardInfo must now reflect the change
        HomeDashboardInfo tenantInfo = client.getTenantHomeDashboardInfo();
        assertNotNull(tenantInfo.getDashboardId());
        assertEquals(dashboardId.getId(), tenantInfo.getDashboardId().getId());

        // getHomeDashboardInfo: inherits the tenant-level setting for the tenant admin user
        HomeDashboardInfo currentUserInfo = client.getHomeDashboardInfo();
        assertNotNull(currentUserInfo.getDashboardId());
        assertEquals(dashboardId.getId(), currentUserInfo.getDashboardId().getId());
    }

    @Test
    public void testCustomerHomeDashboard() throws Exception {
        // switch to customer user authority
        client.login(CUSTOMER_USERNAME, TEST_PASSWORD);

        Dashboard dashboard = new Dashboard();
        dashboard.setTitle(TEST_PREFIX + System.currentTimeMillis());
        dashboard = client.saveDashboard(SaveDashboardArgs.builder()
                .dashboard(dashboard)
                .build());

        // getCustomerHomeDashboardInfo: no home dashboard configured for a new customer
        HomeDashboardInfo initialInfo = client.getCustomerHomeDashboardInfo();
        assertNotNull(initialInfo);
        assertNull("No home dashboard should be set for a new customer", initialInfo.getDashboardId());

        HomeDashboardInfo homeDashboardInfo = new HomeDashboardInfo();
        homeDashboardInfo.setDashboardId(dashboard.getId());
        homeDashboardInfo.setHideDashboardToolbar(false);
        client.setCustomerHomeDashboardInfo(SetCustomerHomeDashboardInfoArgs.builder()
                .homeDashboardInfo(homeDashboardInfo)
                .build());

        HomeDashboardInfo updatedInfo = client.getCustomerHomeDashboardInfo();
        assertNotNull(updatedInfo);
        assertEquals(dashboard.getId(), updatedInfo.getDashboardId());
    }

}
