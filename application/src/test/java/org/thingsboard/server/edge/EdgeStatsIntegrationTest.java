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
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.protobuf.AbstractMessage;
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
import org.thingsboard.server.gen.edge.v1.EntityDataProto;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.service.edge.stats.EdgeStatsService;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.thingsboard.server.dao.edge.stats.EdgeStatsKey.DOWNLINK_MSGS_ADDED;
import static org.thingsboard.server.dao.edge.stats.EdgeStatsKey.DOWNLINK_MSGS_PERMANENTLY_FAILED;
import static org.thingsboard.server.dao.edge.stats.EdgeStatsKey.DOWNLINK_MSGS_PUSHED;
import static org.thingsboard.server.dao.edge.stats.EdgeStatsKey.DOWNLINK_MSGS_TMP_FAILED;

@DaoSqlTest
@Slf4j
public class EdgeStatsIntegrationTest extends AbstractEdgeTest {

    private static final String STATISTICS_DEVICE_PROFILE = "STATISTICS";

    private final Map<EdgeStatsKey, Long> EXPECTED_EDGE_STATS = Map.of(
            DOWNLINK_MSGS_ADDED, 6L,
            DOWNLINK_MSGS_PUSHED, 6L,
            DOWNLINK_MSGS_PERMANENTLY_FAILED, 0L,
            DOWNLINK_MSGS_TMP_FAILED, 0L
    );

    private final Map<EdgeStatsKey, Long> EXPECTED_EMPTY_EDGE_STATS = Map.of(
            DOWNLINK_MSGS_ADDED, 0L,
            DOWNLINK_MSGS_PUSHED, 0L,
            DOWNLINK_MSGS_PERMANENTLY_FAILED, 0L,
            DOWNLINK_MSGS_TMP_FAILED, 0L
    );

    @Autowired
    private EdgeStatsService edgeStatsService;
    @Autowired
    private EdgeStatsCounterService statsCounterService;

    @Test
    public void testFullEdgeStatsCycle() throws Exception {
        // 1. Clear previous stats and prepare test data
        prepareTestData();

        // 2. Wait until Edge counters are updated
        awaitEdgeCountersUpdated();

        // 3. Report statistics
        edgeStatsService.reportStats();

        // 4. Wait until timeseries data is persisted
        awaitStatsMatch(EXPECTED_EDGE_STATS);

        // 5. Send statistics to Edge
        List<TsKvEntry> latestStatsEntries = tsService.findLatest(
                tenantId,
                edge.getId(),
                Arrays.stream(EdgeStatsKey.values()).map(EdgeStatsKey::getKey).toList()
        ).get();
        sendStatsToEdge(latestStatsEntries);

        // 6. Wait until telemetry Proto contains the expected stats
        await().atMost(10, TimeUnit.SECONDS).pollInterval(Duration.ofMillis(200)).untilAsserted(() -> {
            EntityDataProto latestMsg = getLatestEntityDataMessage();
            Map<String, Long> actualStats = toMap(latestMsg.getPostTelemetryMsg().getTsKvList(0));
            assertAllStatsEqual(EXPECTED_EDGE_STATS, actualStats, "Proto stats");
        });
    }

    @Test
    public void testNoMessagesFromEdge() throws ExecutionException, InterruptedException {
        // 1. Clear stats counters for the Edge
        statsCounterService.clear(edge.getId());

        // 2. Report stats with no data from the Edge
        edgeStatsService.reportStats();

        // 3. Verify that persisted timeseries contains only empty stats
        awaitStatsMatch(EXPECTED_EMPTY_EDGE_STATS);

        List<TsKvEntry> actual = tsService.findLatest(
                tenantId,
                edge.getId(),
                Arrays.stream(EdgeStatsKey.values()).map(EdgeStatsKey::getKey).toList()
        ).get();

        assertAllStatsEqual(EXPECTED_EMPTY_EDGE_STATS, toMap(actual), "Empty stats");
    }

    @Test
    public void testRepeatedReportStatsDoesNotDuplicate() throws ExecutionException, InterruptedException {
        // 1. Clear previous stats and prepare test data
        prepareTestData();

        // 2. Wait until Edge counters are updated
        awaitEdgeCountersUpdated();

        // 3. First report call
        edgeStatsService.reportStats();

        // 4. Verify that the persisted stats match expectations
        awaitStatsMatch(EXPECTED_EDGE_STATS);

        // 5. Remove persisted stats to simulate a re-report scenario
        tsService.removeLatest(
                tenantId,
                edge.getId(),
                Arrays.stream(EdgeStatsKey.values()).map(EdgeStatsKey::getKey).toList()
        );

        // 6. Second report call without increments (counters already cleared)
        edgeStatsService.reportStats();

        // 7. Verify that the stats are empty after the second report
        awaitStatsMatch(EXPECTED_EMPTY_EDGE_STATS);
    }

    private void awaitStatsMatch(Map<EdgeStatsKey, Long> expected) {
        await().atMost(10, TimeUnit.SECONDS).pollInterval(Duration.ofMillis(200)).untilAsserted(() -> {
            Map<String, Long> actualStats = fetchLatestStats();
            assertAllStatsEqual(expected, actualStats, "Timeseries stats");
        });
    }

    private Map<String, Long> fetchLatestStats() throws ExecutionException, InterruptedException {
        List<TsKvEntry> latestStatsEntries = tsService.findLatest(
                tenantId,
                edge.getId(),
                Arrays.stream(EdgeStatsKey.values()).map(EdgeStatsKey::getKey).toList()
        ).get();
        return toMap(latestStatsEntries);
    }

