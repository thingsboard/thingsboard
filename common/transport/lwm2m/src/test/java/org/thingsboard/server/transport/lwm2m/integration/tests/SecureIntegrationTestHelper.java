/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 * 
 * Contributors:
 *     Zebra Technologies - initial API and implementation
 *******************************************************************************/

package org.thingsboard.server.transport.lwm2m.integration.tests;

import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.core.observe.ObservationStore;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig.Builder;
import org.eclipse.californium.scandium.dtls.PskPublicInformation;
import org.eclipse.californium.scandium.dtls.cipher.CipherSuite;
import org.eclipse.leshan.client.californium.LeshanClientBuilder;
import org.eclipse.leshan.client.object.Device;
import org.eclipse.leshan.client.object.Security;
import org.eclipse.leshan.client.object.Server;
import org.eclipse.leshan.client.resource.DummyInstanceEnabler;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.resource.ObjectsInitializer;
import org.eclipse.leshan.core.LwM2mId;
import org.eclipse.leshan.core.californium.EndpointFactory;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.core.util.Hex;
import org.eclipse.leshan.server.californium.LeshanServerBuilder;
import org.eclipse.leshan.server.security.*;

import javax.crypto.SecretKey;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.security.spec.*;
import java.util.List;

public class SecureIntegrationTestHelper extends IntegrationTestHelper {

    public static final String GOOD_PSK_ID = "Good_Client_identity";
    public static final byte[] GOOD_PSK_KEY = Hex.decodeHex("73656372657450534b".toCharArray());
    public static final String GOOD_ENDPOINT = "good_endpoint";
    public static final String BAD_PSK_ID = "Bad_Client_identity";
    public static final byte[] BAD_PSK_KEY = Hex.decodeHex("010101010101010101".toCharArray());
    public static final String BAD_ENDPOINT = "bad_endpoint";
    private org.thingsboard.server.transport.lwm2m.integration.tests.SinglePSKStore singlePSKStore;
    protected SecurityStore securityStore;

    public final PublicKey clientPublicKey; // client public key used for RPK
    public final PrivateKey clientPrivateKey; // client private key used for RPK
    public final PublicKey serverPublicKey; // server public key used for RPK
    public final PrivateKey serverPrivateKey; // server private key used for RPK

    // client private key used for X509
    public final PrivateKey clientPrivateKeyFromCert;
    // server private key used for X509
    public final PrivateKey serverPrivateKeyFromCert;
    // client certificate signed by rootCA with a good CN (CN start by leshan_integration_test)
    public final X509Certificate clientX509Cert;
    // client certificate signed by rootCA but with bad CN (CN does not start by leshan_integration_test)
    public final X509Certificate clientX509CertWithBadCN;
    // client certificate self-signed with a good CN (CN start by leshan_integration_test)
    public final X509Certificate clientX509CertSelfSigned;
    // client certificate signed by another CA (not rootCA) with a good CN (CN start by leshan_integration_test)
    public final X509Certificate clientX509CertNotTrusted;
    // server certificate signed by rootCA
    public final X509Certificate serverX509Cert;
    // self-signed server certificate
    public final X509Certificate serverX509CertSelfSigned;
    // rootCA used by the server
    public final X509Certificate rootCAX509Cert;
    // certificates trustedby the server (should contain rootCA)
    public final Certificate[] trustedCertificates = new Certificate[1];

