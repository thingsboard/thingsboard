/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
