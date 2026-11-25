// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.msa.connectivity;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.RandomStringUtils;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;
import org.thingsboard.server.common.data.DeviceProfileType;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.device.data.DefaultDeviceConfiguration;
import org.thingsboard.server.common.data.device.data.DeviceData;
import org.thingsboard.server.common.data.device.data.ModbusDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.device.profile.DisabledDeviceProfileProvisionConfiguration;
import org.thingsboard.server.common.data.device.profile.ModbusDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.transport.modbus.ModbusDataType;
import org.thingsboard.server.common.data.transport.modbus.ModbusFunctionCode;
import org.thingsboard.server.common.data.transport.modbus.ModbusMapping;
import org.thingsboard.server.common.data.transport.modbus.config.ModbusTcpEndpoint;
import org.thingsboard.server.msa.AbstractContainerTest;
import org.thingsboard.server.msa.DisableUIListeners;
import org.thingsboard.server.msa.TestProperties;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Runs against the standalone Modbus transport service and the simulator of docker-compose.modbus.yml.
 * The addresses and the expected values are the ones of advanced/modbus/modbus_server.json.
 */
@DisableUIListeners
public class ModbusClientTest extends AbstractContainerTest {

    private static final int UNIT_ID = 1;
    private static final int VOLTAGE_ADDRESS = 0;
    private static final int TEMPERATURE_ADDRESS = 2;
    private static final int RELAY_ADDRESS = 0;
    private static final int SETPOINT_ADDRESS = 30;
    private static final int CT_RATIO_ADDRESS = 31;
    private static final int SERIAL_ADDRESS = 64;

    private DeviceProfile deviceProfile;
    private Device device;

    @BeforeMethod
    public void setUp() {
        testRestClient.login("tenant@thingsboard.org", "tenant");
        deviceProfile = testRestClient.postDeviceProfile(modbusProfile());
        device = testRestClient.postDevice("", modbusDevice(deviceProfile));
        // an RPC or attribute update sent before the transport has opened the session of a new device is not delivered
        await("first poll of the new device").atMost(TIMEOUT * timeoutMultiplier, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(latestTelemetry("voltage")).containsKey("voltage"));
    }

    @AfterMethod
    public void tearDown() {
        if (device != null) {
            testRestClient.deleteDeviceIfExists(device.getId());
        }
        if (deviceProfile != null) {
            testRestClient.deleteDeviceProfileIfExists(deviceProfile);
        }
    }

    @Test
    public void telemetryAndAttributesArePolled() {
        await("telemetry polled from the input registers").atMost(TIMEOUT * timeoutMultiplier, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(latestTelemetry("voltage", "temperature"))
                        .containsEntry("voltage", "231.5")
                        .containsEntry("temperature", "21.5"));

        await("client attributes polled from the holding registers and coils").atMost(TIMEOUT * timeoutMultiplier, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(clientAttributes("serial", "relay"))
                        .containsEntry("serial", "TB-00001")
                        .containsEntry("relay", "true"));
    }

    @Test
    public void rpcWritesAndReadsRegister() {
        int setpoint = randomRegisterValue();

        JsonNode writeResponse = rpc("setSetpoint", setpoint);
        assertThat(writeResponse.get("result").asText()).isEqualTo("ok");

        JsonNode readResponse = rpc("getSetpoint", null);
        assertThat(readResponse.get("value").asInt()).isEqualTo(setpoint);
    }

    @Test
    public void sharedAttributeUpdateIsWrittenToRegister() {
        int ctRatio = randomRegisterValue();
        ObjectNode sharedAttribute = JacksonUtil.newObjectNode();
        sharedAttribute.put("ctRatio", ctRatio);

        testRestClient.postTelemetryAttribute(device.getId(), AttributeScope.SHARED_SCOPE, sharedAttribute);

        await("shared attribute written to the holding register").atMost(TIMEOUT * timeoutMultiplier, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(rpc("getCtRatio", null).path("value").asInt(-1)).isEqualTo(ctRatio));
    }

    private JsonNode rpc(String method, Object params) {
        ObjectNode request = JacksonUtil.newObjectNode();
        request.put("method", method);
        request.set("params", params == null ? JacksonUtil.newObjectNode() : JacksonUtil.valueToTree(params));
        request.put("timeout", 10000);
        return testRestClient.postServerSideRpc(device.getId(), request);
    }

    private Map<String, String> latestTelemetry(String... keys) {
        return testRestClient.getLatestTimeseries(device.getId(), List.of(keys)).stream()
                .filter(entry -> entry.getValue() != null)
                .collect(Collectors.toMap(TsKvEntry::getKey, TsKvEntry::getValueAsString));
    }

