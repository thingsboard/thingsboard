// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.transport.modbus;

import com.digitalpetri.modbus.server.ModbusTcpServer;
import com.digitalpetri.modbus.server.ProcessImage;
import com.digitalpetri.modbus.server.ReadWriteModbusServices;
import com.digitalpetri.modbus.tcp.server.NettyTcpServerTransport;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.device.data.DefaultDeviceConfiguration;
import org.thingsboard.server.common.data.device.data.DeviceData;
import org.thingsboard.server.common.data.device.data.ModbusDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.ModbusDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.transport.modbus.ModbusDataType;
import org.thingsboard.server.common.data.transport.modbus.ModbusFunctionCode;
import org.thingsboard.server.common.data.transport.modbus.ModbusMapping;
import org.thingsboard.server.common.data.transport.modbus.config.ModbusTcpEndpoint;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Modbus transport inside the running server: a profile and a device created over REST get a session, which polls an
 * in-process Modbus TCP server and writes to it on RPC and shared attribute updates.
 */
@TestPropertySource(properties = {
        "transport.modbus.enabled=true",
        "transport.modbus.min_poll_period_ms=50",
})
@DaoSqlTest
public class ModbusTransportIntegrationTest extends AbstractControllerTest {

    private static final int UNIT_ID = 1;
    private static final int OTHER_UNIT_ID = 2;
    private static final int VOLTAGE_ADDRESS = 0;
    private static final int CURRENT_ADDRESS = 10;
    private static final int CT_RATIO_ADDRESS = 20;
    private static final int SETPOINT_ADDRESS = 30;
    private static final int SERIAL_ADDRESS = 64;

    private final Map<Integer, ProcessImage> images = Map.of(UNIT_ID, new ProcessImage(), OTHER_UNIT_ID, new ProcessImage());
    private ModbusTcpServer server;
    private int port;

    @Before
    public void beforeTest() throws Exception {
        loginTenantAdmin();
        try (ServerSocket socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        }
        var services = new ReadWriteModbusServices() {
            @Override
            protected Optional<ProcessImage> getProcessImage(int unitId) {
                return Optional.ofNullable(images.get(unitId));
            }
        };
        server = ModbusTcpServer.create(NettyTcpServerTransport.create(cfg -> {
            cfg.bindAddress = "127.0.0.1";
            cfg.port = port;
        }), services);
        server.start();
    }

