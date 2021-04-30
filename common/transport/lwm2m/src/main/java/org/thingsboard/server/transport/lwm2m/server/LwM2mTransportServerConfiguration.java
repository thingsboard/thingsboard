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

import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.core.network.stack.BlockwiseLayer;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mNodeDecoder;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mNodeEncoder;
import org.eclipse.leshan.core.util.Hex;
import org.eclipse.leshan.server.californium.LeshanServer;
import org.eclipse.leshan.server.californium.LeshanServerBuilder;
import org.eclipse.leshan.server.californium.registration.CaliforniumRegistrationStore;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.eclipse.leshan.server.security.DefaultAuthorizer;
import org.eclipse.leshan.server.security.EditableSecurityStore;
import org.eclipse.leshan.server.security.SecurityChecker;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.queue.util.TbLwM2mTransportComponent;
import org.thingsboard.server.transport.lwm2m.config.LwM2MTransportServerConfig;
import org.thingsboard.server.transport.lwm2m.secure.LWM2MGenerationPSkRPkECC;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClientContext;
import org.thingsboard.server.transport.lwm2m.utils.LwM2mValueConverterImpl;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.math.BigInteger;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;

import static org.eclipse.californium.scandium.dtls.cipher.CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256;
import static org.eclipse.californium.scandium.dtls.cipher.CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CCM_8;
import static org.eclipse.californium.scandium.dtls.cipher.CipherSuite.TLS_PSK_WITH_AES_128_CBC_SHA256;
import static org.eclipse.californium.scandium.dtls.cipher.CipherSuite.TLS_PSK_WITH_AES_128_CCM_8;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mNetworkConfig.getCoapConfig;

@Slf4j
@Component
@TbLwM2mTransportComponent
public class LwM2mTransportServerConfiguration {
    private PublicKey publicKey;
    private PrivateKey privateKey;
    private boolean pskMode = false;
    private LeshanServer leshanServer;
    private final LwM2mTransportContextServer context;
    private final CaliforniumRegistrationStore registrationStore;
    private final EditableSecurityStore securityStore;
    private final LwM2mClientContext lwM2mClientContext;
    private final LwM2mTransportServiceImpl service;

    public LwM2mTransportServerConfiguration(LwM2mTransportContextServer context, CaliforniumRegistrationStore registrationStore,
                                             EditableSecurityStore securityStore, LwM2mClientContext lwM2mClientContext,
                                             @Lazy LwM2mTransportServiceImpl service) {
        this.context = context;
        this.registrationStore = registrationStore;
        this.securityStore = securityStore;
        this.lwM2mClientContext = lwM2mClientContext;
        this.service = service;
    }

    @PostConstruct
    public void init() {
        if (this.context.getLwM2MTransportServerConfig().getEnableGenNewKeyPskRpk()) {
            new LWM2MGenerationPSkRPkECC();
        }
        this.leshanServer = this.getLhServer();
        this.startLhServer();
    }

    private void startLhServer() {
        this.leshanServer.start();
        LwM2mServerListener lhServerCertListener = new LwM2mServerListener(service);
        this.leshanServer.getRegistrationService().addListener(lhServerCertListener.registrationListener);
        this.leshanServer.getPresenceService().addListener(lhServerCertListener.presenceListener);
        this.leshanServer.getObservationService().addListener(lhServerCertListener.observationListener);
    }

    public LeshanServer getLeshanServer() {
        return  this.leshanServer;
    }

    @PreDestroy
    public void shutdown() {
        log.info("Stopping LwM2M transport Server!");
        leshanServer.destroy();
        log.info("LwM2M transport Server stopped!");
    }

