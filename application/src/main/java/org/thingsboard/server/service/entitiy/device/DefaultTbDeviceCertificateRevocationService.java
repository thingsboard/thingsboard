// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.entitiy.device;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.SslUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MDeviceCredentials;
import org.thingsboard.server.common.data.device.credentials.lwm2m.X509ClientCredential;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.DeviceCertificateInfo;
import org.thingsboard.server.common.data.security.DeviceCertificateRevocation;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.common.msg.rule.engine.DeviceCredentialsUpdateNotificationMsg;
import org.thingsboard.server.dao.device.DeviceCertificateRevocationService;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;

import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.util.HexFormat;
import java.util.List;

@Slf4j
@Service
@TbCoreComponent
@RequiredArgsConstructor
public class DefaultTbDeviceCertificateRevocationService extends AbstractTbEntityService implements TbDeviceCertificateRevocationService {

    private final DeviceCertificateRevocationService revocationService;
    private final DeviceCredentialsService deviceCredentialsService;
    private final DeviceService deviceService;
    private final TenantService tenantService;

    @Override
    public DeviceCertificateRevocation revokeDeviceCertificate(Device device, String reason, User user) throws ThingsboardException {
        TenantId tenantId = device.getTenantId();
        try {
            DeviceCredentials credentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, device.getId());
            String certificate = credentials != null ? getStoredCertificate(credentials) : null;
            if (certificate == null) {
                throw new DataValidationException("Device credentials don't hold an X.509 certificate! Revoke the certificate the device presents by its PEM.");
            }
            DeviceCertificateRevocation revocation = revocationService.revokeCertificate(tenantId, certificate, device.getId(), reason, user.getId());
            onRevoked(revocation, device, credentials);
            logEntityActionService.logEntityAction(tenantId, device.getId(), device, device.getCustomerId(), ActionType.CREDENTIALS_REVOKED, user, revocation);
            return revocation;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, device.getId(), device, device.getCustomerId(), ActionType.CREDENTIALS_REVOKED, user, e);
            throw e;
        }
    }

    @Override
    public DeviceCertificateRevocation revokeCertificate(TenantId tenantId, String certificatePem, String reason, User user) throws ThingsboardException {
        try {
            DeviceCertificateRevocation revocation = revocationService.revokeCertificate(tenantId, certificatePem, null, reason, user.getId());
            Device device = revocation.getDeviceId() != null ? deviceService.findDeviceById(tenantId, revocation.getDeviceId()) : null;
            if (device != null) {
                onRevoked(revocation, device, deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, device.getId()));
                logEntityActionService.logEntityAction(tenantId, device.getId(), device, device.getCustomerId(), ActionType.CREDENTIALS_REVOKED, user, revocation);
            } else {
                onRevoked(revocation, null, null);
                logTenantAction(tenantId, ActionType.CREDENTIALS_REVOKED, user, revocation);
            }
            return revocation;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, tenantId, ActionType.CREDENTIALS_REVOKED, user, e);
            throw e;
        }
    }

    @Override
    public DeviceCertificateInfo getDeviceCertificateInfo(Device device) {
        DeviceCredentials credentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(device.getTenantId(), device.getId());
        String certificatePem = credentials != null ? getStoredCertificate(credentials) : null;
        if (certificatePem == null) {
            return null;
        }
        try {
            List<X509Certificate> certificates = SslUtil.readCertFile(certificatePem);
            if (certificates == null || certificates.size() != 1) {
                return null;
            }
            X509Certificate certificate = certificates.get(0);
            DeviceCertificateInfo info = new DeviceCertificateInfo();
            info.setSubject(certificate.getSubjectX500Principal().getName());
            info.setIssuer(certificate.getIssuerX500Principal().getName());
            info.setSerialNumber(certificate.getSerialNumber().toString(16));
            info.setNotBefore(certificate.getNotBefore().getTime());
            info.setNotAfter(certificate.getNotAfter().getTime());
            info.setSha256Fingerprint(HexFormat.ofDelimiter(":").withUpperCase()
                    .formatHex(MessageDigest.getInstance("SHA-256").digest(certificate.getEncoded())));
            info.setRevocation(revocationService.findRevocationByCertificatePem(device.getTenantId(), certificatePem));
            return info;
        } catch (Exception e) {
            // e.g. a placeholder value of a pre-registered device
            log.debug("[{}][{}] Device credentials hold an invalid certificate: {}", device.getTenantId(), device.getId(), e.getMessage());
            return null;
        }
    }

    @Override
    public void deleteRevocation(DeviceCertificateRevocation revocation, User user) throws ThingsboardException {
        TenantId tenantId = revocation.getTenantId();
        Device device = revocation.getDeviceId() != null ? deviceService.findDeviceById(tenantId, revocation.getDeviceId()) : null;
        try {
            revocationService.deleteRevocation(tenantId, revocation.getId());
            if (device != null) {
                logEntityActionService.logEntityAction(tenantId, device.getId(), device, device.getCustomerId(), ActionType.CREDENTIALS_REVOCATION_DELETED, user, revocation);
            } else {
                logTenantAction(tenantId, ActionType.CREDENTIALS_REVOCATION_DELETED, user, revocation);
            }
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, tenantId, ActionType.CREDENTIALS_REVOCATION_DELETED, user, e, revocation);
            throw e;
        }
    }

    @Override
    public void validateNotRevoked(TenantId tenantId, DeviceId deviceId, DeviceCredentials credentials) {
        if (credentials == null || (credentials.getCredentialsType() != DeviceCredentialsType.X509_CERTIFICATE
                && credentials.getCredentialsType() != DeviceCredentialsType.LWM2M_CREDENTIALS)) {
            return;
        }
        // Same id the transports look the credentials up by: the certificate hash for X.509 credentials
        DeviceCredentials formatted = new DeviceCredentials(credentials);
        try {
            deviceCredentialsService.formatCredentials(formatted);
        } catch (Exception e) {
            return; // invalid credentials are rejected with a proper message when saved
        }
        if (!revocationService.isRevoked(tenantId, formatted.getCredentialsId())) {
            return;
        }
        DeviceCredentials stored = deviceId != null ? deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, deviceId) : null;
        if (stored == null || !formatted.getCredentialsId().equals(stored.getCredentialsId())) {
            throw new DataValidationException("Certificate is revoked! Remove it from the revoked certificates to use it again.");
        }
    }

    // Revocations of certificates no device is bound to have no device to log the action for
    private void logTenantAction(TenantId tenantId, ActionType actionType, User user, DeviceCertificateRevocation revocation) {
        Tenant tenant = tenantService.findTenantById(tenantId);
        logEntityActionService.logEntityAction(tenantId, tenantId, tenant, actionType, user, revocation);
    }

    /**
     * MQTT and LwM2M sessions are closed by the device actor when the revoked certificate is the one the device is bound to.
     * CoAP and LwM2M also keep authenticated DTLS state outside device sessions, which the transports drop by the certificate hash.
     */
    private void onRevoked(DeviceCertificateRevocation revocation, Device device, DeviceCredentials credentials) {
        TenantId tenantId = revocation.getTenantId();
        if (device != null && credentials != null && revocation.getCertificateHash().equals(credentials.getCredentialsId())) {
            tbClusterService.pushMsgToCore(new DeviceCredentialsUpdateNotificationMsg(tenantId, device.getId(), credentials), null);
        }
        tbClusterService.onDeviceCertificateRevoked(tenantId, revocation.getCertificateHash(), null);
    }

    private static String getStoredCertificate(DeviceCredentials credentials) {
        String certificate = null;
        if (credentials.getCredentialsType() == DeviceCredentialsType.X509_CERTIFICATE) {
            certificate = credentials.getCredentialsValue();
        } else if (credentials.getCredentialsType() == DeviceCredentialsType.LWM2M_CREDENTIALS) {
            LwM2MDeviceCredentials lwM2MCredentials = JacksonUtil.fromString(credentials.getCredentialsValue(), LwM2MDeviceCredentials.class);
            if (lwM2MCredentials != null && lwM2MCredentials.getClient() instanceof X509ClientCredential x509Credential) {
                certificate = x509Credential.getCert(); // empty in trust mode: the certificate is not stored
            }
        }
        if (StringUtils.isBlank(certificate)) {
            return null;
        }
        // Stored values may lack the PEM armor: X.509 credentials keep only the Base64 body
        return certificate.contains("-----BEGIN CERTIFICATE-----") ? certificate :
                "-----BEGIN CERTIFICATE-----\n" + certificate.trim() + "\n-----END CERTIFICATE-----";
    }

}
