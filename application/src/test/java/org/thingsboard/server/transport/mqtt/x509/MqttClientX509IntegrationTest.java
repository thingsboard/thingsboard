// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.transport.mqtt.x509;

import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.util.TestSocketUtils;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;
import org.thingsboard.server.common.data.DeviceProfileType;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.device.profile.JsonTransportPayloadConfiguration;
import org.thingsboard.server.common.data.device.profile.MqttDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.X509CertificateChainProvisionConfiguration;
import org.thingsboard.server.common.data.security.DeviceCertificateRevocation;
import org.thingsboard.server.common.data.security.DeviceCertificateRevocationRequest;
import org.thingsboard.server.common.transport.limits.TransportRateLimitService;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.transport.mqtt.AbstractMqttIntegrationTest;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedKeyManager;
import javax.net.ssl.X509TrustManager;
import java.math.BigInteger;
import java.net.Socket;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@DaoSqlTest
@TestPropertySource(properties = {
        "transport.mqtt.ssl.enabled=true",
        "transport.mqtt.ssl.credentials.type=PEM",
        "transport.mqtt.ssl.credentials.pem.cert_file=coap/credentials/server/cert.pem",
        "transport.mqtt.ssl.credentials.pem.key_file=",
        "transport.rate_limits.ip_limits_enabled=true",
        "transport.rate_limits.max_wrong_credentials_per_ip=3",
})
public class MqttClientX509IntegrationTest extends AbstractMqttIntegrationTest {

    private static final int MQTT_SSL_PORT = TestSocketUtils.findAvailableTcpPort();
    private static final int NOT_AUTHORIZED = MqttException.REASON_CODE_NOT_AUTHORIZED;

    @DynamicPropertySource
    static void sslProps(DynamicPropertyRegistry registry) {
        registry.add("transport.mqtt.ssl.bind_port", () -> MQTT_SSL_PORT);
    }

    @Autowired
    private TransportRateLimitService rateLimitService;

    private KeyPair caKeys;
    private X509Certificate caCertificate;
    private MqttClient client;

    @Before
    public void beforeTest() throws Exception {
        loginTenantAdmin();
        ipMap().clear();
        caKeys = generateKeyPair();
        caCertificate = generateCertificate("CN=Device CA " + UUID.randomUUID(), caKeys, null, caKeys.getPrivate(), true);
        createChainProvisioningProfile();
    }

    @After
    public void afterTest() throws Exception {
        if (client != null && client.isConnected()) {
            client.disconnectForcibly(1000);
        }
        ipMap().clear();
    }

    @Test
    public void givenCertificateSignedByProfileCa_whenDeviceConnects_thenItIsProvisionedAtConnect() throws Exception {
        String deviceName = "x509-provisioned-" + UUID.randomUUID();
        KeyPair deviceKeys = generateKeyPair();
        X509Certificate deviceCertificate = generateCertificate("CN=" + deviceName, deviceKeys, caCertificate, caKeys.getPrivate(), false);

        client = connect(deviceCertificate, deviceKeys.getPrivate());

        assertThat(client.isConnected()).isTrue();
        assertThat(doGet("/api/tenant/devices?deviceName=" + deviceName, Device.class).getName()).isEqualTo(deviceName);
    }

    @Test
    public void givenConnectedDevice_whenItsCertificateIsRevoked_thenItIsDisconnectedAndRefusedAfterwards() throws Exception {
        String deviceName = "x509-revoked-" + UUID.randomUUID();
        KeyPair deviceKeys = generateKeyPair();
        X509Certificate deviceCertificate = generateCertificate("CN=" + deviceName, deviceKeys, caCertificate, caKeys.getPrivate(), false);
        client = connect(deviceCertificate, deviceKeys.getPrivate());
        Device device = doGet("/api/tenant/devices?deviceName=" + deviceName, Device.class);

        doPost("/api/device/" + device.getId().getId() + "/credentials/revoke", new DeviceCertificateRevocationRequest(), DeviceCertificateRevocation.class);

        await().atMost(10, TimeUnit.SECONDS).until(() -> !client.isConnected());
        assertThatThrownBy(() -> connect(deviceCertificate, deviceKeys.getPrivate()))
                .isInstanceOfSatisfying(MqttException.class, e -> assertThat(e.getReasonCode()).isEqualTo(NOT_AUTHORIZED));
    }

    @Test
    public void givenClientWithoutPrivateKeyOfCertificate_whenConnecting_thenHandshakeFailsAndNoDeviceIsProvisioned() throws Exception {
        String deviceName = "x509-no-key-" + UUID.randomUUID();
        KeyPair deviceKeys = generateKeyPair();
        X509Certificate deviceCertificate = generateCertificate("CN=" + deviceName, deviceKeys, caCertificate, caKeys.getPrivate(), false);

        assertThatThrownBy(() -> connect(deviceCertificate, generateKeyPair().getPrivate())).isInstanceOf(MqttException.class);

        await().during(2, TimeUnit.SECONDS).atMost(3, TimeUnit.SECONDS).untilAsserted(() ->
                doGet("/api/tenant/devices?deviceName=" + deviceName).andExpect(status().isNotFound()));
    }

