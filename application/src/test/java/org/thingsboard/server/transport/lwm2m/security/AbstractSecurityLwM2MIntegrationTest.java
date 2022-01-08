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
package org.thingsboard.server.transport.lwm2m.security;

import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MBootstrapClientCredentials;
import org.thingsboard.server.common.data.device.credentials.lwm2m.NoSecBootstrapClientCredential;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.transport.lwm2m.AbstractLwM2MIntegrationTest;
import org.thingsboard.server.transport.lwm2m.client.LwM2MTestClient;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

@DaoSqlTest
public abstract class AbstractSecurityLwM2MIntegrationTest extends AbstractLwM2MIntegrationTest {

    protected final String CREDENTIALS_PATH = "lwm2m/credentials/";             // client public key or id used for PSK
    protected final String pskIdentity;             // client public key or id used for PSK
    protected final String pskKey;                  // client private/secret key used for PSK
//    protected final PublicKey clientPublicKey;      // client public key used for RPK
//    protected final PrivateKey clientPrivateKey;    // client private key used for RPK



//    // client certificate signed by rootCA but with bad CN (CN does not start by leshan_integration_test)
//    protected final X509Certificate clientX509CertWithBadCN;
//    // client certificate self-signed with a good CN (CN start by leshan_integration_test)
//    protected final X509Certificate clientX509CertSelfSigned;
//    // client certificate signed by another CA (not rootCA) with a good CN (CN start by leshan_integration_test)
//    protected final X509Certificate clientX509CertNotTrusted;

    // self-signed server certificate
//    protected final X509Certificate serverX509CertSelfSigned;
//    // rootCA used by the server
//    protected final X509Certificate rootCAX509Cert;
    // certificates trustedby the server (should contain rootCA)

    // Server
    protected static final String SERVER_JKS_FOR_TEST = "lwm2mserver";
    protected static final String SERVER_STORE_PWD = "server_ks_password";
    protected static final String SERVER_CERT_ALIAS = "server";
    protected final X509Certificate serverX509Cert;         // server certificate signed by rootCA
//    protected final PrivateKey serverPrivateKeyFromCert;    // server private key used for RPK and X509
    protected final PublicKey serverPublicKeyFromCert;      // server public key used for RPK

//    // Server Trust
//    protected final Certificate[] trustedCertificates = new Certificate[1];

    // Client
    protected LwM2MTestClient client;
    protected static final String CLIENT_ENDPOINT_NO_TRUST = "deviceAEndpoint";
    protected static final String CLIENT_ENDPOINT_TRUST = "LwX50900000000";
    protected static final String CLIENT_JKS_FOR_TEST = "lwm2mclient";
    protected static final String CLIENT_STORE_PWD = "client_ks_password";

    protected static final String CLIENT_CERT_ALIAS = "client_alias_00000000";

    protected final X509Certificate clientX509Cert;         // client certificate signed by intermediate, rootCA with a good CN ("host name")
    protected final PrivateKey clientPrivateKeyFromCert;    // client private key used for X509 and RPK
    protected final PublicKey clientPublicKeyFromCert;      // client public key used for RPK
    private final  String[] resources = new String[]{"1.xml", "2.xml", "3.xml", "5.xml", "9.xml"};


    private final LwM2MBootstrapClientCredentials defaultBootstrapCredentials;



