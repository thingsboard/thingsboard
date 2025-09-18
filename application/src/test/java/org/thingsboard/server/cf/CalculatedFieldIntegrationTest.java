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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.ArgumentType;
import org.thingsboard.server.common.data.cf.configuration.CalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.Output;
import org.thingsboard.server.common.data.cf.configuration.OutputType;
import org.thingsboard.server.common.data.cf.configuration.ReferencedEntityKey;
import org.thingsboard.server.common.data.cf.configuration.RelationQueryDynamicSourceConfiguration;
import org.thingsboard.server.common.data.cf.configuration.ScriptCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.SimpleCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.geofencing.EntityCoordinates;
import org.thingsboard.server.common.data.cf.configuration.geofencing.GeofencingCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.geofencing.ZoneGroupConfiguration;
import org.thingsboard.server.common.data.debug.DebugSettings;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.controller.CalculatedFieldControllerTest;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.common.data.cf.configuration.geofencing.EntityCoordinates.ENTITY_ID_LATITUDE_ARGUMENT_KEY;
import static org.thingsboard.server.common.data.cf.configuration.geofencing.EntityCoordinates.ENTITY_ID_LONGITUDE_ARGUMENT_KEY;
import static org.thingsboard.server.common.data.cf.configuration.geofencing.GeofencingReportStrategy.REPORT_TRANSITION_EVENTS_AND_PRESENCE_STATUS;

@DaoSqlTest
public class CalculatedFieldIntegrationTest extends CalculatedFieldControllerTest {

    public static final int TIMEOUT = 60;
    public static final int POLL_INTERVAL = 1;

    @BeforeEach
    void setUp() throws Exception {
        loginTenantAdmin();
    }

