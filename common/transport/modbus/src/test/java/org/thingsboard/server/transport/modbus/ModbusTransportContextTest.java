// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.transport.modbus;

import com.google.common.util.concurrent.MoreExecutors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.device.data.DeviceData;
import org.thingsboard.server.common.data.device.data.ModbusDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.device.profile.ModbusDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.common.data.transport.modbus.config.ModbusTcpEndpoint;
import org.thingsboard.server.common.transport.DeviceDeletedEvent;
import org.thingsboard.server.common.transport.DeviceUpdatedEvent;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.common.transport.auth.TransportDeviceInfo;
import org.thingsboard.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.thingsboard.server.common.transport.service.DeviceProtoService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.transport.modbus.service.ModbusTransportBalancingService;
import org.thingsboard.server.transport.modbus.service.ModbusTransportService;
import org.thingsboard.server.transport.modbus.session.ModbusSessionContext;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ModbusTransportContextTest {

    private static final String OWNED_HOST = "10.0.0.30";
    private static final String FOREIGN_HOST = "10.0.0.99";

    private final TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
    private final DeviceProfileId profileId = new DeviceProfileId(UUID.randomUUID());
    private final DeviceId deviceId = new DeviceId(UUID.randomUUID());

    private TransportService transportService;
    private DeviceProtoService deviceProtoService;
    private ModbusTransportBalancingService balancingService;
    private ModbusTransportService modbusTransportService;
    private ModbusTransportContext context;

    @BeforeEach
    void setUp() {
        transportService = mock(TransportService.class);
        deviceProtoService = mock(DeviceProtoService.class);
        balancingService = mock(ModbusTransportBalancingService.class);
        modbusTransportService = mock(ModbusTransportService.class);
        TbServiceInfoProvider serviceInfoProvider = mock(TbServiceInfoProvider.class);
        when(serviceInfoProvider.getServiceId()).thenReturn("tb-transport-1");

        context = new ModbusTransportContext(deviceProtoService, balancingService, modbusTransportService);
        ReflectionTestUtils.setField(context, "transportService", transportService);
        ReflectionTestUtils.setField(context, "serviceInfoProvider", serviceInfoProvider);
        ReflectionTestUtils.setField(context, "executor", MoreExecutors.newDirectExecutorService());

        when(balancingService.isManagedByCurrentTransport(anyString()))
                .thenAnswer(inv -> inv.<String>getArgument(0).contains(OWNED_HOST));
        when(deviceProtoService.getDeviceCredentialsByDeviceId(deviceId)).thenReturn(credentials(DeviceCredentialsType.ACCESS_TOKEN));
    }

    @Test
    void ownedDeviceGetsActiveRegisteredSession() {
        context.onDeviceUpdatedOrCreated(new DeviceUpdatedEvent(device(OWNED_HOST)));
        tokenCallback().onSuccess(validResponse());

        ModbusSessionContext session = sessions().get(deviceId);
        assertThat(session).isNotNull();
        assertThat(session.getState()).isEqualTo(ModbusSessionContext.State.ACTIVE);
        assertThat(session.getProfileConfig()).isNotNull();
        verify(transportService).registerAsyncSession(any(), eq(session));
        verify(transportService).process(any(TransportProtos.SessionInfoProto.class), any(TransportProtos.SubscribeToAttributeUpdatesMsg.class), any());
        verify(transportService).process(any(TransportProtos.SessionInfoProto.class), any(TransportProtos.SubscribeToRPCMsg.class), any());
        verify(modbusTransportService, times(1)).startPolling(session);
        verify(transportService).lifecycleEvent(tenantId, deviceId, ComponentLifecycleEvent.STARTED, true, null);
    }

    @Test
    void notOwnedDeviceGetsNoSession() {
        context.onDeviceUpdatedOrCreated(new DeviceUpdatedEvent(device(FOREIGN_HOST)));

        assertThat(sessions()).isEmpty();
        verify(transportService, never()).process(eq(DeviceTransportType.MODBUS), any(TransportProtos.ValidateDeviceTokenRequestMsg.class), any());
    }

    @Test
    void failedTokenValidationReleasesClaimAndAllowsRetry() {
        context.onDeviceUpdatedOrCreated(new DeviceUpdatedEvent(device(OWNED_HOST)));
        tokenCallback().onError(new RuntimeException("core unavailable"));

        assertThat(sessions()).isEmpty();

        context.onDeviceUpdatedOrCreated(new DeviceUpdatedEvent(device(OWNED_HOST)));
        verify(transportService, times(2)).process(eq(DeviceTransportType.MODBUS), any(TransportProtos.ValidateDeviceTokenRequestMsg.class), any());
        assertThat(sessions()).containsKey(deviceId);
    }

    @Test
    void nonAccessTokenCredentialsGetNoSession() {
        when(deviceProtoService.getDeviceCredentialsByDeviceId(deviceId)).thenReturn(credentials(DeviceCredentialsType.X509_CERTIFICATE));

        context.onDeviceUpdatedOrCreated(new DeviceUpdatedEvent(device(OWNED_HOST)));

        assertThat(sessions()).isEmpty();
        verify(transportService, never()).process(eq(DeviceTransportType.MODBUS), any(TransportProtos.ValidateDeviceTokenRequestMsg.class), any());
    }

    @Test
    void sessionDestroyedWhileEstablishingIsNeverRegistered() {
        context.onDeviceUpdatedOrCreated(new DeviceUpdatedEvent(device(OWNED_HOST)));
        TransportServiceCallback<ValidateDeviceCredentialsResponse> callback = tokenCallback();

        context.onDeviceUpdatedOrCreated(new DeviceUpdatedEvent(device(FOREIGN_HOST))); // ownership moves away
        callback.onSuccess(validResponse());

        assertThat(sessions()).isEmpty();
        verify(transportService, never()).registerAsyncSession(any(), any());
        verify(modbusTransportService, never()).startPolling(any());
    }

    @Test
    void repeatedUpdatesWhileEstablishingCreateOneSession() {
        context.onDeviceUpdatedOrCreated(new DeviceUpdatedEvent(device(OWNED_HOST)));
        context.onDeviceUpdatedOrCreated(new DeviceUpdatedEvent(device(OWNED_HOST)));

        verify(transportService, times(1)).process(eq(DeviceTransportType.MODBUS), any(TransportProtos.ValidateDeviceTokenRequestMsg.class), any());
        assertThat(sessions()).hasSize(1);
    }

    @Test
    void endpointMovedToForeignKeyDestroysActiveSession() {
        context.onDeviceUpdatedOrCreated(new DeviceUpdatedEvent(device(OWNED_HOST)));
        tokenCallback().onSuccess(validResponse());
        ModbusSessionContext session = sessions().get(deviceId);

        context.onDeviceUpdatedOrCreated(new DeviceUpdatedEvent(device(FOREIGN_HOST)));

        assertThat(sessions()).isEmpty();
        assertThat(session.getState()).isEqualTo(ModbusSessionContext.State.CLOSED);
        verify(modbusTransportService).stopPolling(session);
        verify(transportService).deregisterSession(session.getSessionInfo());
        verify(transportService).lifecycleEvent(tenantId, deviceId, ComponentLifecycleEvent.STOPPED, true, null);
    }

    @Test
    void deviceDeletedEventForgetsDeviceAndDestroysSession() {
        context.onDeviceUpdatedOrCreated(new DeviceUpdatedEvent(device(OWNED_HOST)));
        tokenCallback().onSuccess(validResponse());
        ModbusSessionContext session = sessions().get(deviceId);

        context.onDeviceDeletedEvent(new DeviceDeletedEvent(deviceId));

        assertThat(sessions()).isEmpty();
        assertThat(allModbusDeviceIds()).isEmpty();
        verify(transportService).deregisterSession(session.getSessionInfo());
    }

    @Test
    void deviceConfigChangeOnActiveSessionRestartsPolling() {
        context.onDeviceUpdatedOrCreated(new DeviceUpdatedEvent(device(OWNED_HOST)));
        tokenCallback().onSuccess(validResponse());
        ModbusSessionContext session = sessions().get(deviceId);

        Device changed = device(OWNED_HOST);
        ((ModbusDeviceTransportConfiguration) changed.getDeviceData().getTransportConfiguration()).setUnitId(7);
        context.onDeviceUpdatedOrCreated(new DeviceUpdatedEvent(changed));

        assertThat(session.getDeviceConfig().getUnitId()).isEqualTo(7);
        verify(modbusTransportService).stopPolling(session);
        verify(modbusTransportService, times(2)).startPolling(session);
        verify(transportService).lifecycleEvent(tenantId, deviceId, ComponentLifecycleEvent.UPDATED, true, null);
    }

    @SuppressWarnings("unchecked")
    private TransportServiceCallback<ValidateDeviceCredentialsResponse> tokenCallback() {
        ArgumentCaptor<TransportServiceCallback<ValidateDeviceCredentialsResponse>> captor = ArgumentCaptor.forClass(TransportServiceCallback.class);
        verify(transportService, org.mockito.Mockito.atLeastOnce())
                .process(eq(DeviceTransportType.MODBUS), any(TransportProtos.ValidateDeviceTokenRequestMsg.class), captor.capture());
        return captor.getValue();
    }

    @SuppressWarnings("unchecked")
    private Map<DeviceId, ModbusSessionContext> sessions() {
        return (Map<DeviceId, ModbusSessionContext>) ReflectionTestUtils.getField(context, "sessions");
    }

    @SuppressWarnings("unchecked")
    private Map<DeviceId, String> allModbusDeviceIds() {
        return (Map<DeviceId, String>) ReflectionTestUtils.getField(context, "allModbusDeviceIds");
    }

    private Device device(String host) {
        ModbusTcpEndpoint endpoint = new ModbusTcpEndpoint();
        endpoint.setHost(host);
        ModbusDeviceTransportConfiguration transportConfig = new ModbusDeviceTransportConfiguration();
        transportConfig.setEndpoint(endpoint);
        DeviceData deviceData = new DeviceData();
        deviceData.setTransportConfiguration(transportConfig);

        Device device = new Device(deviceId);
        device.setTenantId(tenantId);
        device.setDeviceProfileId(profileId);
        device.setName("Meter A");
        device.setDeviceData(deviceData);
        return device;
    }

    private DeviceCredentials credentials(DeviceCredentialsType type) {
        DeviceCredentials credentials = new DeviceCredentials();
        credentials.setDeviceId(deviceId);
        credentials.setCredentialsType(type);
        credentials.setCredentialsId("token-" + deviceId);
        return credentials;
    }

    private ValidateDeviceCredentialsResponse validResponse() {
        TransportDeviceInfo deviceInfo = new TransportDeviceInfo();
        deviceInfo.setTenantId(tenantId);
        deviceInfo.setCustomerId(new CustomerId(CustomerId.NULL_UUID));
        deviceInfo.setDeviceId(deviceId);
        deviceInfo.setDeviceProfileId(profileId);
        deviceInfo.setDeviceName("Meter A");
        deviceInfo.setDeviceType("meter");

        ModbusDeviceProfileTransportConfiguration profileConfig = new ModbusDeviceProfileTransportConfiguration();
        profileConfig.setTimeoutMs(3000);
        profileConfig.setRetries(1);
        profileConfig.setPollPeriodMs(5000L);
        DeviceProfileData profileData = new DeviceProfileData();
        profileData.setTransportConfiguration(profileConfig);
        DeviceProfile profile = new DeviceProfile(profileId);
        profile.setName("meter");
        profile.setTransportType(DeviceTransportType.MODBUS);
        profile.setProfileData(profileData);

        return ValidateDeviceCredentialsResponse.builder()
                .deviceInfo(deviceInfo)
                .deviceProfile(profile)
                .credentials("token-" + deviceId)
                .build();
    }

}
