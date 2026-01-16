/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.msa.cf;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.commons.lang3.RandomStringUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldEventType;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.ArgumentType;
import org.thingsboard.server.common.data.cf.configuration.AttributesOutput;
import org.thingsboard.server.common.data.cf.configuration.PropagationCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.ReferencedEntityKey;
import org.thingsboard.server.common.data.cf.configuration.RelationPathQueryDynamicSourceConfiguration;
import org.thingsboard.server.common.data.cf.configuration.ScriptCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.SimpleCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.TimeSeriesOutput;
import org.thingsboard.server.common.data.cf.configuration.aggregation.AggFunction;
import org.thingsboard.server.common.data.cf.configuration.aggregation.AggFunctionInput;
import org.thingsboard.server.common.data.cf.configuration.aggregation.AggKeyInput;
import org.thingsboard.server.common.data.cf.configuration.aggregation.AggMetric;
import org.thingsboard.server.common.data.cf.configuration.aggregation.RelatedEntitiesAggregationCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.geofencing.EntityCoordinates;
import org.thingsboard.server.common.data.cf.configuration.geofencing.GeofencingCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.geofencing.ZoneGroupConfiguration;
import org.thingsboard.server.common.data.debug.DebugSettings;
import org.thingsboard.server.common.data.device.data.DefaultDeviceConfiguration;
import org.thingsboard.server.common.data.device.data.DefaultDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.data.DeviceData;
import org.thingsboard.server.common.data.event.EventType;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationPathLevel;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileData;
import org.thingsboard.server.msa.AbstractContainerTest;
import org.thingsboard.server.msa.ui.utils.EntityPrototypes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.thingsboard.server.common.data.AttributeScope.SERVER_SCOPE;
import static org.thingsboard.server.common.data.cf.configuration.geofencing.GeofencingReportStrategy.REPORT_TRANSITION_EVENTS_AND_PRESENCE_STATUS;
import static org.thingsboard.server.msa.ui.utils.EntityPrototypes.defaultAssetProfile;
import static org.thingsboard.server.msa.ui.utils.EntityPrototypes.defaultDeviceProfile;
import static org.thingsboard.server.msa.ui.utils.EntityPrototypes.defaultTenantAdmin;

public class CalculatedFieldTest extends AbstractContainerTest {

    public final int TIMEOUT = 60;
    public final int POLL_INTERVAL = 1;

    private final String exampleScript = """
            var avgTemperature = temperature.mean(); // Get average temperature
            var temperatureK = (avgTemperature - 32) * (5 / 9) + 273.15; // Convert Fahrenheit to Kelvin
            
            // Estimate air pressure based on altitude
            var pressure = 101325 * Math.pow((1 - 2.25577e-5 * altitude), 5.25588);
            
            // Air density formula
            var airDensity = pressure / (287.05 * temperatureK);
            
            return {
                "airDensity": toFixed(airDensity, 2)
            }
            """;

    private final String deviceToken = "zmzURIVRsq3lvnTP2XBE";

    private TenantId tenantId;
    private UserId tenantAdminId;
    private DeviceProfileId deviceProfileId;
    private AssetProfileId assetProfileId;
    private Device device;
    private Asset asset;

    @BeforeClass
    public void beforeClass() {
        testRestClient.login("sysadmin@thingsboard.org", "sysadmin");

        updateDefaultTenantProfile(tenantProfile -> {
            TenantProfileData profileData = tenantProfile.getProfileData();
            DefaultTenantProfileConfiguration profileConfiguration = (DefaultTenantProfileConfiguration) profileData.getConfiguration();
            profileConfiguration.setMinAllowedDeduplicationIntervalInSecForCF(1);
            profileConfiguration.setMinAllowedScheduledUpdateIntervalInSecForCF(1);
            tenantProfile.setProfileData(profileData);
        });

        tenantId = testRestClient.postTenant(EntityPrototypes.defaultTenantPrototype("Tenant")).getId();
        tenantAdminId = testRestClient.createUserAndLogin(defaultTenantAdmin(tenantId, "tenantAdmin@thingsboard.org"), "tenant");
    }

    @BeforeMethod
    public void beforeMethod() {
        testRestClient.getAndSetUserToken(tenantAdminId);

        deviceProfileId = testRestClient.postDeviceProfile(defaultDeviceProfile("Device Profile")).getId();
        device = testRestClient.postDevice(deviceToken, createDevice("Device", deviceProfileId));

        assetProfileId = testRestClient.postAssetProfile(defaultAssetProfile("Asset Profile")).getId();
        asset = testRestClient.postAsset(createAsset("Asset", assetProfileId));
    }

    @AfterMethod
    public void tearDown() {
        testRestClient.getAndSetUserToken(tenantAdminId);

        testRestClient.deleteDeviceIfExists(device.getId());
        testRestClient.deleteDeviceProfile(deviceProfileId);

        testRestClient.deleteAsset(asset.getId());
        testRestClient.deleteAssetProfile(assetProfileId);
    }

    @AfterClass
    public void afterClass() {
        testRestClient.resetToken();
        testRestClient.login("sysadmin@thingsboard.org", "sysadmin");
        testRestClient.deleteTenant(tenantId);
    }

