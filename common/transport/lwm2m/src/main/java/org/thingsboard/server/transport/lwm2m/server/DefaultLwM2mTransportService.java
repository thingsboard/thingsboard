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
package org.thingsboard.server.transport.lwm2m.server;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.elements.util.SslContextUtil;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.dtls.cipher.CipherSuite;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mNodeDecoder;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mNodeEncoder;
import org.eclipse.leshan.server.californium.LeshanServer;
import org.eclipse.leshan.server.californium.LeshanServerBuilder;
import org.eclipse.leshan.server.californium.registration.CaliforniumRegistrationStore;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.springframework.stereotype.Component;
import org.thingsboard.server.cache.ota.OtaPackageDataCache;
import org.thingsboard.server.queue.util.TbLwM2mTransportComponent;
import org.thingsboard.server.transport.lwm2m.config.LwM2MTransportServerConfig;
import org.thingsboard.server.transport.lwm2m.secure.TbLwM2MAuthorizer;
import org.thingsboard.server.transport.lwm2m.secure.TbLwM2MDtlsCertificateVerifier;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClientContext;
import org.thingsboard.server.transport.lwm2m.server.store.TbSecurityStore;
import org.thingsboard.server.transport.lwm2m.server.uplink.DefaultLwM2MUplinkMsgHandler;
import org.thingsboard.server.transport.lwm2m.utils.LwM2mValueConverterImpl;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;

import static org.eclipse.californium.scandium.dtls.cipher.CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256;
import static org.eclipse.californium.scandium.dtls.cipher.CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CCM_8;
import static org.eclipse.californium.scandium.dtls.cipher.CipherSuite.TLS_PSK_WITH_AES_128_CBC_SHA256;
import static org.eclipse.californium.scandium.dtls.cipher.CipherSuite.TLS_PSK_WITH_AES_128_CCM_8;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mNetworkConfig.getCoapConfig;
import static org.thingsboard.server.transport.lwm2m.server.ota.DefaultLwM2MOtaUpdateService.FIRMWARE_UPDATE_COAP_RESOURCE;

@Slf4j
@Component
@TbLwM2mTransportComponent
@RequiredArgsConstructor
public class DefaultLwM2mTransportService implements LwM2MTransportService {

    public static final CipherSuite[] RPK_OR_X509_CIPHER_SUITES = {TLS_PSK_WITH_AES_128_CCM_8, TLS_PSK_WITH_AES_128_CBC_SHA256, TLS_ECDHE_ECDSA_WITH_AES_128_CCM_8, TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256};
    public static final CipherSuite[] PSK_CIPHER_SUITES = {TLS_PSK_WITH_AES_128_CCM_8, TLS_PSK_WITH_AES_128_CBC_SHA256};

    private final LwM2mTransportContext context;
    private final LwM2MTransportServerConfig config;
    private final LwM2mTransportServerHelper helper;
    private final OtaPackageDataCache otaPackageDataCache;
    private final DefaultLwM2MUplinkMsgHandler handler;
    private final CaliforniumRegistrationStore registrationStore;
    private final TbSecurityStore securityStore;
    private final LwM2mClientContext lwM2mClientContext;
    private final TbLwM2MDtlsCertificateVerifier certificateVerifier;
    private final TbLwM2MAuthorizer authorizer;

    private LeshanServer server;

    @PostConstruct
    public void init() {
        this.server = getLhServer();
        /*
         * Add a resource to the server.
         * CoapResource ->
         * path = FW_PACKAGE or SW_PACKAGE
         * nameFile = "BC68JAR01A09_TO_BC68JAR01A10.bin"
         * "coap://host:port/{path}/{token}/{nameFile}"
         */
        LwM2mTransportCoapResource otaCoapResource = new LwM2mTransportCoapResource(otaPackageDataCache, FIRMWARE_UPDATE_COAP_RESOURCE);
        this.server.coap().getServer().add(otaCoapResource);
        this.startLhServer();
        this.context.setServer(server);
    }

