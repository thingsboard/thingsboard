// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.security;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.id.DeviceCertificateRevocationId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

import java.io.Serial;

@Schema
@Data
@EqualsAndHashCode(callSuper = true)
public class DeviceCertificateRevocation extends BaseData<DeviceCertificateRevocationId> implements HasTenantId {

    @Serial
    private static final long serialVersionUID = 4625735112893458123L;

    @Schema(description = "JSON object with Tenant Id.", accessMode = Schema.AccessMode.READ_ONLY)
    private TenantId tenantId;
    @Schema(description = "JSON object with Id of the device the certificate was bound to when it was revoked. " +
            "Empty when the certificate was revoked by its PEM and no device used it at that moment.", accessMode = Schema.AccessMode.READ_ONLY)
    private DeviceId deviceId;
    @Schema(description = "SHA3-256 hash of the DER-encoded certificate. Same value as the credentials id of X.509 device credentials.",
            accessMode = Schema.AccessMode.READ_ONLY)
    private String certificateHash;
    @Schema(description = "Certificate serial number in hexadecimal form.", accessMode = Schema.AccessMode.READ_ONLY)
    private String serialNumber;
    @Schema(description = "Distinguished name of the certificate issuer.", accessMode = Schema.AccessMode.READ_ONLY)
    private String issuer;
    @Schema(description = "Distinguished name of the certificate subject.", accessMode = Schema.AccessMode.READ_ONLY)
    private String subject;
    @Schema(description = "Start of the certificate validity period, in milliseconds since epoch.", accessMode = Schema.AccessMode.READ_ONLY)
    private long notBefore;
    @Schema(description = "End of the certificate validity period, in milliseconds since epoch.", accessMode = Schema.AccessMode.READ_ONLY)
    private long notAfter;
    @NoXss
    @Length(fieldName = "reason")
    @Schema(description = "Optional reason of the revocation.", example = "Device lost")
    private String reason;
    @Schema(description = "JSON object with Id of the user who revoked the certificate.", accessMode = Schema.AccessMode.READ_ONLY)
    private UserId revokedBy;

    public DeviceCertificateRevocation() {
        super();
    }

    public DeviceCertificateRevocation(DeviceCertificateRevocationId id) {
        super(id);
    }

    public DeviceCertificateRevocation(DeviceCertificateRevocation revocation) {
        super(revocation);
        this.tenantId = revocation.getTenantId();
        this.deviceId = revocation.getDeviceId();
        this.certificateHash = revocation.getCertificateHash();
        this.serialNumber = revocation.getSerialNumber();
        this.issuer = revocation.getIssuer();
        this.subject = revocation.getSubject();
        this.notBefore = revocation.getNotBefore();
        this.notAfter = revocation.getNotAfter();
        this.reason = revocation.getReason();
        this.revokedBy = revocation.getRevokedBy();
    }

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Schema(description = "Indicates that the certificate validity period has ended.", accessMode = Schema.AccessMode.READ_ONLY)
    public boolean isExpired() {
        return System.currentTimeMillis() > notAfter;
    }

}
