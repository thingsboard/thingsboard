// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.edge.rpc.fetch;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.dao.grouppermission.GroupPermissionService;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Slf4j
public class EntityGroupPermissionsEdgeEventFetcher implements EdgeEventFetcher {

    private final GroupPermissionService groupPermissionService;
    private final EntityType groupType;
    private final EntityGroupId entityGroupId;

    @Override
    public PageLink getPageLink(int pageSize) {
        return new PageLink(pageSize);
    }

    @Override
    public PageData<EdgeEvent> fetchEdgeEvents(TenantId tenantId, Edge edge, PageLink pageLink) {
        log.trace("[{}] start fetching edge events [{}], groupType {}, entityGroupId {}, pageLink {}", tenantId, edge.getId(), groupType, entityGroupId, pageLink);
        PageData<GroupPermission> pageData;
        if (EntityType.USER.equals(groupType)) {
            pageData = groupPermissionService.findGroupPermissionByTenantIdAndUserGroupId(edge.getTenantId(), entityGroupId, pageLink);
        } else {
            pageData = groupPermissionService.findGroupPermissionByTenantIdAndEntityGroupId(edge.getTenantId(), entityGroupId, pageLink);
        }
        List<EdgeEvent> result = new ArrayList<>();
        if (!pageData.getData().isEmpty()) {
            for (GroupPermission groupPermission : pageData.getData()) {
                result.add(EdgeUtils.constructEdgeEvent(tenantId, edge.getId(), EdgeEventType.GROUP_PERMISSION,
                        EdgeEventActionType.ADDED, groupPermission.getId(), null, null));
            }
        }
        return new PageData<>(result, pageData.getTotalPages(), pageData.getTotalElements(), pageData.hasNext());
    }

}
