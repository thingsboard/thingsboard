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
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.ArgumentType;
import org.thingsboard.server.common.data.cf.configuration.CalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.Output;
import org.thingsboard.server.common.data.cf.configuration.OutputType;
import org.thingsboard.server.common.data.cf.configuration.ReferencedEntityKey;
import org.thingsboard.server.common.data.cf.configuration.aggregation.AggFunction;
import org.thingsboard.server.common.data.cf.configuration.aggregation.AggFunctionInput;
import org.thingsboard.server.common.data.cf.configuration.aggregation.AggKeyInput;
import org.thingsboard.server.common.data.cf.configuration.aggregation.AggMetric;
import org.thingsboard.server.common.data.cf.configuration.aggregation.AggSource;
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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.thingsboard.server.cf.CalculatedFieldIntegrationTest.POLL_INTERVAL;

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

    private CalculatedField calculatedField;

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

        calculatedField = createOccupancyCF("Occupied spaces", asset.getId(), List.of(deviceProfile.getId()));

        checkInitialCalculation();
    }

    @After
    public void afterTest() throws Exception {
        loginSysAdmin();

        deleteTenant(savedTenant.getId());
    }

    @Test
    public void testUpdateTelemetry_checkMetricsCalculation() throws Exception {
        postTelemetry(device1.getId(), "{\"occupied\":false}");

        await().alias("update telemetry and perform aggregation").atMost(deduplicationInterval, TimeUnit.MILLISECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode occupancy = getLatestTelemetry(asset.getId(), "freeSpaces", "occupiedSpaces", "totalSpaces");
                    assertThat(occupancy).isNotNull();
                    assertThat(occupancy.get("freeSpaces").get(0).get("value").asText()).isEqualTo("2");
                    assertThat(occupancy.get("occupiedSpaces").get(0).get("value").asText()).isEqualTo("0");
                    assertThat(occupancy.get("totalSpaces").get(0).get("value").asText()).isEqualTo("2");
                });
    }

    @Test
    public void testUpdateTelemetry_checkMetricsCalculationNotExecutedUntilDeduplicationInterval() throws Exception {
        postTelemetry(device1.getId(), "{\"occupied\":false}");

        await().alias("update telemetry -> no changes").atMost(deduplicationInterval / 2, TimeUnit.MILLISECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(this::checkInitialCalculationValues);

        postTelemetry(device2.getId(), "{\"occupied\":false}");

        await().alias("create CF and perform initial calculation").atMost(deduplicationInterval, TimeUnit.MILLISECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode occupancy = getLatestTelemetry(asset.getId(), "freeSpaces", "occupiedSpaces", "totalSpaces");
                    assertThat(occupancy).isNotNull();
                    assertThat(occupancy.get("freeSpaces").get(0).get("value").asText()).isEqualTo("2");
                    assertThat(occupancy.get("occupiedSpaces").get(0).get("value").asText()).isEqualTo("0");
                    assertThat(occupancy.get("totalSpaces").get(0).get("value").asText()).isEqualTo("2");
                });
    }

    @Test
    public void testChangeProfile_checkMetricsCalculation() throws Exception {
        DeviceProfile deviceProfile2 = doPost("/api/deviceProfile", createDeviceProfile("Device Profile 2"), DeviceProfile.class);
        device1.setDeviceProfileId(deviceProfile2.getId());
        device1 = doPost("/api/device?accessToken=" + accessToken1, device1, Device.class);

        postTelemetry(device1.getId(), "{\"occupied\":false}");

        await().alias("change profile and perform aggregation").atMost(deduplicationInterval, TimeUnit.MILLISECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode occupancy = getLatestTelemetry(asset.getId(), "freeSpaces", "occupiedSpaces", "totalSpaces");
                    assertThat(occupancy).isNotNull();
                    assertThat(occupancy.get("freeSpaces").get(0).get("value").asText()).isEqualTo("1");
                    assertThat(occupancy.get("occupiedSpaces").get(0).get("value").asText()).isEqualTo("0");
                    assertThat(occupancy.get("totalSpaces").get(0).get("value").asText()).isEqualTo("1");
                });
    }

    @Test
    public void testAddEntityToProfile_checkMetricsCalculation() throws Exception {
        Device device3 = createDevice("Device 3", deviceProfile.getId(), "1234567890333");

        postTelemetry(device3.getId(), "{\"occupied\":true}");

        await().alias("add entity to profile and no calculation (there is no relation between device and asset)").atMost(deduplicationInterval, TimeUnit.MILLISECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(this::checkInitialCalculationValues);

        createEntityRelation(asset.getId(), device3.getId(), "Contains");

        await().alias("create relation and perform aggregation").atMost(deduplicationInterval, TimeUnit.MILLISECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode occupancy = getLatestTelemetry(asset.getId(), "freeSpaces", "occupiedSpaces", "totalSpaces");
                    assertThat(occupancy).isNotNull();
                    assertThat(occupancy.get("freeSpaces").get(0).get("value").asText()).isEqualTo("1");
                    assertThat(occupancy.get("occupiedSpaces").get(0).get("value").asText()).isEqualTo("2");
                    assertThat(occupancy.get("totalSpaces").get(0).get("value").asText()).isEqualTo("3");
                });
    }

    @Test
    public void testCfOnProfile_checkMetricsCalculation() throws Exception {
        Asset asset2 = createAsset("Asset 2", assetProfile.getId());
        Device device3 = createDevice("Device 3", deviceProfile.getId(), "1234567890333");
        postTelemetry(device3.getId(), "{\"occupied\":false}");
        Device device4 = createDevice("Device 4", deviceProfile.getId(), "1234567890444");
        postTelemetry(device4.getId(), "{\"occupied\":false}");

        createEntityRelation(asset2.getId(), device3.getId(), "Contains");
        createEntityRelation(asset2.getId(), device4.getId(), "Contains");

        CalculatedField calculatedField2 = createOccupancyCF("Occupied spaces 2", assetProfile.getId(), List.of(deviceProfile.getId()));

        await().alias("create CF and perform initial aggregation").atMost(deduplicationInterval, TimeUnit.MILLISECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode occupancy = getLatestTelemetry(asset.getId(), "freeSpaces", "occupiedSpaces", "totalSpaces");
                    assertThat(occupancy).isNotNull();
                    assertThat(occupancy.get("freeSpaces").get(0).get("value").asText()).isEqualTo("1");
                    assertThat(occupancy.get("occupiedSpaces").get(0).get("value").asText()).isEqualTo("1");
                    assertThat(occupancy.get("totalSpaces").get(0).get("value").asText()).isEqualTo("2");

                    ObjectNode occupancy2 = getLatestTelemetry(asset2.getId(), "freeSpaces", "occupiedSpaces", "totalSpaces");
                    assertThat(occupancy2).isNotNull();
                    assertThat(occupancy2.get("freeSpaces").get(0).get("value").asText()).isEqualTo("2");
                    assertThat(occupancy2.get("occupiedSpaces").get(0).get("value").asText()).isEqualTo("0");
                    assertThat(occupancy2.get("totalSpaces").get(0).get("value").asText()).isEqualTo("2");
                });

        postTelemetry(device3.getId(), "{\"occupied\":true}");

        await().alias("update telemetry and perform aggregation").atMost(deduplicationInterval * 2, TimeUnit.MILLISECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode occupancy2 = getLatestTelemetry(asset2.getId(), "freeSpaces", "occupiedSpaces", "totalSpaces");
                    assertThat(occupancy2).isNotNull();
                    assertThat(occupancy2.get("freeSpaces").get(0).get("value").asText()).isEqualTo("1");
                    assertThat(occupancy2.get("occupiedSpaces").get(0).get("value").asText()).isEqualTo("1");
                    assertThat(occupancy2.get("totalSpaces").get(0).get("value").asText()).isEqualTo("2");
                });
    }


    @Test
    public void testDeleteRelation_checkMetricsCalculation() throws Exception {
        deleteEntityRelation(new EntityRelation(asset.getId(), device1.getId(), "Contains", RelationTypeGroup.COMMON));

        await().alias("create relation and perform aggregation").atMost(deduplicationInterval, TimeUnit.MILLISECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode occupancy = getLatestTelemetry(asset.getId(), "freeSpaces", "occupiedSpaces", "totalSpaces");
                    assertThat(occupancy).isNotNull();
                    assertThat(occupancy.get("freeSpaces").get(0).get("value").asText()).isEqualTo("1");
                    assertThat(occupancy.get("occupiedSpaces").get(0).get("value").asText()).isEqualTo("0");
                    assertThat(occupancy.get("totalSpaces").get(0).get("value").asText()).isEqualTo("1");
                });
    }

