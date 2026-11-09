// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.transport.modbus;

import com.digitalpetri.modbus.server.ModbusTcpServer;
import com.digitalpetri.modbus.server.ProcessImage;
import com.digitalpetri.modbus.server.ReadWriteModbusServices;
import com.digitalpetri.modbus.tcp.server.NettyTcpServerTransport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.device.data.ModbusDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.ModbusDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.transport.modbus.ModbusDataType;
import org.thingsboard.server.common.data.transport.modbus.ModbusFunctionCode;
import org.thingsboard.server.common.data.transport.modbus.ModbusMapping;
import org.thingsboard.server.common.data.transport.modbus.config.ModbusTcpEndpoint;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.auth.TransportDeviceInfo;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.modbus.connection.ModbusConnectionPool;
import org.thingsboard.server.transport.modbus.service.ModbusTransportService;
import org.thingsboard.server.transport.modbus.session.ModbusSessionContext;

import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Polls a real in-process Modbus TCP server through the connection pool, poll loops and codec. No Spring, no core.
 */
class ModbusPollingIntegrationTest {

    private static final long WAIT_MS = 5000;

    private final Map<Integer, ProcessImage> images = new ConcurrentHashMap<>();
    private ModbusTcpServer server;
    private int port;

    private TransportService transportService;
    private ModbusConnectionPool pool;
    private ModbusTransportService service;

    @BeforeEach
    void setUp() throws Exception {
        try (ServerSocket socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        }
        images.put(1, new ProcessImage());
        images.put(2, new ProcessImage());
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

        transportService = mock(TransportService.class);
        pool = new ModbusConnectionPool();
        ReflectionTestUtils.setField(pool, "eventLoopThreads", 1);
        ReflectionTestUtils.setField(pool, "responseProcessingParallelism", 2);
        ReflectionTestUtils.setField(pool, "maxInFlightPerEndpoint", 1);
        ReflectionTestUtils.setField(pool, "connectTimeoutMs", 1000L);
        pool.init();
        service = new ModbusTransportService(pool, transportService);
        ReflectionTestUtils.setField(service, "schedulerThreadPoolSize", 2);
        ReflectionTestUtils.setField(service, "blockMergeMaxGap", 8);
        ReflectionTestUtils.setField(service, "minPollPeriodMs", 50L);
        service.init();
    }

