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
package org.thingsboard.server.service.security.auth.mfa.config.account;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.thingsboard.server.service.security.auth.mfa.provider.TwoFactorAuthProviderType;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "providerType")
@JsonSubTypes({
        @JsonSubTypes.Type(value = TotpTwoFactorAuthAccountConfig.class, name = "TOTP"),
        @JsonSubTypes.Type(value = SmsTwoFactorAuthAccountConfig.class, name = "SMS"),
})
public interface TwoFactorAuthAccountConfig {

    @JsonIgnore
    TwoFactorAuthProviderType getProviderType();

}
