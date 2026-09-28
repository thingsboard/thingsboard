// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.edge.rpc.fetch;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.ota.DeviceGroupOtaPackageService;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Slf4j
public class DeviceGroupOtaPackageEdgeEventFetcher implements EdgeEventFetcher {

    private final DeviceGroupOtaPackageService deviceGroupOtaPackageService;

    private final EntityGroupService entityGroupService;

    @Override
    public PageLink getPageLink(int pageSize) {
        return new PageLink(pageSize);
    }

    @Override
    public PageData<EdgeEvent> fetchEdgeEvents(TenantId tenantId, Edge edge, PageLink pageLink) throws Exception {
        PageData<EntityGroup> pageData = entityGroupService.findEdgeEntityGroupsByType(tenantId, edge.getId(), EntityType.DEVICE, pageLink);
        List<EdgeEvent> result = new ArrayList<>();
        if (!pageData.getData().isEmpty()) {
            for (EntityGroup entityGroup : pageData.getData()) {
                deviceGroupOtaPackageService.findDeviceGroupOtaPackageByGroupId(entityGroup.getId())
                        .forEach(otaPackage -> {
                            result.add(EdgeUtils.constructEdgeEvent(tenantId, edge.getId(), EdgeEventType.DEVICE_GROUP_OTA,
                                    EdgeEventActionType.UPDATED, otaPackage.getGroupId(), JacksonUtil.valueToTree(otaPackage)));
                        });
            }
        }
        return new PageData<>(result, 1, result.size(), false);
    }

}
