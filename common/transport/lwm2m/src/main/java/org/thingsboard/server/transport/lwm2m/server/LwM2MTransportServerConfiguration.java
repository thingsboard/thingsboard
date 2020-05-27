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

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.leshan.core.model.ObjectLoader;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mNodeDecoder;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mNodeEncoder;
import org.eclipse.leshan.core.node.codec.LwM2mNodeDecoder;
import org.eclipse.leshan.core.util.SecurityUtil;
import org.eclipse.leshan.server.californium.LeshanServer;
import org.eclipse.leshan.server.californium.LeshanServerBuilder;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.eclipse.leshan.server.model.VersionedModelProvider;
import org.eclipse.leshan.server.redis.RedisRegistrationStore;
import org.eclipse.leshan.server.redis.RedisSecurityStore;
import org.eclipse.leshan.server.security.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.thingsboard.server.transport.lwm2m.server.adaptors.LwM2MProvider;
import org.thingsboard.server.transport.lwm2m.utils.MagicLwM2mValueConverter;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.util.Pool;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

@Slf4j
@Configuration
public class LwM2MTransportServerConfiguration {

    @Autowired
    ResourceLoader resourceLoader;

    @Value("classpath:src/main/resources/models/2048.xml")
    private Resource resData;

    @Autowired
    private LwM2MTransportCtx context;

    private final static String[] modelPaths = LwM2MProvider.modelPaths;

