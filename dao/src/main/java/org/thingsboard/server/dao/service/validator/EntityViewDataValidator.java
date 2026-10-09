// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.service.validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.entityview.EntityViewDao;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.exception.DataValidationException;

import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

@Component
public class EntityViewDataValidator extends DataValidator<EntityView> {

    @Autowired
    private EntityViewDao entityViewDao;

    @Autowired
    @Lazy
    private EntityViewService entityViewService;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private CustomerDao customerDao;

    @Override
    protected void validateCreate(TenantId tenantId, EntityView entityView) {
        validateNameUniqueness(entityView, null, entityViewService::findEntityViewByTenantIdAndName, "Entity view with such name already exists!");
        validateExternalIdUniqueness(entityView, null, entityViewDao, "Entity view with such external id already exists!");
    }

    @Override
    protected EntityView validateUpdate(TenantId tenantId, EntityView entityView) {
        EntityView old = entityViewDao.findById(entityView.getTenantId(), entityView.getId().getId());
        if (old == null) {
            throw new DataValidationException("Can't update non existing entity view!");
        }
        validateNameUniqueness(entityView, old, entityViewService::findEntityViewByTenantIdAndName, "Entity view with such name already exists!");
        validateExternalIdUniqueness(entityView, old, entityViewDao, "Entity view with such external id already exists!");
        return old;
    }

    @Override
    protected void validateDataImpl(TenantId tenantId, EntityView entityView) {
        validateString("Entity view name", entityView.getName());
        validateString("Entity view type", entityView.getType());
        if (entityView.getTenantId() == null) {
            throw new DataValidationException("Entity view should be assigned to tenant!");
        } else {
            if (!tenantService.tenantExists(entityView.getTenantId())) {
                throw new DataValidationException("Entity view is referencing to non-existent tenant!");
            }
        }
        if (entityView.getCustomerId() == null) {
            entityView.setCustomerId(new CustomerId(NULL_UUID));
        } else if (!entityView.getCustomerId().getId().equals(NULL_UUID)) {
            Customer customer = customerDao.findById(tenantId, entityView.getCustomerId().getId());
            if (customer == null) {
                throw new DataValidationException("Can't assign entity view to non-existent customer!");
            }
            if (!customer.getTenantId().getId().equals(entityView.getTenantId().getId())) {
                throw new DataValidationException("Can't assign entity view to customer from different tenant!");
            }
        }
    }
}
