// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent.compose;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.agent.AgentAppUnit;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.common.data.agent.AgentAppUnitType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.gen.agent.v1.ContainerInfo;
import org.thingsboard.server.gen.agent.v1.VolumeInfo;
import org.thingsboard.server.service.agent.AgentMetricsTimeseriesService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.thingsboard.server.service.agent.AgentMetricsKeys.CPU_PERCENT;
import static org.thingsboard.server.service.agent.AgentMetricsKeys.MEMORY_BYTES;
import static org.thingsboard.server.service.agent.AgentMetricsKeys.SIZE_BYTES;

@Service
@TbCoreComponent
@Slf4j
@RequiredArgsConstructor
public class ComposeUnitMetricsRecorder {

    private final AgentMetricsTimeseriesService metricsTsService;

    public void record(TenantId tenantId, Map<AgentAppUnitKey, AgentAppUnit> units,
                       Map<String, ContainerInfo> containerStates,
                       Map<String, VolumeInfo> volumeStates) {
        long ts = System.currentTimeMillis();
        for (var entry : containerStates.entrySet()) {
            AgentAppUnit unit = units.get(new AgentAppUnitKey(AgentAppUnitType.CONTAINER, entry.getKey()));
            if (unit == null) {
                continue;
            }
            ContainerInfo info = entry.getValue();
            List<TsKvEntry> kvEntries = new ArrayList<>(2);
            if (info.hasCpuPercent()) {
                kvEntries.add(new BasicTsKvEntry(ts, new DoubleDataEntry(CPU_PERCENT, info.getCpuPercent())));
            }
            if (info.hasMemoryBytes()) {
                kvEntries.add(new BasicTsKvEntry(ts, new LongDataEntry(MEMORY_BYTES, info.getMemoryBytes())));
            }
            if (kvEntries.isEmpty()) {
                continue;
            }
            metricsTsService.save(tenantId, unit.getId(), kvEntries, "unit " + unit.getId());
        }
        for (var entry : volumeStates.entrySet()) {
            AgentAppUnit unit = units.get(new AgentAppUnitKey(AgentAppUnitType.VOLUME, entry.getKey()));
            if (unit == null) {
                continue;
            }
            VolumeInfo info = entry.getValue();
            if (!info.hasSizeBytes()) {
                continue;
            }
            List<TsKvEntry> kvEntries = List.of(
                    new BasicTsKvEntry(ts, new LongDataEntry(SIZE_BYTES, info.getSizeBytes())));
            metricsTsService.save(tenantId, unit.getId(), kvEntries, "volume " + unit.getId());
        }
    }
}
