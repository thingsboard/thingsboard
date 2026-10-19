// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.AdditionalAnswers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ContextConfiguration;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ResourceExportData;
import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.TbResourceInfo;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.permission.ShareGroupRequest;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityDataSortOrder;
import org.thingsboard.server.common.data.query.EntityGroupListFilter;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.role.RoleType;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.dashboard.DashboardDao;
import org.thingsboard.server.dao.role.RoleService;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.exception.DataValidationException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ContextConfiguration(classes = {DashboardControllerTest.Config.class})
@DaoSqlTest
public class DashboardControllerTest extends AbstractControllerTest {

    private IdComparator<DashboardInfo> idComparator = new IdComparator<>();

    private Tenant savedTenant;
    private User tenantAdmin;

    @Autowired
    private DashboardDao dashboardDao;
    @Autowired
    private RoleService roleService;

    static class Config {
        @Bean
        @Primary
        public DashboardDao dashboardDao(DashboardDao dashboardDao) {
            return Mockito.mock(DashboardDao.class, AdditionalAnswers.delegatesTo(dashboardDao));
        }

    }

    @Before
    public void beforeTest() throws Exception {
        loginSysAdmin();

        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        savedTenant = saveTenant(tenant);
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

        deleteTenant(savedTenant.getId());
    }

    @Test
    public void testSaveDashboard() throws Exception {
        Dashboard dashboard = new Dashboard();
        dashboard.setTitle("My dashboard");

        Mockito.reset(tbClusterService, auditLogService);

        Dashboard savedDashboard = doPost("/api/dashboard", dashboard, Dashboard.class);

        testNotifyEntityEntityGroupNullAllOneTime(savedDashboard, savedDashboard.getId(), savedDashboard.getId(), savedTenant.getId(),
                tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.ADDED);

        Assert.assertNotNull(savedDashboard);
        Assert.assertNotNull(savedDashboard.getId());
        Assert.assertTrue(savedDashboard.getCreatedTime() > 0);
        Assert.assertEquals(savedTenant.getId(), savedDashboard.getTenantId());
        Assert.assertEquals(dashboard.getTitle(), savedDashboard.getTitle());

        savedDashboard.setTitle("My new dashboard");

        Mockito.reset(tbClusterService, auditLogService);

        doPost("/api/dashboard", savedDashboard, Dashboard.class);

        testNotifyEntityEntityGroupNullAllOneTime(savedDashboard, savedDashboard.getId(), savedDashboard.getId(), savedTenant.getId(),
                tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.UPDATED);

        Dashboard foundDashboard = doGet("/api/dashboard/" + savedDashboard.getId().getId().toString(), Dashboard.class);
        Assert.assertEquals(foundDashboard.getTitle(), savedDashboard.getTitle());
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

        String msgError = "You don't have permission to perform 'WRITE' operation with DASHBOARD 'My dashboard'";
        doPost("/api/dashboard", savedDashboard)
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(dashboard, savedDifferentTenant.getId(), savedDifferentTenantUser.getId(),
                DIFFERENT_TENANT_ADMIN_EMAIL, ActionType.UPDATED, new ThingsboardException(msgError, ThingsboardErrorCode.PERMISSION_DENIED));

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
    public void testFindDashboardInfosByIds() throws Exception {
        List<Dashboard> dashboards = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            Dashboard dashboard = new Dashboard();
            dashboard.setTitle("My dashboard " + i);
            dashboards.add(doPost("/api/dashboard", dashboard, Dashboard.class));
        }

        List<Dashboard> expected = dashboards.subList(5, 15);

        String idsParam = expected.stream()
                .map(d -> d.getId().getId().toString())
                .collect(Collectors.joining(","));

        DashboardInfo[] result = doGet(
                "/api/dashboards?dashboardIds=" + idsParam,
                DashboardInfo[].class
        );

        Assert.assertNotNull(result);
        Assert.assertEquals(expected.size(), result.length);

        Map<UUID, DashboardInfo> infoById = Arrays.stream(result)
                .collect(Collectors.toMap(info -> info.getId().getId(), Function.identity()));

        for (Dashboard dashboard : expected) {
            UUID id = dashboard.getId().getId();
            DashboardInfo info = infoById.get(id);
            Assert.assertNotNull("DashboardInfo not found for id " + id, info);

            Assert.assertEquals(dashboard.getId(), info.getId());
            Assert.assertEquals(dashboard.getTitle(), info.getTitle());
        }
    }

