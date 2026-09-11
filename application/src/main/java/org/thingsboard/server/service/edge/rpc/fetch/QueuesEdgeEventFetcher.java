// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.edge.rpc.fetch;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.dao.queue.QueueService;

@AllArgsConstructor
@Slf4j
public class QueuesEdgeEventFetcher extends BasePageableEdgeEventFetcher<Queue> {

    private final QueueService queueService;

    @Override
    PageData<Queue> fetchEntities(TenantId tenantId, Edge edge, PageLink pageLink) {
        return queueService.findQueuesByTenantId(tenantId, pageLink);
    }

    @Override
    EdgeEvent constructEdgeEvent(TenantId tenantId, Edge edge, Queue queue) {
        return EdgeUtils.constructEdgeEvent(tenantId, edge.getId(), EdgeEventType.QUEUE,
                EdgeEventActionType.ADDED, queue.getId(), null);
    }

}
