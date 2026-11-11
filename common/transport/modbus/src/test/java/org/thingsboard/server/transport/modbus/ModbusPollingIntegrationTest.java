// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.transport.modbus;

import com.digitalpetri.modbus.server.ModbusRtuServer;
import com.digitalpetri.modbus.server.ModbusTcpServer;
import com.digitalpetri.modbus.server.ProcessImage;
import com.digitalpetri.modbus.server.ReadWriteModbusServices;
import com.digitalpetri.modbus.tcp.server.NettyRtuServerTransport;
import com.digitalpetri.modbus.tcp.server.NettyTcpServerTransport;
import io.netty.channel.ChannelOption;
import org.awaitility.Awaitility;
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
import org.thingsboard.server.common.data.rpc.RpcStatus;
import org.thingsboard.server.common.data.transport.modbus.ModbusDataType;
import org.thingsboard.server.common.data.transport.modbus.ModbusFunctionCode;
import org.thingsboard.server.common.data.transport.modbus.ModbusMapping;
import org.thingsboard.server.common.data.transport.modbus.config.ModbusTcpEndpoint;
import org.thingsboard.server.common.data.transport.modbus.config.ModbusTcpFraming;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.auth.TransportDeviceInfo;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.modbus.connection.ModbusConnectionPool;
import org.thingsboard.server.transport.modbus.downlink.ModbusDownlinkService;
import org.thingsboard.server.transport.modbus.service.ModbusTransportService;
import org.thingsboard.server.transport.modbus.session.ModbusSessionContext;

import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Polls a real in-process Modbus TCP server through the connection pool, poll loops and codec. No Spring, no core.
 */
class ModbusPollingIntegrationTest {

    private static final long WAIT_MS = 5000;
    /** The client backs off 1 s, 2 s, 4 s… between connect attempts, so recovery is not immediate. */
    private static final long RECOVERY_MS = 20_000;

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
        startServer();

        transportService = mock(TransportService.class);
        pool = new ModbusConnectionPool();
        ReflectionTestUtils.setField(pool, "eventLoopThreads", 1);
        ReflectionTestUtils.setField(pool, "responseProcessingParallelism", 2);
        ReflectionTestUtils.setField(pool, "maxInFlightPerEndpoint", 1);
        ReflectionTestUtils.setField(pool, "maxQueueSizePerEndpoint", 100);
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
    void pollingResumesAfterServerRestart() throws Exception {
        writeInputFloat(1, 0, 1f);
        ModbusSessionContext session = session(1, profile(false, 100L));
        service.startPolling(session);
        verifyVoltagePolled(1.0, WAIT_MS);

        server.stop();
        verify(transportService, timeout(WAIT_MS).atLeastOnce()).errorEvent(any(), any(), anyString(), any());

        writeInputFloat(1, 0, 2f);
        startServer();

        verifyVoltagePolled(2.0, RECOVERY_MS);
        service.stopPolling(session);
    }

    @Test
    void pollingStartsWhenServerComesUpLater() throws Exception {
        server.stop();
        writeInputFloat(1, 0, 3f);
        ModbusSessionContext session = session(1, profile(false, 100L));

        service.startPolling(session);
        verify(transportService, timeout(WAIT_MS).atLeast(2)).errorEvent(any(), any(), anyString(), any());
        verify(transportService, times(0)).recordActivity(session.getSessionInfo());

        startServer();

        verifyVoltagePolled(3.0, RECOVERY_MS);
        verify(transportService, atLeastOnce()).recordActivity(session.getSessionInfo());
        service.stopPolling(session);
    }

    @Test
    void rtuOverTcpPollsAndWrites() throws Exception {
        int rtuPort;
        try (ServerSocket socket = new ServerSocket(0)) {
            rtuPort = socket.getLocalPort();
        }
        ModbusRtuServer rtuServer = ModbusRtuServer.create(NettyRtuServerTransport.create(cfg -> {
            cfg.bindAddress = "127.0.0.1";
            cfg.port = rtuPort;
        }), services());
        rtuServer.start();
        try {
            writeInputFloat(1, 0, 5.5f);
            ModbusDeviceProfileTransportConfiguration profile = profile(false, 100L);
            profile.setRpc(List.of(mapping("setSetpoint", ModbusDataType.UINT16, ModbusFunctionCode.WRITE_SINGLE_REGISTER, 30, null)));
            ModbusSessionContext session = session(1, profile);
            ModbusTcpEndpoint endpoint = (ModbusTcpEndpoint) session.getDeviceConfig().getEndpoint();
            endpoint.setPort(rtuPort);
            endpoint.setFraming(ModbusTcpFraming.RTU);
            session.activate(() -> {});
            service.startPolling(session);

            verifyVoltagePolled(5.5, WAIT_MS);
            assertThat(rpc(session, 1, "setSetpoint", "42").getPayload()).isEqualTo("{\"result\":\"ok\"}");
            assertThat(holdingRegister(1, 30)).containsExactly(0, 42);
            service.stopPolling(session);
        } finally {
            rtuServer.stop();
        }
    }

