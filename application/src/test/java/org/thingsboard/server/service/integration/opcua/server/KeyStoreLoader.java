// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.integration.opcua.server;

import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.milo.opcua.sdk.server.util.HostnameUtil;
import org.eclipse.milo.opcua.stack.core.util.SelfSignedCertificateBuilder;
import org.eclipse.milo.opcua.stack.core.util.SelfSignedCertificateGenerator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
class KeyStoreLoader {

    private static final Pattern IP_ADDR_PATTERN = Pattern.compile(
        "^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");

    private static final String SERVER_ALIAS = "server-ai";
    private static final char[] PASSWORD = "password".toCharArray();

    private X509Certificate[] serverCertificateChain;
    private X509Certificate serverCertificate;
    private KeyPair serverKeyPair;

    KeyStoreLoader load(Path baseDir) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");

        File serverKeyStore = baseDir.resolve("example-server.pfx").toFile();

        log.info("Loading KeyStore at {}", serverKeyStore);

        if (!serverKeyStore.exists()) {
            keyStore.load(null, PASSWORD);

            KeyPair keyPair = SelfSignedCertificateGenerator.generateRsaKeyPair(2048);

            String applicationUri = "urn:eclipse:milo:examples:server:" + UUID.randomUUID();

            SelfSignedCertificateBuilder builder = new SelfSignedCertificateBuilder(keyPair)
                .setCommonName("Eclipse Milo Example Server")
                .setOrganization("digitalpetri")
                .setOrganizationalUnit("dev")
                .setLocalityName("Folsom")
                .setStateName("CA")
                .setCountryCode("US")
                .setApplicationUri(applicationUri);

            // Get as many hostnames and IP addresses as we can listed in the certificate.
            Set<String> hostnames = Sets.union(
                Sets.newHashSet(HostnameUtil.getHostname()),
                HostnameUtil.getHostnames("0.0.0.0", false)
            );

            for (String hostname : hostnames) {
                if (IP_ADDR_PATTERN.matcher(hostname).matches()) {
                    builder.addIpAddress(hostname);
                } else {
                    builder.addDnsName(hostname);
                }
            }

            X509Certificate certificate = builder.build();

            keyStore.setKeyEntry(SERVER_ALIAS, keyPair.getPrivate(), PASSWORD, new X509Certificate[]{certificate});
            keyStore.store(new FileOutputStream(serverKeyStore), PASSWORD);
        } else {
            keyStore.load(new FileInputStream(serverKeyStore), PASSWORD);
        }

        Key serverPrivateKey = keyStore.getKey(SERVER_ALIAS, PASSWORD);
        if (serverPrivateKey instanceof PrivateKey) {
            serverCertificate = (X509Certificate) keyStore.getCertificate(SERVER_ALIAS);

            serverCertificateChain = Arrays.stream(keyStore.getCertificateChain(SERVER_ALIAS))
                .map(X509Certificate.class::cast)
                .toArray(X509Certificate[]::new);

            PublicKey serverPublicKey = serverCertificate.getPublicKey();
            serverKeyPair = new KeyPair(serverPublicKey, (PrivateKey) serverPrivateKey);
        }

        return this;
    }

    X509Certificate getServerCertificate() {
        return serverCertificate;
    }

    public X509Certificate[] getServerCertificateChain() {
        return serverCertificateChain;
    }

    KeyPair getServerKeyPair() {
        return serverKeyPair;
    }

}
