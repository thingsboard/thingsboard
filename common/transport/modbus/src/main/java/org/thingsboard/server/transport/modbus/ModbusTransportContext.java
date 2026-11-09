// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.transport.modbus;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.device.data.ModbusDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.ModbusDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.common.transport.DeviceDeletedEvent;
import org.thingsboard.server.common.transport.DeviceUpdatedEvent;
import org.thingsboard.server.common.transport.TransportContext;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.common.transport.auth.SessionInfoCreator;
import org.thingsboard.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.thingsboard.server.common.transport.service.DeviceProtoService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.SessionInfoProto;
import org.thingsboard.server.queue.util.AfterStartUp;
import org.thingsboard.server.queue.util.TbModbusTransportComponent;
import org.thingsboard.server.transport.modbus.service.ModbusTransportBalancingService;
import org.thingsboard.server.transport.modbus.service.ModbusTransportService;
import org.thingsboard.server.transport.modbus.session.ModbusSessionContext;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@TbModbusTransportComponent
@Component
@RequiredArgsConstructor
public class ModbusTransportContext extends TransportContext {

    private final DeviceProtoService deviceProtoService;
    private final ModbusTransportBalancingService balancingService;
    private final ModbusTransportService modbusTransportService;

    @Value("${transport.modbus.batch_retries}")
    private int batchRetries;

