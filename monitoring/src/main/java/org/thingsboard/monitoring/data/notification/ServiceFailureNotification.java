// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.monitoring.data.notification;

import lombok.Getter;
import org.apache.commons.lang3.exception.ExceptionUtils;

@Getter
public class ServiceFailureNotification implements Notification {

    private final Object serviceKey;
    private final Throwable error;
    private final int failuresCount;

    public ServiceFailureNotification(Object serviceKey, Throwable error, int failuresCount) {
        this.serviceKey = serviceKey;
        this.error = error;
        this.failuresCount = failuresCount;
    }

    @Override
    public String getText() {
        String errorMsg = error.getMessage();
        if (errorMsg == null || errorMsg.equals("null")) {
            Throwable cause = ExceptionUtils.getRootCause(error);
            if (cause != null) {
                errorMsg = cause.getMessage();
            }
        }
        if (errorMsg == null) {
            errorMsg = error.getClass().getSimpleName();
        }
        return String.format("%s - Failure: %s (number of subsequent failures: %s)", serviceKey, errorMsg, failuresCount);
    }

}