    @Test
    void framingConflictOnSameEndpointIsRejected() {
        ModbusSessionContext mbap = session(1, profile(false, 100L));
        service.startPolling(mbap);

        ModbusSessionContext rtu = session(2, profile(false, 100L));
        ((ModbusTcpEndpoint) rtu.getDeviceConfig().getEndpoint()).setFraming(ModbusTcpFraming.RTU);

        assertThatThrownBy(() -> service.startPolling(rtu)).isInstanceOf(IllegalStateException.class);
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
        rtuEndpoint.setFraming(ModbusTcpFraming.RTU);

        var mbapConnection = pool.acquire(mbap);
        var rtuConnection = pool.acquire(rtu);

        assertThat(mbapConnection.getMaxInFlight()).isEqualTo(4);
        assertThat(rtuConnection.getMaxInFlight()).isEqualTo(1);
        assertThat(rtuConnection.getSettings()).isEqualTo("framing=RTU");
        pool.release(mbap, mbapConnection);
        pool.release(rtu, rtuConnection);
        assertThat(pool.getConnectionsCount()).isZero();
    }

    @Test
    void sharedAttributeUpdateWritesHoldingRegister() {
        ModbusSessionContext session = activeWriteSession();

        downlink().onAttributeUpdate(session, TransportProtos.AttributeUpdateNotificationMsg.newBuilder()
                .addSharedUpdated(TransportProtos.TsKvProto.newBuilder().setKv(TransportProtos.KeyValueProto.newBuilder()
                        .setKey("ctRatio").setType(TransportProtos.KeyValueType.LONG_V).setLongV(200))).build());

        Awaitility.await().atMost(Duration.ofMillis(WAIT_MS))
                .untilAsserted(() -> assertThat(holdingRegister(1, 20)).containsExactly(0, 200));
        service.stopPolling(session);
    }

    @Test
    void rpcWritesCoilReadsRegisterAndMaskWritesBit() {
        ModbusSessionContext session = activeWriteSession();
        images.get(1).with(tx -> tx.writeHoldingRegisters(regs -> {
            regs.put(30, new byte[]{0, 7});
            regs.put(100, new byte[]{0, 0x0A});
        }));

        assertThat(rpc(session, 1, "resetEnergy", "true").getPayload()).isEqualTo("{\"result\":\"ok\"}");
        assertThat(images.get(1).<Boolean>get(tx -> tx.readCoils(coils -> coils.get(1)))).isTrue();

        assertThat(rpc(session, 2, "getSetpoint", "{}").getPayload()).isEqualTo("{\"value\":7}");

        assertThat(rpc(session, 3, "remoteControl", "{\"value\": true}").getError()).isEmpty();
        assertThat(holdingRegister(1, 100)).containsExactly(0, 0x8A); // bit 7 set, bits 1 and 3 kept
        service.stopPolling(session);
    }

    @Test
    void rpcWritesWithMultipleRegisterAndMultipleCoilFunctionCodes() {
        ModbusSessionContext session = activeWriteSession();

        assertThat(rpc(session, 1, "setLimit", "231.5").getPayload()).isEqualTo("{\"result\":\"ok\"}");
        byte[] expected = ByteBuffer.allocate(4).putFloat(231.5f).array();
        assertThat(holdingRegister(1, 40)).containsExactly(expected[0], expected[1]);
        assertThat(holdingRegister(1, 41)).containsExactly(expected[2], expected[3]);

        assertThat(rpc(session, 2, "enableOutput", "true").getPayload()).isEqualTo("{\"result\":\"ok\"}");
        assertThat(images.get(1).<Boolean>get(tx -> tx.readCoils(coils -> coils.get(3)))).isTrue();
        service.stopPolling(session);
    }

    @Test
    void rpcErrorsGoToErrorField() {
        ModbusSessionContext session = activeWriteSession();

        assertThat(rpc(session, 1, "nope", "1").getError()).isEqualTo("Unknown RPC method 'nope'");
        assertThat(rpc(session, 2, "setSetpoint", "{}").getError()).startsWith("Value is required");
        assertThat(rpc(session, 3, "setSetpoint", "70000").getError()).contains("out of range for UINT16");
        service.stopPolling(session);
    }

    @Test
    void rpcToUnitThatDoesNotAnswerTimesOut() {
        ModbusSessionContext session = activeWriteSession(5); // the server knows units 1 and 2 only and stays silent for others

        assertThat(rpc(session, 1, "getSetpoint", "{}").getError()).isEqualTo("Device did not respond in time");
        service.stopPolling(session);
    }

