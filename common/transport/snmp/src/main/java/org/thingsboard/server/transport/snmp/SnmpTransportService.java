/**
 * Copyright © 2016-2020 The Thingsboard Authors
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

import lombok.extern.slf4j.Slf4j;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.DeviceInfo;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.device.data.SnmpDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.SnmpDeviceProfileKvMapping;
import org.thingsboard.server.common.data.device.profile.SnmpDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.transport.snmp.session.DeviceSessionCtx;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service("SnmpTransportService")
@ConditionalOnExpression("'${service.type:null}'=='tb-transport' || ('${service.type:null}'=='monolith' && '${transport.api_enabled:true}'=='true' && '${transport.snmp.enabled}'=='true')")
@Slf4j
public class SnmpTransportService {

    private static final int ENTITY_PACK_LIMIT = 1024;

    @Autowired
    private SnmpTransportContext snmpTransportContext;

    @Autowired
    TransportService transportService;

    @Autowired
    DeviceProfileService deviceProfileService;

    @Autowired
    TenantService tenantService;

    @Autowired
    DeviceService deviceService;

    @Autowired
    DeviceCredentialsService deviceCredentialsService;

    private Snmp snmp;
    private ScheduledExecutorService schedulerExecutor;

    //TODO: PDU list should be updated on every device profile event
    private Map<DeviceProfileId, List<PDU>> pduPerProfile;
    private Map<DeviceId, DeviceSessionCtx> deviceSessions;

    @PostConstruct
    public void init() {
        log.info("Starting SNMP transport...");
        initializeSnmp();
        log.info("SNMP transport started!");
    }

    @PreDestroy
    public void shutdown() {
        log.info("Stopping SNMP transport!");
        if (schedulerExecutor != null) {
            schedulerExecutor.shutdownNow();
        }
        if (snmp != null) {
            try {
                snmp.close();
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
        log.info("SNMP transport stopped!");
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(value = 2)
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        log.info("Received application ready event. Starting SNMP polling.");
        initSessionCtxList();
    }

    private void initializeSnmp() {
        try {
            this.snmp = new Snmp(new DefaultUdpTransportMapping());
            this.snmp.listen();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    private void initSessionCtxList() {

        deviceSessions = new ConcurrentHashMap<>();

        for (Tenant tenant : new PageDataIterable<>(tenantService::findTenants, ENTITY_PACK_LIMIT)) {
            TenantId tenantId = tenant.getTenantId();
            PageDataIterable<DeviceProfile> deviceProfilesIterator = new PageDataIterable<>(pageLink -> deviceProfileService.findDeviceProfiles(tenantId, pageLink), ENTITY_PACK_LIMIT);
            for (DeviceProfile deviceProfile : deviceProfilesIterator) {
                if (DeviceTransportType.SNMP.equals(deviceProfile.getTransportType())) {
                    SnmpDeviceProfileTransportConfiguration snmpDeviceProfileTransportConfiguration = (SnmpDeviceProfileTransportConfiguration) deviceProfile.getProfileData().getTransportConfiguration();
                    snmpTransportContext.getDeviceProfileTransportConfig().put(deviceProfile.getId(), snmpDeviceProfileTransportConfiguration);
                    createDeviceSession(tenantId, deviceProfile, snmpDeviceProfileTransportConfiguration, deviceSessions);
                }
            }
        }

        pduPerProfile = new ConcurrentHashMap<>();
        snmpTransportContext.getDeviceProfileTransportConfig().forEach((id, config) -> pduPerProfile.put(id, getPduList(config)));

        this.schedulerExecutor = Executors.newSingleThreadScheduledExecutor(ThingsBoardThreadFactory.forName("snmp-pooling-scheduler"));
        this.schedulerExecutor.scheduleAtFixedRate(() -> executeSnmp(deviceSessions), 1000, 5000, TimeUnit.MILLISECONDS);
    }

    private void createDeviceSession(TenantId tenantId, DeviceProfile deviceProfile, SnmpDeviceProfileTransportConfiguration snmpDeviceProfileTransportConfiguration, Map<DeviceId, DeviceSessionCtx> sessions) {
        PageDataIterable<DeviceInfo> deviceInfosIterator = new PageDataIterable<>(pageLink -> deviceService.findDeviceInfosByTenantIdAndDeviceProfileId(tenantId, deviceProfile.getId(), pageLink), ENTITY_PACK_LIMIT);
        for (DeviceInfo deviceInfo : deviceInfosIterator) {
            DeviceCredentials credentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, deviceInfo.getId());
            if (DeviceCredentialsType.ACCESS_TOKEN.equals(credentials.getCredentialsType())) {
                SnmpDeviceTransportConfiguration snmpDeviceTransportConfiguration = (SnmpDeviceTransportConfiguration) deviceInfo.getDeviceData().getTransportConfiguration();
                if (snmpDeviceTransportConfiguration.isValid()) {
                    DeviceSessionCtx deviceSessionCtx = new DeviceSessionCtx(UUID.randomUUID(), snmpTransportContext, credentials.getCredentialsId());
                    deviceSessionCtx.setDeviceId(deviceInfo.getId());
                    deviceSessionCtx.setTransportConfiguration(snmpDeviceTransportConfiguration);
                    deviceSessionCtx.initTarget(snmpDeviceProfileTransportConfiguration);
                    deviceSessionCtx.createSessionInfo(ctx -> transportService.registerAsyncSession(deviceSessionCtx.getSessionInfo(), deviceSessionCtx));
                    deviceSessionCtx.setDeviceProfile(deviceProfile);

                    sessions.put(deviceInfo.getId(), deviceSessionCtx);
                }
            } else {
                log.warn("[] Expected credentials type is {} but found {}", DeviceCredentialsType.ACCESS_TOKEN, credentials.getCredentialsType());
            }
        }
    }

    private void executeSnmp(Map<DeviceId, DeviceSessionCtx> sessions) {
        sessions.forEach((deviceId, deviceSessionCtx) ->
                pduPerProfile.get(deviceSessionCtx.getDeviceProfile().getId()).forEach(pdu -> {
                    try {
                        log.info("[{}] Sending SNMP message...", pdu.getRequestID());
                        this.snmp.send(pdu,
                                deviceSessionCtx.getTarget(),
                                deviceSessionCtx.getDeviceProfile().getId(),
                                deviceSessionCtx.getSnmpSessionListener());
                    } catch (IOException e) {
                        log.error(e.getMessage(), e);
                    }
                }));
    }

    private List<PDU> getPduList(SnmpDeviceProfileTransportConfiguration deviceProfileConfig) {
        Map<String, List<VariableBinding>> varBindingPerMethod = new HashMap<>();

        Consumer<SnmpDeviceProfileKvMapping> varBindingPerMethodConsumer = mapping -> varBindingPerMethod
                .computeIfAbsent(mapping.getMethod(), v -> new ArrayList<>())
                .add(new VariableBinding(new OID(mapping.getOid())));

        deviceProfileConfig.getKvMappings().forEach(varBindingPerMethodConsumer);

        return varBindingPerMethod.keySet().stream()
                .map(method -> {
                    PDU request = new PDU();
                    request.setType(getSnmpMethod(method));
                    request.addAll(varBindingPerMethod.get(method));
                    return request;
                })
                .collect(Collectors.toList());
    }

    //TODO: temp implementation
    private int getSnmpMethod(String configMethod) {
        switch (configMethod) {
            case "get":
                return PDU.GET;
            default:
                return -1;
        }
    }
}