    @Test
    public void testPerformInitialCalculationForSimpleType() {
        testRestClient.postTelemetry(deviceToken, JacksonUtil.toJsonNode("{\"temperature\":25}"));
        waitInitialTelemetry(device.getId(), "temperature");

        CalculatedField savedCalculatedField = createSimpleCalculatedField(device.getId());

        await().alias("create CF -> perform initial calculation").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    JsonNode fahrenheitTemp = testRestClient.getLatestTelemetry(device.getId());
                    assertThat(fahrenheitTemp).isNotNull();
                    assertThat(fahrenheitTemp.get("fahrenheitTemp")).isNotNull();
                    assertThat(fahrenheitTemp.get("fahrenheitTemp").get(0).get("value").asText()).isEqualTo("77.0");
                });

        testRestClient.deleteCalculatedFieldIfExists(savedCalculatedField.getId());
    }

    @Test
    public void testChangeConfigArgument() {
        testRestClient.postTelemetry(deviceToken, JacksonUtil.toJsonNode("{\"temperature\":25}"));
        waitInitialTelemetry(device.getId(), "temperature");
        testRestClient.postTelemetryAttribute(device.getId(), SERVER_SCOPE, JacksonUtil.toJsonNode("{\"deviceTemperature\":40}"));

        CalculatedField savedCalculatedField = createSimpleCalculatedField(device.getId());

        await().alias("create CF -> perform initial calculation").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    JsonNode fahrenheitTemp = testRestClient.getLatestTelemetry(device.getId());
                    assertThat(fahrenheitTemp).isNotNull();
                    assertThat(fahrenheitTemp.get("fahrenheitTemp")).isNotNull();
                    assertThat(fahrenheitTemp.get("fahrenheitTemp").get(0).get("value").asText()).isEqualTo("77.0");
                });

        assertThat(savedCalculatedField.getConfiguration() instanceof SimpleCalculatedFieldConfiguration).isTrue();

        Argument savedArgument = ((SimpleCalculatedFieldConfiguration) savedCalculatedField.getConfiguration()).getArguments().get("T");
        savedArgument.setRefEntityKey(new ReferencedEntityKey("deviceTemperature", ArgumentType.ATTRIBUTE, SERVER_SCOPE));
        testRestClient.postCalculatedField(savedCalculatedField);

        await().alias("update CF argument -> perform calculation with new argument").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    JsonNode fahrenheitTemp = testRestClient.getLatestTelemetry(device.getId());
                    assertThat(fahrenheitTemp).isNotNull();
                    assertThat(fahrenheitTemp.get("fahrenheitTemp")).isNotNull();
                    assertThat(fahrenheitTemp.get("fahrenheitTemp").get(0).get("value").asText()).isEqualTo("104.0");
                });

        testRestClient.deleteCalculatedFieldIfExists(savedCalculatedField.getId());
    }

    @Test
    public void testChangeConfigOutput() {
        testRestClient.postTelemetry(deviceToken, JacksonUtil.toJsonNode("{\"temperature\":25}"));
        waitInitialTelemetry(device.getId(), "temperature");

        CalculatedField savedCalculatedField = createSimpleCalculatedField(device.getId());

        await().alias("create CF -> perform initial calculation").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    JsonNode fahrenheitTemp = testRestClient.getLatestTelemetry(device.getId());
                    assertThat(fahrenheitTemp).isNotNull();
                    assertThat(fahrenheitTemp.get("fahrenheitTemp")).isNotNull();
                    assertThat(fahrenheitTemp.get("fahrenheitTemp").get(0).get("value").asText()).isEqualTo("77.0");
                });

        AttributesOutput output = new AttributesOutput();
        output.setScope(SERVER_SCOPE);
        output.setName("temperatureF");
        ((SimpleCalculatedFieldConfiguration) savedCalculatedField.getConfiguration()).setOutput(output);

        testRestClient.postCalculatedField(savedCalculatedField);

        await().alias("update CF output -> perform calculation with updated output").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ArrayNode temperatureF = testRestClient.getAttributes(device.getId(), SERVER_SCOPE, "temperatureF");
                    assertThat(temperatureF).isNotNull();
                    assertThat(temperatureF.get(0)).isNotNull();
                    assertThat(temperatureF.get(0).get("value").asText()).isEqualTo("77.0");
                });

        testRestClient.deleteCalculatedFieldIfExists(savedCalculatedField.getId());
    }

    @Test
    public void testChangeConfigExpression() {
        testRestClient.postTelemetry(deviceToken, JacksonUtil.toJsonNode("{\"temperature\":25}"));
        waitInitialTelemetry(device.getId(), "temperature");

        CalculatedField savedCalculatedField = createSimpleCalculatedField(device.getId());
        assertThat(savedCalculatedField.getConfiguration() instanceof SimpleCalculatedFieldConfiguration).isTrue();

        await().alias("create CF -> perform initial calculation").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    JsonNode fahrenheitTemp = testRestClient.getLatestTelemetry(device.getId());
                    assertThat(fahrenheitTemp).isNotNull();
                    assertThat(fahrenheitTemp.get("fahrenheitTemp")).isNotNull();
                    assertThat(fahrenheitTemp.get("fahrenheitTemp").get(0).get("value").asText()).isEqualTo("77.0");
                });

        savedCalculatedField.setName("F to C");
        ((SimpleCalculatedFieldConfiguration) savedCalculatedField.getConfiguration()).setExpression("(T - 32) / 1.8");
        testRestClient.postCalculatedField(savedCalculatedField);

        await().alias("update CF expression -> perform calculation with new expression").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    JsonNode fahrenheitTemp = testRestClient.getLatestTelemetry(device.getId());
                    assertThat(fahrenheitTemp).isNotNull();
                    assertThat(fahrenheitTemp.get("fahrenheitTemp")).isNotNull();
                    assertThat(fahrenheitTemp.get("fahrenheitTemp").get(0).get("value").asText()).isEqualTo("-3.89");
                });

        testRestClient.deleteCalculatedFieldIfExists(savedCalculatedField.getId());
    }

    @Test
    public void testTelemetryUpdated() {
        testRestClient.postTelemetry(deviceToken, JacksonUtil.toJsonNode("{\"temperature\":25}"));
        waitInitialTelemetry(device.getId(), "temperature");

        CalculatedField savedCalculatedField = createSimpleCalculatedField(device.getId());

        await().alias("create CF -> perform initial calculation").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    JsonNode fahrenheitTemp = testRestClient.getLatestTelemetry(device.getId());
                    assertThat(fahrenheitTemp).isNotNull();
                    assertThat(fahrenheitTemp.get("fahrenheitTemp")).isNotNull();
                    assertThat(fahrenheitTemp.get("fahrenheitTemp").get(0).get("value").asText()).isEqualTo("77.0");
                });

        testRestClient.postTelemetry(deviceToken, JacksonUtil.toJsonNode("{\"temperature\":30}"));

        await().alias("update telemetry -> recalculate state").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    JsonNode fahrenheitTemp = testRestClient.getLatestTelemetry(device.getId());
                    assertThat(fahrenheitTemp).isNotNull();
                    assertThat(fahrenheitTemp.get("fahrenheitTemp")).isNotNull();
                    assertThat(fahrenheitTemp.get("fahrenheitTemp").get(0).get("value").asText()).isEqualTo("86.0");
                });

        testRestClient.deleteCalculatedFieldIfExists(savedCalculatedField.getId());
    }

    @Test
    public void testEntityIdIsProfile() {
        testRestClient.postTelemetry(deviceToken, JacksonUtil.toJsonNode("{\"temperature\":25}"));
        waitInitialTelemetry(device.getId(), "temperature");

        CalculatedField savedCalculatedField = createSimpleCalculatedField(deviceProfileId);

        await().alias("create CF -> perform initial calculation for device by profile").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    JsonNode fahrenheitTemp = testRestClient.getLatestTelemetry(device.getId());
                    assertThat(fahrenheitTemp).isNotNull();
                    assertThat(fahrenheitTemp.get("fahrenheitTemp")).isNotNull();
                    assertThat(fahrenheitTemp.get("fahrenheitTemp").get(0).get("value").asText()).isEqualTo("77.0");
                });

        testRestClient.deleteCalculatedFieldIfExists(savedCalculatedField.getId());
    }

    @Test
    public void testEntityAddedAndDeleted() {
        testRestClient.postTelemetry(deviceToken, JacksonUtil.toJsonNode("{\"temperature\":25}"));
        waitInitialTelemetry(device.getId(), "temperature");

        CalculatedField savedCalculatedField = createSimpleCalculatedField(deviceProfileId);

        await().alias("create CF -> perform initial calculation for device by profile").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    JsonNode fahrenheitTemp = testRestClient.getLatestTelemetry(device.getId());
                    assertThat(fahrenheitTemp).isNotNull();
                    assertThat(fahrenheitTemp.get("fahrenheitTemp")).isNotNull();
                    assertThat(fahrenheitTemp.get("fahrenheitTemp").get(0).get("value").asText()).isEqualTo("77.0");
                });

        String deviceToken2 = "beeUmKVqsq3lvnovt6BE";
        Device device2 = testRestClient.postDevice(deviceToken2, createDevice("Device 2", deviceProfileId));

        await().alias("create device by profile -> perform initial calculation for new device by profile").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    // used default value since telemetry is not present
                    JsonNode fahrenheitTemp = testRestClient.getLatestTelemetry(device2.getId());
                    assertThat(fahrenheitTemp).isNotNull();
                    assertThat(fahrenheitTemp.get("fahrenheitTemp")).isNotNull();
                    assertThat(fahrenheitTemp.get("fahrenheitTemp").get(0).get("value").asText()).isEqualTo("53.6");
                });

        DeviceProfile newDeviceProfile = testRestClient.postDeviceProfile(defaultDeviceProfile("Test Profile"));
        device2.setDeviceProfileId(newDeviceProfile.getId());
        testRestClient.postDevice(deviceToken2, device2);

        testRestClient.postTelemetry(deviceToken2, JacksonUtil.toJsonNode("{\"temperature\":25}"));

        await().alias("update telemetry -> no updates").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    JsonNode fahrenheitTemp = testRestClient.getLatestTelemetry(device2.getId());
                    assertThat(fahrenheitTemp).isNotNull();
                    assertThat(fahrenheitTemp.get("fahrenheitTemp")).isNotNull();
                    assertThat(fahrenheitTemp.get("fahrenheitTemp").get(0).get("value").asText()).isEqualTo("53.6");
                });

        testRestClient.deleteCalculatedFieldIfExists(savedCalculatedField.getId());
        testRestClient.deleteDeviceIfExists(device2.getId());
        testRestClient.deleteDeviceProfileIfExists(newDeviceProfile);
    }

    @Test
    public void testEntityIdIsProfileAndRefEntityIsCommon() {
        testRestClient.postTelemetry(deviceToken, JacksonUtil.toJsonNode("{\"temperatureInF\":72.32}"));
        testRestClient.postTelemetry(deviceToken, JacksonUtil.toJsonNode("{\"temperatureInF\":72.86}"));
        testRestClient.postTelemetry(deviceToken, JacksonUtil.toJsonNode("{\"temperatureInF\":73.58}"));
        waitInitialTelemetry(device.getId(), "temperatureInF");

        testRestClient.postTelemetryAttribute(asset.getId(), SERVER_SCOPE, JacksonUtil.toJsonNode("{\"altitude\":1035}"));

        CalculatedField savedCalculatedField = createScriptCalculatedField(deviceProfileId, asset.getId());

        await().alias("create CF -> perform initial calculation for device by profile").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    JsonNode airDensity = testRestClient.getLatestTelemetry(device.getId());
                    assertThat(airDensity).isNotNull();
                    assertThat(airDensity.get("airDensity")).isNotNull();
                    assertThat(airDensity.get("airDensity").get(0).get("value").asText()).isEqualTo("1.05");
                });

        testRestClient.postTelemetryAttribute(asset.getId(), SERVER_SCOPE, JacksonUtil.toJsonNode("{\"altitude\":1531}"));

        await().alias("create CF -> update telemetry for common entity").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    JsonNode airDensity = testRestClient.getLatestTelemetry(device.getId());
                    assertThat(airDensity).isNotNull();
                    assertThat(airDensity.get("airDensity")).isNotNull();
                    assertThat(airDensity.get("airDensity").get(0).get("value").asText()).isEqualTo("0.99");
                });

        testRestClient.deleteCalculatedFieldIfExists(savedCalculatedField.getId());
    }

    @Test
    public void testPerformSerialsOfCalculationsForGeofencingType() {
        // Device and initial coords (inside Allowed, outside Restricted)
        String deviceToken = "geoDeviceTokenA";
        Device device = testRestClient.postDevice(deviceToken, createDevice("GF Device", deviceProfileId));
        testRestClient.postTelemetry(deviceToken, JacksonUtil.toJsonNode("{\"latitude\":50.4730,\"longitude\":30.5050}"));

        // Create zones
        Asset allowed = testRestClient.postAsset(createAsset("Allowed Zone", null));
        testRestClient.postTelemetryAttribute(allowed.getId(), SERVER_SCOPE,
                JacksonUtil.toJsonNode("{\"zone\":[[50.472000,30.504000],[50.472000,30.506000],[50.474000,30.506000],[50.474000,30.504000]]}"));

        Asset restricted = testRestClient.postAsset(createAsset("Restricted Zone", null));
        testRestClient.postTelemetryAttribute(restricted.getId(), SERVER_SCOPE,
                JacksonUtil.toJsonNode("{\"zone\":[[50.475000,30.510000],[50.475000,30.512000],[50.477000,30.512000],[50.477000,30.510000]]}"));

        // Relations FROM device
        testRestClient.postEntityRelation(new EntityRelation(device.getId(), allowed.getId(), "AllowedZone"));
        testRestClient.postEntityRelation(new EntityRelation(device.getId(), restricted.getId(), "RestrictedZone"));

        // Build CF: GEOFENCING -> attributes output
        CalculatedField cf = new CalculatedField();
        cf.setEntityId(device.getId());
        cf.setType(CalculatedFieldType.GEOFENCING);
        cf.setName("Geofencing CF");
        cf.setDebugSettings(DebugSettings.off());

        GeofencingCalculatedFieldConfiguration cfg = new GeofencingCalculatedFieldConfiguration();

        EntityCoordinates entityCoordinates = new EntityCoordinates("latitude", "longitude");
        cfg.setEntityCoordinates(entityCoordinates);

        // Dynamic groups via relations
        ZoneGroupConfiguration allowedZoneGroupConfiguration = new ZoneGroupConfiguration("zone", REPORT_TRANSITION_EVENTS_AND_PRESENCE_STATUS, false);
        var allowedDynamicSourceConfiguration = new RelationPathQueryDynamicSourceConfiguration();
        allowedDynamicSourceConfiguration.setLevels(List.of(new RelationPathLevel(EntitySearchDirection.FROM, "AllowedZone")));
        allowedZoneGroupConfiguration.setRefDynamicSourceConfiguration(allowedDynamicSourceConfiguration);

        ZoneGroupConfiguration restrictedZoneGroupConfiguration = new ZoneGroupConfiguration("zone", REPORT_TRANSITION_EVENTS_AND_PRESENCE_STATUS, false);
        var restrictedDynamicSourceConfiguration = new RelationPathQueryDynamicSourceConfiguration();
        restrictedDynamicSourceConfiguration.setLevels(List.of(new RelationPathLevel(EntitySearchDirection.FROM, "RestrictedZone")));
        restrictedZoneGroupConfiguration.setRefDynamicSourceConfiguration(restrictedDynamicSourceConfiguration);

        cfg.setZoneGroups(Map.of("allowedZones", allowedZoneGroupConfiguration, "restrictedZones", restrictedZoneGroupConfiguration));

        AttributesOutput out = new AttributesOutput();
        out.setScope(SERVER_SCOPE);
        cfg.setOutput(out);
        cf.setConfiguration(cfg);

        CalculatedField saved = testRestClient.postCalculatedField(cf);

        // Initial ENTERED/INSIDE and OUTSIDE
        await().alias("initial geofencing evaluation").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ArrayNode attrs = testRestClient.getAttributes(device.getId(), SERVER_SCOPE,
                            "allowedZonesEvent,allowedZonesStatus,restrictedZonesEvent,restrictedZonesStatus");
                    assertThat(attrs).isNotNull().hasSize(2);
                    Map<String, String> m = kv(attrs);
                    assertThat(m).containsEntry("allowedZonesStatus", "INSIDE")
                            .containsEntry("restrictedZonesStatus", "OUTSIDE");
                });

        // Move device into Restricted zone -> expect LEFT/ENTERED and statuses flipped
        testRestClient.postTelemetry(deviceToken, JacksonUtil.toJsonNode("{\"latitude\":50.4760,\"longitude\":30.5110}"));

        await().alias("transition after movement").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ArrayNode attrs = testRestClient.getAttributes(device.getId(), SERVER_SCOPE,
                            "allowedZonesEvent,allowedZonesStatus,restrictedZonesEvent,restrictedZonesStatus");
                    assertThat(attrs).isNotNull().hasSize(4);
                    Map<String, String> m = kv(attrs);
                    assertThat(m).containsEntry("allowedZonesEvent", "LEFT")
                            .containsEntry("allowedZonesStatus", "OUTSIDE")
                            .containsEntry("restrictedZonesEvent", "ENTERED")
                            .containsEntry("restrictedZonesStatus", "INSIDE");
                });

        testRestClient.deleteCalculatedFieldIfExists(saved.getId());
        testRestClient.deleteDeviceIfExists(device.getId());
        testRestClient.deleteAsset(allowed.getId());
        testRestClient.deleteAsset(restricted.getId());
    }

    @Test
    public void testPropagationCalculatedField_withExpression() {
        // --- Arrange entities ---
        String deviceToken = "propagationDeviceTokenA";
        Device device = testRestClient.postDevice(deviceToken, createDevice("Propagation Device With Expression", deviceProfileId));
        Asset asset1 = testRestClient.postAsset(createAsset("Propagated Asset 1", null));
        Asset asset2 = testRestClient.postAsset(createAsset("Propagated Asset 2", null));

        // Create relations FROM assets TO device
        EntityRelation rel1 = new EntityRelation(asset1.getId(), device.getId(), EntityRelation.CONTAINS_TYPE);
        EntityRelation rel2 = new EntityRelation(asset2.getId(), device.getId(), EntityRelation.CONTAINS_TYPE);
        testRestClient.postEntityRelation(rel1);
        testRestClient.postEntityRelation(rel2);

        // Telemetry on device
        testRestClient.postTelemetry(deviceToken, JacksonUtil.toJsonNode("{\"temperature\":12.5}"));

        // --- Build CF: PROPAGATION with expression ---
        CalculatedField cf = new CalculatedField();
        cf.setEntityId(device.getId());
        cf.setType(CalculatedFieldType.PROPAGATION);
        cf.setName("Propagation CF (expr)");
        cf.setConfigurationVersion(1);

        PropagationCalculatedFieldConfiguration cfg = new PropagationCalculatedFieldConfiguration();
        cfg.setRelation(new RelationPathLevel(EntitySearchDirection.TO, EntityRelation.CONTAINS_TYPE));
        cfg.setApplyExpressionToResolvedArguments(true);

        Argument arg = new Argument();
        arg.setRefEntityKey(new ReferencedEntityKey("temperature", ArgumentType.TS_LATEST, null));
        cfg.setArguments(Map.of("t", arg));

        cfg.setExpression("{\"testResult\": t * 2}");

        AttributesOutput output = new AttributesOutput();
        output.setScope(AttributeScope.SERVER_SCOPE);
        cfg.setOutput(output);

        cf.setConfiguration(cfg);

        CalculatedField saved = testRestClient.postCalculatedField(cf);

        // --- Assert propagated calculation (expression applied) ---
        await().alias("propagation expr mode evaluation")
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ArrayNode attrs1 = testRestClient.getAttributes(asset1.getId(), SERVER_SCOPE, "testResult");
                    assertThat(attrs1).isNotNull().hasSize(1);
                    Map<String, Integer> m1 = intKv(attrs1);
                    assertThat(m1).containsEntry("testResult", 25);

                    ArrayNode attrs2 = testRestClient.getAttributes(asset2.getId(), SERVER_SCOPE, "testResult");
                    assertThat(attrs2).isNotNull().hasSize(1);
                    Map<String, Integer> m2 = intKv(attrs2);
                    assertThat(m2).containsEntry("testResult", 25);
                });

        testRestClient.deleteEntityRelation(asset1.getId(), EntityRelation.CONTAINS_TYPE, device.getId());
        testRestClient.deleteEntityAttributes(asset1.getId(), SERVER_SCOPE, "testResult");

        testRestClient.postTelemetry(deviceToken, JacksonUtil.toJsonNode("{\"temperature\":25}"));

        // --- Assert propagated calculation (expression applied with new temperature argument and one relation removed) ---
        await().alias("propagation expr mode evaluation after temperature update")
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ArrayNode attrs1 = testRestClient.getAttributes(asset1.getId(), SERVER_SCOPE, "testResult");
                    assertThat(attrs1).isNullOrEmpty();

                    ArrayNode attrs2 = testRestClient.getAttributes(asset2.getId(), SERVER_SCOPE, "testResult");
                    assertThat(attrs2).isNotNull().hasSize(1);
                    Map<String, Integer> m2 = intKv(attrs2);
                    assertThat(m2).containsEntry("testResult", 50);
                });

        testRestClient.deleteCalculatedFieldIfExists(saved.getId());
        testRestClient.deleteDeviceIfExists(device.getId());
        testRestClient.deleteAsset(asset1.getId());
        testRestClient.deleteAsset(asset2.getId());
    }

    @Test
    public void testPropagationCalculatedField_withoutExpression() {
        // --- Arrange entities ---
        String deviceToken = "propagationDeviceTokenB";
        Device device = testRestClient.postDevice(deviceToken, createDevice("Propagation Device Without Expression", deviceProfileId));
        Asset asset1 = testRestClient.postAsset(createAsset("Propagated Asset 3", null));
        Asset asset2 = testRestClient.postAsset(createAsset("Propagated Asset 4", null));

        // Create relations FROM assets TO device
        EntityRelation rel1 = new EntityRelation(asset1.getId(), device.getId(), EntityRelation.CONTAINS_TYPE);
        EntityRelation rel2 = new EntityRelation(asset2.getId(), device.getId(), EntityRelation.CONTAINS_TYPE);
        testRestClient.postEntityRelation(rel1);
        testRestClient.postEntityRelation(rel2);

        // Telemetry on device
        long ts = System.currentTimeMillis() - 300000L;
        testRestClient.postTelemetry(deviceToken, JacksonUtil.toJsonNode(String.format("{\"ts\": %s, \"values\": {\"temperature\":12.5}}", ts)));

        // --- Build CF: PROPAGATION without expression ---
        CalculatedField cf = new CalculatedField();
        cf.setEntityId(device.getId());
        cf.setType(CalculatedFieldType.PROPAGATION);
        cf.setName("Propagation CF (args-only)");
        cf.setConfigurationVersion(1);

        PropagationCalculatedFieldConfiguration cfg = new PropagationCalculatedFieldConfiguration();
        cfg.setRelation(new RelationPathLevel(EntitySearchDirection.TO, EntityRelation.CONTAINS_TYPE));
        cfg.setApplyExpressionToResolvedArguments(false); // arguments-only mode

        Argument arg = new Argument();
        arg.setRefEntityKey(new ReferencedEntityKey("temperature", ArgumentType.TS_LATEST, null));
        cfg.setArguments(Map.of("temperatureComputed", arg));

        cfg.setOutput(new TimeSeriesOutput());

        cf.setConfiguration(cfg);

        CalculatedField saved = testRestClient.postCalculatedField(cf);

        // --- Assert propagated calculation (arguments-only mode) ---
        await().alias("propagation args-only evaluation")
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    JsonNode temperature1 = testRestClient.getLatestTelemetry(asset1.getId());
                    assertThat(temperature1).isNotNull();
                    assertThat(temperature1.get("temperatureComputed")).isNotNull();
                    assertThat(temperature1.get("temperatureComputed").get(0).get("ts").asText()).isEqualTo(Long.toString(ts));
                    assertThat(temperature1.get("temperatureComputed").get(0).get("value").asText()).isEqualTo("12.5");

                    JsonNode temperature2 = testRestClient.getLatestTelemetry(asset2.getId());
                    assertThat(temperature2).isNotNull();
                    assertThat(temperature2.get("temperatureComputed")).isNotNull();
                    assertThat(temperature2.get("temperatureComputed").get(0).get("ts").asText()).isEqualTo(Long.toString(ts));
                    assertThat(temperature2.get("temperatureComputed").get(0).get("value").asText()).isEqualTo("12.5");
                });

        testRestClient.deleteEntityRelation(asset1.getId(), EntityRelation.CONTAINS_TYPE, device.getId());
        testRestClient.deleteEntityTimeseries(asset1.getId(), "temperatureComputed", true);

        // Update telemetry on device
        long newTs = System.currentTimeMillis() - 300000L;
        testRestClient.postTelemetry(deviceToken, JacksonUtil.toJsonNode(String.format("{\"ts\": %s, \"values\": {\"temperature\":25}}", newTs)));

        // --- Assert propagated calculation (arguments-only mode after update) ---
        await().alias("propagation args-only evaluation after temperature update")
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    JsonNode temperature1 = testRestClient.getLatestTelemetry(asset1.getId());
                    assertThat(temperature1).isNullOrEmpty();

                    JsonNode temperature2 = testRestClient.getLatestTelemetry(asset2.getId());
                    assertThat(temperature2).isNotNull();
                    assertThat(temperature2.get("temperatureComputed")).isNotNull();
                    assertThat(temperature2.get("temperatureComputed").get(0).get("ts").asText()).isEqualTo(Long.toString(newTs));
                    assertThat(temperature2.get("temperatureComputed").get(0).get("value").asInt()).isEqualTo(25);
                });

        testRestClient.deleteCalculatedFieldIfExists(saved.getId());
        testRestClient.deleteDeviceIfExists(device.getId());
        testRestClient.deleteAsset(asset1.getId());
        testRestClient.deleteAsset(asset2.getId());
    }

    @Test
    public void testRelatedEntitiesAggregationCalculatedField() {
        // --- Create entities ---
        String device_1_1_token = "000000011";
        Device device_1_1 = testRestClient.postDevice(device_1_1_token, createDevice("Device 1-1", deviceProfileId));
        String device_1_2_token = "000000012";
        Device device_1_2 = testRestClient.postDevice(device_1_2_token, createDevice("Device 1-2", deviceProfileId));

        // Create relations FROM asset TO devices
        EntityRelation rel_1_1 = new EntityRelation(asset.getId(), device_1_1.getId(), EntityRelation.CONTAINS_TYPE);
        EntityRelation rel_1_2 = new EntityRelation(asset.getId(), device_1_2.getId(), EntityRelation.CONTAINS_TYPE);
        testRestClient.postEntityRelation(rel_1_1);
        testRestClient.postEntityRelation(rel_1_2);

        // Post telemetry
        testRestClient.postTelemetry(device_1_1_token, JacksonUtil.toJsonNode("{\"occupied\":true}"));
        testRestClient.postTelemetry(device_1_2_token, JacksonUtil.toJsonNode("{\"occupied\":false}"));

        // --- Create CF: Related entities aggregation ---
        CalculatedField calculatedField = createOccupancyCF(assetProfileId);

        // --- Assert aggregation ---
        await().alias("create cf -> check aggregation")
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    JsonNode occupancy = testRestClient.getLatestTelemetry(asset.getId());
                    assertThat(occupancy).isNotNull();

                    assertThat(occupancy.get("freeSpaces")).isNotNull();
                    assertThat(occupancy.get("freeSpaces").get(0).get("value").asText()).isEqualTo("1");

                    assertThat(occupancy.get("occupiedSpaces")).isNotNull();
                    assertThat(occupancy.get("occupiedSpaces").get(0).get("value").asText()).isEqualTo("1");

                    assertThat(occupancy.get("totalSpaces")).isNotNull();
                    assertThat(occupancy.get("totalSpaces").get(0).get("value").asText()).isEqualTo("2");
                });

        // Post telemetry
        testRestClient.postTelemetry(device_1_2_token, JacksonUtil.toJsonNode("{\"occupied\":true}"));

        // --- Assert aggregation ---
        await().alias("update telemetry -> check aggregation")
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    JsonNode occupancy = testRestClient.getLatestTelemetry(asset.getId());
                    assertThat(occupancy).isNotNull();

                    assertThat(occupancy.get("freeSpaces")).isNotNull();
                    assertThat(occupancy.get("freeSpaces").get(0).get("value").asText()).isEqualTo("0");

                    assertThat(occupancy.get("occupiedSpaces")).isNotNull();
                    assertThat(occupancy.get("occupiedSpaces").get(0).get("value").asText()).isEqualTo("2");

                    assertThat(occupancy.get("totalSpaces")).isNotNull();
                    assertThat(occupancy.get("totalSpaces").get(0).get("value").asText()).isEqualTo("2");
                });

        // Add entity to profile
        Asset asset2 = testRestClient.postAsset(createAsset("Asset 2", assetProfileId));
        String device_2_1_token = "000000021";
        Device device_2_1 = testRestClient.postDevice(device_2_1_token, createDevice("Device 2-1", deviceProfileId));
        String device_2_2_token = "000000022";
        Device device_2_2 = testRestClient.postDevice(device_2_2_token, createDevice("Device 2-2", deviceProfileId));

        // Post telemetry
        testRestClient.postTelemetry(device_2_1_token, JacksonUtil.toJsonNode("{\"occupied\":true}"));
        testRestClient.postTelemetry(device_2_2_token, JacksonUtil.toJsonNode("{\"occupied\":false}"));

        // --- Assert aggregation ---
        await().alias("add entity to profile cf -> no aggregated values since no relations")
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    JsonNode occupancy = testRestClient.getLatestTelemetry(asset2.getId());
                    assertThat(occupancy).isNullOrEmpty();
                });

        // Create relations FROM asset TO devices
        EntityRelation rel_2_1 = new EntityRelation(asset2.getId(), device_2_1.getId(), EntityRelation.CONTAINS_TYPE);
        testRestClient.postEntityRelation(rel_2_1);
        EntityRelation rel_2_2 = new EntityRelation(asset2.getId(), device_2_2.getId(), EntityRelation.CONTAINS_TYPE);
        testRestClient.postEntityRelation(rel_2_2);

        // --- Assert aggregation ---
        await().alias("create relation -> check aggregation")
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    JsonNode occupancy = testRestClient.getLatestTelemetry(asset2.getId());
                    assertThat(occupancy).isNotNull();

                    assertThat(occupancy.get("freeSpaces")).isNotNull();
                    assertThat(occupancy.get("freeSpaces").get(0).get("value").asText()).isEqualTo("1");

                    assertThat(occupancy.get("occupiedSpaces")).isNotNull();
                    assertThat(occupancy.get("occupiedSpaces").get(0).get("value").asText()).isEqualTo("1");

                    assertThat(occupancy.get("totalSpaces")).isNotNull();
                    assertThat(occupancy.get("totalSpaces").get(0).get("value").asText()).isEqualTo("2");
                });

        testRestClient.deleteEntityRelation(asset2.getId(), EntityRelation.CONTAINS_TYPE, device_2_2.getId());

        // --- Assert aggregation ---
        await().alias("delete relation -> check aggregation")
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    JsonNode occupancy = testRestClient.getLatestTelemetry(asset2.getId());
                    assertThat(occupancy).isNotNull();

                    assertThat(occupancy.get("freeSpaces")).isNotNull();
                    assertThat(occupancy.get("freeSpaces").get(0).get("value").asText()).isEqualTo("0");

                    assertThat(occupancy.get("occupiedSpaces")).isNotNull();
                    assertThat(occupancy.get("occupiedSpaces").get(0).get("value").asText()).isEqualTo("1");

                    assertThat(occupancy.get("totalSpaces")).isNotNull();
                    assertThat(occupancy.get("totalSpaces").get(0).get("value").asText()).isEqualTo("1");
                });

        testRestClient.deleteCalculatedFieldIfExists(calculatedField.getId());
        testRestClient.deleteDeviceIfExists(device_1_1.getId());
        testRestClient.deleteDeviceIfExists(device_1_2.getId());
        testRestClient.deleteDeviceIfExists(device_2_1.getId());
        testRestClient.deleteDeviceIfExists(device_2_2.getId());
        testRestClient.deleteAsset(asset2.getId());
    }

    @Test
    public void testDebugEvents() {
        // --- Arrange entities ---
        String deviceToken = "12345678901";
        Device device = testRestClient.postDevice(deviceToken, createDevice("Propagation Device", deviceProfileId));
        Asset asset1 = testRestClient.postAsset(createAsset("Propagated Asset 1", null));

        // Create relations FROM asset 1 TO device
        EntityRelation rel1 = new EntityRelation(asset1.getId(), device.getId(), EntityRelation.CONTAINS_TYPE);
        testRestClient.postEntityRelation(rel1);

        // --- Build CF: PROPAGATION ---
        CalculatedField saved = createPropagationCF(device.getId());

        // Create relations FROM asset 2 TO device
        Asset asset2 = testRestClient.postAsset(createAsset("Propagated Asset 2", null));
        EntityRelation rel2 = new EntityRelation(asset2.getId(), device.getId(), EntityRelation.CONTAINS_TYPE);
        testRestClient.postEntityRelation(rel2);

        // Telemetry on device
        testRestClient.postTelemetry(deviceToken, JacksonUtil.toJsonNode("{\"temperature\":25.1}"));

        // Delete relation between asset 1 and device
        testRestClient.deleteEntityRelation(asset1.getId(), EntityRelation.CONTAINS_TYPE, device.getId());

        // --- Assert propagated calculation (arguments-only mode) ---
        await().alias("check debug events")
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    List<String> eventTypes = testRestClient.getEvents(saved.getId(), EventType.DEBUG_CALCULATED_FIELD, tenantId, new TimePageLink(4, 0, null, SortOrder.BY_CREATED_TIME_DESC)).getData().stream()
                            .map(e -> e.getBody().get("msgType").asText())
                            .toList();

                    assertThat(eventTypes).as("Check sequence of debug events")
                            .containsSequence(
                                    CalculatedFieldEventType.RELATION_DELETED.name(),
                                    TbMsgType.POST_TELEMETRY_REQUEST.name(),
                                    CalculatedFieldEventType.RELATION_ADD_OR_UPDATE.name(),
                                    CalculatedFieldEventType.INITIALIZED.name()
                            );
                });

        testRestClient.deleteCalculatedFieldIfExists(saved.getId());
        testRestClient.deleteDeviceIfExists(device.getId());
        testRestClient.deleteAsset(asset1.getId());
        testRestClient.deleteAsset(asset2.getId());
    }

    private CalculatedField createPropagationCF(EntityId entityId) {
        CalculatedField cf = new CalculatedField();
        cf.setEntityId(entityId);
        cf.setType(CalculatedFieldType.PROPAGATION);
        cf.setName("Propagation CF (args-only)");
        cf.setConfigurationVersion(1);

        PropagationCalculatedFieldConfiguration cfg = new PropagationCalculatedFieldConfiguration();
        cfg.setRelation(new RelationPathLevel(EntitySearchDirection.TO, EntityRelation.CONTAINS_TYPE));
        cfg.setApplyExpressionToResolvedArguments(false); // arguments-only mode

        Argument arg = new Argument();
        arg.setRefEntityKey(new ReferencedEntityKey("temperature", ArgumentType.TS_LATEST, null));
        cfg.setArguments(Map.of("deviceTemperature", arg));

        cfg.setOutput(new TimeSeriesOutput());

        cf.setConfiguration(cfg);

        cf.setDebugSettings(DebugSettings.all());

        return testRestClient.postCalculatedField(cf);
    }

    private CalculatedField createOccupancyCF(EntityId entityId) {
        CalculatedField calculatedField = new CalculatedField();
        calculatedField.setName("Occupancy");
        calculatedField.setEntityId(entityId);
        calculatedField.setType(CalculatedFieldType.RELATED_ENTITIES_AGGREGATION);

        RelatedEntitiesAggregationCalculatedFieldConfiguration configuration = new RelatedEntitiesAggregationCalculatedFieldConfiguration();

        configuration.setRelation(new RelationPathLevel(EntitySearchDirection.FROM, "Contains"));

        Map<String, Argument> arguments = new HashMap<>();
        Argument argument = new Argument();
        argument.setRefEntityKey(new ReferencedEntityKey("occupied", ArgumentType.TS_LATEST, null));
        argument.setDefaultValue("false");
        arguments.put("oc", argument);
        configuration.setArguments(arguments);

        configuration.setDeduplicationIntervalInSec(5);
        configuration.setScheduledUpdateInterval(10);

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
        configuration.setMetrics(aggMetrics);

        TimeSeriesOutput output = new TimeSeriesOutput();
        output.setDecimalsByDefault(0);
        configuration.setOutput(output);

        calculatedField.setConfiguration(configuration);
        calculatedField.setDebugSettings(DebugSettings.all());

        return testRestClient.postCalculatedField(calculatedField);
    }

    private CalculatedField createSimpleCalculatedField(EntityId entityId) {
        CalculatedField calculatedField = new CalculatedField();
        calculatedField.setEntityId(entityId);
        calculatedField.setType(CalculatedFieldType.SIMPLE);
        calculatedField.setName("C to F" + RandomStringUtils.insecure().nextAlphabetic(5));
        calculatedField.setDebugSettings(DebugSettings.all());

        SimpleCalculatedFieldConfiguration config = new SimpleCalculatedFieldConfiguration();

        Argument argument = new Argument();
        ReferencedEntityKey refEntityKey = new ReferencedEntityKey("temperature", ArgumentType.TS_LATEST, null);
        argument.setRefEntityKey(refEntityKey);
        argument.setDefaultValue("12");
        config.setArguments(Map.of("T", argument));

        config.setExpression("(T * 9/5) + 32");

        TimeSeriesOutput output = new TimeSeriesOutput();
        output.setName("fahrenheitTemp");
        output.setDecimalsByDefault(2);
        config.setOutput(output);

        calculatedField.setConfiguration(config);

        return testRestClient.postCalculatedField(calculatedField);
    }

    private CalculatedField createScriptCalculatedField(EntityId entityId, EntityId refEntityId) {
        CalculatedField calculatedField = new CalculatedField();
        calculatedField.setEntityId(entityId);
        calculatedField.setType(CalculatedFieldType.SCRIPT);
        calculatedField.setName("Air density" + RandomStringUtils.insecure().nextAlphabetic(5));
        calculatedField.setDebugSettings(DebugSettings.all());

        ScriptCalculatedFieldConfiguration config = new ScriptCalculatedFieldConfiguration();

        Argument argument1 = new Argument();
        argument1.setRefEntityId(refEntityId);
        ReferencedEntityKey refEntityKey1 = new ReferencedEntityKey("altitude", ArgumentType.ATTRIBUTE, SERVER_SCOPE);
        argument1.setRefEntityKey(refEntityKey1);
        Argument argument2 = new Argument();
        ReferencedEntityKey refEntityKey2 = new ReferencedEntityKey("temperatureInF", ArgumentType.TS_ROLLING, null);
        argument2.setTimeWindow(300000L);
        argument2.setLimit(5);
        argument2.setRefEntityKey(refEntityKey2);

        config.setArguments(Map.of("altitude", argument1, "temperature", argument2));

        config.setExpression(exampleScript);

        config.setOutput(new TimeSeriesOutput());

        calculatedField.setConfiguration(config);

        return testRestClient.postCalculatedField(calculatedField);
    }

    private void waitInitialTelemetry(EntityId entityId, String key) {
        await().alias("wait initial telemetry").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    JsonNode telemetry = testRestClient.getLatestTelemetry(entityId);
                    assertThat(telemetry).isNotNull();
                    assertThat(telemetry.get(key)).isNotNull();
                });
    }

    private Device createDevice(String name, DeviceProfileId deviceProfileId) {
        Device device = new Device();
        device.setName(name);
        device.setType("default");
        device.setDeviceProfileId(deviceProfileId);
        DeviceData deviceData = new DeviceData();
        deviceData.setTransportConfiguration(new DefaultDeviceTransportConfiguration());
        deviceData.setConfiguration(new DefaultDeviceConfiguration());
        device.setDeviceData(deviceData);
        return device;
    }

    private Asset createAsset(String name, AssetProfileId assetProfileId) {
        Asset asset = new Asset();
        asset.setName(name);
        asset.setAssetProfileId(assetProfileId);
        return asset;
    }

    private static Map<String, String> kv(ArrayNode attrs) {
        Map<String, String> m = new HashMap<>();
        for (JsonNode n : attrs) {
            m.put(n.get("key").asText(), n.get("value").asText());
        }
        return m;
    }

    private static Map<String, Integer> intKv(ArrayNode attrs) {
        Map<String, Integer> m = new HashMap<>();
        for (JsonNode n : attrs) {
            m.put(n.get("key").asText(), n.get("value").asInt());
        }
        return m;
    }

}
