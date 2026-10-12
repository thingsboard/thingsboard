// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.model.sql;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.id.DeviceCertificateRevocationId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.DeviceCertificateRevocation;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Entity
@Table(name = ModelConstants.DEVICE_CERTIFICATE_REVOCATION_TABLE_NAME)
public class DeviceCertificateRevocationEntity extends BaseSqlEntity<DeviceCertificateRevocation> {

    @Column(name = ModelConstants.TENANT_ID_COLUMN, columnDefinition = "uuid")
    private UUID tenantId;

    @Column(name = ModelConstants.DEVICE_ID_PROPERTY, columnDefinition = "uuid")
    private UUID deviceId;

    @Column(name = ModelConstants.DEVICE_CERTIFICATE_REVOCATION_CERTIFICATE_HASH_PROPERTY)
    private String certificateHash;

    @Column(name = ModelConstants.DEVICE_CERTIFICATE_REVOCATION_SERIAL_NUMBER_PROPERTY)
    private String serialNumber;

    @Column(name = ModelConstants.DEVICE_CERTIFICATE_REVOCATION_ISSUER_PROPERTY)
    private String issuer;

    @Column(name = ModelConstants.DEVICE_CERTIFICATE_REVOCATION_SUBJECT_PROPERTY)
    private String subject;

    @Column(name = ModelConstants.DEVICE_CERTIFICATE_REVOCATION_NOT_BEFORE_PROPERTY)
    private long notBefore;

    @Column(name = ModelConstants.DEVICE_CERTIFICATE_REVOCATION_NOT_AFTER_PROPERTY)
    private long notAfter;

    @Column(name = ModelConstants.DEVICE_CERTIFICATE_REVOCATION_REASON_PROPERTY)
    private String reason;

    @Column(name = ModelConstants.DEVICE_CERTIFICATE_REVOCATION_REVOKED_BY_PROPERTY, columnDefinition = "uuid")
    private UUID revokedBy;

    public DeviceCertificateRevocationEntity(DeviceCertificateRevocation revocation) {
        this.setId(revocation.getUuidId());
        this.setCreatedTime(revocation.getCreatedTime());
        this.tenantId = revocation.getTenantId().getId();
        if (revocation.getDeviceId() != null) {
            this.deviceId = revocation.getDeviceId().getId();
        }
        this.certificateHash = revocation.getCertificateHash();
        this.serialNumber = revocation.getSerialNumber();
        this.issuer = revocation.getIssuer();
        this.subject = revocation.getSubject();
        this.notBefore = revocation.getNotBefore();
        this.notAfter = revocation.getNotAfter();
        this.reason = revocation.getReason();
        if (revocation.getRevokedBy() != null) {
            this.revokedBy = revocation.getRevokedBy().getId();
        }
    }

    @Override
    public DeviceCertificateRevocation toData() {
        DeviceCertificateRevocation revocation = new DeviceCertificateRevocation(new DeviceCertificateRevocationId(getUuid()));
        revocation.setCreatedTime(createdTime);
        revocation.setTenantId(TenantId.fromUUID(tenantId));
        if (deviceId != null) {
            revocation.setDeviceId(new DeviceId(deviceId));
        }
        revocation.setCertificateHash(certificateHash);
        revocation.setSerialNumber(serialNumber);
        revocation.setIssuer(issuer);
        revocation.setSubject(subject);
        revocation.setNotBefore(notBefore);
        revocation.setNotAfter(notAfter);
        revocation.setReason(reason);
        if (revokedBy != null) {
            revocation.setRevokedBy(new UserId(revokedBy));
        }
        return revocation;
    }

}
