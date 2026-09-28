// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent.msg.inbound;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.gen.agent.v1.AgentMetricsSync;
import org.thingsboard.server.service.agent.AgentInboundMsgCtx;
import org.thingsboard.server.service.agent.AgentMetricsTimeseriesService;

import java.util.ArrayList;
import java.util.List;

import static org.thingsboard.server.service.agent.AgentMetricsKeys.CPU_PERCENT;
import static org.thingsboard.server.service.agent.AgentMetricsKeys.DISK_BYTES;
import static org.thingsboard.server.service.agent.AgentMetricsKeys.HOST_DISK_TOTAL;
import static org.thingsboard.server.service.agent.AgentMetricsKeys.HOST_MEMORY_BYTES;
import static org.thingsboard.server.service.agent.AgentMetricsKeys.MEMORY_BYTES;
import static org.thingsboard.server.service.agent.AgentMetricsKeys.ONLINE_CPUS;

@Component
@TbCoreComponent
@Slf4j
@RequiredArgsConstructor
public class AgentMetricsSyncMessageHandler implements AgentInboundMessageHandler {

    private final AgentMetricsTimeseriesService metricsTsService;

    @Override
    public boolean canHandle(AgentInboundMsgCtx msgCtx) {
        return msgCtx.msg().hasAgentMetricsSync();
    }

    @Override
    public void handle(AgentInboundMsgCtx msgCtx) {
        TenantId tenantId = msgCtx.sessionState().getTenantId();
        AgentId agentId = msgCtx.sessionState().getAgentId();
        AgentMetricsSync metrics = msgCtx.msg().getAgentMetricsSync();

        long ts = System.currentTimeMillis();
        List<TsKvEntry> kvEntries = new ArrayList<>(6);
        kvEntries.add(new BasicTsKvEntry(ts, new DoubleDataEntry(CPU_PERCENT, metrics.getCpuPercent())));
        kvEntries.add(new BasicTsKvEntry(ts, new LongDataEntry(MEMORY_BYTES, metrics.getMemoryBytes())));
        kvEntries.add(new BasicTsKvEntry(ts, new LongDataEntry(ONLINE_CPUS, (long) metrics.getOnlineCpus())));
        if (metrics.hasHostMemoryBytes()) {
            kvEntries.add(new BasicTsKvEntry(ts, new LongDataEntry(HOST_MEMORY_BYTES, metrics.getHostMemoryBytes())));
        }
        if (metrics.hasHostDiskTotal()) {
            kvEntries.add(new BasicTsKvEntry(ts, new LongDataEntry(HOST_DISK_TOTAL, metrics.getHostDiskTotal())));
        }
        if (metrics.hasDiskBytes()) {
            kvEntries.add(new BasicTsKvEntry(ts, new LongDataEntry(DISK_BYTES, metrics.getDiskBytes())));
        }

        metricsTsService.save(tenantId, agentId, kvEntries, "agent " + agentId);
    }
}
