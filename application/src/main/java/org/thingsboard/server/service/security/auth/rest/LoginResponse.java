// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.security.auth.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema
@Data
public class LoginResponse {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "JWT token",
            example = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ0ZW5hbnRAdGhpbmdzYm9hcmQub3JnIi...")
    private String token;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Refresh token",
            example = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ0ZW5hbnRAdGhpbmdzYm9hcmQub3JnIi...")
    private String refreshToken;

}
