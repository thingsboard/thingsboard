// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.service.validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.JsonNode;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.role.RoleType;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.role.RoleDao;
import org.thingsboard.server.dao.role.RoleService;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.exception.DataValidationException;

import java.util.Iterator;

import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

@Component
public class RoleDataValidator extends DataValidator<Role> {

    @Autowired
    private RoleDao roleDao;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private CustomerDao customerDao;

    @Autowired
    @Lazy
    private RoleService roleService;

    @Override
    protected void validateCreate(TenantId tenantId, Role role) {
        if (role.getCustomerId() == null || role.getCustomerId().isNullUid()) {
            roleDao.findRoleByTenantIdAndName(role.getTenantId().getId(), role.getName())
                    .ifPresent(e -> {
                        throw new DataValidationException("Role with such name already exists!");
                    });
        } else {
            roleDao.findRoleByByTenantIdAndCustomerIdAndName(role.getTenantId().getId(), role.getCustomerId().getId(), role.getName())
                    .ifPresent(e -> {
                        throw new DataValidationException("Role with such name already exists!");
                    });
        }
    }

    @Override
    protected Role validateUpdate(TenantId tenantId, Role role) {
        if (role.getCustomerId() == null || role.getCustomerId().isNullUid()) {
            roleDao.findRoleByTenantIdAndName(role.getTenantId().getId(), role.getName())
                    .ifPresent(e -> {
                        if (!e.getUuidId().equals(role.getUuidId())) {
                            throw new DataValidationException("Role with such name already exists!");
                        }
                    });
        } else {
            roleDao.findRoleByByTenantIdAndCustomerIdAndName(role.getTenantId().getId(), role.getCustomerId().getId(), role.getName())
                    .ifPresent(e -> {
                        if (!e.getUuidId().equals(role.getUuidId())) {
                            throw new DataValidationException("Role with such name already exists!");
                        }
                    });
        }

        Role before = roleService.findRoleById(tenantId, role.getId());
        if (role.getType() != before.getType()) {
            throw new DataValidationException("Role type cannot be changed after role creation");
        }
        return before;
    }

    @Override
    protected void validateDataImpl(TenantId tenantId, Role role) {
        if (role.getType() == null) {
            throw new DataValidationException("Role type should be specified!");
        }
        if (StringUtils.isEmpty(role.getName())) {
            throw new DataValidationException("Role name should be specified!");
        }
        if (role.getTenantId() == null) {
            role.setTenantId(TenantId.fromUUID(NULL_UUID));
        } else if (!role.getTenantId().isNullUid()) { // not Sys admin level
            if (!tenantService.tenantExists(role.getTenantId())) {
                throw new DataValidationException("Role is referencing to non-existent tenant!");
            }
        }
        if (role.getCustomerId() == null) {
            role.setCustomerId(new CustomerId(NULL_UUID));
        } else if (!role.getCustomerId().isNullUid()) {
            Customer customer = customerDao.findById(tenantId, role.getCustomerId().getId());
            if (customer == null) {
                throw new DataValidationException("Can't assign role to non-existent customer!");
            }
            if (!customer.getTenantId().equals(role.getTenantId())) {
                throw new DataValidationException("Can't assign role to customer from different tenant!");
            }
        }
        validateExcludedPermissions(role);
    }

    private void validateExcludedPermissions(Role role) {
        JsonNode excludedPermissions = role.getExcludedPermissions();
        if (excludedPermissions == null || excludedPermissions.isEmpty()) {
            return;
        }
        if (role.getType() != RoleType.GENERIC) {
            throw new DataValidationException("Excluded permissions are only supported for generic roles!");
        }
        Iterator<String> fieldNames = excludedPermissions.fieldNames();
        while (fieldNames.hasNext()) {
            String resourceName = fieldNames.next();
            try {
                Resource.valueOf(resourceName);
            } catch (IllegalArgumentException e) {
                throw new DataValidationException("Invalid resource in excluded permissions: " + resourceName);
            }
            JsonNode operationsNode = excludedPermissions.get(resourceName);
            if (!operationsNode.isArray() || operationsNode.isEmpty()) {
                throw new DataValidationException("Excluded permissions for resource " + resourceName + " must be a non-empty array of operations!");
            }
            for (JsonNode opNode : operationsNode) {
                try {
                    Operation.valueOf(opNode.asText());
                } catch (IllegalArgumentException e) {
                    throw new DataValidationException("Invalid operation in excluded permissions: " + opNode.asText());
                }
            }
        }
    }
}
