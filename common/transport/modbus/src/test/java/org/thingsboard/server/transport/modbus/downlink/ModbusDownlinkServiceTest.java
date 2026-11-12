// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.transport.modbus.downlink;

import com.digitalpetri.modbus.exceptions.ModbusResponseException;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.auth.TransportDeviceInfo;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.ToDeviceRpcRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToDeviceRpcResponseMsg;
import org.thingsboard.server.transport.modbus.ModbusTransportContext;
import org.thingsboard.server.transport.modbus.connection.ModbusClientAdapter;
import org.thingsboard.server.transport.modbus.connection.ModbusConnection;
import org.thingsboard.server.transport.modbus.session.ModbusSessionContext;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Downlink decisions against a scripted client: no network, every response completes on the calling thread.
 */
class ModbusDownlinkServiceTest {

    private TransportService transportService;
    private ModbusDownlinkService downlink;
    private FakeClient client;
    private ModbusSessionContext session;

    @BeforeEach
    void setUp() {
        transportService = mock(TransportService.class);
        downlink = new ModbusDownlinkService(transportService);
        client = new FakeClient();
        session = session(2);
        session.setConnection(new ModbusConnection("tcp://h:502", "framing=MBAP", 1, 100, Duration.ofSeconds(1), t -> client));
        session.activate(() -> {});
    }

    @Test
    void rpcOnNonActiveSessionIsRejected() {
        session.close();

        downlink.onToDeviceRpcRequest(session, rpc("setSetpoint", "5").build());

        assertThat(response().getError()).isEqualTo("{\"error\":\"Device session is not active\"}"); // JSON, so REST callers get the text
        assertThat(client.calls).isZero();
    }

    @Test
    void attributeUpdateOnNonActiveSessionIsIgnored() {
        session.close();

        downlink.onAttributeUpdate(session, attributeUpdate("ctRatio", 200));

        assertThat(client.calls).isZero();
        verifyNoInteractions(transportService);
    }

    @Test
    void rpcWithoutConnectionIsAnsweredWithError() {
        session.setConnection(null);

        downlink.onToDeviceRpcRequest(session, rpc("setSetpoint", "5").build());

        assertThat(errorOf(response())).isEqualTo("Device is not connected");
    }

    @Test
    void timeoutsAreRetriedUpToProfileRetries() {
        client.timeouts = 2;

        downlink.onToDeviceRpcRequest(session, rpc("setSetpoint", "5").build());

        assertThat(response().getPayload()).isEqualTo("{\"result\":\"ok\"}");
        assertThat(client.calls).isEqualTo(3);
    }

    @Test
    void rpcFailsOnceRetriesAreExhausted() {
        client.timeouts = 10;

        downlink.onToDeviceRpcRequest(session, rpc("getSetpoint", "{}").build());

        assertThat(errorOf(response())).isEqualTo("Device did not respond in time");
        assertThat(client.calls).isEqualTo(3); // first attempt + 2 retries
    }

    @Test
    void deviceExceptionIsNotRetried() {
        client.failure = new ModbusResponseException(6, 2);

        downlink.onToDeviceRpcRequest(session, rpc("setSetpoint", "5").build());

        assertThat(errorOf(response())).isEqualTo("Modbus exception 02 (ILLEGAL_DATA_ADDRESS)");
        assertThat(client.calls).isEqualTo(1);
    }

    @Test
    void expiredRpcIsAnsweredWithErrorAndNeverSent() {
        downlink.onToDeviceRpcRequest(session, rpc("setSetpoint", "5").setExpirationTime(System.currentTimeMillis() - 1000).build());

        assertThat(errorOf(response())).isEqualTo("Request expired before it was sent");
        assertThat(client.calls).isZero();
    }

    @Test
    void successfulAttributeWriteRecordsActivity() {
        downlink.onAttributeUpdate(session, attributeUpdate("ctRatio", 200));

        assertThat(client.calls).isEqualTo(1);
        verify(transportService).recordActivity(session.getSessionInfo());
    }

