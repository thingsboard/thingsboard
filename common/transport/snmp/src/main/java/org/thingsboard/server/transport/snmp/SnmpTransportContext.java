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

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.VariableBinding;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.device.data.SnmpDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.SnmpDeviceProfileKvMapping;
import org.thingsboard.server.common.data.device.profile.SnmpProfileTransportConfiguration;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.common.transport.TransportContext;
import org.thingsboard.server.common.transport.TransportDeviceCache;
import org.thingsboard.server.common.transport.TransportDeviceProfileCache;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.snmp.event.DeviceProfileUpdatedEvent;
import org.thingsboard.server.transport.snmp.event.DeviceUpdatedEvent;
import org.thingsboard.server.transport.snmp.session.DeviceSessionContext;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

@Component
@ConditionalOnExpression("'${service.type:null}'=='tb-transport' || ('${service.type:null}'=='monolith' && '${transport.api_enabled:true}'=='true' && '${transport.snmp.enabled}'=='true')")
@Slf4j
@RequiredArgsConstructor
public class SnmpTransportContext extends TransportContext {
    private final SnmpTransportService snmpTransportService;
    private final TransportDeviceProfileCache deviceProfileCache;
    private final TransportDeviceCache deviceCache;
    private final TransportService transportService;
    private final SnmpTransportBalancingService balancingService;
    private final ApplicationEventPublisher eventPublisher;

    @Getter
    private final Map<DeviceId, DeviceSessionContext> devicesSessions = new ConcurrentHashMap<>();
    @Getter
    private final Map<DeviceProfileId, SnmpProfileTransportConfiguration> profilesTransportConfigs = new ConcurrentHashMap<>();
    @Getter
    private final Map<DeviceProfileId, List<PDU>> profilesPdus = new ConcurrentHashMap<>();

    @PostConstruct
    private void initDevicesSessions() {
        TransportProtos.GetSnmpDevicesResponseMsg devicesIds = transportService.getSnmpDevicesIds(
                TransportProtos.GetSnmpDevicesRequestMsg.getDefaultInstance()
        );
        List<Device> managedDevices = devicesIds.getIdsList().stream()
                .map(UUID::fromString)
                .filter(balancingService::isManagedByCurrentTransport)
                .map(DeviceId::new)
                .map(deviceCache::get)
                .collect(Collectors.toList());

        managedDevices.forEach(this::establishDeviceSession);
    }

    private void establishDeviceSession(Device device) {
        DeviceProfileId deviceProfileId = device.getDeviceProfileId();
        DeviceProfile deviceProfile = deviceProfileCache.get(deviceProfileId);

        SnmpProfileTransportConfiguration snmpTransportConfig = (SnmpProfileTransportConfiguration) deviceProfile.getProfileData().getTransportConfiguration();

        profilesTransportConfigs.putIfAbsent(deviceProfileId, snmpTransportConfig);
        updateDeviceSessionContext(device, deviceProfile, snmpTransportService.getSnmp());
        profilesPdus.computeIfAbsent(deviceProfileId, id -> createPdus(snmpTransportConfig));
    }

    public void updateDeviceSessionContext(Device device, DeviceProfile deviceProfile, Snmp snmp) {
//        DeviceCredentials credentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(device.getTenantId(), device.getId());
        DeviceCredentials credentials = null;
        if (credentials.getCredentialsType() == DeviceCredentialsType.ACCESS_TOKEN) {
            SnmpDeviceTransportConfiguration snmpDeviceTransportConfiguration = (SnmpDeviceTransportConfiguration) device.getDeviceData().getTransportConfiguration();
            if (snmpDeviceTransportConfiguration.isValid()) {
                DeviceSessionContext deviceSessionContext = new DeviceSessionContext(
                        this, credentials.getCredentialsId(), snmpDeviceTransportConfiguration,
                        eventPublisher, snmp, device.getId(), deviceProfile
                );
                deviceSessionContext.createSessionInfo(ctx -> {
                    transportService.registerAsyncSession(deviceSessionContext.getSessionInfo(), deviceSessionContext);
                });
                devicesSessions.put(device.getId(), deviceSessionContext);
            }
        } else {
            log.warn("[{}] Expected credentials type is {} but found {}", device.getId(), DeviceCredentialsType.ACCESS_TOKEN, credentials.getCredentialsType());
        }
    }

    @EventListener(DeviceProfileUpdatedEvent.class)
    public void onDeviceProfileUpdated(DeviceProfileUpdatedEvent deviceProfileUpdatedEvent) {
        DeviceProfile deviceProfile = deviceProfileUpdatedEvent.getDeviceProfile();

        if (deviceProfile.getTransportType() == DeviceTransportType.SNMP) {
            SnmpProfileTransportConfiguration transportConfiguration = (SnmpProfileTransportConfiguration) deviceProfile.getProfileData().getTransportConfiguration();
            profilesTransportConfigs.put(deviceProfile.getId(), transportConfiguration);
            profilesPdus.put(deviceProfile.getId(), createPdus(transportConfiguration));
        } else {
            // destroy session
        }
    }

    @EventListener(DeviceUpdatedEvent.class)
    public void onDeviceUpdated(DeviceUpdatedEvent deviceUpdatedEvent) {
        Device device = deviceUpdatedEvent.getDevice();
        DeviceProfile deviceProfile = deviceUpdatedEvent.getDeviceProfile();

        if (deviceProfile != null && deviceProfile.getTransportType() == DeviceTransportType.SNMP) {
            updateDeviceSessionContext(device, deviceProfile, null);

            SnmpProfileTransportConfiguration profileTransportConfig = (SnmpProfileTransportConfiguration) deviceProfile.getProfileData().getTransportConfiguration();
            SnmpDeviceTransportConfiguration deviceTransportConfig = (SnmpDeviceTransportConfiguration) device.getDeviceData().getTransportConfiguration();
            devicesSessions.get(device.getId()).initTarget(profileTransportConfig, deviceTransportConfig);
        } else {
            //TODO: should the context be removed from the map?
        }
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

    public Collection<DeviceSessionContext> getDevicesSessions() {
        return devicesSessions.values();
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

    //TODO: Extract SNMP methods to enum
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
