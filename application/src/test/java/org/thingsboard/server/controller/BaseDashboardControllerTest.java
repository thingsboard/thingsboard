/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.controller;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.dashboard.DashboardDao;
import org.thingsboard.server.dao.exception.DataValidationException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public abstract class BaseDashboardControllerTest extends AbstractControllerTest {

    private IdComparator<DashboardInfo> idComparator = new IdComparator<>();

    private Tenant savedTenant;
    private User tenantAdmin;

    @SpyBean
    private DashboardDao dashboardDao;

    @Before
    public void beforeTest() throws Exception {
        loginSysAdmin();

        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        savedTenant = doPost("/api/tenant", tenant, Tenant.class);
        Assert.assertNotNull(savedTenant);

        tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(savedTenant.getId());
        tenantAdmin.setEmail("tenant2@thingsboard.org");
        tenantAdmin.setFirstName("Joe");
        tenantAdmin.setLastName("Downs");

        tenantAdmin = createUserAndLogin(tenantAdmin, "testPassword1");
    }

    @After
    public void afterTest() throws Exception {
        loginSysAdmin();

        doDelete("/api/tenant/" + savedTenant.getId().getId().toString())
                .andExpect(status().isOk());
    }

    @Test
    public void testSaveDashboardInfoWithViolationOfValidation() throws Exception {
        Dashboard dashboard = new Dashboard();
        dashboard.setTitle(StringUtils.randomAlphabetic(300));
        String msgError = msgErrorFieldLength("title");

        Mockito.reset(tbClusterService, auditLogService);

        doPost("/api/dashboard", dashboard)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        dashboard.setTenantId(savedTenant.getId());
        testNotifyEntityEqualsOneTimeServiceNeverError(dashboard, savedTenant.getId(),
                tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.ADDED, new DataValidationException(msgError));
        Mockito.reset(tbClusterService, auditLogService);
    }

    @Test
    public void testUpdateDashboardFromDifferentTenant() throws Exception {
        Dashboard dashboard = new Dashboard();
        dashboard.setTitle("My dashboard");
        Dashboard savedDashboard = doPost("/api/dashboard", dashboard, Dashboard.class);

        loginDifferentTenant();

        Mockito.reset(tbClusterService, auditLogService);

        doPost("/api/dashboard", savedDashboard, Dashboard.class, status().isForbidden());

        testNotifyEntityNever(savedDashboard.getId(), savedDashboard);

        deleteDifferentTenant();
    }

    @Test
    public void testFindDashboardById() throws Exception {
        Dashboard dashboard = new Dashboard();
        dashboard.setTitle("My dashboard");
        Dashboard savedDashboard = doPost("/api/dashboard", dashboard, Dashboard.class);
        Dashboard foundDashboard = doGet("/api/dashboard/" + savedDashboard.getId().getId().toString(), Dashboard.class);
        Assert.assertNotNull(foundDashboard);
        Assert.assertEquals(savedDashboard, foundDashboard);
    }

    @Test
    public void testDeleteDashboard() throws Exception {
        Dashboard dashboard = new Dashboard();
        dashboard.setTitle("My dashboard");
        Dashboard savedDashboard = doPost("/api/dashboard", dashboard, Dashboard.class);

        Mockito.reset(tbClusterService, auditLogService);

        doDelete("/api/dashboard/" + savedDashboard.getId().getId().toString()).andExpect(status().isOk());

        testNotifyEntityOneTimeMsgToEdgeServiceNever(savedDashboard, savedDashboard.getId(), savedDashboard.getId(),
                savedDashboard.getTenantId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.DELETED,
                savedDashboard.getId().getId().toString());

        String dashboardIdStr = savedDashboard.getId().getId().toString();
        doGet("/api/dashboard/" + savedDashboard.getId().getId().toString())
                .andExpect(status().isNotFound())
                .andExpect(statusReason(containsString(msgErrorNoFound("Dashboard", dashboardIdStr))));
    }

    @Test
    public void testSaveDashboardWithEmptyTitle() throws Exception {
        Dashboard dashboard = new Dashboard();
        String msgError = "Dashboard title " + msgErrorShouldBeSpecified;;

        Mockito.reset(tbClusterService, auditLogService);

        doPost("/api/dashboard", dashboard)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(dashboard, savedTenant.getId(),
                tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.ADDED, new DataValidationException(msgError));
    }

    @Test
    public void testAssignUnassignDashboardToCustomer() throws Exception {
        Dashboard dashboard = new Dashboard();
        dashboard.setTitle("My dashboard");
        Dashboard savedDashboard = doPost("/api/dashboard", dashboard, Dashboard.class);

        Customer customer = new Customer();
        customer.setTitle("My customer");
        Customer savedCustomer = doPost("/api/customer", customer, Customer.class);

        Mockito.reset(tbClusterService, auditLogService);

        Dashboard assignedDashboard = doPost("/api/customer/" + savedCustomer.getId().getId().toString()
                + "/dashboard/" + savedDashboard.getId().getId().toString(), Dashboard.class);

        Assert.assertTrue(assignedDashboard.getAssignedCustomers().contains(savedCustomer.toShortCustomerInfo()));

        testNotifyEntityAllOneTimeLogEntityActionEntityEqClass(assignedDashboard, assignedDashboard.getId(), assignedDashboard.getId(),
                savedTenant.getId(),  savedCustomer.getId(), tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.ASSIGNED_TO_CUSTOMER,
                assignedDashboard .getId().getId().toString(), savedCustomer.getId().getId().toString(), savedCustomer.getTitle());

        Dashboard foundDashboard = doGet("/api/dashboard/" + savedDashboard.getId().getId().toString(), Dashboard.class);
        Assert.assertTrue(foundDashboard.getAssignedCustomers().contains(savedCustomer.toShortCustomerInfo()));

        Mockito.reset(tbClusterService, auditLogService);

        Dashboard unassignedDashboard =
                doDelete("/api/customer/" + savedCustomer.getId().getId().toString() + "/dashboard/" + savedDashboard.getId().getId().toString(), Dashboard.class);

        testNotifyEntityAllOneTimeLogEntityActionEntityEqClass(assignedDashboard, assignedDashboard.getId(), assignedDashboard.getId(),
                savedTenant.getId(), savedCustomer.getId(), tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.UNASSIGNED_FROM_CUSTOMER,
                unassignedDashboard.getId().getId().toString(), savedCustomer.getId().getId().toString(), savedCustomer.getTitle());

        Assert.assertTrue(unassignedDashboard.getAssignedCustomers() == null || unassignedDashboard.getAssignedCustomers().isEmpty());

        foundDashboard = doGet("/api/dashboard/" + savedDashboard.getId().getId().toString(), Dashboard.class);

        Assert.assertTrue(foundDashboard.getAssignedCustomers() == null || foundDashboard.getAssignedCustomers().isEmpty());
    }

    @Test
    public void testAssignDashboardToNonExistentCustomer() throws Exception {
        Dashboard dashboard = new Dashboard();
        dashboard.setTitle("My dashboard");
        Dashboard savedDashboard = doPost("/api/dashboard", dashboard, Dashboard.class);

        String customerIdStr = Uuids.timeBased().toString();
        doPost("/api/customer/" + customerIdStr
                + "/dashboard/" + savedDashboard.getId().getId().toString())
                .andExpect(status().isNotFound())
                .andExpect(statusReason(containsString(msgErrorNoFound("Customer", customerIdStr))));

        Mockito.reset(tbClusterService, auditLogService);
        testNotifyEntityNever(savedDashboard.getId(), savedDashboard);
    }

    @Test
    public void testAssignDashboardToCustomerFromDifferentTenant() throws Exception {
        loginSysAdmin();

        Tenant tenant2 = new Tenant();
        tenant2.setTitle("Different tenant");
        Tenant savedTenant2 = doPost("/api/tenant", tenant2, Tenant.class);
        Assert.assertNotNull(savedTenant2);

        User tenantAdmin2 = new User();
        tenantAdmin2.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin2.setTenantId(savedTenant2.getId());
        tenantAdmin2.setEmail("tenant3@thingsboard.org");
        tenantAdmin2.setFirstName("Joe");
        tenantAdmin2.setLastName("Downs");

        createUserAndLogin(tenantAdmin2, "testPassword1");

        Customer customer = new Customer();
        customer.setTitle("Different customer");
        Customer savedCustomer = doPost("/api/customer", customer, Customer.class);

        login(tenantAdmin.getEmail(), "testPassword1");

        Dashboard dashboard = new Dashboard();
        dashboard.setTitle("My dashboard");
        Dashboard savedDashboard = doPost("/api/dashboard", dashboard, Dashboard.class);

        doPost("/api/customer/" + savedCustomer.getId().getId().toString()
                + "/dashboard/" + savedDashboard.getId().getId().toString())
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermission)));

        Mockito.reset(tbClusterService, auditLogService);
        testNotifyEntityNever(savedDashboard.getId(), savedDashboard);

        doDelete("/api/tenant/" + savedTenant2.getId().getId().toString())
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermission)));

        testNotifyEntityNever(savedDashboard.getId(), savedDashboard);

        loginSysAdmin();

        doDelete("/api/tenant/" + savedTenant2.getId().getId().toString())
                .andExpect(status().isOk());
    }

    @Test
    public void testFindTenantDashboards() throws Exception {
        List<DashboardInfo> dashboards = new ArrayList<>();

        Mockito.reset(tbClusterService, auditLogService);

        int cntEntity = 173;
        for (int i = 0; i < cntEntity; i++) {
            Dashboard dashboard = new Dashboard();
            dashboard.setTitle("Dashboard" + i);
            dashboards.add(new DashboardInfo(doPost("/api/dashboard", dashboard, Dashboard.class)));
        }

        testNotifyManyEntityManyTimeMsgToEdgeServiceNever(new Dashboard(), new Dashboard(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ADDED, cntEntity);

        List<DashboardInfo> loadedDashboards = new ArrayList<>();
        PageLink pageLink = new PageLink(24);
        PageData<DashboardInfo> pageData = null;
        do {
            pageData = doGetTypedWithPageLink("/api/tenant/dashboards?",
                    new TypeReference<PageData<DashboardInfo>>() {
                    }, pageLink);
            loadedDashboards.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(dashboards, idComparator);
        Collections.sort(loadedDashboards, idComparator);

        Assert.assertEquals(dashboards, loadedDashboards);
    }

    @Test
    public void testFindTenantDashboardsByTitle() throws Exception {
        String title1 = "Dashboard title 1";
        List<DashboardInfo> dashboardsTitle1 = new ArrayList<>();
        int cntEntity = 134;
        for (int i = 0; i < cntEntity; i++) {
            Dashboard dashboard = new Dashboard();
            String suffix = StringUtils.randomAlphanumeric((int) (Math.random() * 15));
            String title = title1 + suffix;
            title = i % 2 == 0 ? title.toLowerCase() : title.toUpperCase();
            dashboard.setTitle(title);
            dashboardsTitle1.add(new DashboardInfo(doPost("/api/dashboard", dashboard, Dashboard.class)));
        }
        String title2 = "Dashboard title 2";
        List<DashboardInfo> dashboardsTitle2 = new ArrayList<>();

        for (int i = 0; i < 112; i++) {
            Dashboard dashboard = new Dashboard();
            String suffix = StringUtils.randomAlphanumeric((int) (Math.random() * 15));
            String title = title2 + suffix;
            title = i % 2 == 0 ? title.toLowerCase() : title.toUpperCase();
            dashboard.setTitle(title);
            dashboardsTitle2.add(new DashboardInfo(doPost("/api/dashboard", dashboard, Dashboard.class)));
        }

        List<DashboardInfo> loadedDashboardsTitle1 = new ArrayList<>();
        PageLink pageLink = new PageLink(15, 0, title1);
        PageData<DashboardInfo> pageData = null;
        do {
            pageData = doGetTypedWithPageLink("/api/tenant/dashboards?",
                    new TypeReference<PageData<DashboardInfo>>() {
                    }, pageLink);
            loadedDashboardsTitle1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(dashboardsTitle1, idComparator);
        Collections.sort(loadedDashboardsTitle1, idComparator);

        Assert.assertEquals(dashboardsTitle1, loadedDashboardsTitle1);

        List<DashboardInfo> loadedDashboardsTitle2 = new ArrayList<>();
        pageLink = new PageLink(4, 0, title2);
        do {
            pageData = doGetTypedWithPageLink("/api/tenant/dashboards?",
                    new TypeReference<PageData<DashboardInfo>>() {
                    }, pageLink);
            loadedDashboardsTitle2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(dashboardsTitle2, idComparator);
        Collections.sort(loadedDashboardsTitle2, idComparator);

        Assert.assertEquals(dashboardsTitle2, loadedDashboardsTitle2);

        Mockito.reset(tbClusterService, auditLogService);

        for (DashboardInfo dashboard : loadedDashboardsTitle1) {
            doDelete("/api/dashboard/" + dashboard.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        testNotifyManyEntityManyTimeMsgToEdgeServiceNeverAdditionalInfoAny(new Dashboard(), new Dashboard(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.DELETED, cntEntity, 1);

        pageLink = new PageLink(4, 0, title1);
        pageData = doGetTypedWithPageLink("/api/tenant/dashboards?",
                new TypeReference<PageData<DashboardInfo>>() {
                }, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (DashboardInfo dashboard : loadedDashboardsTitle2) {
            doDelete("/api/dashboard/" + dashboard.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        pageLink = new PageLink(4, 0, title2);
        pageData = doGetTypedWithPageLink("/api/tenant/dashboards?",
                new TypeReference<PageData<DashboardInfo>>() {
                }, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

    @Test
    public void testFindCustomerDashboards() throws Exception {
        Customer customer = new Customer();
        customer.setTitle("Test customer");
        customer = doPost("/api/customer", customer, Customer.class);
        CustomerId customerId = customer.getId();

        Mockito.reset(tbClusterService, auditLogService);

        int cntEntity = 173;
        List<DashboardInfo> dashboards = new ArrayList<>();
        for (int i = 0; i < cntEntity; i++) {
            Dashboard dashboard = new Dashboard();
            dashboard.setTitle("Dashboard" + i);
            dashboard = doPost("/api/dashboard", dashboard, Dashboard.class);
            dashboards.add(new DashboardInfo(doPost("/api/customer/" + customerId.getId().toString()
                    + "/dashboard/" + dashboard.getId().getId().toString(), Dashboard.class)));
        }

        testNotifyManyEntityManyTimeMsgToEdgeServiceEntityEqAny(new Dashboard(), new Dashboard(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ADDED, ActionType.ASSIGNED_TO_CUSTOMER, cntEntity, cntEntity, cntEntity*2);

        List<DashboardInfo> loadedDashboards = new ArrayList<>();
        PageLink pageLink = new PageLink(21);
        PageData<DashboardInfo> pageData = null;
        do {
            pageData = doGetTypedWithPageLink("/api/customer/" + customerId.getId().toString() + "/dashboards?",
                    new TypeReference<PageData<DashboardInfo>>() {
                    }, pageLink);
            loadedDashboards.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(dashboards, idComparator);
        Collections.sort(loadedDashboards, idComparator);

        Assert.assertEquals(dashboards, loadedDashboards);
    }

    @Test
    public void testAssignDashboardToEdge() throws Exception {
        Edge edge = constructEdge("My edge", "default");
        Edge savedEdge = doPost("/api/edge", edge, Edge.class);

        Dashboard dashboard = new Dashboard();
        dashboard.setTitle("My dashboard");
        Dashboard savedDashboard = doPost("/api/dashboard", dashboard, Dashboard.class);

        Mockito.reset(tbClusterService, auditLogService);

        doPost("/api/edge/" + savedEdge.getId().getId().toString()
                + "/dashboard/" + savedDashboard.getId().getId().toString(), Dashboard.class);

        testNotifyEntityAllOneTime(savedDashboard, savedDashboard.getId(), savedDashboard.getId(), savedTenant.getId(),
                tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.ASSIGNED_TO_EDGE,
                savedDashboard.getId().getId().toString(), savedEdge.getId().getId().toString(), savedEdge.getName());

        PageData<Dashboard> pageData = doGetTypedWithPageLink("/api/edge/" + savedEdge.getId().getId().toString() + "/dashboards?",
                new TypeReference<PageData<Dashboard>>() {
                }, new PageLink(100));

        Assert.assertEquals(1, pageData.getData().size());

        doDelete("/api/edge/" + savedEdge.getId().getId().toString()
                + "/dashboard/" + savedDashboard.getId().getId().toString(), Dashboard.class);

        pageData = doGetTypedWithPageLink("/api/edge/" + savedEdge.getId().getId().toString() + "/dashboards?",
                new TypeReference<PageData<Dashboard>>() {
                }, new PageLink(100));

        Assert.assertEquals(0, pageData.getData().size());
    }

    @Test
    public void testDeleteDashboardWithDeleteRelationsOk() throws Exception {
        DashboardId dashboardId = createDashboard("Dashboard for Test WithRelationsOk").getId();
        testEntityDaoWithRelationsOk(savedTenant.getId(), dashboardId, "/api/dashboard/" + dashboardId);
    }

    @Test
    public void testDeleteDashboardExceptionWithRelationsTransactional() throws Exception {
        DashboardId dashboardId = createDashboard("Dashboard for Test WithRelations Transactional Exception").getId();
        testEntityDaoWithRelationsTransactionalException(dashboardDao, savedTenant.getId(), dashboardId, "/api/dashboard/" + dashboardId);
    }

    private Dashboard createDashboard(String title) {
        Dashboard dashboard = new Dashboard();
        dashboard.setTitle(title);
        return doPost("/api/dashboard", dashboard, Dashboard.class);
    }
}
