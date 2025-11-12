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
package org.thingsboard.server.transport.snmp;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.device.data.DeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.data.SnmpDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.SnmpDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.common.transport.DeviceUpdatedEvent;
import org.thingsboard.server.common.transport.TransportContext;
import org.thingsboard.server.common.transport.TransportDeviceProfileCache;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.common.transport.auth.SessionInfoCreator;
import org.thingsboard.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.SessionInfoProto;
import org.thingsboard.server.queue.util.AfterStartUp;
import org.thingsboard.server.queue.util.TbSnmpTransportComponent;
import org.thingsboard.server.transport.snmp.service.ProtoTransportEntityService;
import org.thingsboard.server.transport.snmp.service.SnmpAuthService;
import org.thingsboard.server.transport.snmp.service.SnmpTransportBalancingService;
import org.thingsboard.server.transport.snmp.service.SnmpTransportService;
import org.thingsboard.server.transport.snmp.session.DeviceSessionContext;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@TbSnmpTransportComponent
@Component
@Slf4j
@RequiredArgsConstructor
public class SnmpTransportContext extends TransportContext {
    @Getter
    private final SnmpTransportService snmpTransportService;
    private final TransportDeviceProfileCache deviceProfileCache;
    private final TransportService transportService;
    private final ProtoTransportEntityService protoEntityService;
    private final SnmpTransportBalancingService balancingService;
    @Getter
    private final SnmpAuthService snmpAuthService;

    private final Map<DeviceId, DeviceSessionContext> sessions = new ConcurrentHashMap<>();
    private final Set<DeviceId> allSnmpDevicesIds = ConcurrentHashMap.newKeySet();

    @Value("${transport.snmp.bootstrap_retries}")
    private int snmpBootstrapMaxRetries;

    @AfterStartUp(order = AfterStartUp.AFTER_TRANSPORT_SERVICE)
    public void fetchDevicesAndEstablishSessions() {
        getExecutor().execute(this::bootstrapWithRetries);
    }

    private void bootstrapWithRetries() {
        for (int attempt = 1; attempt <= snmpBootstrapMaxRetries; attempt++) {
            try {
                doBootstrap();
                return;
            } catch (Exception e) {
                if (attempt >= snmpBootstrapMaxRetries) {
                    log.error("SNMP bootstrap failed after {} attempts.", attempt, e);
                    return;
                }
                log.warn("SNMP bootstrap attempt {}/{} failed. Retrying immediately...", attempt, snmpBootstrapMaxRetries, e);
            }
        }
    }

    private void doBootstrap() {
        log.info("Initializing SNMP devices sessions");
        int batchIndex = 0;
        final int batchSize = 512;
        boolean nextBatchExists = true;

        while (nextBatchExists) {
            TransportProtos.GetSnmpDevicesResponseMsg snmpDevicesResponse = protoEntityService.getSnmpDevicesIds(batchIndex, batchSize);
            snmpDevicesResponse.getIdsList().stream()
                    .map(id -> new DeviceId(UUID.fromString(id)))
                    .peek(allSnmpDevicesIds::add)
                    .filter(deviceId -> balancingService.isManagedByCurrentTransport(deviceId.getId()))
                    .map(protoEntityService::getDeviceById)
                    .forEach(device -> {
                        if (!sessions.containsKey(device.getId())) {
                            getExecutor().execute(() -> establishDeviceSession(device));
                        }
                    });

            nextBatchExists = snmpDevicesResponse.getHasNextPage();
            batchIndex++;
        }

        log.debug("Found all SNMP devices ids: {}", allSnmpDevicesIds.size());
    }

    private void establishDeviceSession(Device device) {
        if (device == null) return;
        log.info("Establishing SNMP session for device {}", device.getId());

        DeviceProfileId deviceProfileId = device.getDeviceProfileId();
        DeviceProfile deviceProfile = deviceProfileCache.get(deviceProfileId);

        DeviceCredentials credentials = protoEntityService.getDeviceCredentialsByDeviceId(device.getId());
        if (credentials.getCredentialsType() != DeviceCredentialsType.ACCESS_TOKEN) {
            log.warn("[{}] Expected credentials type is {} but found {}", device.getId(), DeviceCredentialsType.ACCESS_TOKEN, credentials.getCredentialsType());
            return;
        }

        SnmpDeviceProfileTransportConfiguration profileTransportConfiguration = (SnmpDeviceProfileTransportConfiguration) deviceProfile.getProfileData().getTransportConfiguration();
        SnmpDeviceTransportConfiguration deviceTransportConfiguration = (SnmpDeviceTransportConfiguration) device.getDeviceData().getTransportConfiguration();

        DeviceSessionContext sessionContext;
        try {
            sessionContext = DeviceSessionContext.builder()
                    .tenantId(deviceProfile.getTenantId())
                    .device(device)
                    .deviceProfile(deviceProfile)
                    .token(credentials.getCredentialsId())
                    .profileTransportConfiguration(profileTransportConfiguration)
                    .deviceTransportConfiguration(deviceTransportConfiguration)
                    .snmpTransportContext(this)
                    .build();
            registerSessionMsgListener(sessionContext);
        } catch (Exception e) {
            log.error("Failed to establish session for SNMP device {}", device.getId(), e);
            transportService.errorEvent(device.getTenantId(), device.getId(), "sessionEstablishing", e);
            return;
        }
        sessions.put(device.getId(), sessionContext);
        snmpTransportService.createQueryingTasks(sessionContext);
        log.info("Established SNMP device session for device {}", device.getId());
    }

