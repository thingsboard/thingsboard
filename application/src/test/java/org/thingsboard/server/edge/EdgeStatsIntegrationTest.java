/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.edge;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.dao.edge.stats.EdgeStatsCounterService;
import org.thingsboard.server.dao.edge.stats.EdgeStatsKey;
import org.thingsboard.server.dao.edge.stats.MsgCounters;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.service.edge.stats.EdgeStatsService;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.thingsboard.server.dao.edge.stats.EdgeStatsKey.DOWNLINK_MSGS_ADDED;
import static org.thingsboard.server.dao.edge.stats.EdgeStatsKey.DOWNLINK_MSGS_PERMANENTLY_FAILED;
import static org.thingsboard.server.dao.edge.stats.EdgeStatsKey.DOWNLINK_MSGS_PUSHED;
import static org.thingsboard.server.dao.edge.stats.EdgeStatsKey.DOWNLINK_MSGS_TMP_FAILED;

@DaoSqlTest
@Slf4j
public class EdgeStatsIntegrationTest extends AbstractEdgeTest {

    private static final String STATISTICS_DEVICE_PROFILE = "STATISTICS";

    private static final long EXPECTED_MSGS_ADDED = 6L;
    private static final long EXPECTED_MSGS_PUSHED = 6L;
    private static final long EXPECTED_MSGS_PERMANENTLY_FAILED = 0L;
    private static final long EXPECTED_MSGS_TMP_FAILED = 0L;

    @Autowired
    private EdgeStatsService edgeStatsService;
    @Autowired
    private EdgeStatsCounterService statsCounterService;

    @Test
    public void testReportStats() throws Exception {
        // GIVEN
        simulateEdgeEventsAddedDownlinkPushed();

        // Await Edge Counters Updated
        await().atMost(10, TimeUnit.SECONDS).pollInterval(Duration.ofMillis(200)).untilAsserted(() -> {
            MsgCounters counters = statsCounterService.getMsgCountersByEdge().get(edge.getId());
            assertEquals(EXPECTED_MSGS_ADDED, counters.getMsgsAdded().get());
            assertEquals(EXPECTED_MSGS_PUSHED, counters.getMsgsPushed().get());
            assertEquals(EXPECTED_MSGS_PERMANENTLY_FAILED, counters.getMsgsPermanentlyFailed().get());
            assertEquals(EXPECTED_MSGS_TMP_FAILED, counters.getMsgsTmpFailed().get());
        });

        // WHEN
        edgeStatsService.reportStats();

        // THEN
        await().atMost(10, TimeUnit.SECONDS).pollInterval(Duration.ofMillis(200)).untilAsserted(() -> {
            List<TsKvEntry> actualStats = fetchLatestStats();
            assertEquals(EXPECTED_MSGS_ADDED, getStatsLongValue(actualStats, DOWNLINK_MSGS_ADDED));
            assertEquals(EXPECTED_MSGS_PUSHED, getStatsLongValue(actualStats, DOWNLINK_MSGS_PUSHED));
            assertEquals(EXPECTED_MSGS_PERMANENTLY_FAILED, getStatsLongValue(actualStats, DOWNLINK_MSGS_PERMANENTLY_FAILED));
            assertEquals(EXPECTED_MSGS_TMP_FAILED, getStatsLongValue(actualStats, DOWNLINK_MSGS_TMP_FAILED));
        });
    }

    private long getStatsLongValue(List<TsKvEntry> stats, EdgeStatsKey key) {
        return stats.stream().filter(e -> e.getKey().equals(key.getKey())).findFirst().get().getLongValue().orElse(0L);
    }

    private List<TsKvEntry> fetchLatestStats() throws ExecutionException, InterruptedException {
        return tsService.findLatest(
                tenantId,
                edge.getId(),
                Arrays.stream(EdgeStatsKey.values()).map(EdgeStatsKey::getKey).toList()).get();
    }

    private void simulateEdgeEventsAddedDownlinkPushed() throws InterruptedException, ExecutionException {
        statsCounterService.clear(edge.getId());

        // Save device and assign to edge
        // 2 DOWNLINK_MSGS_ADDED, EdgeEvents: [{DEVICE_PROFILE: ADDED}, {DEVICE: ASSIGNED_TO_EDGE}]
        // 2 DOWNLINK_MSGS_PUSHED, Downlinks: [{deviceProfileUpdateMsg}, {deviceUpdateMsg, deviceProfileUpdateMsg, deviceCredentialsUpdateMsg}]
        edgeImitator.expectMessageAmount(4);
        Device savedDevice = saveDevice("StatisticDevice", STATISTICS_DEVICE_PROFILE);
        doPost("/api/edge/" + edge.getUuidId() + "/device/" + savedDevice.getUuidId(), Device.class);
        Assert.assertTrue(edgeImitator.waitForMessages());

        // Save asset and assign to edge
        // 1 DOWNLINK_MSGS_ADDED, EdgeEvents: [{ASSET: ASSIGNED_TO_EDGE}]
        // 1 DOWNLINK_MSGS_PUSHED, Downlinks: [{assetUpdateMsg, assetProfileUpdateMsg}]
        edgeImitator.expectMessageAmount(2);
        Asset savedAsset = saveAsset("Edge Asset");
        doPost("/api/edge/" + edge.getUuidId()
                + "/asset/" + savedAsset.getUuidId(), Asset.class);
        Assert.assertTrue(edgeImitator.waitForMessages());

        // Create customer and assign edge to the customer
        // 2 DOWNLINK_MSGS_ADDED, EdgeEvents: [{CUSTOMER: ADDED}, {EDGE: ASSIGNED_TO_CUSTOMER}]
        // 2 DOWNLINK_MSGS_PUSHED, Downlinks: [{customerUpdateMsg}, {edgeConfiguration}]
        edgeImitator.expectMessageAmount(2);
        Customer customer = new Customer();
        customer.setTitle("Edge Customer");
        Customer savedCustomer = doPost("/api/customer", customer, Customer.class);
        doPost("/api/customer/" + savedCustomer.getUuidId()
                + "/edge/" + edge.getUuidId(), Edge.class);
        Assert.assertTrue(edgeImitator.waitForMessages());

        // Send device telemetry downlink for the device
        // 1 DOWNLINK_MSGS_ADDED, EdgeEvents: [{DEVICE: TIMESERIES_UPDATED}]
        // 1 DOWNLINK_MSGS_PUSHED, Downlinks: [{entityData}]
        edgeImitator.expectMessageAmount(1);
        String timeseriesData = "{\"data\":{\"temperature\":25},\"ts\":" + System.currentTimeMillis() + "}";
        JsonNode timeseriesEntityData = JacksonUtil.toJsonNode(timeseriesData);
        EdgeEvent edgeEvent = constructEdgeEvent(tenantId, edge.getId(), EdgeEventActionType.TIMESERIES_UPDATED, savedDevice.getId().getId(), EdgeEventType.DEVICE, timeseriesEntityData);
        edgeEventService.saveAsync(edgeEvent).get();
        Assert.assertTrue(edgeImitator.waitForMessages());
    }
}
