// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.permission;

import com.google.common.util.concurrent.Futures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.cache.SimpleTbCacheValueWrapper;
import org.thingsboard.server.cache.TbTransactionalCache;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.permission.MergedUserPermissions;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.role.RoleType;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.grouppermission.GroupPermissionService;
import org.thingsboard.server.dao.role.RoleService;
import org.thingsboard.server.dao.user.UserPermissionCacheKey;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;
import org.thingsboard.server.service.security.permission.DefaultUserPermissionsService;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
public class UserPermissionServiceTest {

    @Mock
    private TbTransactionalCache<UserPermissionCacheKey, MergedUserPermissions> cache;
    @Mock
    private EntityGroupService entityGroupService;
    @Mock
    private GroupPermissionService groupPermissionService;
    @Mock
    private RoleService roleService;
    @Mock
    private DbCallbackExecutorService dbCallbackExecutorService;

    @InjectMocks
    private DefaultUserPermissionsService userPermissionsService;

    private User testUser;
    private TenantId tenantId;

    @BeforeEach
    void setUp() {
        tenantId = new TenantId(UUID.randomUUID());
        CustomerId customerId = new CustomerId(UUID.randomUUID());
        UserId userId = new UserId(UUID.randomUUID());

        testUser = new User();
        testUser.setId(userId);
        testUser.setTenantId(tenantId);
        testUser.setCustomerId(customerId);
        testUser.setAuthority(Authority.TENANT_ADMIN);
    }

    @Test
    void testGetMergedPermissions_sysAdmin() throws Exception {
        testUser.setAuthority(Authority.SYS_ADMIN);
        MergedUserPermissions permissions = userPermissionsService.getMergedPermissions(testUser, false);
        assertNotNull(permissions);
        assertEquals(getSysAdminPermissions(), permissions);
    }

    @Test
    void testGetMergedPermissions_tenantAdmin() throws ThingsboardException {
        MergedUserPermissions cachedPermissions = mock(MergedUserPermissions.class);
        SimpleTbCacheValueWrapper<MergedUserPermissions> cacheValueWrapper = SimpleTbCacheValueWrapper.wrap(cachedPermissions);
        when(cache.get(any())).thenReturn(cacheValueWrapper);

        MergedUserPermissions permissions = userPermissionsService.getMergedPermissions(testUser, false);
        assertEquals(cachedPermissions, permissions);
        verify(cache).get(any());
    }

    @ParameterizedTest
    @CsvSource({"DEVICE", "ASSET"})
    void testOnRoleUpdated(EntityType entityType) throws ThingsboardException {
        Role role = new Role();
        role.setTenantId(tenantId);
        role.setId(new RoleId(UUID.randomUUID()));
        role.setPermissions(JacksonUtil.newObjectNode());

        List<GroupPermission> groupPermissions = List.of(createGroupPermission(tenantId, new EntityGroupId(UUID.randomUUID()), role.getId(), new EntityGroupId(UUID.randomUUID()), entityType));

        PageData<GroupPermission> pageData = new PageData<>(groupPermissions, 0, groupPermissions.size(), false);
        when(groupPermissionService.findGroupPermissionByTenantIdAndRoleId(any(), any(), any())).thenReturn(pageData);

        EntityGroup entityGroup = new EntityGroup();
        entityGroup.setOwnerId(new CustomerId(UUID.randomUUID()));
        when(entityGroupService.findEntityGroupById(any(), any())).thenReturn(entityGroup);

        List<EntityId> entityIds = List.of(new UserId(UUID.randomUUID()));
        when(entityGroupService.findAllEntityIdsAsync(any(), any(), any())).thenReturn(Futures.immediateFuture(entityIds));

        userPermissionsService.onRoleUpdated(role);

        verify(groupPermissionService).findGroupPermissionByTenantIdAndRoleId(any(), any(), any());
        verify(entityGroupService, times(1)).findEntityGroupById(any(), any());
        verify(entityGroupService, times(1)).findAllEntityIdsAsync(any(), any(), any());
        verify(cache, times(1)).evict(any(UserPermissionCacheKey.class));
    }

