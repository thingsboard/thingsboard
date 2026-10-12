// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.service;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.DeviceCertificateRevocation;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.common.msg.EncryptionUtil;
import org.thingsboard.server.dao.device.DeviceCertificateRevocationService;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.exception.DataValidationException;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DaoSqlTest
public class DeviceCertificateRevocationServiceTest extends AbstractServiceTest {

    @Autowired
    DeviceCertificateRevocationService revocationService;

    @Autowired
    DeviceService deviceService;

    @Autowired
    DeviceCredentialsService deviceCredentialsService;

    @Test
    public void testRevokeStoresCertificateDetailsAndHashMatchingTransports() throws Exception {
        long notAfter = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(30);
        X509Certificate certificate = generateCertificate("CN=device-1", false, notAfter);

        DeviceCertificateRevocation revocation = revocationService.revokeCertificate(tenantId, toPem(certificate), null, " Device lost ", null);

        // The same value transports compute for a presented certificate (SslUtil.getCertificateString + getSha3Hash)
        String transportHash = EncryptionUtil.getSha3Hash(Base64.getEncoder().encodeToString(certificate.getEncoded()));
        assertThat(revocation.getId()).isNotNull();
        assertThat(revocation.getCertificateHash()).isEqualTo(transportHash);
        assertThat(revocation.getSubject()).isEqualTo("CN=device-1");
        assertThat(revocation.getSerialNumber()).isEqualTo(certificate.getSerialNumber().toString(16));
        assertThat(revocation.getNotAfter()).isEqualTo(certificate.getNotAfter().getTime());
        assertThat(revocation.getReason()).isEqualTo("Device lost");
        assertThat(revocationService.isRevoked(tenantId, transportHash)).isTrue();
    }

    @Test
    public void testHashIgnoresPemFormatting() throws Exception {
        X509Certificate certificate = generateCertificate("CN=device-2", false, System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1));
        String messyPem = toPem(certificate).replace("\n", "\r\n  ") + "   ";

        DeviceCertificateRevocation revocation = revocationService.revokeCertificate(tenantId, messyPem, null, null, null);

