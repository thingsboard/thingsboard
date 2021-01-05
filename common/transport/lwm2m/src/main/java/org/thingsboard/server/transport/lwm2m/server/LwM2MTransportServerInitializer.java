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
package org.thingsboard.server.transport.lwm2m.server;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.server.californium.LeshanServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.thingsboard.server.transport.lwm2m.secure.LWM2MGenerationPSkRPkECC;
import org.thingsboard.server.transport.lwm2m.secure.LwM2MSecurityMode;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Slf4j
@Component("LwM2MTransportServerInitializer")
@ConditionalOnExpression("('${service.type:null}'=='tb-transport' && '${transport.lwm2m.enabled:false}'=='true' ) || ('${service.type:null}'=='monolith' && '${transport.lwm2m.enabled}'=='true')")
public class LwM2MTransportServerInitializer {

    @Autowired
    @Qualifier("LeshanServerCert")
    private LeshanServer lhServerCert;

    @Autowired
    @Qualifier("LeshanServerNoSecPskRpk")
    private LeshanServer lhServerNoSecPskRpk;
//
//    @Autowired
//    @Qualifier("LeshanServerListener")
//    private LwM2mServerListener lwM2mServerListener;

    @Autowired
    private LwM2mServerListener lwM2mServerListenerNoSecPskRpk;

    @Autowired
    private LwM2mServerListener lwM2mServerListenerCert;

    @Autowired
    private LwM2MTransportContextServer context;

    @PostConstruct
    public void init() {
        if (this.context.getCtxServer().getEnableGenPskRpk()) new LWM2MGenerationPSkRPkECC();
        if (this.context.getCtxServer().isServerStartAll()) {
            this.startLhServerCert();
            this.startLhServerNoSecPskRpk();
        } else {
            if (this.context.getCtxServer().getServerDtlsMode() == LwM2MSecurityMode.X509.code) {
                this.startLhServerCert();
            } else {
                this.startLhServerNoSecPskRpk();
            }
        }
    }

    private void startLhServerCert() {
        this.lhServerCert.start();
        LwM2mServerListener serverListenerCert = this.lwM2mServerListenerCert.init(this.lhServerCert);
        this.lhServerCert.getRegistrationService().addListener(serverListenerCert.registrationListener);
        this.lhServerCert.getPresenceService().addListener(serverListenerCert.presenceListener);
        this.lhServerCert.getObservationService().addListener(serverListenerCert.observationListener);
    }

    private void startLhServerNoSecPskRpk() {
        this.lhServerNoSecPskRpk.start();
        LwM2mServerListener serverListenerNoSecPskRpk = this.lwM2mServerListenerNoSecPskRpk.init(this.lhServerNoSecPskRpk);
        this.lhServerNoSecPskRpk.getRegistrationService().addListener(serverListenerNoSecPskRpk.registrationListener);
        this.lhServerNoSecPskRpk.getPresenceService().addListener(serverListenerNoSecPskRpk.presenceListener);
        this.lhServerNoSecPskRpk.getObservationService().addListener(serverListenerNoSecPskRpk.observationListener);
    }

    @PreDestroy
    public void shutdown() {
        log.info("Stopping LwM2M transport Server!");
        try {
            lhServerCert.destroy();
            lhServerNoSecPskRpk.destroy();
        } finally {
        }
        log.info("LwM2M transport Server stopped!");
    }
}
