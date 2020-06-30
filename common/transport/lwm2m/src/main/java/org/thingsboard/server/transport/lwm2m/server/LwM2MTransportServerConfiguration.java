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
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.leshan.core.model.ObjectLoader;
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
import org.thingsboard.server.transport.lwm2m.server.secure.LwM2MSetSecurityStoreServer;
import org.thingsboard.server.transport.lwm2m.server.secure.LwM2mInMemorySecurityStore;
import org.thingsboard.server.transport.lwm2m.utils.LwM2mValueConverterImpl;
import java.io.*;
import java.net.URISyntaxException;
import java.util.List;
import static org.thingsboard.server.transport.lwm2m.server.LwM2MTransportHandler.*;


@Slf4j
@ComponentScan("org.thingsboard.server.transport.lwm2m.server")
@Configuration("LwM2MTransportServerConfiguration")
@ConditionalOnExpression("'${service.type:null}'=='tb-transport' || ('${service.type:null}'=='monolith' && '${transport.lwm2m.enabled}'=='true')")
public class LwM2MTransportServerConfiguration {

    @Autowired
    private LwM2MTransportContextServer context;

    @Autowired
    private LwM2mInMemorySecurityStore lwM2mInMemorySecurityStore;

    @Bean
    public LeshanServer getLeshanServer() throws URISyntaxException {
        log.info("Starting LwM2M transport Server... PostConstruct");
        LeshanServerBuilder builder = new LeshanServerBuilder();
        builder.setLocalAddress(context.getServerHost(), context.getServerPort());
        builder.setLocalSecureAddress(context.getServerSecureHost(), context.getServerSecurePort());
        builder.setEncoder(new DefaultLwM2mNodeEncoder());
        LwM2mNodeDecoder decoder = new DefaultLwM2mNodeDecoder();
        builder.setDecoder(decoder);
        builder.setEncoder(new DefaultLwM2mNodeEncoder(new LwM2mValueConverterImpl()));

        /** Create CoAP Config */
        NetworkConfig coapConfig;
        File configFile = new File(NetworkConfig.DEFAULT_FILE_NAME);
        if (configFile.isFile()) {
            coapConfig = new NetworkConfig();
            coapConfig.load(configFile);
        } else {
            coapConfig = LeshanServerBuilder.createDefaultNetworkConfig();
            coapConfig.store(configFile);
        }
        builder.setCoapConfig(coapConfig);

        /** Define model provider */
        List<ObjectModel> models = ObjectLoader.loadDefault();
        List<ObjectModel> listModels = ObjectLoader.loadDdfResources(MODEL_DEFAULT_RESOURCE_PATH, modelPaths);
        models.addAll(listModels);
        if (context.getModelFolderPath() != null) {
            models.addAll(ObjectLoader.loadObjectsFromDir(new File(context.getModelFolderPath())));
        }
        LwM2mModelProvider modelProvider = new VersionedModelProvider(models);
        builder.setObjectModelProvider(modelProvider);

        /** Create DTLS Config */
        DtlsConnectorConfig.Builder dtlsConfig = new DtlsConnectorConfig.Builder();
        dtlsConfig.setRecommendedCipherSuitesOnly(context.isSupportDeprecatedCiphersEnable());
        /** Set DTLS Config */
        builder.setDtlsConfig(dtlsConfig);

        /**  Create DTLS security mode
         * There can be only one DTLS security mode
         */
        new LwM2MSetSecurityStoreServer(builder, context, lwM2mInMemorySecurityStore);

        /** Use a magic converter to support bad type send by the UI. */
        builder.setEncoder(new DefaultLwM2mNodeEncoder(new LwM2mValueConverterImpl()));

        /** Create LWM2M server */
        return builder.build();
    }
}