    @AfterEach
    void tearDown() throws Exception {
        service.destroy();
        pool.destroy();
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void pollsTelemetryAndAttributes() {
        writeInputFloat(1, 0, 231.4f);
        writeHoldingString(1, 64, "SN12345");
        ModbusSessionContext session = session(1, profile(false, 200L));

        service.startPolling(session);

        var telemetry = captureTelemetry(1);
        assertThat(value(telemetry, "voltage").getDoubleV()).isEqualTo(231.4);
        var attributes = captureAttributes(1);
        assertThat(value(attributes, "serial").getStringV()).isEqualTo("SN12345");
        verify(transportService, timeout(WAIT_MS).atLeastOnce()).recordActivity(session.getSessionInfo());
        service.stopPolling(session);
    }

    @Test
    void sendDataOnlyOnChangeSuppressesRepeats() {
        writeInputFloat(1, 0, 10f);
        ModbusSessionContext session = session(1, profile(true, 100L));

        service.startPolling(session);

        verify(transportService, timeout(WAIT_MS).times(1)).process(any(TransportProtos.SessionInfoProto.class), any(TransportProtos.PostTelemetryMsg.class), any());
        verify(transportService, after(600).times(1)).process(any(TransportProtos.SessionInfoProto.class), any(TransportProtos.PostTelemetryMsg.class), any());

        writeInputFloat(1, 0, 11f);
        var second = captureTelemetry(2);
        assertThat(value(second, "voltage").getDoubleV()).isEqualTo(11.0);
        service.stopPolling(session);
    }

    @Test
    void devicesBehindOneGatewayShareOneConnection() {
        writeInputFloat(1, 0, 1f);
        writeInputFloat(2, 0, 2f);
        ModbusSessionContext first = session(1, profile(false, 100L));
        ModbusSessionContext second = session(2, profile(false, 100L));

        service.startPolling(first);
        service.startPolling(second);

        assertThat(pool.getConnectionsCount()).isEqualTo(1);
        verify(transportService, timeout(WAIT_MS).atLeastOnce()).recordActivity(first.getSessionInfo());
        verify(transportService, timeout(WAIT_MS).atLeastOnce()).recordActivity(second.getSessionInfo());

        service.stopPolling(first);
        assertThat(pool.getConnectionsCount()).isEqualTo(1);
        service.stopPolling(second);
        assertThat(pool.getConnectionsCount()).isZero();
        assertThat(first.getPollLoops()).isEmpty();
    }

    @Test
    void unreachableDeviceReportsErrors() throws Exception {
        server.stop();
        server = null;
        ModbusSessionContext session = session(1, profile(false, 100L));

        service.startPolling(session);

        verify(transportService, timeout(WAIT_MS).atLeast(2)).errorEvent(any(), any(), anyString(), any());
        verify(transportService, times(0)).recordActivity(session.getSessionInfo());
        service.stopPolling(session);
    }

    @Test
    void framingConflictOnSameEndpointIsRejected() {
        ModbusSessionContext mbap = session(1, profile(false, 100L));
        service.startPolling(mbap);

        ModbusSessionContext rtu = session(2, profile(false, 100L));
        ((ModbusTcpEndpoint) rtu.getDeviceConfig().getEndpoint()).setFraming(org.thingsboard.server.common.data.transport.modbus.config.ModbusTcpFraming.RTU);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.startPolling(rtu)).isInstanceOf(IllegalStateException.class);
        assertThat(rtu.getConnection()).isNull();
        assertThat(pool.getConnectionsCount()).isEqualTo(1);
        service.stopPolling(mbap);
    }

    @Test
    void rtuFramingAlwaysAllowsOneRequestInFlight() {
        ReflectionTestUtils.setField(pool, "maxInFlightPerEndpoint", 4);
        ModbusSessionContext mbap = session(1, profile(false, 100L));
        ModbusSessionContext rtu = session(2, profile(false, 100L));
        ModbusTcpEndpoint rtuEndpoint = (ModbusTcpEndpoint) rtu.getDeviceConfig().getEndpoint();
        rtuEndpoint.setPort(port + 1); // separate key; nothing listens there, the connection just keeps retrying
        rtuEndpoint.setFraming(org.thingsboard.server.common.data.transport.modbus.config.ModbusTcpFraming.RTU);

        var mbapConnection = pool.acquire(mbap);
        var rtuConnection = pool.acquire(rtu);

        assertThat(mbapConnection.getMaxInFlight()).isEqualTo(4);
        assertThat(rtuConnection.getMaxInFlight()).isEqualTo(1);
        assertThat(rtuConnection.getSettings()).isEqualTo("framing=RTU");
        pool.release(mbap, mbapConnection);
        pool.release(rtu, rtuConnection);
        assertThat(pool.getConnectionsCount()).isZero();
    }

    private TransportProtos.PostTelemetryMsg captureTelemetry(int times) {
        ArgumentCaptor<TransportProtos.PostTelemetryMsg> captor = ArgumentCaptor.forClass(TransportProtos.PostTelemetryMsg.class);
        verify(transportService, timeout(WAIT_MS).atLeast(times)).process(any(TransportProtos.SessionInfoProto.class), captor.capture(), any());
        return captor.getAllValues().get(times - 1);
    }

    private TransportProtos.PostAttributeMsg captureAttributes(int times) {
        ArgumentCaptor<TransportProtos.PostAttributeMsg> captor = ArgumentCaptor.forClass(TransportProtos.PostAttributeMsg.class);
        verify(transportService, timeout(WAIT_MS).atLeast(times)).process(any(TransportProtos.SessionInfoProto.class), captor.capture(), any());
        return captor.getAllValues().get(times - 1);
    }