    @Test
    void testOnGroupPermissionUpdated() throws ThingsboardException {
        GroupPermission groupPermission = new GroupPermission();
        groupPermission.setTenantId(tenantId);
        groupPermission.setUserGroupId(new EntityGroupId(UUID.randomUUID()));
        groupPermission.setPublic(false);

        EntityGroup entityGroup = new EntityGroup();
        entityGroup.setOwnerId(new CustomerId(UUID.randomUUID()));
        when(entityGroupService.findEntityGroupById(any(), any())).thenReturn(entityGroup);

        List<EntityId> entityIds = List.of(new UserId(UUID.randomUUID()));
        when(entityGroupService.findAllEntityIdsAsync(any(), any(), any())).thenReturn(Futures.immediateFuture(entityIds));

        userPermissionsService.onGroupPermissionUpdated(groupPermission);

        verify(entityGroupService).findEntityGroupById(any(), any());
        verify(entityGroupService).findAllEntityIdsAsync(any(), any(), any());
        verify(cache).evict(any(UserPermissionCacheKey.class));
    }

    @Test
    void testOnGroupPermissionDeleted() throws ThingsboardException {
        GroupPermission groupPermission = new GroupPermission();
        groupPermission.setTenantId(tenantId);
        groupPermission.setUserGroupId(new EntityGroupId(UUID.randomUUID()));
        groupPermission.setPublic(false);

        EntityGroup entityGroup = new EntityGroup();
        entityGroup.setOwnerId(new CustomerId(UUID.randomUUID()));
        when(entityGroupService.findEntityGroupById(any(), any())).thenReturn(entityGroup);

        List<EntityId> entityIds = List.of(new UserId(UUID.randomUUID()));
        when(entityGroupService.findAllEntityIdsAsync(any(), any(), any())).thenReturn(Futures.immediateFuture(entityIds));

        userPermissionsService.onGroupPermissionDeleted(groupPermission);

        verify(entityGroupService).findEntityGroupById(any(), any());
        verify(entityGroupService).findAllEntityIdsAsync(any(), any(), any());
        verify(cache).evict(any(UserPermissionCacheKey.class));
    }

    @Test
    void testOnUserUpdatedOrRemoved() {
        userPermissionsService.onUserUpdatedOrRemoved(testUser);
        verify(cache).evict((UserPermissionCacheKey) any());
    }

    private MergedUserPermissions getSysAdminPermissions() throws Exception {
        Field field = DefaultUserPermissionsService.class.getDeclaredField("sysAdminPermissions");
        field.setAccessible(true);
        return (MergedUserPermissions) field.get(userPermissionsService);
    }

    @Test
    void testExcludedPermissions_noExclusions_backwardCompatible() throws ThingsboardException {
        MergedUserPermissions result = setupMergeWithSingleRole(
                "{\"ALL\": [\"ALL\"]}",
                null
        );
        assertTrue(result.hasGenericPermission(Resource.ALL, Operation.ALL));
        assertTrue(result.hasGenericPermission(Resource.DEVICE, Operation.DELETE));
    }

    @Test
    void testExcludedPermissions_basicExclusion() throws ThingsboardException {
        MergedUserPermissions result = setupMergeWithSingleRole(
                "{\"DEVICE\": [\"ALL\"]}",
                "{\"DEVICE\": [\"DELETE\"]}"
        );
        Set<Operation> deviceOps = result.getGenericPermissions().get(Resource.DEVICE);
        assertNotNull(deviceOps);
        assertTrue(deviceOps.contains(Operation.READ));
        assertTrue(deviceOps.contains(Operation.WRITE));
        assertFalse(deviceOps.contains(Operation.DELETE));
        assertFalse(deviceOps.contains(Operation.ALL));
    }

    @Test
    void testExcludedPermissions_resourceAllExclusion() throws ThingsboardException {
        MergedUserPermissions result = setupMergeWithSingleRole(
                "{\"ALL\": [\"ALL\"]}",
                "{\"ALL\": [\"DELETE\"]}"
        );
        assertFalse(result.getGenericPermissions().containsKey(Resource.ALL));
        for (Resource resource : Resource.values()) {
            if (resource == Resource.ALL) continue;
            Set<Operation> validOps = Resource.operationsByResource.get(resource);
            if (validOps == null) continue;
            Set<Operation> grantedOps = result.getGenericPermissions().get(resource);
            if (validOps.contains(Operation.DELETE)) {
                assertNotNull(grantedOps, "Expected permissions for " + resource);
                assertFalse(grantedOps.contains(Operation.DELETE), "DELETE should be excluded for " + resource);
            }
        }
    }

