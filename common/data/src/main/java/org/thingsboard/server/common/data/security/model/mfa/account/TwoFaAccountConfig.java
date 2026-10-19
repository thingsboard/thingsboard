// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.security.model.mfa.account;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.thingsboard.server.common.data.security.model.mfa.provider.TwoFaProviderType;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "providerType")
@JsonSubTypes({
        @Type(name = "TOTP", value = TotpTwoFaAccountConfig.class),
        @Type(name = "SMS", value = SmsTwoFaAccountConfig.class),
        @Type(name = "EMAIL", value = EmailTwoFaAccountConfig.class),
        @Type(name = "BACKUP_CODE", value = BackupCodeTwoFaAccountConfig.class)
})
@Schema(
        description = "Base configuration for two-factor authentication accounts",
        discriminatorProperty = "providerType",
        discriminatorMapping = {
                @DiscriminatorMapping(value = "TOTP", schema = TotpTwoFaAccountConfig.class),
                @DiscriminatorMapping(value = "SMS", schema = SmsTwoFaAccountConfig.class),
                @DiscriminatorMapping(value = "EMAIL", schema = EmailTwoFaAccountConfig.class),
                @DiscriminatorMapping(value = "BACKUP_CODE", schema = BackupCodeTwoFaAccountConfig.class)
        }
)
@Data
public abstract class TwoFaAccountConfig implements Serializable {

    private boolean useByDefault;

    @JsonIgnore
    protected transient boolean serializeHiddenFields;

    @JsonIgnore
    public abstract TwoFaProviderType getProviderType();

}
