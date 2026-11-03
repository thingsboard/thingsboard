// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.service.validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.dao.group.EntityGroupDao;
import org.thingsboard.server.dao.grouppermission.GroupPermissionDao;
import org.thingsboard.server.dao.owner.OwnerService;
import org.thingsboard.server.dao.role.RoleDao;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.exception.DataValidationException;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.thingsboard.server.dao.service.validator.GroupPermissionDataValidator.USER_GROUP_OUTSIDE_OWNER_HIERARCHY_ERROR;

@SpringBootTest(classes = GroupPermissionDataValidator.class)
class GroupPermissionDataValidatorTest {

    private final TenantId tenantId = TenantId.fromUUID(UUID.fromString("9ef79cdf-37a8-4119-b682-2e7ed4e018da"));
    private final CustomerId customerA = new CustomerId(UUID.fromString("1b3f0a7e-5c2d-4f6a-9e8b-0c1d2e3f4a5b"));
    private final CustomerId subCustomerA1 = new CustomerId(UUID.fromString("2c4e1b8f-6d3e-4a7b-8f9c-1d2e3f4a5b6c"));
    private final CustomerId customerB = new CustomerId(UUID.fromString("3d5f2c9a-7e4f-4b8c-9a0d-2e3f4a5b6c7d"));
    private final RoleId roleId = new RoleId(UUID.fromString("4e6a3d0b-8f5a-4c9d-8b1e-3f4a5b6c7d8e"));

    @MockitoBean
    TenantService tenantService;
    @MockitoBean
    EntityGroupDao entityGroupDao;
    @MockitoBean
    RoleDao roleDao;
    @MockitoBean
    GroupPermissionDao groupPermissionDao;
    @MockitoBean
    OwnerService ownerService;

    @Autowired
    GroupPermissionDataValidator validator;

    @BeforeEach
    void setUp() {
        willReturn(true).given(tenantService).tenantExists(tenantId);
        given(roleDao.findById(tenantId, roleId.getId())).willReturn(new Role(roleId));
        givenHierarchy(tenantId, tenantId);
        givenHierarchy(customerA, customerA, tenantId);
        givenHierarchy(subCustomerA1, subCustomerA1, customerA, tenantId);
        givenHierarchy(customerB, customerB, tenantId);
    }

    @Test
    void givenCustomerGrantee_whenTargetIsTenantUserGroup_thenRejected() {
        GroupPermission permission = permission(group(EntityType.USER, customerA), group(EntityType.USER, tenantId));

        assertThatThrownBy(() -> validator.validateDataImpl(tenantId, permission))
                .isInstanceOf(DataValidationException.class)
                .hasMessage(USER_GROUP_OUTSIDE_OWNER_HIERARCHY_ERROR);
    }

    @Test
    void givenSubCustomerGrantee_whenTargetIsParentCustomerUserGroup_thenRejected() {
        GroupPermission permission = permission(group(EntityType.USER, subCustomerA1), group(EntityType.USER, customerA));

        assertThatThrownBy(() -> validator.validateDataImpl(tenantId, permission))
                .isInstanceOf(DataValidationException.class)
                .hasMessage(USER_GROUP_OUTSIDE_OWNER_HIERARCHY_ERROR);
    }

    @Test
    void givenCustomerGrantee_whenTargetIsSiblingCustomerUserGroup_thenRejected() {
        GroupPermission permission = permission(group(EntityType.USER, customerA), group(EntityType.USER, customerB));

        assertThatThrownBy(() -> validator.validateDataImpl(tenantId, permission))
                .isInstanceOf(DataValidationException.class)
                .hasMessage(USER_GROUP_OUTSIDE_OWNER_HIERARCHY_ERROR);
    }

    @Test
    void givenCustomerGrantee_whenTargetIsSameCustomerUserGroup_thenAccepted() {
        GroupPermission permission = permission(group(EntityType.USER, customerA), group(EntityType.USER, customerA));

        assertThatCode(() -> validator.validateDataImpl(tenantId, permission)).doesNotThrowAnyException();
        assertThat(permission.getEntityGroupType()).isEqualTo(EntityType.USER);
    }

    @Test
    void givenCustomerGrantee_whenTargetIsSubCustomerUserGroup_thenAccepted() {
        GroupPermission permission = permission(group(EntityType.USER, customerA), group(EntityType.USER, subCustomerA1));

        assertThatCode(() -> validator.validateDataImpl(tenantId, permission)).doesNotThrowAnyException();
    }