    @Test
    void oneWayRpcReportsDelivered() {
        ModbusSessionContext session = activeWriteSession();
        var msg = TransportProtos.ToDeviceRpcRequestMsg.newBuilder().setRequestId(9).setMethodName("resetEnergy").setParams("true").setOneway(true).build();

        downlink().onToDeviceRpcRequest(session, msg);

        verify(transportService, timeout(WAIT_MS)).process(eq(session.getSessionInfo()), eq(msg),
                eq(RpcStatus.DELIVERED), eq(true), any());
        service.stopPolling(session);
    }

    /** Starts (or restarts) the server on the same port; process images survive a restart. */
    private void startServer() throws Exception {
        server = ModbusTcpServer.create(NettyTcpServerTransport.create(cfg -> {
            cfg.bindAddress = "127.0.0.1";
            cfg.port = port;
            cfg.bootstrapCustomizer = bootstrap -> bootstrap.option(ChannelOption.SO_REUSEADDR, true);
        }), services());
        server.start();
    }

    /** Serves the shared process images, by unit id; unknown units get no answer. */
    private ReadWriteModbusServices services() {
        return new ReadWriteModbusServices() {
            @Override
            protected Optional<ProcessImage> getProcessImage(int unitId) {
                return Optional.ofNullable(images.get(unitId));
            }
        };
    }

    private void verifyVoltagePolled(double expected, long timeoutMs) {
        Awaitility.await().atMost(Duration.ofMillis(timeoutMs)).untilAsserted(() -> {
            ArgumentCaptor<TransportProtos.PostTelemetryMsg> captor = ArgumentCaptor.forClass(TransportProtos.PostTelemetryMsg.class);
            verify(transportService, atLeastOnce()).process(any(TransportProtos.SessionInfoProto.class), captor.capture(), any());
            assertThat(captor.getAllValues()).anyMatch(msg -> value(msg, "voltage").getDoubleV() == expected);
        });
    }

    private ModbusDownlinkService downlink() {
        return new ModbusDownlinkService(transportService);
    }

    private ModbusSessionContext activeWriteSession() {
        return activeWriteSession(1);
    }

    private ModbusSessionContext activeWriteSession(int unitId) {
        ModbusDeviceProfileTransportConfiguration profile = profile(false, 1000L);
        ModbusMapping remote = mapping("remoteControl", ModbusDataType.BITS, ModbusFunctionCode.MASK_WRITE_REGISTER, 100, 1);
        remote.setBit(7);
        profile.setAttributeUpdates(List.of(mapping("ctRatio", ModbusDataType.UINT16, ModbusFunctionCode.WRITE_SINGLE_REGISTER, 20, null)));
        profile.setRpc(List.of(
                mapping("resetEnergy", ModbusDataType.BITS, ModbusFunctionCode.WRITE_SINGLE_COIL, 1, 1),
                mapping("getSetpoint", ModbusDataType.UINT16, ModbusFunctionCode.READ_HOLDING_REGISTERS, 30, null),
                mapping("setSetpoint", ModbusDataType.UINT16, ModbusFunctionCode.WRITE_SINGLE_REGISTER, 30, null),
                mapping("setLimit", ModbusDataType.FLOAT32, ModbusFunctionCode.WRITE_MULTIPLE_REGISTERS, 40, null),
                mapping("enableOutput", ModbusDataType.BITS, ModbusFunctionCode.WRITE_MULTIPLE_COILS, 3, 1),
                remote));
        ModbusSessionContext session = session(unitId, profile);
        session.activate(() -> {});
        service.startPolling(session);
        return session;
    }

    private TransportProtos.ToDeviceRpcResponseMsg rpc(ModbusSessionContext session, int requestId, String method, String params) {
        downlink().onToDeviceRpcRequest(session, TransportProtos.ToDeviceRpcRequestMsg.newBuilder()
                .setRequestId(requestId).setMethodName(method).setParams(params).build());
        var response = new AtomicReference<TransportProtos.ToDeviceRpcResponseMsg>();
        Awaitility.await().atMost(Duration.ofMillis(WAIT_MS)).untilAsserted(() -> {
            ArgumentCaptor<TransportProtos.ToDeviceRpcResponseMsg> captor = ArgumentCaptor.forClass(TransportProtos.ToDeviceRpcResponseMsg.class);
            verify(transportService, atLeastOnce()).process(eq(session.getSessionInfo()), captor.capture(), any());
            response.set(captor.getAllValues().stream().filter(r -> r.getRequestId() == requestId).findFirst()
                    .orElseThrow(() -> new AssertionError("No response yet for request " + requestId)));
        });
        return response.get();
    }

    private byte[] holdingRegister(int unitId, int address) {
        return images.get(unitId).get(tx -> tx.readHoldingRegisters(regs -> regs.get(address)));
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