    private void prepareTestData() throws InterruptedException, ExecutionException {
        statsCounterService.clear(edge.getId());
        // 2 stats messages: ADDED Device Profile, ASSIGN Device
        edgeImitator.expectMessageAmount(4);
        Device device = saveDevice("StatisticDevice", STATISTICS_DEVICE_PROFILE);
        doPost("/api/edge/" + edge.getUuidId() + "/device/" + device.getUuidId(), Device.class);
        edgeImitator.waitForMessages();
        // 1 stats message: ASSIGN Asset
        edgeImitator.expectMessageAmount(2);
        Asset savedAsset = saveAsset("Edge Asset 2");
        doPost("/api/edge/" + edge.getUuidId()
                + "/asset/" + savedAsset.getUuidId(), Asset.class);
        Assert.assertTrue(edgeImitator.waitForMessages());

        // 2 stats messages: ADDED Customer, ASSIGN Customer
        edgeImitator.expectMessageAmount(1);
        Customer customer = new Customer();
        customer.setTitle("Edge Customer");
        Customer savedCustomer = doPost("/api/customer", customer, Customer.class);
        Assert.assertFalse(edgeImitator.waitForMessages(5));

        // assign edge to customer
        edgeImitator.expectMessageAmount(2);
        doPost("/api/customer/" + savedCustomer.getUuidId()
                + "/edge/" + edge.getUuidId(), Edge.class);
        Assert.assertTrue(edgeImitator.waitForMessages());

        // 1 stats message: Timeseries
        edgeImitator.expectMessageAmount(1);
        String timeseriesData = "{\"data\":{\"temperature\":25},\"ts\":" + System.currentTimeMillis() + "}";
        JsonNode timeseriesEntityData = JacksonUtil.toJsonNode(timeseriesData);
        EdgeEvent edgeEvent = constructEdgeEvent(tenantId, edge.getId(), EdgeEventActionType.TIMESERIES_UPDATED, device.getId().getId(), EdgeEventType.DEVICE, timeseriesEntityData);
        edgeEventService.saveAsync(edgeEvent).get();
        Assert.assertTrue(edgeImitator.waitForMessages());
    }

    private void assertAllStatsEqual(Map<EdgeStatsKey, Long> expected, Map<String, Long> actual, String context) {
        assertAll(context,
                expected.entrySet().stream()
                        .map(e -> () -> assertEquals(e.getValue(), actual.get(e.getKey().getKey()), "Mismatch for stat: " + e.getKey()))
        );
    }

    private void awaitEdgeCountersUpdated() {
        await().atMost(10, TimeUnit.SECONDS).pollInterval(Duration.ofMillis(200)).untilAsserted(() -> {
            MsgCounters counters = statsCounterService.getMsgCountersByEdge().get(edge.getId());
            Map<String, Long> actualCounters = toMap(
                    Map.entry(DOWNLINK_MSGS_ADDED.getKey(), () -> counters.getMsgsAdded().get()),
                    Map.entry(DOWNLINK_MSGS_PUSHED.getKey(), () -> counters.getMsgsPushed().get()),
                    Map.entry(DOWNLINK_MSGS_PERMANENTLY_FAILED.getKey(), () -> counters.getMsgsPermanentlyFailed().get()),
                    Map.entry(DOWNLINK_MSGS_TMP_FAILED.getKey(), () -> counters.getMsgsTmpFailed().get())
            );
            assertAllStatsEqual(EXPECTED_EDGE_STATS, actualCounters, "Edge counters");
        });
    }

    @SafeVarargs
    private Map<String, Long> toMap(Map.Entry<String, Supplier<Long>>... suppliers) {
        return Arrays.stream(suppliers)
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get()));
    }

    private Map<String, Long> toMap(List<TsKvEntry> stats) {
        return stats.stream()
                .collect(Collectors.toMap(TsKvEntry::getKey, e -> e.getLongValue().orElse(0L)));
    }

    private Map<String, Long> toMap(TransportProtos.TsKvListProto kvList) {
        Map<String, Long> map = kvList.getKvList().stream()
                .collect(Collectors.toMap(
                        TransportProtos.KeyValueProto::getKey,
                        TransportProtos.KeyValueProto::getLongV
                ));
        for (EdgeStatsKey key : EdgeStatsKey.values()) {
            map.putIfAbsent(key.getKey(), 0L);
        }
        return map;
    }

    private void sendStatsToEdge(List<TsKvEntry> stats) throws Exception {
        edgeImitator.expectMessageAmount(1);
        EdgeEvent edgeEvent = constructEdgeEvent(
                tenantId,
                edge.getId(),
                EdgeEventActionType.TIMESERIES_UPDATED,
                edge.getId().getId(),
                EdgeEventType.EDGE,
                buildStatsJson(System.currentTimeMillis(), stats)
        );
        edgeEventService.saveAsync(edgeEvent).get();
        assertTrue(edgeImitator.waitForMessages());
    }

    private EntityDataProto getLatestEntityDataMessage() {
        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        assertInstanceOf(EntityDataProto.class, latestMessage);
        EntityDataProto msg = (EntityDataProto) latestMessage;
        assertEquals(edge.getUuidId().getMostSignificantBits(), msg.getEntityIdMSB());
        assertEquals(edge.getUuidId().getLeastSignificantBits(), msg.getEntityIdLSB());
        assertEquals(edge.getId().getEntityType().name(), msg.getEntityType());
        assertTrue(msg.hasPostTelemetryMsg());
        return msg;
    }

    private ObjectNode buildStatsJson(long ts, List<TsKvEntry> statsEntries) {
        ObjectNode entityBody = JacksonUtil.newObjectNode();
        entityBody.put("ts", ts);
        ObjectNode data = JacksonUtil.newObjectNode();
        statsEntries.forEach(entry -> data.put(entry.getKey(), entry.getValueAsString()));
        entityBody.set("data", data);
        return entityBody;
    }

}
