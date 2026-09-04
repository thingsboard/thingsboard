// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.security.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Schema
@Data
public class SecuritySettings implements Serializable {

    @Serial
    private static final long serialVersionUID = -1307613974597312465L;

    @Schema(description = "The user password policy object.")
    private UserPasswordPolicy passwordPolicy;

    @Schema(description = "Maximum number of failed login attempts allowed before user account is locked.")
    private Integer maxFailedLoginAttempts;

    @Schema(description = "Email to use for notifications about locked users.")
    private String userLockoutNotificationEmail;

    @Schema(description = "Mobile secret key length")
    private Integer mobileSecretKeyLength;

    @NotNull @Min(1) @Max(24)
    @Schema(description = "TTL in hours for user activation link", minimum = "1", maximum = "24", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer userActivationTokenTtl;

    @NotNull @Min(1) @Max(24)
    @Schema(description = "TTL in hours for password reset link", minimum = "1", maximum = "24", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer passwordResetTokenTtl;

}
