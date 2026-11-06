// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.security.auth.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.thingsboard.server.common.data.security.Authority;

/**
 * Documents the body that {@code /api/auth/login} and {@code /api/auth/token} write: a
 * {@link org.thingsboard.server.common.data.security.model.JwtPair} serialized without null fields.
 */
@Schema
@Data
public class LoginResponse {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "JWT token",
            example = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ0ZW5hbnRAdGhpbmdzYm9hcmQub3JnIi...")
    private String token;

    @Schema(description = "Refresh token. Absent when the token is a two-factor authentication token (see scope)",
            example = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ0ZW5hbnRAdGhpbmdzYm9hcmQub3JnIi...")
    private String refreshToken;

    @Schema(description = "Set only when two-factor authentication is required: PRE_VERIFICATION_TOKEN when the user " +
            "must verify a code, MFA_CONFIGURATION_TOKEN when the user must configure two-factor authentication first. " +
            "The token then grants access only to the two-factor authentication API")
    private Authority scope;

}
