// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.permission.AllowedPermissionsInfo;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.permission.GroupPermissionInfo;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.role.RoleType;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class GroupPermissionControllerTest extends AbstractControllerTest {

    private IdComparator<GroupPermission> idComparator;
    private Tenant savedTenant;
    private User tenantAdmin;
    private EntityGroup savedUserGroup;
    private EntityGroup savedDeviceGroup;

    @Before
    public void beforeTest() throws Exception {
        loginSysAdmin();
        idComparator = new IdComparator<>();

        savedTenant = saveTenant(getNewTenant());
        Assert.assertNotNull(savedTenant);

        tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(savedTenant.getId());
        tenantAdmin.setEmail("tenant2@thingsboard.org");
        tenantAdmin.setFirstName("Joe");
        tenantAdmin.setLastName("Downs");
        tenantAdmin = createUserAndLogin(tenantAdmin, "testPassword1");

        savedUserGroup = new EntityGroup();
        savedUserGroup.setType(EntityType.USER);
        savedUserGroup.setName("UserGroup");
        savedUserGroup = doPost("/api/entityGroup", savedUserGroup, EntityGroup.class);

        savedDeviceGroup = new EntityGroup();
        savedDeviceGroup.setType(EntityType.DEVICE);
        savedDeviceGroup.setName("DeviceGroup");
        savedDeviceGroup = doPost("/api/entityGroup", savedDeviceGroup, EntityGroup.class);

    }

    @After
    public void afterTest() throws Exception {
        loginSysAdmin();
        deleteTenant(savedTenant.getId());
    }

    @Test
    public void testFindGroupPermissionById() throws Exception {
        GroupPermission groupPermission = getNewSavedGroupPermission("Test Role", savedUserGroup, null);
        GroupPermission foundGroupPermission = doGet("/api/groupPermission/" + groupPermission.getId().getId().toString(), GroupPermission.class);
        Assert.assertNotNull(foundGroupPermission);
        assertEquals(groupPermission, foundGroupPermission);
    }

    @Test
    public void testSaveGroupPermission() throws Exception {
        GroupPermission groupPermission = getNewSavedGroupPermission("Test Role", savedUserGroup, null);
        Assert.assertNotNull(groupPermission);
        Assert.assertNotNull(groupPermission.getId());
        Assert.assertTrue(groupPermission.getCreatedTime() > 0);
        assertEquals(savedTenant.getId(), groupPermission.getTenantId());
    }

    @Test
    public void testShouldNotSaveTheSameGroupPermissionWithGenericRole() throws Exception {
        Role savedGroupRole = createRole(RandomStringUtils.randomAlphabetic(10), RoleType.GENERIC);
        savedGroupRole = doPost("/api/role", savedGroupRole, Role.class);

        GroupPermission groupPermission = new GroupPermission();
        groupPermission.setRoleId(savedGroupRole.getId());
        groupPermission.setUserGroupId(savedUserGroup.getId());

        groupPermission = doPost("/api/groupPermission", groupPermission, GroupPermission.class);
        doPost("/api/groupPermission", groupPermission)
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testCleanUserGroupCacheAfterGroupPermissionUpdated() throws Exception {
        Customer customer = new Customer();
        customer.setTitle("TEst customer");
        customer = doPost("/api/customer", customer, Customer.class);

        EntityGroup firstUserGroup = new EntityGroup();
        firstUserGroup.setType(EntityType.USER);
        firstUserGroup.setName("UserGroup" + RandomStringUtils.randomAlphabetic(6));
        firstUserGroup.setOwnerId(customer.getId());
        firstUserGroup = doPost("/api/entityGroup", firstUserGroup, EntityGroup.class);

        EntityGroup secondUserGroup = new EntityGroup();
        secondUserGroup.setType(EntityType.USER);
        secondUserGroup.setOwnerId(customer.getId());
        secondUserGroup.setName("UserGroup" + RandomStringUtils.randomAlphabetic(6));
        secondUserGroup = doPost("/api/entityGroup", secondUserGroup, EntityGroup.class);

        User firstGroupUser = new User();
        firstGroupUser.setAuthority(Authority.CUSTOMER_USER);
        firstGroupUser.setTenantId(tenantId);
        firstGroupUser.setCustomerId(customer.getId());
        firstGroupUser.setEmail("customerUser123@thingsboard.org");
        firstGroupUser = createUser(firstGroupUser, "customer", firstUserGroup.getId());

        Role savedGroupRole = createRole(RandomStringUtils.randomAlphabetic(10), RoleType.GROUP);
        savedGroupRole = doPost("/api/role", savedGroupRole, Role.class);

        GroupPermission groupPermission = new GroupPermission();
        groupPermission.setRoleId(savedGroupRole.getId());
        groupPermission.setUserGroupId(firstUserGroup.getId());
        groupPermission.setEntityGroupId(savedDeviceGroup.getId());
        groupPermission.setEntityGroupType(savedDeviceGroup.getType());
        groupPermission = doPost("/api/groupPermission", groupPermission, GroupPermission.class);

        // login as customer user and check permission
        login(firstGroupUser.getEmail(), "customer");
        AllowedPermissionsInfo allowedPermissionsInfo = doGet("/api/permissions/allowedPermissions", AllowedPermissionsInfo.class);
        assertThat(allowedPermissionsInfo.getUserPermissions().getGroupPermissions().containsKey(savedDeviceGroup.getId())).isTrue();

        // update group persmission
        loginUser(tenantAdmin.getEmail(), "testPassword1");
        groupPermission.setUserGroupId(secondUserGroup.getId());
        groupPermission = doPost("/api/groupPermission", groupPermission, GroupPermission.class);

        // login as customer user and check permission
        login(firstGroupUser.getEmail(), "customer");
        AllowedPermissionsInfo allowedPermissionsInfoAfterUpdate = doGet("/api/permissions/allowedPermissions", AllowedPermissionsInfo.class);
        assertThat(allowedPermissionsInfoAfterUpdate.getUserPermissions().getGroupPermissions().containsKey(savedDeviceGroup.getId())).isFalse();
    }

    @Test
    public void testShouldNotSaveTheSameGroupPermissionWithGroupRole() throws Exception {
        Role savedGroupRole = createRole(RandomStringUtils.randomAlphabetic(10));
        savedGroupRole = doPost("/api/role", savedGroupRole, Role.class);

        GroupPermission groupPermission = new GroupPermission();
        groupPermission.setRoleId(savedGroupRole.getId());
        groupPermission.setUserGroupId(savedUserGroup.getId());
        groupPermission.setEntityGroupId(savedDeviceGroup.getId());
        groupPermission.setEntityGroupType(savedUserGroup.getType());

        groupPermission = doPost("/api/groupPermission", groupPermission, GroupPermission.class);
        doPost("/api/groupPermission", groupPermission)
                .andExpect(status().isBadRequest());
    }

    private GroupPermission getNewSavedGroupPermission(String roleName, EntityGroup userGroup, EntityGroup entityGroup) throws Exception {
        Role savedRole = createRole(roleName);
        savedRole = doPost("/api/role", savedRole, Role.class);
        GroupPermission groupPermission = new GroupPermission();
        groupPermission.setRoleId(savedRole.getId());
        groupPermission.setUserGroupId(userGroup.getId());
        if (entityGroup != null) {
            groupPermission.setEntityGroupId(entityGroup.getId());
            groupPermission.setEntityGroupType(entityGroup.getType());
        }

        groupPermission = doPost("/api/groupPermission", groupPermission, GroupPermission.class);
        return groupPermission;
    }

    @Test
    public void testDeleteGroupPermission() throws Exception {
        GroupPermission savedGroupPermission = getNewSavedGroupPermission("Test Role", savedUserGroup, null);

        doDelete("/api/groupPermission/" + savedGroupPermission.getId().getId().toString())
                .andExpect(status().isOk());

        doGet("/api/groupPermission/" + savedGroupPermission.getId().getId().toString())
                .andExpect(status().isNotFound());
    }

    @Test
    public void testGetUserGroupPermissions() throws Exception {
        List<GroupPermission> groupPermissions = new ArrayList<>();
        for (int i = 0; i < 178; i++) {
            groupPermissions.add(getNewSavedGroupPermission("Test role " + i, savedUserGroup, null));
        }

        List<GroupPermissionInfo> loadedGroupPermissionsInfo = doGetTyped("/api/userGroup/" + savedUserGroup.getId() + "/groupPermissions",
                new TypeReference<List<GroupPermissionInfo>>() {});

        List<GroupPermission> loadedGroupPermissions = loadedGroupPermissionsInfo.stream()
                .map(groupPermissionInfo -> new GroupPermission(groupPermissionInfo)).collect(Collectors.toList());

        Collections.sort(groupPermissions, idComparator);
        Collections.sort(loadedGroupPermissions, idComparator);

        assertEquals(groupPermissions, loadedGroupPermissions);
    }

    @Test
    public void testGetEntityGroupPermissions() throws Exception {
        List<GroupPermission> groupPermissions = new ArrayList<>();
        for (int i = 0; i < 128; i++) {
            groupPermissions.add(getNewSavedGroupPermission("Test role " + i, savedUserGroup, savedDeviceGroup));
        }

        List<GroupPermissionInfo> loadedGroupPermissionsInfo = doGetTyped("/api/entityGroup/" + savedDeviceGroup.getId() + "/groupPermissions",
                new TypeReference<List<GroupPermissionInfo>>() {});

        List<GroupPermission> loadedGroupPermissions = loadedGroupPermissionsInfo.stream()
                .map(groupPermissionInfo -> new GroupPermission(groupPermissionInfo)).collect(Collectors.toList());

        Collections.sort(groupPermissions, idComparator);
        Collections.sort(loadedGroupPermissions, idComparator);

        assertEquals(groupPermissions, loadedGroupPermissions);
    }

    @Test
    public void testGetEntityGroupPermissionsForCustomer() throws Exception {
        loginTenantAdmin();
        var customer2 = new Customer();
        customer2.setTitle("Customer 2");
        customer2.setTenantId(tenantId);
        var savedCustomer2 = doPost("/api/customer", customer2, Customer.class);
        var customer2Id = savedCustomer2.getId();

        var entityGroup = new EntityGroup();
        entityGroup.setType(EntityType.DEVICE);
        entityGroup.setName("TestGroup");
        entityGroup = doPost("/api/entityGroup", entityGroup, EntityGroup.class);
        var entityGroupId = entityGroup.getId();

        List<GroupPermissionInfo> loadedGroupPermissionsInfo = doGetTyped("/api/entityGroup/" + entityGroupId + "/groupPermissions",
                new TypeReference<>() {});
        assertEquals(0, loadedGroupPermissionsInfo.size());

        shareGroup(entityGroupId, customerId);
        shareGroup(entityGroupId, subCustomerId);
        shareGroup(entityGroupId, customer2Id);

        loadedGroupPermissionsInfo = doGetTyped("/api/entityGroup/" + entityGroupId + "/groupPermissions",
                new TypeReference<>() {});
        assertEquals(3, loadedGroupPermissionsInfo.size());
        checkPermissionOwners(loadedGroupPermissionsInfo, List.of(customerId, subCustomerId, customer2Id));

        loginCustomerAdminUser();
        loadedGroupPermissionsInfo = doGetTyped("/api/entityGroup/" + entityGroupId + "/groupPermissions",
                new TypeReference<>() {});
        assertEquals(2, loadedGroupPermissionsInfo.size());
        checkPermissionOwners(loadedGroupPermissionsInfo, List.of(customerId, subCustomerId));

        loginSubCustomerAdminUser();
        loadedGroupPermissionsInfo = doGetTyped("/api/entityGroup/" + entityGroupId + "/groupPermissions",
                new TypeReference<>() {});
        assertEquals(1, loadedGroupPermissionsInfo.size());
        checkPermissionOwners(loadedGroupPermissionsInfo, List.of(subCustomerId));
    }

    private void checkPermissionOwners(List<GroupPermissionInfo> groupPermissionsInfos, List<? extends EntityId> ownerIds) {
        for (GroupPermissionInfo groupPermissionInfo : groupPermissionsInfos) {
            assertTrue(ownerIds.contains(groupPermissionInfo.getUserGroupOwnerId()));
        }
    }

    private Role createRole(String roleName) {
        return createRole(roleName, RoleType.GROUP);
    }

    private Role createRole(String roleName, RoleType roleType) {
        Role role = new Role();
        role.setTenantId(savedTenant.getId());
        role.setName(roleName);
        role.setType(roleType);
        return role;
    }

    private Tenant getNewTenant() {
        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        return tenant;
    }
}
