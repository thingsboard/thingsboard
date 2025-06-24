/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.common.data.security.model.mfa.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.security.model.mfa.provider.TwoFaProviderType;

@EqualsAndHashCode(callSuper = true)
@Data
public class SmsTwoFaAccountConfig extends OtpBasedTwoFaAccountConfig {

    @NotBlank
    @Pattern(regexp = "^\\+[1-9]\\d{1,14}$", message = "is not of E.164 format")
    private String phoneNumber;

    @Override
    public TwoFaProviderType getProviderType() {
        return TwoFaProviderType.SMS;
    }

}
