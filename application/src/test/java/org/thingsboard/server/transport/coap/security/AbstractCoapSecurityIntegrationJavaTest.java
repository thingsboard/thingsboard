/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.transport.coap.security;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.elements.util.SslContextUtil;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.dtls.CertificateType;
import org.eclipse.californium.scandium.dtls.x509.SingleCertificateProvider;
import org.eclipse.californium.scandium.dtls.x509.StaticNewAdvancedCertificateVerifier;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.CoapDeviceType;
import org.thingsboard.server.common.data.TransportPayloadType;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.msg.session.FeatureType;
import org.thingsboard.server.transport.coap.CoapTestConfigProperties;
import org.thingsboard.server.transport.coap.x509.CertPrivateKey;
import org.thingsboard.server.transport.coap.x509.CoapClientX509Test;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@Slf4j
public abstract class AbstractCoapSecurityIntegrationJavaTest {

    protected static final String PAYLOAD_VALUES_STR = "{\"key201\":\"value1\", \"key2\":true, \"key3\": 3.0, \"key4\": 4, \"key5\": {\"someNumber\": 42, \"someArray\": [1,2,3], \"someNestedObject\": {\"key\": \"value\"}}}";
    //    protected static final String PAYLOAD_VALUES_STR = "{\"temperature\":50}";
    //    private static final String COAPS_BASE_URL = "coaps://localhost:5684/api/v1/";
//    private static final String COAPS_BASE_URL = "coaps://coap.tbqa.cloud/api/v1/";
//    private static final String COAPS_BASE_URL = "coaps://34.214.198.189:5684/api/v1/";

        private static final String COAPS_BASE_URL = "coaps://coap.thingsboard.cloud/api/v1/";
    protected final String CREDENTIALS_PATH = "coap/credentials/";
    // No trust old lw - 3 sec  (create - keytoool)
//    protected final String CREDENTIALS_PATH_CERT_PEM = "coap/credentials/lw/cert.pem";
//    protected final String CREDENTIALS_PATH_KEY_PEM = "coap/credentials/lw/key.pem";
    // Trust
//    protected final String CREDENTIALS_PATH_CERT_PEM = "coap/credentials/lwTrust/cert.pem";
//    protected final String CREDENTIALS_PATH_KEY_PEM = "coap/credentials/lwTrust/key.pem";
     // git
    protected final String CREDENTIALS_PATH_CERT_PEM = "coap/credentials/git/01/cert.pem";
    protected final String CREDENTIALS_PATH_KEY_PEM = "coap/credentials/git/01/key.pem";
//   protected final String CREDENTIALS_PATH_CERT_PEM = "coap/credentials/git/Rsa/cert.pem";
//    protected final String CREDENTIALS_PATH_KEY_PEM = "coap/credentials/git/Rsa/key.pem";

    // no trust DomenTbqa (create - keytoool) - 1 sec
//    protected final String CREDENTIALS_PATH_CERT_PEM = "coap/credentials/clientDomenTbqa/cert.pem";
//    protected final String CREDENTIALS_PATH_KEY_PEM = "coap/credentials/clientDomenTbqa/key.pem";

    // no trust  (create - keytoool) - 1 sec
//    protected final String CREDENTIALS_PATH_CERT_PEM = "coap/credentials/cert.pem";
//    protected final String CREDENTIALS_PATH_KEY_PEM = "coap/credentials/key.pem";


    //    protected static final String SERVER_JKS_FOR_TEST = "coapserver";
//    protected static final String SERVER_STORE_PWD = "server_ks_password";
//    protected static final String SERVER_CERT_ALIAS = "server";
//    protected final X509Certificate serverX509Cert;
//    protected final PublicKey serverPublicKeyFromCert;
    protected final X509Certificate clientX509CertTrust;                                        // client certificate signed by intermediate, rootCA with a good CN ("host name")
    protected final PrivateKey clientPrivateKeyFromCertTrust;                                   // client private key used for X509 and RPK
    protected final X509Certificate clientX509CertTrustNo;                                      // client certificate signed by intermediate, rootCA with a good CN ("host name")
    protected final PrivateKey clientPrivateKeyFromCertTrustNo;

