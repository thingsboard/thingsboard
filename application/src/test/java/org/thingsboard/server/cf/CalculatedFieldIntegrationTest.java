/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
import org.thingsboard.server.controller.CalculatedFieldControllerTest;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DaoSqlTest
public class CalculatedFieldIntegrationTest extends CalculatedFieldControllerTest {

    @BeforeEach
    void setUp() throws Exception {
        loginTenantAdmin();
    }

    @Test
    public void testSimpleCalculatedField() throws Exception {
        Device testDevice = createDevice("Test device", "1234567890");

        JsonNode timeSeries = JacksonUtil.toJsonNode("{\"temperature\":25}");
        doPost("/api/plugins/telemetry/DEVICE/" + testDevice.getUuidId() + "/timeseries/" + DataConstants.SERVER_SCOPE, timeSeries);

        JsonNode attributes = JacksonUtil.toJsonNode("{\"deviceTemperature\":40}");
        doPost("/api/plugins/telemetry/DEVICE/" + testDevice.getUuidId() + "/attributes/" + DataConstants.SERVER_SCOPE, attributes);

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

        // create CF -> perform initial calculation
        CalculatedField savedCalculatedField = doPost("/api/calculatedField", calculatedField, CalculatedField.class);

        Thread.sleep(300);

        ObjectNode fahrenheitTemp = doGetAsync("/api/plugins/telemetry/DEVICE/" + testDevice.getUuidId() + "/values/timeseries?keys=fahrenheitTemp", ObjectNode.class);
        assertThat(fahrenheitTemp).isNotNull();
        assertThat(fahrenheitTemp.get("fahrenheitTemp").get(0).get("value").asText()).isEqualTo("77.0");

        // update telemetry -> recalculate state
        JsonNode newTelemetry = JacksonUtil.toJsonNode("{\"temperature\":30}");
        doPost("/api/plugins/telemetry/DEVICE/" + testDevice.getUuidId() + "/timeseries/" + DataConstants.SERVER_SCOPE, newTelemetry);

        Thread.sleep(300);

        ObjectNode fahrenheitTempAfterUpdate = doGetAsync("/api/plugins/telemetry/DEVICE/" + testDevice.getUuidId() + "/values/timeseries?keys=fahrenheitTemp", ObjectNode.class);
        assertThat(fahrenheitTempAfterUpdate).isNotNull();
        assertThat(fahrenheitTempAfterUpdate.get("fahrenheitTemp").get(0).get("value").asText()).isEqualTo("86.0");

        // update CF output -> perform calculation with updated output
        Output savedOutput = savedCalculatedField.getConfiguration().getOutput();
        savedOutput.setType(OutputType.ATTRIBUTES);
        savedOutput.setScope(AttributeScope.SERVER_SCOPE);
        savedOutput.setName("temperatureF");
        savedCalculatedField = doPost("/api/calculatedField", savedCalculatedField, CalculatedField.class);

        Thread.sleep(300);

        ArrayNode temperatureF = doGetAsync("/api/plugins/telemetry/DEVICE/" + testDevice.getUuidId() + "/values/attributes/SERVER_SCOPE?keys=temperatureF", ArrayNode.class);
        assertThat(temperatureF).isNotNull();
        assertThat(temperatureF.get(0).get("value").asText()).isEqualTo("86.0");

        // update CF argument -> perform calculation with new argument

        Argument savedArgument = savedCalculatedField.getConfiguration().getArguments().get("T");
        savedArgument.setRefEntityKey(new ReferencedEntityKey("deviceTemperature", ArgumentType.ATTRIBUTE, AttributeScope.SERVER_SCOPE));
        savedCalculatedField = doPost("/api/calculatedField", savedCalculatedField, CalculatedField.class);

        Thread.sleep(300);

        ArrayNode temperatureFAfterUpdateArg = doGetAsync("/api/plugins/telemetry/DEVICE/" + testDevice.getUuidId() + "/values/attributes/SERVER_SCOPE?keys=temperatureF", ArrayNode.class);
        assertThat(temperatureFAfterUpdateArg).isNotNull();
        assertThat(temperatureFAfterUpdateArg.get(0).get("value").asText()).isEqualTo("104.0");

        // update CF expression -> perform calculation with new expression
        savedCalculatedField.getConfiguration().setExpression("1.8 * T + 32");
        savedCalculatedField = doPost("/api/calculatedField", savedCalculatedField, CalculatedField.class);

        Thread.sleep(300);

        ArrayNode temperatureFAfterUpdateExpression = doGetAsync("/api/plugins/telemetry/DEVICE/" + testDevice.getUuidId() + "/values/attributes/SERVER_SCOPE?keys=temperatureF", ArrayNode.class);
        assertThat(temperatureFAfterUpdateExpression).isNotNull();
        assertThat(temperatureFAfterUpdateExpression.get(0).get("value").asText()).isEqualTo("104.0");
    }

