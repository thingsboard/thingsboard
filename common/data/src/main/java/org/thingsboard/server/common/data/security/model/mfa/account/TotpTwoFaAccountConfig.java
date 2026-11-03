// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.security.model.mfa.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.security.model.mfa.provider.TwoFaProviderType;

@Data
@EqualsAndHashCode(callSuper = true)
public class TotpTwoFaAccountConfig extends TwoFaAccountConfig {

    @NotBlank
    @Pattern(regexp = "otpauth://totp/(\\S+?):(\\S+?)\\?issuer=(\\S+?)&secret=(\\w+?)", message = "is invalid")
    private String authUrl;

    @Override
    public TwoFaProviderType getProviderType() {
        return TwoFaProviderType.TOTP;
    }

}

