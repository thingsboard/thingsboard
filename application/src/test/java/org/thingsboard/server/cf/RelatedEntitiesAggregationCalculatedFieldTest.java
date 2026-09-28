// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.cf;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
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
import org.thingsboard.server.common.data.cf.configuration.AttributesOutput;
import org.thingsboard.server.common.data.cf.configuration.Output;
import org.thingsboard.server.common.data.cf.configuration.ReferencedEntityKey;
import org.thingsboard.server.common.data.cf.configuration.TimeSeriesOutput;
import org.thingsboard.server.common.data.cf.configuration.aggregation.AggFunction;
import org.thingsboard.server.common.data.cf.configuration.aggregation.AggFunctionInput;
import org.thingsboard.server.common.data.cf.configuration.aggregation.AggKeyInput;
import org.thingsboard.server.common.data.cf.configuration.aggregation.AggMetric;
import org.thingsboard.server.common.data.cf.configuration.aggregation.RelatedEntitiesAggregationCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.debug.DebugSettings;
import org.thingsboard.server.common.data.device.data.DefaultDeviceConfiguration;
import org.thingsboard.server.common.data.device.data.DefaultDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.data.DeviceData;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.job.Job;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationPathLevel;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.rule.RuleChain;
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

@DaoSqlTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestPropertySource(properties = {
        "queue.calculated_fields.telemetry_fetch_pack_size=5"
})
public class RelatedEntitiesAggregationCalculatedFieldTest extends AbstractControllerTest {

    private Tenant savedTenant;

    private DeviceProfile deviceProfile;
    private Device device1;
    private String accessToken1 = "1234567890111";
    private Device device2;
    private String accessToken2 = "1234567890222";

    private AssetProfile assetProfile;
    private Asset asset;

    private final long deduplicationInterval = 5;

