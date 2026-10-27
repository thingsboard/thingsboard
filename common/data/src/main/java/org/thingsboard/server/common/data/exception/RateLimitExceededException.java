// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.exception;

import org.thingsboard.server.common.data.limit.LimitedApi;

public class RateLimitExceededException extends AbstractRateLimitException {

    public RateLimitExceededException(String message) {
        super(message);
    }

    public RateLimitExceededException(LimitedApi api) {
        super("Rate limit for " + api.getLabel() + " is exceeded");
    }

}
