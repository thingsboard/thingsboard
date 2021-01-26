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
import org.eclipse.leshan.core.node.codec.DefaultLwM2mNodeDecoder;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mNodeEncoder;
import org.eclipse.leshan.core.node.codec.LwM2mNodeDecoder;
import org.eclipse.leshan.core.util.Hex;
import org.eclipse.leshan.server.californium.LeshanServer;
import org.eclipse.leshan.server.californium.LeshanServerBuilder;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.eclipse.leshan.server.model.VersionedModelProvider;
import org.eclipse.leshan.server.redis.RedisRegistrationStore;
import org.eclipse.leshan.server.redis.RedisSecurityStore;
import org.eclipse.leshan.server.security.DefaultAuthorizer;
import org.eclipse.leshan.server.security.EditableSecurityStore;
import org.eclipse.leshan.server.security.SecurityChecker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.thingsboard.server.transport.lwm2m.secure.LwM2MSecurityMode;
import org.thingsboard.server.transport.lwm2m.server.secure.LwM2mInMemorySecurityStore;
import org.thingsboard.server.transport.lwm2m.utils.LwM2mValueConverterImpl;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.util.Pool;

import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.KeySpec;
import java.util.Arrays;

import static org.eclipse.californium.scandium.dtls.cipher.CipherSuite.TLS_PSK_WITH_AES_128_CBC_SHA256;
import static org.thingsboard.server.transport.lwm2m.secure.LwM2MSecurityMode.PSK;
import static org.thingsboard.server.transport.lwm2m.secure.LwM2MSecurityMode.RPK;
import static org.thingsboard.server.transport.lwm2m.secure.LwM2MSecurityMode.X509;
import static org.thingsboard.server.transport.lwm2m.server.LwM2MTransportHandler.getCoapConfig;


@Slf4j
@ComponentScan("org.thingsboard.server.transport.lwm2m.server")
@ComponentScan("org.thingsboard.server.transport.lwm2m.utils")
@Configuration("LwM2MTransportServerConfiguration")
@ConditionalOnExpression("('${service.type:null}'=='tb-transport' && '${transport.lwm2m.enabled:false}'=='true' ) || ('${service.type:null}'=='monolith' && '${transport.lwm2m.enabled}'=='true')")
public class LwM2MTransportServerConfiguration {
    private PublicKey publicKey;
    private PrivateKey privateKey;

    @Autowired
    private LwM2MTransportContextServer context;

    @Autowired
    private LwM2mInMemorySecurityStore lwM2mInMemorySecurityStore;

    @Primary
    @Bean(name = "leshanServerPsk")
    @ConditionalOnExpression("('${transport.lwm2m.server.secure.start_psk:false}'=='true')")
    public LeshanServer getLeshanServerPsk() {
        log.info("Starting LwM2M transport ServerPsk... PostConstruct");
        return getLeshanServer(this.context.getCtxServer().getServerPortNoSecPsk(), this.context.getCtxServer().getServerPortPsk(), PSK);
    }

    @Bean(name = "leshanServerRpk")
    @ConditionalOnExpression("('${transport.lwm2m.server.secure.start_rpk:false}'=='true')")
    public LeshanServer getLeshanServerRpk() {
        log.info("Starting LwM2M transport ServerRpk... PostConstruct");
        return getLeshanServer(this.context.getCtxServer().getServerPortNoSecRpk(), this.context.getCtxServer().getServerPortRpk(), RPK);
    }

    @Bean(name = "leshanServerX509")
    @ConditionalOnExpression("('${transport.lwm2m.server.secure.start_x509:false}'=='true')")
    public LeshanServer getLeshanServerX509() {
        log.info("Starting LwM2M transport ServerX509... PostConstruct");
        return getLeshanServer(this.context.getCtxServer().getServerPortNoSecX509(), this.context.getCtxServer().getServerPortX509(), X509);
    }

