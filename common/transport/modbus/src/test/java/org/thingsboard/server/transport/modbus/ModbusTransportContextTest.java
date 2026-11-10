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
import org.thingsboard.server.transport.modbus.downlink.ModbusDownlinkService;
import org.thingsboard.server.transport.modbus.service.ModbusTransportBalancingService;
import org.thingsboard.server.transport.modbus.service.ModbusTransportService;
import org.thingsboard.server.transport.modbus.session.ModbusSessionContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
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

        context = new ModbusTransportContext(deviceProtoService, balancingService, modbusTransportService,
                mock(ModbusDownlinkService.class));
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
    void sessionSubscribesToDownlinksOnlyOnceActive() {
        // core delivers pending RPCs right after the subscription: the session must already accept them
        List<ModbusSessionContext.State> statesOnSubscribe = new ArrayList<>();
        doAnswer(inv -> statesOnSubscribe.add(sessions().get(deviceId).getState()))
                .when(transportService).process(any(TransportProtos.SessionInfoProto.class), any(TransportProtos.SubscribeToRPCMsg.class), any());
        doAnswer(inv -> statesOnSubscribe.add(sessions().get(deviceId).getState()))
                .when(transportService).process(any(TransportProtos.SessionInfoProto.class), any(TransportProtos.SubscribeToAttributeUpdatesMsg.class), any());

        context.onDeviceUpdatedOrCreated(new DeviceUpdatedEvent(device(OWNED_HOST)));
        tokenCallback().onSuccess(validResponse());

        assertThat(statesOnSubscribe).containsExactly(ModbusSessionContext.State.ACTIVE, ModbusSessionContext.State.ACTIVE);
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

    @Test
    void destroyDuringRestartNeverLeavesPollingOnClosedSession() throws Exception {
        ModbusSessionContext session = activeSession();
        CountDownLatch restartStarted = new CountDownLatch(1);
        CountDownLatch letRestartContinue = new CountDownLatch(1);
        // the restart has seen the session active and is in the middle of starting its poll loops
        List<String> calls = recordPollingCalls(session, "start", restartStarted, letRestartContinue);

        Thread restart = new Thread(() -> context.onDeviceUpdatedOrCreated(new DeviceUpdatedEvent(deviceWithUnitId(7))), "restart");
        restart.start();
        assertThat(restartStarted.await(5, TimeUnit.SECONDS)).isTrue();
        Thread destroy = new Thread(() -> context.onDeviceDeletedEvent(new DeviceDeletedEvent(deviceId)), "destroy");
        destroy.start();
        awaitBlockedOrFinished(destroy);
        letRestartContinue.countDown();
        restart.join(5000);
        destroy.join(5000);

        assertThat(session.getState()).isEqualTo(ModbusSessionContext.State.CLOSED);
        // the destroy waited for the restart, so its stop comes after the loops were started and stops them
        assertThat(calls).containsExactly("stop", "start on ACTIVE", "started", "stop");
    }

    @Test
    void concurrentRestartsDoNotInterleave() throws Exception {
        ModbusSessionContext session = activeSession();
        CountDownLatch restartStarted = new CountDownLatch(1);
        CountDownLatch letRestartContinue = new CountDownLatch(1);
        List<String> calls = recordPollingCalls(session, "stop", restartStarted, letRestartContinue);

        Thread deviceRestart = new Thread(() -> context.onDeviceUpdatedOrCreated(new DeviceUpdatedEvent(deviceWithUnitId(7))), "device-restart");
        deviceRestart.start();
        assertThat(restartStarted.await(5, TimeUnit.SECONDS)).isTrue();
        Thread profileRestart = new Thread(() -> context.onDeviceProfileUpdated(session, profile(9000L)), "profile-restart");
        profileRestart.start();
        awaitBlockedOrFinished(profileRestart);
        letRestartContinue.countDown();
        deviceRestart.join(5000);
        profileRestart.join(5000);

        assertThat(calls).containsExactly("stop", "start on ACTIVE", "started", "stop", "start on ACTIVE", "started");
    }

    private ModbusSessionContext activeSession() {
        context.onDeviceUpdatedOrCreated(new DeviceUpdatedEvent(device(OWNED_HOST)));
        tokenCallback().onSuccess(validResponse());
        return sessions().get(deviceId);
    }

    private Device deviceWithUnitId(int unitId) {
        Device device = device(OWNED_HOST);
        ((ModbusDeviceTransportConfiguration) device.getDeviceData().getTransportConfiguration()).setUnitId(unitId);
        return device;
    }

    /**
     * Records every later stopPolling ("stop") and startPolling ("start on STATE" when it begins, "started" when it returns)
     * in call order. The first call of {@code pausedCall} is held until the test lets it go; later calls pass straight through.
     */
    private List<String> recordPollingCalls(ModbusSessionContext session, String pausedCall, CountDownLatch pauseReached, CountDownLatch letContinue) {
        List<String> calls = Collections.synchronizedList(new ArrayList<>());
        AtomicBoolean first = new AtomicBoolean(true);
        Runnable pause = () -> {
            if (first.getAndSet(false)) {
                pauseReached.countDown();
                try {
                    letContinue.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        };
        doAnswer(inv -> {
            calls.add("stop");
            if (pausedCall.equals("stop")) {
                pause.run();
            }
            return null;
        }).when(modbusTransportService).stopPolling(session);
        doAnswer(inv -> {
            calls.add("start on " + session.getState());
            if (pausedCall.equals("start")) {
                pause.run();
            }
            calls.add("started");
            return null;
        }).when(modbusTransportService).startPolling(session);
        return calls;
    }

    /** Waits until the thread is either parked on the session lock or done, i.e. it has got as far as it can. */
    private static void awaitBlockedOrFinished(Thread thread) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5000;
        while (thread.isAlive() && thread.getState() != Thread.State.BLOCKED && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }
    }

    @SuppressWarnings("unchecked")
    private TransportServiceCallback<ValidateDeviceCredentialsResponse> tokenCallback() {
        ArgumentCaptor<TransportServiceCallback<ValidateDeviceCredentialsResponse>> captor = ArgumentCaptor.forClass(TransportServiceCallback.class);
        verify(transportService, atLeastOnce())
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

        return ValidateDeviceCredentialsResponse.builder()
                .deviceInfo(deviceInfo)
                .deviceProfile(profile(5000L))
                .credentials("token-" + deviceId)
                .build();
    }

    private DeviceProfile profile(long pollPeriodMs) {
        ModbusDeviceProfileTransportConfiguration profileConfig = new ModbusDeviceProfileTransportConfiguration();
        profileConfig.setTimeoutMs(3000);
        profileConfig.setRetries(1);
        profileConfig.setPollPeriodMs(pollPeriodMs);
        DeviceProfileData profileData = new DeviceProfileData();
        profileData.setTransportConfiguration(profileConfig);
        DeviceProfile profile = new DeviceProfile(profileId);
        profile.setName("meter");
        profile.setTransportType(DeviceTransportType.MODBUS);
        profile.setProfileData(profileData);
        return profile;
    }

}
