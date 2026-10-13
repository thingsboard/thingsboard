// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.exception;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.subscription.SubscriptionEntry;
import org.thingsboard.server.common.data.subscription.SubscriptionErrorCode;

@Schema
public class ThingsboardErrorResponse {
    // HTTP Response Status Code
    private final HttpStatus status;

    // General Error message
    private final String message;

    // Error code
    private final ThingsboardErrorCode errorCode;

    private final long timestamp;

    private SubscriptionErrorCode subscriptionErrorCode;

    private SubscriptionEntry subscriptionEntry;

    private JsonNode subscriptionValue;

    protected ThingsboardErrorResponse(final String message, final ThingsboardErrorCode errorCode, HttpStatus status) {
        this(message, errorCode, null, null, null, status);
    }

    protected ThingsboardErrorResponse(final String message, final ThingsboardErrorCode errorCode, SubscriptionErrorCode subscriptionErrorCode,
                                       SubscriptionEntry subscriptionEntry, JsonNode subscriptionValue, HttpStatus status) {
        this.message = message;
        this.errorCode = errorCode;
        this.subscriptionErrorCode = subscriptionErrorCode;
        this.subscriptionEntry = subscriptionEntry;
        this.subscriptionValue = subscriptionValue;
        this.status = status;
        this.timestamp = System.currentTimeMillis();
    }

    public static ThingsboardErrorResponse of(final String message, final ThingsboardErrorCode errorCode, HttpStatus status) {
        return new ThingsboardErrorResponse(message, errorCode, status);
    }

    public static ThingsboardErrorResponse ofSubscriptionViolation(final String message,
                                                                   SubscriptionErrorCode subscriptionErrorCode,
                                                                   SubscriptionEntry subscriptionEntry,
                                                                   JsonNode subscriptionValue,
                                                                   HttpStatus status) {
        return new ThingsboardErrorResponse(message, ThingsboardErrorCode.SUBSCRIPTION_VIOLATION,
                subscriptionErrorCode, subscriptionEntry, subscriptionValue, status);
    }

    @Schema(description = "HTTP Response Status Code", example = "401", accessMode = Schema.AccessMode.READ_ONLY)
    public Integer getStatus() {
        return status.value();
    }

    @Schema(description = "Error message", example = "Authentication failed", accessMode = Schema.AccessMode.READ_ONLY)
    public String getMessage() {
        return message;
    }

    @Schema(description = "Platform error code:" +
            "\n* `2` - General error (HTTP: 500 - Internal Server Error)" +
            "\n\n* `10` - Authentication failed (HTTP: 401 - Unauthorized)" +
            "\n\n* `11` - JWT token expired (HTTP: 401 - Unauthorized)" +
            "\n\n* `15` - Credentials expired (HTTP: 401 - Unauthorized)" +
            "\n\n* `20` - Permission denied (HTTP: 403 - Forbidden)" +
            "\n\n* `30` - Invalid arguments (HTTP: 400 - Bad Request)" +
            "\n\n* `31` - Bad request params (HTTP: 400 - Bad Request)" +
            "\n\n* `32` - Item not found (HTTP: 404 - Not Found)" +
            "\n\n* `33` - Too many requests (HTTP: 429 - Too Many Requests)" +
            "\n\n* `34` - Too many updates (Too many updates over Websocket session)" +
            "\n\n* `35` - Version conflict (HTTP: 409 - Conflict)" +
            "\n\n* `40` - Subscription violation (HTTP: 403 - Forbidden)" +
            "\n\n* `41` - Entities limit exceeded (HTTP: 403 - Forbidden)" +
            "\n\n* `47` - Setup incomplete (HTTP: 423 - Locked)",
            example = "10", type = "integer",
            accessMode = Schema.AccessMode.READ_ONLY)
    public ThingsboardErrorCode getErrorCode() {
        return errorCode;
    }

    @Schema(description = "Timestamp", accessMode = Schema.AccessMode.READ_ONLY)
    public long getTimestamp() {
        return timestamp;
    }

    public SubscriptionErrorCode getSubscriptionErrorCode() {
        return subscriptionErrorCode;
    }

    public SubscriptionEntry getSubscriptionEntry() {
        return subscriptionEntry;
    }

    public JsonNode getSubscriptionValue() {
        return subscriptionValue;
    }

}