    @Test
    void givenTenantGrantee_whenTargetIsCustomerUserGroup_thenAccepted() {
        GroupPermission permission = permission(group(EntityType.USER, tenantId), group(EntityType.USER, subCustomerA1));

        assertThatCode(() -> validator.validateDataImpl(tenantId, permission)).doesNotThrowAnyException();
    }

    @Test
    void givenCustomerGrantee_whenTargetIsTenantDeviceGroup_thenAcceptedWithoutHierarchyCheck() {
        GroupPermission permission = permission(group(EntityType.USER, customerA), group(EntityType.DEVICE, tenantId));

        assertThatCode(() -> validator.validateDataImpl(tenantId, permission)).doesNotThrowAnyException();
        assertThat(permission.getEntityGroupType()).isEqualTo(EntityType.DEVICE);
        verify(ownerService, never()).fetchOwnersHierarchy(any(), any());
    }

    @Test
    void givenGenericPermission_thenAcceptedWithoutHierarchyCheck() {
        GroupPermission permission = permission(group(EntityType.USER, customerA), null);

        assertThatCode(() -> validator.validateDataImpl(tenantId, permission)).doesNotThrowAnyException();
        assertThat(permission.getEntityGroupType()).isNull();
        verify(ownerService, never()).fetchOwnersHierarchy(any(), any());
    }

    @Test
    void givenSelfRegistrationPermissionTargetingUserGroup_thenRejected() {
        List<GroupPermission> permissions = List.of(
                selfRegistrationPermission(group(EntityType.DEVICE, tenantId)),
                selfRegistrationPermission(group(EntityType.USER, tenantId)));

        assertThatThrownBy(() -> GroupPermissionDataValidator.validateSelfRegistrationPermissions(tenantId, permissions, entityGroupDao))
                .isInstanceOf(DataValidationException.class)
                .hasMessage(USER_GROUP_OUTSIDE_OWNER_HIERARCHY_ERROR);
    }

    @Test
    void givenSelfRegistrationPermissionsWithoutUserGroups_thenAccepted() {
        List<GroupPermission> permissions = new ArrayList<>();
        permissions.add(selfRegistrationPermission(group(EntityType.DEVICE, tenantId)));
        permissions.add(selfRegistrationPermission(null));
        GroupPermission withNullUidGroup = new GroupPermission();
        withNullUidGroup.setRoleId(roleId);
        withNullUidGroup.setEntityGroupId(new EntityGroupId(EntityId.NULL_UUID));
        permissions.add(withNullUidGroup);

        assertThatCode(() -> GroupPermissionDataValidator.validateSelfRegistrationPermissions(tenantId, permissions, entityGroupDao))
                .doesNotThrowAnyException();
        assertThatCode(() -> GroupPermissionDataValidator.validateSelfRegistrationPermissions(tenantId, null, entityGroupDao))
                .doesNotThrowAnyException();
    }

    private void givenHierarchy(EntityId ownerId, EntityId... hierarchy) {
        given(ownerService.fetchOwnersHierarchy(tenantId, ownerId)).willReturn(new LinkedHashSet<>(List.of(hierarchy)));
    }

    private EntityGroup group(EntityType type, EntityId ownerId) {
        EntityGroup entityGroup = new EntityGroup(new EntityGroupId(UUID.randomUUID()));
        entityGroup.setType(type);
        entityGroup.setOwnerId(ownerId);
        given(entityGroupDao.findById(tenantId, entityGroup.getUuidId())).willReturn(entityGroup);
        return entityGroup;
    }

    private GroupPermission permission(EntityGroup userGroup, EntityGroup entityGroup) {
        GroupPermission permission = new GroupPermission();
        permission.setTenantId(tenantId);
        permission.setUserGroupId(userGroup.getId());
        permission.setRoleId(roleId);
        if (entityGroup != null) {
            permission.setEntityGroupId(entityGroup.getId());
            permission.setEntityGroupType(entityGroup.getType());
        }
        return permission;
    }

    private GroupPermission selfRegistrationPermission(EntityGroup entityGroup) {
        GroupPermission permission = new GroupPermission();
        permission.setRoleId(roleId);
        if (entityGroup != null) {
            permission.setEntityGroupId(entityGroup.getId());
            permission.setEntityGroupType(entityGroup.getType());
        }
        return permission;
    }

}
