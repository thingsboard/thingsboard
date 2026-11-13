// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.client;

import org.junit.Test;
import org.thingsboard.client.ApiException;
import org.thingsboard.client.api.ThingsboardApi.DeleteEntityGroupArgs;
import org.thingsboard.client.api.ThingsboardApi.DeleteGroupPermissionArgs;
import org.thingsboard.client.api.ThingsboardApi.DeleteRoleArgs;
import org.thingsboard.client.api.ThingsboardApi.GetEntityGroupEntityInfosByOwnerAndTypeAndPageLinkArgs;
import org.thingsboard.client.api.ThingsboardApi.GetEntityGroupPermissionsArgs;
import org.thingsboard.client.api.ThingsboardApi.GetGroupPermissionByIdArgs;
import org.thingsboard.client.api.ThingsboardApi.GetGroupPermissionInfoByIdArgs;
import org.thingsboard.client.api.ThingsboardApi.GetUserGroupPermissionsArgs;
import org.thingsboard.client.api.ThingsboardApi.LoadUserGroupPermissionInfosArgs;
import org.thingsboard.client.api.ThingsboardApi.SaveEntityGroupArgs;
import org.thingsboard.client.api.ThingsboardApi.SaveGroupPermissionArgs;
import org.thingsboard.client.api.ThingsboardApi.SaveRoleArgs;
import org.thingsboard.client.model.EntityGroup;
import org.thingsboard.client.model.EntityGroupId;
import org.thingsboard.client.model.EntityGroupInfo;
import org.thingsboard.client.model.EntityInfo;
import org.thingsboard.client.model.GroupPermission;
import org.thingsboard.client.model.GroupPermissionInfo;
import org.thingsboard.client.model.Role;
import org.thingsboard.client.model.RoleId;
import org.thingsboard.client.model.RoleType;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@DaoSqlTest
public class GroupPermissionApiClientTest extends AbstractApiClientTest {

    @Test
    public void testGroupPermissionLifecycle() throws Exception {
        long ts = System.currentTimeMillis();

        Role role = createRole(TEST_PREFIX + ts + "_role");
        String roleId = role.getId().getId().toString();

        EntityGroupInfo deviceGroup = createDeviceGroup(TEST_PREFIX + ts + "_devices");
        String deviceGroupId = deviceGroup.getId().getId().toString();

        String userGroupId = customerAdminGroupId();

        GroupPermission saved = client.saveGroupPermission(SaveGroupPermissionArgs.builder()
                .groupPermission(buildPermission(userGroupId, roleId, deviceGroupId))
                .build());
        assertNotNull(saved);
        assertNotNull(saved.getId());
        String permissionId = saved.getId().getId().toString();

        GroupPermission fetched = client.getGroupPermissionById(GetGroupPermissionByIdArgs.builder()
                .groupPermissionId(permissionId)
                .build());
        assertNotNull(fetched);
        assertEquals(permissionId, fetched.getId().getId().toString());
        assertEquals(userGroupId, fetched.getUserGroupId().getId().toString());
        assertEquals(roleId, fetched.getRoleId().getId().toString());
        assertEquals(deviceGroupId, fetched.getEntityGroupId().getId().toString());

        GroupPermissionInfo infoEntity = client.getGroupPermissionInfoById(GetGroupPermissionInfoByIdArgs.builder()
                .groupPermissionId(permissionId)
                .isUserGroup(false)
                .build());
        assertNotNull(infoEntity);
        assertEquals(permissionId, infoEntity.getId().getId().toString());
        assertNotNull(infoEntity.getRole());
        assertNotNull(infoEntity.getUserGroupName());

        GroupPermissionInfo infoUser = client.getGroupPermissionInfoById(GetGroupPermissionInfoByIdArgs.builder()
                .groupPermissionId(permissionId)
                .isUserGroup(true)
                .build());
        assertNotNull(infoUser);
        assertEquals(permissionId, infoUser.getId().getId().toString());
        assertNotNull(infoUser.getEntityGroupName());

        client.deleteGroupPermission(DeleteGroupPermissionArgs.builder()
                .groupPermissionId(permissionId)
                .build());
        assertReturns404(() -> client.getGroupPermissionById(GetGroupPermissionByIdArgs.builder()
                .groupPermissionId(permissionId)
                .build()));

        client.deleteEntityGroup(DeleteEntityGroupArgs.builder()
                .entityGroupId(deviceGroupId)
                .build());
        client.deleteRole(DeleteRoleArgs.builder()
                .roleId(roleId)
                .build());
    }

