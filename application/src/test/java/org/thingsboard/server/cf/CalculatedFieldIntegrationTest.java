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
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.ArgumentType;
import org.thingsboard.server.common.data.cf.configuration.Output;
import org.thingsboard.server.common.data.cf.configuration.OutputType;
import org.thingsboard.server.common.data.cf.configuration.ReferencedEntityKey;
import org.thingsboard.server.common.data.cf.configuration.SimpleCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.debug.DebugSettings;
import org.thingsboard.server.common.data.device.data.DefaultDeviceConfiguration;
import org.thingsboard.server.common.data.device.data.DefaultDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.data.DeviceData;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.controller.CalculatedFieldControllerTest;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Slf4j
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

        Argument savedArgument = savedCalculatedField.getConfiguration().getArguments().get("T");
        savedArgument.setRefEntityKey(new ReferencedEntityKey("deviceTemperature", ArgumentType.ATTRIBUTE, AttributeScope.SERVER_SCOPE));
        savedCalculatedField = doPost("/api/calculatedField", savedCalculatedField, CalculatedField.class);

        await().alias("update CF argument -> perform calculation with new argument").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ArrayNode temperatureF = getServerAttributes(testDevice.getId(), "temperatureF");
                    assertThat(temperatureF).isNotNull();
                    assertThat(temperatureF.get(0).get("value").asText()).isEqualTo("104.0");
                });

        savedCalculatedField.getConfiguration().setExpression("1.8 * T + 32");
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
    public void testReprocessCalculatedField() throws Exception {
        Device testDevice = createDevice("Test device", "1234567890");
        AssetProfile assetProfile = doPost("/api/assetProfile", createAssetProfile("Test Asset Profile"), AssetProfile.class);
        Asset testAsset = createAsset("Test asset", assetProfile.getId());

        long currentTime = System.currentTimeMillis();
        // reprocessing time window(TW)
        long startTs = currentTime - TimeUnit.SECONDS.toMillis(120);
        long endTs = currentTime - TimeUnit.SECONDS.toMillis(45);

        long a1Ts = currentTime - TimeUnit.SECONDS.toMillis(140); // outside the TW
        long a2b1Ts = currentTime - TimeUnit.SECONDS.toMillis(130); // outside the TW (but telemetry will be used for initial processing)
        long b2Ts = currentTime - TimeUnit.SECONDS.toMillis(80); // inside the TW
        long b3Ts = currentTime - TimeUnit.SECONDS.toMillis(70); // inside the TW
        long a3Ts = currentTime - TimeUnit.SECONDS.toMillis(60); // inside the TW
        long a4Ts = currentTime - TimeUnit.SECONDS.toMillis(50); // inside the TW
        long b4Ts = currentTime - TimeUnit.SECONDS.toMillis(40); // outside the TW

        pushTelemetry(testDevice.getId(), JacksonUtil.toJsonNode(String.format("{\"ts\":%s, \"values\":{\"a\":1}}", a1Ts)));
        pushTelemetry(testDevice.getId(), JacksonUtil.toJsonNode(String.format("{\"ts\":%s, \"values\":{\"a\":2}}", a2b1Ts)));
        pushTelemetry(testDevice.getId(), JacksonUtil.toJsonNode(String.format("{\"ts\":%s, \"values\":{\"a\":3}}", a3Ts)));
        pushTelemetry(testDevice.getId(), JacksonUtil.toJsonNode(String.format("{\"ts\":%s, \"values\":{\"a\":4}}", a4Ts)));

        pushTelemetry(testAsset.getId(), JacksonUtil.toJsonNode(String.format("{\"ts\":%s, \"values\":{\"b\":10}}", a2b1Ts)));
        pushTelemetry(testAsset.getId(), JacksonUtil.toJsonNode(String.format("{\"ts\":%s, \"values\":{\"b\":20}}", b2Ts)));
        pushTelemetry(testAsset.getId(), JacksonUtil.toJsonNode(String.format("{\"ts\":%s, \"values\":{\"b\":30}}", b3Ts)));
        pushTelemetry(testAsset.getId(), JacksonUtil.toJsonNode(String.format("{\"ts\":%s, \"values\":{\"b\":40}}", b4Ts)));

        /*      telemetry flow:
                             startTs                endTs
                               |______________________|
               |  ts  | 1    2 |   3     4     5    6 |   7
               |device| 1 -> 2 |->    ->    -> 3 -> 4 |
               |asset |      10|-> 20 -> 30 ->   ->   |-> 40
                               |______________________|
                                          |--- reprocessing time window
               the result should be: 12 -> 22 -> 32 -> 33 -> 34
        */

        CalculatedField savedCalculatedField = createCalculatedField(testDevice.getId(), testAsset.getId());

        doGet("/api/calculatedField/reprocess/" + savedCalculatedField.getUuidId() + "?startTs={startTs}&endTs={endTs}", startTs, endTs);

        await().alias("reprocess -> perform calculation for time window").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode fahrenheitTemp = getTimeSeries(testDevice.getId(), startTs, endTs, "result");
                    assertThat(fahrenheitTemp).isNotNull();

                    assertThat(fahrenheitTemp.get("result").get(0).get("ts").asText()).isEqualTo(Long.toString(a4Ts));
                    assertThat(fahrenheitTemp.get("result").get(0).get("value").asText()).isEqualTo("34.0");

                    assertThat(fahrenheitTemp.get("result").get(1).get("ts").asText()).isEqualTo(Long.toString(a3Ts));
                    assertThat(fahrenheitTemp.get("result").get(1).get("value").asText()).isEqualTo("33.0");

                    assertThat(fahrenheitTemp.get("result").get(2).get("ts").asText()).isEqualTo(Long.toString(b3Ts));
                    assertThat(fahrenheitTemp.get("result").get(2).get("value").asText()).isEqualTo("32.0");

                    assertThat(fahrenheitTemp.get("result").get(3).get("ts").asText()).isEqualTo(Long.toString(b2Ts));
                    assertThat(fahrenheitTemp.get("result").get(3).get("value").asText()).isEqualTo("22.0");

                    assertThat(fahrenheitTemp.get("result").get(4).get("ts").asText()).isEqualTo(Long.toString(startTs)); // we use reprocessing startTs instead of telemetry ts for initial calculation
                    assertThat(fahrenheitTemp.get("result").get(4).get("value").asText()).isEqualTo("12.0");
                });
    }

    @Test
    public void testReprocessCalculatedFieldWhenEntityIsProfile() throws Exception {
        long currentTime = System.currentTimeMillis();
        // reprocessing time window(TW)
        long startTs = currentTime - TimeUnit.SECONDS.toMillis(120);
        long endTs = currentTime - TimeUnit.SECONDS.toMillis(45);

        AssetProfile assetProfile = doPost("/api/assetProfile", createAssetProfile("Test Asset Profile"), AssetProfile.class);
        Asset testAsset = createAsset("Test asset", assetProfile.getId());
        long aTs_1 = currentTime - TimeUnit.SECONDS.toMillis(100);
        long aTs_2 = currentTime - TimeUnit.SECONDS.toMillis(85);
        long aTs_3 = currentTime - TimeUnit.SECONDS.toMillis(60);
        pushTelemetry(testAsset.getId(), JacksonUtil.toJsonNode(String.format("{\"ts\":%s, \"values\":{\"b\":100}}", aTs_1)));
        pushTelemetry(testAsset.getId(), JacksonUtil.toJsonNode(String.format("{\"ts\":%s, \"values\":{\"b\":200}}", aTs_2)));
        pushTelemetry(testAsset.getId(), JacksonUtil.toJsonNode(String.format("{\"ts\":%s, \"values\":{\"b\":300}}", aTs_3)));

        DeviceProfile deviceProfile = doPost("/api/deviceProfile", createDeviceProfile("Test Device Profile"), DeviceProfile.class);

        Device testDevice1 = createDevice("Test device 1", "1234567890111", deviceProfile.getId());
        long d1Ts_1 = currentTime - TimeUnit.SECONDS.toMillis(100);
        long d1Ts_2 = currentTime - TimeUnit.SECONDS.toMillis(90);
        pushTelemetry(testDevice1.getId(), JacksonUtil.toJsonNode(String.format("{\"ts\":%s, \"values\":{\"a\":10}}", d1Ts_1)));
        pushTelemetry(testDevice1.getId(), JacksonUtil.toJsonNode(String.format("{\"ts\":%s, \"values\":{\"a\":20}}", d1Ts_2)));


        Device testDevice2 = createDevice("Test device 2", "1234567890222", deviceProfile.getId());
        long d2Ts_1 = currentTime - TimeUnit.SECONDS.toMillis(90);
        long d2Ts_2 = currentTime - TimeUnit.SECONDS.toMillis(55);
        pushTelemetry(testDevice2.getId(), JacksonUtil.toJsonNode(String.format("{\"ts\":%s, \"values\":{\"a\":1}}", d2Ts_1)));
        pushTelemetry(testDevice2.getId(), JacksonUtil.toJsonNode(String.format("{\"ts\":%s, \"values\":{\"a\":2}}", d2Ts_2)));

        /*      telemetry flow:
                     startTs                         endTs
                       |_______________________________|
               |  ts   |   1      2     3      4     5 |
               |device1|  10  -> 20 ->     ->     ->   |
               |device2|      ->  1 ->     ->     -> 2 |
               |asset  |  100 ->    -> 200 -> 300 ->   |
                       |_______________________________|
                                         |--- reprocessing time window
               the result for device 1 should be: 110 -> 120 -> 220 -> 320
               the result for device 2 should be: 101 -> 201 -> 301 -> 302
        */

        CalculatedField savedCalculatedField = createCalculatedField(deviceProfile.getId(), testAsset.getId());

        doGet("/api/calculatedField/reprocess/" + savedCalculatedField.getUuidId() + "?startTs={startTs}&endTs={endTs}", startTs, endTs);

        await().alias("reprocess -> perform calculation for device 1").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode ab_1 = getTimeSeries(testDevice1.getId(), startTs, endTs, "result");
                    assertThat(ab_1).isNotNull();

                    assertThat(ab_1.get("result").get(0).get("ts").asText()).isEqualTo(Long.toString(aTs_3));
                    assertThat(ab_1.get("result").get(0).get("value").asText()).isEqualTo("320.0");

                    assertThat(ab_1.get("result").get(1).get("ts").asText()).isEqualTo(Long.toString(aTs_2));
                    assertThat(ab_1.get("result").get(1).get("value").asText()).isEqualTo("220.0");

                    assertThat(ab_1.get("result").get(2).get("ts").asText()).isEqualTo(Long.toString(d1Ts_2));
                    assertThat(ab_1.get("result").get(2).get("value").asText()).isEqualTo("120.0");

                    assertThat(ab_1.get("result").get(3).get("ts").asText()).isEqualTo(Long.toString(aTs_1));
                    assertThat(ab_1.get("result").get(3).get("value").asText()).isEqualTo("110.0");
                });

        await().alias("reprocess -> perform calculation for device 2").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode ab_2 = getTimeSeries(testDevice2.getId(), startTs, endTs, "result");
                    assertThat(ab_2).isNotNull();

                    assertThat(ab_2.get("result").get(0).get("ts").asText()).isEqualTo(Long.toString(d2Ts_2));
                    assertThat(ab_2.get("result").get(0).get("value").asText()).isEqualTo("302.0");

                    assertThat(ab_2.get("result").get(1).get("ts").asText()).isEqualTo(Long.toString(aTs_3));
                    assertThat(ab_2.get("result").get(1).get("value").asText()).isEqualTo("301.0");

                    assertThat(ab_2.get("result").get(2).get("ts").asText()).isEqualTo(Long.toString(aTs_2));
                    assertThat(ab_2.get("result").get(2).get("value").asText()).isEqualTo("201.0");

                    assertThat(ab_2.get("result").get(3).get("ts").asText()).isEqualTo(Long.toString(d2Ts_1));
                    assertThat(ab_2.get("result").get(3).get("value").asText()).isEqualTo("101.0");
                });
    }

    private CalculatedField createCalculatedField(EntityId entityId, EntityId refEntityId) {
        CalculatedField calculatedField = new CalculatedField();
        calculatedField.setEntityId(entityId);
        calculatedField.setType(CalculatedFieldType.SIMPLE);
        calculatedField.setName("A + B");
        calculatedField.setDebugSettings(DebugSettings.all());
        calculatedField.setConfigurationVersion(1);

        SimpleCalculatedFieldConfiguration config = new SimpleCalculatedFieldConfiguration();

        Argument a = new Argument();
        ReferencedEntityKey refEntityKeyA = new ReferencedEntityKey("a", ArgumentType.TS_LATEST, null);
        a.setRefEntityKey(refEntityKeyA);
        Argument b = new Argument();
        b.setRefEntityId(refEntityId);
        ReferencedEntityKey refEntityKeyB = new ReferencedEntityKey("b", ArgumentType.TS_LATEST, null);
        b.setRefEntityKey(refEntityKeyB);
        config.setArguments(Map.of("a", a, "b", b));
        config.setExpression("a + b");

        Output output = new Output();
        output.setName("result");
        output.setType(OutputType.TIME_SERIES);
        config.setOutput(output);

        calculatedField.setConfiguration(config);

        return doPost("/api/calculatedField", calculatedField, CalculatedField.class);
    }

    private void pushTelemetry(EntityId entityId, JsonNode telemetry) throws Exception {
        doPost("/api/plugins/telemetry/" + entityId.getEntityType() + "/" + entityId.getId() + "/timeseries/" + DataConstants.SERVER_SCOPE, telemetry);
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

    private Asset createAsset(String name, AssetProfileId assetProfileId) {
        Asset asset = new Asset();
        asset.setName(name);
        asset.setAssetProfileId(assetProfileId);
        return doPost("/api/asset", asset, Asset.class);
    }

    private Device createDevice(String name, String accessToken, DeviceProfileId deviceProfileId) {
        Device device = new Device();
        device.setName(name);
        device.setType("default");
        device.setDeviceProfileId(deviceProfileId);
        DeviceData deviceData = new DeviceData();
        deviceData.setTransportConfiguration(new DefaultDeviceTransportConfiguration());
        deviceData.setConfiguration(new DefaultDeviceConfiguration());
        device.setDeviceData(deviceData);
        return doPost("/api/device?accessToken=" + accessToken, device, Device.class);
    }

}
