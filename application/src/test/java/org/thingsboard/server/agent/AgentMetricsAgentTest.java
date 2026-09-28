// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.agent;

import org.awaitility.Awaitility;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.gen.agent.v1.AgentMetricsSync;
import org.thingsboard.server.service.agent.AgentMetricsKeys;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The metrics half of the uplink protocol: six proto fields, three of them {@code optional}, mapped onto the
 * agent's own timeseries. Only an end-to-end push catches a field wired to the wrong {@code hasX()} guard or
 * a key mapped to the wrong metric name.
 */
@DaoSqlTest
public class AgentMetricsAgentTest extends AbstractAgentTest {

    @Autowired
    TimeseriesService timeseriesService;

    @Test
    public void testAgentMetricsSyncPersistsEverySeries() throws Exception {
        agentImitator.sendAgentMetricsSync(AgentMetricsSync.newBuilder()
                .setCpuPercent(12.5)
                .setMemoryBytes(2048L)
                .setOnlineCpus(4)
                .setHostMemoryBytes(8192L)
                .setHostDiskTotal(500_000L)
                .setDiskBytes(1024L)
                .build());

        Map<String, TsKvEntry> series = awaitSeries(AgentMetricsKeys.CPU_PERCENT, AgentMetricsKeys.MEMORY_BYTES,
                AgentMetricsKeys.ONLINE_CPUS, AgentMetricsKeys.HOST_MEMORY_BYTES,
                AgentMetricsKeys.HOST_DISK_TOTAL, AgentMetricsKeys.DISK_BYTES);

        Assert.assertEquals(12.5, series.get(AgentMetricsKeys.CPU_PERCENT).getDoubleValue().orElseThrow(), 0.0001);
        Assert.assertEquals(Long.valueOf(2048L), series.get(AgentMetricsKeys.MEMORY_BYTES).getLongValue().orElseThrow());
        Assert.assertEquals(Long.valueOf(4L), series.get(AgentMetricsKeys.ONLINE_CPUS).getLongValue().orElseThrow());
        Assert.assertEquals(Long.valueOf(8192L), series.get(AgentMetricsKeys.HOST_MEMORY_BYTES).getLongValue().orElseThrow());
        Assert.assertEquals(Long.valueOf(500_000L), series.get(AgentMetricsKeys.HOST_DISK_TOTAL).getLongValue().orElseThrow());
        Assert.assertEquals(Long.valueOf(1024L), series.get(AgentMetricsKeys.DISK_BYTES).getLongValue().orElseThrow());
    }

    @Test
    public void testAgentMetricsSyncSkipsAbsentOptionalFields() throws Exception {
        agentImitator.sendAgentMetricsSync(AgentMetricsSync.newBuilder()
                .setCpuPercent(1.5)
                .setMemoryBytes(64L)
                .setOnlineCpus(2)
                .build());

        awaitSeries(AgentMetricsKeys.CPU_PERCENT, AgentMetricsKeys.MEMORY_BYTES, AgentMetricsKeys.ONLINE_CPUS);

        List<TsKvEntry> optional = timeseriesService.findLatest(tenantId, agent.getId(),
                List.of(AgentMetricsKeys.HOST_MEMORY_BYTES, AgentMetricsKeys.HOST_DISK_TOTAL,
                        AgentMetricsKeys.DISK_BYTES)).get();

        Assert.assertTrue("absent optional fields must not be persisted",
                optional.stream().allMatch(entry -> entry.getValue() == null));
    }

    private Map<String, TsKvEntry> awaitSeries(String... keys) throws Exception {
        List<String> requested = List.of(keys);
        Awaitility.await()
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> timeseriesService.findLatest(tenantId, agent.getId(), requested).get()
                        .stream().allMatch(entry -> entry.getValue() != null));
        return timeseriesService.findLatest(tenantId, agent.getId(), requested).get()
                .stream().collect(Collectors.toMap(TsKvEntry::getKey, Function.identity()));
    }
}