    public SecureIntegrationTestHelper() {
        // create client credentials
        try {
            // Get point values
            byte[] publicX = Hex
                    .decodeHex("89c048261979208666f2bfb188be1968fc9021c416ce12828c06f4e314c167b5".toCharArray());
            byte[] publicY = Hex
                    .decodeHex("cbf1eb7587f08e01688d9ada4be859137ca49f79394bad9179326b3090967b68".toCharArray());
            byte[] privateS = Hex
                    .decodeHex("e67b68d2aaeb6550f19d98cade3ad62b39532e02e6b422e1f7ea189dabaea5d2".toCharArray());

            // Get Elliptic Curve Parameter spec for secp256r1
            AlgorithmParameters algoParameters = AlgorithmParameters.getInstance("EC");
            algoParameters.init(new ECGenParameterSpec("secp256r1"));
            ECParameterSpec parameterSpec = algoParameters.getParameterSpec(ECParameterSpec.class);

            // Create key specs
            KeySpec publicKeySpec = new ECPublicKeySpec(new ECPoint(new BigInteger(publicX), new BigInteger(publicY)),
                    parameterSpec);
            KeySpec privateKeySpec = new ECPrivateKeySpec(new BigInteger(privateS), parameterSpec);

            // Get keys
            clientPublicKey = KeyFactory.getInstance("EC").generatePublic(publicKeySpec);
            clientPrivateKey = KeyFactory.getInstance("EC").generatePrivate(privateKeySpec);

            // Get certificates from key store
            char[] clientKeyStorePwd = "client".toCharArray();
            KeyStore clientKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            try (FileInputStream clientKeyStoreFile = new FileInputStream("./credentials/clientKeyStore.jks")) {
                clientKeyStore.load(clientKeyStoreFile, clientKeyStorePwd);
            }

            clientPrivateKeyFromCert = (PrivateKey) clientKeyStore.getKey("client", clientKeyStorePwd);
            clientX509Cert = (X509Certificate) clientKeyStore.getCertificate("client");
            clientX509CertWithBadCN = (X509Certificate) clientKeyStore.getCertificate("client_bad_cn");
            clientX509CertSelfSigned = (X509Certificate) clientKeyStore.getCertificate("client_self_signed");
            clientX509CertNotTrusted = (X509Certificate) clientKeyStore.getCertificate("client_not_trusted");
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException(e);
        }

        // create server credentials
        try {
            // Get point values
            byte[] publicX = Hex
                    .decodeHex("fcc28728c123b155be410fc1c0651da374fc6ebe7f96606e90d927d188894a73".toCharArray());
            byte[] publicY = Hex
                    .decodeHex("d2ffaa73957d76984633fc1cc54d0b763ca0559a9dff9706e9f4557dacc3f52a".toCharArray());
            byte[] privateS = Hex
                    .decodeHex("1dae121ba406802ef07c193c1ee4df91115aabd79c1ed7f4c0ef7ef6a5449400".toCharArray());

            // Get Elliptic Curve Parameter spec for secp256r1
            AlgorithmParameters algoParameters = AlgorithmParameters.getInstance("EC");
            algoParameters.init(new ECGenParameterSpec("secp256r1"));
            ECParameterSpec parameterSpec = algoParameters.getParameterSpec(ECParameterSpec.class);

            // Create key specs
            KeySpec publicKeySpec = new ECPublicKeySpec(new ECPoint(new BigInteger(publicX), new BigInteger(publicY)),
                    parameterSpec);
            KeySpec privateKeySpec = new ECPrivateKeySpec(new BigInteger(privateS), parameterSpec);

            // Get keys
            serverPublicKey = KeyFactory.getInstance("EC").generatePublic(publicKeySpec);
            serverPrivateKey = KeyFactory.getInstance("EC").generatePrivate(privateKeySpec);

            // Get certificates from key store
            char[] serverKeyStorePwd = "server".toCharArray();
            KeyStore serverKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            try (FileInputStream serverKeyStoreFile = new FileInputStream("./credentials/serverKeyStore.jks")) {
                serverKeyStore.load(serverKeyStoreFile, serverKeyStorePwd);
            }

            serverPrivateKeyFromCert = (PrivateKey) serverKeyStore.getKey("server", serverKeyStorePwd);
            rootCAX509Cert = (X509Certificate) serverKeyStore.getCertificate("rootCA");
            serverX509Cert = (X509Certificate) serverKeyStore.getCertificate("server");
            serverX509CertSelfSigned = (X509Certificate) serverKeyStore.getCertificate("server_self_signed");
            trustedCertificates[0] = rootCAX509Cert;

        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void createPSKClient() {
        createPSKClient(false);
    }

    public void createPSKClientUsingQueueMode() {
        createPSKClient(true);
    }

    public void createPSKClient(boolean queueMode) {
        ObjectsInitializer initializer = new ObjectsInitializer();
        initializer.setInstancesForObject(LwM2mId.SECURITY,
                Security.psk(
                        "coaps://" + server.getSecuredAddress().getHostString() + ":"
                                + server.getSecuredAddress().getPort(),
                        12345, GOOD_PSK_ID.getBytes(StandardCharsets.UTF_8), GOOD_PSK_KEY));
        initializer.setInstancesForObject(LwM2mId.SERVER,
                new Server(12345, LIFETIME, queueMode ? BindingMode.UQ : BindingMode.U, false));
        initializer.setInstancesForObject(LwM2mId.DEVICE, new Device("Eclipse Leshan", MODEL_NUMBER, "12345", "U"));
        initializer.setDummyInstancesForObject(LwM2mId.ACCESS_CONTROL);
        List<LwM2mObjectEnabler> objects = initializer.createAll();

        InetSocketAddress clientAddress = new InetSocketAddress(InetAddress.getLoopbackAddress(), 0);
        LeshanClientBuilder builder = new LeshanClientBuilder(getCurrentEndpoint());
        builder.setLocalAddress(clientAddress.getHostString(), clientAddress.getPort());
        builder.setObjects(objects);
        builder.setDtlsConfig(
                new Builder().setSupportedCipherSuites(CipherSuite.TLS_PSK_WITH_AES_128_CCM_8));

        // set an editable PSK store for tests
        builder.setEndpointFactory(new EndpointFactory() {

            @Override
            public CoapEndpoint createUnsecuredEndpoint(InetSocketAddress address, NetworkConfig coapConfig,
                    ObservationStore store) {
                CoapEndpoint.Builder builder = new CoapEndpoint.Builder();
                builder.setInetSocketAddress(address);
                builder.setNetworkConfig(coapConfig);
                return builder.build();
            }

            @Override
            public CoapEndpoint createSecuredEndpoint(DtlsConnectorConfig dtlsConfig, NetworkConfig coapConfig,
                    ObservationStore store) {
                CoapEndpoint.Builder builder = new CoapEndpoint.Builder();
                Builder dtlsConfigBuilder = new Builder(dtlsConfig);
                if (dtlsConfig.getPskStore() != null) {
                    PskPublicInformation identity = dtlsConfig.getPskStore().getIdentity(null);
                    SecretKey key = dtlsConfig.getPskStore().getKey(identity);
                    singlePSKStore = new org.thingsboard.server.transport.lwm2m.integration.tests.SinglePSKStore(identity, key);
                    dtlsConfigBuilder.setPskStore(singlePSKStore);
                }
                builder.setConnector(new DTLSConnector(dtlsConfigBuilder.build()));
                builder.setNetworkConfig(coapConfig);
                return builder.build();
            }
        });

        // create client;
        client = builder.build();
        setupClientMonitoring();
    }

    public void setNewPsk(String identity, byte[] key) {
        if (identity != null)
            singlePSKStore.setIdentity(identity);
        if (key != null)
            singlePSKStore.setKey(key);
    }

    public void createRPKClient(boolean useServerCertificate) {
        ObjectsInitializer initializer = new ObjectsInitializer();
        initializer.setInstancesForObject(LwM2mId.SECURITY, Security.rpk(
                "coaps://" + server.getSecuredAddress().getHostString() + ":" + server.getSecuredAddress().getPort(),
                12345, clientPublicKey.getEncoded(), clientPrivateKey.getEncoded(),
                useServerCertificate ? serverX509Cert.getPublicKey().getEncoded() : serverPublicKey.getEncoded()));
        initializer.setInstancesForObject(LwM2mId.SERVER, new Server(12345, LIFETIME, BindingMode.U, false));
        initializer.setInstancesForObject(LwM2mId.DEVICE, new Device("Eclipse Leshan", MODEL_NUMBER, "12345", "U"));
        initializer.setClassForObject(LwM2mId.ACCESS_CONTROL, DummyInstanceEnabler.class);
        List<LwM2mObjectEnabler> objects = initializer.createAll();

        InetSocketAddress clientAddress = new InetSocketAddress(InetAddress.getLoopbackAddress(), 0);
        LeshanClientBuilder builder = new LeshanClientBuilder(getCurrentEndpoint());
        builder.setLocalAddress(clientAddress.getHostString(), clientAddress.getPort());
        builder.setObjects(objects);
        client = builder.build();
        setupClientMonitoring();
    }

    public void createRPKClient() {
        createRPKClient(false);
    }

    public void createX509CertClient() throws CertificateEncodingException {
        createX509CertClient(clientX509Cert, clientPrivateKeyFromCert, serverX509Cert);
    }

    public void createX509CertClient(Certificate clientCertificate) throws CertificateEncodingException {
        createX509CertClient(clientCertificate, clientPrivateKeyFromCert, serverX509Cert);
    }

    public void createX509CertClient(Certificate clientCertificate, PrivateKey privatekey)
            throws CertificateEncodingException {
        createX509CertClient(clientCertificate, privatekey, serverX509Cert);
    }

    public void createX509CertClient(Certificate clientCertificate, PrivateKey privatekey,
            Certificate serverCertificate) throws CertificateEncodingException {
        ObjectsInitializer initializer = new ObjectsInitializer();
        initializer.setInstancesForObject(LwM2mId.SECURITY,
                Security.x509(
                        "coaps://" + server.getSecuredAddress().getHostString() + ":"
                                + server.getSecuredAddress().getPort(),
                        12345, clientCertificate.getEncoded(), privatekey.getEncoded(),
                        serverCertificate.getEncoded()));
        initializer.setInstancesForObject(LwM2mId.SERVER, new Server(12345, LIFETIME, BindingMode.U, false));
        initializer.setInstancesForObject(LwM2mId.DEVICE, new Device("Eclipse Leshan", MODEL_NUMBER, "12345", "U"));
        initializer.setClassForObject(LwM2mId.ACCESS_CONTROL, DummyInstanceEnabler.class);
        List<LwM2mObjectEnabler> objects = initializer.createAll();

        InetSocketAddress clientAddress = new InetSocketAddress(InetAddress.getLoopbackAddress(), 0);
        LeshanClientBuilder builder = new LeshanClientBuilder(getCurrentEndpoint());
        builder.setLocalAddress(clientAddress.getHostString(), clientAddress.getPort());
        builder.setObjects(objects);
        client = builder.build();
        setupClientMonitoring();
    }

    @Override
    protected LeshanServerBuilder createServerBuilder() {
        LeshanServerBuilder builder = super.createServerBuilder();
        securityStore = new InMemorySecurityStore();
        builder.setSecurityStore(securityStore);
        Builder dtlsConfig = new Builder();
        dtlsConfig.setMaxRetransmissions(1);
        dtlsConfig.setRetransmissionTimeout(300);
        builder.setDtlsConfig(dtlsConfig);
        return builder;
    }

    public void createServerWithRPK() {
        LeshanServerBuilder builder = createServerBuilder();
        builder.setPublicKey(serverPublicKey);
        builder.setPrivateKey(serverPrivateKey);

        server = builder.build();
        // monitor client registration
        setupServerMonitoring();
    }

    public void createServerWithX509Cert() {
        createServerWithX509Cert(serverX509Cert);
    }

    public void createServerWithX509Cert(X509Certificate serverCertificate) {
        LeshanServerBuilder builder = createServerBuilder();
        builder.setPrivateKey(serverPrivateKeyFromCert);
        builder.setCertificateChain(new X509Certificate[] { serverCertificate });
        builder.setTrustedCertificates(trustedCertificates);
        builder.setAuthorizer(new DefaultAuthorizer(securityStore, new SecurityChecker() {
            @Override
            protected boolean matchX509Identity(String endpoint, String receivedX509CommonName,
                    String expectedX509CommonName) {
                return expectedX509CommonName.startsWith(receivedX509CommonName);
            }
        }));

        server = builder.build();
        // monitor client registration
        setupServerMonitoring();
    }

    public PublicKey getServerPublicKey() {
        return serverPublicKey;
    }

    public EditableSecurityStore getSecurityStore() {
        return (EditableSecurityStore) server.getSecurityStore();
    }

    @Override
    public void dispose() {
        getSecurityStore().remove(getCurrentEndpoint(), false);
        getSecurityStore().remove(BAD_ENDPOINT, false);
        getSecurityStore().remove(GOOD_ENDPOINT, false);
        super.dispose();
    }
}
