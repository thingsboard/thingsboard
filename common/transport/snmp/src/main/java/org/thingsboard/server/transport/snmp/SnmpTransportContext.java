/**
 * Copyright © 2016-2021 The Thingsboard Authors
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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.snmp4j.PDU;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.VariableBinding;
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
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.common.data.transport.snmp.SnmpMapping;
import org.thingsboard.server.common.data.transport.snmp.SnmpMethod;
import org.thingsboard.server.common.transport.DeviceUpdatedEvent;
import org.thingsboard.server.common.transport.TransportContext;
import org.thingsboard.server.common.transport.TransportDeviceProfileCache;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.common.transport.auth.SessionInfoCreator;
import org.thingsboard.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.thingsboard.server.common.transport.session.DeviceAwareSessionContext;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.SessionInfoProto;
import org.thingsboard.server.queue.util.AfterStartUp;
import org.thingsboard.server.queue.util.TbSnmpTransportComponent;
import org.thingsboard.server.transport.snmp.service.ProtoTransportEntityService;
import org.thingsboard.server.transport.snmp.service.SnmpTransportBalancingService;
import org.thingsboard.server.transport.snmp.service.SnmpTransportService;
import org.thingsboard.server.transport.snmp.session.DeviceSessionContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

@TbSnmpTransportComponent
@Component
@Slf4j
@RequiredArgsConstructor
public class SnmpTransportContext extends TransportContext {
    private final SnmpTransportService snmpTransportService;
    private final TransportDeviceProfileCache deviceProfileCache;
    private final TransportService transportService;
    private final ProtoTransportEntityService protoEntityService;
    private final SnmpTransportBalancingService balancingService;

    private final Map<DeviceId, DeviceSessionContext> sessions = new ConcurrentHashMap<>();
    private final Map<DeviceProfileId, SnmpDeviceProfileTransportConfiguration> profilesTransportConfigs = new ConcurrentHashMap<>();
    private final Map<DeviceProfileId, List<PDU>> profilesPdus = new ConcurrentHashMap<>();
    private Collection<DeviceId> allSnmpDevicesIds = new ConcurrentLinkedDeque<>();

    @AfterStartUp(order = 2)
    public void initDevicesSessions() {
        log.info("Initializing SNMP devices sessions");
        allSnmpDevicesIds = protoEntityService.getAllSnmpDevicesIds().stream()
                .map(DeviceId::new)
                .collect(Collectors.toList());
        log.trace("Found all SNMP devices ids: {}", allSnmpDevicesIds);

        List<DeviceId> managedDevicesIds = allSnmpDevicesIds.stream()
                .filter(deviceId -> balancingService.isManagedByCurrentTransport(deviceId.getId()))
                .collect(Collectors.toList());
        log.info("SNMP devices managed by current SNMP transport: {}", managedDevicesIds);

        managedDevicesIds.stream()
                .map(protoEntityService::getDeviceById)
                .collect(Collectors.toList())
                .forEach(device -> {
                    try {
                        establishDeviceSession(device);
                    } catch (Exception e) {
                        log.error("Failed to establish session for SNMP device {}: {}", device.getId(), e.getMessage());
                    }
                });
    }

    private void establishDeviceSession(Device device) {
        if (device == null) return;
        log.info("Establishing SNMP device session for device {}", device.getId());

        DeviceProfileId deviceProfileId = device.getDeviceProfileId();
        DeviceProfile deviceProfile = deviceProfileCache.get(deviceProfileId);

        DeviceCredentials credentials = protoEntityService.getDeviceCredentialsByDeviceId(device.getId());
        if (credentials.getCredentialsType() != DeviceCredentialsType.ACCESS_TOKEN) {
            log.warn("[{}] Expected credentials type is {} but found {}", device.getId(), DeviceCredentialsType.ACCESS_TOKEN, credentials.getCredentialsType());
            return;
        }

        SnmpDeviceProfileTransportConfiguration profileTransportConfiguration = (SnmpDeviceProfileTransportConfiguration) deviceProfile.getProfileData().getTransportConfiguration();
        SnmpDeviceTransportConfiguration deviceTransportConfiguration = (SnmpDeviceTransportConfiguration) device.getDeviceData().getTransportConfiguration();

        profilesTransportConfigs.put(deviceProfileId, profileTransportConfiguration);
        profilesPdus.computeIfAbsent(deviceProfileId, id -> createPdus(profileTransportConfiguration));

        DeviceSessionContext deviceSessionContext = new DeviceSessionContext(
                device, deviceProfile,
                credentials.getCredentialsId(), deviceTransportConfiguration,
                this, snmpTransportService
        );
        registerSessionMsgListener(deviceSessionContext);
        sessions.put(device.getId(), deviceSessionContext);
        log.info("Established SNMP device session for device {}", device.getId());
    }

    private void updateDeviceSession(DeviceSessionContext sessionContext, Device device, DeviceProfile deviceProfile) {
        log.info("Updating SNMP device session for device {}", device.getId());
        DeviceProfileId deviceProfileId = deviceProfile.getId();

        DeviceCredentials credentials = protoEntityService.getDeviceCredentialsByDeviceId(device.getId());
        if (credentials.getCredentialsType() != DeviceCredentialsType.ACCESS_TOKEN) {
            log.warn("[{}] Expected credentials type is {} but found {}", device.getId(), DeviceCredentialsType.ACCESS_TOKEN, credentials.getCredentialsType());
            destroyDeviceSession(sessionContext);
            return;
        }

        SnmpDeviceProfileTransportConfiguration newProfileTransportConfiguration = (SnmpDeviceProfileTransportConfiguration) deviceProfile.getProfileData().getTransportConfiguration();
        SnmpDeviceTransportConfiguration newDeviceTransportConfiguration = (SnmpDeviceTransportConfiguration) device.getDeviceData().getTransportConfiguration();

        if (!newProfileTransportConfiguration.equals(sessionContext.getProfileTransportConfiguration())) {
            profilesPdus.put(deviceProfileId, createPdus(newProfileTransportConfiguration));
            profilesTransportConfigs.put(deviceProfileId, newProfileTransportConfiguration);

            sessionContext.setProfileTransportConfiguration(newProfileTransportConfiguration);
            sessionContext.initTarget(newProfileTransportConfiguration, newDeviceTransportConfiguration);
        } else if (!newDeviceTransportConfiguration.equals(sessionContext.getDeviceTransportConfiguration())) {
            sessionContext.setDeviceTransportConfiguration(newDeviceTransportConfiguration);
            sessionContext.initTarget(newProfileTransportConfiguration, newDeviceTransportConfiguration);
        } else {
            log.trace("Configuration of the device {} was not updated", device);
        }
    }

    private void destroyDeviceSession(DeviceSessionContext sessionContext) {
        if (sessionContext == null) return;
        log.info("Destroying SNMP device session for device {}", sessionContext.getDevice().getId());
        sessionContext.close();
        transportService.deregisterSession(sessionContext.getSessionInfo());
        sessions.remove(sessionContext.getDeviceId());
        log.trace("Deregistered and removed session");

        DeviceProfileId deviceProfileId = sessionContext.getDeviceProfile().getId();
        if (sessions.values().stream()
                .map(DeviceAwareSessionContext::getDeviceProfile)
                .noneMatch(deviceProfile -> deviceProfile.getId().equals(deviceProfileId))) {
            log.trace("Removed values for device profile {} from configs and pdus caches", deviceProfileId);
            profilesTransportConfigs.remove(deviceProfileId);
            profilesPdus.remove(deviceProfileId);
        }
    }

    private void registerSessionMsgListener(DeviceSessionContext deviceSessionContext) {
        transportService.process(DeviceTransportType.SNMP,
                TransportProtos.ValidateDeviceTokenRequestMsg.newBuilder().setToken(deviceSessionContext.getToken()).build(),
                new TransportServiceCallback<ValidateDeviceCredentialsResponse>() {
                    @Override
                    public void onSuccess(ValidateDeviceCredentialsResponse msg) {
                        if (msg.hasDeviceInfo()) {
                            SessionInfoProto sessionInfo = SessionInfoCreator.create(
                                    msg, SnmpTransportContext.this, UUID.randomUUID()
                            );

                            transportService.registerAsyncSession(sessionInfo, deviceSessionContext);
                            deviceSessionContext.setSessionInfo(sessionInfo);
                            deviceSessionContext.setDeviceInfo(msg.getDeviceInfo());
                        } else {
                            log.warn("[{}] Failed to process device auth", deviceSessionContext.getDeviceId());
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        log.warn("[{}] Failed to process device auth: {}", deviceSessionContext.getDeviceId(), e);
                    }
                });
    }

    private List<PDU> createPdus(SnmpDeviceProfileTransportConfiguration deviceProfileConfig) {
        Map<SnmpMethod, List<VariableBinding>> bindingsPerMethod = new HashMap<>();

        deviceProfileConfig.getAllMappings().forEach(mapping -> bindingsPerMethod
                .computeIfAbsent(mapping.getMethod(), v -> new ArrayList<>())
                .add(new VariableBinding(new OID(mapping.getOid()))));

        return bindingsPerMethod.keySet().stream()
                .map(method -> {
                    PDU request = new PDU();
                    request.setType(method.getCode());
                    request.addAll(bindingsPerMethod.get(method));
                    return request;
                })
                .collect(Collectors.toList());
    }

    @EventListener(DeviceUpdatedEvent.class)
    public void onDeviceUpdatedOrCreated(DeviceUpdatedEvent deviceUpdatedEvent) {
        Device device = deviceUpdatedEvent.getDevice();
        log.trace("Got creating or updating device event for device {}", device);
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

    public Map<DeviceProfileId, List<PDU>> getProfilesPdus() {
        return profilesPdus;
    }

    public Optional<SnmpMapping> getAttributeMapping(DeviceProfileId deviceProfileId, OID responseOid) {
        if (profilesTransportConfigs.containsKey(deviceProfileId)) {
            return getMapping(responseOid, profilesTransportConfigs.get(deviceProfileId).getAttributesMappings());
        }
        return Optional.empty();
    }

    public Optional<SnmpMapping> getTelemetryMapping(DeviceProfileId deviceProfileId, OID responseOid) {
        if (profilesTransportConfigs.containsKey(deviceProfileId)) {
            return getMapping(responseOid, profilesTransportConfigs.get(deviceProfileId).getTelemetryMappings());
        }
        return Optional.empty();
    }

    private Optional<SnmpMapping> getMapping(OID oid, List<SnmpMapping> mappings) {
        return mappings.stream()
                .filter(mapping -> new OID(mapping.getOid()).equals(oid))
                .findFirst();
    }
}