    @Test
    void deliveredOneWayRpcReportsActivity() {
        ToDeviceRpcRequestMsg msg = rpc("setSetpoint", "5").setOneway(true).build();

        downlink.onToDeviceRpcRequest(session, msg);

        verify(transportService).process(eq(session.getSessionInfo()), eq(msg), eq(RpcStatus.DELIVERED), eq(true), any());
    }

    @Test
    void failedOneWayRpcReportsFailed() {
        client.failure = new ModbusResponseException(6, 2);
        ToDeviceRpcRequestMsg msg = rpc("setSetpoint", "5").setOneway(true).build();

        downlink.onToDeviceRpcRequest(session, msg);

        verify(transportService).process(eq(session.getSessionInfo()), eq(msg), eq(RpcStatus.FAILED), any());
    }

    @Test
    void failedAttributeWriteBecomesErrorEvent() {
        client.failure = new ModbusResponseException(6, 2);

        downlink.onAttributeUpdate(session, attributeUpdate("ctRatio", 200));

        ArgumentCaptor<Throwable> error = ArgumentCaptor.forClass(Throwable.class);
        verify(transportService).errorEvent(eq(session.getTenantId()), eq(session.getDeviceId()),
                eq(ModbusDownlinkService.ATTRIBUTE_UPDATE_ERROR), error.capture());
        assertThat(error.getValue()).hasMessage("Failed to write 'ctRatio': Modbus exception 02 (ILLEGAL_DATA_ADDRESS)");
        verify(transportService, never()).recordActivity(any());
    }

    @Test
    void attributeWriteRejectedByFullQueueBecomesErrorEvent() {
        FakeClient stuck = new FakeClient();
        stuck.hang = true;
        session.setConnection(new ModbusConnection("tcp://h:502", "framing=MBAP", 1, 1, Duration.ofSeconds(1), t -> stuck));
        downlink.onAttributeUpdate(session, attributeUpdate("ctRatio", 1)); // in flight, never answered
        downlink.onAttributeUpdate(session, attributeUpdate("ctRatio", 2)); // waiting

        downlink.onAttributeUpdate(session, attributeUpdate("ptRatio", 3)); // another attribute needs a place of its own

        ArgumentCaptor<Throwable> error = ArgumentCaptor.forClass(Throwable.class);
        verify(transportService).errorEvent(eq(session.getTenantId()), eq(session.getDeviceId()),
                eq(ModbusDownlinkService.ATTRIBUTE_UPDATE_ERROR), error.capture());
        assertThat(error.getValue()).hasMessageContaining("is overloaded: 1 requests are already waiting");
        assertThat(stuck.calls).isEqualTo(1);
    }

    @Test
    void waitingAttributeWriteIsReplacedByNewerValue() {
        FakeClient slow = hangingClient(100);
        downlink.onAttributeUpdate(session, attributeUpdate("ctRatio", 1)); // in flight
        downlink.onAttributeUpdate(session, attributeUpdate("ctRatio", 2)); // waiting
        downlink.onAttributeUpdate(session, attributeUpdate("ctRatio", 3)); // replaces 2

        slow.hanging.get(0).complete(null);
        slow.hanging.get(1).complete(null);

        assertThat(slow.written).containsExactly(1, 3);
        verify(transportService, times(2)).recordActivity(session.getSessionInfo());
        verify(transportService, never()).errorEvent(any(), any(), any(), any());
    }

    @Test
    void newerValueReplacesWaitingOneEvenWhenQueueIsFull() {
        FakeClient slow = hangingClient(1);
        downlink.onAttributeUpdate(session, attributeUpdate("ctRatio", 1)); // in flight
        downlink.onAttributeUpdate(session, attributeUpdate("ctRatio", 2)); // waiting, the queue is full

        downlink.onAttributeUpdate(session, attributeUpdate("ctRatio", 3));
        slow.hanging.get(0).complete(null);

        assertThat(slow.written).containsExactly(1, 3);
        verify(transportService, never()).errorEvent(any(), any(), any(), any());
    }

