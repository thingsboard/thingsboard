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
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mNodeDecoder;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mNodeEncoder;
import org.eclipse.leshan.core.node.codec.LwM2mNodeDecoder;
import org.eclipse.leshan.server.californium.LeshanServer;
import org.eclipse.leshan.server.californium.LeshanServerBuilder;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.eclipse.leshan.server.model.VersionedModelProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.thingsboard.server.transport.lwm2m.secure.LwM2MSecurityMode;
import org.thingsboard.server.transport.lwm2m.server.secure.LwM2MSetSecurityStoreServer;
import org.thingsboard.server.transport.lwm2m.server.secure.LwM2mInMemorySecurityStore;
import org.thingsboard.server.transport.lwm2m.utils.LwM2mGetModels;
import org.thingsboard.server.transport.lwm2m.utils.LwM2mValueConverterImpl;

import java.util.List;

import static org.thingsboard.server.transport.lwm2m.server.LwM2MTransportHandler.*;
import static org.thingsboard.server.transport.lwm2m.secure.LwM2MSecurityMode.*;


@Slf4j
@ComponentScan("org.thingsboard.server.transport.lwm2m.server")
@ComponentScan("org.thingsboard.server.transport.lwm2m.utils")
@Configuration("LwM2MTransportServerConfiguration")
@ConditionalOnExpression("('${service.type:null}'=='tb-transport' && '${transport.lwm2m.enabled}'=='true' )|| ('${service.type:null}'=='monolith' && '${transport.lwm2m.enabled}'=='true')")
public class LwM2MTransportServerConfiguration {

    @Autowired
    private LwM2MTransportContextServer context;

    @Autowired
    private LwM2mInMemorySecurityStore lwM2mInMemorySecurityStore;

    @Autowired
    LwM2mGetModels lwM2mGetModels;

    @Primary
    @Bean(name = "LeshanServerCert")
    public LeshanServer getLeshanServerCert() {
        log.info("Starting LwM2M transport ServerCert... PostConstruct");
        return getLeshanServer(context.getServerPortCert(), context.getServerSecurePortCert(), X509);
    }

    @Bean(name = "leshanServerNoSecPskRpk")
    public LeshanServer getLeshanServerNoSecPskRpk() {
        log.info("Starting LwM2M transport ServerNoSecPskRpk... PostConstruct");
        return getLeshanServer(context.getServerPort(), context.getServerSecurePort(), RPK);
    }

    private LeshanServer getLeshanServer(Integer serverPort, Integer serverSecurePort, LwM2MSecurityMode dtlsMode) {

        LeshanServerBuilder builder = new LeshanServerBuilder();
        builder.setLocalAddress(context.getServerHost(), serverPort);
        builder.setLocalSecureAddress(context.getServerSecureHost(), serverSecurePort);
        builder.setEncoder(new DefaultLwM2mNodeEncoder());
        LwM2mNodeDecoder decoder = new DefaultLwM2mNodeDecoder();
        builder.setDecoder(decoder);
        builder.setEncoder(new DefaultLwM2mNodeEncoder(new LwM2mValueConverterImpl()));

        /** Create CoAP Config */
        builder.setCoapConfig(getCoapConfig());

        /** Define model provider (Create Models )*/
        LwM2mModelProvider modelProvider = new VersionedModelProvider(lwM2mGetModels.getModels());
        builder.setObjectModelProvider(modelProvider);

        /** Create DTLS Config */
        DtlsConnectorConfig.Builder dtlsConfig = new DtlsConnectorConfig.Builder();
        dtlsConfig.setRecommendedCipherSuitesOnly(context.isSupportDeprecatedCiphersEnable());
        /** Set DTLS Config */
        builder.setDtlsConfig(dtlsConfig);

        /** Use a magic converter to support bad type send by the UI. */
        builder.setEncoder(new DefaultLwM2mNodeEncoder(new LwM2mValueConverterImpl()));

        /**  Create DTLS security mode
         * There can be only one DTLS security mode
         */
        new LwM2MSetSecurityStoreServer(builder, context, lwM2mInMemorySecurityStore, dtlsMode);

        /** Create LWM2M server */
        return builder.build();
    }
}
