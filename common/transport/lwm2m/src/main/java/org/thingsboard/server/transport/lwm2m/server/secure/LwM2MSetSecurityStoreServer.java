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
package org.thingsboard.server.transport.lwm2m.server.secure;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.util.Hex;
import org.eclipse.leshan.server.californium.LeshanServerBuilder;
import org.eclipse.leshan.server.redis.RedisRegistrationStore;
import org.eclipse.leshan.server.redis.RedisSecurityStore;
import org.eclipse.leshan.server.security.DefaultAuthorizer;
import org.eclipse.leshan.server.security.EditableSecurityStore;
import org.eclipse.leshan.server.security.SecurityChecker;
import org.thingsboard.server.transport.lwm2m.secure.LwM2MSecurityMode;
import org.thingsboard.server.transport.lwm2m.server.LwM2MTransportContextServer;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.util.Pool;

import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyStore;
import java.security.PublicKey;
import java.security.PrivateKey;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.GeneralSecurityException;
import java.security.KeyStoreException;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.spec.*;
import java.util.Arrays;

import static org.thingsboard.server.transport.lwm2m.secure.LwM2MSecurityMode.*;

@Slf4j
@Data
public class LwM2MSetSecurityStoreServer {

    private KeyStore keyStore;
    private X509Certificate certificate;
    private PublicKey publicKey;
    private PrivateKey privateKey;
    private LwM2MTransportContextServer context;
    private LwM2mInMemorySecurityStore lwM2mInMemorySecurityStore;

    private LeshanServerBuilder builder;
    EditableSecurityStore securityStore;

    public LwM2MSetSecurityStoreServer(LeshanServerBuilder builder, LwM2MTransportContextServer context, LwM2mInMemorySecurityStore lwM2mInMemorySecurityStore, LwM2MSecurityMode dtlsMode) {
        this.builder = builder;
        this.context = context;
        this.lwM2mInMemorySecurityStore = lwM2mInMemorySecurityStore;
        /** Set securityStore with new registrationStore */
        switch (dtlsMode) {
            /** Use PSK only */
            case PSK:
                generatePSK_RPK();
                if (this.privateKey != null && this.privateKey.getEncoded().length > 0) {
                    builder.setPrivateKey(this.privateKey);
                    builder.setPublicKey(null);
                    getParamsPSK();
                }
                break;
            /** Use RPK only */
            case RPK:
                generatePSK_RPK();
                if (this.publicKey != null && this.publicKey.getEncoded().length > 0 &&
                        this.privateKey != null && this.privateKey.getEncoded().length > 0) {
                    builder.setPublicKey(this.publicKey);
                    builder.setPrivateKey(this.privateKey);
                    getParamsRPK();
                }
                break;
            /** Use x509 only */
            case X509:
                setServerWithX509Cert();
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
                    e.printStackTrace();
                }
                break;
            default:
        }

        /** Set securityStore with new registrationStore (if not redis)*/
        if (dtlsMode.code < REDIS.code) {
            securityStore = lwM2mInMemorySecurityStore;
            if (dtlsMode == X509) {
                builder.setAuthorizer(new DefaultAuthorizer(securityStore, new SecurityChecker() {
                    @Override
                    protected boolean matchX509Identity(String endpoint, String receivedX509CommonName,
                                                        String expectedX509CommonName) {
                        return endpoint.startsWith(expectedX509CommonName);
                    }
                }));
            }
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

    private void setServerWithX509Cert() {
        try {
            if (this.context.getCtxServer().getKeyStoreValue() != null) {
                setBuilderX509();
                X509Certificate rootCAX509Cert = (X509Certificate) this.context.getCtxServer().getKeyStoreValue().getCertificate(this.context.getCtxServer().getRootAlias());
                if (rootCAX509Cert != null) {
                    X509Certificate[] trustedCertificates = new X509Certificate[1];
                    trustedCertificates[0] = rootCAX509Cert;
                    builder.setTrustedCertificates(trustedCertificates);
                } else {
                    /** by default trust all */
                    builder.setTrustedCertificates(new X509Certificate[0]);
                }
            }
            else {
                /** by default trust all */
                this.builder.setTrustedCertificates(new X509Certificate[0]);
                log.error("Unable to load X509 files for LWM2MServer");
            }
        } catch (KeyStoreException ex) {
            log.error("[{}] Unable to load X509 files server", ex.getMessage());
        }
    }

    private void setBuilderX509() {
        /**
         * For deb => KeyStorePathFile == yml or commandline: KEY_STORE_PATH_FILE
         * For idea => KeyStorePathResource == common/transport/lwm2m/src/main/resources/credentials: in LwM2MTransportContextServer: credentials/serverKeyStore.jks
         */
        try {
            X509Certificate serverCertificate = (X509Certificate) this.context.getCtxServer().getKeyStoreValue().getCertificate(this.context.getCtxServer().getServerAlias());
            PrivateKey privateKey = (PrivateKey) this.context.getCtxServer().getKeyStoreValue().getKey(this.context.getCtxServer().getServerAlias(), this.context.getCtxServer().getKeyStorePasswordServer() == null ? null : this.context.getCtxServer().getKeyStorePasswordServer().toCharArray());
            this.builder.setPrivateKey(privateKey);
            this.builder.setCertificateChain(new X509Certificate[]{serverCertificate});
        } catch (Exception ex) {
            log.error("[{}] Unable to load KeyStore  files server", ex.getMessage());
        }
    }

    private void getParamsPSK() {
        log.info("\nServer uses PSK -> private key : \n security key : [{}] \n serverSecureURI : [{}]",
                Hex.encodeHexString(this.privateKey.getEncoded()),
                this.context.getCtxServer().getServerSecureHost() + ":" + Integer.toString(this.context.getCtxServer().getServerSecurePort()));
    }

    private void getParamsRPK() {
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
}
