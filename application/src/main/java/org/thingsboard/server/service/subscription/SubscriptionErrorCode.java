// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.subscription;

public enum SubscriptionErrorCode {

    NO_ERROR(0), INTERNAL_ERROR(1, "Internal Server error!"), BAD_REQUEST(2, "Bad request"), UNAUTHORIZED(3, "Unauthorized");

    private final int code;
    private final String defaultMsg;

    private SubscriptionErrorCode(int code) {
        this(code, null);
    }

    private SubscriptionErrorCode(int code, String defaultMsg) {
        this.code = code;
        this.defaultMsg = defaultMsg;
    }

    public static SubscriptionErrorCode forCode(int code) {
        for (SubscriptionErrorCode errorCode : SubscriptionErrorCode.values()) {
            if (errorCode.getCode() == code) {
                return errorCode;
            }
        }
        throw new IllegalArgumentException("Invalid error code: " + code);
    }

    public int getCode() {
        return code;
    }

    public String getDefaultMsg() {
        return defaultMsg;
    }
}
