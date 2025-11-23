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
package org.thingsboard.server.cf;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.ArgumentType;
import org.thingsboard.server.common.data.cf.configuration.Output;
import org.thingsboard.server.common.data.cf.configuration.ReferencedEntityKey;
import org.thingsboard.server.common.data.cf.configuration.TimeSeriesOutput;
import org.thingsboard.server.common.data.cf.configuration.aggregation.AggFunction;
import org.thingsboard.server.common.data.cf.configuration.aggregation.AggKeyInput;
import org.thingsboard.server.common.data.cf.configuration.aggregation.AggMetric;
import org.thingsboard.server.common.data.cf.configuration.aggregation.single.EntityAggregationCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.aggregation.single.interval.AggInterval;
import org.thingsboard.server.common.data.cf.configuration.aggregation.single.interval.CustomInterval;
import org.thingsboard.server.common.data.cf.configuration.aggregation.single.interval.Watermark;
import org.thingsboard.server.common.data.debug.DebugSettings;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.thingsboard.server.cf.CalculatedFieldIntegrationTest.POLL_INTERVAL;

@DaoSqlTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestPropertySource(properties = {
        "actors.calculated_fields.check_interval=1"
})
public class EntityAggregationCalculatedFieldTest extends AbstractControllerTest {

    private Tenant savedTenant;

    @Before
    public void beforeEach() throws Exception {
        loginSysAdmin();

        updateDefaultTenantProfileConfig(tenantProfileConfig -> {
            tenantProfileConfig.setMinAllowedDeduplicationIntervalInSecForCF(1);
            tenantProfileConfig.setMinAllowedAggregationIntervalInSecForCF(1);
        });

        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        savedTenant = saveTenant(tenant);
        assertThat(savedTenant).isNotNull();

        User tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(savedTenant.getId());
        tenantAdmin.setEmail("tenant@thingsboard.org");
        tenantAdmin.setFirstName("John");
        tenantAdmin.setLastName("Doe");

        createUserAndLogin(tenantAdmin, "testPassword");
    }

    @After
    public void afterTest() throws Exception {
        loginSysAdmin();

        deleteTenant(savedTenant.getId());
    }

