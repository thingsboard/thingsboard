// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.msa.monitoring;

import com.fasterxml.jackson.databind.JsonNode;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.msa.AbstractContainerTest;
import org.thingsboard.server.msa.DisableUIListeners;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.thingsboard.server.msa.prototypes.DevicePrototypes.defaultDevicePrototype;

@DisableUIListeners
public class RuleEngineStatsTest extends AbstractContainerTest {

    private static final int MESSAGE_COUNT = 10;

    private Device device;

    @BeforeMethod
    public void setUp() {
        testRestClient.login("tenant@thingsboard.org", "tenant");
        device = testRestClient.postDevice("", defaultDevicePrototype("rule_engine_stats_"));
    }

    @AfterMethod
    public void tearDown() {
        testRestClient.deleteDeviceIfExists(device.getId());
    }

    // Statistics count every message, traced or not, so this does not depend on the trace coverage setting.
    // Other tests share the tenant, so the totals are checked as lower bounds.
    @Test
    public void givenStatsEnabled_whenTelemetryIsPosted_thenRuleNodeAndQueueStatsAreCounted() {
        String accessToken = testRestClient.getDeviceCredentialsByDeviceId(device.getId()).getCredentialsId();
        long startTs = System.currentTimeMillis();

        for (int i = 0; i < MESSAGE_COUNT; i++) {
            testRestClient.postTelemetry(accessToken, JacksonUtil.toJsonNode(createPayload().toString()));
        }

        await().alias("telemetry -> rule node stats flushed").atMost(2 * TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(2, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    JsonNode rows = testRestClient.getRuleNodeStatsTable(startTs, System.currentTimeMillis());
                    assertThat(sum(rows, "execCount")).isGreaterThanOrEqualTo(MESSAGE_COUNT);
                });

        await().alias("telemetry -> queue stats flushed").atMost(2 * TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(2, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    JsonNode rows = testRestClient.getQueueStatsTable(startTs, System.currentTimeMillis());
                    assertThat(sum(rows, "successCount")).isGreaterThanOrEqualTo(MESSAGE_COUNT);
                });
    }

    private static long sum(JsonNode rows, String field) {
        long total = 0;
        for (JsonNode row : rows) {
            total += row.path(field).asLong();
        }
        return total;
    }
}