    private LeshanServer getLhServer() {
        Integer serverPortNoSec = this.context.getLwM2MTransportServerConfig().getPort();
        Integer serverSecurePort = this.context.getLwM2MTransportServerConfig().getSecurePort();
        LeshanServerBuilder builder = new LeshanServerBuilder();
        builder.setLocalAddress(this.context.getLwM2MTransportServerConfig().getHost(), serverPortNoSec);
        builder.setLocalSecureAddress(this.context.getLwM2MTransportServerConfig().getSecureHost(), serverSecurePort);
        builder.setDecoder(new DefaultLwM2mNodeDecoder());
        /** Use a magic converter to support bad type send by the UI. */
        builder.setEncoder(new DefaultLwM2mNodeEncoder(LwM2mValueConverterImpl.getInstance()));


        /** Create CoAP Config */
        NetworkConfig networkConfig = getCoapConfig(serverPortNoSec, serverSecurePort);
        BlockwiseLayer blockwiseLayer = new BlockwiseLayer(networkConfig);
        builder.setCoapConfig(getCoapConfig(serverPortNoSec, serverSecurePort));

        /** Define model provider (Create Models )*/
        LwM2mModelProvider modelProvider = new LwM2mVersionedModelProvider(this.lwM2mClientContext, this.context);
        this.context.getLwM2MTransportServerConfig().setModelProvider(modelProvider);
        builder.setObjectModelProvider(modelProvider);

        /**  Create credentials */
        this.setServerWithCredentials(builder);

        /** Set securityStore with new registrationStore */
        builder.setSecurityStore(securityStore);
        builder.setRegistrationStore(registrationStore);


        /** Create DTLS Config */
        DtlsConnectorConfig.Builder dtlsConfig = new DtlsConnectorConfig.Builder();
        dtlsConfig.setServerOnly(true);
        dtlsConfig.setRecommendedSupportedGroupsOnly(this.context.getLwM2MTransportServerConfig().isRecommendedSupportedGroups());
        dtlsConfig.setRecommendedCipherSuitesOnly(this.context.getLwM2MTransportServerConfig().isRecommendedCiphers());
        if (this.pskMode) {
            dtlsConfig.setSupportedCipherSuites(
                    TLS_PSK_WITH_AES_128_CCM_8,
                    TLS_PSK_WITH_AES_128_CBC_SHA256);
        } else {
            dtlsConfig.setSupportedCipherSuites(
                    TLS_PSK_WITH_AES_128_CCM_8,
                    TLS_PSK_WITH_AES_128_CBC_SHA256,
                    TLS_ECDHE_ECDSA_WITH_AES_128_CCM_8,
                    TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256);
        }

        /** Set DTLS Config */
        builder.setDtlsConfig(dtlsConfig);

        /** Create LWM2M server */
        return builder.build();
    }

    private void setServerWithCredentials(LeshanServerBuilder builder) {
        try {
            if (this.context.getLwM2MTransportServerConfig().getKeyStoreValue() != null) {
                if (this.setBuilderX509(builder)) {
                    X509Certificate rootCAX509Cert = (X509Certificate) this.context.getLwM2MTransportServerConfig().getKeyStoreValue().getCertificate(this.context.getLwM2MTransportServerConfig().getRootCertificateAlias());
                    if (rootCAX509Cert != null) {
                        X509Certificate[] trustedCertificates = new X509Certificate[1];
                        trustedCertificates[0] = rootCAX509Cert;
                        builder.setTrustedCertificates(trustedCertificates);
                    } else {
                        /** by default trust all */
                        builder.setTrustedCertificates(new X509Certificate[0]);
                    }
                    /** Set securityStore with registrationStore*/
                    builder.setAuthorizer(new DefaultAuthorizer(securityStore, new SecurityChecker() {
                        @Override
                        protected boolean matchX509Identity(String endpoint, String receivedX509CommonName,
                                                            String expectedX509CommonName) {
                            return endpoint.startsWith(expectedX509CommonName);
                        }
                    }));
                }
            } else if (this.setServerRPK(builder)) {
                this.infoPramsUri("RPK");
                this.infoParamsServerKey(this.publicKey, this.privateKey);
            } else {
                /** by default trust all */
                builder.setTrustedCertificates(new X509Certificate[0]);
                log.info("Unable to load X509 files for LWM2MServer");
                this.pskMode = true;
                this.infoPramsUri("PSK");
            }
        } catch (KeyStoreException ex) {
            log.error("[{}] Unable to load X509 files server", ex.getMessage());
        }
    }

    private boolean setBuilderX509(LeshanServerBuilder builder) {
        try {
            X509Certificate serverCertificate = (X509Certificate) this.context.getLwM2MTransportServerConfig().getKeyStoreValue().getCertificate(this.context.getLwM2MTransportServerConfig().getCertificateAlias());
            PrivateKey privateKey = (PrivateKey) this.context.getLwM2MTransportServerConfig().getKeyStoreValue().getKey(this.context.getLwM2MTransportServerConfig().getCertificateAlias(), this.context.getLwM2MTransportServerConfig().getKeyStorePassword() == null ? null : this.context.getLwM2MTransportServerConfig().getKeyStorePassword().toCharArray());
            PublicKey publicKey = serverCertificate.getPublicKey();
            if (privateKey != null && privateKey.getEncoded().length > 0 && publicKey != null && publicKey.getEncoded().length > 0) {
                builder.setPublicKey(serverCertificate.getPublicKey());
                builder.setPrivateKey(privateKey);
                builder.setCertificateChain(new X509Certificate[]{serverCertificate});
                this.infoParamsServerX509(serverCertificate, publicKey, privateKey);
                return true;
            } else {
                return false;
            }
        } catch (Exception ex) {
            log.error("[{}] Unable to load KeyStore  files server", ex.getMessage());
            return false;
        }
    }