    @Test
    void testExcludedPermissions_excludeSpecificResourceFromAll() throws ThingsboardException {
        MergedUserPermissions result = setupMergeWithSingleRole(
                "{\"ALL\": [\"ALL\"]}",
                "{\"DEVICE\": [\"DELETE\"]}"
        );
        assertFalse(result.getGenericPermissions().containsKey(Resource.ALL));
        Set<Operation> deviceOps = result.getGenericPermissions().get(Resource.DEVICE);
        assertNotNull(deviceOps);
        assertFalse(deviceOps.contains(Operation.DELETE));
        assertTrue(deviceOps.contains(Operation.READ));

        Set<Operation> assetOps = result.getGenericPermissions().get(Resource.ASSET);
        assertNotNull(assetOps);
        assertTrue(assetOps.contains(Operation.DELETE));
    }

    @Test
    void testExcludedPermissions_fullResourceExclusion() throws ThingsboardException {
        MergedUserPermissions result = setupMergeWithSingleRole(
                "{\"DEVICE\": [\"READ\", \"WRITE\", \"DELETE\"]}",
                "{\"DEVICE\": [\"ALL\"]}"
        );
        assertFalse(result.getGenericPermissions().containsKey(Resource.DEVICE));
    }

    @Test
    void testExcludedPermissions_multiRoleUnionOverridesExclusion() throws ThingsboardException {
        MergedUserPermissions result = setupMergeWithTwoRoles(
                "{\"DEVICE\": [\"ALL\"]}", "{\"DEVICE\": [\"DELETE\"]}",
                "{\"DEVICE\": [\"DELETE\"]}", null
        );
        Set<Operation> deviceOps = result.getGenericPermissions().get(Resource.DEVICE);
        assertNotNull(deviceOps);
        assertTrue(deviceOps.contains(Operation.DELETE));
        assertTrue(deviceOps.contains(Operation.READ));
    }

    @Test
    void testExcludedPermissions_noopExclusion() throws ThingsboardException {
        MergedUserPermissions result = setupMergeWithSingleRole(
                "{\"DEVICE\": [\"READ\"]}",
                "{\"DEVICE\": [\"DELETE\"]}"
        );
        Set<Operation> deviceOps = result.getGenericPermissions().get(Resource.DEVICE);
        assertNotNull(deviceOps);
        assertTrue(deviceOps.contains(Operation.READ));
        assertEquals(1, deviceOps.size());
    }

    @Test
    void testExcludedPermissions_combinedAllAndSpecificExclusions() throws ThingsboardException {
        MergedUserPermissions result = setupMergeWithSingleRole(
                "{\"ALL\": [\"ALL\"]}",
                "{\"ALL\": [\"DELETE\"], \"DEVICE\": [\"WRITE\"]}"
        );
        assertFalse(result.getGenericPermissions().containsKey(Resource.ALL));

        // DEVICE should have neither DELETE (from ALL exclusion) nor WRITE (from DEVICE exclusion)
        Set<Operation> deviceOps = result.getGenericPermissions().get(Resource.DEVICE);
        assertNotNull(deviceOps);
        assertFalse(deviceOps.contains(Operation.DELETE));
        assertFalse(deviceOps.contains(Operation.WRITE));
        assertTrue(deviceOps.contains(Operation.READ));

        // ASSET should have no DELETE but still have WRITE
        Set<Operation> assetOps = result.getGenericPermissions().get(Resource.ASSET);
        assertNotNull(assetOps);
        assertFalse(assetOps.contains(Operation.DELETE));
        assertTrue(assetOps.contains(Operation.WRITE));
        assertTrue(assetOps.contains(Operation.READ));
    }

    @Test
    void testExcludedPermissions_emptyExcludedPermissions() throws ThingsboardException {
        MergedUserPermissions result = setupMergeWithSingleRole(
                "{\"ALL\": [\"ALL\"]}",
                "{}"
        );
        assertTrue(result.hasGenericPermission(Resource.ALL, Operation.ALL));
    }