    @Test
    public void testDeleteDashboard() throws Exception {
        Dashboard dashboard = new Dashboard();
        dashboard.setTitle("My dashboard");
        Dashboard savedDashboard = doPost("/api/dashboard", dashboard, Dashboard.class);

        Mockito.reset(tbClusterService, auditLogService);

        doDelete("/api/dashboard/" + savedDashboard.getId().getId().toString())
                .andExpect(status().isOk());

        testNotifyEntityEntityGroupNullAllOneTime(savedDashboard, savedDashboard.getId(), savedDashboard.getId(),
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
        String msgError = "Dashboard title " + msgErrorShouldBeSpecified;

        Mockito.reset(tbClusterService, auditLogService);

        doPost("/api/dashboard", dashboard)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(dashboard, savedTenant.getId(),
                tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.ADDED, new DataValidationException(msgError));
    }

    @Test
    public void testFindTenantDashboards() throws Exception {
        List<DashboardInfo> expectedDashboards = new ArrayList<>();
        PageLink pageLink = new PageLink(24);
        PageData<DashboardInfo> pageData = null;
        do {
            pageData = doGetTypedWithPageLink("/api/tenant/dashboards?",
                    new TypeReference<PageData<DashboardInfo>>() {
                    }, pageLink);
            expectedDashboards.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Mockito.reset(tbClusterService, auditLogService);

        int cntEntity = 173;
        for (int i = 0; i < cntEntity; i++) {
            Dashboard dashboard = new Dashboard();
            dashboard.setTitle("Dashboard" + i);
            expectedDashboards.add(new DashboardInfo(doPost("/api/dashboard", dashboard, Dashboard.class)));
        }

        testNotifyManyEntityManyTimeMsgToEdgeServiceEntityEqAny(new Dashboard(), new Dashboard(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ADDED, cntEntity, cntEntity, cntEntity);

        List<DashboardInfo> loadedDashboards = new ArrayList<>();
        do {
            pageData = doGetTypedWithPageLink("/api/tenant/dashboards?",
                    new TypeReference<>() {
                    }, pageLink);
            loadedDashboards.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        expectedDashboards.sort(idComparator);
        loadedDashboards.sort(idComparator);

        Assert.assertEquals(expectedDashboards, loadedDashboards);
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
        PageData<DashboardInfo> pageData;
        do {
            pageData = doGetTypedWithPageLink("/api/tenant/dashboards?",
                    new TypeReference<>() {
                    }, pageLink);
            loadedDashboardsTitle1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        dashboardsTitle1.sort(idComparator);
        loadedDashboardsTitle1.sort(idComparator);

        Assert.assertEquals(dashboardsTitle1, loadedDashboardsTitle1);

        List<DashboardInfo> loadedDashboardsTitle2 = new ArrayList<>();
        pageLink = new PageLink(4, 0, title2);
        do {
            pageData = doGetTypedWithPageLink("/api/tenant/dashboards?",
                    new TypeReference<>() {
                    }, pageLink);
            loadedDashboardsTitle2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        dashboardsTitle2.sort(idComparator);
        loadedDashboardsTitle2.sort(idComparator);

        Assert.assertEquals(dashboardsTitle2, loadedDashboardsTitle2);

        Mockito.reset(tbClusterService, auditLogService);

        for (DashboardInfo dashboard : loadedDashboardsTitle1) {
            doDelete("/api/dashboard/" + dashboard.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        testNotifyManyEntityManyTimeMsgToEdgeServiceEntityEqAnyAdditionalInfoAny(new Dashboard(), new Dashboard(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.DELETED, ActionType.DELETED, cntEntity, cntEntity, 1);

        pageLink = new PageLink(4, 0, title1);
        pageData = doGetTypedWithPageLink("/api/tenant/dashboards?",
                new TypeReference<>() {
                }, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (DashboardInfo dashboard : loadedDashboardsTitle2) {
            doDelete("/api/dashboard/" + dashboard.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        pageLink = new PageLink(4, 0, title2);
        pageData = doGetTypedWithPageLink("/api/tenant/dashboards?",
                new TypeReference<>() {
                }, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

    @Test
    public void testFindCustomerUserDashboards() throws Exception {
        Customer customer = new Customer();
        customer.setTitle("My customer");
        customer.setTenantId(savedTenant.getTenantId());
        Customer savedCustomer = doPost("/api/customer", customer, Customer.class);

        EntityGroup customerUserGroup = new EntityGroup();
        customerUserGroup.setType(EntityType.USER);
        customerUserGroup.setName("Customer User Group");
        customerUserGroup.setOwnerId(savedCustomer.getOwnerId());

        Mockito.reset(tbClusterService, auditLogService);

        customerUserGroup = doPost("/api/entityGroup", customerUserGroup, EntityGroup.class);

        testNotifyManyEntityManyTimeMsgToEdgeServiceEntityEqAnyWithGroup(customerUserGroup, customerUserGroup,
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ADDED, ActionType.ADDED, 1, 0, 1);

        EntityGroup tenantDashboardGroup = new EntityGroup();
        tenantDashboardGroup.setType(EntityType.DASHBOARD);
        tenantDashboardGroup.setName("Tenant Dashboard Group");
        tenantDashboardGroup = doPost("/api/entityGroup", tenantDashboardGroup, EntityGroup.class);

        Role groupRole = new Role();
        groupRole.setTenantId(savedTenant.getId());
        groupRole.setName("Read Group Role");
        groupRole.setType(RoleType.GROUP);
        ArrayNode readPermissions = JacksonUtil.newArrayNode();
        readPermissions.add("READ");
        groupRole.setPermissions(readPermissions);

        Mockito.reset(tbClusterService, auditLogService);

        groupRole = doPost("/api/role", groupRole, Role.class);

        testNotifyEntityAllOneTimeLogEntityActionEntityEqClass(groupRole, groupRole.getId(), groupRole.getId(), savedTenant.getId(),
                tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.ADDED, ActionType.ADDED);

        GroupPermission readTenantDashboardGroupPermission = new GroupPermission();
        readTenantDashboardGroupPermission.setRoleId(groupRole.getId());
        readTenantDashboardGroupPermission.setUserGroupId(customerUserGroup.getId());
        readTenantDashboardGroupPermission.setEntityGroupId(tenantDashboardGroup.getId());
        readTenantDashboardGroupPermission.setEntityGroupType(tenantDashboardGroup.getType());

        Mockito.reset(tbClusterService, auditLogService);

        GroupPermission savedReadTenantDashboardGroupPermission =
                doPost("/api/groupPermission", readTenantDashboardGroupPermission, GroupPermission.class);

        testNotifyEntityAllOneTimeLogEntityActionEntityEqClass(savedReadTenantDashboardGroupPermission,
                savedReadTenantDashboardGroupPermission.getId(), savedReadTenantDashboardGroupPermission.getId(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.ADDED, ActionType.ADDED);

        Role genericRole = new Role();
        genericRole.setTenantId(savedTenant.getId());
        genericRole.setName("Read Generic Role");
        genericRole.setType(RoleType.GENERIC);
        ObjectNode genericPermissions = JacksonUtil.newObjectNode();
        genericPermissions.set("ALL", readPermissions);
        genericRole.setPermissions(genericPermissions);
        genericRole = doPost("/api/role", genericRole, Role.class);

        GroupPermission genericPermission = new GroupPermission();
        genericPermission.setRoleId(genericRole.getId());
        genericPermission.setUserGroupId(customerUserGroup.getId());
        doPost("/api/groupPermission", genericPermission, GroupPermission.class);

        Dashboard dashboard = new Dashboard();
        dashboard.setTitle("Tenant Dashboard");
        new DashboardInfo(doPost("/api/dashboard", dashboard, Dashboard.class));

        Dashboard sharedDashboard = new Dashboard();
        sharedDashboard.setTitle("Shared Dashboard");
        new DashboardInfo(doPost("/api/dashboard?entityGroupId={entityGroupId}", sharedDashboard, Dashboard.class, tenantDashboardGroup.getId().getId().toString()));

        List<DashboardInfo> tenantAdminDashboards = new ArrayList<>();
        PageLink pageLink = new PageLink(100);
        PageData<DashboardInfo> pageData;
        do {
            pageData = doGetTypedWithPageLink("/api/user/dashboards?",
                    new TypeReference<>() {
                    }, pageLink);
            tenantAdminDashboards.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        // Tenant admin user must have access to both dashboards
        Assert.assertEquals(2, tenantAdminDashboards.size());

        User customerUser = new User();
        customerUser.setAuthority(Authority.CUSTOMER_USER);
        customerUser.setTenantId(savedTenant.getId());
        customerUser.setCustomerId(savedCustomer.getId());
        customerUser.setOwnerId(savedCustomer.getId());
        customerUser.setEmail("customerUser@thingsboard.org");
        User savedUser = doPost("/api/user?entityGroupId={entityGroupId}", customerUser, User.class, customerUserGroup.getId().getId().toString());

        List<DashboardInfo> customerUserDashboards = new ArrayList<>();
        do {
            pageData = doGetTypedWithPageLink("/api/user/dashboards?userId={userId}&",
                    new TypeReference<>() {
                    }, pageLink, savedUser.getId().getId().toString());
            customerUserDashboards.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        // Customer user must have access only to a shared dashboard
        Assert.assertEquals(1, customerUserDashboards.size());
    }

    @Test
    public void testReShareDashboardToChild() throws Exception {
        loginTenantAdmin();
        EntityGroup dashboardGroup = new EntityGroup();
        dashboardGroup.setType(EntityType.DASHBOARD);
        dashboardGroup.setName("Dashboard Group");
        dashboardGroup.setOwnerId(tenantId);

        dashboardGroup = doPost("/api/entityGroup", dashboardGroup, EntityGroup.class);

        Dashboard dashboard = new Dashboard();
        dashboard.setTitle("Tenant Dashboard");
        dashboard = doPost("/api/dashboard?entityGroupId={entityGroupId}", dashboard, Dashboard.class, dashboardGroup.getId().getId().toString());

        //share group for customer
        var shareGroupRequest = new ShareGroupRequest(customerId, true, null, false, Collections.emptyList());
        doPost("/api/entityGroup/{entityGroupId}/share", shareGroupRequest, dashboardGroup.getId().toString());

        loginCustomerAdminUser();
        Dashboard foundDashboard = doGet("/api/dashboard/" + dashboard.getId().getId().toString(), Dashboard.class);
        Assert.assertNotNull(foundDashboard);

        //reshare to child customer
        EntityGroup childAllUserGroup = entityGroupService.findEntityGroupByTypeAndName(tenantId, subCustomerId, EntityType.USER, "All").get();
        Role role = new Role();
        role.setTenantId(tenantId);
        role.setCustomerId(customerId);
        role.setType(RoleType.GROUP);
        role.setName("Role to read dashboard");
        role.setPermissions(JacksonUtil.toJsonNode("[\"READ\"]"));

        Role readRole = roleService.saveRole(tenantId, role);

        doPost("/api/entityGroup/" + dashboardGroup.getId().toString() + "/" + childAllUserGroup.getId().toString() + "/" + readRole.getId().toString() + "/share");

        loginSubCustomerAdminUser();
        Dashboard foundDashboard2 = doGet("/api/dashboard/" + dashboard.getId().getId().toString(), Dashboard.class);
        Assert.assertNotNull(foundDashboard2);
    }

    @Test
    public void testReShareDashboardToChildViaV2Api() throws Exception {
        loginTenantAdmin();
        EntityGroup dashboardGroup = new EntityGroup();
        dashboardGroup.setType(EntityType.DASHBOARD);
        dashboardGroup.setName("Dashboard Group");
        dashboardGroup.setOwnerId(tenantId);

        dashboardGroup = doPost("/api/entityGroup", dashboardGroup, EntityGroup.class);

        Dashboard dashboard = new Dashboard();
        dashboard.setTitle("Tenant Dashboard");
        dashboard = doPost("/api/dashboard?entityGroupId={entityGroupId}", dashboard, Dashboard.class, dashboardGroup.getId().getId().toString());

        //share group for customer
        Role roleToReadAndShare = roleService.findOrCreateRole(tenantId, null, RoleType.GROUP, "Role to read and share dashboard groups",
                JacksonUtil.toJsonNode("[\"READ\", \"RPC_CALL\", \"READ_CREDENTIALS\", \"READ_ATTRIBUTES\",\"READ_TELEMETRY\",\"SHARE_GROUP\"]"), null);
        var shareGroupRequest = new ShareGroupRequest(customerId, true, null, false, List.of(roleToReadAndShare.getId()));
        doPost("/api/entityGroup/{entityGroupId}/share", shareGroupRequest, dashboardGroup.getId().toString()).andExpect(status().isOk());

        loginCustomerAdminUser();
        Dashboard foundDashboard = doGet("/api/dashboard/" + dashboard.getId().getId().toString(), Dashboard.class);
        Assert.assertNotNull(foundDashboard);

        //should not reshare with WRITE permission to child customer
        var reshareWithWritePermission = new ShareGroupRequest(subCustomerId, true, null, false, Collections.emptyList());
        doPost("/api/v2/entityGroup/{entityGroupId}/share", reshareWithWritePermission, dashboardGroup.getId().toString()).andExpect(status().isForbidden());

        //should reshare with READ operation
        var reshareWithReadPermission = new ShareGroupRequest(subCustomerId, true, null, true, Collections.emptyList());
        doPost("/api/v2/entityGroup/{entityGroupId}/share", reshareWithReadPermission, dashboardGroup.getId().toString()).andExpect(status().isOk());

        loginSubCustomerAdminUser();
        Dashboard foundDashboard2 = doGet("/api/dashboard/" + dashboard.getId().getId().toString(), Dashboard.class);
        Assert.assertNotNull(foundDashboard2);
    }

    @Test
    public void testDeleteDashboardWithDeleteRelationsOk() throws Exception {
        DashboardId dashboardId = createDashboard("Dashboard for Test WithRelationsOk").getId();
        testEntityDaoWithRelationsOk(savedTenant.getId(), dashboardId, "/api/dashboard/" + dashboardId);
    }

    @Ignore
    @Test
    public void testDeleteDashboardExceptionWithRelationsTransactional() throws Exception {
        DashboardId dashboardId = createDashboard("Dashboard for Test WithRelations Transactional Exception").getId();
        testEntityDaoWithRelationsTransactionalException(dashboardDao, savedTenant.getId(), dashboardId, "/api/dashboard/" + dashboardId);
    }

    @Test
    public void whenDeletingDashboard_ifReferencedByDeviceProfile_thenReturnError() throws Exception {
        Dashboard dashboard = createDashboard("test");
        DeviceProfile deviceProfile = createDeviceProfile("test");
        deviceProfile.setDefaultDashboardId(dashboard.getId());
        doPost("/api/deviceProfile", deviceProfile, DeviceProfile.class);

        String response = doDelete("/api/dashboard/" + dashboard.getUuidId()).andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();
        String errorMessage = JacksonUtil.toJsonNode(response).get("message").asText();
        assertThat(errorMessage).containsIgnoringCase("referenced by a device profile");
    }

    @Test
    public void whenDeletingDashboard_ifReferencedByAssetProfile_thenReturnError() throws Exception {
        Dashboard dashboard = createDashboard("test");
        AssetProfile assetProfile = createAssetProfile("test");
        assetProfile.setDefaultDashboardId(dashboard.getId());
        doPost("/api/assetProfile", assetProfile, AssetProfile.class);

        String response = doDelete("/api/dashboard/" + dashboard.getUuidId()).andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();
        String errorMessage = JacksonUtil.toJsonNode(response).get("message").asText();
        assertThat(errorMessage).containsIgnoringCase("referenced by an asset profile");
    }

    @Test
    public void testExportImportDashboardWithResources() throws Exception {
        loginTenantAdmin();
        TbResourceInfo imageInfo = uploadImage(HttpMethod.POST, "/api/image", "image12", "image/png", ImageControllerTest.PNG_IMAGE);
        TbResource resource = new TbResource();
        resource.setResourceKey("gateway-management-extension.js");
        resource.setFileName(resource.getResourceKey());
        resource.setTitle(resource.getResourceKey());
        resource.setResourceType(ResourceType.JS_MODULE);
        byte[] resourceData = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};
        resource.setData(resourceData);
        TbResourceInfo resourceInfo = doPost("/api/resource", resource, TbResourceInfo.class);
        assertThat(resourceInfo.getLink()).isEqualTo("/api/resource/js_module/tenant/gateway-management-extension.js");

        Dashboard dashboard = new Dashboard();
        dashboard.setTitle("My dashboard");
        dashboard.setConfiguration(JacksonUtil.newObjectNode()
                .put("someImage", "tb-image;/api/images/tenant/" + imageInfo.getResourceKey())
                .<ObjectNode>set("widgets", JacksonUtil.toJsonNode("""
                        {"xxx":
                        {"config":{"actions":{"elementClick":[
                        {"customResources":[{"url":{"entityType":"TB_RESOURCE","id":
                        "tb-resource;/api/resource/js_module/tenant/gateway-management-extension.js"},"isModule":true},
                        {"url":"tb-resource;/api/resource/js_module/tenant/gateway-management-extension.js","isModule":true}]}]}}}}
                        """))
                .put("someResource", "tb-resource;/api/resource/js_module/tenant/gateway-management-extension.js"));
        dashboard = doPost("/api/dashboard", dashboard, Dashboard.class);

        Dashboard exportedDashboard = doGet("/api/dashboard/" + dashboard.getUuidId() + "?includeResources=true", Dashboard.class);
        exportedDashboard.setId(null);
        String imageRef = exportedDashboard.getConfiguration().get("someImage").asText();
        assertThat(imageRef).isEqualTo("tb-image;/api/images/tenant/image12");
        String resourceRef = exportedDashboard.getConfiguration().get("widgets").get("xxx").get("config")
                .get("actions").get("elementClick").get(0).get("customResources").get(0).get("url").asText();
        assertThat(resourceRef).isEqualTo("tb-resource;/api/resource/js_module/tenant/gateway-management-extension.js");

        Map<ResourceType, List<ResourceExportData>> resources = exportedDashboard.getResources().stream()
                .collect(Collectors.groupingBy(ResourceExportData::getType));
        assertThat(resources.get(ResourceType.IMAGE)).singleElement().satisfies(exportedImage -> {
            assertThat(exportedImage.getFileName()).isEqualTo(imageInfo.getResourceKey());
            assertThat(exportedImage.getData()).isEqualTo(Base64.getEncoder().encodeToString(ImageControllerTest.PNG_IMAGE));
        });
        assertThat(resources.get(ResourceType.JS_MODULE)).singleElement().satisfies(exportedJsModule -> {
            assertThat(exportedJsModule.getFileName()).isEqualTo(resourceInfo.getResourceKey());
            assertThat(exportedJsModule.getData()).isEqualTo(Base64.getEncoder().encodeToString(resourceData));
        });

        doDelete("/api/dashboard/" + dashboard.getId()).andExpect(status().isOk());
        doDelete("/api/images/tenant/" + imageInfo.getResourceKey()).andExpect(status().isOk());
        resource = new TbResource(resourceInfo);
        resource.setData(new byte[]{1, 2, 3}); // updating resource data to check that a new resource will be created
        doPost("/api/resource", resource, TbResourceInfo.class);

        Dashboard importedDashboard = doPost("/api/dashboard", exportedDashboard, Dashboard.class);
        String newResourceKey = "gateway-management-extension_(1).js";

        imageRef = importedDashboard.getConfiguration().get("someImage").asText();
        assertThat(imageRef).isEqualTo("tb-image;/api/images/tenant/" + imageInfo.getResourceKey());

        List<String> resourcesRefs = new ArrayList<>();
        resourcesRefs.add(importedDashboard.getConfiguration().get("widgets").get("xxx").get("config")
                .get("actions").get("elementClick").get(0).get("customResources").get(0).get("url").asText());
        resourcesRefs.add(importedDashboard.getConfiguration().get("someResource").asText());
        assertThat(resourcesRefs).allSatisfy(ref -> {
            assertThat(ref).isEqualTo("tb-resource;/api/resource/js_module/tenant/" + newResourceKey);
        });

        TbResourceInfo importedImageInfo = doGet("/api/images/tenant/" + imageInfo.getResourceKey() + "/info", TbResourceInfo.class);
        assertThat(importedImageInfo.getEtag()).isEqualTo(imageInfo.getEtag());
        assertThat(importedImageInfo.getResourceKey()).isEqualTo(imageInfo.getResourceKey());

        TbResourceInfo importedResourceInfo = doGet("/api/resource/js_module/tenant/" + newResourceKey + "/info", TbResourceInfo.class);
        assertThat(importedResourceInfo.getEtag()).isEqualTo(resourceInfo.getEtag());

        loginCustomerAdminUser();
        exportedDashboard.setCustomerId(customerId);
        importedDashboard = doPost("/api/dashboard", exportedDashboard, Dashboard.class);
        imageRef = importedDashboard.getConfiguration().get("someImage").asText();
        assertThat(imageRef).isEqualTo("tb-image;/api/images/tenant/image12_(1)"); // new image is created
        resourceRef = importedDashboard.getConfiguration().get("widgets").get("xxx").get("config")
                .get("actions").get("elementClick").get(0).get("customResources").get(0).get("url").asText();
        assertThat(resourceRef).isEqualTo("tb-resource;/api/resource/js_module/tenant/" + newResourceKey); // left unchanged, using existing tenant resource
    }

    private Dashboard createDashboard(String title) {
        Dashboard dashboard = new Dashboard();
        dashboard.setTitle(title);
        return doPost("/api/dashboard", dashboard, Dashboard.class);
    }

}
