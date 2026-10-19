// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.security.model.mfa.account;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.thingsboard.server.common.data.security.model.mfa.provider.TwoFaProviderType;

import java.util.LinkedHashMap;

@Data
@Schema(description = "Account Two-Factor Authentication Settings")
public class AccountTwoFaSettings {
    private LinkedHashMap<TwoFaProviderType, TwoFaAccountConfig> configs;
}
