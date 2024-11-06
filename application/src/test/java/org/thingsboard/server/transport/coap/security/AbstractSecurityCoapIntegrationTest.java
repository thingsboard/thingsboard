package org.thingsboard.server.transport.coap.security;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.elements.auth.X509CertPath;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.elements.exception.ConnectorException;
import org.eclipse.californium.elements.util.SslContextUtil;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.config.DtlsConfig;
import org.eclipse.californium.scandium.config.DtlsConfig.DtlsRole;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.dtls.CertificateMessage;
import org.eclipse.californium.scandium.dtls.CertificateType;
import org.eclipse.californium.scandium.dtls.cipher.CipherSuite;
import org.eclipse.californium.scandium.dtls.x509.NewAdvancedCertificateVerifier;
import org.eclipse.californium.scandium.dtls.x509.SingleCertificateProvider;
import org.eclipse.leshan.client.security.CertificateVerifierFactory;
import org.eclipse.leshan.server.californium.LwM2mPskStore;
import org.eclipse.leshan.server.californium.endpoint.CaliforniumServerEndpointsProvider;
import org.eclipse.leshan.server.californium.endpoint.coap.CoapServerProtocolProvider;
import org.eclipse.leshan.server.californium.endpoint.coaps.CoapsServerProtocolProvider;
import org.junit.Assert;
import org.mockito.Mockito;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.server.common.data.ClaimRequest;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.SaveDeviceWithCredentialsRequest;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.common.msg.session.FeatureType;
import org.thingsboard.server.dao.device.claim.ClaimResponse;
import org.thingsboard.server.dao.device.claim.ClaimResult;
import org.thingsboard.server.transport.coap.AbstractCoapIntegrationTest;
import org.thingsboard.server.transport.coap.CoapTestClient;
import org.thingsboard.server.transport.coap.CoapTestClientX509;
import org.thingsboard.server.transport.coap.CoapTestConfigProperties;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import org.eclipse.californium.scandium.dtls.x509.StaticNewAdvancedCertificateVerifier;
import org.thingsboard.server.transport.coap.client.SecureClientX509;





import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.eclipse.californium.scandium.config.DtlsConfig.DTLS_CIPHER_SUITES;
import static org.eclipse.californium.scandium.config.DtlsConfig.DTLS_RECOMMENDED_CIPHER_SUITES_ONLY;
import static org.eclipse.californium.scandium.config.DtlsConfig.DTLS_ROLE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = {
        "coap.enabled=true",
        "coap.dtls.enabled=true",
        "coap.dtls.credentials.type=KEYSTORE",
        "coap.dtls.credentials.keystore.store_file=coap/credentials/lwm2mserver.jks",
        "coap.dtls.credentials.keystore.key_password=server_ks_password",
        "coap.dtls.credentials.keystore.key_alias=server",
        "device.connectivity.coaps.enabled=true",
        "service.integrations.supported=ALL",
        "transport.coap.enabled=true",
})
@Slf4j
public abstract class AbstractSecurityCoapIntegrationTest extends AbstractCoapIntegrationTest {

    protected final String CREDENTIALS_PATH = "coap/credentials/";
    protected static final String SERVER_JKS_FOR_TEST = "lwm2mserver";
    protected static final String SERVER_STORE_PWD = "server_ks_password";
    protected static final String SERVER_CERT_ALIAS = "server";
    protected final X509Certificate serverX509Cert;
    protected final PublicKey serverPublicKeyFromCert;
    protected final X509Certificate clientX509CertTrust;                                        // client certificate signed by intermediate, rootCA with a good CN ("host name")
    protected final PrivateKey clientPrivateKeyFromCertTrust;                                   // client private key used for X509 and RPK
    protected final X509Certificate clientX509CertTrustNo;                                      // client certificate signed by intermediate, rootCA with a good CN ("host name")
    protected final PrivateKey clientPrivateKeyFromCertTrustNo;

