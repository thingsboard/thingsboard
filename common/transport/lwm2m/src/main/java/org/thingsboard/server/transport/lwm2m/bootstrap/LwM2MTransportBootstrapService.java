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
package org.thingsboard.server.transport.lwm2m.bootstrap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.elements.util.SslContextUtil;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.leshan.core.model.ObjectLoader;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.StaticModel;
import org.eclipse.leshan.server.bootstrap.BootstrapSessionManager;
import org.eclipse.leshan.server.californium.bootstrap.LeshanBootstrapServer;
import org.eclipse.leshan.server.californium.bootstrap.LeshanBootstrapServerBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.transport.config.ssl.SslCredentials;
import org.thingsboard.server.transport.lwm2m.bootstrap.secure.LwM2MBootstrapSecurityStore;
import org.thingsboard.server.transport.lwm2m.bootstrap.secure.LwM2MInMemoryBootstrapConfigStore;
import org.thingsboard.server.transport.lwm2m.bootstrap.secure.LwM2MInMemoryBootstrapConfigurationAdapter;
import org.thingsboard.server.transport.lwm2m.bootstrap.secure.LwM2mDefaultBootstrapSessionManager;
import org.thingsboard.server.transport.lwm2m.config.LwM2MTransportBootstrapConfig;
import org.thingsboard.server.transport.lwm2m.config.LwM2MTransportServerConfig;
import org.thingsboard.server.transport.lwm2m.server.DefaultLwM2mTransportService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.List;

import static org.thingsboard.server.transport.lwm2m.server.LwM2mNetworkConfig.getCoapConfig;

@Slf4j
@Component
@ConditionalOnExpression("('${service.type:null}'=='tb-transport' && '${transport.lwm2m.enabled:false}'=='true' && '${transport.lwm2m.bootstrap.enable:false}'=='true') || ('${service.type:null}'=='monolith' && '${transport.lwm2m.enabled:false}'=='true'&& '${transport.lwm2m.bootstrap.enable:false}'=='true')")
@RequiredArgsConstructor
public class LwM2MTransportBootstrapService {
    private boolean pskMode = false;

    private final LwM2MTransportServerConfig serverConfig;
    private final LwM2MTransportBootstrapConfig bootstrapConfig;
    private final LwM2MBootstrapSecurityStore lwM2MBootstrapSecurityStore;
    private final LwM2MInMemoryBootstrapConfigStore lwM2MInMemoryBootstrapConfigStore;

    private LeshanBootstrapServer server;

    @PostConstruct
    public void init() {
        log.info("Starting LwM2M transport bootstrap server...");
        this.server = getLhBootstrapServer();
        this.server.start();
        log.info("Started LwM2M transport bootstrap server.");
    }

    @PreDestroy
    public void shutdown() {
        log.info("Stopping LwM2M transport bootstrap server!");
        server.destroy();
        log.info("LwM2M transport bootstrap server stopped!");
    }

    public LeshanBootstrapServer getLhBootstrapServer() {
        LeshanBootstrapServerBuilder builder = new LeshanBootstrapServerBuilder();
        builder.setLocalAddress(bootstrapConfig.getHost(), bootstrapConfig.getPort());
        builder.setLocalSecureAddress(bootstrapConfig.getSecureHost(), bootstrapConfig.getSecurePort());

        /* Create CoAP Config */
        builder.setCoapConfig(getCoapConfig(bootstrapConfig.getPort(), bootstrapConfig.getSecurePort(), serverConfig));

        /* Define model provider (Create Models )*/
        List<ObjectModel> models = ObjectLoader.loadDefault();
        builder.setModel(new StaticModel(models));

        /*  Create credentials */
        this.setServerWithCredentials(builder);

        /* Set securityStore with new ConfigStore */
        builder.setConfigStore(new LwM2MInMemoryBootstrapConfigurationAdapter(lwM2MInMemoryBootstrapConfigStore));

        /* SecurityStore */
        builder.setSecurityStore(lwM2MBootstrapSecurityStore);


        /* Create and Set DTLS Config */
        DtlsConnectorConfig.Builder dtlsConfig = new DtlsConnectorConfig.Builder();
        dtlsConfig.setRecommendedSupportedGroupsOnly(serverConfig.isRecommendedSupportedGroups());
        dtlsConfig.setRecommendedCipherSuitesOnly(serverConfig.isRecommendedCiphers());
        dtlsConfig.setSupportedCipherSuites(this.pskMode ? DefaultLwM2mTransportService.PSK_CIPHER_SUITES : DefaultLwM2mTransportService.RPK_OR_X509_CIPHER_SUITES);

        /* Set DTLS Config */
        builder.setDtlsConfig(dtlsConfig);

        BootstrapSessionManager sessionManager = new LwM2mDefaultBootstrapSessionManager(lwM2MBootstrapSecurityStore);
        builder.setSessionManager(sessionManager);

        /* Create BootstrapServer */
        return builder.build();
    }

    private void setServerWithCredentials(LeshanBootstrapServerBuilder builder) {
        if (this.bootstrapConfig.getSslCredentials() != null) {
            SslCredentials sslCredentials = this.bootstrapConfig.getSslCredentials();
            builder.setPublicKey(sslCredentials.getPublicKey());
            builder.setPrivateKey(sslCredentials.getPrivateKey());
            builder.setCertificateChain(sslCredentials.getCertificateChain());
            if (this.serverConfig.getTrustSslCredentials() != null) {
                builder.setTrustedCertificates(this.serverConfig.getTrustSslCredentials().getTrustedCertificates());
            } else {
                /* by default trust all */
                builder.setTrustedCertificates(new X509Certificate[0]);
            }
        } else {
            /* by default trust all */
            builder.setTrustedCertificates(new X509Certificate[0]);
            log.info("Unable to load X509 files for BootStrapServer");
            this.pskMode = true;
        }
    }
}
