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
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.device.profile.SnmpDeviceProfileKvMapping;
import org.thingsboard.server.common.data.device.profile.SnmpDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.transport.snmp.session.DeviceSessionCtx;
import org.thingsboard.server.transport.snmp.temp.SnmpDeviceProfileTransportConfigFactory;
import org.thingsboard.server.transport.snmp.temp.SnmpDeviceTransportConfigFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service("SnmpTransportService")
@ConditionalOnExpression("'${service.type:null}'=='tb-transport' || ('${service.type:null}'=='monolith' && '${transport.api_enabled:true}'=='true' && '${transport.snmp.enabled}'=='true')")
@Slf4j
public class SnmpTransportService {

    @Autowired
    private SnmpTransportContext snmpTransportContext;

    private Snmp snmp;
    private ScheduledExecutorService schedulerExecutor;

    //TODO: PDU list should be updated on every device profile event
    private Map<DeviceProfileId, List<PDU>> pduPerProfile;
    private List<DeviceSessionCtx> deviceSessionCtxList;

    @PostConstruct
    public void init() {
        log.info("Starting SNMP transport...");

        initializeSnmp();

        DeviceProfileId deviceProfileId = new DeviceProfileId(UUID.fromString("7876dea0-1dbe-11eb-99f0-0719747fb6f9"));
        snmpTransportContext.getDeviceProfileTransportConfig().putAll(Collections.singletonMap(deviceProfileId, SnmpDeviceProfileTransportConfigFactory.getDeviceProfileTransportConfig()));

        deviceSessionCtxList = snmpTransportContext.getDeviceProfileTransportConfig().keySet().stream()
                .map(id -> {
                    DeviceProfile deviceProfile = new DeviceProfile(id);
                    DeviceSessionCtx deviceSessionCtx = new DeviceSessionCtx(UUID.randomUUID(), snmpTransportContext, "A2_TEST_TOKEN");
                    deviceSessionCtx.setDeviceId(new DeviceId(UUID.randomUUID()));
                    deviceSessionCtx.setDeviceProfile(deviceProfile);
                    deviceSessionCtx.setTransportConfiguration(SnmpDeviceTransportConfigFactory.getSnmpTransportConfig());
                    //TODO: re-init target on device transport configuration event
                    deviceSessionCtx.initTarget(snmpTransportContext.getDeviceProfileTransportConfig().get(id));
                    return deviceSessionCtx;
                })
                .collect(Collectors.toList());

        pduPerProfile = new HashMap<>();
        snmpTransportContext.getDeviceProfileTransportConfig().forEach((id, config) -> pduPerProfile.put(id, getPduList(config)));

        this.schedulerExecutor = Executors.newSingleThreadScheduledExecutor(ThingsBoardThreadFactory.forName("snmp-pooling-scheduler"));
        this.schedulerExecutor.scheduleAtFixedRate(this::executeSnmp, 5000, 5000, TimeUnit.MILLISECONDS);

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

    private void initializeSnmp() {
        try {
            this.snmp = new Snmp(new DefaultUdpTransportMapping());
            this.snmp.listen();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    private void executeSnmp() {
        deviceSessionCtxList.forEach(deviceSessionCtx ->
                pduPerProfile.get(deviceSessionCtx.getDeviceProfile().getId())
                        .forEach(pdu -> {
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