    @Test
    void differentAttributesAreNotCoalesced() {
        FakeClient slow = hangingClient(100);
        downlink.onAttributeUpdate(session, attributeUpdate("ctRatio", 1)); // in flight
        downlink.onAttributeUpdate(session, attributeUpdate("ctRatio", 2));
        downlink.onAttributeUpdate(session, attributeUpdate("ptRatio", 3));

        slow.hanging.get(0).complete(null);
        slow.hanging.get(1).complete(null);

        assertThat(slow.written).containsExactly(1, 2, 3);
    }

    @Test
    void sameAttributeOfAnotherDeviceIsNotCoalesced() {
        FakeClient slow = hangingClient(100);
        ModbusSessionContext other = session(2);
        other.setConnection(session.getConnection()); // both devices behind one endpoint
        other.activate(() -> {});
        downlink.onAttributeUpdate(session, attributeUpdate("ctRatio", 1)); // in flight
        downlink.onAttributeUpdate(session, attributeUpdate("ctRatio", 2));
        downlink.onAttributeUpdate(other, attributeUpdate("ctRatio", 3));

        slow.hanging.get(0).complete(null);
        slow.hanging.get(1).complete(null);

        assertThat(slow.written).containsExactly(1, 2, 3);
    }

    @Test
    void timedOutAttributeWriteIsNotRetriedOverNewerValue() {
        FakeClient slow = hangingClient(100);
        downlink.onAttributeUpdate(session, attributeUpdate("ctRatio", 1)); // in flight
        downlink.onAttributeUpdate(session, attributeUpdate("ctRatio", 2)); // waiting

        slow.hanging.get(0).completeExceptionally(new TimeoutException("request timed out"));
        slow.hanging.get(1).complete(null);

        assertThat(slow.written).containsExactly(1, 2); // 1 is not sent again
        verify(transportService, never()).errorEvent(any(), any(), any(), any());
    }

    @Test
    void timedOutAttributeWriteIsRetried() {
        client.timeouts = 2;

        downlink.onAttributeUpdate(session, attributeUpdate("ctRatio", 200));

        assertThat(client.calls).isEqualTo(3);
        verify(transportService).recordActivity(session.getSessionInfo());
        verify(transportService, never()).errorEvent(any(), any(), any(), any());
    }

    @Test
    void rpcWritesAreNotCoalesced() {
        FakeClient slow = hangingClient(100);
        downlink.onToDeviceRpcRequest(session, rpc("setSetpoint", "1").build()); // in flight
        downlink.onToDeviceRpcRequest(session, rpc("setSetpoint", "2").build());
        downlink.onToDeviceRpcRequest(session, rpc("setSetpoint", "3").build());

        slow.hanging.get(0).complete(null);
        slow.hanging.get(1).complete(null);
        slow.hanging.get(2).complete(null);

        assertThat(slow.written).containsExactly(1, 2, 3);
        verify(transportService, times(3)).process(eq(session.getSessionInfo()), any(ToDeviceRpcResponseMsg.class), any());
    }

    @Test
    void attributeWithoutMappingIsIgnored() {
        downlink.onAttributeUpdate(session, attributeUpdate("unmapped", 1));

        assertThat(client.calls).isZero();
        verifyNoInteractions(transportService);
    }

    /** Replaces the session's connection with one whose client answers only when the test completes {@code hanging}. */
    private FakeClient hangingClient(int maxQueueSize) {
        FakeClient slow = new FakeClient();
        slow.hang = true;
        session.setConnection(new ModbusConnection("tcp://h:502", "framing=MBAP", 1, maxQueueSize, Duration.ofSeconds(1), t -> slow));
        return slow;
    }

    private ToDeviceRpcResponseMsg response() {
        ArgumentCaptor<ToDeviceRpcResponseMsg> captor = ArgumentCaptor.forClass(ToDeviceRpcResponseMsg.class);
        verify(transportService).process(eq(session.getSessionInfo()), captor.capture(), any());
        return captor.getValue();
    }