    private void updateDeviceSession(DeviceSessionContext sessionContext, Device device, DeviceProfile deviceProfile) {
        log.info("Updating SNMP session for device {}", device.getId());

        DeviceCredentials credentials = protoEntityService.getDeviceCredentialsByDeviceId(device.getId());
        if (credentials.getCredentialsType() != DeviceCredentialsType.ACCESS_TOKEN) {
            log.warn("[{}] Expected credentials type is {} but found {}", device.getId(), DeviceCredentialsType.ACCESS_TOKEN, credentials.getCredentialsType());
            destroyDeviceSession(sessionContext);
            return;
        }

        SnmpDeviceProfileTransportConfiguration newProfileTransportConfiguration = (SnmpDeviceProfileTransportConfiguration) deviceProfile.getProfileData().getTransportConfiguration();
        SnmpDeviceTransportConfiguration newDeviceTransportConfiguration = (SnmpDeviceTransportConfiguration) device.getDeviceData().getTransportConfiguration();

        try {
            if (!newProfileTransportConfiguration.equals(sessionContext.getProfileTransportConfiguration())) {
                sessionContext.setProfileTransportConfiguration(newProfileTransportConfiguration);
                sessionContext.setDevice(device);
                sessionContext.initializeTarget(newProfileTransportConfiguration, newDeviceTransportConfiguration);
                snmpTransportService.cancelQueryingTasks(sessionContext);
                snmpTransportService.createQueryingTasks(sessionContext);
                transportService.lifecycleEvent(sessionContext.getTenantId(), sessionContext.getDeviceId(), ComponentLifecycleEvent.UPDATED, true, null);
            } else if (!newDeviceTransportConfiguration.equals(sessionContext.getDeviceTransportConfiguration())) {
                sessionContext.setDeviceTransportConfiguration(newDeviceTransportConfiguration);
                sessionContext.setDevice(device);
                sessionContext.initializeTarget(newProfileTransportConfiguration, newDeviceTransportConfiguration);
                transportService.lifecycleEvent(sessionContext.getTenantId(), sessionContext.getDeviceId(), ComponentLifecycleEvent.UPDATED, true, null);
            } else {
                log.trace("Configuration of the device {} was not updated", device);
            }
        } catch (Exception e) {
            log.error("Failed to update session for SNMP device {}", sessionContext.getDeviceId(), e);
            transportService.lifecycleEvent(sessionContext.getTenantId(), sessionContext.getDeviceId(), ComponentLifecycleEvent.UPDATED, false, e);
            destroyDeviceSession(sessionContext);
        }
    }

    private void destroyDeviceSession(DeviceSessionContext sessionContext) {
        if (sessionContext == null) return;
        log.info("Destroying SNMP device session for device {}", sessionContext.getDevice().getId());
        sessionContext.close();
        snmpAuthService.cleanUpSnmpAuthInfo(sessionContext);
        transportService.deregisterSession(sessionContext.getSessionInfo());
        snmpTransportService.cancelQueryingTasks(sessionContext);
        sessions.remove(sessionContext.getDeviceId());
        transportService.lifecycleEvent(sessionContext.getTenantId(), sessionContext.getDeviceId(), ComponentLifecycleEvent.STOPPED, true, null);
        log.trace("Unregistered and removed session");
    }

