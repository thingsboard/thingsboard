// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.edge.rpc.fetch;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
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

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Slf4j
public class EntityGroupEdgeEventFetcher implements EdgeEventFetcher {

    private final EntityGroupService entityGroupService;
    @Getter
    private final EntityType groupType;

    @Override
    public PageLink getPageLink(int pageSize) {
        return new PageLink(pageSize);
    }

    @Override
    public PageData<EdgeEvent> fetchEdgeEvents(TenantId tenantId, Edge edge, PageLink pageLink) {
        log.trace("[{}] start fetching edge events [{}], groupType {}, pageLink {}", tenantId, edge.getId(), groupType, pageLink);
        PageData<EntityGroup> pageData = entityGroupService.findEdgeEntityGroupsByType(tenantId, edge.getId(), groupType, pageLink);
        List<EdgeEvent> result = new ArrayList<>();
        if (!pageData.getData().isEmpty()) {
            for (EntityGroup entityGroup : pageData.getData()) {
                result.add(EdgeUtils.constructEdgeEvent(tenantId, edge.getId(), EdgeEventType.ENTITY_GROUP,
                        EdgeEventActionType.ADDED, entityGroup.getId(), null, null));
            }
        }
        return new PageData<>(result, pageData.getTotalPages(), pageData.getTotalElements(), pageData.hasNext());
    }

}
