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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.*;
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
//        this.dtlsMode =  (this.context.getServerDtlsMode() == REDIS.code && context.getRedisUrl().isEmpty()) ? DEFAULT_MODE.code : this.context.getServerDtlsMode();
        /** Set securityStore with new registrationStore */

//        switch (LwM2MSecurityMode.fromSecurityMode(this.dtlsMode)) {
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
                    jedis = new JedisPool(new URI(context.getRedisUrl()));
                    securityStore = new RedisSecurityStore(jedis);
                    builder.setRegistrationStore(new RedisRegistrationStore(jedis));
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
                break;
            default:
                // TODO support sentinel pool and make pool configurable
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
            if (context.getServerPublicX() != null && !context.getServerPublicX().isEmpty() && context.getServerPublicY() != null && !context.getServerPublicY().isEmpty()) {
                /** Get point values */
                byte[] publicX = Hex.decodeHex(context.getServerPublicX().toCharArray());
                byte[] publicY = Hex.decodeHex(context.getServerPublicY().toCharArray());
                /** Create key specs */
                KeySpec publicKeySpec = new ECPublicKeySpec(new ECPoint(new BigInteger(publicX), new BigInteger(publicY)),
                        parameterSpec);
                /** Get keys */
                this.publicKey = KeyFactory.getInstance("EC").generatePublic(publicKeySpec);
            }
            if (context.getServerPrivateS() != null && !context.getServerPrivateS().isEmpty()) {
                /** Get point values */
                byte[] privateS = Hex.decodeHex(context.getServerPrivateS().toCharArray());
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
            KeyStore keyStoreServer = getKeyStoreServer();
            X509Certificate rootCAX509Cert = (X509Certificate) keyStoreServer.getCertificate(context.getRootAlias());
            if (rootCAX509Cert != null) {
                X509Certificate[] trustedCertificates = new X509Certificate[1];
                trustedCertificates[0] = rootCAX509Cert;
                builder.setTrustedCertificates(trustedCertificates);
            }
            else {
                /** by default trust all */
                builder.setTrustedCertificates(new X509Certificate[0]);
            }
        } catch (KeyStoreException ex) {
            log.error("[{}] Unable to load X509 files server", ex.getMessage());
        }
    }

    private KeyStore getKeyStoreServer() {
        KeyStore keyStoreServer = null;
        try (InputStream inServer = context.getKeyStorePathFile().isEmpty() ?
                ClassLoader.getSystemResourceAsStream(context.getKeyStorePathResource()) : new FileInputStream(new File(context.getKeyStorePathFile()))) {
            keyStoreServer = KeyStore.getInstance(context.getKeyStoreType());
            keyStoreServer.load(inServer, context.getKeyStorePasswordServer() == null ? null : context.getKeyStorePasswordServer().toCharArray());
            X509Certificate serverCertificate = (X509Certificate) keyStoreServer.getCertificate(context.getServerAlias());
            PrivateKey privateKey = (PrivateKey) keyStoreServer.getKey(context.getServerAlias(), context.getKeyStorePasswordServer() == null ? null : context.getKeyStorePasswordServer().toCharArray());
            this.builder.setPrivateKey(privateKey);
            this.builder.setCertificateChain(new X509Certificate[]{serverCertificate});
        } catch (Exception ex) {
            log.error("[{}] Unable to load KeyStore  files server", ex.getMessage());
        }
        return keyStoreServer;
    }

    private void getParamsPSK() {
        log.info("\nServer uses PSK -> private key : \n security key : [{}] \n serverSecureURI : [{}]",
                Hex.encodeHexString(this.privateKey.getEncoded()),
                context.getServerSecureHost() + ":" + Integer.toString(context.getServerSecurePort()));
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
            String params = ((ECPublicKey)this.publicKey).getParams().toString();
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