    @Bean
    public LeshanServer getLeshanServer() throws URISyntaxException, FileNotFoundException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException {
        log.info("Starting LwM2M transport... PostConstruct");
        LeshanServerBuilder builder = new LeshanServerBuilder();
        builder.setLocalAddress(context.getHost(), context.getPort());
        builder.setLocalSecureAddress(context.getSecureHost(), context.getSecurePort());
        builder.setEncoder(new DefaultLwM2mNodeEncoder());
        //         use a magic converter to support bad type send by the UI.
        builder.setEncoder(new DefaultLwM2mNodeEncoder(new MagicLwM2mValueConverter()));
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
        String var1 = "/models/2048.xml";
        String var3 = "/data/security.data";
        Resource resource=resourceLoader.getResource("file:"+var1);
        Resource resource1=resData;
        ClassLoader var2 = this.getClass().getClassLoader();
        InputStream in =  var2 == null ? ClassLoader.getSystemResourceAsStream(var1) : var2.getResourceAsStream(var1);
        InputStream in1 = LwM2MTransportServerConfiguration.class.getResourceAsStream(var1);
        InputStream in2 = LwM2MTransportServerConfiguration.class.getResourceAsStream(var3);
        InputStream in11 = ObjectLoader.class.getResourceAsStream(var1);
        InputStream in13 = ObjectLoader.class.getResourceAsStream(var3);
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
//        boolean supportDeprecatedCiphers = false;
        dtlsConfig.setRecommendedCipherSuitesOnly(true);

//        // get RPK info
//        PublicKey publicKey = null;
//        PrivateKey privateKey = null;
//        X509Certificate serverCertificate = null;
//        X509Certificate certificate = null;
//
//        // get X509 info
//        List<Certificate> trustStore = null;
//        String truststorePath = null;
//        if (truststorePath != null) {
//            trustStore = new ArrayList<>();
//            File input = new File(truststorePath);
//
//            // check input exists
//            if (!input.exists())
//                throw new FileNotFoundException(input.toString());
//
//            // get input files.
//            File[] files;
//            if (input.isDirectory()) {
//                files = input.listFiles();
//            } else {
//                files = new File[] { input };
//            }
//            for (File file : files) {
//                try {
//                    trustStore.add(SecurityUtil.certificate.readFromFile(file.getAbsolutePath()));
//                } catch (Exception e) {
//                    log.warn("Unable to load X509 files [{}]:[{}] ", file.getAbsolutePath(), e.getMessage());
//                }
//            }
//        }
//
//        // Get keystore parameters
//        String keyStorePath = null;
//        String keyStoreType = null;
//        String keyStorePass = null;
//        String keyStoreAlias = null;
//        String keyStoreAliasPass = null;
//
//        if (certificate != null) {
//            // use X.509 mode (+ RPK)
//            serverCertificate = certificate;
//            builder.setPrivateKey(privateKey);
//            builder.setCertificateChain(new X509Certificate[] { serverCertificate });
//        } else if (publicKey != null) {
//            // use RPK only
//            builder.setPublicKey(publicKey);
//            builder.setPrivateKey(privateKey);
//        } else if (keyStorePath != null) {
//           log.warn(
//                    "Keystore way [-ks, -ksp, -kst, -ksa, -ksap] is DEPRECATED for leshan demo and will probably be removed soon, please use [-cert, -prik, -truststore] options");
//
//            // Deprecated way : Set up X.509 mode (+ RPK)
//            try {
//                KeyStore keyStore = KeyStore.getInstance(keyStoreType);
//                try (FileInputStream fis = new FileInputStream(keyStorePath)) {
//                    keyStore.load(fis, keyStorePass == null ? null : keyStorePass.toCharArray());
//                    List<Certificate> trustedCertificates = new ArrayList<>();
//                    for (Enumeration<String> aliases = keyStore.aliases(); aliases.hasMoreElements();) {
//                        String alias = aliases.nextElement();
//                        if (keyStore.isCertificateEntry(alias)) {
//                            trustedCertificates.add(keyStore.getCertificate(alias));
//                        } else if (keyStore.isKeyEntry(alias) && alias.equals(keyStoreAlias)) {
//                            List<X509Certificate> x509CertificateChain = new ArrayList<>();
//                            Certificate[] certificateChain = keyStore.getCertificateChain(alias);
//                            if (certificateChain == null || certificateChain.length == 0) {
//                                log.error("Keystore alias must have a non-empty chain of X509Certificates.");
//                                return null;
//                            }
//
//                            for (Certificate cert : certificateChain) {
//                                if (!(cert instanceof X509Certificate)) {
//                                    log.error("Non-X.509 certificate in alias chain is not supported: [{}]", cert);
//                                    return null;
//                                }
//                                x509CertificateChain.add((X509Certificate) cert);
//                            }
//
//                            Key key = keyStore.getKey(alias,
//                                    keyStoreAliasPass == null ? new char[0] : keyStoreAliasPass.toCharArray());
//                            if (!(key instanceof PrivateKey)) {
//                                log.error("Keystore alias must have a PrivateKey entry, was [{}]",
//                                        key == null ? null : key.getClass().getName());
//                                return null;
//                            }
//                            builder.setPrivateKey((PrivateKey) key);
//                            serverCertificate = (X509Certificate) keyStore.getCertificate(alias);
//                            builder.setCertificateChain(
//                                    x509CertificateChain.toArray(new X509Certificate[x509CertificateChain.size()]));
//                        }
//                    }
//                    builder.setTrustedCertificates(
//                            trustedCertificates.toArray(new Certificate[trustedCertificates.size()]));
//                }
//            } catch (KeyStoreException | IOException e) {
//                log.error("Unable to initialize X.509. Error: [{}]", e.getMessage());
//                return null;
//            }
//        }
//
//        if (publicKey == null && serverCertificate == null) {
//            // public key or server certificated is not defined
//            // use default embedded credentials (X.509 + RPK mode)
//            try {
//                PrivateKey embeddedPrivateKey = SecurityUtil.privateKey.readFromResource("/credentials/server_privkey.der");
//                serverCertificate = SecurityUtil.certificate.readFromResource("/credentials/server_cert.der");
//                builder.setPrivateKey(embeddedPrivateKey);
//                builder.setCertificateChain(new X509Certificate[] { serverCertificate });
//            } catch (Exception e) {
//                log.error("Unable to load embedded X.509 certificate. Error: [{}]", e.getMessage());
//                return null;
//            }
//        }
//
//        // Define trust store
//        if (serverCertificate != null && keyStorePath == null) {
//            if (trustStore != null && !trustStore.isEmpty()) {
//                builder.setTrustedCertificates(trustStore.toArray(new Certificate[trustStore.size()]));
//            } else {
//                // by default trust all
//                builder.setTrustedCertificates(new X509Certificate[0]);
//            }
//        }

        // Set DTLS Config
        builder.setDtlsConfig(dtlsConfig);

        // Set securityStore & registrationStore
        EditableSecurityStore securityStore;
        if (jedis == null) {
            // use file persistence
            var1 = "data/security.data";
            resource=resourceLoader.getResource("file:"+var1);
            resource1=resData;
             var2 = this.getClass().getClassLoader();
            securityStore = new InMemorySecurityStore();
            InputStream is = this.getClass().getClassLoader().getResourceAsStream("data/security.data");
            try (ObjectInputStream in4 = new ObjectInputStream(is);) {
                SecurityInfo[] infos = (SecurityInfo[]) in4.readObject();

                if (infos != null) {
                    for (SecurityInfo info : infos) {
                        securityStore.add(info);
                    }
                    if (infos.length > 0) {
                        log.debug("{} security infos loaded", infos.length);
                    }
                }
            } catch (NonUniqueSecurityInfoException | IOException | ClassNotFoundException e) {
                log.error("Could not load security infos from file", e);
            }
        } else {
            // use Redis Store
            securityStore = new RedisSecurityStore(jedis);
            builder.setRegistrationStore(new RedisRegistrationStore(jedis));
        }
        builder.setSecurityStore(securityStore);

        // use a magic converter to support bad type send by the UI.
        builder.setEncoder(new DefaultLwM2mNodeEncoder(new MagicLwM2mValueConverter()));


        // Create LWM2M server
        return builder.build();
    }
}
