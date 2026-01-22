/**
 * Copyright © 2016-2026 The Thingsboard Authors
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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.junit.Assert;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.TestSocketUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.CoapDeviceType;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.SaveDeviceWithCredentialsRequest;
import org.thingsboard.server.common.data.TransportPayloadType;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.common.msg.session.FeatureType;
import org.thingsboard.server.transport.coap.AbstractCoapIntegrationTest;
import org.thingsboard.server.transport.coap.x509.CertPrivateKey;
import org.thingsboard.server.transport.coap.x509.CoapClientX509Test;
import org.thingsboard.server.transport.coap.CoapTestConfigProperties;
import org.thingsboard.server.utils.PortFinder;

import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@TestPropertySource(properties = {
        "coap.server.enabled=true",
        "coap.dtls.enabled=true",
        "coap.dtls.credentials.pem.cert_file=coap/credentials/server/cert.pem",
        "coap.dtls.x509.skip_validity_check_for_client_cert=true",
        "device.connectivity.coaps.enabled=true",
        "service.integrations.supported=ALL",
        "transport.coap.enabled=true",
})
public abstract class AbstractCoapSecurityIntegrationTest extends AbstractCoapIntegrationTest {

    public static final String COAPS_HOST = "localhost";
    public static final int COAPS_PORT = PortFinder.findAvailableUdpPort();
    public static final String COAPS_BASE_URL = "coaps://" + COAPS_HOST + ":" + COAPS_PORT + "/api/v1/";

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        log.warn("coap.dtls.bind_port = {}", COAPS_PORT);
        registry.add("coap.dtls.bind_port", () -> COAPS_PORT);
    }

    protected final String CREDENTIALS_PATH = "coap/credentials/";
    protected final String CREDENTIALS_PATH_CLIENT = CREDENTIALS_PATH + "client/";
    protected final String CREDENTIALS_PATH_CLIENT_CERT_PEM = CREDENTIALS_PATH_CLIENT + "cert.pem";
    protected final String CREDENTIALS_PATH_CLIENT_KEY_PEM = CREDENTIALS_PATH_CLIENT + "key.pem";
    protected final X509Certificate clientX509CertTrustNo;                                      // client certificate signed by intermediate, rootCA with a good CN ("host name")
    protected final PrivateKey clientPrivateKeyFromCertTrustNo;

    protected static final String CLIENT_JKS_FOR_TEST = "coapclientTest";
    protected static final String CLIENT_STORE_PWD = "client_ks_password";
    protected static final String CLIENT_ALIAS_CERT_TRUST_NO = "client_alias_trust_no";

    protected AbstractCoapSecurityIntegrationTest() {

        try {
            // Get certificates from key store
            char[] clientKeyStorePwd = CLIENT_STORE_PWD.toCharArray();
            KeyStore clientKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            try (InputStream clientKeyStoreFile =
                         this.getClass().getClassLoader().
                                 getResourceAsStream(CREDENTIALS_PATH + CLIENT_JKS_FOR_TEST + ".jks")) {
                clientKeyStore.load(clientKeyStoreFile, clientKeyStorePwd);
            }
            // No trust
            clientPrivateKeyFromCertTrustNo = (PrivateKey) clientKeyStore.getKey(CLIENT_ALIAS_CERT_TRUST_NO, clientKeyStorePwd);
            clientX509CertTrustNo = (X509Certificate) clientKeyStore.getCertificate(CLIENT_ALIAS_CERT_TRUST_NO);
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected Device createDeviceWithX509(String deviceName, DeviceProfileId deviceProfileId, X509Certificate clientX509Cert) throws Exception {
        Device device = new Device();
        device.setName(deviceName);
        device.setType(deviceName);
        device.setDeviceProfileId(deviceProfileId);

        DeviceCredentials deviceCredentials = new DeviceCredentials();
        deviceCredentials.setCredentialsType(DeviceCredentialsType.X509_CERTIFICATE);
        String pemFormatCert = CertPrivateKey.convertCertToPEM(clientX509Cert);
        deviceCredentials.setCredentialsValue(pemFormatCert);

        SaveDeviceWithCredentialsRequest saveRequest = new SaveDeviceWithCredentialsRequest(device, deviceCredentials);
        Device deviceX509 = readResponse(doPost("/api/device-with-credentials", saveRequest)
                .andExpect(status().isOk()), Device.class);
        DeviceCredentials savedDeviceCredentials =
                doGet("/api/device/" + deviceX509.getId().getId() + "/credentials", DeviceCredentials.class);
        Assert.assertNotNull(savedDeviceCredentials);
        Assert.assertNotNull(savedDeviceCredentials.getId());
        Assert.assertEquals(deviceX509.getId(), savedDeviceCredentials.getDeviceId());
        Assert.assertEquals(DeviceCredentialsType.X509_CERTIFICATE, savedDeviceCredentials.getCredentialsType());
        accessToken = savedDeviceCredentials.getCredentialsId();
        assertNotNull(accessToken);
        return deviceX509;
    }

    protected void clientX509FromJksUpdateAttributesTest() throws Exception {
        CertPrivateKey certPrivateKey = new CertPrivateKey(clientX509CertTrustNo, clientPrivateKeyFromCertTrustNo);
        CoapTestConfigProperties configProperties = CoapTestConfigProperties.builder()
                .coapDeviceType(CoapDeviceType.DEFAULT)
                .transportPayloadType(TransportPayloadType.JSON)
                .build();
        DeviceProfile deviceProfile = createCoapDeviceProfile(configProperties);
        assertNotNull(deviceProfile);
        CoapClientX509Test clientX509 = clientX509UpdateTest(FeatureType.ATTRIBUTES, certPrivateKey,
                "CoapX509TrustNo_" + FeatureType.ATTRIBUTES.name(), deviceProfile.getId(), null);
        clientX509.disconnect();
    }

    protected void clientX509FromPathUpdateFeatureTypeTest(FeatureType featureType) throws Exception {
        CertPrivateKey certPrivateKey = new CertPrivateKey(CREDENTIALS_PATH_CLIENT_CERT_PEM, CREDENTIALS_PATH_CLIENT_KEY_PEM);
        CoapTestConfigProperties configProperties = CoapTestConfigProperties.builder()
                .coapDeviceType(CoapDeviceType.DEFAULT)
                .transportPayloadType(TransportPayloadType.JSON)
                .build();
        DeviceProfile deviceProfile = createCoapDeviceProfile(configProperties);
        assertNotNull(deviceProfile);
        CoapClientX509Test clientX509 = clientX509UpdateTest(featureType, certPrivateKey,
                "CoapX509TrustNo_" + featureType.name(), deviceProfile.getId(), null);
        clientX509.disconnect();
    }
    protected void twoClientWithSamePortX509FromPathConnectTest() throws Exception {
        CoapTestConfigProperties configProperties = CoapTestConfigProperties.builder()
                .coapDeviceType(CoapDeviceType.DEFAULT)
                .transportPayloadType(TransportPayloadType.JSON)
                .build();
        DeviceProfile deviceProfile = createCoapDeviceProfile(configProperties);
        CertPrivateKey certPrivateKey = new CertPrivateKey(CREDENTIALS_PATH_CLIENT_CERT_PEM, CREDENTIALS_PATH_CLIENT_KEY_PEM);
        CertPrivateKey certPrivateKey_01 = new CertPrivateKey(CREDENTIALS_PATH_CLIENT + "cert_01.pem",
                CREDENTIALS_PATH_CLIENT + "key_01.pem");
        int fixedPort =  PortFinder.findAvailableUdpPort();
        CoapClientX509Test clientX509 = clientX509UpdateTest(FeatureType.ATTRIBUTES, certPrivateKey,
                "CoapX509TrustNo_" + FeatureType.TELEMETRY.name(), deviceProfile.getId(), fixedPort);
        clientX509.disconnect();
        await("Need to make port " + fixedPort + " free")
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> isUDPPortAvailable(fixedPort));
        CoapClientX509Test clientX509_01 = clientX509UpdateTest(FeatureType.ATTRIBUTES, certPrivateKey_01,
                "CoapX509TrustNo_" + FeatureType.TELEMETRY.name() + "_01", deviceProfile.getId(),
                fixedPort, PAYLOAD_VALUES_STR_01);
        clientX509_01.disconnect();
        await("Await to make port " + fixedPort + " free")
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> isUDPPortAvailable(fixedPort));
    }

    private CoapClientX509Test clientX509UpdateTest(FeatureType featureType, CertPrivateKey certPrivateKey,
                            String deviceName, DeviceProfileId deviceProfileId, Integer fixedPort) throws Exception {
        return clientX509UpdateTest(featureType, certPrivateKey, deviceName, deviceProfileId, fixedPort, null);
    }

    private CoapClientX509Test clientX509UpdateTest(FeatureType featureType, CertPrivateKey certPrivateKey,
              String deviceName, DeviceProfileId deviceProfileId, Integer fixedPort, String payload) throws Exception {
        String payloadValuesStr = payload == null ? PAYLOAD_VALUES_STR : payload;
        Device deviceX509 = createDeviceWithX509(deviceName, deviceProfileId, certPrivateKey.getCert());
        CoapClientX509Test clientX509 = new CoapClientX509Test(certPrivateKey, featureType, COAPS_BASE_URL, fixedPort);
        CoapResponse coapResponseX509 = clientX509.postMethod(payloadValuesStr);
        assertNotNull(coapResponseX509);
        assertEquals(CoAP.ResponseCode.CREATED, coapResponseX509.getCode());

        if (FeatureType.ATTRIBUTES.equals(featureType)) {
            DeviceId deviceId = deviceX509.getId();
            JsonNode expectedNode = JacksonUtil.toJsonNode(payloadValuesStr);
            List<String> expectedKeys = getKeysFromNode(expectedNode);
            List<String> actualKeys = getActualKeysList(deviceId, expectedKeys, "attributes/CLIENT_SCOPE");
            assertNotNull(actualKeys);

            Set<String> actualKeySet = new HashSet<>(actualKeys);
            Set<String> expectedKeySet = new HashSet<>(expectedKeys);
            assertEquals(expectedKeySet, actualKeySet);

            String getAttributesValuesUrl = getAttributesValuesUrl(deviceId, actualKeySet, "attributes/CLIENT_SCOPE");
            List<Map<String, Object>> actualValues = doGetAsyncTyped(getAttributesValuesUrl, new TypeReference<>() {
            });
            assertValuesList(actualValues, expectedNode);
        }
        return clientX509;
    }

    private List<String> getActualKeysList(DeviceId deviceId, List<String> expectedKeys, String apiSuffix) throws Exception {
        long start = System.currentTimeMillis();
        long end = System.currentTimeMillis() + 5000;

        List<String> actualKeys = null;
        while (start <= end) {
            actualKeys = doGetAsyncTyped("/api/plugins/telemetry/DEVICE/" + deviceId + "/keys/" + apiSuffix, new TypeReference<>() {
            });
            if (actualKeys.size() == expectedKeys.size()) {
                break;
            }
            Thread.sleep(100);
            start += 100;
        }
        return actualKeys;
    }

    private String getAttributesValuesUrl(DeviceId deviceId, Set<String> actualKeySet, String apiSuffix) {
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

    private static boolean isUDPPortAvailable(int port) {
        try (DatagramSocket socket = new DatagramSocket(port)) {
            socket.setReuseAddress(true);
            return true;
        } catch (IOException e) {
            log.warn("Failed to open UDP port on port " + port, e);
            return false;
        }
    }
}

