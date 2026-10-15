// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.security;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema
@Data
public class DeviceCertificateInfo {

    @Schema(description = "Distinguished name of the certificate subject.")
    private String subject;
    @Schema(description = "Distinguished name of the certificate issuer.")
    private String issuer;
    @Schema(description = "Certificate serial number in hexadecimal form.")
    private String serialNumber;
    @Schema(description = "Start of the certificate validity period, in milliseconds since epoch.")
    private long notBefore;
    @Schema(description = "End of the certificate validity period, in milliseconds since epoch.")
    private long notAfter;
    @Schema(description = "SHA-256 fingerprint of the DER-encoded certificate, colon-separated hexadecimal.")
    private String sha256Fingerprint;
    @Schema(description = "Revocation of the certificate, if it is revoked.")
    private DeviceCertificateRevocation revocation;

}
