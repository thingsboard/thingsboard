// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.edge.rpc.fetch;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.asset.AssetService;

@AllArgsConstructor
@Slf4j
public class AssetsEdgeEventFetcher extends BasePageableEdgeEventFetcher<Asset> {

    private final AssetService assetService;

    @Override
    PageData<Asset> fetchEntities(TenantId tenantId, Edge edge, PageLink pageLink) {
        return assetService.findAssetsByTenantIdAndEdgeId(tenantId, edge.getId(), pageLink);
    }

    @Override
    EdgeEvent constructEdgeEvent(TenantId tenantId, Edge edge, Asset asset) {
        return EdgeUtils.constructEdgeEvent(tenantId, edge.getId(), EdgeEventType.ASSET,
                EdgeEventActionType.ADDED, asset.getId(), null);
    }
}
