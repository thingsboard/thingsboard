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
package org.thingsboard.server.msa.cf;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.commons.lang3.RandomStringUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.ArgumentType;
import org.thingsboard.server.common.data.cf.configuration.Output;
import org.thingsboard.server.common.data.cf.configuration.OutputType;
import org.thingsboard.server.common.data.cf.configuration.PropagationCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.ReferencedEntityKey;
import org.thingsboard.server.common.data.cf.configuration.RelationPathQueryDynamicSourceConfiguration;
import org.thingsboard.server.common.data.cf.configuration.ScriptCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.SimpleCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.geofencing.EntityCoordinates;
import org.thingsboard.server.common.data.cf.configuration.geofencing.GeofencingCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.geofencing.ZoneGroupConfiguration;
import org.thingsboard.server.common.data.debug.DebugSettings;
import org.thingsboard.server.common.data.device.data.DefaultDeviceConfiguration;
import org.thingsboard.server.common.data.device.data.DefaultDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.data.DeviceData;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationPathLevel;
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

    private final String deviceToken = "zmzURIVRsq3lvnTP2XBE";

    private final String exampleScript = "var avgTemperature = temperature.mean(); // Get average temperature\n" +
                                         "  var temperatureK = (avgTemperature - 32) * (5 / 9) + 273.15; // Convert Fahrenheit to Kelvin\n" +
                                         "\n" +
                                         "  // Estimate air pressure based on altitude\n" +
                                         "  var pressure = 101325 * Math.pow((1 - 2.25577e-5 * altitude), 5.25588);\n" +
                                         "\n" +
                                         "  // Air density formula\n" +
                                         "  var airDensity = pressure / (287.05 * temperatureK);\n" +
                                         "\n" +
                                         "  return {\n" +
                                         "    \"airDensity\": toFixed(airDensity, 2)\n" +
                                         "  };";

    private TenantId tenantId;
    private UserId tenantAdminId;
    private DeviceProfileId deviceProfileId;
    private AssetProfileId assetProfileId;
    private Device device;
    private Asset asset;

    @BeforeClass
    public void beforeClass() {
        testRestClient.login("sysadmin@thingsboard.org", "sysadmin");

        tenantId = testRestClient.postTenant(EntityPrototypes.defaultTenantPrototype("Tenant")).getId();
        tenantAdminId = testRestClient.createUserAndLogin(defaultTenantAdmin(tenantId, "tenantAdmin@thingsboard.org"), "tenant");

        deviceProfileId = testRestClient.postDeviceProfile(defaultDeviceProfile("Device Profile 1")).getId();
        device = testRestClient.postDevice(deviceToken, createDevice("Device 1", deviceProfileId));

        assetProfileId = testRestClient.postAssetProfile(defaultAssetProfile("Asset Profile 1")).getId();
        asset = testRestClient.postAsset(createAsset("Asset 1", assetProfileId));

        testRestClient.postTelemetry(deviceToken, JacksonUtil.toJsonNode("{\"temperature\":25}"));
        testRestClient.postTelemetryAttribute(device.getId(), SERVER_SCOPE, JacksonUtil.toJsonNode("{\"deviceTemperature\":40}"));

        testRestClient.postTelemetry(deviceToken, JacksonUtil.toJsonNode("{\"temperatureInF\":72.32}"));
        testRestClient.postTelemetry(deviceToken, JacksonUtil.toJsonNode("{\"temperatureInF\":72.86}"));
        testRestClient.postTelemetry(deviceToken, JacksonUtil.toJsonNode("{\"temperatureInF\":73.58}"));

        testRestClient.postTelemetryAttribute(asset.getId(), SERVER_SCOPE, JacksonUtil.toJsonNode("{\"altitude\":1035}"));
    }

    @BeforeMethod
    public void beforeMethod() {
        testRestClient.login("sysadmin@thingsboard.org", "sysadmin");
    }

    @AfterClass
    public void afterClass() {
        testRestClient.resetToken();
        testRestClient.login("sysadmin@thingsboard.org", "sysadmin");
        testRestClient.deleteTenant(tenantId);
    }

    @Test
    public void testPerformInitialCalculationForSimpleType() {
        // login tenant admin
        testRestClient.getAndSetUserToken(tenantAdminId);

        CalculatedField savedCalculatedField = createSimpleCalculatedField();

        testRestClient.postTelemetry(deviceToken, JacksonUtil.toJsonNode("{\"temperature\":25}"));

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
        // login tenant admin
        testRestClient.getAndSetUserToken(tenantAdminId);

        CalculatedField savedCalculatedField = createSimpleCalculatedField();
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
        // login tenant admin
        testRestClient.getAndSetUserToken(tenantAdminId);

        CalculatedField savedCalculatedField = createSimpleCalculatedField();

        Output savedOutput = savedCalculatedField.getConfiguration().getOutput();
        savedOutput.setType(OutputType.ATTRIBUTES);
        savedOutput.setScope(SERVER_SCOPE);
        savedOutput.setName("temperatureF");
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
        // login tenant admin
        testRestClient.getAndSetUserToken(tenantAdminId);

        CalculatedField savedCalculatedField = createSimpleCalculatedField();
        assertThat(savedCalculatedField.getConfiguration() instanceof SimpleCalculatedFieldConfiguration).isTrue();

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
        // login tenant admin
        testRestClient.getAndSetUserToken(tenantAdminId);

        CalculatedField savedCalculatedField = createSimpleCalculatedField();

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
        // login tenant admin
        testRestClient.getAndSetUserToken(tenantAdminId);

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
        // login tenant admin
        testRestClient.getAndSetUserToken(tenantAdminId);

        CalculatedField savedCalculatedField = createSimpleCalculatedField(deviceProfileId);

        String newDeviceToken = "mmmXRIVRsq9lbnTP2XBE";
        Device newDevice = testRestClient.postDevice(newDeviceToken, createDevice("Device 2", deviceProfileId));

        await().alias("create device by profile -> perform initial calculation for new device by profile").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    // used default value since telemetry is not present
                    JsonNode fahrenheitTemp = testRestClient.getLatestTelemetry(newDevice.getId());
                    assertThat(fahrenheitTemp).isNotNull();
                    assertThat(fahrenheitTemp.get("fahrenheitTemp")).isNotNull();
                    assertThat(fahrenheitTemp.get("fahrenheitTemp").get(0).get("value").asText()).isEqualTo("53.6");
                });

        DeviceProfile newDeviceProfile = testRestClient.postDeviceProfile(defaultDeviceProfile("Test Profile"));
        newDevice.setDeviceProfileId(newDeviceProfile.getId());
        testRestClient.postDevice(newDeviceToken, newDevice);

        testRestClient.postTelemetry(newDeviceToken, JacksonUtil.toJsonNode("{\"temperature\":25}"));

        await().alias("update telemetry -> no updates").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    JsonNode fahrenheitTemp = testRestClient.getLatestTelemetry(newDevice.getId());
                    assertThat(fahrenheitTemp).isNotNull();
                    assertThat(fahrenheitTemp.get("fahrenheitTemp")).isNotNull();
                    assertThat(fahrenheitTemp.get("fahrenheitTemp").get(0).get("value").asText()).isEqualTo("53.6");
                });

        testRestClient.deleteCalculatedFieldIfExists(savedCalculatedField.getId());
    }

    @Test
    public void testEntityIdIsProfileAndRefEntityIsCommon() {
        // login tenant admin
        testRestClient.getAndSetUserToken(tenantAdminId);

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
        // login tenant admin
        testRestClient.getAndSetUserToken(tenantAdminId);

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

        Output out = new Output();
        out.setType(OutputType.ATTRIBUTES);
        out.setScope(SERVER_SCOPE);
        cfg.setOutput(out);
        cf.setConfiguration(cfg);

        CalculatedField saved = testRestClient.postCalculatedField(cf);

        // Initial ENTERED/INSIDE and OUTSIDE
        await().alias("initial geofencing evaluation").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ArrayNode attrs = testRestClient.getAttributes(device.getId(), SERVER_SCOPE,
                            "allowedZonesEvent,allowedZonesStatus,restrictedZonesStatus");
                    assertThat(attrs).isNotNull().hasSize(3);
                    Map<String, String> m = kv(attrs);
                    assertThat(m).containsEntry("allowedZonesEvent", "ENTERED")
                            .containsEntry("allowedZonesStatus", "INSIDE")
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
    }

    @Test
    public void testPropagationCalculatedField_withExpression() {
        // login tenant admin
        testRestClient.getAndSetUserToken(tenantAdminId);

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

        Output output = new Output();
        output.setType(OutputType.ATTRIBUTES);
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
    }

    @Test
    public void testPropagationCalculatedField_withoutExpression() {
        // login tenant admin
        testRestClient.getAndSetUserToken(tenantAdminId);

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

        Output output = new Output();
        output.setType(OutputType.TIME_SERIES);
        cfg.setOutput(output);

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
    }

    private CalculatedField createSimpleCalculatedField() {
        return createSimpleCalculatedField(device.getId());
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
        argument.setDefaultValue("12"); // not used because real telemetry value in db is present
        config.setArguments(Map.of("T", argument));

        config.setExpression("(T * 9/5) + 32");

        Output output = new Output();
        output.setName("fahrenheitTemp");
        output.setType(OutputType.TIME_SERIES);
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

        Output output = new Output();
        output.setType(OutputType.TIME_SERIES);
        config.setOutput(output);

        calculatedField.setConfiguration(config);

        return testRestClient.postCalculatedField(calculatedField);
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