    private void infoParamsServerX509(X509Certificate certificate, PublicKey publicKey, PrivateKey privateKey) {
        try {
            infoPramsUri("X509");
            log.info("\n- X509 Certificate (Hex): [{}]",
                    Hex.encodeHexString(certificate.getEncoded()));
            this.infoParamsServerKey(publicKey, privateKey);
        } catch (CertificateEncodingException e) {
            log.error("", e);
        }
    }

    private void infoPramsUri(String mode) {
        LwM2MTransportServerConfig lwM2MTransportServerConfig = this.context.getLwM2MTransportServerConfig();
        log.info("Server uses [{}]: serverNoSecureURI : [{}:{}], serverSecureURI : [{}:{}]", mode,
                lwM2MTransportServerConfig.getHost(),
                lwM2MTransportServerConfig.getPort(),
                lwM2MTransportServerConfig.getSecureHost(),
                lwM2MTransportServerConfig.getSecurePort());
    }

    private boolean setServerRPK(LeshanServerBuilder builder) {
        try {
            this.generateKeyForRPK();
            if (this.publicKey != null && this.publicKey.getEncoded().length > 0 &&
                    this.privateKey != null && this.privateKey.getEncoded().length > 0) {
                builder.setPublicKey(this.publicKey);
                builder.setPrivateKey(this.privateKey);
                return true;
            }
        } catch (NoSuchAlgorithmException | InvalidParameterSpecException | InvalidKeySpecException e) {
            log.error("Fail create Server with RPK", e);
        }
        return false;
    }

    private void generateKeyForRPK() throws NoSuchAlgorithmException, InvalidParameterSpecException, InvalidKeySpecException {
        /** Get Elliptic Curve Parameter spec for secp256r1 */
        AlgorithmParameters algoParameters = AlgorithmParameters.getInstance("EC");
        algoParameters.init(new ECGenParameterSpec("secp256r1"));
        ECParameterSpec parameterSpec = algoParameters.getParameterSpec(ECParameterSpec.class);
        LwM2MTransportServerConfig serverConfig = this.context.getLwM2MTransportServerConfig();
        if (StringUtils.isNotEmpty(serverConfig.getPublicX()) && StringUtils.isNotEmpty(serverConfig.getPublicY())) {
            byte[] publicX = Hex.decodeHex(serverConfig.getPublicX().toCharArray());
            byte[] publicY = Hex.decodeHex(serverConfig.getPublicY().toCharArray());
            KeySpec publicKeySpec = new ECPublicKeySpec(new ECPoint(new BigInteger(publicX), new BigInteger(publicY)),
                    parameterSpec);
            this.publicKey = KeyFactory.getInstance("EC").generatePublic(publicKeySpec);
        }
        String privateEncodedKey = serverConfig.getPrivateEncoded();
        if (StringUtils.isNotEmpty(privateEncodedKey)) {
            byte[] privateS = Hex.decodeHex(privateEncodedKey.toCharArray());
            try {
                this.privateKey = KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(privateS));
            } catch (InvalidKeySpecException ignore2) {
                log.error("Invalid Server rpk.PrivateKey.getEncoded () [{}}]. PrivateKey has no EC algorithm", privateEncodedKey);
            }
        }
    }

    private void infoParamsServerKey(PublicKey publicKey, PrivateKey privateKey) {
        /** Get x coordinate */
        byte[] x = ((ECPublicKey) publicKey).getW().getAffineX().toByteArray();
        if (x[0] == 0)
            x = Arrays.copyOfRange(x, 1, x.length);

        /** Get Y coordinate */
        byte[] y = ((ECPublicKey) publicKey).getW().getAffineY().toByteArray();
        if (y[0] == 0)
            y = Arrays.copyOfRange(y, 1, y.length);

        /** Get Curves params */
        String params = ((ECPublicKey) publicKey).getParams().toString();
        String privHex = Hex.encodeHexString(privateKey.getEncoded());
        log.info(" \n- Public Key (Hex): [{}] \n" +
                        "- Private Key (Hex): [{}], \n" +
                        "public_x: \"${LWM2M_SERVER_PUBLIC_X:{}}\" \n" +
                        "public_y: \"${LWM2M_SERVER_PUBLIC_Y:{}}\" \n" +
                        "private_encoded: \"${LWM2M_SERVER_PRIVATE_ENCODED:{}}\" \n" +
                        "- Elliptic Curve parameters  : [{}]",
                Hex.encodeHexString(publicKey.getEncoded()),
                privHex,
                Hex.encodeHexString(x),
                Hex.encodeHexString(y),
                privHex,
                params);
    }

}