    private void startLhServer() {
        log.info("Starting LwM2M transport server...");
        this.server.start();
        LwM2mServerListener lhServerCertListener = new LwM2mServerListener(handler);
        this.server.getRegistrationService().addListener(lhServerCertListener.registrationListener);
        this.server.getPresenceService().addListener(lhServerCertListener.presenceListener);
        this.server.getObservationService().addListener(lhServerCertListener.observationListener);
        log.info("Started LwM2M transport server.");
    }

    @PreDestroy
    public void shutdown() {
        log.info("Stopping LwM2M transport server!");
        server.destroy();
        log.info("LwM2M transport server stopped!");
    }

    private LeshanServer getLhServer() {
        LeshanServerBuilder builder = new LeshanServerBuilder();
        builder.setLocalAddress(config.getHost(), config.getPort());
        builder.setLocalSecureAddress(config.getSecureHost(), config.getSecurePort());
        builder.setDecoder(new DefaultLwM2mNodeDecoder());
        /* Use a magic converter to support bad type send by the UI. */
        builder.setEncoder(new DefaultLwM2mNodeEncoder(LwM2mValueConverterImpl.getInstance()));

        /* Create CoAP Config */
        builder.setCoapConfig(getCoapConfig(config.getPort(), config.getSecurePort(), config));

        /* Define model provider (Create Models )*/
        LwM2mModelProvider modelProvider = new LwM2mVersionedModelProvider(this.lwM2mClientContext, this.helper, this.context);
        config.setModelProvider(modelProvider);
        builder.setObjectModelProvider(modelProvider);

        /* Set securityStore with new registrationStore */
        builder.setSecurityStore(securityStore);
        builder.setRegistrationStore(registrationStore);


        /* Create DTLS Config */
        DtlsConnectorConfig.Builder dtlsConfig = new DtlsConnectorConfig.Builder();

        dtlsConfig.setServerOnly(true);
        dtlsConfig.setRecommendedSupportedGroupsOnly(config.isRecommendedSupportedGroups());
        dtlsConfig.setRecommendedCipherSuitesOnly(config.isRecommendedCiphers());
        /*  Create credentials */
        this.setServerWithCredentials(builder, dtlsConfig);

        /* Set DTLS Config */
        builder.setDtlsConfig(dtlsConfig);

        /* Create LWM2M server */
        return builder.build();
    }

    private void setServerWithCredentials(LeshanServerBuilder builder, DtlsConnectorConfig.Builder dtlsConfig) {
        if (config.getKeyStoreValue() != null && this.setBuilderX509(builder)) {
            dtlsConfig.setAdvancedCertificateVerifier(certificateVerifier);
            builder.setAuthorizer(authorizer);
            dtlsConfig.setSupportedCipherSuites(RPK_OR_X509_CIPHER_SUITES);
        } else {
            /* by default trust all */
            builder.setTrustedCertificates(new X509Certificate[0]);
            log.info("Unable to load X509 files for LWM2MServer");
            dtlsConfig.setSupportedCipherSuites(PSK_CIPHER_SUITES);
        }
    }

    private boolean setBuilderX509(LeshanServerBuilder builder) {
        try {
            X509Certificate[] certificateChain = SslContextUtil.asX509Certificates(config.getKeyStoreValue().getCertificateChain(config.getCertificateAlias()));
            X509Certificate serverCertificate = certificateChain[0];
            PrivateKey privateKey = (PrivateKey) config.getKeyStoreValue().getKey(config.getCertificateAlias(), config.getCertificatePassword() == null ? null : config.getCertificatePassword().toCharArray());
            PublicKey publicKey = serverCertificate.getPublicKey();
            if (privateKey != null && privateKey.getEncoded().length > 0 && publicKey != null && publicKey.getEncoded().length > 0) {
                builder.setPublicKey(serverCertificate.getPublicKey());
                builder.setPrivateKey(privateKey);
                builder.setCertificateChain(certificateChain);
                return true;
            } else {
                return false;
            }
        } catch (Exception ex) {
            log.error("[{}] Unable to load KeyStore  files server", ex.getMessage());
            return false;
        }
    }

    @Override
    public String getName() {
        return "LWM2M";
    }

}