        assertThat(revocationService.findRevocationByCertificatePem(tenantId, toPem(certificate)).getId()).isEqualTo(revocation.getId());
    }

    @Test
    public void testRevocationIsTenantScoped() throws Exception {
        X509Certificate certificate = generateCertificate("CN=device-3", false, System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1));
        Tenant otherTenant = createTenant();
        try {
            DeviceCertificateRevocation revocation = revocationService.revokeCertificate(otherTenant.getId(), toPem(certificate), null, null, null);

            assertThat(revocationService.isRevoked(otherTenant.getId(), revocation.getCertificateHash())).isTrue();
            assertThat(revocationService.isRevoked(tenantId, revocation.getCertificateHash())).isFalse();
            // The same certificate can be revoked independently by another tenant
            revocationService.revokeCertificate(tenantId, toPem(certificate), null, null, null);
            assertThat(revocationService.isRevoked(tenantId, revocation.getCertificateHash())).isTrue();
        } finally {
            tenantService.deleteTenant(otherTenant.getId());
        }
    }

    @Test
    public void testIsRevokedReflectsRevokeAndDeleteDespiteCachedNegativeLookup() throws Exception {
        X509Certificate certificate = generateCertificate("CN=device-4", false, System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1));
        String hash = EncryptionUtil.getSha3Hash(Base64.getEncoder().encodeToString(certificate.getEncoded()));
        assertThat(revocationService.isRevoked(tenantId, hash)).isFalse();

        DeviceCertificateRevocation revocation = revocationService.revokeCertificate(tenantId, toPem(certificate), null, null, null);
        assertThat(revocationService.isRevoked(tenantId, hash)).isTrue();

        revocationService.deleteRevocation(tenantId, revocation.getId());
        assertThat(revocationService.isRevoked(tenantId, hash)).isFalse();
        assertThat(revocationService.findRevocationById(tenantId, revocation.getId())).isNull();
    }

    @Test
    public void testRevokeTwiceIsRejected() throws Exception {
        String pem = toPem(generateCertificate("CN=device-5", false, System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1)));
        revocationService.revokeCertificate(tenantId, pem, null, null, null);

        assertThatThrownBy(() -> revocationService.revokeCertificate(tenantId, pem, null, null, null))
                .isInstanceOf(DataValidationException.class)
                .hasMessage("Certificate is already revoked!");
    }

    @Test
    public void testSelfSignedDeviceCertificateMarkedAsCaCanBeRevoked() throws Exception {
        // "openssl req -x509" makes a CA certificate by default, and devices use such certificates as their own
        String pem = toPem(generateCertificate("CN=self-signed-device", true, System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1)));
        Device device = new Device();
        device.setTenantId(tenantId);
        device.setName("self-signed-device");
        device.setType("default");
        device = deviceService.saveDevice(device);
        DeviceCredentials credentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, device.getId());
        credentials.setCredentialsType(DeviceCredentialsType.X509_CERTIFICATE);
        credentials.setCredentialsValue(pem);
        deviceCredentialsService.updateDeviceCredentials(tenantId, credentials);

        DeviceCertificateRevocation revocation = revocationService.revokeCertificate(tenantId, pem, null, null, null);

        assertThat(revocation.getDeviceId()).isEqualTo(device.getId());
    }

    @Test
    public void testInvalidInputIsRejected() throws Exception {
        String caPem = toPem(generateCertificate("CN=Intermediate CA", true, System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1)));
        String leafPem = toPem(generateCertificate("CN=device-6", false, System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1)));

        assertThatThrownBy(() -> revocationService.revokeCertificate(tenantId, caPem, null, null, null))
                .isInstanceOf(DataValidationException.class)
                .hasMessageStartingWith("CA certificates can't be revoked!");
        assertThatThrownBy(() -> revocationService.revokeCertificate(tenantId, leafPem + caPem, null, null, null))
                .isInstanceOf(DataValidationException.class)
                .hasMessage("Specify a single certificate, not a certificate chain!");
        assertThatThrownBy(() -> revocationService.revokeCertificate(tenantId, "not a certificate", null, null, null))
                .isInstanceOf(DataValidationException.class)
                .hasMessageStartingWith("Invalid certificate");
        assertThatThrownBy(() -> revocationService.revokeCertificate(tenantId, " ", null, null, null))
                .isInstanceOf(DataValidationException.class)
                .hasMessage("Certificate should be specified!");
    }

    @Test
    public void testFindRevocationsByTenantIdWithTextSearch() throws Exception {
        revocationService.revokeCertificate(tenantId, toPem(generateCertificate("CN=sensor-a", false, System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1))), null, null, null);
        revocationService.revokeCertificate(tenantId, toPem(generateCertificate("CN=gateway-b", false, System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1))), null, null, null);

        PageData<DeviceCertificateRevocation> all = revocationService.findRevocationsByTenantId(tenantId, new PageLink(10));
        PageData<DeviceCertificateRevocation> found = revocationService.findRevocationsByTenantId(tenantId, new PageLink(10, 0, "sensor"));

        assertThat(all.getTotalElements()).isEqualTo(2);
        assertThat(found.getData()).extracting(DeviceCertificateRevocation::getSubject).containsExactly("CN=sensor-a");
    }

    @Test
    public void testDeleteRevocationsExpiredBefore() throws Exception {
        long now = System.currentTimeMillis();
        revocationService.revokeCertificate(tenantId, toPem(generateCertificate("CN=expired", false, now - TimeUnit.DAYS.toMillis(10))), null, null, null);
        revocationService.revokeCertificate(tenantId, toPem(generateCertificate("CN=valid", false, now + TimeUnit.DAYS.toMillis(10))), null, null, null);

        int deleted = revocationService.deleteRevocationsExpiredBefore(now - TimeUnit.DAYS.toMillis(7));

        assertThat(deleted).isEqualTo(1);
        assertThat(revocationService.findRevocationsByTenantId(tenantId, new PageLink(10)).getData())
                .extracting(DeviceCertificateRevocation::getSubject).containsExactly("CN=valid");
    }

    @Test
    public void testDeleteTenantRemovesRevocations() throws Exception {
        Tenant otherTenant = createTenant();
        TenantId otherTenantId = otherTenant.getId();
        revocationService.revokeCertificate(otherTenantId, toPem(generateCertificate("CN=device-7", false, System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1))), null, null, null);

        tenantService.deleteTenant(otherTenantId);

        assertThat(revocationService.findRevocationsByTenantId(otherTenantId, new PageLink(10)).getData()).isEmpty();
    }

    private static X509Certificate generateCertificate(String subject, boolean ca, long notAfter) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(256);
        KeyPair keyPair = generator.generateKeyPair();
        X500Name name = new X500Name(subject);
        long notBefore = Math.min(System.currentTimeMillis(), notAfter) - TimeUnit.DAYS.toMillis(30);
        X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(name, BigInteger.valueOf(System.nanoTime()),
                new Date(notBefore), new Date(notAfter), name, keyPair.getPublic());
        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(ca));
        return new JcaX509CertificateConverter().getCertificate(builder.build(new JcaContentSignerBuilder("SHA256withECDSA").build(keyPair.getPrivate())));
    }

    private static String toPem(X509Certificate certificate) throws Exception {
        return "-----BEGIN CERTIFICATE-----\n" +
                Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(certificate.getEncoded()) +
                "\n-----END CERTIFICATE-----\n";
    }

}
