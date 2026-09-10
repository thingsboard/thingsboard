// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.security.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema
@Data
public class ResetPasswordRequest {

    @Schema(description = "The reset token to verify", example = "AAB254FF67D..")
    private String resetToken;
    @Schema(description = "The new password to set", example = "secret")
    private String password;
}
