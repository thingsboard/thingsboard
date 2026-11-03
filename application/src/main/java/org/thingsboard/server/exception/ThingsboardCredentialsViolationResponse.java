// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;

@Schema
public class ThingsboardCredentialsViolationResponse extends ThingsboardErrorResponse {

    protected ThingsboardCredentialsViolationResponse(String message) {
        super(message, ThingsboardErrorCode.PASSWORD_VIOLATION, HttpStatus.UNAUTHORIZED);
    }

    public static ThingsboardCredentialsViolationResponse of(final String message) {
        return new ThingsboardCredentialsViolationResponse(message);
    }

}
