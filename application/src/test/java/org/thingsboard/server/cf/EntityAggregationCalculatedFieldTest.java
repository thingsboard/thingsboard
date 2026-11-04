// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.cf;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.annotation.DirtiesContext;
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
import org.thingsboard.server.common.data.cf.configuration.aggregation.single.interval.HourInterval;
import org.thingsboard.server.common.data.cf.configuration.aggregation.single.interval.Watermark;
import org.thingsboard.server.common.data.debug.DebugSettings;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.job.Job;
import org.thingsboard.server.common.data.job.JobStatus;
import org.thingsboard.server.common.data.job.JobType;
import org.thingsboard.server.common.data.job.task.CfReprocessingTaskResult;
import org.thingsboard.server.common.data.job.task.TaskResult;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.controller.AbstractWebTest;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.thingsboard.server.cf.CalculatedFieldIntegrationTest.POLL_INTERVAL;

@DaoSqlTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class EntityAggregationCalculatedFieldTest extends AbstractControllerTest {

    private final String TZ = "Europe/Kyiv";

    private Tenant savedTenant;

    @Before
    public void beforeEach() throws Exception {
        loginSysAdmin();

        updateDefaultTenantProfileConfig(tenantProfileConfig -> {
            tenantProfileConfig.setMinAllowedDeduplicationIntervalInSecForCF(1);
            tenantProfileConfig.setMinAllowedAggregationIntervalInSecForCF(1);
            tenantProfileConfig.setCfReevaluationCheckInterval(1);
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

        CustomInterval customInterval = new CustomInterval(TZ, 0L, 5L);
        createConsumptionCF(device.getId(), customInterval, null);

        long interval = customInterval.getCurrentIntervalDurationMillis();

        await().alias("create CF and no telemetry during interval -> save metric with default value")
                .atMost(2 * interval, TimeUnit.MILLISECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode result = getLatestTelemetry(device.getId(), "consumption", "avgConsumption");
                    assertThat(result).isNotNull();
                    assertNumericValue(result, "consumption", 9999);
                    assertThat(result.get("avgConsumption").get(0).get("value").isNull()).isTrue();
                });
    }

    @Test
    public void testCreateCfWithoutWatermark_checkAggregation() throws Exception {
        Device device = createDevice("Device", "1234567890111");

        CustomInterval customInterval = new CustomInterval(TZ, 0L, 5L);
        createConsumptionCF(device.getId(), customInterval, null);

        long currentIntervalStartTs = customInterval.getCurrentIntervalStartTs();

        long tsBeforeInterval = currentIntervalStartTs - 1000;
        long tsInInterval_1 = currentIntervalStartTs + 1000;
        long tsInInterval_2 = currentIntervalStartTs + 500;
        long tsInInterval_3 = currentIntervalStartTs + 200;
        postTelemetry(device.getId(), String.format("{\"ts\": \"%s\", \"values\": {\"energy\":120}}", tsBeforeInterval));
        postTelemetry(device.getId(), String.format("{\"ts\": \"%s\", \"values\": {\"energy\":100}}", tsInInterval_1));
        postTelemetry(device.getId(), String.format("{\"ts\": \"%s\", \"values\": {\"energy\":180}}", tsInInterval_2));
        postTelemetry(device.getId(), String.format("{\"ts\": \"%s\", \"values\": {\"energy\":120}}", tsInInterval_3));

        long interval = customInterval.getCurrentIntervalDurationMillis();

        await().alias("create CF -> perform aggregation after interval end")
                .atMost(2 * interval, TimeUnit.MILLISECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode result = getLatestTelemetry(device.getId(), "consumption", "avgConsumption");
                    assertThat(result).isNotNull();
                    assertNumericValue(result, "consumption", 400);
                    assertNumericValue(result, "avgConsumption", 133);
                });

        postTelemetry(device.getId(), String.format("{\"ts\": \"%s\", \"values\": {\"energy\":500}}", tsInInterval_1));

        await().alias("update telemetry that belongs to previous interval -> no aggregation since watermark is not set ")
                .atMost(2 * interval, TimeUnit.MILLISECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode result = getLatestTelemetry(device.getId(), "consumption", "avgConsumption");
                    assertThat(result).isNotNull();
                    assertNumericValue(result, "consumption", 400);
                    assertNumericValue(result, "avgConsumption", 133);
                });
    }

    @Test
    public void testCreateCfWithWatermark_checkAggregationDuringWatermark() throws Exception {
        Device device = createDevice("Device", "1234567890111");

        CustomInterval customInterval = new CustomInterval(TZ, 0L, 5L);
        Watermark watermark = new Watermark(10);
        createConsumptionCF(device.getId(), customInterval, watermark);

        long currentIntervalStartTs = customInterval.getCurrentIntervalStartTs();

        long tsBeforeInterval = currentIntervalStartTs - 1000L;
        long tsInInterval_1 = currentIntervalStartTs + 1000L;
        long tsInInterval_2 = currentIntervalStartTs + 500L;
        long tsInInterval_3 = currentIntervalStartTs + 200L;
        postTelemetry(device.getId(), String.format("{\"ts\": \"%s\", \"values\": {\"energy\":120}}", tsBeforeInterval));
        postTelemetry(device.getId(), String.format("{\"ts\": \"%s\", \"values\": {\"energy\":100}}", tsInInterval_1));
        postTelemetry(device.getId(), String.format("{\"ts\": \"%s\", \"values\": {\"energy\":180}}", tsInInterval_2));
        postTelemetry(device.getId(), String.format("{\"ts\": \"%s\", \"values\": {\"energy\":120}}", tsInInterval_3));

        long interval = customInterval.getCurrentIntervalDurationMillis();

        await().alias("create CF -> perform aggregation after interval end")
                .atMost(2 * interval, TimeUnit.MILLISECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode result = getLatestTelemetry(device.getId(), "consumption", "avgConsumption");
                    assertThat(result).isNotNull();
                    assertNumericValue(result, "consumption", 400);
                    assertNumericValue(result, "avgConsumption", 133);
                });

        postTelemetry(device.getId(), String.format("{\"ts\": \"%s\", \"values\": {\"energy\":300}}", tsInInterval_1));

        await().alias("update telemetry during watermark -> perform aggregation")
                .atMost(2 * 10, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode result = getLatestTelemetry(device.getId(), "consumption", "avgConsumption");
                    assertThat(result).isNotNull();
                    assertNumericValue(result, "consumption", 600);
                    assertNumericValue(result, "avgConsumption", 200);
                });
    }

    @Test
    public void testReprocessCalculatedField() throws Exception {
        Device device = createDevice("Device", "1234567890111");

        LocalDate testDate = LocalDate.of(2025, 11, 11);
        ZonedDateTime dateTime = ZonedDateTime.of(testDate, LocalTime.of(13, 24), ZoneId.of(TZ));
        // reprocessing time window(TW)
        long startTs = dateTime.minusHours(4).toInstant().toEpochMilli(); // 2025-11-11 9:24
        long endTs = dateTime.toInstant().toEpochMilli(); // 2025-11-11 13:24

        // outside the TW
        long interval_1_startTs = ts(testDate, 8, 0, 0);
        long interval_1_1 = ts(testDate, 8, 11, 23);
        long interval_1_2 = ts(testDate, 8, 33, 56);
        long interval_1_3 = ts(testDate, 8, 47, 12);

        postTelemetry(device.getId(), String.format("{\"ts\":%s, \"values\":{\"energy\":11}}", interval_1_1));
        postTelemetry(device.getId(), String.format("{\"ts\":%s, \"values\":{\"energy\":12}}", interval_1_2));
        postTelemetry(device.getId(), String.format("{\"ts\":%s, \"values\":{\"energy\":8}}", interval_1_3));

        // outside the TW (but telemetry will be used for initial processing)
        long interval_2_startTs = ts(testDate, 9, 0, 0);
        long interval_2_1 = ts(testDate, 9, 0, 0);
        long interval_2_2 = ts(testDate, 9, 15, 11);

        postTelemetry(device.getId(), String.format("{\"ts\":%s, \"values\":{\"energy\":13}}", interval_2_1));
        postTelemetry(device.getId(), String.format("{\"ts\":%s, \"values\":{\"energy\":35}}", interval_2_2));

        // inside the TW
        long interval_3_startTs = ts(testDate, 10, 0, 0);
        long interval_3_1 = ts(testDate, 10, 20, 44);
        long interval_3_2 = ts(testDate, 10, 40, 33);
        long interval_3_3 = ts(testDate, 10, 55, 22);

        postTelemetry(device.getId(), String.format("{\"ts\":%s, \"values\":{\"energy\":3}}", interval_3_1));
        postTelemetry(device.getId(), String.format("{\"ts\":%s, \"values\":{\"energy\":22}}", interval_3_2));
        postTelemetry(device.getId(), String.format("{\"ts\":%s, \"values\":{\"energy\":22}}", interval_3_3));

        // inside the TW
        long interval_4_startTs = ts(testDate, 11, 0, 0);

        // inside the TW
        long interval_5_startTs = ts(testDate, 12, 0, 0);
        long interval_5_1 = ts(testDate, 12, 11, 46);
        long interval_5_2 = ts(testDate, 12, 26, 11);
        long interval_5_3 = ts(testDate, 12, 59, 31);

        postTelemetry(device.getId(), String.format("{\"ts\":%s, \"values\":{\"energy\":5}}", interval_5_1));
        postTelemetry(device.getId(), String.format("{\"ts\":%s, \"values\":{\"energy\":51}}", interval_5_2));
        postTelemetry(device.getId(), String.format("{\"ts\":%s, \"values\":{\"energy\":12}}", interval_5_3));

        // outside the TW
        long interval_6_startTs = ts(testDate, 13, 0, 0);
        long interval_6_1 = ts(testDate, 13, 17, 32);
        long interval_6_2 = ts(testDate, 13, 38, 31);

        postTelemetry(device.getId(), String.format("{\"ts\":%s, \"values\":{\"energy\":22}}", interval_6_1));
        postTelemetry(device.getId(), String.format("{\"ts\":%s, \"values\":{\"energy\":11}}", interval_6_2));

        /*
                                              startTs                        endTs
                                                 |-----------------------------|
                             |         |         |         |         |         |         |
               |  intervals  |   8-9   |  9-10   |  10-11  |  11-12  |  12-13  |  13-14  |
               |  telemetry  | 11 12 8 |  13 35  | 3 22 22 |         | 5 51 12 |  22 11  |
                             |         |         |         |         |         |         |
                                                 |-----------------------------|
                                                                |--- reprocessing time window
               consumption should be: 48 -> 47 -> 9999(default value) -> 68
        */

        CalculatedField savedCalculatedField = createConsumptionCF(device.getId(), new HourInterval(TZ, 0L), null);

        reprocessCalculatedField(savedCalculatedField, startTs, endTs);

        await().alias("reprocess -> perform calculation for time window").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode result = getTimeSeries(device.getId(), interval_1_startTs - 1, interval_6_startTs + 1, "consumption", "avgConsumption");
                    assertThat(result).isNotNull();

                    assertThat(result.get("consumption").get(0).get("ts").asText()).isEqualTo(Long.toString(interval_5_startTs));
                    assertNumericValue(result, "consumption", 0, 68);
                    assertThat(result.get("avgConsumption").get(0).get("ts").asText()).isEqualTo(Long.toString(interval_5_startTs));
                    assertNumericValue(result, "avgConsumption", 0, 23);

                    assertThat(result.get("consumption").get(1).get("ts").asText()).isEqualTo(Long.toString(interval_4_startTs));
                    assertNumericValue(result, "consumption", 1, 9999);

                    assertThat(result.get("consumption").get(2).get("ts").asText()).isEqualTo(Long.toString(interval_3_startTs));
                    assertNumericValue(result, "consumption", 2, 47);
                    assertThat(result.get("avgConsumption").get(1).get("ts").asText()).isEqualTo(Long.toString(interval_3_startTs));
                    assertNumericValue(result, "avgConsumption", 1, 16);

                    assertThat(result.get("consumption").get(3).get("ts").asText()).isEqualTo(Long.toString(interval_2_startTs));
                    assertNumericValue(result, "consumption", 3, 48);
                    assertThat(result.get("avgConsumption").get(2).get("ts").asText()).isEqualTo(Long.toString(interval_2_startTs));
                    assertNumericValue(result, "avgConsumption", 2, 24);
                });

        await().atMost(AbstractWebTest.TIMEOUT, TimeUnit.SECONDS).untilAsserted(() -> {
            Job cfReprocessingJob = findJobs(List.of(JobType.CF_REPROCESSING), List.of(device.getUuidId())).stream().findFirst().orElseThrow();
            assertThat(cfReprocessingJob.getStatus()).isEqualTo(JobStatus.COMPLETED);
            assertThat(cfReprocessingJob.getResult().getSuccessfulCount()).isEqualTo(1);
            assertThat(cfReprocessingJob.getResult().getTotalCount()).isEqualTo(1);
            assertThat(cfReprocessingJob.getEntityId()).isEqualTo(device.getId());
            assertThat(cfReprocessingJob.getEntityName()).isEqualTo(device.getName());
        });
    }

    @Test
    public void testReprocessCalculatedFieldWhenNoTimeseriesDataAvailableForTimewindow() throws Exception {
        Device device = createDevice("Device", "1234567890111");

        LocalDate testDate = LocalDate.of(2025, 11, 11);
        ZonedDateTime dateTime = ZonedDateTime.of(testDate, LocalTime.of(13, 24), ZoneId.of(TZ));
        // reprocessing time window(TW)
        long startTs = dateTime.minusHours(4).toInstant().toEpochMilli(); // 2025-11-11 9:24
        long endTs = dateTime.toInstant().toEpochMilli(); // 2025-11-11 13:24

        CalculatedField savedCalculatedField = createConsumptionCF(device.getId(), new HourInterval(TZ, 0L), null, null);

        reprocessCalculatedField(savedCalculatedField, startTs, endTs);

        await().atMost(AbstractWebTest.TIMEOUT, TimeUnit.SECONDS).untilAsserted(() -> {
            Job cfReprocessingJob = findJobs(List.of(JobType.CF_REPROCESSING), List.of(device.getUuidId())).stream().findFirst().orElseThrow();
            assertThat(cfReprocessingJob.getStatus()).isEqualTo(JobStatus.FAILED);
            assertThat(cfReprocessingJob.getResult().getSuccessfulCount()).isEqualTo(0);
            assertThat(cfReprocessingJob.getResult().getTotalCount()).isEqualTo(1);
            assertThat(cfReprocessingJob.getResult().getFailedCount()).isEqualTo(1);
            assertThat(cfReprocessingJob.getResult().getResults()).isNotNull().hasSize(1);
            assertThat(cfReprocessingJob.getEntityId()).isEqualTo(device.getId());
            assertThat(cfReprocessingJob.getEntityName()).isEqualTo(device.getName());

            TaskResult taskResult = cfReprocessingJob.getResult().getResults().get(0);
            assertThat(taskResult).isInstanceOf(CfReprocessingTaskResult.class);
            CfReprocessingTaskResult cfReprocessingTaskResult = (CfReprocessingTaskResult) taskResult;
            assertThat(cfReprocessingTaskResult.getFailure()).isNotNull()
                    .extracting(CfReprocessingTaskResult.CfReprocessingTaskFailure::getError)
                    .isEqualTo("Time series data aggregation for selected reprocessing time window has no results!");
        });
    }

    @Test
    public void testSendFutureTelemetry_checkAggregation() throws Exception {
        Device device = createDevice("Device", "1234567890111");

        CustomInterval customInterval = new CustomInterval(TZ, 0L, 2L);
        createConsumptionCF(device.getId(), customInterval, null);

        long interval = customInterval.getCurrentIntervalDurationMillis();

        // Wait for a fresh interval
        long initialIntervalStart = customInterval.getCurrentIntervalStartTs();
        await().alias("wait for fresh interval")
                .atMost(interval + 100, TimeUnit.MILLISECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .until(() -> customInterval.getCurrentIntervalStartTs() != initialIntervalStart);

        long currentIntervalStartTs = customInterval.getCurrentIntervalStartTs();

        long tsBeforeInterval = currentIntervalStartTs - 1000;
        long tsInInterval_1 = currentIntervalStartTs + 1000;
        long tsInInterval_2 = currentIntervalStartTs + 500;
        long tsInInterval_3 = currentIntervalStartTs + 200;
        postTelemetry(device.getId(), String.format("{\"ts\": \"%s\", \"values\": {\"energy\":120}}", tsBeforeInterval));
        postTelemetry(device.getId(), String.format("{\"ts\": \"%s\", \"values\": {\"energy\":100}}", tsInInterval_1));
        postTelemetry(device.getId(), String.format("{\"ts\": \"%s\", \"values\": {\"energy\":180}}", tsInInterval_2));
        postTelemetry(device.getId(), String.format("{\"ts\": \"%s\", \"values\": {\"energy\":120}}", tsInInterval_3));

        await().alias("create CF -> perform aggregation after interval end")
                .atMost(2 * interval, TimeUnit.MILLISECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode result = getLatestTelemetry(device.getId(), "consumption", "avgConsumption");
                    assertThat(result).isNotNull();
                    assertNumericValue(result, "consumption", 400);
                    assertNumericValue(result, "avgConsumption", 133);
                });

        postTelemetry(device.getId(), String.format("{\"ts\": \"%s\", \"values\": {\"energy\":500}}", currentIntervalStartTs + 4500L));

        await().alias("update telemetry that belongs to future interval -> check aggregation ")
                .atMost(3 * interval, TimeUnit.MILLISECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode result = getLatestTelemetry(device.getId(), "consumption", "avgConsumption");
                    assertThat(result).isNotNull();
                    assertNumericValue(result, "consumption", 500);
                    assertThat(result.get("consumption").get(0).get("ts").asLong()).isEqualTo(currentIntervalStartTs + 4000L);
                    assertNumericValue(result, "avgConsumption", 500);
                    assertThat(result.get("avgConsumption").get(0).get("ts").asLong()).isEqualTo(currentIntervalStartTs + 4000L);
                });
    }

    private long ts(LocalDate date, int hour, int minute, int second) {
        return ZonedDateTime.of(date, LocalTime.of(hour, minute, second), ZoneId.of(TZ))
                .toInstant()
                .toEpochMilli();
    }

    private CalculatedField createConsumptionCF(EntityId entityId, AggInterval aggInterval, Watermark watermark) {
        return createConsumptionCF(entityId, aggInterval, watermark, 9999.0);
    }

    private CalculatedField createConsumptionCF(EntityId entityId, AggInterval aggInterval, Watermark watermark, Double defaultValue) {
        Map<String, Argument> arguments = new HashMap<>();
        Argument argument = new Argument();
        argument.setRefEntityKey(new ReferencedEntityKey("energy", ArgumentType.TS_LATEST, null));
        arguments.put("en", argument);

        Map<String, AggMetric> aggMetrics = new HashMap<>();

        AggMetric consumption = new AggMetric();
        consumption.setFunction(AggFunction.SUM);
        consumption.setInput(new AggKeyInput("en"));
        consumption.setDefaultValue(defaultValue);
        aggMetrics.put("consumption", consumption);

        AggMetric avgEnergyConsumption = new AggMetric();
        avgEnergyConsumption.setFunction(AggFunction.AVG);
        avgEnergyConsumption.setInput(new AggKeyInput("en"));
        aggMetrics.put("avgConsumption", avgEnergyConsumption);

        TimeSeriesOutput output = new TimeSeriesOutput();
        output.setDecimalsByDefault(0);

        return createAggCf("Consumption", entityId,
                aggInterval,
                watermark,
                arguments,
                aggMetrics,
                output);
    }

    @Test
    public void testCreateCfWith2Arguments_checkAggregation() throws Exception {
        Device device = createDevice("Device", "1234567890111");

        CustomInterval customInterval = new CustomInterval("Europe/Kyiv", 0L, 5L);
        createCFWith2Args(device.getId(), customInterval, null);

        long currentIntervalStartTs = customInterval.getCurrentIntervalStartTs();

        long tsBeforeInterval = currentIntervalStartTs - 1000;
        long tsInInterval_1 = currentIntervalStartTs + 1000;
        long tsInInterval_2 = currentIntervalStartTs + 500;
        long tsInInterval_3 = currentIntervalStartTs + 200;
        postTelemetry(device.getId(), String.format("{\"ts\": \"%s\", \"values\": {\"energy\":120, \"temperature\":43}}", tsBeforeInterval));
        postTelemetry(device.getId(), String.format("{\"ts\": \"%s\", \"values\": {\"energy\":100, \"temperature\":39}}", tsInInterval_1));
        postTelemetry(device.getId(), String.format("{\"ts\": \"%s\", \"values\": {\"energy\":180, \"temperature\":27}}", tsInInterval_2));
        postTelemetry(device.getId(), String.format("{\"ts\": \"%s\", \"values\": {\"energy\":120, \"temperature\":50}}", tsInInterval_3));

        long interval = customInterval.getCurrentIntervalDurationMillis();

        await().alias("create CF -> perform aggregation after interval end")
                .atMost(2 * interval, TimeUnit.MILLISECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode result = getLatestTelemetry(device.getId(), "consumption", "avgTemperature");
                    assertThat(result).isNotNull();
                    assertNumericValue(result, "consumption", 400);
                    assertNumericValue(result, "avgTemperature", 39);
                });
    }

    private CalculatedField createCFWith2Args(EntityId entityId, AggInterval aggInterval, Watermark watermark) {
        Map<String, Argument> arguments = new HashMap<>();
        Argument energy = new Argument();
        energy.setRefEntityKey(new ReferencedEntityKey("energy", ArgumentType.TS_LATEST, null));
        arguments.put("en", energy);

        Argument temperature = new Argument();
        temperature.setRefEntityKey(new ReferencedEntityKey("temperature", ArgumentType.TS_LATEST, null));
        arguments.put("temp", temperature);

        Map<String, AggMetric> aggMetrics = new HashMap<>();

        AggMetric consumption = new AggMetric();
        consumption.setFunction(AggFunction.SUM);
        consumption.setInput(new AggKeyInput("en"));
        consumption.setDefaultValue(9999.0);
        aggMetrics.put("consumption", consumption);

        AggMetric avgTemperature = new AggMetric();
        avgTemperature.setFunction(AggFunction.AVG);
        avgTemperature.setInput(new AggKeyInput("temp"));
        aggMetrics.put("avgTemperature", avgTemperature);

        TimeSeriesOutput output = new TimeSeriesOutput();
        output.setDecimalsByDefault(0);

        return createAggCf("CF with 2 args", entityId,
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

    // useStrictDataTypes=true so the value node keeps its stored type (numeric -> JSON number, str_v -> JSON string).
    // Without it the endpoint returns every value via getValueAsString(), masking the string-vs-number distinction.
    private ObjectNode getLatestTelemetry(EntityId entityId, String... keys) throws Exception {
        return doGetAsync("/api/plugins/telemetry/" + entityId.getEntityType() + "/" + entityId.getId() + "/values/timeseries?useStrictDataTypes=true&keys=" + String.join(",", keys), ObjectNode.class);
    }

    // Regression guard: a numeric aggregation result must be stored as a numeric JSON node (ts_kv.dbl_v/long_v),
    // not a JSON string (ts_kv.str_v) - otherwise server-side AVG/SUM return no data. A value-only check would
    // not catch this: asLong()/asText() coerce a string node like "400" to the same value/text, so the node type
    // is asserted explicitly; the numeric comparison then verifies the aggregated value.
    private static void assertNumericValue(ObjectNode result, String key, long expectedValue) {
        assertNumericValue(result, key, 0, expectedValue);
    }

    private static void assertNumericValue(ObjectNode result, String key, int index, long expectedValue) {
        JsonNode value = result.get(key).get(index).get("value");
        assertThat(value.isNumber()).as(key + "[" + index + "] should be stored as a numeric node").isTrue();
        assertThat(value.asLong()).isEqualTo(expectedValue);
    }

    // useStrictDataTypes=true (see getLatestTelemetry) so reprocessed results keep their stored numeric type.
    private ObjectNode getTimeSeries(EntityId entityId, long startTs, long endTs, String... keys) throws Exception {
        return doGetAsync("/api/plugins/telemetry/" + entityId.getEntityType() + "/" + entityId.getId() + "/values/timeseries?useStrictDataTypes=true&keys={keys}&startTs={startTs}&endTs={endTs}", ObjectNode.class, String.join(",", keys), startTs, endTs);
    }

    private Job reprocessCalculatedField(CalculatedField savedCalculatedField, long startTs, long endTs) throws Exception {
        return doGet("/api/calculatedField/" + savedCalculatedField.getUuidId() + "/reprocess?startTs={startTs}&endTs={endTs}", Job.class, startTs, endTs);
    }

}
