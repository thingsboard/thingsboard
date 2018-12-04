package org.thingsboard.server.service.security.permission;

import org.thingsboard.server.common.data.HasCustomerId;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public interface PermissionChecker<T extends HasTenantId, I extends EntityId> {

    default boolean hasPermission(SecurityUser user, TenantId tenantId, Operation operation) {
        return false;
    }

    default boolean hasPermission(SecurityUser user, TenantId tenantId, Operation operation, EntityId entityId) {
        return false;
    }

    default boolean hasPermission(SecurityUser user, Operation operation, I entityId, T entity) {
        return false;
    }

    public class GenericPermissionChecker<T extends HasTenantId, I extends EntityId> implements PermissionChecker<T, I> {

        private final Set<Operation> allowedOperations;

        public GenericPermissionChecker(Operation... operations) {
            allowedOperations = new HashSet<Operation>(Arrays.asList(operations));
        }

        @Override
        public boolean hasPermission(SecurityUser user, TenantId tenantId, Operation operation) {
            return allowedOperations.contains(Operation.ALL) || allowedOperations.contains(operation);
        }

        @Override
        public boolean hasPermission(SecurityUser user, TenantId tenantId, Operation operation, EntityId entityId) {
            return allowedOperations.contains(Operation.ALL) || allowedOperations.contains(operation);
        }

        @Override
        public boolean hasPermission(SecurityUser user, Operation operation, I entityId, T entity) {
            return allowedOperations.contains(Operation.ALL) || allowedOperations.contains(operation);
        }
    }

    public static PermissionChecker denyAllPermissionChecker = new PermissionChecker() {};

    public static PermissionChecker allowAllPermissionChecker = new PermissionChecker<HasTenantId, EntityId>() {

        @Override
        public boolean hasPermission(SecurityUser user, TenantId tenantId, Operation operation) {
            return true;
        }

        @Override
        public boolean hasPermission(SecurityUser user, TenantId tenantId, Operation operation, EntityId entityId) {
            return true;
        }

        @Override
        public boolean hasPermission(SecurityUser user, Operation operation, EntityId entityId, HasTenantId entity) {
            return true;
        }
    };


}
