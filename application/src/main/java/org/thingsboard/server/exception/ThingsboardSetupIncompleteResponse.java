// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.setup.SystemSetupState;

@Schema
public class ThingsboardSetupIncompleteResponse extends ThingsboardErrorResponse {

    private final SystemSetupState setupState;

    protected ThingsboardSetupIncompleteResponse(String message, SystemSetupState setupState, HttpStatus status) {
        super(message, ThingsboardErrorCode.SETUP_INCOMPLETE, status);
        this.setupState = setupState;
    }

    public static ThingsboardSetupIncompleteResponse of(final String message, final SystemSetupState setupState, final HttpStatus status) {
        return new ThingsboardSetupIncompleteResponse(message, setupState, status);
    }

    @Schema(description = "Current system setup state", example = "LICENSE_REQUIRED", accessMode = Schema.AccessMode.READ_ONLY)
    public SystemSetupState getSetupState() {
        return setupState;
    }

}