    /** The message inside the {@code {"error": "…"}} body of a failed RPC, or "" when the RPC succeeded. */
    private static String errorOf(ToDeviceRpcResponseMsg response) {
        return response.getError().isEmpty() ? "" : JsonParser.parseString(response.getError()).getAsJsonObject().get("error").getAsString();
    }

    private static ToDeviceRpcRequestMsg.Builder rpc(String method, String params) {
        return ToDeviceRpcRequestMsg.newBuilder().setRequestId(1).setMethodName(method).setParams(params);
    }

    private static TransportProtos.AttributeUpdateNotificationMsg attributeUpdate(String key, long value) {
        return TransportProtos.AttributeUpdateNotificationMsg.newBuilder()
                .addSharedUpdated(TransportProtos.TsKvProto.newBuilder().setKv(TransportProtos.KeyValueProto.newBuilder()
                        .setKey(key).setType(TransportProtos.KeyValueType.LONG_V).setLongV(value)))
                .build();
    }

    private static ModbusSessionContext session(int retries) {
        ModbusDeviceProfileTransportConfiguration profile = new ModbusDeviceProfileTransportConfiguration();
        profile.setTimeoutMs(1000);
        profile.setRetries(retries);
        profile.setPollPeriodMs(1000L);
        profile.setAttributeUpdates(List.of(
                mapping("ctRatio", ModbusFunctionCode.WRITE_SINGLE_REGISTER, 20),
                mapping("ptRatio", ModbusFunctionCode.WRITE_SINGLE_REGISTER, 21)));
        profile.setRpc(List.of(
                mapping("setSetpoint", ModbusFunctionCode.WRITE_SINGLE_REGISTER, 30),
                mapping("getSetpoint", ModbusFunctionCode.READ_HOLDING_REGISTERS, 30)));

        ModbusDeviceTransportConfiguration deviceConfig = new ModbusDeviceTransportConfiguration();
        deviceConfig.setEndpoint(new ModbusTcpEndpoint());
        deviceConfig.setUnitId(1);

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
                .build());
        return session;
    }

    private static ModbusMapping mapping(String tag, ModbusFunctionCode functionCode, int address) {
        ModbusMapping mapping = new ModbusMapping();
        mapping.setTag(tag);
        mapping.setType(ModbusDataType.UINT16);
        mapping.setFunctionCode(functionCode);
        mapping.setAddress(address);
        return mapping;
    }

    /**
     * Answers every operation the same way: when the test completes {@code hanging} if {@code hang},
     * else {@code timeouts} timeouts first, then {@code failure} if set, else success.
     */
    private static class FakeClient implements ModbusClientAdapter {

        int calls;
        int timeouts;
        Throwable failure;
        boolean hang;
        final List<CompletableFuture<?>> hanging = new ArrayList<>();
        /** Values of single-register writes, in the order they reached the device. */
        final List<Integer> written = new ArrayList<>();

        private <T> CompletionStage<T> answer(T value) {
            calls++;
            if (hang) {
                CompletableFuture<T> future = new CompletableFuture<>();
                hanging.add(future);
                return future;
            }
            if (timeouts > 0) {
                timeouts--;
                return CompletableFuture.failedFuture(new TimeoutException("request timed out"));
            }
            return failure != null ? CompletableFuture.failedFuture(failure) : CompletableFuture.completedFuture(value);
        }

        @Override
        public CompletionStage<Void> connect() {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<Void> disconnect() {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<byte[]> read(int unitId, ModbusFunctionCode functionCode, int address, int count) {
            return answer(new byte[]{0, 7});
        }

        @Override
        public CompletionStage<Void> writeCoil(int unitId, ModbusFunctionCode functionCode, int address, boolean value) {
            return answer(null);
        }

        @Override
        public CompletionStage<Void> writeRegisters(int unitId, ModbusFunctionCode functionCode, int address, byte[] registerBytes) {
            written.add(((registerBytes[0] & 0xFF) << 8) | (registerBytes[1] & 0xFF));
            return answer(null);
        }

        @Override
        public CompletionStage<Void> maskWriteRegister(int unitId, int address, int andMask, int orMask) {
            return answer(null);
        }

    }

}
