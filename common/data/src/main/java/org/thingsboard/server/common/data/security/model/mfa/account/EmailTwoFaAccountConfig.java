// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.security.model.mfa.account;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.security.model.mfa.provider.TwoFaProviderType;

@Data
@EqualsAndHashCode(callSuper = true)
public class EmailTwoFaAccountConfig extends OtpBasedTwoFaAccountConfig {

    @NotBlank
    @Email
    private String email;

    @Override
    public TwoFaProviderType getProviderType() {
        return TwoFaProviderType.EMAIL;
    }

}
