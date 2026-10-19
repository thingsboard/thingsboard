// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.sql.dashboard;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.dashboard.DashboardInfoDao;
import org.thingsboard.server.dao.service.AbstractServiceTest;

import java.util.UUID;

/**
 * Created by Valerii Sosliuk on 5/6/2017.
 */
public class JpaDashboardInfoDaoTest extends AbstractJpaDaoTest {

    @Autowired
    private DashboardInfoDao dashboardInfoDao;

    @Test
    public void testFindDashboardsByTenantId() {
        UUID tenantId1 = Uuids.timeBased();
        UUID tenantId2 = Uuids.timeBased();

        for (int i = 0; i < 20; i++) {
            createDashboard(tenantId1, i);
            createDashboard(tenantId2, i * 2);
        }

        PageLink pageLink = new PageLink(15, 0, "DASHBOARD");
        PageData<DashboardInfo> dashboardInfos1 = dashboardInfoDao.findDashboardsByTenantId(tenantId1, pageLink);
        Assert.assertEquals(15, dashboardInfos1.getData().size());

        PageData<DashboardInfo> dashboardInfos2 = dashboardInfoDao.findDashboardsByTenantId(tenantId1, pageLink.nextPageLink());
        Assert.assertEquals(5, dashboardInfos2.getData().size());
    }

    private void createDashboard(UUID tenantId, int index) {
        DashboardInfo dashboardInfo = new DashboardInfo();
        dashboardInfo.setId(new DashboardId(Uuids.timeBased()));
        dashboardInfo.setTenantId(TenantId.fromUUID(tenantId));
        dashboardInfo.setTitle("DASHBOARD_" + index);
        dashboardInfoDao.save(AbstractServiceTest.SYSTEM_TENANT_ID, dashboardInfo);
    }
}
