// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.install.update;

import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.group.EntityGroupService;

class EdgesGroupAllUpdater extends EntityGroupAllPaginatedUpdater<EdgeId, Edge> {

    private final EdgeService edgeService;

    public EdgesGroupAllUpdater(EdgeService edgeService, CustomerService customerService,
                                EntityGroupService entityGroupService, EntityGroup groupAll, boolean fetchAllTenantEntities) {
        super(customerService,
                entityGroupService,
                groupAll,
                fetchAllTenantEntities,
                edgeService::findEdgesByTenantId,
                edgeService::findEdgesByTenantIdAndIdsAsync,
                entityId -> new EdgeId(entityId.getId()),
                edge -> edge.getId());
        this.edgeService = edgeService;
    }

    @Override
    protected void unassignFromCustomer(Edge entity) {
        entity.setCustomerId(new CustomerId(CustomerId.NULL_UUID));
        edgeService.saveEdge(entity);
    }

    @Override
    protected String getName() {
        return "Edges group all updater";
    }
}
