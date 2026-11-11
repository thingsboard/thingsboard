// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.edge.rpc.fetch;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.customer.CustomerService;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@AllArgsConstructor
public class PublicCustomerUserGroupEdgeEventFetcher implements EdgeEventFetcher {

    private final CustomerService customerService;
    private final EntityId ownerId;

    @Override
    public PageLink getPageLink(int pageSize) {
        return new PageLink(pageSize);
    }

    @Override
    public PageData<EdgeEvent> fetchEdgeEvents(TenantId tenantId, Edge edge, PageLink pageLink) {
        List<EdgeEvent> result = new ArrayList<>();
        EntityGroup publicUserGroup = customerService.findOrCreatePublicUserGroup(tenantId, ownerId);
        result.add(EdgeUtils.constructEdgeEvent(edge.getTenantId(), edge.getId(), EdgeEventType.ENTITY_GROUP,
                EdgeEventActionType.ADDED, publicUserGroup.getId(), null));
        // returns PageData object to be in sync with other fetchers
        return new PageData<>(result, 1, result.size(), false);
    }

}
