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

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;
import org.thingsboard.server.service.security.auth.mfa.provider.TwoFactorAuthProviderType;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Email;

@EqualsAndHashCode(callSuper = true)
@Data
public class EmailTwoFactorAuthAccountConfig extends OtpBasedTwoFactorAuthAccountConfig {

    private boolean useAccountEmail;
    @Email(message = "Email is not valid")
    private String email;

    @Override
    public TwoFactorAuthProviderType getProviderType() {
        return TwoFactorAuthProviderType.EMAIL;
    }


    @AssertTrue(message = "Email must be specified") // TODO [viacheslav]: test !
    private boolean isValid() {
        return useAccountEmail || StringUtils.isNotEmpty(email);
    }

}
