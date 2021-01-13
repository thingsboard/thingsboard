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

import lombok.extern.slf4j.Slf4j;
import org.snmp4j.Snmp;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.DeviceInfo;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.device.profile.SnmpDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.tenant.TenantService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service("SnmpTransportService")
@ConditionalOnExpression("'${service.type:null}'=='tb-transport' || ('${service.type:null}'=='monolith' && '${transport.api_enabled:true}'=='true' && '${transport.snmp.enabled}'=='true')")
@Slf4j
public class SnmpTransportService {

    private static final int ENTITY_PACK_LIMIT = 1024;

    @Autowired
    private SnmpTransportContext snmpTransportContext;

    @Autowired
    DeviceProfileService deviceProfileService;

    @Autowired
    TenantService tenantService;

    @Autowired
    DeviceService deviceService;

    private Snmp snmp;

    private ExecutorService snmpCallbackExecutor;

    @PostConstruct
    public void init() {
        log.info("Starting SNMP transport...");
        this.snmpCallbackExecutor = Executors.newWorkStealingPool(20);
        initializeSnmp();
        log.info("SNMP transport started!");
    }

    @PreDestroy
    public void shutdown() {
        log.info("Stopping SNMP transport!");
        if (snmpCallbackExecutor != null) {
            snmpCallbackExecutor.shutdownNow();
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
        //TODO: This approach works for monolith, in cluster the same data will be fetched by each node.
        for (Tenant tenant : new PageDataIterable<>(tenantService::findTenants, ENTITY_PACK_LIMIT)) {
            TenantId tenantId = tenant.getTenantId();
            for (DeviceProfile deviceProfile : new PageDataIterable<>(pageLink -> deviceProfileService.findDeviceProfiles(tenantId, pageLink), ENTITY_PACK_LIMIT)) {
                if (DeviceTransportType.SNMP.equals(deviceProfile.getTransportType())) {
                    snmpTransportContext.getDeviceProfileTransportConfig().put(deviceProfile.getId(),
                            (SnmpDeviceProfileTransportConfiguration) deviceProfile.getProfileData().getTransportConfiguration());
                    initDeviceSessions(deviceProfile);
                }
            }
        }
        snmpTransportContext.initPduListPerProfile();
    }

    private void initDeviceSessions(DeviceProfile deviceProfile) {
        for (DeviceInfo deviceInfo : new PageDataIterable<>(pageLink -> deviceService.findDeviceInfosByTenantIdAndDeviceProfileId(deviceProfile.getTenantId(), deviceProfile.getId(), pageLink), ENTITY_PACK_LIMIT)) {
            snmpTransportContext.updateDeviceSessionCtx(deviceInfo, deviceProfile, snmp);
        }
    }
}