    @Test
    public void testGetPermissionsByGroupIds() throws Exception {
        long ts = System.currentTimeMillis();

        Role role = createRole(TEST_PREFIX + ts + "_role");
        String roleId = role.getId().getId().toString();

        EntityGroupInfo deviceGroup = createDeviceGroup(TEST_PREFIX + ts + "_devices");
        String deviceGroupId = deviceGroup.getId().getId().toString();

        String userGroupId = customerAdminGroupId();

        GroupPermission saved = client.saveGroupPermission(SaveGroupPermissionArgs.builder()
                .groupPermission(buildPermission(userGroupId, roleId, deviceGroupId))
                .build());
        String permissionId = saved.getId().getId().toString();

        List<GroupPermissionInfo> userGroupPermissions = client.getUserGroupPermissions(GetUserGroupPermissionsArgs.builder()
                .userGroupId(userGroupId)
                .build());
        assertNotNull(userGroupPermissions);
        assertTrue(userGroupPermissions.stream()
                .anyMatch(p -> p.getId().getId().toString().equals(permissionId)));

        List<GroupPermissionInfo> entityGroupPermissions = client.getEntityGroupPermissions(GetEntityGroupPermissionsArgs.builder()
                .entityGroupId(deviceGroupId)
                .build());
        assertNotNull(entityGroupPermissions);
        assertTrue(entityGroupPermissions.stream()
                .anyMatch(p -> p.getId().getId().toString().equals(permissionId)));

        List<GroupPermissionInfo> loaded = client.loadUserGroupPermissionInfos(LoadUserGroupPermissionInfosArgs.builder()
                .groupPermission(List.of(saved))
                .build());
        assertNotNull(loaded);
        assertFalse(loaded.isEmpty());
        assertTrue(loaded.stream()
                .anyMatch(p -> p.getId().getId().toString().equals(permissionId)));

        client.deleteGroupPermission(DeleteGroupPermissionArgs.builder()
                .groupPermissionId(permissionId)
                .build());
        client.deleteEntityGroup(DeleteEntityGroupArgs.builder()
                .entityGroupId(deviceGroupId)
                .build());
        client.deleteRole(DeleteRoleArgs.builder()
                .roleId(roleId)
                .build());
    }

    @Test
    public void testGetGroupPermissionByIdNotFound() {
        assertReturns404(() -> client.getGroupPermissionById(GetGroupPermissionByIdArgs.builder()
                .groupPermissionId(UUID.randomUUID().toString())
                .build()));
    }

    private Role createRole(String name) throws ApiException {
        Role role = new Role();
        role.setName(name);
        role.setType(RoleType.GROUP);
        return client.saveRole(SaveRoleArgs.builder()
                .role(role)
                .build());
    }

    private EntityGroupInfo createDeviceGroup(String name) throws ApiException {
        EntityGroup group = new EntityGroup();
        group.setName(name);
        group.setType(EntityGroup.TypeEnum.DEVICE);
        return client.saveEntityGroup(SaveEntityGroupArgs.builder()
                .entityGroup(group)
                .build());
    }

    private GroupPermission buildPermission(String userGroupId, String roleId, String entityGroupId) {
        GroupPermission permission = new GroupPermission();
        permission.setUserGroupId(new EntityGroupId().id(UUID.fromString(userGroupId)));
        permission.setRoleId(new RoleId().id(UUID.fromString(roleId)));
        permission.setEntityGroupId(new EntityGroupId().id(UUID.fromString(entityGroupId)));
        return permission;
    }

    private String customerAdminGroupId() throws ApiException {
        EntityInfo info = client.getEntityGroupEntityInfosByOwnerAndTypeAndPageLink(GetEntityGroupEntityInfosByOwnerAndTypeAndPageLinkArgs.builder()
                .ownerType("CUSTOMER")
                .ownerId(savedClientCustomer.getId().getId().toString())
                .groupType("USER")
                .pageSize("1")
                .page("0")
                .textSearch("Customer Administrators")
                .build()).getData().get(0);
        return info.getId().getId().toString();
    }

}
