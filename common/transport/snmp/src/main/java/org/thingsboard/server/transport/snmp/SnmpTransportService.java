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
import org.snmp4j.Snmp;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.transport.snmp.session.DeviceSessionContext;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service("SnmpTransportService")
@ConditionalOnExpression("'${service.type:null}'=='tb-transport' || ('${service.type:null}'=='monolith' && '${transport.api_enabled:true}'=='true' && '${transport.snmp.enabled}'=='true')")
@Slf4j
public class SnmpTransportService {
    private final SnmpTransportContext snmpTransportContext;

    @Getter
    private ExecutorService snmpCallbackExecutor;
    @Getter
    private Snmp snmp;
    private ScheduledExecutorService pollingExecutor;

    public SnmpTransportService(@Lazy SnmpTransportContext snmpTransportContext) {
        this.snmpTransportContext = snmpTransportContext;
    }

    @PostConstruct
    private void init() {
        log.info("Starting SNMP transport...");
        pollingExecutor = Executors.newScheduledThreadPool(1, ThingsBoardThreadFactory.forName("snmp-polling"));
        //TODO: Set parallelism value in the config
        snmpCallbackExecutor = Executors.newWorkStealingPool(20);
        initializeSnmp();
        log.info("SNMP transport started!");
    }

    @PreDestroy
    public void shutdown() {
        log.info("Stopping SNMP transport!");
        if (pollingExecutor != null) {
            pollingExecutor.shutdownNow();
        }
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
    @Order(value = 10)
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        log.info("Received application ready event. Starting SNMP polling.");
        startPolling();
    }

    private void initializeSnmp() {
        try {
            this.snmp = new Snmp(new DefaultUdpTransportMapping());
            this.snmp.listen();
        } catch (IOException e) {
            //TODO: what should be done if transport wasn't initialized?
            log.error(e.getMessage(), e);
        }
    }

    private void startPolling() {
        //TODO: Get poll period from configuration;
        int pollPeriodSeconds = 1;

        pollingExecutor.scheduleAtFixedRate(() -> {
            snmpTransportContext.getDevicesSessions()
                    .forEach(DeviceSessionContext::executeSnmpRequest);
        }, 0, pollPeriodSeconds, TimeUnit.SECONDS);
    }
}
