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
import org.eclipse.leshan.server.redis.RedisRegistrationStore;
import org.eclipse.leshan.server.redis.RedisSecurityStore;
import org.eclipse.leshan.server.security.DefaultAuthorizer;
import org.eclipse.leshan.server.security.EditableSecurityStore;
import org.eclipse.leshan.server.security.SecurityChecker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thingsboard.server.transport.lwm2m.server.adaptors.LwM2MProvider;
import org.thingsboard.server.transport.lwm2m.server.adaptors.LwM2mInMemorySecurityStore;
import org.thingsboard.server.transport.lwm2m.server.credentials.LwM2MServerCredentials;
import org.thingsboard.server.transport.lwm2m.utils.LwM2mValueConverterImpl;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.util.Pool;

import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;

@Slf4j
@Configuration("LwM2MTransportServerConfiguration")
@ConditionalOnExpression("'${service.type:null}'=='tb-transport' || ('${service.type:null}'=='monolith' && '${transport.lwm2m.enabled}'=='true')")
public class LwM2MTransportServerConfiguration {

    @Autowired
    private LwM2MTransportCtx context;

    @Autowired
    private LwM2mInMemorySecurityStore lwM2mInMemorySecurityStore;

    private final static String[] modelPaths = LwM2MProvider.modelPaths;

    @Bean
    public LeshanServer getLeshanServer() throws URISyntaxException {
        log.info("Starting LwM2M transport... PostConstruct");
        LeshanServerBuilder builder = new LeshanServerBuilder();
        builder.setLocalAddress(context.getHost(), context.getPort());
        builder.setLocalSecureAddress(context.getSecureHost(), context.getSecurePort());
        builder.setEncoder(new DefaultLwM2mNodeEncoder());
        //         use a magic converter to support bad type send by the UI.
        builder.setEncoder(new DefaultLwM2mNodeEncoder(new LwM2mValueConverterImpl()));
        LwM2mNodeDecoder decoder = new DefaultLwM2mNodeDecoder();
        builder.setDecoder(decoder);

        // Create CoAP Config
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

        // Define model provider
        List<ObjectModel> models = ObjectLoader.loadDefault();
        models.addAll(ObjectLoader.loadDdfResources("/models/", modelPaths));
        String modelsFolderPath = null;
        if (modelsFolderPath != null) {
            models.addAll(ObjectLoader.loadObjectsFromDir(new File(modelsFolderPath)));
        }
        LwM2mModelProvider modelProvider = new VersionedModelProvider(models);
        builder.setObjectModelProvider(modelProvider);

        // Connect to redis if needed
        Pool<Jedis> jedis = null;
        String redisUrl =  context.getRedisUrl();
        if (!redisUrl.isEmpty()) {
            // TODO support sentinel pool and make pool configurable
            jedis = new JedisPool(new URI(redisUrl));
        }

        // Create DTLS Config
        DtlsConnectorConfig.Builder dtlsConfig = new DtlsConnectorConfig.Builder();
        boolean supportDeprecatedCiphers = false;
//        dtlsConfig.setRecommendedCipherSuitesOnly(true);

        X509Certificate serverCertificate = null;
        X509Certificate rootCAX509Cert = null;
        PublicKey publicKey = null;
        PrivateKey privateKey = null;
        // get RPK info: use RPK only
        if (context.isRpkEnabled() && !context.isX509Enabled()) {
            LwM2MServerCredentials lwM2MServerCredentials = new LwM2MServerCredentials(context.getPublicServerX(), context.getPublicServerY(), context.getPrivateServerS());
            publicKey = lwM2MServerCredentials.getServerPublicKey();
            privateKey = lwM2MServerCredentials.getServerPrivateKey();
            if (publicKey != null) {
                builder.setPublicKey(publicKey);
                builder.setPrivateKey(privateKey);
            }
        }
        // get x509 info: use x509 only
        else if (context.isX509Enabled() && !context.isRpkEnabled() && context.getKeyStorePathServer() != null  && !context.getKeyStorePathServer().isEmpty()) {
            setServerWithX509Cert (builder);
        }

        // Set DTLS Config
        builder.setDtlsConfig(dtlsConfig);

        // Set securityStore & registrationStore
        EditableSecurityStore securityStore;

        if (jedis == null) {
            securityStore = lwM2mInMemorySecurityStore;
            if (context.isX509Enabled()) {
                builder.setAuthorizer(new DefaultAuthorizer(securityStore, new SecurityChecker() {
                    @Override
                    protected boolean matchX509Identity(String endpoint, String receivedX509CommonName,
                                                        String expectedX509CommonName) {
//                    return expectedX509CommonName.startsWith(receivedX509CommonName);
                        return endpoint.startsWith(expectedX509CommonName);
                    }
                }));
            }
        } else {
            // use Redis Store
            securityStore = new RedisSecurityStore(jedis);
            builder.setRegistrationStore(new RedisRegistrationStore(jedis));
        }
        builder.setSecurityStore(securityStore);

        // use a magic converter to support bad type send by the UI.
        builder.setEncoder(new DefaultLwM2mNodeEncoder(new LwM2mValueConverterImpl()));

        // Create LWM2M server
        return builder.build();
    }

    private void setServerWithX509Cert (LeshanServerBuilder builder) {
        try (InputStream inServer = ClassLoader.getSystemResourceAsStream(context.getKeyStorePathServer())) {
            KeyStore keyStoreServer = KeyStore.getInstance(context.getKeyStoreType());
            keyStoreServer.load(inServer, context.getKeyStorePasswordServer() == null ? null : context.getKeyStorePasswordServer().toCharArray());
            X509Certificate serverCertificate = (X509Certificate) keyStoreServer.getCertificate(context.getAliasServer());
            PrivateKey privateKey = (PrivateKey) keyStoreServer.getKey(context.getAliasServer(), context.getKeyStorePasswordServer() == null ? null : context.getKeyStorePasswordServer().toCharArray());
            X509Certificate rootCAX509Cert = (X509Certificate) keyStoreServer.getCertificate(context.getRootAlias());
            Certificate[] trustedCertificates = new Certificate[1];
            trustedCertificates[0] = rootCAX509Cert;
            builder.setPrivateKey(privateKey);
            builder.setCertificateChain(new X509Certificate[]{serverCertificate});
            builder.setTrustedCertificates(trustedCertificates);
        } catch (Exception ex) {
            System.err.println("Unable to load X509 files server: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
