// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent.compose;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.gen.agent.v1.ApplicationMetrics;
import org.thingsboard.server.service.agent.AgentMetricsTimeseriesService;

import java.util.ArrayList;
import java.util.List;

import static org.thingsboard.server.service.agent.AgentMetricsKeys.CPU_PERCENT;
import static org.thingsboard.server.service.agent.AgentMetricsKeys.MEMORY_BYTES;
import static org.thingsboard.server.service.agent.AgentMetricsKeys.VOLUME_BYTES;

@Service
@TbCoreComponent
@Slf4j
@RequiredArgsConstructor
public class ComposeApplicationMetricsRecorder {

    private final AgentMetricsTimeseriesService metricsTsService;

    public void record(TenantId tenantId, AgentApplicationId appId, ApplicationMetrics metrics) {
        long ts = System.currentTimeMillis();
        List<TsKvEntry> kvEntries = new ArrayList<>(3);
        kvEntries.add(new BasicTsKvEntry(ts, new DoubleDataEntry(CPU_PERCENT, metrics.getCpuPercent())));
        kvEntries.add(new BasicTsKvEntry(ts, new LongDataEntry(MEMORY_BYTES, metrics.getMemoryBytes())));
        if (metrics.hasVolumeBytes()) {
            kvEntries.add(new BasicTsKvEntry(ts, new LongDataEntry(VOLUME_BYTES, metrics.getVolumeBytes())));
        }
        metricsTsService.save(tenantId, appId, kvEntries, "app " + appId);
    }
}
