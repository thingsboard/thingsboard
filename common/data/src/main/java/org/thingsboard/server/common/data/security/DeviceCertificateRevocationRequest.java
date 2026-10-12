// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.security;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

@Schema
@Data
public class DeviceCertificateRevocationRequest {

    @Schema(description = "PEM of a single X.509 device certificate. Not used when revoking the certificate stored in the device credentials.")
    private String certificate;

    @NoXss
    @Length(fieldName = "reason")
    @Schema(description = "Optional reason of the revocation.", example = "Device lost")
    private String reason;

}