    @Test
    public void testSimpleCalculatedFieldWhenEntityIdIsProfile() throws Exception {
        Device testDevice = createDevice("Test device", "1234567890");
        JsonNode deviceAttributes = JacksonUtil.toJsonNode("{\"x\":40}");
        doPost("/api/plugins/telemetry/DEVICE/" + testDevice.getUuidId() + "/attributes/" + DataConstants.SERVER_SCOPE, deviceAttributes);

        AssetProfile assetProfile = doPost("/api/assetProfile", createAssetProfile("Test Asset Profile"), AssetProfile.class);

        Asset asset1 = new Asset();
        asset1.setName("Test asset 1");
        asset1.setAssetProfileId(assetProfile.getId());

        Asset savedAsset1 = doPost("/api/asset", asset1, Asset.class);

        JsonNode asset1Attributes = JacksonUtil.toJsonNode("{\"y\":11}");
        doPost("/api/plugins/telemetry/ASSET/" + savedAsset1.getUuidId() + "/attributes/" + DataConstants.SERVER_SCOPE, asset1Attributes);

        Asset asset2 = new Asset();
        asset2.setName("Test asset 2");
        asset2.setAssetProfileId(assetProfile.getId());

        Asset savedAsset2 = doPost("/api/asset", asset2, Asset.class);

        JsonNode asset2Attributes = JacksonUtil.toJsonNode("{\"y\":12}");
        doPost("/api/plugins/telemetry/ASSET/" + savedAsset2.getUuidId() + "/attributes/" + DataConstants.SERVER_SCOPE, asset2Attributes);

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

        // create CF and perform initial calculation
        doPost("/api/calculatedField", calculatedField, CalculatedField.class);

        Thread.sleep(300);

        // result of asset 1
        ArrayNode z1 = doGetAsync("/api/plugins/telemetry/ASSET/" + savedAsset1.getUuidId() + "/values/attributes/SERVER_SCOPE?keys=z", ArrayNode.class);
        assertThat(z1).isNotNull();
        assertThat(z1.get(0).get("value").asText()).isEqualTo("51.0");

        // result of asset 2
        ArrayNode z2 = doGetAsync("/api/plugins/telemetry/ASSET/" + savedAsset2.getUuidId() + "/values/attributes/SERVER_SCOPE?keys=z", ArrayNode.class);
        assertThat(z2).isNotNull();
        assertThat(z2.get(0).get("value").asText()).isEqualTo("52.0");

        // update device telemetry -> recalculate state for all assets
        JsonNode updatedDeviceAttributes = JacksonUtil.toJsonNode("{\"x\":25}");
        doPost("/api/plugins/telemetry/DEVICE/" + testDevice.getUuidId() + "/attributes/" + DataConstants.SERVER_SCOPE, updatedDeviceAttributes);

        Thread.sleep(300);

        // result of asset 1
        ArrayNode updZ1 = doGetAsync("/api/plugins/telemetry/ASSET/" + savedAsset1.getUuidId() + "/values/attributes/SERVER_SCOPE?keys=z", ArrayNode.class);
        assertThat(updZ1).isNotNull();
        assertThat(updZ1.get(0).get("value").asText()).isEqualTo("36.0");

        // result of asset 2
        ArrayNode updZ2 = doGetAsync("/api/plugins/telemetry/ASSET/" + savedAsset2.getUuidId() + "/values/attributes/SERVER_SCOPE?keys=z", ArrayNode.class);
        assertThat(updZ2).isNotNull();
        assertThat(updZ2.get(0).get("value").asText()).isEqualTo("37.0");

//        // update asset 1 telemetry -> recalculate state only for asset 1
//        JsonNode updatedAsset1Attributes = JacksonUtil.toJsonNode("{\"x\":15}");
//        doPost("/api/plugins/telemetry/DEVICE/" + asset1.getUuidId() + "/attributes/" + DataConstants.SERVER_SCOPE, updatedAsset1Attributes);
//
//        Thread.sleep(300);
//
//        // result of asset 1
//        updZ1 = doGetAsync("/api/plugins/telemetry/ASSET/" + savedAsset1.getUuidId() + "/values/attributes/SERVER_SCOPE?keys=z", ArrayNode.class);
//        assertThat(updZ1).isNotNull();
//        assertThat(updZ1.get(0).get("value").asText()).isEqualTo("40.0");
//
//        // result of asset 2 (no changes)
//        updZ2 = doGetAsync("/api/plugins/telemetry/ASSET/" + savedAsset2.getUuidId() + "/values/attributes/SERVER_SCOPE?keys=z", ArrayNode.class);
//        assertThat(updZ2).isNotNull();
//        assertThat(updZ2.get(0).get("value").asText()).isEqualTo("37.0");
//
//        // update asset 2 telemetry -> recalculate state only for asset 2
//        JsonNode updatedAsset2Attributes = JacksonUtil.toJsonNode("{\"x\":5}");
//        doPost("/api/plugins/telemetry/DEVICE/" + asset2.getUuidId() + "/attributes/" + DataConstants.SERVER_SCOPE, updatedAsset2Attributes);
//
//        Thread.sleep(300);
//
//        // result of asset 1 (no changes)
//        updZ1 = doGetAsync("/api/plugins/telemetry/ASSET/" + savedAsset1.getUuidId() + "/values/attributes/SERVER_SCOPE?keys=z", ArrayNode.class);
//        assertThat(updZ1).isNotNull();
//        assertThat(updZ1.get(0).get("value").asText()).isEqualTo("40.0");
//
//        // result of asset 2
//        updZ2 = doGetAsync("/api/plugins/telemetry/ASSET/" + savedAsset2.getUuidId() + "/values/attributes/SERVER_SCOPE?keys=z", ArrayNode.class);
//        assertThat(updZ2).isNotNull();
//        assertThat(updZ2.get(0).get("value").asText()).isEqualTo("30.0");
    }

}
