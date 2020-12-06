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
package org.thingsboard.server.transport.lwm2m.bootstrap;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.leshan.core.model.StaticModel;
import org.eclipse.leshan.server.bootstrap.BootstrapSessionManager;
import org.eclipse.leshan.server.californium.bootstrap.LeshanBootstrapServer;
import org.eclipse.leshan.server.californium.bootstrap.LeshanBootstrapServerBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.thingsboard.server.transport.lwm2m.bootstrap.secure.LwM2MBootstrapSecurityStore;
import org.thingsboard.server.transport.lwm2m.bootstrap.secure.LwM2MInMemoryBootstrapConfigStore;
import org.thingsboard.server.transport.lwm2m.bootstrap.secure.LwM2MSetSecurityStoreBootstrap;
import org.thingsboard.server.transport.lwm2m.bootstrap.secure.LwM2mDefaultBootstrapSessionManager;
import org.thingsboard.server.transport.lwm2m.secure.LwM2MSecurityMode;
import org.thingsboard.server.transport.lwm2m.server.LwM2MTransportContextServer;
import static org.thingsboard.server.transport.lwm2m.secure.LwM2MSecurityMode.X509;
import static org.thingsboard.server.transport.lwm2m.secure.LwM2MSecurityMode.RPK;
import static org.thingsboard.server.transport.lwm2m.server.LwM2MTransportHandler.getCoapConfig;

@Slf4j
@Component
@ConditionalOnExpression("('${service.type:null}'=='tb-transport' && '${transport.lwm2m.enabled:false}'=='true'&& '${transport.lwm2m.bootstrap.enable:false}'=='true') || ('${service.type:null}'=='monolith' && '${transport.lwm2m.enabled}'=='true'&& '${transport.lwm2m.bootstrap.enable}'=='true')")
public class LwM2MTransportBootstrapServerConfiguration {

    @Autowired
    private LwM2MTransportContextBootstrap contextBs;

    @Autowired
    private LwM2MTransportContextServer contextS;

    @Autowired
    private LwM2MBootstrapSecurityStore lwM2MBootstrapSecurityStore;

    @Autowired
    private LwM2MInMemoryBootstrapConfigStore lwM2MInMemoryBootstrapConfigStore;


    @Primary
    @Bean(name = "leshanBootstrapCert")
    public LeshanBootstrapServer getLeshanBootstrapServerCert() {
        log.info("Prepare and start BootstrapServerCert... PostConstruct");
        return getLeshanBootstrapServer(this.contextBs.getCtxBootStrap().getBootstrapPortCert(), this.contextBs.getCtxBootStrap().getBootstrapSecurePortCert(), X509);
    }

    @Bean(name = "leshanBootstrapRPK")
    public LeshanBootstrapServer getLeshanBootstrapServerRPK() {
        log.info("Prepare and start BootstrapServerRPK... PostConstruct");
        return getLeshanBootstrapServer(this.contextBs.getCtxBootStrap().getBootstrapPort(), this.contextBs.getCtxBootStrap().getBootstrapSecurePort(), RPK);
    }

    public LeshanBootstrapServer getLeshanBootstrapServer(Integer bootstrapPort, Integer bootstrapSecurePort, LwM2MSecurityMode dtlsMode) {
        LeshanBootstrapServerBuilder builder = new LeshanBootstrapServerBuilder();
        builder.setLocalAddress(this.contextBs.getCtxBootStrap().getBootstrapHost(), bootstrapPort);
        builder.setLocalSecureAddress(this.contextBs.getCtxBootStrap().getBootstrapSecureHost(), bootstrapSecurePort);

        /** Create CoAP Config */
        builder.setCoapConfig(getCoapConfig ());

        /**  ConfigStore */
        builder.setConfigStore(lwM2MInMemoryBootstrapConfigStore);

        /** SecurityStore */
        builder.setSecurityStore(lwM2MBootstrapSecurityStore);

        /** Define model provider (Create Models )*/
        builder.setModel(new StaticModel(contextS.getCtxServer().getModelsValue()));

        /** Create and Set DTLS Config */
        DtlsConnectorConfig.Builder dtlsConfig = new DtlsConnectorConfig.Builder();
        dtlsConfig.setRecommendedCipherSuitesOnly(contextS.getCtxServer().isSupportDeprecatedCiphersEnable());
        builder.setDtlsConfig(dtlsConfig);

        /**  Create credentials */
        new LwM2MSetSecurityStoreBootstrap(builder, contextBs, contextS, dtlsMode);

        BootstrapSessionManager sessionManager = new LwM2mDefaultBootstrapSessionManager(lwM2MBootstrapSecurityStore);
        builder.setSessionManager(sessionManager);

        /** Create BootstrapServer */
        return builder.build();
    }
}