    @Before
    public void beforeEach() throws Exception {
        loginSysAdmin();

        updateDefaultTenantProfileConfig(tenantProfileConfig -> {
            tenantProfileConfig.setMinAllowedDeduplicationIntervalInSecForCF(1);
            tenantProfileConfig.setMinAllowedScheduledUpdateIntervalInSecForCF(1);
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

        await().alias("create CF and perform initial aggregation").atMost(TIMEOUT, TimeUnit.SECONDS)
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

        await().alias("update telemetry and perform aggregation")
                .atLeast(deduplicationInterval / 2, TimeUnit.SECONDS)
                .atMost(TIMEOUT, TimeUnit.SECONDS)
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

        await().alias("add entity to profile with no related entities and perform aggregation").atMost(deduplicationInterval * 2, TimeUnit.SECONDS)
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

        await().alias("create relations and perform aggregation").atMost(deduplicationInterval * 2, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    verifyTelemetry(asset2.getId(), Map.of(
                            "freeSpaces", "0",
                            "occupiedSpaces", "2",
                            "totalSpaces", "2"
                    ));
                });

        postTelemetry(device3.getId(), "{\"occupied\":false}");

        await().alias("update telemetry and perform aggregation").atMost(deduplicationInterval * 2, TimeUnit.SECONDS)
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

        await().alias("create CF and perform initial aggregation").atMost(deduplicationInterval * 2, TimeUnit.SECONDS)
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

        await().alias("change profile and no aggregation").atMost(deduplicationInterval * 2, TimeUnit.SECONDS)
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

        await().alias("create CF and perform aggregation with default values").atMost(deduplicationInterval * 2, TimeUnit.SECONDS)
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

        await().alias("update telemetry and perform aggregation")
                .atLeast(deduplicationInterval / 2, TimeUnit.SECONDS)
                .atMost(TIMEOUT, TimeUnit.SECONDS)
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
    public void testCreateCfAndRelationToRuleChain_checkAggregation() throws Exception {
        Asset asset2 = createAsset("Asset 2", assetProfile.getId());
        Device device3 = createDevice("Device 3", "1234567890333");
        postTelemetry(device3.getId(), "{\"occupied\":true}");

        RuleChain ruleChain = new RuleChain();
        ruleChain.setName("RuleChain");
        ruleChain = doPost("/api/ruleChain", ruleChain, RuleChain.class);
        postTelemetry(ruleChain.getId(), "{\"occupied\":true}");

        createEntityRelation(asset2.getId(), device3.getId(), "Contains");
        createEntityRelation(asset2.getId(), ruleChain.getId(), "Contains");

        createOccupancyCF(asset2.getId());

        await().alias("create CF and perform initial aggregation").atMost(deduplicationInterval * 2, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    verifyTelemetry(asset2.getId(), Map.of(
                            "freeSpaces", "0",
                            "occupiedSpaces", "1",
                            "totalSpaces", "1"
                    ));
                });

        postTelemetry(ruleChain.getId(), "{\"occupied\":true}");

        await().alias("update telemetry on rule chain and no aggregation performed").atMost(deduplicationInterval * 2, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    verifyTelemetry(asset2.getId(), Map.of(
                            "freeSpaces", "0",
                            "occupiedSpaces", "1",
                            "totalSpaces", "1"
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

        await().alias("delete cf and update telemetry and no aggregation").atMost(deduplicationInterval * 2, TimeUnit.SECONDS)
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

        await().alias("update telemetry -> no changes").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(this::checkInitialCalculationValues);

        postTelemetry(device2.getId(), "{\"occupied\":false}");

        await().alias("create CF and perform initial calculation")
                .atLeast(deduplicationInterval / 2, TimeUnit.SECONDS)
                .atMost(TIMEOUT, TimeUnit.SECONDS)
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

        await().alias("create CF and perform aggregation").atMost(deduplicationInterval * 2, TimeUnit.SECONDS)
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

        await().alias("delete latest telemetry and perform aggregation with previous or default values").atMost(deduplicationInterval * 2, TimeUnit.SECONDS)
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
    public void testDeleteAttr_checkAggregationWithDefault() throws Exception {
        Asset asset2 = createAsset("Asset 2", assetProfile.getId());
        Device device3 = createDevice("Device 3", "1234567890333");
        Device device4 = createDevice("Device 4", "1234567890444");

        createEntityRelation(asset2.getId(), device3.getId(), "Contains");
        createEntityRelation(asset2.getId(), device4.getId(), "Contains");

        postAttributes(device3.getId(), AttributeScope.SERVER_SCOPE, "{\"occupied\":true}");
        postAttributes(device4.getId(), AttributeScope.SERVER_SCOPE, "{\"occupied\":true}");

        createOccupancyCFWithAttr(asset2.getId());

        await().alias("create CF and perform aggregation").atMost(deduplicationInterval * 2, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    verifyTelemetry(asset2.getId(), Map.of(
                            "freeSpaces", "0",
                            "occupiedSpaces", "2",
                            "totalSpaces", "2"
                    ));
                });

        doDelete("/api/plugins/telemetry/DEVICE/" + device3.getUuidId() + "/SERVER_SCOPE?keys=occupied", String.class);
        doDelete("/api/plugins/telemetry/DEVICE/" + device4.getUuidId() + "/SERVER_SCOPE?keys=occupied", String.class);

        await().alias("delete attribute and perform aggregation with default values").atMost(deduplicationInterval * 2, TimeUnit.SECONDS)
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
    public void testCreateRelation_checkAggregation() throws Exception {
        Asset asset2 = createAsset("Asset 2", assetProfile.getId());
        Device device3 = createDevice("Device 3", "1234567890333");
        Device device4 = createDevice("Device 4", "1234567890444");

        createEntityRelation(asset2.getId(), device3.getId(), "Contains");
        createEntityRelation(asset2.getId(), device4.getId(), "Contains");

        createOccupancyCF(assetProfile.getId());

        await().alias("create CF and perform initial aggregation").atMost(TIMEOUT, TimeUnit.SECONDS)
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

        Device device5 = createDevice("Device 5", "1234567890555");
        createEntityRelation(asset2.getId(), device5.getId(), "Contains");

        await().alias("create relation and perform aggregation on asset 2")
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    verifyTelemetry(asset.getId(), Map.of(
                            "freeSpaces", "1",
                            "occupiedSpaces", "1",
                            "totalSpaces", "2"
                    ));

                    verifyTelemetry(asset2.getId(), Map.of(
                            "freeSpaces", "3",
                            "occupiedSpaces", "0",
                            "totalSpaces", "3"
                    ));
                });
    }

    @Test
    public void testDeleteRelation_checkAggregation() throws Exception {
        createOccupancyCF(asset.getId());
        checkInitialCalculation();

        deleteEntityRelation(new EntityRelation(asset.getId(), device1.getId(), "Contains", RelationTypeGroup.COMMON));

        await().alias("create relation and perform aggregation").atMost(deduplicationInterval * 2, TimeUnit.SECONDS)
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
    public void testDeleteEntityByRelation_checkAggregation() throws Exception {
        createOccupancyCF(asset.getId());
        checkInitialCalculation();

        doDelete("/api/device/" + device1.getId()).andExpect(status().isOk());

        await().alias("create relation and perform aggregation").atMost(deduplicationInterval * 2, TimeUnit.SECONDS)
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

        var configuration = (RelatedEntitiesAggregationCalculatedFieldConfiguration) cf.getConfiguration();
        configuration.setRelation(new RelationPathLevel(EntitySearchDirection.FROM, "Has"));
        saveCalculatedField(cf);

        await().alias("update relation path and perform aggregation").atMost(deduplicationInterval * 2, TimeUnit.SECONDS)
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

        var configuration = (RelatedEntitiesAggregationCalculatedFieldConfiguration) cf.getConfiguration();
        Argument argument = new Argument();
        argument.setRefEntityKey(new ReferencedEntityKey("oc", ArgumentType.TS_LATEST, null));
        argument.setDefaultValue("false");
        configuration.setArguments(Map.of("oc", argument));
        saveCalculatedField(cf);

        await().alias("update arguments and perform aggregation").atMost(deduplicationInterval * 2, TimeUnit.SECONDS)
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

        await().alias("create avg temp cf and perform initial aggregation").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    verifyTelemetry(asset.getId(), Map.of("avgTemperature", "24"));
                });

        var configuration = (RelatedEntitiesAggregationCalculatedFieldConfiguration) cf.getConfiguration();
        AggMetric aggMetric = new AggMetric();
        aggMetric.setInput(new AggKeyInput("temp"));
        aggMetric.setFilter("return temp < 100;");
        aggMetric.setFunction(AggFunction.MAX);
        configuration.setMetrics(Map.of("maxTemperature", aggMetric));
        saveCalculatedField(cf);

        await().alias("update metrics and perform aggregation").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    verifyTelemetry(asset.getId(), Map.of("maxTemperature", "24"));
                });

        postTelemetry(device1.getId(), "{\"temperature\":101.3}");
        postTelemetry(device2.getId(), "{\"temperature\":25.8}");

        await().alias("update telemetry and perform aggregation")
                .atLeast(deduplicationInterval / 2, TimeUnit.SECONDS)
                .atMost(TIMEOUT, TimeUnit.SECONDS)
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

        await().alias("create avg temp cf and perform initial aggregation").atMost(deduplicationInterval * 2, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    verifyTelemetry(asset.getId(), Map.of("avgTemperature", "24"));
                });