    /** Every Modbus device known to this node (owned or not) with its endpoint key; used to re-check ownership on rebalance. */
    private final Map<DeviceId, String> allModbusDeviceIds = new ConcurrentHashMap<>();
    /** Sessions of devices owned by this node, including ones still being established. */
    private final Map<DeviceId, ModbusSessionContext> sessions = new ConcurrentHashMap<>();
    private final ExecutorService modbusBootstrap = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("modbus-bootstrap"));

    @AfterStartUp(order = AfterStartUp.AFTER_TRANSPORT_SERVICE)
    public void setUp() {
        modbusBootstrap.execute(this::bootstrap);
    }

    @PreDestroy
    public void destroy() {
        modbusBootstrap.shutdown();
    }

    private void bootstrap() {
        log.info("Initializing Modbus devices sessions");
        deviceProtoService.forEachDeviceIdByTransportType("bootstrap", DeviceTransportType.MODBUS, batchRetries, deviceId -> {
            Device device = deviceProtoService.getDeviceById(deviceId);
            if (device != null && getModbusDeviceTransportConfiguration(device) == null) {
                log.warn("[{}] Device is listed as Modbus but has no Modbus transport configuration", deviceId);
            }
            handleDevice(deviceId, device);
        });
    }

    public void onModbusTransportListChanged() {
        log.trace("Modbus transport list changed. Updating sessions");
        allModbusDeviceIds.forEach((deviceId, endpointKey) -> {
            if (!balancingService.isManagedByCurrentTransport(endpointKey)) {
                destroySession(deviceId);
            } else if (!sessions.containsKey(deviceId)) {
                getExecutor().execute(() -> handleDevice(deviceId, deviceProtoService.getDeviceById(deviceId)));
            }
        });
    }

    @EventListener(DeviceUpdatedEvent.class)
    public void onDeviceUpdatedOrCreated(DeviceUpdatedEvent deviceUpdatedEvent) {
        Device device = deviceUpdatedEvent.getDevice();
        getExecutor().execute(() -> handleDevice(device.getId(), device));
    }

    /** Published on every transport node when a device is deleted, so stale entries are cleaned on nodes without a session too. */
    @EventListener(DeviceDeletedEvent.class)
    public void onDeviceDeletedEvent(DeviceDeletedEvent event) {
        onDeviceDeleted(event.getDeviceId());
    }

    public void onDeviceDeleted(DeviceId deviceId) {
        getExecutor().execute(() -> handleDevice(deviceId, null));
    }

    public void onDeviceProfileUpdated(ModbusSessionContext session, DeviceProfile deviceProfile) {
        ModbusDeviceProfileTransportConfiguration newProfileConfig = getModbusProfileTransportConfiguration(deviceProfile);
        if (newProfileConfig == null) {
            log.debug("[{}] Device profile {} is no longer Modbus, destroying session", session.getDevice().getId(), deviceProfile.getId());
            destroySession(session.getDevice().getId());
            return;
        }
        if (!newProfileConfig.equals(session.getProfileConfig())) {
            session.setProfileConfig(newProfileConfig);
            restartPolling(session);
            transportService.lifecycleEvent(session.getTenantId(), session.getDeviceId(), ComponentLifecycleEvent.UPDATED, true, null);
        }
    }

    /**
     * Single entry point for bootstrap, device updates, deletes and rebalance:
     * refreshes the stored endpoint key and makes this node's sessions match ownership.
     * A deleted device ({@code null}) or a device that is not (or no longer) Modbus is forgotten and its session destroyed.
     */
    private void handleDevice(DeviceId deviceId, Device device) {
        var transportConfig = device != null ? getModbusDeviceTransportConfiguration(device) : null;
        if (transportConfig == null) {
            allModbusDeviceIds.remove(deviceId);
            destroySession(deviceId);
            return;
        }
        String endpointKey = transportConfig.getEndpoint().getKey();
        allModbusDeviceIds.put(deviceId, endpointKey);
        reconcile(device, transportConfig, endpointKey);
    }

    private void reconcile(Device device, ModbusDeviceTransportConfiguration transportConfig, String endpointKey) {
        DeviceId deviceId = device.getId();
        if (!balancingService.isManagedByCurrentTransport(endpointKey)) {
            destroySession(deviceId);
        } else if (sessions.containsKey(deviceId)) {
            updateSession(device, transportConfig);
        } else {
            getExecutor().execute(() -> establishSession(device, transportConfig));
        }
    }

    private void establishSession(Device device, ModbusDeviceTransportConfiguration transportConfig) {
        DeviceId deviceId = device.getId();
        ModbusSessionContext session = new ModbusSessionContext(this, device, transportConfig);
        if (sessions.putIfAbsent(deviceId, session) != null) {
            log.trace("[{}] Session already exists or is being established", deviceId);
            return;
        }
        log.info("[{}] Establishing Modbus session", deviceId);

        DeviceCredentials credentials;
        try {
            credentials = deviceProtoService.getDeviceCredentialsByDeviceId(deviceId);
        } catch (Exception e) {
            fail(session, e);
            return;
        }
        if (credentials.getCredentialsType() != DeviceCredentialsType.ACCESS_TOKEN) {
            log.warn("[{}] Expected credentials type is {} but found {}", deviceId, DeviceCredentialsType.ACCESS_TOKEN, credentials.getCredentialsType());
            fail(session, null);
            return;
        }
        verifyCredsAndRegisterSession(device, credentials, deviceId, session);
    }

    private void verifyCredsAndRegisterSession(Device device, DeviceCredentials credentials, DeviceId deviceId, ModbusSessionContext session) {
        transportService.process(DeviceTransportType.MODBUS,
                TransportProtos.ValidateDeviceTokenRequestMsg.newBuilder().setToken(credentials.getCredentialsId()).build(),
                new TransportServiceCallback<>() {
                    @Override
                    public void onSuccess(ValidateDeviceCredentialsResponse msg) {
                        if (!msg.hasDeviceInfo()) {
                            log.warn("[{}] Failed to validate device token", deviceId);
                            fail(session, null);
                            return;
                        }
                        boolean activated;
                        try {
                            activated = session.activate(() -> {
                                registerTransportSession(session, msg);
                                session.setProfileConfig(getModbusProfileTransportConfiguration(msg.getDeviceProfile()));
                                session.setSessionTimeoutHandler(() -> registerTransportSession(session, msg));
                                modbusTransportService.startPolling(session);
                            });
                        } catch (Exception e) {
                            if (session.getSessionInfo() != null) {
                                transportService.deregisterSession(session.getSessionInfo());
                            }
                            transportService.lifecycleEvent(device.getTenantId(), deviceId, ComponentLifecycleEvent.STARTED, false, e);
                            fail(session, e);
                            return;
                        }
                        if (activated) {
                            transportService.lifecycleEvent(session.getTenantId(), session.getDeviceId(), ComponentLifecycleEvent.STARTED, true, null);
                            log.info("[{}] Modbus session established", deviceId);
                        } else {
                            log.debug("[{}] Session was destroyed while being established", deviceId);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        log.warn("[{}] Failed to validate device token", deviceId, e);
                        transportService.lifecycleEvent(device.getTenantId(), deviceId, ComponentLifecycleEvent.STARTED, false, e);
                        fail(session, e);
                    }
                });
    }

    private void registerTransportSession(ModbusSessionContext session, ValidateDeviceCredentialsResponse msg) {
        SessionInfoProto sessionInfo = SessionInfoCreator.create(msg, this, session.getSessionId());
        session.setSessionInfo(sessionInfo);
        session.setDeviceInfo(msg.getDeviceInfo());
        session.setDeviceProfile(msg.getDeviceProfile());

        transportService.registerAsyncSession(sessionInfo, session);
        transportService.process(sessionInfo, TransportProtos.SubscribeToAttributeUpdatesMsg.newBuilder()
                .setSessionType(TransportProtos.SessionType.ASYNC)
                .build(), TransportServiceCallback.EMPTY);
        transportService.process(sessionInfo, TransportProtos.SubscribeToRPCMsg.newBuilder()
                .setSessionType(TransportProtos.SessionType.ASYNC)
                .build(), TransportServiceCallback.EMPTY);
        session.setConnected(true);
    }

    /** Releases the claim of a session that failed to establish, so a later device update can retry. */
    private void fail(ModbusSessionContext session, Throwable error) {
        DeviceId deviceId = session.getDevice().getId();
        sessions.remove(deviceId, session);
        session.close();
        if (error != null) {
            log.warn("[{}] Failed to establish Modbus session", deviceId, error);
        }
    }

    private void destroySession(DeviceId deviceId) {
        ModbusSessionContext session = sessions.remove(deviceId);
        if (session == null) {
            return;
        }
        boolean wasActive = session.close();
        if (wasActive) {
            modbusTransportService.stopPolling(session);
            transportService.deregisterSession(session.getSessionInfo());
            transportService.lifecycleEvent(session.getTenantId(), session.getDeviceId(), ComponentLifecycleEvent.STOPPED, true, null);
        }
        log.info("[{}] Destroyed Modbus session", deviceId);
    }

    private void updateSession(Device device, ModbusDeviceTransportConfiguration transportConfig) {
        ModbusSessionContext session = sessions.get(device.getId());
        if (session == null) {
            return;
        }
        ModbusDeviceTransportConfiguration previousConfig = session.getDeviceConfig();
        session.setDevice(device);
        session.setDeviceConfig(transportConfig);
        if (!session.isActive() || Objects.equals(previousConfig, transportConfig)) {
            return; // still establishing: activation picks up the latest config
        }

        restartPolling(session);
        transportService.lifecycleEvent(session.getTenantId(), session.getDeviceId(), ComponentLifecycleEvent.UPDATED, true, null);
    }

    private void restartPolling(ModbusSessionContext session) {
        modbusTransportService.stopPolling(session);
        if (session.isActive()) {
            modbusTransportService.startPolling(session);
        }
    }

    private static ModbusDeviceTransportConfiguration getModbusDeviceTransportConfiguration(Device device) {
        if (device.getDeviceData() != null
                && device.getDeviceData().getTransportConfiguration() instanceof ModbusDeviceTransportConfiguration transportConfig) {
            return transportConfig;
        }
        return null;
    }

    private static ModbusDeviceProfileTransportConfiguration getModbusProfileTransportConfiguration(DeviceProfile deviceProfile) {
        if (deviceProfile != null && deviceProfile.getProfileData() != null
                && deviceProfile.getProfileData().getTransportConfiguration() instanceof ModbusDeviceProfileTransportConfiguration profileConfig) {
            return profileConfig;
        }
        return null;
    }

}