    private void registerSessionMsgListener(DeviceSessionContext sessionContext) {
        transportService.process(DeviceTransportType.SNMP,
                TransportProtos.ValidateDeviceTokenRequestMsg.newBuilder().setToken(sessionContext.getToken()).build(),
                new TransportServiceCallback<>() {
                    @Override
                    public void onSuccess(ValidateDeviceCredentialsResponse msg) {
                        if (msg.hasDeviceInfo()) {
                            registerTransportSession(sessionContext, msg);
                            sessionContext.setSessionTimeoutHandler(() -> {
                                registerTransportSession(sessionContext, msg);
                            });
                            transportService.lifecycleEvent(sessionContext.getTenantId(), sessionContext.getDeviceId(), ComponentLifecycleEvent.STARTED, true, null);
                        } else {
                            log.warn("[{}] Failed to process device auth", sessionContext.getDeviceId());
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        log.warn("[{}] Failed to process device auth: {}", sessionContext.getDeviceId(), e);
                        transportService.lifecycleEvent(sessionContext.getTenantId(), sessionContext.getDeviceId(), ComponentLifecycleEvent.STARTED, false, e);
                    }
                });
    }

    private void registerTransportSession(DeviceSessionContext deviceSessionContext, ValidateDeviceCredentialsResponse msg) {
        SessionInfoProto sessionInfo = SessionInfoCreator.create(
                msg, SnmpTransportContext.this, UUID.randomUUID()
        );
        log.debug("Registering transport session: {}", sessionInfo);

        transportService.registerAsyncSession(sessionInfo, deviceSessionContext);
        transportService.process(sessionInfo, TransportProtos.SubscribeToAttributeUpdatesMsg.newBuilder()
                .setSessionType(TransportProtos.SessionType.ASYNC)
                .build(), TransportServiceCallback.EMPTY);
        transportService.process(sessionInfo, TransportProtos.SubscribeToRPCMsg.newBuilder()
                .setSessionType(TransportProtos.SessionType.ASYNC)
                .build(), TransportServiceCallback.EMPTY);

        deviceSessionContext.setSessionInfo(sessionInfo);
        deviceSessionContext.setDeviceInfo(msg.getDeviceInfo());
        deviceSessionContext.setConnected(true);
    }

    @EventListener(DeviceUpdatedEvent.class)
    public void onDeviceUpdatedOrCreated(DeviceUpdatedEvent deviceUpdatedEvent) {
        Device device = deviceUpdatedEvent.getDevice();
        log.debug("Got creating or updating device event for device {}", device);
        DeviceTransportType transportType = Optional.ofNullable(device.getDeviceData().getTransportConfiguration())
                .map(DeviceTransportConfiguration::getType)
                .orElse(null);
        if (!allSnmpDevicesIds.contains(device.getId())) {
            if (transportType != DeviceTransportType.SNMP) {
                return;
            }
            allSnmpDevicesIds.add(device.getId());
            if (balancingService.isManagedByCurrentTransport(device.getId().getId())) {
                establishDeviceSession(device);
            }
        } else {
            if (balancingService.isManagedByCurrentTransport(device.getId().getId())) {
                DeviceSessionContext sessionContext = sessions.get(device.getId());
                if (transportType == DeviceTransportType.SNMP) {
                    if (sessionContext != null) {
                        updateDeviceSession(sessionContext, device, deviceProfileCache.get(device.getDeviceProfileId()));
                    } else {
                        establishDeviceSession(device);
                    }
                } else {
                    log.trace("Transport type was changed to {}", transportType);
                    destroyDeviceSession(sessionContext);
                }
            }
        }
    }

    public void onDeviceDeleted(DeviceSessionContext sessionContext) {
        destroyDeviceSession(sessionContext);
    }

    public void onDeviceProfileUpdated(DeviceProfile deviceProfile, DeviceSessionContext sessionContext) {
        log.debug("Handling device profile {} update event for device {}", deviceProfile.getId(), sessionContext.getDeviceId());
        updateDeviceSession(sessionContext, sessionContext.getDevice(), deviceProfile);
    }

    public void onSnmpTransportListChanged() {
        log.trace("SNMP transport list changed. Updating sessions");
        List<DeviceId> deleted = new LinkedList<>();
        for (DeviceId deviceId : allSnmpDevicesIds) {
            if (balancingService.isManagedByCurrentTransport(deviceId.getId())) {
                if (!sessions.containsKey(deviceId)) {
                    Device device = protoEntityService.getDeviceById(deviceId);
                    if (device != null) {
                        log.info("SNMP device {} is now managed by current transport node", deviceId);
                        establishDeviceSession(device);
                    } else {
                        deleted.add(deviceId);
                    }
                }
            } else {
                Optional.ofNullable(sessions.get(deviceId))
                        .ifPresent(sessionContext -> {
                            log.info("SNMP session for device {} is not managed by current transport node anymore", deviceId);
                            destroyDeviceSession(sessionContext);
                        });
            }
        }
        log.trace("Removing deleted SNMP devices: {}", deleted);
        allSnmpDevicesIds.removeAll(deleted);
    }


    public Collection<DeviceSessionContext> getSessions() {
        return sessions.values();
    }

}
