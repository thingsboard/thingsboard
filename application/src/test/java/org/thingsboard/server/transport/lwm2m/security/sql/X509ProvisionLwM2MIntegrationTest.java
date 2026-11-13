// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.lwm2m.security.sql;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.config.DtlsConfig;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.dtls.CertificateMessage;
import org.eclipse.californium.scandium.dtls.ConnectionId;
import org.eclipse.californium.scandium.dtls.x509.SingleCertificateProvider;
import org.eclipse.californium.scandium.dtls.x509.StaticNewAdvancedCertificateVerifier;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MDeviceCredentials;
import org.thingsboard.server.common.data.device.credentials.lwm2m.X509ClientCredential;
import org.thingsboard.server.common.data.device.profile.DisabledDeviceProfileProvisionConfiguration;
import org.thingsboard.server.common.data.device.profile.MqttDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.X509CertificateChainProvisionConfiguration;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.thingsboard.server.common.transport.util.SslUtil;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.device.provision.ProvisionRequest;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.lwm2m.server.LwM2mTransportContext;
import org.thingsboard.server.transport.lwm2m.secure.TbLwM2MDtlsCertificateVerifier;
import org.thingsboard.server.transport.lwm2m.security.AbstractSecurityLwM2MIntegrationTest;
import org.thingsboard.server.transport.mqtt.MqttSslHandlerProvider;

import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.math.BigInteger;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MProfileBootstrapConfigType.NONE;

/** Uses the real transport queue, provisioning service and isolated SQL database. */
public class X509ProvisionLwM2MIntegrationTest extends AbstractSecurityLwM2MIntegrationTest {

    @Autowired
    private TransportService transportService;

    @MockitoSpyBean
    private DeviceService deviceService;

    @Autowired
    private DeviceCredentialsService deviceCredentialsService;

    @Autowired
    private LwM2mTransportContext transportContext;

    @Autowired
    private TbLwM2MDtlsCertificateVerifier certificateVerifier;

    private KeyPair caKey;
    private KeyPair leafKey;
    private X509Certificate ca;
    private X509Certificate leaf;
    private String endpoint;
    private String deviceName;
    private DeviceProfile profile;

    @Before
    public void prepareCertificatesAndProfile() throws Exception {
        deviceName = "provision-" + UUID.randomUUID();
        endpoint = deviceName + ".example.test";
        caKey = keyPair();
        leafKey = keyPair();
        KeyPair rootKey = keyPair();
        X509Certificate root = certificate("test-root", rootKey, null, rootKey, true, false);
        ca = certificate("test-ca-" + UUID.randomUUID(), caKey, root, rootKey, true, false);
        leaf = certificate(endpoint, leafKey, ca, caKey, false, false);
        profile = createLwm2mDeviceProfile("profile-" + UUID.randomUUID(),
                getTransportConfiguration(OBSERVE_ATTRIBUTES_WITHOUT_PARAMS, getBootstrapServerCredentialsNoSec(NONE)));
        X509CertificateChainProvisionConfiguration provision = new X509CertificateChainProvisionConfiguration();
        provision.setProvisionDeviceSecret(SslUtil.getCertificateString(ca));
        provision.setCertificateRegExPattern("(.+)\\.example\\.test");
        provision.setAllowCreateNewDevicesByX509Certificate(true);
        profile.setProvisionType(DeviceProfileProvisionType.X509_CERTIFICATE_CHAIN);
        profile.getProfileData().setProvisionConfiguration(provision);
        saveProfile();
    }

