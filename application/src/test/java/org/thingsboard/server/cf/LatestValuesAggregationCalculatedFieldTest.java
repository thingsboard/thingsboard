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

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.ArgumentType;
import org.thingsboard.server.common.data.cf.configuration.Output;
import org.thingsboard.server.common.data.cf.configuration.OutputType;
import org.thingsboard.server.common.data.cf.configuration.ReferencedEntityKey;
import org.thingsboard.server.common.data.cf.configuration.aggregation.AggFunction;
import org.thingsboard.server.common.data.cf.configuration.aggregation.AggFunctionInput;
import org.thingsboard.server.common.data.cf.configuration.aggregation.AggKeyInput;
import org.thingsboard.server.common.data.cf.configuration.aggregation.AggMetric;
import org.thingsboard.server.common.data.cf.configuration.aggregation.LatestValuesAggregationCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.debug.DebugSettings;
import org.thingsboard.server.common.data.device.data.DefaultDeviceConfiguration;
import org.thingsboard.server.common.data.device.data.DefaultDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.data.DeviceData;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationPathLevel;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.cf.CalculatedFieldIntegrationTest.POLL_INTERVAL;

@Slf4j
@DaoSqlTest
public class LatestValuesAggregationCalculatedFieldTest extends AbstractControllerTest {

    private Tenant savedTenant;

    private DeviceProfile deviceProfile;
    private Device device1;
    private String accessToken1 = "1234567890111";
    private Device device2;
    private String accessToken2 = "1234567890222";

    private AssetProfile assetProfile;
    private Asset asset;

    private long deduplicationInterval = 10000;

    @Before
    public void beforeTest() throws Exception {
        loginSysAdmin();

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

        deviceProfile = doPost("/api/deviceProfile", createDeviceProfile("Device Profile"), DeviceProfile.class);
        device1 = createDevice("Device 1", deviceProfile.getId(), accessToken1);
        device2 = createDevice("Device 2", deviceProfile.getId(), accessToken2);

        postTelemetry(device1.getId(), "{\"occupied\":true}");
        postTelemetry(device2.getId(), "{\"occupied\":false}");

        assetProfile = doPost("/api/assetProfile", createAssetProfile("Asset Profile"), AssetProfile.class);
        asset = createAsset("Asset", assetProfile.getId());

        createEntityRelation(asset.getId(), device1.getId(), "Contains");
        createEntityRelation(asset.getId(), device2.getId(), "Contains");
    }

    @After
    public void afterTest() throws Exception {
        loginSysAdmin();

        deleteTenant(savedTenant.getId());
    }

    @Test
    public void testCreateCfOnProfile_checkInitialAggregation() throws Exception {
        Asset asset2 = createAsset("Asset 2", assetProfile.getId());
        Device device3 = createDevice("Device 3", "1234567890333");
        Device device4 = createDevice("Device 4", "1234567890444");

        createEntityRelation(asset2.getId(), device3.getId(), "Contains");
        createEntityRelation(asset2.getId(), device4.getId(), "Contains");

        createOccupancyCF(assetProfile.getId());

        await().alias("create CF and perform initial aggregation").atMost(deduplicationInterval, TimeUnit.MILLISECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    verifyTelemetry(asset.getId(), Map.of(
                            "freeSpaces", "1",
                            "occupiedSpaces", "1",
                            "totalSpaces", "2"
                    ));

                    verifyTelemetry(asset2.getId(), Map.of(
                            "freeSpaces", "2",
                            "occupiedSpaces", "0",
                            "totalSpaces", "2"
                    ));
                });

        postTelemetry(device3.getId(), "{\"occupied\":true}");

