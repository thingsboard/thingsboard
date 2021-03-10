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
import lombok.extern.slf4j.Slf4j;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.VariableBinding;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.TransportName;
import org.thingsboard.server.common.data.device.data.SnmpDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.SnmpDeviceProfileKvMapping;
import org.thingsboard.server.common.data.device.profile.SnmpProfileTransportConfiguration;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.common.transport.TransportContext;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.transport.snmp.session.DeviceSessionCtx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

@Service
@ConditionalOnExpression("'${service.type:null}'=='tb-transport' || ('${service.type:null}'=='monolith' && '${transport.api_enabled:true}'=='true' && '${transport.snmp.enabled}'=='true')")
@Slf4j
public class SnmpTransportContext extends TransportContext {
    @Autowired
    DeviceCredentialsService deviceCredentialsService;

    @Autowired
    @Lazy
    SnmpTransportService snmpTransportService;

    @Getter
    private final Map<DeviceProfileId, SnmpProfileTransportConfiguration> profileTransportConfig = new ConcurrentHashMap<>();
    @Getter
    private final Map<DeviceProfileId, List<PDU>> pdusPerProfile = new ConcurrentHashMap<>();
    @Getter
    private final Map<DeviceId, DeviceSessionCtx> deviceSessions = new ConcurrentHashMap<>();

    public Optional<SnmpDeviceProfileKvMapping> findAttributesMapping(DeviceProfileId deviceProfileId, OID responseOid) {
        if (profileTransportConfig.containsKey(deviceProfileId)) {
            return findMapping(responseOid, profileTransportConfig.get(deviceProfileId).getAttributes());
        }
        return Optional.empty();
    }

    public Optional<SnmpDeviceProfileKvMapping> findTelemetryMapping(DeviceProfileId deviceProfileId, OID responseOid) {
        if (profileTransportConfig.containsKey(deviceProfileId)) {
            return findMapping(responseOid, profileTransportConfig.get(deviceProfileId).getTelemetry());
        }
        return Optional.empty();
    }

    private Optional<SnmpDeviceProfileKvMapping> findMapping(OID responseOid, List<SnmpDeviceProfileKvMapping> mappings) {
        return mappings.stream()
                .filter(kvMapping -> new OID(kvMapping.getOid()).equals(responseOid))
                //TODO: OID shouldn't be duplicated in the config, add backend and UI verification
                .findFirst();
    }

    public void initPduListPerProfile() {
        profileTransportConfig.forEach(this::updatePduListPerProfile);
    }

    public void updatePduListPerProfile(DeviceProfileId id, SnmpProfileTransportConfiguration config) {
        pdusPerProfile.put(id, createPduList(config));
    }

    public void updateDeviceSessionCtx(Device device, DeviceProfile deviceProfile, Snmp snmp) {
        DeviceCredentials credentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(device.getTenantId(), device.getId());
        if (DeviceCredentialsType.ACCESS_TOKEN.equals(credentials.getCredentialsType())) {
            SnmpDeviceTransportConfiguration snmpDeviceTransportConfiguration = (SnmpDeviceTransportConfiguration) device.getDeviceData().getTransportConfiguration();
            if (snmpDeviceTransportConfiguration.isValid()) {
                DeviceSessionCtx deviceSessionCtx = new DeviceSessionCtx(this, credentials.getCredentialsId(), snmpDeviceTransportConfiguration, snmp, device.getId(), deviceProfile);
                deviceSessionCtx.createSessionInfo(ctx -> getTransportService().registerAsyncSession(deviceSessionCtx.getSessionInfo(), deviceSessionCtx));
                this.deviceSessions.put(device.getId(), deviceSessionCtx);
            }
        } else {
            log.warn("[{}] Expected credentials type is {} but found {}", device.getId(), DeviceCredentialsType.ACCESS_TOKEN, credentials.getCredentialsType());
        }
    }

    public ExecutorService getSnmpCallbackExecutor() {
        return snmpTransportService.getSnmpCallbackExecutor();
    }

    private List<PDU> createPduList(SnmpProfileTransportConfiguration deviceProfileConfig) {
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
