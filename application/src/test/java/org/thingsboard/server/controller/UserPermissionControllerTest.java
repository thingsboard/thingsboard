// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.permission.AllowedPermissionsInfo;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.role.RoleType;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class UserPermissionControllerTest extends AbstractControllerTest {

    @Before
    public void setUp() throws Exception {
        loginTenantAdmin();
    }

    @Test
    public void testDashboardPermission() throws Exception {
        loginCustomerAdminUser();

        EntityGroup dashboardGroup = new EntityGroup();
        dashboardGroup.setName("Entity Group");
        dashboardGroup.setType(EntityType.DASHBOARD);
        dashboardGroup.setOwnerId(customerId);
        dashboardGroup = doPost("/api/entityGroup", dashboardGroup, EntityGroup.class);

        Dashboard dashboard = new Dashboard();
        dashboard.setTitle("Customer dashboard");
        dashboard.setCustomerId(customerId);
        Dashboard savedDashboard = doPost("/api/dashboard", dashboard, Dashboard.class);
        doPost("/api/entityGroup/" + dashboardGroup.getId() + "/addEntities", List.of(savedDashboard.getId().getId().toString()));

        EntityGroup userGroup = new EntityGroup();
        userGroup.setType(EntityType.USER);
        userGroup.setOwnerId(customerId);
        userGroup.setName("UserGroup");
        userGroup = doPost("/api/entityGroup", userGroup, EntityGroup.class);

        User readUser = new User();
        readUser.setAuthority(Authority.CUSTOMER_USER);
        readUser.setTenantId(tenantId);
        readUser.setCustomerId(customerId);
        readUser.setEmail("customerUser123@thingsboard.org");
        createUser(readUser, "customer", userGroup.getId());

        User noPermissionUser = new User();
        noPermissionUser.setAuthority(Authority.CUSTOMER_USER);
        noPermissionUser.setTenantId(tenantId);
        noPermissionUser.setCustomerId(customerId);
        noPermissionUser.setEmail("noPermissionUser123@thingsboard.org");
        createUser(noPermissionUser, "customer");

        Role groupRole = createReadRole("Read dashboard", RoleType.GROUP);
        groupRole = doPost("/api/role", groupRole, Role.class);

        GroupPermission groupPermission = new GroupPermission();
        groupPermission.setRoleId(groupRole.getId());
        groupPermission.setUserGroupId(userGroup.getId());
        groupPermission.setEntityGroupId(dashboardGroup.getId());
        groupPermission.setEntityGroupType(dashboardGroup.getType());

        doPost("/api/groupPermission", groupPermission, GroupPermission.class);

        loginUser("customerUser123@thingsboard.org", "customer");

        assertThat(doGet("/api/permission/DASHBOARD/" + savedDashboard.getId() + "/READ", Boolean.class)).isTrue();
        assertThat(doGet("/api/permission/DASHBOARD/" + savedDashboard.getId() + "/ADD_TO_GROUP", Boolean.class)).isFalse();

        // check user without permission has no access
        loginUser("noPermissionUser123@thingsboard.org", "customer");
        assertThat(doGet("/api/permission/DASHBOARD/" + savedDashboard.getId() + "/READ", Boolean.class)).isFalse();

    }

    @Test
    public void testPublicDashboardPermission() throws Exception {
        loginCustomerAdminUser();

        EntityGroup dashboardGroup = new EntityGroup();
        dashboardGroup.setName("Entity Group");
        dashboardGroup.setType(EntityType.DASHBOARD);
        dashboardGroup.setOwnerId(customerId);
        dashboardGroup = doPost("/api/entityGroup", dashboardGroup, EntityGroup.class);

        Dashboard dashboard = new Dashboard();
        dashboard.setTitle("Customer dashboard");
        dashboard.setCustomerId(customerId);
        Dashboard savedDashboard = doPost("/api/dashboard", dashboard, Dashboard.class);
        doPost("/api/entityGroup/" + dashboardGroup.getId() + "/addEntities", List.of(savedDashboard.getId().getId().toString()));

        doPost("/api/entityGroup/" + dashboardGroup.getUuidId() + "/makePublic").andExpect(status().isOk());
        EntityGroup publicEntityGroup = doGet("/api/entityGroup/" + dashboardGroup.getUuidId(), EntityGroup.class);
        String publicCustomerId = publicEntityGroup.getAdditionalInfo().get("publicCustomerId").asText();

        //retrieve public dashboard
        resetTokens();

        JsonNode publicLoginRequest = JacksonUtil.toJsonNode("{\"publicId\": \"" + publicCustomerId + "\"}");
        JsonNode tokens = doPost("/api/auth/login/public", publicLoginRequest, JsonNode.class);
        this.token = tokens.get("token").asText();

        doGet("/dashboard/" + savedDashboard.getId() + "?publicId=" + publicCustomerId).andExpect(status().isOk());
        assertThat(doGet("/api/permission/DASHBOARD/" + savedDashboard.getId() + "/READ", Boolean.class)).isTrue();

        // login non-public user
        loginCustomerAdminUser();
        User noPermissionUser = new User();
        noPermissionUser.setAuthority(Authority.CUSTOMER_USER);
        noPermissionUser.setTenantId(tenantId);
        noPermissionUser.setCustomerId(customerId);
        noPermissionUser.setEmail("noPermissionUser123@thingsboard.org");
        createUser(noPermissionUser, "customer");
        login("noPermissionUser123@thingsboard.org", "customer");

        assertThat(doGet("/api/permission/DASHBOARD/" + savedDashboard.getId() + "/READ", Boolean.class)).isFalse();
    }

    @Test
    public void testExcludedPermissionDeniesAccess() throws Exception {
        loginCustomerAdminUser();

        EntityGroup deviceGroup = new EntityGroup();
        deviceGroup.setName("Device Group");
        deviceGroup.setType(EntityType.DEVICE);
        deviceGroup.setOwnerId(customerId);
        deviceGroup = doPost("/api/entityGroup", deviceGroup, EntityGroup.class);

        Device device = new Device();
        device.setName("Test Device");
        device.setType("default");
        device.setCustomerId(customerId);
        Device savedDevice = doPost("/api/device", device, Device.class);
        doPost("/api/entityGroup/" + deviceGroup.getId() + "/addEntities", List.of(savedDevice.getId().getId().toString()));

        EntityGroup userGroup = new EntityGroup();
        userGroup.setType(EntityType.USER);
        userGroup.setOwnerId(customerId);
        userGroup.setName("ExcludedPermUserGroup");
        userGroup = doPost("/api/entityGroup", userGroup, EntityGroup.class);

        User testCustomerUser = new User();
        testCustomerUser.setAuthority(Authority.CUSTOMER_USER);
        testCustomerUser.setTenantId(tenantId);
        testCustomerUser.setCustomerId(customerId);
        testCustomerUser.setEmail("excludedPermUser@thingsboard.org");
        createUser(testCustomerUser, "customer", userGroup.getId());

        // Generic role: ALL permissions with DEVICE DELETE excluded
        Role genericRole = new Role();
        genericRole.setTenantId(tenantId);
        genericRole.setCustomerId(customerId);
        genericRole.setName("All minus device delete");
        genericRole.setType(RoleType.GENERIC);
        genericRole.setPermissions(JacksonUtil.toJsonNode("{\"ALL\": [\"ALL\"]}"));
        genericRole.setExcludedPermissions(JacksonUtil.toJsonNode("{\"DEVICE\": [\"DELETE\"]}"));
        genericRole = doPost("/api/role", genericRole, Role.class);

        // Group role: ALL operations on device group
        Role groupRole = new Role();
        groupRole.setTenantId(tenantId);
        groupRole.setCustomerId(customerId);
        groupRole.setName("Read device group access");
        groupRole.setType(RoleType.GROUP);
        groupRole.setPermissions(JacksonUtil.toJsonNode("[\"READ\", \"READ_ATTRIBUTES\", \"READ_TELEMETRY\"]"));
        groupRole = doPost("/api/role", groupRole, Role.class);

        GroupPermission genericGp = new GroupPermission();
        genericGp.setRoleId(genericRole.getId());
        genericGp.setUserGroupId(userGroup.getId());
        doPost("/api/groupPermission", genericGp, GroupPermission.class);

        GroupPermission groupGp = new GroupPermission();
        groupGp.setRoleId(groupRole.getId());
        groupGp.setUserGroupId(userGroup.getId());
        groupGp.setEntityGroupId(deviceGroup.getId());
        groupGp.setEntityGroupType(EntityType.DEVICE);
        doPost("/api/groupPermission", groupGp, GroupPermission.class);

        loginUser("excludedPermUser@thingsboard.org", "customer");

        // READ should be allowed, DELETE should be denied
        assertThat(doGet("/api/permission/DEVICE/" + savedDevice.getId() + "/READ", Boolean.class)).isTrue();
        assertThat(doGet("/api/permission/DEVICE/" + savedDevice.getId() + "/DELETE", Boolean.class)).isFalse();
    }

    @Test
    public void testAllowedPermissionsReflectsExclusions() throws Exception {
        loginCustomerAdminUser();

        EntityGroup userGroup = new EntityGroup();
        userGroup.setType(EntityType.USER);
        userGroup.setOwnerId(customerId);
        userGroup.setName("ExcludedPermUserGroup2");
        userGroup = doPost("/api/entityGroup", userGroup, EntityGroup.class);

        User testCustomerUser = new User();
        testCustomerUser.setAuthority(Authority.CUSTOMER_USER);
        testCustomerUser.setTenantId(tenantId);
        testCustomerUser.setCustomerId(customerId);
        testCustomerUser.setEmail("excludedPermUser2@thingsboard.org");
        createUser(testCustomerUser, "customer", userGroup.getId());

        Role genericRole = new Role();
        genericRole.setTenantId(tenantId);
        genericRole.setCustomerId(customerId);
        genericRole.setName("All minus delete");
        genericRole.setType(RoleType.GENERIC);
        genericRole.setPermissions(JacksonUtil.toJsonNode("{\"ALL\": [\"ALL\"]}"));
        genericRole.setExcludedPermissions(JacksonUtil.toJsonNode("{\"ALL\": [\"DELETE\"]}"));
        genericRole = doPost("/api/role", genericRole, Role.class);

        GroupPermission gp = new GroupPermission();
        gp.setRoleId(genericRole.getId());
        gp.setUserGroupId(userGroup.getId());
        doPost("/api/groupPermission", gp, GroupPermission.class);

        loginUser("excludedPermUser2@thingsboard.org", "customer");

        AllowedPermissionsInfo info = doGet("/api/permissions/allowedPermissions", AllowedPermissionsInfo.class);
        assertThat(info.getUserPermissions().getGenericPermissions()).doesNotContainKey(Resource.ALL);

        Set<Operation> deviceOps = info.getUserPermissions().getGenericPermissions().get(Resource.DEVICE);
        assertThat(deviceOps).isNotNull();
        assertThat(deviceOps).doesNotContain(Operation.DELETE);
        assertThat(deviceOps).contains(Operation.READ);
    }

    @Test
    public void testMultiRoleUnionOverridesExclusion() throws Exception {
        loginCustomerAdminUser();

        EntityGroup deviceGroup = new EntityGroup();
        deviceGroup.setName("Device Group Union");
        deviceGroup.setType(EntityType.DEVICE);
        deviceGroup.setOwnerId(customerId);
        deviceGroup = doPost("/api/entityGroup", deviceGroup, EntityGroup.class);

        Device device = new Device();
        device.setName("Test Device Union");
        device.setType("default");
        device.setCustomerId(customerId);
        Device savedDevice = doPost("/api/device", device, Device.class);
        doPost("/api/entityGroup/" + deviceGroup.getId() + "/addEntities", List.of(savedDevice.getId().getId().toString()));

        EntityGroup userGroup = new EntityGroup();
        userGroup.setType(EntityType.USER);
        userGroup.setOwnerId(customerId);
        userGroup.setName("UnionPermUserGroup");
        userGroup = doPost("/api/entityGroup", userGroup, EntityGroup.class);

        User testCustomerUser = new User();
        testCustomerUser.setAuthority(Authority.CUSTOMER_USER);
        testCustomerUser.setTenantId(tenantId);
        testCustomerUser.setCustomerId(customerId);
        testCustomerUser.setEmail("unionPermUser@thingsboard.org");
        createUser(testCustomerUser, "customer", userGroup.getId());

        // Role 1: ALL permissions with DEVICE DELETE excluded
        Role role1 = new Role();
        role1.setTenantId(tenantId);
        role1.setCustomerId(customerId);
        role1.setName("All minus device delete union");
        role1.setType(RoleType.GENERIC);
        role1.setPermissions(JacksonUtil.toJsonNode("{\"ALL\": [\"ALL\"]}"));
        role1.setExcludedPermissions(JacksonUtil.toJsonNode("{\"DEVICE\": [\"DELETE\"]}"));
        role1 = doPost("/api/role", role1, Role.class);

        // Role 2: DEVICE DELETE explicitly granted
        Role role2 = new Role();
        role2.setTenantId(tenantId);
        role2.setCustomerId(customerId);
        role2.setName("Device delete only");
        role2.setType(RoleType.GENERIC);
        role2.setPermissions(JacksonUtil.toJsonNode("{\"DEVICE\": [\"DELETE\"]}"));
        role2 = doPost("/api/role", role2, Role.class);

        // Group role for entity group access
        Role groupRole = new Role();
        groupRole.setTenantId(tenantId);
        groupRole.setCustomerId(customerId);
        groupRole.setName("Full device group union");
        groupRole.setType(RoleType.GROUP);
        groupRole.setPermissions(JacksonUtil.toJsonNode("[\"ALL\"]"));
        groupRole = doPost("/api/role", groupRole, Role.class);

        GroupPermission gp1 = new GroupPermission();
        gp1.setRoleId(role1.getId());
        gp1.setUserGroupId(userGroup.getId());
        doPost("/api/groupPermission", gp1, GroupPermission.class);

        GroupPermission gp2 = new GroupPermission();
        gp2.setRoleId(role2.getId());
        gp2.setUserGroupId(userGroup.getId());
        doPost("/api/groupPermission", gp2, GroupPermission.class);

        GroupPermission groupGp = new GroupPermission();
        groupGp.setRoleId(groupRole.getId());
        groupGp.setUserGroupId(userGroup.getId());
        groupGp.setEntityGroupId(deviceGroup.getId());
        groupGp.setEntityGroupType(EntityType.DEVICE);
        doPost("/api/groupPermission", groupGp, GroupPermission.class);

        loginUser("unionPermUser@thingsboard.org", "customer");

        // Union should give back DELETE on DEVICE
        assertThat(doGet("/api/permission/DEVICE/" + savedDevice.getId() + "/DELETE", Boolean.class)).isTrue();
        assertThat(doGet("/api/permission/DEVICE/" + savedDevice.getId() + "/READ", Boolean.class)).isTrue();
    }

    @Test
    public void testAllWildcardWithExclusionPreservesAiPermissions() throws Exception {
        Set<Operation> aiOps = getGenericPermissionsForRestrictedTenantAdmin(
                "aiWildcardUser@thingsboard.org", "AiWildcardUserGroup",
                "All minus device delete - AI preserved",
                "{\"ALL\": [\"ALL\"]}",
                "{\"DEVICE\": [\"DELETE\"]}")
                .get(Resource.AI);
        assertThat(aiOps)
                .as("AI is an all-or-nothing resource; the ALL wildcard must survive an unrelated exclusion")
                .isNotNull()
                .containsExactly(Operation.ALL);
    }

    @Test
    public void testAllWildcardWithExclusionPreservesCustomMenuPermissions() throws Exception {
        Set<Operation> customMenuOps = getGenericPermissionsForRestrictedTenantAdmin(
                "customMenuWildcardUser@thingsboard.org", "CustomMenuWildcardUserGroup",
                "All minus device delete - CUSTOM_MENU preserved",
                "{\"ALL\": [\"ALL\"]}",
                "{\"DEVICE\": [\"DELETE\"]}")
                .get(Resource.CUSTOM_MENU);
        assertThat(customMenuOps)
                .as("CUSTOM_MENU permissions should survive an exclusion unrelated to it")
                .isNotNull()
                .contains(Operation.READ, Operation.WRITE, Operation.DELETE);
    }

    @Test
    public void testExplicitCustomMenuDenialRevokesAccessGrantedByWildcard() throws Exception {
        Map<Resource, Set<Operation>> genericPermissions = getGenericPermissionsForRestrictedTenantAdmin(
                "customMenuDeniedUser@thingsboard.org", "CustomMenuDeniedUserGroup", "All minus CUSTOM_MENU",
                "{\"ALL\": [\"ALL\"]}",
                "{\"CUSTOM_MENU\": [\"ALL\"]}");
        assertThat(genericPermissions)
                .as("Explicit CUSTOM_MENU:[ALL] exclusion must revoke the CUSTOM_MENU access granted by {ALL:[ALL]}")
                .doesNotContainKey(Resource.CUSTOM_MENU);
    }

    @Test
    public void testExplicitDenialOverridesExplicitGrantOnSameResource() throws Exception {
        Map<Resource, Set<Operation>> genericPermissions = getGenericPermissionsForRestrictedTenantAdmin(
                "customMenuGrantDenyUser@thingsboard.org", "CustomMenuGrantDenyUserGroup",
                "CUSTOM_MENU granted and denied",
                "{\"CUSTOM_MENU\": [\"ALL\"]}",
                "{\"CUSTOM_MENU\": [\"ALL\"]}");
        assertThat(genericPermissions)
                .as("Explicit CUSTOM_MENU:[ALL] exclusion must override an explicit CUSTOM_MENU:[ALL] grant on the same role")
                .doesNotContainKey(Resource.CUSTOM_MENU);
    }

    private Map<Resource, Set<Operation>> getGenericPermissionsForRestrictedTenantAdmin(
            String email, String userGroupName, String roleName,
            String permissionsJson, String excludedPermissionsJson) throws Exception {
        EntityGroup userGroup = new EntityGroup();
        userGroup.setType(EntityType.USER);
        userGroup.setOwnerId(tenantId);
        userGroup.setName(userGroupName);
        userGroup = doPost("/api/entityGroup", userGroup, EntityGroup.class);

        User user = new User();
        user.setAuthority(Authority.TENANT_ADMIN);
        user.setTenantId(tenantId);
        user.setEmail(email);
        createUser(user, "password", userGroup.getId());

        Role genericRole = new Role();
        genericRole.setTenantId(tenantId);
        genericRole.setName(roleName);
        genericRole.setType(RoleType.GENERIC);
        genericRole.setPermissions(JacksonUtil.toJsonNode(permissionsJson));
        genericRole.setExcludedPermissions(JacksonUtil.toJsonNode(excludedPermissionsJson));
        genericRole = doPost("/api/role", genericRole, Role.class);

        GroupPermission gp = new GroupPermission();
        gp.setRoleId(genericRole.getId());
        gp.setUserGroupId(userGroup.getId());
        doPost("/api/groupPermission", gp, GroupPermission.class);

        loginUser(email, "password");

        return doGet("/api/permissions/allowedPermissions", AllowedPermissionsInfo.class)
                .getUserPermissions()
                .getGenericPermissions();
    }

    private Role createReadRole(String roleName, RoleType roleType) {
        Role role = new Role();
        role.setTenantId(tenantId);
        role.setCustomerId(customerId);
        role.setName(roleName);
        role.setType(roleType);
        role.setPermissions(JacksonUtil.toJsonNode("[\"READ\", \"READ_ATTRIBUTES\", \"READ_TELEMETRY\", \"READ_CREDENTIALS\", \"READ_CALCULATED_FIELD\"]"));
        return role;
    }

}
