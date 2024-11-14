package org.thingsboard.server.transport.coap.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.elements.util.SslContextUtil;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.dtls.CertificateType;
import org.eclipse.californium.scandium.dtls.x509.SingleCertificateProvider;
import org.junit.Assert;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.CoapDeviceType;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.SaveDeviceWithCredentialsRequest;
import org.thingsboard.server.common.data.TransportPayloadType;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.common.msg.session.FeatureType;
import org.thingsboard.server.transport.coap.AbstractCoapIntegrationTest;
import org.thingsboard.server.transport.coap.x509.CertPrivateKey;
import org.thingsboard.server.transport.coap.x509.CoapClientX509Test;
import org.thingsboard.server.transport.coap.CoapTestConfigProperties;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import org.eclipse.californium.scandium.dtls.x509.StaticNewAdvancedCertificateVerifier;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = {
        "coap.enabled=true",
        "coap.dtls.enabled=true",
        "coap.dtls.credentials.type=KEYSTORE",
        "coap.dtls.credentials.keystore.store_file=coap/credentials/coapserver.jks",
        "coap.dtls.credentials.keystore.key_password=server_ks_password",
        "coap.dtls.credentials.keystore.key_alias=server",
        "device.connectivity.coaps.enabled=true",
        "service.integrations.supported=ALL",
        "transport.coap.enabled=true",
})
@Slf4j
public abstract class AbstractCoapSecurityIntegrationTest extends AbstractCoapIntegrationTest {

    protected final String CREDENTIALS_PATH = "coap/credentials/";
    protected final String CREDENTIALS_PATH_CERT_PEM = "coap/credentials/cert.pem";
    protected final String CREDENTIALS_PATH_KEY_PEM = "coap/credentials/key.pem";
    protected static final String SERVER_JKS_FOR_TEST = "coapserver";
    protected static final String SERVER_STORE_PWD = "server_ks_password";
    protected static final String SERVER_CERT_ALIAS = "server";
    protected final X509Certificate serverX509Cert;
    protected final PublicKey serverPublicKeyFromCert;
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

