// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.security.model.mfa.provider;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        description = "Two-factor authentication provider configuration",
        discriminatorProperty = "providerType",
        discriminatorMapping = {
                @DiscriminatorMapping(value = "TOTP", schema = TotpTwoFaProviderConfig.class),
                @DiscriminatorMapping(value = "SMS", schema = SmsTwoFaProviderConfig.class),
                @DiscriminatorMapping(value = "EMAIL", schema = EmailTwoFaProviderConfig.class),
                @DiscriminatorMapping(value = "BACKUP_CODE", schema = BackupCodeTwoFaProviderConfig.class)
        }
)
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
