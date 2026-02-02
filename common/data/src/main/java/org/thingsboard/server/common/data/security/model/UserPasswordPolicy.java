/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.common.data.security.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Schema
@Data
public class UserPasswordPolicy implements Serializable {

    @Schema(description = "Minimum number of symbols in the password." )
    private Integer minimumLength;
    @Schema(description = "Maximum number of symbols in the password." )
    private Integer maximumLength;
    @Schema(description = "Minimum number of uppercase letters in the password." )
    private Integer minimumUppercaseLetters;
    @Schema(description = "Minimum number of lowercase letters in the password." )
    private Integer minimumLowercaseLetters;
    @Schema(description = "Minimum number of digits in the password." )
    private Integer minimumDigits;
    @Schema(description = "Minimum number of special in the password." )
    private Integer minimumSpecialCharacters;
    @Schema(description = "Allow whitespaces")
    private Boolean allowWhitespaces = true;
    @Schema(description = "Force user to update password if existing one does not pass validation")
    private Boolean forceUserToResetPasswordIfNotValid = false;

    @Schema(description = "Password expiration period (days). Force expiration of the password." )
    private Integer passwordExpirationPeriodDays;
    @Schema(description = "Password reuse frequency (days). Disallow to use the same password for the defined number of days" )
    private Integer passwordReuseFrequencyDays;

}