//    @Test
//    public void testCfWithoutTargetProfileSpecified_checkMetricsCalculation() throws Exception {
//        Device device3 = createDevice("Device 3", "1234567890333");
//        postTelemetry(device3.getId(), "{\"occupied\":true}");
//        createEntityRelation(asset.getId(), device3.getId(), "Contains");
//
//        var configuration = (LatestValuesAggregationCalculatedFieldConfiguration) calculatedField.getConfiguration();
//        configuration.getSource().setEntityProfiles(Collections.emptyList());
//        calculatedField.setConfiguration(configuration);
//        saveCalculatedField(calculatedField);
//
//        await().alias("update cf and perform aggregation for 3 devices").atMost(deduplicationInterval, TimeUnit.MILLISECONDS)
//                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
//                .untilAsserted(() -> {
//                    ObjectNode occupancy = getLatestTelemetry(asset.getId(), "freeSpaces", "occupiedSpaces", "totalSpaces");
//                    assertThat(occupancy).isNotNull();
//                    assertThat(occupancy.get("freeSpaces").get(0).get("value").asText()).isEqualTo("1");
//                    assertThat(occupancy.get("occupiedSpaces").get(0).get("value").asText()).isEqualTo("2");
//                    assertThat(occupancy.get("totalSpaces").get(0).get("value").asText()).isEqualTo("3");
//                });
//    }

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

    private CalculatedField createOccupancyCF(String name, EntityId entityId, List<EntityId> profiles) {
        Map<String, AggMetric> aggMetrics = new HashMap<>();

        AggMetric freeSpaces = new AggMetric();
        freeSpaces.setFunction(AggFunction.COUNT);
        freeSpaces.setFilter("return oc == false");
        freeSpaces.setInput(new AggKeyInput("oc"));
        aggMetrics.put("freeSpaces", freeSpaces);

        AggMetric occupiedSpaces = new AggMetric();
        occupiedSpaces.setFunction(AggFunction.COUNT);
        occupiedSpaces.setFilter("return oc == true");
        occupiedSpaces.setInput(new AggKeyInput("oc"));
        aggMetrics.put("occupiedSpaces", occupiedSpaces);

        AggMetric totalSpaces = new AggMetric();
        totalSpaces.setFunction(AggFunction.COUNT);
        totalSpaces.setInput(new AggFunctionInput("return 1;"));
        aggMetrics.put("totalSpaces", totalSpaces);

        Output output = new Output();
        output.setType(OutputType.TIME_SERIES);

        return createAggCf(name, entityId,
                buildSource(EntitySearchDirection.FROM, "Contains", profiles),
                Map.of("oc", new ReferencedEntityKey("occupied", ArgumentType.TS_LATEST, null)),
                aggMetrics,
                output);
    }

    private AggSource buildSource(EntitySearchDirection direction, String relationType, List<EntityId> profiles) {
        AggSource source = new AggSource();
        source.setRelation(new RelationPathLevel(direction, relationType));
        source.setEntityProfiles(profiles);
        return source;
    }

    private CalculatedField createAggCf(String name,
                                        EntityId entityId,
                                        AggSource aggSource,
                                        Map<String, ReferencedEntityKey> inputs,
                                        Map<String, AggMetric> metrics,
                                        Output output) {
        CalculatedField calculatedField = new CalculatedField();
        calculatedField.setName(name);
        calculatedField.setEntityId(entityId);
        calculatedField.setType(CalculatedFieldType.LATEST_VALUES_AGGREGATION);

        LatestValuesAggregationCalculatedFieldConfiguration configuration = new LatestValuesAggregationCalculatedFieldConfiguration();
        configuration.setSource(aggSource);
        configuration.setInputs(inputs);
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

    private ObjectNode getLatestTelemetry(EntityId entityId, String... keys) throws Exception {
        return doGetAsync("/api/plugins/telemetry/" + entityId.getEntityType() + "/" + entityId.getId() + "/values/timeseries?keys=" + String.join(",", keys), ObjectNode.class);
    }

}