    private LeshanServer getLeshanServer(Integer serverPortNoSec, Integer serverSecurePort, LwM2MSecurityMode dtlsMode) {
        LeshanServerBuilder builder = new LeshanServerBuilder();
        builder.setLocalAddress(this.context.getCtxServer().getServerHost(), serverPortNoSec);
        builder.setLocalSecureAddress(this.context.getCtxServer().getServerSecureHost(), serverSecurePort);
        builder.setEncoder(new DefaultLwM2mNodeEncoder());
        LwM2mNodeDecoder decoder = new DefaultLwM2mNodeDecoder();
        builder.setDecoder(decoder);
        builder.setEncoder(new DefaultLwM2mNodeEncoder(LwM2mValueConverterImpl.getInstance()));

        /** Create CoAP Config */
        builder.setCoapConfig(getCoapConfig(serverPortNoSec, serverSecurePort));

        /** Define model provider (Create Models )*/
        LwM2mModelProvider modelProvider = new VersionedModelProvider(this.context.getCtxServer().getModelsValue());
        builder.setObjectModelProvider(modelProvider);

        /**  Create DTLS security mode
         * There can be only one DTLS security mode
         */
        this.LwM2MSetSecurityStoreServer(builder, dtlsMode);

        /** Create DTLS Config */
        DtlsConnectorConfig.Builder dtlsConfig = new DtlsConnectorConfig.Builder();
        if (dtlsMode==PSK) {
            dtlsConfig.setRecommendedCipherSuitesOnly(this.context.getCtxServer().isRecommendedCiphers());
            dtlsConfig.setRecommendedSupportedGroupsOnly(this.context.getCtxServer().isRecommendedSupportedGroups());
            dtlsConfig.setSupportedCipherSuites(TLS_PSK_WITH_AES_128_CBC_SHA256);
        }
        else  {
            dtlsConfig.setRecommendedSupportedGroupsOnly(!this.context.getCtxServer().isRecommendedSupportedGroups());
            dtlsConfig.setRecommendedCipherSuitesOnly(!this.context.getCtxServer().isRecommendedCiphers());
        }
        /** Set DTLS Config */
        builder.setDtlsConfig(dtlsConfig);

        /** Use a magic converter to support bad type send by the UI. */
        builder.setEncoder(new DefaultLwM2mNodeEncoder(LwM2mValueConverterImpl.getInstance()));


        /** Create LWM2M server */
        return builder.build();
    }

    private void  LwM2MSetSecurityStoreServer(LeshanServerBuilder builder, LwM2MSecurityMode dtlsMode) {
        /** Set securityStore with new registrationStore */
        EditableSecurityStore securityStore = lwM2mInMemorySecurityStore;
        switch (dtlsMode) {
            /** Use PSK only */
            case PSK:
                generatePSK_RPK();
                infoParamsPSK();
                break;
            /** Use RPK only */
            case RPK:
                generatePSK_RPK();
                if (this.publicKey != null && this.publicKey.getEncoded().length > 0 &&
                        this.privateKey != null && this.privateKey.getEncoded().length > 0) {
                    builder.setPublicKey(this.publicKey);
                    builder.setPrivateKey(this.privateKey);
                    infoParamsRPK();
                }
                break;
            /** Use x509 only */
            case X509:
                setServerWithX509Cert(builder);
                break;
            /** No security */
            case NO_SEC:
                builder.setTrustedCertificates(new X509Certificate[0]);
                break;
            /** Use x509 with EST */
            case X509_EST:
                // TODO support sentinel pool and make pool configurable
                break;
            case REDIS:
                /**
                 * Set securityStore with new registrationStore (if use redis store)
                 * Connect to redis
                 */
                Pool<Jedis> jedis = null;
                try {
                    jedis = new JedisPool(new URI(this.context.getCtxServer().getRedisUrl()));
                    securityStore = new RedisSecurityStore(jedis);
                    builder.setRegistrationStore(new RedisRegistrationStore(jedis));
                } catch (URISyntaxException e) {
                    log.error("", e);
                }
                break;
            default:
        }

        /** Set securityStore with registrationStore (if x509)*/
        if (dtlsMode == X509) {
            builder.setAuthorizer(new DefaultAuthorizer(securityStore, new SecurityChecker() {
                @Override
                protected boolean matchX509Identity(String endpoint, String receivedX509CommonName,
                                                    String expectedX509CommonName) {
                    return endpoint.startsWith(expectedX509CommonName);
                }
            }));
        }

        /** Set securityStore with new registrationStore */
        builder.setSecurityStore(securityStore);
    }

