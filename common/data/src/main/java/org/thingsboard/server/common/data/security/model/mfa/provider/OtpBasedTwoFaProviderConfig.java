// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.security.model.mfa.provider;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
@Schema
public abstract class OtpBasedTwoFaProviderConfig implements TwoFaProviderConfig {

    @Min(value = 1, message = "is required")
    private int verificationCodeLifetime;

}