    @Test
    public void shouldProvisionDuringDtlsAndRegisterAgainWithStoredCredentials() throws Exception {
        assertNull(findDevice());
        assertEquals(CoAP.ResponseCode.CREATED, register(endpoint).getCode());
        Device device = findDevice();
        assertNotNull(device);
        assertEquals(profile.getId(), device.getDeviceProfileId());
        DeviceCredentials credentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, device.getId());
        assertEquals(DeviceCredentialsType.LWM2M_CREDENTIALS, credentials.getCredentialsType());
        LwM2MDeviceCredentials lwm2m = JacksonUtil.fromString(credentials.getCredentialsValue(), LwM2MDeviceCredentials.class);
        assertTrue(lwm2m.getClient() instanceof X509ClientCredential);
        assertEquals(endpoint, lwm2m.getClient().getEndpoint());
        assertTrue("The provisioned credentials must pin the leaf certificate",
                SslUtil.getCertificateString(leaf).equals(((X509ClientCredential) lwm2m.getClient()).getCert()));
        assertNotNull(lwm2m.getBootstrap());
        assertEquals(CoAP.ResponseCode.CREATED, register(endpoint).getCode());
        assertEquals(device.getId(), findDevice().getId());
        assertTrue(credentials.equals(deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, device.getId())));
    }

    @Test
    public void shouldRejectRegistrationEndpointDifferentFromCertificateCn() throws Exception {
        assertFalse(register(deviceName).isSuccess());
        assertNull(transportContext.getServer().getRegistrationService().getByEndpoint(deviceName));
    }

    @Test
    public void shouldProvisionAfterListenerTrustValidationSucceeds() throws Exception {
        Object previous = ReflectionTestUtils.getField(certificateVerifier, "staticCertificateVerifier");
        try {
            ReflectionTestUtils.setField(certificateVerifier, "staticCertificateVerifier",
                    StaticNewAdvancedCertificateVerifier.builder().setTrustedCertificates(ca).build());
            assertEquals(CoAP.ResponseCode.CREATED, register(endpoint).getCode());
            assertNotNull(findDevice());
        } finally {
            ReflectionTestUtils.setField(certificateVerifier, "staticCertificateVerifier", previous);
        }
    }

    @Test
    public void shouldProvisionRsaClientCertificateDuringDtls() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        leafKey = generator.generateKeyPair();
        leaf = certificate(endpoint, leafKey, ca, caKey, false, false);
        assertEquals(CoAP.ResponseCode.CREATED, register(endpoint).getCode());
        Device device = findDevice();
        assertNotNull(device);
        DeviceCredentials credentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, device.getId());
        assertEquals(DeviceCredentialsType.LWM2M_CREDENTIALS, credentials.getCredentialsType());
        LwM2MDeviceCredentials lwm2m = JacksonUtil.fromString(credentials.getCredentialsValue(), LwM2MDeviceCredentials.class);
        assertTrue(SslUtil.getCertificateString(leaf).equals(((X509ClientCredential) lwm2m.getClient()).getCert()));
        assertEquals(CoAP.ResponseCode.CREATED, register(endpoint).getCode());
        assertEquals(device.getId(), findDevice().getId());
    }

    @Test
    public void shouldRejectUnknownClientDuringDtlsWhenCreationDisabled() throws Exception {
        configuration().setAllowCreateNewDevicesByX509Certificate(false);
        saveProfile();
        assertThrows(IOException.class, () -> register(endpoint));
        assertNull(findDevice());
    }

    @Test
    public void shouldRejectUnknownClientWhenCreationDisabled() throws Exception {
        configuration().setAllowCreateNewDevicesByX509Certificate(false);
        saveProfile();
        assertRejected(leaf, ca);
    }

    @Test
    public void shouldRejectClientSignedByUnauthorizedCaDuringDtls() throws Exception {
        KeyPair otherRootKey = keyPair();
        X509Certificate otherRoot = certificate("untrusted-root", otherRootKey, null, otherRootKey, true, false);
        KeyPair otherKey = keyPair();
        X509Certificate otherCa = certificate("untrusted-ca", otherKey, otherRoot, otherRootKey, true, false);
        leaf = certificate(endpoint, leafKey, otherCa, otherKey, false, false);
        assertThrows(IOException.class, () -> register(endpoint, leaf, otherCa));
        assertNull(findDevice());
    }

    @Test
    public void shouldRejectInvalidCertificateSignatureDuringDtls() throws Exception {
        leaf = certificate(endpoint, leafKey, ca, keyPair(), false, false);
        assertThrows(IOException.class, () -> register(endpoint));
        assertNull(findDevice());
    }

    @Test
    public void shouldRejectExpiredLeafDuringDtls() throws Exception {
        leaf = certificate(endpoint, leafKey, ca, caKey, false, true);
        assertThrows(IOException.class, () -> register(endpoint));
        assertNull(findDevice());
    }

    @Test
    public void shouldRejectServerOnlyCertificateBeforeProvisioning() throws Exception {
        leaf = certificate(endpoint, leafKey, ca, caKey, false, false, KeyPurposeId.id_kp_serverAuth);
        // The client connector rejects this usage locally, so invoke the actual server verifier.
        var result = certificateVerifier.verifyCertificate(new ConnectionId(new byte[]{1}), null,
                new InetSocketAddress("127.0.0.1", 0), true, false, false, new CertificateMessage(List.of(leaf, ca)));
        assertNotNull(result.getException());
        assertNull(findDevice());
    }

    @Test
    public void shouldRejectInvalidNameExtraction() throws Exception {
        for (String regex : List.of("does-not-match-(.+)", "()", "[", ".+")) {
            configuration().setCertificateRegExPattern(regex);
            saveProfile();
            assertRejected(leaf, ca);
        }
    }

    @Test
    public void shouldRejectDisabledStrategy() throws Exception {
        profile.setProvisionType(DeviceProfileProvisionType.DISABLED);
        profile.getProfileData().setProvisionConfiguration(
                new DisabledDeviceProfileProvisionConfiguration(null));
        saveProfile();
        assertRejected(leaf, ca);
    }

    @Test
    public void shouldPreserveExistingIncompatibleCredentialsAndProfile() throws Exception {
        Device device = new Device();
        device.setTenantId(tenantId);
        device.setName(deviceName);
        device.setDeviceProfileId(profile.getId());
        device = deviceService.saveDevice(device);
        DeviceCredentials original = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, device.getId());
        assertFalse(provision(leaf, ca).hasDeviceInfo());
        assertTrue(original.equals(deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, device.getId())));

        DeviceProfile other = createLwm2mDeviceProfile("other-" + UUID.randomUUID(),
                getTransportConfiguration(OBSERVE_ATTRIBUTES_WITHOUT_PARAMS, getBootstrapServerCredentialsNoSec(NONE)));
        device.setDeviceProfileId(other.getId());
        deviceService.saveDevice(device);
        assertFalse(provision(leaf, ca).hasDeviceInfo());
        assertEquals(other.getId(), findDevice().getDeviceProfileId());
    }

    @Test
    public void shouldConvergeOnOneDeviceForConcurrentFirstConnections() throws Exception {
        synchronizeDeviceCreation(4);
        var executor = Executors.newFixedThreadPool(4);
        try {
            List<CompletableFuture<ValidateDeviceCredentialsResponse>> requests = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                requests.add(CompletableFuture.supplyAsync(() -> {
                    try {
                        return provision(leaf, ca);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }, executor));
            }
            List<ValidateDeviceCredentialsResponse> accepted = new ArrayList<>();
            for (var request : requests) {
                try {
                    ValidateDeviceCredentialsResponse response = request.get(30, TimeUnit.SECONDS);
                    if (response.hasDeviceInfo()) {
                        accepted.add(response);
                    }
                } catch (ExecutionException e) {
                    // A competing first request may lose the database uniqueness race and retry.
                }
            }
            assertFalse("At least one first request must create the device", accepted.isEmpty());
            Device device = findDevice();
            assertNotNull(device);
            assertEquals(profile.getId(), device.getDeviceProfileId());
            DeviceCredentials credentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, device.getId());
            assertNotNull(credentials);
            assertEquals(DeviceCredentialsType.LWM2M_CREDENTIALS, credentials.getCredentialsType());
            LwM2MDeviceCredentials lwm2m = JacksonUtil.fromString(credentials.getCredentialsValue(), LwM2MDeviceCredentials.class);
            assertEquals(endpoint, lwm2m.getClient().getEndpoint());
            assertTrue(lwm2m.getClient() instanceof X509ClientCredential);
            assertEquals(SslUtil.getCertificateString(leaf), ((X509ClientCredential) lwm2m.getClient()).getCert());
            assertNotNull(lwm2m.getBootstrap());
            for (ValidateDeviceCredentialsResponse response : accepted) {
                assertEquals(device.getId(), response.getDeviceInfo().getDeviceId());
            }
            for (int i = 0; i < requests.size(); i++) {
                ValidateDeviceCredentialsResponse retry = provision(leaf, ca);
                assertTrue(retry.hasDeviceInfo());
                assertEquals(device.getId(), retry.getDeviceInfo().getDeviceId());
            }
            assertEquals(credentials, deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, device.getId()));
            assertEquals(device.getId(), findDevice().getId());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void shouldProvisionOnlyInTheTenantAuthorizedByTheProfileCa() throws Exception {
        createDifferentTenant();
        Device other = new Device();
        other.setTenantId(differentTenantId);
        other.setName(deviceName);
        other = deviceService.saveDevice(other);
        DeviceCredentials original = deviceCredentialsService.findDeviceCredentialsByDeviceId(differentTenantId, other.getId());
        loginTenantAdmin();

        assertTrue(provision(leaf, ca).hasDeviceInfo());
        assertEquals(tenantId, findDevice().getTenantId());
        assertEquals(profile.getId(), findDevice().getDeviceProfileId());
        assertTrue(original.equals(deviceCredentialsService.findDeviceCredentialsByDeviceId(differentTenantId, other.getId())));
        assertEquals(other.getId(), deviceService.findDeviceByTenantIdAndName(differentTenantId, deviceName).getId());
    }

    @Test
    public void shouldRejectAnotherCertificateMappingToTheSameDeviceName() throws Exception {
        assertTrue(provision(leaf, ca).hasDeviceInfo());
        Device device = findDevice();
        DeviceCredentials original = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, device.getId());
        assertFalse(provision(certificate(endpoint, keyPair(), ca, caKey, false, false), ca).hasDeviceInfo());
        assertTrue(original.equals(deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, device.getId())));
    }

    @Test
    public void shouldRollBackDeviceCreationWhenCertificateAlreadyBelongsToAnotherDevice() throws Exception {
        Device original = new Device();
        original.setTenantId(tenantId);
        original.setName("existing-" + UUID.randomUUID());
        DeviceCredentials credentials = new DeviceCredentials();
        credentials.setCredentialsType(DeviceCredentialsType.X509_CERTIFICATE);
        credentials.setCredentialsValue(SslUtil.getCertificateString(leaf));
        original = deviceService.saveDeviceWithCredentials(original, credentials);
        assertRejected(leaf, ca);
        assertEquals(original.getId(), deviceService.findDeviceByTenantIdAndName(tenantId, original.getName()).getId());
        assertEquals(DeviceCredentialsType.X509_CERTIFICATE,
                deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, original.getId()).getCredentialsType());
    }

    @Test
    public void shouldKeepMqttCertificateChainProvisioningWorking() throws Exception {
        profile.setTransportType(DeviceTransportType.MQTT);
        profile.getProfileData().setTransportConfiguration(new MqttDeviceProfileTransportConfiguration());
        saveProfile();
        assertRejected(leaf, ca);
        CompletableFuture<ValidateDeviceCredentialsResponse> result = new CompletableFuture<>();
        transportService.process(DeviceTransportType.MQTT, TransportProtos.ValidateOrCreateDeviceX509CertRequestMsg.newBuilder()
                .setCertificateChain(SslUtil.getCertificateChainString(new X509Certificate[]{leaf, ca})).build(), callback(result));
        assertTrue(result.get(30, TimeUnit.SECONDS).hasDeviceInfo());
        assertEquals(DeviceCredentialsType.X509_CERTIFICATE,
                deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, findDevice().getId()).getCredentialsType());
    }

    @Test
    public void shouldKeepExistingMqttRsaCertificateChainProvisioningWorking() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        caKey = generator.generateKeyPair();
        leafKey = generator.generateKeyPair();
        ca = certificate("rsa-ca-" + UUID.randomUUID(), caKey, null, caKey, true, false);
        leaf = certificate(endpoint, leafKey, ca, caKey, false, false);
        configuration().setProvisionDeviceSecret(SslUtil.getCertificateString(ca));
        profile.setTransportType(DeviceTransportType.MQTT);
        profile.getProfileData().setTransportConfiguration(new MqttDeviceProfileTransportConfiguration());
        saveProfile();

        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null);
        trustStore.setCertificateEntry("ca", ca);
        TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        factory.init(trustStore);
        MqttSslHandlerProvider provider = new MqttSslHandlerProvider();
        ReflectionTestUtils.setField(provider, "transportService", transportService);
        X509TrustManager trustManager = ReflectionTestUtils.invokeMethod(provider, "getX509TrustManager", factory);
        assertNotNull(trustManager);
        assertNull(findDevice());
        // Exercise the unchanged MQTT signature verification and real provisioning boundary.
        trustManager.checkClientTrusted(new X509Certificate[]{leaf, ca}, "RSA");
        Device device = findDevice();
        assertNotNull(device);
        assertEquals(profile.getId(), device.getDeviceProfileId());
        DeviceCredentials credentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, device.getId());
        assertEquals(DeviceCredentialsType.X509_CERTIFICATE, credentials.getCredentialsType());
        assertTrue(SslUtil.getCertificateString(leaf).equals(credentials.getCredentialsValue()));
        trustManager.checkClientTrusted(new X509Certificate[]{leaf, ca}, "RSA");
        assertEquals(device.getId(), findDevice().getId());
    }

    private CoapResponse register(String registrationEndpoint) throws Exception {
        return register(registrationEndpoint, leaf, ca);
    }

    private CoapResponse register(String registrationEndpoint, X509Certificate... chain) throws Exception {
        Configuration config = Configuration.createStandardWithoutFile();
        config.set(DtlsConfig.DTLS_ROLE, DtlsConfig.DtlsRole.CLIENT_ONLY);
        config.set(DtlsConfig.DTLS_VERIFY_SERVER_CERTIFICATES_SUBJECT, false);
        DtlsConnectorConfig.Builder dtls = new DtlsConnectorConfig.Builder(config)
                .setAddress(new InetSocketAddress("127.0.0.1", 0))
                .setCertificateIdentityProvider(new SingleCertificateProvider(leafKey.getPrivate(), chain))
                .setAdvancedCertificateVerifier(StaticNewAdvancedCertificateVerifier.builder()
                        .setTrustedCertificates(serverX509Cert).build());
        CoapEndpoint coapEndpoint = new CoapEndpoint.Builder().setConfiguration(config).setConnector(new DTLSConnector(dtls.build())).build();
        CoapClient client = new CoapClient(SECURE_URI + "/rd?ep=" + registrationEndpoint + "&lt=300&lwm2m=1.0");
        client.setEndpoint(coapEndpoint);
        client.setTimeout(15000L);
        try {
            CoapResponse response = client.post("</3/0>", 40);
            assertNotNull("Expected a LwM2M registration response after DTLS authentication", response);
            return response;
        } finally {
            client.shutdown();
            coapEndpoint.destroy();
        }
    }

    private ValidateDeviceCredentialsResponse provision(X509Certificate... chain) throws Exception {
        CompletableFuture<ValidateDeviceCredentialsResponse> result = new CompletableFuture<>();
        transportService.process(DeviceTransportType.LWM2M, TransportProtos.ValidateOrCreateDeviceX509CertRequestMsg.newBuilder()
                .setCertificateChain(SslUtil.getCertificateChainString(chain)).build(), callback(result));
        return result.get(30, TimeUnit.SECONDS);
    }

    private TransportServiceCallback<ValidateDeviceCredentialsResponse> callback(CompletableFuture<ValidateDeviceCredentialsResponse> result) {
        return new TransportServiceCallback<>() {
            @Override
            public void onSuccess(ValidateDeviceCredentialsResponse response) {
                result.complete(response);
            }

            @Override
            public void onError(Throwable e) {
                result.completeExceptionally(e);
            }
        };
    }

    private void assertRejected(X509Certificate... chain) throws Exception {
        assertFalse(provision(chain).hasDeviceInfo());
        assertNull(findDevice());
    }

    private Device findDevice() {
        return deviceService.findDeviceByTenantIdAndName(tenantId, deviceName);
    }

    private void synchronizeDeviceCreation(int participants) {
        CyclicBarrier barrier = new CyclicBarrier(participants);
        doAnswer(invocation -> {
            barrier.await(20, TimeUnit.SECONDS);
            return invocation.callRealMethod();
        }).when(deviceService).saveDevice(any(ProvisionRequest.class), any(DeviceProfile.class));
    }

    private X509CertificateChainProvisionConfiguration configuration() {
        return (X509CertificateChainProvisionConfiguration) profile.getProfileData().getProvisionConfiguration();
    }

    private void saveProfile() throws Exception {
        profile = doPost("/api/deviceProfile", profile, DeviceProfile.class);
    }

    private KeyPair keyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(256);
        return generator.generateKeyPair();
    }

    private X509Certificate certificate(String cn, KeyPair subjectKey, X509Certificate issuer, KeyPair issuerKey,
                                        boolean isCa, boolean expired) throws Exception {
        return certificate(cn, subjectKey, issuer, issuerKey, isCa, expired, KeyPurposeId.id_kp_clientAuth);
    }

    private X509Certificate certificate(String cn, KeyPair subjectKey, X509Certificate issuer, KeyPair issuerKey,
                                        boolean isCa, boolean expired, KeyPurposeId purpose) throws Exception {
        X500Name subject = new X500Name("CN=" + cn);
        long now = System.currentTimeMillis();
        var builder = new JcaX509v3CertificateBuilder(issuer == null ? subject : new X500Name(issuer.getSubjectX500Principal().getName()),
                new BigInteger(128, new SecureRandom()), new Date(now - TimeUnit.DAYS.toMillis(2)),
                new Date(now + TimeUnit.DAYS.toMillis(expired ? -1 : 2)), subject, subjectKey.getPublic());
        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(isCa));
        builder.addExtension(Extension.keyUsage, true, new KeyUsage(isCa ? KeyUsage.keyCertSign : KeyUsage.digitalSignature));
        if (!isCa) {
            builder.addExtension(Extension.extendedKeyUsage, false, new ExtendedKeyUsage(purpose));
        }
        return new JcaX509CertificateConverter().getCertificate(builder.build(
                new JcaContentSignerBuilder("RSA".equals(issuerKey.getPrivate().getAlgorithm())
                        ? "SHA256withRSA" : "SHA256withECDSA").build(issuerKey.getPrivate())));
    }
}
