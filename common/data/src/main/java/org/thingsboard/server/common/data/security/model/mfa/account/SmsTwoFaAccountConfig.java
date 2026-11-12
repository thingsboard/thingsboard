// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.common.data.security.model.mfa.account;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.security.model.mfa.provider.TwoFaProviderType;

@Schema
@EqualsAndHashCode(callSuper = true)
@Data
public class SmsTwoFaAccountConfig extends OtpBasedTwoFaAccountConfig {

    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @NotBlank
    @Pattern(regexp = "^\\+[1-9]\\d{1,14}$", message = "is not of E.164 format")
    private String phoneNumber;

    @Override
    public TwoFaProviderType getProviderType() {
        return TwoFaProviderType.SMS;
    }

}
