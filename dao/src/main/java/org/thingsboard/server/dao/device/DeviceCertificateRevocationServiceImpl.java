// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.device;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.common.util.SslUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.DeviceCertificateRevocationId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.DeviceCertificateRevocation;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.msg.EncryptionUtil;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.entity.AbstractCachedEntityService;
import org.thingsboard.server.dao.service.validator.DeviceCertificateRevocationDataValidator;
import org.thingsboard.server.exception.DataValidationException;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validatePageLink;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceCertificateRevocationServiceImpl extends AbstractCachedEntityService<DeviceCertificateRevocationCacheKey, DeviceCertificateRevocation, DeviceCertificateRevocationEvictEvent> implements DeviceCertificateRevocationService {

    private static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    private static final String INCORRECT_REVOCATION_ID = "Incorrect revocationId ";
    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of("createdTime", "subject", "issuer", "serialNumber", "notAfter", "reason");

    private final DeviceCertificateRevocationDao revocationDao;
    private final DeviceCertificateRevocationDataValidator revocationValidator;
    private final DeviceCredentialsService deviceCredentialsService;
    private final DeviceService deviceService;

    @TransactionalEventListener(classes = DeviceCertificateRevocationEvictEvent.class)
    @Override
    public void handleEvictEvent(DeviceCertificateRevocationEvictEvent event) {
        cache.evict(DeviceCertificateRevocationCacheKey.of(event.tenantId(), event.certificateHash()));
    }

    @Override
    public DeviceCertificateRevocation revokeCertificate(TenantId tenantId, String certificatePem, DeviceId deviceId, String reason, UserId revokedBy) {
        log.trace("[{}] Executing revokeCertificate, deviceId [{}]", tenantId, deviceId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        X509Certificate certificate = parseCertificate(certificatePem);
        String certificateHash = getCertificateHash(certificate);
        DeviceId boundDeviceId = deviceId != null ? deviceId : findBoundDeviceId(tenantId, certificateHash);
        // A self-signed device certificate is often a CA certificate as well: "openssl req -x509" makes one by default
        if (certificate.getBasicConstraints() != -1 && boundDeviceId == null) {
            throw new DataValidationException("CA certificates can't be revoked! To stop trusting a CA, change the certificate on the device profile.");
        }
        DeviceCertificateRevocation revocation = new DeviceCertificateRevocation();
        revocation.setTenantId(tenantId);
        revocation.setDeviceId(boundDeviceId);
        revocation.setCertificateHash(certificateHash);
        revocation.setSerialNumber(certificate.getSerialNumber().toString(16));
        revocation.setIssuer(certificate.getIssuerX500Principal().getName());
        revocation.setSubject(certificate.getSubjectX500Principal().getName());
        revocation.setNotBefore(certificate.getNotBefore().getTime());
        revocation.setNotAfter(certificate.getNotAfter().getTime());
        revocation.setReason(StringUtils.isNotBlank(reason) ? reason.trim() : null);
        revocation.setRevokedBy(revokedBy);
        revocationValidator.validate(revocation, DeviceCertificateRevocation::getTenantId);
        try {
            DeviceCertificateRevocation saved = revocationDao.save(tenantId, revocation);
            publishEvictEvent(new DeviceCertificateRevocationEvictEvent(tenantId, saved.getCertificateHash()));
            return saved;
        } catch (Exception e) {
            ConstraintViolationException cve = DaoUtil.extractConstraintViolationException(e).orElse(null);
            if (cve != null && DaoUtil.constraintNameMatches(cve.getConstraintName(), "device_certificate_revocation_hash_unq_key")) {
                throw new DataValidationException("Certificate is already revoked!");
            }
            throw e;
        }
    }

    @Override
    public boolean isRevoked(TenantId tenantId, String certificateHash) {
        log.trace("[{}] Executing isRevoked [{}]", tenantId, certificateHash);
        if (tenantId == null || StringUtils.isEmpty(certificateHash)) {
            return false;
        }
        return cache.getAndPutInTransaction(DeviceCertificateRevocationCacheKey.of(tenantId, certificateHash),
                () -> revocationDao.findByTenantIdAndCertificateHash(tenantId, certificateHash),
                true) != null; // caching null values is essential: almost every lookup is for a certificate that is not revoked
    }

    @Override
    public DeviceCertificateRevocation findRevocationByCertificatePem(TenantId tenantId, String certificatePem) {
        log.trace("[{}] Executing findRevocationByCertificatePem", tenantId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        String certificateHash = getCertificateHash(parseCertificate(certificatePem));
        return cache.getAndPutInTransaction(DeviceCertificateRevocationCacheKey.of(tenantId, certificateHash),
                () -> revocationDao.findByTenantIdAndCertificateHash(tenantId, certificateHash),
                true);
    }

    @Override
    public DeviceCertificateRevocation findRevocationById(TenantId tenantId, DeviceCertificateRevocationId revocationId) {
        log.trace("[{}] Executing findRevocationById [{}]", tenantId, revocationId);
        validateId(revocationId, id -> INCORRECT_REVOCATION_ID + id);
        return revocationDao.findById(tenantId, revocationId.getId());
    }

    @Override
    public PageData<DeviceCertificateRevocation> findRevocationsByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("[{}] Executing findRevocationsByTenantId, pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validatePageLink(pageLink, ALLOWED_SORT_PROPERTIES);
        return revocationDao.findByTenantId(tenantId, pageLink);
    }

    @Override
    public void deleteRevocation(TenantId tenantId, DeviceCertificateRevocationId revocationId) {
        log.trace("[{}] Executing deleteRevocation [{}]", tenantId, revocationId);
        validateId(revocationId, id -> INCORRECT_REVOCATION_ID + id);
        DeviceCertificateRevocation revocation = revocationDao.findById(tenantId, revocationId.getId());
        if (revocation == null) {
            return;
        }
        revocationDao.removeById(tenantId, revocationId.getId());
        publishEvictEvent(new DeviceCertificateRevocationEvictEvent(revocation.getTenantId(), revocation.getCertificateHash()));
    }

    @Override
    public void deleteRevocationsByTenantId(TenantId tenantId) {
        log.trace("[{}] Executing deleteRevocationsByTenantId", tenantId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        // Cached entries of a deleted tenant are left to expire: nothing can authenticate within that tenant anymore.
        revocationDao.removeByTenantId(tenantId);
    }

    @Override
    public int deleteRevocationsExpiredBefore(long ts) {
        // Cached "revoked" entries are left to expire: they keep an already expired certificate blocked a bit longer.
        return revocationDao.deleteAllByNotAfterBefore(ts);
    }

    private DeviceId findBoundDeviceId(TenantId tenantId, String certificateHash) {
        // X.509 and LwM2M X.509 credentials with a stored certificate use the certificate hash as credentials id
        DeviceCredentials credentials = deviceCredentialsService.findDeviceCredentialsByCredentialsId(certificateHash);
        if (credentials == null || credentials.getDeviceId() == null) {
            return null;
        }
        Device device = deviceService.findDeviceById(tenantId, credentials.getDeviceId());
        return device != null && tenantId.equals(device.getTenantId()) ? device.getId() : null;
    }

    private static X509Certificate parseCertificate(String certificatePem) {
        if (StringUtils.isBlank(certificatePem)) {
            throw new DataValidationException("Certificate should be specified!");
        }
        List<X509Certificate> certificates;
        try {
            // PEM pasted from e-mails or chats often comes indented, which the PEM reader rejects
            certificates = SslUtil.readCertFile(certificatePem.lines().map(String::trim).collect(Collectors.joining("\n")));
        } catch (Exception e) {
            throw new DataValidationException("Invalid certificate: " + e.getMessage());
        }
        if (certificates == null || certificates.isEmpty()) {
            throw new DataValidationException("Invalid certificate: no X.509 certificate found in PEM!");
        }
        if (certificates.size() > 1) {
            throw new DataValidationException("Specify a single certificate, not a certificate chain!");
        }
        return certificates.get(0);
    }

    /**
     * Same value as the transports compute for a presented certificate and as X.509 device credentials use as
     * credentials id: SHA3-256 of the Base64-encoded DER. Computed from the parsed certificate, never from the
     * submitted text, so that whitespace or line endings in the PEM can't produce a hash that never matches.
     */
    private static String getCertificateHash(X509Certificate certificate) {
        try {
            return EncryptionUtil.getSha3Hash(Base64.getEncoder().encodeToString(certificate.getEncoded()));
        } catch (CertificateEncodingException e) {
            throw new DataValidationException("Invalid certificate: " + e.getMessage());
        }
    }

}