    public AbstractSecurityLwM2MIntegrationTest() {
        // create client credentials
        setResources(this.resources);
        setEndpoint(CLIENT_ENDPOINT_NO_TRUST);
        try {
//             Get keys PSK
            this.pskIdentity = "SOME_PSK_ID";
            this.pskKey = "73656372657450534b73656372657450";

//            // Get point values
//            byte[] publicX = Hex
//                    .decodeHex("89c048261979208666f2bfb188be1968fc9021c416ce12828c06f4e314c167b5".toCharArray());
//            byte[] publicY = Hex
//                    .decodeHex("cbf1eb7587f08e01688d9ada4be859137ca49f79394bad9179326b3090967b68".toCharArray());
//            byte[] privateS = Hex
//                    .decodeHex("e67b68d2aaeb6550f19d98cade3ad62b39532e02e6b422e1f7ea189dabaea5d2".toCharArray());
//
//            // Get Elliptic Curve Parameter spec for secp256r1
//            AlgorithmParameters algoParameters = AlgorithmParameters.getInstance("EC");
//            algoParameters.init(new ECGenParameterSpec("secp256r1"));
//            ECParameterSpec parameterSpec = algoParameters.getParameterSpec(ECParameterSpec.class);
//
//            // Create key specs
//            KeySpec publicKeySpec = new ECPublicKeySpec(new ECPoint(new BigInteger(publicX), new BigInteger(publicY)),
//                    parameterSpec);
//            KeySpec privateKeySpec = new ECPrivateKeySpec(new BigInteger(privateS), parameterSpec);
//
//            // Get keys RPK
//            clientPublicKey = KeyFactory.getInstance("EC").generatePublic(publicKeySpec);
//            clientPrivateKey = KeyFactory.getInstance("EC").generatePrivate(privateKeySpec);

            // Get certificates from key store
            char[] clientKeyStorePwd = CLIENT_STORE_PWD.toCharArray();
            KeyStore clientKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            try (InputStream clientKeyStoreFile = this.getClass().getClassLoader().getResourceAsStream(CREDENTIALS_PATH + CLIENT_JKS_FOR_TEST + ".jks")) {
                clientKeyStore.load(clientKeyStoreFile, clientKeyStorePwd);
            }

            clientPrivateKeyFromCert = (PrivateKey) clientKeyStore.getKey(CLIENT_CERT_ALIAS, clientKeyStorePwd);
            clientX509Cert = (X509Certificate) clientKeyStore.getCertificate(CLIENT_CERT_ALIAS);
            clientPublicKeyFromCert = clientX509Cert.getPublicKey();

//            clientX509CertWithBadCN = (X509Certificate) clientKeyStore.getCertificate("client_bad_cn");
//            clientX509CertSelfSigned = (X509Certificate) clientKeyStore.getCertificate("client_self_signed");
//            clientX509CertNotTrusted = (X509Certificate) clientKeyStore.getCertificate("client_not_trusted");
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException(e);
        }

        // create server credentials
        try {
//            // Get point values
//            byte[] publicX = Hex
//                    .decodeHex("fcc28728c123b155be410fc1c0651da374fc6ebe7f96606e90d927d188894a73".toCharArray());
//            byte[] publicY = Hex
//                    .decodeHex("d2ffaa73957d76984633fc1cc54d0b763ca0559a9dff9706e9f4557dacc3f52a".toCharArray());
//            byte[] privateS = Hex
//                    .decodeHex("1dae121ba406802ef07c193c1ee4df91115aabd79c1ed7f4c0ef7ef6a5449400".toCharArray());
//
//            // Get Elliptic Curve Parameter spec for secp256r1
//            AlgorithmParameters algoParameters = AlgorithmParameters.getInstance("EC");
//            algoParameters.init(new ECGenParameterSpec("secp256r1"));
//            ECParameterSpec parameterSpec = algoParameters.getParameterSpec(ECParameterSpec.class);
//
//            // Create key specs
//            KeySpec publicKeySpec = new ECPublicKeySpec(new ECPoint(new BigInteger(publicX), new BigInteger(publicY)),
//                    parameterSpec);
//            KeySpec privateKeySpec = new ECPrivateKeySpec(new BigInteger(privateS), parameterSpec);
//
//            // Get keys
//            serverPublicKey = KeyFactory.getInstance("EC").generatePublic(publicKeySpec);
//            serverPrivateKey = KeyFactory.getInstance("EC").generatePrivate(privateKeySpec);


            // Get certificates from key store
            char[] serverKeyStorePwd = SERVER_STORE_PWD.toCharArray();
            KeyStore serverKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            try (InputStream serverKeyStoreFile = this.getClass().getClassLoader().getResourceAsStream(CREDENTIALS_PATH + SERVER_JKS_FOR_TEST + ".jks")) {
                serverKeyStore.load(serverKeyStoreFile, serverKeyStorePwd);
            }

//            serverPrivateKeyFromCert = (PrivateKey) serverKeyStore.getKey("server", serverKeyStorePwd);
            serverX509Cert = (X509Certificate) serverKeyStore.getCertificate(SERVER_CERT_ALIAS);
            serverPublicKeyFromCert = serverX509Cert.getPublicKey();
//            rootCAX509Cert = (X509Certificate) serverKeyStore.getCertificate("rootCA");

//            serverX509CertSelfSigned = (X509Certificate) serverKeyStore.getCertificate("server_self_signed");
//            trustedCertificates[0] = serverX509Cert;
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException(e);
        }

        defaultBootstrapCredentials = new LwM2MBootstrapClientCredentials();

        NoSecBootstrapClientCredential serverCredentials = new NoSecBootstrapClientCredential();

        defaultBootstrapCredentials.setBootstrapServer(serverCredentials);
        defaultBootstrapCredentials.setLwm2mServer(serverCredentials);
    }
}
