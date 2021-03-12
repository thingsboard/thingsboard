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
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
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
import org.thingsboard.server.common.transport.TransportDeviceProfileCache;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.common.transport.auth.SessionInfoCreator;
import org.thingsboard.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.SessionInfoProto;
import org.thingsboard.server.transport.snmp.service.ProtoTransportEntityService;
import org.thingsboard.server.transport.snmp.service.SnmpTransportBalancingService;
import org.thingsboard.server.transport.snmp.service.SnmpTransportService;
import org.thingsboard.server.transport.snmp.session.DeviceSessionContext;

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
    private final TransportService transportService;
    private final ProtoTransportEntityService protoEntityService;
    private final SnmpTransportBalancingService balancingService;
    private final ApplicationEventPublisher eventPublisher;

    private final Map<DeviceId, DeviceSessionContext> devicesSessions = new ConcurrentHashMap<>();
    private final Map<DeviceProfileId, SnmpProfileTransportConfiguration> profilesTransportConfigs = new ConcurrentHashMap<>();
    private final Map<DeviceProfileId, List<PDU>> profilesPdus = new ConcurrentHashMap<>();

    @EventListener(ApplicationReadyEvent.class)
    @Order(2)
    public void initDevicesSessions() {
        List<Device> managedDevices = protoEntityService.getAllSnmpDevicesIds().stream()
                .filter(balancingService::isManagedByCurrentTransport)
                .map(DeviceId::new)
                .map(protoEntityService::getDeviceById)
                .collect(Collectors.toList());

//        managedDevices.forEach(this::establishDeviceSession);
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
        DeviceCredentials credentials = protoEntityService.getDeviceCredentialsByDeviceId(device.getId());

        if (credentials.getCredentialsType() == DeviceCredentialsType.ACCESS_TOKEN) {
            SnmpDeviceTransportConfiguration snmpDeviceTransportConfiguration = (SnmpDeviceTransportConfiguration) device.getDeviceData().getTransportConfiguration();
            if (snmpDeviceTransportConfiguration.isValid()) {
                DeviceSessionContext deviceSessionContext = new DeviceSessionContext(
                        device.getId(), deviceProfile,
                         credentials.getCredentialsId(), snmpDeviceTransportConfiguration,
                        this, snmpTransportService
                );
                registerDeviceSession(deviceSessionContext);
                devicesSessions.put(device.getId(), deviceSessionContext);
            } else {
                // do smth
            }
        } else {
            log.warn("[{}] Expected credentials type is {} but found {}", device.getId(), DeviceCredentialsType.ACCESS_TOKEN, credentials.getCredentialsType());
        }
    }

    // FIXME: will be executed for each device with this profile ?
    public void onDeviceProfileUpdated(DeviceProfile deviceProfile, DeviceSessionContext deviceSessionContext) {
        if (deviceProfile.getTransportType() == DeviceTransportType.SNMP) {
            SnmpProfileTransportConfiguration transportConfiguration = (SnmpProfileTransportConfiguration) deviceProfile.getProfileData().getTransportConfiguration();
            profilesTransportConfigs.put(deviceProfile.getId(), transportConfiguration);
            profilesPdus.put(deviceProfile.getId(), createPdus(transportConfiguration));
        } else {
            deviceSessionContext.close();
            transportService.deregisterSession(deviceSessionContext.getSessionInfo());
            devicesSessions.remove(deviceSessionContext.getDeviceId());

            profilesTransportConfigs.remove(deviceProfile.getId());
            profilesPdus.remove(deviceProfile.getId());
        }
    }

    // FIXME: what if device profile transport type will be SNMP but device transport config will not be of SNMP type?
    public void onDeviceUpdated(Device device, DeviceProfile deviceProfile) {
        if (deviceProfile != null && deviceProfile.getTransportType() == DeviceTransportType.SNMP &&
        device.getDeviceData() != null && device.getDeviceData().get) {
            updateDeviceSessionContext(device, deviceProfile, null);

            SnmpProfileTransportConfiguration profileTransportConfig = (SnmpProfileTransportConfiguration) deviceProfile.getProfileData().getTransportConfiguration();
            SnmpDeviceTransportConfiguration deviceTransportConfig = (SnmpDeviceTransportConfiguration) device.getDeviceData().getTransportConfiguration();
            devicesSessions.get(device.getId()).initTarget(profileTransportConfig, deviceTransportConfig);
        } else {

            //TODO: should the context be removed from the map?
        }
    }

    private void registerDeviceSession(DeviceSessionContext deviceSessionContext) {
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
                        log.warn("[{}] Failed to process device auth", deviceSessionContext.getDeviceId(), e);
                    }
                });
    }

    public Collection<DeviceSessionContext> getDevicesSessions() {
        return devicesSessions.values();
    }

    public Map<DeviceProfileId, List<PDU>> getProfilesPdus() {
        return profilesPdus;
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