    protected static final String CLIENT_JKS_FOR_TEST = "coapclient";
    protected static final String CLIENT_STORE_PWD = "client_ks_password";
    protected static final String CLIENT_ALIAS_CERT_TRUST = "client_alias_00000000";
    protected static final String CLIENT_ALIAS_CERT_TRUST_NO = "client_alias_trust_no";
    protected static final String CLIENT_ENDPOINT_X509_TRUST = "LwX50900000000";
    protected static final String CLIENT_ENDPOINT_X509_TRUST_NO = "LwX509TrustNo";
    protected CoapClientX509Test clientX509;

    protected AbstractCoapSecurityIntegrationJavaTest() {

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
            // No trust
            clientPrivateKeyFromCertTrustNo = (PrivateKey) clientKeyStore.getKey(CLIENT_ALIAS_CERT_TRUST_NO, clientKeyStorePwd);
            clientX509CertTrustNo = (X509Certificate) clientKeyStore.getCertificate(CLIENT_ALIAS_CERT_TRUST_NO);
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException(e);
        }
    }


    protected void processAfterX509Test() throws Exception {
        if (clientX509 != null) {
            clientX509.disconnect();
        }
    }


    protected void clientX509FromJksUpdateAttributesTest() throws Exception {
        CoapTestConfigProperties configProperties = CoapTestConfigProperties.builder()
                .deviceName("Test Post Attribute device json payload, security X509 from jks")
                .coapDeviceType(CoapDeviceType.DEFAULT)
                .transportPayloadType(TransportPayloadType.JSON)
                .build();
        CertPrivateKey certPrivateKey = new CertPrivateKey(clientX509CertTrustNo, clientPrivateKeyFromCertTrustNo);
        clientX509 = new CoapClientX509Test(certPrivateKey, COAPS_BASE_URL);
        CoapResponse coapResponseX509 = clientX509.postMethod(PAYLOAD_VALUES_STR.getBytes());
        assertNotNull(coapResponseX509);
        assertEquals(CoAP.ResponseCode.CREATED, coapResponseX509.getCode());
    }

    protected void clientX509FromPathUpdateFeatureTypeTest(FeatureType featureType) throws Exception {
        String apiSuffix = FeatureType.ATTRIBUTES.equals(featureType) ? "attributes/CLIENT_SCOPE" : "timeseries";
        CoapTestConfigProperties configProperties = CoapTestConfigProperties.builder()
                .deviceName("Test Post Attribute device json payload, security X509 from Path")
                .coapDeviceType(CoapDeviceType.DEFAULT)
                .transportPayloadType(TransportPayloadType.JSON)
                .build();
        CertPrivateKey certPrivateKey = new CertPrivateKey(CREDENTIALS_PATH_CERT_PEM, CREDENTIALS_PATH_KEY_PEM);
        log.warn("Start connect and send post");
        System.out.println("Start connect and send post");
        clientX509 = new CoapClientX509Test(certPrivateKey, featureType, COAPS_BASE_URL);
        CoapResponse coapResponseX509 = clientX509.postMethod(PAYLOAD_VALUES_STR.getBytes());
        assertEquals(CoAP.ResponseCode.CREATED, coapResponseX509.getCode());
        log.warn("Finish, response ok");
        System.out.println("Finish, response ok");
        JsonNode expectedNode = JacksonUtil.toJsonNode(PAYLOAD_VALUES_STR);
        List<String> expectedKeys = getKeysFromNode(expectedNode);
        log.warn("finish: [{}}]", expectedKeys);
        System.out.println("Finish Response successful: " + expectedKeys);
    }


    private String getAttributesValuesUrl(DeviceId deviceId, Set<String> actualKeySet, String apiSuffix) {
        //     "/api/plugins/telemetry/DEVICE/" + deviceId + "/values/timeseries?keys=" + String.join(",", actualKeySet);
        return "/api/plugins/telemetry/DEVICE/" + deviceId + "/values/" + apiSuffix + "?keys=" + String.join(",", actualKeySet);
    }

    private List getKeysFromNode(JsonNode jNode) {
        List<String> jKeys = new ArrayList<>();
        Iterator<String> fieldNames = jNode.fieldNames();
        while (fieldNames.hasNext()) {
            jKeys.add(fieldNames.next());
        }
        return jKeys;
    }

    protected void assertValuesProto(Map<String, List<Map<String, Object>>> actualValues, JsonNode expectedValues) {
        assertTrue(actualValues.size() > 0);
        assertEquals(expectedValues.size(), actualValues.size());
        for (Map.Entry<String, List<Map<String, Object>>> entry : actualValues.entrySet()) {
            String key = entry.getKey();
            List<Map<String, Object>> tsKv = entry.getValue();
            Object actualValue = tsKv.get(0).get("value");
            assertTrue(expectedValues.has(key));
            JsonNode expectedValue = expectedValues.get(key);
            assertExpectedActualValueProto(expectedValue, actualValue);
        }
    }

    protected void assertValuesList(List<Map<String, Object>> actualValues, JsonNode expectedValues) {
        assertTrue(actualValues.size() > 0);
        assertEquals(expectedValues.size(), actualValues.size());
        for (Map<String, Object> map : actualValues) {
            String key = (String) map.get("key");
            Object actualValue = map.get("value");
            assertTrue(expectedValues.has(key));
            JsonNode expectedValue = expectedValues.get(key);
            assertExpectedActualValue(expectedValue, actualValue);
        }
    }

    protected void assertExpectedActualValueProto(JsonNode expectedValue, Object actualValue) {
        switch (expectedValue.getNodeType()) {
            case STRING:
                assertEquals(expectedValue.asText(), actualValue);
                break;
            case NUMBER:
                if (expectedValue.isInt()) {
                    Integer iActual = Integer.valueOf(actualValue.toString());
                    Integer iExpected = expectedValue.asInt();
                    assertEquals(iExpected, iActual);
                } else if (expectedValue.isLong()) {
                    Long lActual = Long.valueOf(actualValue.toString());
                    Long lExpected = expectedValue.asLong();
                    assertEquals(lExpected, lActual);
                } else if (expectedValue.isFloat() || expectedValue.isDouble()) {
                    Double dActual = Double.valueOf(actualValue.toString());
                    Double dExpected = expectedValue.asDouble();
                    assertEquals(dExpected, dActual);
                }
                break;
            case BOOLEAN:
                assertTrue(actualValue.equals("true") || actualValue.equals("false"));
                assertEquals(expectedValue.asBoolean(), actualValue.equals("true"));
                break;
            case ARRAY:
            case OBJECT:
                assertEquals(expectedValue.toString(), actualValue);
                break;
            default:
                break;
        }
    }

    protected void assertExpectedActualValue(JsonNode expectedValue, Object actualValue) {
        switch (expectedValue.getNodeType()) {
            case STRING:
                assertEquals(expectedValue.asText(), actualValue);
                break;
            case NUMBER:
                if (expectedValue.isInt()) {
                    assertEquals(expectedValue.asInt(), actualValue);
                } else if (expectedValue.isLong()) {
                    assertEquals(expectedValue.asLong(), actualValue);
                } else if (expectedValue.isFloat() || expectedValue.isDouble()) {
                    assertEquals(expectedValue.asDouble(), actualValue);
                }
                break;
            case BOOLEAN:
                assertEquals(expectedValue.asBoolean(), actualValue);
                break;
            case ARRAY:
            case OBJECT:
                expectedValue.toString().equals(JacksonUtil.toString(actualValue));
                break;
            default:
                break;
        }
    }

    private static void setupCredentials(DtlsConnectorConfig.Builder config, String keyStoreUriPath, String keyStoreAlias, String trustedAliasPattern, String keyStorePassword) {
        StaticNewAdvancedCertificateVerifier.Builder trustBuilder = StaticNewAdvancedCertificateVerifier.builder();
        try {
            SslContextUtil.Credentials serverCredentials = SslContextUtil.loadCredentials(
                    keyStoreUriPath, keyStoreAlias, keyStorePassword.toCharArray(), keyStorePassword.toCharArray());
            Certificate[] trustedCertificates = SslContextUtil.loadTrustedCertificates(
                    keyStoreUriPath, trustedAliasPattern, keyStorePassword.toCharArray());
            trustBuilder.setTrustedCertificates(trustedCertificates);
            config.setAdvancedCertificateVerifier(trustBuilder.build());
            config.setCertificateIdentityProvider(new SingleCertificateProvider(serverCredentials.getPrivateKey(), serverCredentials.getCertificateChain(), Collections.singletonList(CertificateType.X_509)));
        } catch (GeneralSecurityException e) {
            System.err.println("certificates are invalid!");
            throw new IllegalArgumentException(e.getMessage());
        } catch (IOException e) {
            System.err.println("certificates are missing!");
            throw new IllegalArgumentException(e.getMessage());
        }
    }
}

