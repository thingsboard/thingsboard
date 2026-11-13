// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.service.validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.menu.CMAssigneeType;
import org.thingsboard.server.common.data.menu.CMScope;
import org.thingsboard.server.common.data.menu.CustomMenu;
import org.thingsboard.server.common.data.menu.CustomMenuInfo;
import org.thingsboard.server.dao.menu.CustomMenuService;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.exception.DataValidationException;

@Component
public class CustomMenuValidator extends DataValidator<CustomMenuInfo> {

    @Autowired
    @Lazy
    private CustomMenuService customMenuService;

    @Override
    protected void validateDataImpl(TenantId tenantId, CustomMenuInfo customMenuInfo) {
        if (customMenuInfo.getTenantId() == null) {
            throw new DataValidationException("Custom menu should be assigned to tenant");
        }
        if (customMenuInfo.getScope() == CMScope.TENANT && customMenuInfo.getAssigneeType() == CMAssigneeType.CUSTOMERS) {
            throw new DataValidationException("Tenant custom menu can not be assigned to customers");
        }
        if (!customMenuInfo.getTenantId().isSysTenantId() && customMenuInfo.getScope() == CMScope.SYSTEM) {
            throw new DataValidationException("Tenant custom menu can not have SYSTEM scope! Only TENANT and CUSTOMER are available for tenant");
        }
        if (customMenuInfo.getCustomerId() != null && !customMenuInfo.getCustomerId().isNullUid() && customMenuInfo.getScope() != CMScope.CUSTOMER) {
            throw new DataValidationException("Customer custom menu can have CUSTOMER scope only");
        }
    }

    @Override
    protected CustomMenuInfo validateUpdate(TenantId tenantId, CustomMenuInfo customMenuInfo) {
        CustomMenu old = customMenuService.findCustomMenuById(tenantId, customMenuInfo.getId());
        if (old == null) {
            throw new DataValidationException("Can't update non existing custom menu!");
        }
        if (!old.getScope().equals(customMenuInfo.getScope())) {
            throw new DataValidationException("Can't update custom menu scope!");
        }
        return old;
    }
}
