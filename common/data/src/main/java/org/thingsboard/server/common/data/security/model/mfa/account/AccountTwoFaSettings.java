// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.security.model.mfa.account;

import lombok.Data;
import org.thingsboard.server.common.data.security.model.mfa.provider.TwoFaProviderType;

import java.util.LinkedHashMap;

@Data
public class AccountTwoFaSettings {
    private LinkedHashMap<TwoFaProviderType, TwoFaAccountConfig> configs;
}
