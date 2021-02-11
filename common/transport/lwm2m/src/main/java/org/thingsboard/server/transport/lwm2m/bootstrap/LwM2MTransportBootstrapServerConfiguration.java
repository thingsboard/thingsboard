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

import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.leshan.core.model.StaticModel;
import org.eclipse.leshan.core.util.Hex;
import org.eclipse.leshan.server.bootstrap.BootstrapSessionManager;
import org.eclipse.leshan.server.californium.bootstrap.LeshanBootstrapServer;
import org.eclipse.leshan.server.californium.bootstrap.LeshanBootstrapServerBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.thingsboard.server.transport.lwm2m.bootstrap.secure.LwM2MBootstrapSecurityStore;
import org.thingsboard.server.transport.lwm2m.bootstrap.secure.LwM2MInMemoryBootstrapConfigStore;
import org.thingsboard.server.transport.lwm2m.bootstrap.secure.LwM2mDefaultBootstrapSessionManager;
import org.thingsboard.server.transport.lwm2m.server.LwM2MTransportContextServer;

import java.math.BigInteger;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.KeyStore;
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
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;

import static org.eclipse.californium.scandium.dtls.cipher.CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256;
import static org.eclipse.californium.scandium.dtls.cipher.CipherSuite.TLS_PSK_WITH_AES_128_CBC_SHA256;
import static org.thingsboard.server.transport.lwm2m.server.LwM2MTransportHandler.getCoapConfig;

@Slf4j
@Component
@ConditionalOnExpression("('${service.type:null}'=='tb-transport' && '${transport.lwm2m.enabled:false}'=='true'&& '${transport.lwm2m.bootstrap.enable:false}'=='true') || ('${service.type:null}'=='monolith' && '${transport.lwm2m.enabled:false}'=='true'&& '${transport.lwm2m.bootstrap.enable:false}'=='true')")
public class LwM2MTransportBootstrapServerConfiguration {
    private PublicKey publicKey;
    private PrivateKey privateKey;

    @Autowired
    private LwM2MTransportContextBootstrap contextBs;

    @Autowired
    private LwM2MTransportContextServer contextS;

    @Autowired
    private LwM2MBootstrapSecurityStore lwM2MBootstrapSecurityStore;

    @Autowired
    private LwM2MInMemoryBootstrapConfigStore lwM2MInMemoryBootstrapConfigStore;


    @Bean
    public LeshanBootstrapServer getLeshanBootstrapServer() {
        log.info("Prepare and start BootstrapServer... PostConstruct");
        return this.getLhBootstrapServer(this.contextBs.getCtxBootStrap().getBootstrapPortNoSec(), this.contextBs.getCtxBootStrap().getBootstrapPortSecurity());
    }

    public LeshanBootstrapServer getLhBootstrapServer(Integer bootstrapPortNoSec, Integer bootstrapSecurePort) {
        LeshanBootstrapServerBuilder builder = new LeshanBootstrapServerBuilder();
        builder.setLocalAddress(this.contextBs.getCtxBootStrap().getBootstrapHost(), bootstrapPortNoSec);
        builder.setLocalSecureAddress(this.contextBs.getCtxBootStrap().getBootstrapHostSecurity(), bootstrapSecurePort);

        /** Create CoAP Config */
        builder.setCoapConfig(getCoapConfig(bootstrapPortNoSec, bootstrapSecurePort));

        /** Define model provider (Create Models )*/
        builder.setModel(new StaticModel(contextS.getCtxServer().getModelsValue()));

        /**  Create credentials */
        this.setServerWithCredentials(builder);

        /** Set securityStore with new ConfigStore */
        builder.setConfigStore(lwM2MInMemoryBootstrapConfigStore);

        /** SecurityStore */
        builder.setSecurityStore(lwM2MBootstrapSecurityStore);


        /** Create and Set DTLS Config */
        DtlsConnectorConfig.Builder dtlsConfig = new DtlsConnectorConfig.Builder();
        dtlsConfig.setRecommendedSupportedGroupsOnly(!this.contextS.getCtxServer().isRecommendedSupportedGroups());
        dtlsConfig.setRecommendedCipherSuitesOnly(this.contextS.getCtxServer().isRecommendedCiphers());
        dtlsConfig.setSupportedCipherSuites(TLS_PSK_WITH_AES_128_CBC_SHA256, TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256);

        /** Set DTLS Config */
        builder.setDtlsConfig(dtlsConfig);

        BootstrapSessionManager sessionManager = new LwM2mDefaultBootstrapSessionManager(lwM2MBootstrapSecurityStore);
        builder.setSessionManager(sessionManager);

        /** Create BootstrapServer */
        return builder.build();
    }

