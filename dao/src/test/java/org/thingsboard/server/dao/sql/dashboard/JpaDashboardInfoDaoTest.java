// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.sql.dashboard;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.dashboard.DashboardDao;
import org.thingsboard.server.dao.dashboard.DashboardInfoDao;
import org.thingsboard.server.dao.service.AbstractServiceTest;

import java.util.UUID;

/**
 * Created by Valerii Sosliuk on 5/6/2017.
 */
public class JpaDashboardInfoDaoTest extends AbstractJpaDaoTest {

    @Autowired
    private DashboardInfoDao dashboardInfoDao;

    @Autowired
    private DashboardDao dashboardDao;

    @Autowired
    private CustomerDao customerDao;

    @Test
    public void testFindDashboardsByTenantId() {
        UUID tenantId1 = Uuids.timeBased();
        UUID tenantId2 = Uuids.timeBased();

        for (int i = 0; i < 20; i++) {
            createDashboard(tenantId1, null, i);
            createDashboard(tenantId2, null, i * 2);
        }

        PageLink pageLink = new PageLink(15, 0, "DASHBOARD");
        PageData<DashboardInfo> dashboardInfos1 = dashboardInfoDao.findDashboardsByTenantId(tenantId1, pageLink);
        Assert.assertEquals(15, dashboardInfos1.getData().size());

        PageData<DashboardInfo> dashboardInfos2 = dashboardInfoDao.findDashboardsByTenantId(tenantId1, pageLink.nextPageLink());
        Assert.assertEquals(5, dashboardInfos2.getData().size());
    }

    @Test
    public void testFindDashboardsByTenantIdAndCustomerIdIncludingSubCustomers() {
        UUID tenantId1 = Uuids.timeBased();
        Customer customer1 = createCustomer(tenantId1, null, 0);
        Customer subCustomer2 = createCustomer(tenantId1, customer1.getUuidId(), 1);

        for (int i = 0; i < 20; i++) {
            createDashboard(tenantId1, customer1.getUuidId(), i);
            createDashboard(tenantId1, subCustomer2.getUuidId(), i * 2);
        }

        PageLink pageLink = new PageLink(30, 0, "DASHBOARD", new SortOrder("ownerName", SortOrder.Direction.ASC));
        PageData<DashboardInfo> dashboardInfos1 = dashboardInfoDao.findDashboardsByTenantIdAndCustomerIdIncludingSubCustomers(tenantId1, customer1.getUuidId(), pageLink);
        Assert.assertEquals(30, dashboardInfos1.getData().size());
        dashboardInfos1.getData().forEach(dashboardInfo -> Assert.assertNotEquals("CUSTOMER_0", dashboardInfo.getOwnerName()));

        PageData<DashboardInfo> dashboardInfos2 = dashboardInfoDao.findDashboardsByTenantIdAndCustomerIdIncludingSubCustomers(tenantId1, customer1.getUuidId(), pageLink.nextPageLink());
        Assert.assertEquals(10, dashboardInfos2.getData().size());

        PageData<DashboardInfo> dashboardInfos3 = dashboardInfoDao.findDashboardsByTenantIdAndCustomerIdIncludingSubCustomers(tenantId1, subCustomer2.getUuidId(), pageLink);
        Assert.assertEquals(20, dashboardInfos3.getData().size());
    }

    private void createDashboard(UUID tenantId, UUID customerId, int index) {
        Dashboard dashboard = new Dashboard();
        dashboard.setId(new DashboardId(Uuids.timeBased()));
        dashboard.setTenantId(TenantId.fromUUID(tenantId));
        dashboard.setCustomerId(new CustomerId(customerId));
        dashboard.setTitle("DASHBOARD_" + index);
        dashboardDao.save(AbstractServiceTest.SYSTEM_TENANT_ID, dashboard);
    }

    private Customer createCustomer(UUID tenantId, UUID parentCustomerId, int index) {
        Customer customer = new Customer();
        customer.setId(new CustomerId(Uuids.timeBased()));
        if (parentCustomerId != null) {
            customer.setParentCustomerId(new CustomerId(parentCustomerId));
        }
        customer.setTenantId(TenantId.fromUUID(tenantId));
        customer.setTitle("CUSTOMER_" + index);
        return customerDao.save(TenantId.fromUUID(tenantId), customer);
    }

}
