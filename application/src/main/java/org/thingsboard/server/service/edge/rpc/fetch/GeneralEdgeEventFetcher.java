/**
 * Copyright © 2016-2024 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

@AllArgsConstructor
@Slf4j
public class GeneralEdgeEventFetcher implements EdgeEventFetcher {

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
                queueStartTs,
                System.currentTimeMillis());
    }

    @Override
    public PageData<EdgeEvent> fetchEdgeEvents(TenantId tenantId, Edge edge, PageLink pageLink) {
        try {
            log.trace("[{}] Finding general edge events [{}], seqIdStart = {}, pageLink = {}",
                    tenantId, edge.getId(), seqIdStart, pageLink);
            PageData<EdgeEvent> edgeEvents = edgeEventService.findEdgeEvents(tenantId, edge.getId(), seqIdStart, null, (TimePageLink) pageLink);
            if (edgeEvents.getData().isEmpty()) {
                edgeEvents = edgeEventService.findEdgeEvents(tenantId, edge.getId(), 0L, Math.max(this.maxReadRecordsCount, seqIdStart - this.maxReadRecordsCount), (TimePageLink) pageLink);
                if (edgeEvents.getData().stream().anyMatch(ee -> ee.getSeqId() < seqIdStart)) {
                    log.info("[{}] seqId column of edge_event table started new cycle [{}]", tenantId, edge.getId());
                    this.seqIdNewCycleStarted = true;
                    this.seqIdStart = 0L;
                } else {
                    edgeEvents = new PageData<>();
                    log.warn("[{}] unexpected edge notification message received. " +
                            "no new events found and seqId column of edge_event table doesn't started new cycle [{}]", tenantId, edge.getId());
                }
            }
            return edgeEvents;
        } catch (Exception e) {
            log.error("[{}] failed to find edge events [{}]", tenantId, edge.getId());
        }
        return new PageData<>();
    }

}
