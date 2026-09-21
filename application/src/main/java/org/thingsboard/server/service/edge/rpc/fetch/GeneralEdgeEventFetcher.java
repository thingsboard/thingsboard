// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.edge.rpc.fetch;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.edge.EdgeEventService;

import java.util.concurrent.TimeUnit;

@AllArgsConstructor
@Slf4j
public class GeneralEdgeEventFetcher implements EdgeEventFetcher {
    // Subtract from queueStartTs to ensure no data is lost due to potential misordering of edge events by created_time.
    private static final long MISORDERING_COMPENSATION_MILLIS = TimeUnit.SECONDS.toMillis(60);

    private final Long queueStartTs;
    private Long seqIdStart;
    @Getter
    private boolean seqIdNewCycleStarted;
    private Long maxReadRecordsCount;
    private final EdgeEventService edgeEventService;

    @Override
    public PageLink getPageLink(int pageSize) {
        return new TimePageLink(
                pageSize,
                0,
                null,
                null,
                queueStartTs > 0 ? queueStartTs - MISORDERING_COMPENSATION_MILLIS : 0,
                System.currentTimeMillis());
    }

    @Override
    public PageData<EdgeEvent> fetchEdgeEvents(TenantId tenantId, Edge edge, PageLink pageLink) {
        try {
            log.trace("[{}] Finding general edge events [{}], seqIdStart = {}, pageLink = {}",
                    tenantId, edge.getId(), seqIdStart, pageLink);
            PageData<EdgeEvent> edgeEvents = edgeEventService.findEdgeEvents(tenantId, edge.getId(), seqIdStart, null, (TimePageLink) pageLink);
            if (!edgeEvents.getData().isEmpty()) {
                return edgeEvents;
            }
            if (seqIdStart > this.maxReadRecordsCount) {
                edgeEvents = edgeEventService.findEdgeEvents(tenantId, edge.getId(), 0L, Math.max(this.maxReadRecordsCount, seqIdStart - this.maxReadRecordsCount), (TimePageLink) pageLink);
                if (edgeEvents.getData().stream().anyMatch(ee -> ee.getSeqId() < seqIdStart)) {
                    log.info("[{}] seqId column of edge_event table started new cycle [{}]", tenantId, edge.getId());
                    this.seqIdNewCycleStarted = true;
                    this.seqIdStart = 0L;
                    return edgeEvents;
                }
            }
            log.info("[{}] Unexpected edge notification message received. " +
                    "No new events found, and the seqId column of the edge_event table has not started a new cycle [{}].", tenantId, edge.getId());
            return new PageData<>();
        } catch (Exception e) {
            log.error("[{}] Failed to find edge events [{}]", tenantId, edge.getId(), e);
            return new PageData<>();
        }
    }

}