    private Map<String, String> clientAttributes(String... keys) {
        ArrayNode attributes = testRestClient.getAttributes(device.getId(), AttributeScope.CLIENT_SCOPE, String.join(",", keys));
        return JacksonUtil.convertValue(attributes, new TypeReference<List<JsonNode>>() {}).stream()
                .collect(Collectors.toMap(attribute -> attribute.get("key").asText(), attribute -> attribute.get("value").asText()));
    }

    private static int randomRegisterValue() {
        return ThreadLocalRandom.current().nextInt(1, 60000);
    }

    private static DeviceProfile modbusProfile() {
        ModbusDeviceProfileTransportConfiguration transportConfiguration = new ModbusDeviceProfileTransportConfiguration();
        transportConfiguration.setTimeoutMs(2000);
        transportConfiguration.setRetries(1);
        transportConfiguration.setPollPeriodMs(1000L);
        ModbusMapping temperature = mapping("temperature", ModbusDataType.INT16, ModbusFunctionCode.READ_INPUT_REGISTERS, TEMPERATURE_ADDRESS);
        temperature.setMultiplier(0.1);
        transportConfiguration.setTimeseries(List.of(
                mapping("voltage", ModbusDataType.FLOAT32, ModbusFunctionCode.READ_INPUT_REGISTERS, VOLTAGE_ADDRESS),
                temperature));
        ModbusMapping serial = mapping("serial", ModbusDataType.STRING, ModbusFunctionCode.READ_HOLDING_REGISTERS, SERIAL_ADDRESS);
        serial.setObjectsCount(4);
        transportConfiguration.setAttributes(List.of(
                serial,
                mapping("relay", ModbusDataType.BITS, ModbusFunctionCode.READ_COILS, RELAY_ADDRESS)));
        transportConfiguration.setAttributeUpdates(List.of(
                mapping("ctRatio", ModbusDataType.UINT16, ModbusFunctionCode.WRITE_SINGLE_REGISTER, CT_RATIO_ADDRESS)));
        transportConfiguration.setRpc(List.of(
                mapping("setSetpoint", ModbusDataType.UINT16, ModbusFunctionCode.WRITE_SINGLE_REGISTER, SETPOINT_ADDRESS),
                mapping("getSetpoint", ModbusDataType.UINT16, ModbusFunctionCode.READ_HOLDING_REGISTERS, SETPOINT_ADDRESS),
                mapping("getCtRatio", ModbusDataType.UINT16, ModbusFunctionCode.READ_HOLDING_REGISTERS, CT_RATIO_ADDRESS)));

        DeviceProfileData profileData = new DeviceProfileData();
        profileData.setConfiguration(new DefaultDeviceProfileConfiguration());
        profileData.setTransportConfiguration(transportConfiguration);
        profileData.setProvisionConfiguration(new DisabledDeviceProfileProvisionConfiguration(null));

        DeviceProfile profile = new DeviceProfile();
        profile.setName("modbus_" + RandomStringUtils.secure().nextAlphanumeric(7));
        profile.setType(DeviceProfileType.DEFAULT);
        profile.setTransportType(DeviceTransportType.MODBUS);
        profile.setProvisionType(DeviceProfileProvisionType.DISABLED);
        profile.setProfileData(profileData);
        return profile;
    }

    private static Device modbusDevice(DeviceProfile profile) {
        ModbusTcpEndpoint endpoint = new ModbusTcpEndpoint();
        endpoint.setHost(TestProperties.getModbusSimulatorHost());
        endpoint.setPort(TestProperties.getModbusSimulatorPort());
        ModbusDeviceTransportConfiguration transportConfiguration = new ModbusDeviceTransportConfiguration();
        transportConfiguration.setEndpoint(endpoint);
        transportConfiguration.setUnitId(UNIT_ID);
        DeviceData deviceData = new DeviceData();
        deviceData.setConfiguration(new DefaultDeviceConfiguration());
        deviceData.setTransportConfiguration(transportConfiguration);

        Device device = new Device();
        device.setName("modbus_" + RandomStringUtils.secure().nextAlphanumeric(7));
        device.setDeviceProfileId(profile.getId());
        device.setDeviceData(deviceData);
        return device;
    }

    private static ModbusMapping mapping(String tag, ModbusDataType type, ModbusFunctionCode functionCode, int address) {
        ModbusMapping mapping = new ModbusMapping();
        mapping.setTag(tag);
        mapping.setType(type);
        mapping.setFunctionCode(functionCode);
        mapping.setAddress(address);
        return mapping;
    }

}
