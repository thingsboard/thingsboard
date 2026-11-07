// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.msa.monitoring;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
public class RuleChainTracingTest extends AbstractContainerTest {

    private Device device;
    private JsonNode originalCoverage;

    @BeforeMethod
    public void setUp() {
        testRestClient.login("tenant@thingsboard.org", "tenant");
        device = testRestClient.postDevice("", defaultDevicePrototype("tracing_"));
        originalCoverage = testRestClient.getTraceCoverageSetting();
    }

    @AfterMethod
    public void tearDown() {
        testRestClient.saveTraceCoverageSetting(originalCoverage);
        testRestClient.deleteDeviceIfExists(device.getId());
    }

    @Test
    public void givenTracingEnabled_whenTelemetryIsPosted_thenTraceWithSpansIsStored() {
        ObjectNode coverage = originalCoverage.deepCopy();
        coverage.put("enabled", true);
        coverage.put("tracesPerInterval", 100);
        coverage.put("tracesPerPack", 100);
        coverage.put("intervalSeconds", 1);
        JsonNode saved = testRestClient.saveTraceCoverageSetting(coverage);
        assertThat(saved.get("enabled").asBoolean()).isTrue();

        String accessToken = testRestClient.getDeviceCredentialsByDeviceId(device.getId()).getCredentialsId();
        long startTime = System.currentTimeMillis();

        // The rule engine nodes reload the cached coverage setting once a minute, so keep sending until a message is
        // traced.
        JsonNode trace = await().alias("telemetry -> trace stored").atMost(4 * TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(2, TimeUnit.SECONDS)
                .until(() -> {
                    testRestClient.postTelemetry(accessToken, JacksonUtil.toJsonNode(createPayload().toString()));
                    JsonNode traces = testRestClient.searchTraces(device.getId(), startTime, System.currentTimeMillis());
                    return traces.get("data").isEmpty() ? null : traces.get("data").get(0);
                }, t -> t != null);

        String traceId = trace.get("traceId").asText();
        assertThat(trace.get("spanCount").asInt()).isPositive();
        assertThat(trace.get("hasErrors").asBoolean()).isFalse();

        JsonNode spans = testRestClient.getTraceSpans(traceId);
        assertThat(spans).isNotEmpty();
        spans.forEach(span -> {
            assertThat(span.get("traceId").asText()).isEqualTo(traceId);
            assertThat(span.get("name").asText()).isNotBlank();
        });
    }
}
