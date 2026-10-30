// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.edge;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.edge.stats.EdgeStatsCounterService;
import org.thingsboard.server.gen.transport.TransportProtos.EdgeStatsMsgProto;
import org.thingsboard.server.gen.transport.TransportProtos.ToCoreMsg;
import org.thingsboard.server.service.edge.stats.EdgeStatsService;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.thingsboard.server.dao.edge.stats.EdgeStatsKey.DOWNLINK_MSGS_ADDED;
import static org.thingsboard.server.dao.edge.stats.EdgeStatsKey.DOWNLINK_MSGS_LAG;
import static org.thingsboard.server.dao.edge.stats.EdgeStatsKey.DOWNLINK_MSGS_PERMANENTLY_FAILED;
import static org.thingsboard.server.dao.edge.stats.EdgeStatsKey.DOWNLINK_MSGS_PUSHED;
import static org.thingsboard.server.dao.edge.stats.EdgeStatsKey.DOWNLINK_MSGS_TMP_FAILED;

@ExtendWith(MockitoExtension.class)
public class EdgeStatsTest {

    private static final long REPORT_INTERVAL_MILLIS = 600_000L;

    @Mock
    private TbClusterService clusterService;
    @Captor
    private ArgumentCaptor<ToCoreMsg> captor;

    private EdgeStatsCounterService counterService;
    private EdgeStatsService edgeStatsService;

    private final TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
    private final EdgeId edgeId = new EdgeId(UUID.randomUUID());

    @BeforeEach
    void setUp() {
        counterService = new EdgeStatsCounterService();
        edgeStatsService = new EdgeStatsService(counterService, clusterService);
        ReflectionTestUtils.setField(edgeStatsService, "reportIntervalMillis", REPORT_INTERVAL_MILLIS);
    }

    @Test
    public void testReportStatsSendsCountersToEdgePartitionOwner() {
        // GIVEN
        counterService.recordEvent(DOWNLINK_MSGS_ADDED, tenantId, edgeId, 5);
        counterService.recordEvent(DOWNLINK_MSGS_PUSHED, tenantId, edgeId, 3);
        counterService.recordEvent(DOWNLINK_MSGS_PERMANENTLY_FAILED, tenantId, edgeId, 1);
        counterService.recordEvent(DOWNLINK_MSGS_TMP_FAILED, tenantId, edgeId, 2);
        counterService.recordEvent(DOWNLINK_MSGS_LAG, tenantId, edgeId, 10);
        long before = System.currentTimeMillis();

        // WHEN
        edgeStatsService.reportStats();

        // THEN
        EdgeStatsMsgProto msg = captureSentMsg();
        Assertions.assertEquals(tenantId.getId(), new UUID(msg.getTenantIdMSB(), msg.getTenantIdLSB()));
        Assertions.assertEquals(edgeId.getId(), new UUID(msg.getEdgeIdMSB(), msg.getEdgeIdLSB()));
        Assertions.assertEquals(before - before % REPORT_INTERVAL_MILLIS, msg.getTs());
        Assertions.assertEquals(5, msg.getMsgsAdded());
        Assertions.assertEquals(3, msg.getMsgsPushed());
        Assertions.assertEquals(1, msg.getMsgsPermanentlyFailed());
        Assertions.assertEquals(2, msg.getMsgsTmpFailed());
        Assertions.assertEquals(10, msg.getMsgsLag());
        Assertions.assertTrue(msg.getMsgsLagMeasured());
        Assertions.assertTrue(msg.getMsgsLagTs() > 0 && msg.getMsgsLagTs() <= before);
        Assertions.assertTrue(counterService.getMsgCountersByEdge().isEmpty());
    }

    @Test
    public void testReportStatsSendsLagNotMeasuredWhenNoSession() {
        // GIVEN: events added while the edge is offline, nobody measured the lag
        counterService.recordEvent(DOWNLINK_MSGS_ADDED, tenantId, edgeId, 5);

        // WHEN
        edgeStatsService.reportStats();

        // THEN
        EdgeStatsMsgProto msg = captureSentMsg();
        Assertions.assertEquals(5, msg.getMsgsAdded());
        Assertions.assertFalse(msg.getMsgsLagMeasured());
    }

    @Test
    public void testReportStatsSendsLagNotMeasuredAfterSessionDisconnect() {
        // GIVEN: a session measured lag 0 earlier in the interval, then the edge disconnected and new events were added
        counterService.recordEvent(DOWNLINK_MSGS_LAG, tenantId, edgeId, 0);
        counterService.resetLagMeasured(edgeId);
        counterService.recordEvent(DOWNLINK_MSGS_ADDED, tenantId, edgeId, 5);

        // WHEN
        edgeStatsService.reportStats();

        // THEN
        EdgeStatsMsgProto msg = captureSentMsg();
        Assertions.assertEquals(5, msg.getMsgsAdded());
        Assertions.assertFalse(msg.getMsgsLagMeasured());
    }

    @Test
    public void testReportStatsSendsNothingWithoutCounters() {
        edgeStatsService.reportStats();

        verify(clusterService, never()).pushMsgToCore(any(TenantId.class), any(), any(ToCoreMsg.class), any());
    }

    @Test
    public void testResetLagMeasuredDoesNotCreateCounters() {
        counterService.resetLagMeasured(edgeId);

        Assertions.assertFalse(counterService.getMsgCountersByEdge().containsKey(edgeId));
    }

    private EdgeStatsMsgProto captureSentMsg() {
        verify(clusterService).pushMsgToCore(eq(tenantId), eq(edgeId), captor.capture(), isNull());
        Assertions.assertTrue(captor.getValue().hasEdgeStatsMsg());
        return captor.getValue().getEdgeStatsMsg();
    }

}