    private static TransportProtos.KeyValueProto value(TransportProtos.PostTelemetryMsg msg, String key) {
        return msg.getTsKvListList().stream().flatMap(ts -> ts.getKvList().stream())
                .filter(kv -> kv.getKey().equals(key)).findFirst().orElseThrow();
    }

    private static TransportProtos.KeyValueProto value(TransportProtos.PostAttributeMsg msg, String key) {
        return msg.getKvList().stream().filter(kv -> kv.getKey().equals(key)).findFirst().orElseThrow();
    }

    private ModbusSessionContext session(int unitId, ModbusDeviceProfileTransportConfiguration profile) {
        ModbusTcpEndpoint endpoint = new ModbusTcpEndpoint();
        endpoint.setHost("127.0.0.1");
        endpoint.setPort(port);
        ModbusDeviceTransportConfiguration deviceConfig = new ModbusDeviceTransportConfiguration();
        deviceConfig.setEndpoint(endpoint);
        deviceConfig.setUnitId(unitId);

        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
        Device device = new Device(deviceId);
        device.setTenantId(tenantId);

        ModbusSessionContext session = new ModbusSessionContext(mock(ModbusTransportContext.class), device, deviceConfig);
        session.setProfileConfig(profile);
        TransportDeviceInfo deviceInfo = new TransportDeviceInfo();
        deviceInfo.setTenantId(tenantId);
        deviceInfo.setCustomerId(new CustomerId(CustomerId.NULL_UUID));
        deviceInfo.setDeviceId(deviceId);
        deviceInfo.setDeviceProfileId(new DeviceProfileId(UUID.randomUUID()));
        session.setDeviceInfo(deviceInfo);
        session.setSessionInfo(TransportProtos.SessionInfoProto.newBuilder()
                .setSessionIdMSB(session.getSessionId().getMostSignificantBits())
                .setSessionIdLSB(session.getSessionId().getLeastSignificantBits())
                .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                .build());
        return session;
    }

    private static ModbusDeviceProfileTransportConfiguration profile(boolean onlyOnChange, long periodMs) {
        ModbusDeviceProfileTransportConfiguration profile = new ModbusDeviceProfileTransportConfiguration();
        profile.setTimeoutMs(300);
        profile.setRetries(0);
        profile.setPollPeriodMs(periodMs);
        profile.setSendDataOnlyOnChange(onlyOnChange);
        profile.setTimeseries(List.of(mapping("voltage", ModbusDataType.FLOAT32, ModbusFunctionCode.READ_INPUT_REGISTERS, 0, null)));
        profile.setAttributes(List.of(mapping("serial", ModbusDataType.STRING, ModbusFunctionCode.READ_HOLDING_REGISTERS, 64, 4)));
        return profile;
    }

    private static ModbusMapping mapping(String tag, ModbusDataType type, ModbusFunctionCode fc, int address, Integer count) {
        ModbusMapping mapping = new ModbusMapping();
        mapping.setTag(tag);
        mapping.setType(type);
        mapping.setFunctionCode(fc);
        mapping.setAddress(address);
        mapping.setObjectsCount(count);
        return mapping;
    }

    private void writeInputFloat(int unitId, int address, float value) {
        byte[] bytes = ByteBuffer.allocate(4).putFloat(value).array();
        images.get(unitId).with(tx -> tx.writeInputRegisters(regs -> {
            regs.put(address, new byte[]{bytes[0], bytes[1]});
            regs.put(address + 1, new byte[]{bytes[2], bytes[3]});
        }));
    }

    private void writeHoldingString(int unitId, int address, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.US_ASCII);
        images.get(unitId).with(tx -> tx.writeHoldingRegisters(regs -> {
            for (int i = 0; i < 4; i++) {
                byte high = 2 * i < bytes.length ? bytes[2 * i] : 0;
                byte low = 2 * i + 1 < bytes.length ? bytes[2 * i + 1] : 0;
                regs.put(address + i, new byte[]{high, low});
            }
        }));
    }

}
