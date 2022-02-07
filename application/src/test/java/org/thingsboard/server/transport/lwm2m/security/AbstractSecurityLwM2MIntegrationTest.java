/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
import java.security.cert.X509Certificate;

@DaoSqlTest
public abstract class AbstractSecurityLwM2MIntegrationTest extends AbstractLwM2MIntegrationTest {

    protected final String CREDENTIALS_PATH = "lwm2m/credentials/";             // client public key or id used for PSK
    //             Get keys PSK
    protected final String CLIENT_PSK_IDENTITY = "SOME_PSK_ID";                                 // client public key or id used for PSK
    protected final String CLIENT_PSK_KEY = "73656372657450534b73656372657450";                  // client private/secret key used for PSK

    // Server
    protected static final String SERVER_JKS_FOR_TEST = "lwm2mserver";
    protected static final String SERVER_STORE_PWD = "server_ks_password";
    protected static final String SERVER_CERT_ALIAS = "server";
protected final X509Certificate serverX509Cert;                                                 // server certificate signed by rootCA
    protected final PublicKey serverPublicKeyFromCert;                                          // server public key used for RPK

    // Client
    protected LwM2MTestClient client;
    protected static final String CLIENT_ENDPOINT_NO_SEC = "LwNoSec00000000";
    protected static final String CLIENT_ENDPOINT_PSK = "LwPsk00000000";
    protected static final String CLIENT_ENDPOINT_RPK = "LwRpk00000000";
    protected static final String CLIENT_ENDPOINT_X509_TRUST = "LwX50900000000";
    protected static final String CLIENT_ENDPOINT_X509_TRUST_NO = "LwX509TrustNo";
    protected static final String CLIENT_JKS_FOR_TEST = "lwm2mclient";
    protected static final String CLIENT_STORE_PWD = "client_ks_password";
    protected static final String CLIENT_ALIAS_CERT_TRUST = "client_alias_00000000";
    protected static final String CLIENT_ALIAS_CERT_TRUST_NO = "client_alias_trust_no";

    protected final X509Certificate clientX509CertTrust;         // client certificate signed by intermediate, rootCA with a good CN ("host name")
    protected final PrivateKey clientPrivateKeyFromCertTrust;    // client private key used for X509 and RPK
    protected final PublicKey clientPublicKeyFromCertTrust;      // client public key used for RPK
    protected final X509Certificate clientX509CertTrustNo;         // client certificate signed by intermediate, rootCA with a good CN ("host name")
    protected final PrivateKey clientPrivateKeyFromCertTrustNo;    // client private key used for X509 and RPK
    protected final PublicKey clientPublicKeyFromCertTrustNo;      // client public key used for RPK
    private final  String[] RESOURCES_SECURITY = new String[]{"1.xml", "2.xml", "3.xml", "5.xml", "9.xml"};


    private final LwM2MBootstrapClientCredentials defaultBootstrapCredentials;



    public AbstractSecurityLwM2MIntegrationTest() {
        // create client credentials
        setResources(this.RESOURCES_SECURITY);
        try {
            // Get certificates from key store
            char[] clientKeyStorePwd = CLIENT_STORE_PWD.toCharArray();
            KeyStore clientKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            try (InputStream clientKeyStoreFile = this.getClass().getClassLoader().getResourceAsStream(CREDENTIALS_PATH + CLIENT_JKS_FOR_TEST + ".jks")) {
                clientKeyStore.load(clientKeyStoreFile, clientKeyStorePwd);
            }
            // Trust
            clientPrivateKeyFromCertTrust = (PrivateKey) clientKeyStore.getKey(CLIENT_ALIAS_CERT_TRUST, clientKeyStorePwd);
            clientX509CertTrust = (X509Certificate) clientKeyStore.getCertificate(CLIENT_ALIAS_CERT_TRUST);
            clientPublicKeyFromCertTrust = clientX509CertTrust != null ? clientX509CertTrust.getPublicKey() : null;
            // No trust
            clientPrivateKeyFromCertTrustNo = (PrivateKey) clientKeyStore.getKey(CLIENT_ALIAS_CERT_TRUST_NO, clientKeyStorePwd);
            clientX509CertTrustNo = (X509Certificate) clientKeyStore.getCertificate(CLIENT_ALIAS_CERT_TRUST_NO);
            clientPublicKeyFromCertTrustNo = clientX509CertTrustNo != null ? clientX509CertTrustNo.getPublicKey() : null;

        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException(e);
        }

        // create server credentials
        try {
            // Get certificates from key store
            char[] serverKeyStorePwd = SERVER_STORE_PWD.toCharArray();
            KeyStore serverKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            try (InputStream serverKeyStoreFile = this.getClass().getClassLoader().getResourceAsStream(CREDENTIALS_PATH + SERVER_JKS_FOR_TEST + ".jks")) {
                serverKeyStore.load(serverKeyStoreFile, serverKeyStorePwd);
            }

            serverX509Cert = (X509Certificate) serverKeyStore.getCertificate(SERVER_CERT_ALIAS);
            serverPublicKeyFromCert = serverX509Cert.getPublicKey();
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException(e);
        }

        defaultBootstrapCredentials = new LwM2MBootstrapClientCredentials();

        NoSecBootstrapClientCredential serverCredentials = new NoSecBootstrapClientCredential();

        defaultBootstrapCredentials.setBootstrapServer(serverCredentials);
        defaultBootstrapCredentials.setLwm2mServer(serverCredentials);
    }
}
