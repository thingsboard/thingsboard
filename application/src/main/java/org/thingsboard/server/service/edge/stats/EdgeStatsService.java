// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.edge.stats;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.edge.stats.EdgeStatsCounterService;
import org.thingsboard.server.dao.edge.stats.MsgCounters;
import org.thingsboard.server.gen.transport.TransportProtos.EdgeStatsMsgProto;
import org.thingsboard.server.gen.transport.TransportProtos.ToCoreMsg;

import java.util.HashMap;
import java.util.Map;

/**
 * Runs on every node that counts edge downlink stats (core and rule engine). Each node holds only a part of an edge's
 * counters, so instead of saving them it sends them to the edge's TB_CORE partition owner, which sums the parts
 * of all nodes and is the only writer of the edge's stats (see {@link EdgeStatsAggregationService}).
 */
@ConditionalOnProperty(prefix = "edges.stats", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@Service
@Slf4j
public class EdgeStatsService {

    private final EdgeStatsCounterService statsCounterService;
    private final TbClusterService clusterService;

    @Value("${edges.stats.report-interval-millis:600000}")
    private long reportIntervalMillis;

    @Scheduled(
            fixedDelayString = "${edges.stats.report-interval-millis:600000}",
            initialDelayString = "${edges.stats.report-interval-millis:600000}"
    )
    public void reportStats() {
        log.debug("Reporting Edge communication stats...");
        long now = System.currentTimeMillis();
        long ts = now - (now % reportIntervalMillis);

        Map<EdgeId, MsgCounters> countersByEdgeSnapshot = new HashMap<>(statsCounterService.getMsgCountersByEdge());
        countersByEdgeSnapshot.forEach((edgeId, counters) -> {
            try {
                TenantId tenantId = counters.getTenantId();
                EdgeStatsMsgProto statsMsg = EdgeStatsMsgProto.newBuilder()
                        .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                        .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                        .setEdgeIdMSB(edgeId.getId().getMostSignificantBits())
                        .setEdgeIdLSB(edgeId.getId().getLeastSignificantBits())
                        .setTs(ts)
                        .setMsgsAdded(counters.getMsgsAdded().get())
                        .setMsgsPushed(counters.getMsgsPushed().get())
                        .setMsgsPermanentlyFailed(counters.getMsgsPermanentlyFailed().get())
                        .setMsgsTmpFailed(counters.getMsgsTmpFailed().get())
                        .setMsgsLag(counters.getMsgsLag().get())
                        .setMsgsLagMeasured(counters.getMsgsLagMeasured().get())
                        .setMsgsLagTs(counters.getMsgsLagTs().get())
                        .build();
                log.trace("Reporting Edge communication stats: {} tenantId - {}, edgeId - {}", statsMsg, tenantId, edgeId);
                clusterService.pushMsgToCore(tenantId, edgeId, ToCoreMsg.newBuilder().setEdgeStatsMsg(statsMsg).build(), null);
            } catch (Exception e) {
                log.warn("Failed to report edge communication stats for edge: {}", edgeId, e);
            } finally {
                statsCounterService.clear(edgeId);
            }
        });
    }

}