    @Test
    public void testSimpleCalculatedFieldWhenAllTelemetryPresent() throws Exception {
        Device testDevice = createDevice("Test device", "1234567890");
        doPost("/api/plugins/telemetry/DEVICE/" + testDevice.getUuidId() + "/timeseries/" + DataConstants.SERVER_SCOPE, JacksonUtil.toJsonNode("{\"temperature\":25}"));
        doPost("/api/plugins/telemetry/DEVICE/" + testDevice.getUuidId() + "/attributes/" + DataConstants.SERVER_SCOPE, JacksonUtil.toJsonNode("{\"deviceTemperature\":40}"));

        CalculatedField calculatedField = new CalculatedField();
        calculatedField.setEntityId(testDevice.getId());
        calculatedField.setType(CalculatedFieldType.SIMPLE);
        calculatedField.setName("C to F");
        calculatedField.setDebugSettings(DebugSettings.all());
        calculatedField.setConfigurationVersion(1);

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
        config.setOutput(output);

        calculatedField.setConfiguration(config);
        calculatedField.setVersion(1L);

        CalculatedField savedCalculatedField = doPost("/api/calculatedField", calculatedField, CalculatedField.class);

        await().alias("create CF -> perform initial calculation").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode fahrenheitTemp = getLatestTelemetry(testDevice.getId(), "fahrenheitTemp");
                    assertThat(fahrenheitTemp).isNotNull();
                    assertThat(fahrenheitTemp.get("fahrenheitTemp").get(0).get("value").asText()).isEqualTo("77.0");
                });

        doPost("/api/plugins/telemetry/DEVICE/" + testDevice.getUuidId() + "/timeseries/" + DataConstants.SERVER_SCOPE, JacksonUtil.toJsonNode("{\"temperature\":30}"));

        await().alias("update telemetry -> recalculate state").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode fahrenheitTemp = getLatestTelemetry(testDevice.getId(), "fahrenheitTemp");
                    assertThat(fahrenheitTemp).isNotNull();
                    assertThat(fahrenheitTemp.get("fahrenheitTemp").get(0).get("value").asText()).isEqualTo("86.0");
                });

        Output savedOutput = savedCalculatedField.getConfiguration().getOutput();
        savedOutput.setType(OutputType.ATTRIBUTES);
        savedOutput.setScope(AttributeScope.SERVER_SCOPE);
        savedOutput.setName("temperatureF");
        savedCalculatedField = doPost("/api/calculatedField", savedCalculatedField, CalculatedField.class);

        await().alias("update CF output -> perform calculation with updated output").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ArrayNode temperatureF = getServerAttributes(testDevice.getId(), "temperatureF");
                    assertThat(temperatureF).isNotNull();
                    assertThat(temperatureF.get(0).get("value").asText()).isEqualTo("86.0");
                });

        Argument savedArgument = ((SimpleCalculatedFieldConfiguration) savedCalculatedField.getConfiguration()).getArguments().get("T");
        savedArgument.setRefEntityKey(new ReferencedEntityKey("deviceTemperature", ArgumentType.ATTRIBUTE, AttributeScope.SERVER_SCOPE));
        savedCalculatedField = doPost("/api/calculatedField", savedCalculatedField, CalculatedField.class);

        await().alias("update CF argument -> perform calculation with new argument").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ArrayNode temperatureF = getServerAttributes(testDevice.getId(), "temperatureF");
                    assertThat(temperatureF).isNotNull();
                    assertThat(temperatureF.get(0).get("value").asText()).isEqualTo("104.0");
                });

        ((SimpleCalculatedFieldConfiguration) savedCalculatedField.getConfiguration()).setExpression("1.8 * T + 32");
        savedCalculatedField = doPost("/api/calculatedField", savedCalculatedField, CalculatedField.class);

        await().alias("update CF expression -> perform calculation with new expression").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ArrayNode temperatureF = getServerAttributes(testDevice.getId(), "temperatureF");
                    assertThat(temperatureF).isNotNull();
                    assertThat(temperatureF.get(0).get("value").asText()).isEqualTo("104.0");
                });
    }

    @Test
    public void testSimpleCalculatedFieldWhenNotAllTelemetryPresent() throws Exception {
        Device testDevice = createDevice("Test device", "1234567890");

        CalculatedField calculatedField = new CalculatedField();
        calculatedField.setEntityId(testDevice.getId());
        calculatedField.setType(CalculatedFieldType.SIMPLE);
        calculatedField.setName("C to F");
        calculatedField.setDebugSettings(DebugSettings.all());
        calculatedField.setConfigurationVersion(1);

        SimpleCalculatedFieldConfiguration config = new SimpleCalculatedFieldConfiguration();

        Argument argument = new Argument();
        ReferencedEntityKey refEntityKey = new ReferencedEntityKey("temperature", ArgumentType.TS_LATEST, null);
        argument.setRefEntityKey(refEntityKey);
        config.setArguments(Map.of("T", argument));
        config.setExpression("(T * 9/5) + 32");

        Output output = new Output();
        output.setName("fahrenheitTemp");
        output.setType(OutputType.TIME_SERIES);
        config.setOutput(output);

        calculatedField.setConfiguration(config);
        calculatedField.setVersion(1L);

        CalculatedField savedCalculatedField = doPost("/api/calculatedField", calculatedField, CalculatedField.class);

        await().alias("create CF -> state is not ready -> no calculation performed").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode fahrenheitTemp = getLatestTelemetry(testDevice.getId(), "fahrenheitTemp");
                    assertThat(fahrenheitTemp).isNotNull();
                    assertThat(fahrenheitTemp.get("fahrenheitTemp").get(0).get("value").isNull()).isTrue();
                });

        doPost("/api/plugins/telemetry/DEVICE/" + testDevice.getUuidId() + "/timeseries/" + DataConstants.SERVER_SCOPE, JacksonUtil.toJsonNode("{\"temperature\":30}"));

        await().alias("update telemetry -> perform calculation").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode fahrenheitTemp = getLatestTelemetry(testDevice.getId(), "fahrenheitTemp");
                    assertThat(fahrenheitTemp).isNotNull();
                    assertThat(fahrenheitTemp.get("fahrenheitTemp").get(0).get("value").asText()).isEqualTo("86.0");
                });
    }

    @Test
    public void testSimpleCalculatedFieldWhenNotAllTelemetryPresentButDefaultValueIsSet() throws Exception {
        Device testDevice = createDevice("Test device", "1234567890");

        CalculatedField calculatedField = new CalculatedField();
        calculatedField.setEntityId(testDevice.getId());
        calculatedField.setType(CalculatedFieldType.SIMPLE);
        calculatedField.setName("C to F");
        calculatedField.setDebugSettings(DebugSettings.all());
        calculatedField.setConfigurationVersion(1);

        SimpleCalculatedFieldConfiguration config = new SimpleCalculatedFieldConfiguration();

        Argument argument = new Argument();
        ReferencedEntityKey refEntityKey = new ReferencedEntityKey("temperature", ArgumentType.TS_LATEST, null);
        argument.setRefEntityKey(refEntityKey);
        argument.setDefaultValue("12");
        config.setArguments(Map.of("T", argument));
        config.setExpression("(T * 9/5) + 32");

        Output output = new Output();
        output.setName("fahrenheitTemp");
        output.setType(OutputType.TIME_SERIES);
        config.setOutput(output);

        calculatedField.setConfiguration(config);
        calculatedField.setVersion(1L);

        CalculatedField savedCalculatedField = doPost("/api/calculatedField", calculatedField, CalculatedField.class);

        await().alias("create CF -> perform initial calculation with default value").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode fahrenheitTemp = getLatestTelemetry(testDevice.getId(), "fahrenheitTemp");
                    assertThat(fahrenheitTemp).isNotNull();
                    assertThat(fahrenheitTemp.get("fahrenheitTemp").get(0).get("value").asText()).isEqualTo("53.6");
                });

        doPost("/api/plugins/telemetry/DEVICE/" + testDevice.getUuidId() + "/timeseries/" + DataConstants.SERVER_SCOPE, JacksonUtil.toJsonNode("{\"temperature\":30}"));

        await().alias("update telemetry -> recalculate state").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode fahrenheitTemp = getLatestTelemetry(testDevice.getId(), "fahrenheitTemp");
                    assertThat(fahrenheitTemp).isNotNull();
                    assertThat(fahrenheitTemp.get("fahrenheitTemp").get(0).get("value").asText()).isEqualTo("86.0");
                });
    }

    @Test
    public void testSimpleCalculatedFieldWhenEntityIdIsProfile() throws Exception {
        Device testDevice = createDevice("Test device", "1234567890");
        doPost("/api/plugins/telemetry/DEVICE/" + testDevice.getUuidId() + "/attributes/" + DataConstants.SERVER_SCOPE, JacksonUtil.toJsonNode("{\"x\":40}"));

        AssetProfile assetProfile = doPost("/api/assetProfile", createAssetProfile("Test Asset Profile"), AssetProfile.class);

        Asset asset1 = createAsset("Test asset 1", assetProfile.getId());
        doPost("/api/plugins/telemetry/ASSET/" + asset1.getUuidId() + "/attributes/" + DataConstants.SERVER_SCOPE, JacksonUtil.toJsonNode("{\"y\":11}"));

        Asset asset2 = createAsset("Test asset 2", assetProfile.getId());
        doPost("/api/plugins/telemetry/ASSET/" + asset2.getUuidId() + "/attributes/" + DataConstants.SERVER_SCOPE, JacksonUtil.toJsonNode("{\"y\":12}"));

        CalculatedField calculatedField = new CalculatedField();
        calculatedField.setEntityId(assetProfile.getId());
        calculatedField.setType(CalculatedFieldType.SIMPLE);
        calculatedField.setName("z = x + y");
        calculatedField.setDebugSettings(DebugSettings.all());
        calculatedField.setConfigurationVersion(1);

        SimpleCalculatedFieldConfiguration config = new SimpleCalculatedFieldConfiguration();

        Argument argument1 = new Argument();
        ReferencedEntityKey refEntityKey1 = new ReferencedEntityKey("y", ArgumentType.ATTRIBUTE, AttributeScope.SERVER_SCOPE);
        argument1.setRefEntityKey(refEntityKey1);

        Argument argument2 = new Argument();
        argument2.setRefEntityId(testDevice.getId());
        ReferencedEntityKey refEntityKey2 = new ReferencedEntityKey("x", ArgumentType.ATTRIBUTE, AttributeScope.SERVER_SCOPE);
        argument2.setRefEntityKey(refEntityKey2);

        config.setArguments(Map.of("x", argument2, "y", argument1));

        config.setExpression("x + y");

        Output output = new Output();
        output.setName("z");
        output.setType(OutputType.ATTRIBUTES);
        output.setScope(AttributeScope.SERVER_SCOPE);

        config.setOutput(output);

        calculatedField.setConfiguration(config);
        calculatedField.setVersion(1L);

        doPost("/api/calculatedField", calculatedField, CalculatedField.class);

        await().alias("create CF and perform initial calculation").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    // result of asset 1
                    ArrayNode z1 = getServerAttributes(asset1.getId(), "z");
                    assertThat(z1).isNotNull();
                    assertThat(z1.get(0).get("value").asText()).isEqualTo("51.0");

                    // result of asset 2
                    ArrayNode z2 = getServerAttributes(asset2.getId(), "z");
                    assertThat(z2).isNotNull();
                    assertThat(z2.get(0).get("value").asText()).isEqualTo("52.0");
                });

        doPost("/api/plugins/telemetry/DEVICE/" + testDevice.getUuidId() + "/attributes/" + DataConstants.SERVER_SCOPE, JacksonUtil.toJsonNode("{\"x\":25}"));

        await().alias("update device telemetry -> recalculate state for all assets").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    // result of asset 1
                    ArrayNode z1 = getServerAttributes(asset1.getId(), "z");
                    assertThat(z1).isNotNull();
                    assertThat(z1.get(0).get("value").asText()).isEqualTo("36.0");

                    // result of asset 2
                    ArrayNode z2 = getServerAttributes(asset2.getId(), "z");
                    assertThat(z2).isNotNull();
                    assertThat(z2.get(0).get("value").asText()).isEqualTo("37.0");
                });

        doPost("/api/plugins/telemetry/ASSET/" + asset1.getUuidId() + "/attributes/" + DataConstants.SERVER_SCOPE, JacksonUtil.toJsonNode("{\"y\":15}"));

        await().alias("update asset 1 telemetry -> recalculate state only for asset 1").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    // result of asset 1
                    ArrayNode z1 = getServerAttributes(asset1.getId(), "z");
                    assertThat(z1).isNotNull();
                    assertThat(z1.get(0).get("value").asText()).isEqualTo("40.0");

                    // result of asset 2 (no changes)
                    ArrayNode z2 = getServerAttributes(asset2.getId(), "z");
                    assertThat(z2).isNotNull();
                    assertThat(z2.get(0).get("value").asText()).isEqualTo("37.0");
                });

        doPost("/api/plugins/telemetry/ASSET/" + asset2.getUuidId() + "/attributes/" + DataConstants.SERVER_SCOPE, JacksonUtil.toJsonNode("{\"y\":5}"));

        await().alias("update asset 2 telemetry -> recalculate state only for asset 2").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    // result of asset 1 (no changes)
                    ArrayNode z1 = getServerAttributes(asset1.getId(), "z");
                    assertThat(z1).isNotNull();
                    assertThat(z1.get(0).get("value").asText()).isEqualTo("40.0");

                    // result of asset 2
                    ArrayNode z2 = getServerAttributes(asset2.getId(), "z");
                    assertThat(z2).isNotNull();
                    assertThat(z2.get(0).get("value").asText()).isEqualTo("30.0");
                });

        Asset asset3 = createAsset("Test asset 3", assetProfile.getId());
        doPost("/api/plugins/telemetry/ASSET/" + asset3.getUuidId() + "/attributes/" + DataConstants.SERVER_SCOPE, JacksonUtil.toJsonNode("{\"y\":13}"));

        Asset finalAsset3 = asset3;
        await().alias("add new entity to profile -> calculate state for new entity").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    // result of asset 3
                    ArrayNode z3 = getServerAttributes(finalAsset3.getId(), "z");
                    assertThat(z3).isNotNull();
                    assertThat(z3.get(0).get("value").asText()).isEqualTo("38.0");
                });

        doPost("/api/plugins/telemetry/DEVICE/" + testDevice.getUuidId() + "/attributes/" + DataConstants.SERVER_SCOPE, JacksonUtil.toJsonNode("{\"x\":20}"));

        await().alias("update device telemetry -> recalculate state for all assets").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    // result of asset 1
                    ArrayNode z1 = getServerAttributes(asset1.getId(), "z");
                    assertThat(z1).isNotNull();
                    assertThat(z1.get(0).get("value").asText()).isEqualTo("35.0");

                    // result of asset 2
                    ArrayNode z2 = getServerAttributes(asset2.getId(), "z");
                    assertThat(z2).isNotNull();
                    assertThat(z2.get(0).get("value").asText()).isEqualTo("25.0");

                    // result of asset 3
                    ArrayNode z3 = getServerAttributes(finalAsset3.getId(), "z");
                    assertThat(z3).isNotNull();
                    assertThat(z3.get(0).get("value").asText()).isEqualTo("33.0");
                });

        // update profile for asset 3 -> delete state for asset 3
        AssetProfile newAssetProfile = doPost("/api/assetProfile", createAssetProfile("New Asset Profile"), AssetProfile.class);
        asset3.setAssetProfileId(newAssetProfile.getId());
        asset3 = doPost("/api/asset", asset3, Asset.class);

        doPost("/api/plugins/telemetry/DEVICE/" + testDevice.getUuidId() + "/attributes/" + DataConstants.SERVER_SCOPE, JacksonUtil.toJsonNode("{\"x\":15}"));

        Asset updatedAsset3 = asset3;
        await().alias("update device telemetry -> recalculate state for asset 1 and asset 2").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    // result of asset 1
                    ArrayNode z1 = getServerAttributes(asset1.getId(), "z");
                    assertThat(z1).isNotNull();
                    assertThat(z1.get(0).get("value").asText()).isEqualTo("30.0");

                    // result of asset 2
                    ArrayNode z2 = getServerAttributes(asset2.getId(), "z");
                    assertThat(z2).isNotNull();
                    assertThat(z2.get(0).get("value").asText()).isEqualTo("20.0");

                    // no changes for asset 3
                    ArrayNode z3 = getServerAttributes(updatedAsset3.getId(), "z");
                    assertThat(z3).isNotNull();
                    assertThat(z3.get(0).get("value").asText()).isEqualTo("33.0");
                });
    }

    @Test
    public void testSimpleCalculatedFieldWhenExpressionIsInvalid() throws Exception {
        Device testDevice = createDevice("Test device", "1234567890");
        doPost("/api/plugins/telemetry/DEVICE/" + testDevice.getUuidId() + "/timeseries/" + DataConstants.SERVER_SCOPE, JacksonUtil.toJsonNode("{\"temperature\":25}"));

        CalculatedField calculatedField = new CalculatedField();
        calculatedField.setEntityId(testDevice.getId());
        calculatedField.setType(CalculatedFieldType.SIMPLE);
        calculatedField.setName("C to F");
        calculatedField.setDebugSettings(DebugSettings.all());
        calculatedField.setConfigurationVersion(1);

        SimpleCalculatedFieldConfiguration config = new SimpleCalculatedFieldConfiguration();

        Argument argument = new Argument();
        ReferencedEntityKey refEntityKey = new ReferencedEntityKey("temperature", ArgumentType.TS_LATEST, null);
        argument.setRefEntityKey(refEntityKey);
        argument.setDefaultValue("12"); // not used because real telemetry value in db is present
        config.setArguments(Map.of("T", argument));
        config.setExpression("(T * 9/0) + 32");

        Output output = new Output();
        output.setName("fahrenheitTemp");
        output.setType(OutputType.TIME_SERIES);
        config.setOutput(output);

        calculatedField.setConfiguration(config);
        calculatedField.setVersion(1L);

        CalculatedField savedCalculatedField = doPost("/api/calculatedField", calculatedField, CalculatedField.class);

        await().alias("create CF -> ctx is not initialized -> no calculation perform").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode fahrenheitTemp = getLatestTelemetry(testDevice.getId(), "fahrenheitTemp");
                    assertThat(fahrenheitTemp).isNotNull();
                    assertThat(fahrenheitTemp.get("fahrenheitTemp").get(0).get("value").isNull()).isTrue();
                });

        doPost("/api/plugins/telemetry/DEVICE/" + testDevice.getUuidId() + "/timeseries/" + DataConstants.SERVER_SCOPE, JacksonUtil.toJsonNode("{\"temperature\":30}"));

        await().alias("update telemetry -> ctx is not initialized -> no calculation perform").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode fahrenheitTemp = getLatestTelemetry(testDevice.getId(), "fahrenheitTemp");
                    assertThat(fahrenheitTemp).isNotNull();
                    assertThat(fahrenheitTemp.get("fahrenheitTemp").get(0).get("value").isNull()).isTrue();
                });
    }

    @Test
    public void testSimpleCalculatedFieldWhenUseLatestTsIsTrue() throws Exception {
        Device testDevice = createDevice("Test device", "1234567890");
        long ts = System.currentTimeMillis() - 300000L;
        doPost("/api/plugins/telemetry/DEVICE/" + testDevice.getUuidId() + "/timeseries/" + DataConstants.SERVER_SCOPE, JacksonUtil.toJsonNode(String.format("{\"ts\": %s, \"values\": {\"temperature\":30}}", ts)));

        CalculatedField calculatedField = new CalculatedField();
        calculatedField.setEntityId(testDevice.getId());
        calculatedField.setType(CalculatedFieldType.SIMPLE);
        calculatedField.setName("C to F");
        calculatedField.setDebugSettings(DebugSettings.all());
        calculatedField.setConfigurationVersion(1);

        SimpleCalculatedFieldConfiguration config = new SimpleCalculatedFieldConfiguration();

        Argument argument = new Argument();
        ReferencedEntityKey refEntityKey = new ReferencedEntityKey("temperature", ArgumentType.TS_LATEST, null);
        argument.setRefEntityKey(refEntityKey);
        config.setArguments(Map.of("T", argument));
        config.setExpression("(T * 9/5) + 32");

        Output output = new Output();
        output.setName("fahrenheitTemp");
        output.setType(OutputType.TIME_SERIES);
        config.setOutput(output);

        config.setUseLatestTs(true);

        calculatedField.setConfiguration(config);

        CalculatedField savedCalculatedField = doPost("/api/calculatedField", calculatedField, CalculatedField.class);

        await().alias("create CF -> perform initial calculation").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode fahrenheitTemp = getLatestTelemetry(testDevice.getId(), "fahrenheitTemp");
                    assertThat(fahrenheitTemp).isNotNull();
                    assertThat(fahrenheitTemp.get("fahrenheitTemp").get(0).get("ts").asText()).isEqualTo(Long.toString(ts));
                    assertThat(fahrenheitTemp.get("fahrenheitTemp").get(0).get("value").asText()).isEqualTo("86.0");
                });
    }

    @Test
    public void testSimpleCalculatedFieldWhenUseLatestTsIsTrueAndTelemetryBeforeLatest() throws Exception {
        Device testDevice = createDevice("Test device", "1234567890");
        long ts = System.currentTimeMillis();

        long tsA = ts - 300000L;
        doPost("/api/plugins/telemetry/DEVICE/" + testDevice.getUuidId() + "/timeseries/" + DataConstants.SERVER_SCOPE, JacksonUtil.toJsonNode(String.format("{\"ts\": %s, \"values\": {\"a\":1}}", tsA)));

        long tsB = ts - 300L;
        doPost("/api/plugins/telemetry/DEVICE/" + testDevice.getUuidId() + "/timeseries/" + DataConstants.SERVER_SCOPE, JacksonUtil.toJsonNode(String.format("{\"ts\": %s, \"values\": {\"b\":5}}", tsB)));

        CalculatedField calculatedField = new CalculatedField();
        calculatedField.setEntityId(testDevice.getId());
        calculatedField.setType(CalculatedFieldType.SIMPLE);
        calculatedField.setName("a + b");
        calculatedField.setDebugSettings(DebugSettings.all());
        calculatedField.setConfigurationVersion(1);

        SimpleCalculatedFieldConfiguration config = new SimpleCalculatedFieldConfiguration();

        Argument argument1 = new Argument();
        ReferencedEntityKey refEntityKey1 = new ReferencedEntityKey("a", ArgumentType.TS_LATEST, null);
        argument1.setRefEntityKey(refEntityKey1);
        Argument argument2 = new Argument();
        ReferencedEntityKey refEntityKey2 = new ReferencedEntityKey("b", ArgumentType.TS_LATEST, null);
        argument2.setRefEntityKey(refEntityKey2);
        config.setArguments(Map.of("a", argument1, "b", argument2));
        config.setExpression("a + b");

        Output output = new Output();
        output.setName("c");
        output.setType(OutputType.TIME_SERIES);
        config.setOutput(output);

        config.setUseLatestTs(true);

        calculatedField.setConfiguration(config);

        CalculatedField savedCalculatedField = doPost("/api/calculatedField", calculatedField, CalculatedField.class);

        await().alias("create CF -> perform initial calculation").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode c = getLatestTelemetry(testDevice.getId(), "c");
                    assertThat(c).isNotNull();
                    assertThat(c.get("c").get(0).get("ts").asText()).isEqualTo(Long.toString(tsB));
                    assertThat(c.get("c").get(0).get("value").asText()).isEqualTo("6.0");
                });

        long tsABeforeTsB = tsB - 300L;
        doPost("/api/plugins/telemetry/DEVICE/" + testDevice.getUuidId() + "/timeseries/" + DataConstants.SERVER_SCOPE, JacksonUtil.toJsonNode(String.format("{\"ts\": %s, \"values\": {\"a\":10}}", tsABeforeTsB)));

        await().alias("update telemetry with ts less than latest -> save result with latest ts").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode c = getLatestTelemetry(testDevice.getId(), "c");
                    assertThat(c).isNotNull();
                    assertThat(c.get("c").get(0).get("ts").asText()).isEqualTo(Long.toString(tsB));// also tsB, since this is the latest timestamp
                    assertThat(c.get("c").get(0).get("value").asText()).isEqualTo("15.0");
                });
    }

    @Test
    public void testScriptCalculatedFieldWhenUsedLatestTsInScript() throws Exception {
        Device testDevice = createDevice("Test device", "1234567890");
        long ts = System.currentTimeMillis() - 300000L;
        doPost("/api/plugins/telemetry/DEVICE/" + testDevice.getUuidId() + "/timeseries/" + DataConstants.SERVER_SCOPE, JacksonUtil.toJsonNode(String.format("{\"ts\": %s, \"values\": {\"temperature\":30}}", ts)));

        CalculatedField calculatedField = new CalculatedField();
        calculatedField.setEntityId(testDevice.getId());
        calculatedField.setType(CalculatedFieldType.SCRIPT);
        calculatedField.setName("C to F");
        calculatedField.setDebugSettings(DebugSettings.all());
        calculatedField.setConfigurationVersion(1);

        ScriptCalculatedFieldConfiguration config = new ScriptCalculatedFieldConfiguration();

        Argument argument = new Argument();
        ReferencedEntityKey refEntityKey = new ReferencedEntityKey("temperature", ArgumentType.TS_LATEST, null);
        argument.setRefEntityKey(refEntityKey);
        config.setArguments(Map.of("T", argument));
        config.setExpression("return {\"ts\": ctx.latestTs, \"values\": {\"fahrenheitTemp\": (T * 1.8) + 32}};");

        Output output = new Output();
        output.setType(OutputType.TIME_SERIES);
        config.setOutput(output);

        calculatedField.setConfiguration(config);

        CalculatedField savedCalculatedField = doPost("/api/calculatedField", calculatedField, CalculatedField.class);

        await().alias("create CF -> perform initial calculation").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode fahrenheitTemp = getLatestTelemetry(testDevice.getId(), "fahrenheitTemp");
                    assertThat(fahrenheitTemp).isNotNull();
                    assertThat(fahrenheitTemp.get("fahrenheitTemp").get(0).get("ts").asText()).isEqualTo(Long.toString(ts));
                    assertThat(fahrenheitTemp.get("fahrenheitTemp").get(0).get("value").asText()).isEqualTo("86.0");
                });
    }

    @Test
    public void testGeofencingCalculatedField_withZonesCreatedOnDevice() throws Exception {
        // --- Arrange entities ---
        Device device = createDevice("GF Test Device", "sn-geo-2");

        // Allowed zone polygon (square)
        String allowedPolygon = "[[50.472000, 30.504000], [50.472000, 30.506000], [50.474000, 30.506000], [50.474000, 30.504000]]";
        // Restricted zone polygon (square)
        String restrictedPolygon = "[[50.475000, 30.510000], [50.475000, 30.512000], [50.477000, 30.512000], [50.477000, 30.510000]]";

        doPost("/api/plugins/telemetry/DEVICE/" + device.getUuidId() + "/attributes/" + DataConstants.SERVER_SCOPE,
                JacksonUtil.toJsonNode("{\"allowedZone\":" + allowedPolygon + "}")).andExpect(status().isOk());

        doPost("/api/plugins/telemetry/DEVICE/" + device.getUuidId() + "/attributes/" + DataConstants.SERVER_SCOPE,
                JacksonUtil.toJsonNode("{\"restrictedZone\":" + restrictedPolygon + "}")).andExpect(status().isOk());

        // Initial device coordinates (inside Allowed, outside Restricted)
        doPost("/api/plugins/telemetry/DEVICE/" + device.getUuidId() + "/timeseries/unusedScope",
                JacksonUtil.toJsonNode("{\"latitude\":50.4730,\"longitude\":30.5050}"));

        // --- Build CF: GEOFENCING ---
        CalculatedField cf = new CalculatedField();
        cf.setEntityId(device.getDeviceProfileId());
        cf.setType(CalculatedFieldType.GEOFENCING);
        cf.setName("Geofencing CF");
        cf.setDebugSettings(DebugSettings.off());

        GeofencingCalculatedFieldConfiguration cfg = new GeofencingCalculatedFieldConfiguration();

        // Coordinates: TS_LATEST on the device
        EntityCoordinates entityCoordinates = new EntityCoordinates("latitude", "longitude");
        cfg.setEntityCoordinates(entityCoordinates);

        // Zone groups: ATTRIBUTE on the device
        ZoneGroupConfiguration allowedZonesGroup = new ZoneGroupConfiguration("allowedZone", REPORT_TRANSITION_EVENTS_AND_PRESENCE_STATUS, false);
        ZoneGroupConfiguration restrictedZonesGroup = new ZoneGroupConfiguration("restrictedZone", REPORT_TRANSITION_EVENTS_AND_PRESENCE_STATUS, false);

        cfg.setZoneGroups(Map.of("allowedZones", allowedZonesGroup, "restrictedZones", restrictedZonesGroup));

        // Output to server attributes
        Output out = new Output();
        out.setType(OutputType.ATTRIBUTES);
        out.setScope(AttributeScope.SERVER_SCOPE);
        cfg.setOutput(out);

        cf.setConfiguration(cfg);

        doPost("/api/calculatedField", cf, CalculatedField.class);

        // --- Assert initial evaluation (ENTERED / OUTSIDE) ---
        await().alias("initial geofencing evaluation")
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ArrayNode attrs = getServerAttributes(device.getId(),
                            "allowedZonesEvent", "allowedZonesStatus", "restrictedZonesStatus", "restrictedZonesEvent");
                    // --- no restrictedZonesEvent as no transition happened yet
                    assertThat(attrs).isNotNull().isNotEmpty().hasSize(3);
                    Map<String, String> m = kv(attrs);
                    assertThat(m).containsEntry("allowedZonesEvent", "ENTERED")
                            .containsEntry("allowedZonesStatus", "INSIDE")
                            .containsEntry("restrictedZonesStatus", "OUTSIDE");
                });

        // --- delete attributes reported in previous evaluation
        doDelete("/api/plugins/telemetry/DEVICE/" + device.getUuidId() + "/SERVER_SCOPE?keys=allowedZonesEvent,allowedZonesStatus,restrictedZonesStatus", String.class);

        // --- Update restrictedZone by 'restrictedZone' attribute update
        doPost("/api/plugins/telemetry/DEVICE/" + device.getUuidId() + "/attributes/" + DataConstants.SERVER_SCOPE,
                JacksonUtil.toJsonNode("{\"restrictedZone\":" + restrictedPolygon + "}")).andExpect(status().isOk());

        // --- Assert no transition ---
        // --- Assert attributes updated with the same values for restrictedZones ---
        // --- Assert attributes updated with the new values for allowedZones ---
        await().alias("evaluation after version bump of geo argument")
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ArrayNode attrs = getServerAttributes(device.getId(),
                            "allowedZonesEvent", "allowedZonesStatus",
                            "restrictedZonesEvent", "restrictedZonesStatus");
                    assertThat(attrs).isNotNull().isNotEmpty().hasSize(2);
                    Map<String, String> m = kv(attrs);
                    assertThat(m).containsEntry("allowedZonesStatus", "INSIDE")
                            .containsEntry("restrictedZonesStatus", "OUTSIDE");
                });
    }

    @Test
    public void testGeofencingCalculatedField_withoutRelationsCreationAndDynamicRefresh() throws Exception {
        // --- Arrange entities ---
        Device device = createDevice("GF Device", "sn-geo-1");

        // Allowed zone polygon (square)
        String allowedPolygon = "[[50.472000, 30.504000], [50.472000, 30.506000], [50.474000, 30.506000], [50.474000, 30.504000]]";
        // Restricted zone polygon (square)
        String restrictedPolygon = "[[50.475000, 30.510000], [50.475000, 30.512000], [50.477000, 30.512000], [50.477000, 30.510000]]";

        Asset allowedZoneAsset = createAsset("Allowed Zone", null);
        doPost("/api/plugins/telemetry/ASSET/" + allowedZoneAsset.getUuidId() + "/attributes/" + DataConstants.SERVER_SCOPE,
                JacksonUtil.toJsonNode("{\"zone\":" + allowedPolygon + "}")).andExpect(status().isOk());

        Asset restrictedZoneAsset = createAsset("Restricted Zone", null);
        doPost("/api/plugins/telemetry/ASSET/" + restrictedZoneAsset.getUuidId() + "/attributes/" + DataConstants.SERVER_SCOPE,
                JacksonUtil.toJsonNode("{\"zone\":" + restrictedPolygon + "}")).andExpect(status().isOk());

        // Relations from device to zones
        EntityRelation deviceToAllowedZoneRelation = new EntityRelation();
        deviceToAllowedZoneRelation.setFrom(device.getId());
        deviceToAllowedZoneRelation.setTo(allowedZoneAsset.getId());
        deviceToAllowedZoneRelation.setType("AllowedZone");

        EntityRelation deviceToRestrictedZoneRelation = new EntityRelation();
        deviceToRestrictedZoneRelation.setFrom(device.getId());
        deviceToRestrictedZoneRelation.setTo(restrictedZoneAsset.getId());
        deviceToRestrictedZoneRelation.setType("RestrictedZone");

        doPost("/api/relation", deviceToAllowedZoneRelation).andExpect(status().isOk());
        doPost("/api/relation", deviceToRestrictedZoneRelation).andExpect(status().isOk());

        // Initial device coordinates (inside Allowed, outside Restricted)
        doPost("/api/plugins/telemetry/DEVICE/" + device.getUuidId() + "/timeseries/unusedScope",
                JacksonUtil.toJsonNode("{\"latitude\":50.4730,\"longitude\":30.5050}"));

        // --- Build CF: GEOFENCING ---
        CalculatedField cf = new CalculatedField();
        cf.setEntityId(device.getId());
        cf.setType(CalculatedFieldType.GEOFENCING);
        cf.setName("Geofencing CF");
        cf.setDebugSettings(DebugSettings.off());

        GeofencingCalculatedFieldConfiguration cfg = new GeofencingCalculatedFieldConfiguration();

        // Coordinates: TS_LATEST on the device
        EntityCoordinates entityCoordinates = new EntityCoordinates("latitude", "longitude");
        cfg.setEntityCoordinates(entityCoordinates);

        // Zone groups: ATTRIBUTE on specific assets (one zone per group)
        ZoneGroupConfiguration allowedZonesGroup = new ZoneGroupConfiguration("zone", REPORT_TRANSITION_EVENTS_AND_PRESENCE_STATUS, false);
        var allowedZoneDynamicSourceConfiguration = new RelationQueryDynamicSourceConfiguration();
        allowedZoneDynamicSourceConfiguration.setDirection(EntitySearchDirection.FROM);
        allowedZoneDynamicSourceConfiguration.setRelationType("AllowedZone");
        allowedZoneDynamicSourceConfiguration.setMaxLevel(1);
        allowedZoneDynamicSourceConfiguration.setFetchLastLevelOnly(true);
        allowedZonesGroup.setRefDynamicSourceConfiguration(allowedZoneDynamicSourceConfiguration);

        ZoneGroupConfiguration restrictedZonesGroup = new ZoneGroupConfiguration("zone", REPORT_TRANSITION_EVENTS_AND_PRESENCE_STATUS, false);
        var restrictedZoneDynamicSourceConfiguration = new RelationQueryDynamicSourceConfiguration();
        restrictedZoneDynamicSourceConfiguration.setDirection(EntitySearchDirection.FROM);
        restrictedZoneDynamicSourceConfiguration.setRelationType("RestrictedZone");
        restrictedZoneDynamicSourceConfiguration.setMaxLevel(1);
        restrictedZoneDynamicSourceConfiguration.setFetchLastLevelOnly(true);
        restrictedZonesGroup.setRefDynamicSourceConfiguration(restrictedZoneDynamicSourceConfiguration);

        cfg.setZoneGroups(Map.of("allowedZones", allowedZonesGroup, "restrictedZones", restrictedZonesGroup));

        // Output to server attributes
        Output out = new Output();
        out.setType(OutputType.ATTRIBUTES);
        out.setScope(AttributeScope.SERVER_SCOPE);
        cfg.setOutput(out);

        cf.setConfiguration(cfg);

        doPost("/api/calculatedField", cf, CalculatedField.class);

        // --- Assert initial evaluation (ENTERED / OUTSIDE) ---
        await().alias("initial geofencing evaluation")
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ArrayNode attrs = getServerAttributes(device.getId(),
                            "allowedZonesEvent", "allowedZonesStatus", "restrictedZonesStatus");
                    assertThat(attrs).isNotNull().isNotEmpty().hasSize(3);
                    Map<String, String> m = kv(attrs);
                    assertThat(m).containsEntry("allowedZonesEvent", "ENTERED")
                            .containsEntry("allowedZonesStatus", "INSIDE")
                            .containsEntry("restrictedZonesStatus", "OUTSIDE");
                });

        // --- Move the device into Restricted zone (and outside Allowed) ---
        doPost("/api/plugins/telemetry/DEVICE/" + device.getUuidId() + "/timeseries/unusedScope",
                JacksonUtil.toJsonNode("{\"latitude\":50.4760,\"longitude\":30.5110}"));

        // --- Assert transition (LEFT / ENTERED) ---
        await().alias("transition evaluation after movement")
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ArrayNode attrs = getServerAttributes(device.getId(),
                            "allowedZonesEvent", "allowedZonesStatus",
                            "restrictedZonesEvent", "restrictedZonesStatus");
                    assertThat(attrs).isNotNull().isNotEmpty().hasSize(4);
                    Map<String, String> m = kv(attrs);
                    assertThat(m).containsEntry("allowedZonesEvent", "LEFT")
                            .containsEntry("restrictedZonesEvent", "ENTERED")
                            .containsEntry("allowedZonesStatus", "OUTSIDE")
                            .containsEntry("restrictedZonesStatus", "INSIDE");
                });
    }

    @Test
    public void testGeofencingCalculatedField_DynamicRefresh_RebindsZoneArguments() throws Exception {
        // --- Update min allowed scheduled update intervals for CFs ---
        loginSysAdmin();
        EntityInfo tenantProfileEntityInfo = doGet("/api/tenantProfileInfo/default", EntityInfo.class);
        assertThat(tenantProfileEntityInfo).isNotNull();
        TenantProfile foundTenantProfile = doGet("/api/tenantProfile/" + tenantProfileEntityInfo.getId().getId().toString(), TenantProfile.class);
        assertThat(foundTenantProfile).isNotNull();
        assertThat(foundTenantProfile.getDefaultProfileConfiguration()).isNotNull();
        foundTenantProfile.getDefaultProfileConfiguration().setMinAllowedScheduledUpdateIntervalInSecForCF(TIMEOUT / 10);
        TenantProfile savedTenantProfile = doPost("/api/tenantProfile", foundTenantProfile, TenantProfile.class);
        assertThat(savedTenantProfile).isNotNull();
        assertThat(savedTenantProfile.getDefaultProfileConfiguration().getMinAllowedScheduledUpdateIntervalInSecForCF()).isEqualTo(TIMEOUT / 10);
        loginTenantAdmin();

        // --- Arrange entities ---
        Device device = createDevice("GF Device dyn", "sn-geo-dyn-1");

        // Allowed Zone A: covers initial point (ENTERED)
        String allowedPolygonA = "[[50.472000, 30.504000], [50.472000, 30.506000], [50.474000, 30.506000], [50.474000, 30.504000]]";

        Asset allowedZoneA = createAsset("Allowed Zone A", null);
        doPost("/api/plugins/telemetry/ASSET/" + allowedZoneA.getUuidId() + "/attributes/" + DataConstants.SERVER_SCOPE,
                JacksonUtil.toJsonNode("{\"zone\":" + allowedPolygonA + "}")).andExpect(status().isOk());

        // Relation from device to Allowed Zone A
        EntityRelation relAllowedA = new EntityRelation();
        relAllowedA.setFrom(device.getId());
        relAllowedA.setTo(allowedZoneA.getId());
        relAllowedA.setType("AllowedZone");
        doPost("/api/relation", relAllowedA).andExpect(status().isOk());

        // Initial device coordinates: INSIDE Zone A
        doPost("/api/plugins/telemetry/DEVICE/" + device.getUuidId() + "/timeseries/unusedScope",
                JacksonUtil.toJsonNode("{\"latitude\":50.4730,\"longitude\":30.5050}")).andExpect(status().isOk());

        // --- Build CF: GEOFENCING with dynamic 'allowedZones' and short scheduled refresh ---
        CalculatedField cf = new CalculatedField();
        cf.setEntityId(device.getId());
        cf.setType(CalculatedFieldType.GEOFENCING);
        cf.setName("Geofencing CF (dynamic refresh)");
        cf.setDebugSettings(DebugSettings.off());

        GeofencingCalculatedFieldConfiguration cfg = new GeofencingCalculatedFieldConfiguration();
        cfg.setEntityCoordinates(new EntityCoordinates(ENTITY_ID_LATITUDE_ARGUMENT_KEY, ENTITY_ID_LONGITUDE_ARGUMENT_KEY));

        var allowedZonesGroup = new ZoneGroupConfiguration("zone", REPORT_TRANSITION_EVENTS_AND_PRESENCE_STATUS, false);
        var allowedZoneDynamicSourceConfiguration = new RelationQueryDynamicSourceConfiguration();
        allowedZoneDynamicSourceConfiguration.setDirection(EntitySearchDirection.FROM);
        allowedZoneDynamicSourceConfiguration.setRelationType("AllowedZone");
        allowedZoneDynamicSourceConfiguration.setMaxLevel(1);
        allowedZoneDynamicSourceConfiguration.setFetchLastLevelOnly(true);
        allowedZonesGroup.setRefDynamicSourceConfiguration(allowedZoneDynamicSourceConfiguration);
        cfg.setZoneGroups(Map.of("allowedZones", allowedZonesGroup));

        // Server attributes output
        Output out = new Output();
        out.setType(OutputType.ATTRIBUTES);
        out.setScope(AttributeScope.SERVER_SCOPE);
        cfg.setOutput(out);

        // Enable scheduled refresh with a 6-second interval
        cfg.setScheduledUpdateInterval(6);

        cf.setConfiguration(cfg);
        CalculatedField savedCalculatedField = doPost("/api/calculatedField", cf, CalculatedField.class);
        assertThat(savedCalculatedField).isNotNull();
        CalculatedFieldConfiguration configuration = savedCalculatedField.getConfiguration();
        assertThat(configuration).isInstanceOf(GeofencingCalculatedFieldConfiguration.class);
        var geofencingConfiguration = (GeofencingCalculatedFieldConfiguration) configuration;
        assertThat(geofencingConfiguration.isScheduledUpdateEnabled()).isTrue();

        // --- Assert initial evaluation (ENTERED) ---
        await().alias("initial geofencing evaluation")
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ArrayNode attrs = getServerAttributes(device.getId(), "allowedZonesEvent", "allowedZonesStatus");
                    assertThat(attrs).isNotNull().isNotEmpty().hasSize(2);
                    Map<String, String> m = kv(attrs);
                    assertThat(m).containsEntry("allowedZonesEvent", "ENTERED")
                            .containsEntry("allowedZonesStatus", "INSIDE");
                });

        // --- Move device OUTSIDE Zone A (expect LEFT) ---
        doPost("/api/plugins/telemetry/DEVICE/" + device.getUuidId() + "/timeseries/unusedScope",
                JacksonUtil.toJsonNode("{\"latitude\":50.4760,\"longitude\":30.5110}")).andExpect(status().isOk());

        await().alias("outside zone A (LEFT)")
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ArrayNode attrs = getServerAttributes(device.getId(), "allowedZonesEvent", "allowedZonesStatus");
                    assertThat(attrs).isNotNull().isNotEmpty().hasSize(2);
                    Map<String, String> m = kv(attrs);
                    assertThat(m).containsEntry("allowedZonesEvent", "LEFT")
                            .containsEntry("allowedZonesStatus", "OUTSIDE");
                });

        // --- Create Allowed Zone B covering the CURRENT location ---
        String allowedPolygonB = "[[50.475500, 30.510500], [50.475500, 30.511500], [50.476500, 30.511500], [50.476500, 30.510500]]";

        Asset allowedZoneB = createAsset("Allowed Zone B", null);
        doPost("/api/plugins/telemetry/ASSET/" + allowedZoneB.getUuidId() + "/attributes/" + DataConstants.SERVER_SCOPE,
                JacksonUtil.toJsonNode("{\"zone\":" + allowedPolygonB + "}")).andExpect(status().isOk());

        // Add a new relation
        EntityRelation relAllowedB = new EntityRelation();
        relAllowedB.setFrom(device.getId());
        relAllowedB.setTo(allowedZoneB.getId());
        relAllowedB.setType("AllowedZone");
        doPost("/api/relation", relAllowedB).andExpect(status().isOk());

        awaitForCalculatedFieldEntityMessageProcessorToRegisterCfStateAsDirty(device.getId(), savedCalculatedField.getId());

        // --- Same coordinates as before, but now we expect ENTERED since a new zone is registered ---
        doPost("/api/plugins/telemetry/DEVICE/" + device.getUuidId() + "/timeseries/unusedScope",
                JacksonUtil.toJsonNode("{\"latitude\":50.4760,\"longitude\":30.5110}")).andExpect(status().isOk());

        // --- Assert dynamic refresh picks up new relation and flips event back to ENTERED on the next telemetry update ---
        await().alias("dynamic refresh rebinds allowedZones")
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ArrayNode attrs = getServerAttributes(device.getId(), "allowedZonesEvent", "allowedZonesStatus");
                    assertThat(attrs).isNotNull().isNotEmpty().hasSize(2);
                    Map<String, String> m = kv(attrs);
                    assertThat(m).containsEntry("allowedZonesEvent", "ENTERED")
                            .containsEntry("allowedZonesStatus", "INSIDE");
                });
    }

    private ObjectNode getLatestTelemetry(EntityId entityId, String... keys) throws Exception {
        return doGetAsync("/api/plugins/telemetry/" + entityId.getEntityType() + "/" + entityId.getId() + "/values/timeseries?keys=" + String.join(",", keys), ObjectNode.class);
    }

    private ArrayNode getServerAttributes(EntityId entityId, String... keys) throws Exception {
        return doGetAsync("/api/plugins/telemetry/" + entityId.getEntityType() + "/" + entityId.getId() + "/values/attributes/SERVER_SCOPE?keys=" + String.join(",", keys), ArrayNode.class);
    }

    private Asset createAsset(String name, AssetProfileId assetProfileId) {
        Asset asset = new Asset();
        asset.setName(name);
        asset.setAssetProfileId(assetProfileId);
        return doPost("/api/asset", asset, Asset.class);
    }

    private static Map<String, String> kv(ArrayNode attrs) {
        Map<String, String> m = new HashMap<>();
        for (JsonNode n : attrs) {
            m.put(n.get("key").asText(), n.get("value").asText());
        }
        return m;
    }

}
