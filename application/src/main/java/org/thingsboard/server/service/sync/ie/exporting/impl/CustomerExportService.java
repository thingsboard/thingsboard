// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.sync.ie.exporting.impl;

import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.sync.vc.data.EntitiesExportCtx;

import java.util.Set;

@Service
@TbCoreComponent
public class CustomerExportService extends BaseEntityExportService<CustomerId, Customer, EntityExportData<Customer>> {

    @Override
    protected void setRelatedEntities(EntitiesExportCtx<?> ctx, Customer customer, EntityExportData<Customer> exportData) {
        customer.setParentCustomerId(getExternalIdOrElseInternal(ctx, customer.getParentCustomerId()));
    }

    @Override
    public Set<EntityType> getSupportedEntityTypes() {
        return Set.of(EntityType.CUSTOMER);
    }

}