    @Test
    public void givenRevokedCertificate_whenItKeepsConnecting_thenTheAddressIsBlocked() throws Exception {
        String deviceName = "x509-blocked-" + UUID.randomUUID();
        KeyPair deviceKeys = generateKeyPair();
        X509Certificate deviceCertificate = generateCertificate("CN=" + deviceName, deviceKeys, caCertificate, caKeys.getPrivate(), false);
        DeviceCertificateRevocationRequest revocationRequest = new DeviceCertificateRevocationRequest();
        revocationRequest.setCertificate(toPem(deviceCertificate));
        doPost("/api/deviceCertificateRevocation", revocationRequest, DeviceCertificateRevocation.class);

        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> connect(deviceCertificate, deviceKeys.getPrivate()))
                    .isInstanceOfSatisfying(MqttException.class, e -> assertThat(e.getReasonCode()).isEqualTo(NOT_AUTHORIZED));
        }
        doGet("/api/tenant/devices?deviceName=" + deviceName).andExpect(status().isNotFound());

        // Blocked before TLS: a valid device from the same address is refused as well
        KeyPair otherKeys = generateKeyPair();
        X509Certificate otherCertificate = generateCertificate("CN=x509-other-" + UUID.randomUUID(), otherKeys, caCertificate, caKeys.getPrivate(), false);
        assertThatThrownBy(() -> connect(otherCertificate, otherKeys.getPrivate()))
                .isInstanceOfSatisfying(MqttException.class, e -> assertThat(e.getReasonCode()).isNotEqualTo(NOT_AUTHORIZED));
    }

    private MqttClient connect(X509Certificate deviceCertificate, PrivateKey privateKey) throws Exception {
        SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
        sslContext.init(new KeyManager[]{new DeviceKeyManager(new X509Certificate[]{deviceCertificate, caCertificate}, privateKey)},
                new TrustManager[]{new TrustAllManager()}, null);
        MqttConnectOptions options = new MqttConnectOptions();
        options.setSocketFactory(sslContext.getSocketFactory());
        options.setHttpsHostnameVerificationEnabled(false);
        options.setConnectionTimeout(10);
        options.setCleanSession(true);
        options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);
        MqttClient mqttClient = new MqttClient("ssl://" + MQTT_HOST + ":" + MQTT_SSL_PORT, "x509-" + UUID.randomUUID(), new MemoryPersistence());
        try {
            mqttClient.connect(options);
        } catch (MqttException e) {
            mqttClient.close(true);
            throw e;
        }
        return mqttClient;
    }

    private void createChainProvisioningProfile() {
        X509CertificateChainProvisionConfiguration provisionConfiguration = new X509CertificateChainProvisionConfiguration();
        provisionConfiguration.setProvisionDeviceSecret(toPem(caCertificate));
        provisionConfiguration.setCertificateRegExPattern("(.*)");
        provisionConfiguration.setAllowCreateNewDevicesByX509Certificate(true);
        MqttDeviceProfileTransportConfiguration transportConfiguration = new MqttDeviceProfileTransportConfiguration();
        transportConfiguration.setTransportPayloadTypeConfiguration(new JsonTransportPayloadConfiguration());
        DeviceProfileData profileData = new DeviceProfileData();
        profileData.setConfiguration(new DefaultDeviceProfileConfiguration());
        profileData.setTransportConfiguration(transportConfiguration);
        profileData.setProvisionConfiguration(provisionConfiguration);
        DeviceProfile profile = new DeviceProfile();
        profile.setName("X.509 chain " + UUID.randomUUID());
        profile.setType(DeviceProfileType.DEFAULT);
        profile.setTransportType(DeviceTransportType.MQTT);
        profile.setProvisionType(DeviceProfileProvisionType.X509_CERTIFICATE_CHAIN);
        profile.setProfileData(profileData);
        doPost("/api/deviceProfile", profile, DeviceProfile.class);
    }

    private Map<?, ?> ipMap() {
        Object target = AopTestUtils.getUltimateTargetObject(rateLimitService);
        return (Map<?, ?>) ReflectionTestUtils.getField(target, "ipMap");
    }

    private static KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(256);
        return generator.generateKeyPair();
    }

    private static X509Certificate generateCertificate(String subject, KeyPair subjectKeys, X509Certificate issuer, PrivateKey issuerKey, boolean ca) throws Exception {
        long now = System.currentTimeMillis();
        X500Name subjectName = new X500Name(subject);
        X500Name issuerName = issuer != null ? new X500Name(issuer.getSubjectX500Principal().getName()) : subjectName;
        X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(issuerName, BigInteger.valueOf(System.nanoTime()),
                new Date(now - TimeUnit.DAYS.toMillis(1)), new Date(now + TimeUnit.DAYS.toMillis(30)), subjectName, subjectKeys.getPublic());
        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(ca));
        return new JcaX509CertificateConverter().getCertificate(builder.build(new JcaContentSignerBuilder("SHA256withECDSA").build(issuerKey)));
    }

    private static String toPem(X509Certificate certificate) {
        try {
            return "-----BEGIN CERTIFICATE-----\n" + Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(certificate.getEncoded())
                    + "\n-----END CERTIFICATE-----\n";
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static class DeviceKeyManager extends X509ExtendedKeyManager {

        private final X509Certificate[] chain;
        private final PrivateKey privateKey;

        DeviceKeyManager(X509Certificate[] chain, PrivateKey privateKey) {
            this.chain = chain;
            this.privateKey = privateKey;
        }

        @Override
        public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
            return "device";
        }

        @Override
        public String chooseEngineClientAlias(String[] keyType, Principal[] issuers, SSLEngine engine) {
            return "device";
        }

        @Override
        public X509Certificate[] getCertificateChain(String alias) {
            return chain;
        }

        @Override
        public PrivateKey getPrivateKey(String alias) {
            return privateKey;
        }

        @Override
        public String[] getClientAliases(String keyType, Principal[] issuers) {
            return new String[]{"device"};
        }

        @Override
        public String[] getServerAliases(String keyType, Principal[] issuers) {
            return null;
        }

        @Override
        public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
            return null;
        }

    }

    private static class TrustAllManager implements X509TrustManager {

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }

    }

}
