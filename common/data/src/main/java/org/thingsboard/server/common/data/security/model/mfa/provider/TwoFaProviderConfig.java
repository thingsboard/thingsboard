// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.security.model.mfa.provider;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "providerType")
@JsonSubTypes({
        @Type(name = "TOTP", value = TotpTwoFaProviderConfig.class),
        @Type(name = "SMS", value = SmsTwoFaProviderConfig.class),
        @Type(name = "EMAIL", value = EmailTwoFaProviderConfig.class),
        @Type(name = "BACKUP_CODE", value = BackupCodeTwoFaProviderConfig.class)
})
public interface TwoFaProviderConfig {

    @JsonIgnore
    TwoFaProviderType getProviderType();

}
