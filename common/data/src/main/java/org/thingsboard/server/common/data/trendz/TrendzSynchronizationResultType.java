// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.trendz;

import lombok.Getter;

@Getter
public enum TrendzSynchronizationResultType {

    SYNC_NOT_INITIALIZED("Trendz synchronization is not initialized."),

    SYNC_COMPLETED("Synchronization completed successfully."),

    SYNC_DISABLED("Synchronization is disabled by Trendz configuration."),

    TRENDZ_UNSUPPORTED_VERSION("Trendz version is not supported."),

    TRENDZ_AUTH_INVALID("Trendz authentication failed. Invalid or missing Trendz API key."),

    TRENDZ_URL_UNREACHABLE("Provided Trendz URL is not reachable."),

    TB_URL_MISMATCH("Provided ThingsBoard URL does not match the one stored in ThingsBoard."),

    TB_URL_UNREACHABLE("ThingsBoard URL is not reachable."),

    TB_AUTH_INVALID("ThingsBoard authentication failed. Invalid API key."),

    SYNC_INTERNAL_ERROR("Unexpected internal synchronization error.");

    private final String message;

    TrendzSynchronizationResultType(String message) {
        this.message = message;
    }

}
