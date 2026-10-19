// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.exception;

public class ApiUsageLimitsExceededException extends AbstractRateLimitException {
    public ApiUsageLimitsExceededException(String message) {
        super(message);
    }

    public ApiUsageLimitsExceededException() {
    }
}