        await().alias("update telemetry and perform aggregation").atMost(deduplicationInterval, TimeUnit.MILLISECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    verifyTelemetry(asset2.getId(), Map.of(
                            "freeSpaces", "1",
                            "occupiedSpaces", "1",
                            "totalSpaces", "2"
                    ));
                });
    }

    @Test
    public void testAddEntityToProfile_checkAggregation() throws Exception {
        createOccupancyCF(assetProfile.getId());

        Device device3 = createDevice("Device 3", "1234567890333");
        Device device4 = createDevice("Device 4", "1234567890444");
        postTelemetry(device3.getId(), "{\"occupied\":true}");
        postTelemetry(device4.getId(), "{\"occupied\":true}");

        Asset asset2 = createAsset("Asset 2", assetProfile.getId());

        await().alias("add entity to profile with no related entities and perform aggregation").atMost(deduplicationInterval, TimeUnit.MILLISECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode occupancy = getLatestTelemetry(asset2.getId(), "freeSpaces", "occupiedSpaces", "totalSpaces");
                    assertThat(occupancy).isNotNull();
                    assertThat(occupancy.get("freeSpaces").get(0).get("value").isNull()).isTrue();
                    assertThat(occupancy.get("occupiedSpaces").get(0).get("value").isNull()).isTrue();
                    assertThat(occupancy.get("totalSpaces").get(0).get("value").isNull()).isTrue();
                });

        createEntityRelation(asset2.getId(), device3.getId(), "Contains");
        createEntityRelation(asset2.getId(), device4.getId(), "Contains");

        await().alias("create relations and perform aggregation").atMost(deduplicationInterval, TimeUnit.MILLISECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    verifyTelemetry(asset2.getId(), Map.of(
                            "freeSpaces", "0",
                            "occupiedSpaces", "2",
                            "totalSpaces", "2"
                    ));
                });

        postTelemetry(device3.getId(), "{\"occupied\":false}");

        await().alias("update telemetry and perform aggregation").atMost(deduplicationInterval * 2, TimeUnit.MILLISECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    verifyTelemetry(asset2.getId(), Map.of(
                            "freeSpaces", "1",
                            "occupiedSpaces", "1",
                            "totalSpaces", "2"
                    ));
                });
    }

    @Test
    public void testChangeEntityProfile_checkAggregation() throws Exception {
        Asset asset2 = createAsset("Asset 2", assetProfile.getId());
        Device device3 = createDevice("Device 3", "1234567890333");
        Device device4 = createDevice("Device 4", "1234567890444");

        createEntityRelation(asset2.getId(), device3.getId(), "Contains");
        createEntityRelation(asset2.getId(), device4.getId(), "Contains");

        createOccupancyCF(assetProfile.getId());

        await().alias("create CF and perform initial aggregation").atMost(deduplicationInterval, TimeUnit.MILLISECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    verifyTelemetry(asset.getId(), Map.of(
                            "freeSpaces", "1",
                            "occupiedSpaces", "1",
                            "totalSpaces", "2"
                    ));

                    verifyTelemetry(asset2.getId(), Map.of(
                            "freeSpaces", "2",
                            "occupiedSpaces", "0",
                            "totalSpaces", "2"
                    ));
                });

        AssetProfile newAssetProfile = createAssetProfile("New Asset Profile");
        asset2.setAssetProfileId(newAssetProfile.getId());
        doPost("/api/asset", asset2, Asset.class);

        postTelemetry(device3.getId(), "{\"occupied\":true}");

        await().alias("change profile and no aggregation").atMost(deduplicationInterval, TimeUnit.MILLISECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    verifyTelemetry(asset2.getId(), Map.of(
                            "freeSpaces", "2",
                            "occupiedSpaces", "0",
                            "totalSpaces", "2"
                    ));
                });
    }

    @Test
    public void testCreateCfOnAssetAndNoTelemetryOnDevices_checkDefaultValueUsed() throws Exception {
        Asset asset2 = createAsset("Asset 2", assetProfile.getId());
        Device device3 = createDevice("Device 3", "1234567890333");
        Device device4 = createDevice("Device 4", "1234567890444");

        createEntityRelation(asset2.getId(), device3.getId(), "Contains");
        createEntityRelation(asset2.getId(), device4.getId(), "Contains");

        createOccupancyCF(asset2.getId());

        await().alias("create CF and perform aggregation with default values").atMost(deduplicationInterval, TimeUnit.MILLISECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    verifyTelemetry(asset2.getId(), Map.of(
                            "freeSpaces", "2",
                            "occupiedSpaces", "0",
                            "totalSpaces", "2"
                    ));
                });
    }

    @Test
    public void testCreateCfAndUpdateTelemetry_checkAggregation() throws Exception {
        createOccupancyCF(asset.getId());
        checkInitialCalculation();

        postTelemetry(device1.getId(), "{\"occupied\":false}");

        await().alias("update telemetry and perform aggregation").atMost(deduplicationInterval, TimeUnit.MILLISECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    verifyTelemetry(asset.getId(), Map.of(
                            "freeSpaces", "2",
                            "occupiedSpaces", "0",
                            "totalSpaces", "2"
                    ));
                });
    }

    @Test
    public void testDeleteCf_checkNoAggregation() throws Exception {
        CalculatedField cf = createOccupancyCF(asset.getId());
        checkInitialCalculation();

        doDelete("/api/calculatedField/" + cf.getId().getId().toString())
                .andExpect(status().isOk());

        postTelemetry(device1.getId(), "{\"occupied\":false}");

        await().alias("delete cf and update telemetry and no aggregation").atMost(deduplicationInterval, TimeUnit.MILLISECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    verifyTelemetry(asset.getId(), Map.of(
                            "freeSpaces", "1",
                            "occupiedSpaces", "1",
                            "totalSpaces", "2"
                    ));
                });
    }

    @Test
    public void testUpdateTelemetry_checkAggregationNotExecutedUntilDeduplicationInterval() throws Exception {
        createOccupancyCF(asset.getId());
        checkInitialCalculation();

        postTelemetry(device1.getId(), "{\"occupied\":false}");

        await().alias("update telemetry -> no changes").atMost(deduplicationInterval / 2, TimeUnit.MILLISECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(this::checkInitialCalculationValues);

        postTelemetry(device2.getId(), "{\"occupied\":false}");

        await().alias("create CF and perform initial calculation").atMost(deduplicationInterval, TimeUnit.MILLISECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    verifyTelemetry(asset.getId(), Map.of(
                            "freeSpaces", "2",
                            "occupiedSpaces", "0",
                            "totalSpaces", "2"
                    ));
                });
    }

    @Test
    public void testDeleteTelemetry_checkAggregationWithPreviousValuesOrDefault() throws Exception {
        Asset asset2 = createAsset("Asset 2", assetProfile.getId());
        Device device3 = createDevice("Device 3", "1234567890333");
        Device device4 = createDevice("Device 4", "1234567890444");

        createEntityRelation(asset2.getId(), device3.getId(), "Contains");
        createEntityRelation(asset2.getId(), device4.getId(), "Contains");

        long currentTime = System.currentTimeMillis();
        long firstTs = currentTime - 10;
        long secondTs = currentTime - 10;
        long thirdTs = currentTime - 5;
        postTelemetry(device3.getId(), "{\"ts\": " + firstTs + ", \"values\": {\"occupied\":true}}");
        postTelemetry(device4.getId(), "{\"ts\": " + secondTs + ", \"values\": {\"occupied\":true}}");
        postTelemetry(device3.getId(), "{\"ts\": " + thirdTs + ", \"values\": {\"occupied\":true}}");

        createOccupancyCF(asset2.getId());

        await().alias("create CF and perform aggregation").atMost(deduplicationInterval, TimeUnit.MILLISECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    verifyTelemetry(asset2.getId(), Map.of(
                            "freeSpaces", "0",
                            "occupiedSpaces", "2",
                            "totalSpaces", "2"
                    ));
                });

        doDelete("/api/plugins/telemetry/DEVICE/" + device3.getId() + "/timeseries/delete?keys=occupied&deleteAllDataForKeys=false&rewriteLatestIfDeleted=true&deleteLatest=true&startTs=" + thirdTs + "&endTs=" + thirdTs + 1, String.class);
        doDelete("/api/plugins/telemetry/DEVICE/" + device4.getId() + "/timeseries/delete?keys=occupied&deleteAllDataForKeys=false&rewriteLatestIfDeleted=true&deleteLatest=true&startTs=" + secondTs + "&endTs=" + secondTs + 1, String.class);

        await().alias("delete latest telemetry and perform aggregation with previous or default values").atMost(deduplicationInterval * 2, TimeUnit.MILLISECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    verifyTelemetry(asset2.getId(), Map.of(
                            "freeSpaces", "1",
                            "occupiedSpaces", "1",
                            "totalSpaces", "2"
                    ));
                });
    }

    @Test
    public void testCreateRelation_checkAggregation() throws Exception {
        createOccupancyCF(asset.getId());
        checkInitialCalculation();

        Device device3 = createDevice("Device 3", deviceProfile.getId(), "1234567890333");

        postTelemetry(device3.getId(), "{\"occupied\":true}");

        createEntityRelation(asset.getId(), device3.getId(), "Contains");

        await().alias("create relation and perform aggregation").atMost(deduplicationInterval, TimeUnit.MILLISECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    verifyTelemetry(asset.getId(), Map.of(
                            "freeSpaces", "1",
                            "occupiedSpaces", "2",
                            "totalSpaces", "3"
                    ));
                });
    }

    @Test
    public void testDeleteRelation_checkAggregation() throws Exception {
        createOccupancyCF(asset.getId());
        checkInitialCalculation();

        deleteEntityRelation(new EntityRelation(asset.getId(), device1.getId(), "Contains", RelationTypeGroup.COMMON));

        await().alias("create relation and perform aggregation").atMost(deduplicationInterval, TimeUnit.MILLISECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    verifyTelemetry(asset.getId(), Map.of(
                            "freeSpaces", "1",
                            "occupiedSpaces", "0",
                            "totalSpaces", "1"
                    ));
                });
    }

    @Test
    public void testUpdateRelationPath_checkAggregation() throws Exception {
        CalculatedField cf = createOccupancyCF(asset.getId());
        checkInitialCalculation();

        Device device3 = createDevice("Device 3", "1234567890333");
        createEntityRelation(asset.getId(), device3.getId(), "Has");
        postTelemetry(device3.getId(), "{\"occupied\":true}");

        var configuration = (LatestValuesAggregationCalculatedFieldConfiguration) cf.getConfiguration();
        configuration.setRelation(new RelationPathLevel(EntitySearchDirection.FROM, "Has"));
        saveCalculatedField(cf);

        await().alias("update relation path and perform aggregation").atMost(deduplicationInterval, TimeUnit.MILLISECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    verifyTelemetry(asset.getId(), Map.of(
                            "freeSpaces", "0",
                            "occupiedSpaces", "1",
                            "totalSpaces", "1"
                    ));
                });
    }

    @Test
    public void testUpdateArguments_checkAggregation() throws Exception {
        CalculatedField cf = createOccupancyCF(asset.getId());
        checkInitialCalculation();

        postTelemetry(device1.getId(), "{\"occupiedStatus\":false}");
        postTelemetry(device2.getId(), "{\"occupiedStatus\":false}");

        var configuration = (LatestValuesAggregationCalculatedFieldConfiguration) cf.getConfiguration();
        Argument argument = new Argument();
        argument.setRefEntityKey(new ReferencedEntityKey("oc", ArgumentType.TS_LATEST, null));
        argument.setDefaultValue("false");
        configuration.setArguments(Map.of("oc", argument));
        saveCalculatedField(cf);

        await().alias("update arguments and perform aggregation").atMost(deduplicationInterval, TimeUnit.MILLISECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    verifyTelemetry(asset.getId(), Map.of(
                            "freeSpaces", "2",
                            "occupiedSpaces", "0",
                            "totalSpaces", "2"
                    ));
                });
    }

    @Test
    public void testUpdateMetrics_checkAggregation() throws Exception {
        postTelemetry(device1.getId(), "{\"temperature\":24.2}");
        postTelemetry(device2.getId(), "{\"temperature\":19.6}");
        CalculatedField cf = createAvgTemperatureCF(asset.getId());

        await().alias("create avg temp cf and perform initial aggregation").atMost(deduplicationInterval, TimeUnit.MILLISECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    verifyTelemetry(asset.getId(), Map.of("avgTemperature", "24"));
                });

        var configuration = (LatestValuesAggregationCalculatedFieldConfiguration) cf.getConfiguration();
        AggMetric aggMetric = new AggMetric();
        aggMetric.setInput(new AggKeyInput("temp"));
        aggMetric.setFilter("return temp < 100;");
        aggMetric.setFunction(AggFunction.MAX);
        configuration.setMetrics(Map.of("maxTemperature", aggMetric));
        saveCalculatedField(cf);

        await().alias("update metrics and perform aggregation").atMost(deduplicationInterval / 2, TimeUnit.MILLISECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    verifyTelemetry(asset.getId(), Map.of("maxTemperature", "24"));
                });

        postTelemetry(device1.getId(), "{\"temperature\":101.3}");
        postTelemetry(device2.getId(), "{\"temperature\":25.8}");

        await().alias("update telemetry and perform aggregation").atMost(deduplicationInterval, TimeUnit.MILLISECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    verifyTelemetry(asset.getId(), Map.of("maxTemperature", "26"));
                });
    }

    @Test
    public void testUpdateOutput_checkAggregation() throws Exception {
        postTelemetry(device1.getId(), "{\"temperature\":24.2}");
        postTelemetry(device2.getId(), "{\"temperature\":19.6}");
        CalculatedField cf = createAvgTemperatureCF(asset.getId());

        await().alias("create avg temp cf and perform initial aggregation").atMost(deduplicationInterval, TimeUnit.MILLISECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    verifyTelemetry(asset.getId(), Map.of("avgTemperature", "24"));
                });

        var configuration = (LatestValuesAggregationCalculatedFieldConfiguration) cf.getConfiguration();
        Output output = new Output();
        output.setType(OutputType.ATTRIBUTES);
        output.setScope(AttributeScope.SERVER_SCOPE);
        configuration.setOutput(output);
        saveCalculatedField(cf);

        await().alias("update output and perform aggregation").atMost(deduplicationInterval, TimeUnit.MILLISECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ArrayNode avgTemperature = getServerAttributes(asset.getId(), "avgTemperature");
                    assertThat(avgTemperature).isNotNull();
                    assertThat(avgTemperature.get(0)).isNotNull();
                    assertThat(avgTemperature.get(0).get("value").asText()).isEqualTo("24.2");
                });
    }

    @Test
    public void testUpdateDeduplicationInterval_checkAggregationNotExecutedUntilDeduplicationInterval() throws Exception {
        postTelemetry(device1.getId(), "{\"temperature\":24.2}");
        postTelemetry(device2.getId(), "{\"temperature\":19.6}");
        CalculatedField cf = createAvgTemperatureCF(asset.getId());

        await().alias("create avg temp cf and perform initial aggregation").atMost(deduplicationInterval, TimeUnit.MILLISECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    verifyTelemetry(asset.getId(), Map.of("avgTemperature", "24"));
                });

        var configuration = (LatestValuesAggregationCalculatedFieldConfiguration) cf.getConfiguration();
        configuration.setDeduplicationIntervalMillis(2 * deduplicationInterval);
        saveCalculatedField(cf);

        await().alias("update deduplication interval and perform aggregation").atMost(deduplicationInterval / 2, TimeUnit.MILLISECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    verifyTelemetry(asset.getId(), Map.of("avgTemperature", "24"));
                });

        postTelemetry(device2.getId(), "{\"temperature\":32.1}");

        await().alias("update telemetry and perform aggregation").atMost(2 * deduplicationInterval, TimeUnit.MILLISECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    verifyTelemetry(asset.getId(), Map.of("avgTemperature", "28"));
                });
    }

    private void checkInitialCalculation() {
        await().alias("create CF and perform initial aggregation").atMost(deduplicationInterval, TimeUnit.MILLISECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(this::checkInitialCalculationValues);
    }

    private void checkInitialCalculationValues() throws Exception {
        ObjectNode occupancy = getLatestTelemetry(asset.getId(), "freeSpaces", "occupiedSpaces", "totalSpaces");
        assertThat(occupancy).isNotNull();
        assertThat(occupancy.get("freeSpaces").get(0).get("value").asText()).isEqualTo("1");
        assertThat(occupancy.get("occupiedSpaces").get(0).get("value").asText()).isEqualTo("1");
        assertThat(occupancy.get("totalSpaces").get(0).get("value").asText()).isEqualTo("2");
    }

    private CalculatedField createAvgTemperatureCF(EntityId entityId) {
        Map<String, Argument> arguments = new HashMap<>();
        Argument argument = new Argument();
        argument.setRefEntityKey(new ReferencedEntityKey("temperature", ArgumentType.TS_LATEST, null));
        argument.setDefaultValue("20");
        arguments.put("temp", argument);

        Map<String, AggMetric> aggMetrics = new HashMap<>();

        AggMetric avgMetric = new AggMetric();
        avgMetric.setFunction(AggFunction.AVG);
        avgMetric.setFilter("return temp >= 20;");
        avgMetric.setInput(new AggKeyInput("temp"));
        aggMetrics.put("avgTemperature", avgMetric);

        Output output = new Output();
        output.setType(OutputType.TIME_SERIES);
        output.setDecimalsByDefault(0);

        return createAggCf("Average temperature", entityId,
                new RelationPathLevel(EntitySearchDirection.FROM, "Contains"),
                arguments,
                aggMetrics,
                output);
    }

    private CalculatedField createOccupancyCF(EntityId entityId) {
        Map<String, Argument> arguments = new HashMap<>();
        Argument argument = new Argument();
        argument.setRefEntityKey(new ReferencedEntityKey("occupied", ArgumentType.TS_LATEST, null));
        argument.setDefaultValue("false");
        arguments.put("oc", argument);

        Map<String, AggMetric> aggMetrics = new HashMap<>();

        AggMetric freeSpaces = new AggMetric();
        freeSpaces.setFunction(AggFunction.COUNT);
        freeSpaces.setFilter("return oc == false;");
        freeSpaces.setInput(new AggKeyInput("oc"));
        aggMetrics.put("freeSpaces", freeSpaces);

        AggMetric occupiedSpaces = new AggMetric();
        occupiedSpaces.setFunction(AggFunction.COUNT);
        occupiedSpaces.setFilter("return oc == true;");
        occupiedSpaces.setInput(new AggKeyInput("oc"));
        aggMetrics.put("occupiedSpaces", occupiedSpaces);

        AggMetric totalSpaces = new AggMetric();
        totalSpaces.setFunction(AggFunction.COUNT);
        totalSpaces.setInput(new AggFunctionInput("return 1;"));
        aggMetrics.put("totalSpaces", totalSpaces);

        Output output = new Output();
        output.setType(OutputType.TIME_SERIES);
        output.setDecimalsByDefault(0);

        return createAggCf("Occupied spaces", entityId,
                new RelationPathLevel(EntitySearchDirection.FROM, "Contains"),
                arguments,
                aggMetrics,
                output);
    }

    private CalculatedField createAggCf(String name,
                                        EntityId entityId,
                                        RelationPathLevel relation,
                                        Map<String, Argument> inputs,
                                        Map<String, AggMetric> metrics,
                                        Output output) {
        CalculatedField calculatedField = new CalculatedField();
        calculatedField.setName(name);
        calculatedField.setEntityId(entityId);
        calculatedField.setType(CalculatedFieldType.LATEST_VALUES_AGGREGATION);

        LatestValuesAggregationCalculatedFieldConfiguration configuration = new LatestValuesAggregationCalculatedFieldConfiguration();
        configuration.setRelation(relation);
        configuration.setArguments(inputs);
        configuration.setDeduplicationIntervalMillis(deduplicationInterval);
        configuration.setMetrics(metrics);
        configuration.setOutput(output);

        calculatedField.setConfiguration(configuration);
        calculatedField.setDebugSettings(DebugSettings.all());
        return saveCalculatedField(calculatedField);
    }

    private Device createDevice(String name, DeviceProfileId deviceProfileId, String accessToken) {
        Device device = new Device();
        device.setName(name);
        device.setDeviceProfileId(deviceProfileId);
        DeviceData deviceData = new DeviceData();
        deviceData.setTransportConfiguration(new DefaultDeviceTransportConfiguration());
        deviceData.setConfiguration(new DefaultDeviceConfiguration());
        device.setDeviceData(deviceData);
        return doPost("/api/device?accessToken=" + accessToken, device, Device.class);
    }

    private Asset createAsset(String name, AssetProfileId assetProfileId) {
        Asset asset = new Asset();
        asset.setName(name);
        asset.setAssetProfileId(assetProfileId);
        return doPost("/api/asset", asset, Asset.class);
    }

    private void verifyTelemetry(EntityId entityId, Map<String, String> expectedResults) throws Exception {
        ObjectNode result = getLatestTelemetry(entityId, expectedResults.keySet().toArray(new String[0]));
        assertThat(result).isNotNull();
        expectedResults.forEach((key, value) -> assertThat(result.get(key).get(0).get("value").asText()).isEqualTo(value));
    }

    private ObjectNode getLatestTelemetry(EntityId entityId, String... keys) throws Exception {
        return doGetAsync("/api/plugins/telemetry/" + entityId.getEntityType() + "/" + entityId.getId() + "/values/timeseries?keys=" + String.join(",", keys), ObjectNode.class);
    }

    private ArrayNode getServerAttributes(EntityId entityId, String... keys) throws Exception {
        return doGetAsync("/api/plugins/telemetry/" + entityId.getEntityType() + "/" + entityId.getId() + "/values/attributes/SERVER_SCOPE?keys=" + String.join(",", keys), ArrayNode.class);
    }

}