    @Test
    public void testCreateCfAndNoTelemetryDuringInterval_checkAggregation() throws Exception {
        Device device = createDevice("Device", "1234567890111");

        CustomInterval customInterval = new CustomInterval("Europe/Kyiv", 0L, 5L);
        long intervalEndTs = customInterval.getCurrentIntervalEndTs();

        CalculatedField totalConsumptionCF = createTotalConsumptionCF(device.getId(), customInterval, null);
        long interval = customInterval.getCurrentIntervalDurationMillis();

        await().alias("create CF and no telemetry during interval -> save metric with default value")
                .atMost(2 * interval, TimeUnit.MILLISECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode result = getLatestTelemetry(device.getId(), "consumption");
                    assertThat(result).isNotNull();
                    assertThat(result.get("consumption").get(0).get("value").asText()).isEqualTo("9999");
                });
    }

    @Test
    public void testCreateCfWithoutWatermark_checkAggregation() throws Exception {
        Device device = createDevice("Device", "1234567890111");

        CustomInterval customInterval = new CustomInterval("Europe/Kyiv", 0L, 5L);
        long currentIntervalStartTs = customInterval.getCurrentIntervalStartTs();
        long currentIntervalEndTs = customInterval.getCurrentIntervalEndTs();

        long tsBeforeInterval = currentIntervalStartTs - 1000;
        long tsInInterval_1 = currentIntervalStartTs + 1000;
        long tsInInterval_2 = currentIntervalStartTs + 500;
        long tsInInterval_3 = currentIntervalStartTs + 200;
        postTelemetry(device.getId(), String.format("{\"ts\": \"%s\", \"values\": {\"energy\":120}}", tsBeforeInterval));
        postTelemetry(device.getId(), String.format("{\"ts\": \"%s\", \"values\": {\"energy\":100}}", tsInInterval_1));
        postTelemetry(device.getId(), String.format("{\"ts\": \"%s\", \"values\": {\"energy\":180}}", tsInInterval_2));
        postTelemetry(device.getId(), String.format("{\"ts\": \"%s\", \"values\": {\"energy\":120}}", tsInInterval_3));

        long interval = customInterval.getCurrentIntervalDurationMillis();
        CalculatedField totalConsumptionCF = createTotalConsumptionCF(device.getId(), customInterval, null);

        await().alias("create CF -> perform aggregation after interval end")
                .atMost(2 * interval, TimeUnit.MILLISECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode result = getLatestTelemetry(device.getId(), "consumption");
                    assertThat(result).isNotNull();
                    assertThat(result.get("consumption").get(0).get("value").asText()).isEqualTo("400");
                });

        postTelemetry(device.getId(), String.format("{\"ts\": \"%s\", \"values\": {\"energy\":500}}", tsInInterval_1));

        await().alias("update telemetry that belongs to previous interval -> no aggregation since watermark is not set ")
                .atMost(2 * interval, TimeUnit.MILLISECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode result = getLatestTelemetry(device.getId(), "consumption");
                    assertThat(result).isNotNull();
                    assertThat(result.get("consumption").get(0).get("value").asText()).isEqualTo("400");
                });
    }

    @Test
    public void testCreateCfWithWatermark_checkAggregationDuringWatermark() throws Exception {
        Device device = createDevice("Device", "1234567890111");

        CustomInterval customInterval = new CustomInterval("Europe/Kyiv", 0L, 5L);
        long currentIntervalStartTs = customInterval.getCurrentIntervalStartTs();
        long currentIntervalEndTs = customInterval.getCurrentIntervalEndTs();

        long tsBeforeInterval = currentIntervalStartTs - 1000L;
        long tsInInterval_1 = currentIntervalStartTs + 1000L;
        long tsInInterval_2 = currentIntervalStartTs + 500L;
        long tsInInterval_3 = currentIntervalStartTs + 200L;
        postTelemetry(device.getId(), String.format("{\"ts\": \"%s\", \"values\": {\"energy\":120}}", tsBeforeInterval));
        postTelemetry(device.getId(), String.format("{\"ts\": \"%s\", \"values\": {\"energy\":100}}", tsInInterval_1));
        postTelemetry(device.getId(), String.format("{\"ts\": \"%s\", \"values\": {\"energy\":180}}", tsInInterval_2));
        postTelemetry(device.getId(), String.format("{\"ts\": \"%s\", \"values\": {\"energy\":120}}", tsInInterval_3));

        long interval = customInterval.getCurrentIntervalDurationMillis();
        Watermark watermark = new Watermark(10);
        CalculatedField totalConsumptionCF = createTotalConsumptionCF(device.getId(), customInterval, watermark);

        await().alias("create CF -> perform aggregation after interval end")
                .atMost(2 * interval, TimeUnit.MILLISECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode result = getLatestTelemetry(device.getId(), "consumption");
                    assertThat(result).isNotNull();
                    assertThat(result.get("consumption").get(0).get("value").asText()).isEqualTo("400");
                });

        postTelemetry(device.getId(), String.format("{\"ts\": \"%s\", \"values\": {\"energy\":300}}", tsInInterval_1));

        await().alias("update telemetry during watermark -> perform aggregation")
                .atMost(2 * 10, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode result = getLatestTelemetry(device.getId(), "consumption");
                    assertThat(result).isNotNull();
                    assertThat(result.get("consumption").get(0).get("value").asText()).isEqualTo("600");
                });
    }

    private CalculatedField createTotalConsumptionCF(EntityId entityId, AggInterval aggInterval, Watermark watermark) {
        Map<String, Argument> arguments = new HashMap<>();
        Argument argument = new Argument();
        argument.setRefEntityKey(new ReferencedEntityKey("energy", ArgumentType.TS_LATEST, null));
        arguments.put("en", argument);

        Map<String, AggMetric> aggMetrics = new HashMap<>();

        AggMetric consumption = new AggMetric();
        consumption.setFunction(AggFunction.SUM);
        consumption.setInput(new AggKeyInput("en"));
        consumption.setDefaultValue(9999L);
        aggMetrics.put("consumption", consumption);

        TimeSeriesOutput output = new TimeSeriesOutput();
        output.setDecimalsByDefault(0);

        return createAggCf("Consumption per minute", entityId,
                aggInterval,
                watermark,
                arguments,
                aggMetrics,
                output);
    }

    private CalculatedField createAggCf(String name,
                                        EntityId entityId,
                                        AggInterval aggInterval,
                                        Watermark watermark,
                                        Map<String, Argument> inputs,
                                        Map<String, AggMetric> metrics,
                                        Output output) {
        CalculatedField calculatedField = new CalculatedField();
        calculatedField.setName(name);
        calculatedField.setEntityId(entityId);
        calculatedField.setType(CalculatedFieldType.ENTITY_AGGREGATION);

        EntityAggregationCalculatedFieldConfiguration configuration = new EntityAggregationCalculatedFieldConfiguration();

        configuration.setArguments(inputs);
        configuration.setMetrics(metrics);
        configuration.setInterval(aggInterval);
        if (watermark != null) {
            configuration.setWatermark(watermark);
        }
        configuration.setOutput(output);

        calculatedField.setConfiguration(configuration);
        calculatedField.setDebugSettings(DebugSettings.all());
        return saveCalculatedField(calculatedField);
    }

    private ObjectNode getLatestTelemetry(EntityId entityId, String... keys) throws Exception {
        return doGetAsync("/api/plugins/telemetry/" + entityId.getEntityType() + "/" + entityId.getId() + "/values/timeseries?keys=" + String.join(",", keys), ObjectNode.class);
    }

}