    private MergedUserPermissions setupMergeWithSingleRole(String permissionsJson, String excludedPermissionsJson) throws ThingsboardException {
        setupExecutorMock();
        when(cache.get(any())).thenReturn(null);

        EntityGroupId userGroupId = new EntityGroupId(UUID.randomUUID());
        when(entityGroupService.findEntityGroupsForEntityAsync(any(), any()))
                .thenReturn(Futures.immediateFuture(List.of(userGroupId)));

        RoleId roleId = new RoleId(UUID.randomUUID());
        Role role = new Role(roleId);
        role.setTenantId(tenantId);
        role.setType(RoleType.GENERIC);
        role.setPermissions(JacksonUtil.toJsonNode(permissionsJson));
        if (excludedPermissionsJson != null) {
            role.setExcludedPermissions(JacksonUtil.toJsonNode(excludedPermissionsJson));
        }

        GroupPermission gp = new GroupPermission();
        gp.setTenantId(tenantId);
        gp.setUserGroupId(userGroupId);
        gp.setRoleId(roleId);

        when(groupPermissionService.findGroupPermissionListByTenantIdAndUserGroupId(eq(tenantId), eq(userGroupId)))
                .thenReturn(List.of(gp));
        when(roleService.findRoleById(eq(tenantId), eq(roleId))).thenReturn(role);

        return userPermissionsService.getMergedPermissions(testUser, false);
    }

    private MergedUserPermissions setupMergeWithTwoRoles(
            String permissions1, String excluded1,
            String permissions2, String excluded2) throws ThingsboardException {
        setupExecutorMock();
        when(cache.get(any())).thenReturn(null);

        EntityGroupId userGroupId = new EntityGroupId(UUID.randomUUID());
        when(entityGroupService.findEntityGroupsForEntityAsync(any(), any()))
                .thenReturn(Futures.immediateFuture(List.of(userGroupId)));

        RoleId roleId1 = new RoleId(UUID.randomUUID());
        Role role1 = new Role(roleId1);
        role1.setTenantId(tenantId);
        role1.setType(RoleType.GENERIC);
        role1.setPermissions(JacksonUtil.toJsonNode(permissions1));
        if (excluded1 != null) {
            role1.setExcludedPermissions(JacksonUtil.toJsonNode(excluded1));
        }

        RoleId roleId2 = new RoleId(UUID.randomUUID());
        Role role2 = new Role(roleId2);
        role2.setTenantId(tenantId);
        role2.setType(RoleType.GENERIC);
        role2.setPermissions(JacksonUtil.toJsonNode(permissions2));
        if (excluded2 != null) {
            role2.setExcludedPermissions(JacksonUtil.toJsonNode(excluded2));
        }

        GroupPermission gp1 = new GroupPermission();
        gp1.setTenantId(tenantId);
        gp1.setUserGroupId(userGroupId);
        gp1.setRoleId(roleId1);

        GroupPermission gp2 = new GroupPermission();
        gp2.setTenantId(tenantId);
        gp2.setUserGroupId(userGroupId);
        gp2.setRoleId(roleId2);

        when(groupPermissionService.findGroupPermissionListByTenantIdAndUserGroupId(eq(tenantId), eq(userGroupId)))
                .thenReturn(List.of(gp1, gp2));
        when(roleService.findRoleById(eq(tenantId), eq(roleId1))).thenReturn(role1);
        when(roleService.findRoleById(eq(tenantId), eq(roleId2))).thenReturn(role2);

        return userPermissionsService.getMergedPermissions(testUser, false);
    }

    private void setupExecutorMock() {
        doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        }).when(dbCallbackExecutorService).execute(any(Runnable.class));
    }

    private GroupPermission createGroupPermission(TenantId tenantId, EntityGroupId userGroupId, RoleId roleId, EntityGroupId entityGroupId, EntityType entityGroupType) {
        GroupPermission groupPermission = new GroupPermission();
        groupPermission.setTenantId(tenantId);
        groupPermission.setUserGroupId(userGroupId);
        groupPermission.setRoleId(roleId);
        groupPermission.setEntityGroupId(entityGroupId);
        groupPermission.setEntityGroupType(entityGroupType);
        return groupPermission;
    }

}