    protected static final String CLIENT_JKS_FOR_TEST = "lwm2mclient";
    protected static final String CLIENT_STORE_PWD = "client_ks_password";
    protected static final String CLIENT_ALIAS_CERT_TRUST = "client_alias_00000000";
    protected static final String CLIENT_ALIAS_CERT_TRUST_NO = "client_alias_trust_no";
    protected static final String CLIENT_ENDPOINT_X509_TRUST = "CoapsX50900000000";
    protected static final String CLIENT_ENDPOINT_X509_TRUST_NO = "CapsX509TrustNo";
    protected CoapTestClientX509 clientX509;

    protected AbstractSecurityCoapIntegrationTest() {

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


    protected void processBeforeTestX509(CoapTestConfigProperties config) throws Exception {
        loginTenantAdmin();
        DeviceProfile deviceProfile = createCoapDeviceProfile(config);
        assertNotNull(deviceProfile);

        Device device = new Device();
        device.setName(CLIENT_ENDPOINT_X509_TRUST_NO);
        device.setType(deviceProfile.getName());
        device.setDeviceProfileId(deviceProfile.getId());

        DeviceCredentials deviceCredentials = new DeviceCredentials();
        deviceCredentials.setCredentialsType(DeviceCredentialsType.X509_CERTIFICATE);
        String pemFormatCert = convertToPEM(clientX509CertTrustNo);
        deviceCredentials.setCredentialsValue(pemFormatCert);

        SaveDeviceWithCredentialsRequest saveRequest = new SaveDeviceWithCredentialsRequest(device, deviceCredentials);
        Device savedDevice = readResponse(doPost("/api/device-with-credentials", saveRequest).andExpect(status().isOk()), Device.class);
        DeviceCredentials savedDeviceCredentials =
                doGet("/api/device/" + savedDevice.getId().getId() + "/credentials", DeviceCredentials.class);
        Assert.assertNotNull(savedDeviceCredentials);
        Assert.assertNotNull(savedDeviceCredentials.getId());
        Assert.assertEquals(savedDevice.getId(), savedDeviceCredentials.getDeviceId());
        Assert.assertEquals(DeviceCredentialsType.X509_CERTIFICATE, savedDeviceCredentials.getCredentialsType());
        accessToken =  savedDeviceCredentials.getCredentialsId();
        assertNotNull(accessToken);
    }

    protected void processAfterTestX509() throws Exception {
        if (clientX509 != null) {
            clientX509.disconnect();
        }
    }

    private static String convertToPEM(X509Certificate certificate) throws Exception {
        StringBuilder pemBuilder = new StringBuilder();
        pemBuilder.append("-----BEGIN CERTIFICATE-----\n");

        // Кодування сертифіката в Base64 та розбивка рядка на частини по 64 символи
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

    protected void processAttributesTestX509() throws Exception {
        List<String> expectedKeys = Arrays.asList("key1", "key2", "key3", "key4", "key5");
//        StaticNewAdvancedCertificateVerifier.Builder trustBuilder = StaticNewAdvancedCertificateVerifier.builder();
//        builder.setAdvancedCertificateVerifier(trustBuilder.build());
//        DtlsConnectorConfig.Builder builder = new DtlsConnectorConfig.Builder(new Configuration());
//        X509Certificate[] clientChain = new X509Certificate[]{clientX509CertTrustNo};
//        builder.setCertificateIdentityProvider(new SingleCertificateProvider(clientPrivateKeyFromCertTrustNo, clientChain, Collections.singletonList(CertificateType.X_509)));
//        DtlsConnectorConfig dtlsConnectorConfig = builder.build();
//        DTLSConnector dtlsConnector = new DTLSConnector(dtlsConnectorConfig);
        Configuration configuration = new Configuration();
        configuration.setTransient(DTLS_RECOMMENDED_CIPHER_SUITES_ONLY);
        configuration.set(DTLS_ROLE, DtlsRole.CLIENT_ONLY);
        configuration.set(DTLS_CIPHER_SUITES, Arrays.asList(CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256));
        CertificateVerifierFactory certificateVerifierFactory = new CertificateVerifierFactory();

//        DtlsConnectorConfig.Builder dtlsConfig = new DtlsConnectorConfig.Builder(configuration);


        // Створення DTLS-конектора

//        DTLSConnector dtlsConnector = new DTLSConnector(dtlsConfig);
//        clientX509 = new CoapTestClientX509(dtlsConnector, accessToken, FeatureType.ATTRIBUTES);




        // Create DTLS config
        DtlsConnectorConfig.Builder builder = new DtlsConnectorConfig.Builder(configuration);
        builder.setCertificateIdentityProvider(
                new SingleCertificateProvider(clientPrivateKeyFromCertTrustNo, new X509Certificate[]{clientX509CertTrustNo}, Collections.singletonList(CertificateType.X_509))
        );
//        setupCredentials(builder, keyStoreUriPath, keyStoreAlias, trustedAliasPattern, keyStorePassword);
        DTLSConnector dtlsConnector = new DTLSConnector(builder.build());
        clientX509 = new CoapTestClientX509(dtlsConnector, accessToken, FeatureType.ATTRIBUTES);
//        SecureClientX509 client = new SecureClientX509(dtlsConnector, host, port, clientKeys, sharedKeys);
//        CoapResponse coapResponse = clientX509.postMethod(PAYLOAD_VALUES_STR.getBytes());
//        assertEquals(CoAP.ResponseCode.CREATED, coapResponse.getCode());

        DeviceId deviceId = savedDevice.getId();
        List<String> actualKeys = getActualKeysList(deviceId, expectedKeys);
        assertNotNull(actualKeys);

        Set<String> actualKeySet = new HashSet<>(actualKeys);
        Set<String> expectedKeySet = new HashSet<>(expectedKeys);
        assertEquals(expectedKeySet, actualKeySet);

        String getAttributesValuesUrl = getAttributesValuesUrl(deviceId, actualKeySet);
        List<Map<String, Object>> values = doGetAsyncTyped(getAttributesValuesUrl, new TypeReference<>() {});
            assertAttributesValues(values, actualKeySet);
        String deleteAttributesUrl = "/api/plugins/telemetry/DEVICE/" + deviceId + "/CLIENT_SCOPE?keys=" + String.join(",", actualKeySet);
        doDelete(deleteAttributesUrl);
    }

    private List<String> getActualKeysList(DeviceId deviceId, List<String> expectedKeys) throws Exception {
        long start = System.currentTimeMillis();
        long end = System.currentTimeMillis() + 5000;

        List<String> actualKeys = null;
        while (start <= end) {
            actualKeys = doGetAsyncTyped("/api/plugins/telemetry/DEVICE/" + deviceId + "/keys/attributes/CLIENT_SCOPE", new TypeReference<>() {});
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

    protected void assertAttributesValues(List<Map<String, Object>> deviceValues, Set<String> keySet) {
        for (Map<String, Object> map : deviceValues) {
            String key = (String) map.get("key");
            Object value = map.get("value");
            assertTrue(keySet.contains(key));
            switch (key) {
                case "key1":
                    assertEquals("value1", value);
                    break;
                case "key2":
                    assertEquals(true, value);
                    break;
                case "key3":
                    assertEquals(3.0, value);
                    break;
                case "key4":
                    assertEquals(4, value);
                    break;
                case "key5":
                    assertNotNull(value);
                    assertEquals(3, ((LinkedHashMap) value).size());
                    assertEquals(42, ((LinkedHashMap) value).get("someNumber"));
                    assertEquals(Arrays.asList(1, 2, 3), ((LinkedHashMap) value).get("someArray"));
                    LinkedHashMap<String, String> someNestedObject = (LinkedHashMap) ((LinkedHashMap) value).get("someNestedObject");
                    assertEquals("value", someNestedObject.get("key"));
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
