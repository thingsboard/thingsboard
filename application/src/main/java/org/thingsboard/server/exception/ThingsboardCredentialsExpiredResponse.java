// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;

@Schema
public class ThingsboardCredentialsExpiredResponse extends ThingsboardErrorResponse {

    private final String resetToken;

    protected ThingsboardCredentialsExpiredResponse(String message, String resetToken) {
        super(message, ThingsboardErrorCode.CREDENTIALS_EXPIRED, HttpStatus.UNAUTHORIZED);
        this.resetToken = resetToken;
    }

    public static ThingsboardCredentialsExpiredResponse of(final String message, final String resetToken) {
        return new ThingsboardCredentialsExpiredResponse(message, resetToken);
    }

    @Schema(description = "Password reset token", accessMode = Schema.AccessMode.READ_ONLY)
    public String getResetToken() {
        return resetToken;
    }
}
