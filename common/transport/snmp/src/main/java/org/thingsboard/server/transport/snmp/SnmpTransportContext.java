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
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.device.data.DeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.data.SnmpDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.SnmpDeviceProfileKvMapping;
import org.thingsboard.server.common.data.device.profile.SnmpProfileTransportConfiguration;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
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
import org.thingsboard.server.queue.util.TbSnmpTransportComponent;
import org.thingsboard.server.transport.snmp.event.SnmpTransportListChangedEvent;
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
import java.util.concurrent.ExecutorService;
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
    private final Map<DeviceProfileId, SnmpProfileTransportConfiguration> profilesTransportConfigs = new ConcurrentHashMap<>();
    private final Map<DeviceProfileId, List<PDU>> profilesPdus = new ConcurrentHashMap<>();
    private List<DeviceId> allSnmpDevicesIds = new LinkedList<>();

    @EventListener(ApplicationReadyEvent.class)
    @Order(2)
    public void initDevicesSessions() {
        allSnmpDevicesIds = protoEntityService.getAllSnmpDevicesIds().stream()
                .map(DeviceId::new)
                .collect(Collectors.toList());
        List<Device> managedDevices = allSnmpDevicesIds.stream()
                .filter(deviceId -> balancingService.isManagedByCurrentTransport(deviceId.getId()))
                .map(protoEntityService::getDeviceById)
                .collect(Collectors.toList());

        managedDevices.forEach(this::establishDeviceSession);
    }

    private void establishDeviceSession(Device device) {
        DeviceProfileId deviceProfileId = device.getDeviceProfileId();
        DeviceProfile deviceProfile = deviceProfileCache.get(deviceProfileId);

        DeviceCredentials credentials = protoEntityService.getDeviceCredentialsByDeviceId(device.getId());
        if (credentials.getCredentialsType() != DeviceCredentialsType.ACCESS_TOKEN) {
            log.warn("[{}] Expected credentials type is {} but found {}", device.getId(), DeviceCredentialsType.ACCESS_TOKEN, credentials.getCredentialsType());
            return;
        }

        SnmpDeviceTransportConfiguration deviceTransportConfiguration = (SnmpDeviceTransportConfiguration) device.getDeviceData().getTransportConfiguration();
        if (!deviceTransportConfiguration.isValid()) {
            log.warn("SNMP device transport configuration is not valid");
            return;
        }

        SnmpProfileTransportConfiguration profileTransportConfiguration = (SnmpProfileTransportConfiguration) deviceProfile.getProfileData().getTransportConfiguration();
        profilesPdus.computeIfAbsent(deviceProfileId, id -> createPdus(profileTransportConfiguration));

        DeviceSessionContext deviceSessionContext = new DeviceSessionContext(
                device, deviceProfile,
                credentials.getCredentialsId(), deviceTransportConfiguration,
                this, snmpTransportService
        );
        registerSessionMsgListener(deviceSessionContext);
        sessions.put(device.getId(), deviceSessionContext);
    }

    private void updateDeviceSession(DeviceSessionContext sessionContext, Device device, DeviceProfile deviceProfile) {
        DeviceProfileId deviceProfileId = deviceProfile.getId();

        DeviceCredentials credentials = protoEntityService.getDeviceCredentialsByDeviceId(device.getId());
        if (credentials.getCredentialsType() != DeviceCredentialsType.ACCESS_TOKEN) {
            log.warn("[{}] Expected credentials type is {} but found {}", device.getId(), DeviceCredentialsType.ACCESS_TOKEN, credentials.getCredentialsType());
            destroyDeviceSession(sessionContext);
            return;
        }

        SnmpProfileTransportConfiguration profileTransportConfiguration = (SnmpProfileTransportConfiguration) deviceProfile.getProfileData().getTransportConfiguration();
        SnmpDeviceTransportConfiguration deviceTransportConfiguration = (SnmpDeviceTransportConfiguration) device.getDeviceData().getTransportConfiguration();
        sessionContext.setProfileTransportConfiguration(profileTransportConfiguration);
        sessionContext.setDeviceTransportConfiguration(deviceTransportConfiguration);
        if (!deviceTransportConfiguration.isValid()) {
            destroyDeviceSession(sessionContext);
            return;
        }

        if (!profileTransportConfiguration.equals(profilesTransportConfigs.get(deviceProfileId))) {
            profilesPdus.put(deviceProfileId, createPdus(profileTransportConfiguration));
            profilesTransportConfigs.put(deviceProfileId, profileTransportConfiguration);
            sessionContext.initTarget(profileTransportConfiguration, deviceTransportConfiguration);
        } else if (!deviceTransportConfiguration.equals(sessionContext.getDeviceTransportConfiguration())) {
            sessionContext.initTarget(profileTransportConfiguration, deviceTransportConfiguration);
        }
    }

    private void destroyDeviceSession(DeviceSessionContext sessionContext) {
        if (sessionContext == null) return;
        sessionContext.close();
        transportService.deregisterSession(sessionContext.getSessionInfo());
        sessions.remove(sessionContext.getDeviceId());

        DeviceProfileId deviceProfileId = sessionContext.getDeviceProfile().getId();
        if (sessions.values().stream()
                .map(DeviceAwareSessionContext::getDeviceProfile)
                .noneMatch(deviceProfile -> deviceProfile.getId().equals(deviceProfileId))) {
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

    private List<PDU> createPdus(SnmpProfileTransportConfiguration deviceProfileConfig) {
        Map<String, List<VariableBinding>> varBindingPerMethod = new HashMap<>();

        deviceProfileConfig.getKvMappings().forEach(mapping -> varBindingPerMethod
                .computeIfAbsent(mapping.getMethod(), v -> new ArrayList<>())
                .add(new VariableBinding(new OID(mapping.getOid()))));

        return varBindingPerMethod.keySet().stream()
                .map(method -> {
                    PDU request = new PDU();
                    request.setType(getSnmpMethod(method));
                    request.addAll(varBindingPerMethod.get(method));
                    return request;
                })
                .collect(Collectors.toList());
    }

    @EventListener(DeviceUpdatedEvent.class)
    public void onDeviceUpdatedOrCreated(DeviceUpdatedEvent deviceUpdatedEvent) {
        Device device = deviceUpdatedEvent.getDevice();
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

    @EventListener(SnmpTransportListChangedEvent.class)
    public void onSnmpTransportListChanged() {
        for (DeviceId deviceId : allSnmpDevicesIds) {
            if (balancingService.isManagedByCurrentTransport(deviceId.getId())) {
                if (!sessions.containsKey(deviceId)) {
                    establishDeviceSession(protoEntityService.getDeviceById(deviceId));
                }
            } else {
                Optional.ofNullable(sessions.get(deviceId))
                        .ifPresent(this::destroyDeviceSession);
            }
        }
    }


    public Collection<DeviceSessionContext> getSessions() {
        return sessions.values();
    }

    public Map<DeviceProfileId, List<PDU>> getProfilesPdus() {
        return profilesPdus;
    }

    public Optional<SnmpDeviceProfileKvMapping> getAttributesMapping(DeviceProfileId deviceProfileId, OID responseOid) {
        if (profilesTransportConfigs.containsKey(deviceProfileId)) {
            return getMapping(responseOid, profilesTransportConfigs.get(deviceProfileId).getAttributes());
        }
        return Optional.empty();
    }

    public Optional<SnmpDeviceProfileKvMapping> getTelemetryMapping(DeviceProfileId deviceProfileId, OID responseOid) {
        if (profilesTransportConfigs.containsKey(deviceProfileId)) {
            return getMapping(responseOid, profilesTransportConfigs.get(deviceProfileId).getTelemetry());
        }
        return Optional.empty();
    }

    private Optional<SnmpDeviceProfileKvMapping> getMapping(OID responseOid, List<SnmpDeviceProfileKvMapping> mappings) {
        return mappings.stream()
                .filter(kvMapping -> new OID(kvMapping.getOid()).equals(responseOid))
                //TODO: OID shouldn't be duplicated in the config, add backend and UI verification
                .findFirst();
    }

    public ExecutorService getSnmpCallbackExecutor() {
        return snmpTransportService.getSnmpCallbackExecutor();
    }

    private int getSnmpMethod(String configMethod) {
        switch (configMethod) {
            case "get":
                return PDU.GET;
            case "getNext":
            case "response":
            case "set":
            default:
                return -1;
        }
    }
}
