// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.edge.rpc.fetch;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.DeviceProfile;
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
import org.thingsboard.server.dao.device.DeviceProfileService;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Slf4j
public class DefaultProfilesEdgeEventFetcher implements EdgeEventFetcher {

    private final DeviceProfileService deviceProfileService;
    private final AssetProfileService assetProfileService;

    @Override
    public PageLink getPageLink(int pageSize) {
        return null;
    }

    @Override
    public PageData<EdgeEvent> fetchEdgeEvents(TenantId tenantId, Edge edge, PageLink pageLink) {
        List<EdgeEvent> result = new ArrayList<>();

        DeviceProfile deviceProfile = deviceProfileService.findDefaultDeviceProfile(tenantId);
        result.add(EdgeUtils.constructEdgeEvent(tenantId, edge.getId(), EdgeEventType.DEVICE_PROFILE,
                EdgeEventActionType.ADDED, deviceProfile.getId(), null));

        AssetProfile assetProfile = assetProfileService.findDefaultAssetProfile(tenantId);
        result.add(EdgeUtils.constructEdgeEvent(tenantId, edge.getId(), EdgeEventType.ASSET_PROFILE,
                EdgeEventActionType.ADDED, assetProfile.getId(), null));

        return new PageData<>(result, 1, result.size(), false);
    }

}
