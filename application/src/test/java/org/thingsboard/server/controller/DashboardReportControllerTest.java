// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.ResultActions;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.DashboardReportService;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.dashboardreport.DashboardReportConfig;
import org.thingsboard.server.common.data.dashboardreport.DashboardReportData;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.role.RoleType;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class DashboardReportControllerTest extends AbstractControllerTest {

    private static final String DASHBOARD_VIEWER_EMAIL = "dashboard.viewer@thingsboard.org";
    private static final String DASHBOARD_VIEWER_PASSWORD = "viewer";
    private static final String REPORTS_SERVER_URL = "http://localhost:8383";
    private static final String REPORT_BASE_URL = "http://localhost:8081";

    @MockitoSpyBean
    private DashboardReportService dashboardReportService;

    private Dashboard dashboard;

    @Before
    public void beforeTest() throws Exception {
        loginSubCustomerAdminUser();
        Dashboard dashboard = new Dashboard();
        dashboard.setTitle("Sub-customer dashboard");
        this.dashboard = doPost("/api/dashboard", dashboard, Dashboard.class);

        doAnswer(invocation -> {
            Consumer<DashboardReportData> onSuccess = invocation.getArgument(3);
            DashboardReportData reportData = new DashboardReportData();
            reportData.setData(new byte[]{1});
            reportData.setName("report.pdf");
            reportData.setContentType("application/pdf");
            onSuccess.accept(reportData);
            return null;
        }).when(dashboardReportService).generateReport(any(), any(), any(), any(), any());
    }

    @Test
    public void testDownloadTestReportRequiresPermissionOverReportUser() throws Exception {
        loginDifferentTenant();
        UserId otherTenantUserId = savedDifferentTenantUser.getId();
        loginDifferentCustomer();

        loginCustomerAdminUser();
        doPostTestReport(tenantAdminUserId.toString()).andExpect(status().isForbidden());
        doPostTestReport(differentCustomerUserId.toString()).andExpect(status().isForbidden());
        doPostTestReport(otherTenantUserId.toString()).andExpect(status().isForbidden());

        loginSubCustomerAdminUser();
        doPostTestReport(customerAdminUserId.toString()).andExpect(status().isForbidden());

        loginTenantAdmin();
        doPostTestReport(otherTenantUserId.toString()).andExpect(status().isForbidden());

        createDashboardViewer();
        login(DASHBOARD_VIEWER_EMAIL, DASHBOARD_VIEWER_PASSWORD);
        doPostTestReport(customerAdminUserId.toString()).andExpect(status().isForbidden());
        doPostTestReport(customerUserId.toString()).andExpect(status().isForbidden());

        verify(dashboardReportService, never()).generateReport(any(), any(), any(), any(), any());
    }

    @Test
    public void testDownloadTestReportAllowedForOwnUser() throws Exception {
        loginCustomerAdminUser();
        doPostTestReport(customerAdminUserId.toString()).andExpect(status().isOk());
        verifyReportGenerated(customerAdminUserId);

        UserId dashboardViewerId = createDashboardViewer();
        login(DASHBOARD_VIEWER_EMAIL, DASHBOARD_VIEWER_PASSWORD);
        doPostTestReport(dashboardViewerId.toString()).andExpect(status().isOk());
        verifyReportGenerated(dashboardViewerId);
    }

    @Test
    public void testDownloadTestReportAllowedForOwnedUser() throws Exception {
        loginTenantAdmin();
        doPostTestReport(customerUserId.toString()).andExpect(status().isOk());
        verifyReportGenerated(customerUserId);

        loginCustomerAdminUser();
        doPostTestReport(subCustomerAdminUserId.toString()).andExpect(status().isOk());
        verifyReportGenerated(subCustomerAdminUserId);
        doPostTestReport(customerUserId.toString()).andExpect(status().isOk());
    }

    @Test
    public void testDownloadTestReportWithCurrentUserCredentialsUsesCurrentUserWhenReportUserNotAccessible() throws Exception {
        loginCustomerAdminUser();
        doPostTestReport(tenantAdminUserId.toString(), null, true).andExpect(status().isOk());
        verifyReportGenerated(customerAdminUserId);
    }

    @Test
    public void testDownloadTestReportRequiresUserId() throws Exception {
        loginCustomerAdminUser();
        doPostTestReport(null).andExpect(status().isBadRequest());
        doPostTestReport("not-a-uuid").andExpect(status().isBadRequest());
        verify(dashboardReportService, never()).generateReport(any(), any(), any(), any(), any());
    }

    @Test
    public void testDownloadTestReportUsesSystemReportServerForCustomerUser() throws Exception {
        loginCustomerAdminUser();
        doPostTestReport(customerAdminUserId.toString(), REPORTS_SERVER_URL).andExpect(status().isOk());
        verify(dashboardReportService).generateReport(eq(tenantId), any(), isNull(), any(), any());

        loginTenantAdmin();
        doPostTestReport(tenantAdminUserId.toString(), REPORTS_SERVER_URL).andExpect(status().isOk());
        verify(dashboardReportService).generateReport(eq(tenantId), any(), eq(REPORTS_SERVER_URL), any(), any());
    }

    private UserId createDashboardViewer() throws Exception {
        loginTenantAdmin();
        Role role = new Role();
        role.setTenantId(tenantId);
        role.setCustomerId(customerId);
        role.setType(RoleType.GENERIC);
        role.setName("Dashboard viewer");
        role.setPermissions(JacksonUtil.toJsonNode("{\"DASHBOARD\":[\"READ\"]}"));
        role = doPost("/api/role", role, Role.class);

        EntityGroup userGroup = new EntityGroup();
        userGroup.setName("Dashboard viewers");
        userGroup.setType(EntityType.USER);
        userGroup.setOwnerId(customerId);
        userGroup = doPost("/api/entityGroup", userGroup, EntityGroup.class);
        doPost("/api/groupPermission", new GroupPermission(tenantId, userGroup.getId(), role.getId(), null, null, false), GroupPermission.class);

        User user = new User();
        user.setAuthority(Authority.CUSTOMER_USER);
        user.setTenantId(tenantId);
        user.setCustomerId(customerId);
        user.setEmail(DASHBOARD_VIEWER_EMAIL);
        return createUser(user, DASHBOARD_VIEWER_PASSWORD, userGroup.getId()).getId();
    }

    private ResultActions doPostTestReport(String userId) throws Exception {
        return doPostTestReport(userId, null);
    }

    private ResultActions doPostTestReport(String userId, String reportsServerEndpointUrl) throws Exception {
        return doPostTestReport(userId, reportsServerEndpointUrl, false);
    }

    private ResultActions doPostTestReport(String userId, String reportsServerEndpointUrl, boolean useCurrentUserCredentials) throws Exception {
        DashboardReportConfig reportConfig = new DashboardReportConfig();
        reportConfig.setBaseUrl(REPORT_BASE_URL);
        reportConfig.setDashboardId(dashboard.getId().toString());
        reportConfig.setTimezone("UTC");
        reportConfig.setNamePattern("report");
        reportConfig.setType("pdf");
        reportConfig.setUserId(userId);
        reportConfig.setUseCurrentUserCredentials(useCurrentUserCredentials);
        String url = "/api/report/test";
        if (reportsServerEndpointUrl != null) {
            url += "?reportsServerEndpointUrl=" + reportsServerEndpointUrl;
        }
        return doPostAsync(url, reportConfig, TimeUnit.SECONDS.toMillis(30));
    }

    private void verifyReportGenerated(UserId userId) throws Exception {
        verify(dashboardReportService).generateReport(eq(tenantId), argThat(config -> userId.toString().equals(config.getUserId())), any(), any(), any());
    }

}
