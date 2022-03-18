/**
 * Copyright © 2016-2022 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.security.auth.mfa.config;

import lombok.Data;
import org.checkerframework.checker.index.qual.NonNegative;
import org.thingsboard.server.service.security.auth.mfa.config.provider.TwoFactorAuthProviderConfig;
import org.thingsboard.server.service.security.auth.mfa.provider.TwoFactorAuthProviderType;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.List;
import java.util.Optional;

@Data
public class TwoFactorAuthSettings {

    @NotNull
    private Boolean useSystemTwoFactorAuthSettings;
    @Valid
    private List<TwoFactorAuthProviderConfig> providers;

    @Pattern(regexp = "\\d+:\\d+")
    private String verificationCodeSendRateLimit; // 1:60 - one time in a minute
    @Pattern(regexp = "\\d+:\\d+")
    private String verificationCodeCheckRateLimit; // soft lockout, on session level
    @Min(0)
    private Integer maxVerificationCodeSubmitAttemptsBeforeUserBlocking;


    public Optional<TwoFactorAuthProviderConfig> getProviderConfig(TwoFactorAuthProviderType providerType) {
        return Optional.ofNullable(providers)
                .flatMap(providersConfigs -> providersConfigs.stream()
                        .filter(providerConfig -> providerConfig.getProviderType() == providerType)
                        .findFirst());
    }

}