    private void setServerWithCredentials(LeshanBootstrapServerBuilder builder) {
        try {
            if (this.contextS.getCtxServer().getKeyStoreValue() != null) {
                KeyStore keyStoreServer = this.contextS.getCtxServer().getKeyStoreValue();
                if (this.setBuilderX509(builder)) {
                    X509Certificate rootCAX509Cert = (X509Certificate) keyStoreServer.getCertificate(this.contextS.getCtxServer().getRootAlias());
                    if (rootCAX509Cert != null) {
                        X509Certificate[] trustedCertificates = new X509Certificate[1];
                        trustedCertificates[0] = rootCAX509Cert;
                        builder.setTrustedCertificates(trustedCertificates);
                    } else {
                        /** by default trust all */
                        builder.setTrustedCertificates(new X509Certificate[0]);
                    }
                } else if (this.setServerRPK(builder)) {
                    this.infoParamsServerRPK();
                } else {
                    /** by default trust all */
                    builder.setTrustedCertificates(new X509Certificate[0]);
                    log.info("Unable to load X509 files for BootStrapServer");
                    this.infoParamsServerPSK();
                }
            }
        } catch (KeyStoreException ex) {
            log.error("[{}] Unable to load X509 files server", ex.getMessage());
        }
    }

    private boolean setBuilderX509(LeshanBootstrapServerBuilder builder) {
        /**
         * For deb => KeyStorePathFile == yml or commandline: KEY_STORE_PATH_FILE
         * For idea => KeyStorePathResource == common/transport/lwm2m/src/main/resources/credentials: in LwM2MTransportContextServer: credentials/serverKeyStore.jks
         */
        try {
            X509Certificate serverCertificate = (X509Certificate) this.contextS.getCtxServer().getKeyStoreValue().getCertificate(this.contextBs.getCtxBootStrap().getBootstrapAlias());
            PrivateKey privateKey = (PrivateKey) this.contextS.getCtxServer().getKeyStoreValue().getKey(this.contextBs.getCtxBootStrap().getBootstrapAlias(), this.contextS.getCtxServer().getKeyStorePasswordServer() == null ? null : this.contextS.getCtxServer().getKeyStorePasswordServer().toCharArray());
            PublicKey publicKey = serverCertificate.getPublicKey();
            if (serverCertificate != null &&
                    privateKey != null && privateKey.getEncoded().length > 0 &&
                    publicKey != null && publicKey.getEncoded().length > 0) {
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
            log.info("Bootstrap Server uses X509 : \n X509 Certificate (Hex): [{}] \n Public Key (Hex): [{}] \n Private Key (Hex): [{}]",
                    Hex.encodeHexString(certificate.getEncoded()),
                    Hex.encodeHexString(publicKey.getEncoded()),
                    Hex.encodeHexString(privateKey.getEncoded()));
        } catch (CertificateEncodingException e) {
            log.error("", e);
        }
    }

    private boolean setServerRPK(LeshanBootstrapServerBuilder builder) {
        try {
            this.generateKeyForBootstrapRPK();
            if (this.publicKey != null && this.publicKey.getEncoded().length > 0 &&
                    this.privateKey != null && this.privateKey.getEncoded().length > 0) {
                builder.setPublicKey(this.publicKey);
                builder.setPrivateKey(this.privateKey);
                return true;
            }
        } catch (NoSuchAlgorithmException | InvalidParameterSpecException | InvalidKeySpecException e) {
            log.error("Fail create Bootstrap Server with RPK", e);
        }
        return false;
    }


    /**
     * From yml^ bootstrap
     * public_x: "${LWM2M_SERVER_PUBLIC_X_BS:993ef2b698c6a9c0c1d8be78b13a9383c0854c7c7c7a504d289b403794648183}"
     * public_y: "${LWM2M_SERVER_PUBLIC_Y_BS:267412d5fc4e5ceb2257cb7fd7f76ebdac2fa9aa100afb162e990074cc0bfaa2}"
     * private_s: "${LWM2M_SERVER_PRIVATE_S_BS:9dbdbb073fc63570693a9aaf1013414e261c571f27e27fc6a8c1c2ad9347875a}"
     */
    private void generateKeyForBootstrapRPK() throws NoSuchAlgorithmException, InvalidParameterSpecException, InvalidKeySpecException {
        /** Get Elliptic Curve Parameter spec for secp256r1 */
        AlgorithmParameters algoParameters = AlgorithmParameters.getInstance("EC");
        algoParameters.init(new ECGenParameterSpec("secp256r1"));
        ECParameterSpec parameterSpec = algoParameters.getParameterSpec(ECParameterSpec.class);
        if (this.contextBs.getCtxBootStrap().getBootstrapPublicX() != null && !this.contextBs.getCtxBootStrap().getBootstrapPublicX().isEmpty() && this.contextBs.getCtxBootStrap().getBootstrapPublicY() != null && !this.contextBs.getCtxBootStrap().getBootstrapPublicY().isEmpty()) {
            /** Get point values */
            byte[] publicX = Hex.decodeHex(this.contextBs.getCtxBootStrap().getBootstrapPublicX().toCharArray());
            byte[] publicY = Hex.decodeHex(this.contextBs.getCtxBootStrap().getBootstrapPublicY().toCharArray());
            /** Create key specs */
            KeySpec publicKeySpec = new ECPublicKeySpec(new ECPoint(new BigInteger(publicX), new BigInteger(publicY)),
                    parameterSpec);
            /** Get keys */
            this.publicKey = KeyFactory.getInstance("EC").generatePublic(publicKeySpec);
        }
        if (this.contextBs.getCtxBootStrap().getBootstrapPrivateS() != null && !this.contextBs.getCtxBootStrap().getBootstrapPrivateS().isEmpty()) {
            /** Get point values */
            byte[] privateS = Hex.decodeHex(this.contextBs.getCtxBootStrap().getBootstrapPrivateS().toCharArray());
            /** Create key specs */
            KeySpec privateKeySpec = new ECPrivateKeySpec(new BigInteger(privateS), parameterSpec);
            /** Get keys */
            this.privateKey = KeyFactory.getInstance("EC").generatePrivate(privateKeySpec);
        }
    }

    private void infoParamsServerRPK() {
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
        String privHex = Hex.encodeHexString(this.privateKey.getEncoded());
        log.info("Server uses RPK -> serverNoSecureURI : [{}], serverSecureURI : [{}], \n" +
                        "Public Key (Hex): [{}] \n" +
                        "Private Key (Hex): [{}], \n" +
                        "- public_x :  [{}] \n" +
                        "- public_y :  [{}] \n" +
                        "- private_s : [{}] \n" +
                        "- Elliptic Curve parameters  : [{}]",
                this.contextBs.getCtxBootStrap().getBootstrapHost() + ":" + this.contextBs.getCtxBootStrap().getBootstrapPortNoSec(),
                this.contextBs.getCtxBootStrap().getBootstrapHostSecurity() + ":" + this.contextBs.getCtxBootStrap().getBootstrapPortSecurity(),
                Hex.encodeHexString(this.publicKey.getEncoded()),
                Hex.encodeHexString(this.privateKey.getEncoded()),
                Hex.encodeHexString(x),
                Hex.encodeHexString(y),
                privHex.substring(privHex.length() - 64),
                params);
    }

    private void infoParamsServerPSK() {
        log.info("Server uses PSK -> serverNoSecureURI : [{}], serverSecureURI : [{}]",
                this.contextBs.getCtxBootStrap().getBootstrapHost() + ":" + this.contextBs.getCtxBootStrap().getBootstrapPortNoSec(),
                this.contextBs.getCtxBootStrap().getBootstrapHostSecurity() + ":" + this.contextBs.getCtxBootStrap().getBootstrapPortSecurity());
    }

}
