// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
