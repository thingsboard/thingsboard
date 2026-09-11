// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.security.model.mfa.provider;

public enum TwoFaProviderType {
    TOTP,
    SMS,
    EMAIL,
    BACKUP_CODE
}
