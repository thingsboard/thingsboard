// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.msa.trendz;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.trendz.TrendzHealthcheckResult;
import org.thingsboard.server.common.data.trendz.TrendzSummary;
import org.thingsboard.server.common.data.trendz.TrendzSynchronizationResult;
import org.thingsboard.server.common.data.trendz.TrendzSynchronizationStatus;
import org.thingsboard.server.msa.AbstractContainerTest;
import org.thingsboard.server.msa.DisableUIListeners;
import org.thingsboard.server.msa.ui.utils.EntityPrototypes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.msa.ui.utils.EntityPrototypes.defaultTenantAdmin;

@DisableUIListeners
public class TrendzTest extends AbstractContainerTest {
    private TenantId tenantId;

    @BeforeClass
    public void beforeClass() {
        testRestClient.login(SYS_ADMIN_EMAIL, SYS_ADMIN_PASSWORD);
        TrendzSynchronizationResult result = testRestClient.connectTrendz();
        assertThat(result.status())
                .isEqualTo(TrendzSynchronizationStatus.SYNCED);

        tenantId = testRestClient.postTenant(EntityPrototypes.defaultTenantPrototype("Tenant")).getId();
        testRestClient.createUserAndLogin(defaultTenantAdmin(tenantId, "tenantAdmin@thingsboard.org"), "tenant");
    }

    @Test
    public void testTrendzHealthCheck() {
        TrendzHealthcheckResult result = testRestClient.trendzHealthcheck();
        assertThat(result.status())
                .isEqualTo(TrendzSynchronizationStatus.SYNCED);
    }

    @Test
    public void testGetTrendzSummary() {
        TrendzSummary trendzSummary = testRestClient.getTrendzSummary();
        assertThat(trendzSummary)
                .isNotNull();
    }

    @AfterClass
    public void afterClass() {
        testRestClient.resetToken();
        testRestClient.login(SYS_ADMIN_EMAIL, SYS_ADMIN_PASSWORD);
        testRestClient.deleteTenant(tenantId);
    }
}
