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

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Slf4j
@Component("LwM2MTransportServerInitializer")
@ConditionalOnExpression("('${service.type:null}'=='tb-transport' && '${transport.lwm2m.enabled:false}'=='true' ) || ('${service.type:null}'=='monolith' && '${transport.lwm2m.enabled}'=='true')")
public class LwM2MTransportServerInitializer {


    @Autowired
    private LwM2MTransportServiceImpl service;

    @Autowired
    @Qualifier("leshanServerX509")
    private LeshanServer lhServerX509;

    @Autowired
    @Qualifier("leshanServerPsk")
    private LeshanServer lhServerPsk;

    @Autowired
    @Qualifier("leshanServerRpk")
    private LeshanServer lhServerRpk;

    @Autowired
    private LwM2MTransportContextServer context;

    @PostConstruct
    public void init() {
        if (this.context.getCtxServer().getEnableGenPskRpk()) new LWM2MGenerationPSkRPkECC();
        if (this.context.getCtxServer().isServerStartPsk()) {
            this.startLhServerPsk();
        }
        if (this.context.getCtxServer().isServerStartRpk()) {
            this.startLhServerRpk();
        }
        if (this.context.getCtxServer().isServerStartX509()) {
            this.startLhServerX509();
        }
    }

    private void startLhServerPsk() {
        this.lhServerPsk.start();
        LwM2mServerListener lhServerPskListener = new LwM2mServerListener(this.lhServerPsk, service);
        this.lhServerPsk.getRegistrationService().addListener(lhServerPskListener.registrationListener);
        this.lhServerPsk.getPresenceService().addListener(lhServerPskListener.presenceListener);
        this.lhServerPsk.getObservationService().addListener(lhServerPskListener.observationListener);
    }

    private void startLhServerRpk() {
        this.lhServerRpk.start();
        LwM2mServerListener lhServerRpkListener = new LwM2mServerListener(this.lhServerRpk, service);
        this.lhServerRpk.getRegistrationService().addListener(lhServerRpkListener.registrationListener);
        this.lhServerRpk.getPresenceService().addListener(lhServerRpkListener.presenceListener);
        this.lhServerRpk.getObservationService().addListener(lhServerRpkListener.observationListener);
    }

    private void startLhServerX509() {
        this.lhServerX509.start();
        LwM2mServerListener lhServerCertListener = new LwM2mServerListener(this.lhServerX509, service);
        this.lhServerX509.getRegistrationService().addListener(lhServerCertListener.registrationListener);
        this.lhServerX509.getPresenceService().addListener(lhServerCertListener.presenceListener);
        this.lhServerX509.getObservationService().addListener(lhServerCertListener.observationListener);
    }

    @PreDestroy
    public void shutdown() {
        log.info("Stopping LwM2M transport Server!");
        lhServerPsk.destroy();
        lhServerRpk.destroy();
        lhServerX509.destroy();
        log.info("LwM2M transport Server stopped!");
    }
}
