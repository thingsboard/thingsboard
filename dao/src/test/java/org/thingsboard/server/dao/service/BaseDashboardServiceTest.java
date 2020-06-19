/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.dao.service;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.exception.DataValidationException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

public abstract class BaseDashboardServiceTest extends AbstractServiceTest {
    
    private IdComparator<DashboardInfo> idComparator = new IdComparator<>();
    
    private TenantId tenantId;

    @Before
    public void before() {
        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        Tenant savedTenant = tenantService.saveTenant(tenant);
        Assert.assertNotNull(savedTenant);
        tenantId = savedTenant.getId();
    }

    @After
    public void after() {
        tenantService.deleteTenant(tenantId);
    }
    
    @Test
    public void testSaveDashboard() throws IOException {
        Dashboard dashboard = new Dashboard();
        dashboard.setTenantId(tenantId);
        dashboard.setTitle("My dashboard");
        Dashboard savedDashboard = dashboardService.saveDashboard(dashboard);
        
        Assert.assertNotNull(savedDashboard);
        Assert.assertNotNull(savedDashboard.getId());
        Assert.assertTrue(savedDashboard.getCreatedTime() > 0);
        Assert.assertEquals(dashboard.getTenantId(), savedDashboard.getTenantId());
        Assert.assertEquals(dashboard.getTitle(), savedDashboard.getTitle());
        
        savedDashboard.setTitle("My new dashboard");
        
        dashboardService.saveDashboard(savedDashboard);
        Dashboard foundDashboard = dashboardService.findDashboardById(tenantId, savedDashboard.getId());
        Assert.assertEquals(foundDashboard.getTitle(), savedDashboard.getTitle());
        
        dashboardService.deleteDashboard(tenantId, savedDashboard.getId());
    }
    
    @Test(expected = DataValidationException.class)
    public void testSaveDashboardWithEmptyTitle() {
        Dashboard dashboard = new Dashboard();
        dashboard.setTenantId(tenantId);
        dashboardService.saveDashboard(dashboard);
    }
    
    @Test(expected = DataValidationException.class)
    public void testSaveDashboardWithEmptyTenant() {
        Dashboard dashboard = new Dashboard();
        dashboard.setTitle("My dashboard");
        dashboardService.saveDashboard(dashboard);
    }
    
    @Test(expected = DataValidationException.class)
    public void testSaveDashboardWithInvalidTenant() {
        Dashboard dashboard = new Dashboard();
        dashboard.setTitle("My dashboard");
        dashboard.setTenantId(new TenantId(Uuids.timeBased()));
        dashboardService.saveDashboard(dashboard);
    }
    
    @Test(expected = DataValidationException.class)
    public void testAssignDashboardToNonExistentCustomer() {
        Dashboard dashboard = new Dashboard();
        dashboard.setTitle("My dashboard");
        dashboard.setTenantId(tenantId);
        dashboard = dashboardService.saveDashboard(dashboard);
        try {
            dashboardService.assignDashboardToCustomer(tenantId, dashboard.getId(), new CustomerId(Uuids.timeBased()));
        } finally {
            dashboardService.deleteDashboard(tenantId, dashboard.getId());
        }
    }
    
    @Test(expected = DataValidationException.class)
    public void testAssignDashboardToCustomerFromDifferentTenant() {
        Dashboard dashboard = new Dashboard();
        dashboard.setTitle("My dashboard");
        dashboard.setTenantId(tenantId);
        dashboard = dashboardService.saveDashboard(dashboard);
        Tenant tenant = new Tenant();
        tenant.setTitle("Test different tenant [dashboard]");
        tenant = tenantService.saveTenant(tenant);
        Customer customer = new Customer();
        customer.setTenantId(tenant.getId());
        customer.setTitle("Test different customer");
        customer = customerService.saveCustomer(customer);
        try {
            dashboardService.assignDashboardToCustomer(tenantId, dashboard.getId(), customer.getId());
        } finally {
            dashboardService.deleteDashboard(tenantId, dashboard.getId());
            tenantService.deleteTenant(tenant.getId());
        }
    }
    
