// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.install.update;

import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.group.EntityGroupService;

class AssetsGroupAllUpdater extends EntityGroupAllPaginatedUpdater<AssetId, Asset> {

    private final AssetService assetService;

    public AssetsGroupAllUpdater(AssetService assetService, CustomerService customerService,
                                 EntityGroupService entityGroupService, EntityGroup groupAll, boolean fetchAllTenantEntities) {
        super(customerService,
                entityGroupService,
                groupAll,
                fetchAllTenantEntities,
                (tenantId, pageLink) -> assetService.findAssetsByTenantId(tenantId, pageLink),
                (tenantId, assetIds) -> assetService.findAssetsByTenantIdAndIdsAsync(tenantId, assetIds),
                entityId -> new AssetId(entityId.getId()),
                asset -> asset.getId());
        this.assetService = assetService;
    }

    @Override
    protected void unassignFromCustomer(Asset entity) {
        entity.setCustomerId(new CustomerId(CustomerId.NULL_UUID));
        assetService.saveAsset(entity);
    }

    @Override
    protected String getName() {
        return "Assets group all updater";
    }


}
