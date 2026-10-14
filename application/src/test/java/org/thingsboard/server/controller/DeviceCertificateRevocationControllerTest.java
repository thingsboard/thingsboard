// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.audit.AuditLog;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.DeviceCertificateInfo;
import org.thingsboard.server.common.data.security.DeviceCertificateRevocation;
import org.thingsboard.server.common.data.security.DeviceCertificateRevocationRequest;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class DeviceCertificateRevocationControllerTest extends AbstractControllerTest {

    @Before
    public void setUp() throws Exception {
        loginTenantAdmin();
    }

    @Test
    public void testRevokeDeviceCertificate() throws Exception {
        String pem = toPem(generateCertificate("CN=device-1", false));
        Device device = createDevice("device-1");
        DeviceCredentials credentials = setX509Credentials(device, pem);

        DeviceCertificateRevocation revocation = revokeDeviceCertificate(device, "Device lost");

        assertThat(revocation.getDeviceId()).isEqualTo(device.getId());
        assertThat(revocation.getCertificateHash()).isEqualTo(credentials.getCredentialsId());
        assertThat(revocation.getSubject()).isEqualTo("CN=device-1");
        assertThat(revocation.getReason()).isEqualTo("Device lost");
        DeviceCertificateInfo certificateInfo = doGet("/api/device/" + device.getId() + "/credentials/certificate", DeviceCertificateInfo.class);
        assertThat(certificateInfo.getSubject()).isEqualTo("CN=device-1");
        assertThat(certificateInfo.getSha256Fingerprint()).matches("([0-9A-F]{2}:){31}[0-9A-F]{2}");
        assertThat(certificateInfo.getRevocation().getId()).isEqualTo(revocation.getId());
        assertThat(getRevocations().getData()).extracting(DeviceCertificateRevocation::getId).containsExactly(revocation.getId());
        // The device keeps its credentials: chain provisioning binds a renewed certificate to it
        assertThat(doGet("/api/device/" + device.getId() + "/credentials", DeviceCredentials.class).getCredentialsId())
                .isEqualTo(credentials.getCredentialsId());

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            PageData<AuditLog> auditLogs = doGetTypedWithPageLink("/api/audit/logs/entity/DEVICE/" + device.getId() + "?",
                    new TypeReference<>() {}, new PageLink(10));
            assertThat(auditLogs.getData()).filteredOn(auditLog -> auditLog.getActionType() == ActionType.CREDENTIALS_REVOKED)
                    .singleElement().satisfies(auditLog -> {
                        assertThat(auditLog.getActionData().at("/revocation/subject").asText()).isEqualTo("CN=device-1");
                        assertThat(auditLog.getActionData().at("/revocation/reason").asText()).isEqualTo("Device lost");
                    });
        });
    }

    @Test
    public void testRevokedCertificateCanNotBeSavedAgain() throws Exception {
        String pem = toPem(generateCertificate("CN=device-2", false));
        Device device = createDevice("device-2");
        setX509Credentials(device, pem);
        revokeDeviceCertificate(device, null);

        Device otherDevice = createDevice("device-2-other");
        DeviceCredentials otherCredentials = doGet("/api/device/" + otherDevice.getId() + "/credentials", DeviceCredentials.class);
        otherCredentials.setCredentialsType(DeviceCredentialsType.X509_CERTIFICATE);
        otherCredentials.setCredentialsValue(pem);
        doPost("/api/device/credentials", otherCredentials)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Certificate is revoked!")));
    }

    @Test
    public void testDeviceKeepsItsRevokedCertificateWhenSavedAgain() throws Exception {
        String pem = toPem(generateCertificate("CN=device-5", false));
        Device device = createDevice("device-5");
        setX509Credentials(device, pem);
        revokeDeviceCertificate(device, null);

        // E.g. a bulk import that changes the label of the device sends its stored credentials back
        DeviceCredentials storedCredentials = doGet("/api/device/" + device.getId() + "/credentials", DeviceCredentials.class);
        doPost("/api/device/credentials", storedCredentials).andExpect(status().isOk());
    }

    @Test
    public void testRevokeLwM2MDeviceCertificate() throws Exception {
        X509Certificate certificate = generateCertificate("CN=lwm2m-device", false);
        Device device = createDevice("lwm2m-device");
        DeviceCredentials credentials = setLwM2MX509Credentials(device, "lwm2m-device", certificate);

        DeviceCertificateInfo certificateInfo = doGet("/api/device/" + device.getId() + "/credentials/certificate", DeviceCertificateInfo.class);
        assertThat(certificateInfo.getSubject()).isEqualTo("CN=lwm2m-device");
        DeviceCertificateRevocation revocation = revokeDeviceCertificate(device, null);
        assertThat(revocation.getCertificateHash()).isEqualTo(credentials.getCredentialsId());
        assertThat(revocation.getDeviceId()).isEqualTo(device.getId());

        Device otherDevice = createDevice("lwm2m-device-other");
        DeviceCredentials otherCredentials = doGet("/api/device/" + otherDevice.getId() + "/credentials", DeviceCredentials.class);
        otherCredentials.setCredentialsType(DeviceCredentialsType.LWM2M_CREDENTIALS);
        otherCredentials.setCredentialsValue(lwm2mX509Credentials("lwm2m-device-other", certificate));
        doPost("/api/device/credentials", otherCredentials)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Certificate is revoked!")));
    }

    @Test
    public void testCustomerUserSeesRevocationWithoutRevokedBy() throws Exception {
        String pem = toPem(generateCertificate("CN=customer-device", false));
        loginCustomerAdminUser();
        Device device = new Device();
        device.setName("customer-device");
        device.setType("default");
        device.setCustomerId(customerId);
        device = doPost("/api/device", device, Device.class);
        setX509Credentials(device, pem);
        // A revocation applies to the whole tenant
        doPost("/api/device/" + device.getId() + "/credentials/revoke", request(null, null)).andExpect(status().isForbidden());

        loginTenantAdmin();
        revokeDeviceCertificate(device, null);

        loginCustomerAdminUser();
        assertThat(doGet("/api/device/" + device.getId() + "/credentials/certificate", DeviceCertificateInfo.class)
                .getRevocation().getRevokedBy()).isNull();
        // The customer's copy is redacted, not the cached revocation
        loginTenantAdmin();
        assertThat(doGet("/api/device/" + device.getId() + "/credentials/certificate", DeviceCertificateInfo.class)
                .getRevocation().getRevokedBy()).isEqualTo(tenantAdminUserId);
    }

    @Test
    public void testRevokeByPemAndDeleteRevocation() throws Exception {
        String pem = toPem(generateCertificate("CN=previous-certificate", false));

        DeviceCertificateRevocation revocation = revokeCertificate(pem, "Superseded");
        assertThat(revocation.getDeviceId()).isNull();
        doPost("/api/deviceCertificateRevocation", request(pem, null))
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Certificate is already revoked!")));

        doDelete("/api/deviceCertificateRevocation/" + revocation.getId().getId()).andExpect(status().isOk());

        assertThat(getRevocations().getData()).isEmpty();
        assertThat(revokeCertificate(pem, null).getId()).isNotEqualTo(revocation.getId());
        // A certificate that is not bound to a device is audited on the tenant
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            PageData<AuditLog> auditLogs = doGetTypedWithPageLink("/api/audit/logs/entity/TENANT/" + tenantId.getId() + "?",
                    new TypeReference<>() {}, new PageLink(10));
            assertThat(auditLogs.getData()).extracting(AuditLog::getActionType)
                    .contains(ActionType.CREDENTIALS_REVOKED, ActionType.CREDENTIALS_REVOCATION_DELETED);
            assertThat(auditLogs.getData()).filteredOn(auditLog -> auditLog.getActionType() == ActionType.CREDENTIALS_REVOCATION_DELETED)
                    .singleElement().satisfies(auditLog -> assertThat(auditLog.getActionData().at("/revocation/id/id").asText())
                            .isEqualTo(revocation.getId().getId().toString()));
        });
    }

    @Test
    public void testRevokeByPemFindsBoundDevice() throws Exception {
        String pem = toPem(generateCertificate("CN=device-3", false));
        Device device = createDevice("device-3");
        setX509Credentials(device, pem);

        assertThat(revokeCertificate(pem, null).getDeviceId()).isEqualTo(device.getId());
    }

    @Test
    public void testInvalidRevocationsAreRejected() throws Exception {
        Device device = createDevice("device-with-token");
        doPost("/api/device/" + device.getId() + "/credentials/revoke", request(null, null))
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Device credentials don't hold an X.509 certificate!")));

        doPost("/api/deviceCertificateRevocation", request(toPem(generateCertificate("CN=Intermediate CA", true)), null))
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("CA certificates can't be revoked!")));
    }

    @Test
    public void testCustomerUserCanNotManageTenantRevocations() throws Exception {
        String pem = toPem(generateCertificate("CN=device-4", false));
        loginCustomerUser();

        doGet("/api/deviceCertificateRevocations?pageSize=10&page=0").andExpect(status().isForbidden());
        doPost("/api/deviceCertificateRevocation", request(pem, null)).andExpect(status().isForbidden());
    }

    private DeviceCredentials setX509Credentials(Device device, String pem) throws Exception {
        DeviceCredentials credentials = doGet("/api/device/" + device.getId() + "/credentials", DeviceCredentials.class);
        credentials.setCredentialsType(DeviceCredentialsType.X509_CERTIFICATE);
        credentials.setCredentialsValue(pem);
        return doPost("/api/device/credentials", credentials, DeviceCredentials.class);
    }

    private DeviceCredentials setLwM2MX509Credentials(Device device, String endpoint, X509Certificate certificate) throws Exception {
        DeviceCredentials credentials = doGet("/api/device/" + device.getId() + "/credentials", DeviceCredentials.class);
        credentials.setCredentialsType(DeviceCredentialsType.LWM2M_CREDENTIALS);
        credentials.setCredentialsValue(lwm2mX509Credentials(endpoint, certificate));
        return doPost("/api/device/credentials", credentials, DeviceCredentials.class);
    }

    private static String lwm2mX509Credentials(String endpoint, X509Certificate certificate) throws Exception {
        return "{\"client\": {\"securityConfigClientMode\": \"X509\", \"endpoint\": \"" + endpoint + "\", " +
                "\"cert\": \"" + Base64.getEncoder().encodeToString(certificate.getEncoded()) + "\"}, " +
                "\"bootstrap\": {\"bootstrapServer\": {\"securityMode\": \"NO_SEC\"}, \"lwm2mServer\": {\"securityMode\": \"NO_SEC\"}}}";
    }

    private DeviceCertificateRevocation revokeDeviceCertificate(Device device, String reason) {
        return doPost("/api/device/" + device.getId() + "/credentials/revoke", request(null, reason), DeviceCertificateRevocation.class);
    }

    private DeviceCertificateRevocation revokeCertificate(String pem, String reason) {
        return doPost("/api/deviceCertificateRevocation", request(pem, reason), DeviceCertificateRevocation.class);
    }

    private PageData<DeviceCertificateRevocation> getRevocations() throws Exception {
        return doGetTypedWithPageLink("/api/deviceCertificateRevocations?", new TypeReference<>() {}, new PageLink(10));
    }

    private static DeviceCertificateRevocationRequest request(String certificate, String reason) {
        DeviceCertificateRevocationRequest request = new DeviceCertificateRevocationRequest();
        request.setCertificate(certificate);
        request.setReason(reason);
        return request;
    }

    private static X509Certificate generateCertificate(String subject, boolean ca) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(256);
        KeyPair keyPair = generator.generateKeyPair();
        X500Name name = new X500Name(subject);
        long now = System.currentTimeMillis();
        X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(name, BigInteger.valueOf(System.nanoTime()),
                new Date(now - TimeUnit.DAYS.toMillis(1)), new Date(now + TimeUnit.DAYS.toMillis(30)), name, keyPair.getPublic());
        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(ca));
        return new JcaX509CertificateConverter().getCertificate(builder.build(new JcaContentSignerBuilder("SHA256withECDSA").build(keyPair.getPrivate())));
    }

    private static String toPem(X509Certificate certificate) throws Exception {
        return "-----BEGIN CERTIFICATE-----\n" +
                Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(certificate.getEncoded()) +
                "\n-----END CERTIFICATE-----\n";
    }

}