    @After
    public void afterTest() throws Exception {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    public void devicePollsTelemetryAndWritesOnRpcAndSharedAttributeUpdate() throws Exception {
        writeInputFloat(UNIT_ID, VOLTAGE_ADDRESS, 231.4f);
        writeHoldingString(UNIT_ID, SERIAL_ADDRESS, "SN12345");
        DeviceProfile profile = doPost("/api/deviceProfile", modbusProfile(), DeviceProfile.class);
        Device device = doPost("/api/device", modbusDevice(profile), Device.class);
        String deviceId = device.getId().getId().toString();

        await("voltage polled").atMost(30, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(latestValue(deviceId, "voltage")).isEqualTo("231.4"));
        await("serial polled as a client attribute").atMost(30, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(clientAttribute(deviceId, "serial")).isEqualTo("SN12345"));

        String setResult = doPostAsync("/api/rpc/twoway/" + deviceId, "{\"method\":\"setSetpoint\",\"params\":42}", String.class, status().isOk());
        assertThat(setResult).isEqualTo("{\"result\":\"ok\"}");
        assertThat(holdingRegister(SETPOINT_ADDRESS)).isEqualTo(42);

        String getResult = doPostAsync("/api/rpc/twoway/" + deviceId, "{\"method\":\"getSetpoint\",\"params\":{}}", String.class, status().isOk());
        assertThat(getResult).isEqualTo("{\"value\":42}");

        doPostAsync("/api/plugins/telemetry/DEVICE/" + deviceId + "/attributes/SHARED_SCOPE", "{\"ctRatio\":200}", String.class, status().isOk());
        await("ctRatio written").atMost(30, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(holdingRegister(CT_RATIO_ADDRESS)).isEqualTo(200));
    }

    @Test
    public void oneWayRpcWritesAndRpcErrorsReachTheRestCaller() throws Exception {
        DeviceProfile profile = doPost("/api/deviceProfile", modbusProfile(), DeviceProfile.class);
        Device device = doPost("/api/device", modbusDevice(profile), Device.class);
        String deviceId = device.getId().getId().toString();
        await("session established").atMost(30, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(latestValue(deviceId, "voltage")).isNotNull());

        doPostAsync("/api/rpc/oneway/" + deviceId, "{\"method\":\"setSetpoint\",\"params\":7}", String.class, status().isOk());
        await("one-way write").atMost(30, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(holdingRegister(SETPOINT_ADDRESS)).isEqualTo(7));

        String unknownMethod = doPostAsync("/api/rpc/twoway/" + deviceId, "{\"method\":\"nope\",\"params\":1}", String.class, status().isOk());
        assertThat(unknownMethod).isEqualTo("{\"error\":\"Unknown RPC method 'nope'\"}");

        String outOfRange = doPostAsync("/api/rpc/twoway/" + deviceId, "{\"method\":\"setSetpoint\",\"params\":70000}", String.class, status().isOk());
        assertThat(outOfRange).contains("out of range for UINT16");
        assertThat(holdingRegister(SETPOINT_ADDRESS)).isEqualTo(7);
    }

    @Test
    public void deviceAndProfileUpdatesApplyToTheRunningSession() throws Exception {
        writeInputFloat(UNIT_ID, VOLTAGE_ADDRESS, 1.5f);
        writeInputFloat(OTHER_UNIT_ID, VOLTAGE_ADDRESS, 2.5f);
        writeInputFloat(OTHER_UNIT_ID, CURRENT_ADDRESS, 7.25f);
        DeviceProfile profile = doPost("/api/deviceProfile", modbusProfile(), DeviceProfile.class);
        Device device = doPost("/api/device", modbusDevice(profile), Device.class);
        String deviceId = device.getId().getId().toString();
        await("voltage of unit 1").atMost(30, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(latestValue(deviceId, "voltage")).isEqualTo("1.5"));

        ((ModbusDeviceTransportConfiguration) device.getDeviceData().getTransportConfiguration()).setUnitId(OTHER_UNIT_ID);
        device = doPost("/api/device", device, Device.class);
        await("voltage of unit 2 after the device update").atMost(30, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(latestValue(deviceId, "voltage")).isEqualTo("2.5"));

        ModbusDeviceProfileTransportConfiguration profileConfig =
                (ModbusDeviceProfileTransportConfiguration) profile.getProfileData().getTransportConfiguration();
        profileConfig.setTimeseries(List.of(
                mapping("voltage", ModbusDataType.FLOAT32, ModbusFunctionCode.READ_INPUT_REGISTERS, VOLTAGE_ADDRESS),
                mapping("current", ModbusDataType.FLOAT32, ModbusFunctionCode.READ_INPUT_REGISTERS, CURRENT_ADDRESS)));
        doPost("/api/deviceProfile", profile, DeviceProfile.class);
        await("current, mapped by the profile update").atMost(30, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(latestValue(deviceId, "current")).isEqualTo("7.25"));
    }

    private DeviceProfile modbusProfile() {
        ModbusDeviceProfileTransportConfiguration config = new ModbusDeviceProfileTransportConfiguration();
        config.setTimeoutMs(1000);
        config.setRetries(0);
        config.setPollPeriodMs(200L);
        config.setTimeseries(List.of(mapping("voltage", ModbusDataType.FLOAT32, ModbusFunctionCode.READ_INPUT_REGISTERS, VOLTAGE_ADDRESS)));
        ModbusMapping serial = mapping("serial", ModbusDataType.STRING, ModbusFunctionCode.READ_HOLDING_REGISTERS, SERIAL_ADDRESS);
        serial.setObjectsCount(4);
        config.setAttributes(List.of(serial));
        config.setAttributeUpdates(List.of(mapping("ctRatio", ModbusDataType.UINT16, ModbusFunctionCode.WRITE_SINGLE_REGISTER, CT_RATIO_ADDRESS)));
        config.setRpc(List.of(
                mapping("setSetpoint", ModbusDataType.UINT16, ModbusFunctionCode.WRITE_SINGLE_REGISTER, SETPOINT_ADDRESS),
                mapping("getSetpoint", ModbusDataType.UINT16, ModbusFunctionCode.READ_HOLDING_REGISTERS, SETPOINT_ADDRESS)));
        return createDeviceProfile("Modbus meter", config);
    }

    private Device modbusDevice(DeviceProfile profile) {
        ModbusTcpEndpoint endpoint = new ModbusTcpEndpoint();
        endpoint.setHost("127.0.0.1");
        endpoint.setPort(port);
        ModbusDeviceTransportConfiguration transportConfig = new ModbusDeviceTransportConfiguration();
        transportConfig.setEndpoint(endpoint);
        transportConfig.setUnitId(UNIT_ID);
        DeviceData deviceData = new DeviceData();
        deviceData.setConfiguration(new DefaultDeviceConfiguration());
        deviceData.setTransportConfiguration(transportConfig);

        Device device = new Device();
        device.setName("Meter 1");
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

    private String latestValue(String deviceId, String key) throws Exception {
        Map<String, List<Map<String, Object>>> values = doGetAsyncTyped(
                "/api/plugins/telemetry/DEVICE/" + deviceId + "/values/timeseries?keys=" + key, new TypeReference<>() {});
        List<Map<String, Object>> series = values.get(key);
        return series == null || series.isEmpty() ? null : String.valueOf(series.get(0).get("value"));
    }

    private String clientAttribute(String deviceId, String key) throws Exception {
        List<Map<String, Object>> attributes = doGetAsyncTyped(
                "/api/plugins/telemetry/DEVICE/" + deviceId + "/values/attributes/CLIENT_SCOPE?keys=" + key, new TypeReference<>() {});
        return attributes.isEmpty() ? null : String.valueOf(attributes.get(0).get("value"));
    }

    private void writeHoldingString(int unitId, int address, String value) {
        byte[] bytes = Arrays.copyOf(value.getBytes(StandardCharsets.US_ASCII), 8);
        images.get(unitId).with(tx -> tx.writeHoldingRegisters(registers -> {
            for (int i = 0; i < 4; i++) {
                registers.put(address + i, new byte[]{bytes[2 * i], bytes[2 * i + 1]});
            }
        }));
    }

    private void writeInputFloat(int unitId, int address, float value) {
        byte[] bytes = ByteBuffer.allocate(4).putFloat(value).array();
        images.get(unitId).with(tx -> tx.writeInputRegisters(registers -> {
            registers.put(address, new byte[]{bytes[0], bytes[1]});
            registers.put(address + 1, new byte[]{bytes[2], bytes[3]});
        }));
    }

    private int holdingRegister(int address) {
        byte[] register = images.get(UNIT_ID).get(tx -> tx.readHoldingRegisters(registers -> registers.get(address)));
        return register == null ? -1 : ByteBuffer.wrap(register).getShort() & 0xFFFF;
    }

}
