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
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.scheduler.SchedulerEvent;
import org.thingsboard.server.dao.scheduler.SchedulerEventService;

@AllArgsConstructor
@Slf4j
public class SchedulerEventsEdgeEventFetcher extends BasePageableEdgeEventFetcher<SchedulerEvent> {

    private final SchedulerEventService schedulerEventService;

    @Override
    PageData<SchedulerEvent> fetchEntities(TenantId tenantId, Edge edge, PageLink pageLink) {
        return schedulerEventService.findSchedulerEventsByTenantIdAndEdgeId(tenantId, edge.getId(), pageLink);
    }

    @Override
    EdgeEvent constructEdgeEvent(TenantId tenantId, Edge edge, SchedulerEvent schedulerEvent) {
        return EdgeUtils.constructEdgeEvent(tenantId, edge.getId(), EdgeEventType.SCHEDULER_EVENT,
                EdgeEventActionType.ADDED, schedulerEvent.getId(), null);
    }

}
