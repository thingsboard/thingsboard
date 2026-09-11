// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.security.model.mfa.provider;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public abstract class OtpBasedTwoFaProviderConfig implements TwoFaProviderConfig {

    @Min(value = 1, message = "is required")
    private int verificationCodeLifetime;

}
