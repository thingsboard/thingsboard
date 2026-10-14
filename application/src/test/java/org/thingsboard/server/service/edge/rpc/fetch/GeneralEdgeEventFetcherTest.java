// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.edge.rpc.fetch;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.edge.EdgeEventService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeneralEdgeEventFetcherTest {

    @Mock
    private EdgeEventService edgeEventService;

    private final TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
    private final Edge edge = new Edge(new EdgeId(UUID.randomUUID()));
    private final long queueStartTs = System.currentTimeMillis();

    @Test
    void youngEdgeIsNotANewCycle() {
        GeneralEdgeEventFetcher fetcher = fetcherWithLowSeqIdEventAt(queueStartTs - 30_000);

        PageData<EdgeEvent> edgeEvents = fetcher.fetchEdgeEvents(tenantId, edge, fetcher.getPageLink(50));

        assertThat(edgeEvents.getData()).isEmpty();
        assertThat(fetcher.isSeqIdNewCycleStarted()).isFalse();
    }

    @Test
    void wrappedSequenceIsANewCycle() {
        GeneralEdgeEventFetcher fetcher = fetcherWithLowSeqIdEventAt(queueStartTs);

        PageData<EdgeEvent> edgeEvents = fetcher.fetchEdgeEvents(tenantId, edge, fetcher.getPageLink(50));

        assertThat(edgeEvents.getData()).singleElement().extracting(EdgeEvent::getSeqId).isEqualTo(1L);
        assertThat(fetcher.isSeqIdNewCycleStarted()).isTrue();
    }

    private GeneralEdgeEventFetcher fetcherWithLowSeqIdEventAt(long createdTime) {
        EdgeEvent edgeEvent = new EdgeEvent();
        edgeEvent.setSeqId(1L);
        edgeEvent.setCreatedTime(createdTime);
        when(edgeEventService.findEdgeEvents(eq(tenantId), eq(edge.getId()), eq(51L), isNull(), any(TimePageLink.class)))
                .thenReturn(new PageData<>());
        when(edgeEventService.findEdgeEvents(eq(tenantId), eq(edge.getId()), eq(0L), eq(50L), any(TimePageLink.class)))
                .thenReturn(new PageData<>(List.of(edgeEvent), 1, 1, false));
        return new GeneralEdgeEventFetcher(queueStartTs, 51L, false, 50L, edgeEventService, 60_000L);
    }

}