    @Test
    public void testFindDashboardById() {
        Dashboard dashboard = new Dashboard();
        dashboard.setTenantId(tenantId);
        dashboard.setTitle("My dashboard");
        Dashboard savedDashboard = dashboardService.saveDashboard(dashboard);
        Dashboard foundDashboard = dashboardService.findDashboardById(tenantId, savedDashboard.getId());
        Assert.assertNotNull(foundDashboard);
        Assert.assertEquals(savedDashboard, foundDashboard);
        dashboardService.deleteDashboard(tenantId, savedDashboard.getId());
    }
    
    @Test
    public void testDeleteDashboard() {
        Dashboard dashboard = new Dashboard();
        dashboard.setTenantId(tenantId);
        dashboard.setTitle("My dashboard");
        Dashboard savedDashboard = dashboardService.saveDashboard(dashboard);
        Dashboard foundDashboard = dashboardService.findDashboardById(tenantId, savedDashboard.getId());
        Assert.assertNotNull(foundDashboard);
        dashboardService.deleteDashboard(tenantId, savedDashboard.getId());
        foundDashboard = dashboardService.findDashboardById(tenantId, savedDashboard.getId());
        Assert.assertNull(foundDashboard);
    }
    
    @Test
    public void testFindDashboardsByTenantId() {
        Tenant tenant = new Tenant();
        tenant.setTitle("Test tenant");
        tenant = tenantService.saveTenant(tenant);
        
        TenantId tenantId = tenant.getId();
        
        List<DashboardInfo> dashboards = new ArrayList<>();
        for (int i=0;i<165;i++) {
            Dashboard dashboard = new Dashboard();
            dashboard.setTenantId(tenantId);
            dashboard.setTitle("Dashboard"+i);
            dashboards.add(new DashboardInfo(dashboardService.saveDashboard(dashboard)));
        }
        
        List<DashboardInfo> loadedDashboards = new ArrayList<>();
        PageLink pageLink = new PageLink(16);
        PageData<DashboardInfo> pageData = null;
        do {
            pageData = dashboardService.findDashboardsByTenantId(tenantId, pageLink);
            loadedDashboards.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());
        
        Collections.sort(dashboards, idComparator);
        Collections.sort(loadedDashboards, idComparator);
        
        Assert.assertEquals(dashboards, loadedDashboards);
        
        dashboardService.deleteDashboardsByTenantId(tenantId);

        pageLink = new PageLink(31);
        pageData = dashboardService.findDashboardsByTenantId(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertTrue(pageData.getData().isEmpty());
        
        tenantService.deleteTenant(tenantId);
    }
    
    @Test
    public void testFindDashboardsByTenantIdAndTitle() {
        String title1 = "Dashboard title 1";
        List<DashboardInfo> dashboardsTitle1 = new ArrayList<>();
        for (int i=0;i<123;i++) {
            Dashboard dashboard = new Dashboard();
            dashboard.setTenantId(tenantId);
            String suffix = RandomStringUtils.randomAlphanumeric((int)(Math.random()*17));
            String title = title1+suffix;
            title = i % 2 == 0 ? title.toLowerCase() : title.toUpperCase();
            dashboard.setTitle(title);
            dashboardsTitle1.add(new DashboardInfo(dashboardService.saveDashboard(dashboard)));
        }
        String title2 = "Dashboard title 2";
        List<DashboardInfo> dashboardsTitle2 = new ArrayList<>();
        for (int i=0;i<193;i++) {
            Dashboard dashboard = new Dashboard();
            dashboard.setTenantId(tenantId);
            String suffix = RandomStringUtils.randomAlphanumeric((int)(Math.random()*15));
            String title = title2+suffix;
            title = i % 2 == 0 ? title.toLowerCase() : title.toUpperCase();
            dashboard.setTitle(title);
            dashboardsTitle2.add(new DashboardInfo(dashboardService.saveDashboard(dashboard)));
        }
        
        List<DashboardInfo> loadedDashboardsTitle1 = new ArrayList<>();
        PageLink pageLink = new PageLink(19, 0, title1);
        PageData<DashboardInfo> pageData = null;
        do {
            pageData = dashboardService.findDashboardsByTenantId(tenantId, pageLink);
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
            pageData = dashboardService.findDashboardsByTenantId(tenantId, pageLink);
            loadedDashboardsTitle2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(dashboardsTitle2, idComparator);
        Collections.sort(loadedDashboardsTitle2, idComparator);
        
        Assert.assertEquals(dashboardsTitle2, loadedDashboardsTitle2);

        for (DashboardInfo dashboard : loadedDashboardsTitle1) {
            dashboardService.deleteDashboard(tenantId, dashboard.getId());
        }
        
        pageLink = new PageLink(4, 0, title1);
        pageData = dashboardService.findDashboardsByTenantId(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
        
        for (DashboardInfo dashboard : loadedDashboardsTitle2) {
            dashboardService.deleteDashboard(tenantId, dashboard.getId());
        }
        
        pageLink = new PageLink(4, 0, title2);
        pageData = dashboardService.findDashboardsByTenantId(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }
    
    @Test
    public void testFindDashboardsByTenantIdAndCustomerId() throws ExecutionException, InterruptedException {
        Tenant tenant = new Tenant();
        tenant.setTitle("Test tenant");
        tenant = tenantService.saveTenant(tenant);
        
        TenantId tenantId = tenant.getId();
        
        Customer customer = new Customer();
        customer.setTitle("Test customer");
        customer.setTenantId(tenantId);
        customer = customerService.saveCustomer(customer);
        CustomerId customerId = customer.getId();
        
        List<DashboardInfo> dashboards = new ArrayList<>();
        for (int i=0;i<223;i++) {
            Dashboard dashboard = new Dashboard();
            dashboard.setTenantId(tenantId);
            dashboard.setTitle("Dashboard"+i);
            dashboard = dashboardService.saveDashboard(dashboard);
            dashboards.add(new DashboardInfo(dashboardService.assignDashboardToCustomer(tenantId, dashboard.getId(), customerId)));
        }
        
        List<DashboardInfo> loadedDashboards = new ArrayList<>();
        PageLink pageLink = new PageLink(23);
        PageData<DashboardInfo> pageData = null;
        do {
            pageData = dashboardService.findDashboardsByTenantIdAndCustomerId(tenantId, customerId, pageLink);
            loadedDashboards.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());
        
        Collections.sort(dashboards, idComparator);
        Collections.sort(loadedDashboards, idComparator);
        
        Assert.assertEquals(dashboards, loadedDashboards);
        
        dashboardService.unassignCustomerDashboards(tenantId, customerId);

        pageLink = new PageLink(42);
        pageData = dashboardService.findDashboardsByTenantIdAndCustomerId(tenantId, customerId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertTrue(pageData.getData().isEmpty());
        
        tenantService.deleteTenant(tenantId);
    }

    @Test(expected = DataValidationException.class)
    public void testAssignDashboardToNonExistentEdge() {
        Dashboard dashboard = new Dashboard();
        dashboard.setTitle("My dashboard");
        dashboard.setTenantId(tenantId);
        dashboard = dashboardService.saveDashboard(dashboard);
        try {
            dashboardService.assignDashboardToEdge(tenantId, dashboard.getId(), new EdgeId(Uuids.timeBased()));
        } finally {
            dashboardService.deleteDashboard(tenantId, dashboard.getId());
        }
    }

    @Test(expected = DataValidationException.class)
    public void testAssignDashboardToEdgeFromDifferentTenant() {
        Dashboard dashboard = new Dashboard();
        dashboard.setTitle("My dashboard");
        dashboard.setTenantId(tenantId);
        dashboard = dashboardService.saveDashboard(dashboard);
        Tenant tenant = new Tenant();
        tenant.setTitle("Test different tenant [edge]");
        tenant = tenantService.saveTenant(tenant);
        Edge edge = new Edge();
        edge.setTenantId(tenant.getId());
        edge.setType("default");
        edge.setName("Test different edge");
        edge.setType("default");
        edge = edgeService.saveEdge(edge);
        try {
            dashboardService.assignDashboardToEdge(tenantId, dashboard.getId(), edge.getId());
        } finally {
            dashboardService.deleteDashboard(tenantId, dashboard.getId());
            tenantService.deleteTenant(tenant.getId());
        }
    }
}
