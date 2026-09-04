// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.security.model.mfa.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.security.model.mfa.provider.TwoFaProviderType;

@EqualsAndHashCode(callSuper = true)
@Data
public class SmsTwoFaAccountConfig extends OtpBasedTwoFaAccountConfig {

    @NotBlank
    @Pattern(regexp = "^\\+[1-9]\\d{1,14}$", message = "is not of E.164 format")
    private String phoneNumber;

    @Override
    public TwoFaProviderType getProviderType() {
        return TwoFaProviderType.SMS;
    }

}
