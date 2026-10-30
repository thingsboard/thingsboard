// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.msa.edge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.RandomStringUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.group.EntityGroupInfo;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.msa.AbstractContainerTest;
import org.thingsboard.server.msa.DisableUIListeners;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.thingsboard.server.msa.ui.utils.EntityPrototypes.defaultTenantAdmin;
import static org.thingsboard.server.msa.ui.utils.EntityPrototypes.defaultTenantPrototype;

/**
 * Edge downlink stats are counted on the node that creates the edge event: the TB_CORE partition owner of the changed
 * entity, so updates of different devices are counted on different core nodes. Each node sends its part to the edge's
 * partition owner, which sums them and is the only writer of the edge stats. Without that, the nodes saved their own
 * parts at the same interval ts and overwrote each other, so the total of downlinkMsgsAdded came out too low.
 * <p>
 * The suite runs tb-core1/2 and tb-rule-engine1/2 with EDGES_STATS_REPORT_INTERVAL_MS=10000 and
 * EDGES_STATS_FLUSH_INTERVAL_MS=5000 (see ContainerTestSuite). There is no edge container, so only downlinkMsgsAdded,
 * the counter spread across nodes, is checked.
 */
@DisableUIListeners
public class EdgeStatsClusterTest extends AbstractContainerTest {

    private static final String DOWNLINK_MSGS_ADDED = "downlinkMsgsAdded";
    private static final int DEVICES = 20;
    private static final int UPDATES_PER_DEVICE = 3;
    // longer than one report interval (10 s) plus one flush interval (5 s)
    private static final long STABLE_PERIOD_MS = TimeUnit.SECONDS.toMillis(20);

    private TenantId tenantId;
    private Edge edge;
    private final List<Device> devices = new ArrayList<>();
    private long startTs;

    @BeforeClass
    public void beforeClass() {
        testRestClient.login(SYS_ADMIN_EMAIL, SYS_ADMIN_PASSWORD);
        tenantId = testRestClient.postTenant(defaultTenantPrototype("Edge Stats Cluster Tenant")).getId();
        testRestClient.createUserAndLogin(defaultTenantAdmin(tenantId, "edgeStatsCluster" + RandomStringUtils.secure().nextAlphabetic(5).toLowerCase() + "@thingsboard.org"), "tenant");

        startTs = System.currentTimeMillis();
        edge = testRestClient.postEdge(edgePrototype("Edge Stats Cluster Edge"));
        // edge events are dropped for an edge that never connected, so mark it as connected
        testRestClient.postTelemetryAttribute(edge.getId(), AttributeScope.SERVER_SCOPE, JacksonUtil.newObjectNode().put("active", true));

        EntityGroup deviceGroup = new EntityGroup();
        deviceGroup.setName("Edge Stats Cluster Devices");
        deviceGroup.setType(EntityType.DEVICE);
        EntityGroupInfo savedGroup = testRestClient.postEntityGroup(deviceGroup);
        for (int i = 0; i < DEVICES; i++) {
            Device device = new Device();
            device.setName("Edge Stats Cluster Device " + i);
            device.setType("default");
            devices.add(testRestClient.postDevice(RandomStringUtils.secure().nextAlphanumeric(20), device, savedGroup.getId()));
        }
        testRestClient.assignEntityGroupToEdge(edge.getId(), savedGroup.getId(), EntityType.DEVICE);
    }

    @AfterClass
    public void afterClass() {
        testRestClient.resetToken();
        testRestClient.login(SYS_ADMIN_EMAIL, SYS_ADMIN_PASSWORD);
        testRestClient.deleteTenant(tenantId);
    }

    @Test
    public void testDownlinkMsgsAddedIsSummedAcrossClusterNodes() {
        // events of the setup (group assignment, devices) are reported and saved first
        long baseline = awaitStableAddedTotal();

        for (int round = 0; round < UPDATES_PER_DEVICE; round++) {
            for (int i = 0; i < devices.size(); i++) {
                Device device = devices.get(i);
                device.setLabel("label-" + round);
                devices.set(i, testRestClient.updateDevice(device));
            }
        }

        // every device update adds exactly one edge event; parts counted on different nodes must add up, not overwrite
        long expected = baseline + (long) DEVICES * UPDATES_PER_DEVICE;
        await().atMost(2, TimeUnit.MINUTES).pollInterval(5, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(getAddedTotal()).isEqualTo(expected));
        assertThat(awaitStableAddedTotal()).isEqualTo(expected);
    }

    private long awaitStableAddedTotal() {
        AtomicLong lastTotal = new AtomicLong(-1);
        AtomicLong lastChangeTs = new AtomicLong(System.currentTimeMillis());
        await().atMost(2, TimeUnit.MINUTES).pollInterval(5, TimeUnit.SECONDS).until(() -> {
            long total = getAddedTotal();
            if (total != lastTotal.getAndSet(total)) {
                lastChangeTs.set(System.currentTimeMillis());
                return false;
            }
            return System.currentTimeMillis() - lastChangeTs.get() >= STABLE_PERIOD_MS;
        });
        return lastTotal.get();
    }

    private long getAddedTotal() {
        long endTs = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(1);
        ObjectNode response = testRestClient.getAggregatedTimeSeries(edge.getId(), DOWNLINK_MSGS_ADDED,
                startTs - TimeUnit.MINUTES.toMillis(1), endTs, endTs - startTs + TimeUnit.MINUTES.toMillis(1), Aggregation.SUM);
        JsonNode values = response.get(DOWNLINK_MSGS_ADDED);
        if (values == null || values.isEmpty()) {
            return 0;
        }
        long total = 0;
        for (JsonNode value : values) {
            // aggregated values come back as strings, possibly with a fraction part ("60.0")
            total += Math.round(value.get("value").asDouble());
        }
        return total;
    }

    private Edge edgePrototype(String name) {
        Edge edge = new Edge();
        edge.setName(name);
        edge.setType("default");
        edge.setRoutingKey(RandomStringUtils.secure().nextAlphanumeric(20));
        edge.setSecret(RandomStringUtils.secure().nextAlphanumeric(20));
        edge.setEdgeLicenseKey(RandomStringUtils.secure().nextAlphanumeric(20));
        edge.setCloudEndpoint("http://localhost:8080");
        return edge;
    }

}