        var configuration = (RelatedEntitiesAggregationCalculatedFieldConfiguration) cf.getConfiguration();
        AttributesOutput output = new AttributesOutput();
        output.setScope(AttributeScope.SERVER_SCOPE);
        configuration.setOutput(output);
        saveCalculatedField(cf);

        await().alias("update output and perform aggregation").atMost(deduplicationInterval * 2, TimeUnit.SECONDS)
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

        await().alias("create avg temp cf and perform initial aggregation").atMost(deduplicationInterval * 2, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    verifyTelemetry(asset.getId(), Map.of("avgTemperature", "24"));
                });

        var configuration = (RelatedEntitiesAggregationCalculatedFieldConfiguration) cf.getConfiguration();
        configuration.setDeduplicationIntervalInSec(2 * deduplicationInterval);
        saveCalculatedField(cf);

        await().alias("update deduplication interval and perform aggregation").atMost(deduplicationInterval / 2, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    verifyTelemetry(asset.getId(), Map.of("avgTemperature", "24"));
                });

        postTelemetry(device2.getId(), "{\"temperature\":32.1}");

        await().alias("update telemetry and perform aggregation").atMost(2 * deduplicationInterval + 10, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    verifyTelemetry(asset.getId(), Map.of("avgTemperature", "28"));
                });
    }

    @Test
    public void testReprocessing() throws Exception {
        long currentTime = System.currentTimeMillis();
        // reprocessing time window(TW)
        long startTs = currentTime - TimeUnit.SECONDS.toMillis(120);
        long endTs = currentTime - TimeUnit.SECONDS.toMillis(45);

        long d_1_1Ts = currentTime - TimeUnit.SECONDS.toMillis(130); // outside the TW (but telemetry will be used for initial processing)
        long d_2_1Ts = currentTime - TimeUnit.SECONDS.toMillis(100); // inside the TW
        long d_1_2_2Ts = currentTime - TimeUnit.SECONDS.toMillis(80); // inside the TW
        long d_1_3Ts = currentTime - TimeUnit.SECONDS.toMillis(60); // inside the TW
        long d_2_3Ts = currentTime - TimeUnit.SECONDS.toMillis(30); // outside the TW

        postTelemetry(device1.getId(), String.format("{\"ts\":%s, \"values\":{\"temperature\":112}}", d_1_1Ts));
        postTelemetry(device1.getId(), String.format("{\"ts\":%s, \"values\":{\"temperature\":160}}", d_1_2_2Ts));
        postTelemetry(device1.getId(), String.format("{\"ts\":%s, \"values\":{\"temperature\":135}}", d_1_3Ts));

        postTelemetry(device2.getId(), String.format("{\"ts\":%s, \"values\":{\"temperature\":185}}", d_2_1Ts));
        postTelemetry(device2.getId(), String.format("{\"ts\":%s, \"values\":{\"temperature\":130}}", d_1_2_2Ts));
        postTelemetry(device2.getId(), String.format("{\"ts\":%s, \"values\":{\"temperature\":171}}", d_2_3Ts));

        /*      telemetry flow:
                          startTs               endTs
                             |_____________________|
               |  ts   |  1  |    2      3      4  |   5
               |device1| 112 |->     -> 160 -> 135 |
               |device2|     |-> 185 -> 130 ->     |-> 171
                             |_____________________|
                                        |--- reprocessing time window
               the result should be: 66 -> 149 -> 145 -> 133
        */

        CalculatedField cf = createAvgTemperatureCF(asset.getId());

        doGet("/api/calculatedField/" + cf.getUuidId() + "/reprocess?startTs={startTs}&endTs={endTs}", Job.class, startTs, endTs);

        await().alias("reprocess -> perform aggregation").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode airDensity = getTimeSeries(asset.getId(), startTs, endTs, "avgTemperature");
                    assertThat(airDensity).isNotNull();

                    assertThat(airDensity.get("avgTemperature").get(0).get("ts").asText()).isEqualTo(Long.toString(d_1_3Ts));
                    assertThat(airDensity.get("avgTemperature").get(0).get("value").asText()).isEqualTo("133");

                    assertThat(airDensity.get("avgTemperature").get(1).get("ts").asText()).isEqualTo(Long.toString(d_1_2_2Ts));
                    assertThat(airDensity.get("avgTemperature").get(1).get("value").asText()).isEqualTo("145");

                    assertThat(airDensity.get("avgTemperature").get(2).get("ts").asText()).isEqualTo(Long.toString(d_2_1Ts));
                    assertThat(airDensity.get("avgTemperature").get(2).get("value").asText()).isEqualTo("149");

                    assertThat(airDensity.get("avgTemperature").get(3).get("ts").asText()).isEqualTo(Long.toString(startTs));
                    assertThat(airDensity.get("avgTemperature").get(3).get("value").asText()).isEqualTo("66");
                });
    }

    @Test
    public void testReprocessing_whenTelemetryExceedsFetchPackSize() throws Exception {
        long currentTime = System.currentTimeMillis();
        // reprocessing time window(TW)
        long startTs = currentTime - TimeUnit.SECONDS.toMillis(120);
        long endTs = currentTime - TimeUnit.SECONDS.toMillis(45);

        // 7 datapoints per related entity, all inside the reprocessing window.
        // Fetch pack size is 5, so each per-entity buffer
        // drains after t5 and must be refilled to reach t6 and t7.
        long t1 = currentTime - TimeUnit.SECONDS.toMillis(105);
        long t2 = currentTime - TimeUnit.SECONDS.toMillis(90);
        long t3 = currentTime - TimeUnit.SECONDS.toMillis(70);
        long t4 = currentTime - TimeUnit.SECONDS.toMillis(65);
        long t5 = currentTime - TimeUnit.SECONDS.toMillis(60);
        long t6 = currentTime - TimeUnit.SECONDS.toMillis(55);
        long t7 = currentTime - TimeUnit.SECONDS.toMillis(50);

        postTelemetry(device1.getId(), String.format("{\"ts\":%s, \"values\":{\"temperature\":20}}", t1));
        postTelemetry(device1.getId(), String.format("{\"ts\":%s, \"values\":{\"temperature\":30}}", t2));
        postTelemetry(device1.getId(), String.format("{\"ts\":%s, \"values\":{\"temperature\":40}}", t3));
        postTelemetry(device1.getId(), String.format("{\"ts\":%s, \"values\":{\"temperature\":50}}", t4));
        postTelemetry(device1.getId(), String.format("{\"ts\":%s, \"values\":{\"temperature\":60}}", t5));
        postTelemetry(device1.getId(), String.format("{\"ts\":%s, \"values\":{\"temperature\":70}}", t6));
        postTelemetry(device1.getId(), String.format("{\"ts\":%s, \"values\":{\"temperature\":80}}", t7));

        postTelemetry(device2.getId(), String.format("{\"ts\":%s, \"values\":{\"temperature\":22}}", t1));
        postTelemetry(device2.getId(), String.format("{\"ts\":%s, \"values\":{\"temperature\":32}}", t2));
        postTelemetry(device2.getId(), String.format("{\"ts\":%s, \"values\":{\"temperature\":42}}", t3));
        postTelemetry(device2.getId(), String.format("{\"ts\":%s, \"values\":{\"temperature\":52}}", t4));
        postTelemetry(device2.getId(), String.format("{\"ts\":%s, \"values\":{\"temperature\":62}}", t5));
        postTelemetry(device2.getId(), String.format("{\"ts\":%s, \"values\":{\"temperature\":72}}", t6));
        postTelemetry(device2.getId(), String.format("{\"ts\":%s, \"values\":{\"temperature\":82}}", t7));

        CalculatedField cf = createAvgTemperatureCF(asset.getId());

        doGet("/api/calculatedField/" + cf.getUuidId() + "/reprocess?startTs={startTs}&endTs={endTs}", Job.class, startTs, endTs);

        await().alias("reprocess -> perform aggregation").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode result = getTimeSeries(asset.getId(), startTs, endTs, "avgTemperature");
                    assertThat(result).isNotNull();

                    assertThat(result.get("avgTemperature").get(0).get("ts").asText()).isEqualTo(Long.toString(t7));
                    assertThat(result.get("avgTemperature").get(0).get("value").asText()).isEqualTo("81");

                    assertThat(result.get("avgTemperature").get(1).get("ts").asText()).isEqualTo(Long.toString(t6));
                    assertThat(result.get("avgTemperature").get(1).get("value").asText()).isEqualTo("71");

                    assertThat(result.get("avgTemperature").get(2).get("ts").asText()).isEqualTo(Long.toString(t5));
                    assertThat(result.get("avgTemperature").get(2).get("value").asText()).isEqualTo("61");

                    assertThat(result.get("avgTemperature").get(3).get("ts").asText()).isEqualTo(Long.toString(t4));
                    assertThat(result.get("avgTemperature").get(3).get("value").asText()).isEqualTo("51");

                    assertThat(result.get("avgTemperature").get(4).get("ts").asText()).isEqualTo(Long.toString(t3));
                    assertThat(result.get("avgTemperature").get(4).get("value").asText()).isEqualTo("41");

                    assertThat(result.get("avgTemperature").get(5).get("ts").asText()).isEqualTo(Long.toString(t2));
                    assertThat(result.get("avgTemperature").get(5).get("value").asText()).isEqualTo("31");

                    assertThat(result.get("avgTemperature").get(6).get("ts").asText()).isEqualTo(Long.toString(t1));
                    assertThat(result.get("avgTemperature").get(6).get("value").asText()).isEqualTo("21");
                });
    }

    @Test
    public void testTheSameRelationTypeAndKeyButDifferentCfs_checkAggregation() throws Exception {
        postTelemetry(device1.getId(), "{\"temperature\":24.2}");
        postTelemetry(device2.getId(), "{\"temperature\":19.6}");
        CalculatedField cf = createAvgTemperatureCF(asset.getId());

        Asset asset2 = createAsset("Asset 2", assetProfile.getId());
        Device device3 = createDevice("Device 3", "1234567890333");
        createEntityRelation(asset2.getId(), device3.getId(), "Contains");
        postTelemetry(device3.getId(), "{\"temperature\":10.2}");

        CalculatedField calculatedField = new CalculatedField();
        calculatedField.setName("Average temperature");
        calculatedField.setEntityId(asset2.getId());
        calculatedField.setType(CalculatedFieldType.RELATED_ENTITIES_AGGREGATION);

        var config = (RelatedEntitiesAggregationCalculatedFieldConfiguration) cf.getConfiguration();
        AggMetric avgMetric = new AggMetric();
        avgMetric.setFunction(AggFunction.AVG);
        avgMetric.setInput(new AggKeyInput("temp"));
        config.setMetrics(Map.of("avgTemperature_2", avgMetric));

        calculatedField.setConfiguration(config);
        calculatedField.setDebugSettings(DebugSettings.all());
        saveCalculatedField(calculatedField);

        await().alias("create avg temp cf and perform initial aggregation").atMost(deduplicationInterval * 2, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode avgTemperature = getLatestTelemetry(asset.getId(), "avgTemperature", "avgTemperature_2");
                    assertThat(avgTemperature).isNotNull();
                    assertThat(avgTemperature.get("avgTemperature").get(0).get("value").asText()).isEqualTo("24");
                    assertThat(avgTemperature.get("avgTemperature_2").get(0).get("value").isNull()).isTrue();
                });

        postTelemetry(device1.getId(), "{\"temperature\":26.2}");

        await().alias("create avg temp cf and perform initial aggregation").atMost(deduplicationInterval * 2, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode avgTemperature = getLatestTelemetry(asset.getId(), "avgTemperature", "avgTemperature_2");
                    assertThat(avgTemperature).isNotNull();
                    assertThat(avgTemperature.get("avgTemperature").get(0).get("value").asText()).isEqualTo("26");
                    assertThat(avgTemperature.get("avgTemperature_2").get(0).get("value").isNull()).isTrue();
                });
    }

    @Test
    public void testUpdateMaxRelatedEntitiesPerArgument_checkAggregation() throws Exception {
        loginSysAdmin();

        updateDefaultTenantProfileConfig(tenantProfileConfig -> {
            tenantProfileConfig.setMaxRelatedEntitiesToReturnPerCfArgument(1);
        });

        login("tenant@thingsboard.org", "testPassword");

        createCountCF(asset.getId());

        await().alias("create CF and perform initial aggregation").atMost(deduplicationInterval * 2, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode numberOfDevices = getLatestTelemetry(asset.getId(), "numberOfDevices");
                    assertThat(numberOfDevices).isNotNull();
                    assertThat(numberOfDevices.get("numberOfDevices").get(0).get("value").asText()).isEqualTo("1");
                });

        loginSysAdmin();

        updateDefaultTenantProfileConfig(tenantProfileConfig -> {
            tenantProfileConfig.setMaxRelatedEntitiesToReturnPerCfArgument(10);
        });

        login("tenant@thingsboard.org", "testPassword");

        await().alias("update max related entities per argument and perform initial aggregation").atMost(deduplicationInterval * 2, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode numberOfDevices = getLatestTelemetry(asset.getId(), "numberOfDevices");
                    assertThat(numberOfDevices).isNotNull();
                    assertThat(numberOfDevices.get("numberOfDevices").get(0).get("value").asText()).isEqualTo("2");
                });
    }

    private void checkInitialCalculation() {
        await().alias("create CF and perform initial aggregation").atMost(deduplicationInterval * 2, TimeUnit.SECONDS)
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

        TimeSeriesOutput output = new TimeSeriesOutput();
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

        TimeSeriesOutput output = new TimeSeriesOutput();
        output.setDecimalsByDefault(0);

        return createAggCf("Occupied spaces", entityId,
                new RelationPathLevel(EntitySearchDirection.FROM, "Contains"),
                arguments,
                aggMetrics,
                output);
    }

    private CalculatedField createOccupancyCFWithAttr(EntityId entityId) {
        Map<String, Argument> arguments = new HashMap<>();
        Argument argument = new Argument();
        argument.setRefEntityKey(new ReferencedEntityKey("occupied", ArgumentType.ATTRIBUTE, AttributeScope.SERVER_SCOPE));
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

        TimeSeriesOutput output = new TimeSeriesOutput();
        output.setDecimalsByDefault(0);

        return createAggCf("Occupied spaces", entityId,
                new RelationPathLevel(EntitySearchDirection.FROM, "Contains"),
                arguments,
                aggMetrics,
                output);
    }

    private CalculatedField createCountCF(EntityId entityId) {
        Map<String, Argument> arguments = new HashMap<>();
        Argument argument = new Argument();
        argument.setRefEntityKey(new ReferencedEntityKey("active", ArgumentType.TS_LATEST, null));
        argument.setDefaultValue("true");
        arguments.put("active", argument);

        Map<String, AggMetric> aggMetrics = new HashMap<>();

        AggMetric avgMetric = new AggMetric();
        avgMetric.setFunction(AggFunction.COUNT);
        avgMetric.setInput(new AggKeyInput("active"));
        aggMetrics.put("numberOfDevices", avgMetric);

        TimeSeriesOutput output = new TimeSeriesOutput();
        output.setDecimalsByDefault(0);

        return createAggCf("Number of devices", entityId,
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
        calculatedField.setType(CalculatedFieldType.RELATED_ENTITIES_AGGREGATION);

        RelatedEntitiesAggregationCalculatedFieldConfiguration configuration = new RelatedEntitiesAggregationCalculatedFieldConfiguration();
        configuration.setRelation(relation);
        configuration.setArguments(inputs);
        configuration.setDeduplicationIntervalInSec(deduplicationInterval);
        configuration.setScheduledUpdateInterval(10);
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

    private ObjectNode getTimeSeries(EntityId entityId, long startTs, long endTs, String... keys) throws Exception {
        return doGetAsync("/api/plugins/telemetry/" + entityId.getEntityType() + "/" + entityId.getId() + "/values/timeseries?keys={keys}&startTs={startTs}&endTs={endTs}", ObjectNode.class, String.join(",", keys), startTs, endTs);
    }

    private ObjectNode getLatestTelemetry(EntityId entityId, String... keys) throws Exception {
        return doGetAsync("/api/plugins/telemetry/" + entityId.getEntityType() + "/" + entityId.getId() + "/values/timeseries?keys=" + String.join(",", keys), ObjectNode.class);
    }

    private ArrayNode getServerAttributes(EntityId entityId, String... keys) throws Exception {
        return doGetAsync("/api/plugins/telemetry/" + entityId.getEntityType() + "/" + entityId.getId() + "/values/attributes/SERVER_SCOPE?keys=" + String.join(",", keys), ArrayNode.class);
    }

}
