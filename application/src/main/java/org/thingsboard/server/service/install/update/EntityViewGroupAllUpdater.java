// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.install.update;

import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.group.EntityGroupService;

class EntityViewGroupAllUpdater extends EntityGroupAllPaginatedUpdater<EntityViewId, EntityView> {

    private final EntityViewService entityViewService;

    public EntityViewGroupAllUpdater(EntityViewService entityViewService, CustomerService customerService,
                                     EntityGroupService entityGroupService, EntityGroup groupAll, boolean fetchAllTenantEntities) {
        super(customerService,
                entityGroupService,
                groupAll,
                fetchAllTenantEntities,
                (tenantId, pageLink) -> entityViewService.findEntityViewByTenantId(tenantId, pageLink),
                (tenantId, entityViewIds) -> entityViewService.findEntityViewsByTenantIdAndIdsAsync(tenantId, entityViewIds),
                entityId -> new EntityViewId(entityId.getId()),
                entityView -> entityView.getId());
        this.entityViewService = entityViewService;
    }


    @Override
    protected void unassignFromCustomer(EntityView entity) {
        entity.setCustomerId(new CustomerId(CustomerId.NULL_UUID));
        entityViewService.saveEntityView(entity);
    }

    @Override
    protected String getName() {
        return "Entity views group all updater";
    }

}
