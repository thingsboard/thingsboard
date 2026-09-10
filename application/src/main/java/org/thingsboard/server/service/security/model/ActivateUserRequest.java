// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.security.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema
@Data
public class ActivateUserRequest {

    @Schema(description = "The activate token to verify", example = "AAB254FF67D..")
    private String activateToken;
    @Schema(description = "The new password to set", example = "secret")
    private String password;
}
