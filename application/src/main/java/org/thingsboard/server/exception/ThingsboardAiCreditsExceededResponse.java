// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;

@Schema
public class ThingsboardAiCreditsExceededResponse extends ThingsboardErrorResponse {

    private final String aiCreditsLimit;

    protected ThingsboardAiCreditsExceededResponse(String message, String aiCreditsLimit) {
        super(message, ThingsboardErrorCode.TOO_MANY_REQUESTS, HttpStatus.TOO_MANY_REQUESTS);
        this.aiCreditsLimit = aiCreditsLimit;
    }

    public static ThingsboardAiCreditsExceededResponse of(String message, String aiCreditsLimit) {
        return new ThingsboardAiCreditsExceededResponse(message, aiCreditsLimit);
    }

    @Schema(description = "Which AI credits limit is used up: TENANT for the tenant's monthly limit", accessMode = Schema.AccessMode.READ_ONLY)
    public String getAiCreditsLimit() {
        return aiCreditsLimit;
    }

}