    protected AbstractCoapSecurityIntegrationTest() {

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

        // create server credentials
        try {
            char[] serverKeyStorePwd = SERVER_STORE_PWD.toCharArray();
            KeyStore serverKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            try (InputStream serverKeyStoreFile = this.getClass().getClassLoader().getResourceAsStream(CREDENTIALS_PATH + SERVER_JKS_FOR_TEST + ".jks")) {
                serverKeyStore.load(serverKeyStoreFile, serverKeyStorePwd);
            }
            this.serverX509Cert = (X509Certificate) serverKeyStore.getCertificate(SERVER_CERT_ALIAS);
            this.serverPublicKeyFromCert = serverX509Cert.getPublicKey();
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected Device createDeviceWithX509(CoapTestConfigProperties config, String deviceName, X509Certificate clientX509Cert) throws Exception {
        loginTenantAdmin();
        DeviceProfile deviceProfile = createCoapDeviceProfile(config);
        assertNotNull(deviceProfile);

        Device device = new Device();
        device.setName(deviceName);
        device.setType(deviceProfile.getName());
        device.setDeviceProfileId(deviceProfile.getId());

        DeviceCredentials deviceCredentials = new DeviceCredentials();
        deviceCredentials.setCredentialsType(DeviceCredentialsType.X509_CERTIFICATE);
        String pemFormatCert = convertToPEM(clientX509Cert);
        deviceCredentials.setCredentialsValue(pemFormatCert);

        SaveDeviceWithCredentialsRequest saveRequest = new SaveDeviceWithCredentialsRequest(device, deviceCredentials);
        Device deviceX509 = readResponse(doPost("/api/device-with-credentials", saveRequest).andExpect(status().isOk()), Device.class);
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

    protected void processAfterX509Test() throws Exception {
        if (clientX509 != null) {
            clientX509.disconnect();
        }
    }

    private static String convertToPEM(X509Certificate certificate) throws Exception {
        StringBuilder pemBuilder = new StringBuilder();
        pemBuilder.append("-----BEGIN CERTIFICATE-----\n");
        // Copy cert to Base64
        String base64EncodedCert = Base64.getEncoder().encodeToString(certificate.getEncoded());
        int index = 0;
        while (index < base64EncodedCert.length()) {
            pemBuilder.append(base64EncodedCert, index, Math.min(index + 64, base64EncodedCert.length()));
            pemBuilder.append("\n");
            index += 64;
        }
        pemBuilder.append("-----END CERTIFICATE-----\n");
        return pemBuilder.toString();
    }

    protected void clientX509FromJksUpdateAttributesTest() throws Exception {
        CoapTestConfigProperties configProperties = CoapTestConfigProperties.builder()
                .deviceName("Test Post Attribute device json payload, security X509 from jks")
                .coapDeviceType(CoapDeviceType.DEFAULT)
                .transportPayloadType(TransportPayloadType.JSON)
                .build();
        CertPrivateKey certPrivateKey = new CertPrivateKey(clientX509CertTrustNo, clientPrivateKeyFromCertTrustNo);
        Device deviceX509 = createDeviceWithX509(configProperties, CLIENT_ENDPOINT_X509_TRUST_NO, certPrivateKey.getCert());
        clientX509 = new CoapClientX509Test(certPrivateKey);
        CoapResponse coapResponseX509 = clientX509.postMethod(PAYLOAD_VALUES_STR.getBytes());
        assertEquals(CoAP.ResponseCode.CREATED, coapResponseX509.getCode());
        DeviceId deviceId = deviceX509.getId();
        JsonNode expectedNode = JacksonUtil.toJsonNode(PAYLOAD_VALUES_STR);
        List<String> expectedKeys = getKeysFromNode(expectedNode);
        List<String> actualKeys = getActualKeysList(deviceId, expectedKeys);
        assertNotNull(actualKeys);

        Set<String> actualKeySet = new HashSet<>(actualKeys);
        Set<String> expectedKeySet = new HashSet<>(expectedKeys);
        assertEquals(expectedKeySet, actualKeySet);

        String getAttributesValuesUrl = getAttributesValuesUrl(deviceId, actualKeySet);
        List<Map<String, Object>> actualValues = doGetAsyncTyped(getAttributesValuesUrl, new TypeReference<>() {
        });
        assertAttributesValues(actualValues, expectedNode);
    }
    protected void clientX509FromPathUpdateAttributesTest() throws Exception {
        CoapTestConfigProperties configProperties = CoapTestConfigProperties.builder()
                .deviceName("Test Post Attribute device json payload, security X509 from Path")
                .coapDeviceType(CoapDeviceType.DEFAULT)
                .transportPayloadType(TransportPayloadType.JSON)
                .build();
        CertPrivateKey certPrivateKey = new CertPrivateKey(CREDENTIALS_PATH_CERT_PEM, CREDENTIALS_PATH_KEY_PEM);
        Device deviceX509 = createDeviceWithX509(configProperties, "CoapX509TrustNoFromPath", certPrivateKey.getCert());
        clientX509 = new CoapClientX509Test(certPrivateKey, FeatureType.ATTRIBUTES);
        CoapResponse coapResponseX509 = clientX509.postMethod(PAYLOAD_VALUES_STR.getBytes());
        assertEquals(CoAP.ResponseCode.CREATED, coapResponseX509.getCode());
        DeviceId deviceId = deviceX509.getId();
        JsonNode expectedNode = JacksonUtil.toJsonNode(PAYLOAD_VALUES_STR);
        List<String> expectedKeys = getKeysFromNode(expectedNode);
        List<String> actualKeys = getActualKeysList(deviceId, expectedKeys);
        assertNotNull(actualKeys);

        Set<String> actualKeySet = new HashSet<>(actualKeys);
        Set<String> expectedKeySet = new HashSet<>(expectedKeys);
        assertEquals(expectedKeySet, actualKeySet);

        String getAttributesValuesUrl = getAttributesValuesUrl(deviceId, actualKeySet);
        List<Map<String, Object>> actualValues = doGetAsyncTyped(getAttributesValuesUrl, new TypeReference<>() {
        });
        assertAttributesValues(actualValues, expectedNode);
    }

    private List<String> getActualKeysList(DeviceId deviceId, List<String> expectedKeys) throws Exception {
        long start = System.currentTimeMillis();
        long end = System.currentTimeMillis() + 5000;

        List<String> actualKeys = null;
        while (start <= end) {
            actualKeys = doGetAsyncTyped("/api/plugins/telemetry/DEVICE/" + deviceId + "/keys/attributes/CLIENT_SCOPE", new TypeReference<>() {
            });
            if (actualKeys.size() == expectedKeys.size()) {
                break;
            }
            Thread.sleep(100);
            start += 100;
        }
        return actualKeys;
    }

    private String getAttributesValuesUrl(DeviceId deviceId, Set<String> actualKeySet) {
        return "/api/plugins/telemetry/DEVICE/" + deviceId + "/values/attributes/CLIENT_SCOPE?keys=" + String.join(",", actualKeySet);
    }

    private List getKeysFromNode(JsonNode jNode) {
        List<String> jKeys = new ArrayList<>();
        Iterator<String> fieldNames = jNode.fieldNames();
        while (fieldNames.hasNext()) {
            jKeys.add(fieldNames.next());
        }
        return jKeys;
    }

    protected void assertAttributesValues(List<Map<String, Object>> actualValues, JsonNode expectedValues) {
        for (Map<String, Object> map : actualValues) {
            String key = (String) map.get("key");
            Object actualValue = map.get("value");
            assertTrue(expectedValues.has(key));
            JsonNode expectedValue = expectedValues.get(key);
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
                    assertNotNull(actualValue);
                    assertEquals(expectedValue.size(), ((LinkedHashMap) actualValue).size());
                    break;
                default:
                    break;
            }
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

