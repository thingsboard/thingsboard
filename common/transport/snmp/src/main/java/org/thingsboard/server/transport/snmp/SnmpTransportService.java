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
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.Target;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardThreadFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service("SnmpTransportService")
@ConditionalOnExpression("'${service.type:null}'=='tb-transport' || ('${service.type:null}'=='monolith' && '${transport.api_enabled:true}'=='true' && '${transport.snmp.enabled}'=='true')")
@Slf4j
public class SnmpTransportService {

    private Target target;
    private Snmp snmp;
    private ScheduledExecutorService schedulerExecutor;

    @PostConstruct
    public void init() {
        log.info("Starting SNMP transport...");

        this.target = getCommunityTarget();
        initializeSnmp();
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

    private Target getCommunityTarget() {
        CommunityTarget communityTarget = new CommunityTarget();
        communityTarget.setVersion(SnmpConstants.version2c);
        communityTarget.setAddress(GenericAddress.parse(GenericAddress.TYPE_UDP + ":" + "192.168.1.131" + "/" + 161));
        communityTarget.setCommunity(new OctetString("test321"));
        communityTarget.setTimeout(5000);
        communityTarget.setRetries(5);
        return communityTarget;
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
        PDU request = new PDU();
        request.setType(PDU.GET);
        request.add(new VariableBinding(new OID("1.3.6.1.2.1.25.1.1.0")));

        try {
            PDU response = this.snmp.send(request, this.target).getResponse();
            for (int i = 0; i < response.size(); i++) {
                VariableBinding vb = response.get(i);
                log.info("SNMP response received: {}, {}", vb.getOid(), vb.getVariable());
            }

        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }
}
