// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.security.model.mfa.provider;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class SmsTwoFaProviderConfig extends OtpBasedTwoFaProviderConfig {

    @NotBlank(message = "is required")
    @Pattern(regexp = ".*\\$\\{code}.*", message = "must contain verification code")
    private String smsVerificationMessageTemplate;

    @Override
    public TwoFaProviderType getProviderType() {
        return TwoFaProviderType.SMS;
    }

}
