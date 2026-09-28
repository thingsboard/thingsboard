// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.security.permission;

import org.thingsboard.server.common.data.TenantEntity;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.group.EntityGroupInfo;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.menu.CustomMenuInfo;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public interface PermissionChecker<I extends EntityId, T extends TenantEntity> {

    default boolean hasPermission(SecurityUser user, Resource resource, Operation operation) throws ThingsboardException {
        return false;
    }

    default boolean hasPermission(SecurityUser user, Operation operation, I entityId, T entity) throws ThingsboardException {
        return false;
    }

    default boolean hasPermission(SecurityUser user, Operation operation, I entityId, T entity, EntityGroupId entityGroupId) throws ThingsboardException {
        return false;
    }

    default boolean hasEntityGroupPermission(SecurityUser user, Operation operation, EntityGroup entityGroup) throws ThingsboardException {
        return false;
    }

    default boolean hasEntityGroupInfoPermission(SecurityUser user, Operation operation, EntityGroupInfo entityGroupInfo) throws ThingsboardException {
        return false;
    }

    default boolean hasCustomMenuPermission(SecurityUser user, Operation operation, CustomMenuInfo customMenu) throws ThingsboardException {
        return false;
    }

    public class GenericPermissionChecker<I extends EntityId, T extends TenantEntity> implements PermissionChecker<I, T> {

        private final Set<Operation> allowedOperations;

        public GenericPermissionChecker(Operation... operations) {
            allowedOperations = new HashSet<Operation>(Arrays.asList(operations));
        }

        @Override
        public boolean hasPermission(SecurityUser user, Resource resource, Operation operation) {
            return allowedOperations.contains(Operation.ALL) || allowedOperations.contains(operation);
        }

        @Override
        public boolean hasPermission(SecurityUser user, Operation operation, I entityId, T entity) {
            return allowedOperations.contains(Operation.ALL) || allowedOperations.contains(operation);
        }

        @Override
        public boolean hasPermission(SecurityUser user, Operation operation, I entityId, T entity, EntityGroupId entityGroupId) {
            return allowedOperations.contains(Operation.ALL) || allowedOperations.contains(operation);
        }

        @Override
        public boolean hasEntityGroupPermission(SecurityUser user, Operation operation, EntityGroup entityGroup) {
            return allowedOperations.contains(Operation.ALL) || allowedOperations.contains(operation);
        }

        @Override
        public boolean hasEntityGroupInfoPermission(SecurityUser user, Operation operation, EntityGroupInfo entityGroup) {
            return allowedOperations.contains(Operation.ALL) || allowedOperations.contains(operation);
        }

    }

    public static PermissionChecker denyAllPermissionChecker = new PermissionChecker() {
    };

    public static PermissionChecker allowAllPermissionChecker = new PermissionChecker() {

        @Override
        public boolean hasPermission(SecurityUser user, Resource resource, Operation operation) {
            return true;
        }

        @Override
        public boolean hasPermission(SecurityUser user, Operation operation, EntityId entityId, TenantEntity entity) {
            return true;
        }

        @Override
        public boolean hasPermission(SecurityUser user, Operation operation, EntityId entityId, TenantEntity entity, EntityGroupId entityGroupId) {
            return true;
        }

        @Override
        public boolean hasEntityGroupPermission(SecurityUser user, Operation operation, EntityGroup entityGroup) {
            return true;
        }

        @Override
        public boolean hasEntityGroupInfoPermission(SecurityUser user, Operation operation, EntityGroupInfo entityGroup) {
            return true;
        }

    };


}
