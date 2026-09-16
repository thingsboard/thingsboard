// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.edge.rpc.fetch;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.asset.AssetProfileService;

@AllArgsConstructor
@Slf4j
public class AssetProfilesEdgeEventFetcher extends BasePageableEdgeEventFetcher<AssetProfile> {

    private final AssetProfileService assetProfileService;

    @Override
    PageData<AssetProfile> fetchEntities(TenantId tenantId, Edge edge, PageLink pageLink) {
        return assetProfileService.findAssetProfiles(tenantId, pageLink);
    }

    @Override
    EdgeEvent constructEdgeEvent(TenantId tenantId, Edge edge, AssetProfile assetProfile) {
        return EdgeUtils.constructEdgeEvent(tenantId, edge.getId(), EdgeEventType.ASSET_PROFILE,
                EdgeEventActionType.ADDED, assetProfile.getId(), null);
    }

}