    private void generatePSK_RPK() {
        try {
            /** Get Elliptic Curve Parameter spec for secp256r1 */
            AlgorithmParameters algoParameters = AlgorithmParameters.getInstance("EC");
            algoParameters.init(new ECGenParameterSpec("secp256r1"));
            ECParameterSpec parameterSpec = algoParameters.getParameterSpec(ECParameterSpec.class);
            if (this.context.getCtxServer().getServerPublicX() != null && !this.context.getCtxServer().getServerPublicX().isEmpty() && this.context.getCtxServer().getServerPublicY() != null && !this.context.getCtxServer().getServerPublicY().isEmpty()) {
                /** Get point values */
                byte[] publicX = Hex.decodeHex(this.context.getCtxServer().getServerPublicX().toCharArray());
                byte[] publicY = Hex.decodeHex(this.context.getCtxServer().getServerPublicY().toCharArray());
                /** Create key specs */
                KeySpec publicKeySpec = new ECPublicKeySpec(new ECPoint(new BigInteger(publicX), new BigInteger(publicY)),
                        parameterSpec);
                /** Get keys */
                this.publicKey = KeyFactory.getInstance("EC").generatePublic(publicKeySpec);
            }
            if (this.context.getCtxServer().getServerPrivateS() != null && !this.context.getCtxServer().getServerPrivateS().isEmpty()) {
                /** Get point values */
                byte[] privateS = Hex.decodeHex(this.context.getCtxServer().getServerPrivateS().toCharArray());
                /** Create key specs */
                KeySpec privateKeySpec = new ECPrivateKeySpec(new BigInteger(privateS), parameterSpec);
                /** Get keys */
                this.privateKey = KeyFactory.getInstance("EC").generatePrivate(privateKeySpec);
            }
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            log.error("[{}] Failed generate Server PSK/RPK", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private void infoParamsPSK() {
        log.info("\nServer uses PSK -> private key : \n security key : [{}] \n serverSecureURI : [{}]",
                Hex.encodeHexString(this.privateKey.getEncoded()),
                this.context.getCtxServer().getServerSecureHost() + ":" + Integer.toString(this.context.getCtxServer().getServerPortPsk()));
    }

    private void infoParamsRPK() {
        if (this.publicKey instanceof ECPublicKey) {
            /** Get x coordinate */
            byte[] x = ((ECPublicKey) this.publicKey).getW().getAffineX().toByteArray();
            if (x[0] == 0)
                x = Arrays.copyOfRange(x, 1, x.length);

            /** Get Y coordinate */
            byte[] y = ((ECPublicKey) this.publicKey).getW().getAffineY().toByteArray();
            if (y[0] == 0)
                y = Arrays.copyOfRange(y, 1, y.length);

            /** Get Curves params */
            String params = ((ECPublicKey) this.publicKey).getParams().toString();
            log.info(
                    " \nServer uses RPK : \n Elliptic Curve parameters  : [{}] \n Public x coord : [{}] \n Public y coord : [{}] \n Public Key (Hex): [{}] \n Private Key (Hex): [{}]",
                    params, Hex.encodeHexString(x), Hex.encodeHexString(y),
                    Hex.encodeHexString(this.publicKey.getEncoded()),
                    Hex.encodeHexString(this.privateKey.getEncoded()));
        } else {
            throw new IllegalStateException("Unsupported Public Key Format (only ECPublicKey supported).");
        }
    }


    private void setServerWithX509Cert(LeshanServerBuilder builder) {
        try {
            if (this.context.getCtxServer().getKeyStoreValue() != null) {
                setBuilderX509(builder);
                X509Certificate rootCAX509Cert = (X509Certificate) this.context.getCtxServer().getKeyStoreValue().getCertificate(this.context.getCtxServer().getRootAlias());
                if (rootCAX509Cert != null) {
                    X509Certificate[] trustedCertificates = new X509Certificate[1];
                    trustedCertificates[0] = rootCAX509Cert;
                    builder.setTrustedCertificates(trustedCertificates);
                } else {
                    /** by default trust all */
                    builder.setTrustedCertificates(new X509Certificate[0]);
                }
            } else {
                /** by default trust all */
                builder.setTrustedCertificates(new X509Certificate[0]);
                log.error("Unable to load X509 files for LWM2MServer");
            }
        } catch (KeyStoreException ex) {
            log.error("[{}] Unable to load X509 files server", ex.getMessage());
        }
    }

    private void setBuilderX509(LeshanServerBuilder builder) {
        /**
         * For deb => KeyStorePathFile == yml or commandline: KEY_STORE_PATH_FILE
         * For idea => KeyStorePathResource == common/transport/lwm2m/src/main/resources/credentials: in LwM2MTransportContextServer: credentials/serverKeyStore.jks
         */
        try {
            X509Certificate serverCertificate = (X509Certificate) this.context.getCtxServer().getKeyStoreValue().getCertificate(this.context.getCtxServer().getServerAlias());
            PrivateKey privateKey = (PrivateKey) this.context.getCtxServer().getKeyStoreValue().getKey(this.context.getCtxServer().getServerAlias(), this.context.getCtxServer().getKeyStorePasswordServer() == null ? null : this.context.getCtxServer().getKeyStorePasswordServer().toCharArray());
            builder.setPrivateKey(privateKey);
            builder.setCertificateChain(new X509Certificate[]{serverCertificate});
            this.infoParamsX509(serverCertificate, privateKey);
        } catch (Exception ex) {
            log.error("[{}] Unable to load KeyStore  files server", ex.getMessage());
        }
//        /**
//         * For deb => KeyStorePathFile == yml or commandline: KEY_STORE_PATH_FILE
//         * For idea => KeyStorePathResource == common/transport/lwm2m/src/main/resources/credentials: in LwM2MTransportContextServer: credentials/serverKeyStore.jks
//         */
//        try {
//            X509Certificate serverCertificate = (X509Certificate) this.context.getCtxServer().getKeyStoreValue().getCertificate(this.context.getCtxServer().getServerPrivateS());
//            this.privateKey = (PrivateKey) this.context.getCtxServer().getKeyStoreValue().getKey(this.context.getCtxServer().getServerAlias(), this.context.getCtxServer().getKeyStorePasswordServer() == null ? null : this.context.getCtxServer().getKeyStorePasswordServer().toCharArray());
//            if (this.privateKey != null && this.privateKey.getEncoded().length > 0) {
//                builder.setPrivateKey(this.privateKey);
//            }
//            if (serverCertificate != null) {
//                builder.setCertificateChain(new X509Certificate[]{serverCertificate});
//                this.infoParamsX509(serverCertificate);
//            }
//        } catch (Exception ex) {
//            log.error("[{}] Unable to load KeyStore  files server", ex.getMessage());
//        }
    }

    private void infoParamsX509(X509Certificate certificate, PrivateKey privateKey) {
        try {
            log.info("Server uses X509 : \n X509 Certificate (Hex): [{}] \n Private Key (Hex): [{}]",
                    Hex.encodeHexString(certificate.getEncoded()),
                    Hex.encodeHexString(privateKey.getEncoded()));
        } catch (CertificateEncodingException e) {
            log.error("", e);
        }
    }
}
